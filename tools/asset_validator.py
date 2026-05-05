#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

from transaction_save import atomic_write_json


VALID_TYPES = {
    "texture",
    "mesh",
    "material",
    "character",
    "animation",
    "vfx",
    "scene",
    "sound",
    "video",
    "world",
    "mechanic",
    "diagnostic",
    "shader",
    "font",
    "ui",
    "prefab",
}

VALID_STATES = {
    "valid",
    "invalid",
    "pending",
    "incompatible",
    "missing_file",
}

RECOMMENDED_OPTIONAL_FIELDS = {
    "assetSubType",
    "sourceFormat",
    "runtimeFormat",
}


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return "sha256:" + h.hexdigest()


def load_json(path: Path) -> tuple[Any | None, str | None]:
    try:
        return json.loads(path.read_text(encoding="utf-8")), None
    except Exception as exc:
        return None, str(exc)


def validate_asset(asset_dir: Path) -> dict[str, Any]:
    report: dict[str, Any] = {
        "schema": "solum.asset_validation_report",
        "schemaVersion": 1,
        "assetDir": str(asset_dir),
        "status": "valid",
        "errors": [],
        "warnings": [],
        "files": []
    }

    manifest_path = asset_dir / "asset_manifest.json"
    if not manifest_path.exists():
        report["status"] = "invalid"
        report["errors"].append("missing asset_manifest.json")
        return report

    manifest, err = load_json(manifest_path)
    if err:
        report["status"] = "invalid"
        report["errors"].append(f"manifest json error: {err}")
        return report

    required = [
        "schema",
        "schemaVersion",
        "assetId",
        "assetType",
        "displayName",
        "createdAt",
        "createdBy",
        "fileList",
        "contentHashes",
        "schemaCompatibleWith",
        "validationState",
    ]

    for key in required:
        if key not in manifest:
            report["errors"].append(f"missing required field: {key}")

    for key in RECOMMENDED_OPTIONAL_FIELDS:
        if key not in manifest:
            report["warnings"].append(f"recommended optional field missing: {key}")
        elif not isinstance(manifest.get(key), str) or not manifest.get(key):
            report["errors"].append(f"{key} must be a non-empty string when present")

    if manifest.get("schema") != "solum.asset":
        report["errors"].append("schema must be solum.asset")

    if manifest.get("schemaVersion") != 1:
        report["errors"].append("schemaVersion must be 1")

    if manifest.get("assetType") not in VALID_TYPES:
        report["errors"].append(f"invalid assetType: {manifest.get('assetType')}")

    if manifest.get("validationState") not in VALID_STATES:
        report["errors"].append(f"invalid validationState: {manifest.get('validationState')}")

    file_list = manifest.get("fileList", [])
    content_hashes = manifest.get("contentHashes", {})

    if not isinstance(file_list, list):
        report["errors"].append("fileList must be array")
        file_list = []

    if not isinstance(content_hashes, dict):
        report["errors"].append("contentHashes must be object")
        content_hashes = {}

    for rel in file_list:
        if not isinstance(rel, str) or not rel:
            report["errors"].append("fileList contains invalid path")
            continue

        if rel == "asset_manifest.json":
            report["warnings"].append("asset_manifest.json should not usually be inside fileList")
            continue

        if ".." in Path(rel).parts:
            report["errors"].append(f"path escapes asset dir: {rel}")
            continue

        f = asset_dir / rel
        entry = {
            "path": rel,
            "exists": f.exists(),
            "sha256": None,
            "expected": content_hashes.get(rel),
            "hashMatches": None
        }

        if not f.exists():
            report["errors"].append(f"missing file: {rel}")
            report["status"] = "missing_file"
            report["files"].append(entry)
            continue

        actual = sha256_file(f)
        entry["sha256"] = actual

        expected = content_hashes.get(rel)
        if not expected:
            report["errors"].append(f"missing content hash for: {rel}")
            entry["hashMatches"] = False
        elif expected != actual:
            report["errors"].append(f"hash mismatch for: {rel}")
            entry["hashMatches"] = False
        else:
            entry["hashMatches"] = True

        report["files"].append(entry)

    for rel in content_hashes.keys():
        if rel not in file_list:
            report["warnings"].append(f"contentHashes has unused entry: {rel}")

    if report["errors"] and report["status"] == "valid":
        report["status"] = "invalid"

    return report


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: asset_validator.py <asset_folder>")
        return 2

    asset_dir = Path(sys.argv[1]).expanduser().resolve()
    report = validate_asset(asset_dir)
    report_path = asset_dir / "validation_report.json"
    save_report = atomic_write_json(report_path, report)
    atomic_write_json(asset_dir / "save_report.json", save_report)

    print(f"Asset: {asset_dir}")
    print(f"Status: {report['status']}")
    print(f"Report: {report_path}")
    if report["errors"]:
        print("Errors:")
        for e in report["errors"]:
            print(f"- {e}")
    if report["warnings"]:
        print("Warnings:")
        for w in report["warnings"]:
            print(f"- {w}")
    return 0 if report["status"] == "valid" else 1


if __name__ == "__main__":
    raise SystemExit(main())
