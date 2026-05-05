#pragma once
#include "gpu_buffer.hpp"

namespace solum {

struct MeshResource {
    GpuBuffer vertexBuffer;
    uint32_t vertexCount = 0;
    std::string debugName;

    void destroy() {
        vertexBuffer.destroy();
        vertexCount = 0;
        debugName.clear();
    }

    bool ready() const { return vertexBuffer.valid() && vertexCount > 0; }

    bool createValidationCube(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
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
            {-s,-s,-s, 0.65f,0.32f,1.0f}, { s,-s, s, 0.78f,0.48f,1.0f}, {-s,-s, s, 0.78f,0.48f,1.0f}
        };

        debugName = "validation_cube_mesh";
        vertexCount = (uint32_t)(sizeof(verts) / sizeof(verts[0]));
        bool ok = vertexBuffer.createHostVisible(physicalDevice, device, sizeof(verts), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, verts, error);
        if (!ok) vertexCount = 0;
        return ok;
    }

    void bind(VkCommandBuffer cmd) const {
        VkDeviceSize offsets[] = { 0 };
        vkCmdBindVertexBuffers(cmd, 0, 1, &vertexBuffer.buffer, offsets);
    }
};

} // namespace solum
