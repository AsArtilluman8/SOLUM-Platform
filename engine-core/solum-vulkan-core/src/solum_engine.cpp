#define VK_USE_PLATFORM_ANDROID_KHR 1

#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <vulkan/vulkan.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <sys/stat.h>

struct SolumEngine {
    std::string outputRoot;
    std::string status = "SOLUM Engine\nNative object created";
    std::string gpuName = "unknown";
    std::string gpuType = "unknown";
    std::string apiVersion = "unknown";

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
    uint64_t framesRendered = 0;
    bool firstFrameRendered = false;
};

static std::string vkResultName(VkResult r) {
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
        default: {
            char b[64];
            std::snprintf(b, sizeof(b), "VkResult(%d)", (int)r);
            return b;
        }
    }
}

static const char* deviceTypeName(VkPhysicalDeviceType t) {
    switch (t) {
        case VK_PHYSICAL_DEVICE_TYPE_OTHER: return "OTHER";
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "INTEGRATED_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "DISCRETE_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "VIRTUAL_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_CPU: return "CPU";
        default: return "UNKNOWN";
    }
}

static void ensureDir(const std::string& path) {
    std::string cur;
    for (char c : path) {
        cur.push_back(c);
        if (c == '/') mkdir(cur.c_str(), 0775);
    }
    mkdir(path.c_str(), 0775);
}

static bool endsWith(const std::string& s, const std::string& suffix) {
    return s.size() >= suffix.size() && s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
}

static std::string escapeJson(const std::string& s) {
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

static std::string reportDirFor(SolumEngine* e) {
    std::string root = (e && !e->outputRoot.empty()) ? e->outputRoot : "/storage/emulated/0/SOLUMCreative";
    if (endsWith(root, "/diagnostics/latest") || endsWith(root, "/solum_diagnostics")) return root;
    return root + "/diagnostics/latest";
}

static void writeRuntimeReport(SolumEngine* e, const std::string& status, const std::string& reason) {
    if (!e) return;
    std::string dir = reportDirFor(e);
    ensureDir(dir + "/");
    std::string path = dir + "/runtime_vulkan_caps.json";
    std::ofstream f(path);
    f << "{\n";
    f << "  \"schema\": \"solum.runtime_vulkan_caps\",\n";
    f << "  \"schemaVersion\": 1,\n";
    f << "  \"status\": \"" << escapeJson(status) << "\",\n";
    f << "  \"reason\": \"" << escapeJson(reason) << "\",\n";
    f << "  \"rendererPath\": \"Android Native Vulkan\",\n";
    f << "  \"deviceName\": \"" << escapeJson(e->gpuName) << "\",\n";
    f << "  \"deviceType\": \"" << escapeJson(e->gpuType) << "\",\n";
    f << "  \"apiVersion\": \"" << escapeJson(e->apiVersion) << "\",\n";
    f << "  \"swapchainFormat\": " << (int)e->swapchainFormat << ",\n";
    f << "  \"extent\": { \"width\": " << e->extent.width << ", \"height\": " << e->extent.height << " },\n";
    f << "  \"framesRendered\": " << e->framesRendered << ",\n";
    f << "  \"firstFrameRendered\": " << (e->firstFrameRendered ? "true" : "false") << ",\n";
    f << "  \"note\": \"Termux Vulkan may show llvmpipe CPU. This runtime report is the Android APK path.\"\n";
    f << "}\n";
}

static void cleanupFrameResources(SolumEngine* e) {
    if (!e || e->device == VK_NULL_HANDLE) return;
    vkDeviceWaitIdle(e->device);

    if (e->inFlight != VK_NULL_HANDLE) { vkDestroyFence(e->device, e->inFlight, nullptr); e->inFlight = VK_NULL_HANDLE; }
    if (e->renderFinished != VK_NULL_HANDLE) { vkDestroySemaphore(e->device, e->renderFinished, nullptr); e->renderFinished = VK_NULL_HANDLE; }
    if (e->imageAvailable != VK_NULL_HANDLE) { vkDestroySemaphore(e->device, e->imageAvailable, nullptr); e->imageAvailable = VK_NULL_HANDLE; }

    for (VkFramebuffer fb : e->framebuffers) vkDestroyFramebuffer(e->device, fb, nullptr);
    e->framebuffers.clear();

    if (e->commandPool != VK_NULL_HANDLE) { vkDestroyCommandPool(e->device, e->commandPool, nullptr); e->commandPool = VK_NULL_HANDLE; }
    e->commandBuffers.clear();

    if (e->renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(e->device, e->renderPass, nullptr); e->renderPass = VK_NULL_HANDLE; }

    for (VkImageView view : e->swapchainImageViews) vkDestroyImageView(e->device, view, nullptr);
    e->swapchainImageViews.clear();
    e->swapchainImages.clear();
}

static void cleanupVulkan(SolumEngine* e) {
    if (!e) return;
    cleanupFrameResources(e);
    if (e->device != VK_NULL_HANDLE) vkDeviceWaitIdle(e->device);
    if (e->swapchain != VK_NULL_HANDLE) { vkDestroySwapchainKHR(e->device, e->swapchain, nullptr); e->swapchain = VK_NULL_HANDLE; }
    if (e->device != VK_NULL_HANDLE) { vkDestroyDevice(e->device, nullptr); e->device = VK_NULL_HANDLE; }
    if (e->surface != VK_NULL_HANDLE) { vkDestroySurfaceKHR(e->instance, e->surface, nullptr); e->surface = VK_NULL_HANDLE; }
    if (e->instance != VK_NULL_HANDLE) { vkDestroyInstance(e->instance, nullptr); e->instance = VK_NULL_HANDLE; }
    e->physicalDevice = VK_NULL_HANDLE;
    e->graphicsQueue = VK_NULL_HANDLE;
    e->graphicsQueueFamily = UINT32_MAX;
    e->swapchainFormat = VK_FORMAT_UNDEFINED;
    e->extent = {};
    e->firstFrameRendered = false;
    e->framesRendered = 0;
}

static bool createImageViews(SolumEngine* e) {
    uint32_t count = 0;
    VkResult r = vkGetSwapchainImagesKHR(e->device, e->swapchain, &count, nullptr);
    if (r != VK_SUCCESS || count == 0) {
        e->status = "SOLUM Engine\nSwapchain image query failed: " + vkResultName(r);
        return false;
    }
    e->swapchainImages.resize(count);
    vkGetSwapchainImagesKHR(e->device, e->swapchain, &count, e->swapchainImages.data());
    e->swapchainImageViews.resize(count, VK_NULL_HANDLE);

    for (uint32_t i = 0; i < count; ++i) {
        VkImageViewCreateInfo ci{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
        ci.image = e->swapchainImages[i];
        ci.viewType = VK_IMAGE_VIEW_TYPE_2D;
        ci.format = e->swapchainFormat;
        ci.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        ci.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        ci.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        ci.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        ci.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        ci.subresourceRange.baseMipLevel = 0;
        ci.subresourceRange.levelCount = 1;
        ci.subresourceRange.baseArrayLayer = 0;
        ci.subresourceRange.layerCount = 1;
        r = vkCreateImageView(e->device, &ci, nullptr, &e->swapchainImageViews[i]);
        if (r != VK_SUCCESS) {
            e->status = "SOLUM Engine\nImageView failed: " + vkResultName(r);
            return false;
        }
    }
    return true;
}

static bool createRenderPass(SolumEngine* e) {
    VkAttachmentDescription color{};
    color.format = e->swapchainFormat;
    color.samples = VK_SAMPLE_COUNT_1_BIT;
    color.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    color.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    color.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    color.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    color.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    color.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentReference colorRef{};
    colorRef.attachment = 0;
    colorRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorRef;

    VkSubpassDependency dep{};
    dep.srcSubpass = VK_SUBPASS_EXTERNAL;
    dep.dstSubpass = 0;
    dep.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dep.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dep.srcAccessMask = 0;
    dep.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;

    VkRenderPassCreateInfo ci{ VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO };
    ci.attachmentCount = 1;
    ci.pAttachments = &color;
    ci.subpassCount = 1;
    ci.pSubpasses = &subpass;
    ci.dependencyCount = 1;
    ci.pDependencies = &dep;

    VkResult r = vkCreateRenderPass(e->device, &ci, nullptr, &e->renderPass);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nRenderPass failed: " + vkResultName(r);
        return false;
    }
    return true;
}

static bool createFramebuffers(SolumEngine* e) {
    e->framebuffers.resize(e->swapchainImageViews.size(), VK_NULL_HANDLE);
    for (size_t i = 0; i < e->swapchainImageViews.size(); ++i) {
        VkImageView attachments[] = { e->swapchainImageViews[i] };
        VkFramebufferCreateInfo ci{ VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO };
        ci.renderPass = e->renderPass;
        ci.attachmentCount = 1;
        ci.pAttachments = attachments;
        ci.width = e->extent.width;
        ci.height = e->extent.height;
        ci.layers = 1;
        VkResult r = vkCreateFramebuffer(e->device, &ci, nullptr, &e->framebuffers[i]);
        if (r != VK_SUCCESS) {
            e->status = "SOLUM Engine\nFramebuffer failed: " + vkResultName(r);
            return false;
        }
    }
    return true;
}

static bool createCommandsAndSync(SolumEngine* e) {
    VkCommandPoolCreateInfo pool{ VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO };
    pool.queueFamilyIndex = e->graphicsQueueFamily;
    pool.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    VkResult r = vkCreateCommandPool(e->device, &pool, nullptr, &e->commandPool);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nCommandPool failed: " + vkResultName(r);
        return false;
    }

    e->commandBuffers.resize(e->framebuffers.size(), VK_NULL_HANDLE);
    VkCommandBufferAllocateInfo ai{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO };
    ai.commandPool = e->commandPool;
    ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    ai.commandBufferCount = (uint32_t)e->commandBuffers.size();
    r = vkAllocateCommandBuffers(e->device, &ai, e->commandBuffers.data());
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nCommandBuffers failed: " + vkResultName(r);
        return false;
    }

    VkSemaphoreCreateInfo si{ VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO };
    if ((r = vkCreateSemaphore(e->device, &si, nullptr, &e->imageAvailable)) != VK_SUCCESS) {
        e->status = "SOLUM Engine\nImageAvailable semaphore failed: " + vkResultName(r);
        return false;
    }
    if ((r = vkCreateSemaphore(e->device, &si, nullptr, &e->renderFinished)) != VK_SUCCESS) {
        e->status = "SOLUM Engine\nRenderFinished semaphore failed: " + vkResultName(r);
        return false;
    }

    VkFenceCreateInfo fi{ VK_STRUCTURE_TYPE_FENCE_CREATE_INFO };
    fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
    if ((r = vkCreateFence(e->device, &fi, nullptr, &e->inFlight)) != VK_SUCCESS) {
        e->status = "SOLUM Engine\nFence failed: " + vkResultName(r);
        return false;
    }
    return true;
}

static bool recordClearCommand(SolumEngine* e, uint32_t imageIndex) {
    VkCommandBuffer cmd = e->commandBuffers[imageIndex];
    VkCommandBufferBeginInfo bi{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO };
    bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    VkResult r = vkBeginCommandBuffer(cmd, &bi);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nBegin command buffer failed: " + vkResultName(r);
        return false;
    }

    VkClearValue clear{};
    clear.color.float32[0] = 0.02f;
    clear.color.float32[1] = 0.07f;
    clear.color.float32[2] = 0.09f;
    clear.color.float32[3] = 1.0f;

    VkRenderPassBeginInfo rp{ VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO };
    rp.renderPass = e->renderPass;
    rp.framebuffer = e->framebuffers[imageIndex];
    rp.renderArea.offset = {0, 0};
    rp.renderArea.extent = e->extent;
    rp.clearValueCount = 1;
    rp.pClearValues = &clear;

    vkCmdBeginRenderPass(cmd, &rp, VK_SUBPASS_CONTENTS_INLINE);
    vkCmdEndRenderPass(cmd);

    r = vkEndCommandBuffer(cmd);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nEnd command buffer failed: " + vkResultName(r);
        return false;
    }
    return true;
}

static bool renderOneFrame(SolumEngine* e) {
    if (!e || e->device == VK_NULL_HANDLE || e->swapchain == VK_NULL_HANDLE || e->commandBuffers.empty()) return false;

    vkWaitForFences(e->device, 1, &e->inFlight, VK_TRUE, UINT64_MAX);
    vkResetFences(e->device, 1, &e->inFlight);

    uint32_t imageIndex = 0;
    VkResult r = vkAcquireNextImageKHR(e->device, e->swapchain, UINT64_MAX, e->imageAvailable, VK_NULL_HANDLE, &imageIndex);
    if (r == VK_ERROR_OUT_OF_DATE_KHR) {
        e->status = "SOLUM Engine\nSwapchain out of date before frame.";
        return false;
    }
    if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR) {
        e->status = "SOLUM Engine\nAcquire image failed: " + vkResultName(r);
        return false;
    }

    vkResetCommandBuffer(e->commandBuffers[imageIndex], 0);
    if (!recordClearCommand(e, imageIndex)) return false;

    VkPipelineStageFlags waitStages[] = { VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT };
    VkSubmitInfo submit{ VK_STRUCTURE_TYPE_SUBMIT_INFO };
    submit.waitSemaphoreCount = 1;
    submit.pWaitSemaphores = &e->imageAvailable;
    submit.pWaitDstStageMask = waitStages;
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &e->commandBuffers[imageIndex];
    submit.signalSemaphoreCount = 1;
    submit.pSignalSemaphores = &e->renderFinished;

    r = vkQueueSubmit(e->graphicsQueue, 1, &submit, e->inFlight);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nQueue submit failed: " + vkResultName(r);
        return false;
    }

    VkPresentInfoKHR present{ VK_STRUCTURE_TYPE_PRESENT_INFO_KHR };
    present.waitSemaphoreCount = 1;
    present.pWaitSemaphores = &e->renderFinished;
    present.swapchainCount = 1;
    present.pSwapchains = &e->swapchain;
    present.pImageIndices = &imageIndex;

    r = vkQueuePresentKHR(e->graphicsQueue, &present);
    if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR && r != VK_ERROR_OUT_OF_DATE_KHR) {
        e->status = "SOLUM Engine\nPresent failed: " + vkResultName(r);
        return false;
    }

    e->framesRendered += 1;
    e->firstFrameRendered = true;
    return true;
}

static bool createFrameLoopResources(SolumEngine* e) {
    if (!createImageViews(e)) return false;
    if (!createRenderPass(e)) return false;
    if (!createFramebuffers(e)) return false;
    if (!createCommandsAndSync(e)) return false;
    return true;
}

static bool initVulkan(SolumEngine* e, ANativeWindow* window, int width, int height) {
    cleanupVulkan(e);
    e->status = "SOLUM Engine\nInitializing Android Native Vulkan...";

    const char* instanceExts[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
    VkApplicationInfo app{ VK_STRUCTURE_TYPE_APPLICATION_INFO };
    app.pApplicationName = "SOLUM Engine";
    app.applicationVersion = VK_MAKE_VERSION(0, 5, 0);
    app.pEngineName = "SOLUM";
    app.engineVersion = VK_MAKE_VERSION(0, 5, 0);
    app.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo ici{ VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO };
    ici.pApplicationInfo = &app;
    ici.enabledExtensionCount = 2;
    ici.ppEnabledExtensionNames = instanceExts;

    VkResult r = vkCreateInstance(&ici, nullptr, &e->instance);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nVulkan instance failed: " + vkResultName(r);
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    VkAndroidSurfaceCreateInfoKHR sci{ VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR };
    sci.window = window;
    r = vkCreateAndroidSurfaceKHR(e->instance, &sci, nullptr, &e->surface);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nAndroid surface failed: " + vkResultName(r);
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    uint32_t deviceCount = 0;
    r = vkEnumeratePhysicalDevices(e->instance, &deviceCount, nullptr);
    if (r != VK_SUCCESS || deviceCount == 0) {
        e->status = "SOLUM Engine\nNo Vulkan physical device: " + vkResultName(r);
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(e->instance, &deviceCount, devices.data());

    for (auto pd : devices) {
        uint32_t qCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, nullptr);
        std::vector<VkQueueFamilyProperties> qProps(qCount);
        vkGetPhysicalDeviceQueueFamilyProperties(pd, &qCount, qProps.data());
        for (uint32_t i = 0; i < qCount; ++i) {
            VkBool32 present = VK_FALSE;
            vkGetPhysicalDeviceSurfaceSupportKHR(pd, i, e->surface, &present);
            if ((qProps[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && present) {
                e->physicalDevice = pd;
                e->graphicsQueueFamily = i;
                break;
            }
        }
        if (e->physicalDevice != VK_NULL_HANDLE) break;
    }

    if (e->physicalDevice == VK_NULL_HANDLE) {
        e->status = "SOLUM Engine\nNo graphics+present queue.";
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    VkPhysicalDeviceProperties props{};
    vkGetPhysicalDeviceProperties(e->physicalDevice, &props);
    e->gpuName = props.deviceName;
    e->gpuType = deviceTypeName(props.deviceType);
    char api[32];
    std::snprintf(api, sizeof(api), "%u.%u.%u", VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion), VK_VERSION_PATCH(props.apiVersion));
    e->apiVersion = api;

    const float prio = 1.0f;
    VkDeviceQueueCreateInfo qci{ VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO };
    qci.queueFamilyIndex = e->graphicsQueueFamily;
    qci.queueCount = 1;
    qci.pQueuePriorities = &prio;
    const char* deviceExts[] = { "VK_KHR_swapchain" };
    VkDeviceCreateInfo dci{ VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO };
    dci.queueCreateInfoCount = 1;
    dci.pQueueCreateInfos = &qci;
    dci.enabledExtensionCount = 1;
    dci.ppEnabledExtensionNames = deviceExts;
    r = vkCreateDevice(e->physicalDevice, &dci, nullptr, &e->device);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nDevice create failed: " + vkResultName(r);
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }
    vkGetDeviceQueue(e->device, e->graphicsQueueFamily, 0, &e->graphicsQueue);

    VkSurfaceCapabilitiesKHR caps{};
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(e->physicalDevice, e->surface, &caps);
    uint32_t fmtCount = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(e->physicalDevice, e->surface, &fmtCount, nullptr);
    std::vector<VkSurfaceFormatKHR> formats(fmtCount);
    vkGetPhysicalDeviceSurfaceFormatsKHR(e->physicalDevice, e->surface, &fmtCount, formats.data());
    VkSurfaceFormatKHR chosen = formats.empty() ? VkSurfaceFormatKHR{VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR} : formats[0];
    for (const auto& f : formats) {
        if (f.format == VK_FORMAT_R8G8B8A8_UNORM || f.format == VK_FORMAT_B8G8R8A8_UNORM) { chosen = f; break; }
    }
    e->swapchainFormat = chosen.format;
    e->extent.width = caps.currentExtent.width == 0xFFFFFFFF ? (uint32_t)width : caps.currentExtent.width;
    e->extent.height = caps.currentExtent.height == 0xFFFFFFFF ? (uint32_t)height : caps.currentExtent.height;
    if (e->extent.width == 0 || e->extent.height == 0) {
        e->status = "SOLUM Engine\nInvalid surface extent.";
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }
    uint32_t imageCount = caps.minImageCount + 1;
    if (caps.maxImageCount > 0 && imageCount > caps.maxImageCount) imageCount = caps.maxImageCount;

    VkSwapchainCreateInfoKHR sw{ VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR };
    sw.surface = e->surface;
    sw.minImageCount = imageCount;
    sw.imageFormat = chosen.format;
    sw.imageColorSpace = chosen.colorSpace;
    sw.imageExtent = e->extent;
    sw.imageArrayLayers = 1;
    sw.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    sw.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    sw.preTransform = caps.currentTransform;
    sw.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    sw.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    sw.clipped = VK_TRUE;
    r = vkCreateSwapchainKHR(e->device, &sw, nullptr, &e->swapchain);
    if (r != VK_SUCCESS) {
        e->status = "SOLUM Engine\nSwapchain failed: " + vkResultName(r);
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    if (!createFrameLoopResources(e)) {
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    bool frameOk = renderOneFrame(e);
    if (!frameOk) {
        writeRuntimeReport(e, "failed", e->status);
        return false;
    }

    e->status = "SOLUM Engine\nRenderer path: Android Native Vulkan\nGPU: " + e->gpuName + "\nType: " + e->gpuType + "\nAPI: " + e->apiVersion + "\nSwapchain: created\nRender pass: clear color OK\nFrames rendered: " + std::to_string(e->framesRendered) + "\nNext: pipeline + first triangle draw";
    writeRuntimeReport(e, "valid", "Android native Vulkan initialized; swapchain/render pass/frame clear/present succeeded.");
    return true;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_solum_engine_MainActivity_nativeCreate(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(new SolumEngine());
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    auto* e = reinterpret_cast<SolumEngine*>(handle);
    cleanupVulkan(e);
    delete e;
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceCreated(JNIEnv* env, jclass, jlong handle, jobject surface, jstring outputRoot) {
    auto* e = reinterpret_cast<SolumEngine*>(handle);
    const char* root = env->GetStringUTFChars(outputRoot, nullptr);
    e->outputRoot = root ? root : "/storage/emulated/0/SOLUMCreative";
    env->ReleaseStringUTFChars(outputRoot, root);
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    if (!win) {
        e->status = "SOLUM Engine\nANativeWindow_fromSurface failed.";
        writeRuntimeReport(e, "failed", e->status);
        return;
    }
    int w = ANativeWindow_getWidth(win);
    int h = ANativeWindow_getHeight(win);
    initVulkan(e, win, w, h);
    ANativeWindow_release(win);
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceChanged(JNIEnv* env, jclass, jlong handle, jobject surface, jint width, jint height) {
    auto* e = reinterpret_cast<SolumEngine*>(handle);
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    if (win) {
        initVulkan(e, win, width, height);
        ANativeWindow_release(win);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*, jclass, jlong handle) {
    auto* e = reinterpret_cast<SolumEngine*>(handle);
    cleanupVulkan(e);
    e->status = "SOLUM Engine\nSurface destroyed. Vulkan cleaned.";
}

extern "C" JNIEXPORT jstring JNICALL Java_com_solum_engine_MainActivity_nativeGetStatus(JNIEnv* env, jclass, jlong handle) {
    auto* e = reinterpret_cast<SolumEngine*>(handle);
    return env->NewStringUTF(e ? e->status.c_str() : "SOLUM Engine\nNo native handle");
}
