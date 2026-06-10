#!/usr/bin/env python3
import argparse
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
APP_ENV = ROOT / "apps/engine/src/main/assets/env"
DOWNLOADS = ROOT / "_work/env_asset_downloads"
MAX_BUNDLED_BYTES = 10 * 1024 * 1024
SAFE_LICENSES = {"CC0-1.0", "public-domain", "NASA-public-domain", "Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause"}


def tool(name):
    return shutil.which(name)


def load_manifest():
    return json.loads(MANIFEST.read_text(encoding="utf-8"))


def slots(manifest):
    return {slot["id"]: slot for slot in manifest.get("slots", [])}


def source_for(slot_id):
    matches = sorted(DOWNLOADS.glob(f"{slot_id}_source.*"))
    return matches[0] if matches else None


def require_safe(slot):
    license_value = str(slot.get("license", ""))
    if license_value not in SAFE_LICENSES:
        raise SystemExit(f"unsafe_or_unapproved_license:{slot.get('id')}:{license_value}")


def run_cmgen(cmgen, source, out_dir, size):
    cmd = [cmgen, "--format=ktx", f"--size={size}", "--extract-blur=0.1", f"--deploy={out_dir}", str(source)]
    subprocess.run(cmd, check=True)


def main():
    parser = argparse.ArgumentParser(description="Build SOLUM env KTX pack from already downloaded safe sources. Not used by normal Gradle builds.")
    parser.add_argument("--slot", action="append", choices=["day", "sunset", "night", "cloudy"], help="Slot(s) to convert. Default: day sunset night.")
    parser.add_argument("--size", type=int, default=256, help="cmgen cubemap size. Starter target is 128 or 256.")
    parser.add_argument("--dry-run", action="store_true", help="Report actions without converting.")
    args = parser.parse_args()

    cmgen = tool("cmgen")
    toktx = tool("toktx")
    wanted = args.slot or ["day", "sunset", "night"]
    manifest = load_manifest()
    by_id = slots(manifest)
    report = {
        "status": "ready" if cmgen else "conversion_tool_unavailable",
        "cmgen": cmgen or "missing",
        "toktx": toktx or "missing",
        "normalBuildRequiresInternet": False,
        "maxBundledEnvironmentBytes": MAX_BUNDLED_BYTES,
        "actions": [],
    }
    if not cmgen:
        report["nextCommand"] = "Install or expose Filament cmgen matching the app Filament runtime, then run python3 tools/env_asset_pack_build.py"
        print(json.dumps(report, indent=2, sort_keys=True))
        return 2

    APP_ENV.mkdir(parents=True, exist_ok=True)
    for slot_id in wanted:
        if slot_id not in by_id:
            raise SystemExit(f"missing_slot:{slot_id}")
        slot = by_id[slot_id]
        require_safe(slot)
        source = source_for(slot_id)
        if source is None:
            report["actions"].append({"slot": slot_id, "status": "source_missing", "expected": str(DOWNLOADS / f"{slot_id}_source.hdr")})
            continue
        if source.stat().st_size > 25 * 1024 * 1024:
            raise SystemExit(f"refusing_large_source:{source}:{source.stat().st_size}")
        action = {"slot": slot_id, "source": str(source.relative_to(ROOT)), "status": "dry_run" if args.dry_run else "converted"}
        if not args.dry_run:
            run_cmgen(cmgen, source, APP_ENV, args.size)
        report["actions"].append(action)
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0 if report["status"] == "ready" else 2


if __name__ == "__main__":
    raise SystemExit(main())
