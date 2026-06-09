# SOLUM Roadmap

Status: public strategic roadmap.

SOLUM is a mobile-first open-source platform for building games and interactive applications on Android-class devices. The project goal is not only to render models, but to provide a practical engine/editor foundation that can later support creative labs, reusable packages, AI-assisted workflows, and a community asset/mechanic ecosystem.

This roadmap is intentionally device-neutral. Performance targets are defined by device tiers rather than by one maintainer's personal test phone.

## Product Vision

SOLUM should become:

- a mobile-first engine/editor foundation;
- a scene workspace for composing gameplay scenes;
- an asset and package system for reusable content;
- a set of specialized Labs for materials, VFX, animation, UI, physics reactions, water, audio, and tools;
- an Agent Console where users can connect local or cloud agents under explicit permissions;
- a future package marketplace for assets, mechanics, templates, and agent skills.

SOLUM must support both simple and advanced workflows:

- simple users can use presets, sliders, assets, and ready-made mechanics;
- technical users can use nodes, events, scripting, APIs, and agents;
- advanced users can extend the platform with native code, custom material pipelines, or custom modules.

## Near-Term Roadmap

### P47B — FPS Truth + Control Truth

Goal: make render diagnostics and existing UI controls trustworthy.

Required outcomes:

- primary FPS must not use Java callback FPS as the main user-facing value;
- the HUD should show a conservative, game-facing FPS estimate and a simple GOOD / OK / JANK / BAD status;
- Debug should expose timing sources, FrameMetrics, p95/worst/jank, and disagreement flags;
- all current buttons and sliders must either work, or clearly report requested-only / requires-recreate / not-exposed;
- Low, Medium, High, Ultra, and Screenshot profiles must force documented expected settings;
- Debug must show requested versus actual state for important controls.

### P48 — Mobile Performance Targets

Goal: define public quality and FPS targets for device tiers.

Required outcomes:

- define Baseline, Mid-range, Upper-mid, Flagship, and Desktop/External tiers;
- define Low Safe, Medium Mobile, High Preview, Ultra Preview, and Screenshot profiles;
- define what FPS range is acceptable for gameplay versus preview/screenshot;
- define what effects are allowed or forbidden in each quality profile.

### P49 — Render API Extraction

Goal: stop trapping renderer ownership, settings, and diagnostics inside one Activity.

Expected modules:

- `SolumRenderSettings`
- `SolumRenderDiagnostics`
- `SolumRenderStatus`
- `SolumRenderPreset`
- `SolumRenderController`

Future Labs and editor panels must use these modules instead of copy-pasting Activity-local renderer code.

### P50 — Full Render Control Center

Goal: expose the verified render API surface in a structured UI.

Required sections:

- Basic: safe controls for normal users;
- Advanced: deeper quality and rendering controls;
- Debug: diagnostics, unsupported states, requested/actual state, profiler commands;
- Screenshot/Experimental: expensive visual modes that are not gameplay targets.

Rule: do not expose controls as production features unless the API is available, the state can be applied, and the UI reports truthfully whether it works live or requires recreation/restart.

### P51 — Scene Workspace

Goal: move beyond a single model preview.

Required outcomes:

- multiple objects/models in one scene;
- object list / hierarchy;
- selected object state;
- duplicate/delete/select;
- transform values;
- object lifecycle and unload/release rules.

### P52 — Asset Shelf v1

Goal: create the local asset browser and registry foundation.

Required outcomes:

- scan local SOLUM asset folders;
- preview assets;
- validate file type and metadata;
- add assets to scene;
- unload/release assets safely;
- prepare the structure for future cloud/community packages.

### P53 — Transform Gizmo v1

Goal: add touch-first object transform tools.

Required outcomes:

- move / rotate / scale;
- world/local modes;
- snap and precision controls;
- numeric values as secondary precision controls;
- mobile-friendly input handling.

Reference existing good solutions before implementation, such as common transform-control patterns in professional editors and open-source libraries. Reuse ideas where license-compatible and technically practical.

### P54 — Animation Preview v1

Goal: inspect and play glTF/GLB animations.

Required outcomes:

- list clips;
- play / pause / stop;
- loop toggle;
- speed control;
- timeline/scrub;
- diagnostics for skeleton, morph targets, and unsupported states.

### P55 — Scene Save/Load v1

Goal: persist scene state separately from temporary render preview settings.

Required outcomes:

- save scene objects, transforms, asset references, and basic settings;
- load saved scenes;
- separate scene save from render control presets;
- prepare for project files and package dependencies.

## Foundation Exit Rule

Renderer/editor foundation is not complete while FPS truth, render settings, scene state, assets, transforms, animation preview, and save/load remain trapped inside one Activity.

SOLUM can move into major Labs only when the engine/editor foundation provides reusable systems for:

- render settings and diagnostics;
- quality profiles;
- scene workspace;
- asset registry/shelf;
- transform gizmo;
- animation preview;
- scene save/load;
- package metadata.

## Later Labs

After the foundation is usable, SOLUM should add specialized Labs:

- Material Lab;
- Glass Lab;
- VFX Lab;
- Animation / Character Lab;
- Physics Reaction Lab;
- Water Lab;
- UI Lab;
- Audio / Cutscene Lab;
- Performance Profiler;
- Agent Console;
- Package Hub.

## Strategic Future

Long-term SOLUM should support:

- AI agent integration through safe permissions;
- reusable mechanic packages;
- cloud/community asset packages;
- package reviews, ratings, previews, and dependency reports;
- monetization options such as sponsorships, verified packs, or future revenue-share models;
- community growth through public roadmap, GitHub, Telegram, short devlogs, and testing reports.
