# SOLUM Master Roadmap After Filament

## Strategic state

Filament is primary renderer.
Legacy Vulkan renderer is deprecated.
SOLUM must become:
- mobile AAA renderer/toolkit;
- procedural asset economy system;
- physical material simulation system;
- Niagara-like VFX framework;
- world reaction engine.

## Immediate renderer/tools path

P45 — Runtime Truth + Performance Defaults
- honest FPS/jank/p95/worst frame;
- SSR manual-only, never default, not even Ultra;
- config save/load verification;
- profiling status.

P46 — Legacy Native Vulkan Purge Audit
- remove or isolate remaining native Vulkan renderer;
- keep only future custom pass foundations if needed.

P47 — Material Slot Inspector
- full slot/material/texture/extension inspector.

P48 — Material Override + Glass Tools
- selected slot overrides only;
- author material default;
- glass/clearcoat/sheeen/transmission/ior tools.

P49 — Scene Stage / Ground / Preview Environment
- ground, shadow catcher, scene presets, camera bookmarks.

P50 — Sky / Sun / Moon / Stars / Time of Day
- sky, sun disc, moon, stars, time slider, sunrise/sunset/night.

P51 — Post Process / Cinematic Controls
- tone mapping, color modes, safe bloom, fog, atmosphere controls.

P52 — GLB Animation Support
- clips, play/pause/loop/speed, skeleton/morph status.

P53 — Asset Workspace / Library
- asset browser, imported model/IBL/texture lists, thumbnails/metadata.

## Asset economy path

P54 — Master Material Preset System
- master materials and presets.

P55 — Texture Library + Compression Rules
- KTX2/Basis/ASTC/ORM packing/atlas rules.

P56 — Asset Manifest System
- reusable asset metadata, variation ranges.

P57 — Prop Procedural Variation
- seed-based transform/material/mask variation.

P58 — Modular Building Generator Foundation
- parts, facade rules, roofs, windows, damage/moss/dirt.

P59 — Mask Painter / Substance-lite Foundation
- dirt/wetness/moss/scratch/crack masks.

P60 — NPC Modular Generator Foundation
- body/head/hair/clothes/walk/voice/personality seeds.

## Physical material path

P61 — Physical Material Database
- density, hardness, friction, buoyancy, sound, break modes.

P62 — Object Construction Profiles
- solid/hollow/shell/porous/sealed/open/fill/leak/displacement.

P63 — Buoyancy / Floating / Sinking Prototype
- crate/barrel/glass/stone/water tests.

P64 — Impact / Damage / Sound Routing
- energy thresholds, crack/dent/shatter/splinter/sound.

## VFX / world path

P65 — VFX Architecture Audit
- WickedEngine GPU particles as main architecture reference;
- Effekseer as authoring/runtime candidate;
- Sascha computeparticles minimal reference.

P66 — SOLUM VFX Core Runtime
- emitter/modules/curves/events/budget/pooling.

P67 — Filament VFX Adapter
- billboards, mesh VFX, ribbons, transparent/emissive materials.

P68 — AAA Magic VFX Pack v1
- shockwave, trails, sparks, smoke, decals, light pulse.

P69 — Weather / Atmosphere
- rain/snow/fog/cloud layer/lightning/wind.

P70 — Water / Liquid Master System
- water material, ripples, foam, splash hooks.

P71 — Reactive World Foundation
- footsteps, impacts, decals, grass/mud/snow/sand/water response.

## Product rule

Every major system must produce a visible demo.

SOLUM is not only code.
SOLUM is a product and proof of capability.
