# SOLUM Platform

**SOLUM Platform** is an open-source, mobile-first game engine and creative toolchain for building AAA-style 3D games directly on Android devices.

The long-term goal is to make an Unreal Engine-like workflow possible from a phone: rendering, assets, materials, VFX, world building, diagnostics, and gameplay tooling without requiring a desktop workstation.

> Status: early-stage research and engineering project. The renderer and tooling foundation is active; gameplay systems and full editor workflows are still in development.

## Why SOLUM exists

Many creators, students, modders, and indie developers do not have access to powerful PCs. SOLUM explores whether advanced game creation can be made practical on mobile-first hardware using Android, Termux, Kotlin/Java, native components, Filament, glTF/GLB pipelines, procedural asset systems, and performance-aware tooling.

SOLUM is not intended to be a marketplace asset dump. The project focuses on **small reusable resources + procedural variation + strong tooling** so large worlds can be built without requiring huge asset libraries.

## Core vision

SOLUM aims to become a mobile-first creative platform with:

- **AAA-style mobile rendering** using Filament, PBR, HDR/IBL, lighting, glass/material inspection, and performance controls.
- **Phone-first development workflow** using Android and Termux.
- **Procedural Asset Economy**: master materials, reusable meshes, masks, seed-based variation, modular buildings, and asset deduplication.
- **Physical Material System**: materials that define density, hardness, buoyancy, breakage, sound, wetness, fire reaction, and world interaction.
- **Reactive World Systems**: grass, mud, snow, sand, water, props, and terrain reacting to footsteps, impacts, weather, and VFX.
- **Niagara-like VFX direction**: mobile-friendly magic, particles, trails, decals, light pulses, shockwaves, and cinematic impacts.
- **Accessible game creation** for people who want to learn engine development or build games without a workstation.

## Current focus

The project is currently focused on the rendering and asset-tooling foundation:

- Android application shell.
- Filament-based GLB/glTF preview.
- HDR/IBL environment loading path.
- Render control center.
- Camera/model transform controls.
- Material/glass/debug tooling foundations.
- Android/Termux build workflow.
- Documentation and roadmap cleanup for open-source development.

Legacy native Vulkan experiments remain in history and in some build areas, but the project direction has moved toward Filament as the primary renderer and SOLUM-specific systems layered above it.

## Key ideas

### Mobile-first, not desktop-down

SOLUM is designed around phone constraints from the beginning: Mali-class GPUs, small memory budgets, dynamic resolution, quality profiles, ASTC/KTX2/Basis compression, LOD, instancing, culling, streaming, and perception-based quality control.

### Procedural Asset Economy

Instead of shipping thousands of unique assets, SOLUM targets a small high-quality base library: modular building parts, reusable props, nature meshes, texture sets, master materials, masks, and seed-driven variations.

### Physical materials

SOLUM separates visual material, physical material, object construction, and runtime state. This allows a crate, barrel, glass cup, boat, rock, tree, and character body part to behave differently even when their visual meshes are simple.

### Perception-based performance

SOLUM should spend GPU/CPU budget where the player can perceive it: near the camera, in the center of attention, and around important gameplay events. Far, hidden, peripheral, or fast-motion details can be simplified smoothly.

## Planned systems

High-level roadmap:

1. Honest FPS/jank diagnostics and mobile performance defaults.
2. Filament material slot inspector and material override tools.
3. Master material presets and texture library.
4. Asset manifest and procedural variation system.
5. Modular building generator.
6. Physical material database and object construction profiles.
7. Terrain impact/crater events and world reaction systems.
8. Niagara-like SOLUM VFX framework.
9. Water/liquid master system.
10. Playable showcase scene.

See: [`ROADMAP.md`](ROADMAP.md)

## Repository structure

```text
apps/engine/     Android app and Filament preview activity
engine-core/     Native/engine experiments and legacy components
tools/           Build and helper scripts
docs/            Architecture, concepts, roadmap, status, OSS notes
tasks/           Patch/task planning and agent-readable notes
```

## Screenshots

Screenshots and visual proof should be collected in `docs/screenshots/` and referenced from `docs/SCREENSHOTS.md`.

Recommended screenshots:

- Filament GLB model preview.
- HDR/IBL environment preview.
- Render Control Center UI.
- Material/glass/debug tabs.
- Android/Termux build proof.
- Future VFX/water/world reaction demos.

## Open-source maintenance

This repository is being prepared as a public OSS project. The current priorities are clear README and roadmap, Apache-2.0 license, contribution guidelines, project status documentation, stable build notes, issue/task triage, safer architecture, and cleaner maintenance workflow.

See:

- [`CONTRIBUTING.md`](CONTRIBUTING.md)
- [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)
- [`docs/OPENAI_CODEX_FOR_OSS_APPLICATION.md`](docs/OPENAI_CODEX_FOR_OSS_APPLICATION.md)

## License

SOLUM Platform is released under the **Apache License 2.0**. See [`LICENSE`](LICENSE).

## Maintainer

Primary maintainer: [`AsArtilluman8`](https://github.com/AsArtilluman8)
