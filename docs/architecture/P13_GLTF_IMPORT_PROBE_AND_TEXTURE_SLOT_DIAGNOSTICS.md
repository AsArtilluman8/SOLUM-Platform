# P13 glTF/GLB Import Probe + Texture Slot Diagnostics

## Goal

P13 verifies glTF/GLB model and material metadata before any PBR shader work.

This avoids fake materials and prevents guessing when a GLB does not look like the author intended.

## What P13 does

```text
.gltf / .glb sample
↓
parse JSON / GLB JSON chunk
↓
count scenes / nodes / meshes / primitives
↓
inspect glTF 2.0 metallic-roughness materials
↓
inspect texture slots
↓
write runtime_model_state.json
↓
write runtime_material_state.json
↓
write runtime_texture_state.json
```

## What P13 does not do

- no image decoding
- no texture upload
- no PBR shader
- no glTF mesh upload to GPU
- no fallback fake material pretending to be final

## Expected sample path

```text
/storage/emulated/0/SOLUMCreative/assets/models/*.gltf
/storage/emulated/0/SOLUMCreative/assets/models/*.glb
```

Or explicit:

```text
SOLUM_GLTF_SAMPLE=/path/to/model.glb python3 tools/gltf_import_probe.py
```

## Material rules checked

- baseColor: sRGB texture, linear factor in shader math
- normal: linear/non-color, tangent-space expected
- metallicRoughness: linear/non-color, B=metallic, G=roughness in glTF convention
- occlusion: linear/non-color
- emissive: sRGB to linear
- alphaMode: OPAQUE / MASK / BLEND

## Next

P14 should add actual glTF mesh upload or texture upload only after this probe reports a clean material/texture slot map.
