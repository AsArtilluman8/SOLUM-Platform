# SOLUM Codex Fast Map

Patch: `P_UDW_Weather_Showcase_Camera_Cleanup_01`

## Filament preview / camera / gestures

- `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java`
  - `createViewer()` owns `ModelViewer`, `SurfaceView`, touch routing.
  - `handleWorkspaceTouch()` is the viewport gesture owner.
  - `handleCameraOrbitDrag()` handles one-finger orbit.
  - `handleCameraPinchPan()` handles pinch zoom and two-finger pan.
  - `applyCameraControls()` writes real Filament camera `lookAt`.
  - `buildCameraPanel()` owns camera UI/debug fields.

## Model registry / selection

- `apps/engine/src/main/java/com/solum/engine/scene/SceneRegistry.java`
  - Flat runtime object registry and selected object id.
- `apps/engine/src/main/java/com/solum/engine/scene/SceneObject.java`
  - Runtime object transform/status data.
- `FilamentGlbPreviewActivity.java`
  - `syncSceneRegistryForActiveModel()`
  - `requestPick()`
  - `selectNextObject()`
  - `setSelectedObjectTransform()`
  - `ensureWorkspaceGizmo()`

## Weather UI / runtime

- `apps/engine/src/main/java/com/solum/engine/environment/EnvironmentController.java`
- `apps/engine/src/main/java/com/solum/engine/environment/EnvironmentSettings.java`
- `apps/engine/src/main/java/com/solum/engine/environment/EnvironmentActualState.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherRuntimeParameters.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherPreset.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherVfxRecipe.java`
- `apps/engine/src/main/assets/weather/weather_presets.json`
- `apps/engine/src/main/assets/weather/solum_udw_runtime_recipe.json`
- `FilamentGlbPreviewActivity.java`
  - `setWeatherPreset()`
  - `applyEnvironmentStateToActivity()`
  - weather showcase geometry is owned by the Filament scene, not OpenGL ES.

## Legacy OpenGL ES / weather candidates

- `apps/engine/src/main/java/com/solum/engine/WeatherAlphaProofActivity.java`
- `apps/engine/src/main/java/com/solum/engine/WeatherAlphaProofActivity.java.*backup*`
- `apps/engine/src/main/java/com/solum/engine/WeatherAlphaProofActivity.java.*_bak`
- `apps/engine/src/main/assets/weather_v43c/`
- `apps/engine/src/main/assets/weather_v43d/`
- `apps/engine/src/main/assets/weather_v44b/`
- `apps/engine/src/main/assets/weather_v44c/`
- `apps/engine/src/main/assets/weather_v45d/`
- top-level `assets/weather/` if it is old generated demo data, not app runtime.

## Recon toolkit output

- Toolkit script:
  - `/data/data/com.termux/files/home/SOLUM_RECON_TOOLKIT_MASTER/solum_ue_recon_toolkit_light_v1.py`
- Latest output:
  - `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/`
- Key files:
  - `manifest_light.json`
  - `10_ASSET_MAP/asset_inventory_light.json`
  - `21_BLUEPRINT_NODE_TABLES/all_node_tables_light.json`
  - `60_RECIPES/solum_recon_light_recipe.json`
  - `70_REPORTS/reconstruction_light_report.md`

## Fast commands

```bash
bash tools/agent_gate.sh && bash tools/agent_brief.sh
git status --short --branch
git log --oneline -3
python3 tools/solum_weather_fast_inventory.py
python3 tools/solum_uds_udw_runtime_recon.py
bash tools/build_native_engine.sh && ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle --no-daemon -p "$PWD" clean assembleDebug
```

## Files to touch in this patch

- `docs/SOLUM_CODEX_FAST_MAP.md`
- `tools/solum_weather_fast_inventory.py`
- `tools/solum_uds_udw_runtime_recon.py`
- `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherPreset.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherRuntimeParameters.java`
- `apps/engine/src/main/java/com/solum/engine/environment/WeatherVfxRecipe.java`
- `apps/engine/src/main/java/com/solum/engine/environment/EnvironmentSettings.java`
- `apps/engine/src/main/java/com/solum/engine/environment/EnvironmentActualState.java`
- `apps/engine/src/main/assets/weather/solum_udw_runtime_recipe.json`
- patch reports under `docs/`

## Do not reread without a new reason

- Full `docs/patch_history/PATCH_HISTORY.md`
- Full `docs/RENDER_LAB.md`
- Full generated shader headers.
- Full `apps/engine/build/`.
- Full old weather asset trees after inventory has summarized them.
- Full Unreal Engine repo. Use the already verified targeted references only.
