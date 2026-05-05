# P09_RENDERER_CORE_ARCHITECTURE

## Why this exists

Earlier SOLUM Vulkan patches proved the path step by step. That was useful, but continuing to add features inside `solum_engine.cpp` would recreate the old monolith problem.

P09 creates module boundaries before materials, lighting, shadows and Asset Hub integration.

## Module boundaries

### JNI bridge

File:

```text
engine-core/solum-vulkan-core/src/solum_engine.cpp
```

Owns only Android JNI entry points and calls `RendererCore`.

### RendererCore

File:

```text
engine-core/solum-vulkan-core/src/solum/renderer_core.hpp
```

Owns Vulkan instance/device/surface/swapchain/frame lifecycle.

### RuntimeDiagnostics

File:

```text
engine-core/solum-vulkan-core/src/solum/runtime_diagnostics.hpp
```

Owns runtime state writing. Normal UI should stay compact; detailed state belongs in reports.

### GpuBuffer

File:

```text
engine-core/solum-vulkan-core/src/solum/gpu_buffer.hpp
```

Owns Vulkan buffer and memory lifetime.

### MeshResource

File:

```text
engine-core/solum-vulkan-core/src/solum/mesh_resource.hpp
```

Owns mesh vertex data on GPU. P09 keeps a validation triangle as smoke test.

### PipelineBundle

File:

```text
engine-core/solum-vulkan-core/src/solum/pipeline_bundle.hpp
```

Owns pipeline layout and graphics pipeline.

## Future direction

```text
P10 Asset Mesh Upload
↓
P11 Transform/Camera
↓
P12 Material Foundation
↓
P13 Lighting Foundation
↓
P14 Shadow Architecture Slice
```

The validation triangle must remain only as a smoke test, not as scene content.
