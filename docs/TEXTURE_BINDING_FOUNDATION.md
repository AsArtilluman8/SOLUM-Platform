# TEXTURE_BINDING_FOUNDATION — P07

## P22 emissive material presets lab

P22 switches the active lab to `scene19_emissive_material_presets_lab`.

The texture/material binding path remains the existing per-material-slot Vulkan path. P22 adds safe emissive factor metadata and selected-slot presets through uniforms/push constants only. No bloom, real light contribution, new pass, transparent sorting, glass/refraction/transmission, texture rebuild, or model reupload is added for slider or preset changes.

Presets:

- Balanced
- Car Paint
- Metal
- Fabric
- Rubber
- Plastic
- Glass Metadata
- Emissive Safe

Emissive handling:

- glTF `emissiveFactor` is recorded per material slot.
- `emissiveTexture` is metadata-only unless an existing safe texture path is available.
- emissive contribution is clamped in the final shaded path and does not illuminate other objects.

Alpha/cutout foundation:

- glTF `alphaMode` is recognized per material slot: `OPAQUE`, `MASK`, and `BLEND`.
- `MASK` uses baseColor factor alpha and baseColor texture alpha for shader discard.
- `alphaCutoff` is read from glTF material metadata and can be adjusted through a uniform-only Material tab slider.
- `BLEND` is preserved as metadata and routed to safe cutout/opaque fallback diagnostics until a later transparent-sorting stage.

Double-sided foundation:

- glTF `doubleSided` is recorded per slot.
- Current raster state uses `VK_CULL_MODE_NONE`, so double-sided cards/leaves/fabric planes can be visible without a new pipeline permutation.
- Shader normal handling uses face orientation for a safe two-sided normal foundation.

Material hints added:

- `cutout_like`
- `glass_like` metadata-only, rendered as safe opaque/cutout
- `decal_like`

Debug views added:

- Alpha Mask
- Alpha Mode
- Double Sided
- Cutout Hint
- Transparency Status

## P20 runtime material workflow polish

P20 keeps the P19 selected-slot override foundation and switches the active lab to `scene17_runtime_material_workflow_lab`.

Runtime UX and workflow additions:

- active model metadata is persisted after successful import/upload;
- surface recreate/resume attempts safe re-upload from cached parsed model/local path before allowing fallback cube;
- diagnostics expose `resumeRestoreStatus`, `activeModelPersistenceStatus`, `activeModelRestoreAttemptCount`, `activeModelRestoreResult`, `fallbackCubeReason`, and `surfaceRecreateStatus`;
- inspector is capped to 30 percent of screen height when expanded and scrolls internally;
- slider and camera drag temporarily lower inspector alpha for live visual inspection;
- Material tab shows selected slot name/hint, texture summary, selected-slot controls, and Reset Selected Slot;
- Assets tab shows active model, fallback reason, Import/Scan/Export, and Reload Active Model.

Preserved material/render rules:

- selected-slot overrides remain uniform/push-constant only;
- no texture rebuild while sliding;
- no model reupload while sliding;
- fabric matte routing remains guarded;
- P18 IBL and P17 gloss/calib/coat diagnostics remain active.

## P19 material slot override foundation

P19 keeps the P18 directional sky/ground IBL foundation and switches the lab to `scene16_material_slot_editor_lab`.

New selected-slot inputs:

- selected material slot index;
- metallic override `0.0..1.0`;
- roughness override `0.0..1.0`;
- normal scale override `0.0..2.0`;
- AO override `0.0..1.5`;
- gloss override `0.0..1.0`;
- coat override `0.0..1.0`.

Routing:

- overrides are applied only to primitives whose material slot equals the selected slot;
- other slots keep their original material factors;
- no texture rebuild, model upload, tangent rebuild, extra render pass, cubemap upload, shadow map, CSM, glass, transmission, or refraction is introduced;
- implementation mode is `foundation_selected_slot_uniform` using per-primitive push constants in the existing Vulkan path.

Debug views added:

```text
Selected Material
Material Override
Slot Metallic
Slot Roughness
Slot AO
```

Diagnostics added:

```text
materialSlotEditorStatus
selectedMaterialSlot
selectedMaterialSlotCount
selectedMaterialTypeHint
selectedMaterialName
selectedMaterialSummaryStatus
perMaterialOverrideStatus
perMaterialOverrideMode
selectedSlotMetallicOverride
selectedSlotRoughnessOverride
selectedSlotNormalScaleOverride
selectedSlotAoOverride
selectedSlotGlossOverride
selectedSlotCoatOverride
selectedSlotOverrideApplied
perMaterialUniformUpdateStatus
materialSlotControlsUiStatus
perMaterialOverridePerformanceStatus
```

## P18 environment IBL foundation

P18 keeps P17 material calibration/gloss controls and switches the lab to `scene15_environment_ibl_lab`.

New shader/control inputs:

- `environmentIntensity` push constant from the Lighting tab Env slider.
- `environmentPreset` push constant for Studio, Warm, Cool, Outdoor, Sunset.
- `horizonStrength` push constant from the optional Horizon slider.
- Directional sky/ground/horizon environment sampling in shader.

Material response:

- IBL diffuse samples the environment from the normal direction.
- IBL specular samples the environment from the reflection direction.
- Roughness approximately blurs and reduces specular energy.
- Metallic tints reflections with base color.
- Dielectric reflections stay subtle through F0.
- Fabric material hints keep specular response suppressed.

This is a foundation only:

- no external cubemap file;
- no heavy prefilter pipeline;
- no extra render pass;
- no texture upload per slider;
- no shadows, shadow maps, or CSM;
- no glass/transmission/refraction.

Diagnostics added:

```text
environmentIblStatus
environmentIblMode
environmentSourceStatus
environmentSourceType
environmentSkyColorStatus
environmentGroundColorStatus
environmentHorizonStatus
environmentPerformanceStatus
iblDiffuseStatus
iblSpecularStatus
iblRoughnessResponseStatus
iblMetallicResponseStatus
iblDielectricResponseStatus
iblFabricPreserveStatus
iblOverbrightGuardStatus
environmentUiStatus
environmentPreset
environmentIntensity
environmentSliderStatus
skyPresetStatus
horizonControlStatus
environmentUniformUpdateStatus
environmentDebugViewStatus
reflectionDirectionDebugViewStatus
environmentColorDebugViewStatus
iblPerformanceStatus
```

## P16 material calibration pack

P16 keeps the P15 inspector, analytic IBL, contact grounding, import/export/debug ZIP, and live FPS, then switches the lab to `scene13_material_calibration_lab`.

New shader/control inputs:

- `calibrationPreset` push constant: Neutral, Matte Safe, Balanced, Punchy.
- `calibrationStrength` push constant from the Material tab Calib slider.
- `materialTypeHint` per material slot: `fabric_like`, `paint_like`, `metal_like`, `rubber_like`, `unknown`.
- albedo energy normalization, diffuse brightness clamp, luminance guard, AO indirect weighting, roughness remap, and metallic/roughness clamps.

This is a foundation only:

- no cubemap or prefiltered IBL;
- no real shadows, shadow maps, or CSM;
- no clearcoat/glass/transmission;
- no texture/model rebuild from calibration sliders.

Debug views added:

```text
Calibrated Albedo
Material Type
AO Influence
Luminance Guard
```

Diagnostics added:

```text
materialCalibrationStatus
materialCalibrationMode
albedoEnergyStatus
albedoClampStatus
diffuseClampStatus
luminanceGuardStatus
aoCalibrationStatus
roughnessRemapStatus
metallicRoughnessClampStatus
emissiveGuardStatus
materialTypeHintStatus
materialSlotCalibrationStatus
calibrationPreset
calibrationSliderValue
calibrationUniformUpdateStatus
calibratedAlbedoDebugViewStatus
materialTypeDebugViewStatus
aoInfluenceDebugViewStatus
luminanceGuardDebugViewStatus
materialCalibrationPerformanceStatus
```

## P15 contact grounding + inspector

P15 keeps the P14 analytic IBL/reflection foundation and adds `scene12_grounding_inspector_lab`.

New shader/control inputs:

- `contactShadowIntensity` push constant, controlled by the Ground slider in the Lighting inspector tab.
- `inLocalPosition` varying from the existing model vertex position.
- analytic contact mask near the normalized model bottom, used as a cheap grounding darken.

This is a foundation approximation only:

- no Vulkan shadow pass;
- no CSM;
- no depth shadow map;
- no mesh rebuild on slider move;
- no texture rebuild on slider move.

Debug views:

- Existing views are preserved through BRDF Status.
- New view: Grounding / Contact Shadow.

Diagnostics added:

```text
inspectorUiStatus
inspectorUiMode = tabbed_compact_inspector
activeInspectorTab
assetsTabStatus
cameraTabStatus
lightingTabStatus
materialTabStatus
debugTabStatus
contactGroundingStatus = foundation_analytic
contactShadowStatus
contactShadowMode = analytic_blob_or_grounding_approx
contactShadowIntensity
contactShadowPerformanceStatus
groundingUsesModelBounds
groundingUniformUpdateStatus
groundSliderStatus
contactGroundingSliderStatus
groundingDebugViewStatus
```

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
