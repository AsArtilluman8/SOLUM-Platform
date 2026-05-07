#pragma once
#include "renderer_types.hpp"

namespace solum {

enum class RenderLabScene {
    Scene01FoundationCube,
    Scene02MaterialLab,
    Scene03CameraDepthLab,
    Scene04LightShadowLab,
    Scene05ImportLab,
    Scene06PerformanceLab
};

struct RenderLabState {
    RenderLabScene currentScene = RenderLabScene::Scene01FoundationCube;
    bool cubeReady = false;
    bool depthReady = false;
    bool cameraReady = false;
    bool indexBufferReady = false;
    bool uniformOrPushConstantsReady = false;
    bool triangleFallbackAvailable = true;
    bool triangleFallbackEnabled = false;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    uint64_t framesRendered = 0;

    const char* sceneId() const {
        switch (currentScene) {
            case RenderLabScene::Scene01FoundationCube: return "scene01_foundation_cube";
            case RenderLabScene::Scene02MaterialLab: return "scene02_material_lab";
            case RenderLabScene::Scene03CameraDepthLab: return "scene03_camera_depth_lab";
            case RenderLabScene::Scene04LightShadowLab: return "scene04_light_shadow_lab";
            case RenderLabScene::Scene05ImportLab: return "scene05_import_lab";
            case RenderLabScene::Scene06PerformanceLab: return "scene06_performance_lab";
            default: return "unknown";
        }
    }

    const char* sceneName() const {
        switch (currentScene) {
            case RenderLabScene::Scene01FoundationCube: return "Scene01 Foundation Cube";
            case RenderLabScene::Scene02MaterialLab: return "Scene02 Material Lab";
            case RenderLabScene::Scene03CameraDepthLab: return "Scene03 Camera/Depth Lab";
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
        f << indent << "  \"renderingStatus\": \"foundation_only\",\n";
        f << indent << "  \"cubeStatus\": \"" << (cubeReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"depthStatus\": \"" << (depthReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"cameraStatus\": \"" << (cameraReady ? "ok" : "failed") << "\",\n";
        f << indent << "  \"indexBufferReady\": " << (indexBufferReady ? "true" : "false") << ",\n";
        f << indent << "  \"uniformOrPushConstantsReady\": " << (uniformOrPushConstantsReady ? "true" : "false") << ",\n";
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
