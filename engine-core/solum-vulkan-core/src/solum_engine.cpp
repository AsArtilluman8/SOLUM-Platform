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
        return env->NewStringUTF("{\"currentLabScene\":\"scene02_model_import_lab\",\"currentLabSceneName\":\"Scene02 Model Import Lab\",\"status\":\"native_handle_missing\",\"gpuUploadStatus\":\"not_implemented\",\"drawStatus\":\"not_implemented\"}");
    }
    std::string json = std::string("{\"currentLabScene\":\"") + renderer->diagnostics.renderLab.sceneId()
        + "\",\"currentLabSceneName\":\"" + renderer->diagnostics.renderLab.sceneName()
        + "\",\"renderingStatus\":\"foundation_only\""
        + ",\"assetImportStatus\":\"not run\""
        + ",\"activeModelName\":\"none\""
        + ",\"activeModelSummary\":\"metadata parsed by Java GLB CPU parser when a model is imported\""
        + ",\"gpuUploadStatus\":\"not_implemented\""
        + ",\"drawStatus\":\"not_implemented\""
        + ",\"cubeStatus\":\"" + (renderer->diagnostics.cubeReady ? "ok" : "failed") + "\""
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
