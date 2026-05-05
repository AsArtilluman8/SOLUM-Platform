#pragma once
#include "pipeline_bundle.hpp"
#include "math3d.hpp"
#include "runtime_diagnostics.hpp"

namespace solum {

struct RendererCore {
    std::string outputRoot;
    std::string status = "SOLUM Engine\nNative object created";
    RuntimeDiagnostics diagnostics;
    CameraState camera;

    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkQueue graphicsQueue = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamily = UINT32_MAX;
    VkFormat swapchainFormat = VK_FORMAT_UNDEFINED;
    VkExtent2D extent{};

    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    VkRenderPass renderPass = VK_NULL_HANDLE;
    std::vector<VkFramebuffer> framebuffers;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> commandBuffers;
    VkSemaphore imageAvailable = VK_NULL_HANDLE;
    VkSemaphore renderFinished = VK_NULL_HANDLE;
    VkFence inFlight = VK_NULL_HANDLE;

    GpuImage depthImage;
    MeshResource validationMesh;
    PipelineBundle objectPipeline;

    uint64_t framesRendered = 0;
    bool object3dReady = false;
    bool rendererCoreReady = false;

    void setOutputRoot(const std::string& root) {
        outputRoot = root;
        diagnostics.outputRoot = root;
    }

    void syncDiagnostics() {
        diagnostics.swapchainFormat = swapchainFormat;
        diagnostics.extent = extent;
        diagnostics.rendererCoreReady = rendererCoreReady;
        diagnostics.depthReady = depthImage.valid();
        diagnostics.object3dReady = object3dReady;
        diagnostics.meshReady = validationMesh.ready();
        diagnostics.vertexCount = validationMesh.vertexCount;
        diagnostics.framesRendered = framesRendered;
    }

    void fail(const std::string& reason) {
        status = "SOLUM Engine\n" + reason;
        syncDiagnostics();
        diagnostics.write("failed", reason);
    }

    void destroyFrameResources() {
        if (device == VK_NULL_HANDLE) return;
        vkDeviceWaitIdle(device);

        validationMesh.destroy();
        objectPipeline.destroy();
        depthImage.destroy();

        if (inFlight != VK_NULL_HANDLE) { vkDestroyFence(device, inFlight, nullptr); inFlight = VK_NULL_HANDLE; }
        if (renderFinished != VK_NULL_HANDLE) { vkDestroySemaphore(device, renderFinished, nullptr); renderFinished = VK_NULL_HANDLE; }
        if (imageAvailable != VK_NULL_HANDLE) { vkDestroySemaphore(device, imageAvailable, nullptr); imageAvailable = VK_NULL_HANDLE; }

        for (VkFramebuffer fb : framebuffers) vkDestroyFramebuffer(device, fb, nullptr);
        framebuffers.clear();

        if (commandPool != VK_NULL_HANDLE) { vkDestroyCommandPool(device, commandPool, nullptr); commandPool = VK_NULL_HANDLE; }
        commandBuffers.clear();

        if (renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(device, renderPass, nullptr); renderPass = VK_NULL_HANDLE; }

        for (VkImageView view : swapchainImageViews) vkDestroyImageView(device, view, nullptr);
        swapchainImageViews.clear();
        swapchainImages.clear();

        object3dReady = false;
    }

    void destroy() {
        destroyFrameResources();
        if (device != VK_NULL_HANDLE) vkDeviceWaitIdle(device);
        if (swapchain != VK_NULL_HANDLE) { vkDestroySwapchainKHR(device, swapchain, nullptr); swapchain = VK_NULL_HANDLE; }
        if (device != VK_NULL_HANDLE) { vkDestroyDevice(device, nullptr); device = VK_NULL_HANDLE; }
        if (surface != VK_NULL_HANDLE) { vkDestroySurfaceKHR(instance, surface, nullptr); surface = VK_NULL_HANDLE; }
        if (instance != VK_NULL_HANDLE) { vkDestroyInstance(instance, nullptr); instance = VK_NULL_HANDLE; }

        physicalDevice = VK_NULL_HANDLE;
        graphicsQueue = VK_NULL_HANDLE;
        graphicsQueueFamily = UINT32_MAX;
        swapchainFormat = VK_FORMAT_UNDEFINED;
        extent = {};
        framesRendered = 0;
        rendererCoreReady = false;
        object3dReady = false;
    }

    bool createInstance() {
        const char* instanceExts[] = { "VK_KHR_surface", "VK_KHR_android_surface" };

        VkApplicationInfo app{ VK_STRUCTURE_TYPE_APPLICATION_INFO };
        app.pApplicationName = "SOLUM Engine";
        app.applicationVersion = VK_MAKE_VERSION(0, 10, 0);
        app.pEngineName = "SOLUM";
        app.engineVersion = VK_MAKE_VERSION(0, 10, 0);
        app.apiVersion = VK_API_VERSION_1_0;

        VkInstanceCreateInfo ici{ VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO };
        ici.pApplicationInfo = &app;
        ici.enabledExtensionCount = 2;
        ici.ppEnabledExtensionNames = instanceExts;

        VkResult r = vkCreateInstance(&ici, nullptr, &instance);
        if (r != VK_SUCCESS) { fail("Vulkan instance failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool createSurface(ANativeWindow* window) {
        VkAndroidSurfaceCreateInfoKHR sci{ VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR };
        sci.window = window;
        VkResult r = vkCreateAndroidSurfaceKHR(instance, &sci, nullptr, &surface);
        if (r != VK_SUCCESS) { fail("Android surface failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool pickDeviceAndQueue() {
        uint32_t deviceCount = 0;
        VkResult r = vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
        if (r != VK_SUCCESS || deviceCount == 0) { fail("No Vulkan physical device: " + vkResultName(r)); return false; }

        std::vector<VkPhysicalDevice> devices(deviceCount);
        vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

        for (auto pd : devices) {
            uint32_t qCount = 0;
            vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, nullptr);
            std::vector<VkQueueFamilyProperties> qProps(qCount);
            vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, qProps.data());

            for (uint32_t i = 0; i < qCount; ++i) {
                VkBool32 present = VK_FALSE;
                vkGetPhysicalDeviceSurfaceSupportKHR(pd, i, surface, &present);
                if ((qProps[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && present) {
                    physicalDevice = pd;
                    graphicsQueueFamily = i;
                    break;
                }
            }
            if (physicalDevice != VK_NULL_HANDLE) break;
        }

        if (physicalDevice == VK_NULL_HANDLE) { fail("No graphics+present queue"); return false; }

        VkPhysicalDeviceProperties props{};
        vkGetPhysicalDeviceProperties(physicalDevice, &props);
        diagnostics.gpuName = props.deviceName;
        diagnostics.gpuType = deviceTypeName(props.deviceType);

        char api[32];
        std::snprintf(api, sizeof(api), "%u.%u.%u", VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion), VK_VERSION_PATCH(props.apiVersion));
        diagnostics.apiVersion = api;
        return true;
    }

    bool createDevice() {
        const float prio = 1.0f;

        VkDeviceQueueCreateInfo qci{ VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO };
        qci.queueFamilyIndex = graphicsQueueFamily;
        qci.queueCount = 1;
        qci.pQueuePriorities = &prio;

        const char* deviceExts[] = { "VK_KHR_swapchain" };

        VkDeviceCreateInfo dci{ VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO };
        dci.queueCreateInfoCount = 1;
        dci.pQueueCreateInfos = &qci;
        dci.enabledExtensionCount = 1;
        dci.ppEnabledExtensionNames = deviceExts;

        VkResult r = vkCreateDevice(physicalDevice, &dci, nullptr, &device);
        if (r != VK_SUCCESS) { fail("Device create failed: " + vkResultName(r)); return false; }

        vkGetDeviceQueue(device, graphicsQueueFamily, 0, &graphicsQueue);
        return true;
    }

    bool createSwapchain(int width, int height) {
        VkSurfaceCapabilitiesKHR caps{};
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, &caps);

        uint32_t fmtCount = 0;
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &fmtCount, nullptr);
        std::vector<VkSurfaceFormatKHR> formats(fmtCount);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, &fmtCount, formats.data());

        VkSurfaceFormatKHR chosen = formats.empty() ? VkSurfaceFormatKHR{VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR} : formats[0];
        for (const auto& f : formats) {
            if (f.format == VK_FORMAT_R8G8B8A8_UNORM || f.format == VK_FORMAT_B8G8R8A8_UNORM) { chosen = f; break; }
        }

        swapchainFormat = chosen.format;
        extent.width = caps.currentExtent.width == 0xFFFFFFFF ? (uint32_t)width : caps.currentExtent.width;
        extent.height = caps.currentExtent.height == 0xFFFFFFFF ? (uint32_t)height : caps.currentExtent.height;

        if (extent.width == 0 || extent.height == 0) { fail("Invalid surface extent"); return false; }

        uint32_t imageCount = caps.minImageCount + 1;
        if (caps.maxImageCount > 0 && imageCount > caps.maxImageCount) imageCount = caps.maxImageCount;

        VkSwapchainCreateInfoKHR sw{ VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR };
        sw.surface = surface;
        sw.minImageCount = imageCount;
        sw.imageFormat = chosen.format;
        sw.imageColorSpace = chosen.colorSpace;
        sw.imageExtent = extent;
        sw.imageArrayLayers = 1;
        sw.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        sw.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
        sw.preTransform = caps.currentTransform;
        sw.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
        sw.presentMode = VK_PRESENT_MODE_FIFO_KHR;
        sw.clipped = VK_TRUE;

        VkResult r = vkCreateSwapchainKHR(device, &sw, nullptr, &swapchain);
        if (r != VK_SUCCESS) { fail("Swapchain failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool createImageViews() {
        uint32_t count = 0;
        VkResult r = vkGetSwapchainImagesKHR(device, swapchain, &count, nullptr);
        if (r != VK_SUCCESS || count == 0) { fail("Swapchain image query failed: " + vkResultName(r)); return false; }

        swapchainImages.resize(count);
        vkGetSwapchainImagesKHR(device, swapchain, &count, swapchainImages.data());
        swapchainImageViews.resize(count, VK_NULL_HANDLE);

        for (uint32_t i = 0; i < count; ++i) {
            VkImageViewCreateInfo ci{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
            ci.image = swapchainImages[i];
            ci.viewType = VK_IMAGE_VIEW_TYPE_2D;
            ci.format = swapchainFormat;
            ci.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            ci.subresourceRange.baseMipLevel = 0;
            ci.subresourceRange.levelCount = 1;
            ci.subresourceRange.baseArrayLayer = 0;
            ci.subresourceRange.layerCount = 1;

            r = vkCreateImageView(device, &ci, nullptr, &swapchainImageViews[i]);
            if (r != VK_SUCCESS) { fail("ImageView failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    bool createDepthResources() {
        std::string error;
        if (!depthImage.createDepth(physicalDevice, device, extent.width, extent.height, error)) {
            fail("Depth buffer failed: " + error);
            return false;
        }
        return true;
    }

    bool createRenderPass() {
        VkAttachmentDescription color{};
        color.format = swapchainFormat;
        color.samples = VK_SAMPLE_COUNT_1_BIT;
        color.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        color.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
        color.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        color.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        color.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        color.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

        VkAttachmentDescription depth{};
        depth.format = VK_FORMAT_D32_SFLOAT;
        depth.samples = VK_SAMPLE_COUNT_1_BIT;
        depth.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        depth.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        depth.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        depth.finalLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;

        VkAttachmentDescription attachments[] = { color, depth };

        VkAttachmentReference colorRef{};
        colorRef.attachment = 0;
        colorRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

        VkAttachmentReference depthRef{};
        depthRef.attachment = 1;
        depthRef.layout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;

        VkSubpassDescription subpass{};
        subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
        subpass.colorAttachmentCount = 1;
        subpass.pColorAttachments = &colorRef;
        subpass.pDepthStencilAttachment = &depthRef;

        VkSubpassDependency deps[2]{};
        deps[0].srcSubpass = VK_SUBPASS_EXTERNAL;
        deps[0].dstSubpass = 0;
        deps[0].srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        deps[0].dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        deps[0].dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

        deps[1].srcSubpass = 0;
        deps[1].dstSubpass = VK_SUBPASS_EXTERNAL;
        deps[1].srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        deps[1].dstStageMask = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
        deps[1].srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;

        VkRenderPassCreateInfo ci{ VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO };
        ci.attachmentCount = 2;
        ci.pAttachments = attachments;
        ci.subpassCount = 1;
        ci.pSubpasses = &subpass;
        ci.dependencyCount = 2;
        ci.pDependencies = deps;

        VkResult r = vkCreateRenderPass(device, &ci, nullptr, &renderPass);
        if (r != VK_SUCCESS) { fail("RenderPass color+depth failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool createFramebuffers() {
        framebuffers.resize(swapchainImageViews.size(), VK_NULL_HANDLE);

        for (size_t i = 0; i < swapchainImageViews.size(); ++i) {
            VkImageView attachments[] = { swapchainImageViews[i], depthImage.view };

            VkFramebufferCreateInfo ci{ VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO };
            ci.renderPass = renderPass;
            ci.attachmentCount = 2;
            ci.pAttachments = attachments;
            ci.width = extent.width;
            ci.height = extent.height;
            ci.layers = 1;

            VkResult r = vkCreateFramebuffer(device, &ci, nullptr, &framebuffers[i]);
            if (r != VK_SUCCESS) { fail("Framebuffer color+depth failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    bool createCommandsAndSync() {
        VkCommandPoolCreateInfo pool{ VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO };
        pool.queueFamilyIndex = graphicsQueueFamily;
        pool.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

        VkResult r = vkCreateCommandPool(device, &pool, nullptr, &commandPool);
        if (r != VK_SUCCESS) { fail("CommandPool failed: " + vkResultName(r)); return false; }

        commandBuffers.resize(framebuffers.size(), VK_NULL_HANDLE);

        VkCommandBufferAllocateInfo ai{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO };
        ai.commandPool = commandPool;
        ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        ai.commandBufferCount = (uint32_t)commandBuffers.size();

        r = vkAllocateCommandBuffers(device, &ai, commandBuffers.data());
        if (r != VK_SUCCESS) { fail("CommandBuffers failed: " + vkResultName(r)); return false; }

        VkSemaphoreCreateInfo si{ VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO };
        if ((r = vkCreateSemaphore(device, &si, nullptr, &imageAvailable)) != VK_SUCCESS) { fail("ImageAvailable semaphore failed: " + vkResultName(r)); return false; }
        if ((r = vkCreateSemaphore(device, &si, nullptr, &renderFinished)) != VK_SUCCESS) { fail("RenderFinished semaphore failed: " + vkResultName(r)); return false; }

        VkFenceCreateInfo fi{ VK_STRUCTURE_TYPE_FENCE_CREATE_INFO };
        fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        if ((r = vkCreateFence(device, &fi, nullptr, &inFlight)) != VK_SUCCESS) { fail("Fence failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool recordFrame(uint32_t imageIndex) {
        VkCommandBuffer cmd = commandBuffers[imageIndex];

        VkCommandBufferBeginInfo bi{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO };
        bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

        VkResult r = vkBeginCommandBuffer(cmd, &bi);
        if (r != VK_SUCCESS) { fail("Begin command buffer failed: " + vkResultName(r)); return false; }

        VkClearValue clears[2]{};
        clears[0].color.float32[0] = 0.02f;
        clears[0].color.float32[1] = 0.07f;
        clears[0].color.float32[2] = 0.09f;
        clears[0].color.float32[3] = 1.0f;
        clears[1].depthStencil.depth = 1.0f;
        clears[1].depthStencil.stencil = 0;

        VkRenderPassBeginInfo rp{ VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO };
        rp.renderPass = renderPass;
        rp.framebuffer = framebuffers[imageIndex];
        rp.renderArea.extent = extent;
        rp.clearValueCount = 2;
        rp.pClearValues = clears;

        vkCmdBeginRenderPass(cmd, &rp, VK_SUBPASS_CONTENTS_INLINE);
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, objectPipeline.pipeline);
        validationMesh.bind(cmd);
        Mat4 mvp = makeCameraMvp(extent.width, extent.height, camera);
        vkCmdPushConstants(cmd, objectPipeline.layout, VK_SHADER_STAGE_VERTEX_BIT, 0, sizeof(Mat4), &mvp);
        vkCmdDraw(cmd, validationMesh.vertexCount, 1, 0, 0);
        vkCmdEndRenderPass(cmd);

        r = vkEndCommandBuffer(cmd);
        if (r != VK_SUCCESS) { fail("End command buffer failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool renderOneFrame() {
        if (device == VK_NULL_HANDLE || swapchain == VK_NULL_HANDLE || commandBuffers.empty() || !validationMesh.ready() || !depthImage.valid()) return false;

        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        vkResetFences(device, 1, &inFlight);

        uint32_t imageIndex = 0;
        VkResult r = vkAcquireNextImageKHR(device, swapchain, UINT64_MAX, imageAvailable, VK_NULL_HANDLE, &imageIndex);
        if (r == VK_ERROR_OUT_OF_DATE_KHR) { fail("Swapchain out of date before frame"); return false; }
        if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR) { fail("Acquire image failed: " + vkResultName(r)); return false; }

        vkResetCommandBuffer(commandBuffers[imageIndex], 0);
        if (!recordFrame(imageIndex)) return false;

        VkPipelineStageFlags waitStages[] = { VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT };

        VkSubmitInfo submit{ VK_STRUCTURE_TYPE_SUBMIT_INFO };
        submit.waitSemaphoreCount = 1;
        submit.pWaitSemaphores = &imageAvailable;
        submit.pWaitDstStageMask = waitStages;
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &commandBuffers[imageIndex];
        submit.signalSemaphoreCount = 1;
        submit.pSignalSemaphores = &renderFinished;

        r = vkQueueSubmit(graphicsQueue, 1, &submit, inFlight);
        if (r != VK_SUCCESS) { fail("Queue submit failed: " + vkResultName(r)); return false; }

        VkPresentInfoKHR present{ VK_STRUCTURE_TYPE_PRESENT_INFO_KHR };
        present.waitSemaphoreCount = 1;
        present.pWaitSemaphores = &renderFinished;
        present.swapchainCount = 1;
        present.pSwapchains = &swapchain;
        present.pImageIndices = &imageIndex;

        r = vkQueuePresentKHR(graphicsQueue, &present);
        if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR && r != VK_ERROR_OUT_OF_DATE_KHR) { fail("Present failed: " + vkResultName(r)); return false; }

        framesRendered += 1;
        object3dReady = true;
        return true;
    }

    bool createRendererResources() {
        std::string error;
        if (!createImageViews()) return false;
        if (!createDepthResources()) return false;
        if (!createRenderPass()) return false;
        if (!createFramebuffers()) return false;
        if (!validationMesh.createValidationCube(physicalDevice, device, error)) { fail("Cube MeshResource failed: " + error); return false; }
        if (!objectPipeline.createObjectPipeline(device, renderPass, extent, error)) { fail("Object PipelineBundle failed: " + error); return false; }
        if (!createCommandsAndSync()) return false;
        return true;
    }

    bool init(ANativeWindow* window, int width, int height, const std::string& root) {
        setOutputRoot(root);
        destroy();
        status = "SOLUM Engine\nInitializing 3D object path...";

        if (!createInstance()) return false;
        if (!createSurface(window)) return false;
        if (!pickDeviceAndQueue()) return false;
        if (!createDevice()) return false;
        if (!createSwapchain(width, height)) return false;
        if (!createRendererResources()) return false;
        if (!renderOneFrame()) return false;

        rendererCoreReady = true;
        object3dReady = true;
        syncDiagnostics();
        diagnostics.write("valid", "3D object path initialized: cube mesh, color+depth render pass, MVP push constants and object pipeline rendered.");
        status =
            "SOLUM Engine\nRenderer path: Android Native Vulkan\nGPU: " + diagnostics.gpuName +
            "\nType: " + diagnostics.gpuType +
            "\nAPI: " + diagnostics.apiVersion +
            "\nSwapchain: created\nRender pass: color+depth OK\nRenderer core: OK\n3D object: OK\nFrames rendered: " + std::to_string(framesRendered) +
            "\nNext: Material Foundation";

        return true;
    }
};

} // namespace solum
