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
    source_root: str | Path | None = None,
    source_manifest: str | Path | None = None,
    map_path: str | Path | None = None,
    dataset: str | Path,
    incremental: bool = False,
) -> dict[str, Any]:
    dataset_path = Path(dataset)
    inputs: list[Path] = []
    if source_root:
        inputs.append(Path(source_root))
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
    if not value.startswith("/"):
        return None
    package = value.split(":", 1)[0].split(".", 1)[0]
    return package if package.startswith(("/Game/", "/Engine/", "/Script/")) else None


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
        for path in self.package_files():
            try:
                with UnrealPackage(path) as package:
                    name = package.summary.package_name
                    if not name or not name.startswith("/"):
                        raise ValueError("package summary has no canonical package name")
                    if name in self.packages and self.packages[name]["path"] != str(path):
                        self.errors.append({
                            "status": "AMBIGUOUS_PACKAGE", "package": name,
                            "paths": [self.packages[name]["path"], str(path)],
                        })
                        continue
                    record = {
                        "package_name": name,
                        "path": str(path),
                        "size": path.stat().st_size,
                        "sha256": package.sha256,
                        "file_version_ue4": package.summary.file_version_ue4,
                        "file_version_ue5": package.summary.file_version_ue5,
                        "engine": (
                            package.summary.saved_by_engine_version.display
                            if package.summary.saved_by_engine_version else None
                        ),
                        "integrity": {
                            "exports": len(package.exports),
                            "missing_exports": sum(
                                item.payload_availability not in ("available", "empty")
                                for item in package.exports
                            ),
                        },
                    }
                    self.packages[name] = record
                    self._cache_record(record)
            except (UEAssetError, OSError, ValueError) as exc:
                self.errors.append({
                    "status": "PACKAGE_INDEX_ERROR", "path": str(path),
                    "reason": f"{type(exc).__name__}: {exc}",
                })
        return {
            "schema": "ueassettool.package-index/v1",
            "roots": [str(path) for path in self.roots],
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
            except (UEAssetError, OSError) as exc:
                missing.append({
                    "source_package": source_name, "status": "DECODE_ERROR",
                    "reason": f"{type(exc).__name__}: {exc}",
                })
                continue
            for reference in references:
                target_name = reference["target_package"]
                target = self.packages.get(target_name)
                if target:
                    status = "RESOLVED"
                    resolved = target["path"]
                    target_hash = target["sha256"]
                    if target_name not in visited:
                        queue.append(target_name)
                elif target_name.startswith("/Script/"):
                    status = "EXTERNAL_SCRIPT_PACKAGE"
                    resolved = None
                    target_hash = None
                else:
                    status = "MISSING_PACKAGE"
                    resolved = None
                    target_hash = None
                edge = {
                    "source_object": source_name,
                    "source_package": source_name,
                    "target_object_path": reference["target_object"],
                    "target_package": target_name,
                    "reference_type": reference["reference_type"],
                    "resolved_local_file": resolved,
                    "source_sha256": source_record["sha256"],
                    "target_sha256": target_hash,
                    "required_sidecars": [],
                    "terminal_status": status,
                    "reason": None if status == "RESOLVED" else (
                        "native script package is not a supplied content package"
                        if status == "EXTERNAL_SCRIPT_PACKAGE"
                        else "no exact package-name match in indexed local roots"
                    ),
                }
                edges.append(edge)
                if status == "MISSING_PACKAGE":
                    missing.append(edge)
        return {
            "schema": DEPENDENCY_SCHEMA,
            "root_package": start_name,
            "root_path": str(start_path),
            "root_sha256": start_hash,
            "visited_package_count": len(visited),
            "edge_count": len(edges),
            "resolved_count": sum(item["terminal_status"] == "RESOLVED" for item in edges),
            "missing_count": sum(item["terminal_status"] == "MISSING_PACKAGE" for item in edges),
            "edges": edges,
            "missing": missing,
        }
