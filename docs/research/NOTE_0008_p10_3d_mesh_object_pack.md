# NOTE-0008: P10 3D Mesh/Object Pack

## Purpose

P10 upgrades the renderer from a 2D validation triangle to the first real 3D object path.

The target is not visual beauty yet. The target is a correct Vulkan foundation for future materials, lighting and shadows.

## Scope

- 3D vertex layout: position + color.
- Colored validation cube mesh.
- Depth image, memory and image view.
- Color + depth render pass.
- Color + depth framebuffers.
- Depth test/write pipeline state.
- MVP push constants.
- Minimal perspective projection and static camera.
- Runtime proof: `3D object: OK`.

## Reference decision

- Khronos Vulkan Samples: reference for depth and push constants.
- SaschaWillems Vulkan: reference for cube/MVP flow.
- ARM Mali Vulkan best practices: keep depth path simple and bandwidth-conscious.
- The Forge: reference only for future renderer resource ownership.
- Filament: reference only for future material/camera architecture.

## Out of scope

- Asset import.
- glTF.
- PBR.
- Textures.
- Lighting.
- Shadows.
- Camera controls.
- Editor gizmo.

## Expected result

```text
SOLUM Engine
Vulkan: Mali-G57 MC2
Status: 3D Object OK
Next: Material Foundation
```

Visual result: first colored cube/object with depth.
