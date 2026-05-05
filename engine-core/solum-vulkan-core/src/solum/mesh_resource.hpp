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

    bool createValidationTriangle(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        const Vertex2D verts[] = {
            { 0.0f, -0.55f },
            { 0.55f, 0.45f },
            { -0.55f, 0.45f },
        };
        debugName = "validation_triangle_mesh";
        vertexCount = 3;
        const bool ok = vertexBuffer.createHostVisible(
            physicalDevice,
            device,
            sizeof(verts),
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
            verts,
            error
        );
        if (!ok) vertexCount = 0;
        return ok;
    }

    void bind(VkCommandBuffer cmd) const {
        VkDeviceSize offsets[] = { 0 };
        vkCmdBindVertexBuffers(cmd, 0, 1, &vertexBuffer.buffer, offsets);
    }
};

} // namespace solum
