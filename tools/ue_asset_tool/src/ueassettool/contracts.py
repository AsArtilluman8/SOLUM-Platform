from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any, Callable

from .blueprint import BlueprintGraphDecoder
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


def export_blueprint_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        classes = ("Blueprint", "BlueprintGeneratedClass", "EdGraph", "EdGraphSchema", "Function", "TimelineTemplate", "SimpleConstructionScript", "SCS_Node")
        exports = _exports(package, lambda _i, cls, _p: cls.startswith("K2Node_") or cls in classes)
        graph = BlueprintGraphDecoder(package).decode(include_niagara=False)
        return {
            "schema": "ueassettool.blueprint-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports, "graph": graph,
            "dependencies": _dependencies(package),
            "semantics": "Serialized editor graph/default contract; not decompiled C++.",
        }


def export_niagara_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        exports = _exports(package, lambda _i, cls, obj: "Niagara" in cls or cls.startswith("EdGraph") or "Niagara" in obj)
        graph = BlueprintGraphDecoder(package).decode(include_niagara=True)
        return {
            "schema": "ueassettool.niagara-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports, "graph": graph,
            "dependencies": _dependencies(package),
            "semantics": "Serialized parameters, curves, data interfaces and editor links; unsupported native/script bytes retain provenance.",
        }


def export_metasound_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        exports = _exports(package, lambda _i, cls, obj: "metasound" in cls.lower() or "metasound" in obj.lower())
        return {
            "schema": "ueassettool.metasound-contract/v1", "status": _aggregate_status(exports),
            "source": _source(package), "exports": exports,
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
