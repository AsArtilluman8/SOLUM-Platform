# SOLUM / UDS / Renderer Working Plan

Status: **working direction, not a locked roadmap**.

This document records the current architecture discussion so it is not lost between chats. It must not be converted into a rigid implementation sequence without additional discussion, source inspection, prototypes, benchmarks, and Android device tests.

## Why this document exists

SOLUM already has:

- a Filament-based renderer path and a maintained Filament fork direction;
- GLB/glTF preview and render controls;
- a first SLPK/custom-package route;
- UDS/UDW extraction work;
- prototypes for sub-object/cluster visibility and mobile-oriented quality control;
- early environment, material, post-process, lighting, and diagnostics tooling.

The next risk is not lack of ideas. It is stacking systems on top of measurements, contracts, or renderer assumptions that are not yet trustworthy.

## Stable principles

1. **Measure before optimizing.**
2. **Real runtime/data truth outranks a convincing demo.**
3. **UDS is primarily a weather/time/atmosphere system, not a geometry-extraction project.**
4. **Visible sky, environmental lighting, direct lights, and local reflections are separate systems.**
5. **Filament is a foundation and extension point, not an untouchable black box.**
6. **SLPK should package, stream, deduplicate, and preserve provenance rather than replace every standard asset format.**
7. **Complex phases must be discussed and tested before a detailed Codex task is issued.**
8. **Android/Mali limits are architectural inputs, not late optimization concerns.**
9. **No fake FPS, fake sky, fake material graph, fake Niagara runtime, or fake completion status.**
10. **Each Codex task must state the recommended model and reasoning effort using `docs/CODEX_MODEL_SELECTION.md`.**

# Working areas

The order below is provisional. Some areas may run as small research spikes in parallel, but no large implementation should begin before its dependency and measurement requirements are understood.

## A. P62 — UDS/UDW System Truth

The next planned UDS phase should focus on the actual system:

- exposed variables and defaults;
- Blueprint/Kismet functions and state transitions;
- curves and timelines;
- sun and moon control;
- SkyAtmosphere, VolumetricCloud, fog, lights, skylight, and post-process bindings;
- materials, material functions, MICs, MPCs, textures, volume textures, and LUTs;
- Niagara systems, emitters, parameters, and runtime gaps;
- rain, snow, dust, wind, lightning, wetness, and weather transitions;
- audio components, sounds, loops, mixes, and trigger paths;
- capability classification for SOLUM.

Map/geometry reconstruction should remain a separate gate. A failed map gate must not block extraction of verified time/weather/material/audio logic.

Possible contracts include time, sun/moon, atmosphere, clouds, fog, weather transitions, precipitation, wind, lightning, wetness, and audio. Exact schemas must be designed from evidence, not from guesses.

## B. Honest Filament Frame Telemetry

Current FPS must not be accepted as truthful when the scene visibly behaves like 8-16 FPS while the HUD reports around 60.

The telemetry layer should distinguish:

- display/presented FPS;
- rendered/accepted FPS;
- skipped `beginFrame` calls and reasons;
- CPU update, culling, render-graph, submission, and UI overhead;
- GPU total frame time when supported;
- per-pass GPU timings where supported;
- p50, p95, p99, worst frame, 1% low, jitter, and jank;
- dynamic-resolution scale;
- thermal state and throttling indicators;
- frame IDs/timestamps that allow cross-checking with Android FrameTimeline/framestats.

The forked Filament path should be evaluated for internal hooks around:

```text
beginFrame
render graph
shadow/depth/color/AO/SSR/post passes
command submission
GPU completion
overlay/HUD update
swap/present
```

Where GPU timestamps are unavailable, the UI must show `UNAVAILABLE`, not substitute CPU time and label it GPU time.

Validation should include controlled CPU stalls, GPU-heavy materials/passes, SSR on/off, render-scale changes, and external Android frame statistics.

No optimization project may claim a speedup until this layer is trusted.

## C. Dynamic Environment Lighting and UDS Reflections

A sky sphere controls what the camera sees. It does not automatically create IBL.

SOLUM should separate:

```text
SkyVisualState
EnvironmentLightingState
DirectCelestialLights
ReflectionState
```

Working pipeline:

```text
Time / weather state
→ atmosphere + cloud + sun + moon visual state
→ HDR cubemap capture or generated environment
→ diffuse irradiance
→ prefiltered specular mip chain
→ Filament IndirectLight
→ PBR materials / metal / glass / water
```

Sun and moon should also remain direct directional lights for illumination, highlights, and shadows. The environment contributes ambient lighting and reflected sky/cloud/horizon energy.

Mobile strategies to investigate:

- prepared IBL states and blending;
- threshold-driven updates;
- lower-resolution captures;
- incremental cubemap-face updates;
- adaptive update frequency;
- separate quality tiers;
- high-quality manual mode;
- local reflection probes;
- SSR only for selective nearby reflections, with IBL/probes as fallback.

A dynamic UDS-derived HDR environment should aim to preserve the photographic quality previously seen with HDR IBL while allowing day/night and weather to change.

## D. SLPK v2 and Asset Streaming

Do not discard SLPK, but refine its responsibility.

Preferred role:

- package manifest;
- dependency graph;
- content-addressed chunks;
- deduplication;
- provenance and hashes;
- scene nodes;
- platform variants;
- streaming priority;
- cluster/LOD metadata;
- material/VFX/physics contracts;
- original and derived asset relationships.

Prefer established payloads where practical:

- GLB/glTF for interchange and source geometry;
- KTX2/Basis/ASTC for textures;
- suitable standard audio payloads;
- JSON/CBOR or another reviewed schema for contracts;
- explicit SOLUM cluster streams only where standard formats are insufficient.

Questions to settle through tests:

- random access and partial loading;
- compression and decompression cost on Android;
- patchability;
- deduplication granularity;
- platform-specific texture/material variants;
- source-vs-cooked provenance;
- whether cluster data belongs beside or inside GLB payloads.

## E. Perceptual Cluster Renderer

Earlier prototypes reportedly improved performance by dividing objects into chunks/clusters and hiding only invisible parts rather than removing the entire object like ordinary object-level LOD/culling.

This direction should be investigated as a mobile-first SOLUM renderer rather than described as full Nanite.

Potential layers:

### Visibility

- object frustum culling;
- cluster frustum culling;
- backface-cone culling;
- HZB occlusion;
- portal/room culling;
- distance culling for minor objects.

### Geometry detail

- cluster LOD;
- screen-space error;
- progressive geometry selection;
- streaming by visibility and importance.

### Shading and update quality

For distant, peripheral, occluded, or out-of-focus content, gradually reduce:

- material complexity;
- normal/detail frequency;
- shadow resolution/update rate;
- reflection update rate;
- contact shadows;
- light shadow eligibility;
- animation update frequency;
- VFX simulation/spawn/update rate;
- physics update detail where safe.

Transitions must use hysteresis, temporal smoothing, minimum dwell time, gradual blending, and quality locks after fast camera movement so changes are not obvious.

The cluster size must be benchmarked. Too many tiny clusters can lose performance through draw calls, metadata, driver cost, poor cache locality, and excessive culling work.

## F. SOLUM VFX Core and Editor

The goal is not to copy Niagara source code or lock SOLUM to a third-party runtime.

Preferred architecture:

```text
SolumVfxSystem
→ emitters
→ ordered module stacks
→ typed parameters
→ events/data interfaces
→ sprite/mesh/ribbon/decal/light renderers
→ CPU/GPU/Filament adapters
```

Initial module families may include spawn rate, bursts, lifetime, position, velocity, gravity, drag, noise, color/size/rotation over life, sprite/mesh/ribbon rendering, collisions, decals, light pulses, event spawning, and camera-distance scaling.

Use Unreal/Niagara source as behavior, serialization, and architecture reference only. SOLUM implementation must remain independent and license-safe.

Any external candidate runtime should be evaluated as an adapter behind a SOLUM-owned intermediate representation.

The editor should favor clear modules, exposed parameters, timeline/preview, profiling, reusable templates, and mobile-friendly controls rather than reproducing Niagara's complexity blindly.

## G. Core Editor and Tools

Build working editor capabilities in validated slices:

- project browser;
- asset browser and folders;
- scene hierarchy;
- selection and inspector;
- transform gizmo;
- undo/redo;
- save/load/validation;
- adding/spawning objects;
- lighting controls and multiple lights;
- post-process settings;
- Material Lab;
- VFX editor;
- Landscape editor;
- world generation tools;
- build/performance panel.

Do not create empty decorative tabs. Each tool must own real state and produce a verifiable project/runtime change.

## H. World Systems and Physical Interaction

Later systems include:

- water and liquids;
- wind;
- terrain and landscape;
- physical materials;
- buoyancy;
- impacts and damage responses;
- decals and impact VFX;
- mud, snow, sand, grass, and wetness reactions;
- object construction/breakage;
- audio routing from physical interactions.

These systems should consume shared physical-material and event contracts rather than becoming isolated effects.

# Texture and Audio Improvement Policy

Future asset enhancement is permitted, but original extracted truth must remain intact.

Use separate roots such as:

```text
original_verified/
enhanced_derived/
```

Every derived asset should record:

- source path and SHA-256;
- semantic role;
- operation/tool/model;
- prompt or deterministic settings;
- output path and SHA-256;
- review status;
- whether the asset is artistic or technical.

Artistic image-to-image work may be appropriate for moon imagery, stars, decorative sky textures, and understood albedo content.

Do not blindly regenerate:

- normal maps;
- roughness/metallic/AO masks;
- opacity or packed channel maps;
- LUTs;
- flow maps;
- heightmaps;
- volume textures;
- lookup/noise textures whose mathematical role is not understood.

Audio remastering must preserve loop points, duration, channels, transitions, and runtime bindings.

# Open questions before exact tasks

The following require more discussion and evidence:

- which Filament fork hooks are currently available and stable;
- which Android devices expose reliable Vulkan GPU timestamp queries;
- how to measure actual presented frames in the current Surface/ModelViewer path;
- how complete P62 can make Blueprint/Kismet/material/Niagara control paths;
- whether UDS cloud output can be captured into environment lighting efficiently;
- acceptable dynamic-cubemap resolution and update cadence on Mali-G57;
- the correct SLPK v2 chunk and provenance model;
- optimal cluster size and whether Filament draw submission needs deeper modification;
- which VFX third-party candidate was previously selected and whether it still fits;
- which systems belong in the Filament fork versus SOLUM-owned layers above it;
- how to create repeatable reference scenes and visual regression captures.

# Provisional milestone names

These names are memory anchors only, not authorized implementation tasks:

```text
P62 — UDS/UDW System Truth
P63 — Honest Filament Frame Telemetry
P64 — Dynamic Environment Lighting / UDS IBL
P65 — SLPK v2 and Streaming Contract
P66 — Perceptual Cluster Renderer Research
P67 — SOLUM VFX Core Architecture
P68+ — Editor and Reactive World vertical slices
```

Before any milestone begins:

1. discuss the exact goal and expected visible result;
2. inspect the current repository and prior prototypes;
3. define evidence and benchmark scenes;
4. choose the Codex model using `docs/CODEX_MODEL_SELECTION.md`;
5. write a bounded task with explicit out-of-scope items and stop conditions.
