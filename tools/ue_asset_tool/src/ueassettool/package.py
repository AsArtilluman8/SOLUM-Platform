from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .errors import BoundsError, FormatError, UnsupportedError
from .model import EngineVersion, FNameRef, NameEntry, ObjectExport, ObjectImport, PackageSummary, to_plain
from . import versions as ver


class UnrealPackage:
    """Parse a classic (non-Zen) .uasset/.umap package without guessing.

    The core reader covers legacy package summaries, names, imports and
    exports.  Semantic export decoding is intentionally a separate layer so
    unsupported native serialization can never masquerade as a decoded value.
    """

    def __init__(self, path: str | Path):
        self.path = Path(path)
        self.issues: list[dict[str, Any]] = []
        self.reader = BinaryReader(self.path)
        try:
            self.summary = self._read_summary()
            self._validate_summary()
            self.names = self._read_names()
            self.imports = self._read_imports()
            self.exports = self._read_exports()
            self._resolve_exports()
        except Exception:
            self.reader.close()
            raise

    def close(self) -> None:
        self.reader.close()

    def __enter__(self) -> "UnrealPackage":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    @property
    def sha256(self) -> str:
        digest = hashlib.sha256()
        with self.path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()

    def _read_engine_version(self) -> EngineVersion:
        r = self.reader
        return EngineVersion(r.u16(), r.u16(), r.u16(), r.u32(), r.fstring())

    def _read_custom_versions(self, legacy: int) -> list[dict[str, Any]]:
        r = self.reader
        count = r.count("custom version", maximum=100_000)
        result: list[dict[str, Any]] = []
        for _ in range(count):
            if legacy == -2:
                result.append({"tag": r.u32(), "version": r.i32()})
            elif -5 <= legacy < -2:
                result.append({"guid": r.guid(), "version": r.i32(), "friendly_name": r.fstring()})
            elif legacy < -5:
                result.append({"guid": r.guid(), "version": r.i32()})
            else:
                raise UnsupportedError(f"custom version encoding for LegacyFileVersion={legacy}")
        return result

    def _read_summary(self) -> PackageSummary:
        r = self.reader
        tag = r.u32()
        if tag == ver.PACKAGE_FILE_TAG_SWAPPED:
            raise UnsupportedError("byte-swapped Unreal packages are not supported")
        if tag != ver.PACKAGE_FILE_TAG:
            raise FormatError(f"bad package magic 0x{tag:08x}")

        legacy = r.i32()
        if legacy >= 0:
            raise UnsupportedError("UE3 packages are outside this reader")
        if legacy < -9:
            raise UnsupportedError(f"future LegacyFileVersion {legacy} cannot be parsed safely")
        legacy_ue3 = None if legacy == -4 else r.i32()
        ue4 = r.i32()
        ue5 = r.i32() if legacy <= -8 else 0
        licensee = r.i32()
        if ue4 > ver.UE4_AUTOMATIC_VERSION or ue5 > ver.UE5_AUTOMATIC_VERSION:
            raise UnsupportedError(
                f"package object version UE4={ue4}, UE5={ue5} is newer than verified "
                f"UE4={ver.UE4_AUTOMATIC_VERSION}, UE5={ver.UE5_AUTOMATIC_VERSION}"
            )
        unversioned = ue4 == 0 and ue5 == 0 and licensee == 0
        saved_hash = None
        total_header_size = 0
        if ue5 >= ver.UE5_PACKAGE_SAVED_HASH:
            saved_hash = r.read(20).hex()
            total_header_size = r.i32()
        custom_versions = self._read_custom_versions(legacy) if legacy <= -2 else []
        if ue5 < ver.UE5_PACKAGE_SAVED_HASH:
            total_header_size = r.i32()

        s = PackageSummary(
            tag=tag,
            legacy_file_version=legacy,
            legacy_ue3_version=legacy_ue3,
            file_version_ue4=ue4,
            file_version_ue5=ue5,
            file_version_licensee=licensee,
            custom_versions=custom_versions,
            unversioned=unversioned,
            saved_hash=saved_hash,
            total_header_size=total_header_size,
        )
        s.package_name = r.fstring()
        s.package_flags = r.u32()
        s.name_count, s.name_offset = r.i32(), r.i32()
        if ue5 >= ver.UE5_ADD_SOFTOBJECTPATH_LIST:
            s.soft_object_paths_count, s.soft_object_paths_offset = r.i32(), r.i32()
        if not s.filter_editor_only and ue4 >= ver.UE4_ADDED_PACKAGE_SUMMARY_LOCALIZATION_ID:
            s.localization_id = r.fstring()
        if ue4 >= ver.UE4_SERIALIZE_TEXT_IN_PACKAGES:
            s.gatherable_text_count, s.gatherable_text_offset = r.i32(), r.i32()
        s.export_count, s.export_offset = r.i32(), r.i32()
        s.import_count, s.import_offset = r.i32(), r.i32()
        if ue5 >= ver.UE5_VERSE_CELLS:
            s.cell_export_count, s.cell_export_offset = r.i32(), r.i32()
            s.cell_import_count, s.cell_import_offset = r.i32(), r.i32()
        if ue5 >= ver.UE5_METADATA_SERIALIZATION_OFFSET:
            s.metadata_offset = r.i32()
        s.depends_offset = r.i32()
        if ue4 >= ver.UE4_ADD_STRING_ASSET_REFERENCES_MAP:
            s.soft_package_references_count, s.soft_package_references_offset = r.i32(), r.i32()
        if ue4 >= ver.UE4_ADDED_SEARCHABLE_NAMES:
            s.searchable_names_offset = r.i32()
        s.thumbnail_table_offset = r.i32()
        if ue5 >= ver.UE5_IMPORT_TYPE_HIERARCHIES:
            s.import_type_hierarchies_count, s.import_type_hierarchies_offset = r.i32(), r.i32()
        if ue5 < ver.UE5_PACKAGE_SAVED_HASH:
            s.guid = r.guid()
        if not s.filter_editor_only and ue4 >= ver.UE4_ADDED_PACKAGE_OWNER:
            s.persistent_guid = r.guid()
            if ue4 < ver.UE4_NON_OUTER_PACKAGE_IMPORT:
                _owner_persistent_guid = r.guid()

        generation_count = r.count("generation", maximum=1_000_000)
        for _ in range(generation_count):
            s.generations.append({"export_count": r.i32(), "name_count": r.i32()})
        if ue4 >= ver.UE4_ENGINE_VERSION_OBJECT:
            s.saved_by_engine_version = self._read_engine_version()
        else:
            changelist = r.i32()
            s.saved_by_engine_version = EngineVersion(4, 0, 0, changelist, "") if changelist else None
        if ue4 >= ver.UE4_PACKAGE_SUMMARY_COMPATIBLE_ENGINE_VERSION:
            s.compatible_engine_version = self._read_engine_version()
        else:
            s.compatible_engine_version = s.saved_by_engine_version

        s.compression_flags = r.u32()
        compressed_count = r.count("compressed chunk", maximum=1_000_000)
        s.compressed_chunks_count = compressed_count
        if compressed_count:
            raise UnsupportedError("legacy package-level compressed chunks require a decompression layer")
        s.package_source = r.u32()
        additional_count = r.count("additional package to cook", maximum=1_000_000)
        for _ in range(additional_count):
            r.fstring()
        if legacy > -7:
            allocation_count = r.i32()
            if allocation_count:
                raise UnsupportedError("legacy texture allocation table is not implemented")
        s.asset_registry_data_offset = r.i32()
        s.bulk_data_start_offset = r.i64()
        if ue4 >= ver.UE4_WORLD_LEVEL_INFO:
            s.world_tile_info_data_offset = r.i32()
        if ue4 >= ver.UE4_CHANGED_CHUNKID_TO_ARRAY:
            chunk_count = r.count("chunk id", maximum=1_000_000)
            s.chunk_ids = [r.i32() for _ in range(chunk_count)]
        if ue4 >= ver.UE4_PRELOAD_DEPENDENCIES:
            s.preload_dependency_count, s.preload_dependency_offset = r.i32(), r.i32()
        if ue5 >= ver.UE5_NAMES_REFERENCED_FROM_EXPORT_DATA:
            s.names_referenced_from_export_data_count = r.i32()
        else:
            s.names_referenced_from_export_data_count = s.name_count
        if ue5 >= ver.UE5_PAYLOAD_TOC:
            s.payload_toc_offset = r.i64()
        if ue5 >= ver.UE5_DATA_RESOURCES:
            s.data_resource_offset = r.i32()
        s.summary_end = r.position
        return s

    def _validate_summary(self) -> None:
        s, size = self.summary, self.reader.size
        if s.unversioned:
            raise UnsupportedError(
                "unversioned packages require an explicit engine version and usually a .usmap schema"
            )
        for label, count in (("name", s.name_count), ("import", s.import_count), ("export", s.export_count)):
            if not 0 <= count <= 10_000_000:
                raise FormatError(f"invalid {label} count {count}")
        for label, offset in (
            ("summary end", s.summary_end),
            ("name map", s.name_offset),
            ("import map", s.import_offset),
            ("export map", s.export_offset),
        ):
            if not 0 <= offset <= size:
                raise BoundsError(f"{label} offset 0x{offset:x} outside file size 0x{size:x}")
        if not 0 <= s.total_header_size <= size:
            # A partial upload/package is still inspectable if its summary and
            # maps are present. Record truncation and let per-export resolution
            # enumerate unavailable byte ranges; never seek to this value.
            self.issues.append({
                "code": "DECLARED_HEADER_EXCEEDS_FILE",
                "severity": "error",
                "declared_total_header_size": s.total_header_size,
                "actual_file_size": size,
                "minimum_missing_bytes": max(0, s.total_header_size - size),
            })
        if s.summary_end > s.name_offset:
            raise FormatError(
                f"summary ends at 0x{s.summary_end:x}, after name map at 0x{s.name_offset:x}"
            )

    def fname(self, raw: tuple[int, int]) -> FNameRef:
        index, number = raw
        if not 0 <= index < len(self.names):
            raise FormatError(f"FName index {index} outside 0..{len(self.names) - 1}")
        if number < 0:
            raise FormatError(f"negative FName number {number}")
        return FNameRef(index=index, number=number, text=self.names[index].text)

    def _read_fname(self) -> FNameRef:
        return self.fname(self.reader.fname_raw())

    def _read_names(self) -> list[NameEntry]:
        r, s = self.reader, self.summary
        r.seek(s.name_offset)
        names: list[NameEntry] = []
        for _ in range(s.name_count):
            text = r.fstring(max_units=1_048_576)
            if s.file_version_ue4 >= ver.UE4_NAME_HASHES_SERIALIZED:
                names.append(NameEntry(text, r.u16(), r.u16()))
            else:
                names.append(NameEntry(text))
        if not names or not any(entry.text == "None" for entry in names):
            raise FormatError("name map invariant failed: no canonical 'None' entry")
        return names

    def _read_imports(self) -> list[ObjectImport]:
        r, s = self.reader, self.summary
        r.seek(s.import_offset)
        result: list[ObjectImport] = []
        for _ in range(s.import_count):
            class_package = self._read_fname()
            class_name = self._read_fname()
            outer_index = r.i32()
            object_name = self._read_fname()
            package_name = None
            if s.file_version_ue4 >= ver.UE4_NON_OUTER_PACKAGE_IMPORT and not s.filter_editor_only:
                package_name = self._read_fname()
            optional = r.boolean32() if s.file_version_ue5 >= ver.UE5_OPTIONAL_RESOURCES else False
            result.append(ObjectImport(class_package, class_name, outer_index, object_name, package_name, optional))
        return result

    def _read_exports(self) -> list[ObjectExport]:
        r, s = self.reader, self.summary
        r.seek(s.export_offset)
        result: list[ObjectExport] = []
        for _ in range(s.export_count):
            class_index = r.i32()
            super_index = r.i32()
            template_index = r.i32() if s.file_version_ue4 >= ver.UE4_TEMPLATE_INDEX_IN_COOKED_EXPORTS else 0
            outer_index = r.i32()
            object_name = self._read_fname()
            object_flags = r.u32()
            if s.file_version_ue4 >= ver.UE4_64BIT_EXPORTMAP_SERIALSIZES:
                serial_size, serial_offset = r.i64(), r.i64()
            else:
                serial_size, serial_offset = r.i32(), r.i32()
            forced_export, not_for_client, not_for_server = r.boolean32(), r.boolean32(), r.boolean32()
            package_guid = r.guid() if s.file_version_ue5 < ver.UE5_REMOVE_OBJECT_EXPORT_PACKAGE_GUID else None
            inherited_instance = (
                r.boolean32() if s.file_version_ue5 >= ver.UE5_TRACK_OBJECT_EXPORT_IS_INHERITED else False
            )
            package_flags = r.u32()
            not_always = r.boolean32() if s.file_version_ue4 >= ver.UE4_LOAD_FOR_EDITOR_GAME else False
            is_asset = r.boolean32() if s.file_version_ue4 >= ver.UE4_COOKED_ASSETS_IN_EDITOR_SUPPORT else False
            generate_hash = r.boolean32() if s.file_version_ue5 >= ver.UE5_OPTIONAL_RESOURCES else False
            deps = [-1, 0, 0, 0, 0]
            if s.file_version_ue4 >= ver.UE4_PRELOAD_DEPENDENCIES:
                deps = [r.i32() for _ in range(5)]
            script_start = script_end = 0
            if not s.unversioned and s.file_version_ue5 >= ver.UE5_SCRIPT_SERIALIZATION_OFFSET:
                script_start, script_end = r.i64(), r.i64()
            if serial_size < 0 or serial_offset < 0:
                raise FormatError(f"negative export range offset={serial_offset}, size={serial_size}")
            result.append(
                ObjectExport(
                    class_index, super_index, template_index, outer_index, object_name, object_flags,
                    serial_size, serial_offset, forced_export, not_for_client, not_for_server,
                    package_guid, inherited_instance, package_flags, not_always, is_asset,
                    generate_hash, *deps, script_start, script_end,
                )
            )
        return result

    def resolve_index_name(self, index: int) -> str:
        if index == 0:
            return "None"
        if index < 0:
            at = -index - 1
            return self.imports[at].object_name.display if 0 <= at < len(self.imports) else f"<bad-import:{index}>"
        at = index - 1
        return self.exports[at].object_name.display if 0 <= at < len(self.exports) else f"<bad-export:{index}>"

    def custom_version(self, guid: str, default: int = -1) -> int:
        wanted = guid.lower()
        for item in self.summary.custom_versions:
            if str(item.get("guid", "")).lower() == wanted:
                return int(item["version"])
        return default

    def object_path(self, index: int, _seen: set[int] | None = None) -> str:
        if index == 0:
            return self.summary.package_name or self.path.stem
        seen = set() if _seen is None else _seen
        if index in seen:
            return "<outer-cycle>"
        seen.add(index)
        if index < 0:
            at = -index - 1
            if not 0 <= at < len(self.imports):
                return f"<bad-import:{index}>"
            obj = self.imports[at]
            parent = self.object_path(obj.outer_index, seen) if obj.outer_index else ""
            return f"{parent}.{obj.object_name.display}" if parent else obj.object_name.display
        at = index - 1
        if not 0 <= at < len(self.exports):
            return f"<bad-export:{index}>"
        obj = self.exports[at]
        parent = self.object_path(obj.outer_index, seen) if obj.outer_index else self.summary.package_name
        return f"{parent}.{obj.object_name.display}" if parent else obj.object_name.display

    def _resolve_exports(self) -> None:
        for i, item in enumerate(self.exports, 1):
            item.class_name = self.resolve_index_name(item.class_index)
            item.outer_path = self.object_path(item.outer_index) if item.outer_index else self.summary.package_name
            if item.serial_size == 0:
                item.payload_availability = "empty"
                item.payload_source = str(self.path)
                item.payload_physical_offset = item.serial_offset
                continue
            if item.serial_offset + item.serial_size <= self.reader.size:
                item.payload_availability = "available"
                item.payload_source = str(self.path)
                item.payload_physical_offset = item.serial_offset
                continue

            # Cooked split packages use logical package offsets for bytes held
            # in the companion .uexp.  Validate both layouts seen in the wild;
            # never select a candidate that does not contain the whole export.
            sidecar = self.path.with_suffix(".uexp")
            if sidecar.exists():
                sidecar_size = sidecar.stat().st_size
                candidates = [
                    item.serial_offset - self.summary.total_header_size,
                    item.serial_offset - self.reader.size,
                ]
                valid = [at for at in dict.fromkeys(candidates) if 0 <= at and at + item.serial_size <= sidecar_size]
                if len(valid) == 1:
                    item.payload_availability = "available"
                    item.payload_source = str(sidecar)
                    item.payload_physical_offset = valid[0]
                    continue
                if len(valid) > 1 and valid[0] == valid[1]:
                    item.payload_availability = "available"
                    item.payload_source = str(sidecar)
                    item.payload_physical_offset = valid[0]
                    continue

            item.payload_availability = "missing_sidecar"
            self.issues.append({
                "code": "MISSING_EXPORT_PAYLOAD",
                "severity": "error",
                "export_index": i,
                "object": item.object_name.display,
                "class": item.class_name,
                "logical_offset": item.serial_offset,
                "size": item.serial_size,
                "required": str(sidecar),
            })

    def inspect_dict(self) -> dict[str, Any]:
        available = sum(x.payload_availability in ("available", "empty") for x in self.exports)
        missing = len(self.exports) - available
        max_export_end = max((x.serial_offset + x.serial_size for x in self.exports), default=0)
        if missing:
            self.issues.insert(0, {
                "code": "PACKAGE_INCOMPLETE",
                "severity": "error",
                "message": "one or more declared export byte ranges are not present",
                "available_exports": available,
                "missing_exports": missing,
                "actual_uasset_size": self.reader.size,
                "minimum_declared_export_end": max_export_end,
                "minimum_missing_bytes": max(0, max_export_end - self.reader.size),
            })
        return {
            "schema": "ueassettool.inspect/v1",
            "source": {
                "path": str(self.path),
                "size": self.reader.size,
                "sha256": self.sha256,
            },
            "issues": self.issues,
            "integrity": {
                "available_exports": available,
                "missing_exports": missing,
                "maximum_declared_export_end": max_export_end,
            },
            "summary": to_plain(self.summary),
            "names": [to_plain(x) for x in self.names],
            "imports": [to_plain(x) for x in self.imports],
            "exports": [to_plain(x) for x in self.exports],
        }
