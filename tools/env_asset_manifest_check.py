#!/usr/bin/env python3
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_MANIFEST = ROOT / "assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
APP_MANIFEST = ROOT / "apps/engine/src/main/assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
APP_ASSETS = ROOT / "apps/engine/src/main/assets"
REQUIRED_SLOTS = {"day", "sunset", "night", "studio_debug"}
OPTIONAL_SLOTS = {"cloudy"}
SAFE_LICENSES = {
    "CC0-1.0",
    "public-domain",
    "NASA-public-domain",
    "Apache-2.0",
    "MIT",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "project-generated-fallback",
}
UNSAFE_LICENSE_FRAGMENTS = [
    "royalty-free",
    "editorial",
    "personal use",
    "non-commercial",
    "sketchfab",
    "unity asset store",
    "unknown",
    "todo",
]
ALLOWED_STATUS = {"bundled", "missing_fallback", "planned", "conversion_required"}
MAX_BUNDLED_BYTES = 10 * 1024 * 1024
WARN_FILE_BYTES = 4 * 1024 * 1024


def load_manifest(path):
    if not path.is_file():
        raise ValueError(f"missing_manifest:{path.relative_to(ROOT)}")
    return json.loads(path.read_text(encoding="utf-8"))


def fail(message, errors):
    errors.append(message)


def validate_manifest(manifest, label, errors, warnings):
    if manifest.get("schema") != "solum_environment_assets_manifest":
        fail(f"{label}:bad_schema", errors)
    slots = manifest.get("slots")
    if not isinstance(slots, list):
        fail(f"{label}:slots_not_list", errors)
        return
    ids = {str(slot.get("id", "")) for slot in slots}
    missing_required = sorted(REQUIRED_SLOTS - ids)
    if missing_required:
        fail(f"{label}:missing_required_slots:{','.join(missing_required)}", errors)
    unknown_slots = sorted(ids - REQUIRED_SLOTS - OPTIONAL_SLOTS)
    if unknown_slots:
        warnings.append(f"{label}:unknown_slots:{','.join(unknown_slots)}")
    total_estimate = 0
    total_existing = 0
    for slot in slots:
        slot_id = str(slot.get("id", ""))
        license_value = str(slot.get("license", "")).strip()
        license_lower = license_value.lower()
        if not license_value:
            fail(f"{label}:{slot_id}:missing_license", errors)
        if license_value not in SAFE_LICENSES:
            fail(f"{label}:{slot_id}:unsafe_or_unapproved_license:{license_value}", errors)
        for fragment in UNSAFE_LICENSE_FRAGMENTS:
            if fragment in license_lower:
                fail(f"{label}:{slot_id}:unsafe_license_fragment:{fragment}", errors)
        status = str(slot.get("status", ""))
        if status not in ALLOWED_STATUS:
            fail(f"{label}:{slot_id}:bad_status:{status}", errors)
        bundled = bool(slot.get("bundled", False))
        estimate = int(slot.get("estimatedSizeBytes", 0) or 0)
        if estimate < 0:
            fail(f"{label}:{slot_id}:negative_estimated_size", errors)
        total_estimate += estimate
        for key in ("localIblPath", "localSkyboxPath", "localStarsPath"):
            rel = str(slot.get(key, "") or "")
            if not rel:
                continue
            if rel.startswith("/") or ".." in Path(rel).parts:
                fail(f"{label}:{slot_id}:{key}:unsafe_path:{rel}", errors)
                continue
            path = APP_ASSETS / rel
            if path.suffix.lower() in {".hdr", ".exr"}:
                fail(f"{label}:{slot_id}:{key}:raw_hdr_exr_not_allowed_in_apk:{rel}", errors)
            if path.exists():
                size = path.stat().st_size
                total_existing += size
                if estimate and estimate != size:
                    fail(f"{label}:{slot_id}:{key}:estimated_size_mismatch:{estimate}!={size}", errors)
                if size > WARN_FILE_BYTES:
                    warnings.append(f"{label}:{slot_id}:{key}:large_file:{size}")
            elif bundled:
                fail(f"{label}:{slot_id}:{key}:bundled_file_missing:{rel}", errors)
    if total_estimate > MAX_BUNDLED_BYTES:
        fail(f"{label}:estimated_bundle_too_large:{total_estimate}", errors)
    if total_existing > MAX_BUNDLED_BYTES:
        fail(f"{label}:existing_bundle_too_large:{total_existing}", errors)
    return total_estimate, total_existing


def main():
    errors = []
    warnings = []
    source = load_manifest(SOURCE_MANIFEST)
    app = load_manifest(APP_MANIFEST)
    if source != app:
        fail("source_and_app_manifest_differ", errors)
    source_estimate, source_existing = validate_manifest(source, "source", errors, warnings)
    validate_manifest(app, "app", errors, warnings)
    for raw in APP_ASSETS.glob("env/**/*"):
        if raw.is_file() and raw.suffix.lower() in {".hdr", ".exr"}:
            fail(f"raw_hdr_exr_file_in_app_assets:{raw.relative_to(ROOT)}", errors)
    result = {
        "status": "ok" if not errors else "failed",
        "sourceManifest": str(SOURCE_MANIFEST.relative_to(ROOT)),
        "appManifest": str(APP_MANIFEST.relative_to(ROOT)),
        "totalEstimatedBundledBytes": source_estimate,
        "totalExistingBundledBytes": source_existing,
        "errors": errors,
        "warnings": warnings,
    }
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if not errors else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(json.dumps({"status": "failed", "error": str(exc)}, indent=2, sort_keys=True))
        raise SystemExit(1)
