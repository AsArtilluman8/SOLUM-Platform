#pragma once
#include "renderer_types.hpp"

namespace solum {

enum class RenderLabScene {
    Scene01FoundationCube,
    Scene02ModelImportLab,
    Scene03GlbMeshRenderLab,
    Scene04TextureBindingLab,
    Scene04LightShadowLab,
    Scene05ImportLab,
    Scene06PerformanceLab
};

struct RenderLabState {
    RenderLabScene currentScene = RenderLabScene::Scene04TextureBindingLab;
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
    const char* vertexLayout = "POSITION,NORMAL,TEXCOORD_0,COLOR_0";
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
            case RenderLabScene::Scene05ImportLab: return "scene05_import_lab";
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
            case RenderLabScene::Scene05ImportLab: return "Scene05 Import Lab";
            case RenderLabScene::Scene06PerformanceLab: return "Scene06 Performance Lab";
            default: return "Unknown";
        }
    }

    void writeJsonFields(std::ofstream& f, const char* indent = "  ") const {
        f << indent << "\"renderLab\": {\n";
        f << indent << "  \"schema\": \"solum.render_lab_state\",\n";
        f << indent << "  \"schemaVersion\": 1,\n";
        f << indent << "  \"currentLabScene\": \"" << sceneId() << "\",\n";
        f << indent << "  \"currentLabSceneName\": \"" << sceneName() << "\",\n";
        f << indent << "  \"renderingStatus\": \"" << (model.modelReady() ? "model_first_primitive" : "fallback_cube") << "\",\n";
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
