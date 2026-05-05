#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import os
import time
import uuid
from pathlib import Path

from transaction_save import atomic_write_json


def solum_root() -> Path:
    preferred = Path("/storage/emulated/0/SOLUMCreative")
    if preferred.parent.exists():
        return preferred
    return Path("/storage/emulated/0/Download/SOLUMCreative")


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return "sha256:" + h.hexdigest()


def main() -> int:
    root = solum_root()
    asset_dir = root / "assets" / "materials" / "sample_material"
    asset_dir.mkdir(parents=True, exist_ok=True)

    material = {
        "schema": "solum.material",
        "schemaVersion": 1,
        "baseColor": [0.8, 0.2, 0.12, 1.0],
        "roughness": 0.42,
        "metallic": 0.0,
        "notes": "Sample material for Patch 03 validation."
    }

    material_path = asset_dir / "material.json"
    save_report = atomic_write_json(material_path, material)

    manifest = {
        "schema": "solum.asset",
        "schemaVersion": 1,
        "assetId": str(uuid.uuid4()),
        "assetType": "material",
        "displayName": "Sample Material",
        "createdAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "createdBy": "solum.patch03.sample",
        "fileList": [
            "material.json"
        ],
        "contentHashes": {
            "material.json": sha256_file(material_path)
        },
        "schemaCompatibleWith": ">=1.0 <2.0",
        "validationState": "pending",
        "dependencies": []
    }

    atomic_write_json(asset_dir / "asset_manifest.json", manifest)
    atomic_write_json(asset_dir / "save_report.json", save_report)

    print(f"Sample asset created: {asset_dir}")
    print("Next:")
    print(f"python3 tools/asset_validator.py {asset_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
