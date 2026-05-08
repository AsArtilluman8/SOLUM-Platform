#define VK_USE_PLATFORM_ANDROID_KHR 1

#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <string>

#include "solum/renderer_core.hpp"

extern "C" JNIEXPORT jlong JNICALL Java_com_solum_engine_MainActivity_nativeCreate(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(new solum::RendererCore());
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (renderer) renderer->destroy();
    delete renderer;
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceCreated(JNIEnv* env, jclass, jlong handle, jobject surface, jstring outputRoot) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    const char* root = env->GetStringUTFChars(outputRoot, nullptr);
    std::string rootStr = root ? root : "/storage/emulated/0/SOLUMCreative";
    env->ReleaseStringUTFChars(outputRoot, root);
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    if (!win) {
        renderer->status = "SOLUM Engine\nANativeWindow_fromSurface failed.";
        renderer->fail("ANativeWindow_fromSurface failed");
        return;
    }
    int w = ANativeWindow_getWidth(win);
    int h = ANativeWindow_getHeight(win);
    renderer->init(win, w, h, rootStr);
    ANativeWindow_release(win);
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceChanged(JNIEnv* env, jclass, jlong handle, jobject surface, jint width, jint height) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    if (win) {
        renderer->init(win, width, height, renderer->outputRoot);
        ANativeWindow_release(win);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*, jclass, jlong handle) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    renderer->destroy();
    renderer->status = "SOLUM Engine\nSurface destroyed. Vulkan cleaned.";
}

extern "C" JNIEXPORT jstring JNICALL Java_com_solum_engine_MainActivity_nativeGetStatus(JNIEnv* env, jclass, jlong handle) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    return env->NewStringUTF(renderer ? renderer->status.c_str() : "SOLUM Engine\nNo native handle");
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSetCamera(JNIEnv*, jclass, jlong handle, jfloat yawDeg, jfloat pitchDeg, jfloat distance) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    renderer->setCamera(yawDeg, pitchDeg, distance, true);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_solum_engine_MainActivity_nativeGetRenderLabState(JNIEnv* env, jclass, jlong handle) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) {
        return env->NewStringUTF("{\"currentLabScene\":\"scene04_texture_binding_lab\",\"currentLabSceneName\":\"Scene04 Texture Binding Lab\",\"status\":\"native_handle_missing\",\"gpuUploadStatus\":\"failed\",\"drawStatus\":\"fallback\",\"textureUploadStatus\":\"missing\",\"baseColorTextureStatus\":\"missing\",\"textureFallbackUsed\":true,\"fallbackCubeVisible\":true,\"fallbackCubeStatus\":\"on\"}");
    }
    std::string json = std::string("{\"currentLabScene\":\"") + renderer->diagnostics.renderLab.sceneId()
        + "\",\"currentLabSceneName\":\"" + renderer->diagnostics.renderLab.sceneName()
        + "\",\"renderingStatus\":\"" + (renderer->model.modelReady() ? "model_first_primitive" : "fallback_cube") + "\""
        + ",\"assetImportStatus\":\"" + (renderer->model.activeModelName == "none" ? "no active model" : "active model") + "\""
        + ",\"activeModelName\":\"" + solum::escapeJson(renderer->model.activeModelName) + "\""
        + ",\"activeModelPath\":\"" + solum::escapeJson(renderer->model.activeModelPath) + "\""
        + ",\"activePrimitiveIndex\":" + std::to_string(renderer->model.activePrimitiveIndex)
        + ",\"gpuUploadStatus\":\"" + solum::escapeJson(renderer->model.gpuUploadStatus) + "\""
        + ",\"drawStatus\":\"" + solum::escapeJson(renderer->model.drawStatus) + "\""
        + ",\"meshDrawStatus\":\"" + solum::escapeJson(renderer->model.meshDrawStatus) + "\""
        + ",\"textureUploadStatus\":\"" + solum::escapeJson(renderer->model.textureUploadStatus) + "\""
        + ",\"baseColorTextureStatus\":\"" + solum::escapeJson(renderer->model.baseColorTextureStatus) + "\""
        + ",\"baseColorTextureName\":\"" + solum::escapeJson(renderer->model.baseColorTextureName) + "\""
        + ",\"baseColorTextureSource\":\"" + solum::escapeJson(renderer->model.baseColorTextureSource) + "\""
        + ",\"baseColorTextureMimeType\":\"" + solum::escapeJson(renderer->model.baseColorTextureMimeType) + "\""
        + ",\"textureWidth\":" + std::to_string(renderer->model.textureWidth)
        + ",\"textureHeight\":" + std::to_string(renderer->model.textureHeight)
        + ",\"textureBytes\":" + std::to_string(renderer->model.textureBytes)
        + ",\"textureFallbackUsed\":" + (renderer->model.textureFallbackUsed ? "true" : "false")
        + ",\"uploadedVertexCount\":" + std::to_string(renderer->model.uploadedVertexCount)
        + ",\"uploadedIndexCount\":" + std::to_string(renderer->model.uploadedIndexCount)
        + ",\"modelVertexLayout\":\"" + renderer->model.modelVertexLayout + "\""
        + ",\"modelBoundsMin\":[" + std::to_string(renderer->model.boundsMin[0]) + "," + std::to_string(renderer->model.boundsMin[1]) + "," + std::to_string(renderer->model.boundsMin[2]) + "]"
        + ",\"modelBoundsMax\":[" + std::to_string(renderer->model.boundsMax[0]) + "," + std::to_string(renderer->model.boundsMax[1]) + "," + std::to_string(renderer->model.boundsMax[2]) + "]"
        + ",\"modelBoundsCenter\":[" + std::to_string(renderer->model.boundsCenter[0]) + "," + std::to_string(renderer->model.boundsCenter[1]) + "," + std::to_string(renderer->model.boundsCenter[2]) + "]"
        + ",\"modelScale\":" + std::to_string(renderer->model.modelScale)
        + ",\"modelRenderMode\":\"first_primitive\""
        + ",\"fallbackCubeVisible\":" + (renderer->model.fallbackCubeVisible ? "true" : "false")
        + ",\"fallbackCubeStatus\":\"" + solum::escapeJson(renderer->model.fallbackCubeStatus) + "\""
        + ",\"reason\":\"" + solum::escapeJson(renderer->model.reason) + "\""
        + ",\"cubeStatus\":\"" + ((renderer->diagnostics.cubeReady || !renderer->model.fallbackCubeVisible) ? "ok" : "failed") + "\""
        + ",\"depthStatus\":\"" + (renderer->diagnostics.depthReady ? "ok" : "failed") + "\""
        + ",\"cameraStatus\":\"" + (renderer->diagnostics.cameraReady ? "ok" : "failed") + "\""
        + ",\"cameraMvpStatus\":\"" + (renderer->diagnostics.cameraMvpReady ? "ok" : "failed") + "\""
        + ",\"cameraControlsStatus\":\"" + (renderer->diagnostics.cameraControlsReady ? "ok" : "not_implemented") + "\""
        + ",\"cameraYawDeg\":" + std::to_string(renderer->diagnostics.camera.yawDeg)
        + ",\"cameraPitchDeg\":" + std::to_string(renderer->diagnostics.camera.pitchDeg)
        + ",\"cameraDistance\":" + std::to_string(renderer->diagnostics.camera.distance)
        + ",\"indexBufferReady\":" + (renderer->diagnostics.indexBufferReady ? "true" : "false")
        + ",\"uniformOrPushConstantsReady\":" + (renderer->diagnostics.uniformOrPushConstantsReady ? "true" : "false")
        + ",\"materialConstantsReady\":" + (renderer->diagnostics.materialConstantsReady ? "true" : "false")
        + ",\"meshAttributeLayoutReady\":" + (renderer->diagnostics.meshAttributeLayoutReady ? "true" : "false")
        + ",\"vertexLayout\":\"POSITION,NORMAL,TEXCOORD_0,COLOR_0\""
        + ",\"vertexStrideBytes\":" + std::to_string(renderer->diagnostics.vertexStrideBytes)
        + ",\"material\":{\"materialId\":" + std::to_string(renderer->diagnostics.material.materialId)
        + ",\"baseColorFactor\":[" + std::to_string(renderer->diagnostics.material.baseColorFactor[0]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[1]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[2]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[3]) + "]"
        + ",\"metallicFactor\":" + std::to_string(renderer->diagnostics.material.metallicFactor)
        + ",\"roughnessFactor\":" + std::to_string(renderer->diagnostics.material.roughnessFactor)
        + ",\"emissiveFactor\":[" + std::to_string(renderer->diagnostics.material.emissiveFactor[0]) + "," + std::to_string(renderer->diagnostics.material.emissiveFactor[1]) + "," + std::to_string(renderer->diagnostics.material.emissiveFactor[2]) + "]"
        + ",\"alphaMode\":\"OPAQUE\"}"
        + ",\"vertexCount\":" + std::to_string(renderer->diagnostics.vertexCount)
        + ",\"indexCount\":" + std::to_string(renderer->diagnostics.indexCount)
        + ",\"framesRendered\":" + std::to_string(renderer->diagnostics.framesRendered)
        + ",\"rendererPath\":\"Android Native Vulkan\""
        + ",\"triangleFallback\":\"available/disabled\""
        + ",\"screenshot\":{\"status\":\"not_available\",\"reason\":\"renderer_readback_not_implemented\"}}";
    return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_solum_engine_MainActivity_nativeUploadModelFirstPrimitive(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring modelName,
    jstring modelPath,
    jfloatArray vertexData,
    jintArray indexData,
    jfloatArray boundsMinArray,
    jfloatArray boundsMaxArray,
    jfloatArray boundsCenterArray,
    jfloat modelScale,
    jfloatArray baseColorArray
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer || !vertexData) return JNI_FALSE;
    const char* nameChars = env->GetStringUTFChars(modelName, nullptr);
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    std::string name = nameChars ? nameChars : "imported.glb";
    std::string path = pathChars ? pathChars : "";
    jsize vertexFloatCount = env->GetArrayLength(vertexData);
    if (vertexFloatCount <= 0 || (vertexFloatCount % 11) != 0) {
        renderer->setModelFallback(name, path, "vertex data must use POSITION,NORMAL,TEXCOORD_0,COLOR_0 stride 11 floats");
        if (nameChars) env->ReleaseStringUTFChars(modelName, nameChars);
        if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);
        return JNI_FALSE;
    }
    jfloat* vertices = env->GetFloatArrayElements(vertexData, nullptr);
    jint* indices = indexData ? env->GetIntArrayElements(indexData, nullptr) : nullptr;
    jsize indexCount = indexData ? env->GetArrayLength(indexData) : 0;
    float boundsMin[3] = {0.0f, 0.0f, 0.0f};
    float boundsMax[3] = {0.0f, 0.0f, 0.0f};
    float boundsCenter[3] = {0.0f, 0.0f, 0.0f};
    float baseColor[4] = {1.0f, 1.0f, 1.0f, 1.0f};
    if (boundsMinArray && env->GetArrayLength(boundsMinArray) >= 3) env->GetFloatArrayRegion(boundsMinArray, 0, 3, boundsMin);
    if (boundsMaxArray && env->GetArrayLength(boundsMaxArray) >= 3) env->GetFloatArrayRegion(boundsMaxArray, 0, 3, boundsMax);
    if (boundsCenterArray && env->GetArrayLength(boundsCenterArray) >= 3) env->GetFloatArrayRegion(boundsCenterArray, 0, 3, boundsCenter);
    if (baseColorArray && env->GetArrayLength(baseColorArray) >= 4) env->GetFloatArrayRegion(baseColorArray, 0, 4, baseColor);
    std::vector<uint32_t> index32;
    if (indices && indexCount > 0) {
        index32.resize((size_t)indexCount);
        for (jsize i = 0; i < indexCount; ++i) index32[(size_t)i] = (uint32_t)indices[i];
    }
    std::string error;
    bool ok = renderer->uploadModelFirstPrimitive(
        name,
        path,
        reinterpret_cast<const solum::Vertex3D*>(vertices),
        (uint32_t)(vertexFloatCount / 11),
        index32.empty() ? nullptr : index32.data(),
        (uint32_t)index32.size(),
        boundsMin,
        boundsMax,
        boundsCenter,
        modelScale,
        baseColor,
        error
    );
    env->ReleaseFloatArrayElements(vertexData, vertices, JNI_ABORT);
    if (indices) env->ReleaseIntArrayElements(indexData, indices, JNI_ABORT);
    if (nameChars) env->ReleaseStringUTFChars(modelName, nameChars);
    if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSetModelFallback(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring modelName,
    jstring modelPath,
    jstring reason
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    const char* nameChars = env->GetStringUTFChars(modelName, nullptr);
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    const char* reasonChars = env->GetStringUTFChars(reason, nullptr);
    renderer->setModelFallback(
        nameChars ? nameChars : "none",
        pathChars ? pathChars : "",
        reasonChars ? reasonChars : "no active model"
    );
    if (nameChars) env->ReleaseStringUTFChars(modelName, nameChars);
    if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);
    if (reasonChars) env->ReleaseStringUTFChars(reason, reasonChars);
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_solum_engine_MainActivity_nativeUploadBaseColorTexture(
    JNIEnv* env,
    jclass,
    jlong handle,
    jintArray rgbaPixels,
    jint width,
    jint height,
    jstring textureName,
    jstring textureSource,
    jstring mimeType
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer || !rgbaPixels || width <= 0 || height <= 0) return JNI_FALSE;
    const char* nameChars = env->GetStringUTFChars(textureName, nullptr);
    const char* sourceChars = env->GetStringUTFChars(textureSource, nullptr);
    const char* mimeChars = env->GetStringUTFChars(mimeType, nullptr);
    jsize pixelCount = env->GetArrayLength(rgbaPixels);
    const int64_t expected = (int64_t)width * (int64_t)height;
    if (expected <= 0 || pixelCount < expected) {
        renderer->model.textureUploadStatus = "failed";
        renderer->model.baseColorTextureStatus = "failed";
        renderer->model.textureFallbackUsed = true;
        renderer->model.reason = "texture pixel count mismatch";
        if (nameChars) env->ReleaseStringUTFChars(textureName, nameChars);
        if (sourceChars) env->ReleaseStringUTFChars(textureSource, sourceChars);
        if (mimeChars) env->ReleaseStringUTFChars(mimeType, mimeChars);
        return JNI_FALSE;
    }
    jint* pixels = env->GetIntArrayElements(rgbaPixels, nullptr);
    std::vector<uint8_t> rgba((size_t)expected * 4u);
    for (int64_t i = 0; i < expected; ++i) {
        uint32_t argb = (uint32_t)pixels[i];
        rgba[(size_t)i * 4u] = (uint8_t)((argb >> 16) & 0xffu);
        rgba[(size_t)i * 4u + 1u] = (uint8_t)((argb >> 8) & 0xffu);
        rgba[(size_t)i * 4u + 2u] = (uint8_t)(argb & 0xffu);
        rgba[(size_t)i * 4u + 3u] = (uint8_t)((argb >> 24) & 0xffu);
    }
    std::string error;
    bool ok = renderer->uploadBaseColorTexture(
        rgba.data(),
        (uint32_t)width,
        (uint32_t)height,
        nameChars ? nameChars : "baseColorTexture",
        sourceChars ? sourceChars : "glb.bufferView",
        mimeChars ? mimeChars : "unknown",
        error
    );
    if (!ok) renderer->model.reason = error.empty() ? "texture upload failed" : error;
    env->ReleaseIntArrayElements(rgbaPixels, pixels, JNI_ABORT);
    if (nameChars) env->ReleaseStringUTFChars(textureName, nameChars);
    if (sourceChars) env->ReleaseStringUTFChars(textureSource, sourceChars);
    if (mimeChars) env->ReleaseStringUTFChars(mimeType, mimeChars);
    return ok ? JNI_TRUE : JNI_FALSE;
}
