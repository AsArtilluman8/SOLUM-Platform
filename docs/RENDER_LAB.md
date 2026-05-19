# RENDER_LAB — foundation scenes

Render Lab — controlled scene set for future Vulkan renderer validation.

## Scene24 Transparent Glass Pass Lab

Patch: P27 — Transparent Glass Pass v1

Current scene:

```text
scene24_transparent_glass_pass_lab
Scene24 Transparent Glass Pass Lab
```

## P27B — Universal Material Role + Transparent Glass Routing Fix

Scene24 keeps the P27 transparent glass pass, but routing is no longer tied blindly to `selectedMaterialSlot`.

Material Role foundation:

```text
Roles: Glass, Paint, Metal, Fabric, Rubber, Plastic, Emissive, Unknown
Detection: material name + alpha metadata + existing materialTypeHint + metallic/emissive factors
No asset-specific slot hardcode.
```

Transparent Glass routing:

```text
Fake Safe: P26 fake glass fallback.
Transparent v1: selected slot only when role=Glass, otherwise best Glass candidate.
Auto: best Glass candidate.
Manual override: Assign Selected As Glass.
```

New Material tab controls:

```text
Selected: slot/name/role
Active Glass: slot/name/role/source
Select Glass Candidate
Assign Selected As Glass
Clear Manual Role
```

Diagnostics added:

```text
materialRoleSystemStatus
materialRoleDetectionMode
selectedMaterialRole
selectedMaterialRoleConfidence
selectedMaterialRoleSource
materialRoleCandidateCount
glassCandidateCount
bestGlassCandidateSlot
bestGlassCandidateName
bestGlassCandidateConfidence
bestGlassCandidateSource
materialRoleNoHardcodeStatus = ok_no_asset_specific_slot_hardcode
transparentGlassRoutingStatus
transparentGlassRoutingMode
activeTransparentGlassSlot
activeTransparentGlassMaterialName
activeTransparentGlassMaterialRole
activeTransparentGlassRoleSource
activeTransparentGlassSlotSource
transparentGlassSelectedSlotAllowedStatus
transparentGlassSelectedSlotRejectedStatus
transparentGlassAutoDetectStatus
transparentGlassManualOverrideStatus
transparentGlassFallbackReason
transparentGlassWrongSlotGuardStatus = ok_non_glass_slots_not_transparent_by_default
materialRoleUiStatus
selectedSlotRoleSummaryUiStatus
activeGlassSlotSummaryUiStatus
selectGlassCandidateButtonStatus
assignSelectedAsGlassButtonStatus
clearManualRoleButtonStatus
selectedSlotHighlightToggleStatus
transparentGlassSkippedNonGlassCount
transparentGlassSkippedFabricCount
transparentGlassSkippedMetalCount
transparentGlassSkippedPaintCount
p27TransparentPassPreservedStatus
p27bPerformanceStatus
p27bNoExtraHeavyPassStatus
p27bNoTextureRebuildStatus
p27bNoModelReuploadStatus
p27bNoFrameGlbParseStatus
p27bNoFrameFileWriteStatus
p27bNoShadowStatus
p27bNoRealMirrorStatus
p27bNoSsrStatus
p27bNoRealRefractionStatus
materialRoleDebugViewStatus
glassCandidateDebugViewStatus
activeGlassSlotDebugViewStatus
transparentRoutingDebugViewStatus
wrongSlotGuardDebugViewStatus
```

Debug views added:

```text
Material Role
Glass Candidate
Active Glass Slot
Transparent Routing
Wrong Slot Guard
```

P27 preserves P26 polished fake glass, P25 glass foundation, P24 fake cubemap/procedural reflection, P23 clearcoat, P22 emissive/presets, P21 alpha/cutout, P20 runtime workflow, P19 selected-slot controls, P18 IBL, Debug ZIP, and live FPS.

Transparent glass pass v1:

- Opaque primitives draw first.
- One selected glass-eligible material slot can draw again after opaque primitives through a transparent Vulkan pipeline.
- Transparent pipeline uses alpha blending and disables depth writes while keeping depth testing.
- Glass opacity controls real blended surface alpha in Transparent v1.
- Glass tint, P26 edge/Fresnel reflection, P24 reflection source, and roughness response remain visible.
- `Fake Safe` keeps the P26 fake glass path.
- `Auto` can fall back to fake glass during motion guard or when the selected slot is not glass-eligible.

Limits:

- No full transparent world sorting.
- No real refraction.
- No screen-space refraction.
- No real mirror, SSR, raytracing, shadow pass, shadow maps, or CSM.
- No texture rebuild, model reupload, GLB parse, or file writes in the frame loop.

Material tab:

- Glass Enable is preserved.
- Glass Render Mode cycles Fake Safe / Transparent v1 / Auto.
- Existing Glass preset, opacity, tint, edge, rough, and clearcoat controls remain.
- Compact summary shows glass mode and selected slot.

Debug views added:

```text
Transparent Glass Mask
Transparent Glass Alpha
Transparent Glass Draw Order
Transparent Glass Fallback
Transparent Glass Safety
```

Required honest diagnostics:

```text
transparentGlassPassMode = opaque_first_selected_glass_after
transparentGlassRefractionStatus = deferred_no_real_refraction
transparentGlassSortingStatus = limited_selected_slot_no_full_sort
```

Diagnostics added at top level and under `renderLab`:

```text
transparentGlassPassStatus
transparentGlassPassMode
transparentGlassSelectedSlotStatus
transparentGlassDrawOrderStatus
transparentGlassBlendStatus
transparentGlassOpacityStatus
transparentGlassTintStatus
transparentGlassFresnelStatus
transparentGlassReflectionStatus
transparentGlassSortingStatus
transparentGlassRefractionStatus
transparentGlassFallbackStatus
transparentGlassPerformanceStatus
transparentGlassUiStatus
glassRenderModeStatus
activeGlassRenderMode
transparentGlassToggleStatus
glassModeSummaryUiStatus
p26GlassPolishPreservedStatus
fakeGlassFallbackStatus
glassAutoFallbackStatus
glassMotionFallbackStatus
transparentGlassMaterialRoutingStatus
transparentGlassAppliedSlot
transparentGlassAppliedMaterialCount
transparentGlassSkippedOpaqueCount
transparentGlassFabricGuardStatus
transparentGlassMetalGuardStatus
transparentGlassCarPaintGuardStatus
transparentGlassMaskDebugViewStatus
transparentGlassAlphaDebugViewStatus
transparentGlassDrawOrderDebugViewStatus
transparentGlassFallbackDebugViewStatus
transparentGlassSafetyDebugViewStatus
p27PerformanceStatus
p27NoShadowStatus
p27NoRaytraceStatus
p27NoSsrStatus
p27NoRealMirrorStatus
p27NoScreenRefractionStatus
p27NoTextureRebuildStatus
p27NoModelReuploadStatus
```

## Scene23 Glass Reflection Polish Lab

Patch: P26 — Glass / Reflection / Clearcoat Polish

Current scene:

```text
scene23_glass_reflection_polish_lab
Scene23 Glass Reflection Polish Lab
```

P26 preserves P25 glass foundation, P24 fake cubemap/procedural reflection controls, P23 clearcoat, P22 emissive/presets, P21 alpha/cutout, P20 runtime restore/reload/UI cap/scroll/alpha, P19 selected-slot controls, P18 IBL, Debug ZIP, and live FPS.

Implementation boundaries:

- Single-pass fake glass polish only.
- No real transparent pass.
- No real refraction or screen-space refraction.
- No real mirror.
- No full transparent sorting overhaul.
- No shadow pass, shadow maps, or CSM.

Glass polish:

- Stronger Schlick/Fresnel edge response.
- Tint blends more visibly into the retained surface.
- Fake thickness uses edge darken/brighten tint.
- Opacity curve keeps a minimum readable surface so glass cannot disappear.
- Clear glass and rough/dirty glass now separate more clearly.
- Glass reflection uses the existing P24 procedural probe with guarded boost.

Glass presets:

Clear Glass / Blue Glass / Green Glass / Smoke Glass / Warm Glass / Magic Glass / Dirty Glass Lite / Crystal Lite.

Presets affect tint, opacity, edge strength, roughness, reflection weight, and fake thickness.

Clearcoat / car paint polish:

- Clearcoat highlight is stronger but clamped.
- Car paint separates base, coat, and reflection response more clearly.
- Fabric remains matte.
- Plastic/rubber/metal keep previous routing.

Reflection polish:

- Procedural reflection zones have stronger sky/horizon/ground/side contrast.
- Side/rim reflection is more readable.
- Roughness response is smoothed for mobile-friendly math.
- Reflection energy and overbright guards remain active.

Material tab:

- Existing glass controls are preserved.
- Glass preset text now names full presets.
- Compact material summary includes preset, opacity/edge/roughness.
- No large UI redesign.

Debug views added:

```text
Glass Polish
Glass Edge
Glass Thickness Fake
Glass Reflection Polish
Clearcoat Polish
Paint Reflection
```

Diagnostics added at top level and under `renderLab`:

```text
glassPolishStatus
glassPolishMode = single_pass_fake_glass_polish
glassReadabilityStatus
glassEdgePolishStatus
glassTintPolishStatus
glassThicknessPolishStatus
glassOpacityCurveStatus
glassRoughnessPolishStatus
glassReflectionPolishStatus
glassInvisibleGuardStatus
glassStillFakeTransparencyStatus = yes_no_real_transparent_pass
glassPresetPolishStatus
activeGlassPreset
clearGlassPresetStatus
blueGlassPresetStatus
greenGlassPresetStatus
smokeGlassPresetStatus
warmGlassPresetStatus
magicGlassPresetStatus
dirtyGlassLitePresetStatus
crystalLitePresetStatus
glassPresetVisualResponseStatus
clearcoatPolishStatus
clearcoatHighlightPolishStatus
clearcoatReflectionSeparationStatus
carPaintPolishStatus
carPaintLayerReadabilityStatus
carPaintReflectionSeparationStatus
paintOverbrightGuardStatus
fabricMattePreserveStatus
p26ReflectionPolishStatus
reflectionZonePolishStatus
sideRimReflectionPolishStatus
roughnessReflectionPolishStatus
glassReflectionZoneStatus
clearcoatReflectionZoneStatus
reflectionEnergyGuardStatus
reflectionOverbrightGuardStatus
p26UiChangeStatus
glassSummaryUiStatus
glassStrengthSliderStatus
glassControlsPreservedStatus
glassPolishDebugViewStatus
glassEdgeDebugViewStatus
glassThicknessDebugViewStatus
glassReflectionPolishDebugViewStatus
clearcoatPolishDebugViewStatus
paintReflectionDebugViewStatus
p25GlassPreservedStatus
p24ReflectionPreservedStatus
p23ClearcoatPreservedStatus
p22EmissivePreservedStatus
p22PresetsPreservedStatus
p21AlphaPreservedStatus
p20RuntimeWorkflowPreservedStatus
p19SlotControlsPreservedStatus
p18IblPreservedStatus
p26PerformanceStatus
p26NoRealRefractionStatus
p26NoScreenRefractionStatus
p26NoMirrorPassStatus
p26NoFullSortingStatus
p26NoTextureRebuildStatus
p26NoModelReuploadStatus
p26NoNewShadowPassStatus
renderLoopAllocationGuardStatus
noFrameFileWriteStatus
noFrameGlbParseStatus
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

## Scene22 Glass Material Foundation Lab

Patch P25 switches the active lab to:

```text
scene22_glass_material_foundation_lab
Scene22 Glass Material Foundation Lab
```

P25 preserves P24 fake cubemap/procedural reflection probe, P23 clearcoat, P22 emissive/presets, P21 alpha/cutout, P20 runtime restore/reload/scroll workflow, P19 selected-slot controls, P18 IBL, Debug ZIP, and live FPS.

Glass foundation:

- Single-pass safe glass-like material path for selected slot and `glass_like` hint.
- Tint presets: Clear, Blue, Green, Smoke, Warm, Magic.
- Fake opacity reduces base surface strength without real transparent sorting.
- Fresnel/edge reflection uses the P24 procedural fake cubemap probe.
- Roughness reduces glass clarity/reflection intensity.
- Fake thickness darkens/tints edges.
- No real refraction, no screen-space refraction, no full transparent sorting, no new shadow pass.

Material controls added:

- Glass Enable.
- Glass Tint preset cycle.
- Glass Opacity slider `0.0-1.0`.
- Glass Edge slider `0.0-2.0`.
- Glass Rough slider `0.0-1.0`.

Debug views added:

```text
Glass Mask
Glass Opacity
Glass Fresnel
Glass Reflection
Glass Tint
Glass Safety Status
```

Required honest diagnostics:

```text
glassRefractionStatus = deferred_not_real_refraction
glassSortingStatus = safe_no_full_transparent_sorting
glassReflectionSourceStatus = p24_fake_cubemap_probe
```

Performance constraints:

- no refraction pass;
- no screen-space refraction;
- no transparent sorting overhaul;
- no texture rebuild/model reupload/tangent rebuild while sliders move;
- no Java frame-loop allocations, GLB parsing, or file writes.

## Scene21 Better Environment Reflection Lab

Patch P24 switches the active lab to:

```text
scene21_better_environment_reflection_lab
Scene21 Better Environment Reflection Lab
```

P24 preserves P23 clearcoat, P22 emissive/presets, P21 alpha/cutout, P20 runtime restore/reload, P19 selected-slot controls, P18 procedural IBL, Debug ZIP, and live FPS. It adds a single-pass fake cubemap/procedural directional probe for richer reflection zones and a first camera-motion performance guard.

Reflection foundation:

- No real cubemap texture loading, no render-to-cubemap, and no extra pass.
- The shader blends sky, horizon, ground, side/rim, and glint zones from the reflection direction.
- Presets: Studio, Outdoor, Warm Room, Cold Room, Sunset, Cave.
- Metal and car paint receive stronger tinted reflections and clearcoat separation.
- Fabric remains matte; rubber remains low-reflection.
- `glass_like` stays metadata only; no refraction, transmission, transparent sorting, or real glass.

Controls:

- Reflection Contrast slider, `0.0-2.0`, uniform-only.
- Reflection Saturation slider, `0.0-2.0`, uniform-only.
- Environment Zone preset button cycles Studio / Outdoor / Warm Room / Cold Room / Sunset / Cave.
- Existing Env, Sky/Horizon, Clearcoat, Emissive, Alpha, and material slot controls remain capped and scrollable.

Motion guard:

- Camera drag updates `cameraMotionSpeed`, `cameraMotionQualityTier`, and `cameraMotionQualityBlend`.
- While moving, reflection and clearcoat scales are reduced by uniform values only.
- Quality restores smoothly over about `520 ms`.
- No model reupload, texture rebuild, per-frame GLB parsing, or per-frame file/JSON writes.

Debug views added:

```text
Fake Cubemap
Reflection Zones
Reflection Contrast
Reflection Energy Guard
Clearcoat Reflection
Motion Quality
```

Diagnostics added at top level and under `renderLab`:

```text
fakeCubemapStatus
fakeCubemapMode
fakeCubemapSourceStatus
environmentZoneStatus
skyZoneStatus
horizonZoneStatus
groundZoneStatus
sideReflectionZoneStatus
fakeCubemapPerformanceStatus
reflectionQualityStatus
reflectionContrastStatus
reflectionSaturationStatus
roughnessReflectionBlurStatus
clearcoatReflectionBoostStatus
metalReflectionTintStatus
fabricReflectionSuppressStatus
reflectionEnergyGuardStatus
reflectionOverbrightGuardStatus
reflectionQualityUiStatus
reflectionContrastSliderStatus
reflectionContrastValue
reflectionSaturationSliderStatus
reflectionSaturationValue
environmentZonePresetStatus
environmentZonePreset
reflectionUniformUpdateStatus
motionPerformanceGuardStatus
cameraMotionStatus
cameraMotionSpeed
cameraMotionQualityTier
cameraMotionQualityBlend
motionReflectionScale
motionClearcoatScale
motionDiagnosticsThrottleStatus
motionQualityRestoreStatus
motionQualityTransitionMs
cameraMovingFpsLast
cameraStillFpsLast
targetMovingFps
movingFpsGuardStatus
carPaintReflectionStatus
metalReflectionStatus
plasticReflectionStatus
rubberReflectionStatus
glassMetadataReflectionStatus
materialPresetReflectionStatus
fakeCubemapDebugViewStatus
reflectionZonesDebugViewStatus
reflectionContrastDebugViewStatus
reflectionEnergyGuardDebugViewStatus
clearcoatReflectionDebugViewStatus
motionQualityDebugViewStatus
p23ClearcoatPreservedStatus
p24PerformanceStatus
fakeCubemapNoTextureStatus
fakeCubemapNoNewPassStatus
reflectionNoTextureRebuildStatus
reflectionNoModelReuploadStatus
noFrameFileWriteStatus
noFrameGlbParseStatus
```

## Scene20 Clearcoat Paint Layer Lab

Patch P23 switches the active lab to:

```text
scene20_clearcoat_paint_layer_lab
Scene20 Clearcoat Paint Layer Lab
```

P23 preserves P22 emissive/presets, P21 alpha/cutout, P20 runtime restore/reload, P19 selected-slot controls, P18 directional sky/ground IBL, P17 gloss/calib/coat controls, Debug ZIP, and live FPS. It adds a single-pass, uniform-only clearcoat/paint layer foundation for car paint style gloss and view-angle highlights.

Material tab additions:

- Clearcoat slider, `0.0-2.0`, uniform-only.
- Clearcoat Rough slider, `0.0-1.0`, uniform-only.
- Existing Preset cycle/apply, Emissive, Alpha Cutoff, Metallic/Rough/Normal/AO, Calib/Gloss/Coat controls remain.

Clearcoat routing:

- `clearcoatIntensity` and `clearcoatRoughness` are push-constant uniforms.
- `paint_like` and Car Paint preset receive the strongest coat response.
- `metal_like` can receive a limited controlled coat response.
- `fabric_like` and rubber stay matte unless a later explicit manual override is added.
- `glass_like` remains metadata only; no real glass, refraction, transmission, transparent sorting, or extra pass.
- No cubemap texture loading, no new render pass, no texture rebuild, no model reupload, no tangent rebuild while sliders move.

Debug views added:

```text
Clearcoat
Clearcoat Highlight
Paint Layer
Paint Energy Guard
```

Diagnostics added at top level and under `renderLab`:

```text
clearcoatStatus
clearcoatMode
clearcoatIntensity
clearcoatRoughness
clearcoatFresnelStatus
clearcoatHighlightStatus
clearcoatMaterialRoutingStatus
clearcoatOverbrightGuardStatus
clearcoatPerformanceStatus
clearcoatAppliedStatus
clearcoatWeight
clearcoatRoughnessApplied
clearcoatGuardApplied
carPaintLayerStatus
carPaintPresetV2Status
carPaintClearcoatStatus
paintLayerEnergyGuardStatus
paintLayerMaterialHintStatus
clearcoatSliderStatus
clearcoatSliderValue
clearcoatRoughnessSliderStatus
clearcoatRoughnessSliderValue
clearcoatUniformUpdateStatus
clearcoatUiStatus
clearcoatDebugViewStatus
clearcoatHighlightDebugViewStatus
paintLayerDebugViewStatus
paintEnergyGuardDebugViewStatus
p22EmissivePreservedStatus
p22PresetsPreservedStatus
p23PerformanceStatus
clearcoatNoNewPassStatus
clearcoatNoTextureRebuildStatus
clearcoatNoModelReuploadStatus
clearcoatNoTransparentSortingStatus
```

## Scene19 Emissive Material Presets Lab

Patch P22 switches the active lab to:

```text
scene19_emissive_material_presets_lab
Scene19 Emissive Material Presets Lab
```

P22 preserves P21 alpha/cutout, P20 runtime restore/reload, P19 selected-slot controls, P18 directional sky/ground IBL, P17 gloss/calib/coat controls, Debug ZIP, and live FPS. It adds safe glTF emissive metadata/factor handling and selected-slot material presets without real light contribution, bloom, glass, shadow maps, CSM, extra render passes, texture rebuilds, or model reupload while sliding.

Material tab additions:

- Preset cycle and Apply Preset controls.
- Emissive slider, uniform-only.
- Alpha Cutoff slider, uniform-only.
- Alpha Mode debug cycle.
- Double-sided debug view button.
- Reset Alpha button.

Alpha handling:

- glTF `alphaMode` metadata recognizes `OPAQUE`, `MASK`, and `BLEND`.
- `MASK` uses baseColor factor/texture alpha and shader discard with `alphaCutoff`.
- `BLEND` is diagnostics-only fallback for now: no full transparent sorting or real blending.
- Per-slot diagnostics include `alphaMode`, `alphaCutoff`, `alphaTextureStatus`, `alphaMaskAppliedStatus`, and `alphaBlendFallbackStatus`.

Double-sided handling:

- glTF `doubleSided` metadata is recorded per material slot.
- Current pipeline already uses `VK_CULL_MODE_NONE`; diagnostics mark no new raster permutation.
- Shader normal handling uses face orientation for a two-sided normal foundation.
- Per-slot diagnostics include `doubleSided` and `doubleSidedAppliedStatus`.

Thin/edge polish:

- `fabric_like` remains matte.
- `cutout_like` is used for MASK/double-sided/card/leaf/hair/grille-like materials.
- `glass_like` is metadata only and renders through safe opaque/cutout behavior until a future glass stage.
- `decal_like` is a safe hint for alpha/low-roughness flat-ish materials.

Debug views added:

```text
Alpha Mask
Alpha Mode
Double Sided
Cutout Hint
Transparency Status
```

Diagnostics added at top level and under `renderLab`:

```text
emissiveMaterialStatus
materialPresetStatus
activeMaterialPreset
emissiveSliderStatus
p21AlphaPreservedStatus
alphaMaterialStatus
alphaModeSupportStatus
alphaMaskStatus
alphaBlendStatus
alphaCutoffStatus
alphaCutoffValue
alphaDiscardStatus
alphaTextureChannelStatus
alphaFallbackStatus
doubleSidedMaterialStatus
doubleSidedMode
doubleSidedNormalStatus
doubleSidedRasterStatus
doubleSidedFallbackStatus
thinMaterialPolishStatus
cutoutMaterialHintStatus
fabricEdgeStatus
glassMetadataStatus
decalMaterialHintStatus
transparencyDeferredStatus
alphaUiStatus
alphaCutoffSliderStatus
alphaDebugViewStatus
doubleSidedDebugViewStatus
alphaResetButtonStatus
alphaUniformUpdateStatus
alphaSliderUpdateMode
alphaMaskDebugViewStatus
alphaModeDebugViewStatus
cutoutHintDebugViewStatus
transparencyStatusDebugViewStatus
alphaPerformanceStatus
alphaNoNewPassStatus
alphaNoTextureRebuildStatus
alphaNoModelReuploadStatus
p20RuntimeWorkflowPreservedStatus
p19SlotControlsPreservedStatus
p18IblPreservedStatus
p17GlossPreservedStatus
fpsStatus
fpsUpdateMode
debugZipStatus
debugZipPath
```

## Scene17 Runtime Material Workflow Lab

Patch P20 switches the active lab to:

```text
scene17_runtime_material_workflow_lab
Scene17 Runtime Material Workflow Lab
```

Scene17 preserves Scene16/P19 material slot controls, P18 directional sky/ground IBL, P17 gloss/calib/coat controls, import/scan/export, Debug ZIP, and live FPS. It adds runtime model persistence for surface recreate/resume, a capped scrollable phone inspector, dynamic inspector alpha while dragging sliders or the camera, selected-slot material workflow polish, and Reload Active Model.

Runtime restore diagnostics:

```text
resumeRestoreStatus
resumeRestoreMode
activeModelPersistenceStatus
activeModelRestoreAttemptCount
activeModelRestoreResult
fallbackCubeReason
surfaceRecreateStatus
modelUploadRepeatCount
renderLoopAllocationGuardStatus
```

Inspector workflow diagnostics:

```text
inspectorHeightMode = capped_30_percent
inspectorScrollStatus = ok
inspectorExpandedMaxHeightPercent = 30
inspectorCollapsedStatus
materialTabScrollStatus
inspectorTouchTargetStatus
inspectorDynamicAlphaStatus
inspectorAlphaIdle
inspectorAlphaWhileSliderDrag
inspectorAlphaWhileCameraMove
inspectorAlphaRestoreStatus
sliderDragVisualMode
cameraMoveVisualMode
```

Material and Assets workflow diagnostics:

```text
materialWorkflowStatus
materialSlotSummaryUiStatus
selectedSlotResetButtonStatus
selectedSlotResetStatus
selectedMaterialTextureSummaryStatus
assetsWorkflowStatus
reloadActiveModelButtonStatus
reloadActiveModelStatus
activeModelDisplayStatus
fallbackReasonDisplayStatus
p19PreservedStatus
p18IblPreservedStatus
p17GlossPreservedStatus
runtimeStateDebugViewStatus
restoreStateDebugViewStatus
uiStateDebugViewStatus
```

## Scene16 Material Slot Editor Lab

Patch P19 switches the active lab to:

```text
scene16_material_slot_editor_lab
Scene16 Material Slot Editor Lab
```

Scene16 preserves import/scan/export, Debug ZIP, live FPS, inspector tabs, P18 Env/Sky/Horizon controls, and P17/P18 Calib/Gloss/Coat controls. It adds compact Material tab slot controls and selected-slot overrides for metallic, roughness, normal scale, AO, gloss, and coat.

Selected-slot override routing is uniform/push-constant only in the existing per-primitive draw path: the renderer applies override values only when the primitive material slot equals the selected slot. Other slots keep their source material factors, and fabric-like slots remain matte unless their selected-slot gloss/coat values are raised.

Diagnostics added at top level and under `renderLab`:

```text
materialSlotEditorStatus
selectedMaterialSlot
selectedMaterialSlotCount
selectedMaterialTypeHint
selectedMaterialName
selectedMaterialSummaryStatus
materialSlotSelectionUiStatus
perMaterialOverrideStatus
perMaterialOverrideMode
selectedSlotMetallicOverride
selectedSlotRoughnessOverride
selectedSlotNormalScaleOverride
selectedSlotAoOverride
selectedSlotGlossOverride
selectedSlotCoatOverride
selectedSlotOverrideApplied
selectedSlotResetStatus
perMaterialUniformUpdateStatus
materialSlotControlsUiStatus
metallicSlotSliderStatus
roughnessSlotSliderStatus
normalSlotSliderStatus
aoSlotSliderStatus
selectedMaterialDebugViewStatus
materialOverrideDebugViewStatus
slotMetallicDebugViewStatus
slotRoughnessDebugViewStatus
slotAoDebugViewStatus
perMaterialOverridePerformanceStatus
```

Debug views added:

```text
Selected Material
Material Override
Slot Metallic
Slot Roughness
Slot AO
```

Out of scope:

- shadows, shadow maps, CSM;
- cubemap texture pipeline;
- glass, transmission, refraction;
- skeletal animation;
- large UI rewrite.

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
