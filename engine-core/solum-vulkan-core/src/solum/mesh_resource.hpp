#pragma once
#include "gpu_buffer.hpp"

namespace solum {

struct MeshResource {
    GpuBuffer vertexBuffer;
    GpuBuffer indexBuffer;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    VkIndexType indexType = VK_INDEX_TYPE_UINT16;
    std::string debugName;

    void destroy() {
        indexBuffer.destroy();
        vertexBuffer.destroy();
        vertexCount = 0;
        indexCount = 0;
        indexType = VK_INDEX_TYPE_UINT16;
        debugName.clear();
    }

    bool ready() const { return vertexBuffer.valid() && vertexCount > 0; }
    bool indexedReady() const { return ready() && indexBuffer.valid() && indexCount > 0; }

    bool createValidationTriangle(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        const Vertex3D verts[] = {
            { 0.0f, -0.55f, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.0f, 1.0f, 0.48f, 0.12f },
            { 0.55f, 0.45f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.48f, 0.12f },
            { -0.55f, 0.45f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.48f, 0.12f },
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

    bool createFoundationCube(VkPhysicalDevice physicalDevice, VkDevice device, std::string& error) {
        const Vertex3D verts[] = {
            { -0.6f, -0.6f, -0.6f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f, 0.95f, 0.18f, 0.15f },
            {  0.6f, -0.6f, -0.6f,  0.0f,  0.0f, -1.0f, 1.0f, 0.0f, 0.95f, 0.72f, 0.18f },
            {  0.6f,  0.6f, -0.6f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f, 0.15f, 0.72f, 0.95f },
            { -0.6f,  0.6f, -0.6f,  0.0f,  0.0f, -1.0f, 0.0f, 1.0f, 0.25f, 0.95f, 0.42f },
            { -0.6f, -0.6f,  0.6f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f, 0.85f, 0.22f, 0.95f },
            {  0.6f, -0.6f,  0.6f,  0.0f,  0.0f,  1.0f, 1.0f, 0.0f, 0.12f, 0.86f, 0.82f },
            {  0.6f,  0.6f,  0.6f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f, 0.95f, 0.94f, 0.34f },
            { -0.6f,  0.6f,  0.6f,  0.0f,  0.0f,  1.0f, 0.0f, 1.0f, 0.94f, 0.48f, 0.16f },
            { -0.6f, -0.6f, -0.6f,  0.0f, -1.0f,  0.0f, 0.0f, 0.0f, 0.95f, 0.18f, 0.15f },
            {  0.6f, -0.6f, -0.6f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f, 0.95f, 0.72f, 0.18f },
            {  0.6f, -0.6f,  0.6f,  0.0f, -1.0f,  0.0f, 1.0f, 1.0f, 0.12f, 0.86f, 0.82f },
            { -0.6f, -0.6f,  0.6f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f, 0.85f, 0.22f, 0.95f },
            { -0.6f,  0.6f, -0.6f,  0.0f,  1.0f,  0.0f, 0.0f, 0.0f, 0.25f, 0.95f, 0.42f },
            {  0.6f,  0.6f, -0.6f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f, 0.15f, 0.72f, 0.95f },
            {  0.6f,  0.6f,  0.6f,  0.0f,  1.0f,  0.0f, 1.0f, 1.0f, 0.95f, 0.94f, 0.34f },
            { -0.6f,  0.6f,  0.6f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f, 0.94f, 0.48f, 0.16f },
            { -0.6f, -0.6f, -0.6f, -1.0f,  0.0f,  0.0f, 0.0f, 0.0f, 0.95f, 0.18f, 0.15f },
            { -0.6f,  0.6f, -0.6f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f, 0.25f, 0.95f, 0.42f },
            { -0.6f,  0.6f,  0.6f, -1.0f,  0.0f,  0.0f, 1.0f, 1.0f, 0.94f, 0.48f, 0.16f },
            { -0.6f, -0.6f,  0.6f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f, 0.85f, 0.22f, 0.95f },
            {  0.6f, -0.6f, -0.6f,  1.0f,  0.0f,  0.0f, 0.0f, 0.0f, 0.95f, 0.72f, 0.18f },
            {  0.6f,  0.6f, -0.6f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f, 0.15f, 0.72f, 0.95f },
            {  0.6f,  0.6f,  0.6f,  1.0f,  0.0f,  0.0f, 1.0f, 1.0f, 0.95f, 0.94f, 0.34f },
            {  0.6f, -0.6f,  0.6f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f, 0.12f, 0.86f, 0.82f },
        };
        const uint16_t indices[] = {
            0, 2, 1, 0, 3, 2,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 15, 14, 12, 14, 13,
            16, 19, 18, 16, 18, 17,
            20, 21, 22, 20, 22, 23,
        };
        debugName = "scene01_foundation_cube_mesh";
        vertexCount = 24;
        indexCount = 36;
        indexType = VK_INDEX_TYPE_UINT16;
        if (!vertexBuffer.createHostVisible(physicalDevice, device, sizeof(verts), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, verts, error)) {
            vertexCount = 0;
            indexCount = 0;
            return false;
        }
        if (!indexBuffer.createHostVisible(physicalDevice, device, sizeof(indices), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, indices, error)) {
            vertexCount = 0;
            indexCount = 0;
            return false;
        }
        return true;
    }

    bool createFromInterleaved(
        VkPhysicalDevice physicalDevice,
        VkDevice device,
        const Vertex3D* vertices,
        uint32_t inVertexCount,
        const uint32_t* indices,
        uint32_t inIndexCount,
        const std::string& name,
        std::string& error
    ) {
        destroy();
        if (!vertices || inVertexCount == 0) {
            error = "model vertex data is empty";
            return false;
        }
        debugName = name;
        vertexCount = inVertexCount;
        indexCount = inIndexCount;
        indexType = VK_INDEX_TYPE_UINT32;
        if (!vertexBuffer.createHostVisible(
                physicalDevice,
                device,
                sizeof(Vertex3D) * (VkDeviceSize)inVertexCount,
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                vertices,
                error
            )) {
            vertexCount = 0;
            indexCount = 0;
            return false;
        }
        if (indices && inIndexCount > 0) {
            if (!indexBuffer.createHostVisible(
                    physicalDevice,
                    device,
                    sizeof(uint32_t) * (VkDeviceSize)inIndexCount,
                    VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    indices,
                    error
                )) {
                vertexCount = 0;
                indexCount = 0;
                return false;
            }
        }
        return true;
    }

    void bind(VkCommandBuffer cmd) const {
        VkDeviceSize offsets[] = { 0 };
        vkCmdBindVertexBuffers(cmd, 0, 1, &vertexBuffer.buffer, offsets);
    }

    void bindIndexed(VkCommandBuffer cmd) const {
        bind(cmd);
        vkCmdBindIndexBuffer(cmd, indexBuffer.buffer, 0, indexType);
    }
};

} // namespace solum
