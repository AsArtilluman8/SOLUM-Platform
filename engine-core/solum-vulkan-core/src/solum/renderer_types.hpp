#pragma once
#define VK_USE_PLATFORM_ANDROID_KHR 1
#include <vulkan/vulkan.h>
#include <android/native_window.h>
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <cstdint>
#include <sys/stat.h>

namespace solum {

struct Vertex2D { float x; float y; };
struct Vertex3D {
    float px; float py; float pz;
    float nx; float ny; float nz;
    float u; float v;
    float r; float g; float b;
};

struct CameraState {
    float yawDeg = 28.0f;
    float pitchDeg = -18.0f;
    float distance = 4.2f;
    bool controlsActive = false;
};

struct MaterialConstants {
    float baseColorFactor[4] = { 0.92f, 0.78f, 1.0f, 1.0f };
    float metallicFactor = 0.0f;
    float roughnessFactor = 0.65f;
    float normalScale = 1.0f;
    float occlusionStrength = 1.0f;
    float emissiveFactor[3] = { 0.0f, 0.0f, 0.0f };
    int alphaMode = 0;
    int materialId = 1;
    int baseColorTextureReady = 0;
    int metallicRoughnessTextureReady = 0;
    int normalTextureReady = 0;
    int occlusionTextureReady = 0;
    float sunDirection[3] = { -0.35f, -0.82f, -0.45f };
    float sunIntensity = 1.35f;
    float sunColor[3] = { 1.0f, 0.96f, 0.88f };
    float ambientIntensity = 0.34f;
    float ambientColor[3] = { 0.42f, 0.52f, 0.62f };
    int lightPreset = 0;
    int activeDebugView = 0;
    int toneMappingMode = 1;
};

inline const char* lightPresetName(int preset) {
    if (preset == 1) return "Outdoor";
    if (preset == 2) return "Soft Preview";
    return "Studio";
}

inline const char* materialDebugViewName(int view) {
    if (view == 1) return "BaseColor";
    if (view == 2) return "AO";
    if (view == 3) return "Metallic";
    if (view == 4) return "Roughness";
    if (view == 5) return "PBR Status";
    return "Final Shaded";
}

inline const char* toneMappingModeName(int mode) {
    if (mode == 2) return "aces_lite";
    if (mode == 0) return "none";
    return "reinhard";
}

struct PrimitiveDrawRange {
    uint32_t firstIndex = 0;
    uint32_t indexCount = 0;
    uint32_t firstVertex = 0;
    uint32_t vertexCount = 0;
    int materialSlot = 0;
    int textureSlot = -1;
};

struct MaterialSlotState {
    float baseColorFactor[4] = { 1.0f, 1.0f, 1.0f, 1.0f };
    float metallicFactor = 0.0f;
    float roughnessFactor = 1.0f;
    float normalScale = 1.0f;
    float occlusionStrength = 1.0f;
    int alphaMode = 0;
    float alphaCutoff = 0.5f;
    bool doubleSided = false;
    int baseColorTextureSlot = -1;
    int metallicRoughnessTextureSlot = -1;
    int normalTextureSlot = -1;
    int occlusionTextureSlot = -1;
};

struct ModelRenderState {
    std::string activeModelName = "none";
    std::string activeModelPath;
    int activePrimitiveIndex = 0;
    std::string gpuUploadStatus = "failed";
    std::string drawStatus = "fallback";
    uint32_t uploadedVertexCount = 0;
    uint32_t uploadedIndexCount = 0;
    const char* modelVertexLayout = "POSITION,NORMAL,TEXCOORD_0,COLOR_0";
    float boundsMin[3] = { 0.0f, 0.0f, 0.0f };
    float boundsMax[3] = { 0.0f, 0.0f, 0.0f };
    float boundsCenter[3] = { 0.0f, 0.0f, 0.0f };
    float modelScale = 1.0f;
    const char* modelRenderMode = "multi_primitive_static";
    uint32_t primitiveCountTotal = 0;
    uint32_t primitiveCountRendered = 0;
    uint32_t primitiveCountSkipped = 0;
    uint32_t unsupportedPrimitiveCount = 0;
    uint32_t materialSlotCount = 0;
    uint32_t materialSlotCountRendered = 0;
    uint32_t textureSlotCount = 0;
    uint32_t uploadedTextureCount = 0;
    uint32_t textureFallbackCount = 0;
    uint32_t skippedTextureCount = 0;
    uint32_t textureSlotLimit = 8;
    std::string pbrMapsStatus = "missing";
    std::string metallicRoughnessStatus = "missing";
    std::string normalMapStatus = "missing";
    std::string occlusionMapStatus = "missing";
    float metallicFactor = 0.0f;
    float roughnessFactor = 1.0f;
    float normalScale = 1.0f;
    float occlusionStrength = 1.0f;
    uint32_t pbrTextureSlotCount = 0;
    uint32_t uploadedPbrTextureCount = 0;
    uint32_t skippedPbrTextureCount = 0;
    uint32_t pbrTextureFallbackCount = 0;
    std::string materialSlotDiagnostics = "[]";
    std::string lightingStatus = "ok";
    float sunDirection[3] = { -0.35f, -0.82f, -0.45f };
    float sunColor[3] = { 1.0f, 0.96f, 0.88f };
    float sunIntensity = 1.35f;
    float ambientColor[3] = { 0.42f, 0.52f, 0.62f };
    float ambientIntensity = 0.34f;
    std::string lightPreset = "Studio";
    std::string materialResponseStatus = "foundation_simple_lit";
    std::string toneMappingStatus = "ok";
    std::string toneMappingMode = "reinhard";
    std::string activeDebugView = "Final Shaded";
    std::string debugViewStatus = "shader_applied";
    float fpsCurrent = 0.0f;
    float frameTimeMs = 0.0f;
    const char* fpsSource = "java_ui_frame_delta";
    std::string debugZipStatus = "not_run";
    std::string debugZipPath = "";
    std::string debugZipIncludedFiles = "";
    std::string debugZipReason = "not_run";
    bool fallbackCubeVisible = true;
    std::string meshDrawStatus = "fallback";
    std::string fallbackCubeStatus = "on";
    std::string textureUploadStatus = "missing";
    std::string baseColorTextureStatus = "missing";
    std::string baseColorTextureName = "none";
    std::string baseColorTextureSource = "none";
    std::string baseColorTextureMimeType = "none";
    uint32_t textureWidth = 0;
    uint32_t textureHeight = 0;
    uint32_t textureBytes = 0;
    bool textureFallbackUsed = true;
    std::string reason = "no active model";

    bool modelReady() const {
        return gpuUploadStatus == "ok" && (drawStatus == "ok" || drawStatus == "partial_ok") && uploadedVertexCount > 0 && primitiveCountRendered > 0;
    }
};

struct Mat4 {
    float m[16]{};

    static Mat4 identity() {
        Mat4 out{};
        out.m[0] = 1.0f;
        out.m[5] = 1.0f;
        out.m[10] = 1.0f;
        out.m[15] = 1.0f;
        return out;
    }
};

struct PushConstants {
    Mat4 mvp;
    MaterialConstants material;
};

inline Mat4 multiply(const Mat4& a, const Mat4& b) {
    Mat4 out{};
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            out.m[col * 4 + row] =
                a.m[0 * 4 + row] * b.m[col * 4 + 0] +
                a.m[1 * 4 + row] * b.m[col * 4 + 1] +
                a.m[2 * 4 + row] * b.m[col * 4 + 2] +
                a.m[3 * 4 + row] * b.m[col * 4 + 3];
        }
    }
    return out;
}

inline Mat4 translation(float x, float y, float z) {
    Mat4 out = Mat4::identity();
    out.m[12] = x;
    out.m[13] = y;
    out.m[14] = z;
    return out;
}

inline Mat4 rotationY(float radians) {
    Mat4 out = Mat4::identity();
    const float c = std::cos(radians);
    const float s = std::sin(radians);
    out.m[0] = c;
    out.m[2] = -s;
    out.m[8] = s;
    out.m[10] = c;
    return out;
}

inline Mat4 rotationX(float radians) {
    Mat4 out = Mat4::identity();
    const float c = std::cos(radians);
    const float s = std::sin(radians);
    out.m[5] = c;
    out.m[6] = s;
    out.m[9] = -s;
    out.m[10] = c;
    return out;
}

inline Mat4 perspective(float fovyRadians, float aspect, float nearPlane, float farPlane) {
    Mat4 out{};
    const float f = 1.0f / std::tan(fovyRadians * 0.5f);
    out.m[0] = f / aspect;
    out.m[5] = -f;
    out.m[10] = farPlane / (farPlane - nearPlane);
    out.m[11] = 1.0f;
    out.m[14] = -(farPlane * nearPlane) / (farPlane - nearPlane);
    return out;
}

inline std::string vkResultName(VkResult r) {
    switch (r) {
        case VK_SUCCESS: return "VK_SUCCESS";
        case VK_NOT_READY: return "VK_NOT_READY";
        case VK_TIMEOUT: return "VK_TIMEOUT";
        case VK_EVENT_SET: return "VK_EVENT_SET";
        case VK_EVENT_RESET: return "VK_EVENT_RESET";
        case VK_INCOMPLETE: return "VK_INCOMPLETE";
        case VK_ERROR_OUT_OF_HOST_MEMORY: return "VK_ERROR_OUT_OF_HOST_MEMORY";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY: return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
        case VK_ERROR_INITIALIZATION_FAILED: return "VK_ERROR_INITIALIZATION_FAILED";
        case VK_ERROR_DEVICE_LOST: return "VK_ERROR_DEVICE_LOST";
        case VK_ERROR_MEMORY_MAP_FAILED: return "VK_ERROR_MEMORY_MAP_FAILED";
        case VK_ERROR_LAYER_NOT_PRESENT: return "VK_ERROR_LAYER_NOT_PRESENT";
        case VK_ERROR_EXTENSION_NOT_PRESENT: return "VK_ERROR_EXTENSION_NOT_PRESENT";
        case VK_ERROR_FEATURE_NOT_PRESENT: return "VK_ERROR_FEATURE_NOT_PRESENT";
        case VK_ERROR_INCOMPATIBLE_DRIVER: return "VK_ERROR_INCOMPATIBLE_DRIVER";
        case VK_ERROR_TOO_MANY_OBJECTS: return "VK_ERROR_TOO_MANY_OBJECTS";
        case VK_ERROR_FORMAT_NOT_SUPPORTED: return "VK_ERROR_FORMAT_NOT_SUPPORTED";
        case VK_ERROR_SURFACE_LOST_KHR: return "VK_ERROR_SURFACE_LOST_KHR";
        case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR: return "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
        case VK_SUBOPTIMAL_KHR: return "VK_SUBOPTIMAL_KHR";
        case VK_ERROR_OUT_OF_DATE_KHR: return "VK_ERROR_OUT_OF_DATE_KHR";
        default: { char b[64]; std::snprintf(b, sizeof(b), "VkResult(%d)", (int)r); return b; }
    }
}

inline const char* deviceTypeName(VkPhysicalDeviceType t) {
    switch (t) {
        case VK_PHYSICAL_DEVICE_TYPE_OTHER: return "OTHER";
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "INTEGRATED_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "DISCRETE_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "VIRTUAL_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_CPU: return "CPU";
        default: return "UNKNOWN";
    }
}

inline void ensureDir(const std::string& path) {
    std::string cur;
    for (char c : path) { cur.push_back(c); if (c == '/') mkdir(cur.c_str(), 0775); }
    mkdir(path.c_str(), 0775);
}

inline bool endsWith(const std::string& s, const std::string& suffix) {
    return s.size() >= suffix.size() && s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
}

inline std::string escapeJson(const std::string& s) {
    std::ostringstream o;
    for (char c : s) {
        switch (c) {
            case '\\': o << "\\\\"; break;
            case '"': o << "\\\""; break;
            case '\n': o << "\\n"; break;
            case '\r': o << "\\r"; break;
            case '\t': o << "\\t"; break;
            default: o << c;
        }
    }
    return o.str();
}

inline std::string reportDirFor(const std::string& outputRoot) {
    std::string root = outputRoot.empty() ? "/storage/emulated/0/SOLUMCreative" : outputRoot;
    if (endsWith(root, "/diagnostics/latest") || endsWith(root, "/solum_diagnostics")) return root;
    return root + "/diagnostics/latest";
}

} // namespace solum
