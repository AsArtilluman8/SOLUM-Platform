# RENDER_LAB — foundation scenes

Render Lab — controlled scene set for future Vulkan renderer validation.

Current status:

```text
foundation only
currentLabScene = scene02_model_import_lab
current implementation = real indexed Vulkan cube fallback/current render + GLB import/scan/CPU metadata parser
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
Render Lab: Scene02 Model Import Lab
Import: OK/FAILED/not run
Active model: name or none
Meshes / primitives / materials / textures
GPU Upload: not implemented
Draw Model: not implemented
Next: GLB Mesh GPU Upload
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

## Scene03 Camera/Depth Lab

Purpose:

- camera projection checks;
- near/far/depth precision checks;
- viewport resize checks.

Current state:

```text
scene id: scene03_camera_depth_lab
status: planned
```

## Scene04 Light/Shadow Lab

Purpose:

- first real light/shadow validation;
- future CSM checks;
- mobile shadow budget tracking.

Current state:

```text
scene id: scene04_light_shadow_lab
status: planned
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
    "currentLabScene": "scene02_model_import_lab",
    "currentLabSceneName": "Scene02 Model Import Lab",
    "renderingStatus": "foundation_only",
    "assetImportStatus": "not run",
    "activeModelName": "none",
    "gpuUploadStatus": "not_implemented",
    "drawStatus": "not_implemented",
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
