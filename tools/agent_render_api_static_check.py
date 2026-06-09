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
    "RenderOwnershipMap",
    "RenderFeatureDescriptor",
    "RenderCostDiagnostics",
]:
    require_file(name, RENDER / f"{name}.java")

require_file("SceneObject", SCENE / "SceneObject.java")
require_file("SceneRegistry", SCENE / "SceneRegistry.java")

activity_text = ACTIVITY.read_text(encoding="utf-8") if ACTIVITY.is_file() else ""
api_text = (RENDER / "RenderControlApi.java").read_text(encoding="utf-8") if (RENDER / "RenderControlApi.java").is_file() else ""
diagnostics_text = (RENDER / "RenderDiagnostics.java").read_text(encoding="utf-8") if (RENDER / "RenderDiagnostics.java").is_file() else ""
controller_text = (RENDER / "FilamentRenderController.java").read_text(encoding="utf-8") if (RENDER / "FilamentRenderController.java").is_file() else ""
activity_refs = ["RenderControlApi", "FilamentRenderController"]
checks["activity_references_render_api"] = {
    "status": "present" if any(ref in activity_text for ref in activity_refs) else "missing",
    "refs": activity_refs,
}
if checks["activity_references_render_api"]["status"] != "present":
    missing.append("activity_references_render_api")

api_methods = [
    "setQualityProfile", "setRenderScale", "setDynamicResolution", "setMsaa", "setFxaa", "setTaa",
    "setDithering", "setSsr", "setRefraction", "setAoMode", "setBloomMode", "setBloomStrength",
    "setBloomHighlight", "setShadowMode", "setFogMode", "setFogDensity", "setFogHeight",
    "setFogStart", "setFogEnd", "setFogColorRgb", "setColorExposure", "setColorContrast",
    "setColorSaturation", "setColorTemperature", "setColorTint", "setSunIntensity",
    "setAmbientIntensity", "setFillIntensity", "setBackgroundIntensity", "setSunDirection",
    "setLightingPreset", "setLightRig", "setIblIntensity", "setIblRotation", "setSkyboxEnabled",
    "setSunGlareEnabled", "setSunGlareStrength", "setSunGlareSize", "setModelScale",
    "setModelOffset", "setModelRotation", "setCameraPreset", "getDiagnostics", "getActualState",
    "getSettings", "getFeatureDescriptors", "getOwnershipMap", "buildShortReport", "exportFullReport",
]
missing_methods = [method for method in api_methods if method not in api_text]
checks["render_api_contract_methods"] = {
    "status": "present" if not missing_methods else "missing",
    "missing_methods": missing_methods,
}
if missing_methods:
    missing.append("render_api_contract_methods")

for key, haystack, terms in [
    ("diagnostics_on_demand_activity", activity_text, ["Copy Short Report", "Export Full Report", "buildShortDiagnosticsReport", "buildFullDiagnosticsReportJson", "SOLUM_REPORTS"]),
    ("fps_confidence_terms", activity_text + diagnostics_text, ["primaryFrameMs", "primaryFps", "primaryFpsSource", "javaCallbackFps", "p50FrameMs", "p95FrameMs", "worstFrameMs", "jitterMs", "fpsStability", "fpsConfidence", "timingDisagreement"]),
    ("ownership_terms", controller_text + diagnostics_text, ["RenderOwnershipMap", "ownershipSummary", "getOwnershipMap"]),
    ("feature_descriptor_terms", controller_text, ["RenderFeatureDescriptor", "mobileCost", "mobileSafe"]),
    ("cost_diagnostics_terms", controller_text + activity_text, ["RenderCostDiagnostics", "estimated_cost", "not_runtime_measured", "needs_cost_probe_later"]),
    ("fog_visibility_terms", controller_text + activity_text, ["fogVisibilityConfidence", "may_be_hidden_by_skybox_or_exposure", "fogWarning"]),
    ("render_control_center_sections", activity_text, ["Render Control Center: Basic", "Render Control Center: Lighting", "Render Control Center: Sky / IBL", "Render Control Center: PostFX", "Render Control Center: Color / Fog", "Debug"]),
    ("render_control_center_api_driven_status", activity_text, ["featureLine", "compactOwnershipSummary", "compactCostSummary", "RenderFeatureDescriptor", "RenderCostDiagnostics"]),
    ("render_control_center_debug_on_demand", activity_text, ["debugPanel.getVisibility() == View.VISIBLE", "Copy Short Report", "Export Full Report"]),
]:
    missing_terms = [term for term in terms if term not in haystack]
    checks[key] = {"status": "present" if not missing_terms else "missing", "missing_terms": missing_terms}
    if missing_terms:
        missing.append(key)

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
