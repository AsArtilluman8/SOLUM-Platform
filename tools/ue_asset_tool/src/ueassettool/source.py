from __future__ import annotations

import hashlib
import json
import zipfile
from collections import deque
from pathlib import Path, PurePosixPath
from typing import Any, Iterable

from .dataset import sha256_file, write_json
from .errors import UEAssetError
from .package import UnrealPackage
from .properties import PropertyParser


SOURCE_SCHEMA = "ueassettool.source-manifest/v1"
DEPENDENCY_SCHEMA = "ueassettool.dependency-closure/v1"
PACKAGE_SUFFIXES = {".uasset", ".umap"}
SIDECAR_SUFFIXES = (".uexp", ".ubulk", ".uptnl", ".upayload", ".usmap")


def _safe_zip_member(name: str) -> PurePosixPath:
    value = PurePosixPath(name)
    if value.is_absolute() or not value.parts or ".." in value.parts:
        raise ValueError(f"unsafe ZIP member {name!r}")
    return value


def _atomic_bytes(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_bytes(data)
    temporary.replace(path)


def prepare_zip_source(archive: Path, dataset: Path, *, incremental: bool) -> dict[str, Any]:
    archive_hash = sha256_file(archive)
    root = dataset / "_source_cache" / archive_hash
    files: list[dict[str, Any]] = []
    with zipfile.ZipFile(archive) as source:
        bad = source.testzip()
        if bad:
            raise ValueError(f"ZIP CRC failure in {bad}")
        for item in source.infolist():
            if item.is_dir():
                continue
            relative = _safe_zip_member(item.filename)
            if relative.suffix.lower() not in PACKAGE_SUFFIXES | set(SIDECAR_SUFFIXES):
                continue
            raw = source.read(item)
            digest = hashlib.sha256(raw).hexdigest()
            target = root.joinpath(*relative.parts)
            reused = (
                incremental and target.is_file() and target.stat().st_size == len(raw)
                and sha256_file(target) == digest
            )
            if not reused:
                _atomic_bytes(target, raw)
            files.append({
                "archive_member": item.filename,
                "path": str(target),
                "size": len(raw),
                "sha256": digest,
                "crc32": f"{item.CRC:08x}",
                "cache_status": "REUSED" if reused else "WRITTEN",
            })
    return {
        "kind": "zip", "archive": str(archive), "archive_sha256": archive_hash,
        "root": str(root), "files": files,
    }


def load_source_manifest(path: Path) -> list[Path]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(document, list):
        values = document
    elif isinstance(document, dict):
        values = document.get("roots", []) + document.get("files", [])
    else:
        raise ValueError("source manifest must be an array or object")
    result: list[Path] = []
    for item in values:
        value = item.get("path") if isinstance(item, dict) else item
        if not isinstance(value, str) or not value:
            raise ValueError("source manifest path is invalid")
        candidate = Path(value)
        if not candidate.is_absolute():
            candidate = path.parent / candidate
        result.append(candidate)
    return result


def prepare_source_roots(
    *,
    source_root: str | Path | Iterable[str | Path] | None = None,
    source_manifest: str | Path | None = None,
    map_path: str | Path | None = None,
    dataset: str | Path,
    incremental: bool = False,
) -> dict[str, Any]:
    dataset_path = Path(dataset)
    inputs: list[Path] = []
    if source_root:
        values = (source_root,) if isinstance(source_root, (str, Path)) else source_root
        inputs.extend(Path(value) for value in values)
    if source_manifest:
        inputs.extend(load_source_manifest(Path(source_manifest)))
    if map_path:
        inputs.append(Path(map_path))
    if not inputs:
        raise ValueError("one of --source-root, --source-manifest or --map is required")

    roots: list[Path] = []
    records: list[dict[str, Any]] = []
    direct_files: list[Path] = []
    for value in inputs:
        if value.is_file() and value.suffix.lower() == ".zip":
            record = prepare_zip_source(value, dataset_path, incremental=incremental)
            roots.append(Path(record["root"]))
            records.append(record)
        elif value.is_dir():
            roots.append(value)
            records.append({"kind": "directory", "root": str(value)})
        elif value.is_file() and value.suffix.lower() in PACKAGE_SUFFIXES:
            direct_files.append(value)
            roots.append(value.parent)
            records.append({
                "kind": "package", "path": str(value), "size": value.stat().st_size,
                "sha256": sha256_file(value),
            })
        else:
            raise FileNotFoundError(value)
    unique_roots = list(dict.fromkeys(path.resolve() for path in roots))
    manifest = {
        "schema": SOURCE_SCHEMA,
        "inputs": records,
        "roots": [str(path) for path in unique_roots],
        "direct_files": [str(path.resolve()) for path in direct_files],
        "incremental": incremental,
    }
    write_json(dataset_path / "source_manifest.json", manifest)
    return manifest


def _package_name_from_object_path(value: str) -> str | None:
    value = value.strip("'\"")
    if "'" in value and value.endswith("'"):
        value = value.split("'", 1)[1][:-1]
    if not value.startswith("/"):
        return None
    package = value.split(":", 1)[0].split(".", 1)[0]
    return package if package.startswith(("/Game/", "/Engine/", "/Script/")) else None


def normalize_object_reference(value: str) -> dict[str, Any]:
    """Separate a UE object/subobject/class path without inventing a package."""
    raw = value
    value = value.strip("'\"")
    if "'" in value and value.endswith("'"):
        value = value.split("'", 1)[1][:-1]
    package = _package_name_from_object_path(value)
    before_subobject, _, subobject = value.partition(":")
    object_name = before_subobject.split(".", 1)[1] if "." in before_subobject else None
    kind = "PACKAGE_OR_OBJECT"
    if subobject:
        kind = "SUBOBJECT"
    package_leaf = package.rsplit("/", 1)[-1] if package else None
    generated = bool(object_name and (
        object_name.startswith("Default__") or object_name == f"{package_leaf}_C"
    ))
    if generated and not subobject:
        kind = "GENERATED_CLASS_OR_CDO"
    return {"raw": raw, "package": package, "object": object_name,
            "subobject": subobject or None, "generated_class_or_cdo": generated, "kind": kind}


class PackageIndex:
    def __init__(self, roots: Iterable[str | Path], *, cache_root: str | Path | None = None):
        self.roots = [Path(value) for value in roots]
        self.cache_root = Path(cache_root) if cache_root else None
        self.packages: dict[str, dict[str, Any]] = {}
        self.errors: list[dict[str, Any]] = []

    def package_files(self) -> list[Path]:
        found: dict[Path, None] = {}
        for root in self.roots:
            if root.is_file() and root.suffix.lower() in PACKAGE_SUFFIXES:
                found[root.resolve()] = None
            elif root.is_dir():
                for suffix in PACKAGE_SUFFIXES:
                    for path in root.rglob(f"*{suffix}"):
                        found[path.resolve()] = None
        return sorted(found)

    def _cache_record(self, record: dict[str, Any]) -> None:
        if self.cache_root is None:
            return
        digest = str(record["sha256"])
        write_json(self.cache_root / "sha256" / f"{digest}.json", {
            "schema": "ueassettool.content-addressed-cache/v1",
            "sha256": digest,
            "size": record["size"],
            "source_path": record["path"],
            "package_name": record["package_name"],
            "content_copied": False,
        })

    def build(self) -> dict[str, Any]:
        files = self.package_files()
        deferred: list[tuple[Path, dict[str, Any]]] = []
        mounts: dict[Path, set[str]] = {root.resolve(): set() for root in self.roots if root.is_dir()}
        for path in files:
            try:
                with UnrealPackage(path) as package:
                    name = package.summary.package_name
                    base = {
                        "path": str(path), "size": path.stat().st_size, "sha256": package.sha256,
                        "file_version_ue4": package.summary.file_version_ue4,
                        "file_version_ue5": package.summary.file_version_ue5,
                        "engine": package.summary.saved_by_engine_version.display if package.summary.saved_by_engine_version else None,
                        "asset_classes": sorted({item.class_name for item in package.exports if item.is_asset and item.class_name}),
                        "integrity": {"exports": len(package.exports), "missing_exports": sum(
                            item.payload_availability not in ("available", "empty") for item in package.exports)},
                    }
                    if not name or not name.startswith("/"):
                        deferred.append((path, base))
                        continue
                    for root in mounts:
                        if path.is_relative_to(root):
                            suffix = "/" + path.relative_to(root).with_suffix("").as_posix()
                            if name.endswith(suffix): mounts[root].add(name[:-len(suffix)])
                    if name in self.packages and self.packages[name]["path"] != str(path):
                        self.errors.append({
                            "status": "AMBIGUOUS_PACKAGE", "package": name,
                            "paths": [self.packages[name]["path"], str(path)],
                        })
                        continue
                    record = {"package_name": name, "package_name_basis": "PACKAGE_SUMMARY", **base}
                    self.packages[name] = record
                    self._cache_record(record)
            except (UEAssetError, OSError, ValueError) as exc:
                self.errors.append({
                    "status": "PARSE_ERROR", "terminal_status": "PARSE_ERROR", "path": str(path),
                    "sha256": sha256_file(path), "sidecars": self._sidecars(path),
                    "reason": f"{type(exc).__name__}: {exc}",
                })
        for path, base in deferred:
            roots = [root for root in mounts if path.is_relative_to(root)]
            candidates = []
            for root in roots:
                for mount in mounts[root]:
                    candidates.append(mount + "/" + path.relative_to(root).with_suffix("").as_posix())
            candidates = sorted(set(candidates))
            if len(candidates) != 1:
                self.errors.append({"status": "AMBIGUOUS", "terminal_status": "AMBIGUOUS",
                    "path": str(path), "sha256": base["sha256"], "sidecars": self._sidecars(path),
                    "reason": "source-root mount mapping is not unique", "candidates": candidates})
                continue
            name = candidates[0]
            if name in self.packages:
                self.errors.append({"status": "AMBIGUOUS", "terminal_status": "AMBIGUOUS",
                    "path": str(path), "sha256": base["sha256"], "sidecars": self._sidecars(path),
                    "reason": "derived package name duplicates indexed package", "package": name})
                continue
            record = {"package_name": name, "package_name_basis": "PROVEN_SOURCE_ROOT_MOUNT", **base}
            self.packages[name] = record
            self._cache_record(record)
        return {
            "schema": "ueassettool.package-index/v1",
            "roots": [str(path) for path in self.roots],
            "file_count": len(files),
            "package_count": len(self.packages),
            "packages": sorted(self.packages.values(), key=lambda item: item["package_name"]),
            "errors": self.errors,
        }

    @staticmethod
    def _header_references(package: UnrealPackage) -> list[dict[str, str]]:
        references: list[dict[str, str]] = []
        for index in range(1, len(package.imports) + 1):
            object_path = package.object_path(-index)
            target = _package_name_from_object_path(object_path)
            if target:
                references.append({
                    "target_object": object_path, "target_package": target,
                    "reference_type": "IMPORT_TABLE",
                })
        for item in package.soft_object_paths:
            object_path = str(item.get("object_path", ""))
            target = _package_name_from_object_path(object_path)
            if target:
                references.append({
                    "target_object": object_path, "target_package": target,
                    "reference_type": "SOFT_OBJECT_PATH_TABLE",
                })
        for value in package.soft_package_references:
            target = _package_name_from_object_path(str(value)) or str(value)
            if target.startswith("/"):
                references.append({
                    "target_object": str(value), "target_package": target,
                    "reference_type": "SOFT_PACKAGE_REFERENCE_TABLE",
                })
        unique: dict[tuple[str, str, str], dict[str, str]] = {}
        for item in references:
            unique[(item["target_object"], item["target_package"], item["reference_type"])] = item
        return list(unique.values())

    @staticmethod
    def _property_references(package: UnrealPackage) -> list[dict[str, Any]]:
        """Return only object paths emitted by decoded serialized properties."""
        refs: list[dict[str, Any]] = []
        parser = PropertyParser(package)
        for index in range(1, len(package.exports) + 1):
            try:
                decoded = parser.parse_export(index)
            except UEAssetError:
                continue
            for prop in decoded.get("properties", []):
                if not str(prop.get("decode_status", "")).startswith("decoded"):
                    continue
                stack = [prop.get("value")]
                while stack:
                    value = stack.pop()
                    if isinstance(value, dict):
                        object_path = value.get("object") or value.get("object_path")
                        if isinstance(object_path, str):
                            package_name = _package_name_from_object_path(object_path)
                            if package_name:
                                refs.append({
                                    "source_object": package.object_path(index),
                                    "source_export": index,
                                    "source_property": prop.get("name"),
                                    "source_property_provenance": prop.get("raw"),
                                    "target_object": object_path,
                                    "target_package": package_name,
                                    "reference_type": "DECODED_PROPERTY",
                                })
                        stack.extend(value.values())
                    elif isinstance(value, list):
                        stack.extend(value)
        return refs

    @staticmethod
    def _sidecars(path: Path) -> list[dict[str, Any]]:
        return [{"path": str(path.with_suffix(suffix)), "present": path.with_suffix(suffix).is_file(),
                 "sha256": sha256_file(path.with_suffix(suffix)) if path.with_suffix(suffix).is_file() else None}
                for suffix in SIDECAR_SUFFIXES]

    def _resolve_package(self, package_name: str) -> tuple[dict[str, Any] | None, str | None]:
        target = self.packages.get(package_name)
        if target:
            return target, None
        aliases: list[tuple[str, str]] = []
        if package_name.endswith("_C"):
            aliases.append((package_name[:-2], "GENERATED_CLASS_SUFFIX"))
        if "/Default__" in package_name:
            aliases.append((package_name.replace("/Default__", "/").removesuffix("_C"), "CDO_PATH"))
        matches = [(self.packages[name], basis) for name, basis in aliases if name in self.packages]
        return matches[0] if len(matches) == 1 else (None, None)

    def dependency_closure(self, start: str | Path) -> dict[str, Any]:
        start_path = Path(start)
        with UnrealPackage(start_path) as start_package:
            start_name = str(start_package.summary.package_name)
            start_hash = start_package.sha256
        if not self.packages:
            self.build()
        queue = deque([start_name])
        visited: set[str] = set()
        edges: list[dict[str, Any]] = []
        missing: list[dict[str, Any]] = []
        while queue:
            source_name = queue.popleft()
            if source_name in visited:
                continue
            visited.add(source_name)
            source_record = self.packages.get(source_name)
            if source_record is None:
                if source_name == start_name:
                    source_record = {
                        "path": str(start_path), "sha256": start_hash,
                        "package_name": start_name,
                    }
                else:
                    continue
            try:
                with UnrealPackage(source_record["path"]) as package:
                    references = self._header_references(package)
                    # Step 2 requires decoded actor/component/object edges from the
                    # selected map.  Transitive packages remain table-closed here;
                    # recursively decoding all editor exports is a later contract.
                    if source_name == start_name:
                        references += self._property_references(package)
            except (UEAssetError, OSError) as exc:
                missing.append({
                    "source_package": source_name, "status": "DECODE_ERROR",
                    "reason": f"{type(exc).__name__}: {exc}",
                })
                continue
            for reference in references:
                normalized = normalize_object_reference(reference["target_object"])
                target_name = normalized["package"] or reference["target_package"]
                target, alias_basis = self._resolve_package(target_name)
                if target:
                    status = "REDIRECTOR" if "ObjectRedirector" in target.get("asset_classes", []) else "RESOLVED"
                    resolved = target["path"]
                    target_hash = target["sha256"]
                    sidecars = self._sidecars(Path(resolved))
                    if status == "RESOLVED" and target_name not in visited:
                        queue.append(target_name)
                elif target_name.startswith("/Script/"):
                    status = "EXTERNAL_SCRIPT_PACKAGE"
                    resolved = None
                    target_hash = None
                    sidecars = []
                elif target_name.startswith("/Engine/"):
                    status = "EXTERNAL_ENGINE_PACKAGE"
                    resolved = None
                    target_hash = None
                    sidecars = []
                else:
                    status = "MISSING_PACKAGE"
                    resolved = None
                    target_hash = None
                    sidecars = []
                edge = {
                    "source_object": reference.get("source_object", source_name),
                    "source_export": reference.get("source_export"),
                    "source_property": reference.get("source_property"),
                    "source_property_provenance": reference.get("source_property_provenance"),
                    "source_package": source_name,
                    "target_object_path": reference["target_object"],
                    "target_package": target_name,
                    "target_path_normalization": normalized,
                    "resolution_alias_basis": alias_basis,
                    "reference_type": reference["reference_type"],
                    "resolved_local_file": resolved,
                    "source_sha256": source_record["sha256"],
                    "target_sha256": target_hash,
                    "required_sidecars": sidecars,
                    "terminal_status": status,
                    "reason": None if status == "RESOLVED" else (
                        "indexed target is an ObjectRedirector; destination is not yet proven"
                        if status == "REDIRECTOR" else
                        "native script package is not a supplied content package"
                        if status == "EXTERNAL_SCRIPT_PACKAGE"
                        else "no exact package-name match in indexed local roots"
                    ),
                }
                edges.append(edge)
                if status == "MISSING_PACKAGE":
                    missing.append(edge)
        for item in edges:
            if item["terminal_status"] == "REDIRECTOR":
                item["dependency_classification"] = {"classification": "REDIRECTOR", "basis": "indexed asset class is ObjectRedirector"}
            elif item["terminal_status"] not in ("RESOLVED", "EXTERNAL_SCRIPT_PACKAGE", "EXTERNAL_ENGINE_PACKAGE"):
                item["dependency_classification"] = classify_dependency(item)
        unique = {(item["source_package"], item["source_object"], item["source_property"], item["target_package"], item["target_object_path"], item["reference_type"]): item for item in edges}
        status_counts: dict[str, int] = {}
        type_counts: dict[str, int] = {}
        for item in unique.values():
            status_counts[item["terminal_status"]] = status_counts.get(item["terminal_status"], 0) + 1
            type_counts[item["reference_type"]] = type_counts.get(item["reference_type"], 0) + 1
        return {
            "schema": DEPENDENCY_SCHEMA,
            "root_package": start_name,
            "root_path": str(start_path),
            "root_sha256": start_hash,
            "visited_package_count": len(visited),
            "edge_count": len(edges),
            "unique_edge_count": len(unique),
            "resolved_count": sum(item["terminal_status"] == "RESOLVED" for item in edges),
            "missing_count": sum(item["terminal_status"] == "MISSING_PACKAGE" for item in edges),
            "unique_missing_package_count": len({item["target_package"] for item in unique.values() if item["terminal_status"] == "MISSING_PACKAGE"}),
            "unique_resolved_package_count": len({item["target_package"] for item in unique.values() if item["terminal_status"] == "RESOLVED"}),
            "counts_by_status": status_counts,
            "counts_by_reference_type": type_counts,
            "unique_edges": list(unique.values()),
            "edges": edges,
            "missing": missing,
        }


CLASSIFICATIONS = {
    "REQUIRED_FOR_RENDERED_SCENE", "REQUIRED_FOR_TRANSFORM", "REQUIRED_FOR_MATERIAL",
    "REQUIRED_FOR_LANDSCAPE", "OPTIONAL_RUNTIME", "EDITOR_ONLY", "STALE_SOFT_REFERENCE",
    "SUBOBJECT_ALREADY_CONTAINED", "GENERATED_CLASS_OR_CDO", "REDIRECTOR", "AMBIGUOUS",
    "TRUE_MISSING_INPUT", "PARSE_ERROR",
}


def classify_dependency(edge: dict[str, Any]) -> dict[str, str]:
    target = str(edge.get("target_object_path", ""))
    package = str(edge.get("target_package", ""))
    source = str(edge.get("source_object", ""))
    prop = str(edge.get("source_property") or "")
    ref_type = str(edge.get("reference_type", ""))
    normalized = normalize_object_reference(target)
    if normalized["kind"] == "SUBOBJECT" and package == edge.get("source_package"):
        value, basis = "SUBOBJECT_ALREADY_CONTAINED", "subobject belongs to the serialized source package"
    elif normalized["kind"] == "GENERATED_CLASS_OR_CDO":
        value, basis = "GENERATED_CLASS_OR_CDO", "object name is a generated class or Default__ CDO"
    elif any(token in target or token in source for token in ("/Editor_UI/", "/Textures/Icons/", "/README", "/Tools/")):
        value, basis = "EDITOR_ONLY", "path is in an explicit editor/tool/icon/readme namespace"
    elif any(token in prop for token in ("RootComponent", "AttachParent", "RelativeLocation", "RelativeRotation", "RelativeScale")):
        value, basis = "REQUIRED_FOR_TRANSFORM", "serialized property participates in ownership/transform composition"
    elif "Landscape" in source + target or any(token in prop for token in ("Heightmap", "Weightmap")):
        value, basis = "REQUIRED_FOR_LANDSCAPE", "landscape actor/component dependency"
    elif any(token in prop for token in ("StaticMesh", "SkeletalMesh")) or "/Meshes/" in target:
        value, basis = "REQUIRED_FOR_RENDERED_SCENE", "geometry package is referenced by scene data"
    elif any(token in prop for token in ("Material", "Texture")) or any(token in target for token in ("/Materials/", "/Textures/")):
        value, basis = "REQUIRED_FOR_MATERIAL", "material or texture dependency"
    elif any(token in target for token in ("/Sound/", "/Particles/", "/Weather_Presets/", "/Climate_Presets/")):
        value, basis = "OPTIONAL_RUNTIME", "runtime weather/audio/VFX branch is not required for static verified subset"
    elif ref_type in ("SOFT_OBJECT_PATH_TABLE", "SOFT_PACKAGE_REFERENCE_TABLE"):
        value, basis = "STALE_SOFT_REFERENCE", "unresolved soft reference has no selected-scene hard/property proof"
    else:
        value, basis = "TRUE_MISSING_INPUT", "unresolved hard/property package reference remains after normalization"
    return {"classification": value, "basis": basis}
