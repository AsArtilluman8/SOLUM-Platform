# SOLUM Render API Foundation

Status: P48 foundation.

## Purpose

`FilamentGlbPreviewActivity` remains the UI shell. It owns Android widgets, panel layout, import flows, and legacy diagnostics while the renderer control state starts moving into reusable engine classes.

Future Labs and panels should call the Render API instead of copying Activity-local render code.

## Main Pieces

- `RenderSettings` = requested user state.
- `RenderActualState` = applied/truth state.
- `RenderDiagnostics` = short truth report and not-exposed/not-verified list.
- `RenderControlApi` = stable control interface for UI/tools.
- `FilamentRenderController` = first concrete Filament implementation.
- `SceneRegistry` = list of objects currently known to the editor/engine.

## Current Boundary

P48 is intentionally not a visual-feature patch.

Routed through `RenderControlApi`:

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
- color exposure/contrast/saturation/temperature as requested settings.

Still Activity-local in P48:

- Android UI layout and labels;
- ColorGrading object lifecycle;
- fog apply details;
- lighting and shadow caster entity updates;
- performance/FPS timing;
- config import/export;
- model import/load lifecycle.

These remain Activity-local because moving them safely needs smaller follow-up patches with build/runtime proof.

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
