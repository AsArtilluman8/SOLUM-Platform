#pragma once
#include "renderer_types.hpp"

namespace solum {

enum class RenderLabScene {
    Scene01FoundationCube,
    Scene02ModelImportLab,
    Scene03GlbMeshRenderLab,
    Scene04TextureBindingLab,
    Scene04LightShadowLab,
    Scene05MultiPrimitiveRenderLab,
    Scene06PbrMaterialMapsLab,
    Scene07LightingFoundationLab,
    Scene08TangentNormalExposureLab,
    Scene09BrdfMaterialResponseLab,
    Scene10LightingControlLab,
    Scene11EnvironmentReflectionLab,
    Scene12GroundingInspectorLab,
    Scene13MaterialCalibrationLab,
    Scene14SpecularGlossLab,
    Scene06PerformanceLab
};

struct RenderLabState {
    RenderLabScene currentScene = RenderLabScene::Scene14SpecularGlossLab;
    bool cubeReady = false;
    bool depthReady = false;
    bool cameraReady = false;
    bool cameraMvpReady = false;
    bool cameraControlsReady = false;
    bool materialConstantsReady = false;
    bool meshAttributeLayoutReady = false;
    bool indexBufferReady = false;
    bool uniformOrPushConstantsReady = false;
    bool triangleFallbackAvailable = true;
    bool triangleFallbackEnabled = false;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    uint32_t vertexStrideBytes = 0;
    float cameraYawDeg = 0.0f;
    float cameraPitchDeg = 0.0f;
    float cameraDistance = 0.0f;
    const char* vertexLayout = "POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT";
    MaterialConstants material;
    uint64_t framesRendered = 0;
    ModelRenderState model;

    const char* sceneId() const {
        switch (currentScene) {
            case RenderLabScene::Scene01FoundationCube: return "scene01_foundation_cube";
            case RenderLabScene::Scene02ModelImportLab: return "scene02_model_import_lab";
            case RenderLabScene::Scene03GlbMeshRenderLab: return "scene03_glb_mesh_render_lab";
            case RenderLabScene::Scene04TextureBindingLab: return "scene04_texture_binding_lab";
            case RenderLabScene::Scene04LightShadowLab: return "scene04_light_shadow_lab";
            case RenderLabScene::Scene05MultiPrimitiveRenderLab: return "scene05_multi_primitive_render_lab";
            case RenderLabScene::Scene06PbrMaterialMapsLab: return "scene06_pbr_material_maps_lab";
            case RenderLabScene::Scene07LightingFoundationLab: return "scene07_lighting_foundation_lab";
            case RenderLabScene::Scene08TangentNormalExposureLab: return "scene08_tangent_normal_exposure_lab";
            case RenderLabScene::Scene09BrdfMaterialResponseLab: return "scene09_brdf_material_response_lab";
            case RenderLabScene::Scene10LightingControlLab: return "scene10_lighting_control_lab";
            case RenderLabScene::Scene11EnvironmentReflectionLab: return "scene11_environment_reflection_lab";
            case RenderLabScene::Scene12GroundingInspectorLab: return "scene12_grounding_inspector_lab";
            case RenderLabScene::Scene13MaterialCalibrationLab: return "scene13_material_calibration_lab";
            case RenderLabScene::Scene14SpecularGlossLab: return "scene14_specular_gloss_lab";
            case RenderLabScene::Scene06PerformanceLab: return "scene06_performance_lab";
            default: return "unknown";
        }
    }

    const char* sceneName() const {
        switch (currentScene) {
            case RenderLabScene::Scene01FoundationCube: return "Scene01 Foundation Cube";
            case RenderLabScene::Scene02ModelImportLab: return "Scene02 Model Import Lab";
            case RenderLabScene::Scene03GlbMeshRenderLab: return "Scene03 GLB Mesh Render Lab";
            case RenderLabScene::Scene04TextureBindingLab: return "Scene04 Texture Binding Lab";
            case RenderLabScene::Scene04LightShadowLab: return "Scene04 Light/Shadow Lab";
            case RenderLabScene::Scene05MultiPrimitiveRenderLab: return "Scene05 Multi Primitive Render Lab";
            case RenderLabScene::Scene06PbrMaterialMapsLab: return "Scene06 PBR Material Maps Lab";
            case RenderLabScene::Scene07LightingFoundationLab: return "Scene07 Lighting Foundation Lab";
            case RenderLabScene::Scene08TangentNormalExposureLab: return "Scene08 Tangent Normal Exposure Lab";
            case RenderLabScene::Scene09BrdfMaterialResponseLab: return "Scene09 BRDF Material Response Lab";
            case RenderLabScene::Scene10LightingControlLab: return "Scene10 Lighting Control Lab";
            case RenderLabScene::Scene11EnvironmentReflectionLab: return "Scene11 Environment Reflection Lab";
            case RenderLabScene::Scene12GroundingInspectorLab: return "Scene12 Grounding Inspector Lab";
            case RenderLabScene::Scene13MaterialCalibrationLab: return "Scene13 Material Calibration Lab";
            case RenderLabScene::Scene14SpecularGlossLab: return "Scene14 Specular Gloss Lab";
            case RenderLabScene::Scene06PerformanceLab: return "Scene06 Performance Lab";
            default: return "Unknown";
        }
    }

    void writeJsonFields(std::ofstream& f, const char* indent = "  ") const {
        f << indent << "\"renderLab\": {\n";
        f << indent << "  \"schema\": \"solum.render_lab_state\",\n";
        f << indent << "  \"schemaVersion\": 1,\n";
        f << indent << "  \"currentScene\": \"" << sceneId() << "\",\n";
        f << indent << "  \"currentLabScene\": \"" << sceneId() << "\",\n";
        f << indent << "  \"currentLabSceneName\": \"" << sceneName() << "\",\n";
        f << indent << "  \"renderingStatus\": \"" << (model.modelReady() ? "multi_primitive_static" : "fallback_cube") << "\",\n";
        f << indent << "  \"assetImportStatus\": \"" << (model.activeModelName == "none" ? "no active model" : "active model") << "\",\n";
        f << indent << "  \"activeModelName\": \"" << escapeJson(model.activeModelName) << "\",\n";
        f << indent << "  \"activeModelPath\": \"" << escapeJson(model.activeModelPath) << "\",\n";
        f << indent << "  \"activePrimitiveIndex\": " << model.activePrimitiveIndex << ",\n";
        f << indent << "  \"gpuUploadStatus\": \"" << escapeJson(model.gpuUploadStatus) << "\",\n";
        f << indent << "  \"drawStatus\": \"" << escapeJson(model.drawStatus) << "\",\n";
        f << indent << "  \"meshDrawStatus\": \"" << escapeJson(model.meshDrawStatus) << "\",\n";
        f << indent << "  \"textureUploadStatus\": \"" << escapeJson(model.textureUploadStatus) << "\",\n";
        f << indent << "  \"baseColorTextureStatus\": \"" << escapeJson(model.baseColorTextureStatus) << "\",\n";
        f << indent << "  \"baseColorTextureName\": \"" << escapeJson(model.baseColorTextureName) << "\",\n";
        f << indent << "  \"baseColorTextureSource\": \"" << escapeJson(model.baseColorTextureSource) << "\",\n";
        f << indent << "  \"baseColorTextureMimeType\": \"" << escapeJson(model.baseColorTextureMimeType) << "\",\n";
        f << indent << "  \"textureWidth\": " << model.textureWidth << ",\n";
        f << indent << "  \"textureHeight\": " << model.textureHeight << ",\n";
        f << indent << "  \"textureBytes\": " << model.textureBytes << ",\n";
        f << indent << "  \"textureFallbackUsed\": " << (model.textureFallbackUsed ? "true" : "false") << ",\n";
        f << indent << "  \"uploadedVertexCount\": " << model.uploadedVertexCount << ",\n";
        f << indent << "  \"uploadedIndexCount\": " << model.uploadedIndexCount << ",\n";
        f << indent << "  \"modelVertexLayout\": \"" << model.modelVertexLayout << "\",\n";
        f << indent << "  \"modelBoundsMin\": [" << model.boundsMin[0] << ", " << model.boundsMin[1] << ", " << model.boundsMin[2] << "],\n";
        f << indent << "  \"modelBoundsMax\": [" << model.boundsMax[0] << ", " << model.boundsMax[1] << ", " << model.boundsMax[2] << "],\n";
        f << indent << "  \"modelBoundsCenter\": [" << model.boundsCenter[0] << ", " << model.boundsCenter[1] << ", " << model.boundsCenter[2] << "],\n";
        f << indent << "  \"modelScale\": " << model.modelScale << ",\n";
        f << indent << "  \"modelRenderMode\": \"" << model.modelRenderMode << "\",\n";
        f << indent << "  \"primitiveCountTotal\": " << model.primitiveCountTotal << ",\n";
        f << indent << "  \"primitiveCountRendered\": " << model.primitiveCountRendered << ",\n";
        f << indent << "  \"primitiveCountSkipped\": " << model.primitiveCountSkipped << ",\n";
        f << indent << "  \"unsupportedPrimitiveCount\": " << model.unsupportedPrimitiveCount << ",\n";
        f << indent << "  \"materialSlotCount\": " << model.materialSlotCount << ",\n";
        f << indent << "  \"materialSlotCountRendered\": " << model.materialSlotCountRendered << ",\n";
        f << indent << "  \"textureSlotCount\": " << model.textureSlotCount << ",\n";
        f << indent << "  \"uploadedTextureCount\": " << model.uploadedTextureCount << ",\n";
        f << indent << "  \"textureFallbackCount\": " << model.textureFallbackCount << ",\n";
        f << indent << "  \"skippedTextureCount\": " << model.skippedTextureCount << ",\n";
        f << indent << "  \"textureSlotLimit\": " << model.textureSlotLimit << ",\n";
        f << indent << "  \"pbrMapsStatus\": \"" << escapeJson(model.pbrMapsStatus) << "\",\n";
        f << indent << "  \"metallicRoughnessStatus\": \"" << escapeJson(model.metallicRoughnessStatus) << "\",\n";
        f << indent << "  \"normalMapStatus\": \"" << escapeJson(model.normalMapStatus) << "\",\n";
        f << indent << "  \"normalMapAppliedStatus\": \"" << escapeJson(model.normalMapAppliedStatus) << "\",\n";
        f << indent << "  \"occlusionMapStatus\": \"" << escapeJson(model.occlusionMapStatus) << "\",\n";
        f << indent << "  \"tangentStatus\": \"" << escapeJson(model.tangentStatus) << "\",\n";
        f << indent << "  \"tangentSource\": \"" << escapeJson(model.tangentSource) << "\",\n";
        f << indent << "  \"tangentGeneratedCount\": " << model.tangentGeneratedCount << ",\n";
        f << indent << "  \"tangentFallbackGeneratedCount\": " << model.tangentFallbackGeneratedCount << ",\n";
        f << indent << "  \"tangentMissingCount\": " << model.tangentMissingCount << ",\n";
        f << indent << "  \"tangentDegenerateTriangleCount\": " << model.tangentDegenerateTriangleCount << ",\n";
        f << indent << "  \"tangentFallbackReason\": \"" << escapeJson(model.tangentFallbackReason) << "\",\n";
        f << indent << "  \"tangentBuildMode\": \"" << escapeJson(model.tangentBuildMode) << "\",\n";
        f << indent << "  \"metallicFactor\": " << model.metallicFactor << ",\n";
        f << indent << "  \"roughnessFactor\": " << model.roughnessFactor << ",\n";
        f << indent << "  \"normalScale\": " << model.normalScale << ",\n";
        f << indent << "  \"occlusionStrength\": " << model.occlusionStrength << ",\n";
        f << indent << "  \"pbrTextureSlotCount\": " << model.pbrTextureSlotCount << ",\n";
        f << indent << "  \"uploadedPbrTextureCount\": " << model.uploadedPbrTextureCount << ",\n";
        f << indent << "  \"skippedPbrTextureCount\": " << model.skippedPbrTextureCount << ",\n";
        f << indent << "  \"pbrTextureFallbackCount\": " << model.pbrTextureFallbackCount << ",\n";
        f << indent << "  \"materialSlotDiagnostics\": " << (model.materialSlotDiagnostics.empty() ? "[]" : model.materialSlotDiagnostics) << ",\n";
        f << indent << "  \"lightingStatus\": \"" << escapeJson(model.lightingStatus) << "\",\n";
        f << indent << "  \"lightingControlStatus\": \"" << escapeJson(model.lightingControlStatus) << "\",\n";
        f << indent << "  \"lightingUiMode\": \"" << escapeJson(model.lightingUiMode) << "\",\n";
        f << indent << "  \"sunDirection\": [" << model.sunDirection[0] << ", " << model.sunDirection[1] << ", " << model.sunDirection[2] << "],\n";
        f << indent << "  \"sunColor\": [" << model.sunColor[0] << ", " << model.sunColor[1] << ", " << model.sunColor[2] << "],\n";
        f << indent << "  \"sunIntensity\": " << model.sunIntensity << ",\n";
        f << indent << "  \"ambientColor\": [" << model.ambientColor[0] << ", " << model.ambientColor[1] << ", " << model.ambientColor[2] << "],\n";
        f << indent << "  \"ambientIntensity\": " << model.ambientIntensity << ",\n";
        f << indent << "  \"lightPreset\": \"" << escapeJson(model.lightPreset) << "\",\n";
        f << indent << "  \"specularBoost\": " << model.specularBoost << ",\n";
        f << indent << "  \"specularBoostStatus\": \"" << escapeJson(model.specularBoostStatus) << "\",\n";
        f << indent << "  \"reflectionIntensity\": " << model.reflectionIntensity << ",\n";
        f << indent << "  \"iblStatus\": \"" << escapeJson(model.iblStatus) << "\",\n";
        f << indent << "  \"iblMode\": \"" << escapeJson(model.iblMode) << "\",\n";
        f << indent << "  \"reflectionFoundationStatus\": \"" << escapeJson(model.reflectionFoundationStatus) << "\",\n";
        f << indent << "  \"reflectionMode\": \"" << escapeJson(model.reflectionMode) << "\",\n";
        f << indent << "  \"environmentReflectionStatus\": \"" << escapeJson(model.environmentReflectionStatus) << "\",\n";
        f << indent << "  \"environmentReflectionMode\": \"" << escapeJson(model.environmentReflectionMode) << "\",\n";
        f << indent << "  \"environmentSource\": \"" << escapeJson(model.environmentSource) << "\",\n";
        f << indent << "  \"reflectionColorStatus\": \"" << escapeJson(model.reflectionColorStatus) << "\",\n";
        f << indent << "  \"reflectionRoughnessResponseStatus\": \"" << escapeJson(model.reflectionRoughnessResponseStatus) << "\",\n";
        f << indent << "  \"metallicReflectionStatus\": \"" << escapeJson(model.metallicReflectionStatus) << "\",\n";
        f << indent << "  \"dielectricReflectionStatus\": \"" << escapeJson(model.dielectricReflectionStatus) << "\",\n";
        f << indent << "  \"reflectionPerformanceStatus\": \"" << escapeJson(model.reflectionPerformanceStatus) << "\",\n";
        f << indent << "  \"inspectorUiStatus\": \"" << escapeJson(model.inspectorUiStatus) << "\",\n";
        f << indent << "  \"inspectorUiMode\": \"" << escapeJson(model.inspectorUiMode) << "\",\n";
        f << indent << "  \"activeInspectorTab\": \"" << escapeJson(model.activeInspectorTab) << "\",\n";
        f << indent << "  \"assetsTabStatus\": \"" << escapeJson(model.assetsTabStatus) << "\",\n";
        f << indent << "  \"cameraTabStatus\": \"" << escapeJson(model.cameraTabStatus) << "\",\n";
        f << indent << "  \"lightingTabStatus\": \"" << escapeJson(model.lightingTabStatus) << "\",\n";
        f << indent << "  \"materialTabStatus\": \"" << escapeJson(model.materialTabStatus) << "\",\n";
        f << indent << "  \"debugTabStatus\": \"" << escapeJson(model.debugTabStatus) << "\",\n";
        f << indent << "  \"contactGroundingStatus\": \"" << escapeJson(model.contactGroundingStatus) << "\",\n";
        f << indent << "  \"contactShadowStatus\": \"" << escapeJson(model.contactShadowStatus) << "\",\n";
        f << indent << "  \"contactShadowMode\": \"" << escapeJson(model.contactShadowMode) << "\",\n";
        f << indent << "  \"contactShadowIntensity\": " << model.contactShadowIntensity << ",\n";
        f << indent << "  \"contactShadowPerformanceStatus\": \"" << escapeJson(model.contactShadowPerformanceStatus) << "\",\n";
        f << indent << "  \"groundingUsesModelBounds\": \"" << escapeJson(model.groundingUsesModelBounds) << "\",\n";
        f << indent << "  \"groundingUniformUpdateStatus\": \"" << escapeJson(model.groundingUniformUpdateStatus) << "\",\n";
        f << indent << "  \"groundSliderStatus\": \"" << escapeJson(model.groundSliderStatus) << "\",\n";
        f << indent << "  \"contactGroundingSliderStatus\": \"" << escapeJson(model.contactGroundingSliderStatus) << "\",\n";
        f << indent << "  \"lightingUniformUpdateStatus\": \"" << escapeJson(model.lightingUniformUpdateStatus) << "\",\n";
        f << indent << "  \"sliderUpdateMode\": \"" << escapeJson(model.sliderUpdateMode) << "\",\n";
        f << indent << "  \"sliderTouchStatus\": \"" << escapeJson(model.sliderTouchStatus) << "\",\n";
        f << indent << "  \"sunSliderStatus\": \"" << escapeJson(model.sunSliderStatus) << "\",\n";
        f << indent << "  \"ambientSliderStatus\": \"" << escapeJson(model.ambientSliderStatus) << "\",\n";
        f << indent << "  \"exposureSliderStatus\": \"" << escapeJson(model.exposureSliderStatus) << "\",\n";
        f << indent << "  \"specularSliderStatus\": \"" << escapeJson(model.specularSliderStatus) << "\",\n";
        f << indent << "  \"reflectionSliderStatus\": \"" << escapeJson(model.reflectionSliderStatus) << "\",\n";
        f << indent << "  \"brdfStatus\": \"" << escapeJson(model.brdfStatus) << "\",\n";
        f << indent << "  \"brdfMode\": \"" << escapeJson(model.brdfMode) << "\",\n";
        f << indent << "  \"diffuseStatus\": \"" << escapeJson(model.diffuseStatus) << "\",\n";
        f << indent << "  \"specularStatus\": \"" << escapeJson(model.specularStatus) << "\",\n";
        f << indent << "  \"fresnelStatus\": \"" << escapeJson(model.fresnelStatus) << "\",\n";
        f << indent << "  \"f0Status\": \"" << escapeJson(model.f0Status) << "\",\n";
        f << indent << "  \"metallicResponseStatus\": \"" << escapeJson(model.metallicResponseStatus) << "\",\n";
        f << indent << "  \"roughnessResponseStatus\": \"" << escapeJson(model.roughnessResponseStatus) << "\",\n";
        f << indent << "  \"directLightingStatus\": \"" << escapeJson(model.directLightingStatus) << "\",\n";
        f << indent << "  \"materialResponseStatus\": \"" << escapeJson(model.materialResponseStatus) << "\",\n";
        f << indent << "  \"pbrQualityTier\": \"" << escapeJson(model.pbrQualityTier) << "\",\n";
        f << indent << "  \"brdfPerformanceStatus\": \"" << escapeJson(model.brdfPerformanceStatus) << "\",\n";
        f << indent << "  \"toneMappingStatus\": \"" << escapeJson(model.toneMappingStatus) << "\",\n";
        f << indent << "  \"toneMappingMode\": \"" << escapeJson(model.toneMappingMode) << "\",\n";
        f << indent << "  \"exposureStatus\": \"" << escapeJson(model.exposureStatus) << "\",\n";
        f << indent << "  \"exposureValue\": " << model.exposureValue << ",\n";
        f << indent << "  \"ambientFloor\": " << model.ambientFloor << ",\n";
        f << indent << "  \"brightnessPreset\": \"" << escapeJson(model.brightnessPreset) << "\",\n";
        f << indent << "  \"activeDebugView\": \"" << escapeJson(model.activeDebugView) << "\",\n";
        f << indent << "  \"debugViewStatus\": \"" << escapeJson(model.debugViewStatus) << "\",\n";
        f << indent << "  \"normalDebugViewStatus\": \"" << escapeJson(model.normalDebugViewStatus) << "\",\n";
        f << indent << "  \"ndotlDebugViewStatus\": \"" << escapeJson(model.ndotlDebugViewStatus) << "\",\n";
        f << indent << "  \"diffuseDebugViewStatus\": \"" << escapeJson(model.diffuseDebugViewStatus) << "\",\n";
        f << indent << "  \"specularDebugViewStatus\": \"" << escapeJson(model.specularDebugViewStatus) << "\",\n";
        f << indent << "  \"f0DebugViewStatus\": \"" << escapeJson(model.f0DebugViewStatus) << "\",\n";
        f << indent << "  \"reflectionDebugViewStatus\": \"" << escapeJson(model.reflectionDebugViewStatus) << "\",\n";
        f << indent << "  \"iblDiffuseDebugViewStatus\": \"" << escapeJson(model.iblDiffuseDebugViewStatus) << "\",\n";
        f << indent << "  \"iblSpecularDebugViewStatus\": \"" << escapeJson(model.iblSpecularDebugViewStatus) << "\",\n";
        f << indent << "  \"brdfStatusDebugViewStatus\": \"" << escapeJson(model.brdfStatusDebugViewStatus) << "\",\n";
        f << indent << "  \"groundingDebugViewStatus\": \"" << escapeJson(model.groundingDebugViewStatus) << "\",\n";
        f << indent << "  \"materialCalibrationStatus\": \"" << escapeJson(model.materialCalibrationStatus) << "\",\n";
        f << indent << "  \"materialCalibrationMode\": \"" << escapeJson(model.materialCalibrationMode) << "\",\n";
        f << indent << "  \"albedoEnergyStatus\": \"" << escapeJson(model.albedoEnergyStatus) << "\",\n";
        f << indent << "  \"albedoClampStatus\": \"" << escapeJson(model.albedoClampStatus) << "\",\n";
        f << indent << "  \"diffuseClampStatus\": \"" << escapeJson(model.diffuseClampStatus) << "\",\n";
        f << indent << "  \"luminanceGuardStatus\": \"" << escapeJson(model.luminanceGuardStatus) << "\",\n";
        f << indent << "  \"aoCalibrationStatus\": \"" << escapeJson(model.aoCalibrationStatus) << "\",\n";
        f << indent << "  \"roughnessRemapStatus\": \"" << escapeJson(model.roughnessRemapStatus) << "\",\n";
        f << indent << "  \"metallicRoughnessClampStatus\": \"" << escapeJson(model.metallicRoughnessClampStatus) << "\",\n";
        f << indent << "  \"emissiveGuardStatus\": \"" << escapeJson(model.emissiveGuardStatus) << "\",\n";
        f << indent << "  \"fabricMattePreserveStatus\": \"" << escapeJson(model.fabricMattePreserveStatus) << "\",\n";
        f << indent << "  \"paintMaterialCalibrationStatus\": \"" << escapeJson(model.paintMaterialCalibrationStatus) << "\",\n";
        f << indent << "  \"metalMaterialCalibrationStatus\": \"" << escapeJson(model.metalMaterialCalibrationStatus) << "\",\n";
        f << indent << "  \"materialTypeHintStatus\": \"" << escapeJson(model.materialTypeHintStatus) << "\",\n";
        f << indent << "  \"materialSlotCalibrationStatus\": \"" << escapeJson(model.materialSlotCalibrationStatus) << "\",\n";
        f << indent << "  \"calibrationUiStatus\": \"" << escapeJson(model.calibrationUiStatus) << "\",\n";
        f << indent << "  \"calibrationPreset\": \"" << escapeJson(model.calibrationPreset) << "\",\n";
        f << indent << "  \"calibrationSliderStatus\": \"" << escapeJson(model.calibrationSliderStatus) << "\",\n";
        f << indent << "  \"calibrationSliderValue\": " << model.calibrationSliderValue << ",\n";
        f << indent << "  \"calibrationUniformUpdateStatus\": \"" << escapeJson(model.calibrationUniformUpdateStatus) << "\",\n";
        f << indent << "  \"calibratedAlbedoDebugViewStatus\": \"" << escapeJson(model.calibratedAlbedoDebugViewStatus) << "\",\n";
        f << indent << "  \"materialTypeDebugViewStatus\": \"" << escapeJson(model.materialTypeDebugViewStatus) << "\",\n";
        f << indent << "  \"aoInfluenceDebugViewStatus\": \"" << escapeJson(model.aoInfluenceDebugViewStatus) << "\",\n";
        f << indent << "  \"luminanceGuardDebugViewStatus\": \"" << escapeJson(model.luminanceGuardDebugViewStatus) << "\",\n";
        f << indent << "  \"calibrationVisualStrength\": " << model.calibrationVisualStrength << ",\n";
        f << indent << "  \"calibrationAffectsAlbedo\": \"" << escapeJson(model.calibrationAffectsAlbedo) << "\",\n";
        f << indent << "  \"calibrationAffectsAo\": \"" << escapeJson(model.calibrationAffectsAo) << "\",\n";
        f << indent << "  \"calibrationAffectsRoughness\": \"" << escapeJson(model.calibrationAffectsRoughness) << "\",\n";
        f << indent << "  \"calibrationVisibleResponseStatus\": \"" << escapeJson(model.calibrationVisibleResponseStatus) << "\",\n";
        f << indent << "  \"materialCalibrationPerformanceStatus\": \"" << escapeJson(model.materialCalibrationPerformanceStatus) << "\",\n";
        f << indent << "  \"specularGlossStatus\": \"" << escapeJson(model.specularGlossStatus) << "\",\n";
        f << indent << "  \"specularGlossMode\": \"" << escapeJson(model.specularGlossMode) << "\",\n";
        f << indent << "  \"specularResponseStatus\": \"" << escapeJson(model.specularResponseStatus) << "\",\n";
        f << indent << "  \"glossResponseStatus\": \"" << escapeJson(model.glossResponseStatus) << "\",\n";
        f << indent << "  \"roughnessRemapV2Status\": \"" << escapeJson(model.roughnessRemapV2Status) << "\",\n";
        f << indent << "  \"metallicSpecularBoostStatus\": \"" << escapeJson(model.metallicSpecularBoostStatus) << "\",\n";
        f << indent << "  \"dielectricGlossStatus\": \"" << escapeJson(model.dielectricGlossStatus) << "\",\n";
        f << indent << "  \"fabricSpecularSuppressStatus\": \"" << escapeJson(model.fabricSpecularSuppressStatus) << "\",\n";
        f << indent << "  \"specularOverbrightGuardStatus\": \"" << escapeJson(model.specularOverbrightGuardStatus) << "\",\n";
        f << indent << "  \"viewDependentHighlightStatus\": \"" << escapeJson(model.viewDependentHighlightStatus) << "\",\n";
        f << indent << "  \"paintGlossLiteStatus\": \"" << escapeJson(model.paintGlossLiteStatus) << "\",\n";
        f << indent << "  \"paintGlossLiteMode\": \"" << escapeJson(model.paintGlossLiteMode) << "\",\n";
        f << indent << "  \"paintGlossIntensity\": " << model.paintGlossIntensity << ",\n";
        f << indent << "  \"paintGlossRoughness\": " << model.paintGlossRoughness << ",\n";
        f << indent << "  \"paintGlossMaterialHintStatus\": \"" << escapeJson(model.paintGlossMaterialHintStatus) << "\",\n";
        f << indent << "  \"paintGlossPerformanceStatus\": \"" << escapeJson(model.paintGlossPerformanceStatus) << "\",\n";
        f << indent << "  \"paintGlossTargetStatus\": \"" << escapeJson(model.paintGlossTargetStatus) << "\",\n";
        f << indent << "  \"paintGlossAppliedMaterialCount\": " << model.paintGlossAppliedMaterialCount << ",\n";
        f << indent << "  \"paintGlossSkippedFabricCount\": " << model.paintGlossSkippedFabricCount << ",\n";
        f << indent << "  \"paintGlossFallbackRouting\": \"" << escapeJson(model.paintGlossFallbackRouting) << "\",\n";
        f << indent << "  \"paintGlossVisibleResponseStatus\": \"" << escapeJson(model.paintGlossVisibleResponseStatus) << "\",\n";
        f << indent << "  \"glossSliderStatus\": \"" << escapeJson(model.glossSliderStatus) << "\",\n";
        f << indent << "  \"glossSliderValue\": " << model.glossSliderValue << ",\n";
        f << indent << "  \"paintGlossSliderStatus\": \"" << escapeJson(model.paintGlossSliderStatus) << "\",\n";
        f << indent << "  \"paintGlossSliderValue\": " << model.paintGlossSliderValue << ",\n";
        f << indent << "  \"glossUniformUpdateStatus\": \"" << escapeJson(model.glossUniformUpdateStatus) << "\",\n";
        f << indent << "  \"glossResponseDebugViewStatus\": \"" << escapeJson(model.glossResponseDebugViewStatus) << "\",\n";
        f << indent << "  \"specularGuardDebugViewStatus\": \"" << escapeJson(model.specularGuardDebugViewStatus) << "\",\n";
        f << indent << "  \"paintGlossDebugViewStatus\": \"" << escapeJson(model.paintGlossDebugViewStatus) << "\",\n";
        f << indent << "  \"metalResponseDebugViewStatus\": \"" << escapeJson(model.metalResponseDebugViewStatus) << "\",\n";
        f << indent << "  \"paintTargetDebugViewStatus\": \"" << escapeJson(model.paintTargetDebugViewStatus) << "\",\n";
        f << indent << "  \"calibrationResponseDebugViewStatus\": \"" << escapeJson(model.calibrationResponseDebugViewStatus) << "\",\n";
        f << indent << "  \"materialTypeSpecularRoutingStatus\": \"" << escapeJson(model.materialTypeSpecularRoutingStatus) << "\",\n";
        f << indent << "  \"paintMaterialGlossStatus\": \"" << escapeJson(model.paintMaterialGlossStatus) << "\",\n";
        f << indent << "  \"metalMaterialGlossStatus\": \"" << escapeJson(model.metalMaterialGlossStatus) << "\",\n";
        f << indent << "  \"rubberMaterialGlossStatus\": \"" << escapeJson(model.rubberMaterialGlossStatus) << "\",\n";
        f << indent << "  \"specularGlossPerformanceStatus\": \"" << escapeJson(model.specularGlossPerformanceStatus) << "\",\n";
        f << indent << "  \"glossVisibleResponseStatus\": \"" << escapeJson(model.glossVisibleResponseStatus) << "\",\n";
        f << indent << "  \"glossAffectsSpecularLobe\": \"" << escapeJson(model.glossAffectsSpecularLobe) << "\",\n";
        f << indent << "  \"glossAffectsReflectionWeight\": \"" << escapeJson(model.glossAffectsReflectionWeight) << "\",\n";
        f << indent << "  \"fpsCurrent\": " << model.fpsCurrent << ",\n";
        f << indent << "  \"frameTimeMs\": " << model.frameTimeMs << ",\n";
        f << indent << "  \"fpsSource\": \"" << escapeJson(model.fpsSource) << "\",\n";
        f << indent << "  \"fpsLastStable\": " << model.fpsLastStable << ",\n";
        f << indent << "  \"frameTimeLastStableMs\": " << model.frameTimeLastStableMs << ",\n";
        f << indent << "  \"fpsStatus\": \"" << escapeJson(model.fpsStatus) << "\",\n";
        f << indent << "  \"fpsUpdateMode\": \"" << escapeJson(model.fpsUpdateMode) << "\",\n";
        f << indent << "  \"fpsSampleWindowMs\": " << model.fpsSampleWindowMs << ",\n";
        f << indent << "  \"framesRenderedLive\": " << model.framesRenderedLive << ",\n";
        f << indent << "  \"modelUploadRepeatCount\": " << model.modelUploadRepeatCount << ",\n";
        f << indent << "  \"uploadGenerationId\": " << model.uploadGenerationId << ",\n";
        f << indent << "  \"renderLoopAllocationGuardStatus\": \"" << escapeJson(model.renderLoopAllocationGuardStatus) << "\",\n";
        f << indent << "  \"debugZipStatus\": \"" << escapeJson(model.debugZipStatus) << "\",\n";
        f << indent << "  \"debugZipPath\": \"" << escapeJson(model.debugZipPath) << "\",\n";
        f << indent << "  \"debugZipIncludedFiles\": \"" << escapeJson(model.debugZipIncludedFiles) << "\",\n";
        f << indent << "  \"debugZipReason\": \"" << escapeJson(model.debugZipReason) << "\",\n";
        f << indent << "  \"fallbackCubeVisible\": " << (model.fallbackCubeVisible ? "true" : "false") << ",\n";
        f << indent << "  \"fallbackCubeStatus\": \"" << escapeJson(model.fallbackCubeStatus) << "\",\n";
        f << indent << "  \"reason\": \"" << escapeJson(model.reason) << "\",\n";
        f << indent << "  \"cubeStatus\": \"" << (cubeReady || !model.fallbackCubeVisible ? "ok" : "failed") << "\",\n";
        f << indent << "  \"depthStatus\": \"" << (depthReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"cameraStatus\": \"" << (cameraReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"cameraMvpStatus\": \"" << (cameraMvpReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"cameraControlsStatus\": \"" << (cameraControlsReady ? "ok" : "not_implemented") << "\",\n";
        f << indent << "  \"cameraYawDeg\": " << cameraYawDeg << ",\n";
        f << indent << "  \"cameraPitchDeg\": " << cameraPitchDeg << ",\n";
        f << indent << "  \"cameraDistance\": " << cameraDistance << ",\n";
        f << indent << "  \"indexBufferReady\": " << (indexBufferReady ? "true" : "false") << ",\n";
        f << indent << "  \"uniformOrPushConstantsReady\": " << (uniformOrPushConstantsReady ? "true" : "false") << ",\n";
        f << indent << "  \"materialConstantsReady\": " << (materialConstantsReady ? "true" : "false") << ",\n";
        f << indent << "  \"meshAttributeLayoutReady\": " << (meshAttributeLayoutReady ? "true" : "false") << ",\n";
        f << indent << "  \"vertexLayout\": \"" << vertexLayout << "\",\n";
        f << indent << "  \"vertexStrideBytes\": " << vertexStrideBytes << ",\n";
        f << indent << "  \"material\": {\n";
        f << indent << "    \"materialId\": " << material.materialId << ",\n";
        f << indent << "    \"baseColorFactor\": [" << material.baseColorFactor[0] << ", " << material.baseColorFactor[1] << ", " << material.baseColorFactor[2] << ", " << material.baseColorFactor[3] << "],\n";
        f << indent << "    \"metallicFactor\": " << material.metallicFactor << ",\n";
        f << indent << "    \"roughnessFactor\": " << material.roughnessFactor << ",\n";
        f << indent << "    \"normalScale\": " << material.normalScale << ",\n";
        f << indent << "    \"occlusionStrength\": " << material.occlusionStrength << ",\n";
        f << indent << "    \"emissiveFactor\": [" << material.emissiveFactor[0] << ", " << material.emissiveFactor[1] << ", " << material.emissiveFactor[2] << "],\n";
        f << indent << "    \"alphaMode\": \"OPAQUE\"\n";
        f << indent << "  },\n";
        f << indent << "  \"vertexCount\": " << vertexCount << ",\n";
        f << indent << "  \"indexCount\": " << indexCount << ",\n";
        f << indent << "  \"framesRendered\": " << framesRendered << ",\n";
        f << indent << "  \"rendererPath\": \"Android Native Vulkan\",\n";
        f << indent << "  \"triangleFallback\": \"" << (triangleFallbackEnabled ? "enabled" : "available/disabled") << "\",\n";
        f << indent << "  \"screenshot\": { \"status\": \"not_available\", \"reason\": \"renderer_readback_not_implemented\" }\n";
        f << indent << "}";
    }
};

} // namespace solum
