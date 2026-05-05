# NOTE-0005: Patch P06 Graphics Pipeline + First Validation Triangle

## Problem

P05 proved the first Vulkan frame loop:

```text
acquire swapchain image
↓
record command buffer
↓
clear render pass
↓
submit
↓
present
```

P06 must prove the next permanent renderer layer: shader modules and graphics pipeline creation, followed by the first validation draw call.

This is not a throwaway triangle demo. The triangle only validates that the final render path can bind a pipeline and issue a draw call.

## Current SOLUM state

- Android APK builds in Termux.
- Native Vulkan runtime reaches Mali-G57 MC2.
- Swapchain creation works.
- Render pass clear and present work.
- Build output now tells the user what to verify.
- Runtime report export remains a known diagnostics issue.

## References checked

- Android NDK Vulkan samples.
- Khronos Vulkan Samples.
- SaschaWillems Vulkan examples.
- ARM Mali Vulkan best practices.

## What the references teach

### Android NDK Vulkan samples

Android surface and swapchain lifecycle remain the root of the renderer. Pipeline creation must be tied to swapchain/render pass compatibility.

### Khronos Vulkan Samples

A basic graphics pipeline requires:

```text
shader stages
↓
vertex input state
↓
input assembly
↓
viewport/scissor
↓
rasterization
↓
multisample
↓
color blend
↓
pipeline layout
↓
render pass compatibility
```

### SaschaWillems Vulkan examples

A hardcoded validation triangle without vertex buffer is acceptable for early pipeline proof if it is clearly replaced by real vertex buffers in the next patch.

### ARM Mali guidance

Avoid unnecessary extra render passes and bandwidth. P06 keeps the existing single color attachment and one draw call.

## Options

### A — Shader modules + graphics pipeline + vertex-index triangle

Status: SMALL_SLICE.

Pros:
- Proves pipeline and draw call.
- No vertex buffer yet, lower risk.
- Keeps P06 focused.

Cons:
- The triangle is not asset/mesh data yet.

### B — Pipeline + vertex buffer + triangle mesh

Status: ADAPTER later.

Pros:
- Closer to mesh path.

Cons:
- Mixes pipeline proof and buffer/memory allocation in one patch.

### C — UI/canvas triangle overlay

Status: REJECT.

Reason: fake renderer path, violates Vulkan-first rule.

## Recommended choice

Use Option A for P06:

```text
GLSL shader source
↓
Termux shader compile to SPIR-V header
↓
VkShaderModule
↓
VkPipelineLayout
↓
VkPipeline
↓
vkCmdDraw(3)
```

Next patch can add vertex buffer and real mesh upload path.

## SOLUM adaptation

P06 adds:

- `engine-core/solum-vulkan-core/shaders/triangle.vert`.
- `engine-core/solum-vulkan-core/shaders/triangle.frag.glsl`.
- `tools/build_shaders.sh`.
- Generated SPIR-V headers during native build.
- `VkPipelineLayout` and `VkPipeline` ownership in `SolumEngine`.
- Triangle draw inside the existing P05 render pass.

## What not to copy

- Do not import sample app frameworks.
- Do not add OpenGL or Canvas fallback.
- Do not introduce PBR/material/shadow logic here.
- Do not treat the triangle as final scene content.

## Diagnostics/test plan

Build:

```bash
bash tools/build_engine_apk.sh
```

Expected build output:

```text
SOLUM SHADER BUILD: OK
SOLUM BUILD RESULT: OK
```

Runtime overlay:

```text
Render pass: clear color OK
Triangle draw: OK
Frames rendered: 1
```

Visual result:

```text
orange triangle over dark teal Vulkan clear color
```

If build fails, send:

```text
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

## Known risks

- `glslc` or `glslangValidator` may be missing in Termux.
- Shader compilation can fail if shader compiler package is absent.
- Pipeline creation can fail if generated SPIR-V is incompatible.

## Next

If P06 succeeds: vertex buffer + simple mesh upload path.
