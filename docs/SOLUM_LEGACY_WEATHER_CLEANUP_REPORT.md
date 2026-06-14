# SOLUM Legacy Weather Cleanup Report

Patch: `P_UDW_Weather_Showcase_Camera_Cleanup_01`

## LEGACY_OPENGL_ES_WEATHER

Status: `isolated`

- `WeatherAlphaProofActivity.java` was not registered in `AndroidManifest.xml`.
- It was an old Activity-level OpenGL ES weather proof path, separate from current `FilamentGlbPreviewActivity`.
- It is moved out of app source to `_work/agent_reports/P_UDW_Weather_Showcase_Camera_Cleanup_01/legacy_isolated/java/`.

## LEGACY_V45_WEATHER_EXPERIMENT

Status: `isolated`

Exact isolated files:

- `WeatherAlphaProofActivity.java`
- `WeatherAlphaProofActivity.java.v44c_backup`
- `WeatherAlphaProofActivity.java.v45b_backup_20260613_214928`
- `WeatherAlphaProofActivity.java.v45c_backup_20260613_220159`
- `WeatherAlphaProofActivity.java.v45d_bak`

Exact isolated asset trees:

- `apps/engine/src/main/assets/weather_v43c/`
- `apps/engine/src/main/assets/weather_v43d/`
- `apps/engine/src/main/assets/weather_v44b/`
- `apps/engine/src/main/assets/weather_v44c/`
- `apps/engine/src/main/assets/weather_v45d/`

## CURRENT_FILAMENT_RUNTIME

Status: `kept`

- `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java`
- `apps/engine/src/main/java/com/solum/engine/environment/*`
- `apps/engine/src/main/assets/weather/weather_presets.json`
- `apps/engine/src/main/assets/weather/solum_udw_runtime_recipe.json`

## CURRENT_USED_ASSETS

Status: `procedural runtime recipe bundled; decoded candidates kept private`

- `apps/engine/src/main/assets/weather/weather_presets.json`
- `apps/engine/src/main/assets/weather/solum_udw_runtime_recipe.json`

Decoded PNG/WAV candidates were identified locally, but not committed into app assets because SOLUM policy forbids committing potentially paid Marketplace-derived binaries. Weather visuals in this patch use procedural Filament scene geometry.

## SAFE_REMOVED

No tracked production file was deleted blindly.

The old app-shipped paths were isolated with exact `mv` commands into `_work/agent_reports/.../legacy_isolated/`.

## KEPT_FOR_RECON_REFERENCE

- Recon toolkit output under `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/` was not modified.
- Isolated legacy assets remain under `_work/agent_reports/P_UDW_Weather_Showcase_Camera_Cleanup_01/legacy_isolated/assets/`.
- Decoded texture/audio candidates remain private/local reference only.

## HOLD_UNCERTAIN

- top-level `assets/weather/` was present before this patch and was not touched.
- Deeper `.uasset` decode remains `HOLD_UASSET_DECODE`.
- Weather audio remains `HOLD_PRIVATE_ASSET_PIPELINE`.

## Risk Notes

- Old weather trees were untracked before this patch, so isolation removes them from app packaging but may not appear as tracked deletions.
- Current weather visuals are Filament scene geometry foundation, not final UDS/UDW material graph reconstruction.
