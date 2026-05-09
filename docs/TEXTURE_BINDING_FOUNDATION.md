# TEXTURE_BINDING_FOUNDATION — P07

## P11 tangent normal exposure foundation

P11 keeps the P10 lighting/material response and changes normal maps from blocked metadata to real shader input when tangent data exists.

Shader inputs used in Scene08:

- baseColor texture/factor;
- metallicRoughness texture/factors;
- occlusion texture and `occlusionStrength`;
- normal texture and `normalScale`;
- generated or glTF tangent with handedness;
- exposure value, ambient floor, and tone mapping.

Debug views:

```text
Final Shaded
BaseColor
AO
Metallic
Roughness
Normal
NdotL
PBR Status
```

Diagnostics added:

```text
tangentStatus
tangentSource
tangentGeneratedCount
tangentMissingCount
tangentFallbackReason
normalMapAppliedStatus
exposureStatus
exposureValue
ambientFloor
brightnessPreset
normalDebugViewStatus
ndotlDebugViewStatus
```

Limits:

- no fake tangent fallback when POSITION/NORMAL/TEXCOORD_0 are insufficient;
- no shadows;
- no IBL/reflections.

## P10 lighting/material response

P10 keeps the P09 texture slot system and adds a small shader material response layer.

Shader inputs used in Scene07:

- baseColor texture/factor;
- occlusion texture and `occlusionStrength`;
- `metallicFactor`;
- `roughnessFactor`;
- vertex normal for simple lighting;
- directional sun and ambient light controls.

Debug views:

```text
Final Shaded
BaseColor
AO
Metallic
Roughness
PBR Status
```

Diagnostics added:

```text
lightingStatus
sunDirection
sunColor
sunIntensity
ambientColor
ambientIntensity
lightPreset
materialResponseStatus
toneMappingStatus
toneMappingMode
activeDebugView
debugViewStatus
```

Limits:

- no shadows;
- no IBL/reflections;
- no real tangent-space normal response until tangent generation exists;
- `normalMapStatus=blocked_no_tangent` remains the honest status for ToyCar without `TANGENT`.

## P09 PBR texture slots

P09 extends the existing texture slot system. It does not create a new renderer.

Per material slot, the renderer may bind:

- `baseColorTexture`;
- `metallicRoughnessTexture`;
- `normalTexture`;
- `occlusionTexture`.

Supported image storage:

- embedded GLB `image.bufferView`;
- `image/png`;
- `image/jpeg`.

Unsupported sources are explicit:

```text
unsupported_external_uri
unsupported_data_uri
missing
failed + reason
```

P09 diagnostics:

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
```

Shader foundation:

- baseColor path is preserved;
- metallic/roughness maps are sampled and values are available for diagnostics/future lighting;
- AO may darken baseColor;
- normal map upload/sampling is allowed only when safe, otherwise `normalMapStatus=blocked_no_tangent`.

## P08 texture slots

P08 extends P07 single baseColor texture binding into a small texture slot system for multi-primitive static GLB rendering.

Rules:

- baseColor texture is resolved per material slot;
- native renderer can bind a texture descriptor per primitive draw range;
- `textureSlotLimit` is 8 in P08;
- overflow or unsupported texture decode increments skipped/fallback texture diagnostics;
- shader remains simple: `baseColorTexture * baseColorFactor * vertexColor/default`;
- metallicRoughness, normal, AO are handled by P09 foundation;
- emissive texture remains metadata only.

Diagnostics:

```text
textureSlotCount
uploadedTextureCount
textureFallbackCount
skippedTextureCount
textureSlotLimit
```

## Supported now

- Active imported GLB, mesh 0 / primitive 0.
- `pbrMetallicRoughness.baseColorTexture.index`.
- `textures[index].source`.
- `images[source].bufferView`.
- Embedded GLB BIN image bytes through `bufferView.byteOffset` and `bufferView.byteLength`.
- MIME:
  - `image/png`;
  - `image/jpeg` when Android `BitmapFactory` decodes it.
- Android-side decode to ARGB pixels.
- JNI upload through `nativeUploadBaseColorTexture`.
- Vulkan RGBA8 `VkImage`, `VkDeviceMemory`, `VkImageView`, `VkSampler`.
- Minimal combined image sampler descriptor.
- Fragment shader `sampler2D` sampling with `TEXCOORD_0`.
- Fallback white/baseColor path when texture is missing or failed.

## Not supported yet

- normal map;
- metallicRoughness map;
- AO map;
- emissive map;
- PBR lighting;
- mipmaps;
- anisotropy;
- texture arrays;
- multi-material / multi-primitive texture binding;
- external image URI loading.

## Runtime truth

Diagnostics must distinguish mesh draw from texture status:

```text
gpuUploadStatus
drawStatus
meshDrawStatus
textureUploadStatus
baseColorTextureStatus
baseColorTextureName/source
baseColorTextureMimeType
textureWidth
textureHeight
textureBytes
textureFallbackUsed
fallbackCubeStatus
fallbackCubeVisible
```

Texture failure must not mark `drawStatus` failed when mesh draw still works.
