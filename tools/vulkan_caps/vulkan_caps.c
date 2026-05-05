#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <vulkan/vulkan.h>

static const char* device_type_name(VkPhysicalDeviceType t) {
    switch (t) {
        case VK_PHYSICAL_DEVICE_TYPE_OTHER: return "OTHER";
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "INTEGRATED_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "DISCRETE_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "VIRTUAL_GPU";
        case VK_PHYSICAL_DEVICE_TYPE_CPU: return "CPU";
        default: return "UNKNOWN";
    }
}

static void print_json_string(FILE* f, const char* s) {
    fputc('"', f);
    for (const unsigned char* p = (const unsigned char*)s; *p; ++p) {
        switch (*p) {
            case '\\': fputs("\\\\", f); break;
            case '"': fputs("\\\"", f); break;
            case '\n': fputs("\\n", f); break;
            case '\r': fputs("\\r", f); break;
            case '\t': fputs("\\t", f); break;
            default:
                if (*p < 32) fprintf(f, "\\u%04x", *p);
                else fputc(*p, f);
        }
    }
    fputc('"', f);
}

#define VK_CHECK(expr) do { VkResult _r = (expr); if (_r != VK_SUCCESS) { result = _r; goto fail; } } while (0)

int main(int argc, char** argv) {
    const char* out_path = argc > 1 ? argv[1] : "vulkan_caps.json";
    FILE* f = fopen(out_path, "w");
    if (!f) {
        fprintf(stderr, "failed to open output: %s\n", out_path);
        return 2;
    }

    VkPhysicalDevice* devices = NULL;
    VkResult result = VK_SUCCESS;
    VkInstance instance = VK_NULL_HANDLE;
    VkApplicationInfo app = {0};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.pApplicationName = "SOLUM Vulkan Caps";
    app.applicationVersion = VK_MAKE_VERSION(0, 2, 0);
    app.pEngineName = "SOLUM";
    app.engineVersion = VK_MAKE_VERSION(0, 2, 0);
    app.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo ci = {0};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &app;

    result = vkCreateInstance(&ci, NULL, &instance);
    if (result != VK_SUCCESS) goto fail;

    uint32_t device_count = 0;
    VK_CHECK(vkEnumeratePhysicalDevices(instance, &device_count, NULL));
    if (device_count == 0) {
        fprintf(f, "{\n");
        fprintf(f, "  \"schema\": \"solum.vulkan_caps\",\n");
        fprintf(f, "  \"schemaVersion\": 1,\n");
        fprintf(f, "  \"status\": \"failed\",\n");
        fprintf(f, "  \"reason\": \"No Vulkan physical devices found\",\n");
        fprintf(f, "  \"devices\": []\n");
        fprintf(f, "}\n");
        if (instance) vkDestroyInstance(instance, NULL);
        fclose(f);
        return 1;
    }
    if (device_count > 0) {
        devices = (VkPhysicalDevice*)calloc(device_count, sizeof(VkPhysicalDevice));
        if (!devices) { result = VK_ERROR_OUT_OF_HOST_MEMORY; goto fail; }
        VK_CHECK(vkEnumeratePhysicalDevices(instance, &device_count, devices));
    }

    fprintf(f, "{\n");
    fprintf(f, "  \"schema\": \"solum.vulkan_caps\",\n");
    fprintf(f, "  \"schemaVersion\": 1,\n");
    fprintf(f, "  \"status\": \"ok\",\n");
    fprintf(f, "  \"deviceCount\": %u,\n", device_count);
    fprintf(f, "  \"devices\": [\n");

    for (uint32_t i = 0; i < device_count; ++i) {
        VkPhysicalDeviceProperties props;
        VkPhysicalDeviceFeatures features;
        VkPhysicalDeviceMemoryProperties mem;
        vkGetPhysicalDeviceProperties(devices[i], &props);
        vkGetPhysicalDeviceFeatures(devices[i], &features);
        vkGetPhysicalDeviceMemoryProperties(devices[i], &mem);

        uint32_t ext_count = 0;
        vkEnumerateDeviceExtensionProperties(devices[i], NULL, &ext_count, NULL);
        VkExtensionProperties* exts = NULL;
        if (ext_count > 0) {
            exts = (VkExtensionProperties*)calloc(ext_count, sizeof(VkExtensionProperties));
            if (exts) vkEnumerateDeviceExtensionProperties(devices[i], NULL, &ext_count, exts);
        }

        fprintf(f, "    {\n");
        fprintf(f, "      \"deviceName\": "); print_json_string(f, props.deviceName); fprintf(f, ",\n");
        fprintf(f, "      \"deviceType\": "); print_json_string(f, device_type_name(props.deviceType)); fprintf(f, ",\n");
        fprintf(f, "      \"vendorID\": %u,\n", props.vendorID);
        fprintf(f, "      \"deviceID\": %u,\n", props.deviceID);
        fprintf(f, "      \"apiVersion\": \"%u.%u.%u\",\n", VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion), VK_VERSION_PATCH(props.apiVersion));
        fprintf(f, "      \"driverVersion\": %u,\n", props.driverVersion);
        fprintf(f, "      \"features\": {\n");
        fprintf(f, "        \"geometryShader\": %s,\n", features.geometryShader ? "true" : "false");
        fprintf(f, "        \"tessellationShader\": %s,\n", features.tessellationShader ? "true" : "false");
        fprintf(f, "        \"samplerAnisotropy\": %s,\n", features.samplerAnisotropy ? "true" : "false");
        fprintf(f, "        \"textureCompressionASTC_LDR\": %s,\n", features.textureCompressionASTC_LDR ? "true" : "false");
        fprintf(f, "        \"textureCompressionETC2\": %s,\n", features.textureCompressionETC2 ? "true" : "false");
        fprintf(f, "        \"shaderFloat64\": %s\n", features.shaderFloat64 ? "true" : "false");
        fprintf(f, "      },\n");
        fprintf(f, "      \"limits\": {\n");
        fprintf(f, "        \"maxImageDimension2D\": %u,\n", props.limits.maxImageDimension2D);
        fprintf(f, "        \"maxUniformBufferRange\": %u,\n", props.limits.maxUniformBufferRange);
        fprintf(f, "        \"maxStorageBufferRange\": %u,\n", props.limits.maxStorageBufferRange);
        fprintf(f, "        \"maxPushConstantsSize\": %u,\n", props.limits.maxPushConstantsSize);
        fprintf(f, "        \"minUniformBufferOffsetAlignment\": %llu\n", (unsigned long long)props.limits.minUniformBufferOffsetAlignment);
        fprintf(f, "      },\n");
        fprintf(f, "      \"memory\": {\n");
        fprintf(f, "        \"memoryTypeCount\": %u,\n", mem.memoryTypeCount);
        fprintf(f, "        \"memoryHeapCount\": %u\n", mem.memoryHeapCount);
        fprintf(f, "      },\n");
        fprintf(f, "      \"extensions\": [");
        for (uint32_t e = 0; e < ext_count && exts; ++e) {
            if (e) fprintf(f, ", ");
            print_json_string(f, exts[e].extensionName);
        }
        fprintf(f, "],\n");
        fprintf(f, "      \"missingCritical\": []\n");
        fprintf(f, "    }%s\n", (i + 1 < device_count) ? "," : "");
        free(exts);
    }

    fprintf(f, "  ]\n");
    fprintf(f, "}\n");

    free(devices);
    if (instance) vkDestroyInstance(instance, NULL);
    fclose(f);
    return 0;

fail:
    fprintf(f, "{\n");
    fprintf(f, "  \"schema\": \"solum.vulkan_caps\",\n");
    fprintf(f, "  \"schemaVersion\": 1,\n");
    fprintf(f, "  \"status\": \"failed\",\n");
    fprintf(f, "  \"vkResult\": %d,\n", result);
    fprintf(f, "  \"reason\": \"Vulkan capability query failed before device enumeration completed\",\n");
    fprintf(f, "  \"devices\": []\n");
    fprintf(f, "}\n");
    free(devices);
    if (instance) vkDestroyInstance(instance, NULL);
    fclose(f);
    return 1;
}
