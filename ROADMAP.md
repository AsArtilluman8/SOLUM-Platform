# SOLUM Platform Roadmap

SOLUM is a mobile-first game engine and creative toolchain for building AAA-style games directly on Android.

## Stage 1 — Public OSS foundation

- Public repository.
- Apache-2.0 license.
- User-facing README.
- Contribution guide.
- Code of Conduct.
- Project status document.
- Screenshots document.
- Codex for OSS application notes.

## Stage 2 — Renderer and tooling foundation

- Filament as primary renderer.
- GLB/glTF import and preview.
- HDR/IBL environment loading.
- Render Control Center.
- Camera and model transform tools.
- Material/glass/debug tabs.
- Honest FPS, frame time, jank, and runtime diagnostics.
- Mobile-safe defaults.

Rule: SSR is manual-only and must never be part of default presets, not even Ultra.

## Stage 3 — Material and asset tooling

- Material slot inspector.
- Material override system.
- Master material preset system.
- Texture library rules.
- KTX2/Basis/ASTC compression workflow.
- ORM packing and deduplication.
- Asset manifest system.

## Stage 4 — Procedural Asset Economy

Goal: make large worlds from small high-quality bases.

- Reusable texture sets.
- Reusable modular meshes.
- Master materials.
- Masks for dirt, wetness, moss, scratches, cracks, snow, burn, blood, and color variation.
- Prop variation system.
- Modular building generator.
- NPC modular generator.
- Mask painter foundation.
- LOD and collision generation.

## Stage 5 — Physical Material System

Goal: materials should define behavior, not only appearance.

- Physical material database.
- Density, hardness, friction, elasticity, water absorption, flammability, sound profile, and break mode.
- Object construction profiles: solid, hollow, shell, porous, sealed, open, filled, leaking.
- Buoyancy and floating/sinking prototypes.
- Impact energy, dent, crack, splinter, shatter, and sound routing.

## Stage 6 — VFX and world reaction

- SOLUM VFX core runtime.
- Filament VFX adapter.
- Magic impact VFX.
- Terrain impact and crater events.
- Grass, mud, snow, sand, water, and prop reactions.
- Water/liquid master system.
- Weather and atmosphere.

## Stage 7 — Gameplay/editor showcase

- Playable showcase scene.
- Character movement and camera.
- Interactive props.
- Transport or mount prototype.
- Spatial inventory concept prototype.
- World reaction demo.
- Android performance profiles.

## Long-term product vision

SOLUM should become a practical open-source mobile creative platform for learning engine development, building mobile-first game prototypes, experimenting with assets/materials/VFX, and maintaining advanced Android creative tooling.
