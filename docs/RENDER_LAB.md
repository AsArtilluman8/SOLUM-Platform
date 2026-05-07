# RENDER_LAB — foundation scenes

Render Lab — controlled scene set for future Vulkan renderer validation.

Current status:

```text
foundation only
currentLabScene = scene01_foundation_cube
current implementation = real indexed Vulkan cube + depth + interactive camera + material constants + mesh attribute layout foundation
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
Render Lab: Scene01 Foundation Cube
Cube draw: OK
Depth: OK
Camera: controls OK
Material constants: OK
Mesh layout: OK
Next: Texture Binding / Asset Mesh Upload
```

## Scene02 Material Lab

Purpose:

- material parameter validation;
- texture slot validation;
- future Material Studio preview target.

Current state:

```text
scene id: scene02_material_lab
status: planned
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
    "currentLabScene": "scene01_foundation_cube",
    "renderingStatus": "foundation_only",
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
