# PATCH_HISTORY — история патчей SOLUM

Этот файл фиксирует историю патчей, результаты, ошибки, диагностику и следующие шаги.

## Patch P20 — Runtime UX Stability + Material Workflow Polish

Scope:

- Scene17 Runtime Material Workflow Lab.
- Runtime active model metadata persistence and restore attempt on surface recreate/resume.
- Phone inspector capped to 30 percent height with internal scroll.
- Dynamic inspector alpha while dragging sliders or camera.
- Material tab selected-slot workflow summary, texture ok/missing summary, selected-slot controls, and Reset Selected Slot.
- Assets tab active model/fallback reason display plus Reload Active Model.
- P19 selected-slot overrides, P18 IBL, P17 gloss/calib/coat, Debug ZIP, and live FPS preserved.

Diagnostics:

- `currentScene = scene17_runtime_material_workflow_lab`
- `resumeRestoreStatus`
- `resumeRestoreMode`
- `activeModelPersistenceStatus`
- `activeModelRestoreAttemptCount`
- `activeModelRestoreResult`
- `fallbackCubeReason`
- `surfaceRecreateStatus`
- `inspectorHeightMode = capped_30_percent`
- `inspectorScrollStatus`
- `inspectorDynamicAlphaStatus`
- `materialWorkflowStatus`
- `reloadActiveModelStatus`
- `p19PreservedStatus`
- `p18IblPreservedStatus`
- `p17GlossPreservedStatus`

Out of scope:

- Shadows, shadow maps, CSM.
- Glass/refraction/transmission.
- Skeletal animation.
- Material math rewrite or new render pass.

## Patch P19 — Material Slot Controls / Per-Material Override Foundation

Scope:

- Scene16 Material Slot Editor Lab.
- Compact Material tab slot selector and selected-slot summary.
- Selected-slot metallic, roughness, normal scale, AO, gloss, and coat override controls.
- Renderer applies overrides only to the selected material slot in the existing per-primitive Vulkan draw path.
- Debug views: Selected Material, Material Override, Slot Metallic, Slot Roughness, Slot AO.
- P18 Env/Sky/Horizon controls, P17/P18 Calib/Gloss/Coat controls, import/scan/export, Debug ZIP, live FPS, and inspector tabs preserved.

Diagnostics:

- `currentScene = scene16_material_slot_editor_lab`
- `materialSlotEditorStatus`
- `selectedMaterialSlot`
- `selectedMaterialSlotCount`
- `selectedMaterialTypeHint`
- `selectedMaterialName`
- `perMaterialOverrideStatus = foundation_selected_slot_uniform`
- `perMaterialOverrideMode = cpu_selected_slot_push_constants`
- `selectedSlotMetallicOverride`
- `selectedSlotRoughnessOverride`
- `selectedSlotNormalScaleOverride`
- `selectedSlotAoOverride`
- `selectedSlotGlossOverride`
- `selectedSlotCoatOverride`
- `selectedSlotOverrideApplied`
- `materialSlotControlsUiStatus`
- `perMaterialOverridePerformanceStatus`

Out of scope:

- Shadows, shadow maps, CSM.
- Cubemap texture pipeline.
- Glass/transmission/refraction.
- Skeletal animation.
- Big UI rewrite.

## Patch P18 — Environment Cubemap / Real IBL Foundation

Scope:

- Scene15 Environment IBL Lab.
- Lightweight procedural directional sky/ground/horizon IBL source, without external cubemap files.
- IBL diffuse/specular split in the existing mobile shader path.
- Compact Lighting controls: Env slider, Sky preset cycle, Horizon slider.
- Debug views: Environment, Reflection Direction, Environment Color.
- P17 Calib/Gloss/Coat controls, import/scan/export, Debug ZIP, live FPS, and inspector tabs preserved.

Diagnostics:

- `currentScene = scene15_environment_ibl_lab`
- `environmentIblMode = directional_sky_ground_ibl`
- `environmentSourceType = directional_sky_ground_shader_model`
- `iblDiffuseStatus`
- `iblSpecularStatus`
- `environmentSliderStatus`
- `environmentDebugViewStatus`
- `materialResponseStatus = p18_environment_ibl_foundation`
- `pbrQualityTier = mobile_direct_lighting_ibl_v1`

Out of scope:

- Shadows, shadow maps, CSM.
- External cubemap or prefiltered IBL pipeline.
- Glass/transmission/refraction.
- Skeletal animation.
- Big UI rewrite.

## Patch P16 — Material Calibration Pack

Scope:

- Scene13 Material Calibration Lab.
- Shader/runtime material calibration: albedo energy normalization, diffuse/luminance guard, AO indirect weighting, roughness remap, and metallic/roughness clamps.
- Material type hints foundation per material slot: `fabric_like`, `paint_like`, `metal_like`, `rubber_like`, `unknown`.
- Material tab adds compact Calibration preset cycle and Calib slider.
- Debug views add Calibrated Albedo, Material Type, AO Influence, and Luminance Guard.
- Scene12 inspector tabs, P14/P15 sliders, analytic IBL, contact grounding, Debug ZIP, and live FPS are preserved.

Diagnostics:

- `currentScene = scene13_material_calibration_lab`
- `materialCalibrationStatus`
- `albedoEnergyStatus`
- `luminanceGuardStatus`
- `aoCalibrationStatus`
- `roughnessRemapStatus`
- `materialTypeHintStatus`
- `calibrationPreset`
- `calibratedAlbedoDebugViewStatus`
- `materialCalibrationPerformanceStatus`

Out of scope:

- Real shadows, shadow maps, CSM.
- Cubemap / prefiltered IBL.
- Glass/clearcoat/transmission.
- Skeletal animation.
- Big UI rewrite.

## Patch P15 — Unified Inspector Tabs + Contact Grounding Foundation

Scope:

- Scene12 Grounding Inspector Lab.
- Unified compact inspector tabs: Assets, Camera, Lighting, Material, Debug.
- Lighting tab preserves Sun/Amb/Exp/Spec/Refl and adds Ground `0.0..1.5`.
- Contact grounding foundation uses analytic model-local darkening, not real shadow maps.
- Grounding / Contact Shadow debug view added after BRDF Status.
- Scene11 IBL/reflection foundation, import/scan/export, Debug ZIP, and live FPS are preserved.

Diagnostics:

- `currentScene = scene12_grounding_inspector_lab`
- `inspectorUiMode = tabbed_compact_inspector`
- `contactGroundingStatus = foundation_analytic`
- `contactShadowMode = analytic_blob_or_grounding_approx`
- `groundSliderStatus`
- `groundingDebugViewStatus`
- existing IBL, slider, FPS, and Debug ZIP fields preserved.

Out of scope:

- Full CSM shadows.
- Real-time shadow maps.
- Glass/clearcoat/transmission.
- Skeletal animation.
- Big UI rewrite.

## Patch P14 — Environment Reflections Foundation + Lighting Sliders

Scope:

- Scene11 Environment Reflection Lab.
- Compact native sliders for sun, ambient, exposure, specular, and reflection intensity.
- Analytic procedural sky/ground environment approximation for diffuse and specular response.
- Roughness reduces reflection intensity; metallic surfaces get stronger tinted reflection; dielectric surfaces keep subtle F0 reflection.
- Debug views add Reflection, IBL Diffuse, and IBL Specular.
- Diagnostics add IBL/reflection statuses, slider statuses, reflection intensity, and uniform-only update evidence.
- Live FPS, model import/scan/export, Debug ZIP, and P13 BRDF/direct lighting path are preserved.

Out of scope:

- full cubemap / prefiltered IBL pipeline;
- shadows / CSM;
- glass/clearcoat/transmission;
- skeletal animation;
- big UI rewrite.

## Patch P13 — Lighting UX + Specular Reflection Foundation

Scope:

- Scene10 Lighting Control Lab.
- Compact lighting controls for Soft / Studio / Outdoor / Bright / Ultra presets.
- Step controls for sun, ambient, exposure, and specular boost.
- Bright Preview defaults make the scene more readable without changing debug-view intent.
- Shader adds `specularBoost` and cheap analytic view-dependent specular fill.
- Diagnostics add lighting UI/control, specular boost, reflection foundation, and uniform-only update statuses.
- Live FPS, model import/scan/export, Debug ZIP, and Scene09 BRDF logic are preserved.

Out of scope:

- full IBL cubemap;
- shadows / CSM;
- glass/clearcoat/transmission;
- skeletal animation;
- big UI rewrite.

## Patch P12 — BRDF Material Response Upgrade

Scope:

- Scene09 BRDF Material Response Lab.
- Direct mobile BRDF foundation with baseColor, normal/normal map, AO, metallic, roughness, exposure, and tone mapping.
- Fresnel Schlick and F0 foundation: dielectric 0.04, metallic baseColor, `mix(0.04, baseColor, metallic)`.
- Metallic response reduces diffuse and tints F0; roughness changes specular width/intensity.
- Material debug views now include Diffuse, Specular, F0, and BRDF Status.
- Diagnostics add BRDF/material response statuses and debug-view statuses.
- Live FPS, model draw, multi-primitive draw, texture slots, tangent diagnostics, and Debug ZIP flow are preserved.

Out of scope:

- shadows / CSM;
- IBL/reflections;
- glass/clearcoat/transmission;
- skeletal animation;
- big UI rewrite.

## Patch P11 — Tangent Generation + Real Normal Map Support + Exposure Defaults

Scope:

- Scene08 Tangent Normal Exposure Lab.
- Vertex layout updated to `POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT`.
- GLB tangents are read from `TANGENT` when present.
- CPU tangent generation runs when POSITION, NORMAL, and TEXCOORD_0 exist.
- Shader builds TBN and applies normal texture with `normalScale`.
- Exposure, ambient floor, and brightness presets make ToyCar less dark than P10.
- Material debug views now include Normal and NdotL.
- Diagnostics add tangent, normal-map-applied, exposure, brightness, Normal, and NdotL status fields.

Out of scope:

- shadows / CSM;
- IBL/reflections;
- glass/clearcoat/transmission;
- skeletal animation;
- renderer rewrite.

Next:

```text
Manual runtime verify on device with ToyCar_pbr_test.glb and exported diagnostics/debug ZIP.
```

## Patch P11B — Live FPS Diagnostics + Tangent/Normal Correctness Fix

Scope:

- FPS diagnostics update from a live Java Choreographer sample window.
- Tangent generation keeps indexed triangle accumulation and counts degenerate UV triangles separately.
- Safe normal-orthogonal tangent fallback is counted as fallback, not hidden success.
- Normal map applied status is evaluated per material/primitive instead of globally blocked by partial tangent gaps.
- Diagnostics add FPS status/update mode, tangent fallback/degenerate counts, and upload/render-loop guard fields.

Out of scope:

- shadows / CSM;
- IBL/reflections;
- glass/clearcoat/transmission;
- renderer rewrite.

## Patch P10 — Lighting Foundation + Simple Material Response + Debug Views

Scope:

- Scene07 Lighting Foundation Lab.
- Directional sun light and ambient light foundation.
- Light presets: Studio, Outdoor, Soft Preview.
- Simple shader material response using baseColor, AO, metallic, roughness, diffuse, and specular approximation.
- Material debug views: Final Shaded, BaseColor, AO, Metallic, Roughness, PBR Status.
- Tone mapping foundation with `none`, `reinhard`, and `aces_lite`.
- Diagnostics add lighting, material response, debug view, tone mapping, PBR map, FPS/frame time, and Debug ZIP fields.
- Scene06 ToyCar multi-primitive/PBR map foundation is preserved.

Out of scope:

- shadows;
- IBL/reflections;
- tangent generation / real normal map response;
- alpha/glass/clearcoat/transmission;
- skeletal animation.

Next:

```text
P11 — Tangent Generation + Normal Map Real Support
```

## Patch P08B — Diagnostics FPS + Debug ZIP Completeness Fix

Scope:

- Engine diagnostics now export FPS from the same stable Java/UI HUD source.
- `engine_runtime_state.json` uses `fpsSource = java_ui_frame_delta_stable`, `java_ui_frame_delta_last_stable`, or `not_ready`.
- Added stable FPS fields: `fpsLastStable`, `frameTimeLastStableMs`.
- Debug ZIP packaging writes/collects required files from one report directory:
  - `engine_runtime_state.json`;
  - `engine_diagnostics_manifest.json`;
  - `model_import_state.json`;
  - `asset_report.json`;
  - `debug_zip_runtime_note.txt`.
- `glb_model_summary.json` remains optional when available.
- After successful Debug ZIP export, diagnostics are written again with final `debugZipStatus`, `debugZipPath`, `debugZipIncludedFiles`, and `debugZipReason`.

Out of scope:

- renderer behavior;
- model upload/texture slots;
- P09/PBR features;
- storage permission expansion.

## Patch P09 — PBR Material Maps Foundation

Scope:

- Scene06 PBR Material Maps Lab.
- GLB material extraction for metallicFactor, roughnessFactor, metallicRoughnessTexture, normalTexture scale, occlusionTexture strength, emissive metadata, alphaMode, and doubleSided.
- Embedded GLB PNG/JPEG image decode through bufferView for PBR texture maps.
- Vulkan texture slots extended for baseColor, metallicRoughness, normal, and occlusion per material slot.
- Shader foundation samples metallicRoughness, normal, and occlusion while preserving baseColor draw.
- AO can darken baseColor; normal map is blocked when tangents are absent.
- Diagnostics add `pbrMapsStatus`, `metallicRoughnessStatus`, `normalMapStatus`, `occlusionMapStatus`, `uploadedPbrTextureCount`, and `materialSlotDiagnostics`.

Out of scope:

- full PBR lighting;
- shadows;
- IBL/reflections;
- alpha/glass/clearcoat/transmission;
- skeletal animation.

## Patch P08 — Multi Primitive + Material Slots + FPS + Debug ZIP Export + UI Buttons Cleanup

Scope:

- Scene05 Multi Primitive Render Lab.
- Static multi-primitive GLB extraction and Vulkan upload/draw.
- Per-primitive material slot metadata and baseColorFactor.
- BaseColor texture slot upload with P08 `textureSlotLimit = 8`.
- FPS/frameMs HUD fields from Java/UI-side frame pulse.
- Export Debug ZIP to `/storage/emulated/0/Download/SOLUM_EXPORTS/`.
- Collapsible Assets / Camera / Debug panels with larger touch buttons.
- Future roadmap docs for glTF compatibility, engine roadmap, and UI/product roadmap.

Out of scope:

- skeletal animation;
- full PBR lighting;
- metallicRoughness/normal/AO sampling;
- full product UI rewrite.

Next:

```text
P09 PBR Material Maps Foundation: metallicRoughness + normal + AO.
```

## Формат записи

```markdown
## Patch PXX — Название

### Goal
Что должен был закрыть патч.

### Scope
Что входит.

### Changed files/modules
Список модулей/файлов.

### Build result
SUCCESS / FAILED / NOT TESTED

### Runtime result
SUCCESS / FAILED / NOT TESTED

### Diagnostics
Путь к latest/archive ZIP/report.

### User-visible result
Что пользователь должен увидеть.

### Known issues
Что осталось.

### Lessons
Что запомнить в future patches.

### Next
Следующий шаг.
```

---

## Patch P07 — Texture Binding Foundation + BaseColor Texture + Diagnostics/UI Cleanup

### Goal

Render the active first GLB primitive with its embedded baseColor texture when available, while preserving mesh draw and white/baseColor fallback.

### Scope

- Resolve `pbrMetallicRoughness.baseColorTexture.index` for the active first primitive.
- Decode embedded GLB `image.bufferView` PNG/JPEG through Android `BitmapFactory`.
- Upload one RGBA8 baseColor texture to Vulkan image/memory/view/sampler.
- Add minimal combined image sampler descriptor and `sampler2D` shader path.
- Keep fallback white/baseColor path for missing or failed textures.
- Split mesh draw truth from texture truth in diagnostics.
- Replace misleading fallback cube truth with `fallbackCubeStatus` + `fallbackCubeVisible`.
- Keep Export Diagnostics visible through a compact dock button.

### Out of scope

- PBR lighting.
- normal, metallicRoughness, AO, emissive textures.
- mipmaps, anisotropy, texture arrays.
- skeletal animation.
- renderer replacement.

### Changed files/modules

- `apps/engine/src/main/java/com/solum/engine/MainActivity.java`
- `engine-core/solum-vulkan-core/shaders/triangle.vert`
- `engine-core/solum-vulkan-core/shaders/triangle.frag.glsl`
- `engine-core/solum-vulkan-core/src/solum/texture_resource.hpp`
- `engine-core/solum-vulkan-core/src/solum/pipeline_bundle.hpp`
- `engine-core/solum-vulkan-core/src/solum/renderer_core.hpp`
- `engine-core/solum-vulkan-core/src/solum/renderer_types.hpp`
- `engine-core/solum-vulkan-core/src/solum/render_lab.hpp`
- `engine-core/solum-vulkan-core/src/solum/runtime_diagnostics.hpp`
- `engine-core/solum-vulkan-core/src/solum_engine.cpp`
- `docs/RENDER_LAB.md`
- `docs/GLB_IMPORT_PIPELINE.md`
- `docs/EDITOR_UI_FOUNDATION.md`
- `docs/TEXTURE_BINDING_FOUNDATION.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS through:

```text
bash tools/agent_build_runner.sh
```

Output APK:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
```

### Runtime result

Manual phone verification required.

### Diagnostics

Expected added fields:

```text
meshDrawStatus
textureUploadStatus
baseColorTextureStatus
baseColorTextureName
baseColorTextureSource
baseColorTextureMimeType
textureWidth
textureHeight
textureBytes
textureFallbackUsed
fallbackCubeStatus
```

### User-visible result

Engine shows:

```text
Render Lab: Scene04 Texture Binding Lab
Active model: name
GPU Upload: ok
Draw Model: ok
BaseColor Texture: ok/missing/failed
Texture size
Fallback texture: yes/no
Next: PBR Material Maps Foundation
```

### Known issues

Only baseColor texture is supported. Other PBR maps and lighting are deferred.

### Next

P08 PBR Material Maps Foundation: metallicRoughness + normal + AO.

---

## Patch P06 — GLB Mesh GPU Upload + Single Primitive Render + Compact Editor UI Foundation

### Goal

Render the first primitive of the active imported GLB through the existing Vulkan renderer and reduce Engine UI overlap.

### Scope

- CPU extraction of active model mesh 0 / primitive 0.
- Supported attributes: POSITION, NORMAL, TEXCOORD_0, COLOR_0.
- Supported indices: UNSIGNED_SHORT / UNSIGNED_INT.
- Accessor `byteOffset`, bufferView `byteOffset` and stride handling.
- Vulkan model vertex/index buffers in the existing renderer path.
- First primitive draw with current material constants and baseColorFactor.
- Auto center/scale from model bounds.
- Honest fallback cube when no active model or unsupported data is detected.
- Runtime diagnostics for upload/draw/counts/bounds/scale/fallback.
- Compact top HUD, side rail and collapsible Assets/Camera/Diagnostics panels.

### Out of scope

- Texture binding.
- PBR lighting.
- Skeletal animation.
- New renderer.
- Full editor framework.

### Changed files/modules

- `apps/engine/src/main/java/com/solum/engine/MainActivity.java`
- `engine-core/solum-vulkan-core/src/solum_engine.cpp`
- `engine-core/solum-vulkan-core/src/solum/renderer_core.hpp`
- `engine-core/solum-vulkan-core/src/solum/renderer_types.hpp`
- `engine-core/solum-vulkan-core/src/solum/mesh_resource.hpp`
- `engine-core/solum-vulkan-core/src/solum/render_lab.hpp`
- `engine-core/solum-vulkan-core/src/solum/runtime_diagnostics.hpp`
- `docs/RENDER_LAB.md`
- `docs/GLB_IMPORT_PIPELINE.md`
- `docs/EDITOR_UI_FOUNDATION.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

Pending this patch validation.

### Runtime result

Manual phone verification required.

### Diagnostics

Expected fields:

```text
activeModelName
activeModelPath
activePrimitiveIndex
gpuUploadStatus
drawStatus
uploadedVertexCount
uploadedIndexCount
modelVertexLayout
modelBoundsMin
modelBoundsMax
modelBoundsCenter
modelScale
modelRenderMode = first_primitive
fallbackCubeVisible
reason
```

### User-visible result

Engine shows:

```text
Render Lab: Scene03 GLB Mesh Render Lab
Active model: name
GPU Upload: ok/failed
Draw Model: ok/fallback
Vertices / indices
Fallback cube: on/off
Next: Texture Binding Foundation
```

### Known issues

Texture sampling and PBR are deferred to P07.

### Next

P07 Texture Binding Foundation + BaseColor Texture.

---

## Patch P05 — GLB Import Button + Asset Library + Model Diagnostics

### Goal

Add a real Engine-side GLB import path without claiming model rendering.

### Scope

- `Import GLB` button in `apps/engine`.
- Android file picker through `ACTION_OPEN_DOCUMENT`.
- Copy selected `.glb` into:

```text
/storage/emulated/0/SOLUMCreative/assets/models/imported/
```

- Route reporting: `saf/direct/fallback/failed`.
- `Scan Models` button.
- Active model = last imported model or first scanned `.glb`.
- CPU-only GLB parser foundation:
  - magic `glTF`;
  - version 2;
  - total length;
  - JSON chunk;
  - BIN chunk;
  - metadata counts for scenes/nodes/meshes/primitives/accessors/bufferViews/buffers/materials/images/textures/samplers/skins;
  - attributes POSITION, NORMAL, TEXCOORD_0, COLOR_0, TANGENT, JOINTS_0, WEIGHTS_0.
- Diagnostics:
  - `model_import_state.json`;
  - `asset_report.json`;
  - `engine_runtime_state.json` asset fields.

### Out of scope

- GPU mesh upload.
- Model draw.
- Texture binding.
- PBR lighting.
- Skeletal animation.
- Renderer replacement.

### Changed files/modules

- `apps/engine/src/main/java/com/solum/engine/MainActivity.java`
- `engine-core/solum-vulkan-core/src/solum/render_lab.hpp`
- `engine-core/solum-vulkan-core/src/solum/runtime_diagnostics.hpp`
- `engine-core/solum-vulkan-core/src/solum/renderer_core.hpp`
- `engine-core/solum-vulkan-core/src/solum_engine.cpp`
- `docs/GLB_IMPORT_PIPELINE.md`
- `docs/RENDER_LAB.md`
- `docs/RUNTIME_TRUTH.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS through the allowed SOLUM runner:

```text
bash tools/agent_build_runner.sh
```

Output APK:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
```

### Runtime result

Manual phone verification required.

### Diagnostics

Expected files:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/model_import_state.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/asset_report.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/engine_runtime_state.json
```

### User-visible result

Engine shows:

```text
Import GLB
Scan Models
Render Lab: Scene02 Model Import Lab
Import: OK/FAILED/not run
Active model: name or none
GPU Upload: not implemented
Draw Model: not implemented
Next: GLB Mesh GPU Upload
```

The cube remains the current visible Vulkan render.

### Known issues

Model GPU upload and draw are intentionally `not_implemented`.

### Next

P06 GLB Mesh GPU Upload + Single Primitive Render.

## Patch P03B/P04A — Interactive Camera + Material Constants + Mesh Attribute Layout

### Goal

Extend Scene01 Foundation Cube with user-controlled camera, honest material constants and real mesh attribute layout without replacing the Vulkan renderer.

### Scope

- Added drag yaw/pitch camera controls and pinch/buttons zoom in `apps/engine`.
- Added native camera state fields: `cameraYawDeg`, `cameraPitchDeg`, `cameraDistance`.
- Split diagnostics into `cameraMvpStatus` and `cameraControlsStatus`.
- Added default material constants: `materialId = 1`, `baseColorFactor`, `metallicFactor`, `roughnessFactor`, `emissiveFactor`, `alphaMode = OPAQUE`.
- Extended push constants so fragment shader applies `baseColorFactor`.
- Updated cube mesh to `POSITION,NORMAL,TEXCOORD_0,COLOR_0` with 24 vertices / 36 indices.
- Added `materialConstantsReady`, `meshAttributeLayoutReady`, `vertexStrideBytes`, `vertexLayout`.
- Added STRICT SILENT MODE rules for patch work.

### Out of scope

- Texture binding.
- PBR lighting.
- Shadows.
- Model import.
- The Forge import.

### Expected runtime status

```text
Cube: OK
Depth: OK
Camera: controls OK
Material constants: OK
Mesh layout: OK
Next: Texture Binding / Asset Mesh Upload
```

### Diagnostics

`engine_runtime_state.json` and `engine_diagnostics_manifest.json` must report:

```text
cameraMvpStatus = ok
cameraControlsStatus = ok
cameraYawDeg = current value
cameraPitchDeg = current value
cameraDistance = current value
materialConstantsReady = true
meshAttributeLayoutReady = true
vertexLayout = POSITION,NORMAL,TEXCOORD_0,COLOR_0
vertexStrideBytes = 44
vertexCount = 24
indexCount = 36
```

### Next

P04B Texture Binding Foundation or Asset Mesh Upload, depending on runtime result.

## Patch P03 — Real Vulkan 3D Render Lab Foundation Pack

### Goal

Upgrade the existing Android Native Vulkan triangle path into Scene01 Foundation Cube without creating a parallel renderer.

### Scope

- Extended `RendererCore` existing swapchain/render pass/pipeline path.
- Added indexed cube geometry with position + color attributes.
- Added index buffer support beside the existing vertex buffer helper.
- Added depth image, depth memory, depth image view, depth format selection and color+depth framebuffers.
- Added perspective camera/model/view/projection foundation through vertex push constants.
- Kept triangle validation path as fallback/debug availability, disabled for Scene01.
- Updated runtime status and native/Java diagnostics export fields.

### Out of scope

- Materials/PBR.
- Texture binding.
- Shadows.
- Model import.
- The Forge import.
- Screenshot/readback.

### Expected runtime status

```text
Render Lab: Scene01 Foundation Cube
Cube draw: OK
Depth: OK
Camera: OK
Frames rendered: >= 1
Next: Material Constants / Asset Mesh Upload
```

### Diagnostics

`engine_runtime_state.json` and `engine_diagnostics_manifest.json` must report:

```text
renderLab.currentLabScene = scene01_foundation_cube
cubeStatus = ok
depthStatus = ok
cameraStatus = ok
indexBufferReady = true
uniformOrPushConstantsReady = true
vertexCount = 8
indexCount = 36
screenshot.reason = renderer_readback_not_implemented
```

### Next

P04 Material Constants + Mesh Attribute Layout.

## Patch P02D — Engine Diagnostics SAF + Visible Button Feedback

### Goal

Make Engine diagnostics export visible on screen and writable to `SOLUMCreative` through Android SAF.

### Scope

- Added `Choose Diagnostics Folder` to `apps/engine`.
- Added `ACTION_OPEN_DOCUMENT_TREE` folder picker with persisted read/write URI permission.
- Added SAF export route for:

```text
diagnostics/latest/engine_runtime_state.json
diagnostics/latest/engine_diagnostics_manifest.json
```

- Kept direct public path and app-specific fallback routes.
- Added visible diagnostics status panel.
- Added export button states: `Exporting...`, `Export OK`, `Export Failed`.
- Added `SOLUM_ENGINE_DIAG` logcat events.
- Documented honest screenshot status:

```text
renderer_readback_not_implemented
```

### Changed files/modules

- `apps/engine/src/main/java/com/solum/engine/MainActivity.java`
- `docs/RUNTIME_TRUTH.md`
- `docs/ENGINE_DIAGNOSTICS_EXPORT.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS through the allowed SOLUM runner:

```text
bash tools/agent_build_runner.sh
```

Output APK:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
```

### Runtime result

Manual phone verification required.

### Diagnostics

Expected preferred files:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/engine_runtime_state.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/engine_diagnostics_manifest.json
```

### User-visible result

Engine screen now has:

```text
Choose Diagnostics Folder
Export Engine Diagnostics
Diagnostics folder: configured/not configured
Last export: not run/running/ok/failed
Last export route: saf/direct/fallback/failed
```

### Known issues

Screenshot/readback remains intentionally unavailable.

### Lessons

Runtime diagnostics need persistent SAF route plus visible status, not only a button and Toast.

### Next

P03 upgrade existing triangle renderer to cube + camera + depth.

---

## Patch P02A/P02B/P02C — Runtime Truth, Engine Diagnostics Export, Render Lab Foundation

### Goal

Fix runtime truth before deeper Vulkan work.

### Scope

- P02A: Low-Chatter Mode rules, autopilot intensity modes, explicit engine/companion APK paths.
- P02B: engine-native diagnostics export from `apps/engine`.
- P02C: Render Lab docs and minimal render lab state/config foundation.

### Changed files/modules

- `AGENTS.md`
- `docs/AGENT_AUTOPILOT_WORKFLOW.md`
- `docs/RUNTIME_TRUTH.md`
- `docs/RENDER_LAB.md`
- `docs/RENDER_BACKEND_DECISION.md`
- `docs/PROJECT_MEMORY_INDEX.md`
- `docs/patch_history/PATCH_HISTORY.md`
- `tools/agent_build_runner.sh`
- `tools/build_native_engine.sh`
- `apps/engine/src/main/java/com/solum/engine/MainActivity.java`
- `engine-core/solum-vulkan-core/src/solum/render_lab.hpp`
- `engine-core/solum-vulkan-core/src/solum/runtime_diagnostics.hpp`
- `engine-core/solum-vulkan-core/src/solum_engine.cpp`

### Build result

BUILD_SUCCESS through the allowed SOLUM runner:

```text
bash tools/agent_build_runner.sh
```

Runner executed:

```text
native_command=bash tools/build_native_engine.sh
gradle assembleDebug
```

Output APKs:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
/storage/emulated/0/Download/SOLUM_APK/SOLUM-companion-debug.apk
```

### Runtime result

Manual phone verification required.

### Diagnostics

Engine export writes:

```text
engine_runtime_state.json
engine_diagnostics_manifest.json
```

If renderer readback is unavailable, manifest reports:

```text
screenshot.status = not_available
reason = renderer_readback_not_implemented
```

### User-visible result

Engine UI includes:

```text
Export Engine Diagnostics
```

Build runner reports:

```text
ENGINE_APK=/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
COMPANION_APK=/storage/emulated/0/Download/SOLUM_APK/SOLUM-companion-debug.apk
```

### Known issues

Render Lab is foundation-only. Cube/depth/camera are not implemented in this patch.

### Lessons

Runtime/render truth must live in `apps/engine`. Companion visual packs are evidence support, not renderer truth.

### Next

P03 real Vulkan cube + camera + depth.

---

## Patch P01I — Companion Real Visual Capture Button

### Goal

Add a real manual visual diagnostics button to SOLUM Companion.

### Scope

- Added `Run Visual Diagnostics` to the launcher Activity.
- Added UI status lines for Accessibility service, SAF output folder and last visual diagnostics.
- Added a safe `SolumAccessibilityService.currentInstance` bridge inside companion.
- Kept capture limited to SOLUM allowlist plus `com.solum.companion` self-test.
- Added SAF PNG write for `diagnostics/latest/final.png`.
- Added SAF writes for `action_log.json`, `ui_tree.json` and `visual_diagnostics_manifest.json`.
- Added exact failure reasons:

```text
accessibility_service_not_connected
screenshot_api_unavailable
screenshot_failed
saf_not_configured
package_not_allowlisted
```

### Out of scope

- Vulkan/renderer/material changes.
- taps/gestures/autoclicks.
- MANAGE_EXTERNAL_STORAGE.
- package launch/force-stop automation.
- package installation automation.

### Changed files/modules

- `apps/solum-companion/src/main/java/com/solum/companion/MainActivity.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumDeviceAgentState.kt`
- `apps/solum-companion/README.md`
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`
- `docs/VISUAL_DIAGNOSTICS_PACK.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS through the allowed SOLUM runner:

```text
bash tools/agent_build_runner.sh
```

Runner executed:

```text
gradle assembleDebug
```

Confirmed tasks:

```text
:apps:engine:assembleDebug
:apps:solum-companion:assembleDebug
```

Additional checks:

```text
grep -R --exclude-dir=build "Run Visual Diagnostics" apps/solum-companion
grep -R --exclude-dir=build "takeScreenshot" apps/solum-companion
grep -R --exclude-dir=build "rootInActiveWindow" apps/solum-companion
grep -R --exclude-dir=build "final.png" apps/solum-companion docs
grep -R --exclude-dir=build "package_not_allowlisted" apps/solum-companion docs
python3 tools/mcp_server/solum_mcp_server.py smoke-test
git diff --check
```

### Runtime result

Manual phone verification required after installing the companion APK and enabling Accessibility.

### User-visible result

Companion screen shows:

```text
Accessibility service: enabled / disabled / unknown
SAF output folder: configured / not configured
Last visual diagnostics: ok / partial / failed
Run Visual Diagnostics
```

Expected output after SAF + Accessibility setup:

```text
device_agent/latest/action_log.json
device_agent/latest/ui_tree.json
diagnostics/latest/final.png
diagnostics/latest/visual_diagnostics_manifest.json
```

### Known issues

Build can prove APK creation only. Real screenshot capture must be checked on the phone because Android controls Accessibility consent and screenshot availability.

### Next

Install companion APK, choose `/storage/emulated/0/SOLUMCreative`, enable Accessibility, press `Run Visual Diagnostics`, then inspect the four pack files.

---

## Patch P01H3 — Companion SAF Storage Permission + Evidence Write Fix

### Goal

Fix Companion manual evidence writes on Android scoped storage by using a persisted SAF output folder permission.

### Scope

- Replaced the old output-folder action with `Choose SOLUMCreative Output Folder`.
- Added `ACTION_OPEN_DOCUMENT_TREE` folder picker handling.
- Persisted selected tree URI in `SharedPreferences`.
- Added `takePersistableUriPermission` for read/write access.
- Added SAF evidence writes through `DocumentsContract`.
- Kept direct `/storage/emulated/0/SOLUMCreative` fallback with explicit reason:

```text
direct_public_storage_failed_choose_output_folder
```

- Added `Clear Output Folder Permission`.
- Documented manual SAF setup flow.

### Changed files/modules

- `apps/solum-companion/src/main/java/com/solum/companion/MainActivity.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumDeviceAgentState.kt`
- `apps/solum-companion/README.md`
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS through the allowed SOLUM runner:

```text
bash tools/agent_build_runner.sh
```

Runner executed `gradle assembleDebug`; both app tasks completed:

```text
:apps:engine:assembleDebug
:apps:solum-companion:assembleDebug
```

Additional checks:

```text
grep -R --exclude-dir=build "ACTION_OPEN_DOCUMENT_TREE" apps/solum-companion
grep -R --exclude-dir=build "takePersistableUriPermission" apps/solum-companion
grep -R --exclude-dir=build "DocumentsContract" apps/solum-companion
grep -R --exclude-dir=build "direct_public_storage_failed_choose_output_folder" apps/solum-companion docs
python3 tools/mcp_server/solum_mcp_server.py smoke-test
git diff --check
```

All passed.

### Runtime result

Manual phone verification required after installing the companion APK.

### User-visible result

Companion screen shows:

```text
SAF output folder: configured / not configured
treeUri: redacted/short
```

After choosing `/storage/emulated/0/SOLUMCreative`, `Test Write Evidence Files` should write:

```text
device_agent/latest/action_log.json
diagnostics/latest/visual_diagnostics_manifest.json
```

### Known issues

Runtime grant cannot be proven by build alone. The phone must manually select the folder once.

Build runner copied `SOLUM_LATEST.apk` from the first found APK; use the explicit companion APK path for install:

```text
apps/solum-companion/build/outputs/apk/debug/solum-companion-debug.apk
```

### Report

```text
_work/agent_reports/P01H3_COMPANION_SAF_REPORT.txt
_work/agent_reports/P01H3_COMPANION_SAF_DASHBOARD.html
```

---

## Patch P01H2 — Companion Launcher Activity + Manual Test Screen

### Goal

Make SOLUM Companion a normal openable Android app while preserving the existing Accessibility service.

### Scope

- Added native Android/Kotlin `MainActivity` for `com.solum.companion`.
- Added `MAIN` / `LAUNCHER` manifest entry.
- Added manual buttons:
  - Open Accessibility Settings;
  - Open App Details Settings;
  - Test Write Evidence Files;
  - Open Output Folder.
- Extended `SolumDeviceAgentState` for output path text and manual evidence JSON writes.
- Documented install/open/manual test/Restricted Settings flow.

### Out of scope

- Vulkan renderer/material logic.
- Accessibility tap/gesture automation.
- Telegram UI automation.
- APK auto-install.

### Expected manual evidence outputs

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

### Build result

SUCCESS after one user-approved additional Gradle build cycle.

```text
gradle :apps:solum-companion:assembleDebug
gradle :apps:engine:assembleDebug
```

Both commands completed with `BUILD SUCCESSFUL`.

The earlier Kotlin compile issue in `MainActivity.kt` was corrected by using `ViewGroup.LayoutParams`.

### Report

```text
_work/agent_reports/P01H2_COMPANION_LAUNCHER_REPORT.txt
_work/agent_reports/P01H2_COMPANION_LAUNCHER_DASHBOARD.html
```

### Known issue

Some TECNO/HiOS builds can hide `Allow restricted settings`. If so, use adb/wireless debugging install route.

---

## Patch P01H — Accessibility Companion Real Routes

### Goal

Добавить real route layer для SOLUM Accessibility Companion: status, screenshot, UI tree, action log и visual manifest.

### Scope

- SOLUM-only active package tracking.
- `takeScreenshot` route for API >= 30.
- Honest screenshot failure manifest when screenshot is unavailable.
- UI tree export.
- Action log writer.
- Visual diagnostics manifest.
- Gradle module wiring for `:apps:solum-companion`.
- Docs update for companion, visual pack and MCP future commands.

### Changed files/modules

- `settings.gradle`
- `build.gradle`
- `apps/solum-companion/build.gradle`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumDeviceAgentState.kt`
- `apps/solum-companion/README.md`
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`
- `docs/VISUAL_DIAGNOSTICS_PACK.md`
- `docs/MCP_SERVER_SETUP.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

BUILD_SUCCESS for current wired Gradle app after running:

```text
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk bash tools/agent_build_runner.sh
```

Follow-up BUILD_SUCCESS for companion:

```text
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle :apps:solum-companion:assembleDebug
```

APK:

```text
apps/solum-companion/build/outputs/apk/debug/solum-companion-debug.apk
```

Note: AGP 8.2.2 failed under current Gradle 9.4.1 with `Cannot mutate the dependencies of configuration ':apps:solum-companion:debugCompileClasspath' after the configuration was resolved`. The follow-up updates Android Gradle Plugin to 9.1.0, which provides built-in Kotlin support. During the first AGP 9.1 build, Gradle auto-downloaded Android SDK Build-Tools 36 before the process could be stopped; final build reuses that installed SDK component.

### Runtime result

NOT TESTED on Android device in this patch session. Accessibility Service must be enabled manually.

### Diagnostics

Expected output paths:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

### User-visible result

Companion now has real code paths for allowlisted SOLUM package evidence. Non-allowlisted packages return:

```text
status=blocked
reason=package_not_allowlisted
```

### Known issues

- Companion is still not connected to MCP direct Android service invocation.
- Launch and force-stop remain stub only.
- Tap/gesture automation is intentionally not implemented.
- Runtime screenshot requires API >= 30 and enabled Accessibility Service.
- Runtime screenshot still needs a manual device test after installing/enabling the companion APK.

### Lessons

Device automation must keep package allowlist enforcement at every route boundary.

### Next

P01I — connect MCP/bridge command dispatch to the Android companion service or define the device-side IPC entrypoint.

---

## Patch P01G — Accessibility Companion Skeleton

### Goal

Добавить skeleton будущего Android Accessibility companion без real device action.

### Scope

- `apps/solum-companion` skeleton.
- AccessibilityService stub.
- SOLUM-only allowlist.
- Accessibility service XML с screenshot/window content capability flags.
- Companion/visual diagnostics/MCP docs.

### Changed files/modules

- `apps/solum-companion/README.md`
- `apps/solum-companion/AndroidManifest.xml`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt`
- `apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt`
- `apps/solum-companion/src/main/res/xml/solum_accessibility_service.xml`
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`
- `docs/VISUAL_DIAGNOSTICS_PACK.md`
- `docs/MCP_SERVER_SETUP.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

NOT TESTED — P01G intentionally does not touch Gradle/build system.

### Runtime result

NOT TESTED — stubs only, no real Android device actions.

### Diagnostics

Planned P01H output paths:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

### User-visible result

Repo now contains the companion skeleton and docs for the future MCP/device evidence route.

### Known issues

- Companion is not connected to Gradle.
- No real screenshot capture.
- No real UI tree dump.
- No real launch/force-stop implementation.

### Lessons

Accessibility automation must start from a strict SOLUM-only allowlist and stubs before real device control.

### Next

P01H — implement controlled real screenshot/UI tree/action-log path for allowlisted SOLUM apps.

---

## Patch P01F — Real MCP Server Wrapper

### Goal

Добавить локальный MCP-style wrapper foundation поверх существующего SOLUM tool bridge.

### Scope

- `tools/mcp_server/solum_mcp_server.py`.
- Explicit tool schema.
- MCP-compatible JSON-RPC 2.0 stdio foundation.
- JSON-RPC methods:
  - `initialize`
  - `tools/list`
  - `tools/call`
- CLI support:
  - `smoke-test`
  - `print-config`
- Tools:
  - `solum_print_status`
  - `solum_latest_paths`
  - `solum_generate_report`
  - `solum_send_telegram_report`
  - `solum_foundation_readiness`
- Structured JSON contract:
  - `ok`
  - `tool`
  - `dry_run`
  - `result`
  - `errors`
- Docs for setup, bridge, tools README.
- Human report + Telegram report flow.

### Changed files/modules

- `tools/mcp_server/solum_mcp_server.py`
- `docs/MCP_SERVER_SETUP.md`
- `docs/MCP_LOCAL_TOOLS_BRIDGE.md`
- `tools/agent_tools/README.md`
- `docs/patch_history/PATCH_HISTORY.md`

### Build result

SUCCESS — Python compile/help/tool dry-runs, smoke-test and stdin stdio checks passed locally.

### Runtime result

NOT TESTED — no Android runtime/Vulkan/Gradle changes.

### Diagnostics

Expected report outputs:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

Telegram send result:

```text
summary sent
HTML dashboard attached
TXT report attached
```

P01F follow-up result:

```text
smoke-test passed
serve-stdio stdin test passed
print-config passed
no Telegram send in follow-up
```

### User-visible result

Agent can call safe local SOLUM tools through a stable MCP-style wrapper without arbitrary shell access.

### Known issues

- Wrapper is a MCP-compatible stdio JSON-RPC foundation, not a packaged MCP SDK server.
- Accessibility companion remains planned separately.

### Lessons

MCP integration must wrap existing allowlisted tools instead of exposing shell.

### Next

Connect the wrapper from Codex/another agent config after review.

---

## Patch P01 — Repository / Documentation Foundation

### Goal

Создать начальную память проекта и зафиксировать правила, которые были обсуждены до начала кода.

### Scope

- README.
- Project memory index.
- Current stage.
- Agent rules.
- Architecture rules.
- UX/workflow rules.
- Patch roadmap.
- Rendering target spec.
- Asset format spec.
- Error knowledge base.
- ADR foundation.
- UX negative cases.
- Ideas foundation.
- Repository folder skeleton placeholders.

### Build result

NOT TESTED — documentation-only patch.

### Runtime result

NOT TESTED — documentation-only patch.

### User-visible result

GitHub repo becomes a structured project memory instead of empty repository.

### Known issues

- No build system yet.
- No Android app yet.
- No diagnostics script yet.
- No Vulkan capability check yet.

### Lessons

Start narrow. Do not build multi-APK suite before core/diagnostics/asset/Vulkan foundation.

### Next

Patch P02 — Diagnostics v1 + Vulkan Capability Check.

---

## Patch P01A — Foundation Readiness Check

### Goal

Add a small repo/build/tools readiness layer after agent foundation so future agents can verify Patch 01 state before touching runtime work.

### Scope

- `tools/check_foundation_readiness.sh`.
- Foundation readiness block in `tools/agent_build_runner.sh`.
- `docs/FOUNDATION_READINESS.md`.
- `docs/PROJECT_MEMORY_INDEX.md` link.
- GitHub PR template.

### Build result

NO_VALID_GRADLE_BUILD in current Ubuntu/proot environment.

Foundation preflight result:

```text
FOUNDATION_READINESS=FOUNDATION_READY
```

### Runtime result

NOT TESTED — no runtime/Vulkan changes in this patch.

### Diagnostics

Runner output:

```text
_work/agent_reports/latest/SOLUM_FOUNDATION_READINESS.txt
_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt
/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
```

### User-visible result

Agents get a single runner path that reports whether repo/docs/tools foundation is present before attempting heavier build/runtime work.

### Known issues

- Global Gradle in the current proot/Termux environment still reports that `/root/SOLUM-Platform` is not a valid Gradle build even though Gradle markers exist.
- This patch records the state but does not redesign Gradle or create a new Android app.

### Lessons

Patch 01 readiness should be explicit and cheap. Build environment problems must be reported separately from runtime failures.

### Next

Fix Gradle environment/root recognition as a scoped build-foundation patch, or continue only with tasks that do not require Android Gradle execution.

---

## Patch P01B — Telegram Report + Local Agent Tools Foundation

### Goal

Add a small local report tool so agents can produce a concise Telegram-ready status report without network access or secrets.

### Scope

- `tools/agent_telegram_report.py`.
- `docs/AGENT_LOCAL_TOOLS.md`.
- `docs/PROJECT_MEMORY_INDEX.md` link.
- Local sample output in `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt`.

### Build result

NOT TESTED — local docs/Python tool patch only.

### Runtime result

NOT TESTED — no Android runtime, Vulkan, Gradle or build-system changes.

### Diagnostics

Local report output:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

### User-visible result

Agent can generate one short copyable report with changed files, checks, output paths, known issues and next step.

### Known issues

- No Telegram Bot API by design.
- No message sending by design.
- No token or chat configuration by design.

### Lessons

Agent communication helpers must stay local and secret-free unless the user explicitly approves a real integration.

### Next

Review PR, then keep future Telegram/API integration as a separate explicit patch if needed.

---

## Patch P01C — Real Telegram Send Foundation

### Goal

Add a real Telegram send layer for the existing local SOLUM Telegram report.

### Scope

- `tools/send_telegram_report.py`.
- `docs/TELEGRAM_REPORTING.md`.
- `docs/AGENT_LOCAL_TOOLS.md` update.
- `tools/agent_telegram_report.py` test report generation.

### Changed files/modules

- `tools/send_telegram_report.py`
- `docs/TELEGRAM_REPORTING.md`
- `docs/AGENT_LOCAL_TOOLS.md`
- `docs/patch_history/PATCH_HISTORY.md`
- `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt`

### Build result

NOT TESTED — no Android/Gradle/build-system changes.

Local checks:

```text
python3 -m py_compile tools/send_telegram_report.py
python3 tools/send_telegram_report.py --dry-run
python3 tools/send_telegram_report.py --send
```

### Runtime result

NOT TESTED — no Android runtime, Vulkan, Gradle or build-system changes.

### Diagnostics

Telegram report:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

### User-visible result

Agent can generate a local report and send it to the configured Telegram chat without printing the bot token.

### Known issues

- Telegram send depends on `~/.solum/secrets/telegram.env` and network/API availability.
- Telegram token must stay outside the repo.

### Lessons

Real external integrations must stay explicit, secret-scoped, and separated from report generation.

### Next

Review PR, then use Telegram send only for explicitly approved agent reports.

---

## Patch P01D — Human-Friendly Telegram + HTML Report Pack

### Goal

Сделать отчёты агента понятными обычному человеку: короткий русский Telegram summary + HTML-файл.

### Scope

- `tools/agent_telegram_report.py` создаёт TXT и HTML отчёты.
- `tools/send_telegram_report.py` отправляет summary через `sendMessage` и прикрепляет HTML/TXT через `sendDocument`.
- `docs/HUMAN_REPORTS_SPEC.md`.
- `docs/AGENT_DASHBOARD_REPORTS.md`.
- `docs/CODEX_LAUNCH_MODES.md`.
- `docs/AGENT_LOCAL_TOOLS.md` update.
- Тестовые отчёты в `_work/agent_reports/latest/`.

### Changed files/modules

- `tools/agent_telegram_report.py`
- `tools/send_telegram_report.py`
- `docs/HUMAN_REPORTS_SPEC.md`
- `docs/AGENT_DASHBOARD_REPORTS.md`
- `docs/CODEX_LAUNCH_MODES.md`
- `docs/AGENT_LOCAL_TOOLS.md`
- `docs/patch_history/PATCH_HISTORY.md`
- `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt`
- `_work/agent_reports/latest/SOLUM_AGENT_REPORT.html`
- `_work/agent_reports/latest/SOLUM_AGENT_METRICS.json`

### Build result

NOT TESTED — no Android/Gradle/build-system changes.

Local checks:

```text
python3 -m py_compile tools/agent_telegram_report.py
python3 -m py_compile tools/send_telegram_report.py
python3 tools/agent_telegram_report.py --help
python3 tools/send_telegram_report.py --dry-run
python3 tools/send_telegram_report.py --send
```

### Runtime result

NOT TESTED — no Android runtime, Vulkan, Gradle or build-system changes.

### Diagnostics

Human reports:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
_work/agent_reports/latest/SOLUM_AGENT_METRICS.json
```

### User-visible result

Пользователь получает короткий Telegram summary на русском и HTML dashboard attachment. TXT attachment остаётся optional.

### Known issues

- Telegram send зависит от `~/.solum/secrets/telegram.env`, сети и Telegram API.
- Точные токены недоступны, используется только LOW/MEDIUM/HIGH оценка.
- Runtime/FPS/visual metrics могут быть `not_available`, если diagnostics не запускались.

### Lessons

Отчёты агента должны быть человекочитаемыми, но secrets и network остаются строго отделены от генерации отчёта.

### Next

Review PR.

---

## Patch P01E — MCP/local tools bridge foundation

### Goal

Add a local CLI bridge foundation for future MCP tools without creating a real MCP server or touching runtime/Vulkan/Gradle.

### Scope

- `docs/MCP_LOCAL_TOOLS_BRIDGE.md`.
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`.
- `tools/agent_tools/README.md`.
- `tools/agent_tools/solum_tool_bridge.py`.
- `docs/AGENT_LOCAL_TOOLS.md` update.
- Bridge commands:
  - `generate-report`;
  - `send-telegram-report`;
  - `foundation-readiness`;
  - `latest-paths`;
  - `print-status`.

### Changed files/modules

- `docs/MCP_LOCAL_TOOLS_BRIDGE.md`
- `docs/ACCESSIBILITY_COMPANION_PLAN.md`
- `docs/AGENT_LOCAL_TOOLS.md`
- `docs/patch_history/PATCH_HISTORY.md`
- `tools/agent_telegram_report.py`
- `tools/agent_tools/README.md`
- `tools/agent_tools/solum_tool_bridge.py`

### Build result

NOT TESTED — no Android/Gradle/build-system changes.

Local checks planned:

```text
python3 -m py_compile tools/agent_tools/solum_tool_bridge.py
python3 tools/agent_tools/solum_tool_bridge.py --help
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run
```

P01E follow-up adds structured JSON output for future MCP wrapping:

```text
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py latest-paths --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run --json
```

### Runtime result

NOT TESTED — no Android runtime, Vulkan, Gradle or build-system changes.

### Diagnostics

Expected local report outputs:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

### User-visible result

Agents get one local bridge entry point that can generate reports, send explicitly approved Telegram reports, print latest paths and run foundation readiness.

### Known issues

- Not a real MCP server yet.
- Accessibility companion is a plan only.
- Telegram send depends on `~/.solum/secrets/telegram.env`, network and Telegram API.

### Lessons

MCP integration should start as an allowlisted local bridge before exposing a server. Do not expose arbitrary shell as MCP.

### Next

Wrap the CLI bridge in a real MCP server with structured JSON outputs and add a separate SOLUM-only Accessibility companion when runtime/UI diagnostics need device-side evidence.

---

## Patch P02 — Diagnostics v1 + Vulkan Capability Check

### Goal

Create the first real diagnostics layer and collect device/env/git/storage/Vulkan capability facts before renderer work.

### Scope

- `tools/collect_diagnostics.sh`.
- `tools/report_builder.py`.
- `tools/vulkan_caps/vulkan_caps.c`.
- `tools/vulkan_caps/build_and_run_vulkan_caps.sh`.
- `tools/vulkan_caps/README.md`.
- Latest/archive diagnostics layout.
- `SOLUM_LATEST_DIAGNOSTICS.zip` output.
- `SOLUM_LATEST_REPORT.html` output.
- `vulkan_caps.json` schema.
- `performance_history.json` v1 placeholder.

### Build result

NOT TESTED in GitHub environment — must be run on Termux/Android target device.

### Runtime result

NOT TESTED in GitHub environment — no Android app/runtime in Patch P02.

### Diagnostics

Expected after user runs:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
/storage/emulated/0/SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
```

Fallback root:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

### User-visible result

User runs one command and gets one diagnostics ZIP + one HTML report.

### Known issues

- Vulkan caps may fail from Termux shell on some devices.
- If Vulkan caps fails, report must show honest `status=failed` and include build log.
- No Vulkan swapchain, renderer, triangle, material or shadow system in this patch by design.

### Lessons

Diagnostics must be useful but low-overhead. Patch P02 establishes facts before graphics systems.

### Next

After user provides diagnostics ZIP: fix P02 if needed, then Patch P03 — Asset Schema v1 + Transaction Save.

---

## Patch P03 — Asset Schema v1 + Transaction Save

### Goal

Create the first real SOLUM asset format foundation and safe file write path.

### Scope

- `schemas/asset_manifest.schema.json`.
- `schemas/project_manifest.schema.json`.
- `tools/transaction_save.py`.
- `tools/asset_validator.py`.
- `tools/create_sample_asset.py`.
- `docs/research/NOTE_0002_p03_asset_schema_transaction_save.md`.

### Build result

NOT TESTED in GitHub environment — Python/Termux tool patch.

### Runtime result

NOT TESTED in GitHub environment — no Android runtime.

### Diagnostics

Verified locally by user in Termux:

```text
Sample asset created: /storage/emulated/0/SOLUMCreative/assets/materials/sample_material
Status: valid
Report: /storage/emulated/0/SOLUMCreative/assets/materials/sample_material/validation_report.json
```

### User-visible result

User can create a sample material asset folder and validate it.

### Known issues

- No Asset Hub UI yet.
- No material preview yet.
- No glTF import yet.
- No zip/bundle format yet.

### Lessons

Asset format must exist before UI tools. Transaction save prevents corrupted files and gives future editors safe write behavior.

### Next

Patch P04 — Vulkan Foundation v1 or Patch P03A fix if validation fails on device.

---

## Patch P04 — Vulkan Foundation v1

### Goal

Create the first Android APK runtime path for SOLUM Engine and verify Android Native Vulkan separately from Termux llvmpipe.

### Scope

- Root Gradle settings and Android application module.
- `apps/engine` Android Activity and SurfaceView.
- Manual prebuilt `.so` route for Termux.
- Native C++ Vulkan foundation.
- VkInstance.
- Android surface.
- physical device selection.
- VkDevice.
- swapchain creation.
- runtime Vulkan capability report.
- APK output script.

### Build result

NOT TESTED in GitHub environment — must be built in Termux.

### Runtime result

NOT TESTED in GitHub environment — must run APK on target Android phone.

### Diagnostics

Expected build outputs:

```text
/storage/emulated/0/SOLUMCreative/releases/latest/SOLUM_LATEST.apk
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Expected runtime output after launching APK:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/runtime_vulkan_caps.json
```

### User-visible result

APK opens `SOLUM Engine` and shows runtime status overlay:

```text
Renderer path: Android Native Vulkan
GPU: ...
Swapchain: created
```

### Known issues

- P04 may only create swapchain/runtime report. Clear/triangle draw pass can move to P04A after build/runtime foundation is verified.
- Manual prebuilt `.so` route is chosen for Termux reliability.
- Real Mali-G57 appears only through Android APK runtime path, not Termux shell Vulkan.

### Lessons

Do not start PBR/materials/shadows before Android Vulkan runtime path is proven.

### Next

Run `bash tools/build_engine_apk.sh`, install/open APK manually, then send diagnostics ZIP/logs if build or runtime fails.

---

## Patch P05 — Vulkan Frame Loop + First Render Pass

### Goal

Add the first permanent Vulkan frame-loop layer after P04 swapchain proof.

This is not a throwaway triangle demo. P05 creates the renderer foundation that future mesh/material/shadow passes will extend.

### Scope

- Swapchain image views.
- Render pass.
- Framebuffers.
- Command pool.
- Command buffers.
- Image-available semaphore.
- Render-finished semaphore.
- In-flight fence.
- Acquire → record → submit → present path.
- First clear-color frame.
- `framesRendered` and `firstFrameRendered` runtime state.
- Research note `NOTE_0004_p05_vulkan_frame_loop_render_pass.md`.

### Build result

NOT TESTED in GitHub environment — must be built in Termux.

### Runtime result

NOT TESTED in GitHub environment — must run APK on target Android phone.

### Diagnostics

Expected build outputs:

```text
/storage/emulated/0/SOLUMCreative/releases/latest/SOLUM_LATEST.apk
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Expected runtime overlay after launch:

```text
Render pass: clear color OK
Frames rendered: 1
```

### User-visible result

APK should show a dark teal Vulkan clear color and status text confirming first render pass/present succeeded.

### Known issues

- Runtime report export is still tracked separately by `docs/diagnostics/KNOWN_ISSUE_P04_RUNTIME_REPORT_EXPORT.md`.
- No graphics pipeline or triangle draw yet.
- No shaders, PBR, materials, meshes or shadows yet.

### Lessons

First renderer layer must prove command submission and present before adding shader pipeline/material complexity.

### Next

If P05 build/runtime succeeds: P06/P05A — graphics pipeline + first validation triangle draw.

---

## Patch P06 — Graphics Pipeline + First Validation Triangle

### Goal

Create the first permanent graphics pipeline layer and prove a real Vulkan draw call.

This is not a throwaway triangle demo. The triangle validates shader modules, pipeline layout, graphics pipeline and `vkCmdDraw` inside the final Vulkan render path.

### Scope

- GLSL shader sources for validation triangle.
- `tools/build_shaders.sh` for GLSL → SPIR-V header generation.
- Generated shader headers ignored by Git.
- Shader module creation.
- Pipeline layout.
- Graphics pipeline.
- Pipeline binding inside render pass.
- `vkCmdDraw(3)` validation draw.
- Runtime status: `Triangle draw: OK`.
- Build summary updated for P06.
- Research note `NOTE_0005_p06_graphics_pipeline_triangle.md`.

### Build result

NOT TESTED in GitHub environment — must be built in Termux.

### Runtime result

NOT TESTED in GitHub environment — must run APK on target Android phone.

### Diagnostics

Expected build outputs:

```text
/storage/emulated/0/SOLUMCreative/releases/latest/SOLUM_LATEST.apk
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Expected build text:

```text
SOLUM SHADER BUILD: OK
SOLUM BUILD RESULT: OK
Patch: P06 Graphics Pipeline + First Validation Triangle
```

Expected runtime overlay after launch:

```text
Render pass: clear color OK
Triangle draw: OK
Frames rendered: 1
```

### User-visible result

APK should show an orange validation triangle over the dark teal Vulkan clear color.

### Known issues

- Requires `glslc` or `glslangValidator` available in Termux.
- Runtime report export remains tracked separately by `docs/diagnostics/KNOWN_ISSUE_P04_RUNTIME_REPORT_EXPORT.md`.
- No vertex buffer or mesh upload yet.
- No materials, textures, PBR, lighting or shadows yet.

### Lessons

Shader/pipeline proof should be separated from vertex buffer and mesh upload. This keeps failure causes diagnosable.

### Next

If P06 build/runtime succeeds: P07 — vertex buffer + simple mesh upload path.
