from __future__ import annotations

import hashlib
import struct
from pathlib import Path
from typing import Any, Callable

from .blueprint import BlueprintGraphDecoder
from .bytecode import StructScriptDecoder
from .errors import UEAssetError
from .package import UnrealPackage
from .properties import PropertyParser
from .trailer import read_package_trailer


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
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
    }
    if short not in formats:
        return {"status": "RAW_VERIFIED", "reason": f"no scalar decoder for {short or '<unknown>'}"}
    fmt, kind = formats[short]
    size = struct.calcsize(fmt)
    if offset < 0 or offset + size > len(data):
        return {"status": "UNSUPPORTED", "reason": f"offset {offset}+{size} exceeds ParameterData {len(data)}"}
    raw = data[offset:offset + size]
    values = struct.unpack(fmt, raw)
    value: Any = values[0] if len(values) == 1 else list(values)
    if kind == "bool32":
        value = bool(value)
    return {"status": "VERIFIED", "kind": kind, "value": value, "raw_hex": raw.hex()}


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


def export_blueprint_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        classes = ("Blueprint", "BlueprintGeneratedClass", "EdGraph", "EdGraphSchema", "Function", "TimelineTemplate", "SimpleConstructionScript", "SCS_Node")
        exports = _exports(package, lambda _i, cls, _p: cls.startswith("K2Node_") or cls in classes)
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
        return {
            "schema": "ueassettool.niagara-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports, "systems": systems, "graph": graph,
            "dependencies": _dependencies(package),
            "semantics": "Serialized parameters, curves, data interfaces and editor links; unsupported native/script bytes retain provenance.",
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
    if any(value in ("Blueprint", "BlueprintGeneratedClass") for value in classes):
        return export_blueprint_contract(path)
    with UnrealPackage(path) as package:
        return {
            "schema": "ueassettool.asset-contract/v1", "status": "RAW_VERIFIED",
            "source": _source(package), "exports": _exports(package, lambda *_: True),
            "dependencies": _dependencies(package),
        }
