# RENDER_LAB — foundation scenes

Render Lab — controlled scene set for future Vulkan renderer validation.

## Scene06 PBR Material Maps Lab

P09 scene id:

```text
scene06_pbr_material_maps_lab
```

Runtime status text:

```text
Render Lab: Scene06 PBR Material Maps Lab
modelRenderMode: multi_primitive_static
Next: Lighting Foundation
```

Scene06 preserves Scene05 multi-primitive/baseColor drawing and adds PBR material map foundation without full lighting.

Required PBR diagnostics:

```text
pbrMapsStatus
metallicRoughnessStatus
normalMapStatus
occlusionMapStatus
metallicFactor
roughnessFactor
normalScale
occlusionStrength
pbrTextureSlotCount
uploadedPbrTextureCount
skippedPbrTextureCount
pbrTextureFallbackCount
materialSlotDiagnostics
currentScene = scene06_pbr_material_maps_lab
renderLab.currentLabScene = scene06_pbr_material_maps_lab
renderLab.currentLabSceneName = Scene06 PBR Material Maps Lab
```

Supported P09 map foundation:

- embedded GLB `image.bufferView` PNG/JPEG decode for baseColor, metallicRoughness, normal, and occlusion textures;
- external URI and data URI are reported as unsupported, not as success;
- metallic/roughness factors and map status are reported per material slot;
- AO can darken baseColor in the current shader;
- normal map is blocked with `normalMapStatus=blocked_no_tangent` when `TANGENT` is absent;
- failed PBR texture upload falls back without failing mesh draw.

Out of scope:

- full PBR lighting;
- shadows;
- IBL/reflections;
- alpha/glass/clearcoat/transmission.

## Scene05 Multi Primitive Render Lab

P08 scene id:

```text
scene05_multi_primitive_render_lab
```

Runtime status text:

```text
Render Lab: Scene05 Multi Primitive Render Lab
modelRenderMode: multi_primitive_static
Next: PBR Material Maps Foundation
```

Scene05 validates active imported GLB rendering across all supported static primitives, material slots with baseColorFactor, baseColor texture slots, skipped primitive diagnostics, fallback cube if all primitives are unsupported, FPS/frameMs HUD, and debug ZIP export status.

Required diagnostics:

```text
modelRenderMode = multi_primitive_static
primitiveCountTotal
primitiveCountRendered
primitiveCountSkipped
unsupportedPrimitiveCount
materialSlotCount
materialSlotCountRendered
textureSlotCount
uploadedTextureCount
textureFallbackCount
skippedTextureCount
textureSlotLimit
fpsCurrent
frameTimeMs
fpsSource
fpsLastStable
frameTimeLastStableMs
debugZipStatus
debugZipPath
debugZipIncludedFiles
debugZipReason
fallbackCubeStatus
fallbackCubeVisible
drawStatus
gpuUploadStatus
```

Current status:

```text
texture binding foundation
currentLabScene = scene04_texture_binding_lab
current implementation = real indexed Vulkan cube fallback + GLB import/scan/CPU metadata parser + first primitive Vulkan GPU upload/draw + baseColor texture decode/upload/sample
```

No shadow/import/performance feature is claimed ready until a real Vulkan implementation and diagnostics proof exist.

## Scene01 Foundation Cube

Purpose:

- first real Vulkan cube target;
- interactive camera baseline;
- depth baseline;
- material constants foundation;
- mesh attribute layout foundation;
- engine diagnostics smoke scene.

Current state:

```text
scene id: scene01_foundation_cube
status: implemented_foundation
geometry: indexed cube
attributes: POSITION,NORMAL,TEXCOORD_0,COLOR_0
depth: color + depth render pass attachment
camera: drag rotate + pinch/buttons zoom, perspective MVP through push constants
material constants: baseColorFactor, metallicFactor, roughnessFactor, emissiveFactor, alphaMode, materialId
shader material use: vertexColor * baseColorFactor.rgb, alpha = baseColorFactor.a
triangle fallback: available/disabled
screenshot/readback: not_available, renderer_readback_not_implemented
```

Expected runtime status:

```text
Render Lab: Scene04 Texture Binding Lab
Import: OK/FAILED/not run
Active model: name or none
Meshes / primitives / materials / textures
GPU Upload: ok/failed
Draw Model: ok/fallback
BaseColor Texture: ok/missing/failed
Texture size: width x height or none
Fallback texture: yes/no
Fallback cube: on/off
Next: PBR Material Maps Foundation
```

## Scene02 Model Import Lab

Purpose:

- import `.glb` through Android file picker;
- copy model into the SOLUM asset library;
- scan imported model assets;
- parse GLB header/chunks and JSON metadata on CPU;
- write model diagnostics without claiming GPU upload or model draw.

Current state:

```text
scene id: scene02_model_import_lab
status: implemented_import_foundation
asset root: /storage/emulated/0/SOLUMCreative/assets/models/imported/
current render: Scene01 cube fallback preserved
gpuUploadStatus: not_implemented
drawStatus: not_implemented
next: GLB Mesh GPU Upload
```

## Scene03 GLB Mesh Render Lab

Purpose:

- upload first active GLB mesh primitive to Vulkan buffers;
- draw the first primitive with POSITION,NORMAL,TEXCOORD_0,COLOR_0 layout;
- preserve cube fallback when no active model or unsupported data is detected;
- report runtime truth for upload, draw, bounds, scale and fallback state.

Current state:

```text
scene id: scene03_glb_mesh_render_lab
status: implemented_foundation
render mode: first_primitive
supported POSITION/NORMAL/TEXCOORD_0: FLOAT VEC3/VEC2
supported indices: UNSIGNED_SHORT / UNSIGNED_INT
unsupported accessor/component: gpuUploadStatus=failed, drawStatus=fallback, exact reason
fallback: Scene01 cube remains visible
next: Texture Binding Foundation
```

## Scene04 Texture Binding Lab

Purpose:

- extract first primitive material `pbrMetallicRoughness.baseColorTexture`;
- decode embedded GLB `image.bufferView` PNG/JPEG through Android `BitmapFactory`;
- upload one RGBA8 baseColor texture to Vulkan image/imageView/sampler;
- sample texture in the current material shader using `TEXCOORD_0`;
- preserve white/baseColor fallback when texture is absent or failed;
- report texture status without changing mesh draw success.

Current state:

```text
scene id: scene04_texture_binding_lab
status: implemented_foundation
supported texture slots: baseColorTexture only
supported image storage: embedded GLB image.bufferView in BIN chunk
supported MIME: image/png, image/jpeg when Android decode supports it
textureUploadStatus: ok/failed/missing
baseColorTextureStatus: ok/failed/missing
fallback: white/baseColor when texture missing/failed
next: PBR Material Maps Foundation
```

## Scene05 Import Lab

Purpose:

- imported mesh/material smoke checks;
- asset schema compatibility checks.

Current state:

```text
scene id: scene05_import_lab
status: planned
```

## Scene06 Performance Lab

Purpose:

- baseline frame timing;
- diagnostics overhead checks;
- future regression snapshots.

Current state:

```text
scene id: scene06_performance_lab
status: planned
```

## Diagnostics

Engine diagnostics must include:

```json
{
  "renderLab": {
    "schema": "solum.render_lab_state",
    "schemaVersion": 1,
    "currentLabScene": "scene04_texture_binding_lab",
    "currentLabSceneName": "Scene04 Texture Binding Lab",
    "renderingStatus": "model_first_primitive",
    "assetImportStatus": "active model",
    "activeModelName": "cottage_medieval.glb",
    "activeModelPath": ".../cottage_medieval.glb",
    "activePrimitiveIndex": 0,
    "gpuUploadStatus": "ok",
    "drawStatus": "ok",
    "meshDrawStatus": "ok",
    "textureUploadStatus": "ok",
    "baseColorTextureStatus": "ok",
    "baseColorTextureName": "baseColorTexture_0",
    "baseColorTextureSource": "textures[0].source=images[0].bufferView=4",
    "baseColorTextureMimeType": "image/png",
    "textureWidth": 1024,
    "textureHeight": 1024,
    "textureBytes": 4194304,
    "textureFallbackUsed": false,
    "uploadedVertexCount": 4374,
    "uploadedIndexCount": 7002,
    "modelVertexLayout": "POSITION,NORMAL,TEXCOORD_0,COLOR_0",
    "modelBoundsMin": [0.0, 0.0, 0.0],
    "modelBoundsMax": [0.0, 0.0, 0.0],
    "modelBoundsCenter": [0.0, 0.0, 0.0],
    "modelScale": 1.0,
    "modelRenderMode": "first_primitive",
    "fallbackCubeVisible": false,
    "fallbackCubeStatus": "off",
    "reason": "first primitive uploaded to Vulkan buffers",
    "cubeStatus": "ok",
    "depthStatus": "ok",
    "cameraStatus": "ok",
    "cameraMvpStatus": "ok",
    "cameraControlsStatus": "ok",
    "cameraYawDeg": 28.0,
    "cameraPitchDeg": -18.0,
    "cameraDistance": 4.2,
    "materialConstantsReady": true,
    "meshAttributeLayoutReady": true,
    "vertexLayout": "POSITION,NORMAL,TEXCOORD_0,COLOR_0",
    "vertexStrideBytes": 44,
    "indexBufferReady": true,
    "uniformOrPushConstantsReady": true,
    "vertexCount": 24,
    "indexCount": 36,
    "material": {
      "materialId": 1,
      "baseColorFactor": [0.92, 0.78, 1.0, 1.0],
      "metallicFactor": 0.0,
      "roughnessFactor": 0.65,
      "emissiveFactor": [0.0, 0.0, 0.0],
      "alphaMode": "OPAQUE"
    },
    "rendererPath": "Android Native Vulkan",
    "screenshot": {
      "status": "not_available",
      "reason": "renderer_readback_not_implemented"
    }
  }
}
```
