from __future__ import annotations

from dataclasses import asdict, dataclass
from pathlib import Path

from .binary import BinaryReader
from .compression import decompress_compressed_buffer
from .errors import BoundsError, FormatError, UnsupportedError


HEADER_TAG = 0xD1C43B2E80A5F697
FOOTER_TAG = 0x29BFCA045138DE76
PACKAGE_TAG = 0x9E2A83C1
HEADER_STATIC_SIZE = 28
ENTRY_SIZE_V2 = 49
FOOTER_SIZE = 20


@dataclass(frozen=True)
class TrailerEntry:
    identifier: str
    offset: int
    compressed_size: int
    raw_size: int
    payload_flags: int
    filter_flags: int
    access_mode: int
    absolute_offset: int | None


@dataclass(frozen=True)
class PackageTrailer:
    offset: int
    version: int
    header_length: int
    payload_data_length: int
    entries: tuple[TrailerEntry, ...]
    footer_offset: int
    trailer_length: int

    def to_dict(self) -> dict[str, object]:
        return asdict(self)


def read_package_trailer(path: str | Path, *, offset: int | None = None) -> PackageTrailer:
    """Read UE::FPackageTrailer v0-v2 using its on-disk invariants."""
    source = Path(path)
    with BinaryReader(source) as r:
        if r.size < FOOTER_SIZE:
            raise FormatError("file is too small for an FPackageTrailer footer")
        r.seek(r.size - FOOTER_SIZE)
        footer_tag = r.u64()
        trailer_length = r.u64()
        package_tag = r.u32()
        if footer_tag != FOOTER_TAG:
            raise FormatError(f"FPackageTrailer footer tag 0x{footer_tag:016x} is invalid")
        if package_tag != PACKAGE_TAG:
            raise FormatError(f"FPackageTrailer package tag 0x{package_tag:08x} is invalid")
        if not FOOTER_SIZE <= trailer_length <= r.size:
            raise BoundsError(f"FPackageTrailer length {trailer_length} exceeds file size {r.size}")
        discovered = r.size - trailer_length
        if offset is not None and offset != discovered:
            raise FormatError(
                f"summary payload toc 0x{offset:x} != footer-derived trailer 0x{discovered:x}"
            )
        r.seek(discovered)
        tag = r.u64()
        if tag != HEADER_TAG:
            raise FormatError(f"FPackageTrailer header tag 0x{tag:016x} is invalid")
        version = r.u32()
        if version > 2:
            raise UnsupportedError(f"FPackageTrailer version {version} is newer than verified v2")
        header_length = r.u32()
        payload_length = r.u64()
        legacy_access = None
        if version < 1:
            legacy_access = r.u8()
        count = r.count("package trailer payload", maximum=1_000_000)
        minimum_entry_size = 20 + 8 + 8 + 8 + (4 if version >= 2 else 0) + (1 if version >= 1 else 0)
        expected_header = HEADER_STATIC_SIZE + (1 if version < 1 else 0) + count * minimum_entry_size
        if header_length != expected_header:
            raise FormatError(f"trailer header length {header_length} != serialized {expected_header}")
        if discovered + header_length + payload_length + FOOTER_SIZE != r.size:
            raise BoundsError("trailer header/payload/footer lengths do not end at physical EOF")
        entries: list[TrailerEntry] = []
        for _ in range(count):
            identifier = r.read(20).hex()
            entry_offset = r.i64()
            compressed = r.u64()
            raw = r.u64()
            payload_flags = r.u16() if version >= 2 else 0
            filter_flags = r.u16() if version >= 2 else 0
            access = r.u8() if version >= 1 else (legacy_access or 0)
            if access not in (0, 1, 2):
                raise FormatError(f"invalid trailer access mode {access}")
            absolute: int | None
            if access == 0:
                if entry_offset < 0 or entry_offset + compressed > payload_length:
                    raise BoundsError("local trailer payload leaves payload data segment")
                absolute = discovered + header_length + entry_offset
            elif access == 1:
                absolute = entry_offset if entry_offset >= 0 else None
            else:
                absolute = None
            entries.append(TrailerEntry(
                identifier, entry_offset, compressed, raw, payload_flags,
                filter_flags, access, absolute,
            ))
        if r.position != discovered + header_length:
            raise FormatError("trailer lookup table did not consume HeaderLength")
        return PackageTrailer(
            discovered, version, header_length, payload_length, tuple(entries),
            r.size - FOOTER_SIZE, trailer_length,
        )


def load_local_payload(path: str | Path, entry: TrailerEntry, *, max_output: int) -> bytes:
    if entry.access_mode != 0 or entry.absolute_offset is None:
        raise UnsupportedError("payload is referenced/virtualized rather than stored in this package")
    header, raw = decompress_compressed_buffer(path, entry.absolute_offset, max_output=max_output)
    if header.total_compressed_size != entry.compressed_size:
        raise FormatError("FCompressedBuffer size does not match trailer lookup entry")
    if header.total_raw_size != entry.raw_size:
        raise FormatError("FCompressedBuffer raw size does not match trailer lookup entry")
    if not header.raw_hash.startswith(entry.identifier):
        raise FormatError("FCompressedBuffer raw hash prefix does not match trailer IoHash")
    return raw
