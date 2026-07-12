from __future__ import annotations

import hashlib
import struct
import tempfile
import zlib
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .compression import decompress_compressed_buffer, read_compressed_buffer_header
from .editor_bulk import (
    LEGACY_COMPRESSED_ZLIB,
    LEGACY_FORCE_INLINE,
    LEGACY_NO_OFFSET_FIXUP,
    LEGACY_OPTIONAL_PAYLOAD,
    LEGACY_PAYLOAD_AT_END,
    LEGACY_SEPARATE_FILE,
    match_trailer_entry,
    parse_editor_bulk_data,
    parse_legacy_bulk_data,
)
from .errors import BoundsError, FormatError, UnsupportedError
from .extract import extract_verified
from .package import UnrealPackage
from .properties import PropertyParser
from .trailer import load_local_payload, read_package_trailer


GUID_UE5_MAIN = "697dd581-e64f41ab-aa4a51ec-beb7b628"
PACKAGE_FILE_TAG_BYTES = b"\xc1\x83\x2a\x9e"


def validate_png(data: bytes) -> dict[str, Any]:
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise FormatError("PNG signature is absent")
    position = 8
    chunks: list[str] = []
    width = height = bit_depth = color_type = None
    saw_iend = False
    while position < len(data):
        if position + 12 > len(data):
            raise FormatError("truncated PNG chunk header")
        length = int.from_bytes(data[position:position + 4], "big")
        kind = data[position + 4:position + 8]
        end = position + 12 + length
        if end > len(data):
            raise FormatError("PNG chunk leaves file boundary")
        payload = data[position + 8:position + 8 + length]
        expected_crc = int.from_bytes(data[position + 8 + length:end], "big")
        actual_crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        if expected_crc != actual_crc:
            raise FormatError(f"PNG {kind!r} CRC mismatch")
        name = kind.decode("ascii", "strict")
        chunks.append(name)
        if kind == b"IHDR":
            if position != 8 or length != 13:
                raise FormatError("PNG IHDR position/length is invalid")
            width, height, bit_depth, color_type = struct.unpack(">IIBB", payload[:10])
            if width <= 0 or height <= 0:
                raise FormatError("PNG dimensions are invalid")
        if kind == b"IEND":
            if length != 0 or end != len(data):
                raise FormatError("PNG IEND is not the final empty chunk")
            saw_iend = True
        position = end
    if not saw_iend or width is None or "IDAT" not in chunks:
        raise FormatError("PNG lacks IHDR/IDAT/IEND")
    chunk_counts = {name: chunks.count(name) for name in dict.fromkeys(chunks)}
    return {"format": "PNG", "width": width, "height": height, "bit_depth": bit_depth, "color_type": color_type, "chunk_count": len(chunks), "chunk_counts": chunk_counts}


def validate_wav(data: bytes) -> dict[str, Any]:
    if len(data) < 12 or data[:4] != b"RIFF" or data[8:12] != b"WAVE":
        raise FormatError("RIFF/WAVE header is invalid")
    declared = int.from_bytes(data[4:8], "little") + 8
    if declared != len(data):
        raise FormatError(f"RIFF length {declared} != file size {len(data)}")
    position = 12
    fmt: dict[str, int] | None = None
    data_size = None
    chunks: list[str] = []
    while position < len(data):
        if position + 8 > len(data):
            raise FormatError("truncated RIFF chunk header")
        kind = data[position:position + 4]
        length = int.from_bytes(data[position + 4:position + 8], "little")
        start = position + 8
        end = start + length
        if end > len(data):
            raise FormatError("RIFF chunk leaves file boundary")
        chunks.append(kind.decode("ascii", "replace"))
        if kind == b"fmt ":
            if length < 16:
                raise FormatError("WAVE fmt chunk is too small")
            codec, channels, rate, byte_rate, block_align, bits = struct.unpack_from("<HHIIHH", data, start)
            if not channels or not rate or not block_align:
                raise FormatError("WAVE fmt values are invalid")
            fmt = {"codec": codec, "channels": channels, "sample_rate": rate, "byte_rate": byte_rate, "block_align": block_align, "bits_per_sample": bits}
        if kind == b"data":
            data_size = length
        position = end + (length & 1)
    if position != len(data) or fmt is None or data_size is None:
        raise FormatError("WAVE lacks fmt/data or has invalid padding")
    return {"format": "WAV", **fmt, "audio_bytes": data_size, "chunks": chunks}


def validate_jpeg(data: bytes) -> dict[str, Any]:
    if len(data) < 4 or not data.startswith(b"\xff\xd8") or not data.endswith(b"\xff\xd9"):
        raise FormatError("JPEG SOI/EOI markers are invalid")
    position = 2
    sof_markers = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}
    result: dict[str, Any] | None = None
    while position < len(data) - 2:
        if data[position] != 0xFF:
            raise FormatError(f"JPEG marker prefix is absent at 0x{position:x}")
        while position < len(data) and data[position] == 0xFF:
            position += 1
        if position >= len(data):
            raise FormatError("truncated JPEG marker")
        marker = data[position]
        position += 1
        if marker == 0xD9:
            break
        if marker == 0x00 or marker in range(0xD0, 0xD8) or marker == 0x01:
            raise FormatError(f"unexpected standalone JPEG marker 0x{marker:02x} before scan data")
        if position + 2 > len(data):
            raise FormatError("truncated JPEG segment length")
        length = int.from_bytes(data[position:position + 2], "big")
        if length < 2 or position + length > len(data):
            raise FormatError("JPEG segment leaves file boundary")
        payload = position + 2
        if marker in sof_markers:
            if length < 8:
                raise FormatError("JPEG SOF segment is too small")
            precision = data[payload]
            height = int.from_bytes(data[payload + 1:payload + 3], "big")
            width = int.from_bytes(data[payload + 3:payload + 5], "big")
            components = data[payload + 5]
            if width <= 0 or height <= 0 or components <= 0:
                raise FormatError("JPEG SOF dimensions/components are invalid")
            result = {
                "format": "JPEG", "width": width, "height": height,
                "precision": precision, "components": components, "sof_marker": marker,
            }
        if marker == 0xDA:
            # Entropy-coded bytes can contain stuffed FF00 and restart markers;
            # full scan decoding is outside this container validator. The final
            # EOI marker was already checked, and dimensions must precede SOS.
            break
        position += length
    if result is None:
        raise FormatError("JPEG has no supported SOF dimensions")
    return result


def _validate_media(data: bytes, suffix: str) -> dict[str, Any]:
    if suffix == ".png":
        return validate_png(data)
    if suffix == ".wav":
        return validate_wav(data)
    if suffix in (".jpg", ".jpeg"):
        return validate_jpeg(data)
    if suffix == ".ogg":
        if not data.startswith(b"OggS"):
            raise FormatError("Ogg capture pattern is absent")
        return {"format": "Ogg"}
    raise UnsupportedError(f"no strict validator for media suffix {suffix}")


def _write_atomic(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_bytes(data)
    temporary.replace(path)


def _property_values(decoded: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for prop in decoded.get("properties", []):
        if str(prop.get("decode_status", "")).startswith("decoded"):
            result[str(prop["name"])] = prop.get("value")
    return result


def _nested_values(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {}
    return _property_values(value)


def _png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def _encode_source_png(raw: bytes, width: int, height: int, source_format: str) -> bytes:
    if width <= 0 or height <= 0:
        raise FormatError(f"invalid source texture dimensions {width}x{height}")
    if source_format == "TSF_G8":
        stride, bit_depth, color_type = width, 8, 0
        pixels = raw[:stride * height]
    elif source_format in ("TSF_BGRA8", "TSF_RGBA8"):
        stride, bit_depth, color_type = width * 4, 8, 6
        pixels = raw[:stride * height]
        if source_format == "TSF_BGRA8":
            converted = bytearray(len(pixels))
            for at in range(0, len(pixels), 4):
                converted[at:at + 4] = bytes((pixels[at + 2], pixels[at + 1], pixels[at], pixels[at + 3]))
            pixels = bytes(converted)
    elif source_format in ("TSF_G16", "TSF_RGBA16"):
        channels = 1 if source_format == "TSF_G16" else 4
        stride, bit_depth, color_type = width * channels * 2, 16, 0 if channels == 1 else 6
        little = raw[:stride * height]
        pixels = b"".join(little[at:at + 2][::-1] for at in range(0, len(little), 2))
    else:
        raise UnsupportedError(f"uncompressed ETextureSourceFormat {source_format} cannot be emitted as PNG")
    expected = stride * height
    if len(pixels) != expected:
        raise BoundsError(f"texture source has {len(pixels)} top-mip bytes, expected {expected}")
    scanlines = b"".join(b"\x00" + pixels[row * stride:(row + 1) * stride] for row in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, bit_depth, color_type, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + _png_chunk(b"IHDR", ihdr) + _png_chunk(b"IDAT", zlib.compress(scanlines, 9)) + _png_chunk(b"IEND", b"")


def _decode_uedelta_bgra8(
    payload: bytes,
    *,
    width: int,
    height: int,
    source_format: str,
    num_mips: int,
    num_slices: int,
    layer_color_info: Any,
) -> tuple[bytes, dict[str, Any]]:
    """Invert UE ImageCore's byte delta for a bounded one-mip 8-bit image.

    ImageCore resets its vertical byte predictor every 32 rows.  This path is
    deliberately restricted to the exact BGRA8/RGBA8 one-layer layout proven
    by the source metadata.  Decoded channel extrema must match UE's serialized
    FTextureSourceLayerColorInfo before the pixels may be emitted.
    """
    if width <= 0 or height <= 0:
        raise FormatError(f"invalid TSCF_UEDELTA dimensions {width}x{height}")
    if source_format not in ("TSF_BGRA8", "TSF_RGBA8"):
        raise UnsupportedError(f"TSCF_UEDELTA {source_format} needs a format-specific inverse")
    if num_mips != 1 or num_slices != 1:
        raise UnsupportedError("TSCF_UEDELTA currently requires exactly one mip and one slice")
    expected = width * height * 4
    if len(payload) != expected:
        raise FormatError(f"TSCF_UEDELTA payload {len(payload)} != {width}x{height}x4 ({expected})")
    if not isinstance(layer_color_info, dict):
        raise UnsupportedError("TSCF_UEDELTA requires serialized LayerColorInfo for independent validation")
    items = layer_color_info.get("items")
    if not isinstance(items, list) or len(items) != 1:
        raise UnsupportedError("TSCF_UEDELTA requires exactly one LayerColorInfo entry")
    color_fields = _nested_values(items[0])
    declared_min = color_fields.get("ColorMin")
    declared_max = color_fields.get("ColorMax")
    if not isinstance(declared_min, dict) or not isinstance(declared_max, dict):
        raise UnsupportedError("TSCF_UEDELTA LayerColorInfo lacks ColorMin/ColorMax")
    if any(
        channel not in declared_min
        or channel not in declared_max
        or not isinstance(declared_min[channel], (int, float))
        or not isinstance(declared_max[channel], (int, float))
        for channel in ("r", "g", "b", "a")
    ):
        raise UnsupportedError("TSCF_UEDELTA LayerColorInfo has incomplete channel extrema")

    decoded = bytearray(payload)
    stride = width * 4
    for tile_start in range(0, height, 32):
        tile_end = min(tile_start + 32, height)
        for y in range(tile_start + 1, tile_end):
            row = y * stride
            previous = row - stride
            for x in range(stride):
                decoded[row + x] = (decoded[row + x] + decoded[previous + x]) & 0xFF

    raw_channels = [decoded[channel::4] for channel in range(4)]
    order = ("b", "g", "r", "a") if source_format == "TSF_BGRA8" else ("r", "g", "b", "a")
    extrema = {
        name: {"min": min(values), "max": max(values)}
        for name, values in zip(order, raw_channels)
    }
    tolerance = 0.5 / 255.0 + 1e-7
    mismatches = []
    for channel in ("r", "g", "b", "a"):
        actual_min = extrema[channel]["min"] / 255.0
        actual_max = extrema[channel]["max"] / 255.0
        expected_min = float(declared_min[channel])
        expected_max = float(declared_max[channel])
        if abs(actual_min - expected_min) > tolerance or abs(actual_max - expected_max) > tolerance:
            mismatches.append({
                "channel": channel,
                "actual": [actual_min, actual_max],
                "declared": [expected_min, expected_max],
            })
    if mismatches:
        raise FormatError(f"TSCF_UEDELTA inverse does not match LayerColorInfo: {mismatches}")
    return bytes(decoded), {
        "status": "VERIFIED",
        "transform": "TSCF_UEDELTA vertical byte inverse",
        "predictor_reset_rows": 32,
        "stored_size": len(payload),
        "stored_sha256": hashlib.sha256(payload).hexdigest(),
        "decoded_size": len(decoded),
        "decoded_sha256": hashlib.sha256(decoded).hexdigest(),
        "channel_extrema_u8": extrema,
        "layer_color_info_match": True,
    }


def _exact_texture_source(
    asset: Path,
    output: Path,
    *,
    max_output: int,
) -> dict[str, Any]:
    with UnrealPackage(asset) as package:
        candidates = [
            (index, export) for index, export in enumerate(package.exports, 1)
            if export.class_name in ("Texture2D", "TextureCube") and export.is_asset
        ]
        if len(candidates) != 1:
            raise UnsupportedError(f"expected one top-level Texture2D/TextureCube export, found {len(candidates)}")
        export_index, export = candidates[0]
        if export.class_name != "Texture2D":
            raise UnsupportedError("exact editor TextureCube native suffix is not implemented")
        decoded = PropertyParser(package).parse_export(export_index)
        trailing = decoded.get("trailing_native")
        if not trailing:
            raise UnsupportedError("Texture2D has no native source-bulk stream")
        values = _property_values(decoded)
        source = _nested_values(values.get("Source"))
        required = ("SizeX", "SizeY", "NumMips", "CompressionFormat", "Format")
        if any(name not in source for name in required):
            raise UnsupportedError("TextureSource tagged metadata is incomplete")
        width, height = int(source["SizeX"]), int(source["SizeY"])
        start = int(trailing["physical_offset"])
        end = int(export.payload_physical_offset or 0) + export.serial_size
        source_path = Path(export.payload_source or asset)
        with BinaryReader(source_path) as reader:
            reader.seek(start)
            with reader.bounded(end):
                texture_strip = {"global": reader.u8(), "class": reader.u8()}
                if texture_strip != {"global": 0, "class": 0}:
                    raise UnsupportedError(f"TextureSource editor data is stripped: {texture_strip}")
                engine = package.summary.saved_by_engine_version
                allow_old_registered = bool(engine and engine.major == 5 and engine.minor == 0)
                bulk = parse_editor_bulk_data(reader, allow_legacy_registered_flag=allow_old_registered)
                texture2d_strip = {"global": reader.u8(), "class": reader.u8()}
                cooked = reader.boolean32()
                if cooked:
                    raise UnsupportedError("Texture2D is cooked; editor TextureSource path is unavailable")
                if reader.position != end:
                    raise FormatError(f"Texture2D native suffix leaves {end - reader.position} bytes")
        trailer_info: dict[str, Any] | None = None
        if bulk.storage == "package-trailer":
            trailer = read_package_trailer(
                source_path,
                offset=package.summary.payload_toc_offset if package.summary.payload_toc_offset >= 0 else None,
            )
            entry = match_trailer_entry(bulk, trailer)
            raw = load_local_payload(source_path, entry, max_output=max_output)
            payload_offset = entry.absolute_offset
            compressed_size = entry.compressed_size
            trailer_info = {
                "trailer_offset": trailer.offset,
                "trailer_version": trailer.version,
                "entry": {
                    "identifier": entry.identifier,
                    "offset": entry.offset,
                    "absolute_offset": entry.absolute_offset,
                    "compressed_size": entry.compressed_size,
                    "raw_size": entry.raw_size,
                    "payload_flags": entry.payload_flags,
                    "filter_flags": entry.filter_flags,
                    "access_mode": entry.access_mode,
                },
            }
        elif bulk.offset_in_file is not None:
            payload_offset = bulk.offset_in_file
            header = read_compressed_buffer_header(source_path, payload_offset)
            if header.total_raw_size != bulk.payload_size:
                raise FormatError(
                    f"TextureSource size {bulk.payload_size} != FCompressedBuffer raw size {header.total_raw_size}"
                )
            if header.raw_hash[:40] != bulk.payload_content_id:
                raise FormatError("TextureSource PayloadContentId does not match FCompressedBuffer raw hash")
            raw_header, raw = decompress_compressed_buffer(source_path, payload_offset, max_output=max_output)
            compressed_size = raw_header.total_compressed_size
        else:
            raise UnsupportedError(f"TextureSource payload storage {bulk.storage} needs a different resolver")
        stored_payload = raw
        compression = str(source["CompressionFormat"])
        source_format = str(source["Format"])
        source_transform: dict[str, Any] | None = None
        if compression == "TSCF_PNG":
            image, suffix = raw, ".png"
        elif compression == "TSCF_JPEG":
            image, suffix = raw, ".jpg"
        elif compression == "TSCF_UEDELTA":
            raw, source_transform = _decode_uedelta_bgra8(
                raw,
                width=width,
                height=height,
                source_format=source_format,
                num_mips=int(source["NumMips"]),
                num_slices=int(source.get("NumSlices", 1)),
                layer_color_info=source.get("LayerColorInfo_LockProtected"),
            )
            image, suffix = _encode_source_png(raw, width, height, source_format), ".png"
        elif compression in ("TSCF_None", "None"):
            image, suffix = _encode_source_png(raw, width, height, source_format), ".png"
        else:
            raise UnsupportedError(f"TextureSource compression {compression} is not implemented")
        validation = _validate_media(image, suffix)
        if validation.get("width") != width or validation.get("height") != height:
            raise FormatError(
                f"TextureSource metadata {width}x{height} != image {validation.get('width')}x{validation.get('height')}"
            )
        if output.suffix.lower() != suffix:
            raise FormatError(f"requested suffix {output.suffix} does not match TextureSource {suffix}")
        _write_atomic(output, image)
        return {
            "schema": "ueassettool.texture-export/v2",
            "status": "VERIFIED",
            "source": {"path": str(asset), "sha256": package.sha256},
            "texture": {
                "export_index": export_index,
                "object": package.object_path(export_index),
                "class": export.class_name,
                "width": width,
                "height": height,
                "num_mips": int(source["NumMips"]),
                "num_slices": int(source.get("NumSlices", 1)),
                "source_format": source_format,
                "compression_format": compression,
            },
            "bulk": {
                **bulk.to_dict(),
                "source_file": str(source_path),
                "compressed_buffer_offset": payload_offset,
                "compressed_size": compressed_size,
                "raw_size": len(stored_payload),
                "raw_sha256": hashlib.sha256(stored_payload).hexdigest(),
                "content_id_match": True,
                "package_trailer": trailer_info,
            },
            "source_transform": source_transform,
            "native_layout": {
                "texture_strip_flags": texture_strip,
                "texture2d_strip_flags": texture2d_strip,
                "cooked": cooked,
                "native_stream_exact": True,
            },
            "validation": validation,
            "output": {"path": str(output), "size": len(image), "sha256": hashlib.sha256(image).hexdigest()},
        }


def _exact_soundwave(asset: Path, output: Path, *, max_output: int) -> dict[str, Any]:
    with UnrealPackage(asset) as package:
        candidates = [
            (index, export) for index, export in enumerate(package.exports, 1)
            if export.class_name == "SoundWave" and export.is_asset
        ]
        if len(candidates) != 1:
            raise UnsupportedError(f"expected one top-level SoundWave export, found {len(candidates)}")
        export_index, export = candidates[0]
        decoded = PropertyParser(package).parse_export(export_index)
        trailing = decoded.get("trailing_native")
        if not trailing:
            raise UnsupportedError("SoundWave has no native bulk stream")
        values = _property_values(decoded)
        start = int(trailing["physical_offset"])
        end = int(export.payload_physical_offset or 0) + export.serial_size
        source_path = Path(export.payload_source or asset)
        with BinaryReader(source_path) as reader:
            reader.seek(start)
            with reader.bounded(end):
                sound_flags = reader.u32()
                if sound_flags & 1:
                    raise UnsupportedError("cooked SoundWave platform/streaming data needs its dedicated decoder")
                if package.custom_version(GUID_UE5_MAIN) >= 0:
                    raise UnsupportedError("current FEditorBulkData SoundWave RawData path is not verified by this fixture")
                bulk = parse_legacy_bulk_data(reader)
                if bulk.size_on_disk > max_output:
                    raise BoundsError(
                        f"refusing {bulk.size_on_disk} SoundWave bytes; increase --max-output explicitly"
                    )
                if bulk.flags & LEGACY_FORCE_INLINE or not bulk.flags & LEGACY_PAYLOAD_AT_END:
                    payload_source = source_path
                    payload_offset = reader.position
                    reader.seek(payload_offset + bulk.size_on_disk)
                else:
                    if bulk.flags & LEGACY_SEPARATE_FILE:
                        suffix = ".uptnl" if bulk.flags & LEGACY_OPTIONAL_PAYLOAD else ".ubulk"
                        payload_source = asset.with_suffix(suffix)
                        if not payload_source.is_file():
                            raise UnsupportedError(f"SoundWave requires missing bulk sidecar {payload_source}")
                        payload_offset = bulk.offset_in_file
                    else:
                        payload_source = source_path
                        payload_offset = (
                            bulk.offset_in_file if bulk.flags & LEGACY_NO_OFFSET_FIXUP
                            else package.summary.bulk_data_start_offset + bulk.offset_in_file
                        )
                compressed_data_guid = reader.guid()
                if reader.position != end:
                    raise FormatError(f"SoundWave native suffix leaves {end - reader.position} bytes")
        if bulk.flags & LEGACY_COMPRESSED_ZLIB:
            raise UnsupportedError("legacy chunked-zlib SoundWave RawData is not implemented")
        if not payload_source.is_file():
            raise UnsupportedError(f"SoundWave payload source is missing: {payload_source}")
        payload_file_size = payload_source.stat().st_size
        if payload_offset < 0 or payload_offset + bulk.size_on_disk > payload_file_size:
            raise BoundsError(
                f"SoundWave payload 0x{payload_offset:x}+{bulk.size_on_disk} exceeds {payload_source}"
            )
        with payload_source.open("rb") as handle:
            handle.seek(payload_offset)
            raw = handle.read(bulk.size_on_disk)
        identified_suffix = ".wav" if raw.startswith(b"RIFF") else ".ogg" if raw.startswith(b"OggS") else ""
        if not identified_suffix:
            raise UnsupportedError("SoundWave RawData is not a supported self-describing WAV/Ogg container")
        validation = _validate_media(raw, identified_suffix)
        if output.suffix.lower() != identified_suffix:
            raise FormatError(f"requested suffix {output.suffix} does not match SoundWave {identified_suffix}")
        if identified_suffix == ".wav":
            expected_channels = values.get("NumChannels")
            expected_rate = values.get("SampleRate") or values.get("ImportedSampleRate")
            if expected_channels and validation["channels"] != expected_channels:
                raise FormatError(f"SoundWave channels {expected_channels} != WAV channels {validation['channels']}")
            if expected_rate and validation["sample_rate"] != expected_rate:
                raise FormatError(f"SoundWave sample rate {expected_rate} != WAV rate {validation['sample_rate']}")
            frames = validation["audio_bytes"] // validation["block_align"]
            expected_samples = values.get("TotalSamples")
            if expected_samples is not None and abs(float(expected_samples) - frames) > 0.5:
                raise FormatError(f"SoundWave TotalSamples {expected_samples} != WAV frames {frames}")
            expected_duration = values.get("Duration")
            actual_duration = frames / validation["sample_rate"]
            if expected_duration is not None and abs(float(expected_duration) - actual_duration) > 0.02:
                raise FormatError(
                    f"SoundWave Duration {expected_duration} != WAV duration {actual_duration:.6f}"
                )
            validation["sample_frames"] = frames
            validation["duration_seconds"] = actual_duration
        _write_atomic(output, raw)
        return {
            "schema": "ueassettool.audio-export/v2",
            "status": "VERIFIED",
            "source": {"path": str(asset), "sha256": package.sha256},
            "soundwave": {
                "export_index": export_index,
                "object": package.object_path(export_index),
                "flags": sound_flags,
                "compressed_data_guid": compressed_data_guid,
                "num_channels": values.get("NumChannels"),
                "sample_rate": values.get("SampleRate"),
                "duration": values.get("Duration"),
            },
            "bulk": {
                **bulk.to_dict(),
                "payload_source": str(payload_source),
                "payload_offset": payload_offset,
                "payload_range_exact": True,
            },
            "validation": validation,
            "output": {"path": str(output), "size": len(raw), "sha256": hashlib.sha256(raw).hexdigest()},
        }


def export_media(asset: str | Path, output: str | Path, *, kind: str, max_output: int = 2 * 1024 * 1024 * 1024) -> dict[str, Any]:
    source_asset, target_output = Path(asset), Path(output)
    exact_error: str | None = None
    try:
        if kind == "texture":
            return _exact_texture_source(source_asset, target_output, max_output=max_output)
        if kind == "audio":
            return _exact_soundwave(source_asset, target_output, max_output=max_output)
    except UnsupportedError as exc:
        exact_error = str(exc)
    with tempfile.TemporaryDirectory(prefix="ueasset-media-") as directory:
        manifest = extract_verified(asset, directory, max_output=max_output)
        matches = [item for item in manifest["items"] if item.get("status") == "verified" and item.get("kind") == kind]
        if len(matches) != 1:
            rejected = [item for item in manifest["items"] if item.get("status") == "rejected_candidate"]
            detail = f"; rejected candidates: {rejected}" if rejected else ""
            prefix = f"structured decoder: {exact_error}; " if exact_error else ""
            raise UnsupportedError(f"{prefix}expected exactly one verified {kind} payload, found {len(matches)}{detail}")
        source_path = Path(str(matches[0]["path"]))
        data = source_path.read_bytes()
        validation = _validate_media(data, source_path.suffix.lower())
        target = target_output
        if target.suffix.lower() != source_path.suffix.lower():
            raise FormatError(f"requested suffix {target.suffix} does not match extracted {source_path.suffix}; transcoding is not implicit")
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_name(target.name + ".tmp")
        temporary.write_bytes(data)
        temporary.replace(target)
        return {
            "schema": f"ueassettool.{kind}-export/v1", "status": "VERIFIED",
            "source": manifest["source"], "payload": {**matches[0], "path": str(target)},
            "validation": validation, "output": {"path": str(target), "size": len(data), "sha256": hashlib.sha256(data).hexdigest()},
        }
