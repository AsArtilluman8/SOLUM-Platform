# RENDER_RESOURCE_LIFETIME_RULES — render thread and GPU resource ownership

Этот файл фиксирует правила безопасной загрузки ассетов и управления GPU ресурсами.

## Problem

Старый runtime GLB path показал риск:

```text
UI thread выбрал GLB
↓
Kotlin parser загрузил mesh
↓
JNI upload пересоздал buffers
↓
render thread мог параллельно рисовать старые buffers
↓
камера/input/state ломались или модель появлялась не сразу
```

Такой путь запрещён для SOLUM architecture.

## LAW: Render thread owns GPU resources

GPU resource creation/destruction должен происходить через render thread или строго синхронизированную render command queue.

UI thread не должен напрямую разрушать/создавать active Vulkan resources.

## Correct runtime asset upload flow

```text
user selects asset
↓
copy/import to sandbox
↓
parse asset on worker/UI-safe layer
↓
validate asset data
↓
create UploadRequest
↓
enqueue to RenderCommandQueue
↓
render thread reaches safe point
↓
create GPU buffers/images/descriptors
↓
swap active resource handle atomically
↓
old resource released after GPU safe frame/fence
↓
write upload_report.json
↓
UI receives success/failure state
```

## RenderCommandQueue

Future render backend should have explicit command queue:

```text
UploadMeshCommand
UploadTextureCommand
DestroyResourceCommand
ReloadShaderCommand
ResizeSwapchainCommand
SetCameraCommand
```

Each command must report:

- accepted;
- completed;
- failed;
- error message;
- affected resource id.

## No random vkDeviceWaitIdle

`vkDeviceWaitIdle` is allowed only for:

- shutdown;
- critical recovery;
- explicit debug tool;
- controlled resource rebuild with report.

It must not be normal upload path for every asset change.

## Resource handles

Runtime systems should pass handles, not raw pointers everywhere.

Example:

```text
MeshHandle
TextureHandle
MaterialHandle
PipelineHandle
```

A handle can become invalid, but must be checked and reported.

## Resource lifetime states

```text
PendingImport
CPUParsed
UploadQueued
GPUReady
Active
Retired
Destroyed
Failed
```

Diagnostics should show resource state.

## Stale resource prevention

When replacing mesh/material/texture:

```text
new resource created
↓
new handle validated
↓
renderer switches active handle
↓
old handle marked Retired
↓
old GPU object destroyed after fence/safe delay
```

Never destroy active resource while command buffer may still reference it.

## Asset upload diagnostics

Upload report should include:

```json
{
  "assetId": "...",
  "assetType": "mesh",
  "vertices": 0,
  "indices": 0,
  "textures": 0,
  "uploadQueued": true,
  "uploadCompleted": true,
  "activeHandle": "mesh_001",
  "error": null
}
```

## UI rule

Asset upload must have visible state:

```text
Importing
Validating
Uploading to GPU
Ready
Failed
```

If model appears only after second action, upload path is not complete.

## Thread ownership summary

```text
UI thread owns: UI state, selection, file picker result
Asset system owns: parse/validate/import transaction
Render thread owns: GPU resources and draw state
Input system owns: gesture routing
Diagnostics owns: reports and snapshots
```

No module should secretly mutate another owner's state without command/event/report.
