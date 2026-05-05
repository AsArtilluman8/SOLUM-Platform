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
