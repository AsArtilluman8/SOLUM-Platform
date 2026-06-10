#!/usr/bin/env python3
import argparse
import json
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "_work/filament_tools"


def download(url, dest, max_bytes):
    request = urllib.request.Request(url, headers={"User-Agent": "SOLUM-env-tool-fetch/1.0"})
    total = 0
    with urllib.request.urlopen(request, timeout=120) as response, dest.open("wb") as fh:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            total += len(chunk)
            if total > max_bytes:
                raise RuntimeError(f"download_too_large:{total}>{max_bytes}")
            fh.write(chunk)
    return total


def safe_extract(tar, dest):
    root = dest.resolve()
    for member in tar.getmembers():
        target = (dest / member.name).resolve()
        if not str(target).startswith(str(root)):
            raise RuntimeError(f"unsafe_archive_path:{member.name}")
    tar.extractall(dest)


def find_tools(root):
    matches = {}
    for name in ("cmgen", "toktx"):
        found = [path for path in root.rglob(name) if path.is_file()]
        matches[name] = str(found[0]) if found else "missing"
    return matches


def main():
    parser = argparse.ArgumentParser(description="Manual helper to fetch Filament desktop tools for env asset conversion. Never used by Gradle.")
    parser.add_argument("--version", default="1.71.4", help="Filament runtime version from apps/engine/build.gradle.")
    parser.add_argument("--max-mb", type=int, default=250, help="Refuse tool archives larger than this.")
    parser.add_argument("--url", help="Explicit Filament tools archive URL if release asset naming changes.")
    args = parser.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)
    version = args.version.lstrip("v")
    candidates = []
    if args.url:
        candidates.append(args.url)
    candidates.extend([
        f"https://github.com/google/filament/releases/download/v{version}/filament-v{version}-linux.tgz",
        f"https://github.com/google/filament/releases/download/v{version}/filament-{version}-linux.tgz",
        f"https://github.com/google/filament/releases/download/v{version}/filament-v{version}-linux.tar.gz",
    ])

    errors = []
    archive = OUT / f"filament-tools-v{version}.tgz"
    for url in candidates:
        try:
            size = download(url, archive, args.max_mb * 1024 * 1024)
            with tarfile.open(archive, "r:*") as tar:
                safe_extract(tar, OUT)
            tools = find_tools(OUT)
            bin_dir = Path(tools["cmgen"]).parent if tools.get("cmgen") != "missing" else OUT
            print(json.dumps({
                "status": "downloaded",
                "url": url,
                "bytes": size,
                "tools": tools,
                "nextCommand": f"PATH=\"{bin_dir}:$PATH\" python3 tools/env_asset_pack_build.py --slot day --slot sunset --slot night --size 256",
            }, indent=2, sort_keys=True))
            return 0 if tools.get("cmgen") != "missing" else 2
        except Exception as exc:
            errors.append({"url": url, "error": str(exc)})
    print(json.dumps({
        "status": "failed_to_fetch_filament_tools",
        "version": version,
        "errors": errors,
        "fallback": "real_env_assets_blocked_by_missing_cmgen",
    }, indent=2, sort_keys=True))
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
