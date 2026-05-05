# P10_3D_OBJECT_RENDER_PATH

## New render path

```text
RendererCore
↓
MeshResource::createValidationCube
↓
GpuBuffer vertex upload
↓
GpuImage depth buffer
↓
PipelineBundle object pipeline
↓
MVP push constants
↓
vkCmdDraw cube vertices
```

## Why depth buffer now

A flat triangle can render without depth. A 3D object cannot. Depth buffer stores which pixel is closer to the camera, so back faces do not draw over front faces.

## Why push constants now

Push constants are a small fast path for tiny per-draw data. P10 uses them for one MVP matrix. Later patches can move to uniform buffers for many objects.

## Next

P11 should add Material Foundation or Camera/Transform Controls depending on current pain.
