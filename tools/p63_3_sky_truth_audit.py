#!/usr/bin/env python3
"""Validate and package only the truth-safe P63.3 Moon/star payloads."""

from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path("/mnt/shared/Download/SOLUM_UDS_TRUTH_SAFE_MOON_STARS_P64AD/assets/textures")
PRIVATE = ROOT / "apps/engine/src/main/assets/private_premium/p63_3/sky"
SELECTED = {
    "moon_color.png": "8a8ff79b0d06946bfd09efcada50cc4a9891076b7f2a00b49fa8e182bbb6e375",
    "moon_normal.png": "1b0e0306afc8626bdf1e06f809c6f3fe01b3cb997234ca2653a5ef89af9a9998",
    "real_stars.png": "feb52ae23909cd4a9faf1f9384d1661711c5dedc07dde31e4dda444e7745f69c",
    "tiling_stars.png": "841f09169dc0e955a580c0faef8b1a62372d06e5340b701b757a64f51242e8c5",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> None:
    PRIVATE.mkdir(parents=True, exist_ok=True)
    result: dict[str, str] = {}
    for name, expected in SELECTED.items():
        source = SOURCE / name
        if not source.is_file():
            result[name] = "UNAVAILABLE_PROCEDURAL_FALLBACK"
            continue
        actual = sha256(source)
        if actual != expected:
            raise RuntimeError(f"truth hash mismatch for {name}: {actual}")
        shutil.copy2(source, PRIVATE / name)
        result[name] = "UDS_VERIFIED_PACKAGED_LOCAL_PRIVATE"
    print(json.dumps({"status": "PASS", "private": str(PRIVATE), "assets": result}, sort_keys=True))


if __name__ == "__main__":
    main()
