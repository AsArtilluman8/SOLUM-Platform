from __future__ import annotations

import ast
import hashlib
import math
import struct
import tempfile
import zlib
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .compression import (
    decompress_compressed_buffer, decompress_legacy_chunked_zlib,
    read_compressed_buffer_header,
)
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
from .hashes import blake3_digest
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
    interlace = None
    idat_payloads: list[bytes] = []
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
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", payload,
            )
            if width <= 0 or height <= 0:
                raise FormatError("PNG dimensions are invalid")
            if compression != 0 or filtering != 0 or interlace not in (0, 1):
                raise UnsupportedError("PNG compression/filter/interlace method is unsupported")
        if kind == b"IDAT":
            idat_payloads.append(payload)
        if kind == b"IEND":
            if length != 0 or end != len(data):
                raise FormatError("PNG IEND is not the final empty chunk")
            saw_iend = True
        position = end
    if not saw_iend or width is None or "IDAT" not in chunks:
        raise FormatError("PNG lacks IHDR/IDAT/IEND")
    if interlace != 0:
        raise UnsupportedError("Adam7-interlaced PNG pixel validation is not implemented")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color_type)
    allowed_depths = {
        0: (1, 2, 4, 8, 16), 2: (8, 16), 3: (1, 2, 4, 8),
        4: (8, 16), 6: (8, 16),
    }
    if channels is None or bit_depth not in allowed_depths[color_type]:
        raise UnsupportedError(f"PNG color type/bit depth {color_type}/{bit_depth} is unsupported")
    if color_type == 3 and "PLTE" not in chunks:
        raise FormatError("indexed PNG lacks PLTE")
    row_bytes = (width * channels * bit_depth + 7) // 8
    expected_pixels = height * (row_bytes + 1)
    decoder = zlib.decompressobj()
    pixels = decoder.decompress(b"".join(idat_payloads), expected_pixels + 1)
    if decoder.unconsumed_tail or decoder.unused_data or not decoder.eof:
        raise FormatError("PNG IDAT zlib stream is incomplete, oversized or has trailing data")
    if len(pixels) != expected_pixels:
        raise BoundsError(f"PNG scanline bytes {len(pixels)} != expected {expected_pixels}")
    if any(pixels[row * (row_bytes + 1)] > 4 for row in range(height)):
        raise FormatError("PNG scanline contains an invalid filter type")
    chunk_counts = {name: chunks.count(name) for name in dict.fromkeys(chunks)}
    return {
        "format": "PNG", "width": width, "height": height,
        "bit_depth": bit_depth, "color_type": color_type,
        "scanline_bytes": len(pixels), "idat_zlib_verified": True,
        "chunk_count": len(chunks), "chunk_counts": chunk_counts,
    }


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
            if fmt is not None:
                raise FormatError("WAVE contains more than one fmt chunk")
            codec, channels, rate, byte_rate, block_align, bits = struct.unpack_from("<HHIIHH", data, start)
            if not channels or not rate or not block_align:
                raise FormatError("WAVE fmt values are invalid")
            if codec in (1, 3):
                if bits not in (8, 16, 24, 32, 64) or block_align != channels * ((bits + 7) // 8):
                    raise FormatError("PCM/float WAVE bits or block alignment is invalid")
                if byte_rate != rate * block_align:
                    raise FormatError("PCM/float WAVE byte rate is inconsistent")
            fmt = {"codec": codec, "channels": channels, "sample_rate": rate, "byte_rate": byte_rate, "block_align": block_align, "bits_per_sample": bits}
        if kind == b"data":
            if data_size is not None:
                raise FormatError("WAVE contains more than one data chunk")
            data_size = length
        position = end + (length & 1)
    if position != len(data) or fmt is None or data_size is None:
        raise FormatError("WAVE lacks fmt/data or has invalid padding")
    if data_size % fmt["block_align"]:
        raise FormatError("WAVE data size is not a whole number of sample frames")
    return {"format": "WAV", **fmt, "audio_bytes": data_size, "chunks": chunks}


def _ogg_crc_entry(value: int) -> int:
    value <<= 24
    for _ in range(8):
        value = (
            ((value << 1) ^ 0x04C11DB7) & 0xFFFFFFFF
            if value & 0x80000000 else (value << 1) & 0xFFFFFFFF
        )
    return value


_OGG_CRC_TABLE = tuple(_ogg_crc_entry(value) for value in range(256))


def _ogg_crc32(data: bytes) -> int:
    crc = 0
    for byte in data:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ _OGG_CRC_TABLE[((crc >> 24) & 0xFF) ^ byte]
    return crc


def validate_ogg(data: bytes) -> dict[str, Any]:
    """Validate complete Ogg pages, CRCs and logical-stream sequences."""
    position = 0
    pages = 0
    sequences: dict[int, int] = {}
    bos_serials: set[int] = set()
    eos_serials: set[int] = set()
    first_packet = bytearray()
    first_packet_complete = False
    while position < len(data):
        if position + 27 > len(data) or data[position:position + 4] != b"OggS":
            raise FormatError(f"Ogg page header is invalid at 0x{position:x}")
        if data[position + 4] != 0:
            raise UnsupportedError(f"Ogg bitstream version {data[position + 4]} is unsupported")
        header_type = data[position + 5]
        if header_type & ~0x07:
            raise FormatError(f"Ogg page has unknown header flags 0x{header_type:02x}")
        serial = int.from_bytes(data[position + 14:position + 18], "little")
        sequence = int.from_bytes(data[position + 18:position + 22], "little")
        expected_checksum = int.from_bytes(data[position + 22:position + 26], "little")
        segment_count = data[position + 26]
        table_end = position + 27 + segment_count
        if table_end > len(data):
            raise BoundsError("Ogg segment table is truncated")
        lacing = data[position + 27:table_end]
        page_end = table_end + sum(lacing)
        if page_end > len(data):
            raise BoundsError("Ogg page body leaves file boundary")
        page = bytearray(data[position:page_end])
        page[22:26] = b"\0\0\0\0"
        actual_checksum = _ogg_crc32(page)
        if actual_checksum != expected_checksum:
            raise FormatError(
                f"Ogg page CRC32 0x{actual_checksum:08x} != serialized 0x{expected_checksum:08x}"
            )
        if serial not in sequences:
            if sequence != 0 or not header_type & 0x02:
                raise FormatError("Ogg logical stream does not start with BOS sequence 0")
            sequences[serial] = 0
            bos_serials.add(serial)
        elif sequence != sequences[serial] + 1:
            raise FormatError(f"Ogg stream {serial} page sequence is not contiguous")
        sequences[serial] = sequence
        if header_type & 0x04:
            eos_serials.add(serial)
        if not first_packet_complete:
            body_at = table_end
            for segment_size in lacing:
                first_packet.extend(data[body_at:body_at + segment_size])
                body_at += segment_size
                if segment_size < 255:
                    first_packet_complete = True
                    break
        pages += 1
        position = page_end
    if not pages or position != len(data) or not first_packet_complete:
        raise FormatError("Ogg container has no complete first packet")
    if bos_serials != eos_serials:
        raise FormatError("Ogg container does not close every logical stream with EOS")
    if first_packet.startswith(b"\x01vorbis"):
        codec = "Vorbis"
        if len(first_packet) < 30:
            raise BoundsError("Vorbis identification packet is truncated")
        version = int.from_bytes(first_packet[7:11], "little")
        channels = first_packet[11]
        sample_rate = int.from_bytes(first_packet[12:16], "little")
        if version != 0 or not channels or not sample_rate or not first_packet[29] & 1:
            raise FormatError("Vorbis identification fields are invalid")
        codec_details = {
            "codec_version": version, "channels": channels, "sample_rate": sample_rate,
        }
    elif first_packet.startswith(b"OpusHead"):
        codec = "Opus"
        if len(first_packet) < 19:
            raise BoundsError("OpusHead packet is truncated")
        version = first_packet[8]
        channels = first_packet[9]
        pre_skip = int.from_bytes(first_packet[10:12], "little")
        input_sample_rate = int.from_bytes(first_packet[12:16], "little")
        if not 1 <= version <= 15 or not channels:
            raise FormatError("OpusHead version/channel count is invalid")
        codec_details = {
            "codec_version": version, "channels": channels,
            "pre_skip": pre_skip, "input_sample_rate": input_sample_rate,
            "mapping_family": first_packet[18],
        }
    else:
        raise UnsupportedError("Ogg first packet is neither Vorbis identification nor OpusHead")
    return {
        "format": "Ogg", "codec": codec, "page_count": pages,
        "logical_stream_count": len(sequences), "serials": sorted(sequences),
        "crc_verified": True, **codec_details,
    }


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


def validate_hdr(data: bytes) -> dict[str, Any]:
    marker = b"\n\n-Y "
    if not data.startswith(b"#?RADIANCE\n") or b"FORMAT=32-bit_rle_rgbe\n" not in data:
        raise FormatError("Radiance HDR signature/format is invalid")
    at = data.find(marker)
    if at < 0:
        raise FormatError("Radiance HDR resolution line is absent")
    line_end = data.find(b"\n", at + len(marker))
    if line_end < 0:
        raise FormatError("Radiance HDR resolution line is truncated")
    parts = data[at + 2:line_end].decode("ascii", "strict").split()
    if len(parts) != 4 or parts[0] != "-Y" or parts[2] != "+X":
        raise FormatError("Radiance HDR orientation is not -Y +X")
    height, width = int(parts[1]), int(parts[3])
    if width < 8 or height <= 0 or width > 0x7FFF:
        raise FormatError("Radiance HDR dimensions are invalid")
    position = line_end + 1
    for _ in range(height):
        if position + 4 > len(data) or data[position:position + 2] != b"\x02\x02":
            raise FormatError("Radiance HDR scanline marker is invalid")
        if int.from_bytes(data[position + 2:position + 4], "big") != width:
            raise FormatError("Radiance HDR scanline width mismatch")
        position += 4
        for _channel in range(4):
            produced = 0
            while produced < width:
                if position >= len(data):
                    raise BoundsError("Radiance HDR scanline packet is truncated")
                count = data[position]
                position += 1
                if count > 128:
                    run = count - 128
                    if not run or position >= len(data):
                        raise FormatError("Radiance HDR run packet is invalid")
                    position += 1
                    produced += run
                else:
                    if not count or position + count > len(data):
                        raise FormatError("Radiance HDR literal packet is invalid")
                    position += count
                    produced += count
                if produced > width:
                    raise BoundsError("Radiance HDR packet leaves scanline boundary")
    if position != len(data):
        raise FormatError(f"Radiance HDR has {len(data) - position} trailing bytes")
    return {"format": "Radiance HDR", "width": width, "height": height, "channels": "RGBE"}


def validate_npy(data: bytes) -> dict[str, Any]:
    if len(data) < 10 or data[:6] != b"\x93NUMPY" or data[6:8] != b"\x01\x00":
        raise FormatError("NPY v1.0 signature is invalid")
    header_size = int.from_bytes(data[8:10], "little")
    header_end = 10 + header_size
    if header_end > len(data) or not data[header_end - 1:header_end] == b"\n":
        raise BoundsError("NPY header leaves file boundary")
    try:
        header = ast.literal_eval(data[10:header_end].decode("latin-1").strip())
    except (SyntaxError, ValueError) as exc:
        raise FormatError(f"NPY header dictionary is invalid: {exc}") from exc
    if not isinstance(header, dict) or header.get("fortran_order") is not False:
        raise FormatError("NPY must contain a C-order header dictionary")
    descr = header.get("descr")
    shape = header.get("shape")
    sizes = {"|u1": 1, "<u2": 2, "<f2": 2, "<f4": 4}
    if descr not in sizes or not isinstance(shape, tuple) or not shape or any(
        not isinstance(value, int) or value <= 0 for value in shape
    ):
        raise UnsupportedError(f"NPY dtype/shape is not in the verified set: {descr}, {shape}")
    count = math.prod(shape)
    expected = header_end + count * sizes[descr]
    if expected != len(data):
        raise BoundsError(f"NPY payload length {len(data) - header_end} != {count * sizes[descr]}")
    return {"format": "NPY", "version": "1.0", "dtype": descr, "shape": list(shape), "element_count": count}


def _validate_media(data: bytes, suffix: str) -> dict[str, Any]:
    if suffix == ".png":
        return validate_png(data)
    if suffix == ".wav":
        return validate_wav(data)
    if suffix in (".jpg", ".jpeg"):
        return validate_jpeg(data)
    if suffix == ".hdr":
        return validate_hdr(data)
    if suffix == ".npy":
        return validate_npy(data)
    if suffix == ".ogg":
        return validate_ogg(data)
    raise UnsupportedError(f"no strict validator for media suffix {suffix}")


def _write_atomic(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_bytes(data)
    temporary.replace(path)


def _verify_recovered_compressed_buffer_payload(
    data: bytes, *, expected_size: int, expected_blake3: str,
) -> dict[str, Any]:
    """Prove an independently recovered raw buffer against UE metadata.

    This is useful on hosts where an Oodle backend is unavailable.  It does
    not trust the recovered filename or media appearance: both the exact raw
    length and the serialized 256-bit FCompressedBuffer BLAKE3 must match.
    """
    if len(data) != expected_size:
        raise BoundsError(
            f"recovered payload size {len(data)} != serialized raw size {expected_size}"
        )
    actual = blake3_digest(data).hex()
    if actual != expected_blake3:
        raise FormatError(
            f"recovered payload BLAKE3 {actual} != serialized RawHash {expected_blake3}"
        )
    return {
        "status": "VERIFIED",
        "basis": "exact FCompressedBuffer raw size and serialized BLAKE3",
        "raw_size": len(data),
        "raw_blake3": actual,
    }


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
            converted[0::4] = pixels[2::4]
            converted[1::4] = pixels[1::4]
            converted[2::4] = pixels[0::4]
            converted[3::4] = pixels[3::4]
            pixels = bytes(converted)
    elif source_format in ("TSF_G16", "TSF_RGBA16"):
        channels = 1 if source_format == "TSF_G16" else 4
        stride, bit_depth, color_type = width * channels * 2, 16, 0 if channels == 1 else 6
        little = raw[:stride * height]
        converted = bytearray(len(little))
        converted[0::2] = little[1::2]
        converted[1::2] = little[0::2]
        pixels = bytes(converted)
    else:
        raise UnsupportedError(f"uncompressed ETextureSourceFormat {source_format} cannot be emitted as PNG")
    expected = stride * height
    if len(pixels) != expected:
        raise BoundsError(f"texture source has {len(pixels)} top-mip bytes, expected {expected}")
    scanlines = b"".join(b"\x00" + pixels[row * stride:(row + 1) * stride] for row in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, bit_depth, color_type, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + _png_chunk(b"IHDR", ihdr) + _png_chunk(b"IDAT", zlib.compress(scanlines, 6)) + _png_chunk(b"IEND", b"")


def _encode_source_hdr(raw: bytes, width: int, height: int, source_format: str) -> bytes:
    if source_format != "TSF_BGRE8":
        raise UnsupportedError(f"Radiance HDR source format {source_format} is not verified")
    expected = width * height * 4
    if width < 8 or width > 0x7FFF or height <= 0:
        raise UnsupportedError("Radiance scanline RLE requires width 8..32767 and positive height")
    if len(raw) != expected:
        raise BoundsError(f"BGRE8 source has {len(raw)} bytes, expected {expected}")
    output = bytearray(f"#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y {height} +X {width}\n".encode("ascii"))
    row_size = width * 4
    for y in range(height):
        row = raw[y * row_size:(y + 1) * row_size]
        output.extend((2, 2, width >> 8, width & 0xFF))
        for channel in (2, 1, 0, 3):  # UE FColor memory BGRE -> Radiance RGBE
            values = row[channel::4]
            for start in range(0, width, 128):
                literal = values[start:start + 128]
                output.append(len(literal))
                output.extend(literal)
    return bytes(output)


def _encode_source_npy(
    raw: bytes, width: int, height: int, slices: int, source_format: str,
) -> tuple[bytes, dict[str, Any]]:
    formats = {
        "TSF_G8": ("|u1", 1, 1),
        "TSF_BGRA8": ("|u1", 1, 4),
        "TSF_RGBA8": ("|u1", 1, 4),
        "TSF_G16": ("<u2", 2, 1),
        "TSF_RGBA16": ("<u2", 2, 4),
        "TSF_RGBA16F": ("<f2", 2, 4),
        "TSF_R16F": ("<f2", 2, 1),
        "TSF_R32F": ("<f4", 4, 1),
    }
    if source_format not in formats:
        raise UnsupportedError(f"volume source format {source_format} has no verified NPY mapping")
    descr, component_size, channels = formats[source_format]
    expected = width * height * slices * channels * component_size
    if len(raw) != expected:
        raise BoundsError(
            f"volume source has {len(raw)} bytes, expected {width}x{height}x{slices}x{channels}x{component_size}={expected}"
        )
    payload = raw
    if source_format == "TSF_BGRA8":
        converted = bytearray(len(raw))
        for at in range(0, len(raw), 4):
            converted[at:at + 4] = bytes((raw[at + 2], raw[at + 1], raw[at], raw[at + 3]))
        payload = bytes(converted)
    shape = (slices, height, width) if channels == 1 else (slices, height, width, channels)
    dictionary = str({"descr": descr, "fortran_order": False, "shape": shape})
    header_size = len(dictionary) + 1
    header_size += (-10 - header_size) % 16
    header = dictionary.ljust(header_size - 1) + "\n"
    encoded = b"\x93NUMPY\x01\x00" + struct.pack("<H", len(header)) + header.encode("latin-1") + payload
    return encoded, {
        "dtype": descr,
        "shape": list(shape),
        "channel_order": "RGBA" if channels == 4 else "R",
        "source_bytes_sha256": hashlib.sha256(raw).hexdigest(),
        "array_bytes_sha256": hashlib.sha256(payload).hexdigest(),
    }


def _decode_uedelta_bgra8(
    payload: bytes,
    *,
    width: int,
    height: int,
    source_format: str,
    num_mips: int,
    num_slices: int,
    layer_color_info: Any,
    source_id: str | None = None,
) -> tuple[bytes, dict[str, Any]]:
    """Invert UE ImageCore's byte delta for a bounded one-mip 8-bit image.

    ImageCore applies the predictor independently to split image views. This
    path is restricted to exact G8/BGRA8/RGBA8/BGRE8 one-layer layouts. The
    split height is accepted only when the reconstructed bytes reproduce the
    serialized LayerColorInfo and, when bGuidIsHash is set, TextureSource.Id.
    """
    if width <= 0 or height <= 0:
        raise FormatError(f"invalid TSCF_UEDELTA dimensions {width}x{height}")
    if source_format not in ("TSF_G8", "TSF_BGRA8", "TSF_RGBA8", "TSF_BGRE8"):
        raise UnsupportedError(f"TSCF_UEDELTA {source_format} needs a format-specific inverse")
    if num_mips != 1 or num_slices != 1:
        raise UnsupportedError("TSCF_UEDELTA currently requires exactly one mip and one slice")
    bytes_per_pixel = 1 if source_format == "TSF_G8" else 4
    expected = width * height * bytes_per_pixel
    if len(payload) != expected:
        raise FormatError(
            f"TSCF_UEDELTA payload {len(payload)} != {width}x{height}x{bytes_per_pixel} ({expected})"
        )
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

    try:
        import numpy as np
    except ImportError:  # pragma: no cover - exercised on dependency-minimal installs
        np = None

    def inverse(reset_rows: int) -> bytearray:
        if np is not None:
            # UE's inverse is a vertical unsigned-byte prefix sum.  Keeping the
            # accumulator at uint8 intentionally reproduces FImageCoreDelta's
            # modulo-256 arithmetic while moving large (for example 4K BGRA)
            # sources through a bounded native loop instead of Python per-byte
            # iteration.
            stride = width * bytes_per_pixel
            encoded = np.frombuffer(payload, dtype=np.uint8).reshape(height, stride)
            decoded = np.empty_like(encoded)
            for tile_start in range(0, height, reset_rows):
                tile_end = min(tile_start + reset_rows, height)
                np.add.accumulate(
                    encoded[tile_start:tile_end], axis=0, dtype=np.uint8,
                    out=decoded[tile_start:tile_end],
                )
            return bytearray(decoded.tobytes())
        value = bytearray(payload)
        stride = width * bytes_per_pixel
        for tile_start in range(0, height, reset_rows):
            tile_end = min(tile_start + reset_rows, height)
            for y in range(tile_start + 1, tile_end):
                row = y * stride
                previous = row - stride
                for x in range(stride):
                    value[row + x] = (value[row + x] + value[previous + x]) & 0xFF
        return value

    def srgb_to_linear(item: float) -> float:
        return item / 12.92 if item <= 0.04045 else ((item + 0.055) / 1.055) ** 2.4

    def linear_extrema(value: bytes, transfer: str) -> dict[str, dict[str, float | int]]:
        if source_format == "TSF_BGRE8":
            result = {name: {"min": math.inf, "max": -math.inf} for name in ("r", "g", "b", "a")}
            for at in range(0, len(value), 4):
                blue, green, red, exponent = value[at:at + 4]
                scale = math.ldexp(1.0, exponent - (128 + 8)) if exponent else 0.0
                channels = {
                    "r": (red + 0.5) * scale if exponent else 0.0,
                    "g": (green + 0.5) * scale if exponent else 0.0,
                    "b": (blue + 0.5) * scale if exponent else 0.0,
                    "a": 1.0,
                }
                for name, item in channels.items():
                    result[name]["min"] = min(float(result[name]["min"]), item)
                    result[name]["max"] = max(float(result[name]["max"]), item)
            return result
        if source_format == "TSF_G8":
            low, high = min(value) / 255.0, max(value) / 255.0
            if transfer == "sRGB":
                low, high = srgb_to_linear(low), srgb_to_linear(high)
            return {
                "r": {"min": low, "max": high},
                "g": {"min": low, "max": high},
                "b": {"min": low, "max": high},
                "a": {"min": 1.0, "max": 1.0},
            }
        raw_channels = [value[channel::4] for channel in range(4)]
        order = ("b", "g", "r", "a") if source_format == "TSF_BGRA8" else ("r", "g", "b", "a")
        result: dict[str, dict[str, float | int]] = {}
        for name, values in zip(order, raw_channels):
            low, high = min(values) / 255.0, max(values) / 255.0
            if transfer == "sRGB" and name != "a":
                low, high = srgb_to_linear(low), srgb_to_linear(high)
            result[name] = {"min": low, "max": high}
        return result

    def mismatches(extrema: dict[str, dict[str, float | int]]) -> list[dict[str, Any]]:
        result = []
        tolerance = 1e-7 if source_format == "TSF_BGRE8" else 0.5 / 255.0 + 1e-7
        for channel in ("r", "g", "b", "a"):
            actual_min = float(extrema[channel]["min"])
            actual_max = float(extrema[channel]["max"])
            expected_min = float(declared_min[channel])
            expected_max = float(declared_max[channel])
            if abs(actual_min - expected_min) > tolerance or abs(actual_max - expected_max) > tolerance:
                result.append({
                    "channel": channel,
                    "actual": [actual_min, actual_max],
                    "declared": [expected_min, expected_max],
                })
        return result

    # 32 rows is ImageCore's common large-image split in the supplied UE5
    # corpus; neighbouring powers cover the other bounded split views.  The
    # order affects speed only: no candidate is trusted without provenance.
    reset_candidates = [
        value for value in (32, 64, 128, 256, 512, 1024, 2048, 4096, 16) if value <= height
    ]
    if height not in reset_candidates:
        reset_candidates.append(height)
    expected_source_hash_guid: bytes | None = None
    if source_id is not None:
        parts = source_id.split("-")
        if len(parts) != 4 or any(len(part) != 8 for part in parts):
            raise FormatError(f"TextureSource.Id {source_id!r} is not an FGuid")
        try:
            expected_source_hash_guid = b"".join(
                int(part, 16).to_bytes(4, "little") for part in parts
            )
        except ValueError as exc:
            raise FormatError(f"TextureSource.Id {source_id!r} is not hexadecimal") from exc
    matches: list[
        tuple[int, bytearray, dict[str, dict[str, float | int]], list[str], bytes]
    ] = []
    last_mismatches: list[dict[str, Any]] = []
    for reset_rows in reset_candidates:
        candidate = inverse(reset_rows)
        transfers = ("RGBE linear",) if source_format == "TSF_BGRE8" else ("linear", "sRGB")
        transfer_matches = []
        selected_extrema = None
        for transfer in transfers:
            candidate_extrema = linear_extrema(candidate, transfer)
            candidate_mismatches = mismatches(candidate_extrema)
            if not candidate_mismatches:
                transfer_matches.append(transfer)
                selected_extrema = candidate_extrema
            last_mismatches = candidate_mismatches
        # BLAKE3 is the strongest discriminator here, but hashing every rejected
        # reset candidate is unnecessarily expensive for large source images.
        # LayerColorInfo is serialized independently and gives us a cheap first
        # gate; only candidates which reproduce it are hashed against Source.Id.
        if transfer_matches and selected_extrema is not None:
            digest = blake3_digest(bytes(candidate))
            source_id_matches = (
                expected_source_hash_guid is None or digest[:16] == expected_source_hash_guid
            )
            if source_id_matches:
                matches.append((reset_rows, candidate, selected_extrema, transfer_matches, digest))
                # An exact BLAKE3-derived Source.Id identifies the original
                # bytes. Testing slower alternative partitions cannot add
                # meaningful certainty beyond that 128-bit serialized match.
                if expected_source_hash_guid is not None:
                    break
    if len(matches) != 1:
        raise FormatError(
            f"TSCF_UEDELTA inverse does not match LayerColorInfo uniquely: "
            f"matched {len(matches)} predictor reset candidates; "
            f"candidates={reset_candidates}, last mismatches={last_mismatches}"
        )
    predictor_reset_rows, decoded, extrema, transfer_matches, decoded_blake3 = matches[0]
    return bytes(decoded), {
        "status": "VERIFIED",
        "transform": "TSCF_UEDELTA vertical byte inverse",
        "predictor_reset_rows": predictor_reset_rows,
        "extrema_transfer_candidates": transfer_matches,
        "stored_size": len(payload),
        "stored_sha256": hashlib.sha256(payload).hexdigest(),
        "decoded_size": len(decoded),
        "decoded_sha256": hashlib.sha256(decoded).hexdigest(),
        "decoded_blake3": decoded_blake3.hex(),
        "texture_source_id": source_id,
        "texture_source_id_match": None if expected_source_hash_guid is None else True,
        "channel_extrema": extrema,
        "layer_color_info_match": True,
    }


def _uedelta_view_rectangles(width: int, height: int, bytes_per_pixel: int) -> list[tuple[int, int, int, int]]:
    """Reproduce UE ImageCoreDelta's machine-independent split rectangles."""
    if width <= 0 or height <= 0 or bytes_per_pixel <= 0:
        raise FormatError("invalid UEDELTA image dimensions")
    if width * height <= 136 * 136:
        return [(0, width, 0, height)]

    def rows_per_cut(size_x: int, size_y: int) -> int:
        pixels = size_x * size_y
        cuts = 1 if pixels <= 32768 else pixels // 32768
        while cuts > 512:
            cuts >>= 1
        return (size_y + cuts - 1) // cuts

    stride = width * bytes_per_pixel
    rectangles: list[tuple[int, int, int, int]] = []
    if stride <= 4096:
        rows = rows_per_cut(width, height)
        for start_y in range(0, height, rows):
            rectangles.append((0, width, start_y, min(rows, height - start_y)))
        return rectangles

    horizontal_parts = (stride + 4095) // 4096
    part_bytes = (stride + horizontal_parts // 2) // horizontal_parts
    part_bytes = (part_bytes + 63) & ~63
    part_pixels = part_bytes // bytes_per_pixel
    for start_x in range(0, width, part_pixels):
        strip_width = min(part_pixels, width - start_x)
        rows = rows_per_cut(strip_width, height)
        for start_y in range(0, height, rows):
            rectangles.append((start_x, strip_width, start_y, min(rows, height - start_y)))
    return rectangles


def _decode_uedelta_texture_source(
    payload: bytes,
    *,
    width: int,
    height: int,
    source_format: str,
    num_mips: int,
    num_slices: int,
    source_id: str | None,
) -> tuple[bytes, dict[str, Any]]:
    """Invert UE's exact multi-mip FTextureSource UEDELTA transform.

    Layout and split constants follow ImageCoreDelta.cpp and Texture.cpp.  A
    BLAKE3-derived TextureSource.Id is required so a decoded byte stream cannot
    be accepted merely because its dimensions look plausible.
    """
    bytes_per_pixel = {
        "TSF_G8": 1, "TSF_BGRA8": 4, "TSF_RGBA8": 4, "TSF_BGRE8": 4,
        "TSF_G16": 2, "TSF_RGBA16": 8,
    }.get(source_format)
    if bytes_per_pixel is None:
        raise UnsupportedError(f"TSCF_UEDELTA {source_format} needs a format-specific inverse")
    if num_mips <= 0 or num_slices <= 0:
        raise FormatError("UEDELTA mip/slice count must be positive")
    if source_id is None:
        raise UnsupportedError("multi-mip UEDELTA requires serialized bGuidIsHash TextureSource.Id")

    mip_layout: list[dict[str, Any]] = []
    expected = 0
    for mip in range(num_mips):
        mip_width = max(1, width >> mip)
        mip_height = max(1, height >> mip)
        slice_size = mip_width * mip_height * bytes_per_pixel
        mip_layout.append({
            "mip": mip, "width": mip_width, "height": mip_height,
            "slices": num_slices, "offset": expected,
            "size": slice_size * num_slices,
        })
        expected += slice_size * num_slices
    if len(payload) != expected:
        raise FormatError(f"UEDELTA payload {len(payload)} != calculated mip chain {expected}")

    output = bytearray(payload)
    for mip in mip_layout:
        mip_width = int(mip["width"])
        mip_height = int(mip["height"])
        stride = mip_width * bytes_per_pixel
        slice_size = stride * mip_height
        rectangles = _uedelta_view_rectangles(mip_width, mip_height, bytes_per_pixel)
        mip["split_rectangles"] = [list(item) for item in rectangles]
        for slice_index in range(num_slices):
            base = int(mip["offset"]) + slice_index * slice_size
            for start_x, rect_width, start_y, rect_height in rectangles:
                byte_x = start_x * bytes_per_pixel
                byte_width = rect_width * bytes_per_pixel
                for local_y in range(1, rect_height):
                    row = base + (start_y + local_y) * stride + byte_x
                    previous = row - stride
                    for x in range(byte_width):
                        output[row + x] = (output[row + x] + output[previous + x]) & 0xFF

    parts = source_id.split("-")
    if len(parts) != 4 or any(len(part) != 8 for part in parts):
        raise FormatError(f"TextureSource.Id {source_id!r} is not an FGuid")
    try:
        expected_guid = b"".join(int(part, 16).to_bytes(4, "little") for part in parts)
    except ValueError as exc:
        raise FormatError(f"TextureSource.Id {source_id!r} is not hexadecimal") from exc
    decoded_blake3 = blake3_digest(bytes(output))
    if decoded_blake3[:16] != expected_guid:
        raise FormatError("UEDELTA decoded mip chain does not match TextureSource.Id")
    return bytes(output), {
        "status": "VERIFIED",
        "transform": "FImageCoreDelta exact multi-mip inverse",
        "authority": [
            "Engine/Source/Runtime/ImageCore/Private/ImageCoreDelta.cpp",
            "Engine/Source/Runtime/Engine/Private/Texture.cpp:FTextureSource::DoUEDeltaTransform",
        ],
        "stored_size": len(payload),
        "stored_sha256": hashlib.sha256(payload).hexdigest(),
        "decoded_size": len(output),
        "decoded_sha256": hashlib.sha256(output).hexdigest(),
        "decoded_blake3": decoded_blake3.hex(),
        "texture_source_id": source_id,
        "texture_source_id_match": True,
        "mips": mip_layout,
    }


def _exact_texture_source(
    asset: Path,
    output: Path,
    *,
    max_output: int,
    export_index: int | None = None,
    recovered_source: Path | None = None,
) -> dict[str, Any]:
    with UnrealPackage(asset) as package:
        candidates = [
            (index, export) for index, export in enumerate(package.exports, 1)
            if export.class_name in ("Texture2D", "TextureCube", "VolumeTexture")
            and (index == export_index if export_index is not None else export.is_asset)
        ]
        if len(candidates) != 1:
            raise UnsupportedError(
                f"expected one top-level Texture2D/TextureCube/VolumeTexture export, found {len(candidates)}"
            )
        export_index, export = candidates[0]
        decoded = PropertyParser(package).parse_export(export_index)
        trailing = decoded.get("trailing_native")
        if not trailing:
            raise UnsupportedError("Texture2D has no native source-bulk stream")
        values = _property_values(decoded)
        source = _nested_values(values.get("Source"))
        required = ("SizeX", "SizeY", "NumMips", "Format")
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
                legacy_bulk = package.summary.file_version_ue5 == 0
                bulk = (
                    parse_legacy_bulk_data(reader)
                    if legacy_bulk
                    else parse_editor_bulk_data(reader, allow_legacy_registered_flag=allow_old_registered)
                )
                class_strip = {"global": reader.u8(), "class": reader.u8()}
                if class_strip != {"global": 0, "class": 0}:
                    raise UnsupportedError(f"{export.class_name} class data is stripped: {class_strip}")
                cooked = reader.boolean32()
                if cooked:
                    raise UnsupportedError(
                        f"{export.class_name} is cooked; editor TextureSource path is unavailable"
                    )
                if reader.position != end:
                    raise FormatError(f"{export.class_name} native suffix leaves {end - reader.position} bytes")
        trailer_info: dict[str, Any] | None = None
        legacy_compression: dict[str, Any] | None = None
        recovered_payload: dict[str, Any] | None = None
        if legacy_bulk:
            if bulk.flags & LEGACY_FORCE_INLINE or not bulk.flags & LEGACY_PAYLOAD_AT_END:
                raise UnsupportedError("inline UE4 TextureSource FByteBulkData is not verified")
            if bulk.flags & LEGACY_SEPARATE_FILE:
                payload_source = asset.with_suffix(
                    ".uptnl" if bulk.flags & LEGACY_OPTIONAL_PAYLOAD else ".ubulk"
                )
                if not payload_source.is_file():
                    raise UnsupportedError(f"TextureSource requires missing sidecar {payload_source}")
                payload_offset = bulk.offset_in_file
            else:
                payload_source = source_path
                payload_offset = (
                    bulk.offset_in_file if bulk.flags & LEGACY_NO_OFFSET_FIXUP
                    else package.summary.bulk_data_start_offset + bulk.offset_in_file
                )
            if bulk.flags & LEGACY_COMPRESSED_ZLIB:
                legacy_compression, raw = decompress_legacy_chunked_zlib(
                    payload_source, payload_offset, bulk.size_on_disk, bulk.element_count,
                    max_output=max_output,
                )
            else:
                if bulk.element_count != bulk.size_on_disk:
                    raise UnsupportedError(
                        "UE4 TextureSource has differing element/stored sizes without a verified compression flag"
                    )
                if payload_offset < 0 or payload_offset + bulk.size_on_disk > payload_source.stat().st_size:
                    raise BoundsError("UE4 TextureSource payload range leaves its source file")
                with payload_source.open("rb") as handle:
                    handle.seek(payload_offset)
                    raw = handle.read(bulk.size_on_disk)
            compressed_size = bulk.size_on_disk
        elif bulk.storage == "package-trailer":
            payload_source = source_path
            trailer = read_package_trailer(
                source_path,
                offset=package.summary.payload_toc_offset if package.summary.payload_toc_offset >= 0 else None,
            )
            entry = match_trailer_entry(bulk, trailer)
            try:
                raw = load_local_payload(source_path, entry, max_output=max_output)
            except UnsupportedError:
                if recovered_source is None:
                    raise
                header = read_compressed_buffer_header(source_path, entry.absolute_offset)
                if recovered_source.stat().st_size != header.total_raw_size:
                    raise BoundsError(
                        f"recovered payload size {recovered_source.stat().st_size} "
                        f"!= serialized raw size {header.total_raw_size}"
                    )
                raw = recovered_source.read_bytes()
                recovered_payload = _verify_recovered_compressed_buffer_payload(
                    raw,
                    expected_size=header.total_raw_size,
                    expected_blake3=header.raw_hash,
                )
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
            if bulk.storage == "payload-sidecar":
                payload_source = asset.with_suffix(".upayload")
                if not payload_source.is_file():
                    raise UnsupportedError(
                        f"TextureSource requires missing editor payload sidecar {payload_source}"
                    )
            elif bulk.storage == "inline-or-end-of-package":
                payload_source = source_path
            else:
                raise UnsupportedError(
                    f"TextureSource disk-backed storage {bulk.storage} needs a different resolver"
                )
            payload_offset = bulk.offset_in_file
            header = read_compressed_buffer_header(payload_source, payload_offset)
            if header.total_raw_size != bulk.payload_size:
                raise FormatError(
                    f"TextureSource size {bulk.payload_size} != FCompressedBuffer raw size {header.total_raw_size}"
                )
            if header.raw_hash[:40] != bulk.payload_content_id:
                raise FormatError("TextureSource PayloadContentId does not match FCompressedBuffer raw hash")
            try:
                raw_header, raw = decompress_compressed_buffer(
                    payload_source, payload_offset, max_output=max_output,
                )
            except UnsupportedError:
                if recovered_source is None:
                    raise
                if recovered_source.stat().st_size != header.total_raw_size:
                    raise BoundsError(
                        f"recovered payload size {recovered_source.stat().st_size} "
                        f"!= serialized raw size {header.total_raw_size}"
                    )
                raw = recovered_source.read_bytes()
                recovered_payload = _verify_recovered_compressed_buffer_payload(
                    raw,
                    expected_size=header.total_raw_size,
                    expected_blake3=header.raw_hash,
                )
                raw_header = header
            compressed_size = raw_header.total_compressed_size
        else:
            raise UnsupportedError(f"TextureSource payload storage {bulk.storage} needs a different resolver")
        stored_payload = raw
        serialized_compression = source.get("CompressionFormat")
        if serialized_compression is not None:
            compression = str(serialized_compression)
            compression_basis = "serialized TextureSource.CompressionFormat"
        elif raw.startswith(b"\x89PNG\r\n\x1a\n"):
            compression = "TSCF_PNG"
            compression_basis = "bounded payload PNG signature (legacy default omitted from tags)"
        elif raw.startswith(b"\xff\xd8"):
            compression = "TSCF_JPEG"
            compression_basis = "bounded payload JPEG signature (legacy default omitted from tags)"
        else:
            compression = "TSCF_None"
            compression_basis = "serialized default: property absent and payload is not PNG/JPEG"
        source_format = str(source["Format"])
        num_mips = int(source["NumMips"])
        num_slices = int(source.get("NumSlices", 1))
        block_offsets = source.get("BlockDataOffsets")
        if isinstance(block_offsets, dict) and block_offsets.get("items") != [0]:
            raise UnsupportedError(
                f"TextureSource block offsets {block_offsets.get('items')} need a multi-block decoder"
            )
        source_transform: dict[str, Any] | None = None
        array_layout: dict[str, Any] | None = None
        if compression == "TSCF_PNG":
            if num_slices != 1 or num_mips != 1:
                raise UnsupportedError("PNG TextureSource is only verified for one slice/mip")
            image, suffix = raw, ".png"
        elif compression == "TSCF_JPEG":
            if num_slices != 1 or num_mips != 1:
                raise UnsupportedError("JPEG TextureSource is only verified for one slice/mip")
            image, suffix = raw, ".jpg"
        elif compression == "TSCF_UEDELTA":
            source_id = str(source.get("Id")) if source.get("bGuidIsHash") is True else None
            if num_mips == 1 and num_slices == 1:
                raw, source_transform = _decode_uedelta_bgra8(
                    raw,
                    width=width,
                    height=height,
                    source_format=source_format,
                    num_mips=num_mips,
                    num_slices=num_slices,
                    layer_color_info=source.get("LayerColorInfo_LockProtected"),
                    source_id=source_id,
                )
            else:
                raw, source_transform = _decode_uedelta_texture_source(
                    raw,
                    width=width,
                    height=height,
                    source_format=source_format,
                    num_mips=num_mips,
                    num_slices=num_slices,
                    source_id=source_id,
                )
            if source_format == "TSF_BGRE8":
                image, suffix = _encode_source_hdr(raw, width, height, source_format), ".hdr"
            else:
                image, suffix = _encode_source_png(raw, width, height, source_format), ".png"
        elif compression in ("TSCF_None", "None"):
            if (
                export.class_name == "VolumeTexture"
                or num_slices > 1
                or source_format in ("TSF_RGBA16F", "TSF_R16F", "TSF_R32F")
            ):
                if num_mips != 1:
                    raise UnsupportedError("multi-mip volume TextureSource needs per-mip range decoding")
                image, array_layout = _encode_source_npy(
                    raw, width, height, num_slices, source_format
                )
                suffix = ".npy"
            elif source_format == "TSF_BGRE8":
                image, suffix = _encode_source_hdr(raw, width, height, source_format), ".hdr"
            else:
                image, suffix = _encode_source_png(raw, width, height, source_format), ".png"
        else:
            raise UnsupportedError(f"TextureSource compression {compression} is not implemented")
        validation = _validate_media(image, suffix)
        if suffix != ".npy" and (
            validation.get("width") != width or validation.get("height") != height
        ):
            raise FormatError(
                f"TextureSource metadata {width}x{height} != image {validation.get('width')}x{validation.get('height')}"
            )
        if suffix == ".npy" and array_layout and validation.get("shape") != array_layout["shape"]:
            raise FormatError("NPY validation shape does not match the encoded volume layout")
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
                "num_mips": num_mips,
                "num_slices": num_slices,
                "source_format": source_format,
                "compression_format": compression,
                "compression_format_serialized": serialized_compression,
                "compression_format_basis": compression_basis,
            },
            "bulk": {
                **bulk.to_dict(),
                "source_file": str(payload_source),
                "compressed_buffer_offset": payload_offset,
                "compressed_size": compressed_size,
                "raw_size": len(stored_payload),
                "raw_sha256": hashlib.sha256(stored_payload).hexdigest(),
                "content_id_match": None if legacy_bulk else True,
                "package_trailer": trailer_info,
                "legacy_compression": legacy_compression,
            },
            "source_transform": source_transform,
            "recovered_payload": recovered_payload,
            "array_layout": array_layout,
            "native_layout": {
                "texture_strip_flags": texture_strip,
                "class_strip_flags": class_strip,
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
        if not payload_source.is_file():
            raise UnsupportedError(f"SoundWave payload source is missing: {payload_source}")
        payload_file_size = payload_source.stat().st_size
        if payload_offset < 0 or payload_offset + bulk.size_on_disk > payload_file_size:
            raise BoundsError(
                f"SoundWave payload 0x{payload_offset:x}+{bulk.size_on_disk} exceeds {payload_source}"
            )
        legacy_compression: dict[str, Any] | None = None
        if bulk.flags & LEGACY_COMPRESSED_ZLIB:
            legacy_compression, raw = decompress_legacy_chunked_zlib(
                payload_source, payload_offset, bulk.size_on_disk, bulk.element_count,
                max_output=max_output,
            )
        else:
            if bulk.element_count != bulk.size_on_disk:
                raise UnsupportedError(
                    "SoundWave has differing element/stored sizes without a verified compression flag"
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
                "legacy_compression": legacy_compression,
                "raw_size": len(raw),
                "raw_sha256": hashlib.sha256(raw).hexdigest(),
            },
            "validation": validation,
            "output": {
                "path": str(output),
                "size": len(raw),
                "sha256": hashlib.sha256(raw).hexdigest(),
            },
        }


def export_media(
    asset: str | Path,
    output: str | Path,
    *,
    kind: str,
    max_output: int = 2 * 1024 * 1024 * 1024,
    export_index: int | None = None,
    recovered_source: str | Path | None = None,
) -> dict[str, Any]:
    source_asset, target_output = Path(asset), Path(output)
    exact_error: str | None = None
    try:
        if kind == "texture":
            return _exact_texture_source(
                source_asset, target_output, max_output=max_output, export_index=export_index,
                recovered_source=Path(recovered_source) if recovered_source is not None else None,
            )
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
