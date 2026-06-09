#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java"

CHECKS = {
    "requestedMSAA": ["requestedSampleCount", "requestedMSAA"],
    "actualMSAA": ["actualSampleCount", "actualMSAA"],
    "msaaApplyStatus": ["msaaApplyStatus"],
    "requestedTAA": ["requestedTaa", "requestedTAA"],
    "actualTAA": ["actualTaa", "actualTAA"],
    "requestedDynamicResolution": ["requestedDynamicResolution"],
    "actualDynamicResolution": ["actualDynamicResolution"],
    "timing_disagreement": ["timingDisagreement", "timing_disagreement"],
    "javaCallbackFps_debug_only": ["Java callback FPS debug-only"],
    "FrameMetrics": ["FrameMetrics", "frameMetricsTotalMs"],
    "p95_worst_jank": ["p95FrameMs", "worstFrameMs", "jankFrameCounter"],
    "performance_doctor_cause": ["performanceDoctorCause", "cause:"],
    "preset_mismatch": ["presetMismatchStatus", "presetMismatch"],
    "control_truth_statuses": ["requested_", "actual_", "requires_recreate", "not_exposed"],
    "ao_manual_handler": ["aoButton", "aoMode = aoMode.next()", "applyAoOptions", "requestedAoMode", "actualAoMode", "aoApplyStatus"],
    "bloom_manual_handler": ["bloomButton", "bloomMode = bloomMode.next()", "applyBloomOptions", "requestedBloomMode", "actualBloomMode", "bloomApplyStatus"],
    "msaa_manual_handler": ["msaaButton", "requestedSampleCount = requestedSampleCount == 1 ? 2 : (requestedSampleCount == 2 ? 4 : 1)", "setSampleCount", "msaaApplyStatus"],
    "taa_manual_handler": ["taaButton", "taaEnabled = !taaEnabled", "applyTemporalAaOptions", "requestedTaa", "actualTaa", "taaApplyStatus"],
    "color_manual_handlers": ["Color Exposure", "Color Contrast", "Color Saturation", "Color Temperature", "applyColorGrading", "colorGradingRequestKey"],
    "manual_override_status": ["markManualOverride", "manualOverrideStatus", "manualOverrideCount"],
    "labels_after_manual_clicks": ["refreshUiNow", "updateToggleLabels", "updateAllSliderLabels", "setLastAction"],
    "preset_mismatch_status_exists": ["updatePresetMismatchStatus", "presetMismatchStatus", "presetMismatch"],
}

def status_for(text, needles):
    missing = [needle for needle in needles if needle not in text]
    if not missing:
        return {"status": "present"}
    return {"status": "missing", "missing_terms": missing}

def method_body(text, name):
    marker = f"private void {name}("
    start = text.find(marker)
    if start < 0:
        return ""
    depth = 0
    opened = False
    for index in range(start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
            opened = True
        elif char == "}":
            depth -= 1
            if opened and depth == 0:
                return text[start:index + 1]
    return text[start:]

def separation_check(text):
    apply_body = method_body(text, "applyQualityProfile")
    defaults_body = method_body(text, "applyQualityProfileDefaults")
    if not apply_body:
        return {"status": "missing", "missing_terms": ["applyQualityProfile"]}
    if not defaults_body:
        return {"status": "missing", "missing_terms": ["applyQualityProfileDefaults"]}
    if "applyQualityProfileDefaults(" in apply_body:
        return {"status": "warning", "warning": "applyQualityProfile calls applyQualityProfileDefaults; manual overrides may be reset"}
    return {"status": "present", "note": "applyQualityProfileDefaults not called from applyQualityProfile"}

def main():
    if not TARGET.exists():
        print(json.dumps({"target": str(TARGET), "status": "missing_file"}, indent=2))
        return 1
    text = TARGET.read_text(encoding="utf-8", errors="replace")
    result = {
        "target": str(TARGET.relative_to(ROOT)),
        "status": "static_check_only_not_runtime_proof",
        "checks": {name: status_for(text, needles) for name, needles in CHECKS.items()}
    }
    result["checks"]["profile_defaults_not_called_from_apply"] = separation_check(text)
    missing = [name for name, item in result["checks"].items() if item["status"] != "present"]
    result["summary"] = "warning_missing_terms_static_only" if missing else "present_static_only_not_runtime_proof"
    result["warnings"] = missing
    result["missing"] = missing
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
