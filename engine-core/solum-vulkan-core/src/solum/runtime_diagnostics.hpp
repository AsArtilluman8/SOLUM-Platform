#pragma once
#include "renderer_types.hpp"

namespace solum {

struct RuntimeDiagnostics {
    std::string outputRoot;
    std::string status = "created";
    std::string reason;
    std::string gpuName = "unknown";
    std::string gpuType = "unknown";
    std::string apiVersion = "unknown";
    VkFormat swapchainFormat = VK_FORMAT_UNDEFINED;
    VkExtent2D extent{};
    bool rendererCoreReady = false;
    bool depthReady = false;
    bool object3dReady = false;
    bool meshReady = false;
    uint32_t vertexCount = 0;
    uint64_t framesRendered = 0;
    double fpsAvg = 0.0;
    double frameMsAvg = 0.0;
    float cameraFovDegrees = 52.0f;
    float cameraNear = 0.10f;
    float cameraFar = 64.0f;
    float cameraDistance = 5.8f;
    float depthPrecisionRatio = 640.0f;
    std::string depthStatus = "depth_ready";
    std::string materialStatus = "schema_ready_not_pbr";


    void writeRenderState() {
        std::string dir = reportDirFor(outputRoot);
        ensureDir(dir + "/");
        std::ofstream f(dir + "/runtime_render_state.json");
        f << "{\n";
        f << "  \"schema\": \"solum.runtime_render_state\",\n";
        f << "  \"schemaVersion\": 1,\n";
        f << "  \"fpsAvg\": " << fpsAvg << ",\n";
        f << "  \"frameMsAvg\": " << frameMsAvg << ",\n";
        f << "  \"framesRendered\": " << framesRendered << ",\n";
        f << "  \"fpsAvg\": " << fpsAvg << ",\n";
        f << "  \"frameMsAvg\": " << frameMsAvg << ",\n";
        f << "  \"camera\": { \"fovDegrees\": " << cameraFovDegrees << ", \"near\": " << cameraNear << ", \"far\": " << cameraFar << ", \"distance\": " << cameraDistance << " },\n";
        f << "  \"depthStatus\": \"" << escapeJson(depthStatus) << "\",\n";
        f << "  \"depthPrecisionRatio\": " << depthPrecisionRatio << ",\n";
        f << "  \"framebuffer\": { \"width\": " << extent.width << ", \"height\": " << extent.height << " }\n";
        f << "}\n";
        writeRenderState();
        writeModelState();
        writeMaterialState();
    }

    void writeModelState() {
        std::string dir = reportDirFor(outputRoot);
        ensureDir(dir + "/");
        std::ofstream f(dir + "/runtime_model_state.json");
        f << "{\n";
        f << "  \"schema\": \"solum.runtime_model_state\",\n";
        f << "  \"schemaVersion\": 1,\n";
        f << "  \"modelSource\": \"validation_cube_internal\",\n";
        f << "  \"meshStatus\": \"3d_validation_mesh_ready\",\n";
        f << "  \"vertexCount\": " << vertexCount << ",\n";
        f << "  \"triangleCount\": " << (vertexCount / 3) << ",\n";
        f << "  \"indexBuffer\": false,\n";
        f << "  \"gltfLoaded\": false\n";
        f << "}\n";
    }

    void writeMaterialState() {
        std::string dir = reportDirFor(outputRoot);
        ensureDir(dir + "/");
        std::ofstream f(dir + "/runtime_material_state.json");
        f << "{\n";
        f << "  \"schema\": \"solum.runtime_material_state\",\n";
        f << "  \"schemaVersion\": 1,\n";
        f << "  \"status\": \"" << escapeJson(materialStatus) << "\",\n";
        f << "  \"materialModel\": \"glTF_2_0_PBR_target\",\n";
        f << "  \"baseColorTexture\": \"not_loaded_yet\",\n";
        f << "  \"normalTexture\": \"not_loaded_yet\",\n";
        f << "  \"metallicRoughnessTexture\": \"not_loaded_yet\",\n";
        f << "  \"occlusionTexture\": \"not_loaded_yet\",\n";
        f << "  \"emissiveTexture\": \"not_loaded_yet\",\n";
        f << "  \"alphaMode\": \"OPAQUE_target\",\n";
        f << "  \"colorSpaceRule\": \"baseColor_sRGB__lighting_linear\"\n";
        f << "}\n";
    }

    void write(const std::string& newStatus, const std::string& newReason) {
        status = newStatus;
        reason = newReason;
        std::string dir = reportDirFor(outputRoot);
        ensureDir(dir + "/");
        std::ofstream f(dir + "/runtime_vulkan_caps.json");
        f << "{\n";
        f << "  \"schema\": \"solum.runtime_vulkan_caps\",\n";
        f << "  \"schemaVersion\": 3,\n";
        f << "  \"status\": \"" << escapeJson(status) << "\",\n";
        f << "  \"reason\": \"" << escapeJson(reason) << "\",\n";
        f << "  \"rendererPath\": \"Android Native Vulkan\",\n";
        f << "  \"rendererCoreReady\": " << (rendererCoreReady ? "true" : "false") << ",\n";
        f << "  \"depthReady\": " << (depthReady ? "true" : "false") << ",\n";
        f << "  \"object3dReady\": " << (object3dReady ? "true" : "false") << ",\n";
        f << "  \"meshReady\": " << (meshReady ? "true" : "false") << ",\n";
        f << "  \"deviceName\": \"" << escapeJson(gpuName) << "\",\n";
        f << "  \"deviceType\": \"" << escapeJson(gpuType) << "\",\n";
        f << "  \"apiVersion\": \"" << escapeJson(apiVersion) << "\",\n";
        f << "  \"swapchainFormat\": " << (int)swapchainFormat << ",\n";
        f << "  \"extent\": { \"width\": " << extent.width << ", \"height\": " << extent.height << " },\n";
        f << "  \"vertexCount\": " << vertexCount << ",\n";
        f << "  \"framesRendered\": " << framesRendered << "\n";
        f << "}\n";
    }
};

} // namespace solum
