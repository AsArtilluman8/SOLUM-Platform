# RENDERING_TARGET_SPEC — Vulkan/render target SOLUM

## Главный target

SOLUM Engine — Android/Vulkan runtime.

Цель: AAA-like mobile rendering на телефоне, с реальным рендер-пайплайном, материалами, светом, тенями, post-process и диагностикой.

## Запрещённые подмены

Нельзя:

- использовать OpenGL как production path вместо Vulkan;
- делать blob shadows вместо shadow system;
- использовать low-poly/fake visual target как финальную цель;
- делать bitmap/canvas demo вместо Vulkan renderer;
- делать временный renderer, который потом нужно удалить;
- скрывать проблемы производительности отключением фичи без диагностики.

## Разрешённое упрощение

Можно делать минимальную версию финальной системы.

Примеры:

```text
CSM target → ShadowSystem v1 с одной real shadow map
Material graph target → MaterialDocument v1 с baseColor/roughness/metallic
Volumetric/cloud target → CloudSystem v1 с 2 параметрами
Renderer target → Vulkan triangle/swapchain foundation
```

Главное: система должна расширяться, а не удаляться.

## Vulkan-first rule

SOLUM render foundation должен идти через Vulkan.

OpenGL может существовать только как отдельный debug/prototype вне production path, если пользователь явно одобрил.

## Patch 02: Vulkan capability dump

До полноценного renderer нужно получить `vulkan_caps.json`.

Минимум:

```json
{
  "apiVersion": "...",
  "driverVersion": "...",
  "vendorID": "...",
  "deviceName": "...",
  "deviceType": "...",
  "features": {},
  "limits": {},
  "extensions": [],
  "missingCritical": []
}
```

Важно заранее знать:

- Vulkan API version;
- device name;
- vendor;
- driver;
- supported extensions;
- features;
- limits;
- texture compression support;
- UBO alignment;
- push constant size;
- max texture size;
- compute support;
- timestamp/timing availability if possible.

## Mali/Android constraints

Целевой телефон пользователя: TECNO CI8n / Mali-G57 class.

Нужно учитывать:

- tile-based GPU;
- bandwidth sensitive render passes;
- не полагаться на geometry shaders;
- не ожидать desktop GPU behavior;
- validation layers могут быть тяжелыми;
- GPU timing может быть ограничен без root/profilers;
- CPU-side timing может быть приближением, но должен явно называться approximation;
- thermal throttling может менять FPS.

## Diagnostics for render

Каждая render/Vulkan фича должна писать:

- active renderer path;
- enabled features;
- shader path;
- material path;
- frame timing;
- errors/warnings;
- device capabilities used;
- render state JSON.

## External repo reference rule

Перед сложными системами изучать проверенные источники.

### Vulkan foundation

- Android NDK Vulkan samples.
- Khronos Vulkan Samples.

### Renderer architecture

- The Forge.
- Filament.
- bgfx.
- GPUOpen/Cauldron concepts.

### Materials

- Filament PBR model.
- glTF material definitions.
- KTX2/BasisU texture pipeline.

### Asset pipeline

- tinygltf.
- meshoptimizer.
- KTX-Software.

### Animation

- ozz-animation.

### ECS/mechanics

- EnTT.

Правило:

```text
изучить → выделить принцип → записать решение → адаптировать маленький slice под SOLUM
```

Нельзя:

```text
слепо импортировать огромный framework без build proof и adapter plan
```

## The Forge rule

The Forge не заменяет SOLUM.

Использовать как reference для:

- renderer abstraction;
- resource lifetime;
- descriptor management;
- render graph / frame graph concepts;
- barriers/synchronization patterns.

Если берём решение — записать в ADR/research:

- что изучили;
- что берём;
- что не берём;
- почему это подходит Android/Vulkan/Termux;
- как это вписывается в SOLUM architecture.

## Definition of done for render feature

Render фича считается готовой только если есть:

- build success;
- runtime proof;
- diagnostics report;
- active feature state;
- known issues;
- performance snapshot;
- user-visible expected result.

Без этого нельзя писать “работает”.
