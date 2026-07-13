from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass(frozen=True)
class FNameRef:
    index: int
    number: int = 0
    text: str | None = None

    @property
    def display(self) -> str:
        base = self.text if self.text is not None else f"<name:{self.index}>"
        return base if self.number == 0 else f"{base}_{self.number - 1}"


@dataclass
class NameEntry:
    text: str
    non_case_hash: int | None = None
    case_hash: int | None = None


@dataclass
class EngineVersion:
    major: int
    minor: int
    patch: int
    changelist: int
    branch: str

    @property
    def display(self) -> str:
        suffix = f"-{self.changelist}" if self.changelist else ""
        return f"{self.major}.{self.minor}.{self.patch}{suffix} {self.branch}".strip()


@dataclass
class PackageSummary:
    tag: int
    legacy_file_version: int
    legacy_ue3_version: int | None
    file_version_ue4: int
    file_version_ue5: int
    file_version_licensee: int
    custom_versions: list[dict[str, Any]] = field(default_factory=list)
    unversioned: bool = False
    saved_hash: str | None = None
    total_header_size: int = 0
    package_name: str = ""
    package_flags: int = 0
    name_count: int = 0
    name_offset: int = 0
    soft_object_paths_count: int = 0
    soft_object_paths_offset: int = 0
    localization_id: str | None = None
    gatherable_text_count: int = 0
    gatherable_text_offset: int = 0
    export_count: int = 0
    export_offset: int = 0
    import_count: int = 0
    import_offset: int = 0
    cell_export_count: int = 0
    cell_export_offset: int = 0
    cell_import_count: int = 0
    cell_import_offset: int = 0
    metadata_offset: int = 0
    depends_offset: int = 0
    soft_package_references_count: int = 0
    soft_package_references_offset: int = 0
    searchable_names_offset: int = 0
    thumbnail_table_offset: int = 0
    import_type_hierarchies_count: int = 0
    import_type_hierarchies_offset: int = 0
    guid: str | None = None
    persistent_guid: str | None = None
    generations: list[dict[str, int]] = field(default_factory=list)
    saved_by_engine_version: EngineVersion | None = None
    compatible_engine_version: EngineVersion | None = None
    compression_flags: int = 0
    compressed_chunks_count: int = 0
    package_source: int = 0
    asset_registry_data_offset: int = 0
    bulk_data_start_offset: int = 0
    world_tile_info_data_offset: int = 0
    chunk_ids: list[int] = field(default_factory=list)
    preload_dependency_count: int = -1
    preload_dependency_offset: int = 0
    names_referenced_from_export_data_count: int = 0
    payload_toc_offset: int = -1
    data_resource_offset: int = -1
    summary_end: int = 0

    @property
    def filter_editor_only(self) -> bool:
        return bool(self.package_flags & 0x80000000)


@dataclass
class ObjectImport:
    class_package: FNameRef
    class_name: FNameRef
    outer_index: int
    object_name: FNameRef
    package_name: FNameRef | None = None
    optional: bool = False


@dataclass
class ObjectExport:
    class_index: int
    super_index: int
    template_index: int
    outer_index: int
    object_name: FNameRef
    object_flags: int
    serial_size: int
    serial_offset: int
    forced_export: bool
    not_for_client: bool
    not_for_server: bool
    package_guid: str | None
    inherited_instance: bool
    package_flags: int
    not_always_loaded_for_editor_game: bool
    is_asset: bool
    generate_public_hash: bool
    first_export_dependency: int
    serialization_before_serialization_dependencies: int
    create_before_serialization_dependencies: int
    serialization_before_create_dependencies: int
    create_before_create_dependencies: int
    script_serialization_start_offset: int = 0
    script_serialization_end_offset: int = 0
    class_name: str | None = None
    outer_path: str | None = None
    payload_availability: str = "unknown"
    payload_source: str | None = None
    payload_physical_offset: int | None = None
    payload_sha256: str | None = None
    preload_dependencies: dict[str, list[dict[str, Any]]] = field(default_factory=dict)


def to_plain(value: Any) -> Any:
    if hasattr(value, "__dataclass_fields__"):
        return {k: to_plain(v) for k, v in asdict(value).items()}
    if isinstance(value, dict):
        return {str(k): to_plain(v) for k, v in value.items()}
    if isinstance(value, (list, tuple)):
        return [to_plain(v) for v in value]
    return value
