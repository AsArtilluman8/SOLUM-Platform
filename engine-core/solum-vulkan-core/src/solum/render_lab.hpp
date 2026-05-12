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
    Scene06PerformanceLab
};

struct RenderLabState {
    RenderLabScene currentScene = RenderLabScene::Scene10LightingControlLab;
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
        f << indent << "  \"reflectionFoundationStatus\": \"" << escapeJson(model.reflectionFoundationStatus) << "\",\n";
        f << indent << "  \"reflectionMode\": \"" << escapeJson(model.reflectionMode) << "\",\n";
        f << indent << "  \"environmentReflectionStatus\": \"" << escapeJson(model.environmentReflectionStatus) << "\",\n";
        f << indent << "  \"lightingUniformUpdateStatus\": \"" << escapeJson(model.lightingUniformUpdateStatus) << "\",\n";
        f << indent << "  \"sliderUpdateMode\": \"" << escapeJson(model.sliderUpdateMode) << "\",\n";
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
        f << indent << "  \"brdfStatusDebugViewStatus\": \"" << escapeJson(model.brdfStatusDebugViewStatus) << "\",\n";
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
