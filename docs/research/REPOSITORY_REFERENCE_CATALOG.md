# REPOSITORY_REFERENCE_CATALOG — проверенные repo/docs для SOLUM

Цель файла: не изобретать сложные системы из фантазии GPT/Claude, а перед каждым крупным патчем смотреть проверенные реализации, архитектуру и правила.

Перед сложным патчем читать также:

- `docs/research/RESEARCH_GATE_RULES.md`
- `docs/research/PATCH_RESEARCH_TEMPLATE.md`
- `docs/DEPENDENCY_AND_LICENSE_POLICY.md`

## Процесс

```text
сложная задача
↓
найти 2–5 repo/docs
↓
изучить подход
↓
решить: REFERENCE_ONLY / SMALL_SLICE / ADAPTER / DEPENDENCY / REJECT
↓
Research Summary
↓
выбор пользователя
↓
SOLUM-compatible patch
↓
diagnostics/test
```

Запрещено:

```text
GPT сам придумывает CSM/material/water/VFX систему без сверки с reference.
```

---

## 1. Vulkan foundation / Android Vulkan

### android/ndk-samples

**Use for:** Android Vulkan app skeleton, native activity patterns, swapchain basics, Android build/reference.

**Когда смотреть:** Patch 02 Vulkan capability check, Patch 04 Vulkan Foundation, Android surface/swapchain, lifecycle.

### KhronosGroup/Vulkan-Samples

**Use for:** Vulkan best practices, swapchain, render pass, descriptors, synchronization, pipeline setup.

**Когда смотреть:** Vulkan Foundation, descriptor/pipeline layout, render pass/synchronization, texture/shader loading.

### SaschaWillems/Vulkan

**Use for:** practical Vulkan examples: PBR/IBL, CSM, glTF skinning, deferred rendering, SSAO, bloom, compute particles.

**Когда смотреть:** any Vulkan feature patch where an example exists.

### ARM-software/vulkan_best_practice_for_mobile_developers

**Use for:** official ARM/Mali Vulkan performance guidance.

**Когда смотреть:** before Vulkan renderer, shadows, post-process, terrain, water, particles, any optimization.

**Critical Mali topics:** tile-based rendering, load/store ops, bandwidth, render pass design, subpasses, FP16/mediump.

---

## 2. Renderer architecture / frame graph / resource lifetime

### ConfettiFX/The-Forge

**Use for:** renderer abstraction, resource lifetime, descriptor management, barriers, frame graph/render graph ideas.

**Когда смотреть:** render-core design, descriptor strategy, resource lifetime, barriers, CSM architecture, multi-pass renderer.

**Rule:** The Forge does not replace SOLUM. Use architecture/slices/adapters only if justified.

### google/filament

**Use for:** mobile PBR/material model, material system concepts, tone mapping, IBL, renderer architecture.

**Когда смотреть:** Material Studio, PBR/toon, lighting model, exposure, reflections/IBL, material preview.

### bgfx/bgfx

**Use for:** renderer abstraction, handles, cross-platform graphics API design.

### skaarj1989/FrameGraph

**Use for:** Vulkan frame graph ideas, resource transitions, transient resources.

### zeux/niagara

**Use for:** Vulkan renderer with GPU culling/occlusion ideas.

---

## 3. PBR / Material system / IBL

### SaschaWillems/Vulkan-glTF-PBR

**Use for:** focused Vulkan glTF 2.0 PBR example, Cook-Torrance BRDF, IBL, metallic/roughness workflow.

**Когда смотреть:** Material Studio v1+, PBR material renderer, glTF material compatibility.

### google/filament

**Use for:** production-grade mobile PBR model, tone mapping, IBL lookups, material system concepts.

### Khronos glTF material specs

**Use for:** metallic-roughness workflow, texture slots, normal/ORM maps, alpha modes.

### KhronosGroup/glTF-IBL-Sampler

**Use for:** HDR panorama → KTX2 cubemap with prefiltered mips + BRDF LUT.

### derkreature/IBLBaker

**Use for:** baking diffuse irradiance and specular prefiltered cubemaps.

---

## 4. Shadows / CSM / lighting

### SaschaWillems/Vulkan/examples/shadowmappingcascade

**Use for:** Cascaded Shadow Maps on Vulkan: layered depth texture, frustum splits, PCF.

**Когда смотреть:** any ShadowSystem/CSM patch.

### diharaw/cascaded-shadow-maps

**Use for:** clean CSM/PSSM implementation, stable cascades, PCF concepts.

### Filament docs/code

**Use for:** mobile lighting and shadow quality/performance tradeoffs.

### ARM Mali docs / best practice

**Use for:** bandwidth, tile-based render pass design, mobile shadow pass constraints.

**Research Gate requirement:** Shadow patch must check at least one CSM implementation + one Mali/ARM performance reference.

---

## 5. Sky / atmosphere / sun

### andrewwillmott/sun-sky

**Use for:** Preetham and Hosek-Wilkie sky models, sun disc, skybox shader.

### wwwtyro/glsl-atmosphere

**Use for:** simple Rayleigh + Mie GLSL atmosphere.

### ebruneton/precomputed_atmospheric_scattering

**Use for:** physically accurate precomputed atmosphere. Likely reference only early.

### diharaw/sky-models

**Use for:** compare Bruneton, Preetham, Hosek-Wilkie.

---

## 6. Post-processing / anti-aliasing

### SaschaWillems Vulkan bloom/HDR examples

**Use for:** bloom/HDR post pipeline examples.

### iryoku/smaa

**Use for:** SMAA shader reference.

### FrameGraph references

**Use for:** organizing post passes and barriers.

---

## 7. Asset pipeline / import/export / textures

### syoyo/tinygltf

**Use for:** lightweight glTF 2.0 import/export, meshes, materials, animations, skins.

### KhronosGroup/glTF-Sample-Models

**Use for:** test assets for importer/materials/renderer smoke tests.

### zeux/meshoptimizer

**Use for:** mesh simplification, vertex/index cache optimization, overdraw reduction, LOD generation.

### KhronosGroup/KTX-Software

**Use for:** KTX2 texture format and streaming.

### BinomialLLC/basis_universal

**Use for:** Basis Universal / KTX2 compression and transcoding to ASTC/ETC2.

### ARM-software/astc-encoder

**Use for:** ASTC texture compression for Mali.

### assimp/assimp

**Use for:** broad model import reference. Heavy; avoid early core dependency unless justified.

---

## 8. Vulkan utilities / shader tools

### GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator

**Use for:** Vulkan memory allocation. Header-only, MIT.

### zeux/volk

**Use for:** Vulkan function loader, dynamic loading.

### KhronosGroup/glslang

**Use for:** GLSL → SPIR-V compilation.

### KhronosGroup/SPIRV-Tools

**Use for:** spirv-opt, spirv-val, spirv-dis.

### KhronosGroup/SPIRV-Cross

**Use for:** SPIR-V reflection/cross-compile analysis.

---

## 9. Math / base utilities

### g-truc/glm

**Use for:** vec/mat/quat/frustum math.

### Auburn/FastNoiseLite

**Use for:** terrain heightmaps, clouds, texture variation.

### nothings/stb

**Use for:** image/font loading.

### nlohmann/json

**Use for:** JSON configs/manifests in C++.

### taskflow/taskflow

**Use for:** future task graph/background loading.

---

## 10. Terrain / world generation / architecture

### Jaysmito101/TerraForge3D

**Use for:** node-based procedural terrain, erosion, GLSL export ideas.

### AntonHakansson/procedural-terrain

**Use for:** terrain with PBR, CSM, SSR, normal maps.

### fegennari/3DWorld

**Use for:** open-world systems: terrain, cities, buildings, vegetation, rain, day/night.

### pvallet/CGA_interpreter

**Use for:** CGA grammar procedural buildings.

---

## 11. Water / ocean

### kentril0/WaterSurfaceRendering

**Use for:** Vulkan FFT water, Tessendorf, displacement+normal map via compute, PBR water shading.

### deiss/fftocean

**Use for:** Tessendorf/Phillips spectrum concepts.

### Themaister/GLFFT

**Use for:** GPU FFT, FP16 support relevant to Mali.

### ARM Ocean FFT sample

**Use for:** official ARM/Mali ocean FFT optimization ideas.

---

## 12. Vegetation / foliage / grass / hair

### nickmcd/VulkanGrassRendering

**Use for:** responsive real-time grass, Bezier grass, compute culling, wind.

### Twinklebear/Vulkan-Forest-Rendering-Engine

**Use for:** Vulkan forest rendering, GPU instancing, billboard LOD, wind.

### GPUOpen-Effects/TressFX

**Use for:** GPU hair/fur simulation reference.

---

## 13. VFX / particles / volumetrics

### SaschaWillems/Vulkan/examples/computeparticles

**Use for:** GPU particles through compute shaders, SSBO data.

### turanszkij/WickedEngine

**Use for:** AAA-like GPU particle system principles.

### effekseer/Effekseer

**Use for:** VFX editor/runtime architecture.

### AmanSachan1/Meteoros

**Use for:** Vulkan volumetric clouds/god rays references.

### Erkaman/glsl-godrays

**Use for:** god rays radial blur technique.

### diharaw/volumetric-clouds

**Use for:** volumetric clouds/ray marching/compute ideas.

---

## 14. Animation / skeleton / character runtime

### guillaumeblanc/ozz-animation

**Use for:** skeletal animation runtime, blending, sampling, IK, GPU skinning pipeline.

### SaschaWillems/Vulkan/examples/gltfskinning

**Use for:** glTF skinning through Vulkan.

### glTF skinning/animation spec

**Use for:** animation import/export compatibility.

### sebastianstarke/AI4Animation

**Use for:** future ML/advanced locomotion concepts.

---

## 15. Character generation / authoring

### makehumancommunity/makehuman

**Use for:** human generator concepts, morph targets, character parameter model.

### makehumancommunity/mpfb2

**Use for:** MakeHuman/Blender pipeline, rigging, glTF export concepts.

### ReadyPlayerMe SDK concepts

**Use for:** avatar customization, glTF avatar pipeline, blend shapes.

---

## 16. ECS / mechanics / gameplay architecture

### skypjack/entt

**Use for:** lightweight C++ ECS.

### SanderMertens/flecs

**Use for:** richer ECS with queries/prefabs/hierarchies.

### Unreal Gameplay Ability System concepts/docs

**Use for:** abilities, cooldowns, tags, effects, status states, ARPG skills.

---

## 17. Physics / navigation

### jrouwe/JoltPhysics

**Use for:** modern physics, Android/ARM potential, character controllers later.

### bulletphysics/bullet3

**Use for:** older stable physics reference.

### recastnavigation/recastnavigation

**Use for:** Recast/Detour navmesh and pathfinding.

---

## 18. Sound

### mackron/miniaudio

**Use for:** single-file C audio, WAV/MP3/OGG, 3D positional audio, Android.

### jarikomppa/soloud

**Use for:** fuller audio engine, 3D audio, reverb, filters.

---

## 19. UI / HUD / node graph / reports

### ocornut/imgui

**Use for:** debug/editor UI concepts, Vulkan backend, sliders, panels.

**Risk:** Android touch UX must be adapted; do not make desktop ImGui UI as final mobile UX.

### Nelarius/imnodes

**Use for:** node editor concepts.

### epezent/implot

**Use for:** profiling graphs.

### Shapr3D / Procreate / DaVinci Resolve iPad / GarageBand

**Use for:** mobile-first professional editor UX patterns.

---

## 20. Diagnostics / profiling / performance

### wolfpld/tracy

**Use for:** CPU/GPU frame profiler concepts.

### google/perfetto

**Use for:** Android system profiling.

### Android GPU Inspector

**Use for:** GPU frame analysis.

### ARM Mali Offline Compiler

**Use for:** SPIR-V shader cycle estimates on Mali.

### KhronosGroup/Vulkan-ValidationLayers

**Use for:** Vulkan API validation in debug builds.

---

## 21. Mega reference lists

### vinjn/awesome-vulkan

**Use for:** navigation through Vulkan ecosystem.

### Gforcex/OpenGraphic

**Use for:** graphics techniques index.

### Caerind/AwesomeCppGameDev

**Use for:** C++ gamedev library discovery.

---

## Mali-G57 MC2 rules

Required considerations:

- correct render pass load/store ops;
- prefer DONT_CARE when contents are not needed;
- avoid unnecessary full-screen passes;
- use ASTC/KTX2 compressed textures later;
- consider FP16/mediump where precision allows;
- avoid branch-heavy monolithic shaders;
- validation/debug layers only in debug;
- profile shader cost before complex effects;
- diagnostics must not sample expensive metrics every frame.

## Stage-to-reference map

```text
P02 Diagnostics + Vulkan Caps:
  android/ndk-samples, ARM Vulkan Best Practice, Perfetto docs

P04 Vulkan Foundation:
  android/ndk-samples, Khronos Vulkan Samples, SaschaWillems/Vulkan, VMA, volk

Material Studio / PBR:
  SaschaWillems/Vulkan-glTF-PBR, Filament, glTF spec, glTF-IBL-Sampler

Shadows / CSM:
  SaschaWillems shadowmappingcascade, diharaw/cascaded-shadow-maps, ARM Mali guide

Sky / atmosphere:
  sun-sky, glsl-atmosphere, sky-models

Terrain / World:
  TerraForge3D, procedural-terrain, 3DWorld, FastNoiseLite, CGA_interpreter

Water:
  WaterSurfaceRendering, GLFFT, ARM Ocean FFT, fftocean

VFX:
  Sascha compute particles, WickedEngine, Effekseer

Animation:
  ozz-animation, Sascha gltfskinning, glTF animation spec

ECS / Mechanics:
  EnTT, Flecs, GAS concepts

Diagnostics / Profiling:
  Tracy, Perfetto, AGI, Mali Offline Compiler
```

## Agent rule

Before any major patch in these areas, agent must check this catalog and produce Research Summary:

- Vulkan renderer;
- shadows/CSM;
- material system;
- water/terrain;
- animation;
- VFX;
- asset pipeline;
- ECS/mechanics;
- diagnostics/profiling.

If agent does not check relevant references, patch is incomplete.
