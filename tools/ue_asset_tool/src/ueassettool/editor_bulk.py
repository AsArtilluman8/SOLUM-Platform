from __future__ import annotations

import hashlib
import struct
from dataclasses import asdict, dataclass
from typing import Any

from .binary import BinaryReader
from .errors import FormatError, UnsupportedError
from .trailer import PackageTrailer, TrailerEntry


IS_VIRTUALIZED = 1 << 0
HAS_PAYLOAD_SIDECAR_FILE = 1 << 1
REFERENCES_LEGACY_FILE = 1 << 2
LEGACY_FILE_IS_COMPRESSED = 1 << 3
DISABLE_PAYLOAD_COMPRESSION = 1 << 4
LEGACY_KEY_WAS_GUID_DERIVED = 1 << 5
HAS_REGISTERED = 1 << 6
IS_TORN_OFF = 1 << 7
REFERENCES_WORKSPACE_DOMAIN = 1 << 8
STORED_IN_PACKAGE_TRAILER = 1 << 9
IS_COOKED = 1 << 10
WAS_DETACHED = 1 << 11

FLAG_NAMES = {
    IS_VIRTUALIZED: "IsVirtualized",
    HAS_PAYLOAD_SIDECAR_FILE: "HasPayloadSidecarFile",
    REFERENCES_LEGACY_FILE: "ReferencesLegacyFile",
    LEGACY_FILE_IS_COMPRESSED: "LegacyFileIsCompressed",
    DISABLE_PAYLOAD_COMPRESSION: "DisablePayloadCompression",
    LEGACY_KEY_WAS_GUID_DERIVED: "LegacyKeyWasGuidDerived",
    HAS_REGISTERED: "HasRegistered",
    IS_TORN_OFF: "IsTornOff",
    REFERENCES_WORKSPACE_DOMAIN: "ReferencesWorkspaceDomain",
    STORED_IN_PACKAGE_TRAILER: "StoredInPackageTrailer",
    IS_COOKED: "IsCooked",
    WAS_DETACHED: "WasDetached",
}
KNOWN_FLAGS = sum(FLAG_NAMES)
TRANSIENT_FLAGS = HAS_REGISTERED | IS_TORN_OFF | WAS_DETACHED
REFERENCES_BY_PACKAGE_PATH = REFERENCES_LEGACY_FILE | REFERENCES_WORKSPACE_DOMAIN

LEGACY_PAYLOAD_AT_END = 1 << 0
LEGACY_COMPRESSED_ZLIB = 1 << 1
LEGACY_FORCE_SINGLE_ELEMENT = 1 << 2
LEGACY_SINGLE_USE = 1 << 3
LEGACY_UNUSED = 1 << 5
LEGACY_FORCE_INLINE = 1 << 6
LEGACY_FORCE_STREAM = 1 << 7
LEGACY_SEPARATE_FILE = 1 << 8
LEGACY_COMPRESSED_BIT_WINDOW = 1 << 9
LEGACY_FORCE_NOT_INLINE = 1 << 10
LEGACY_OPTIONAL_PAYLOAD = 1 << 11
LEGACY_MEMORY_MAPPED = 1 << 12
LEGACY_SIZE_64_BIT = 1 << 13
LEGACY_DUPLICATE_NON_OPTIONAL = 1 << 14
LEGACY_BAD_DATA_VERSION = 1 << 15
LEGACY_NO_OFFSET_FIXUP = 1 << 16
LEGACY_WORKSPACE_DOMAIN = 1 << 17
LEGACY_LAZY_LOADABLE = 1 << 18
LEGACY_ALWAYS_ALLOW_DISCARD = 1 << 28
LEGACY_ASYNC_READ_PENDING = 1 << 29
LEGACY_DATA_IS_MEMORY_MAPPED = 1 << 30
LEGACY_USES_IO_DISPATCHER = 1 << 31

LEGACY_FLAG_NAMES = {
    LEGACY_PAYLOAD_AT_END: "PayloadAtEndOfFile",
    LEGACY_COMPRESSED_ZLIB: "SerializeCompressedZLIB",
    LEGACY_FORCE_SINGLE_ELEMENT: "ForceSingleElementSerialization",
    LEGACY_SINGLE_USE: "SingleUse",
    LEGACY_UNUSED: "Unused",
    LEGACY_FORCE_INLINE: "ForceInlinePayload",
    LEGACY_FORCE_STREAM: "ForceStreamPayload",
    LEGACY_SEPARATE_FILE: "PayloadInSeparateFile",
    LEGACY_COMPRESSED_BIT_WINDOW: "SerializeCompressedBitWindow",
    LEGACY_FORCE_NOT_INLINE: "ForceNotInlinePayload",
    LEGACY_OPTIONAL_PAYLOAD: "OptionalPayload",
    LEGACY_MEMORY_MAPPED: "MemoryMappedPayload",
    LEGACY_SIZE_64_BIT: "Size64Bit",
    LEGACY_DUPLICATE_NON_OPTIONAL: "DuplicateNonOptionalPayload",
    LEGACY_BAD_DATA_VERSION: "BadDataVersion",
    LEGACY_NO_OFFSET_FIXUP: "NoOffsetFixUp",
    LEGACY_WORKSPACE_DOMAIN: "WorkspaceDomainPayload",
    LEGACY_LAZY_LOADABLE: "LazyLoadable",
    LEGACY_ALWAYS_ALLOW_DISCARD: "AlwaysAllowDiscard",
    LEGACY_ASYNC_READ_PENDING: "HasAsyncReadPending",
    LEGACY_DATA_IS_MEMORY_MAPPED: "DataIsMemoryMapped",
    LEGACY_USES_IO_DISPATCHER: "UsesIoDispatcher",
}
LEGACY_KNOWN_FLAGS = sum(LEGACY_FLAG_NAMES)


def _guid(raw: bytes) -> str:
    a, b, c, d = struct.unpack("<IIII", raw)
    return f"{a:08x}-{b:08x}-{c:08x}-{d:08x}"


def _serialized_bytes(reader: BinaryReader, start: int, end: int) -> bytes:
    current = reader.position
    reader.seek(start)
    raw = reader.read(end - start)
    reader.seek(current)
    return raw


@dataclass(frozen=True)
class EditorBulkData:
    metadata_offset: int
    metadata_size: int
    metadata_sha256: str
    flags: int
    flag_names: tuple[str, ...]
    bulk_data_id: str
    bulk_data_id_bytes: str
    payload_content_id: str
    payload_size: int
    offset_in_file: int | None
    storage: str

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class MeshDescriptionBulkData:
    editor_bulk: EditorBulkData
    mesh_guid: str
    mesh_guid_bytes: str
    guid_is_hash: bool
    serialized_size: int
    serialized_sha256: str

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class LegacyBulkData:
    metadata_offset: int
    metadata_size: int
    metadata_sha256: str
    flags: int
    flag_names: tuple[str, ...]
    element_count: int
    size_on_disk: int
    offset_in_file: int
    duplicate_flags: int | None = None
    duplicate_size_on_disk: int | None = None
    duplicate_offset_in_file: int | None = None

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def parse_editor_bulk_data(reader: BinaryReader, *, allow_legacy_registered_flag: bool = False) -> EditorBulkData:
    """Parse the persistent UE5 FEditorBulkData layout used by UE 5.5.

    This mirrors FEditorBulkData::Serialize: EFlags, FGuid, FIoHash, int64,
    followed by OffsetInFile only for non-trailer, non-virtualized, non-cooked
    payloads. Older FByteBulkData is deliberately not accepted here.
    """
    start = reader.position
    flags = reader.u32()
    unknown = flags & ~KNOWN_FLAGS
    if unknown:
        raise UnsupportedError(f"FEditorBulkData has unknown flags 0x{unknown:08x} at 0x{start:x}")
    persisted_transient = flags & TRANSIENT_FLAGS
    if persisted_transient and not (
        allow_legacy_registered_flag and persisted_transient == HAS_REGISTERED
    ):
        raise FormatError(f"FEditorBulkData persisted transient flags 0x{persisted_transient:08x} at 0x{start:x}")
    if flags & IS_VIRTUALIZED and flags & REFERENCES_BY_PACKAGE_PATH:
        raise FormatError("FEditorBulkData cannot be both virtualized and package-path referenced")
    if flags & LEGACY_FILE_IS_COMPRESSED and not flags & REFERENCES_LEGACY_FILE:
        raise FormatError("LegacyFileIsCompressed is set without ReferencesLegacyFile")
    if flags & LEGACY_KEY_WAS_GUID_DERIVED and not flags & REFERENCES_LEGACY_FILE:
        raise FormatError("LegacyKeyWasGuidDerived is set without ReferencesLegacyFile")

    bulk_id_raw = reader.read(16)
    payload_id_raw = reader.read(20)
    payload_size = reader.i64()
    if payload_size < 0:
        raise FormatError(f"negative FEditorBulkData payload size {payload_size} at 0x{start:x}")
    if payload_size and not any(payload_id_raw) and not flags & IS_COOKED:
        raise FormatError("non-empty FEditorBulkData has a zero PayloadContentId")
    if payload_size and not any(bulk_id_raw) and not flags & IS_COOKED:
        raise FormatError("non-empty FEditorBulkData has an invalid BulkDataId")

    offset_in_file: int | None = None
    if not flags & (STORED_IN_PACKAGE_TRAILER | IS_VIRTUALIZED | IS_COOKED):
        offset_in_file = reader.i64()
        if payload_size and offset_in_file < 0:
            raise FormatError("non-empty disk-backed FEditorBulkData has a negative OffsetInFile")

    if flags & IS_COOKED:
        storage = "cooked-no-editor-payload"
    elif flags & STORED_IN_PACKAGE_TRAILER:
        storage = "package-trailer"
    elif flags & IS_VIRTUALIZED:
        storage = "virtualized"
    elif flags & HAS_PAYLOAD_SIDECAR_FILE:
        storage = "payload-sidecar"
    elif flags & REFERENCES_WORKSPACE_DOMAIN:
        storage = "workspace-domain"
    elif flags & REFERENCES_LEGACY_FILE:
        storage = "legacy-file"
    else:
        storage = "inline-or-end-of-package"

    end = reader.position
    raw = _serialized_bytes(reader, start, end)
    return EditorBulkData(
        metadata_offset=start,
        metadata_size=end - start,
        metadata_sha256=hashlib.sha256(raw).hexdigest(),
        flags=flags,
        flag_names=tuple(name for bit, name in FLAG_NAMES.items() if flags & bit),
        bulk_data_id=_guid(bulk_id_raw),
        bulk_data_id_bytes=bulk_id_raw.hex(),
        payload_content_id=payload_id_raw.hex(),
        payload_size=payload_size,
        offset_in_file=offset_in_file,
        storage=storage,
    )


def parse_legacy_bulk_data(reader: BinaryReader) -> LegacyBulkData:
    """Read FBulkMetaResource exactly as serialized by UE's FByteBulkData."""
    start = reader.position
    flags = reader.u32()
    unknown = flags & ~LEGACY_KNOWN_FLAGS
    if unknown:
        raise UnsupportedError(f"FByteBulkData has unknown flags 0x{unknown:08x} at 0x{start:x}")
    if flags & LEGACY_SIZE_64_BIT:
        element_count, size_on_disk, offset = reader.i64(), reader.i64(), reader.i64()
    else:
        element_count, size_on_disk, offset = reader.i32(), reader.i32(), reader.i64()
    if element_count < 0 or size_on_disk < 0:
        raise FormatError(
            f"negative FByteBulkData count/size {element_count}/{size_on_disk} at 0x{start:x}"
        )
    if flags & LEGACY_BAD_DATA_VERSION:
        reader.u16()
    duplicate_flags = duplicate_size = duplicate_offset = None
    if flags & LEGACY_DUPLICATE_NON_OPTIONAL:
        duplicate_flags = reader.u32()
        if duplicate_flags & ~LEGACY_KNOWN_FLAGS:
            raise UnsupportedError(f"duplicate FByteBulkData has unknown flags 0x{duplicate_flags:08x}")
        duplicate_size = reader.i64() if flags & LEGACY_SIZE_64_BIT else reader.i32()
        duplicate_offset = reader.i64()
        if duplicate_size < 0:
            raise FormatError("negative duplicate FByteBulkData size")
    if flags & LEGACY_FORCE_INLINE and flags & LEGACY_PAYLOAD_AT_END:
        raise FormatError("FByteBulkData cannot be both inline and at end of file")
    if flags & LEGACY_OPTIONAL_PAYLOAD and not flags & LEGACY_SEPARATE_FILE:
        raise FormatError("optional FByteBulkData is not marked as a separate payload")
    end = reader.position
    raw = _serialized_bytes(reader, start, end)
    return LegacyBulkData(
        metadata_offset=start,
        metadata_size=end - start,
        metadata_sha256=hashlib.sha256(raw).hexdigest(),
        flags=flags,
        flag_names=tuple(name for bit, name in LEGACY_FLAG_NAMES.items() if flags & bit),
        element_count=element_count,
        size_on_disk=size_on_disk,
        offset_in_file=offset,
        duplicate_flags=duplicate_flags,
        duplicate_size_on_disk=duplicate_size,
        duplicate_offset_in_file=duplicate_offset,
    )


def parse_mesh_description_bulk_data(
    reader: BinaryReader,
    *,
    end: int,
    allow_legacy_registered_flag: bool = False,
) -> MeshDescriptionBulkData:
    """Parse current FMeshDescriptionBulkData and require exact stream use."""
    start = reader.position
    if end - start < 68:
        raise UnsupportedError(
            f"mesh bulk metadata is {end - start} bytes; current FEditorBulkData layout needs 68"
        )
    editor_bulk = parse_editor_bulk_data(
        reader, allow_legacy_registered_flag=allow_legacy_registered_flag,
    )
    mesh_guid_raw = reader.read(16)
    guid_is_hash = reader.boolean32()
    if reader.position != end:
        raise UnsupportedError(
            f"mesh bulk metadata left {end - reader.position} unclassified bytes at 0x{reader.position:x}"
        )
    if editor_bulk.payload_size == 0:
        if any(mesh_guid_raw) or guid_is_hash:
            raise FormatError("empty FMeshDescriptionBulkData has a non-empty hash GUID")
    elif guid_is_hash and mesh_guid_raw != bytes.fromhex(editor_bulk.payload_content_id[:32]):
        raise FormatError("FMeshDescriptionBulkData hash GUID does not match its PayloadContentId")
    raw = _serialized_bytes(reader, start, end)
    return MeshDescriptionBulkData(
        editor_bulk=editor_bulk,
        mesh_guid=_guid(mesh_guid_raw),
        mesh_guid_bytes=mesh_guid_raw.hex(),
        guid_is_hash=guid_is_hash,
        serialized_size=len(raw),
        serialized_sha256=hashlib.sha256(raw).hexdigest(),
    )


def match_trailer_entry(bulk: EditorBulkData, trailer: PackageTrailer) -> TrailerEntry:
    """Resolve an EditorBulkData payload by its exact FIoHash and validate size/storage."""
    if bulk.storage != "package-trailer":
        raise UnsupportedError(f"bulk payload storage is {bulk.storage}, not package-trailer")
    matches = [entry for entry in trailer.entries if entry.identifier == bulk.payload_content_id]
    if len(matches) != 1:
        raise FormatError(
            f"expected exactly one trailer entry for payload {bulk.payload_content_id}, found {len(matches)}"
        )
    entry = matches[0]
    if entry.raw_size != bulk.payload_size:
        raise FormatError(
            f"FEditorBulkData size {bulk.payload_size} != trailer raw size {entry.raw_size}"
        )
    return entry
