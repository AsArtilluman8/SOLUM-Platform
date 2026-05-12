# TEXTURE_BINDING_FOUNDATION — P07

## P14 analytic environment reflection foundation

P14 keeps the P13 direct BRDF and lighting controls, switches Scene11 to compact sliders, and adds a mobile-friendly analytic environment approximation. It is intentionally named `analytic_environment_approx`, not full cubemap IBL.

Shader/control inputs used in Scene11:

- sun intensity `0.5..4.0`;
- ambient intensity `0.1..2.0`;
- exposure `0.8..3.0`;
- `specularBoost` `0.5..3.0`;
- `reflectionIntensity` `0.0..2.0`;
- procedural sky/ground environment gradient;
- view-dependent reflection vector;
- roughness-weighted environment specular;
- metallic-tinted environment reflection and subtle dielectric F0 reflection.

Debug views:

```text
Final Shaded
BaseColor
Normal
Roughness
Metallic
AO
Diffuse
Specular
F0
Reflection
IBL Diffuse
IBL Specular
BRDF Status
```

Diagnostics added:

```text
iblStatus
iblMode
environmentReflectionStatus
environmentReflectionMode
environmentSource
reflectionIntensity
reflectionColorStatus
reflectionRoughnessResponseStatus
metallicReflectionStatus
dielectricReflectionStatus
reflectionPerformanceStatus
lightingUiMode = compact_sliders
sliderUpdateMode = uniform_only
reflectionDebugViewStatus
iblDiffuseDebugViewStatus
iblSpecularDebugViewStatus
```

Limits:

- no external cubemap or prefiltered environment pipeline yet;
- no shadows;
- no glass/clearcoat/transmission;
- no skeletal animation.

## P13 lighting control and specular foundation

P13 keeps the P12 BRDF foundation and adds Scene10 lighting UX plus a cheap analytic specular/reflection foundation without real IBL.

Shader/control inputs used in Scene10:

- sun intensity `0.5..4.0`;
- ambient intensity `0.1..2.0`;
- exposure `0.8..3.0`;
- ambient floor for readable dark areas;
- `specularBoost` `0.5..3.0`;
- Schlick Fresnel/F0, roughness width, metallic tinted specular;
- analytic view-dependent specular fill named `analytic_specular_only`.

Debug views:

```text
Final Shaded
BaseColor
Normal
Roughness
Metallic
AO
Diffuse
Specular
F0
BRDF Status
```

Diagnostics added:

```text
lightingControlStatus
lightingUiMode
specularBoost
specularBoostStatus
reflectionFoundationStatus
reflectionMode
environmentReflectionStatus
lightingUniformUpdateStatus
sliderUpdateMode
```

Limits:

- no full IBL cubemap;
- no shadows;
- no glass/clearcoat/transmission;
- no skeletal animation.

## P12 BRDF material response foundation

P12 keeps the P11 texture/tangent foundation and upgrades direct material response before IBL/reflections.

Shader inputs used in Scene09:

- baseColor texture/factor;
- metallicRoughness texture/factors;
- occlusion texture and `occlusionStrength`;
- normal texture and `normalScale` when tangent data is available;
- direct sun, ambient floor, exposure, and tone mapping;
- Fresnel Schlick with dielectric `F0 = 0.04` and metallic `F0 = baseColor`.

Debug views:

```text
Final Shaded
BaseColor
AO
Metallic
Roughness
Normal
NdotL
Diffuse
Specular
F0
BRDF Status
```

Diagnostics added:

```text
brdfStatus
brdfMode
diffuseStatus
specularStatus
fresnelStatus
f0Status
metallicResponseStatus
roughnessResponseStatus
directLightingStatus
pbrQualityTier
brdfPerformanceStatus
diffuseDebugViewStatus
specularDebugViewStatus
f0DebugViewStatus
brdfStatusDebugViewStatus
```

Limits:

- no shadows;
- no IBL/reflections;
- no glass/clearcoat/transmission;
- no skeletal animation.

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
tangentFallbackGeneratedCount
tangentMissingCount
tangentDegenerateTriangleCount
tangentFallbackReason
tangentBuildMode
normalMapAppliedStatus
fpsStatus
fpsUpdateMode
fpsSampleWindowMs
framesRenderedLive
exposureStatus
exposureValue
ambientFloor
brightnessPreset
normalDebugViewStatus
ndotlDebugViewStatus
```

Limits:

- safe tangent fallback is allowed only from a valid normal with a stable orthogonal tangent and is reported separately;
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
