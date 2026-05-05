# P13C_REAL_GLB_AUDIT_IMPORT_PLAN

## Goal

P13C prevents fake material work. Before SOLUM renders glTF/PBR materials, every imported GLB must be audited and reported.

## What the audit checks

- meshes / primitives / vertices / triangle estimate;
- material count;
- texture slots;
- alpha modes;
- doubleSided flags;
- normal maps;
- metallicRoughness maps;
- occlusion / emissive maps;
- skins and animation presence;
- glTF extensions used and required.

## Runtime files

```text
runtime_model_state.json
runtime_material_state.json
runtime_texture_state.json
```

## Renderer correctness target

SOLUM should not claim author-like rendering until the following are implemented:

```text
glTF metallic-roughness PBR
baseColor sRGB sampling
linear metallicRoughness sampling
tangent-space normal maps
alpha MASK / BLEND
doubleSided
IBL/environment lighting
PBR Neutral tone mapping
KHR material extensions used by the asset
```

## Dependency direction

- `cgltf`: first native importer candidate.
- `tinygltf`: fallback/alternative.
- Filament: reference for glTF/PBR correctness.
- The Forge: reference for renderer architecture, descriptors and resource lifetime.
- KTX2/BasisU: later texture compression path.
- meshoptimizer: later mesh optimization/compression path.
