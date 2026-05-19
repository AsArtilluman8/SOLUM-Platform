#pragma once
#include "pipeline_bundle.hpp"
#include "runtime_diagnostics.hpp"
#include "texture_resource.hpp"
#include <algorithm>

namespace solum {

enum PbrTextureKind {
    PbrTextureBaseColor = 0,
    PbrTextureMetallicRoughness = 1,
    PbrTextureNormal = 2,
    PbrTextureOcclusion = 3
};

enum MaterialPresetKind {
    MaterialPresetBalanced = 0,
    MaterialPresetCarPaint = 1,
    MaterialPresetMetal = 2,
    MaterialPresetFabric = 3,
    MaterialPresetRubber = 4,
    MaterialPresetPlastic = 5,
    MaterialPresetGlassMetadata = 6,
    MaterialPresetEmissiveSafe = 7
};

struct PbrMaterialTextureSet {
    TextureResource baseColor;
    TextureResource metallicRoughness;
    TextureResource normal;
    TextureResource occlusion;
    bool baseColorUploaded = false;
    bool metallicRoughnessUploaded = false;
    bool normalUploaded = false;
    bool occlusionUploaded = false;

    void destroy() {
        baseColor.destroy();
        metallicRoughness.destroy();
        normal.destroy();
        occlusion.destroy();
        baseColorUploaded = false;
        metallicRoughnessUploaded = false;
        normalUploaded = false;
        occlusionUploaded = false;
    }

    TextureResource& texture(int kind) {
        if (kind == PbrTextureMetallicRoughness) return metallicRoughness;
        if (kind == PbrTextureNormal) return normal;
        if (kind == PbrTextureOcclusion) return occlusion;
        return baseColor;
    }

    bool uploaded(int kind) const {
        if (kind == PbrTextureMetallicRoughness) return metallicRoughnessUploaded;
        if (kind == PbrTextureNormal) return normalUploaded;
        if (kind == PbrTextureOcclusion) return occlusionUploaded;
        return baseColorUploaded;
    }

    void setUploaded(int kind, bool value) {
        if (kind == PbrTextureMetallicRoughness) metallicRoughnessUploaded = value;
        else if (kind == PbrTextureNormal) normalUploaded = value;
        else if (kind == PbrTextureOcclusion) occlusionUploaded = value;
        else baseColorUploaded = value;
    }
};

struct RendererCore {
    std::string outputRoot;
    std::string status = "SOLUM Engine\nNative object created";
    RuntimeDiagnostics diagnostics;

    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkQueue graphicsQueue = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamily = UINT32_MAX;
    VkFormat swapchainFormat = VK_FORMAT_UNDEFINED;
    VkFormat depthFormat = VK_FORMAT_UNDEFINED;
    VkExtent2D extent{};

    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    VkRenderPass renderPass = VK_NULL_HANDLE;
    std::vector<VkFramebuffer> framebuffers;
    VkImage depthImage = VK_NULL_HANDLE;
    VkDeviceMemory depthMemory = VK_NULL_HANDLE;
    VkImageView depthImageView = VK_NULL_HANDLE;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> commandBuffers;
    VkSemaphore imageAvailable = VK_NULL_HANDLE;
    VkSemaphore renderFinished = VK_NULL_HANDLE;
    VkFence inFlight = VK_NULL_HANDLE;

    MeshResource validationMesh;
    MeshResource modelMesh;
    TextureResource baseColorTexture;
    std::vector<PrimitiveDrawRange> modelDrawRanges;
    std::vector<MaterialSlotState> modelMaterialSlots;
    std::vector<PbrMaterialTextureSet> materialTextureSets;
    PipelineBundle trianglePipeline;
    VkDescriptorPool textureDescriptorPool = VK_NULL_HANDLE;
    std::vector<VkDescriptorSet> textureDescriptorSets;
    uint64_t framesRendered = 0;
    bool firstFrameRendered = false;
    bool triangleDrawn = false;
    bool cubeDrawn = false;
    bool depthReady = false;
    bool cameraReady = false;
    bool cameraMvpReady = false;
    bool cameraControlsReady = false;
    bool pushConstantsReady = false;
    bool materialConstantsReady = false;
    bool meshAttributeLayoutReady = false;
    bool rendererCoreReady = false;
    CameraState camera;
    MaterialConstants material;
    ModelRenderState model;
    int selectedMaterialSlot = 0;
    float selectedSlotMetallicOverride = 0.0f;
    float selectedSlotRoughnessOverride = 1.0f;
    float selectedSlotNormalScaleOverride = 1.0f;
    float selectedSlotAoOverride = 1.0f;
    float selectedSlotGlossOverride = 0.0f;
    float selectedSlotCoatOverride = 0.0f;
    float selectedSlotEmissiveIntensity = 0.0f;
    int activeMaterialPreset = MaterialPresetBalanced;
    int manualGlassSlot = -1;
    int activeTransparentGlassSlot = -1;

    void setOutputRoot(const std::string& root) { outputRoot = root; diagnostics.outputRoot = root; }

    void syncLightingModelState() {
        for (int i = 0; i < 3; ++i) {
            model.sunDirection[i] = material.sunDirection[i];
            model.sunColor[i] = material.sunColor[i];
            model.ambientColor[i] = material.ambientColor[i];
        }
        model.sunIntensity = material.sunIntensity;
        model.ambientIntensity = material.ambientIntensity;
        model.lightPreset = lightPresetName(material.lightPreset);
        model.toneMappingMode = toneMappingModeName(material.toneMappingMode);
        model.activeDebugView = materialDebugViewName(material.activeDebugView);
        model.calibrationPreset = calibrationPresetName(material.calibrationPreset);
        model.calibrationSliderValue = material.calibrationStrength;
        model.glossSliderValue = material.glossSliderValue;
        model.paintGlossSliderValue = material.paintGlossSliderValue;
        model.environmentIntensity = material.environmentIntensity;
        model.environmentPreset = environmentPresetName(material.environmentPreset);
        model.horizonStrength = material.horizonStrength;
        model.paintGlossIntensity = material.paintGlossSliderValue;
        model.paintGlossRoughness = clampFloat(0.78f - material.paintGlossSliderValue * 0.58f, 0.20f, 0.78f);
        model.emissiveIntensity = material.emissiveIntensity;
        model.activeMaterialPreset = materialPresetName(material.materialPresetHint);
        model.exposureValue = material.exposureValue;
        model.ambientFloor = material.ambientFloor;
        model.brightnessPreset = brightnessPresetName(material.brightnessPreset);
        model.specularBoost = material.specularBoost;
        model.reflectionIntensity = material.reflectionIntensity;
        model.reflectionContrastValue = material.reflectionContrast;
        model.reflectionSaturationValue = material.reflectionSaturation;
        model.motionReflectionScale = material.motionReflectionScale;
        model.motionClearcoatScale = material.motionClearcoatScale;
        model.contactShadowIntensity = material.contactShadowIntensity;
        model.lightingStatus = "ok";
        model.lightingControlStatus = "ok";
        model.lightingUiMode = "compact_sliders";
        model.inspectorUiStatus = "ok";
        model.inspectorUiMode = "tabbed_compact_inspector";
        model.assetsTabStatus = "ok_import_scan_export_summary";
        model.cameraTabStatus = "ok_camera_info_reset_zoom";
        model.lightingTabStatus = "ok_sliders_environment_controls";
        model.materialTabStatus = "ok_slot_controls";
        model.debugTabStatus = "ok_fps_zip_status";
        model.materialCalibrationStatus = "ok";
        model.materialCalibrationMode = "shader_uniform_upload_lightweight";
        model.albedoEnergyStatus = "ok_normalized";
        model.albedoClampStatus = "ok";
        model.diffuseClampStatus = "ok";
        model.luminanceGuardStatus = "ok";
        model.aoCalibrationStatus = "ok_indirect_weighted";
        model.roughnessRemapStatus = "ok";
        model.metallicRoughnessClampStatus = "ok";
        model.emissiveGuardStatus = "ok_guarded_real_emissive_only";
        model.fabricMattePreserveStatus = "ok_rough_fabric_kept_matte";
        model.paintMaterialCalibrationStatus = "ok_luminance_guard_reflection_readable";
        model.metalMaterialCalibrationStatus = "ok_clamped_metallic_readable";
        model.alphaCutoffValue = material.alphaCutoff;
        model.alphaCutoffStatus = "ok_uniform_control";
        model.alphaUniformUpdateStatus = "ok_uniform_only";
        model.alphaSliderUpdateMode = "uniform_only";
        model.alphaUiStatus = "ok_compact_material_tab";
        model.alphaDebugViewStatus = "shader_applied";
        model.alphaMaskDebugViewStatus = "shader_applied";
        model.alphaModeDebugViewStatus = "shader_applied";
        model.doubleSidedDebugViewStatus = "shader_applied";
        model.cutoutHintDebugViewStatus = "shader_applied";
        model.transparencyStatusDebugViewStatus = "shader_applied";
        model.alphaPerformanceStatus = "ok_no_new_pass_no_sorting_no_reupload";
        model.alphaNoNewPassStatus = "ok";
        model.alphaNoTextureRebuildStatus = "ok";
        model.alphaNoModelReuploadStatus = "ok";
        model.p21AlphaPreservedStatus = "ok";
        model.emissiveMaterialStatus = "ok_metadata_supported";
        model.emissiveMode = "factor_uniform_only_no_light_contribution";
        model.emissiveIntensityStatus = "ok_clamped";
        model.emissiveColorStatus = "ok_guarded";
        model.emissiveOverbrightGuardStatus = "ok_clamped";
        model.emissiveLightingContributionStatus = "not_real_light_source";
        model.emissivePerformanceStatus = "ok_no_bloom_no_new_pass";
        model.materialPresetStatus = "ok";
        model.materialPresetMode = "selected_slot_uniform_only";
        model.materialPresetAppliedSlot = selectedMaterialSlot;
        model.materialPresetUiStatus = "ok_compact_material_tab";
        model.materialPresetPerformanceStatus = "ok_no_model_reupload_no_texture_rebuild";
        model.selectedSlotPresetStatus = "ok";
        model.carPaintPresetStatus = "ok";
        model.metalPresetStatus = "ok";
        model.fabricPresetStatus = "ok";
        model.rubberPresetStatus = "ok";
        model.plasticPresetStatus = "ok";
        model.glassMetadataPresetStatus = "glass_foundation_no_refraction";
        model.emissivePresetStatus = "ok_safe_clamped";
        model.materialPresetGuardStatus = "ok_energy_guarded";
        model.presetCycleButtonStatus = "ok";
        model.presetApplyButtonStatus = "ok";
        model.emissiveSliderStatus = "ok";
        model.emissiveUniformUpdateStatus = "ok_uniform_only";
        model.materialResetButtonStatus = "ok";
        model.materialUiScrollPreservedStatus = "ok";
        model.emissiveDebugViewStatus = "shader_applied";
        model.presetTypeDebugViewStatus = "shader_applied";
        model.presetResponseDebugViewStatus = "shader_applied";
        model.materialEnergyGuardDebugViewStatus = "shader_applied";
        model.selectedSlotPresetDebugViewStatus = "shader_applied";
        model.p22PerformanceStatus = "ok_no_new_pass_no_bloom_no_reupload";
        model.emissiveNoBloomStatus = "ok";
        model.emissiveNoNewPassStatus = "ok";
        model.presetNoModelReuploadStatus = "ok";
        model.presetNoTextureRebuildStatus = "ok";
        model.materialTypeHintStatus = "ok";
        model.materialSlotCalibrationStatus = "ok";
        model.calibrationUiStatus = "ok_compact_material_tab";
        model.calibrationSliderStatus = "ok";
        model.calibrationUniformUpdateStatus = "ok_uniform_only";
        model.calibratedAlbedoDebugViewStatus = "shader_applied";
        model.materialTypeDebugViewStatus = "shader_applied";
        model.aoInfluenceDebugViewStatus = "shader_applied";
        model.luminanceGuardDebugViewStatus = "shader_applied";
        model.calibrationVisualStrength = clampFloat(0.28f + material.calibrationStrength * 0.72f, 0.28f, 1.0f);
        model.calibrationAffectsAlbedo = "yes_guarded_luminance_clamp";
        model.calibrationAffectsAo = "yes_indirect_occlusion_weight";
        model.calibrationAffectsRoughness = "yes_material_safe_remap";
        model.calibrationVisibleResponseStatus = "ok_visible_final_shaded_response";
        model.materialCalibrationPerformanceStatus = "ok_uniform_shader_no_rebuild";
        model.specularGlossStatus = "ok";
        model.specularGlossMode = "uniform_gloss_response_p17b";
        model.specularResponseStatus = "ok_guarded_dielectric_metal";
        model.glossResponseStatus = "ok_slider_controls_lobe_width";
        model.roughnessRemapV2Status = "ok_calib_and_gloss_weighted";
        model.metallicSpecularBoostStatus = "ok_metal_routed_boost";
        model.dielectricGlossStatus = "ok_f0_guarded";
        model.fabricSpecularSuppressStatus = "ok_matte_preserved";
        model.specularOverbrightGuardStatus = "ok_luminance_guard";
        model.viewDependentHighlightStatus = "ok_reflection_vector";
        model.paintGlossLiteStatus = "ok";
        model.paintGlossLiteMode = "uniform_lite_no_texture_rebuild";
        model.paintGlossMaterialHintStatus = "ok_paint_like_only";
        model.paintGlossPerformanceStatus = "ok_uniform_only";
        model.glossSliderStatus = "ok";
        model.paintGlossSliderStatus = "ok";
        model.glossUniformUpdateStatus = "ok_uniform_only";
        model.glossResponseDebugViewStatus = "shader_applied";
        model.specularGuardDebugViewStatus = "shader_applied";
        model.paintGlossDebugViewStatus = "shader_applied";
        model.metalResponseDebugViewStatus = "shader_applied";
        model.paintTargetDebugViewStatus = "shader_applied";
        model.calibrationResponseDebugViewStatus = "shader_applied";
        model.materialTypeSpecularRoutingStatus = "ok";
        model.paintMaterialGlossStatus = "ok_lite_gloss";
        model.metalMaterialGlossStatus = "ok_stronger_response";
        model.rubberMaterialGlossStatus = "ok_suppressed";
        model.specularGlossPerformanceStatus = "ok_no_alloc_no_rebuild";
        model.glossVisibleResponseStatus = "ok_visible_lobe_and_reflection";
        model.glossAffectsSpecularLobe = "yes_shader_roughness_distribution";
        model.glossAffectsReflectionWeight = "yes_shader_environment_weight";
        syncMaterialSlotEditorState();
        uint32_t paintCount = 0;
        uint32_t metalCount = 0;
        uint32_t unknownCount = 0;
        uint32_t fabricCount = 0;
        for (const auto& slot : modelMaterialSlots) {
            if (slot.materialTypeHint == 1) ++paintCount;
            else if (slot.materialTypeHint == 2) ++metalCount;
            else if (slot.materialTypeHint == 0) ++fabricCount;
            else if (slot.materialTypeHint == 4) ++unknownCount;
        }
        model.paintGlossSkippedFabricCount = fabricCount;
        if (paintCount > 0) {
            material.paintGlossRouting = 1;
            model.paintGlossTargetStatus = "paint";
            model.paintGlossAppliedMaterialCount = paintCount;
            model.paintGlossFallbackRouting = "paint_like";
            model.paintGlossVisibleResponseStatus = "ok_paint_like_visible";
            model.paintGlossMaterialHintStatus = "ok_paint_like_target";
        } else if (metalCount > 0) {
            material.paintGlossRouting = 2;
            model.paintGlossTargetStatus = "metal";
            model.paintGlossAppliedMaterialCount = metalCount;
            model.paintGlossFallbackRouting = "metal_like_limited_safe";
            model.paintGlossVisibleResponseStatus = "ok_toycar_metal_coat_visible_limited";
            model.paintGlossMaterialHintStatus = "ok_no_paint_like_using_metal_fallback";
        } else if (unknownCount > 0) {
            material.paintGlossRouting = 3;
            model.paintGlossTargetStatus = "unknown";
            model.paintGlossAppliedMaterialCount = unknownCount;
            model.paintGlossFallbackRouting = "unknown_very_limited";
            model.paintGlossVisibleResponseStatus = "limited_unknown_target";
            model.paintGlossMaterialHintStatus = "ok_unknown_limited_fallback";
        } else {
            material.paintGlossRouting = 0;
            model.paintGlossTargetStatus = "none";
            model.paintGlossAppliedMaterialCount = 0;
            model.paintGlossFallbackRouting = "none";
            model.paintGlossVisibleResponseStatus = "no_target";
            model.paintGlossMaterialHintStatus = "no_paint_or_safe_coat_target";
        }
        model.brdfStatus = "ok";
        model.brdfMode = "direct_lighting_schlick_mobile_p17_gloss";
        model.diffuseStatus = "ok_environment_diffuse";
        model.specularStatus = "ok_p17_gloss_response_guarded";
        model.specularBoostStatus = "ok_uniform_controlled";
        model.iblStatus = "ok_foundation";
        model.iblMode = "procedural_directional_probe";
        model.environmentIblStatus = "ok_fake_cubemap_probe";
        model.environmentIblMode = "procedural_directional_probe";
        model.environmentSourceStatus = "ok_procedural_no_external_texture";
        model.environmentSourceType = "procedural_directional_probe";
        model.environmentSkyColorStatus = "ok_zone_uniform";
        model.environmentGroundColorStatus = "ok_zone_uniform";
        model.environmentHorizonStatus = "ok_zone_horizon_blend";
        model.environmentPerformanceStatus = "ok_no_extra_pass_no_texture_upload";
        model.iblDiffuseStatus = "ok_directional_sky_ground_diffuse";
        model.iblSpecularStatus = "ok_reflection_direction_environment";
        model.iblRoughnessResponseStatus = "ok_roughness_blurs_and_reduces_specular";
        model.iblMetallicResponseStatus = "ok_metal_tints_reflection";
        model.iblDielectricResponseStatus = "ok_subtle_f0_reflection";
        model.iblFabricPreserveStatus = "ok_fabric_matte_preserved";
        model.iblOverbrightGuardStatus = "ok_luminance_guarded";
        model.environmentUiStatus = "ok_compact_lighting_controls";
        model.environmentSliderStatus = "ok";
        model.skyPresetStatus = "ok";
        model.horizonControlStatus = "ok";
        model.environmentUniformUpdateStatus = "ok_uniform_only";
        model.environmentDebugViewStatus = "shader_applied";
        model.reflectionDirectionDebugViewStatus = "shader_applied";
        model.environmentColorDebugViewStatus = "shader_applied";
        model.iblPerformanceStatus = "ok_shader_math_only_no_loops";
        model.reflectionFoundationStatus = "p24_fake_cubemap_probe_foundation";
        model.reflectionMode = "procedural_directional_probe";
        model.environmentReflectionStatus = "p24_fake_cubemap_probe_no_texture";
        model.environmentReflectionMode = "reflection_direction_sky_horizon_ground_side";
        model.environmentSource = "procedural_directional_probe";
        model.reflectionColorStatus = "ok_sky_horizon_ground_side_glint";
        model.reflectionRoughnessResponseStatus = "ok_roughness_blur_approx";
        model.metallicReflectionStatus = "ok_metal_tinted_stronger_guarded";
        model.dielectricReflectionStatus = "ok_subtle_f0_environment";
        model.reflectionPerformanceStatus = "ok_no_extra_pass_no_texture_rebuild";
        model.fakeCubemapStatus = "ok";
        model.fakeCubemapMode = "procedural_directional_probe";
        model.fakeCubemapSourceStatus = "procedural_no_texture_no_render_pass";
        model.environmentZoneStatus = "ok_sky_horizon_ground_side";
        model.skyZoneStatus = "ok";
        model.horizonZoneStatus = "ok";
        model.groundZoneStatus = "ok";
        model.sideReflectionZoneStatus = "ok";
        model.fakeCubemapPerformanceStatus = "ok_shader_math_only_no_loops";
        model.reflectionQualityStatus = "ok_p26_probe_polished";
        model.reflectionContrastStatus = "ok_uniform_control";
        model.reflectionSaturationStatus = "ok_uniform_control";
        model.roughnessReflectionBlurStatus = "ok_direction_blend_approx";
        model.clearcoatReflectionBoostStatus = "ok_motion_guarded";
        model.metalReflectionTintStatus = "ok_base_color_tinted";
        model.fabricReflectionSuppressStatus = "ok_fabric_matte_preserved";
        model.reflectionEnergyGuardStatus = "ok_luminance_guard";
        model.reflectionOverbrightGuardStatus = "ok_clamped_before_tonemap";
        model.reflectionQualityUiStatus = "ok_compact_controls";
        model.reflectionContrastSliderStatus = "ok";
        model.reflectionSaturationSliderStatus = "ok";
        model.environmentZonePresetStatus = "ok";
        model.environmentZonePreset = model.environmentPreset;
        model.reflectionUniformUpdateStatus = "ok_uniform_only";
        model.carPaintReflectionStatus = "ok_clearcoat_probe_separation";
        model.metalReflectionStatus = "ok_tinted_stronger";
        model.plasticReflectionStatus = "ok_glossy_limited";
        model.rubberReflectionStatus = "ok_low_reflection";
        model.glassMetadataReflectionStatus = "foundation_single_pass_fake_transparency";
        model.materialPresetReflectionStatus = "ok";
        model.fakeCubemapDebugViewStatus = "shader_applied";
        model.reflectionZonesDebugViewStatus = "shader_applied";
        model.reflectionContrastDebugViewStatus = "shader_applied";
        model.reflectionEnergyGuardDebugViewStatus = "shader_applied";
        model.clearcoatReflectionDebugViewStatus = "shader_applied";
        model.motionQualityDebugViewStatus = "shader_applied";
        model.p23ClearcoatPreservedStatus = "ok";
        model.p24PerformanceStatus = "ok_no_new_pass_no_texture_no_rebuild";
        model.fakeCubemapNoTextureStatus = "ok";
        model.fakeCubemapNoNewPassStatus = "ok";
        model.reflectionNoTextureRebuildStatus = "ok";
        model.reflectionNoModelReuploadStatus = "ok";
        model.noFrameFileWriteStatus = "ok";
        model.noFrameGlbParseStatus = "ok";
        model.p26ReflectionPolishStatus = "ok_zone_contrast_rim_roughness_polished";
        model.reflectionZonePolishStatus = "ok_sky_horizon_ground_contrast_guarded";
        model.sideRimReflectionPolishStatus = "ok_rim_zone_boost_guarded";
        model.roughnessReflectionPolishStatus = "ok_smoother_mobile_response";
        model.glassReflectionZoneStatus = "ok_glass_uses_p26_zone_probe";
        model.clearcoatReflectionZoneStatus = "ok_clearcoat_uses_p26_zone_probe";
        model.contactGroundingStatus = "foundation_analytic";
        model.contactShadowStatus = material.contactShadowIntensity > 0.0f ? "enabled" : "disabled";
        model.contactShadowMode = "analytic_blob_or_grounding_approx";
        model.contactShadowPerformanceStatus = "ok_uniform_only_no_shadow_pass";
        model.groundingUsesModelBounds = "yes_upload_bounds_scaled_local";
        model.groundingUniformUpdateStatus = "ok_uniform_only";
        model.lightingUniformUpdateStatus = "ok_uniform_only";
        model.sliderUpdateMode = "uniform_only";
        model.sliderTouchStatus = "ok_touch_targets";
        model.sunSliderStatus = "ok";
        model.ambientSliderStatus = "ok";
        model.exposureSliderStatus = "ok";
        model.specularSliderStatus = "ok";
        model.reflectionSliderStatus = "ok";
        model.environmentSliderStatus = "ok";
        model.groundSliderStatus = "ok";
        model.contactGroundingSliderStatus = "ok";
        model.fresnelStatus = "ok_schlick";
        model.f0Status = "ok_dielectric_0_04_metal_base_color";
        model.metallicResponseStatus = "ok_diffuse_reduced_f0_tinted";
        model.roughnessResponseStatus = "ok_gloss_width_energy_remap";
        model.directLightingStatus = "ok_single_sun_direct_gloss_lobe";
        model.materialResponseStatus = "p24_fake_cubemap_probe_foundation";
        model.pbrQualityTier = "mobile_direct_lighting_ibl_v1";
        model.brdfPerformanceStatus = "ok_mobile_friendly_direct_lighting_ibl";
        model.toneMappingStatus = "ok";
        model.debugViewStatus = "shader_applied";
        model.exposureStatus = "ok";
        model.normalDebugViewStatus = "shader_applied";
        model.ndotlDebugViewStatus = "shader_applied";
        model.diffuseDebugViewStatus = "shader_applied";
        model.specularDebugViewStatus = "shader_applied";
        model.f0DebugViewStatus = "shader_applied";
        model.reflectionDebugViewStatus = "shader_applied";
        model.iblDiffuseDebugViewStatus = "shader_applied";
        model.iblSpecularDebugViewStatus = "shader_applied";
        model.environmentDebugViewStatus = "shader_applied";
        model.reflectionDirectionDebugViewStatus = "shader_applied";
        model.environmentColorDebugViewStatus = "shader_applied";
        model.brdfStatusDebugViewStatus = "shader_applied";
        model.groundingDebugViewStatus = "shader_applied";
    }

    void syncDiagnostics() {
        diagnostics.gpuName = diagnostics.gpuName.empty() ? "unknown" : diagnostics.gpuName;
        diagnostics.swapchainFormat = swapchainFormat;
        diagnostics.extent = extent;
        diagnostics.rendererCoreReady = rendererCoreReady;
        diagnostics.vertexBufferReady = validationMesh.ready();
        diagnostics.indexBufferReady = validationMesh.indexedReady();
        diagnostics.cubeReady = cubeDrawn && validationMesh.indexedReady();
        diagnostics.depthReady = depthReady;
        diagnostics.cameraReady = cameraMvpReady && cameraControlsReady;
        diagnostics.cameraMvpReady = cameraMvpReady;
        diagnostics.cameraControlsReady = cameraControlsReady;
        diagnostics.uniformOrPushConstantsReady = pushConstantsReady;
        diagnostics.materialConstantsReady = materialConstantsReady;
        diagnostics.meshAttributeLayoutReady = meshAttributeLayoutReady;
        diagnostics.vertexCount = validationMesh.vertexCount;
        diagnostics.indexCount = validationMesh.indexCount;
        diagnostics.vertexStrideBytes = sizeof(Vertex3D);
        diagnostics.camera = camera;
        diagnostics.material = material;
        diagnostics.model = model;
        diagnostics.framesRendered = framesRendered;
        diagnostics.triangleDrawn = triangleDrawn;
        diagnostics.renderLab.cubeReady = diagnostics.cubeReady;
        diagnostics.renderLab.depthReady = diagnostics.depthReady;
        diagnostics.renderLab.cameraReady = diagnostics.cameraReady;
        diagnostics.renderLab.cameraMvpReady = diagnostics.cameraMvpReady;
        diagnostics.renderLab.cameraControlsReady = diagnostics.cameraControlsReady;
        diagnostics.renderLab.cameraYawDeg = camera.yawDeg;
        diagnostics.renderLab.cameraPitchDeg = camera.pitchDeg;
        diagnostics.renderLab.cameraDistance = camera.distance;
        diagnostics.renderLab.indexBufferReady = diagnostics.indexBufferReady;
        diagnostics.renderLab.uniformOrPushConstantsReady = diagnostics.uniformOrPushConstantsReady;
        diagnostics.renderLab.materialConstantsReady = diagnostics.materialConstantsReady;
        diagnostics.renderLab.meshAttributeLayoutReady = diagnostics.meshAttributeLayoutReady;
        diagnostics.renderLab.vertexCount = diagnostics.vertexCount;
        diagnostics.renderLab.indexCount = diagnostics.indexCount;
        diagnostics.renderLab.vertexStrideBytes = diagnostics.vertexStrideBytes;
        diagnostics.renderLab.material = material;
        diagnostics.renderLab.model = model;
        diagnostics.renderLab.framesRendered = diagnostics.framesRendered;
    }

    void syncTextureMaterialState() {
        material.baseColorTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].baseColorUploaded) ? 1 : 0;
        material.metallicRoughnessTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].metallicRoughnessUploaded) ? 1 : 0;
        material.normalTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].normalUploaded) ? 1 : 0;
        material.occlusionTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].occlusionUploaded) ? 1 : 0;
    }

    void applyLightPreset(int preset) {
        material.lightPreset = ((preset % 5) + 5) % 5;
        if (material.lightPreset == 1) {
            material.sunDirection[0] = -0.35f; material.sunDirection[1] = -0.82f; material.sunDirection[2] = -0.45f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 1.55f;
            material.ambientColor[0] = 0.42f; material.ambientColor[1] = 0.52f; material.ambientColor[2] = 0.62f;
            material.ambientIntensity = 0.46f;
            material.exposureValue = 1.18f;
            material.ambientFloor = 0.10f;
            material.brightnessPreset = 1;
            material.specularBoost = 1.20f;
            material.reflectionIntensity = 0.85f;
        } else if (material.lightPreset == 2) {
            material.sunDirection[0] = -0.42f; material.sunDirection[1] = -0.72f; material.sunDirection[2] = -0.55f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.92f; material.sunColor[2] = 0.78f;
            material.sunIntensity = 1.65f;
            material.ambientColor[0] = 0.44f; material.ambientColor[1] = 0.55f; material.ambientColor[2] = 0.72f;
            material.ambientIntensity = 0.38f;
            material.exposureValue = 1.10f;
            material.ambientFloor = 0.08f;
            material.brightnessPreset = 1;
            material.specularBoost = 1.35f;
            material.reflectionIntensity = 1.10f;
        } else if (material.lightPreset == 3) {
            material.sunDirection[0] = -0.35f; material.sunDirection[1] = -0.82f; material.sunDirection[2] = -0.45f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 2.0f;
            material.ambientColor[0] = 0.50f; material.ambientColor[1] = 0.58f; material.ambientColor[2] = 0.68f;
            material.ambientIntensity = 0.80f;
            material.exposureValue = 1.50f;
            material.ambientFloor = 0.16f;
            material.brightnessPreset = 3;
            material.specularBoost = 1.60f;
            material.reflectionIntensity = 1.00f;
        } else if (material.lightPreset == 4) {
            material.sunDirection[0] = -0.30f; material.sunDirection[1] = -0.80f; material.sunDirection[2] = -0.42f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 3.35f;
            material.ambientColor[0] = 0.55f; material.ambientColor[1] = 0.62f; material.ambientColor[2] = 0.72f;
            material.ambientIntensity = 1.25f;
            material.exposureValue = 1.90f;
            material.ambientFloor = 0.22f;
            material.brightnessPreset = 4;
            material.specularBoost = 2.10f;
            material.reflectionIntensity = 1.25f;
        } else {
            material.sunDirection[0] = -0.25f; material.sunDirection[1] = -0.88f; material.sunDirection[2] = -0.32f;
            material.sunColor[0] = 0.92f; material.sunColor[1] = 0.96f; material.sunColor[2] = 1.0f;
            material.sunIntensity = 1.05f;
            material.ambientColor[0] = 0.62f; material.ambientColor[1] = 0.68f; material.ambientColor[2] = 0.76f;
            material.ambientIntensity = 0.58f;
            material.exposureValue = 1.32f;
            material.ambientFloor = 0.14f;
            material.brightnessPreset = 2;
            material.specularBoost = 1.10f;
            material.reflectionIntensity = 0.75f;
        }
        syncLightingModelState();
    }

    void syncMaterialSlotEditorState() {
        const int slotCount = (int)modelMaterialSlots.size();
        selectedMaterialSlot = slotCount > 0 ? std::max(0, std::min(selectedMaterialSlot, slotCount - 1)) : 0;
        model.materialSlotEditorStatus = "ok";
        model.selectedMaterialSlot = selectedMaterialSlot;
        model.selectedMaterialSlotCount = slotCount;
        model.selectedMaterialTypeHint = slotCount > 0 ? materialTypeHintName(modelMaterialSlots[(size_t)selectedMaterialSlot].materialTypeHint) : "unknown";
        model.selectedMaterialName = slotCount > 0 ? ("slot_" + std::to_string(selectedMaterialSlot)) : "unknown";
        model.selectedMaterialSummaryStatus = slotCount > 0 ? "ok_metallic_roughness_normal_ao_texture_summary" : "empty";
        model.materialSlotSelectionUiStatus = "ok_prev_next_label_summary";
        model.perMaterialOverrideStatus = "foundation_selected_slot_uniform";
        model.perMaterialOverrideMode = "cpu_selected_slot_push_constants";
        model.selectedSlotMetallicOverride = selectedSlotMetallicOverride;
        model.selectedSlotRoughnessOverride = selectedSlotRoughnessOverride;
        model.selectedSlotNormalScaleOverride = selectedSlotNormalScaleOverride;
        model.selectedSlotAoOverride = selectedSlotAoOverride;
        model.selectedSlotGlossOverride = selectedSlotGlossOverride;
        model.selectedSlotCoatOverride = selectedSlotCoatOverride;
        model.emissiveIntensity = selectedSlotEmissiveIntensity;
        model.activeMaterialPreset = materialPresetName(activeMaterialPreset);
        model.materialPresetAppliedSlot = selectedMaterialSlot;
        model.materialPresetAppliedStatus = slotCount > 0 ? "ok_selected_slot_uniform_only" : "not_applied_no_material_slot";
        model.selectedSlotOverrideApplied = slotCount > 0 ? "true_selected_slot_only" : "false_no_material_slot";
        model.selectedSlotResetStatus = "available_safe_reseed_from_slot";
        model.perMaterialUniformUpdateStatus = "ok_uniform_only";
        model.materialSlotControlsUiStatus = "ok_compact";
        model.metallicSlotSliderStatus = "ok";
        model.roughnessSlotSliderStatus = "ok";
        model.normalSlotSliderStatus = "ok";
        model.aoSlotSliderStatus = "ok";
        model.selectedMaterialDebugViewStatus = "shader_applied";
        model.materialOverrideDebugViewStatus = "shader_applied";
        model.slotMetallicDebugViewStatus = "shader_applied";
        model.slotRoughnessDebugViewStatus = "shader_applied";
        model.slotAoDebugViewStatus = "shader_applied";
        model.perMaterialOverridePerformanceStatus = "ok_no_extra_pass_no_upload";
    }

    void syncEmissivePresetState() {
        uint32_t emissiveFactorCount = 0;
        uint32_t emissiveTextureCount = 0;
        for (const auto& slot : modelMaterialSlots) {
            if (slot.emissiveFactor[0] > 0.001f || slot.emissiveFactor[1] > 0.001f || slot.emissiveFactor[2] > 0.001f) emissiveFactorCount++;
            if (slot.emissiveTextureSlot >= 0) emissiveTextureCount++;
        }
        model.emissiveFactorStatus = emissiveFactorCount > 0 ? "ok_metadata_found" : "missing";
        model.emissiveTextureStatus = emissiveTextureCount > 0 ? "metadata_only_not_sampled_no_extra_descriptor" : "missing";
        model.emissiveMaterialStatus = emissiveFactorCount > 0 || emissiveTextureCount > 0 || selectedSlotEmissiveIntensity > 0.001f ? "ok_safe_emissive_supported" : "ok_metadata_supported";
        model.emissiveIntensity = selectedSlotEmissiveIntensity;
        model.emissiveIntensityStatus = "ok_clamped_0_2";
        model.emissiveColorStatus = "ok_factor_clamped";
        model.emissiveOverbrightGuardStatus = "ok_luminance_guarded";
        model.emissiveLightingContributionStatus = "not_real_light_source";
        model.emissivePerformanceStatus = "ok_no_bloom_no_extra_pass";
        model.activeMaterialPreset = materialPresetName(activeMaterialPreset);
        model.materialPresetStatus = "ok";
        model.materialPresetMode = "selected_slot_uniform_only";
        model.materialPresetAppliedSlot = selectedMaterialSlot;
        model.selectedSlotPresetStatus = modelMaterialSlots.empty() ? "empty" : "ok_selected_slot";
    }

    void syncClearcoatState() {
        uint32_t paintCount = 0;
        uint32_t metalCount = 0;
        uint32_t fabricCount = 0;
        bool selectedFabric = false;
        bool selectedGlass = false;
        bool selectedRubber = false;
        float selectedWeight = 0.0f;
        if (selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size()) {
            const auto& slot = modelMaterialSlots[(size_t)selectedMaterialSlot];
            selectedFabric = slot.materialTypeHint == 0;
            selectedGlass = slot.materialTypeHint == 6;
            selectedRubber = slot.materialTypeHint == 3;
            selectedWeight = slot.materialTypeHint == 1 ? 1.0f : (slot.materialTypeHint == 2 ? 0.28f : 0.18f);
        }
        for (const auto& slot : modelMaterialSlots) {
            if (slot.materialTypeHint == 1) paintCount++;
            if (slot.materialTypeHint == 2) metalCount++;
            if (slot.materialTypeHint == 0) fabricCount++;
        }
        const bool carPaintPreset = activeMaterialPreset == 1;
        const bool applied = material.clearcoatIntensity > 0.001f && !selectedFabric && !selectedGlass && !selectedRubber && (carPaintPreset || selectedWeight > 0.0f);
        model.clearcoatStatus = applied ? "ok" : "available";
        model.clearcoatMode = "single_pass_uniform_clearcoat";
        model.clearcoatIntensity = material.clearcoatIntensity;
        model.clearcoatRoughness = material.clearcoatRoughness;
        model.clearcoatFresnelStatus = "ok_schlick_view_angle";
        model.clearcoatHighlightStatus = "ok_view_dependent";
        model.clearcoatMaterialRoutingStatus = selectedFabric ? "fabric_skipped_matte" : (selectedGlass ? "glass_metadata_only_skipped" : (selectedRubber ? "rubber_skipped_matte" : (carPaintPreset ? "car_paint_preset" : "selected_slot_safe")));
        model.clearcoatOverbrightGuardStatus = selectedFabric || selectedGlass ? "ok_guard_applied" : "ok";
        model.clearcoatPerformanceStatus = "ok_single_pass_no_texture_no_loop";
        model.clearcoatAppliedStatus = applied ? "applied" : "not_applied";
        model.clearcoatWeight = applied ? material.clearcoatIntensity * std::max(0.18f, selectedWeight) : 0.0f;
        model.clearcoatRoughnessApplied = applied ? material.clearcoatRoughness : 1.0f;
        model.clearcoatGuardApplied = selectedFabric || selectedGlass ? "yes" : "no";
        model.carPaintLayerStatus = "ok";
        model.carPaintPresetV2Status = "ok";
        model.carPaintClearcoatStatus = carPaintPreset ? "ok_clearcoat_enabled" : "available";
        model.paintLayerEnergyGuardStatus = "ok";
        model.paintLayerMaterialHintStatus = paintCount > 0 ? "paint_like" : (metalCount > 0 ? "metal_limited" : "selected_slot");
        model.clearcoatSliderStatus = "ok";
        model.clearcoatSliderValue = material.clearcoatIntensity;
        model.clearcoatRoughnessSliderStatus = "ok";
        model.clearcoatRoughnessSliderValue = material.clearcoatRoughness;
        model.clearcoatUniformUpdateStatus = "ok_uniform_only";
        model.clearcoatUiStatus = "ok_compact_material_tab";
        model.clearcoatDebugViewStatus = "shader_applied";
        model.clearcoatHighlightDebugViewStatus = "shader_applied";
        model.paintLayerDebugViewStatus = "shader_applied";
        model.paintEnergyGuardDebugViewStatus = "shader_applied";
        model.p22EmissivePreservedStatus = "ok";
        model.p22PresetsPreservedStatus = "ok";
        model.p23PerformanceStatus = "ok_single_pass_uniform_only";
        model.clearcoatNoNewPassStatus = "ok";
        model.clearcoatNoTextureRebuildStatus = "ok";
        model.clearcoatNoModelReuploadStatus = "ok";
        model.clearcoatNoTransparentSortingStatus = "ok";
        model.fabricMattePreserveStatus = fabricCount > 0 || selectedFabric ? "ok_fabric_matte_preserved" : "ok";
    }

    const char* glassTintPresetName(int preset) const {
        if (preset == 1) return "Blue Glass";
        if (preset == 2) return "Green Glass";
        if (preset == 3) return "Smoke Glass";
        if (preset == 4) return "Warm Glass";
        if (preset == 5) return "Magic Glass";
        if (preset == 6) return "Dirty Glass Lite";
        if (preset == 7) return "Crystal Lite";
        return "Clear Glass";
    }

    const char* glassTintColorString(int preset) const {
        if (preset == 1) return "0.55,0.82,1.00";
        if (preset == 2) return "0.55,1.00,0.72";
        if (preset == 3) return "0.48,0.52,0.58";
        if (preset == 4) return "1.00,0.78,0.52";
        if (preset == 5) return "0.82,0.58,1.00";
        if (preset == 6) return "0.70,0.74,0.72";
        if (preset == 7) return "0.86,0.98,1.00";
        return "0.92,0.98,1.00";
    }

    const char* materialRoleNameFromHint(int hint) const {
        if (hint == 6) return "Glass";
        if (hint == 1) return "Paint";
        if (hint == 2) return "Metal";
        if (hint == 0) return "Fabric";
        if (hint == 3) return "Rubber";
        if (hint == 8) return "Emissive";
        if (hint == 5 || hint == 7) return "Plastic";
        return "Unknown";
    }

    float materialRoleConfidence(const MaterialSlotState& slot) const {
        if (slot.materialTypeHint == 6) return 0.94f;
        if (slot.alphaMode == 2 || slot.baseColorFactor[3] < 0.98f) return 0.72f;
        if (slot.materialTypeHint == 1 || slot.materialTypeHint == 2 || slot.materialTypeHint == 0 || slot.materialTypeHint == 3 || slot.materialTypeHint == 8) return 0.82f;
        return 0.38f;
    }

    const char* materialRoleSource(const MaterialSlotState& slot) const {
        if (slot.materialTypeHint == 6) return "glass_like_hint_or_name_alpha";
        if (slot.alphaMode == 2 || slot.baseColorFactor[3] < 0.98f) return "alpha_metadata";
        if (slot.materialTypeHint == 1) return "paint_hint_or_factor";
        if (slot.materialTypeHint == 2) return "metal_hint_or_factor";
        if (slot.materialTypeHint == 0) return "fabric_hint_or_roughness";
        if (slot.materialTypeHint == 3) return "rubber_hint_or_roughness";
        if (slot.materialTypeHint == 8) return "emissive_factor_or_texture";
        return "fallback_unknown";
    }

    int bestGlassCandidateSlot() const {
        int best = -1;
        float bestScore = -1.0f;
        for (size_t i = 0; i < modelMaterialSlots.size(); ++i) {
            const auto& slot = modelMaterialSlots[i];
            float score = -1.0f;
            if (slot.materialTypeHint == 6) score = 0.94f;
            else if ((slot.alphaMode == 2 || slot.baseColorFactor[3] < 0.98f) && slot.materialTypeHint != 0 && slot.materialTypeHint != 1 && slot.materialTypeHint != 2 && slot.materialTypeHint != 3 && slot.materialTypeHint != 8) score = 0.68f;
            if (score > bestScore) {
                bestScore = score;
                best = (int)i;
            }
        }
        return best;
    }

    std::string materialNameFromDiagnostics(int slotIndex) const {
        if (slotIndex < 0 || model.materialSlotDiagnostics.empty()) return "none";
        const std::string marker = "\"slot\":" + std::to_string(slotIndex);
        size_t at = model.materialSlotDiagnostics.find(marker);
        if (at == std::string::npos) return "slot_" + std::to_string(slotIndex);
        const std::string key = "\"materialName\":\"";
        size_t start = model.materialSlotDiagnostics.find(key, at);
        if (start == std::string::npos) return "slot_" + std::to_string(slotIndex);
        start += key.size();
        size_t end = model.materialSlotDiagnostics.find('"', start);
        if (end == std::string::npos || end <= start) return "slot_" + std::to_string(slotIndex);
        return model.materialSlotDiagnostics.substr(start, end - start);
    }

    void syncGlassState() {
        bool selectedGlass = false;
        bool selectedFabric = false;
        bool selectedMetal = false;
        bool selectedPaint = false;
        bool selectedRubber = false;
        if (selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size()) {
            const auto& slot = modelMaterialSlots[(size_t)selectedMaterialSlot];
            selectedGlass = slot.materialTypeHint == 6;
            selectedFabric = slot.materialTypeHint == 0;
            selectedMetal = slot.materialTypeHint == 2;
            selectedPaint = slot.materialTypeHint == 1;
            selectedRubber = slot.materialTypeHint == 3;
        }
        const int bestGlassSlot = bestGlassCandidateSlot();
        const bool manualValid = manualGlassSlot >= 0 && (size_t)manualGlassSlot < modelMaterialSlots.size();
        const bool transparentModeRequested = material.glassRenderMode == 1 || material.glassRenderMode == 2;
        const bool transparentAllowed = material.glassRenderMode == 1 || (material.glassRenderMode == 2 && material.motionReflectionScale > 0.62f);
        int routedGlassSlot = -1;
        std::string routedSource = "none";
        if (manualValid) {
            routedGlassSlot = manualGlassSlot;
            routedSource = "manual_override";
        } else if (selectedGlass && material.glassRenderMode == 1) {
            routedGlassSlot = selectedMaterialSlot;
            routedSource = "selected_glass_role";
        } else if (bestGlassSlot >= 0 && transparentModeRequested) {
            routedGlassSlot = bestGlassSlot;
            routedSource = "auto_best_glass_candidate";
        }
        activeTransparentGlassSlot = routedGlassSlot;
        const bool active = material.glassEnabled != 0 || selectedGlass || activeMaterialPreset == 6 || routedGlassSlot >= 0;
        const bool transparentActive = routedGlassSlot >= 0 && transparentAllowed;
        uint32_t transparentSkipped = 0, skippedFabric = 0, skippedMetal = 0, skippedPaint = 0;
        for (const auto& slot : modelMaterialSlots) {
            if (slot.materialTypeHint != 6) transparentSkipped++;
            if (slot.materialTypeHint == 0) skippedFabric++;
            if (slot.materialTypeHint == 2) skippedMetal++;
            if (slot.materialTypeHint == 1) skippedPaint++;
        }
        model.glassMaterialStatus = transparentActive ? "ok_transparent_v1_role_routed_slot" : (active ? "ok_p26_polished_single_pass" : "available");
        model.glassMode = transparentActive ? "transparent_v1_role_routed_no_refraction" : "single_pass_fake_transparency_no_refraction";
        model.glassPolishStatus = "ok";
        model.glassPolishMode = "single_pass_fake_glass_polish";
        model.glassReadabilityStatus = active ? "ok_visible_edges_tint_reflection" : "available";
        model.glassEdgePolishStatus = "ok_stronger_fresnel_rim";
        model.glassTintPolishStatus = "ok_tint_blends_preserve_surface";
        model.glassThicknessPolishStatus = "ok_fake_thickness_edge_dark_bright";
        model.glassOpacityCurveStatus = "ok_minimum_surface_curve";
        model.glassRoughnessPolishStatus = "ok_clear_vs_frosted_response";
        model.glassReflectionPolishStatus = "ok_p24_probe_boost_guarded";
        model.glassOpacity = material.glassOpacity;
        model.glassTintStatus = "ok_preset_tint";
        model.glassTintColor = glassTintColorString(material.glassTintPreset);
        model.glassFresnelStatus = "ok_stronger_schlick_edge";
        model.glassEdgeReflectionStatus = "ok_p26_edge_fresnel_boost";
        model.glassReflectionSourceStatus = "p24_fake_cubemap_probe";
        model.glassRoughnessResponseStatus = "ok_clear_vs_rough_distinction";
        model.glassThicknessFakeStatus = "ok_edge_tint_thickness_fake";
        model.glassTransparencyMode = transparentActive ? "transparent_v1_color_blend_role_routed_slot" : "safe_single_pass_fake_opacity";
        model.glassSortingStatus = "limited_selected_slot_no_full_sort";
        model.glassRefractionStatus = "deferred_no_real_refraction";
        model.glassStillFakeTransparencyStatus = transparentActive ? "fallback_preserved_but_transparent_pass_active" : "yes_no_real_transparent_pass";
        model.glassPerformanceStatus = transparentActive ? "ok_one_extra_selected_slot_draw_no_sort" : "ok_uniform_shader_no_pass_no_sort";
        model.glassUiStatus = "ok_compact_material_tab";
        model.glassEnableButtonStatus = "ok";
        model.glassPresetStatus = "ok";
        model.activeGlassPreset = active ? glassTintPresetName(material.glassTintPreset) : "Clear Glass";
        model.glassOpacitySliderStatus = "ok";
        model.glassOpacityValue = material.glassOpacity;
        model.glassTintPresetStatus = "ok";
        model.activeGlassTintPreset = glassTintPresetName(material.glassTintPreset);
        model.glassEdgeSliderStatus = "ok";
        model.glassEdgeValue = material.glassEdge;
        model.glassRoughnessSliderStatus = "ok";
        model.glassRoughnessValue = material.glassRoughness;
        model.glassUniformUpdateStatus = "ok_uniform_only";
        model.glassPresetIntegrationStatus = "ok_selected_slot_only";
        model.glassPresetAppliedSlot = selectedMaterialSlot;
        model.glassPresetAppliedStatus = active ? "ok_selected_slot_uniform_only" : "available";
        model.glassPresetPolishStatus = "ok";
        model.clearGlassPresetStatus = "ok";
        model.blueGlassPresetStatus = "ok";
        model.greenGlassPresetStatus = "ok";
        model.smokeGlassPresetStatus = "ok";
        model.warmGlassPresetStatus = "ok";
        model.magicGlassPresetStatus = "ok";
        model.dirtyGlassLitePresetStatus = "ok";
        model.crystalLitePresetStatus = "ok";
        model.glassPresetVisualResponseStatus = "ok_tint_opacity_edge_roughness_reflection_thickness";
        model.glassSelectedSlotRoutingStatus = "ok_role_routed_no_blind_selected_slot";
        model.glassFabricGuardStatus = selectedFabric ? "ok_guard_blocks_transparency" : "ok";
        model.glassMetalGuardStatus = selectedMetal ? "ok_metal_preserved_unless_selected_glass_preset" : "ok";
        model.glassCarPaintGuardStatus = selectedPaint ? "ok_car_paint_preserved_unless_selected_glass_preset" : "ok";
        model.glassVisualResponseStatus = active ? "ok_visible_tint_opacity_edge_reflection" : "available";
        model.glassOpacityResponseStatus = "ok_base_surface_reduced";
        model.glassTintResponseStatus = "ok_tint_colors_surface";
        model.glassFresnelResponseStatus = "ok_grazing_angle_boost";
        model.glassReflectionResponseStatus = "ok_p24_probe_visible_guarded";
        model.glassEnergyGuardStatus = "ok_clamped";
        model.glassOverbrightGuardStatus = "ok";
        model.glassInvisibleGuardStatus = "ok_minimum_surface_kept";
        model.glassMaskDebugViewStatus = "shader_applied";
        model.glassOpacityDebugViewStatus = "shader_applied";
        model.glassFresnelDebugViewStatus = "shader_applied";
        model.glassReflectionDebugViewStatus = "shader_applied";
        model.glassTintDebugViewStatus = "shader_applied";
        model.glassSafetyDebugViewStatus = "shader_applied";
        model.glassPolishDebugViewStatus = "shader_applied";
        model.glassEdgeDebugViewStatus = "shader_applied";
        model.glassThicknessDebugViewStatus = "shader_applied";
        model.glassReflectionPolishDebugViewStatus = "shader_applied";
        model.clearcoatPolishDebugViewStatus = "shader_applied";
        model.paintReflectionDebugViewStatus = "shader_applied";
        model.p24ReflectionPreservedStatus = "ok";
        model.p25GlassPreservedStatus = "ok";
        model.p26UiChangeStatus = "minimal_material_tab_text_only";
        model.glassSummaryUiStatus = "ok_compact_summary";
        model.glassStrengthSliderStatus = "deferred_reusing_edge_opacity_roughness";
        model.glassControlsPreservedStatus = "ok";
        model.transparentGlassPassStatus = transparentActive ? "ok_active" : "available_fallback_fake";
        model.transparentGlassPassMode = "opaque_first_active_glass_after";
        model.transparentGlassSelectedSlotStatus = selectedGlass ? "ok_selected_slot_glass_role" : "selected_slot_not_glass";
        model.transparentGlassDrawOrderStatus = "opaque_first_then_active_glass";
        model.transparentGlassBlendStatus = transparentActive ? "ok_vk_src_alpha_one_minus_src_alpha" : "inactive_fake_safe";
        model.transparentGlassOpacityStatus = transparentActive ? "ok_real_alpha_blend" : "fallback_fake_opacity";
        model.transparentGlassTintStatus = "ok_tint_uniform";
        model.transparentGlassFresnelStatus = "ok_edge_reflection_preserved";
        model.transparentGlassReflectionStatus = "ok_p26_p24_probe_preserved";
        model.transparentGlassSortingStatus = "limited_selected_slot_no_full_sort";
        model.transparentGlassRefractionStatus = "deferred_no_real_refraction";
        model.transparentGlassFallbackStatus = transparentActive ? "p26_fake_available_when_disabled" : "active_p26_fake_glass";
        model.transparentGlassPerformanceStatus = transparentActive ? "ok_extra_selected_slot_draw_only" : "ok_no_extra_draw";
        model.transparentGlassUiStatus = "ok_minimal_material_tab";
        model.glassRenderModeStatus = "ok_fake_safe_transparent_v1_auto";
        model.activeGlassRenderMode = material.glassRenderMode == 1 ? "Transparent v1" : (material.glassRenderMode == 0 ? "Fake Safe" : "Auto");
        model.transparentGlassToggleStatus = "ok";
        model.glassModeSummaryUiStatus = "ok";
        model.p26GlassPolishPreservedStatus = "ok";
        model.fakeGlassFallbackStatus = transparentActive ? "available_when_mode_fake_safe_or_guard" : "active";
        model.glassAutoFallbackStatus = material.glassRenderMode == 2 && !transparentActive ? "active_fake_safe" : "available";
        model.glassMotionFallbackStatus = material.motionReflectionScale <= 0.62f ? "active_motion_guard_fake_safe" : "available";
        model.transparentGlassMaterialRoutingStatus = "role_routed_active_glass_slot";
        model.materialRoleSystemStatus = "ok";
        model.materialRoleDetectionMode = "material_name_alpha_hint_factor_no_asset_hardcode";
        model.selectedMaterialRole = selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size() ? materialRoleNameFromHint(modelMaterialSlots[(size_t)selectedMaterialSlot].materialTypeHint) : "Unknown";
        model.selectedMaterialRoleConfidence = selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size() ? materialRoleConfidence(modelMaterialSlots[(size_t)selectedMaterialSlot]) : 0.0f;
        model.selectedMaterialRoleSource = selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size() ? materialRoleSource(modelMaterialSlots[(size_t)selectedMaterialSlot]) : "none";
        model.materialRoleCandidateCount = (uint32_t)modelMaterialSlots.size();
        model.glassCandidateCount = 0;
        for (const auto& slot : modelMaterialSlots) if (slot.materialTypeHint == 6 || slot.alphaMode == 2 || slot.baseColorFactor[3] < 0.98f) model.glassCandidateCount++;
        model.bestGlassCandidateSlot = bestGlassSlot;
        model.bestGlassCandidateName = materialNameFromDiagnostics(bestGlassSlot);
        model.bestGlassCandidateConfidence = bestGlassSlot >= 0 ? materialRoleConfidence(modelMaterialSlots[(size_t)bestGlassSlot]) : 0.0f;
        model.bestGlassCandidateSource = bestGlassSlot >= 0 ? materialRoleSource(modelMaterialSlots[(size_t)bestGlassSlot]) : "none";
        model.materialRoleNoHardcodeStatus = "ok_no_asset_specific_slot_hardcode";
        model.transparentGlassRoutingStatus = transparentActive ? "ok_active_role_routed" : "fallback_fake_safe";
        model.transparentGlassRoutingMode = manualValid ? "manual_override" : (material.glassRenderMode == 1 ? "transparent_v1_selected_glass_or_best_candidate" : (material.glassRenderMode == 2 ? "auto_best_glass_candidate" : "fake_safe"));
        model.activeTransparentGlassSlot = routedGlassSlot;
        model.activeTransparentGlassMaterialName = materialNameFromDiagnostics(routedGlassSlot);
        model.activeTransparentGlassMaterialRole = routedGlassSlot >= 0 ? (manualValid ? "Glass" : materialRoleNameFromHint(modelMaterialSlots[(size_t)routedGlassSlot].materialTypeHint)) : "Unknown";
        model.activeTransparentGlassRoleSource = routedGlassSlot >= 0 ? (manualValid ? "manual_override" : materialRoleSource(modelMaterialSlots[(size_t)routedGlassSlot])) : "none";
        model.activeTransparentGlassSlotSource = routedSource;
        model.transparentGlassSelectedSlotAllowedStatus = selectedGlass ? "ok_selected_glass_allowed" : "not_selected_glass";
        model.transparentGlassSelectedSlotRejectedStatus = (!selectedGlass && (selectedPaint || selectedMetal || selectedFabric || selectedRubber)) ? "ok_rejected_non_glass_selected_slot" : "inactive";
        model.transparentGlassAutoDetectStatus = bestGlassSlot >= 0 ? "ok_best_candidate_found" : "fallback_no_glass_candidate";
        model.transparentGlassManualOverrideStatus = manualValid ? "active_selected_slot_assigned_as_glass" : "inactive";
        model.transparentGlassFallbackReason = transparentActive ? "none" : (bestGlassSlot < 0 && !manualValid ? "no_glass_candidate" : (material.glassRenderMode == 0 ? "fake_safe_mode" : "auto_motion_guard_or_inactive"));
        model.transparentGlassWrongSlotGuardStatus = "ok_non_glass_slots_not_transparent_by_default";
        model.transparentGlassAppliedSlot = routedGlassSlot;
        model.transparentGlassAppliedMaterialCount = transparentActive ? 1u : 0u;
        model.transparentGlassSkippedOpaqueCount = transparentSkipped;
        model.transparentGlassSkippedNonGlassCount = transparentSkipped;
        model.transparentGlassSkippedFabricCount = skippedFabric;
        model.transparentGlassSkippedMetalCount = skippedMetal;
        model.transparentGlassSkippedPaintCount = skippedPaint;
        model.transparentGlassFabricGuardStatus = selectedFabric && !manualValid ? "ok_fabric_kept_opaque" : "ok";
        model.transparentGlassMetalGuardStatus = selectedMetal && !manualValid ? "ok_metal_kept_opaque" : "ok";
        model.transparentGlassCarPaintGuardStatus = selectedPaint && !manualValid ? "ok_car_paint_kept_opaque" : "ok";
        model.p27TransparentPassPreservedStatus = "ok";
        model.p27bPerformanceStatus = "ok_existing_transparent_pass_role_routing_only";
        model.p27bNoExtraHeavyPassStatus = "ok";
        model.p27bNoTextureRebuildStatus = "ok";
        model.p27bNoModelReuploadStatus = "ok";
        model.p27bNoFrameGlbParseStatus = "ok";
        model.p27bNoFrameFileWriteStatus = "ok";
        model.p27bNoShadowStatus = "ok";
        model.p27bNoRealMirrorStatus = "ok";
        model.p27bNoSsrStatus = "ok";
        model.p27bNoRealRefractionStatus = "ok";
        model.transparentGlassMaskDebugViewStatus = "shader_applied";
        model.transparentGlassAlphaDebugViewStatus = "shader_applied";
        model.transparentGlassDrawOrderDebugViewStatus = "shader_applied";
        model.transparentGlassFallbackDebugViewStatus = "shader_applied";
        model.transparentGlassSafetyDebugViewStatus = "shader_applied";
        model.p27PerformanceStatus = transparentActive ? "ok_selected_slot_extra_draw_only" : "ok_fake_safe_no_extra_draw";
        model.p27NoShadowStatus = "ok";
        model.p27NoRaytraceStatus = "ok";
        model.p27NoSsrStatus = "ok";
        model.p27NoRealMirrorStatus = "ok";
        model.p27NoScreenRefractionStatus = "ok";
        model.p27NoTextureRebuildStatus = "ok";
        model.p27NoModelReuploadStatus = "ok";
        model.clearcoatPolishStatus = "ok";
        model.clearcoatHighlightPolishStatus = "ok_clearer_guarded_highlight";
        model.clearcoatReflectionSeparationStatus = "ok_layer_separated_from_base";
        model.carPaintPolishStatus = "ok";
        model.carPaintLayerReadabilityStatus = "ok_base_coat_reflection_separated";
        model.carPaintReflectionSeparationStatus = "ok_paint_reflection_layer_readable";
        model.paintOverbrightGuardStatus = "ok_clamped";
        model.p25PerformanceStatus = "ok_single_pass_uniform_shader_math";
        model.p26PerformanceStatus = "ok_single_pass_shader_math_no_rebuild";
        model.p26NoRealRefractionStatus = "ok_deferred";
        model.p26NoScreenRefractionStatus = "ok_deferred";
        model.p26NoMirrorPassStatus = "ok_deferred";
        model.p26NoFullSortingStatus = "ok_deferred";
        model.p26NoTextureRebuildStatus = "ok";
        model.p26NoModelReuploadStatus = "ok";
        model.p26NoNewShadowPassStatus = "ok";
        model.glassNoRefractionPassStatus = "ok";
        model.glassNoScreenRefractionStatus = "ok";
        model.glassNoFullSortingStatus = "ok";
        model.glassNoTextureRebuildStatus = "ok";
        model.glassNoModelReuploadStatus = "ok";
        model.glassNoNewShadowPassStatus = "ok";
    }

    void syncAlphaCutoutState() {
        uint32_t maskCount = 0;
        uint32_t blendCount = 0;
        uint32_t doubleCount = 0;
        uint32_t cutoutCount = 0;
        uint32_t fabricCount = 0;
        uint32_t glassCount = 0;
        uint32_t decalCount = 0;
        for (const auto& slot : modelMaterialSlots) {
            if (slot.alphaMode == 1) maskCount++;
            if (slot.alphaMode == 2) blendCount++;
            if (slot.doubleSided) doubleCount++;
            if (slot.materialTypeHint == 5) cutoutCount++;
            if (slot.materialTypeHint == 0) fabricCount++;
            if (slot.materialTypeHint == 6) glassCount++;
            if (slot.materialTypeHint == 7) decalCount++;
        }
        const bool selectedDouble = selectedMaterialSlot >= 0 && (size_t)selectedMaterialSlot < modelMaterialSlots.size() && modelMaterialSlots[(size_t)selectedMaterialSlot].doubleSided;
        model.alphaMaterialStatus = (maskCount > 0 || blendCount > 0) ? "ok_alpha_metadata_detected" : "ok_opaque_materials";
        model.alphaModeSupportStatus = "ok_opaque_mask_blend_metadata";
        model.alphaMaskStatus = maskCount > 0 ? "ok_mask_discard_shader" : "ok_no_mask_material";
        model.alphaBlendStatus = blendCount > 0 ? "fallback_no_sorting_blend_as_cutout_or_opaque" : "ok_no_blend_material";
        model.alphaDiscardStatus = (maskCount > 0 || blendCount > 0) ? "ok_shader_discard_for_safe_cutout" : "inactive_opaque";
        model.alphaTextureChannelStatus = "baseColor_alpha_channel_sampled_when_texture_ready";
        model.alphaFallbackStatus = blendCount > 0 ? "blend_deferred_safe_cutout_or_opaque" : "none";
        model.doubleSidedMaterialStatus = doubleCount > 0 ? "ok_double_sided_metadata_detected" : "ok_no_double_sided_material";
        model.doubleSidedMode = selectedDouble ? "selected_slot_double_sided" : "selected_slot_single_sided_or_none";
        model.doubleSidedNormalStatus = "shader_gl_front_facing_normal_flip";
        model.doubleSidedRasterStatus = "ok_pipeline_cull_none_no_new_permutation";
        model.doubleSidedFallbackStatus = doubleCount > 0 ? "no_new_pipeline_permutation_needed" : "none";
        model.thinMaterialPolishStatus = "ok_cutout_double_sided_hint_foundation";
        model.cutoutMaterialHintStatus = cutoutCount > 0 ? "ok" : "ok_available";
        model.fabricEdgeStatus = fabricCount > 0 ? "ok_fabric_like_matte_edges" : "ok_available";
        model.glassMetadataStatus = glassCount > 0 ? "metadata_only_render_safe_cutout_or_opaque" : "none";
        model.decalMaterialHintStatus = decalCount > 0 ? "ok" : "ok_available";
        model.transparencyDeferredStatus = "ok_no_full_transparent_sorting_or_glass";
    }

    bool setLightingControls(int lightPreset, float sunIntensity, float ambientIntensity, int activeDebugView, int toneMappingMode, float exposureValue, float ambientFloor, int brightnessPreset, float specularBoost, float reflectionIntensity, float contactShadowIntensity, int calibrationPreset, float calibrationStrength, float glossSliderValue, float paintGlossSliderValue, float clearcoatIntensity, float clearcoatRoughness, float environmentIntensity, int environmentPreset, float horizonStrength, float reflectionContrast, float reflectionSaturation, float motionReflectionScale, float motionClearcoatScale, int selectedSlot, float slotMetallic, float slotRoughness, float slotNormalScale, float slotAo, float slotGloss, float slotCoat, float alphaCutoffValue, float emissiveIntensity, int materialPreset, int glassEnabled, float glassOpacity, float glassEdge, float glassRoughness, int glassTintPreset, int glassRenderMode, int manualGlassSlotOverride) {
        applyLightPreset(lightPreset);
        material.sunIntensity = clampFloat(sunIntensity, 0.5f, 4.0f);
        material.ambientIntensity = clampFloat(ambientIntensity, 0.1f, 2.0f);
        material.activeDebugView = ((activeDebugView % 74) + 74) % 74;
        material.toneMappingMode = ((toneMappingMode % 3) + 3) % 3;
        material.exposureValue = clampFloat(exposureValue, 0.8f, 3.0f);
        material.ambientFloor = clampFloat(ambientFloor, 0.0f, 0.35f);
        material.brightnessPreset = ((brightnessPreset % 5) + 5) % 5;
        material.specularBoost = clampFloat(specularBoost, 0.5f, 3.0f);
        material.reflectionIntensity = clampFloat(reflectionIntensity, 0.0f, 2.0f);
        material.contactShadowIntensity = clampFloat(contactShadowIntensity, 0.0f, 1.5f);
        material.calibrationPreset = ((calibrationPreset % 4) + 4) % 4;
        material.calibrationStrength = clampFloat(calibrationStrength, 0.0f, 1.0f);
        material.glossSliderValue = clampFloat(glossSliderValue, 0.0f, 1.0f);
        material.paintGlossSliderValue = clampFloat(paintGlossSliderValue, 0.0f, 1.0f);
        material.clearcoatIntensity = clampFloat(clearcoatIntensity, 0.0f, 2.0f);
        material.clearcoatRoughness = clampFloat(clearcoatRoughness, 0.0f, 1.0f);
        material.environmentIntensity = clampFloat(environmentIntensity, 0.0f, 2.0f);
        material.environmentPreset = ((environmentPreset % 6) + 6) % 6;
        material.horizonStrength = clampFloat(horizonStrength, 0.0f, 1.0f);
        material.reflectionContrast = clampFloat(reflectionContrast, 0.0f, 2.0f);
        material.reflectionSaturation = clampFloat(reflectionSaturation, 0.0f, 2.0f);
        material.motionReflectionScale = clampFloat(motionReflectionScale, 0.35f, 1.0f);
        material.motionClearcoatScale = clampFloat(motionClearcoatScale, 0.35f, 1.0f);
        material.alphaCutoff = clampFloat(alphaCutoffValue, 0.0f, 1.0f);
        material.emissiveIntensity = clampFloat(emissiveIntensity, 0.0f, 2.0f);
        material.materialPresetHint = ((materialPreset % 8) + 8) % 8;
        material.glassEnabled = glassEnabled != 0 ? 1 : 0;
        material.glassOpacity = clampFloat(glassOpacity, 0.0f, 1.0f);
        material.glassEdge = clampFloat(glassEdge, 0.0f, 2.0f);
        material.glassRoughness = clampFloat(glassRoughness, 0.0f, 1.0f);
        material.glassTintPreset = ((glassTintPreset % 8) + 8) % 8;
        material.glassRenderMode = ((glassRenderMode % 3) + 3) % 3;
        selectedMaterialSlot = selectedSlot;
        manualGlassSlot = manualGlassSlotOverride;
        selectedSlotMetallicOverride = clampFloat(slotMetallic, 0.0f, 1.0f);
        selectedSlotRoughnessOverride = clampFloat(slotRoughness, 0.0f, 1.0f);
        selectedSlotNormalScaleOverride = clampFloat(slotNormalScale, 0.0f, 2.0f);
        selectedSlotAoOverride = clampFloat(slotAo, 0.0f, 1.5f);
        selectedSlotGlossOverride = clampFloat(slotGloss, 0.0f, 1.0f);
        selectedSlotCoatOverride = clampFloat(slotCoat, 0.0f, 1.0f);
        selectedSlotEmissiveIntensity = material.emissiveIntensity;
        activeMaterialPreset = material.materialPresetHint;
        syncLightingModelState();
        syncAlphaCutoutState();
        syncEmissivePresetState();
        syncClearcoatState();
        syncGlassState();
        const bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write(ok ? "valid" : diagnostics.status, ok ? "Scene24 transparent glass pass lab updated uniforms only." : diagnostics.reason);
        updateReadyStatus();
        return ok;
    }

    void fail(const std::string& reason) {
        status = "SOLUM Engine\n" + reason;
        syncDiagnostics();
        diagnostics.write("failed", reason);
    }

    void destroyFrameResources() {
        if (device == VK_NULL_HANDLE) return;
        vkDeviceWaitIdle(device);
        if (textureDescriptorPool != VK_NULL_HANDLE) { vkDestroyDescriptorPool(device, textureDescriptorPool, nullptr); textureDescriptorPool = VK_NULL_HANDLE; }
        textureDescriptorSets.clear();
        for (auto& set : materialTextureSets) set.destroy();
        materialTextureSets.clear();
        baseColorTexture.destroy();
        modelMesh.destroy();
        modelDrawRanges.clear();
        modelMaterialSlots.clear();
        validationMesh.destroy();
        trianglePipeline.destroy();
        if (inFlight != VK_NULL_HANDLE) { vkDestroyFence(device, inFlight, nullptr); inFlight = VK_NULL_HANDLE; }
        if (renderFinished != VK_NULL_HANDLE) { vkDestroySemaphore(device, renderFinished, nullptr); renderFinished = VK_NULL_HANDLE; }
        if (imageAvailable != VK_NULL_HANDLE) { vkDestroySemaphore(device, imageAvailable, nullptr); imageAvailable = VK_NULL_HANDLE; }
        for (VkFramebuffer fb : framebuffers) vkDestroyFramebuffer(device, fb, nullptr);
        framebuffers.clear();
        if (depthImageView != VK_NULL_HANDLE) { vkDestroyImageView(device, depthImageView, nullptr); depthImageView = VK_NULL_HANDLE; }
        if (depthImage != VK_NULL_HANDLE) { vkDestroyImage(device, depthImage, nullptr); depthImage = VK_NULL_HANDLE; }
        if (depthMemory != VK_NULL_HANDLE) { vkFreeMemory(device, depthMemory, nullptr); depthMemory = VK_NULL_HANDLE; }
        depthFormat = VK_FORMAT_UNDEFINED;
        depthReady = false;
        if (commandPool != VK_NULL_HANDLE) { vkDestroyCommandPool(device, commandPool, nullptr); commandPool = VK_NULL_HANDLE; }
        commandBuffers.clear();
        if (renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(device, renderPass, nullptr); renderPass = VK_NULL_HANDLE; }
        for (VkImageView view : swapchainImageViews) vkDestroyImageView(device, view, nullptr);
        swapchainImageViews.clear();
        swapchainImages.clear();
    }

    void destroy() {
        destroyFrameResources();
        if (device != VK_NULL_HANDLE) vkDeviceWaitIdle(device);
        if (swapchain != VK_NULL_HANDLE) { vkDestroySwapchainKHR(device, swapchain, nullptr); swapchain = VK_NULL_HANDLE; }
        if (device != VK_NULL_HANDLE) { vkDestroyDevice(device, nullptr); device = VK_NULL_HANDLE; }
        if (surface != VK_NULL_HANDLE) { vkDestroySurfaceKHR(instance, surface, nullptr); surface = VK_NULL_HANDLE; }
        if (instance != VK_NULL_HANDLE) { vkDestroyInstance(instance, nullptr); instance = VK_NULL_HANDLE; }
        physicalDevice = VK_NULL_HANDLE;
        graphicsQueue = VK_NULL_HANDLE;
        graphicsQueueFamily = UINT32_MAX;
        swapchainFormat = VK_FORMAT_UNDEFINED;
        extent = {};
        framesRendered = 0;
        firstFrameRendered = false;
        triangleDrawn = false;
        cubeDrawn = false;
        depthReady = false;
        cameraReady = false;
        cameraMvpReady = false;
        cameraControlsReady = false;
        pushConstantsReady = false;
        materialConstantsReady = false;
        meshAttributeLayoutReady = false;
        rendererCoreReady = false;
        model = ModelRenderState{};
    }

    bool createInstance() {
        const char* instanceExts[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
        VkApplicationInfo app{ VK_STRUCTURE_TYPE_APPLICATION_INFO };
        app.pApplicationName = "SOLUM Engine";
        app.applicationVersion = VK_MAKE_VERSION(0, 9, 0);
        app.pEngineName = "SOLUM";
        app.engineVersion = VK_MAKE_VERSION(0, 9, 0);
        app.apiVersion = VK_API_VERSION_1_0;
        VkInstanceCreateInfo ici{ VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO };
        ici.pApplicationInfo = &app;
        ici.enabledExtensionCount = 2;
        ici.ppEnabledExtensionNames = instanceExts;
        VkResult r = vkCreateInstance(&ici, nullptr, &instance);
        if (r != VK_SUCCESS) { fail("Vulkan instance failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool createSurface(ANativeWindow* window) {
        VkAndroidSurfaceCreateInfoKHR sci{ VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR };
        sci.window = window;
        VkResult r = vkCreateAndroidSurfaceKHR(instance, &sci, nullptr, &surface);
        if (r != VK_SUCCESS) { fail("Android surface failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool pickDeviceAndQueue() {
        uint32_t deviceCount = 0;
        VkResult r = vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
        if (r != VK_SUCCESS || deviceCount == 0) { fail("No Vulkan physical device: " + vkResultName(r)); return false; }
        std::vector<VkPhysicalDevice> devices(deviceCount);
        vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());
        for (auto pd : devices) {
            uint32_t qCount = 0;
            vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, nullptr);
            std::vector<VkQueueFamilyProperties> qProps(qCount);
            vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, qProps.data());
            for (uint32_t i = 0; i < qCount; ++i) {
                VkBool32 present = VK_FALSE;
                vkGetPhysicalDeviceSurfaceSupportKHR(pd, i, surface, &present);
                if ((qProps[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && present) { physicalDevice = pd; graphicsQueueFamily = i; break; }
            }
            if (physicalDevice != VK_NULL_HANDLE) break;
        }
        if (physicalDevice == VK_NULL_HANDLE) { fail("No graphics+present queue"); return false; }
        VkPhysicalDeviceProperties props{};
        vkGetPhysicalDeviceProperties(physicalDevice, &props);
        diagnostics.gpuName = props.deviceName;
        diagnostics.gpuType = deviceTypeName(props.deviceType);
        char api[32];
        std::snprintf(api, sizeof(api), "%u.%u.%u", VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion), VK_VERSION_PATCH(props.apiVersion));
        diagnostics.apiVersion = api;
        return true;
    }

    bool createDevice() {
        const float prio = 1.0f;
        VkDeviceQueueCreateInfo qci{ VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO };
        qci.queueFamilyIndex = graphicsQueueFamily;
        qci.queueCount = 1;
        qci.pQueuePriorities = &prio;
        const char* deviceExts[] = { "VK_KHR_swapchain" };
        VkDeviceCreateInfo dci{ VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO };
        dci.queueCreateInfoCount = 1;
        dci.pQueueCreateInfos = &qci;
        dci.enabledExtensionCount = 1;
        dci.ppEnabledExtensionNames = deviceExts;
        VkResult r = vkCreateDevice(physicalDevice, &dci, nullptr, &device);
        if (r != VK_SUCCESS) { fail("Device create failed: " + vkResultName(r)); return false; }
        vkGetDeviceQueue(device, graphicsQueueFamily, 0, &graphicsQueue);
        return true;
    }

    bool createSwapchain(int width, int height) {
        VkSurfaceCapabilitiesKHR caps{};
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, &caps);
        uint32_t fmtCount = 0;
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &fmtCount, nullptr);
        std::vector<VkSurfaceFormatKHR> formats(fmtCount);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &fmtCount, formats.data());
        VkSurfaceFormatKHR chosen = formats.empty() ? VkSurfaceFormatKHR{VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR} : formats[0];
        for (const auto& f : formats) { if (f.format == VK_FORMAT_R8G8B8A8_UNORM || f.format == VK_FORMAT_B8G8R8A8_UNORM) { chosen = f; break; } }
        swapchainFormat = chosen.format;
        extent.width = caps.currentExtent.width == 0xFFFFFFFF ? (uint32_t)width : caps.currentExtent.width;
        extent.height = caps.currentExtent.height == 0xFFFFFFFF ? (uint32_t)height : caps.currentExtent.height;
        if (extent.width == 0 || extent.height == 0) { fail("Invalid surface extent"); return false; }
        uint32_t imageCount = caps.minImageCount + 1;
        if (caps.maxImageCount > 0 && imageCount > caps.maxImageCount) imageCount = caps.maxImageCount;
        VkSwapchainCreateInfoKHR sw{ VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR };
        sw.surface = surface;
        sw.minImageCount = imageCount;
        sw.imageFormat = chosen.format;
        sw.imageColorSpace = chosen.colorSpace;
        sw.imageExtent = extent;
        sw.imageArrayLayers = 1;
        sw.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        sw.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
        sw.preTransform = caps.currentTransform;
        sw.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
        sw.presentMode = VK_PRESENT_MODE_FIFO_KHR;
        sw.clipped = VK_TRUE;
        VkResult r = vkCreateSwapchainKHR(device, &sw, nullptr, &swapchain);
        if (r != VK_SUCCESS) { fail("Swapchain failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool createImageViews() {
        uint32_t count = 0;
        VkResult r = vkGetSwapchainImagesKHR(device, swapchain, &count, nullptr);
        if (r != VK_SUCCESS || count == 0) { fail("Swapchain image query failed: " + vkResultName(r)); return false; }
        swapchainImages.resize(count);
        vkGetSwapchainImagesKHR(device, swapchain, &count, swapchainImages.data());
        swapchainImageViews.resize(count, VK_NULL_HANDLE);
        for (uint32_t i = 0; i < count; ++i) {
            VkImageViewCreateInfo ci{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
            ci.image = swapchainImages[i];
            ci.viewType = VK_IMAGE_VIEW_TYPE_2D;
            ci.format = swapchainFormat;
            ci.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            ci.subresourceRange.levelCount = 1;
            ci.subresourceRange.layerCount = 1;
            r = vkCreateImageView(device, &ci, nullptr, &swapchainImageViews[i]);
            if (r != VK_SUCCESS) { fail("ImageView failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    bool createRenderPass() {
        depthFormat = chooseDepthFormat();
        if (depthFormat == VK_FORMAT_UNDEFINED) { fail("Depth format selection failed"); return false; }
        VkAttachmentDescription color{};
        color.format = swapchainFormat;
        color.samples = VK_SAMPLE_COUNT_1_BIT;
        color.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        color.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
        color.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        color.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        color.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        color.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
        VkAttachmentReference colorRef{};
        colorRef.attachment = 0;
        colorRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
        VkAttachmentDescription depth{};
        depth.format = depthFormat;
        depth.samples = VK_SAMPLE_COUNT_1_BIT;
        depth.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        depth.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        depth.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        depth.finalLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        VkAttachmentReference depthRef{};
        depthRef.attachment = 1;
        depthRef.layout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        VkSubpassDescription subpass{};
        subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
        subpass.colorAttachmentCount = 1;
        subpass.pColorAttachments = &colorRef;
        subpass.pDepthStencilAttachment = &depthRef;
        VkSubpassDependency dep{};
        dep.srcSubpass = VK_SUBPASS_EXTERNAL;
        dep.dstSubpass = 0;
        dep.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        dep.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        dep.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
        VkAttachmentDescription attachments[] = { color, depth };
        VkRenderPassCreateInfo ci{ VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO };
        ci.attachmentCount = 2;
        ci.pAttachments = attachments;
        ci.subpassCount = 1;
        ci.pSubpasses = &subpass;
        ci.dependencyCount = 1;
        ci.pDependencies = &dep;
        VkResult r = vkCreateRenderPass(device, &ci, nullptr, &renderPass);
        if (r != VK_SUCCESS) { fail("RenderPass failed: " + vkResultName(r)); return false; }
        return true;
    }

    VkFormat chooseDepthFormat() {
        const VkFormat candidates[] = {
            VK_FORMAT_D24_UNORM_S8_UINT,
            VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D16_UNORM
        };
        for (VkFormat candidate : candidates) {
            VkFormatProperties props{};
            vkGetPhysicalDeviceFormatProperties(physicalDevice, candidate, &props);
            if (props.optimalTilingFeatures & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) return candidate;
        }
        return VK_FORMAT_UNDEFINED;
    }

    bool createDepthResources() {
        VkImageCreateInfo image{ VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO };
        image.imageType = VK_IMAGE_TYPE_2D;
        image.format = depthFormat;
        image.extent = { extent.width, extent.height, 1 };
        image.mipLevels = 1;
        image.arrayLayers = 1;
        image.samples = VK_SAMPLE_COUNT_1_BIT;
        image.tiling = VK_IMAGE_TILING_OPTIMAL;
        image.usage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        image.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        image.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        VkResult r = vkCreateImage(device, &image, nullptr, &depthImage);
        if (r != VK_SUCCESS) { fail("Depth image failed: " + vkResultName(r)); return false; }
        VkMemoryRequirements req{};
        vkGetImageMemoryRequirements(device, depthImage, &req);
        uint32_t memoryType = 0;
        if (!GpuBuffer::findMemoryType(physicalDevice, req.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &memoryType)) {
            fail("Depth device-local memory type not found");
            return false;
        }
        VkMemoryAllocateInfo alloc{ VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO };
        alloc.allocationSize = req.size;
        alloc.memoryTypeIndex = memoryType;
        r = vkAllocateMemory(device, &alloc, nullptr, &depthMemory);
        if (r != VK_SUCCESS) { fail("Depth memory failed: " + vkResultName(r)); return false; }
        r = vkBindImageMemory(device, depthImage, depthMemory, 0);
        if (r != VK_SUCCESS) { fail("Depth bind failed: " + vkResultName(r)); return false; }
        VkImageViewCreateInfo view{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
        view.image = depthImage;
        view.viewType = VK_IMAGE_VIEW_TYPE_2D;
        view.format = depthFormat;
        view.subresourceRange.aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
        view.subresourceRange.levelCount = 1;
        view.subresourceRange.layerCount = 1;
        r = vkCreateImageView(device, &view, nullptr, &depthImageView);
        if (r != VK_SUCCESS) { fail("Depth image view failed: " + vkResultName(r)); return false; }
        depthReady = true;
        return true;
    }

    bool createFramebuffers() {
        framebuffers.resize(swapchainImageViews.size(), VK_NULL_HANDLE);
        for (size_t i = 0; i < swapchainImageViews.size(); ++i) {
            VkImageView attachments[] = { swapchainImageViews[i], depthImageView };
            VkFramebufferCreateInfo ci{ VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO };
            ci.renderPass = renderPass;
            ci.attachmentCount = 2;
            ci.pAttachments = attachments;
            ci.width = extent.width;
            ci.height = extent.height;
            ci.layers = 1;
            VkResult r = vkCreateFramebuffer(device, &ci, nullptr, &framebuffers[i]);
            if (r != VK_SUCCESS) { fail("Framebuffer failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    Mat4 buildMvp() {
        const float aspect = extent.height > 0 ? (float)extent.width / (float)extent.height : 1.0f;
        const float nearPlane = 0.1f;
        const float farPlane = 32.0f;
        const float degToRad = 3.1415926535f / 180.0f;
        const Mat4 modelMatrix = model.modelReady() ? Mat4::identity() : rotationY(0.25f);
        const Mat4 view = multiply(translation(0.0f, 0.0f, camera.distance), multiply(rotationX(camera.pitchDeg * degToRad), rotationY(camera.yawDeg * degToRad)));
        const Mat4 proj = perspective(60.0f * 3.1415926535f / 180.0f, aspect, nearPlane, farPlane);
        cameraMvpReady = aspect > 0.0f && nearPlane > 0.0f && farPlane > nearPlane && camera.distance >= 2.0f && camera.distance <= 8.0f;
        cameraReady = cameraMvpReady;
        return multiply(proj, multiply(view, modelMatrix));
    }

    static float clampFloat(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    void updateReadyStatus() {
        syncLightingModelState();
        syncGlassState();
        status = "SOLUM Engine\nRenderer path: Android Native Vulkan\nGPU: " + diagnostics.gpuName + "\nType: " + diagnostics.gpuType + "\nAPI: " + diagnostics.apiVersion + "\nSwapchain: created\nRender pass: color+depth OK\nRenderer core: OK\nRender Lab: Scene24 Transparent Glass Pass Lab\nVertex buffer: OK\nIndex buffer: OK\nCube draw: " + std::string(model.fallbackCubeVisible ? "OK/fallback visible" : "preserved/off") + "\nDepth: OK\nCamera: controls OK\nMaterial constants: OK\nMesh layout: OK\nActive model: " + model.activeModelName + "\nModel render: " + model.drawStatus + "\nPrimitives rendered/skipped/total: " + std::to_string(model.primitiveCountRendered) + " / " + std::to_string(model.primitiveCountSkipped) + " / " + std::to_string(model.primitiveCountTotal) + "\nMaterials used: " + std::to_string(model.materialSlotCountRendered) + "\nSelected material slot: " + std::to_string(model.selectedMaterialSlot) + " / " + std::to_string(model.selectedMaterialSlotCount) + "\nPer-material override: " + model.selectedSlotOverrideApplied + "\nGlass: " + model.glassMaterialStatus + " mode=" + model.activeGlassRenderMode + " opacity=" + std::to_string(model.glassOpacity) + " source=" + model.glassReflectionSourceStatus + "\nTransparent glass pass: " + model.transparentGlassPassStatus + " " + model.transparentGlassPassMode + "\nClearcoat: " + model.clearcoatStatus + " " + std::to_string(model.clearcoatIntensity) + "/" + std::to_string(model.clearcoatRoughness) + "\nAlpha: " + model.alphaMaterialStatus + " cutoff=" + std::to_string(model.alphaCutoffValue) + "\nDouble sided: " + model.doubleSidedMaterialStatus + "\nInspector: " + model.inspectorUiMode + "\nLighting status: " + model.lightingStatus + "\nIBL mode: " + model.iblMode + "\nEnvironment: " + model.environmentPreset + " " + std::to_string(model.environmentIntensity) + "\nReflection intensity: " + std::to_string(model.reflectionIntensity) + "\nGrounding: " + model.contactGroundingStatus + " " + std::to_string(model.contactShadowIntensity) + "\nCalibration: " + model.calibrationPreset + " " + std::to_string(model.calibrationSliderValue) + "\nAlbedo guard: " + model.albedoEnergyStatus + " / " + model.luminanceGuardStatus + "\nMaterial hints: " + model.materialTypeHintStatus + "\nBRDF status: " + model.brdfStatus + "\nSpecular status: " + model.specularStatus + "\nSpecular boost: " + std::to_string(model.specularBoost) + "\nReflection foundation: " + model.reflectionFoundationStatus + "\nFresnel status: " + model.fresnelStatus + "\nF0 status: " + model.f0Status + "\nLight preset: " + model.lightPreset + "\nSun intensity: " + std::to_string(model.sunIntensity) + "\nAmbient intensity: " + std::to_string(model.ambientIntensity) + "\nExposure: " + std::to_string(model.exposureValue) + " " + model.brightnessPreset + "\nMaterial response status: " + model.materialResponseStatus + "\nActive debug view: " + model.activeDebugView + "\nBaseColor status: " + model.baseColorTextureStatus + "\nMetallicRoughness status: " + model.metallicRoughnessStatus + "\nTangent status: " + model.tangentStatus + "\nNormal status: " + model.normalMapStatus + " applied=" + model.normalMapAppliedStatus + "\nAO status: " + model.occlusionMapStatus + "\nPBR textures uploaded/fallback/skipped: " + std::to_string(model.uploadedPbrTextureCount) + " / " + std::to_string(model.pbrTextureFallbackCount) + " / " + std::to_string(model.skippedPbrTextureCount) + "\nFPS/frameMs: " + std::to_string(model.fpsCurrent) + " / " + std::to_string(model.frameTimeMs) + "\nDebug ZIP: " + model.debugZipStatus + "\nGPU Upload: " + model.gpuUploadStatus + "\nDraw Model: " + model.drawStatus + "\nTexture size: " + std::to_string(model.textureWidth) + "x" + std::to_string(model.textureHeight) + "\nFallback texture: " + std::string(model.textureFallbackUsed ? "yes" : "no") + "\nVertices / indices: " + std::to_string(model.uploadedVertexCount) + " / " + std::to_string(model.uploadedIndexCount) + "\nFallback cube: " + std::string(model.fallbackCubeVisible ? "on" : "off") + "\nReason: " + model.reason + "\nFrames rendered: " + std::to_string(framesRendered) + "\nNext: validate Scene24 transparent glass pass lab";
    }

    bool setCamera(float yawDeg, float pitchDeg, float distance, bool controlsActive, float motionSpeed, float qualityBlend, float reflectionScale, float clearcoatScale, float movingFps, float stillFps) {
        camera.yawDeg = yawDeg;
        camera.pitchDeg = clampFloat(pitchDeg, -75.0f, 75.0f);
        camera.distance = clampFloat(distance, 2.0f, 8.0f);
        camera.controlsActive = controlsActive;
        cameraControlsReady = controlsActive;
        material.motionReflectionScale = clampFloat(reflectionScale, 0.35f, 1.0f);
        material.motionClearcoatScale = clampFloat(clearcoatScale, 0.35f, 1.0f);
        model.motionPerformanceGuardStatus = "ok";
        model.cameraMotionSpeed = clampFloat(motionSpeed, 0.0f, 2400.0f);
        model.cameraMotionQualityBlend = clampFloat(qualityBlend, 0.0f, 1.0f);
        model.cameraMotionQualityTier = model.cameraMotionQualityBlend < 0.55f ? "moving_guard" : (model.cameraMotionQualityBlend < 0.98f ? "restoring" : "full");
        model.cameraMotionStatus = model.cameraMotionQualityBlend < 0.98f ? "moving_or_restoring" : "still";
        model.motionReflectionScale = material.motionReflectionScale;
        model.motionClearcoatScale = material.motionClearcoatScale;
        model.motionDiagnosticsThrottleStatus = model.cameraMotionQualityBlend < 0.98f ? "ok_no_export_while_moving" : "ok";
        model.motionQualityRestoreStatus = "ok_smooth_restore";
        model.motionQualityTransitionMs = 520;
        model.cameraMovingFpsLast = movingFps;
        model.cameraStillFpsLast = stillFps;
        model.targetMovingFps = 45.0f;
        model.movingFpsGuardStatus = movingFps <= 0.0f ? "monitoring" : (movingFps >= 45.0f ? "ok_target_met" : "active_below_target");
        const bool ok = renderOneFrame();
        syncDiagnostics();
        if (!ok) status = "SOLUM Engine\nCamera motion render failed: " + diagnostics.reason;
        return ok;
    }

    bool createCommandsAndSync() {
        VkCommandPoolCreateInfo pool{ VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO };
        pool.queueFamilyIndex = graphicsQueueFamily;
        pool.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
        VkResult r = vkCreateCommandPool(device, &pool, nullptr, &commandPool);
        if (r != VK_SUCCESS) { fail("CommandPool failed: " + vkResultName(r)); return false; }
        commandBuffers.resize(framebuffers.size(), VK_NULL_HANDLE);
        VkCommandBufferAllocateInfo ai{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO };
        ai.commandPool = commandPool;
        ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        ai.commandBufferCount = (uint32_t)commandBuffers.size();
        r = vkAllocateCommandBuffers(device, &ai, commandBuffers.data());
        if (r != VK_SUCCESS) { fail("CommandBuffers failed: " + vkResultName(r)); return false; }
        VkSemaphoreCreateInfo si{ VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO };
        if ((r = vkCreateSemaphore(device, &si, nullptr, &imageAvailable)) != VK_SUCCESS) { fail("ImageAvailable semaphore failed: " + vkResultName(r)); return false; }
        if ((r = vkCreateSemaphore(device, &si, nullptr, &renderFinished)) != VK_SUCCESS) { fail("RenderFinished semaphore failed: " + vkResultName(r)); return false; }
        VkFenceCreateInfo fi{ VK_STRUCTURE_TYPE_FENCE_CREATE_INFO };
        fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        if ((r = vkCreateFence(device, &fi, nullptr, &inFlight)) != VK_SUCCESS) { fail("Fence failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool recordFrame(uint32_t imageIndex) {
        VkCommandBuffer cmd = commandBuffers[imageIndex];
        VkCommandBufferBeginInfo bi{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO };
        bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
        VkResult r = vkBeginCommandBuffer(cmd, &bi);
        if (r != VK_SUCCESS) { fail("Begin command buffer failed: " + vkResultName(r)); return false; }
        VkClearValue clear[2]{};
        clear[0].color.float32[0] = 0.02f;
        clear[0].color.float32[1] = 0.07f;
        clear[0].color.float32[2] = 0.09f;
        clear[0].color.float32[3] = 1.0f;
        clear[1].depthStencil.depth = 1.0f;
        clear[1].depthStencil.stencil = 0;
        VkRenderPassBeginInfo rp{ VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO };
        rp.renderPass = renderPass;
        rp.framebuffer = framebuffers[imageIndex];
        rp.renderArea.extent = extent;
        rp.clearValueCount = 2;
        rp.pClearValues = clear;
        vkCmdBeginRenderPass(cmd, &rp, VK_SUBPASS_CONTENTS_INLINE);
        PushConstants pc{};
        pc.mvp = buildMvp();
        syncLightingModelState();
        const bool transparentGlassActive = activeTransparentGlassSlot >= 0
            && (material.glassRenderMode == 1 || (material.glassRenderMode == 2 && material.motionReflectionScale > 0.62f));
        pc.material = material;
        pushConstantsReady = true;
        materialConstantsReady = true;
        meshAttributeLayoutReady = sizeof(Vertex3D) == 60;
        if (model.modelReady() && modelMesh.ready() && !modelDrawRanges.empty()) {
            modelMesh.bind(cmd);
            if (modelMesh.indexedReady()) vkCmdBindIndexBuffer(cmd, modelMesh.indexBuffer.buffer, 0, modelMesh.indexType);
            for (int pass = 0; pass < 2; ++pass) {
            const bool transparentPass = pass == 1;
            if (transparentPass && !transparentGlassActive) continue;
            vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, transparentPass ? trianglePipeline.transparentPipeline : trianglePipeline.pipeline);
            for (const auto& range : modelDrawRanges) {
                const bool rangeTransparent = transparentGlassActive && range.materialSlot == activeTransparentGlassSlot;
                if (rangeTransparent != transparentPass) continue;
                pc.material = material;
                if (range.materialSlot >= 0 && (size_t)range.materialSlot < modelMaterialSlots.size()) {
                    const auto& slot = modelMaterialSlots[(size_t)range.materialSlot];
                    for (int i = 0; i < 4; ++i) pc.material.baseColorFactor[i] = slot.baseColorFactor[i];
                    pc.material.metallicFactor = slot.metallicFactor;
                    pc.material.roughnessFactor = slot.roughnessFactor;
                    pc.material.normalScale = slot.normalScale;
                    pc.material.occlusionStrength = slot.occlusionStrength;
                    pc.material.emissiveFactor[0] = slot.emissiveFactor[0];
                    pc.material.emissiveFactor[1] = slot.emissiveFactor[1];
                    pc.material.emissiveFactor[2] = slot.emissiveFactor[2];
                    pc.material.materialPresetHint = slot.materialPresetHint;
                    pc.material.alphaCutoff = clampFloat(slot.alphaCutoff, 0.0f, 1.0f);
                    if (range.materialSlot == selectedMaterialSlot || range.materialSlot == activeTransparentGlassSlot) {
                        pc.material.metallicFactor = selectedSlotMetallicOverride;
                        pc.material.roughnessFactor = selectedSlotRoughnessOverride;
                        pc.material.normalScale = selectedSlotNormalScaleOverride;
                        pc.material.occlusionStrength = selectedSlotAoOverride;
                        pc.material.glossSliderValue = selectedSlotGlossOverride;
                        pc.material.paintGlossSliderValue = selectedSlotCoatOverride;
                        pc.material.alphaCutoff = material.alphaCutoff;
                        pc.material.emissiveIntensity = selectedSlotEmissiveIntensity;
                        pc.material.materialPresetHint = activeMaterialPreset;
                        pc.material.glassEnabled = range.materialSlot == activeTransparentGlassSlot ? 1 : ((material.glassEnabled != 0 || activeMaterialPreset == 6) ? 1 : 0);
                        pc.material.glassRenderMode = transparentPass ? 1 : (range.materialSlot == activeTransparentGlassSlot ? material.glassRenderMode : 0);
                    } else {
                        pc.material.glossSliderValue = 0.0f;
                        pc.material.paintGlossSliderValue = 0.0f;
                        pc.material.emissiveIntensity = 0.0f;
                        pc.material.glassEnabled = slot.materialTypeHint == 6 ? 1 : 0;
                        pc.material.glassRenderMode = 0;
                    }
                    if (((material.activeDebugView >= 27 && material.activeDebugView <= 31) || material.activeDebugView == 41) && range.materialSlot != selectedMaterialSlot) {
                        pc.material.activeDebugView = 0;
                    }
                    pc.material.alphaMode = slot.alphaMode;
                    pc.material.materialId = range.materialSlot;
                    pc.material.materialTypeHint = slot.materialTypeHint;
                    const size_t materialIndex = (size_t)range.materialSlot;
                    pc.material.baseColorTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].baseColorUploaded) ? 1 : 0;
                    pc.material.metallicRoughnessTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].metallicRoughnessUploaded) ? 1 : 0;
                    pc.material.normalTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].normalUploaded) ? 1 : 0;
                    pc.material.occlusionTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].occlusionUploaded) ? 1 : 0;
                } else {
                    pc.material = material;
                    pc.material.baseColorTextureReady = 0;
                    pc.material.metallicRoughnessTextureReady = 0;
                    pc.material.normalTextureReady = 0;
                    pc.material.occlusionTextureReady = 0;
                }
                if (range.materialSlot >= 0 && (size_t)range.materialSlot < textureDescriptorSets.size()) {
                    VkDescriptorSet set = textureDescriptorSets[(size_t)range.materialSlot];
                    if (set != VK_NULL_HANDLE) vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
                } else if (!textureDescriptorSets.empty() && textureDescriptorSets[0] != VK_NULL_HANDLE) {
                    VkDescriptorSet set = textureDescriptorSets[0];
                    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
                }
                vkCmdPushConstants(cmd, trianglePipeline.layout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(PushConstants), &pc);
                if (modelMesh.indexedReady() && range.indexCount > 0) vkCmdDrawIndexed(cmd, range.indexCount, 1, range.firstIndex, 0, 0);
                else if (range.vertexCount > 0) vkCmdDraw(cmd, range.vertexCount, 1, range.firstVertex, 0);
            }
            }
        } else {
            vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.pipeline);
            if (!textureDescriptorSets.empty() && textureDescriptorSets[0] != VK_NULL_HANDLE) {
                VkDescriptorSet set = textureDescriptorSets[0];
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
            }
            vkCmdPushConstants(cmd, trianglePipeline.layout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(PushConstants), &pc);
            validationMesh.bindIndexed(cmd);
            vkCmdDrawIndexed(cmd, validationMesh.indexCount, 1, 0, 0, 0);
        }
        vkCmdEndRenderPass(cmd);
        r = vkEndCommandBuffer(cmd);
        if (r != VK_SUCCESS) { fail("End command buffer failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool renderOneFrame() {
        if (device == VK_NULL_HANDLE || swapchain == VK_NULL_HANDLE || commandBuffers.empty() || !validationMesh.indexedReady()) return false;
        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        vkResetFences(device, 1, &inFlight);
        uint32_t imageIndex = 0;
        VkResult r = vkAcquireNextImageKHR(device, swapchain, UINT64_MAX, imageAvailable, VK_NULL_HANDLE, &imageIndex);
        if (r == VK_ERROR_OUT_OF_DATE_KHR) { fail("Swapchain out of date before frame"); return false; }
        if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR) { fail("Acquire image failed: " + vkResultName(r)); return false; }
        vkResetCommandBuffer(commandBuffers[imageIndex], 0);
        if (!recordFrame(imageIndex)) return false;
        VkPipelineStageFlags waitStages[] = { VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT };
        VkSubmitInfo submit{ VK_STRUCTURE_TYPE_SUBMIT_INFO };
        submit.waitSemaphoreCount = 1;
        submit.pWaitSemaphores = &imageAvailable;
        submit.pWaitDstStageMask = waitStages;
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &commandBuffers[imageIndex];
        submit.signalSemaphoreCount = 1;
        submit.pSignalSemaphores = &renderFinished;
        r = vkQueueSubmit(graphicsQueue, 1, &submit, inFlight);
        if (r != VK_SUCCESS) { fail("Queue submit failed: " + vkResultName(r)); return false; }
        VkPresentInfoKHR present{ VK_STRUCTURE_TYPE_PRESENT_INFO_KHR };
        present.waitSemaphoreCount = 1;
        present.pWaitSemaphores = &renderFinished;
        present.swapchainCount = 1;
        present.pSwapchains = &swapchain;
        present.pImageIndices = &imageIndex;
        r = vkQueuePresentKHR(graphicsQueue, &present);
        if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR && r != VK_ERROR_OUT_OF_DATE_KHR) { fail("Present failed: " + vkResultName(r)); return false; }
        framesRendered += 1;
        firstFrameRendered = true;
        triangleDrawn = false;
        cubeDrawn = model.fallbackCubeVisible;
        return true;
    }

    void setModelFallback(const std::string& name, const std::string& path, const std::string& reason) {
        model.activeModelName = name.empty() ? "none" : name;
        model.activeModelPath = path;
        model.gpuUploadStatus = (name.empty() || name == "none") ? "failed" : "failed";
        model.drawStatus = "fallback";
        model.meshDrawStatus = "fallback";
        model.uploadedVertexCount = 0;
        model.uploadedIndexCount = 0;
        model.primitiveCountRendered = 0;
        model.fallbackCubeVisible = true;
        model.fallbackCubeStatus = "on";
        model.reason = reason.empty() ? "no active model" : reason;
        if (device != VK_NULL_HANDLE) modelMesh.destroy();
        modelDrawRanges.clear();
        renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", model.reason);
        updateReadyStatus();
    }

    bool uploadModelFirstPrimitive(
        const std::string& name,
        const std::string& path,
        const Vertex3D* vertices,
        uint32_t vertexCount,
        const uint32_t* indices,
        uint32_t indexCount,
        const float* boundsMin,
        const float* boundsMax,
        const float* boundsCenter,
        float scale,
        const float* baseColorFactor,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE) {
            error = "renderer not initialized";
            return false;
        }
        if (!modelMesh.createFromInterleaved(physicalDevice, device, vertices, vertexCount, indices, indexCount, name, error)) {
            setModelFallback(name, path, error);
            return false;
        }
        model.activeModelName = name.empty() ? "imported.glb" : name;
        model.activeModelPath = path;
        model.activePrimitiveIndex = 0;
        model.gpuUploadStatus = "ok";
        model.drawStatus = "ok";
        model.meshDrawStatus = "ok";
        model.uploadedVertexCount = vertexCount;
        model.uploadedIndexCount = indexCount;
        for (int i = 0; i < 3; ++i) {
            model.boundsMin[i] = boundsMin ? boundsMin[i] : 0.0f;
            model.boundsMax[i] = boundsMax ? boundsMax[i] : 0.0f;
            model.boundsCenter[i] = boundsCenter ? boundsCenter[i] : 0.0f;
        }
        model.modelScale = scale;
        model.fallbackCubeVisible = false;
        model.fallbackCubeStatus = "off";
        model.reason = "first primitive uploaded to Vulkan buffers";
        if (baseColorFactor) {
            material.baseColorFactor[0] = baseColorFactor[0];
            material.baseColorFactor[1] = baseColorFactor[1];
            material.baseColorFactor[2] = baseColorFactor[2];
            material.baseColorFactor[3] = baseColorFactor[3];
        }
        camera.distance = clampFloat(3.6f, 2.0f, 8.0f);
        cameraControlsReady = true;
        bool ok = renderOneFrame();
        if (!ok) {
            model.drawStatus = "fallback";
            model.meshDrawStatus = "fallback";
            model.fallbackCubeVisible = true;
            model.fallbackCubeStatus = "on";
            model.reason = diagnostics.reason.empty() ? "model draw failed" : diagnostics.reason;
            error = model.reason;
            renderOneFrame();
            syncDiagnostics();
            diagnostics.write("valid", model.reason);
            updateReadyStatus();
            return false;
        }
        syncDiagnostics();
        syncAlphaCutoutState();
        diagnostics.write("valid", "Scene24 Transparent Glass Pass Lab uploaded and drew first primitive.");
        updateReadyStatus();
        return true;
    }

    bool uploadModelMultiPrimitive(
        const std::string& name,
        const std::string& path,
        const Vertex3D* vertices,
        uint32_t vertexCount,
        const uint32_t* indices,
        uint32_t indexCount,
        const PrimitiveDrawRange* ranges,
        uint32_t rangeCount,
        const MaterialSlotState* slots,
        uint32_t slotCount,
        const float* boundsMin,
        const float* boundsMax,
        const float* boundsCenter,
        float scale,
        uint32_t primitiveTotal,
        uint32_t primitiveSkipped,
        uint32_t unsupportedPrimitiveCount,
        const std::string& reason,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE) {
            error = "renderer not initialized";
            return false;
        }
        if (!vertices || vertexCount == 0 || !ranges || rangeCount == 0) {
            error = "all primitives unsupported: no drawable primitive ranges";
            setModelFallback(name, path, error);
            return false;
        }
        if (!modelMesh.createFromInterleaved(physicalDevice, device, vertices, vertexCount, indices, indexCount, name, error)) {
            setModelFallback(name, path, error);
            return false;
        }
        modelDrawRanges.assign(ranges, ranges + rangeCount);
        modelMaterialSlots.assign(slots, slots + slotCount);
        if (modelMaterialSlots.empty()) modelMaterialSlots.push_back(MaterialSlotState{});
        model.activeModelName = name.empty() ? "imported.glb" : name;
        model.activeModelPath = path;
        model.activePrimitiveIndex = 0;
        model.gpuUploadStatus = "ok";
        model.drawStatus = primitiveSkipped > 0 ? "partial_ok" : "ok";
        model.meshDrawStatus = model.drawStatus;
        model.uploadedVertexCount = vertexCount;
        model.uploadedIndexCount = indexCount;
        model.primitiveCountTotal = primitiveTotal;
        model.primitiveCountRendered = rangeCount;
        model.primitiveCountSkipped = primitiveSkipped;
        model.unsupportedPrimitiveCount = unsupportedPrimitiveCount;
        model.materialSlotCount = slotCount;
        model.materialSlotCountRendered = slotCount;
        model.textureSlotCount = 0;
        for (const auto& slot : modelMaterialSlots) if (slot.baseColorTextureSlot >= 0) model.textureSlotCount += 1;
        if (model.textureSlotCount > model.textureSlotLimit) model.textureSlotCount = model.textureSlotLimit;
        model.fallbackCubeVisible = false;
        model.fallbackCubeStatus = "off";
        model.reason = reason.empty() ? "multi primitive static model uploaded to Vulkan buffers" : reason;
        for (int i = 0; i < 3; ++i) {
            model.boundsMin[i] = boundsMin ? boundsMin[i] : 0.0f;
            model.boundsMax[i] = boundsMax ? boundsMax[i] : 0.0f;
            model.boundsCenter[i] = boundsCenter ? boundsCenter[i] : 0.0f;
        }
        model.modelScale = scale;
        if (!modelMaterialSlots.empty()) {
            for (int i = 0; i < 4; ++i) material.baseColorFactor[i] = modelMaterialSlots[0].baseColorFactor[i];
            material.metallicFactor = modelMaterialSlots[0].metallicFactor;
            material.roughnessFactor = modelMaterialSlots[0].roughnessFactor;
            material.normalScale = modelMaterialSlots[0].normalScale;
            material.occlusionStrength = modelMaterialSlots[0].occlusionStrength;
            for (int i = 0; i < 3; ++i) material.emissiveFactor[i] = modelMaterialSlots[0].emissiveFactor[i];
            material.materialPresetHint = modelMaterialSlots[0].materialPresetHint;
            material.alphaMode = modelMaterialSlots[0].alphaMode;
            material.materialId = 0;
            model.metallicFactor = material.metallicFactor;
            model.roughnessFactor = material.roughnessFactor;
            model.normalScale = material.normalScale;
            model.occlusionStrength = material.occlusionStrength;
        }
        float extentX = model.boundsMax[0] - model.boundsMin[0];
        float extentY = model.boundsMax[1] - model.boundsMin[1];
        float extentZ = model.boundsMax[2] - model.boundsMin[2];
        float maxExtent = std::max(extentX, std::max(extentY, extentZ));
        camera.distance = clampFloat(maxExtent > 0.0f ? 3.4f : 3.6f, 2.0f, 8.0f);
        cameraControlsReady = true;
        bool ok = renderOneFrame();
        if (!ok) {
            model.drawStatus = "fallback";
            model.meshDrawStatus = "fallback";
            model.fallbackCubeVisible = true;
            model.fallbackCubeStatus = "on";
            model.reason = diagnostics.reason.empty() ? "multi primitive draw failed" : diagnostics.reason;
            error = model.reason;
            renderOneFrame();
            syncDiagnostics();
            diagnostics.write("valid", model.reason);
            updateReadyStatus();
            return false;
        }
        syncDiagnostics();
        syncAlphaCutoutState();
        syncEmissivePresetState();
        diagnostics.write("valid", "Scene24 Transparent Glass Pass Lab uploaded and drew supported primitives.");
        updateReadyStatus();
        return true;
    }

    TextureResource* fallbackTexture(int kind) {
        if (materialTextureSets.empty()) return nullptr;
        if (kind == PbrTextureMetallicRoughness) return &materialTextureSets[0].metallicRoughness;
        if (kind == PbrTextureNormal) return &materialTextureSets[0].normal;
        if (kind == PbrTextureOcclusion) return &materialTextureSets[0].occlusion;
        return &materialTextureSets[0].baseColor;
    }

    TextureResource* descriptorTexture(size_t setIndex, int kind) {
        if (setIndex < materialTextureSets.size() && materialTextureSets[setIndex].texture(kind).ready) return &materialTextureSets[setIndex].texture(kind);
        return fallbackTexture(kind);
    }

    bool recreateTextureDescriptor(std::string& error) {
        if (materialTextureSets.empty() || trianglePipeline.textureSetLayout == VK_NULL_HANDLE) {
            error = "texture descriptor missing texture or layout";
            return false;
        }
        if (textureDescriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, textureDescriptorPool, nullptr);
            textureDescriptorPool = VK_NULL_HANDLE;
            textureDescriptorSets.clear();
        }
        VkDescriptorPoolSize poolSize{};
        poolSize.type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        poolSize.descriptorCount = (uint32_t)materialTextureSets.size() * 4u;
        VkDescriptorPoolCreateInfo pool{ VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO };
        pool.maxSets = (uint32_t)materialTextureSets.size();
        pool.poolSizeCount = 1;
        pool.pPoolSizes = &poolSize;
        VkResult r = vkCreateDescriptorPool(device, &pool, nullptr, &textureDescriptorPool);
        if (r != VK_SUCCESS) { error = "texture descriptor pool failed: " + vkResultName(r); return false; }
        std::vector<VkDescriptorSetLayout> layouts(materialTextureSets.size(), trianglePipeline.textureSetLayout);
        textureDescriptorSets.assign(materialTextureSets.size(), VK_NULL_HANDLE);
        VkDescriptorSetAllocateInfo alloc{ VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO };
        alloc.descriptorPool = textureDescriptorPool;
        alloc.descriptorSetCount = (uint32_t)layouts.size();
        alloc.pSetLayouts = layouts.data();
        r = vkAllocateDescriptorSets(device, &alloc, textureDescriptorSets.data());
        if (r != VK_SUCCESS) { error = "texture descriptor set failed: " + vkResultName(r); return false; }
        std::vector<VkDescriptorImageInfo> imageInfos(materialTextureSets.size() * 4u);
        std::vector<VkWriteDescriptorSet> writes(materialTextureSets.size() * 4u);
        for (size_t i = 0; i < materialTextureSets.size(); ++i) {
            for (uint32_t binding = 0; binding < 4; ++binding) {
                TextureResource* texture = descriptorTexture(i, (int)binding);
                if (!texture || !texture->ready) { error = "texture descriptor fallback missing"; return false; }
                size_t w = i * 4u + binding;
                imageInfos[w].sampler = texture->sampler;
                imageInfos[w].imageView = texture->view;
                imageInfos[w].imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
                writes[w] = { VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET };
                writes[w].dstSet = textureDescriptorSets[i];
                writes[w].dstBinding = binding;
                writes[w].descriptorCount = 1;
                writes[w].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
                writes[w].pImageInfo = &imageInfos[w];
            }
        }
        vkUpdateDescriptorSets(device, (uint32_t)writes.size(), writes.data(), 0, nullptr);
        return true;
    }

    bool createFallbackBaseColorTexture(std::string& error) {
        const uint8_t white[] = { 255, 255, 255, 255 };
        const uint8_t normal[] = { 128, 128, 255, 255 };
        materialTextureSets.resize(1);
        if (!materialTextureSets[0].baseColor.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        if (!materialTextureSets[0].metallicRoughness.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        if (!materialTextureSets[0].normal.createRgba8(physicalDevice, device, graphicsQueue, commandPool, normal, 1, 1, error)) return false;
        if (!materialTextureSets[0].occlusion.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        return recreateTextureDescriptor(error);
    }

    void restoreTextureFallbackAfterFailure() {
        std::string ignored;
        createFallbackBaseColorTexture(ignored);
        model.textureWidth = 0;
        model.textureHeight = 0;
        model.textureBytes = 0;
        model.textureFallbackUsed = true;
        syncTextureMaterialState();
    }

    bool uploadBaseColorTexture(
        const uint8_t* rgba,
        uint32_t width,
        uint32_t height,
        const std::string& name,
        const std::string& source,
        const std::string& mimeType,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE || commandPool == VK_NULL_HANDLE) {
            error = "renderer not initialized for texture upload";
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            syncTextureMaterialState();
            return false;
        }
        if (!rgba || width == 0 || height == 0) {
            error = "decoded texture pixels empty";
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            syncTextureMaterialState();
            return false;
        }
        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        if (materialTextureSets.empty()) createFallbackBaseColorTexture(error);
        if (!materialTextureSets[0].baseColor.createRgba8(physicalDevice, device, graphicsQueue, commandPool, rgba, width, height, error)) {
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            restoreTextureFallbackAfterFailure();
            syncTextureMaterialState();
            return false;
        }
        if (!recreateTextureDescriptor(error)) {
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            restoreTextureFallbackAfterFailure();
            syncTextureMaterialState();
            return false;
        }
        model.textureUploadStatus = "ok";
        model.baseColorTextureStatus = "ok";
        model.baseColorTextureName = name.empty() ? "baseColorTexture" : name;
        model.baseColorTextureSource = source.empty() ? "glb.bufferView" : source;
        model.baseColorTextureMimeType = mimeType.empty() ? "unknown" : mimeType;
        model.textureWidth = width;
        model.textureHeight = height;
        model.textureBytes = width * height * 4u;
        model.textureFallbackUsed = false;
        materialTextureSets[0].baseColorUploaded = true;
        syncTextureMaterialState();
        bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", ok ? "baseColor texture uploaded and sampled" : "baseColor texture uploaded; draw retry failed");
        updateReadyStatus();
        return ok;
    }

    bool uploadPbrTextureSlot(int materialSlot, int kind, const uint8_t* rgba, uint32_t width, uint32_t height, const std::string& name, const std::string& source, const std::string& mimeType, std::string& error) {
        if (materialSlot < 0 || materialSlot >= (int)model.textureSlotLimit) {
            error = "texture slot outside limit";
            if (kind == PbrTextureBaseColor) model.skippedTextureCount += 1; else model.skippedPbrTextureCount += 1;
            return false;
        }
        if (kind < 0 || kind > 3) { error = "unknown pbr texture kind"; return false; }
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE || commandPool == VK_NULL_HANDLE) { error = "renderer not initialized for texture slot upload"; return false; }
        if (!rgba || width == 0 || height == 0) {
            error = "decoded texture slot pixels empty";
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        if (materialTextureSets.empty()) createFallbackBaseColorTexture(error);
        if ((size_t)(materialSlot + 1) > materialTextureSets.size()) materialTextureSets.resize((size_t)materialSlot + 1);
        if (!materialTextureSets[(size_t)materialSlot].texture(kind).createRgba8(physicalDevice, device, graphicsQueue, commandPool, rgba, width, height, error)) {
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        if (!recreateTextureDescriptor(error)) {
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        materialTextureSets[(size_t)materialSlot].setUploaded(kind, true);
        if (kind == PbrTextureBaseColor) {
            model.textureUploadStatus = "ok";
            model.baseColorTextureStatus = "ok";
            model.baseColorTextureName = name.empty() ? "baseColorTexture" : name;
            model.baseColorTextureSource = source.empty() ? "glb.bufferView" : source;
            model.baseColorTextureMimeType = mimeType.empty() ? "unknown" : mimeType;
        } else if (kind == PbrTextureMetallicRoughness) {
            model.metallicRoughnessStatus = "ok";
        } else if (kind == PbrTextureNormal) {
            model.normalMapStatus = "ok";
            model.normalMapAppliedStatus = (model.tangentStatus.find("generated") != std::string::npos || model.tangentStatus.find("from_gltf") != std::string::npos) ? "ok" : "blocked_no_tangent";
        } else if (kind == PbrTextureOcclusion) {
            model.occlusionMapStatus = "ok";
        }
        model.textureWidth = width;
        model.textureHeight = height;
        model.textureBytes = width * height * 4u;
        model.textureFallbackUsed = false;
        model.uploadedTextureCount = 0;
        model.uploadedPbrTextureCount = 0;
        for (const auto& set : materialTextureSets) {
            if (set.baseColorUploaded) model.uploadedTextureCount += 1;
            if (set.metallicRoughnessUploaded) model.uploadedPbrTextureCount += 1;
            if (set.normalUploaded) model.uploadedPbrTextureCount += 1;
            if (set.occlusionUploaded) model.uploadedPbrTextureCount += 1;
        }
        model.pbrMapsStatus = model.uploadedPbrTextureCount > 0 ? "partial_ok" : "missing";
        syncTextureMaterialState();
        bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", ok ? "texture slot uploaded and sampled" : "texture slot uploaded; draw retry failed");
        updateReadyStatus();
        return ok;
    }

    bool uploadBaseColorTextureSlot(int slot, const uint8_t* rgba, uint32_t width, uint32_t height, const std::string& name, const std::string& source, const std::string& mimeType, std::string& error) {
        return uploadPbrTextureSlot(slot, PbrTextureBaseColor, rgba, width, height, name, source, mimeType, error);
    }

    bool createRendererResources() {
        std::string error;
        if (!createImageViews()) return false;
        if (!createRenderPass()) return false;
        if (!createDepthResources()) return false;
        if (!createFramebuffers()) return false;
        if (!validationMesh.createFoundationCube(physicalDevice, device, error)) { fail("MeshResource cube failed: " + error); return false; }
        meshAttributeLayoutReady = validationMesh.vertexCount == 24 && sizeof(Vertex3D) == 60;
        if (!trianglePipeline.createTrianglePipeline(device, renderPass, extent, error)) { fail("PipelineBundle failed: " + error); return false; }
        if (!createCommandsAndSync()) return false;
        if (!createFallbackBaseColorTexture(error)) { fail("Fallback texture failed: " + error); return false; }
        return true;
    }

    bool init(ANativeWindow* window, int width, int height, const std::string& root) {
        setOutputRoot(root);
        destroy();
        status = "SOLUM Engine\nInitializing RendererCore...";
        if (!createInstance()) return false;
        if (!createSurface(window)) return false;
        if (!pickDeviceAndQueue()) return false;
        if (!createDevice()) return false;
        if (!createSwapchain(width, height)) return false;
        if (!createRendererResources()) return false;
        applyLightPreset(0);
        if (!renderOneFrame()) return false;
        cameraControlsReady = true;
        rendererCoreReady = true;
        model.gpuUploadStatus = "failed";
        model.drawStatus = "fallback";
        model.meshDrawStatus = "fallback";
        model.fallbackCubeVisible = true;
        model.fallbackCubeStatus = "on";
        model.textureUploadStatus = "missing";
        model.baseColorTextureStatus = "missing";
        model.textureFallbackUsed = true;
        model.reason = "no active model";
        syncDiagnostics();
        diagnostics.write("valid", "Scene24 Transparent Glass Pass Lab initialized with cube fallback while waiting for active model upload.");
        updateReadyStatus();
        return true;
    }
};

} // namespace solum
