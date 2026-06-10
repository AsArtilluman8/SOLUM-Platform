#!/usr/bin/env python3
"""Fail if paid/native Unreal asset source files are tracked by Git."""

from __future__ import annotations

import subprocess
import sys
from pathlib import PurePosixPath


FORBIDDEN_EXTENSIONS = {
    ".uasset",
    ".umap",
    ".uexp",
    ".ubulk",
    ".uplugin",
}

PRIVATE_FOLDERS = (
    "private_assets/",
    "_private_assets/",
    "local_assets/",
    "apps/engine/src/main/assets/private_env/",
    "apps/engine/src/main/assets/private_weather/",
    "apps/engine/src/main/assets/private_premium/",
)

ALLOWED_PRIVATE_PLACEHOLDERS = {
    "private_assets/README.md",
}


def git_tracked_files() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files", "-z"],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return [item for item in result.stdout.decode("utf-8").split("\0") if item]


def main() -> int:
    tracked = git_tracked_files()
    forbidden = []
    private_tracked = []

    for path in tracked:
        suffix = PurePosixPath(path).suffix.lower()
        if suffix in FORBIDDEN_EXTENSIONS:
            forbidden.append(path)

        if path not in ALLOWED_PRIVATE_PLACEHOLDERS and any(
            path.startswith(folder) for folder in PRIVATE_FOLDERS
        ):
            private_tracked.append(path)

    if private_tracked:
        print("WARNING: tracked files found in private asset folders:")
        for path in private_tracked:
            print(f"  {path}")

    if forbidden:
        print("ERROR: forbidden Unreal/Fab/Marketplace asset files are tracked:")
        for path in forbidden:
            print(f"  {path}")
        return 1

    print("OK: no forbidden paid/native Unreal asset files are tracked.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
