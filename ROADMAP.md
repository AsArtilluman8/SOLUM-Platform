# SOLUM Platform Roadmap

SOLUM is a mobile-first game engine and creative toolchain for building AAA-style games directly on Android.

This file is a high-level roadmap, not a locked implementation schedule. Complex renderer, UDS, VFX, asset-format, editor, and optimization stages must be discussed, prototyped, measured, and refined before they become exact patch tasks.

Current working architecture notes:

- [`docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md`](docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md)
- [`docs/runtime/SOLUM_SLPK_PRISM_FLUX_GOVERNOR.md`](docs/runtime/SOLUM_SLPK_PRISM_FLUX_GOVERNOR.md)
- [`tasks/P65_SLPK_PRISM_FLUX_SAFE_SPIKE.md`](tasks/P65_SLPK_PRISM_FLUX_SAFE_SPIKE.md)
- [`docs/CODEX_MODEL_SELECTION.md`](docs/CODEX_MODEL_SELECTION.md)

The runtime-format document separates implemented foundations, local experiments, simulations, project-required work, device-required work, and negative results. The P65 task is the safe Codex entry point for testing one real exact asset path without enabling several optimizations at once.

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

- Filament as primary renderer and maintained extension/fork point.
- GLB/glTF import and preview.
- HDR/IBL environment loading.
- Render Control Center.
- Camera and model transform tools.
- Material/glass/debug tabs.
- Honest FPS, frame time, jank, rendered/presented frame, and runtime diagnostics.
- Internal renderer/pass telemetry where supported.
- Mobile-safe defaults.

Rule: SSR is manual-only and must never be part of default presets, not even Ultra.

## Stage 3 — UDS/UDW system truth and environment lighting

- Reconstruct UDS/UDW controls, Blueprint/Kismet paths, curves, materials, MPCs, textures, Niagara, audio, weather transitions, and runtime capability status.
- Keep map/geometry reconstruction separate from system reconstruction.
- Separate visible sky, direct sun/moon lights, environmental lighting, and local reflections.
- Explore UDS-driven HDR environment capture, diffuse irradiance, and prefiltered specular IBL for Filament.
- Build mobile quality/update strategies before enabling dynamic capture broadly.

## Stage 4 — Material and asset tooling

- Material slot inspector.
- Material override system.
- Master material preset system.
- Texture library rules.
- KTX2/Basis/ASTC compression workflow.
- ORM packing and deduplication.
- Asset manifest system.
- Refine SLPK as a package, provenance, dependency, and streaming layer.
- Validate one raw/passthrough exact SLPK path before adding compression, paging, or adaptive runtime behavior.
- Preserve standard payload formats and use SLPK as the versioned package and streaming layer rather than replacing every codec.

## Stage 5 — Procedural Asset Economy and perceptual rendering

Goal: make large worlds from small high-quality bases and spend quality where it is visible.

- Reusable texture sets.
- Reusable modular meshes.
- Master materials.
- Masks for dirt, wetness, moss, scratches, cracks, snow, burn, blood, and color variation.
- Prop variation system.
- Modular building generator.
- NPC modular generator.
- Mask painter foundation.
- LOD and collision generation.
- Research cluster/meshlet visibility, cluster LOD, HZB occlusion, streaming, and attention-based shading/update quality.
- Add Prism and Flux modules one at a time behind flags, beginning in telemetry-only or `OBSERVE` state.
- Keep `STRICT_EXACT`, `DISTRIBUTION_EXACT`, and `BOUNDED_PERCEPTUAL` as separate quality contracts.
- Do not claim optimization gains before honest renderer telemetry is validated.
- Do not combine independent microbenchmark multipliers into a total-engine speed claim.

## Stage 6 — Physical Material System

Goal: materials should define behavior, not only appearance.

- Physical material database.
- Density, hardness, friction, elasticity, water absorption, flammability, sound profile, and break mode.
- Object construction profiles: solid, hollow, shell, porous, sealed, open, filled, leaking.
- Buoyancy and floating/sinking prototypes.
- Impact energy, dent, crack, splinter, shatter, and sound routing.

## Stage 7 — VFX and world reaction

- SOLUM-owned VFX intermediate representation.
- Systems, emitters, ordered modules, parameters, events, and renderer adapters.
- Filament VFX adapter.
- Magic impact VFX.
- Terrain impact and crater events.
- Grass, mud, snow, sand, water, and prop reactions.
- Water/liquid master system.
- Weather and atmosphere integration.
- Niagara/UE source may inform behavior and serialization research, but SOLUM implementation must remain independent and license-safe.

## Stage 8 — Gameplay/editor showcase

- Project and asset browser.
- Scene hierarchy and inspector.
- Selection, gizmo, undo/redo, save/load, and validation.
- Material, lighting, post-process, VFX, landscape, and world tools in verified slices.
- Playable showcase scene.
- Character movement and camera.
- Interactive props.
- Transport or mount prototype.
- Spatial inventory concept prototype.
- World reaction demo.
- Android performance profiles.

## Long-term product vision

SOLUM should become a practical open-source mobile creative platform for learning engine development, building mobile-first game prototypes, experimenting with assets/materials/VFX, and maintaining advanced Android creative tooling.

Before an exact milestone begins:

1. discuss the expected visible result and scope;
2. inspect current code, prototypes, and evidence;
3. define benchmarks and verification;
4. select the Codex model/reasoning level using `docs/CODEX_MODEL_SELECTION.md`;
5. write a bounded task with explicit out-of-scope items and stop conditions.
