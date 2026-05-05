# NOTE-0009: P12 glTF / GLB Material Import Gate

## Reason

Do not implement fake materials. SOLUM must follow glTF 2.0 material semantics so imported GLB models can appear close to the author's intent.

## References

- glTF 2.0 specification — material model and texture slots.
- Google Filament — reference for glTF/PBR correctness.
- Khronos Vulkan Samples — descriptor/image/sampler layout references.
- The Forge — resource lifetime and renderer architecture reference.
- meshoptimizer — future mesh optimization.
- KTX-Software/BasisU — future KTX2/Basis compressed texture path.

## Material mapping target

```text
glTF material
↓
baseColorFactor + baseColorTexture
↓
metallicFactor / roughnessFactor + metallicRoughnessTexture
↓
normalTexture
↓
occlusionTexture
↓
emissiveFactor + emissiveTexture
↓
alphaMode: OPAQUE / MASK / BLEND
↓
doubleSided
```

## Color correctness rules

- BaseColor textures are sampled as sRGB.
- MetallicRoughness, normal and occlusion textures are sampled as linear/non-color data.
- Emissive texture is sRGB and converted for lighting.
- Normal maps require tangent-space basis before final PBR correctness.

## Dependency decision

P12 should choose between:

```text
cgltf: smaller C parser, easier Termux/native integration
tinygltf: C++ header library, easier JSON/images path but heavier
```

Current decision: evaluate `cgltf` first as SMALL_SLICE / DEPENDENCY candidate.

## Diagnostics required

Every material import must produce:

```text
runtime_material_state.json
runtime_texture_state.json
runtime_model_state.json
```

These reports must show found/missing textures, color space, alpha mode, vertex/triangle count and fallback reasons.

## Do not do

- Do not make baseColor-only fake material as final material system.
- Do not use a texture atlas as a replacement for glTF material slots.
- Do not ignore sRGB/linear separation.
- Do not add lighting before material input is inspectable.
