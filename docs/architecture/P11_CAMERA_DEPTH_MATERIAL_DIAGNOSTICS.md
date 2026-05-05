# P11_CAMERA_DEPTH_MATERIAL_DIAGNOSTICS

## Purpose

P11 prepares SOLUM for real material and GLB work by making camera/depth state observable.

Materials must not be debugged from screenshots only. The runtime ZIP must show render, model and material state.

## Rules

```text
camera/depth first
↓
material mapping second
↓
PBR shader third
```

## Diagnostics targets

Runtime ZIP should include:

```text
runtime_render_state.json
runtime_model_state.json
runtime_material_state.json
runtime_vulkan_caps.json when Android storage allows it
```

## Current material status

P11 does not implement PBR. It creates a schema-ready material diagnostics file so P12/P13 cannot fake material readiness.

## Depth correctness baseline

Initial camera:

```text
fovDegrees=52
near=0.10
far=64.0
distance=5.8
```

This keeps the object framed and avoids extreme near/far ratios before testing real imported meshes.
