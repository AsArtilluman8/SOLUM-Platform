# GLTF_MATERIAL_MAPPING_SPEC — SOLUM glTF/GLB material correctness rules

## Goal

SOLUM must render imported glTF/GLB assets according to glTF 2.0 material intent, not as a hand-made approximate shader.

The first rule:

```text
glTF material data
↓
material diagnostics
↓
texture binding plan
↓
shader layout
↓
only then PBR rendering
```

## Required glTF material inputs

PBR material support must be designed around these glTF 2.0 fields:

```text
baseColorFactor
baseColorTexture
metallicFactor
roughnessFactor
metallicRoughnessTexture
normalTexture
occlusionTexture
emissiveFactor
emissiveTexture
alphaMode
alphaCutoff
doubleSided
```

## Color space rules

Correct color space is mandatory.

```text
baseColorTexture: sRGB
emissiveTexture: sRGB
normalTexture: linear
metallicRoughnessTexture: linear
occlusionTexture: linear
```

Meaning:

- sRGB textures store visible color.
- linear textures store data for math.
- If these are mixed up, assets become too dark, too bright, or physically wrong.

## Texture channel rules

For standard glTF metallic-roughness:

```text
metallicRoughnessTexture.R: unused by core metallic-roughness
metallicRoughnessTexture.G: roughness
metallicRoughnessTexture.B: metallic
```

Occlusion usually uses:

```text
occlusionTexture.R: occlusion
```

## Normal map rules

Normal maps must be tangent-space normal maps.

Required future path:

```text
mesh positions
mesh normals
mesh tangents
normalTexture
↓
TBN matrix in shader
↓
correct tangent-space lighting
```

Do not fake normal map support without tangent data.

## Alpha rules

glTF alpha modes:

```text
OPAQUE — normal solid object
MASK — cutout alpha using alphaCutoff
BLEND — transparent blending path
```

Important:

- OPAQUE and MASK can write depth normally.
- BLEND needs sorted/controlled transparent rendering later.
- Do not mix BLEND into the opaque pass as a fake shortcut.

## Depth and z-fighting rules

Material bugs are often camera/depth bugs.

Before blaming material:

```text
check near plane
check far plane
check depth format
check depth test/write state
check mesh overlaps
check duplicate faces
```

Current target:

```text
near = 0.1
far = 32.0
first depth format = VK_FORMAT_D24_UNORM_S8_UINT or VK_FORMAT_D32_SFLOAT fallback
```

## Importer decision

P12B does not import GLB yet. It defines the decision gate.

Candidates:

```text
cgltf — small C single-header style, easier for Termux/native integration
tinygltf — C++ JSON-based loader, useful but heavier
```

Initial recommendation:

```text
cgltf first
```

Reason:

- small dependency surface;
- easy to vendor later;
- good for glTF/GLB parsing;
- keeps Android/Termux build controllable.

## Runtime diagnostics contract

Every model/material import patch must produce diagnostics:

```text
runtime_model_state.json
runtime_material_state.json
runtime_texture_state.json
runtime_render_state.json
```

Minimum material diagnostics fields:

```json
{
  "schema": "solum.runtime_material_state",
  "schemaVersion": 2,
  "status": "mapping_ready_not_rendering_pbr_yet",
  "standard": "glTF 2.0 metallic-roughness",
  "importerDecision": "cgltf_first",
  "textureSlots": {
    "baseColor": { "requiredColorSpace": "sRGB" },
    "normal": { "requiredColorSpace": "linear", "requiresTangents": true },
    "metallicRoughness": { "requiredColorSpace": "linear", "roughnessChannel": "G", "metallicChannel": "B" },
    "occlusion": { "requiredColorSpace": "linear", "channel": "R" },
    "emissive": { "requiredColorSpace": "sRGB" }
  }
}
```

## Explicitly forbidden

```text
baseColor-only material pretending to be PBR
random shader constants called material system
normal map without tangent handling
alpha blend inside opaque pass as permanent solution
texture atlas as replacement for glTF material mapping
manual color tweaks to imitate author intent
```

## Next implementation step

P13 should add a tiny importer gate and sample asset scan:

```text
sample GLB/folder path
↓
parse material list
↓
write material/texture diagnostics
↓
no PBR rendering until mapping is proven
```
