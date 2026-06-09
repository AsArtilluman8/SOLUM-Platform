# SOLUM Render Foundation Plan

Status: public render foundation design.

SOLUM must become a reusable engine/editor foundation, not a single Activity with many local controls. This document defines the render foundation path before major Labs are created.

## Current Priority

Before adding new visual systems, SOLUM must make existing controls, diagnostics, and quality profiles trustworthy.

Immediate priorities:

1. FPS Truth;
2. Control Truth;
3. Quality Profile Truth;
4. Mobile Performance Targets;
5. Render API extraction;
6. Render Control Center;
7. Scene/Asset/Gizmo/Animation foundation.

## Patch Path

### P47B — FPS Truth + Control Truth

- Fix existing buttons and sliders.
- Add requested versus actual diagnostics.
- Make primary FPS conservative and user-facing.
- Keep Java callback FPS debug-only.
- Add human-readable performance causes.
- Add runtime control truth table.

### P48 — Mobile Performance Targets

- Add public device tier and FPS target documentation.
- Define Low/Medium/High/Ultra/Screenshot expected state.
- Make performance goals visible to developers and contributors.

### P49 — Render API Extraction

Extract reusable renderer modules:

- `SolumRenderSettings`
- `SolumRenderDiagnostics`
- `SolumRenderStatus`
- `SolumRenderPreset`
- `SolumRenderController`

Goal: future tools and Labs must use the same render/settings/diagnostics system.

### P50 — Render Control Center

Use the audited API surface to build a structured UI:

- Basic;
- Advanced;
- Debug;
- Screenshot/Experimental.

Do not expose controls as production features if they are not wired, not verified, or require recreation without explaining it.

### P51 — Scene Workspace

- multiple models/assets in scene;
- object list;
- selected object;
- transforms;
- object lifecycle;
- delete/duplicate/unload.

### P52 — Asset Shelf v1

- local asset registry;
- preview;
- file validation;
- add asset to scene;
- unload/release;
- package metadata preparation.

### P53 — Transform Gizmo v1

- move / rotate / scale;
- world/local modes;
- snap;
- touch-first controls;
- numeric precision controls.

### P54 — Animation Preview v1

- list glTF/GLB clips;
- play/pause/stop;
- timeline/scrub;
- playback speed;
- loop;
- skeleton/morph diagnostics.

### P55 — Scene Save/Load v1

- save/load scene graph;
- asset references;
- transforms;
- render settings separated from scene state;
- package dependency preparation.

## Render API Goals

The render system should expose:

- quality profile state;
- requested versus actual state;
- render settings model;
- diagnostics model;
- performance state;
- control status;
- scene render ownership;
- safe apply/recreate behavior.

## Future Labs Dependency

The following Labs depend on this foundation:

- Material Lab;
- Glass Lab;
- VFX Lab;
- Animation/Character Lab;
- Physics Reaction Lab;
- Water Lab;
- UI Lab;
- Audio/Cutscene Lab;
- Performance Profiler;
- Agent Console.

Labs must not copy Activity-local renderer code.

## Exit Gate

Renderer foundation is not complete until:

- FPS truth is good enough for gameplay decisions;
- controls report requested/actual state;
- quality profiles are stable;
- render settings and diagnostics are reusable modules;
- multiple objects can exist in a scene;
- assets can be added from a shelf;
- objects can be transformed with gizmo/numeric controls;
- animations can be previewed;
- scenes can be saved and loaded.
