# SOLUM Sky Weather Foundation

Status: P53 foundation.

## Patch Boundaries

- P51 = Environment API/time system.
- P52 = manifest/asset slots/fallback.
- P53 = real starter assets attempt + smooth sky visual/weather foundation.
- P54 next = dedicated cloud shadows/rain/snow VFX/performance if not completed in P53.

True volumetric clouds are future, not P53.

## Smooth Day/Night

P53 removes hard light jumps by using smoothstep/lerp curves for:

- sun elevation/intensity/color temperature;
- moon intensity;
- ambient / IBL strength;
- background brightness;
- exposure hint;
- stars intensity.

KTX skybox blending is not implemented yet. Runtime diagnostics must report:

```text
skyboxBlendStatus=discrete_preset_switch_light_blend_smooth
```

This means the skybox preset may switch discretely, but sun/moon/ambient/exposure/stars curves remain smooth.

## Sky Visual Layer

P53 uses a lightweight screen overlay for:

- sun disk, visible only when the computed sun is above the horizon;
- moon disk placeholder, visible at night;
- optional glare controlled by the existing Sun Glare setting.

The moon disk is a placeholder, not a real ephemeris. Stars remain `stars_asset_missing_placeholder` until a verified small star texture is converted.

## Cheap Cloud / Weather Foundation

P53 state:

- `cloudCoverage` 0..1;
- `cloudDensity` 0..1;
- `cloudSpeed`;
- `cloudDirectionDeg`;
- `cloudShadowStrength`, `cloudShadowScale`, `cloudShadowSpeed`;
- precipitation type `NONE/RAIN/SNOW`;
- precipitation intensity 0..1.

Applied now:

- cloud coverage/density attenuate sun intensity through `sunOcclusion`;
- ambient becomes slightly cooler/diffuse through the existing light/background path;
- reports expose cloud/weather truth.

Planned:

- scrolling cheap cloud noise/layer;
- soft projected cloud shadow mask;
- rain/snow particles and performance budget.

## Shadow Truth

Skybox/IBL does not cast hard shadows. Sun/moon directional lights do. Cloud shadows are future soft projected masks, not real volumetric shadows.
