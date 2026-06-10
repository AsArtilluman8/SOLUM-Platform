# SOLUM Sky Weather Foundation

Status: P53B honest foundation.

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

## Sky Visual Layer Truth

P53B rejects the P53 screen-space sun/moon disk approach. Android View overlays are not acceptable sky objects because they attach to the camera/screen and can draw over scene objects.

Current required statuses:

- `screenSpaceSunMoonOverlayStatus=disabled_screen_space_overlay_rejected`;
- `sunVisualStatus=world_space_sky_disk_not_implemented`;
- `moonVisualStatus=world_space_sky_disk_not_implemented`.

The old Sun Glare Android overlay may remain only as optional/debug glare. It is not a sun disk, moon disk, skybox, atmosphere, or renderer-owned sky layer.

Correct future implementation:

- world-space sky disk renderables;
- skybox content;
- or a renderer-owned sky/atmosphere pass.

Stars remain `stars_asset_missing_placeholder` until a verified small star texture is converted.

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
