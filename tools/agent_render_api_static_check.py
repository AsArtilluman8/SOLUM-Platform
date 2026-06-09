#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RENDER = ROOT / "apps/engine/src/main/java/com/solum/engine/render"
SCENE = ROOT / "apps/engine/src/main/java/com/solum/engine/scene"
ACTIVITY = ROOT / "apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java"

checks = {}
missing = []

def require_file(key, path):
    ok = path.is_file()
    checks[key] = {"status": "present" if ok else "missing", "path": str(path.relative_to(ROOT))}
    if not ok:
        missing.append(key)
    return ok

checks["render_package"] = {"status": "present" if RENDER.is_dir() else "missing", "path": str(RENDER.relative_to(ROOT))}
if not RENDER.is_dir():
    missing.append("render_package")

for name in [
    "RenderSettings",
    "RenderActualState",
    "RenderDiagnostics",
    "RenderControlApi",
    "FilamentRenderController",
]:
    require_file(name, RENDER / f"{name}.java")

require_file("SceneObject", SCENE / "SceneObject.java")
require_file("SceneRegistry", SCENE / "SceneRegistry.java")

activity_text = ACTIVITY.read_text(encoding="utf-8") if ACTIVITY.is_file() else ""
activity_refs = ["RenderControlApi", "FilamentRenderController"]
checks["activity_references_render_api"] = {
    "status": "present" if any(ref in activity_text for ref in activity_refs) else "missing",
    "refs": activity_refs,
}
if checks["activity_references_render_api"]["status"] != "present":
    missing.append("activity_references_render_api")

for term in [
    "requestedSampleCount",
    "actualSampleCount",
    "msaaApplyStatus",
    "requestedTaa",
    "actualTaa",
    "taaApplyStatus",
    "manualOverrideStatus",
    "EXPECTED_APK",
    "DOWNLOAD_APK_PATH",
    "COPIED_TO_DOWNLOAD",
]:
    haystack = activity_text
    if term in ["EXPECTED_APK", "DOWNLOAD_APK_PATH", "COPIED_TO_DOWNLOAD"]:
        report = ROOT / "tools/agent_build_report.sh"
        haystack = report.read_text(encoding="utf-8") if report.is_file() else ""
    ok = term in haystack
    checks[f"p47b2_{term}"] = {"status": "present" if ok else "missing"}
    if not ok:
        missing.append(f"p47b2_{term}")

result = {
    "status": "static_check_only_not_runtime_proof" if not missing else "missing_required_items",
    "summary": "present_static_only_not_runtime_proof" if not missing else "missing: " + ", ".join(missing),
    "checks": checks,
    "missing": missing,
}
print(json.dumps(result, indent=2, sort_keys=True))
raise SystemExit(0 if not missing else 1)
