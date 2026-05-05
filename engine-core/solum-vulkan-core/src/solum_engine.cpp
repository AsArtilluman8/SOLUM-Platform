#define VK_USE_PLATFORM_ANDROID_KHR 1

#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <vulkan/vulkan.h>

#include <chrono>
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

static void writeRuntimeReport(SolumEngine* e, const std::string& status, const std::string& reason) {
    if (!e) return;
    std::string root = e->outputRoot.empty() ? "/storage/emulated/0/SOLUMCreative" : e->outputRoot;
    std::string dir = root + "/diagnostics/latest";
    ensureDir(root + "/");
    ensureDir(root + "/diagnostics/");
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
    f << "  \"note\": \"Termux Vulkan may show llvmpipe CPU. This runtime report is the Android APK path.\"\n";
    f << "}\n";
}

static void cleanupVulkan(SolumEngine* e) {
    if (!e) return;
    if (e->device != VK_NULL_HANDLE) vkDeviceWaitIdle(e->device);
    if (e->swapchain != VK_NULL_HANDLE) { vkDestroySwapchainKHR(e->device, e->swapchain, nullptr); e->swapchain = VK_NULL_HANDLE; }
    if (e->device != VK_NULL_HANDLE) { vkDestroyDevice(e->device, nullptr); e->device = VK_NULL_HANDLE; }
    if (e->surface != VK_NULL_HANDLE) { vkDestroySurfaceKHR(e->instance, e->surface, nullptr); e->surface = VK_NULL_HANDLE; }
    if (e->instance != VK_NULL_HANDLE) { vkDestroyInstance(e->instance, nullptr); e->instance = VK_NULL_HANDLE; }
}

static bool initVulkan(SolumEngine* e, ANativeWindow* window, int width, int height) {
    cleanupVulkan(e);
    e->status = "SOLUM Engine\nInitializing Android Native Vulkan...";

    const char* instanceExts[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
    VkApplicationInfo app{ VK_STRUCTURE_TYPE_APPLICATION_INFO };
    app.pApplicationName = "SOLUM Engine";
    app.applicationVersion = VK_MAKE_VERSION(0, 4, 0);
    app.pEngineName = "SOLUM";
    app.engineVersion = VK_MAKE_VERSION(0, 4, 0);
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

    e->status = "SOLUM Engine\nRenderer path: Android Native Vulkan\nGPU: " + e->gpuName + "\nType: " + e->gpuType + "\nAPI: " + e->apiVersion + "\nSwapchain: created\nNext: render clear/triangle pass";
    writeRuntimeReport(e, "partial", "Android native Vulkan initialized; swapchain created; draw pass not yet implemented.");
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
