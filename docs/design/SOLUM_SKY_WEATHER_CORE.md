# SOLUM Sky / Weather Core

Status: P55 first owned implementation.

## Goal

P55 adds the first SOLUM-owned sky/weather core without importing Ultra Dynamic Sky assets.

The implementation is intentionally lightweight:

- Java data/controller layer for sky and weather truth;
- smooth time-of-day curves;
- renderer-owned procedural sky clear color / skybox fallback;
- sun directional light attenuation by cloud coverage/density;
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

Real in P55:

- renderer-owned procedural sky/background color driven by time and weather;
- sun directional light remains renderer light, not UI overlay;
- cloud sun-occlusion attenuates sun light;
- fog/haze can drive existing Filament fog parameters;
- diagnostics and reports expose sky/weather truth.

Placeholder in P55:

- sun visual disk;
- moon visual disk;
- stars texture/points;
- cloud texture/noise layer;
- rain/snow particles;
- cloud shadow mask;
- wetness/snow material response;
- aurora rendering.

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

P55 does not add generated public image assets.

Future public assets may include tiny SOLUM-generated noise/star textures if provenance and size are documented in a manifest.
