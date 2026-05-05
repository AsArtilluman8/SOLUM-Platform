#pragma once
#include "gpu_buffer.hpp"
#include <fstream>

namespace solum {

struct MeshResource {
    GpuBuffer vertexBuffer;
    uint32_t vertexCount = 0;
    std::string debugName;
    bool loadedFromGlbCache = false;

    void destroy() {
        vertexBuffer.destroy();
        vertexCount = 0;
        debugName.clear();
        loadedFromGlbCache = false;
    }

    bool ready() const { return vertexBuffer.valid() && vertexCount > 0; }

    bool readAll(const char* path, std::vector<unsigned char>& out) {
        std::ifstream f(path, std::ios::binary);
        if (!f.good()) return false;
        f.seekg(0, std::ios::end);
        const std::streamoff size = f.tellg();
        if (size <= 0) return false;
        f.seekg(0, std::ios::beg);
        out.resize((size_t)size);
        f.read((char*)out.data(), size);
        return f.good();
    }

    bool createFromSolumMeshCache(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        const char* paths[] = {
            "/storage/emulated/0/SOLUMCreative/assets/models/cache/active_mesh_v1.bin",
            "/storage/emulated/0/Download/SOLUMCreative/assets/models/cache/active_mesh_v1.bin",
        };
        std::vector<unsigned char> bytes;
        const char* usedPath = nullptr;
        for (const char* p : paths) {
            if (readAll(p, bytes)) { usedPath = p; break; }
        }
        if (!usedPath) { error = "active_mesh_v1.bin not found"; return false; }
        if (bytes.size() < 16 || std::memcmp(bytes.data(), "SOLMESH1", 8) != 0) {
            error = "invalid SOLMESH1 header"; return false;
        }
        uint32_t count = 0;
        std::memcpy(&count, bytes.data() + 8, sizeof(uint32_t));
        const size_t expected = 16ull + (size_t)count * sizeof(Vertex3D);
        if (count < 3 || expected > bytes.size()) {
            error = "invalid SOLMESH1 vertex payload"; return false;
        }
        debugName = "glb_active_mesh_cache";
        vertexCount = count;
        const bool ok = vertexBuffer.createHostVisible(
            physicalDevice, device, (VkDeviceSize)(count * sizeof(Vertex3D)),
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, bytes.data() + 16, error
        );
        if (!ok) { vertexCount = 0; loadedFromGlbCache = false; return false; }
        loadedFromGlbCache = true;
        return true;
    }

    bool createFallbackCube(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        const float s = 0.82f;
        const Vertex3D verts[] = {
            {-s,-s, s, 1.0f,0.45f,0.08f}, { s,-s, s, 1.0f,0.45f,0.08f}, { s, s, s, 1.0f,0.62f,0.16f},
            {-s,-s, s, 1.0f,0.45f,0.08f}, { s, s, s, 1.0f,0.62f,0.16f}, {-s, s, s, 1.0f,0.62f,0.16f},
            { s,-s,-s, 0.15f,0.65f,1.0f}, {-s,-s,-s, 0.15f,0.65f,1.0f}, {-s, s,-s, 0.22f,0.82f,1.0f},
            { s,-s,-s, 0.15f,0.65f,1.0f}, {-s, s,-s, 0.22f,0.82f,1.0f}, { s, s,-s, 0.22f,0.82f,1.0f},
            {-s,-s,-s, 0.32f,1.0f,0.42f}, {-s,-s, s, 0.32f,1.0f,0.42f}, {-s, s, s, 0.50f,1.0f,0.56f},
            {-s,-s,-s, 0.32f,1.0f,0.42f}, {-s, s, s, 0.50f,1.0f,0.56f}, {-s, s,-s, 0.50f,1.0f,0.56f},
            { s,-s, s, 1.0f,0.24f,0.32f}, { s,-s,-s, 1.0f,0.24f,0.32f}, { s, s,-s, 1.0f,0.36f,0.48f},
            { s,-s, s, 1.0f,0.24f,0.32f}, { s, s,-s, 1.0f,0.36f,0.48f}, { s, s, s, 1.0f,0.36f,0.48f},
            {-s, s, s, 0.92f,0.92f,0.20f}, { s, s, s, 0.92f,0.92f,0.20f}, { s, s,-s, 1.0f,1.0f,0.34f},
            {-s, s, s, 0.92f,0.92f,0.20f}, { s, s,-s, 1.0f,1.0f,0.34f}, {-s, s,-s, 1.0f,1.0f,0.34f},
            {-s,-s,-s, 0.65f,0.32f,1.0f}, { s,-s,-s, 0.65f,0.32f,1.0f}, { s,-s, s, 0.78f,0.48f,1.0f},
            {-s,-s,-s, 0.65f,0.32f,1.0f}, { s,-s, s, 0.78f,0.48f,1.0f}, {-s,-s, s, 0.78f,0.48f,1.0f},
        };
        debugName = "validation_cube_mesh";
        vertexCount = (uint32_t)(sizeof(verts) / sizeof(verts[0]));
        loadedFromGlbCache = false;
        const bool ok = vertexBuffer.createHostVisible(physicalDevice, device, sizeof(verts), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, verts, error);
        if (!ok) vertexCount = 0;
        return ok;
    }

    bool createValidationCube(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        std::string cacheError;
        if (createFromSolumMeshCache(physicalDevice, device, cacheError)) return true;
        error.clear();
        return createFallbackCube(physicalDevice, device, error);
    }

    void bind(VkCommandBuffer cmd) const {
        VkDeviceSize offsets[] = { 0 };
        vkCmdBindVertexBuffers(cmd, 0, 1, &vertexBuffer.buffer, offsets);
    }
};

} // namespace solum
