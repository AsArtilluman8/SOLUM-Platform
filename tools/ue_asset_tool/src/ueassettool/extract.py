from __future__ import annotations

import hashlib
import struct
from pathlib import Path

from .compression import COMPRESSED_BUFFER_MAGIC, decompress_compressed_buffer, write_verified_output
from .errors import FormatError


def _kind(data: bytes) -> tuple[str, str] | None:
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "texture", ".png"
    if data.startswith((b"RIFF", b"RIFX")) and len(data) >= 12 and data[8:12] == b"WAVE":
        return "audio", ".wav"
    if data.startswith(b"OggS"):
        return "audio", ".ogg"
    if data.startswith(b"\xff\xd8\xff"):
        return "texture", ".jpg"
    return None


def extract_verified(path: str | Path, output_dir: str | Path) -> dict[str, object]:
    """Carve only self-describing payloads whose length/hash can be verified."""
    source, target = Path(path), Path(output_dir)
    blob = source.read_bytes()
    target.mkdir(parents=True, exist_ok=True)
    items: list[dict[str, object]] = []
    seen: set[str] = set()

    # UE5 FCompressedBuffer headers are big-endian and internally length checked.
    needle = struct.pack(">I", COMPRESSED_BUFFER_MAGIC)
    cursor = 0
    while (offset := blob.find(needle, cursor)) >= 0:
        cursor = offset + 1
        try:
            header, raw = decompress_compressed_buffer(source, offset)
        except Exception as exc:
            items.append({"offset": offset, "status": "rejected_candidate", "reason": str(exc)})
            continue
        digest = hashlib.sha256(raw).hexdigest()
        if digest in seen:
            continue
        seen.add(digest)
        identified = _kind(raw)
        suffix = identified[1] if identified else ".bin"
        kind = identified[0] if identified else "verified_compressed_payload"
        out = target / f"{source.stem}.payload_{offset:08x}{suffix}"
        record = write_verified_output(out, raw)
        record.update({"kind": kind, "source_offset": offset, "compression_method": header.method,
                       "declared_raw_hash": header.raw_hash, "status": "verified"})
        items.append(record)

    # SoundWave cooked/editor bulk commonly stores a complete RIFF container.
    cursor = 0
    while (offset := blob.find(b"RIFF", cursor)) >= 0:
        cursor = offset + 4
        if offset + 12 > len(blob) or blob[offset + 8:offset + 12] != b"WAVE":
            continue
        length = 8 + int.from_bytes(blob[offset + 4:offset + 8], "little")
        if length < 12 or offset + length > len(blob):
            continue
        raw = blob[offset:offset + length]
        digest = hashlib.sha256(raw).hexdigest()
        if digest in seen:
            continue
        seen.add(digest)
        out = target / f"{source.stem}.payload_{offset:08x}.wav"
        record = write_verified_output(out, raw)
        record.update({"kind": "audio", "source_offset": offset, "status": "verified"})
        items.append(record)

    return {
        "schema": "ueassettool.extraction/v1",
        "source": {"path": str(source), "size": len(blob), "sha256": hashlib.sha256(blob).hexdigest()},
        "items": items,
        "policy": "Only bounded self-describing containers or hash-checked FCompressedBuffer payloads are emitted.",
    }
