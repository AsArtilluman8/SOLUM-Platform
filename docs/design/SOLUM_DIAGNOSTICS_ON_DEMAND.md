# SOLUM Diagnostics On Demand

Status: P49 foundation.

## Rule

Diagnostics must not become the performance problem.

Live HUD stays cheap and compact:

- FPS;
- frame ms;
- p95 ms;
- health;
- confidence;
- short cost cause.

Full diagnostics are generated only when the user taps a button.

## User Actions

Copy Short Report:

- copies compact text to clipboard;
- includes timestamp, profile, FPS/frame ms, confidence/stability, expensive features, scene/model summary, ownership summary, and not-verified/not-exposed summary.

Export Full Report:

- writes JSON to `/storage/emulated/0/Download/SOLUM_REPORTS/render_report_YYYYMMDD_HHMMSS.json`;
- if the path is unavailable, the app must not crash;
- status is shown in Debug and report state remains available in app memory for the current session.

## Report Truth

Frame ms is the primary technical truth.

FPS is derived from frame ms:

```text
fps = 1000 / frame_ms
```

Java callback FPS is debug-only. It is not the primary user-facing FPS.

GPU timing is not claimed unless exposed by Android/Filament metrics. If unavailable:

```text
gpu_timing_unavailable_frame_metrics_only
```

Cost diagnostics are estimated until a future cost probe/profiler exists:

```text
estimated_cost
not_runtime_measured
needs_cost_probe_later
```
