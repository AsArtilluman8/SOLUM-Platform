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
    "control_truth_statuses": ["requested_", "actual_", "requires_recreate", "not_exposed"]
}

def status_for(text, needles):
    missing = [needle for needle in needles if needle not in text]
    if not missing:
        return {"status": "present"}
    return {"status": "missing", "missing_terms": missing}

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
    missing = [name for name, item in result["checks"].items() if item["status"] != "present"]
    result["summary"] = "warning_missing_terms" if missing else "present"
    result["missing"] = missing
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
