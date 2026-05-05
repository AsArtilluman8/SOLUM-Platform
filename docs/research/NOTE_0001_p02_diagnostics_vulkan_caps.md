# NOTE-0001: Patch P02 Diagnostics v1 + Vulkan Capability Check

## Problem

Before building renderer/materials/shadows, SOLUM needs a reliable diagnostics package and real Vulkan capability data from the target Android/Termux device.

## References studied

- `android/ndk-samples` — Android/NDK lifecycle and build patterns.
- `KhronosGroup/Vulkan-Samples` — Vulkan instance/device/features/limits/extensions patterns.
- `ARM-software/vulkan_best_practice_for_mobile_developers` — mobile/Mali Vulkan constraints and low-overhead thinking.
- Perfetto docs — future profiling/reporting reference.
- SOLUM docs:
  - `DIAGNOSTICS_SPEC.md`
  - `BUILD_ENV_SPEC.md`
  - `PERFORMANCE_BUDGETS.md`
  - `RENDERING_TARGET_SPEC.md`

## Adopted principles

- Diagnostics first, renderer later.
- Vulkan caps dump before swapchain/triangle.
- latest/archive outputs.
- One ZIP + one HTML report for the user.
- Low-overhead diagnostics by default.
- Honest failure status instead of fake success.

## Rejected parts

- Full renderer in Patch P02.
- Vulkan swapchain/triangle in Patch P02.
- Heavy profiling/Perfetto integration in Patch P02.
- OpenGL/Canvas fallback.
- Random output files in Download root.

## SOLUM adaptation

Patch P02 creates shell/Python diagnostics and a tiny C Vulkan capability dumper.

If the native caps tool cannot run from Termux, diagnostics still succeeds with `vulkan_caps.json` status=`failed`, build log, and a clear next step.

## Patch impact

Adds:

- `tools/collect_diagnostics.sh`
- `tools/report_builder.py`
- `tools/vulkan_caps/*`
- patch history entry

## Diagnostics/tests

User runs:

```bash
cd ~/SOLUM-Platform && git pull && bash tools/collect_diagnostics.sh
```

Expected outputs:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
/storage/emulated/0/SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
```
