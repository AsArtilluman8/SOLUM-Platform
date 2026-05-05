# NOTE-0009: P12B glTF/GLB Material Mapping Foundation

## Why

The project already had previous material attempts where models did not look like the author intended. P12B prevents repeating that mistake.

The patch does not implement fake PBR. It defines the glTF material contract, runtime diagnostics schema and importer decision gate.

## References

Research gate decisions:

```text
glTF 2.0 material model: REFERENCE_ONLY / STANDARD
Google Filament glTF material path: REFERENCE_ONLY
The Forge resource architecture: REFERENCE_ONLY / ADAPTER LATER
Khronos Vulkan Samples descriptors/textures: REFERENCE_ONLY
SaschaWillems texture/PBR samples: REFERENCE_ONLY
cgltf: DEPENDENCY CANDIDATE
tinygltf: DEPENDENCY CANDIDATE / fallback
```

## Decision

Initial importer candidate:

```text
cgltf first
```

P12B result:

```text
material rules
↓
texture slot rules
↓
color space rules
↓
runtime diagnostics schema
↓
no shader fake
```

## Out of scope

- No GLB parser vendored yet.
- No texture upload yet.
- No descriptors yet.
- No PBR shader yet.
- No normal map rendering yet.
- No alpha blending path yet.

## Success criteria

```text
build still OK
APK still opens
3D object still visible
FPS still visible
SOLUM_RUNTIME_DIAGNOSTICS.zip generated
runtime_material_state.json documents mapping_ready_not_rendering_pbr_yet
runtime_texture_state.json documents required texture slots
```

## Next

P13 — glTF/GLB Import Probe + Texture Slot Diagnostics.
