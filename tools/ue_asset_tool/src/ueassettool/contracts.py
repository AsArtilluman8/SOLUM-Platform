from __future__ import annotations

import hashlib
import math
import struct
from pathlib import Path
from typing import Any, Callable

from .blueprint import BlueprintGraphDecoder
from .bytecode import StructScriptDecoder
from .curve import CURVE_CLASSES, export_curve_contract
from .errors import BoundsError, UEAssetError
from .material import MATERIAL_ROOT_CLASSES, export_material_contract
from .package import UnrealPackage
from .properties import PropertyParser
from .trailer import read_package_trailer


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _export_payload_sha256(package: UnrealPackage, export_index: int) -> str:
    export = package.exports[export_index - 1]
    source = Path(export.payload_source or package.path)
    offset = int(export.payload_physical_offset or 0)
    digest = hashlib.sha256()
    remaining = export.serial_size
    with source.open("rb") as handle:
        handle.seek(offset)
        while remaining:
            chunk = handle.read(min(remaining, 1024 * 1024))
            if not chunk:
                raise BoundsError(
                    f"export {export_index} payload ended before {export.serial_size} bytes"
                )
            digest.update(chunk)
            remaining -= len(chunk)
    return digest.hexdigest()


def _source(package: UnrealPackage) -> dict[str, Any]:
    version = package.summary.saved_by_engine_version
    return {
        "path": str(package.path), "size": package.path.stat().st_size, "sha256": package.sha256,
        "package": package.summary.package_name,
        "engine": version.display if version else None,
        "file_version_ue4": package.summary.file_version_ue4,
        "file_version_ue5": package.summary.file_version_ue5,
    }


def _exports(package: UnrealPackage, predicate: Callable[[int, str, str], bool]) -> list[dict[str, Any]]:
    parser = PropertyParser(package)
    result: list[dict[str, Any]] = []
    for index, export in enumerate(package.exports, 1):
        class_name = export.class_name or ""
        path = package.object_path(index)
        if not predicate(index, class_name, path):
            continue
        if export.payload_availability not in ("available", "empty"):
            result.append({
                "export_index": index, "object": path, "class": class_name,
                "status": "MISSING_INPUT", "reason": export.payload_availability,
            })
            continue
        try:
            decoded = parser.parse_export(index)
            decoded["truth_status"] = "VERIFIED" if decoded.get("parse_status") == "decoded_properties" and not decoded.get("trailing_native") else "RAW_VERIFIED"
            result.append(decoded)
        except (UEAssetError, UnicodeError) as exc:
            result.append({
                "export_index": index, "object": path, "class": class_name,
                "status": "UNSUPPORTED", "reason": str(exc),
            })
    return result


def _dependencies(package: UnrealPackage) -> list[dict[str, Any]]:
    result = []
    for index, item in enumerate(package.imports, 1):
        result.append({
            "import_index": -index, "object": package.object_path(-index),
            "class_package": item.class_package.display, "class": item.class_name.display,
            "optional": item.optional,
        })
    return result


def _aggregate_status(exports: list[dict[str, Any]]) -> str:
    statuses = {item.get("status") or item.get("truth_status") for item in exports}
    if "MISSING_INPUT" in statuses:
        return "MISSING_INPUT"
    if "UNSUPPORTED" in statuses:
        return "UNSUPPORTED"
    if "RAW_VERIFIED" in statuses:
        return "RAW_VERIFIED"
    return "VERIFIED" if exports else "UNSUPPORTED"


def _is_blueprint_contract_export(class_name: str, object_path: str) -> bool:
    """Select graph objects and the generated class default object.

    Blueprint CDO exports use the project-specific generated class name (for
    example ``Ultra_Dynamic_Sky_C``), so class-name-only selection silently
    discarded the serialized defaults that drive user-visible controls.
    ``Default__`` is Unreal's serialized CDO identity and does not select
    arbitrary generated-class instances.
    """
    classes = (
        "Blueprint", "BlueprintGeneratedClass", "EdGraph", "EdGraphSchema",
        "Function", "TimelineTemplate", "SimpleConstructionScript", "SCS_Node",
    )
    object_name = object_path.rsplit(".", 1)[-1]
    return (
        class_name.startswith("K2Node_")
        or class_name in classes
        or class_name.endswith("Blueprint")
        or class_name.endswith("BlueprintGeneratedClass")
        or object_name.startswith("Default__")
    )


def _decoded_values(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {}
    result: dict[str, Any] = {}
    for prop in value.get("properties", []):
        if str(prop.get("decode_status", "")).startswith("decoded"):
            result[str(prop["name"])] = prop.get("value")
    return result


def _niagara_type(variable: dict[str, Any]) -> dict[str, Any]:
    fields = _decoded_values(variable.get("type_definition"))
    class_value = fields.get("ClassStructOrEnum")
    return {
        "object": class_value.get("object") if isinstance(class_value, dict) else None,
        "package_index": class_value.get("package_index") if isinstance(class_value, dict) else None,
        "underlying_type": fields.get("UnderlyingType"),
        "flags": fields.get("Flags"),
    }


def _niagara_scalar(data: bytes, offset: int, type_object: str | None) -> dict[str, Any]:
    short = (type_object or "").rsplit(".", 1)[-1]
    formats = {
        "NiagaraFloat": ("<f", "float"),
        "NiagaraInt32": ("<i", "int32"),
        "NiagaraBool": ("<i", "bool32"),
        "NiagaraVec2": ("<2f", "float2"),
        "NiagaraVec3": ("<3f", "float3"),
        "NiagaraPosition": ("<3f", "position3f"),
        "NiagaraVec4": ("<4f", "float4"),
        "NiagaraColor": ("<4f", "color4f"),
        "NiagaraQuat": ("<4f", "quat4f"),
        "LinearColor": ("<4f", "linear-color4f"),
        "Vector2f": ("<2f", "float2"),
        "Vector3f": ("<3f", "float3"),
        "Vector4f": ("<4f", "float4"),
        "Quat4f": ("<4f", "quat4f"),
    }
    if short not in formats:
        return {"status": "RAW_VERIFIED", "reason": f"no scalar decoder for {short or '<unknown>'}"}
    fmt, kind = formats[short]
    size = struct.calcsize(fmt)
    if offset < 0 or offset + size > len(data):
        return {"status": "UNSUPPORTED", "reason": f"offset {offset}+{size} exceeds ParameterData {len(data)}"}
    raw = data[offset:offset + size]
    values = struct.unpack(fmt, raw)
    if any(isinstance(item, float) and not math.isfinite(item) for item in values):
        return {
            "status": "RAW_VERIFIED", "reason": "non-finite scalar retained as raw bytes",
            "kind": kind, "size": size, "raw_hex": raw.hex(),
        }
    value: Any = values[0] if len(values) == 1 else list(values)
    if kind == "bool32":
        raw_value = int(value)
        if raw_value not in (0, 1):
            return {
                "status": "RAW_VERIFIED", "kind": kind,
                "reason": f"bool32 value {raw_value} is outside 0/1",
                "raw_value": raw_value, "size": size, "raw_hex": raw.hex(),
            }
        value = bool(raw_value)
        return {
            "status": "VERIFIED", "kind": kind, "value": value,
            "raw_value": raw_value, "size": size, "raw_hex": raw.hex(),
        }
    return {
        "status": "VERIFIED", "kind": kind, "value": value,
        "size": size, "raw_hex": raw.hex(),
    }


def _niagara_variable_bytes(variable: dict[str, Any]) -> bytes | None:
    data_hex = variable.get("data_hex")
    if data_hex is None:
        return None
    if not isinstance(data_hex, str) or len(data_hex) % 2:
        raise ValueError("Niagara variable data_hex is malformed")
    data = bytes.fromhex(data_hex)
    if variable.get("data_size") != len(data):
        raise ValueError("Niagara variable data_size does not match data_hex")
    if variable.get("data_sha256") != hashlib.sha256(data).hexdigest():
        raise ValueError("Niagara variable data SHA-256 does not match data_hex")
    return data


def _niagara_variable_summary(variable: Any) -> dict[str, Any]:
    if not isinstance(variable, dict):
        return {"status": "UNSUPPORTED", "reason": "Niagara variable is not a decoded structure"}
    if variable.get("serialization") == "legacy-tagged":
        fields = _decoded_values(variable)
        return {
            "status": "RAW_VERIFIED",
            "serialization": "legacy-tagged",
            "fields": fields,
            "reason": "legacy tagged fields retained; normalized type identity is not inferred",
        }
    type_info = _niagara_type(variable)
    try:
        data = _niagara_variable_bytes(variable)
    except (TypeError, ValueError) as exc:
        return {
            "status": "UNSUPPORTED", "name": variable.get("name"),
            "type": type_info, "reason": str(exc),
        }
    default: dict[str, Any] | None
    if data is None:
        default = None
    elif not data:
        default = {"status": "NOT_PRESENT", "size": 0, "raw_hex": ""}
    else:
        default = _niagara_scalar(data, 0, type_info.get("object"))
        if default.get("size") != len(data):
            default = {
                "status": "RAW_VERIFIED",
                "reason": f"decoded scalar size {default.get('size')} != variable data size {len(data)}",
                "size": len(data), "raw_hex": data.hex(),
            }
    status = "VERIFIED"
    if default and default.get("status") not in ("VERIFIED", "NOT_PRESENT"):
        status = "RAW_VERIFIED"
    return {
        "status": status,
        "serialization": variable.get("serialization"),
        "name": variable.get("name"),
        "type": type_info,
        "default": default,
        "data_size": None if data is None else len(data),
        "data_sha256": None if data is None else hashlib.sha256(data).hexdigest(),
    }


def _array_items(value: Any) -> list[Any]:
    return value.get("items", []) if isinstance(value, dict) and isinstance(value.get("items"), list) else []


def _niagara_parameter_store(value: Any) -> dict[str, Any]:
    """Normalize a tagged FNiagaraParameterStore with offset/bounds checks."""
    if not isinstance(value, dict):
        return {"status": "UNSUPPORTED", "reason": "parameter store is not a decoded struct"}
    fields = _decoded_values(value)
    if not fields:
        return {
            "status": "VERIFIED", "parameters": [], "parameter_count": 0,
            "parameter_data": {"size": 0, "sha256": hashlib.sha256(b"").hexdigest()},
            "data_interfaces": [], "uobjects": [],
            "integrity": {"duplicate_name_count": 0, "overlap_count": 0, "invalid_offset_count": 0},
        }
    parameter_items = _array_items(fields.get("SortedParameterOffsets"))
    raw_items = _array_items(fields.get("ParameterData"))
    if any(not isinstance(item, int) or not 0 <= item <= 255 for item in raw_items):
        return {"status": "UNSUPPORTED", "reason": "ParameterData is not a decoded byte array"}
    parameter_data = bytes(raw_items)
    data_interfaces = _array_items(fields.get("DataInterfaces"))
    uobjects = _array_items(fields.get("UObjects"))
    parameters: list[dict[str, Any]] = []
    spans: list[tuple[int, int, str | None]] = []
    invalid_offsets = 0
    names: list[str | None] = []
    for variable in parameter_items:
        if not isinstance(variable, dict):
            parameters.append({"status": "UNSUPPORTED", "reason": "offset entry is not a variable"})
            invalid_offsets += 1
            continue
        type_info = _niagara_type(variable)
        name = variable.get("name")
        names.append(name if isinstance(name, str) else None)
        offset = variable.get("offset")
        if not isinstance(offset, int) or offset < 0:
            decoded_value = {"status": "UNSUPPORTED", "reason": f"invalid offset {offset!r}"}
            invalid_offsets += 1
        else:
            type_object = str(type_info.get("object") or "")
            if "NiagaraDataInterface" in type_object:
                resolved = data_interfaces[offset] if offset < len(data_interfaces) else None
                decoded_value = {
                    "status": "VERIFIED" if resolved is not None else "UNSUPPORTED",
                    "kind": "data_interface", "index": offset, "object": resolved,
                }
            elif type_info.get("underlying_type") == 1:
                resolved = uobjects[offset] if offset < len(uobjects) else None
                decoded_value = {
                    "status": "VERIFIED" if resolved is not None else "UNSUPPORTED",
                    "kind": "uobject", "index": offset, "object": resolved,
                }
            else:
                decoded_value = _niagara_scalar(parameter_data, offset, type_info.get("object"))
                decoded_size = decoded_value.get("size")
                if isinstance(decoded_size, int) and decoded_value.get("status") == "VERIFIED":
                    spans.append((offset, offset + decoded_size, name if isinstance(name, str) else None))
        parameters.append({
            "status": decoded_value.get("status"), "name": name,
            "type": type_info, "offset": offset, "value": decoded_value,
        })
    duplicate_names = len(names) - len(set(names))
    overlaps = 0
    for previous, current in zip(sorted(spans), sorted(spans)[1:]):
        if current[0] < previous[1]:
            overlaps += 1
    all_verified = all(item.get("status") == "VERIFIED" for item in parameters)
    status = "VERIFIED" if all_verified and not duplicate_names and not overlaps and not invalid_offsets else "UNSUPPORTED"
    return {
        "status": status, "parameters": parameters, "parameter_count": len(parameters),
        "parameter_data": {
            "size": len(parameter_data), "sha256": hashlib.sha256(parameter_data).hexdigest(),
        },
        "data_interfaces": data_interfaces, "uobjects": uobjects,
        "integrity": {
            "duplicate_name_count": duplicate_names, "overlap_count": overlaps,
            "invalid_offset_count": invalid_offsets,
        },
        "debug_name": fields.get("DebugName"),
    }


def _niagara_system_summary(package: UnrealPackage) -> list[dict[str, Any]]:
    parser = PropertyParser(package)
    systems: list[dict[str, Any]] = []
    for index, export in enumerate(package.exports, 1):
        if export.class_name != "NiagaraSystem" or export.payload_availability != "available":
            continue
        decoded = parser.parse_export(index)
        values = _decoded_values({"properties": decoded.get("properties", [])})
        emitters = []
        emitter_value = values.get("EmitterHandles")
        if isinstance(emitter_value, dict):
            for item in emitter_value.get("items", []):
                fields = _decoded_values(item)
                versioned = _decoded_values(fields.get("VersionedInstance"))
                emitters.append({
                    "name": fields.get("Name"), "id": fields.get("Id"), "id_name": fields.get("IdName"),
                    "enabled": fields.get("bIsEnabled"), "mode": fields.get("EmitterMode"),
                    "emitter": versioned.get("Emitter"), "version": versioned.get("Version"),
                    "stateless_emitter": fields.get("StatelessEmitter"),
                })
        exposed = _decoded_values(values.get("ExposedParameters"))
        parameter_bytes = bytes(exposed.get("ParameterData", {}).get("items", [])) if isinstance(exposed.get("ParameterData"), dict) else b""
        data_interfaces = exposed.get("DataInterfaces", {}).get("items", []) if isinstance(exposed.get("DataInterfaces"), dict) else []
        uobjects = exposed.get("UObjects", {}).get("items", []) if isinstance(exposed.get("UObjects"), dict) else []
        parameters = []
        sorted_offsets = exposed.get("SortedParameterOffsets")
        if isinstance(sorted_offsets, dict):
            for variable in sorted_offsets.get("items", []):
                type_info = _niagara_type(variable)
                offset = int(variable.get("offset", -1))
                type_object = type_info.get("object") or ""
                if "NiagaraDataInterface" in type_object:
                    resolved = data_interfaces[offset] if 0 <= offset < len(data_interfaces) else None
                    value = {
                        "status": "VERIFIED" if resolved is not None else "UNSUPPORTED",
                        "kind": "data_interface", "index": offset, "object": resolved,
                    }
                elif type_info.get("underlying_type") == 1:
                    resolved = uobjects[offset] if 0 <= offset < len(uobjects) else None
                    value = {
                        "status": "VERIFIED" if resolved is not None else "UNSUPPORTED",
                        "kind": "uobject", "index": offset, "object": resolved,
                    }
                else:
                    value = _niagara_scalar(parameter_bytes, offset, type_object)
                parameters.append({
                    "name": variable.get("name"), "type": type_info, "offset": offset, "value": value,
                })
        redirects = []
        redirect_map = exposed.get("UserParameterRedirects")
        if isinstance(redirect_map, dict):
            redirects = [
                {"source": item["key"].get("name"), "target": item["value"].get("name")}
                for item in redirect_map.get("entries", [])
            ]
        systems.append({
            "export_index": index, "object": package.object_path(index),
            "emitters": emitters, "emitter_count": len(emitters),
            "exposed_parameters": parameters, "exposed_parameter_count": len(parameters),
            "user_parameter_redirects": redirects,
            "parameter_data": {
                "size": len(parameter_bytes), "sha256": hashlib.sha256(parameter_bytes).hexdigest(),
                "data_interface_count": len(data_interfaces), "uobject_count": len(uobjects),
            },
        })
    return systems


def _property_record(decoded: dict[str, Any], name: str) -> dict[str, Any] | None:
    return next((item for item in decoded.get("properties", []) if item.get("name") == name), None)


def _niagara_parameter_collections(package: UnrealPackage) -> list[dict[str, Any]]:
    parser = PropertyParser(package)
    decoded_by_index: dict[int, dict[str, Any]] = {}
    for index, export in enumerate(package.exports, 1):
        if export.class_name in ("NiagaraParameterCollection", "NiagaraParameterCollectionInstance"):
            decoded_by_index[index] = parser.parse_export(index)
    result: list[dict[str, Any]] = []
    for index, export in enumerate(package.exports, 1):
        if export.class_name != "NiagaraParameterCollection":
            continue
        decoded = decoded_by_index[index]
        values = _decoded_values(decoded)
        namespace = values.get("Namespace")
        parameter_items = _array_items(values.get("Parameters"))
        default_ref = values.get("DefaultInstance")
        default_index = default_ref.get("package_index") if isinstance(default_ref, dict) else None
        instance_decoded = decoded_by_index.get(default_index) if isinstance(default_index, int) else None
        instance_values = _decoded_values(instance_decoded) if instance_decoded else {}
        store = _niagara_parameter_store(instance_values.get("ParameterStorage"))
        store_by_name = {
            item.get("name"): item for item in store.get("parameters", [])
            if isinstance(item.get("name"), str)
        }
        parameters: list[dict[str, Any]] = []
        cross_mismatches = 0
        names: list[str | None] = []
        expected_prefix = f"NPC.{namespace}." if isinstance(namespace, str) else None
        for variable in parameter_items:
            summary = _niagara_variable_summary(variable)
            name = summary.get("name")
            names.append(name if isinstance(name, str) else None)
            stored = store_by_name.get(name)
            direct_raw = (summary.get("default") or {}).get("raw_hex")
            stored_raw = ((stored or {}).get("value") or {}).get("raw_hex")
            type_match = bool(stored) and stored.get("type") == summary.get("type")
            value_match = bool(stored) and direct_raw == stored_raw
            namespace_match = bool(expected_prefix and isinstance(name, str) and name.startswith(expected_prefix))
            cross_verified = type_match and value_match and namespace_match
            if not cross_verified:
                cross_mismatches += 1
            parameters.append({
                **summary,
                "friendly_name": name[len(expected_prefix):] if namespace_match and expected_prefix else None,
                "store_offset": stored.get("offset") if stored else None,
                "cross_validation": {
                    "present_in_default_store": stored is not None,
                    "type_match": type_match, "value_bytes_match": value_match,
                    "namespace_match": namespace_match, "status": "VERIFIED" if cross_verified else "UNSUPPORTED",
                },
            })
        duplicate_names = len(names) - len(set(names))
        instance_collection = instance_values.get("Collection")
        instance_parent_match = (
            isinstance(instance_collection, dict)
            and instance_collection.get("package_index") == index
        )
        count_match = len(parameter_items) == store.get("parameter_count")
        collection_status = "VERIFIED" if all((
            isinstance(namespace, str),
            store.get("status") == "VERIFIED",
            instance_parent_match,
            count_match,
            not duplicate_names,
            not cross_mismatches,
            all(item.get("status") == "VERIFIED" for item in parameters),
        )) else "UNSUPPORTED"
        parameters_record = _property_record(decoded, "Parameters") or {}
        storage_record = _property_record(instance_decoded or {}, "ParameterStorage") or {}
        result.append({
            "status": collection_status,
            "export_index": index, "object": package.object_path(index),
            "namespace": namespace, "compile_id": values.get("CompileId"),
            "source_material_collection": values.get("SourceMaterialCollection"),
            "default_instance": default_ref,
            "parameters": parameters, "parameter_count": len(parameters),
            "default_parameter_store": store,
            "integrity": {
                "default_instance_parent_match": instance_parent_match,
                "parameter_count_match": count_match,
                "duplicate_name_count": duplicate_names,
                "cross_validation_mismatch_count": cross_mismatches,
            },
            "provenance": {
                "parameters_property_sha256": (parameters_record.get("raw") or {}).get("sha256"),
                "parameter_storage_property_sha256": (storage_record.get("raw") or {}).get("sha256"),
                "basis": "UNiagaraParameterCollection reflected fields and FNiagaraParameterStore offsets",
            },
        })
    return result


def _niagara_text_source(value: Any) -> str | None:
    if not isinstance(value, dict):
        return None
    source = value.get("source")
    return source if isinstance(source, str) else None


def _niagara_compile_hash(value: Any) -> dict[str, Any] | None:
    fields = _decoded_values(value)
    items = _array_items(fields.get("DataHash"))
    if not items:
        return None
    if any(not isinstance(item, int) or not 0 <= item <= 255 for item in items):
        return {"status": "UNSUPPORTED", "reason": "compile hash is not a byte array"}
    data = bytes(items)
    if len(data) != 20:
        return {
            "status": "UNSUPPORTED", "size": len(data), "hex": data.hex(),
            "reason": "Niagara compile hash is not the 20-byte FSHA1 digest layout",
        }
    return {
        "status": "VERIFIED", "size": len(data), "hex": data.hex(),
        "sha256": hashlib.sha256(data).hexdigest(),
    }


def _niagara_vm_bytecode(value: Any) -> dict[str, Any] | None:
    vm_fields = _decoded_values(value)
    bytecode_fields = _decoded_values(vm_fields.get("ByteCode"))
    items = _array_items(bytecode_fields.get("Data"))
    if not items and "ByteCode" not in vm_fields:
        return None
    if any(not isinstance(item, int) or not 0 <= item <= 255 for item in items):
        return {"status": "UNSUPPORTED", "reason": "VM bytecode is not a byte array"}
    data = bytes(items)
    return {
        "status": "VERIFIED", "size": len(data), "hex": data.hex(),
        "sha256": hashlib.sha256(data).hexdigest(),
        "last_compile_status": vm_fields.get("LastCompileStatus"),
        "num_temp_registers": vm_fields.get("NumTempRegisters"),
        "stat_scopes": vm_fields.get("StatScopes"),
    }


def _niagara_script_version(item: Any) -> dict[str, Any]:
    fields = _decoded_values(item)
    version = _decoded_values(fields.get("Version"))
    return {
        "version": {
            "major": version.get("MajorVersion"), "minor": version.get("MinorVersion"),
            "guid": version.get("VersionGuid"),
            "visible": version.get("bIsVisibleInVersionSelector"),
        },
        "change_description": _niagara_text_source(fields.get("VersionChangeDescription")),
        "module_usage_bitmask": fields.get("ModuleUsageBitmask"),
        "category": _niagara_text_source(fields.get("Category")),
        "description": _niagara_text_source(fields.get("Description")),
        "keywords": _niagara_text_source(fields.get("Keywords")),
        "suggested": fields.get("bSuggested"), "deprecated": fields.get("bDeprecated"),
        "deprecation_message": _niagara_text_source(fields.get("DeprecationMessage")),
        "experimental": fields.get("bExperimental"),
        "experimental_message": _niagara_text_source(fields.get("ExperimentalMessage")),
        "provided_dependencies": fields.get("ProvidedDependencies"),
        "required_dependencies": fields.get("RequiredDependencies"),
        "library_visibility": fields.get("LibraryVisibility"),
        "source": fields.get("Source"),
    }


def _niagara_scripts(package: UnrealPackage, graph_contract: dict[str, Any]) -> list[dict[str, Any]]:
    parser = PropertyParser(package)
    decoded_by_index: dict[int, dict[str, Any]] = {}
    for index, export in enumerate(package.exports, 1):
        if export.class_name in (
            "NiagaraScript", "NiagaraScriptSource", "NiagaraGraph", "NiagaraScriptVariable",
        ):
            decoded_by_index[index] = parser.parse_export(index)
    graphs_by_path = {item.get("graph"): item for item in graph_contract.get("graphs", [])}
    scripts: list[dict[str, Any]] = []
    for index, export in enumerate(package.exports, 1):
        if export.class_name != "NiagaraScript":
            continue
        decoded = decoded_by_index[index]
        values = _decoded_values(decoded)
        source_index = next((
            candidate for candidate, source_export in enumerate(package.exports, 1)
            if source_export.class_name == "NiagaraScriptSource" and source_export.outer_index == index
        ), None)
        source_values = _decoded_values(decoded_by_index.get(source_index)) if source_index else {}
        graph_ref = source_values.get("NodeGraph")
        graph_index = graph_ref.get("package_index") if isinstance(graph_ref, dict) else None
        graph_path = graph_ref.get("object") if isinstance(graph_ref, dict) else None
        graph = graphs_by_path.get(graph_path)
        versions = [_niagara_script_version(item) for item in _array_items(values.get("VersionData"))]
        rapid_iteration = _niagara_parameter_store(values.get("RapidIterationParameters"))
        vm_id = _decoded_values(values.get("CachedScriptVMId"))
        vm_bytecode = _niagara_vm_bytecode(values.get("CachedScriptVM"))
        script_variables = []
        for variable_index, variable_export in enumerate(package.exports, 1):
            if variable_export.class_name != "NiagaraScriptVariable" or variable_export.outer_index != graph_index:
                continue
            variable_decoded = decoded_by_index[variable_index]
            variable_values = _decoded_values(variable_decoded)
            script_variables.append({
                "export_index": variable_index, "object": package.object_path(variable_index),
                "variable": _niagara_variable_summary(variable_values.get("Variable")),
                "metadata": variable_values.get("Metadata"),
                "default_mode": variable_values.get("DefaultMode"),
                "default_binding": variable_values.get("DefaultBinding"),
                "default_value_variant": variable_values.get("DefaultValueVariant"),
                "subscribed_to_parameter_definitions": variable_values.get("bSubscribedToParameterDefinitions"),
                "change_id": variable_values.get("ChangeId"),
            })
        function_calls = []
        inputs = []
        outputs = []
        operations = []
        custom_hlsl = []
        for node in (graph or {}).get("nodes", []):
            properties = node.get("properties", {})
            if node.get("class") == "NiagaraNodeFunctionCall":
                function_calls.append({
                    "node": node.get("object"), "display_name": properties.get("FunctionDisplayName"),
                    "function_script": properties.get("FunctionScript"),
                    "selected_script_version": properties.get("SelectedScriptVersion"),
                    "signature": properties.get("Signature"),
                    "function_specifiers": properties.get("FunctionSpecifiers"),
                })
            elif node.get("class") == "NiagaraNodeInput":
                inputs.append({"node": node.get("object"), "variable": _niagara_variable_summary(properties.get("Input"))})
            elif node.get("class") == "NiagaraNodeOutput":
                outputs.append({
                    "node": node.get("object"), "usage": properties.get("ScriptType"),
                    "variables": [_niagara_variable_summary(item) for item in _array_items(properties.get("Outputs"))],
                })
            elif node.get("class") == "NiagaraNodeOp":
                operations.append({"node": node.get("object"), "op_name": properties.get("OpName")})
            elif node.get("class") == "NiagaraNodeCustomHlsl":
                custom_hlsl.append({
                    "node": node.get("object"), "source": properties.get("CustomHlsl"),
                    "signature": properties.get("Signature"),
                })
        status = "VERIFIED" if all((
            graph is not None,
            graph_contract.get("status") == "VERIFIED",
            rapid_iteration.get("status") == "VERIFIED",
            bool(versions),
            all(item["variable"].get("status") in ("VERIFIED", "RAW_VERIFIED") for item in script_variables),
        )) else "RAW_VERIFIED"
        scripts.append({
            "status": status, "export_index": index, "object": package.object_path(index),
            "usage": values.get("Usage"), "exposed_version": values.get("ExposedVersion"),
            "versions": versions, "version_count": len(versions),
            "source": {"export_index": source_index, "node_graph": graph_ref},
            "graph": {
                "object": graph_path, "node_count": len((graph or {}).get("nodes", [])),
                "edge_count": len((graph or {}).get("edges", [])),
            },
            "inputs": inputs, "outputs": outputs, "operations": operations,
            "function_calls": function_calls, "custom_hlsl": custom_hlsl,
            "script_variables": script_variables,
            "rapid_iteration_parameters": rapid_iteration,
            "cached_parameter_collections": _array_items(values.get("CachedParameterCollectionReferences")),
            "vm": {
                "compiler_version_id": vm_id.get("CompilerVersionID"),
                "script_usage": vm_id.get("ScriptUsageType"),
                "additional_defines": _array_items(vm_id.get("AdditionalDefines")),
                "base_compile_hash": _niagara_compile_hash(vm_id.get("BaseScriptCompileHash")),
                "referenced_compile_hashes": vm_id.get("ReferencedCompileHashes"),
                "bytecode": vm_bytecode,
            },
            "provenance": {
                "script_export_sha256": _export_payload_sha256(package, index),
                "basis": "UNiagaraScript reflected fields plus verified EdGraph native pin stream",
            },
        })
    return scripts


def export_blueprint_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        exports = _exports(
            package,
            lambda _i, cls, path: _is_blueprint_contract_export(cls, path),
        )
        graph = BlueprintGraphDecoder(package).decode(include_niagara=False)
        bytecode = StructScriptDecoder(package).decode_functions()
        return {
            "schema": "ueassettool.blueprint-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports, "graph": graph, "bytecode": bytecode,
            "dependencies": _dependencies(package),
            "semantics": "Serialized editor graph/default contract; not decompiled C++.",
        }


def export_niagara_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        exports = _exports(package, lambda _i, cls, obj: "Niagara" in cls or cls.startswith("EdGraph") or "Niagara" in obj)
        graph = BlueprintGraphDecoder(package).decode(include_niagara=True)
        systems = _niagara_system_summary(package)
        scripts = _niagara_scripts(package, graph)
        collections = _niagara_parameter_collections(package)
        return {
            "schema": "ueassettool.niagara-contract/v2", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports,
            "systems": systems, "scripts": scripts,
            "parameter_collections": collections, "graph": graph,
            "dependencies": _dependencies(package),
            "coverage": {
                "system_count": len(systems), "script_count": len(scripts),
                "parameter_collection_count": len(collections),
                "verified_script_count": sum(item.get("status") == "VERIFIED" for item in scripts),
                "verified_parameter_collection_count": sum(item.get("status") == "VERIFIED" for item in collections),
            },
            "semantics": "Serialized parameters, VM bytecode, module nodes/pins/links, data interfaces and dependencies; unsupported native bytes retain provenance.",
        }


def export_metasound_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        exports = _exports(package, lambda _i, cls, obj: "metasound" in cls.lower() or "metasound" in obj.lower())
        audio_references = [
            {
                "import_index": -index,
                "object": package.object_path(-index),
                "class": item.class_name.display,
                "embedded": False,
                "availability": "referenced-asset-not-contained-in-this-package",
            }
            for index, item in enumerate(package.imports, 1)
            if item.class_name.display in ("SoundWave", "MetaSoundSource")
        ]
        return {
            "schema": "ueassettool.metasound-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports, "audio_references": audio_references,
            "dependencies": _dependencies(package),
            "semantics": "Serialized MetaSound properties/references; unknown frontend native data remains RAW_VERIFIED.",
        }


def verify_package(path: str | Path) -> dict[str, Any]:
    source = Path(path)
    with UnrealPackage(source) as package:
        inspection = package.inspect_dict()
        missing = int(inspection["integrity"]["missing_exports"])
        companions = []
        for suffix in (".uexp", ".ubulk", ".uptnl", ".usmap"):
            candidate = source.with_suffix(suffix)
            companions.append({
                "path": str(candidate), "present": candidate.is_file(),
                "size": candidate.stat().st_size if candidate.is_file() else None,
                "sha256": _file_sha256(candidate) if candidate.is_file() else None,
            })
        trailer: dict[str, Any] | None = None
        trailer_error: str | None = None
        if package.summary.payload_toc_offset >= 0:
            try:
                trailer = read_package_trailer(source, offset=package.summary.payload_toc_offset).to_dict()
            except UEAssetError as exc:
                trailer_error = str(exc)
        status = "TRUNCATED" if missing else "UNSUPPORTED" if trailer_error else "VERIFIED"
        return {
            "schema": "ueassettool.verify/v1", "status": status,
            "source": inspection["source"], "engine": _source(package),
            "integrity": inspection["integrity"], "issues": inspection["issues"],
            "companions": companions, "package_trailer": trailer,
            "package_trailer_error": trailer_error,
        }


def export_auto_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        classes = {item.class_name or "" for item in package.exports}
    if any("Niagara" in value for value in classes):
        return export_niagara_contract(path)
    if any("MetaSound" in value or "Metasound" in value for value in classes):
        return export_metasound_contract(path)
    if any(
        value in ("Blueprint", "BlueprintGeneratedClass")
        or value.endswith("Blueprint")
        or value.endswith("BlueprintGeneratedClass")
        for value in classes
    ):
        return export_blueprint_contract(path)
    if any(value in MATERIAL_ROOT_CLASSES or value.startswith("MaterialExpression") for value in classes):
        return export_material_contract(path)
    if any(value in CURVE_CLASSES for value in classes):
        return export_curve_contract(path)
    with UnrealPackage(path) as package:
        return {
            "schema": "ueassettool.asset-contract/v1", "status": "RAW_VERIFIED",
            "source": _source(package), "exports": _exports(package, lambda *_: True),
            "dependencies": _dependencies(package),
        }
