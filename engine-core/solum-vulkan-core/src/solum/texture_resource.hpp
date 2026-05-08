#pragma once
#include "gpu_buffer.hpp"
#include <functional>

namespace solum {

struct TextureResource {
    VkDevice device = VK_NULL_HANDLE;
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkImageView view = VK_NULL_HANDLE;
    VkSampler sampler = VK_NULL_HANDLE;
    uint32_t width = 0;
    uint32_t height = 0;
    uint32_t bytes = 0;
    bool ready = false;

    void destroy() {
        if (device != VK_NULL_HANDLE) {
            if (sampler != VK_NULL_HANDLE) vkDestroySampler(device, sampler, nullptr);
            if (view != VK_NULL_HANDLE) vkDestroyImageView(device, view, nullptr);
            if (image != VK_NULL_HANDLE) vkDestroyImage(device, image, nullptr);
            if (memory != VK_NULL_HANDLE) vkFreeMemory(device, memory, nullptr);
        }
        image = VK_NULL_HANDLE;
        memory = VK_NULL_HANDLE;
        view = VK_NULL_HANDLE;
        sampler = VK_NULL_HANDLE;
        device = VK_NULL_HANDLE;
        width = 0;
        height = 0;
        bytes = 0;
        ready = false;
    }

    static bool runSingleUseCommand(VkDevice device, VkQueue queue, VkCommandPool pool, const std::function<void(VkCommandBuffer)>& record, std::string& error) {
        VkCommandBuffer cmd = VK_NULL_HANDLE;
        VkCommandBufferAllocateInfo ai{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO };
        ai.commandPool = pool;
        ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        ai.commandBufferCount = 1;
        VkResult r = vkAllocateCommandBuffers(device, &ai, &cmd);
        if (r != VK_SUCCESS) { error = "texture command buffer allocate failed: " + vkResultName(r); return false; }
        VkCommandBufferBeginInfo bi{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO };
        bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
        r = vkBeginCommandBuffer(cmd, &bi);
        if (r != VK_SUCCESS) { error = "texture command begin failed: " + vkResultName(r); vkFreeCommandBuffers(device, pool, 1, &cmd); return false; }
        record(cmd);
        r = vkEndCommandBuffer(cmd);
        if (r != VK_SUCCESS) { error = "texture command end failed: " + vkResultName(r); vkFreeCommandBuffers(device, pool, 1, &cmd); return false; }
        VkSubmitInfo submit{ VK_STRUCTURE_TYPE_SUBMIT_INFO };
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &cmd;
        r = vkQueueSubmit(queue, 1, &submit, VK_NULL_HANDLE);
        if (r == VK_SUCCESS) r = vkQueueWaitIdle(queue);
        vkFreeCommandBuffers(device, pool, 1, &cmd);
        if (r != VK_SUCCESS) { error = "texture command submit/wait failed: " + vkResultName(r); return false; }
        return true;
    }

    bool createRgba8(
        VkPhysicalDevice physicalDevice,
        VkDevice inDevice,
        VkQueue queue,
        VkCommandPool commandPool,
        const uint8_t* rgba,
        uint32_t inWidth,
        uint32_t inHeight,
        std::string& error
    ) {
        destroy();
        if (!rgba || inWidth == 0 || inHeight == 0) {
            error = "texture pixels empty";
            return false;
        }
        device = inDevice;
        width = inWidth;
        height = inHeight;
        bytes = width * height * 4u;
        GpuBuffer staging;
        if (!staging.createHostVisible(physicalDevice, device, bytes, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, rgba, error)) return false;

        VkImageCreateInfo imageInfo{ VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO };
        imageInfo.imageType = VK_IMAGE_TYPE_2D;
        imageInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
        imageInfo.extent = { width, height, 1 };
        imageInfo.mipLevels = 1;
        imageInfo.arrayLayers = 1;
        imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
        imageInfo.tiling = VK_IMAGE_TILING_OPTIMAL;
        imageInfo.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
        imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        VkResult r = vkCreateImage(device, &imageInfo, nullptr, &image);
        if (r != VK_SUCCESS) { error = "texture image create failed: " + vkResultName(r); staging.destroy(); return false; }
        VkMemoryRequirements req{};
        vkGetImageMemoryRequirements(device, image, &req);
        uint32_t memoryType = 0;
        if (!GpuBuffer::findMemoryType(physicalDevice, req.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &memoryType)) {
            error = "texture device-local memory type not found";
            staging.destroy();
            return false;
        }
        VkMemoryAllocateInfo alloc{ VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO };
        alloc.allocationSize = req.size;
        alloc.memoryTypeIndex = memoryType;
        r = vkAllocateMemory(device, &alloc, nullptr, &memory);
        if (r != VK_SUCCESS) { error = "texture memory allocate failed: " + vkResultName(r); staging.destroy(); return false; }
        r = vkBindImageMemory(device, image, memory, 0);
        if (r != VK_SUCCESS) { error = "texture image bind failed: " + vkResultName(r); staging.destroy(); return false; }

        bool copied = runSingleUseCommand(device, queue, commandPool, [&](VkCommandBuffer cmd) {
            VkImageMemoryBarrier toTransfer{ VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER };
            toTransfer.oldLayout = VK_IMAGE_LAYOUT_UNDEFINED;
            toTransfer.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
            toTransfer.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            toTransfer.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            toTransfer.image = image;
            toTransfer.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            toTransfer.subresourceRange.levelCount = 1;
            toTransfer.subresourceRange.layerCount = 1;
            toTransfer.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0, nullptr, 1, &toTransfer);
            VkBufferImageCopy copy{};
            copy.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            copy.imageSubresource.layerCount = 1;
            copy.imageExtent = { width, height, 1 };
            vkCmdCopyBufferToImage(cmd, staging.buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &copy);
            VkImageMemoryBarrier toShader{ VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER };
            toShader.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
            toShader.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            toShader.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            toShader.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            toShader.image = image;
            toShader.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            toShader.subresourceRange.levelCount = 1;
            toShader.subresourceRange.layerCount = 1;
            toShader.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
            toShader.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, 0, nullptr, 0, nullptr, 1, &toShader);
        }, error);
        staging.destroy();
        if (!copied) return false;

        VkImageViewCreateInfo viewInfo{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
        viewInfo.image = image;
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.layerCount = 1;
        r = vkCreateImageView(device, &viewInfo, nullptr, &view);
        if (r != VK_SUCCESS) { error = "texture image view create failed: " + vkResultName(r); return false; }
        VkSamplerCreateInfo samplerInfo{ VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO };
        samplerInfo.magFilter = VK_FILTER_LINEAR;
        samplerInfo.minFilter = VK_FILTER_LINEAR;
        samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
        samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT;
        samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT;
        samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT;
        samplerInfo.maxLod = 0.0f;
        r = vkCreateSampler(device, &samplerInfo, nullptr, &sampler);
        if (r != VK_SUCCESS) { error = "texture sampler create failed: " + vkResultName(r); return false; }
        ready = true;
        return true;
    }
};

} // namespace solum
