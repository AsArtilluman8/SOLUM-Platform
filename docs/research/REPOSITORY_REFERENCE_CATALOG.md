# REPOSITORY_REFERENCE_CATALOG — проверенные repo/docs для SOLUM

Цель файла: не изобретать сложные системы из фантазии GPT/Claude, а перед каждым крупным патчем смотреть проверенные реализации, архитектуру и правила.

Важно: этот файл не означает “слепо импортировать всё”.

Правильный процесс:

```text
есть сложная задача
↓
открыть этот catalog
↓
найти релевантные repo/docs
↓
изучить нужный модуль/пример
↓
выписать принцип
↓
решить: reference / slice / adapter / dependency / reject
↓
записать решение в ADR или research note
↓
реализовать SOLUM-compatible версию
↓
добавить diagnostics/test
```

Запрещено:

```text
GPT сам придумывает CSM/material/water/VFX систему без сверки с reference.
```

---

## 1. Vulkan foundation / Android Vulkan

### android/ndk-samples

**Use for:** Android Vulkan app skeleton, native activity patterns, swapchain basics, Android build/reference.

**Когда смотреть:**

- Patch 02 Vulkan capability check;
- Patch 04 Vulkan Foundation;
- Android surface/swapchain problems;
- NDK/CMake Android examples.

**Как использовать:**

- reference only / small slice;
- не копировать весь sample как architecture;
- проверить Android-specific lifecycle.

### KhronosGroup/Vulkan-Samples

**Use for:** Vulkan best practices, swapchain, render pass, descriptors, synchronization, pipeline setup.

**Когда смотреть:**

- Vulkan Foundation;
- descriptor/pipeline layout;
- render pass/synchronization;
- texture/shader loading;
- future shadow/material systems.

**Риск:** desktop/general Vulkan patterns могут быть тяжелее, чем нужно для Mali. Адаптировать под mobile.

---

## 2. Renderer architecture / frame graph / resource lifetime

### ConfettiFX/The-Forge

**Use for:** renderer abstraction, resource lifetime, descriptor management, barriers, frame graph/render graph ideas.

**Когда смотреть:**

- render-core design;
- descriptor set strategy;
- resource lifetime;
- barrier/sync problems;
- CSM/render pass architecture;
- multi-platform renderer design decisions.

**Правило:** The Forge не заменяет SOLUM. Использовать как architecture reference, slice/adapter only if justified.

### google/filament

**Use for:** mobile PBR/material model, material system concepts, physically-based rendering, mobile renderer ideas.

**Когда смотреть:**

- Material Studio;
- PBR/toon material design;
- lighting model;
- tone mapping/exposure;
- reflections/IBL later.

**Правило:** изучать material model and architecture, не тащить весь Filament.

### bgfx/bgfx

**Use for:** renderer abstraction, cross-platform graphics API design, resource handles.

**Когда смотреть:**

- renderer abstraction discussion;
- handle/resource API design;
- future multi-backend thinking.

**Правило:** reference only. SOLUM target remains Android Vulkan.

### GPUOpen-LibrariesAndSDKs / Cauldron-style references

**Use for:** render graph/frame graph/resource transition ideas, sample framework patterns.

**Когда смотреть:**

- render graph planning;
- post-process chain;
- multi-pass systems.

---

## 3. Shadows / CSM / lighting

### Khronos Vulkan Samples — shadow mapping / cascaded shadows samples if available

**Use for:** Vulkan shadow map setup, depth pass, descriptors, sampler comparison references.

**Когда смотреть:**

- first ShadowSystem;
- CSM v1;
- depth image layout/sampling issues.

### The Forge examples / renderer code

**Use for:** pass ordering, shadow atlas/resource lifetime, descriptor/bindless patterns if applicable.

**Когда смотреть:**

- CSM architecture;
- shadow atlas;
- render graph integration.

### Filament docs/code

**Use for:** mobile lighting/material response, shadow quality/performance tradeoffs.

**Когда смотреть:**

- directional light;
- cascaded shadow stability ideas;
- mobile-friendly lighting choices.

### Arm Mali docs / performance guides

**Use for:** mobile tile-based GPU rules, bandwidth, render pass load/store, overdraw, shader cost.

**Когда смотреть:**

- before CSM;
- before expensive post-process;
- before volumetrics;
- before terrain/material-heavy scenes.

**Особое правило:** если shadow/material FPS просел, сначала смотреть Mali constraints + diagnostics, а не гадать.

---

## 4. Materials / PBR / toon / textures

### google/filament

**Use for:** PBR model, material definitions, mobile lighting/material response.

**Когда смотреть:**

- Material Studio v1;
- MaterialDocument fields;
- material preview lighting;
- future IBL/reflection/tone mapping.

### Khronos glTF material specs

**Use for:** compatible material properties, metallic-roughness workflow, texture slots, normal/ORM maps.

**Когда смотреть:**

- Asset Schema material fields;
- glTF import/export;
- Material Studio schema.

### KTX-Software / BasisU

**Use for:** KTX2, Basis Universal, compressed texture pipeline.

**Когда смотреть:**

- texture import;
- mobile memory optimization;
- material asset pipeline.

### zeux/meshoptimizer

**Use for:** mesh simplification/optimization, vertex/index cache optimization.

**Когда смотреть:**

- glTF import;
- asset optimization;
- world/character geometry pipeline.

---

## 5. Asset pipeline / import/export

### syoyo/tinygltf

**Use for:** glTF import/export parser, lightweight C++ asset import.

**Когда смотреть:**

- character/object import;
- material import;
- mesh import;
- GLB export compatibility.

### KhronosGroup/glTF-Sample-Models

**Use for:** test assets for importer/materials.

**Когда смотреть:**

- asset schema tests;
- material preview validation;
- renderer smoke tests.

### assimp/assimp

**Use for:** broad model import reference.

**Риск:** heavy dependency, may be too big for Termux/mobile start.

**Use mode:** reference / optional later, not early core dependency.

---

## 6. Animation / skeleton / motion

### guillaumeblanc/ozz-animation

**Use for:** skeletal animation runtime, animation clips, sampling, blending.

**Когда смотреть:**

- Motion Studio;
- Character animation runtime;
- animation asset schema;
- clip blending.

### glTF skinning/animation spec

**Use for:** animation import/export compatibility.

**Когда смотреть:**

- Character Studio;
- Motion Studio;
- GLB import/export.

---

## 7. ECS / gameplay mechanics / runtime architecture

### skypjack/entt

**Use for:** lightweight C++ ECS patterns.

**Когда смотреть:**

- gameplay entity/component design;
- mechanics system;
- scene runtime;
- asset/entity binding.

### Unreal Gameplay Ability System concepts/docs

**Use for:** abilities, cooldowns, effects, tags, prediction concepts.

**Когда смотреть:**

- Mechanics Studio design;
- ARPG skills;
- dash/attack/loot systems.

**Rule:** use concepts, do not copy Unreal architecture blindly.

---

## 8. VFX / particles / graph concepts

### The Forge / Vulkan samples particle examples if relevant

**Use for:** GPU/CPU particle architecture references.

### Niagara / Unity VFX Graph concepts

**Use for:** node graph concepts, emitter/lifetime/curve systems.

**Когда смотреть:**

- VFX Studio;
- VfxClip schema;
- SpriteEmitter v1;
- future GPU particles.

**Rule:** first SOLUM VFX v1 must be minimal final system: VfxClip + SpriteEmitter, not GIF overlay.

---

## 9. World / terrain / procedural generation

### Godot terrain/editor concepts

**Use for:** editor UX, scene/resources, open-source patterns.

### Unity terrain tool concepts / procedural generation references

**Use for:** brush tools, splat maps, terrain layers, road/biome workflow.

### meshoptimizer + glTF + KTX2

**Use for:** generated world asset optimization.

**Когда смотреть:**

- World Studio;
- terrain chunks;
- roads/biomes;
- LOD/streaming later.

---

## 10. UI / mobile editor UX references

### Shapr3D

**Use for:** mobile/tablet 3D editor gestures, viewport-first UX, precise manipulation.

### Procreate

**Use for:** viewport is sacred, compact tool model, gesture-driven professional UI.

### DaVinci Resolve iPad

**Use for:** timeline + preview balance on small screens.

### GarageBand mobile

**Use for:** professional audio tool on touch device.

**Rule:** study UX patterns, not visual copying.

---

## 11. Diagnostics / profiling / reports

### Arm Mobile Studio docs

**Use for:** Mali profiling, GPU bottlenecks, frame analysis.

### Android perf docs / Perfetto docs

**Use for:** tracing, CPU/frame timing, system performance.

### AGI — Android GPU Inspector docs

**Use for:** GPU frame analysis reference.

**Когда смотреть:**

- Diagnostics v1+;
- FPS regression database;
- GPU/frame timing reports;
- Vulkan profiling.

---

## Required research note format

When a repo influences a patch, create a note:

```text
docs/research/NOTE_XXXX_topic.md
```

Format:

```markdown
# NOTE-XXXX: Topic

## Problem
What SOLUM problem we are solving.

## References studied
- repo/path/file or docs link

## Adopted principles
What we take.

## Rejected parts
What we do not take.

## SOLUM adaptation
How it fits our architecture.

## Patch impact
Which patch uses this.

## Diagnostics/tests
How we prove it works.
```

## Agent rule

Before any major patch in these areas, agent must check this catalog:

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
