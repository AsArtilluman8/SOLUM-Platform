#!/usr/bin/env python3
import argparse
import json
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
OUT = ROOT / "_work/env_asset_downloads"
METADATA = OUT / "download_manifest.json"
SAFE_LICENSES = {"CC0-1.0", "public-domain", "NASA-public-domain", "Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause"}


def load_manifest():
    return json.loads(MANIFEST.read_text(encoding="utf-8"))


def slot_map(manifest):
    return {slot["id"]: slot for slot in manifest.get("slots", [])}


def content_length(url):
    request = Request(url, method="HEAD", headers={"User-Agent": "SOLUM-env-asset-tool/1.0"})
    with urlopen(request, timeout=30) as response:
        value = response.headers.get("Content-Length")
        return int(value) if value else None


def download(url, dest, max_bytes):
    request = Request(url, headers={"User-Agent": "SOLUM-env-asset-tool/1.0"})
    total = 0
    with urlopen(request, timeout=120) as response, dest.open("wb") as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            total += len(chunk)
            if total > max_bytes:
                raise ValueError(f"download_too_large:{total}>{max_bytes}")
            out.write(chunk)
    return total


def record_download(entry):
    records = []
    if METADATA.is_file():
        records = json.loads(METADATA.read_text(encoding="utf-8"))
    records = [item for item in records if not (item.get("slot") == entry.get("slot") and item.get("sourceUrl") == entry.get("sourceUrl"))]
    records.append(entry)
    METADATA.write_text(json.dumps(records, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Download selected SOLUM environment source assets only. Does not convert or commit files.")
    parser.add_argument("--slot", required=True, help="Manifest slot id: day, sunset, night, cloudy, studio_debug")
    parser.add_argument("--url", help="Explicit source URL. Defaults to the slot sourceUrl.")
    parser.add_argument("--max-mb", type=int, default=25, help="Refuse downloads above this size unless --force is set.")
    parser.add_argument("--force", action="store_true", help="Allow source downloads above --max-mb. Never affects APK bundling.")
    args = parser.parse_args()

    manifest = load_manifest()
    slots = slot_map(manifest)
    if args.slot not in slots:
        raise SystemExit(f"unknown_slot:{args.slot}")
    slot = slots[args.slot]
    license_value = str(slot.get("license", ""))
    if license_value not in SAFE_LICENSES:
        raise SystemExit(f"unsafe_or_unapproved_license:{license_value}")
    url = args.url or str(slot.get("sourceUrl", ""))
    if not (url.startswith("https://") or url.startswith("http://")):
        raise SystemExit(f"download_url_required_for_slot:{args.slot}")
    max_bytes = args.max_mb * 1024 * 1024
    size = content_length(url)
    if size is not None and size > max_bytes and not args.force:
        raise SystemExit(f"refusing_large_download:{size}>{max_bytes}; rerun with --force only for source cache, never APK bundling")
    OUT.mkdir(parents=True, exist_ok=True)
    suffix = Path(url.split("?", 1)[0]).suffix or ".bin"
    dest = OUT / f"{args.slot}_source{suffix}"
    actual = download(url, dest, max_bytes if not args.force else 1024 * 1024 * 1024)
    entry = {
        "status": "downloaded_source_only_not_converted_not_committed",
        "slot": args.slot,
        "path": str(dest.relative_to(ROOT)),
        "bytes": actual,
        "license": license_value,
        "sourceName": str(slot.get("sourceName", "")),
        "sourceUrl": url,
        "manifest": str(MANIFEST.relative_to(ROOT)),
    }
    record_download(entry)
    print(json.dumps(entry, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
