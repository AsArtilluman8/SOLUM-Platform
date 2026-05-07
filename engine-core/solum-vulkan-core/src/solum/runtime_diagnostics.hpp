#pragma once
#include "render_lab.hpp"
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
    bool vertexBufferReady = false;
    bool indexBufferReady = false;
    bool cubeReady = false;
    bool depthReady = false;
    bool cameraReady = false;
    bool uniformOrPushConstantsReady = false;
    bool triangleDrawn = false;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    uint64_t framesRendered = 0;
    RenderLabState renderLab;

    void write(const std::string& newStatus, const std::string& newReason) {
        status = newStatus;
        reason = newReason;
        std::string dir = reportDirFor(outputRoot);
        ensureDir(dir + "/");
        std::ofstream f(dir + "/runtime_vulkan_caps.json");
        f << "{\n";
        f << "  \"schema\": \"solum.runtime_vulkan_caps\",\n";
        f << "  \"schemaVersion\": 2,\n";
        f << "  \"status\": \"" << escapeJson(status) << "\",\n";
        f << "  \"reason\": \"" << escapeJson(reason) << "\",\n";
        f << "  \"rendererPath\": \"Android Native Vulkan\",\n";
        f << "  \"rendererCoreReady\": " << (rendererCoreReady ? "true" : "false") << ",\n";
        f << "  \"deviceName\": \"" << escapeJson(gpuName) << "\",\n";
        f << "  \"deviceType\": \"" << escapeJson(gpuType) << "\",\n";
        f << "  \"apiVersion\": \"" << escapeJson(apiVersion) << "\",\n";
        f << "  \"swapchainFormat\": " << (int)swapchainFormat << ",\n";
        f << "  \"extent\": { \"width\": " << extent.width << ", \"height\": " << extent.height << " },\n";
        f << "  \"vertexBufferReady\": " << (vertexBufferReady ? "true" : "false") << ",\n";
        f << "  \"indexBufferReady\": " << (indexBufferReady ? "true" : "false") << ",\n";
        f << "  \"uniformOrPushConstantsReady\": " << (uniformOrPushConstantsReady ? "true" : "false") << ",\n";
        f << "  \"vertexCount\": " << vertexCount << ",\n";
        f << "  \"indexCount\": " << indexCount << ",\n";
        f << "  \"framesRendered\": " << framesRendered << ",\n";
        f << "  \"cubeStatus\": \"" << (cubeReady ? "ok" : "failed") << "\",\n";
        f << "  \"depthStatus\": \"" << (depthReady ? "ok" : "failed") << "\",\n";
        f << "  \"cameraStatus\": \"" << (cameraReady ? "ok" : "failed") << "\",\n";
        f << "  \"screenshot\": { \"status\": \"not_available\", \"reason\": \"renderer_readback_not_implemented\" },\n";
        f << "  \"triangleDrawn\": " << (triangleDrawn ? "true" : "false") << ",\n";
        renderLab.writeJsonFields(f);
        f << "\n";
        f << "}\n";
    }
};

} // namespace solum
