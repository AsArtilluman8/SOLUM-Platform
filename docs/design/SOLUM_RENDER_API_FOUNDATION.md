# SOLUM Render API Foundation

Status: P49 contract foundation.

## Purpose

`FilamentGlbPreviewActivity` remains the UI shell. It owns Android widgets, panel layout, import flows, and legacy diagnostics while the renderer control state starts moving into reusable engine classes.

Future Labs and panels should call the Render API instead of copying Activity-local render code.

## Main Pieces

- `RenderSettings` = requested user state.
- `RenderActualState` = applied/truth state.
- `RenderDiagnostics` = short truth report and not-exposed/not-verified list.
- `RenderControlApi` = stable control interface for UI/tools.
- `FilamentRenderController` = first concrete Filament implementation.
- `RenderOwnershipMap` = who owns requested/apply/actual state for each render feature.
- `RenderFeatureDescriptor` = small metadata card for future Render Control Center.
- `RenderCostDiagnostics` = honest estimated mobile cost diagnostics.
- `SceneRegistry` = list of objects currently known to the editor/engine.

## Current Boundary

P48 is intentionally not a visual-feature patch.

Contract-covered by `RenderControlApi`:

- quality profile;
- render scale;
- dynamic resolution;
- MSAA;
- FXAA;
- TAA;
- dithering;
- SSR;
- refraction;
- AO mode;
- bloom mode;
- bloom strength;
- bloom highlight;
- shadows;
- fog mode/density/start/end/height/color;
- color exposure/contrast/saturation/temperature/tint as requested settings;
- sun, ambient, fill, background, preset, light rig;
- IBL intensity/rotation and skybox;
- sun glare;
- model transform and camera preset;
- diagnostics, ownership map, feature descriptors, short report, full report export.

Still Activity-local or partial in P49:

- Android UI layout and labels;
- ColorGrading object lifecycle;
- fog apply details;
- lighting and shadow caster entity updates;
- performance/FPS timing collection;
- config import/export;
- model import/load lifecycle.

These are not faked as controller-owned. They are marked `activity_local`, `partial`,
`not_exposed`, `not_verified`, or `planned` in diagnostics and ownership map.

## Diagnostics Policy

Live HUD stays lightweight:

- FPS;
- primary frame ms;
- p95 frame ms;
- health label;
- confidence;
- short likely cost cause.

Full diagnostics are generated only on demand:

- Copy Short Report = compact text for developer/user sharing.
- Export Full Report JSON = richer state for Codex/AI/debug.

Frame ms is the primary truth. FPS is derived as `1000 / frame_ms`.
This is still not perfect GPU timing. If GPU timing is unavailable, reports must say
`gpu_timing_unavailable_frame_metrics_only`.

## Scene Registry Stub

`SceneRegistry` currently registers the loaded GLB/glTF as one selected `SceneObject`.

Fields:

- id;
- name;
- type;
- source;
- selected;
- transform placeholder;
- status.

It is not a multi-object editor yet. No gizmo, asset shelf, hierarchy editing, duplicate/delete, or scene save/load is implemented in P48.

## Future Direction

Next patches should move more Activity-local state behind the API:

1. Split quality profile defaults into reusable preset data.
2. Move color/fog lifecycle into controller-owned apply helpers.
3. Expand SceneRegistry into multi-object scene workspace.
4. Add scene save/load after registry ownership is stable.
