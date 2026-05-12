#pragma once
#include "pipeline_bundle.hpp"
#include "runtime_diagnostics.hpp"
#include "texture_resource.hpp"
#include <algorithm>

namespace solum {

enum PbrTextureKind {
    PbrTextureBaseColor = 0,
    PbrTextureMetallicRoughness = 1,
    PbrTextureNormal = 2,
    PbrTextureOcclusion = 3
};

struct PbrMaterialTextureSet {
    TextureResource baseColor;
    TextureResource metallicRoughness;
    TextureResource normal;
    TextureResource occlusion;
    bool baseColorUploaded = false;
    bool metallicRoughnessUploaded = false;
    bool normalUploaded = false;
    bool occlusionUploaded = false;

    void destroy() {
        baseColor.destroy();
        metallicRoughness.destroy();
        normal.destroy();
        occlusion.destroy();
        baseColorUploaded = false;
        metallicRoughnessUploaded = false;
        normalUploaded = false;
        occlusionUploaded = false;
    }

    TextureResource& texture(int kind) {
        if (kind == PbrTextureMetallicRoughness) return metallicRoughness;
        if (kind == PbrTextureNormal) return normal;
        if (kind == PbrTextureOcclusion) return occlusion;
        return baseColor;
    }

    bool uploaded(int kind) const {
        if (kind == PbrTextureMetallicRoughness) return metallicRoughnessUploaded;
        if (kind == PbrTextureNormal) return normalUploaded;
        if (kind == PbrTextureOcclusion) return occlusionUploaded;
        return baseColorUploaded;
    }

    void setUploaded(int kind, bool value) {
        if (kind == PbrTextureMetallicRoughness) metallicRoughnessUploaded = value;
        else if (kind == PbrTextureNormal) normalUploaded = value;
        else if (kind == PbrTextureOcclusion) occlusionUploaded = value;
        else baseColorUploaded = value;
    }
};

struct RendererCore {
    std::string outputRoot;
    std::string status = "SOLUM Engine\nNative object created";
    RuntimeDiagnostics diagnostics;

    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkQueue graphicsQueue = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamily = UINT32_MAX;
    VkFormat swapchainFormat = VK_FORMAT_UNDEFINED;
    VkFormat depthFormat = VK_FORMAT_UNDEFINED;
    VkExtent2D extent{};

    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    VkRenderPass renderPass = VK_NULL_HANDLE;
    std::vector<VkFramebuffer> framebuffers;
    VkImage depthImage = VK_NULL_HANDLE;
    VkDeviceMemory depthMemory = VK_NULL_HANDLE;
    VkImageView depthImageView = VK_NULL_HANDLE;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> commandBuffers;
    VkSemaphore imageAvailable = VK_NULL_HANDLE;
    VkSemaphore renderFinished = VK_NULL_HANDLE;
    VkFence inFlight = VK_NULL_HANDLE;

    MeshResource validationMesh;
    MeshResource modelMesh;
    TextureResource baseColorTexture;
    std::vector<PrimitiveDrawRange> modelDrawRanges;
    std::vector<MaterialSlotState> modelMaterialSlots;
    std::vector<PbrMaterialTextureSet> materialTextureSets;
    PipelineBundle trianglePipeline;
    VkDescriptorPool textureDescriptorPool = VK_NULL_HANDLE;
    std::vector<VkDescriptorSet> textureDescriptorSets;
    uint64_t framesRendered = 0;
    bool firstFrameRendered = false;
    bool triangleDrawn = false;
    bool cubeDrawn = false;
    bool depthReady = false;
    bool cameraReady = false;
    bool cameraMvpReady = false;
    bool cameraControlsReady = false;
    bool pushConstantsReady = false;
    bool materialConstantsReady = false;
    bool meshAttributeLayoutReady = false;
    bool rendererCoreReady = false;
    CameraState camera;
    MaterialConstants material;
    ModelRenderState model;

    void setOutputRoot(const std::string& root) { outputRoot = root; diagnostics.outputRoot = root; }

    void syncLightingModelState() {
        for (int i = 0; i < 3; ++i) {
            model.sunDirection[i] = material.sunDirection[i];
            model.sunColor[i] = material.sunColor[i];
            model.ambientColor[i] = material.ambientColor[i];
        }
        model.sunIntensity = material.sunIntensity;
        model.ambientIntensity = material.ambientIntensity;
        model.lightPreset = lightPresetName(material.lightPreset);
        model.toneMappingMode = toneMappingModeName(material.toneMappingMode);
        model.activeDebugView = materialDebugViewName(material.activeDebugView);
        model.exposureValue = material.exposureValue;
        model.ambientFloor = material.ambientFloor;
        model.brightnessPreset = brightnessPresetName(material.brightnessPreset);
        model.specularBoost = material.specularBoost;
        model.lightingStatus = "ok";
        model.lightingControlStatus = "ok";
        model.lightingUiMode = "compact_step_controls";
        model.brdfStatus = "ok";
        model.brdfMode = "direct_lighting_schlick_mobile";
        model.diffuseStatus = "ok_non_metal_diffuse";
        model.specularStatus = "ok_roughness_view_dependent_boosted";
        model.specularBoostStatus = "ok_uniform_controlled";
        model.reflectionFoundationStatus = "analytic_specular_only";
        model.reflectionMode = "analytic_view_dependent";
        model.environmentReflectionStatus = "not_yet_real_ibl";
        model.lightingUniformUpdateStatus = "ok_uniform_only";
        model.sliderUpdateMode = "uniform_only";
        model.fresnelStatus = "ok_schlick";
        model.f0Status = "ok_dielectric_0_04_metal_base_color";
        model.metallicResponseStatus = "ok_diffuse_reduced_f0_tinted";
        model.roughnessResponseStatus = "ok_highlight_width_intensity";
        model.directLightingStatus = "ok_single_sun_direct";
        model.materialResponseStatus = "brdf_direct_lit";
        model.pbrQualityTier = "mobile_direct_lighting";
        model.brdfPerformanceStatus = "ok_mobile_friendly_direct_lighting";
        model.toneMappingStatus = "ok";
        model.debugViewStatus = "shader_applied";
        model.exposureStatus = "ok";
        model.normalDebugViewStatus = "shader_applied";
        model.ndotlDebugViewStatus = "shader_applied";
        model.diffuseDebugViewStatus = "shader_applied";
        model.specularDebugViewStatus = "shader_applied";
        model.f0DebugViewStatus = "shader_applied";
        model.brdfStatusDebugViewStatus = "shader_applied";
    }

    void syncDiagnostics() {
        diagnostics.gpuName = diagnostics.gpuName.empty() ? "unknown" : diagnostics.gpuName;
        diagnostics.swapchainFormat = swapchainFormat;
        diagnostics.extent = extent;
        diagnostics.rendererCoreReady = rendererCoreReady;
        diagnostics.vertexBufferReady = validationMesh.ready();
        diagnostics.indexBufferReady = validationMesh.indexedReady();
        diagnostics.cubeReady = cubeDrawn && validationMesh.indexedReady();
        diagnostics.depthReady = depthReady;
        diagnostics.cameraReady = cameraMvpReady && cameraControlsReady;
        diagnostics.cameraMvpReady = cameraMvpReady;
        diagnostics.cameraControlsReady = cameraControlsReady;
        diagnostics.uniformOrPushConstantsReady = pushConstantsReady;
        diagnostics.materialConstantsReady = materialConstantsReady;
        diagnostics.meshAttributeLayoutReady = meshAttributeLayoutReady;
        diagnostics.vertexCount = validationMesh.vertexCount;
        diagnostics.indexCount = validationMesh.indexCount;
        diagnostics.vertexStrideBytes = sizeof(Vertex3D);
        diagnostics.camera = camera;
        diagnostics.material = material;
        diagnostics.model = model;
        diagnostics.framesRendered = framesRendered;
        diagnostics.triangleDrawn = triangleDrawn;
        diagnostics.renderLab.cubeReady = diagnostics.cubeReady;
        diagnostics.renderLab.depthReady = diagnostics.depthReady;
        diagnostics.renderLab.cameraReady = diagnostics.cameraReady;
        diagnostics.renderLab.cameraMvpReady = diagnostics.cameraMvpReady;
        diagnostics.renderLab.cameraControlsReady = diagnostics.cameraControlsReady;
        diagnostics.renderLab.cameraYawDeg = camera.yawDeg;
        diagnostics.renderLab.cameraPitchDeg = camera.pitchDeg;
        diagnostics.renderLab.cameraDistance = camera.distance;
        diagnostics.renderLab.indexBufferReady = diagnostics.indexBufferReady;
        diagnostics.renderLab.uniformOrPushConstantsReady = diagnostics.uniformOrPushConstantsReady;
        diagnostics.renderLab.materialConstantsReady = diagnostics.materialConstantsReady;
        diagnostics.renderLab.meshAttributeLayoutReady = diagnostics.meshAttributeLayoutReady;
        diagnostics.renderLab.vertexCount = diagnostics.vertexCount;
        diagnostics.renderLab.indexCount = diagnostics.indexCount;
        diagnostics.renderLab.vertexStrideBytes = diagnostics.vertexStrideBytes;
        diagnostics.renderLab.material = material;
        diagnostics.renderLab.model = model;
        diagnostics.renderLab.framesRendered = diagnostics.framesRendered;
    }

    void syncTextureMaterialState() {
        material.baseColorTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].baseColorUploaded) ? 1 : 0;
        material.metallicRoughnessTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].metallicRoughnessUploaded) ? 1 : 0;
        material.normalTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].normalUploaded) ? 1 : 0;
        material.occlusionTextureReady = (!materialTextureSets.empty() && materialTextureSets[0].occlusionUploaded) ? 1 : 0;
    }

    void applyLightPreset(int preset) {
        material.lightPreset = ((preset % 5) + 5) % 5;
        if (material.lightPreset == 1) {
            material.sunDirection[0] = -0.35f; material.sunDirection[1] = -0.82f; material.sunDirection[2] = -0.45f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 1.55f;
            material.ambientColor[0] = 0.42f; material.ambientColor[1] = 0.52f; material.ambientColor[2] = 0.62f;
            material.ambientIntensity = 0.46f;
            material.exposureValue = 1.18f;
            material.ambientFloor = 0.10f;
            material.brightnessPreset = 1;
            material.specularBoost = 1.20f;
        } else if (material.lightPreset == 2) {
            material.sunDirection[0] = -0.42f; material.sunDirection[1] = -0.72f; material.sunDirection[2] = -0.55f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.92f; material.sunColor[2] = 0.78f;
            material.sunIntensity = 1.65f;
            material.ambientColor[0] = 0.44f; material.ambientColor[1] = 0.55f; material.ambientColor[2] = 0.72f;
            material.ambientIntensity = 0.38f;
            material.exposureValue = 1.10f;
            material.ambientFloor = 0.08f;
            material.brightnessPreset = 1;
            material.specularBoost = 1.35f;
        } else if (material.lightPreset == 3) {
            material.sunDirection[0] = -0.35f; material.sunDirection[1] = -0.82f; material.sunDirection[2] = -0.45f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 2.0f;
            material.ambientColor[0] = 0.50f; material.ambientColor[1] = 0.58f; material.ambientColor[2] = 0.68f;
            material.ambientIntensity = 0.80f;
            material.exposureValue = 1.50f;
            material.ambientFloor = 0.16f;
            material.brightnessPreset = 3;
            material.specularBoost = 1.60f;
        } else if (material.lightPreset == 4) {
            material.sunDirection[0] = -0.30f; material.sunDirection[1] = -0.80f; material.sunDirection[2] = -0.42f;
            material.sunColor[0] = 1.0f; material.sunColor[1] = 0.96f; material.sunColor[2] = 0.88f;
            material.sunIntensity = 3.35f;
            material.ambientColor[0] = 0.55f; material.ambientColor[1] = 0.62f; material.ambientColor[2] = 0.72f;
            material.ambientIntensity = 1.25f;
            material.exposureValue = 2.25f;
            material.ambientFloor = 0.22f;
            material.brightnessPreset = 4;
            material.specularBoost = 2.35f;
        } else {
            material.sunDirection[0] = -0.25f; material.sunDirection[1] = -0.88f; material.sunDirection[2] = -0.32f;
            material.sunColor[0] = 0.92f; material.sunColor[1] = 0.96f; material.sunColor[2] = 1.0f;
            material.sunIntensity = 1.05f;
            material.ambientColor[0] = 0.62f; material.ambientColor[1] = 0.68f; material.ambientColor[2] = 0.76f;
            material.ambientIntensity = 0.58f;
            material.exposureValue = 1.32f;
            material.ambientFloor = 0.14f;
            material.brightnessPreset = 2;
            material.specularBoost = 1.10f;
        }
        syncLightingModelState();
    }

    bool setLightingControls(int lightPreset, float sunIntensity, float ambientIntensity, int activeDebugView, int toneMappingMode, float exposureValue, float ambientFloor, int brightnessPreset, float specularBoost) {
        applyLightPreset(lightPreset);
        material.sunIntensity = clampFloat(sunIntensity, 0.5f, 4.0f);
        material.ambientIntensity = clampFloat(ambientIntensity, 0.1f, 2.0f);
        material.activeDebugView = ((activeDebugView % 10) + 10) % 10;
        material.toneMappingMode = ((toneMappingMode % 3) + 3) % 3;
        material.exposureValue = clampFloat(exposureValue, 0.8f, 3.0f);
        material.ambientFloor = clampFloat(ambientFloor, 0.0f, 0.35f);
        material.brightnessPreset = ((brightnessPreset % 5) + 5) % 5;
        material.specularBoost = clampFloat(specularBoost, 0.5f, 3.0f);
        syncLightingModelState();
        const bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write(ok ? "valid" : diagnostics.status, ok ? "Scene10 lighting controls updated uniforms only." : diagnostics.reason);
        updateReadyStatus();
        return ok;
    }

    void fail(const std::string& reason) {
        status = "SOLUM Engine\n" + reason;
        syncDiagnostics();
        diagnostics.write("failed", reason);
    }

    void destroyFrameResources() {
        if (device == VK_NULL_HANDLE) return;
        vkDeviceWaitIdle(device);
        if (textureDescriptorPool != VK_NULL_HANDLE) { vkDestroyDescriptorPool(device, textureDescriptorPool, nullptr); textureDescriptorPool = VK_NULL_HANDLE; }
        textureDescriptorSets.clear();
        for (auto& set : materialTextureSets) set.destroy();
        materialTextureSets.clear();
        baseColorTexture.destroy();
        modelMesh.destroy();
        modelDrawRanges.clear();
        modelMaterialSlots.clear();
        validationMesh.destroy();
        trianglePipeline.destroy();
        if (inFlight != VK_NULL_HANDLE) { vkDestroyFence(device, inFlight, nullptr); inFlight = VK_NULL_HANDLE; }
        if (renderFinished != VK_NULL_HANDLE) { vkDestroySemaphore(device, renderFinished, nullptr); renderFinished = VK_NULL_HANDLE; }
        if (imageAvailable != VK_NULL_HANDLE) { vkDestroySemaphore(device, imageAvailable, nullptr); imageAvailable = VK_NULL_HANDLE; }
        for (VkFramebuffer fb : framebuffers) vkDestroyFramebuffer(device, fb, nullptr);
        framebuffers.clear();
        if (depthImageView != VK_NULL_HANDLE) { vkDestroyImageView(device, depthImageView, nullptr); depthImageView = VK_NULL_HANDLE; }
        if (depthImage != VK_NULL_HANDLE) { vkDestroyImage(device, depthImage, nullptr); depthImage = VK_NULL_HANDLE; }
        if (depthMemory != VK_NULL_HANDLE) { vkFreeMemory(device, depthMemory, nullptr); depthMemory = VK_NULL_HANDLE; }
        depthFormat = VK_FORMAT_UNDEFINED;
        depthReady = false;
        if (commandPool != VK_NULL_HANDLE) { vkDestroyCommandPool(device, commandPool, nullptr); commandPool = VK_NULL_HANDLE; }
        commandBuffers.clear();
        if (renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(device, renderPass, nullptr); renderPass = VK_NULL_HANDLE; }
        for (VkImageView view : swapchainImageViews) vkDestroyImageView(device, view, nullptr);
        swapchainImageViews.clear();
        swapchainImages.clear();
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
        firstFrameRendered = false;
        triangleDrawn = false;
        cubeDrawn = false;
        depthReady = false;
        cameraReady = false;
        cameraMvpReady = false;
        cameraControlsReady = false;
        pushConstantsReady = false;
        materialConstantsReady = false;
        meshAttributeLayoutReady = false;
        rendererCoreReady = false;
        model = ModelRenderState{};
    }

    bool createInstance() {
        const char* instanceExts[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
        VkApplicationInfo app{ VK_STRUCTURE_TYPE_APPLICATION_INFO };
        app.pApplicationName = "SOLUM Engine";
        app.applicationVersion = VK_MAKE_VERSION(0, 9, 0);
        app.pEngineName = "SOLUM";
        app.engineVersion = VK_MAKE_VERSION(0, 9, 0);
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
                if ((qProps[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && present) { physicalDevice = pd; graphicsQueueFamily = i; break; }
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
        for (const auto& f : formats) { if (f.format == VK_FORMAT_R8G8B8A8_UNORM || f.format == VK_FORMAT_B8G8R8A8_UNORM) { chosen = f; break; } }
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
            ci.subresourceRange.levelCount = 1;
            ci.subresourceRange.layerCount = 1;
            r = vkCreateImageView(device, &ci, nullptr, &swapchainImageViews[i]);
            if (r != VK_SUCCESS) { fail("ImageView failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    bool createRenderPass() {
        depthFormat = chooseDepthFormat();
        if (depthFormat == VK_FORMAT_UNDEFINED) { fail("Depth format selection failed"); return false; }
        VkAttachmentDescription color{};
        color.format = swapchainFormat;
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
        VkAttachmentDescription depth{};
        depth.format = depthFormat;
        depth.samples = VK_SAMPLE_COUNT_1_BIT;
        depth.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        depth.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        depth.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
        depth.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        depth.finalLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        VkAttachmentReference depthRef{};
        depthRef.attachment = 1;
        depthRef.layout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        VkSubpassDescription subpass{};
        subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
        subpass.colorAttachmentCount = 1;
        subpass.pColorAttachments = &colorRef;
        subpass.pDepthStencilAttachment = &depthRef;
        VkSubpassDependency dep{};
        dep.srcSubpass = VK_SUBPASS_EXTERNAL;
        dep.dstSubpass = 0;
        dep.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        dep.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
        dep.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
        VkAttachmentDescription attachments[] = { color, depth };
        VkRenderPassCreateInfo ci{ VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO };
        ci.attachmentCount = 2;
        ci.pAttachments = attachments;
        ci.subpassCount = 1;
        ci.pSubpasses = &subpass;
        ci.dependencyCount = 1;
        ci.pDependencies = &dep;
        VkResult r = vkCreateRenderPass(device, &ci, nullptr, &renderPass);
        if (r != VK_SUCCESS) { fail("RenderPass failed: " + vkResultName(r)); return false; }
        return true;
    }

    VkFormat chooseDepthFormat() {
        const VkFormat candidates[] = {
            VK_FORMAT_D24_UNORM_S8_UINT,
            VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D16_UNORM
        };
        for (VkFormat candidate : candidates) {
            VkFormatProperties props{};
            vkGetPhysicalDeviceFormatProperties(physicalDevice, candidate, &props);
            if (props.optimalTilingFeatures & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) return candidate;
        }
        return VK_FORMAT_UNDEFINED;
    }

    bool createDepthResources() {
        VkImageCreateInfo image{ VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO };
        image.imageType = VK_IMAGE_TYPE_2D;
        image.format = depthFormat;
        image.extent = { extent.width, extent.height, 1 };
        image.mipLevels = 1;
        image.arrayLayers = 1;
        image.samples = VK_SAMPLE_COUNT_1_BIT;
        image.tiling = VK_IMAGE_TILING_OPTIMAL;
        image.usage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        image.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        image.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        VkResult r = vkCreateImage(device, &image, nullptr, &depthImage);
        if (r != VK_SUCCESS) { fail("Depth image failed: " + vkResultName(r)); return false; }
        VkMemoryRequirements req{};
        vkGetImageMemoryRequirements(device, depthImage, &req);
        uint32_t memoryType = 0;
        if (!GpuBuffer::findMemoryType(physicalDevice, req.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &memoryType)) {
            fail("Depth device-local memory type not found");
            return false;
        }
        VkMemoryAllocateInfo alloc{ VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO };
        alloc.allocationSize = req.size;
        alloc.memoryTypeIndex = memoryType;
        r = vkAllocateMemory(device, &alloc, nullptr, &depthMemory);
        if (r != VK_SUCCESS) { fail("Depth memory failed: " + vkResultName(r)); return false; }
        r = vkBindImageMemory(device, depthImage, depthMemory, 0);
        if (r != VK_SUCCESS) { fail("Depth bind failed: " + vkResultName(r)); return false; }
        VkImageViewCreateInfo view{ VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO };
        view.image = depthImage;
        view.viewType = VK_IMAGE_VIEW_TYPE_2D;
        view.format = depthFormat;
        view.subresourceRange.aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
        view.subresourceRange.levelCount = 1;
        view.subresourceRange.layerCount = 1;
        r = vkCreateImageView(device, &view, nullptr, &depthImageView);
        if (r != VK_SUCCESS) { fail("Depth image view failed: " + vkResultName(r)); return false; }
        depthReady = true;
        return true;
    }

    bool createFramebuffers() {
        framebuffers.resize(swapchainImageViews.size(), VK_NULL_HANDLE);
        for (size_t i = 0; i < swapchainImageViews.size(); ++i) {
            VkImageView attachments[] = { swapchainImageViews[i], depthImageView };
            VkFramebufferCreateInfo ci{ VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO };
            ci.renderPass = renderPass;
            ci.attachmentCount = 2;
            ci.pAttachments = attachments;
            ci.width = extent.width;
            ci.height = extent.height;
            ci.layers = 1;
            VkResult r = vkCreateFramebuffer(device, &ci, nullptr, &framebuffers[i]);
            if (r != VK_SUCCESS) { fail("Framebuffer failed: " + vkResultName(r)); return false; }
        }
        return true;
    }

    Mat4 buildMvp() {
        const float aspect = extent.height > 0 ? (float)extent.width / (float)extent.height : 1.0f;
        const float nearPlane = 0.1f;
        const float farPlane = 32.0f;
        const float degToRad = 3.1415926535f / 180.0f;
        const Mat4 modelMatrix = model.modelReady() ? Mat4::identity() : rotationY(0.25f);
        const Mat4 view = multiply(translation(0.0f, 0.0f, camera.distance), multiply(rotationX(camera.pitchDeg * degToRad), rotationY(camera.yawDeg * degToRad)));
        const Mat4 proj = perspective(60.0f * 3.1415926535f / 180.0f, aspect, nearPlane, farPlane);
        cameraMvpReady = aspect > 0.0f && nearPlane > 0.0f && farPlane > nearPlane && camera.distance >= 2.0f && camera.distance <= 8.0f;
        cameraReady = cameraMvpReady;
        return multiply(proj, multiply(view, modelMatrix));
    }

    static float clampFloat(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    void updateReadyStatus() {
        syncLightingModelState();
        status = "SOLUM Engine\nRenderer path: Android Native Vulkan\nGPU: " + diagnostics.gpuName + "\nType: " + diagnostics.gpuType + "\nAPI: " + diagnostics.apiVersion + "\nSwapchain: created\nRender pass: color+depth OK\nRenderer core: OK\nRender Lab: Scene10 Lighting Control Lab\nVertex buffer: OK\nIndex buffer: OK\nCube draw: " + std::string(model.fallbackCubeVisible ? "OK/fallback visible" : "preserved/off") + "\nDepth: OK\nCamera: controls OK\nMaterial constants: OK\nMesh layout: OK\nActive model: " + model.activeModelName + "\nModel render: " + model.drawStatus + "\nPrimitives rendered/skipped/total: " + std::to_string(model.primitiveCountRendered) + " / " + std::to_string(model.primitiveCountSkipped) + " / " + std::to_string(model.primitiveCountTotal) + "\nMaterials used: " + std::to_string(model.materialSlotCountRendered) + "\nLighting status: " + model.lightingStatus + "\nBRDF status: " + model.brdfStatus + "\nSpecular status: " + model.specularStatus + "\nSpecular boost: " + std::to_string(model.specularBoost) + "\nReflection foundation: " + model.reflectionFoundationStatus + "\nFresnel status: " + model.fresnelStatus + "\nF0 status: " + model.f0Status + "\nLight preset: " + model.lightPreset + "\nSun intensity: " + std::to_string(model.sunIntensity) + "\nAmbient intensity: " + std::to_string(model.ambientIntensity) + "\nExposure: " + std::to_string(model.exposureValue) + " " + model.brightnessPreset + "\nMaterial response status: " + model.materialResponseStatus + "\nActive debug view: " + model.activeDebugView + "\nBaseColor status: " + model.baseColorTextureStatus + "\nMetallicRoughness status: " + model.metallicRoughnessStatus + "\nTangent status: " + model.tangentStatus + "\nNormal status: " + model.normalMapStatus + " applied=" + model.normalMapAppliedStatus + "\nAO status: " + model.occlusionMapStatus + "\nPBR textures uploaded/fallback/skipped: " + std::to_string(model.uploadedPbrTextureCount) + " / " + std::to_string(model.pbrTextureFallbackCount) + " / " + std::to_string(model.skippedPbrTextureCount) + "\nFPS/frameMs: " + std::to_string(model.fpsCurrent) + " / " + std::to_string(model.frameTimeMs) + "\nDebug ZIP: " + model.debugZipStatus + "\nGPU Upload: " + model.gpuUploadStatus + "\nDraw Model: " + model.drawStatus + "\nTexture size: " + std::to_string(model.textureWidth) + "x" + std::to_string(model.textureHeight) + "\nFallback texture: " + std::string(model.textureFallbackUsed ? "yes" : "no") + "\nVertices / indices: " + std::to_string(model.uploadedVertexCount) + " / " + std::to_string(model.uploadedIndexCount) + "\nFallback cube: " + std::string(model.fallbackCubeVisible ? "on" : "off") + "\nReason: " + model.reason + "\nFrames rendered: " + std::to_string(framesRendered) + "\nNext: IBL/reflections later";
    }

    bool setCamera(float yawDeg, float pitchDeg, float distance, bool controlsActive) {
        camera.yawDeg = yawDeg;
        camera.pitchDeg = clampFloat(pitchDeg, -75.0f, 75.0f);
        camera.distance = clampFloat(distance, 2.0f, 8.0f);
        camera.controlsActive = controlsActive;
        cameraControlsReady = controlsActive;
        const bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write(ok ? "valid" : diagnostics.status, ok ? "Camera controls updated MVP and rendered Scene01." : diagnostics.reason);
        if (ok) updateReadyStatus();
        return ok;
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
        VkClearValue clear[2]{};
        clear[0].color.float32[0] = 0.02f;
        clear[0].color.float32[1] = 0.07f;
        clear[0].color.float32[2] = 0.09f;
        clear[0].color.float32[3] = 1.0f;
        clear[1].depthStencil.depth = 1.0f;
        clear[1].depthStencil.stencil = 0;
        VkRenderPassBeginInfo rp{ VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO };
        rp.renderPass = renderPass;
        rp.framebuffer = framebuffers[imageIndex];
        rp.renderArea.extent = extent;
        rp.clearValueCount = 2;
        rp.pClearValues = clear;
        vkCmdBeginRenderPass(cmd, &rp, VK_SUBPASS_CONTENTS_INLINE);
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.pipeline);
        PushConstants pc{};
        pc.mvp = buildMvp();
        pc.material = material;
        syncLightingModelState();
        pushConstantsReady = true;
        materialConstantsReady = true;
        meshAttributeLayoutReady = sizeof(Vertex3D) == 60;
        if (model.modelReady() && modelMesh.ready() && !modelDrawRanges.empty()) {
            modelMesh.bind(cmd);
            if (modelMesh.indexedReady()) vkCmdBindIndexBuffer(cmd, modelMesh.indexBuffer.buffer, 0, modelMesh.indexType);
            for (const auto& range : modelDrawRanges) {
                if (range.materialSlot >= 0 && (size_t)range.materialSlot < modelMaterialSlots.size()) {
                    const auto& slot = modelMaterialSlots[(size_t)range.materialSlot];
                    for (int i = 0; i < 4; ++i) pc.material.baseColorFactor[i] = slot.baseColorFactor[i];
                    pc.material.metallicFactor = slot.metallicFactor;
                    pc.material.roughnessFactor = slot.roughnessFactor;
                    pc.material.normalScale = slot.normalScale;
                    pc.material.occlusionStrength = slot.occlusionStrength;
                    pc.material.alphaMode = slot.alphaMode;
                    pc.material.materialId = range.materialSlot;
                    const size_t materialIndex = (size_t)range.materialSlot;
                    pc.material.baseColorTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].baseColorUploaded) ? 1 : 0;
                    pc.material.metallicRoughnessTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].metallicRoughnessUploaded) ? 1 : 0;
                    pc.material.normalTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].normalUploaded) ? 1 : 0;
                    pc.material.occlusionTextureReady = (materialIndex < materialTextureSets.size() && materialTextureSets[materialIndex].occlusionUploaded) ? 1 : 0;
                } else {
                    pc.material = material;
                    pc.material.baseColorTextureReady = 0;
                    pc.material.metallicRoughnessTextureReady = 0;
                    pc.material.normalTextureReady = 0;
                    pc.material.occlusionTextureReady = 0;
                }
                if (range.materialSlot >= 0 && (size_t)range.materialSlot < textureDescriptorSets.size()) {
                    VkDescriptorSet set = textureDescriptorSets[(size_t)range.materialSlot];
                    if (set != VK_NULL_HANDLE) vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
                } else if (!textureDescriptorSets.empty() && textureDescriptorSets[0] != VK_NULL_HANDLE) {
                    VkDescriptorSet set = textureDescriptorSets[0];
                    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
                }
                vkCmdPushConstants(cmd, trianglePipeline.layout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(PushConstants), &pc);
                if (modelMesh.indexedReady() && range.indexCount > 0) vkCmdDrawIndexed(cmd, range.indexCount, 1, range.firstIndex, 0, 0);
                else if (range.vertexCount > 0) vkCmdDraw(cmd, range.vertexCount, 1, range.firstVertex, 0);
            }
        } else {
            if (!textureDescriptorSets.empty() && textureDescriptorSets[0] != VK_NULL_HANDLE) {
                VkDescriptorSet set = textureDescriptorSets[0];
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, trianglePipeline.layout, 0, 1, &set, 0, nullptr);
            }
            vkCmdPushConstants(cmd, trianglePipeline.layout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(PushConstants), &pc);
            validationMesh.bindIndexed(cmd);
            vkCmdDrawIndexed(cmd, validationMesh.indexCount, 1, 0, 0, 0);
        }
        vkCmdEndRenderPass(cmd);
        r = vkEndCommandBuffer(cmd);
        if (r != VK_SUCCESS) { fail("End command buffer failed: " + vkResultName(r)); return false; }
        return true;
    }

    bool renderOneFrame() {
        if (device == VK_NULL_HANDLE || swapchain == VK_NULL_HANDLE || commandBuffers.empty() || !validationMesh.indexedReady()) return false;
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
        firstFrameRendered = true;
        triangleDrawn = false;
        cubeDrawn = model.fallbackCubeVisible;
        return true;
    }

    void setModelFallback(const std::string& name, const std::string& path, const std::string& reason) {
        model.activeModelName = name.empty() ? "none" : name;
        model.activeModelPath = path;
        model.gpuUploadStatus = (name.empty() || name == "none") ? "failed" : "failed";
        model.drawStatus = "fallback";
        model.meshDrawStatus = "fallback";
        model.uploadedVertexCount = 0;
        model.uploadedIndexCount = 0;
        model.primitiveCountRendered = 0;
        model.fallbackCubeVisible = true;
        model.fallbackCubeStatus = "on";
        model.reason = reason.empty() ? "no active model" : reason;
        if (device != VK_NULL_HANDLE) modelMesh.destroy();
        modelDrawRanges.clear();
        renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", model.reason);
        updateReadyStatus();
    }

    bool uploadModelFirstPrimitive(
        const std::string& name,
        const std::string& path,
        const Vertex3D* vertices,
        uint32_t vertexCount,
        const uint32_t* indices,
        uint32_t indexCount,
        const float* boundsMin,
        const float* boundsMax,
        const float* boundsCenter,
        float scale,
        const float* baseColorFactor,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE) {
            error = "renderer not initialized";
            return false;
        }
        if (!modelMesh.createFromInterleaved(physicalDevice, device, vertices, vertexCount, indices, indexCount, name, error)) {
            setModelFallback(name, path, error);
            return false;
        }
        model.activeModelName = name.empty() ? "imported.glb" : name;
        model.activeModelPath = path;
        model.activePrimitiveIndex = 0;
        model.gpuUploadStatus = "ok";
        model.drawStatus = "ok";
        model.meshDrawStatus = "ok";
        model.uploadedVertexCount = vertexCount;
        model.uploadedIndexCount = indexCount;
        for (int i = 0; i < 3; ++i) {
            model.boundsMin[i] = boundsMin ? boundsMin[i] : 0.0f;
            model.boundsMax[i] = boundsMax ? boundsMax[i] : 0.0f;
            model.boundsCenter[i] = boundsCenter ? boundsCenter[i] : 0.0f;
        }
        model.modelScale = scale;
        model.fallbackCubeVisible = false;
        model.fallbackCubeStatus = "off";
        model.reason = "first primitive uploaded to Vulkan buffers";
        if (baseColorFactor) {
            material.baseColorFactor[0] = baseColorFactor[0];
            material.baseColorFactor[1] = baseColorFactor[1];
            material.baseColorFactor[2] = baseColorFactor[2];
            material.baseColorFactor[3] = baseColorFactor[3];
        }
        camera.distance = clampFloat(3.6f, 2.0f, 8.0f);
        cameraControlsReady = true;
        bool ok = renderOneFrame();
        if (!ok) {
            model.drawStatus = "fallback";
            model.meshDrawStatus = "fallback";
            model.fallbackCubeVisible = true;
            model.fallbackCubeStatus = "on";
            model.reason = diagnostics.reason.empty() ? "model draw failed" : diagnostics.reason;
            error = model.reason;
            renderOneFrame();
            syncDiagnostics();
            diagnostics.write("valid", model.reason);
            updateReadyStatus();
            return false;
        }
        syncDiagnostics();
        diagnostics.write("valid", "Scene10 Lighting Control Lab uploaded and drew first primitive.");
        updateReadyStatus();
        return true;
    }

    bool uploadModelMultiPrimitive(
        const std::string& name,
        const std::string& path,
        const Vertex3D* vertices,
        uint32_t vertexCount,
        const uint32_t* indices,
        uint32_t indexCount,
        const PrimitiveDrawRange* ranges,
        uint32_t rangeCount,
        const MaterialSlotState* slots,
        uint32_t slotCount,
        const float* boundsMin,
        const float* boundsMax,
        const float* boundsCenter,
        float scale,
        uint32_t primitiveTotal,
        uint32_t primitiveSkipped,
        uint32_t unsupportedPrimitiveCount,
        const std::string& reason,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE) {
            error = "renderer not initialized";
            return false;
        }
        if (!vertices || vertexCount == 0 || !ranges || rangeCount == 0) {
            error = "all primitives unsupported: no drawable primitive ranges";
            setModelFallback(name, path, error);
            return false;
        }
        if (!modelMesh.createFromInterleaved(physicalDevice, device, vertices, vertexCount, indices, indexCount, name, error)) {
            setModelFallback(name, path, error);
            return false;
        }
        modelDrawRanges.assign(ranges, ranges + rangeCount);
        modelMaterialSlots.assign(slots, slots + slotCount);
        if (modelMaterialSlots.empty()) modelMaterialSlots.push_back(MaterialSlotState{});
        model.activeModelName = name.empty() ? "imported.glb" : name;
        model.activeModelPath = path;
        model.activePrimitiveIndex = 0;
        model.gpuUploadStatus = "ok";
        model.drawStatus = primitiveSkipped > 0 ? "partial_ok" : "ok";
        model.meshDrawStatus = model.drawStatus;
        model.uploadedVertexCount = vertexCount;
        model.uploadedIndexCount = indexCount;
        model.primitiveCountTotal = primitiveTotal;
        model.primitiveCountRendered = rangeCount;
        model.primitiveCountSkipped = primitiveSkipped;
        model.unsupportedPrimitiveCount = unsupportedPrimitiveCount;
        model.materialSlotCount = slotCount;
        model.materialSlotCountRendered = slotCount;
        model.textureSlotCount = 0;
        for (const auto& slot : modelMaterialSlots) if (slot.baseColorTextureSlot >= 0) model.textureSlotCount += 1;
        if (model.textureSlotCount > model.textureSlotLimit) model.textureSlotCount = model.textureSlotLimit;
        model.fallbackCubeVisible = false;
        model.fallbackCubeStatus = "off";
        model.reason = reason.empty() ? "multi primitive static model uploaded to Vulkan buffers" : reason;
        for (int i = 0; i < 3; ++i) {
            model.boundsMin[i] = boundsMin ? boundsMin[i] : 0.0f;
            model.boundsMax[i] = boundsMax ? boundsMax[i] : 0.0f;
            model.boundsCenter[i] = boundsCenter ? boundsCenter[i] : 0.0f;
        }
        model.modelScale = scale;
        if (!modelMaterialSlots.empty()) {
            for (int i = 0; i < 4; ++i) material.baseColorFactor[i] = modelMaterialSlots[0].baseColorFactor[i];
            material.metallicFactor = modelMaterialSlots[0].metallicFactor;
            material.roughnessFactor = modelMaterialSlots[0].roughnessFactor;
            material.normalScale = modelMaterialSlots[0].normalScale;
            material.occlusionStrength = modelMaterialSlots[0].occlusionStrength;
            material.alphaMode = modelMaterialSlots[0].alphaMode;
            material.materialId = 0;
            model.metallicFactor = material.metallicFactor;
            model.roughnessFactor = material.roughnessFactor;
            model.normalScale = material.normalScale;
            model.occlusionStrength = material.occlusionStrength;
        }
        float extentX = model.boundsMax[0] - model.boundsMin[0];
        float extentY = model.boundsMax[1] - model.boundsMin[1];
        float extentZ = model.boundsMax[2] - model.boundsMin[2];
        float maxExtent = std::max(extentX, std::max(extentY, extentZ));
        camera.distance = clampFloat(maxExtent > 0.0f ? 3.4f : 3.6f, 2.0f, 8.0f);
        cameraControlsReady = true;
        bool ok = renderOneFrame();
        if (!ok) {
            model.drawStatus = "fallback";
            model.meshDrawStatus = "fallback";
            model.fallbackCubeVisible = true;
            model.fallbackCubeStatus = "on";
            model.reason = diagnostics.reason.empty() ? "multi primitive draw failed" : diagnostics.reason;
            error = model.reason;
            renderOneFrame();
            syncDiagnostics();
            diagnostics.write("valid", model.reason);
            updateReadyStatus();
            return false;
        }
        syncDiagnostics();
        diagnostics.write("valid", "Scene10 Lighting Control Lab uploaded and drew supported primitives.");
        updateReadyStatus();
        return true;
    }

    TextureResource* fallbackTexture(int kind) {
        if (materialTextureSets.empty()) return nullptr;
        if (kind == PbrTextureMetallicRoughness) return &materialTextureSets[0].metallicRoughness;
        if (kind == PbrTextureNormal) return &materialTextureSets[0].normal;
        if (kind == PbrTextureOcclusion) return &materialTextureSets[0].occlusion;
        return &materialTextureSets[0].baseColor;
    }

    TextureResource* descriptorTexture(size_t setIndex, int kind) {
        if (setIndex < materialTextureSets.size() && materialTextureSets[setIndex].texture(kind).ready) return &materialTextureSets[setIndex].texture(kind);
        return fallbackTexture(kind);
    }

    bool recreateTextureDescriptor(std::string& error) {
        if (materialTextureSets.empty() || trianglePipeline.textureSetLayout == VK_NULL_HANDLE) {
            error = "texture descriptor missing texture or layout";
            return false;
        }
        if (textureDescriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, textureDescriptorPool, nullptr);
            textureDescriptorPool = VK_NULL_HANDLE;
            textureDescriptorSets.clear();
        }
        VkDescriptorPoolSize poolSize{};
        poolSize.type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        poolSize.descriptorCount = (uint32_t)materialTextureSets.size() * 4u;
        VkDescriptorPoolCreateInfo pool{ VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO };
        pool.maxSets = (uint32_t)materialTextureSets.size();
        pool.poolSizeCount = 1;
        pool.pPoolSizes = &poolSize;
        VkResult r = vkCreateDescriptorPool(device, &pool, nullptr, &textureDescriptorPool);
        if (r != VK_SUCCESS) { error = "texture descriptor pool failed: " + vkResultName(r); return false; }
        std::vector<VkDescriptorSetLayout> layouts(materialTextureSets.size(), trianglePipeline.textureSetLayout);
        textureDescriptorSets.assign(materialTextureSets.size(), VK_NULL_HANDLE);
        VkDescriptorSetAllocateInfo alloc{ VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO };
        alloc.descriptorPool = textureDescriptorPool;
        alloc.descriptorSetCount = (uint32_t)layouts.size();
        alloc.pSetLayouts = layouts.data();
        r = vkAllocateDescriptorSets(device, &alloc, textureDescriptorSets.data());
        if (r != VK_SUCCESS) { error = "texture descriptor set failed: " + vkResultName(r); return false; }
        std::vector<VkDescriptorImageInfo> imageInfos(materialTextureSets.size() * 4u);
        std::vector<VkWriteDescriptorSet> writes(materialTextureSets.size() * 4u);
        for (size_t i = 0; i < materialTextureSets.size(); ++i) {
            for (uint32_t binding = 0; binding < 4; ++binding) {
                TextureResource* texture = descriptorTexture(i, (int)binding);
                if (!texture || !texture->ready) { error = "texture descriptor fallback missing"; return false; }
                size_t w = i * 4u + binding;
                imageInfos[w].sampler = texture->sampler;
                imageInfos[w].imageView = texture->view;
                imageInfos[w].imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
                writes[w] = { VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET };
                writes[w].dstSet = textureDescriptorSets[i];
                writes[w].dstBinding = binding;
                writes[w].descriptorCount = 1;
                writes[w].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
                writes[w].pImageInfo = &imageInfos[w];
            }
        }
        vkUpdateDescriptorSets(device, (uint32_t)writes.size(), writes.data(), 0, nullptr);
        return true;
    }

    bool createFallbackBaseColorTexture(std::string& error) {
        const uint8_t white[] = { 255, 255, 255, 255 };
        const uint8_t normal[] = { 128, 128, 255, 255 };
        materialTextureSets.resize(1);
        if (!materialTextureSets[0].baseColor.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        if (!materialTextureSets[0].metallicRoughness.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        if (!materialTextureSets[0].normal.createRgba8(physicalDevice, device, graphicsQueue, commandPool, normal, 1, 1, error)) return false;
        if (!materialTextureSets[0].occlusion.createRgba8(physicalDevice, device, graphicsQueue, commandPool, white, 1, 1, error)) return false;
        return recreateTextureDescriptor(error);
    }

    void restoreTextureFallbackAfterFailure() {
        std::string ignored;
        createFallbackBaseColorTexture(ignored);
        model.textureWidth = 0;
        model.textureHeight = 0;
        model.textureBytes = 0;
        model.textureFallbackUsed = true;
        syncTextureMaterialState();
    }

    bool uploadBaseColorTexture(
        const uint8_t* rgba,
        uint32_t width,
        uint32_t height,
        const std::string& name,
        const std::string& source,
        const std::string& mimeType,
        std::string& error
    ) {
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE || commandPool == VK_NULL_HANDLE) {
            error = "renderer not initialized for texture upload";
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            syncTextureMaterialState();
            return false;
        }
        if (!rgba || width == 0 || height == 0) {
            error = "decoded texture pixels empty";
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            syncTextureMaterialState();
            return false;
        }
        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        if (materialTextureSets.empty()) createFallbackBaseColorTexture(error);
        if (!materialTextureSets[0].baseColor.createRgba8(physicalDevice, device, graphicsQueue, commandPool, rgba, width, height, error)) {
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            restoreTextureFallbackAfterFailure();
            syncTextureMaterialState();
            return false;
        }
        if (!recreateTextureDescriptor(error)) {
            model.textureUploadStatus = "failed";
            model.baseColorTextureStatus = "failed";
            model.textureFallbackUsed = true;
            restoreTextureFallbackAfterFailure();
            syncTextureMaterialState();
            return false;
        }
        model.textureUploadStatus = "ok";
        model.baseColorTextureStatus = "ok";
        model.baseColorTextureName = name.empty() ? "baseColorTexture" : name;
        model.baseColorTextureSource = source.empty() ? "glb.bufferView" : source;
        model.baseColorTextureMimeType = mimeType.empty() ? "unknown" : mimeType;
        model.textureWidth = width;
        model.textureHeight = height;
        model.textureBytes = width * height * 4u;
        model.textureFallbackUsed = false;
        materialTextureSets[0].baseColorUploaded = true;
        syncTextureMaterialState();
        bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", ok ? "baseColor texture uploaded and sampled" : "baseColor texture uploaded; draw retry failed");
        updateReadyStatus();
        return ok;
    }

    bool uploadPbrTextureSlot(int materialSlot, int kind, const uint8_t* rgba, uint32_t width, uint32_t height, const std::string& name, const std::string& source, const std::string& mimeType, std::string& error) {
        if (materialSlot < 0 || materialSlot >= (int)model.textureSlotLimit) {
            error = "texture slot outside limit";
            if (kind == PbrTextureBaseColor) model.skippedTextureCount += 1; else model.skippedPbrTextureCount += 1;
            return false;
        }
        if (kind < 0 || kind > 3) { error = "unknown pbr texture kind"; return false; }
        if (device == VK_NULL_HANDLE || physicalDevice == VK_NULL_HANDLE || commandPool == VK_NULL_HANDLE) { error = "renderer not initialized for texture slot upload"; return false; }
        if (!rgba || width == 0 || height == 0) {
            error = "decoded texture slot pixels empty";
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        vkWaitForFences(device, 1, &inFlight, VK_TRUE, UINT64_MAX);
        if (materialTextureSets.empty()) createFallbackBaseColorTexture(error);
        if ((size_t)(materialSlot + 1) > materialTextureSets.size()) materialTextureSets.resize((size_t)materialSlot + 1);
        if (!materialTextureSets[(size_t)materialSlot].texture(kind).createRgba8(physicalDevice, device, graphicsQueue, commandPool, rgba, width, height, error)) {
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        if (!recreateTextureDescriptor(error)) {
            if (kind == PbrTextureBaseColor) model.textureFallbackCount += 1; else model.pbrTextureFallbackCount += 1;
            return false;
        }
        materialTextureSets[(size_t)materialSlot].setUploaded(kind, true);
        if (kind == PbrTextureBaseColor) {
            model.textureUploadStatus = "ok";
            model.baseColorTextureStatus = "ok";
            model.baseColorTextureName = name.empty() ? "baseColorTexture" : name;
            model.baseColorTextureSource = source.empty() ? "glb.bufferView" : source;
            model.baseColorTextureMimeType = mimeType.empty() ? "unknown" : mimeType;
        } else if (kind == PbrTextureMetallicRoughness) {
            model.metallicRoughnessStatus = "ok";
        } else if (kind == PbrTextureNormal) {
            model.normalMapStatus = "ok";
            model.normalMapAppliedStatus = (model.tangentStatus.find("generated") != std::string::npos || model.tangentStatus.find("from_gltf") != std::string::npos) ? "ok" : "blocked_no_tangent";
        } else if (kind == PbrTextureOcclusion) {
            model.occlusionMapStatus = "ok";
        }
        model.textureWidth = width;
        model.textureHeight = height;
        model.textureBytes = width * height * 4u;
        model.textureFallbackUsed = false;
        model.uploadedTextureCount = 0;
        model.uploadedPbrTextureCount = 0;
        for (const auto& set : materialTextureSets) {
            if (set.baseColorUploaded) model.uploadedTextureCount += 1;
            if (set.metallicRoughnessUploaded) model.uploadedPbrTextureCount += 1;
            if (set.normalUploaded) model.uploadedPbrTextureCount += 1;
            if (set.occlusionUploaded) model.uploadedPbrTextureCount += 1;
        }
        model.pbrMapsStatus = model.uploadedPbrTextureCount > 0 ? "partial_ok" : "missing";
        syncTextureMaterialState();
        bool ok = renderOneFrame();
        syncDiagnostics();
        diagnostics.write("valid", ok ? "texture slot uploaded and sampled" : "texture slot uploaded; draw retry failed");
        updateReadyStatus();
        return ok;
    }

    bool uploadBaseColorTextureSlot(int slot, const uint8_t* rgba, uint32_t width, uint32_t height, const std::string& name, const std::string& source, const std::string& mimeType, std::string& error) {
        return uploadPbrTextureSlot(slot, PbrTextureBaseColor, rgba, width, height, name, source, mimeType, error);
    }

    bool createRendererResources() {
        std::string error;
        if (!createImageViews()) return false;
        if (!createRenderPass()) return false;
        if (!createDepthResources()) return false;
        if (!createFramebuffers()) return false;
        if (!validationMesh.createFoundationCube(physicalDevice, device, error)) { fail("MeshResource cube failed: " + error); return false; }
        meshAttributeLayoutReady = validationMesh.vertexCount == 24 && sizeof(Vertex3D) == 60;
        if (!trianglePipeline.createTrianglePipeline(device, renderPass, extent, error)) { fail("PipelineBundle failed: " + error); return false; }
        if (!createCommandsAndSync()) return false;
        if (!createFallbackBaseColorTexture(error)) { fail("Fallback texture failed: " + error); return false; }
        return true;
    }

    bool init(ANativeWindow* window, int width, int height, const std::string& root) {
        setOutputRoot(root);
        destroy();
        status = "SOLUM Engine\nInitializing RendererCore...";
        if (!createInstance()) return false;
        if (!createSurface(window)) return false;
        if (!pickDeviceAndQueue()) return false;
        if (!createDevice()) return false;
        if (!createSwapchain(width, height)) return false;
        if (!createRendererResources()) return false;
        applyLightPreset(0);
        if (!renderOneFrame()) return false;
        cameraControlsReady = true;
        rendererCoreReady = true;
        model.gpuUploadStatus = "failed";
        model.drawStatus = "fallback";
        model.meshDrawStatus = "fallback";
        model.fallbackCubeVisible = true;
        model.fallbackCubeStatus = "on";
        model.textureUploadStatus = "missing";
        model.baseColorTextureStatus = "missing";
        model.textureFallbackUsed = true;
        model.reason = "no active model";
        syncDiagnostics();
        diagnostics.write("valid", "Scene10 Lighting Control Lab initialized with cube fallback while waiting for active model upload.");
        updateReadyStatus();
        return true;
    }
};

} // namespace solum
