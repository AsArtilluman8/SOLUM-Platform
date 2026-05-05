#pragma once
#include "renderer_types.hpp"

namespace solum {

struct GpuBuffer {
    VkDevice device = VK_NULL_HANDLE;
    VkBuffer buffer = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkDeviceSize size = 0;

    bool valid() const { return buffer != VK_NULL_HANDLE && memory != VK_NULL_HANDLE; }

    void destroy() {
        if (device != VK_NULL_HANDLE) {
            if (buffer != VK_NULL_HANDLE) vkDestroyBuffer(device, buffer, nullptr);
            if (memory != VK_NULL_HANDLE) vkFreeMemory(device, memory, nullptr);
        }
        buffer = VK_NULL_HANDLE;
        memory = VK_NULL_HANDLE;
        size = 0;
        device = VK_NULL_HANDLE;
    }

    static bool findMemoryType(VkPhysicalDevice physicalDevice, uint32_t typeBits, VkMemoryPropertyFlags required, uint32_t* outIndex) {
        VkPhysicalDeviceMemoryProperties mem{};
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, &mem);
        for (uint32_t i = 0; i < mem.memoryTypeCount; ++i) {
            if ((typeBits & (1u << i)) && ((mem.memoryTypes[i].propertyFlags & required) == required)) {
                *outIndex = i;
                return true;
            }
        }
        return false;
    }

    bool createHostVisible(VkPhysicalDevice physicalDevice, VkDevice inDevice, VkDeviceSize inSize, VkBufferUsageFlags usage, const void* data, std::string& error) {
        destroy();
        device = inDevice;
        size = inSize;

        VkBufferCreateInfo bci{ VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO };
        bci.size = inSize;
        bci.usage = usage;
        bci.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        VkResult r = vkCreateBuffer(device, &bci, nullptr, &buffer);
        if (r != VK_SUCCESS) { error = "VkBuffer create failed: " + vkResultName(r); return false; }

        VkMemoryRequirements req{};
        vkGetBufferMemoryRequirements(device, buffer, &req);
        uint32_t memoryType = 0;
        if (!findMemoryType(physicalDevice, req.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, &memoryType)) {
            error = "Host-visible coherent memory type not found";
            return false;
        }

        VkMemoryAllocateInfo ai{ VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO };
        ai.allocationSize = req.size;
        ai.memoryTypeIndex = memoryType;
        r = vkAllocateMemory(device, &ai, nullptr, &memory);
        if (r != VK_SUCCESS) { error = "VkDeviceMemory allocate failed: " + vkResultName(r); return false; }

        void* mapped = nullptr;
        r = vkMapMemory(device, memory, 0, inSize, 0, &mapped);
        if (r != VK_SUCCESS || !mapped) { error = "VkDeviceMemory map failed: " + vkResultName(r); return false; }
        std::memcpy(mapped, data, (size_t)inSize);
        vkUnmapMemory(device, memory);

        r = vkBindBufferMemory(device, buffer, memory, 0);
        if (r != VK_SUCCESS) { error = "VkBuffer bind memory failed: " + vkResultName(r); return false; }
        return true;
    }
};

struct GpuImage {
    VkDevice device = VK_NULL_HANDLE;
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkImageView view = VK_NULL_HANDLE;
    VkFormat format = VK_FORMAT_UNDEFINED;

    bool valid() const { return image != VK_NULL_HANDLE && memory != VK_NULL_HANDLE && view != VK_NULL_HANDLE; }

    void destroy() {
        if (device != VK_NULL_HANDLE) {
            if (view != VK_NULL_HANDLE) vkDestroyImageView(device, view, nullptr);
            if (image != VK_NULL_HANDLE) vkDestroyImage(device, image, nullptr);
            if (memory != VK_NULL_HANDLE) vkFreeMemory(device, memory, nullptr);
        }
        view = VK_NULL_HANDLE;
        image = VK_NULL_HANDLE;
        memory = VK_NULL_HANDLE;
        device = VK_NULL_HANDLE;
        format = VK_FORMAT_UNDEFINED;
    }

    bool createDepth(VkPhysicalDevice physicalDevice, VkDevice inDevice, uint32_t width, uint32_t height, std::string& error) {
        destroy();
        device = inDevice;
        format = VK_FORMAT_D32_SFLOAT;

        VkImageCreateInfo ici{ VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO };
        ici.imageType = VK_IMAGE_TYPE_2D;
        ici.extent.width = width;
        ici.extent.height = height;
        ici.extent.depth = 1;
        ici.mipLevels = 1;
        ici.arrayLayers = 1;
        ici.format = format;
        ici.tiling = VK_IMAGE_TILING_OPTIMAL;
        ici.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        ici.usage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        ici.samples = VK_SAMPLE_COUNT_1_BIT;
        ici.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

        VkResult r = vkCreateImage(device, &ici, nullptr, &image);
        if (r != VK_SUCCESS) { error = "Depth image create failed: " + vkResultName(r); return false; }

        VkMemoryRequirements req{};
        vkGetImageMemoryRequirements(device, image, &req);
        uint32_t memoryType = 0;
        if (!GpuBuffer::findMemoryType(physicalDevice, req.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &memoryType)) {
            error = "Device-local depth memory type not found";
            return false;
        }

        VkMemoryAllocateInfo ai{ VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO };
        ai.allocationSize = req.size;
        ai.memoryTypeIndex = memoryType;
        r = vkAllocateMemory(device, &ai, nullptr, &memory);
        if (r != VK_SUCCESS) { error = "Depth memory allocate failed: " + vkResultName(r); return false; }

        r = vkBindImageMemory(device, image, memory, 0);
        if (r != VK_SUCCESS) { error = "Depth image bind failed: " + vkResultName(r); return false; }

        VkImageViewCreateInfo vci{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
        vci.image = image;
        vci.viewType = VK_IMAGE_VIEW_TYPE_2D;
        vci.format = format;
        vci.subresourceRange.aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
        vci.subresourceRange.baseMipLevel = 0;
        vci.subresourceRange.levelCount = 1;
        vci.subresourceRange.baseArrayLayer = 0;
        vci.subresourceRange.layerCount = 1;
        r = vkCreateImageView(device, &vci, nullptr, &view);
        if (r != VK_SUCCESS) { error = "Depth image view failed: " + vkResultName(r); return false; }

        return true;
    }
};

} // namespace solum
