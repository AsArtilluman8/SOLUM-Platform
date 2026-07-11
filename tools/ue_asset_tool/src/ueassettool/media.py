from __future__ import annotations

import hashlib
import struct
import tempfile
import zlib
from pathlib import Path
from typing import Any

from .errors import FormatError, UnsupportedError
from .extract import extract_verified


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


def _validate_media(data: bytes, suffix: str) -> dict[str, Any]:
    if suffix == ".png":
        return validate_png(data)
    if suffix == ".wav":
        return validate_wav(data)
    if suffix in (".jpg", ".jpeg"):
        if not data.startswith(b"\xff\xd8\xff") or not data.endswith(b"\xff\xd9"):
            raise FormatError("JPEG SOI/EOI markers are invalid")
        return {"format": "JPEG"}
    if suffix == ".ogg":
        if not data.startswith(b"OggS"):
            raise FormatError("Ogg capture pattern is absent")
        return {"format": "Ogg"}
    raise UnsupportedError(f"no strict validator for media suffix {suffix}")


def export_media(asset: str | Path, output: str | Path, *, kind: str, max_output: int = 2 * 1024 * 1024 * 1024) -> dict[str, Any]:
    with tempfile.TemporaryDirectory(prefix="ueasset-media-") as directory:
        manifest = extract_verified(asset, directory, max_output=max_output)
        matches = [item for item in manifest["items"] if item.get("status") == "verified" and item.get("kind") == kind]
        if len(matches) != 1:
            rejected = [item for item in manifest["items"] if item.get("status") == "rejected_candidate"]
            detail = f"; rejected candidates: {rejected}" if rejected else ""
            raise UnsupportedError(f"expected exactly one verified {kind} payload, found {len(matches)}{detail}")
        source_path = Path(str(matches[0]["path"]))
        data = source_path.read_bytes()
        validation = _validate_media(data, source_path.suffix.lower())
        target = Path(output)
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
