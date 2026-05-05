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
