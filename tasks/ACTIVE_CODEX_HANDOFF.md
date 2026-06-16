# ACTIVE CODEX HANDOFF

Date: 2026-06-16

## Current goal

Restore order before more feature patches.

Priority:
1. Clean and simplify active working context.
2. Fix camera/gizmo touch behavior.
3. Fix mobile performance defaults.
4. Continue UDS/UDW weather integration from the completed Blueprint pin-link work.

## Do not touch in camera task

- UDS/UDW/weather integration.
- Glass/material/native Vulkan files.
- Filament dependencies.
- Renderer architecture.
- Files outside this repo.

## Current known issues

- Camera still does not move correctly on device.
- Prior camera/gizmo Codex report was not proven by diff.
- Code review found `ModelViewer(..., manipulator)` still present in `FilamentGlbPreviewActivity.java` while Activity also applies manual camera `lookAt`.
- Screenshots showed FPS around 11-12 with frame time around 82-92 ms and cause `TAA, MSAA 4x`.
- Screenshots also showed `ibl=missing_asset_fallback` and `sky=missing_asset_fallback`.

## Dirty working tree from latest local audit

Unrelated local changes were present in native/glass/weather files plus untracked `assets/weather/` and `tools/solum_blueprint_pinlink_extractor.py`. Do not mix those into the camera patch.

## Completed pin-link milestone

Reusable Blueprint pin-link extraction for Ultra Dynamic Sky reached a working milestone.

Validated stats:

```text
k2_node_exports              11950
nodes_with_pins              11681
pin_count                    29433
edge_count                   14857
strict_edge_count            11711
edge_with_target_pin_match   11711
selected_recipe_count        11
```

Main extracted weather recipes:

1. Set Fog
2. Set Current Fog Base Colors
3. Set Previous Weather Variables
4. Set Cloud Coverage
5. Get Cloud Coverage Local
6. Get Current Sky Light Color and Intensity
7. Get Volumetric Cloud Emissive Colors
8. SetBloomMaxBrightness
9. SetBloomThreshold
10. SetBloomTint
11. SetCastShadows

## Patch queue

### P29A cleanup/context safety

Only classify dirty files and write a cleanup plan. Do not delete, reset, or edit production code.

### P29B camera owner truth fix

Target file should be `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java` unless evidence proves another file is required.

Goal:
- one clear camera owner;
- stable orbit/pan/zoom;
- UI touch must not move scene;
- no full UI refresh or preferences write on every touch move;
- debug status: cameraOwner, activeGesture, uiConsumedTouch, yaw, pitch, distance, target, transformMode, renderCameraApplied.

### P29C performance defaults

LOW/BAD must not keep TAA or MSAA 4x. HUD must separate profile label from actual TAA/MSAA state.

### P29D environment fallback truth

Make sky/IBL fallback honest and not confused with real UDS/UDW assets.

## Build command

```bash
bash tools/build_native_engine.sh && ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle --no-daemon -p "$PWD" clean assembleDebug && mkdir -p /storage/emulated/0/Download/SOLUM_APK && cp apps/engine/build/outputs/apk/debug/engine-debug.apk /storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk
```
