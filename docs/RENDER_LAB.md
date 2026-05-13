# RENDER_LAB — foundation scenes

Render Lab — controlled scene set for future Vulkan renderer validation.

## Scene15 Environment IBL Lab

Patch P18 switches the active lab to:

```text
scene15_environment_ibl_lab
Scene15 Environment IBL Lab
```

Scene15 keeps import/scan/export, Debug ZIP, live FPS, inspector tabs, and P17 Calib/Gloss/Coat controls. It replaces the P17 analytic-only environment status with a lightweight procedural directional sky/ground IBL foundation.

Environment controls in Lighting:

```text
Env 0.0..2.0
Sky: Studio/Warm/Cool/Outdoor/Sunset
Horizon 0.0..1.0
```

Diagnostics added at top level and under `renderLab`:

```text
environmentIblStatus
environmentIblMode = directional_sky_ground_ibl
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
materialResponseStatus = p18_environment_ibl_foundation
pbrQualityTier = mobile_direct_lighting_ibl_v1
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

Debug views added:

```text
Environment
Reflection Direction
Environment Color
```

Out of scope:

- real shadow pass, shadow maps, CSM;
- external cubemap textures or prefiltered IBL pipeline;
- glass, transmission, refraction;
- skeletal animation;
- large UI rewrite.

## Scene14 Specular Gloss Lab

Patch P17B completes the Scene14 material controls:

```text
scene14_specular_gloss_lab
```

Material tab controls:

```text
Calib preset
Calib slider
Gloss slider
Paint Gloss slider
Paint/Coat target status
Debug view
```

Required P17 diagnostics are emitted at top level and under `renderLab`:

```text
specularGlossStatus
specularGlossMode
specularResponseStatus
glossResponseStatus
roughnessRemapV2Status
metallicSpecularBoostStatus
dielectricGlossStatus
fabricSpecularSuppressStatus
specularOverbrightGuardStatus
viewDependentHighlightStatus
paintGlossLiteStatus
paintGlossLiteMode
paintGlossIntensity
paintGlossRoughness
paintGlossMaterialHintStatus
paintGlossPerformanceStatus
glossSliderStatus
glossSliderValue
paintGlossSliderStatus
paintGlossSliderValue
glossUniformUpdateStatus
glossResponseDebugViewStatus
specularGuardDebugViewStatus
paintGlossDebugViewStatus
metalResponseDebugViewStatus
materialTypeSpecularRoutingStatus
fabricMattePreserveStatus
paintMaterialGlossStatus
metalMaterialGlossStatus
rubberMaterialGlossStatus
specularGlossPerformanceStatus
calibrationVisualStrength
calibrationAffectsAlbedo
calibrationAffectsAo
calibrationAffectsRoughness
calibrationVisibleResponseStatus
paintGlossTargetStatus
paintGlossAppliedMaterialCount
paintGlossSkippedFabricCount
paintGlossFallbackRouting
paintGlossVisibleResponseStatus
glossVisibleResponseStatus
glossAffectsSpecularLobe
glossAffectsReflectionWeight
paintTargetDebugViewStatus
calibrationResponseDebugViewStatus
```

Debug views added:

```text
Gloss Response
Specular Guard
Paint Gloss
Metal Response
Paint Target
Calibration Response
```

Performance guard:

- Calib, Gloss, and Paint Gloss update uniforms only.
- No texture rebuild, model upload, tangent rebuild, cubemap texture, CSM, or shadow pass is introduced.

## Scene13 Material Calibration Lab

Patch P16 advances the Render Lab to:

```text
scene13_material_calibration_lab
```

UI title/status:

```text
Render Lab: Scene13 Material Calibration Lab
Inspector: Assets / Camera / Lighting / Material / Debug
Active debug view: Final Shaded / BaseColor / Normal / Roughness / Metallic / AO / Diffuse / Specular / F0 / Reflection / IBL Diffuse / IBL Specular / BRDF Status / Grounding / Contact Shadow / Calibrated Albedo / Material Type / AO Influence / Luminance Guard
```

Scene13 preserves Scene12 inspector tabs, Sun/Amb/Exp/Spec/Refl/Ground sliders, analytic IBL, contact grounding, model import/scan/export, Debug ZIP, and live FPS. It adds a mobile-friendly material calibration stage for albedo energy normalization, diffuse/luminance guarding, AO indirect weighting, roughness remap, metallic/roughness clamps, and material type hints. This is not a cubemap, clearcoat, transmission, CSM, or shadow-map patch.

Required P16 diagnostics:

```text
currentScene = scene13_material_calibration_lab
renderLab.currentLabScene = scene13_material_calibration_lab
renderLab.currentLabSceneName = Scene13 Material Calibration Lab
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
fabricMattePreserveStatus
paintMaterialCalibrationStatus
metalMaterialCalibrationStatus
materialTypeHintStatus
materialSlotCalibrationStatus
calibrationUiStatus
calibrationPreset
calibrationSliderStatus
calibrationSliderValue
calibrationUniformUpdateStatus
calibratedAlbedoDebugViewStatus
materialTypeDebugViewStatus
aoInfluenceDebugViewStatus
luminanceGuardDebugViewStatus
materialCalibrationPerformanceStatus
inspectorUiStatus
iblStatus
environmentReflectionStatus
contactGroundingStatus
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

Material slot diagnostics include:

```text
materialTypeHint = fabric_like / paint_like / metal_like / rubber_like / unknown
calibrationApplied
albedoLuminance
calibratedRoughness
calibratedMetallic
aoInfluence
emissiveGuardApplied
```

Performance guard:

- Calibration preset and Calib slider update uniforms only.
- No GLB parse/upload, texture rebuild, tangent rebuild, or allocation-heavy frame callback work is introduced.
- Debug views are shader branches only.

## Scene12 Grounding Inspector Lab

Patch P15 advances the Render Lab to:

```text
scene12_grounding_inspector_lab
```

UI title/status:

```text
Render Lab: Scene12 Grounding Inspector Lab
Inspector: Assets / Camera / Lighting / Material / Debug
Active debug view: Final Shaded / BaseColor / Normal / Roughness / Metallic / AO / Diffuse / Specular / F0 / Reflection / IBL Diffuse / IBL Specular / BRDF Status / Grounding / Contact Shadow
```

Scene12 preserves Scene11 analytic environment reflections, compact Sun/Amb/Exp/Spec/Refl controls, model import/scan/export, Debug ZIP, and live FPS. It replaces separate Assets/Camera/Debug buttons with one tabbed compact inspector and adds a cheap analytic contact grounding foundation. This is not a Vulkan shadow pass, CSM, or real-time shadow map.

Required P15 diagnostics:

```text
currentScene = scene12_grounding_inspector_lab
renderLab.currentLabScene = scene12_grounding_inspector_lab
renderLab.currentLabSceneName = Scene12 Grounding Inspector Lab
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
iblStatus
iblMode = analytic_environment_approx
environmentReflectionStatus
reflectionIntensity
lightingUiMode
sliderUpdateMode = uniform_only
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

Debug ZIP must include:

```text
engine_runtime_state.json
engine_diagnostics_manifest.json
model_import_state.json
asset_report.json
glb_model_summary.json
debug_zip_runtime_note.txt
```

Performance guard:

- Ground slider updates uniforms only.
- Contact grounding uses the existing model-local position and uploaded bounds foundation.
- No shadow pass, depth shadow map, CSM, mesh rebuild, GLB parse, or texture rebuild is introduced per slider/frame.

## Scene11 Environment Reflection Lab

P14 scene id:

```text
scene11_environment_reflection_lab
```

Runtime status text:

```text
Render Lab: Scene11 Environment Reflection Lab
Lighting: Soft / Studio / Outdoor / Bright / Ultra
Active debug view: Final Shaded / BaseColor / Normal / Roughness / Metallic / AO / Diffuse / Specular / F0 / Reflection / IBL Diffuse / IBL Specular / BRDF Status
```

Scene11 preserves Scene10 lighting controls, model import/scan/export, Debug ZIP, and live FPS while adding a mobile-friendly analytic environment reflection foundation. This is not a full cubemap or prefiltered IBL pipeline.

Required P14 diagnostics:

```text
currentScene = scene11_environment_reflection_lab
renderLab.currentLabScene = scene11_environment_reflection_lab
renderLab.currentLabSceneName = Scene11 Environment Reflection Lab
iblStatus
iblMode = analytic_environment_approx
environmentReflectionStatus = foundation_approx
environmentReflectionMode
environmentSource = procedural_mobile_gradient
reflectionIntensity
reflectionColorStatus
reflectionRoughnessResponseStatus
metallicReflectionStatus
dielectricReflectionStatus
reflectionPerformanceStatus
lightingUiMode = compact_sliders
sliderUpdateMode = uniform_only
sliderTouchStatus
sunSliderStatus
ambientSliderStatus
exposureSliderStatus
specularSliderStatus
reflectionSliderStatus
reflectionDebugViewStatus
iblDiffuseDebugViewStatus
iblSpecularDebugViewStatus
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

Supported P14 foundation:

- procedural sky/ground gradient environment color;
- view-dependent reflection vector;
- roughness reduces reflection sharpness/intensity approximately;
- metallic surfaces receive stronger tinted environment specular;
- dielectric surfaces receive subtle F0-based reflection;
- sliders update uniforms only, with no texture/model rebuild on slider move;
- no shadows, no glass/clearcoat/transmission, no skeletal animation.

## Scene10 Lighting Control Lab

P13 scene id:

```text
scene10_lighting_control_lab
```

Runtime status text:

```text
Render Lab: Scene10 Lighting Control Lab
Lighting: Soft / Studio / Outdoor / Bright / Ultra
Active debug view: Final Shaded / BaseColor / Normal / Roughness / Metallic / AO / Diffuse / Specular / F0 / BRDF Status
```

Scene10 preserves Scene09 BRDF material response, model import/scan/export, Debug ZIP, and live FPS while adding compact lighting controls and analytic specular reflection foundation without real IBL.

Required P13 diagnostics:

```text
currentScene = scene10_lighting_control_lab
renderLab.currentLabScene = scene10_lighting_control_lab
renderLab.currentLabSceneName = Scene10 Lighting Control Lab
lightingControlStatus
lightingUiMode
sunIntensity
ambientIntensity
exposureValue
ambientFloor
specularBoost
specularBoostStatus
reflectionFoundationStatus = analytic_specular_only
reflectionMode
environmentReflectionStatus = not_yet_real_ibl
lightingUniformUpdateStatus
sliderUpdateMode = uniform_only
materialResponseStatus
brdfStatus
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

Supported P13 foundation:

- default Bright Preview uses stronger sun, ambient, exposure, and ambient floor for a less dark ToyCar preview;
- compact step controls update sun, ambient, exposure, and specular boost as uniforms only;
- shader keeps Schlick Fresnel/F0 and adds cheap view-dependent analytic specular fill;
- no cubemap, no shadows, no glass/clearcoat/transmission, no skeletal animation.

## Scene09 BRDF Material Response Lab

P12 scene id:

```text
scene09_brdf_material_response_lab
```

Runtime status text:

```text
Render Lab: Scene09 BRDF Material Response Lab
Material response status: brdf_direct_lit
BRDF status: ok
Active debug view: Final Shaded / BaseColor / AO / Metallic / Roughness / Normal / NdotL / Diffuse / Specular / F0 / BRDF Status
```

Scene09 preserves Scene08 model draw, multi-primitive materials, normal-map support, camera controls, Debug ZIP, and live FPS while upgrading direct material response before IBL/reflections.

Required P12 diagnostics:

```text
currentScene = scene09_brdf_material_response_lab
renderLab.currentLabScene = scene09_brdf_material_response_lab
renderLab.currentLabSceneName = Scene09 BRDF Material Response Lab
brdfStatus
brdfMode
diffuseStatus
specularStatus
fresnelStatus
f0Status
metallicResponseStatus
roughnessResponseStatus
directLightingStatus
materialResponseStatus = brdf_direct_lit
pbrQualityTier = mobile_direct_lighting
brdfPerformanceStatus
activeDebugView
debugViewStatus
diffuseDebugViewStatus
specularDebugViewStatus
f0DebugViewStatus
brdfStatusDebugViewStatus
fpsStatus
fpsUpdateMode
framesRenderedLive
tangentStatus
normalMapAppliedStatus
debugZipStatus
debugZipPath
```

Supported P12 foundation:

- direct mobile BRDF uses baseColor, AO, metallic, roughness, normal/normal map, exposure, and tone mapping;
- Fresnel Schlick is used with `F0 = mix(0.04, baseColor, metallic)`;
- non-metal diffuse is reduced by metallic and specular is controlled by roughness;
- debug views expose diffuse, specular, F0, and BRDF status terms;
- no GLB parse/upload or tangent rebuild is added to the render loop.

Out of scope:

- shadows / CSM;
- IBL/reflections;
- glass/clearcoat/transmission;
- skeletal animation.

## Scene08 Tangent Normal Exposure Lab

P11 scene id:

```text
scene08_tangent_normal_exposure_lab
```

Runtime status text:

```text
Render Lab: Scene08 Tangent Normal Exposure Lab
Tangent status: from_gltf / generated / missing_or_blocked
Normal status: ok / blocked_no_tangent / missing
Exposure: Low / Normal / Bright / Preview
Active debug view: Final Shaded / BaseColor / AO / Metallic / Roughness / Normal / NdotL / PBR Status
```

Scene08 preserves Scene07 lighting and Scene05/P09 multi-primitive GLB draw while adding real tangent-space normal map response when tangent data is available or generated.

Required P11 diagnostics:

```text
currentScene = scene08_tangent_normal_exposure_lab
renderLab.currentLabScene = scene08_tangent_normal_exposure_lab
renderLab.currentLabSceneName = Scene08 Tangent Normal Exposure Lab
tangentStatus
tangentSource
tangentGeneratedCount
tangentFallbackGeneratedCount
tangentMissingCount
tangentDegenerateTriangleCount
tangentFallbackReason
tangentBuildMode
normalMapStatus
normalMapAppliedStatus
fpsStatus
fpsUpdateMode
fpsSampleWindowMs
framesRenderedLive
modelUploadRepeatCount
uploadGenerationId
renderLoopAllocationGuardStatus
exposureStatus
exposureValue
ambientFloor
brightnessPreset
activeDebugView
debugViewStatus
normalDebugViewStatus
ndotlDebugViewStatus
vertexLayout = POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT
vertexStrideBytes = 60
```

Supported P11 foundation:

- GLB `TANGENT` attribute is read when present;
- CPU tangent generation runs for primitives with POSITION, NORMAL, and TEXCOORD_0;
- generated tangents accumulate indexed triangle data per vertex and store handedness;
- vertices without a valid UV tangent basis can receive a normal-orthogonal safe fallback and are counted separately;
- normal textures are sampled only when tangent data is ready;
- FPS diagnostics use a live Java Choreographer sample window instead of input/export event deltas;
- tangent generation and GLB upload are import/upload-time work, not render-loop work;
- exposure and ambient floor defaults make ToyCar clearer without flattening all lighting contrast;
- Normal and NdotL debug views expose normal-map and lighting direction checks.

Out of scope:

- shadows / CSM;
- IBL/reflections;
- glass/clearcoat/transmission;
- skeletal animation.

## Scene07 Lighting Foundation Lab

P10 scene id:

```text
scene07_lighting_foundation_lab
```

Runtime status text:

```text
Render Lab: Scene07 Lighting Foundation Lab
Lighting status: ok
Light preset: Studio / Outdoor / Soft Preview
Material response status: foundation_simple_lit
Active debug view: Final Shaded / BaseColor / AO / Metallic / Roughness / PBR Status
Next: Tangent Generation + Normal Map Real Support
```

Scene07 preserves Scene06 multi-primitive/PBR map foundation and adds a simple, honest lighting/material response layer. It is not full PBR.

Required P10 diagnostics:

```text
currentScene = scene07_lighting_foundation_lab
renderLab.currentLabScene = scene07_lighting_foundation_lab
renderLab.currentLabSceneName = Scene07 Lighting Foundation Lab
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
pbrMapsStatus
metallicRoughnessStatus
normalMapStatus
occlusionMapStatus
drawStatus
gpuUploadStatus
fpsCurrent
frameTimeMs
debugZipStatus
debugZipPath
```

Supported P10 foundation:

- directional sun light and ambient light;
- Studio, Outdoor, and Soft Preview presets;
- simple diffuse plus simple specular approximation;
- baseColor, AO, metallic, and roughness influence final shaded output;
- material debug views for final shaded, baseColor, AO, metallic, roughness, and PBR status;
- tone mapping foundation with `none`, `reinhard`, and `aces_lite` modes.

Out of scope:

- shadow maps;
- IBL/reflections;
- tangent-space normal map response when `TANGENT` is absent;
- alpha/glass/clearcoat/transmission.

## Scene06 PBR Material Maps Lab

P09 scene id:

```text
scene06_pbr_material_maps_lab
```

Runtime status text:

```text
Render Lab: Scene06 PBR Material Maps Lab
modelRenderMode: multi_primitive_static
Next: Lighting Foundation
```

Scene06 preserves Scene05 multi-primitive/baseColor drawing and adds PBR material map foundation without full lighting.

Required PBR diagnostics:

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
currentScene = scene06_pbr_material_maps_lab
renderLab.currentLabScene = scene06_pbr_material_maps_lab
renderLab.currentLabSceneName = Scene06 PBR Material Maps Lab
```

Supported P09 map foundation:

- embedded GLB `image.bufferView` PNG/JPEG decode for baseColor, metallicRoughness, normal, and occlusion textures;
- external URI and data URI are reported as unsupported, not as success;
- metallic/roughness factors and map status are reported per material slot;
- AO can darken baseColor in the current shader;
- normal map is blocked with `normalMapStatus=blocked_no_tangent` when `TANGENT` is absent;
- failed PBR texture upload falls back without failing mesh draw.

Out of scope:

- full PBR lighting;
- shadows;
- IBL/reflections;
- alpha/glass/clearcoat/transmission.

## Scene05 Multi Primitive Render Lab

P08 scene id:

```text
scene05_multi_primitive_render_lab
```

Runtime status text:

```text
Render Lab: Scene05 Multi Primitive Render Lab
modelRenderMode: multi_primitive_static
Next: PBR Material Maps Foundation
```

Scene05 validates active imported GLB rendering across all supported static primitives, material slots with baseColorFactor, baseColor texture slots, skipped primitive diagnostics, fallback cube if all primitives are unsupported, FPS/frameMs HUD, and debug ZIP export status.

Required diagnostics:

```text
modelRenderMode = multi_primitive_static
primitiveCountTotal
primitiveCountRendered
primitiveCountSkipped
unsupportedPrimitiveCount
materialSlotCount
materialSlotCountRendered
textureSlotCount
uploadedTextureCount
textureFallbackCount
skippedTextureCount
textureSlotLimit
fpsCurrent
frameTimeMs
fpsSource
fpsLastStable
frameTimeLastStableMs
debugZipStatus
debugZipPath
debugZipIncludedFiles
debugZipReason
fallbackCubeStatus
fallbackCubeVisible
drawStatus
gpuUploadStatus
```

Current status:

```text
texture binding foundation
currentLabScene = scene04_texture_binding_lab
current implementation = real indexed Vulkan cube fallback + GLB import/scan/CPU metadata parser + first primitive Vulkan GPU upload/draw + baseColor texture decode/upload/sample
```

No shadow/import/performance feature is claimed ready until a real Vulkan implementation and diagnostics proof exist.

## Scene01 Foundation Cube

Purpose:

- first real Vulkan cube target;
- interactive camera baseline;
- depth baseline;
- material constants foundation;
- mesh attribute layout foundation;
- engine diagnostics smoke scene.

Current state:

```text
scene id: scene01_foundation_cube
status: implemented_foundation
geometry: indexed cube
attributes: POSITION,NORMAL,TEXCOORD_0,COLOR_0
depth: color + depth render pass attachment
camera: drag rotate + pinch/buttons zoom, perspective MVP through push constants
material constants: baseColorFactor, metallicFactor, roughnessFactor, emissiveFactor, alphaMode, materialId
shader material use: vertexColor * baseColorFactor.rgb, alpha = baseColorFactor.a
triangle fallback: available/disabled
screenshot/readback: not_available, renderer_readback_not_implemented
```

Expected runtime status:

```text
Render Lab: Scene04 Texture Binding Lab
Import: OK/FAILED/not run
Active model: name or none
Meshes / primitives / materials / textures
GPU Upload: ok/failed
Draw Model: ok/fallback
BaseColor Texture: ok/missing/failed
Texture size: width x height or none
Fallback texture: yes/no
Fallback cube: on/off
Next: PBR Material Maps Foundation
```

## Scene02 Model Import Lab

Purpose:

- import `.glb` through Android file picker;
- copy model into the SOLUM asset library;
- scan imported model assets;
- parse GLB header/chunks and JSON metadata on CPU;
- write model diagnostics without claiming GPU upload or model draw.

Current state:

```text
scene id: scene02_model_import_lab
status: implemented_import_foundation
asset root: /storage/emulated/0/SOLUMCreative/assets/models/imported/
current render: Scene01 cube fallback preserved
gpuUploadStatus: not_implemented
drawStatus: not_implemented
next: GLB Mesh GPU Upload
```

## Scene03 GLB Mesh Render Lab

Purpose:

- upload first active GLB mesh primitive to Vulkan buffers;
- draw the first primitive with POSITION,NORMAL,TEXCOORD_0,COLOR_0 layout;
- preserve cube fallback when no active model or unsupported data is detected;
- report runtime truth for upload, draw, bounds, scale and fallback state.

Current state:

```text
scene id: scene03_glb_mesh_render_lab
status: implemented_foundation
render mode: first_primitive
supported POSITION/NORMAL/TEXCOORD_0: FLOAT VEC3/VEC2
supported indices: UNSIGNED_SHORT / UNSIGNED_INT
unsupported accessor/component: gpuUploadStatus=failed, drawStatus=fallback, exact reason
fallback: Scene01 cube remains visible
next: Texture Binding Foundation
```

## Scene04 Texture Binding Lab

Purpose:

- extract first primitive material `pbrMetallicRoughness.baseColorTexture`;
- decode embedded GLB `image.bufferView` PNG/JPEG through Android `BitmapFactory`;
- upload one RGBA8 baseColor texture to Vulkan image/imageView/sampler;
- sample texture in the current material shader using `TEXCOORD_0`;
- preserve white/baseColor fallback when texture is absent or failed;
- report texture status without changing mesh draw success.

Current state:

```text
scene id: scene04_texture_binding_lab
status: implemented_foundation
supported texture slots: baseColorTexture only
supported image storage: embedded GLB image.bufferView in BIN chunk
supported MIME: image/png, image/jpeg when Android decode supports it
textureUploadStatus: ok/failed/missing
baseColorTextureStatus: ok/failed/missing
fallback: white/baseColor when texture missing/failed
next: PBR Material Maps Foundation
```

## Scene05 Import Lab

Purpose:

- imported mesh/material smoke checks;
- asset schema compatibility checks.

Current state:

```text
scene id: scene05_import_lab
status: planned
```

## Scene06 Performance Lab

Purpose:

- baseline frame timing;
- diagnostics overhead checks;
- future regression snapshots.

Current state:

```text
scene id: scene06_performance_lab
status: planned
```

## Diagnostics

Engine diagnostics must include:

```json
{
  "renderLab": {
    "schema": "solum.render_lab_state",
    "schemaVersion": 1,
    "currentLabScene": "scene04_texture_binding_lab",
    "currentLabSceneName": "Scene04 Texture Binding Lab",
    "renderingStatus": "model_first_primitive",
    "assetImportStatus": "active model",
    "activeModelName": "cottage_medieval.glb",
    "activeModelPath": ".../cottage_medieval.glb",
    "activePrimitiveIndex": 0,
    "gpuUploadStatus": "ok",
    "drawStatus": "ok",
    "meshDrawStatus": "ok",
    "textureUploadStatus": "ok",
    "baseColorTextureStatus": "ok",
    "baseColorTextureName": "baseColorTexture_0",
    "baseColorTextureSource": "textures[0].source=images[0].bufferView=4",
    "baseColorTextureMimeType": "image/png",
    "textureWidth": 1024,
    "textureHeight": 1024,
    "textureBytes": 4194304,
    "textureFallbackUsed": false,
    "uploadedVertexCount": 4374,
    "uploadedIndexCount": 7002,
    "modelVertexLayout": "POSITION,NORMAL,TEXCOORD_0,COLOR_0",
    "modelBoundsMin": [0.0, 0.0, 0.0],
    "modelBoundsMax": [0.0, 0.0, 0.0],
    "modelBoundsCenter": [0.0, 0.0, 0.0],
    "modelScale": 1.0,
    "modelRenderMode": "first_primitive",
    "fallbackCubeVisible": false,
    "fallbackCubeStatus": "off",
    "reason": "first primitive uploaded to Vulkan buffers",
    "cubeStatus": "ok",
    "depthStatus": "ok",
    "cameraStatus": "ok",
    "cameraMvpStatus": "ok",
    "cameraControlsStatus": "ok",
    "cameraYawDeg": 28.0,
    "cameraPitchDeg": -18.0,
    "cameraDistance": 4.2,
    "materialConstantsReady": true,
    "meshAttributeLayoutReady": true,
    "vertexLayout": "POSITION,NORMAL,TEXCOORD_0,COLOR_0",
    "vertexStrideBytes": 44,
    "indexBufferReady": true,
    "uniformOrPushConstantsReady": true,
    "vertexCount": 24,
    "indexCount": 36,
    "material": {
      "materialId": 1,
      "baseColorFactor": [0.92, 0.78, 1.0, 1.0],
      "metallicFactor": 0.0,
      "roughnessFactor": 0.65,
      "emissiveFactor": [0.0, 0.0, 0.0],
      "alphaMode": "OPAQUE"
    },
    "rendererPath": "Android Native Vulkan",
    "screenshot": {
      "status": "not_available",
      "reason": "renderer_readback_not_implemented"
    }
  }
}
```
