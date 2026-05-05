# NOTE-0006: Patch P07 Vertex Buffer + Simple Mesh Upload Path

## Problem

P06 proved shader modules, graphics pipeline creation and `vkCmdDraw(3)`, but the triangle positions were still embedded in shader logic earlier in the path.

P07 moves triangle geometry into a real GPU vertex buffer. This creates the first small slice of the future mesh upload system.

## Current SOLUM state

- Android Native Vulkan works on Mali-G57 MC2.
- Swapchain, render pass, command submission and present work.
- Graphics pipeline and validation draw work.
- Shader build pipeline exists.

## References checked

- Khronos Vulkan Samples.
- SaschaWillems Vulkan examples.
- Android NDK Vulkan samples.
- ARM Mali Vulkan best practices.

## Decision

Use `SMALL_SLICE`.

P07 adds a host-visible coherent vertex buffer first:

```text
CPU vertex data
↓
VkBuffer
↓
VkDeviceMemory
↓
map / copy / unmap
↓
vkBindBufferMemory
↓
vkCmdBindVertexBuffers
↓
vkCmdDraw(vertexCount)
```

This is not yet the final high-performance staging-buffer upload path. It is the minimal correct foundation to prove real mesh data reaches the GPU.

## Why host-visible coherent first

For the first mesh-upload slice, host-visible coherent memory is easiest to diagnose on Android:

- CPU can write vertex data directly.
- No transfer queue/staging buffer yet.
- Fewer Vulkan objects can fail.
- Good for tiny validation geometry.

Future mesh upload patches should introduce staging buffers and device-local memory for larger meshes.

## Scope

- `Vertex2D` CPU vertex struct.
- `VkBuffer` for vertex data.
- `VkDeviceMemory` allocation.
- Memory type selection.
- `vkMapMemory` / `memcpy` / `vkUnmapMemory`.
- `vkBindBufferMemory`.
- Vertex input binding/attribute layout.
- `vkCmdBindVertexBuffers`.
- Runtime state: `Vertex buffer: OK`.

## Out of scope

- Index buffer.
- Staging buffer.
- Device-local mesh memory.
- Mesh asset loader.
- glTF import.
- Materials/textures/PBR.
- Lighting/shadows.

## Expected result

Runtime overlay:

```text
Vertex buffer: OK
Triangle draw: OK
Frames rendered: 1
```

Visual:

```text
orange triangle still visible, now fed from a GPU vertex buffer
```

## Next

If P07 succeeds:

```text
P08 — Mesh Resource Foundation
```

P08 should start extracting mesh data into a small internal mesh resource structure before asset import/material work.
