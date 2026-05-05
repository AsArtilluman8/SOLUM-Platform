# NOTE-0004: Patch P05 Vulkan Frame Loop + First Render Pass

## Problem

P04 proved Android Native Vulkan on Mali-G57 MC2 and swapchain creation. The next layer must prove that SOLUM can record GPU commands, clear a swapchain image, submit work to the graphics queue, and present the frame.

This is not a throwaway triangle demo. It is the first permanent frame-loop layer of the final renderer.

## Current SOLUM state

- Android APK builds in Termux.
- Native `libsolum_engine.so` builds through manual prebuilt route.
- APK starts and reaches Android Native Vulkan.
- Real GPU path is confirmed: Mali-G57 MC2, integrated GPU, Vulkan API 1.1.177.
- Swapchain creation works.
- Runtime report export is tracked separately as a diagnostics known issue.

## References checked

- Android NDK Vulkan samples.
- Khronos Vulkan Samples.
- SaschaWillems Vulkan examples.
- ARM Mali Vulkan best practices.

## What the references teach

### Android NDK Vulkan samples

Use Android surface lifecycle carefully. Recreate Vulkan surface/swapchain resources when the surface changes.

### Khronos Vulkan Samples

Frame submission path should be explicit:

```text
acquire swapchain image
↓
record command buffer
↓
submit to queue
↓
present image
```

### SaschaWillems Vulkan examples

Clear/render pass/framebuffer/command pool are standard renderer foundation pieces and can later expand into pipelines, descriptors, materials and shadow passes.

### ARM Mali guidance

For tile-based mobile GPUs, use render pass load/store operations deliberately. For the first pass:

```text
loadOp = CLEAR
storeOp = STORE
initialLayout = UNDEFINED
finalLayout = PRESENT_SRC_KHR
```

This avoids reading old frame contents that are not needed.

## Options

### A — Clear color render pass only

Status: SMALL_SLICE.

Pros:
- Proves command buffers, render pass, framebuffers, sync and present.
- No shader/pipeline complexity yet.
- Correct permanent renderer layer.

Cons:
- No triangle yet.

### B — Clear color + triangle pipeline

Status: ADAPTER later.

Pros:
- More visible draw proof.

Cons:
- Requires shader/pipeline creation in same patch.
- Higher risk after P04 runtime/build instability.

### C — UI/Canvas fake preview

Status: REJECT.

Reason: violates Vulkan-first and non-throwaway rules.

## Recommended choice

Use Option A first:

```text
P05 = frame loop + first render pass + clear/present
P06/P05A = pipeline + first triangle draw
```

This keeps the renderer foundation permanent and testable without adding shader/pipeline noise too early.

## SOLUM adaptation

P05 adds:

- swapchain image views;
- render pass;
- framebuffers;
- command pool;
- command buffers;
- semaphores;
- fence;
- acquire/submit/present path;
- first clear color frame;
- `framesRendered` and `firstFrameRendered` in runtime report.

## What not to copy

- Do not import an entire sample framework.
- Do not add OpenGL/Canvas fallback.
- Do not add a fake triangle as UI overlay.
- Do not add PBR/material/shadow systems before frame loop is proven.

## Diagnostics/test plan

Build:

```bash
bash tools/build_engine_apk.sh
```

Run APK.

Expected screen:

```text
Render pass: clear color OK
Frames rendered: 1
```

If it fails, user sends:

```text
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Runtime report export remains a known issue from P04 and is not blocking this render-path proof.
