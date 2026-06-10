# SOLUM Environment API

Status: P51 foundation, P52 asset slots connected, P53 smooth sky/weather foundation.

P51 is system/API first. It creates `EnvironmentApi` as the owner of time of day, sun/moon intent, ambient/background hints, IBL/skybox preset slots, stars placeholder state, fallback policy, and diagnostics.

P52 is manifest/asset slots/fallback. It adds source/license manifest, app asset paths, validation tools, and safe runtime loader slots.

P53 is real starter assets attempt + smooth sky visual/weather foundation. If `cmgen`/`toktx` are unavailable, P53 keeps fallback active and reports `conversion_tool_unavailable` instead of faking KTX assets.

## Ownership

- `EnvironmentApi`: time, sun, moon, stars, environment preset slots, fallback truth.
- `RenderControlApi`: render quality, post-processing, color/fog controls, Activity-local apply bridge.
- Filament runtime: `Skybox`, `IndirectLight`, `DirectionalLight`, camera exposure, clear/background.

The Activity may apply current Filament knobs, but it must not own the environment model.

## Units

- World scale: 1 world unit = 1 meter.
- Sun/moon directional light: lux.
- Future point/spot/local lights: lumens/candela/range.
- Color temperature: Kelvin.

## P51 Scope

- Simple non-astronomical time approximation.
- Sun direction/intensity/color temperature.
- Moon state and diagnostics placeholder.
- Stars intensity placeholder.
- IBL/skybox preset slots.
- Neutral fallback when assets are missing.
- Short/full report environment sections.

P51 does not add weather, volumetric clouds, heavy HDRI assets, star assets, Bruneton/Hosek runtime atmosphere, or a fake blue sphere sky.

P52 keeps the same rule for advanced atmosphere: it adds manifest/tooling/loader slots only, not weather, volumetric clouds, Bruneton/Hosek runtime atmosphere, or a fake production sky.

P53 adds smoothstep/lerp curves for sun, moon, ambient/IBL strength, background brightness, exposure hint, and stars intensity. True skybox blending between KTX assets is not implemented yet; diagnostics must say `skyboxBlendStatus=discrete_preset_switch_light_blend_smooth`.

P53 weather is a cheap foundation only:

- `cloudCoverage`, `cloudDensity`, `cloudSpeed`, `cloudDirectionDeg`;
- cloud light attenuation through `sunOcclusion`;
- cloud shadow mask slots/status only;
- precipitation enum/status placeholder only;
- `volumetricCloudsStatus=not_implemented_mobile_future`.

True volumetric clouds are future, not P53. Cheap clouds use future scrolling noise/layer, sun attenuation, and a future projected cloud shadow mask. Cloud shadows are soft projected masks, not real volumetric shadows.

## Runtime Truth

Live:

- time-of-day slider and presets;
- sun direction/intensity/color temperature applied through current Filament directional light path;
- ambient/background/skybox visibility hints applied through current Activity-local Filament controls;
- IBL strength/rotation routed to current IBL controls when available.

On demand:

- full JSON report with `environmentSettings`, `environmentActualState`, `environmentDiagnostics`;
- copy short report environment section.

P52 asset pipeline:

- manifest exists at `assets/env/ENVIRONMENT_ASSETS_MANIFEST.json`;
- app-bundled manifest copy exists at `apps/engine/src/main/assets/env/ENVIRONMENT_ASSETS_MANIFEST.json`;
- day/sunset/night/cloudy/studio_debug slots map to planned KTX paths;
- the Activity checks Android assets and reuses the existing Filament `KTX1Loader` path if a KTX is present;
- if KTX files are missing or load fails, P51 neutral/current fallback stays active and diagnostics report `missing_asset_fallback`.

Placeholder / P52B:

- moon second directional light;
- real day/sunset/night IBL assets generated with a Filament-version-matched `cmgen`;
- real star texture;
- actual converted starter KTX bundle.

P54 next, if not completed in P53:

- dedicated cloud shadows;
- rain/snow VFX;
- cheap cloud visual layer performance validation.

Skybox/IBL does not cast hard shadows. Sun/moon directional lights are the source for hard/soft shadows. Cloud shadows are a future soft projected mask, not volumetric shadows.

Fallback is allowed only as neutral placeholder. It is not the final visual sky system.

## Asset Slot Convention

```text
assets/env/day_ibl.ktx
assets/env/day_skybox.ktx
assets/env/sunset_ibl.ktx
assets/env/sunset_skybox.ktx
assets/env/night_ibl.ktx
assets/env/night_skybox.ktx
assets/env/stars_milkyway.ktx
assets/env/studio_debug_ibl.ktx
assets/env/studio_debug_skybox.ktx
```

If missing, the app must not crash. Diagnostics must say `missing_asset_fallback`, `slot_ready_asset_missing`, or `planned_p52_assets`.
