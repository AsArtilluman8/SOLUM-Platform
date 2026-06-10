# SOLUM Sky / Weather Core

Status: P55B visible core implementation.

## Goal

P55 added the first SOLUM-owned sky/weather core without importing Ultra Dynamic Sky assets. P55B adds the first real renderer-owned visible layer.

The implementation is intentionally lightweight:

- Java data/controller layer for sky and weather truth;
- smooth time-of-day curves;
- renderer-owned procedural sky clear color / skybox fallback;
- renderer-owned runtime-generated glTF layer for sun, moon, stars, clouds, rain, and snow;
- sun directional light attenuation by cloud coverage/density;
- generated low-volume weather audio loop when weather is active;
- weather parameters for future materials and particles;
- truthful diagnostics for every real vs placeholder visual feature.

## Public repo safety

Ultra Dynamic Sky may be used only as local reference/audit material.

Forbidden in Git:

- `.rar` archives;
- `.uasset`, `.umap`, `.uexp`, `.ubulk`, `.uplugin`;
- copied Marketplace/Fab/Unreal content folders;
- extracted UDS textures, particles, materials, blueprints, maps, meshes, or sounds.

P55 adds no paid public assets.

## API classes

Package:

```text
apps/engine/src/main/java/com/solum/engine/skyweather/
```

Classes:

- `SkySettings`
- `SkyActualState`
- `SkyDiagnostics`
- `SkyController`
- `WeatherSettings`
- `WeatherActualState`
- `WeatherDiagnostics`
- `WeatherController`
- `WeatherPreset`
- `SkyWeatherVisualLayer`

## Required state covered

Sky:

- `timeOfDayHours`
- day/night phase
- sun azimuth/elevation/intensity/color temperature
- moon azimuth/elevation/intensity/phase placeholder
- stars intensity
- sky gradient colors
- sun occlusion by clouds
- aurora intensity placeholder

Weather:

- cloud coverage/density/speed/direction
- rain intensity
- snow intensity
- fog/haze intensity
- wind intensity/direction
- wetness amount
- snow amount

## Visual implementation

Real in P55/P55B:

- renderer-owned procedural sky/background color driven by time and weather;
- sun directional light remains renderer light, not UI overlay;
- cloud sun-occlusion attenuates sun light;
- fog/haze can drive existing Filament fog parameters;
- world-space sun disk generated at runtime from SOLUM geometry;
- world-space moon disk generated at runtime with simple phase scaling;
- runtime-generated star quads visible at night;
- runtime-generated cheap cloud quads driven by coverage/density;
- runtime-generated rain/snow quads for Rain, Snow, and Storm presets;
- generated PCM weather audio loop for rain/wind/storm when volume is above zero;
- diagnostics and reports expose sky/weather truth.

Placeholder after P55B:

- cloud shadow mask;
- wetness/snow material response;
- aurora rendering.
- weather sound design beyond generated placeholder loop.

Important: sun/moon screen-space overlays are disabled. P55 must not draw UI sun/moon over the model.

## Mobile safety

P55 avoids:

- volumetric clouds;
- heavy GPU particles;
- Niagara or Unreal content;
- per-frame heavy diagnostics;
- large textures.

The first visual layer is a final-system-compatible slice, not a fake renderer.

## Diagnostics

Short and full reports include:

- sky system status;
- weather system status;
- sun/moon visual status;
- stars status;
- cloud visual status;
- rain/snow status;
- private assets enabled;
- paid assets tracked;
- fallback status;
- generated asset sizes.

## Generated assets

P55B does not add generated public image assets to Git. The visible layer is generated at runtime as a small GLB byte buffer owned by SOLUM code.

Report fields:

- `generatedSkyWeatherAssetBytes`;
- `publicSkyWeatherAssetCount`;
- `paidAssetsTracked=false`;
- `privateAssetsEnabled=false`.

Future public assets may include tiny SOLUM-generated noise/star textures if provenance and size are documented in a manifest.
