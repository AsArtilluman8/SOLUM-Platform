# SOLUM Environment API

Status: P51 foundation.

P51 is system/API first. It creates `EnvironmentApi` as the owner of time of day, sun/moon intent, ambient/background hints, IBL/skybox preset slots, stars placeholder state, fallback policy, and diagnostics.

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

## Runtime Truth

Live:

- time-of-day slider and presets;
- sun direction/intensity/color temperature applied through current Filament directional light path;
- ambient/background/skybox visibility hints applied through current Activity-local Filament controls;
- IBL strength/rotation routed to current IBL controls when available.

On demand:

- full JSON report with `environmentSettings`, `environmentActualState`, `environmentDiagnostics`;
- copy short report environment section.

Placeholder / P52:

- moon second directional light;
- real day/sunset/night IBL assets;
- real star texture;
- asset pipeline/cooks.

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
```

If missing, the app must not crash. Diagnostics must say `missing_asset_fallback`, `slot_ready_asset_missing`, or `planned_p52_assets`.
