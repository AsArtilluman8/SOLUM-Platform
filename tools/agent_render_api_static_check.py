#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RENDER = ROOT / "apps/engine/src/main/java/com/solum/engine/render"
ENVIRONMENT = ROOT / "apps/engine/src/main/java/com/solum/engine/environment"
SCENE = ROOT / "apps/engine/src/main/java/com/solum/engine/scene"
ACTIVITY = ROOT / "apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java"
ENV_MANIFEST = ROOT / "assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
APP_ENV_MANIFEST = ROOT / "apps/engine/src/main/assets/env/ENVIRONMENT_ASSETS_MANIFEST.json"
ENV_ASSET_DOC = ROOT / "docs/assets/SOLUM_ENVIRONMENT_ASSETS.md"
ENV_PIPELINE_DOC = ROOT / "docs/design/SOLUM_ENVIRONMENT_ASSET_PIPELINE.md"

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

checks["environment_package"] = {"status": "present" if ENVIRONMENT.is_dir() else "missing", "path": str(ENVIRONMENT.relative_to(ROOT))}
if not ENVIRONMENT.is_dir():
    missing.append("environment_package")

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
for name in [
    "EnvironmentApi",
    "EnvironmentSettings",
    "EnvironmentActualState",
    "EnvironmentDiagnostics",
    "TimeOfDayController",
    "SkyIblPreset",
    "CelestialBodyState",
    "EnvironmentController",
]:
    require_file(name, ENVIRONMENT / f"{name}.java")

activity_text = ACTIVITY.read_text(encoding="utf-8") if ACTIVITY.is_file() else ""
api_text = (RENDER / "RenderControlApi.java").read_text(encoding="utf-8") if (RENDER / "RenderControlApi.java").is_file() else ""
environment_api_text = (ENVIRONMENT / "EnvironmentApi.java").read_text(encoding="utf-8") if (ENVIRONMENT / "EnvironmentApi.java").is_file() else ""
environment_text = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in ENVIRONMENT.glob("*.java")) if ENVIRONMENT.is_dir() else ""
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

environment_methods = [
    "getSettings", "getActualState", "getDiagnostics", "setTimeOfDay", "setTimeSpeed",
    "setEnvironmentPreset", "setSunEnabled", "setSunAzimuth", "setSunElevation",
    "setSunIntensityLux", "setSunColorTemperatureKelvin", "setMoonEnabled",
    "setMoonAzimuth", "setMoonElevation", "setMoonIntensityLux", "setMoonPhase",
    "setIblPreset", "setIblStrength", "setIblRotation", "setSkyboxPreset",
    "setSkyboxVisible", "setStarsEnabled", "setStarsIntensity", "setCloudAmount",
    "setCloudCoverage", "setCloudDensity", "setCloudSpeed", "setCloudDirectionDeg",
    "setCloudShadowStrength", "setCloudShadowScale", "setCloudShadowSpeed",
    "setPrecipitationType", "setPrecipitationIntensity",
    "apply", "update",
]
missing_environment_methods = [method for method in environment_methods if method not in environment_api_text]
checks["environment_api_contract_methods"] = {
    "status": "present" if not missing_environment_methods else "missing",
    "missing_methods": missing_environment_methods,
}
if missing_environment_methods:
    missing.append("environment_api_contract_methods")

for key, haystack, terms in [
    ("diagnostics_on_demand_activity", activity_text, ["Copy Short Report", "Export Full Report", "buildShortDiagnosticsReport", "buildFullDiagnosticsReportJson", "SOLUM_REPORTS"]),
    ("fps_confidence_terms", activity_text + diagnostics_text, ["primaryFrameMs", "primaryFps", "primaryFpsSource", "javaCallbackFps", "p50FrameMs", "p95FrameMs", "worstFrameMs", "jitterMs", "fpsStability", "fpsConfidence", "timingDisagreement"]),
    ("ownership_terms", controller_text + diagnostics_text, ["RenderOwnershipMap", "ownershipSummary", "getOwnershipMap"]),
    ("feature_descriptor_terms", controller_text, ["RenderFeatureDescriptor", "mobileCost", "mobileSafe"]),
    ("cost_diagnostics_terms", controller_text + activity_text, ["RenderCostDiagnostics", "estimated_cost", "not_runtime_measured", "needs_cost_probe_later"]),
    ("fog_visibility_terms", controller_text + activity_text, ["fogVisibilityConfidence", "may_be_hidden_by_skybox_or_exposure", "fogWarning"]),
    ("render_control_center_sections", activity_text, ["Render Control Center: Basic", "Render Control Center: Lighting", "Render Control Center: Sky / IBL", "Render Control Center: PostFX", "Render Control Center: Color / Fog", "Debug"]),
    ("p51_environment_ui_report_terms", activity_text, ["Environment / Time of Day", "Time", "Dawn", "Noon", "Sunset", "Night", "environmentSettings", "environmentActualState", "environmentDiagnostics", "environmentShortReportSection"]),
    ("p51_environment_runtime_terms", activity_text + environment_text, ["EnvironmentController", "TimeOfDayController", "setTimeOfDay", "setSunIntensityLux", "setMoonIntensityLux", "slot_ready_asset_missing", "placeholder_not_rendered", "planned_p52_assets"]),
    ("p52_environment_asset_pipeline_terms", activity_text, ["ENVIRONMENT_ASSETS_MANIFEST", "activeIblAssetStatus", "activeSkyboxAssetStatus", "activeStarsAssetStatus", "fallbackActive", "assetLicenseStatus", "totalEnvAssetSizeEstimate"]),
    ("p53_smooth_sky_terms", activity_text + environment_text, ["smoothBlendStatus", "skyboxBlendStatus", "timeBlendPhase", "sunCurveT", "nightCurveT", "starsCurveT", "discrete_preset_switch_light_blend_smooth"]),
    ("p53_sky_visual_terms", activity_text + environment_text, ["sunDiskStatus", "moonDiskStatus", "stars_asset_missing_placeholder", "visible_overlay_billboard_lightweight", "visible_placeholder_overlay_no_ephemeris"]),
    ("p53_cloud_weather_terms", activity_text + environment_text, ["cloudCoverage", "cloudDensity", "sunOcclusion", "cloudShadowStatus", "precipitationStatus", "volumetricCloudsStatus", "not_implemented_mobile_future"]),
    ("render_control_center_api_driven_status", activity_text, ["featureLine", "compactOwnershipSummary", "compactCostSummary", "RenderFeatureDescriptor", "RenderCostDiagnostics"]),
    ("render_control_center_debug_on_demand", activity_text, ["debugPanel.getVisibility() == View.VISIBLE", "Copy Short Report", "Export Full Report"]),
    ("startup_crash_report_fallback", activity_text, ["installCrashReporter", "writeCrashReport", "SOLUM_CRASHES", "solum_crashes", "startupMilestone"]),
    ("startup_milestone_terms", activity_text, ["onCreate_start", "crash_handler_installed", "before_build_ui", "after_build_ui", "before_create_viewer", "after_create_viewer", "before_load_model", "after_load_model", "onCreate_done"]),
]:
    missing_terms = [term for term in terms if term not in haystack]
    checks[key] = {"status": "present" if not missing_terms else "missing", "missing_terms": missing_terms}
    if missing_terms:
        missing.append(key)

for key, path in [
    ("p52_environment_source_manifest", ENV_MANIFEST),
    ("p52_environment_app_manifest", APP_ENV_MANIFEST),
    ("p52_environment_asset_doc", ENV_ASSET_DOC),
    ("p52_environment_pipeline_doc", ENV_PIPELINE_DOC),
    ("p53_env_pack_tool", ROOT / "tools/env_asset_pack_build.py"),
    ("p53_sky_weather_doc", ROOT / "docs/design/SOLUM_SKY_WEATHER_FOUNDATION.md"),
]:
    ok = path.is_file()
    checks[key] = {"status": "present" if ok else "missing", "path": str(path.relative_to(ROOT))}
    if not ok:
        missing.append(key)

unsafe_license_matches = []
if ENV_MANIFEST.is_file():
    manifest_lower = ENV_MANIFEST.read_text(encoding="utf-8", errors="replace").lower()
    for needle in ["royalty-free", "non-commercial", "personal use", "unknown license", "sketchfab", "unity asset store"]:
        if needle in manifest_lower:
            unsafe_license_matches.append(needle)
checks["p52_environment_unsafe_license_strings"] = {
    "status": "present" if not unsafe_license_matches else "missing",
    "matches": unsafe_license_matches,
}
if unsafe_license_matches:
    missing.append("p52_environment_unsafe_license_strings")

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
