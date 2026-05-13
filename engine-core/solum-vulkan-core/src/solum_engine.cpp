#define VK_USE_PLATFORM_ANDROID_KHR 1

#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <string>
#include <vector>

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

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSetLightingControls(
    JNIEnv*,
    jclass,
    jlong handle,
    jint lightPreset,
    jfloat sunIntensity,
    jfloat ambientIntensity,
    jint activeDebugView,
    jint toneMappingMode,
    jfloat exposureValue,
    jfloat ambientFloor,
    jint brightnessPreset,
    jfloat specularBoost,
    jfloat reflectionIntensity,
    jfloat contactShadowIntensity,
    jint calibrationPreset,
    jfloat calibrationStrength,
    jfloat glossSliderValue,
    jfloat paintGlossSliderValue,
    jfloat environmentIntensity,
    jint environmentPreset,
    jfloat horizonStrength
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    renderer->setLightingControls(lightPreset, sunIntensity, ambientIntensity, activeDebugView, toneMappingMode, exposureValue, ambientFloor, brightnessPreset, specularBoost, reflectionIntensity, contactShadowIntensity, calibrationPreset, calibrationStrength, glossSliderValue, paintGlossSliderValue, environmentIntensity, environmentPreset, horizonStrength);
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeUpdateUiDiagnostics(
    JNIEnv* env,
    jclass,
    jlong handle,
    jfloat fpsCurrent,
    jfloat frameTimeMs,
    jfloat fpsLastStable,
    jfloat frameTimeLastStableMs,
    jstring fpsSource,
    jstring fpsStatus,
    jstring fpsUpdateMode,
    jint fpsSampleWindowMs,
    jlong framesRenderedLive,
    jstring debugZipStatus,
    jstring debugZipPath,
    jstring debugZipIncludedFiles,
    jstring debugZipReason
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    const char* fpsSourceChars = env->GetStringUTFChars(fpsSource, nullptr);
    const char* fpsStatusChars = env->GetStringUTFChars(fpsStatus, nullptr);
    const char* fpsModeChars = env->GetStringUTFChars(fpsUpdateMode, nullptr);
    const char* statusChars = env->GetStringUTFChars(debugZipStatus, nullptr);
    const char* pathChars = env->GetStringUTFChars(debugZipPath, nullptr);
    const char* filesChars = env->GetStringUTFChars(debugZipIncludedFiles, nullptr);
    const char* reasonChars = env->GetStringUTFChars(debugZipReason, nullptr);
    renderer->model.fpsCurrent = fpsCurrent;
    renderer->model.frameTimeMs = frameTimeMs;
    renderer->model.fpsLastStable = fpsLastStable;
    renderer->model.frameTimeLastStableMs = frameTimeLastStableMs;
    renderer->model.fpsSource = fpsSourceChars ? fpsSourceChars : "not_ready";
    renderer->model.fpsStatus = fpsStatusChars ? fpsStatusChars : "not_ready";
    renderer->model.fpsUpdateMode = fpsModeChars ? fpsModeChars : "java_choreographer_live";
    renderer->model.fpsSampleWindowMs = (uint32_t)std::max(0, (int)fpsSampleWindowMs);
    renderer->model.framesRenderedLive = (uint64_t)std::max((long long)0, (long long)framesRenderedLive);
    renderer->model.debugZipStatus = statusChars ? statusChars : "not_run";
    renderer->model.debugZipPath = pathChars ? pathChars : "";
    renderer->model.debugZipIncludedFiles = filesChars ? filesChars : "";
    renderer->model.debugZipReason = reasonChars ? reasonChars : "";
    renderer->syncDiagnostics();
    if (fpsSourceChars) env->ReleaseStringUTFChars(fpsSource, fpsSourceChars);
    if (fpsStatusChars) env->ReleaseStringUTFChars(fpsStatus, fpsStatusChars);
    if (fpsModeChars) env->ReleaseStringUTFChars(fpsUpdateMode, fpsModeChars);
    if (statusChars) env->ReleaseStringUTFChars(debugZipStatus, statusChars);
    if (pathChars) env->ReleaseStringUTFChars(debugZipPath, pathChars);
    if (filesChars) env->ReleaseStringUTFChars(debugZipIncludedFiles, filesChars);
    if (reasonChars) env->ReleaseStringUTFChars(debugZipReason, reasonChars);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_solum_engine_MainActivity_nativeGetRenderLabState(JNIEnv* env, jclass, jlong handle) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) {
        return env->NewStringUTF("{\"currentScene\":\"scene15_environment_ibl_lab\",\"currentLabScene\":\"scene15_environment_ibl_lab\",\"currentLabSceneName\":\"Scene15 Environment IBL Lab\",\"status\":\"native_handle_missing\",\"lightingStatus\":\"failed\",\"lightingControlStatus\":\"failed\",\"lightingUiMode\":\"compact_sliders\",\"inspectorUiStatus\":\"ok\",\"inspectorUiMode\":\"tabbed_compact_inspector\",\"activeInspectorTab\":\"Assets\",\"assetsTabStatus\":\"ok_import_scan_export_summary\",\"cameraTabStatus\":\"ok_camera_info_reset_zoom\",\"lightingTabStatus\":\"ok_sliders_environment_controls\",\"materialTabStatus\":\"ok_debug_views\",\"debugTabStatus\":\"ok_fps_zip_status\",\"reflectionIntensity\":1.15,\"environmentIntensity\":1.0,\"environmentPreset\":\"Studio\",\"contactGroundingStatus\":\"foundation_analytic\",\"contactShadowStatus\":\"enabled\",\"contactShadowMode\":\"analytic_blob_or_grounding_approx\",\"contactShadowIntensity\":0.65,\"contactShadowPerformanceStatus\":\"ok_uniform_only_no_shadow_pass\",\"groundingUsesModelBounds\":\"yes_upload_bounds_scaled_local\",\"groundingUniformUpdateStatus\":\"ok_uniform_only\",\"groundSliderStatus\":\"ok\",\"contactGroundingSliderStatus\":\"ok\",\"groundingDebugViewStatus\":\"not_applied\",\"iblStatus\":\"missing\",\"iblMode\":\"directional_sky_ground_ibl\",\"environmentIblStatus\":\"ok_foundation\",\"environmentIblMode\":\"directional_sky_ground_ibl\",\"environmentSourceStatus\":\"ok_procedural_no_external_texture\",\"environmentSourceType\":\"directional_sky_ground_shader_model\",\"environmentSkyColorStatus\":\"ok_preset_uniform\",\"environmentGroundColorStatus\":\"ok_preset_uniform\",\"environmentHorizonStatus\":\"ok_directional_horizon_blend\",\"environmentPerformanceStatus\":\"ok_no_extra_pass_no_texture_upload\",\"iblDiffuseStatus\":\"ok_directional_sky_ground_diffuse\",\"iblSpecularStatus\":\"ok_reflection_direction_environment\",\"iblRoughnessResponseStatus\":\"ok_roughness_blurs_and_reduces_specular\",\"iblMetallicResponseStatus\":\"ok_metal_tints_reflection\",\"iblDielectricResponseStatus\":\"ok_subtle_f0_reflection\",\"iblFabricPreserveStatus\":\"ok_fabric_matte_preserved\",\"iblOverbrightGuardStatus\":\"ok_luminance_guarded\",\"environmentUiStatus\":\"ok_compact_lighting_controls\",\"environmentSliderStatus\":\"ok\",\"skyPresetStatus\":\"ok\",\"horizonControlStatus\":\"ok\",\"environmentUniformUpdateStatus\":\"ok_uniform_only\",\"environmentDebugViewStatus\":\"not_applied\",\"iblDiffuseDebugViewStatus\":\"not_applied\",\"iblSpecularDebugViewStatus\":\"not_applied\",\"reflectionDirectionDebugViewStatus\":\"not_applied\",\"environmentColorDebugViewStatus\":\"not_applied\",\"iblPerformanceStatus\":\"ok_shader_math_only_no_loops\",\"environmentReflectionStatus\":\"p18_environment_directional_source_no_texture_cubemap\",\"environmentReflectionMode\":\"reflection_direction_sky_ground_ibl\",\"environmentSource\":\"directional_sky_ground_shader_model\",\"reflectionColorStatus\":\"missing\",\"reflectionRoughnessResponseStatus\":\"missing\",\"metallicReflectionStatus\":\"missing\",\"dielectricReflectionStatus\":\"missing\",\"reflectionPerformanceStatus\":\"not_ready\",\"sliderUpdateMode\":\"uniform_only\",\"sliderTouchStatus\":\"ok_touch_targets\",\"sunSliderStatus\":\"ok\",\"ambientSliderStatus\":\"ok\",\"exposureSliderStatus\":\"ok\",\"specularSliderStatus\":\"ok\",\"reflectionSliderStatus\":\"ok\",\"reflectionDebugViewStatus\":\"not_applied\",\"gpuUploadStatus\":\"failed\",\"drawStatus\":\"fallback\",\"textureUploadStatus\":\"missing\",\"baseColorTextureStatus\":\"missing\",\"textureFallbackUsed\":true,\"fallbackCubeVisible\":true,\"fallbackCubeStatus\":\"on\",\"modelRenderMode\":\"multi_primitive_static\"}");
    }
    std::string json = std::string("{\"currentScene\":\"") + renderer->diagnostics.renderLab.sceneId()
        + "\",\"currentLabScene\":\"" + renderer->diagnostics.renderLab.sceneId()
        + "\",\"currentLabSceneName\":\"" + renderer->diagnostics.renderLab.sceneName()
        + "\",\"renderingStatus\":\"" + (renderer->model.modelReady() ? "multi_primitive_static" : "fallback_cube") + "\""
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
        + ",\"modelRenderMode\":\"" + renderer->model.modelRenderMode + "\""
        + ",\"primitiveCountTotal\":" + std::to_string(renderer->model.primitiveCountTotal)
        + ",\"primitiveCountRendered\":" + std::to_string(renderer->model.primitiveCountRendered)
        + ",\"primitiveCountSkipped\":" + std::to_string(renderer->model.primitiveCountSkipped)
        + ",\"unsupportedPrimitiveCount\":" + std::to_string(renderer->model.unsupportedPrimitiveCount)
        + ",\"materialSlotCount\":" + std::to_string(renderer->model.materialSlotCount)
        + ",\"materialSlotCountRendered\":" + std::to_string(renderer->model.materialSlotCountRendered)
        + ",\"textureSlotCount\":" + std::to_string(renderer->model.textureSlotCount)
        + ",\"uploadedTextureCount\":" + std::to_string(renderer->model.uploadedTextureCount)
        + ",\"textureFallbackCount\":" + std::to_string(renderer->model.textureFallbackCount)
        + ",\"skippedTextureCount\":" + std::to_string(renderer->model.skippedTextureCount)
        + ",\"textureSlotLimit\":" + std::to_string(renderer->model.textureSlotLimit)
        + ",\"pbrMapsStatus\":\"" + solum::escapeJson(renderer->model.pbrMapsStatus) + "\""
        + ",\"metallicRoughnessStatus\":\"" + solum::escapeJson(renderer->model.metallicRoughnessStatus) + "\""
        + ",\"normalMapStatus\":\"" + solum::escapeJson(renderer->model.normalMapStatus) + "\""
        + ",\"normalMapAppliedStatus\":\"" + solum::escapeJson(renderer->model.normalMapAppliedStatus) + "\""
        + ",\"occlusionMapStatus\":\"" + solum::escapeJson(renderer->model.occlusionMapStatus) + "\""
        + ",\"tangentStatus\":\"" + solum::escapeJson(renderer->model.tangentStatus) + "\""
        + ",\"tangentSource\":\"" + solum::escapeJson(renderer->model.tangentSource) + "\""
        + ",\"tangentGeneratedCount\":" + std::to_string(renderer->model.tangentGeneratedCount)
        + ",\"tangentFallbackGeneratedCount\":" + std::to_string(renderer->model.tangentFallbackGeneratedCount)
        + ",\"tangentMissingCount\":" + std::to_string(renderer->model.tangentMissingCount)
        + ",\"tangentDegenerateTriangleCount\":" + std::to_string(renderer->model.tangentDegenerateTriangleCount)
        + ",\"tangentFallbackReason\":\"" + solum::escapeJson(renderer->model.tangentFallbackReason) + "\""
        + ",\"tangentBuildMode\":\"" + solum::escapeJson(renderer->model.tangentBuildMode) + "\""
        + ",\"metallicFactor\":" + std::to_string(renderer->model.metallicFactor)
        + ",\"roughnessFactor\":" + std::to_string(renderer->model.roughnessFactor)
        + ",\"normalScale\":" + std::to_string(renderer->model.normalScale)
        + ",\"occlusionStrength\":" + std::to_string(renderer->model.occlusionStrength)
        + ",\"pbrTextureSlotCount\":" + std::to_string(renderer->model.pbrTextureSlotCount)
        + ",\"uploadedPbrTextureCount\":" + std::to_string(renderer->model.uploadedPbrTextureCount)
        + ",\"skippedPbrTextureCount\":" + std::to_string(renderer->model.skippedPbrTextureCount)
        + ",\"pbrTextureFallbackCount\":" + std::to_string(renderer->model.pbrTextureFallbackCount)
        + ",\"materialSlotDiagnostics\":" + (renderer->model.materialSlotDiagnostics.empty() ? "[]" : renderer->model.materialSlotDiagnostics)
        + ",\"materialCalibrationStatus\":\"" + solum::escapeJson(renderer->model.materialCalibrationStatus) + "\""
        + ",\"materialCalibrationMode\":\"" + solum::escapeJson(renderer->model.materialCalibrationMode) + "\""
        + ",\"albedoEnergyStatus\":\"" + solum::escapeJson(renderer->model.albedoEnergyStatus) + "\""
        + ",\"albedoClampStatus\":\"" + solum::escapeJson(renderer->model.albedoClampStatus) + "\""
        + ",\"diffuseClampStatus\":\"" + solum::escapeJson(renderer->model.diffuseClampStatus) + "\""
        + ",\"luminanceGuardStatus\":\"" + solum::escapeJson(renderer->model.luminanceGuardStatus) + "\""
        + ",\"aoCalibrationStatus\":\"" + solum::escapeJson(renderer->model.aoCalibrationStatus) + "\""
        + ",\"roughnessRemapStatus\":\"" + solum::escapeJson(renderer->model.roughnessRemapStatus) + "\""
        + ",\"metallicRoughnessClampStatus\":\"" + solum::escapeJson(renderer->model.metallicRoughnessClampStatus) + "\""
        + ",\"emissiveGuardStatus\":\"" + solum::escapeJson(renderer->model.emissiveGuardStatus) + "\""
        + ",\"fabricMattePreserveStatus\":\"" + solum::escapeJson(renderer->model.fabricMattePreserveStatus) + "\""
        + ",\"paintMaterialCalibrationStatus\":\"" + solum::escapeJson(renderer->model.paintMaterialCalibrationStatus) + "\""
        + ",\"metalMaterialCalibrationStatus\":\"" + solum::escapeJson(renderer->model.metalMaterialCalibrationStatus) + "\""
        + ",\"materialTypeHintStatus\":\"" + solum::escapeJson(renderer->model.materialTypeHintStatus) + "\""
        + ",\"materialSlotCalibrationStatus\":\"" + solum::escapeJson(renderer->model.materialSlotCalibrationStatus) + "\""
        + ",\"calibrationUiStatus\":\"" + solum::escapeJson(renderer->model.calibrationUiStatus) + "\""
        + ",\"calibrationPreset\":\"" + solum::escapeJson(renderer->model.calibrationPreset) + "\""
        + ",\"calibrationSliderStatus\":\"" + solum::escapeJson(renderer->model.calibrationSliderStatus) + "\""
        + ",\"calibrationSliderValue\":" + std::to_string(renderer->model.calibrationSliderValue)
        + ",\"calibrationUniformUpdateStatus\":\"" + solum::escapeJson(renderer->model.calibrationUniformUpdateStatus) + "\""
        + ",\"calibratedAlbedoDebugViewStatus\":\"" + solum::escapeJson(renderer->model.calibratedAlbedoDebugViewStatus) + "\""
        + ",\"materialTypeDebugViewStatus\":\"" + solum::escapeJson(renderer->model.materialTypeDebugViewStatus) + "\""
        + ",\"aoInfluenceDebugViewStatus\":\"" + solum::escapeJson(renderer->model.aoInfluenceDebugViewStatus) + "\""
        + ",\"luminanceGuardDebugViewStatus\":\"" + solum::escapeJson(renderer->model.luminanceGuardDebugViewStatus) + "\""
        + ",\"calibrationVisualStrength\":" + std::to_string(renderer->model.calibrationVisualStrength)
        + ",\"calibrationAffectsAlbedo\":\"" + solum::escapeJson(renderer->model.calibrationAffectsAlbedo) + "\""
        + ",\"calibrationAffectsAo\":\"" + solum::escapeJson(renderer->model.calibrationAffectsAo) + "\""
        + ",\"calibrationAffectsRoughness\":\"" + solum::escapeJson(renderer->model.calibrationAffectsRoughness) + "\""
        + ",\"calibrationVisibleResponseStatus\":\"" + solum::escapeJson(renderer->model.calibrationVisibleResponseStatus) + "\""
        + ",\"materialCalibrationPerformanceStatus\":\"" + solum::escapeJson(renderer->model.materialCalibrationPerformanceStatus) + "\""
        + ",\"specularGlossStatus\":\"" + solum::escapeJson(renderer->model.specularGlossStatus) + "\""
        + ",\"specularGlossMode\":\"" + solum::escapeJson(renderer->model.specularGlossMode) + "\""
        + ",\"specularResponseStatus\":\"" + solum::escapeJson(renderer->model.specularResponseStatus) + "\""
        + ",\"glossResponseStatus\":\"" + solum::escapeJson(renderer->model.glossResponseStatus) + "\""
        + ",\"roughnessRemapV2Status\":\"" + solum::escapeJson(renderer->model.roughnessRemapV2Status) + "\""
        + ",\"metallicSpecularBoostStatus\":\"" + solum::escapeJson(renderer->model.metallicSpecularBoostStatus) + "\""
        + ",\"dielectricGlossStatus\":\"" + solum::escapeJson(renderer->model.dielectricGlossStatus) + "\""
        + ",\"fabricSpecularSuppressStatus\":\"" + solum::escapeJson(renderer->model.fabricSpecularSuppressStatus) + "\""
        + ",\"specularOverbrightGuardStatus\":\"" + solum::escapeJson(renderer->model.specularOverbrightGuardStatus) + "\""
        + ",\"viewDependentHighlightStatus\":\"" + solum::escapeJson(renderer->model.viewDependentHighlightStatus) + "\""
        + ",\"paintGlossLiteStatus\":\"" + solum::escapeJson(renderer->model.paintGlossLiteStatus) + "\""
        + ",\"paintGlossLiteMode\":\"" + solum::escapeJson(renderer->model.paintGlossLiteMode) + "\""
        + ",\"paintGlossIntensity\":" + std::to_string(renderer->model.paintGlossIntensity)
        + ",\"paintGlossRoughness\":" + std::to_string(renderer->model.paintGlossRoughness)
        + ",\"paintGlossMaterialHintStatus\":\"" + solum::escapeJson(renderer->model.paintGlossMaterialHintStatus) + "\""
        + ",\"paintGlossPerformanceStatus\":\"" + solum::escapeJson(renderer->model.paintGlossPerformanceStatus) + "\""
        + ",\"paintGlossTargetStatus\":\"" + solum::escapeJson(renderer->model.paintGlossTargetStatus) + "\""
        + ",\"paintGlossAppliedMaterialCount\":" + std::to_string(renderer->model.paintGlossAppliedMaterialCount)
        + ",\"paintGlossSkippedFabricCount\":" + std::to_string(renderer->model.paintGlossSkippedFabricCount)
        + ",\"paintGlossFallbackRouting\":\"" + solum::escapeJson(renderer->model.paintGlossFallbackRouting) + "\""
        + ",\"paintGlossVisibleResponseStatus\":\"" + solum::escapeJson(renderer->model.paintGlossVisibleResponseStatus) + "\""
        + ",\"glossSliderStatus\":\"" + solum::escapeJson(renderer->model.glossSliderStatus) + "\""
        + ",\"glossSliderValue\":" + std::to_string(renderer->model.glossSliderValue)
        + ",\"paintGlossSliderStatus\":\"" + solum::escapeJson(renderer->model.paintGlossSliderStatus) + "\""
        + ",\"paintGlossSliderValue\":" + std::to_string(renderer->model.paintGlossSliderValue)
        + ",\"glossUniformUpdateStatus\":\"" + solum::escapeJson(renderer->model.glossUniformUpdateStatus) + "\""
        + ",\"glossResponseDebugViewStatus\":\"" + solum::escapeJson(renderer->model.glossResponseDebugViewStatus) + "\""
        + ",\"specularGuardDebugViewStatus\":\"" + solum::escapeJson(renderer->model.specularGuardDebugViewStatus) + "\""
        + ",\"paintGlossDebugViewStatus\":\"" + solum::escapeJson(renderer->model.paintGlossDebugViewStatus) + "\""
        + ",\"metalResponseDebugViewStatus\":\"" + solum::escapeJson(renderer->model.metalResponseDebugViewStatus) + "\""
        + ",\"paintTargetDebugViewStatus\":\"" + solum::escapeJson(renderer->model.paintTargetDebugViewStatus) + "\""
        + ",\"calibrationResponseDebugViewStatus\":\"" + solum::escapeJson(renderer->model.calibrationResponseDebugViewStatus) + "\""
        + ",\"materialTypeSpecularRoutingStatus\":\"" + solum::escapeJson(renderer->model.materialTypeSpecularRoutingStatus) + "\""
        + ",\"paintMaterialGlossStatus\":\"" + solum::escapeJson(renderer->model.paintMaterialGlossStatus) + "\""
        + ",\"metalMaterialGlossStatus\":\"" + solum::escapeJson(renderer->model.metalMaterialGlossStatus) + "\""
        + ",\"rubberMaterialGlossStatus\":\"" + solum::escapeJson(renderer->model.rubberMaterialGlossStatus) + "\""
        + ",\"specularGlossPerformanceStatus\":\"" + solum::escapeJson(renderer->model.specularGlossPerformanceStatus) + "\""
        + ",\"glossVisibleResponseStatus\":\"" + solum::escapeJson(renderer->model.glossVisibleResponseStatus) + "\""
        + ",\"glossAffectsSpecularLobe\":\"" + solum::escapeJson(renderer->model.glossAffectsSpecularLobe) + "\""
        + ",\"glossAffectsReflectionWeight\":\"" + solum::escapeJson(renderer->model.glossAffectsReflectionWeight) + "\""
        + ",\"lightingStatus\":\"" + solum::escapeJson(renderer->model.lightingStatus) + "\""
        + ",\"lightingControlStatus\":\"" + solum::escapeJson(renderer->model.lightingControlStatus) + "\""
        + ",\"lightingUiMode\":\"" + solum::escapeJson(renderer->model.lightingUiMode) + "\""
        + ",\"sunDirection\":[" + std::to_string(renderer->model.sunDirection[0]) + "," + std::to_string(renderer->model.sunDirection[1]) + "," + std::to_string(renderer->model.sunDirection[2]) + "]"
        + ",\"sunColor\":[" + std::to_string(renderer->model.sunColor[0]) + "," + std::to_string(renderer->model.sunColor[1]) + "," + std::to_string(renderer->model.sunColor[2]) + "]"
        + ",\"sunIntensity\":" + std::to_string(renderer->model.sunIntensity)
        + ",\"ambientColor\":[" + std::to_string(renderer->model.ambientColor[0]) + "," + std::to_string(renderer->model.ambientColor[1]) + "," + std::to_string(renderer->model.ambientColor[2]) + "]"
        + ",\"ambientIntensity\":" + std::to_string(renderer->model.ambientIntensity)
        + ",\"lightPreset\":\"" + solum::escapeJson(renderer->model.lightPreset) + "\""
        + ",\"specularBoost\":" + std::to_string(renderer->model.specularBoost)
        + ",\"specularBoostStatus\":\"" + solum::escapeJson(renderer->model.specularBoostStatus) + "\""
        + ",\"reflectionIntensity\":" + std::to_string(renderer->model.reflectionIntensity)
        + ",\"iblStatus\":\"" + solum::escapeJson(renderer->model.iblStatus) + "\""
        + ",\"iblMode\":\"" + solum::escapeJson(renderer->model.iblMode) + "\""
        + ",\"environmentIblStatus\":\"" + solum::escapeJson(renderer->model.environmentIblStatus) + "\""
        + ",\"environmentIblMode\":\"" + solum::escapeJson(renderer->model.environmentIblMode) + "\""
        + ",\"environmentSourceStatus\":\"" + solum::escapeJson(renderer->model.environmentSourceStatus) + "\""
        + ",\"environmentSourceType\":\"" + solum::escapeJson(renderer->model.environmentSourceType) + "\""
        + ",\"environmentSkyColorStatus\":\"" + solum::escapeJson(renderer->model.environmentSkyColorStatus) + "\""
        + ",\"environmentGroundColorStatus\":\"" + solum::escapeJson(renderer->model.environmentGroundColorStatus) + "\""
        + ",\"environmentHorizonStatus\":\"" + solum::escapeJson(renderer->model.environmentHorizonStatus) + "\""
        + ",\"environmentPerformanceStatus\":\"" + solum::escapeJson(renderer->model.environmentPerformanceStatus) + "\""
        + ",\"iblDiffuseStatus\":\"" + solum::escapeJson(renderer->model.iblDiffuseStatus) + "\""
        + ",\"iblSpecularStatus\":\"" + solum::escapeJson(renderer->model.iblSpecularStatus) + "\""
        + ",\"iblRoughnessResponseStatus\":\"" + solum::escapeJson(renderer->model.iblRoughnessResponseStatus) + "\""
        + ",\"iblMetallicResponseStatus\":\"" + solum::escapeJson(renderer->model.iblMetallicResponseStatus) + "\""
        + ",\"iblDielectricResponseStatus\":\"" + solum::escapeJson(renderer->model.iblDielectricResponseStatus) + "\""
        + ",\"iblFabricPreserveStatus\":\"" + solum::escapeJson(renderer->model.iblFabricPreserveStatus) + "\""
        + ",\"iblOverbrightGuardStatus\":\"" + solum::escapeJson(renderer->model.iblOverbrightGuardStatus) + "\""
        + ",\"environmentUiStatus\":\"" + solum::escapeJson(renderer->model.environmentUiStatus) + "\""
        + ",\"environmentPreset\":\"" + solum::escapeJson(renderer->model.environmentPreset) + "\""
        + ",\"environmentIntensity\":" + std::to_string(renderer->model.environmentIntensity)
        + ",\"environmentSliderStatus\":\"" + solum::escapeJson(renderer->model.environmentSliderStatus) + "\""
        + ",\"skyPresetStatus\":\"" + solum::escapeJson(renderer->model.skyPresetStatus) + "\""
        + ",\"horizonControlStatus\":\"" + solum::escapeJson(renderer->model.horizonControlStatus) + "\""
        + ",\"environmentUniformUpdateStatus\":\"" + solum::escapeJson(renderer->model.environmentUniformUpdateStatus) + "\""
        + ",\"environmentDebugViewStatus\":\"" + solum::escapeJson(renderer->model.environmentDebugViewStatus) + "\""
        + ",\"reflectionDirectionDebugViewStatus\":\"" + solum::escapeJson(renderer->model.reflectionDirectionDebugViewStatus) + "\""
        + ",\"environmentColorDebugViewStatus\":\"" + solum::escapeJson(renderer->model.environmentColorDebugViewStatus) + "\""
        + ",\"iblPerformanceStatus\":\"" + solum::escapeJson(renderer->model.iblPerformanceStatus) + "\""
        + ",\"reflectionFoundationStatus\":\"" + solum::escapeJson(renderer->model.reflectionFoundationStatus) + "\""
        + ",\"reflectionMode\":\"" + solum::escapeJson(renderer->model.reflectionMode) + "\""
        + ",\"environmentReflectionStatus\":\"" + solum::escapeJson(renderer->model.environmentReflectionStatus) + "\""
        + ",\"environmentReflectionMode\":\"" + solum::escapeJson(renderer->model.environmentReflectionMode) + "\""
        + ",\"environmentSource\":\"" + solum::escapeJson(renderer->model.environmentSource) + "\""
        + ",\"reflectionColorStatus\":\"" + solum::escapeJson(renderer->model.reflectionColorStatus) + "\""
        + ",\"reflectionRoughnessResponseStatus\":\"" + solum::escapeJson(renderer->model.reflectionRoughnessResponseStatus) + "\""
        + ",\"metallicReflectionStatus\":\"" + solum::escapeJson(renderer->model.metallicReflectionStatus) + "\""
        + ",\"dielectricReflectionStatus\":\"" + solum::escapeJson(renderer->model.dielectricReflectionStatus) + "\""
        + ",\"reflectionPerformanceStatus\":\"" + solum::escapeJson(renderer->model.reflectionPerformanceStatus) + "\""
        + ",\"inspectorUiStatus\":\"" + solum::escapeJson(renderer->model.inspectorUiStatus) + "\""
        + ",\"inspectorUiMode\":\"" + solum::escapeJson(renderer->model.inspectorUiMode) + "\""
        + ",\"activeInspectorTab\":\"" + solum::escapeJson(renderer->model.activeInspectorTab) + "\""
        + ",\"assetsTabStatus\":\"" + solum::escapeJson(renderer->model.assetsTabStatus) + "\""
        + ",\"cameraTabStatus\":\"" + solum::escapeJson(renderer->model.cameraTabStatus) + "\""
        + ",\"lightingTabStatus\":\"" + solum::escapeJson(renderer->model.lightingTabStatus) + "\""
        + ",\"materialTabStatus\":\"" + solum::escapeJson(renderer->model.materialTabStatus) + "\""
        + ",\"debugTabStatus\":\"" + solum::escapeJson(renderer->model.debugTabStatus) + "\""
        + ",\"contactGroundingStatus\":\"" + solum::escapeJson(renderer->model.contactGroundingStatus) + "\""
        + ",\"contactShadowStatus\":\"" + solum::escapeJson(renderer->model.contactShadowStatus) + "\""
        + ",\"contactShadowMode\":\"" + solum::escapeJson(renderer->model.contactShadowMode) + "\""
        + ",\"contactShadowIntensity\":" + std::to_string(renderer->model.contactShadowIntensity)
        + ",\"contactShadowPerformanceStatus\":\"" + solum::escapeJson(renderer->model.contactShadowPerformanceStatus) + "\""
        + ",\"groundingUsesModelBounds\":\"" + solum::escapeJson(renderer->model.groundingUsesModelBounds) + "\""
        + ",\"groundingUniformUpdateStatus\":\"" + solum::escapeJson(renderer->model.groundingUniformUpdateStatus) + "\""
        + ",\"groundSliderStatus\":\"" + solum::escapeJson(renderer->model.groundSliderStatus) + "\""
        + ",\"contactGroundingSliderStatus\":\"" + solum::escapeJson(renderer->model.contactGroundingSliderStatus) + "\""
        + ",\"lightingUniformUpdateStatus\":\"" + solum::escapeJson(renderer->model.lightingUniformUpdateStatus) + "\""
        + ",\"sliderUpdateMode\":\"" + solum::escapeJson(renderer->model.sliderUpdateMode) + "\""
        + ",\"sliderTouchStatus\":\"" + solum::escapeJson(renderer->model.sliderTouchStatus) + "\""
        + ",\"sunSliderStatus\":\"" + solum::escapeJson(renderer->model.sunSliderStatus) + "\""
        + ",\"ambientSliderStatus\":\"" + solum::escapeJson(renderer->model.ambientSliderStatus) + "\""
        + ",\"exposureSliderStatus\":\"" + solum::escapeJson(renderer->model.exposureSliderStatus) + "\""
        + ",\"specularSliderStatus\":\"" + solum::escapeJson(renderer->model.specularSliderStatus) + "\""
        + ",\"reflectionSliderStatus\":\"" + solum::escapeJson(renderer->model.reflectionSliderStatus) + "\""
        + ",\"environmentSliderStatus\":\"" + solum::escapeJson(renderer->model.environmentSliderStatus) + "\""
        + ",\"brdfStatus\":\"" + solum::escapeJson(renderer->model.brdfStatus) + "\""
        + ",\"brdfMode\":\"" + solum::escapeJson(renderer->model.brdfMode) + "\""
        + ",\"diffuseStatus\":\"" + solum::escapeJson(renderer->model.diffuseStatus) + "\""
        + ",\"specularStatus\":\"" + solum::escapeJson(renderer->model.specularStatus) + "\""
        + ",\"fresnelStatus\":\"" + solum::escapeJson(renderer->model.fresnelStatus) + "\""
        + ",\"f0Status\":\"" + solum::escapeJson(renderer->model.f0Status) + "\""
        + ",\"metallicResponseStatus\":\"" + solum::escapeJson(renderer->model.metallicResponseStatus) + "\""
        + ",\"roughnessResponseStatus\":\"" + solum::escapeJson(renderer->model.roughnessResponseStatus) + "\""
        + ",\"directLightingStatus\":\"" + solum::escapeJson(renderer->model.directLightingStatus) + "\""
        + ",\"materialResponseStatus\":\"" + solum::escapeJson(renderer->model.materialResponseStatus) + "\""
        + ",\"pbrQualityTier\":\"" + solum::escapeJson(renderer->model.pbrQualityTier) + "\""
        + ",\"brdfPerformanceStatus\":\"" + solum::escapeJson(renderer->model.brdfPerformanceStatus) + "\""
        + ",\"toneMappingStatus\":\"" + solum::escapeJson(renderer->model.toneMappingStatus) + "\""
        + ",\"toneMappingMode\":\"" + solum::escapeJson(renderer->model.toneMappingMode) + "\""
        + ",\"exposureStatus\":\"" + solum::escapeJson(renderer->model.exposureStatus) + "\""
        + ",\"exposureValue\":" + std::to_string(renderer->model.exposureValue)
        + ",\"ambientFloor\":" + std::to_string(renderer->model.ambientFloor)
        + ",\"brightnessPreset\":\"" + solum::escapeJson(renderer->model.brightnessPreset) + "\""
        + ",\"activeDebugView\":\"" + solum::escapeJson(renderer->model.activeDebugView) + "\""
        + ",\"debugViewStatus\":\"" + solum::escapeJson(renderer->model.debugViewStatus) + "\""
        + ",\"normalDebugViewStatus\":\"" + solum::escapeJson(renderer->model.normalDebugViewStatus) + "\""
        + ",\"ndotlDebugViewStatus\":\"" + solum::escapeJson(renderer->model.ndotlDebugViewStatus) + "\""
        + ",\"diffuseDebugViewStatus\":\"" + solum::escapeJson(renderer->model.diffuseDebugViewStatus) + "\""
        + ",\"specularDebugViewStatus\":\"" + solum::escapeJson(renderer->model.specularDebugViewStatus) + "\""
        + ",\"f0DebugViewStatus\":\"" + solum::escapeJson(renderer->model.f0DebugViewStatus) + "\""
        + ",\"reflectionDebugViewStatus\":\"" + solum::escapeJson(renderer->model.reflectionDebugViewStatus) + "\""
        + ",\"iblDiffuseDebugViewStatus\":\"" + solum::escapeJson(renderer->model.iblDiffuseDebugViewStatus) + "\""
        + ",\"iblSpecularDebugViewStatus\":\"" + solum::escapeJson(renderer->model.iblSpecularDebugViewStatus) + "\""
        + ",\"environmentDebugViewStatus\":\"" + solum::escapeJson(renderer->model.environmentDebugViewStatus) + "\""
        + ",\"reflectionDirectionDebugViewStatus\":\"" + solum::escapeJson(renderer->model.reflectionDirectionDebugViewStatus) + "\""
        + ",\"environmentColorDebugViewStatus\":\"" + solum::escapeJson(renderer->model.environmentColorDebugViewStatus) + "\""
        + ",\"brdfStatusDebugViewStatus\":\"" + solum::escapeJson(renderer->model.brdfStatusDebugViewStatus) + "\""
        + ",\"groundingDebugViewStatus\":\"" + solum::escapeJson(renderer->model.groundingDebugViewStatus) + "\""
        + ",\"fpsCurrent\":" + std::to_string(renderer->model.fpsCurrent)
        + ",\"frameTimeMs\":" + std::to_string(renderer->model.frameTimeMs)
        + ",\"fpsSource\":\"" + solum::escapeJson(renderer->model.fpsSource) + "\""
        + ",\"fpsLastStable\":" + std::to_string(renderer->model.fpsLastStable)
        + ",\"frameTimeLastStableMs\":" + std::to_string(renderer->model.frameTimeLastStableMs)
        + ",\"fpsStatus\":\"" + solum::escapeJson(renderer->model.fpsStatus) + "\""
        + ",\"fpsUpdateMode\":\"" + solum::escapeJson(renderer->model.fpsUpdateMode) + "\""
        + ",\"fpsSampleWindowMs\":" + std::to_string(renderer->model.fpsSampleWindowMs)
        + ",\"framesRenderedLive\":" + std::to_string(renderer->model.framesRenderedLive)
        + ",\"modelUploadRepeatCount\":" + std::to_string(renderer->model.modelUploadRepeatCount)
        + ",\"uploadGenerationId\":" + std::to_string(renderer->model.uploadGenerationId)
        + ",\"renderLoopAllocationGuardStatus\":\"" + solum::escapeJson(renderer->model.renderLoopAllocationGuardStatus) + "\""
        + ",\"debugZipStatus\":\"" + solum::escapeJson(renderer->model.debugZipStatus) + "\""
        + ",\"debugZipPath\":\"" + solum::escapeJson(renderer->model.debugZipPath) + "\""
        + ",\"debugZipIncludedFiles\":\"" + solum::escapeJson(renderer->model.debugZipIncludedFiles) + "\""
        + ",\"debugZipReason\":\"" + solum::escapeJson(renderer->model.debugZipReason) + "\""
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
        + ",\"vertexLayout\":\"POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT\""
        + ",\"vertexStrideBytes\":" + std::to_string(renderer->diagnostics.vertexStrideBytes)
        + ",\"material\":{\"materialId\":" + std::to_string(renderer->diagnostics.material.materialId)
        + ",\"baseColorFactor\":[" + std::to_string(renderer->diagnostics.material.baseColorFactor[0]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[1]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[2]) + "," + std::to_string(renderer->diagnostics.material.baseColorFactor[3]) + "]"
        + ",\"metallicFactor\":" + std::to_string(renderer->diagnostics.material.metallicFactor)
        + ",\"roughnessFactor\":" + std::to_string(renderer->diagnostics.material.roughnessFactor)
        + ",\"normalScale\":" + std::to_string(renderer->diagnostics.material.normalScale)
        + ",\"occlusionStrength\":" + std::to_string(renderer->diagnostics.material.occlusionStrength)
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
    if (vertexFloatCount <= 0 || (vertexFloatCount % 15) != 0) {
        renderer->setModelFallback(name, path, "vertex data must use POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT stride 15 floats");
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
        (uint32_t)(vertexFloatCount / 15),
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

extern "C" JNIEXPORT jboolean JNICALL Java_com_solum_engine_MainActivity_nativeUploadModelMultiPrimitive(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring modelName,
    jstring modelPath,
    jfloatArray vertexData,
    jintArray indexData,
    jintArray rangeData,
    jfloatArray materialData,
    jfloatArray boundsMinArray,
    jfloatArray boundsMaxArray,
    jfloatArray boundsCenterArray,
    jfloat modelScale,
    jint primitiveTotal,
    jint primitiveSkipped,
    jint unsupportedPrimitiveCount,
    jstring reasonText
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer || !vertexData || !rangeData) return JNI_FALSE;
    const char* nameChars = env->GetStringUTFChars(modelName, nullptr);
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    const char* reasonChars = env->GetStringUTFChars(reasonText, nullptr);
    std::string name = nameChars ? nameChars : "imported.glb";
    std::string path = pathChars ? pathChars : "";
    std::string reason = reasonChars ? reasonChars : "";
    jsize vertexFloatCount = env->GetArrayLength(vertexData);
    jsize rangeIntCount = env->GetArrayLength(rangeData);
    if (vertexFloatCount <= 0 || (vertexFloatCount % 15) != 0 || rangeIntCount <= 0 || (rangeIntCount % 6) != 0) {
        renderer->setModelFallback(name, path, "multi primitive data has invalid vertex/range stride");
        if (nameChars) env->ReleaseStringUTFChars(modelName, nameChars);
        if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);
        if (reasonChars) env->ReleaseStringUTFChars(reasonText, reasonChars);
        return JNI_FALSE;
    }
    jfloat* vertices = env->GetFloatArrayElements(vertexData, nullptr);
    jint* indices = indexData ? env->GetIntArrayElements(indexData, nullptr) : nullptr;
    jint* ranges = env->GetIntArrayElements(rangeData, nullptr);
    jfloat* materials = materialData ? env->GetFloatArrayElements(materialData, nullptr) : nullptr;
    jsize indexCount = indexData ? env->GetArrayLength(indexData) : 0;
    jsize materialFloatCount = materialData ? env->GetArrayLength(materialData) : 0;
    std::vector<uint32_t> index32;
    if (indices && indexCount > 0) {
        index32.resize((size_t)indexCount);
        for (jsize i = 0; i < indexCount; ++i) index32[(size_t)i] = (uint32_t)indices[i];
    }
    std::vector<solum::PrimitiveDrawRange> drawRanges((size_t)rangeIntCount / 6u);
    for (size_t i = 0; i < drawRanges.size(); ++i) {
        drawRanges[i].firstIndex = (uint32_t)ranges[i * 6u];
        drawRanges[i].indexCount = (uint32_t)ranges[i * 6u + 1u];
        drawRanges[i].firstVertex = (uint32_t)ranges[i * 6u + 2u];
        drawRanges[i].vertexCount = (uint32_t)ranges[i * 6u + 3u];
        drawRanges[i].materialSlot = ranges[i * 6u + 4u];
        drawRanges[i].textureSlot = ranges[i * 6u + 5u];
    }
    std::vector<solum::MaterialSlotState> slots;
    if (materials && materialFloatCount >= 16 && (materialFloatCount % 16) == 0) {
        slots.resize((size_t)materialFloatCount / 16u);
        for (size_t i = 0; i < slots.size(); ++i) {
            slots[i].baseColorFactor[0] = materials[i * 16u];
            slots[i].baseColorFactor[1] = materials[i * 16u + 1u];
            slots[i].baseColorFactor[2] = materials[i * 16u + 2u];
            slots[i].baseColorFactor[3] = materials[i * 16u + 3u];
            slots[i].alphaMode = (int)materials[i * 16u + 4u];
            slots[i].alphaCutoff = materials[i * 16u + 5u];
            slots[i].doubleSided = materials[i * 16u + 6u] != 0.0f;
            slots[i].baseColorTextureSlot = (int)materials[i * 16u + 7u];
            slots[i].metallicFactor = materials[i * 16u + 8u];
            slots[i].roughnessFactor = materials[i * 16u + 9u];
            slots[i].metallicRoughnessTextureSlot = (int)materials[i * 16u + 10u];
            slots[i].normalTextureSlot = (int)materials[i * 16u + 11u];
            slots[i].occlusionTextureSlot = (int)materials[i * 16u + 12u];
            slots[i].normalScale = materials[i * 16u + 13u];
            slots[i].occlusionStrength = materials[i * 16u + 14u];
            slots[i].materialTypeHint = (int)materials[i * 16u + 15u];
        }
    } else if (materials && materialFloatCount >= 8 && (materialFloatCount % 8) == 0) {
        slots.resize((size_t)materialFloatCount / 8u);
        for (size_t i = 0; i < slots.size(); ++i) {
            slots[i].baseColorFactor[0] = materials[i * 8u];
            slots[i].baseColorFactor[1] = materials[i * 8u + 1u];
            slots[i].baseColorFactor[2] = materials[i * 8u + 2u];
            slots[i].baseColorFactor[3] = materials[i * 8u + 3u];
            slots[i].alphaMode = (int)materials[i * 8u + 4u];
            slots[i].alphaCutoff = materials[i * 8u + 5u];
            slots[i].doubleSided = materials[i * 8u + 6u] != 0.0f;
            slots[i].baseColorTextureSlot = (int)materials[i * 8u + 7u];
        }
    } else {
        slots.resize(1);
    }
    float boundsMin[3] = {0.0f, 0.0f, 0.0f};
    float boundsMax[3] = {0.0f, 0.0f, 0.0f};
    float boundsCenter[3] = {0.0f, 0.0f, 0.0f};
    if (boundsMinArray && env->GetArrayLength(boundsMinArray) >= 3) env->GetFloatArrayRegion(boundsMinArray, 0, 3, boundsMin);
    if (boundsMaxArray && env->GetArrayLength(boundsMaxArray) >= 3) env->GetFloatArrayRegion(boundsMaxArray, 0, 3, boundsMax);
    if (boundsCenterArray && env->GetArrayLength(boundsCenterArray) >= 3) env->GetFloatArrayRegion(boundsCenterArray, 0, 3, boundsCenter);
    std::string error;
    bool ok = renderer->uploadModelMultiPrimitive(
        name,
        path,
        reinterpret_cast<const solum::Vertex3D*>(vertices),
        (uint32_t)(vertexFloatCount / 15),
        index32.empty() ? nullptr : index32.data(),
        (uint32_t)index32.size(),
        drawRanges.data(),
        (uint32_t)drawRanges.size(),
        slots.data(),
        (uint32_t)slots.size(),
        boundsMin,
        boundsMax,
        boundsCenter,
        modelScale,
        (uint32_t)primitiveTotal,
        (uint32_t)primitiveSkipped,
        (uint32_t)unsupportedPrimitiveCount,
        reason,
        error
    );
    env->ReleaseFloatArrayElements(vertexData, vertices, JNI_ABORT);
    if (indices) env->ReleaseIntArrayElements(indexData, indices, JNI_ABORT);
    env->ReleaseIntArrayElements(rangeData, ranges, JNI_ABORT);
    if (materials) env->ReleaseFloatArrayElements(materialData, materials, JNI_ABORT);
    if (nameChars) env->ReleaseStringUTFChars(modelName, nameChars);
    if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);
    if (reasonChars) env->ReleaseStringUTFChars(reasonText, reasonChars);
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

extern "C" JNIEXPORT jboolean JNICALL Java_com_solum_engine_MainActivity_nativeUploadBaseColorTextureSlot(
    JNIEnv* env,
    jclass,
    jlong handle,
    jint slot,
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
        renderer->model.textureFallbackCount += 1;
        renderer->model.reason = "texture slot pixel count mismatch";
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
    bool ok = renderer->uploadBaseColorTextureSlot(
        slot,
        rgba.data(),
        (uint32_t)width,
        (uint32_t)height,
        nameChars ? nameChars : "baseColorTexture",
        sourceChars ? sourceChars : "glb.bufferView",
        mimeChars ? mimeChars : "unknown",
        error
    );
    if (!ok) renderer->model.reason = error.empty() ? "texture slot upload failed" : error;
    env->ReleaseIntArrayElements(rgbaPixels, pixels, JNI_ABORT);
    if (nameChars) env->ReleaseStringUTFChars(textureName, nameChars);
    if (sourceChars) env->ReleaseStringUTFChars(textureSource, sourceChars);
    if (mimeChars) env->ReleaseStringUTFChars(mimeType, mimeChars);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_solum_engine_MainActivity_nativeUploadPbrTextureSlot(
    JNIEnv* env,
    jclass,
    jlong handle,
    jint materialSlot,
    jint textureKind,
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
        renderer->model.pbrTextureFallbackCount += 1;
        renderer->model.reason = "pbr texture slot pixel count mismatch";
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
    bool ok = renderer->uploadPbrTextureSlot(
        materialSlot,
        textureKind,
        rgba.data(),
        (uint32_t)width,
        (uint32_t)height,
        nameChars ? nameChars : "pbrTexture",
        sourceChars ? sourceChars : "glb.bufferView",
        mimeChars ? mimeChars : "unknown",
        error
    );
    if (!ok) renderer->model.reason = error.empty() ? "pbr texture slot upload failed" : error;
    env->ReleaseIntArrayElements(rgbaPixels, pixels, JNI_ABORT);
    if (nameChars) env->ReleaseStringUTFChars(textureName, nameChars);
    if (sourceChars) env->ReleaseStringUTFChars(textureSource, sourceChars);
    if (mimeChars) env->ReleaseStringUTFChars(mimeType, mimeChars);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_solum_engine_MainActivity_nativeSetPbrDiagnostics(
    JNIEnv* env,
    jclass,
    jlong handle,
    jstring pbrMapsStatus,
    jstring metallicRoughnessStatus,
    jstring normalMapStatus,
    jstring normalMapAppliedStatus,
    jstring occlusionMapStatus,
    jstring tangentStatus,
    jstring tangentSource,
    jint tangentGeneratedCount,
    jint tangentFallbackGeneratedCount,
    jint tangentMissingCount,
    jint tangentDegenerateTriangleCount,
    jstring tangentFallbackReason,
    jstring tangentBuildMode,
    jint modelUploadRepeatCount,
    jint uploadGenerationId,
    jstring renderLoopAllocationGuardStatus,
    jfloat metallicFactor,
    jfloat roughnessFactor,
    jfloat normalScale,
    jfloat occlusionStrength,
    jint pbrTextureSlotCount,
    jint uploadedPbrTextureCount,
    jint skippedPbrTextureCount,
    jint pbrTextureFallbackCount,
    jstring materialSlotDiagnostics
) {
    auto* renderer = reinterpret_cast<solum::RendererCore*>(handle);
    if (!renderer) return;
    const char* pbrChars = env->GetStringUTFChars(pbrMapsStatus, nullptr);
    const char* mrChars = env->GetStringUTFChars(metallicRoughnessStatus, nullptr);
    const char* normalChars = env->GetStringUTFChars(normalMapStatus, nullptr);
    const char* normalAppliedChars = env->GetStringUTFChars(normalMapAppliedStatus, nullptr);
    const char* aoChars = env->GetStringUTFChars(occlusionMapStatus, nullptr);
    const char* tangentStatusChars = env->GetStringUTFChars(tangentStatus, nullptr);
    const char* tangentSourceChars = env->GetStringUTFChars(tangentSource, nullptr);
    const char* tangentFallbackChars = env->GetStringUTFChars(tangentFallbackReason, nullptr);
    const char* tangentBuildModeChars = env->GetStringUTFChars(tangentBuildMode, nullptr);
    const char* allocationGuardChars = env->GetStringUTFChars(renderLoopAllocationGuardStatus, nullptr);
    const char* diagChars = env->GetStringUTFChars(materialSlotDiagnostics, nullptr);
    renderer->model.pbrMapsStatus = pbrChars ? pbrChars : "missing";
    renderer->model.metallicRoughnessStatus = mrChars ? mrChars : "missing";
    renderer->model.normalMapStatus = normalChars ? normalChars : "missing";
    renderer->model.normalMapAppliedStatus = normalAppliedChars ? normalAppliedChars : "missing";
    renderer->model.occlusionMapStatus = aoChars ? aoChars : "missing";
    renderer->model.tangentStatus = tangentStatusChars ? tangentStatusChars : "missing_or_blocked";
    renderer->model.tangentSource = tangentSourceChars ? tangentSourceChars : "missing";
    renderer->model.tangentGeneratedCount = (uint32_t)std::max(0, (int)tangentGeneratedCount);
    renderer->model.tangentFallbackGeneratedCount = (uint32_t)std::max(0, (int)tangentFallbackGeneratedCount);
    renderer->model.tangentMissingCount = (uint32_t)std::max(0, (int)tangentMissingCount);
    renderer->model.tangentDegenerateTriangleCount = (uint32_t)std::max(0, (int)tangentDegenerateTriangleCount);
    renderer->model.tangentFallbackReason = tangentFallbackChars ? tangentFallbackChars : "not_loaded";
    renderer->model.tangentBuildMode = tangentBuildModeChars ? tangentBuildModeChars : "once_on_upload";
    renderer->model.modelUploadRepeatCount = (uint32_t)std::max(0, (int)modelUploadRepeatCount);
    renderer->model.uploadGenerationId = (uint32_t)std::max(0, (int)uploadGenerationId);
    renderer->model.renderLoopAllocationGuardStatus = allocationGuardChars ? allocationGuardChars : "ok_no_java_glb_parse_or_upload_in_frame_callback";
    renderer->model.metallicFactor = metallicFactor;
    renderer->model.roughnessFactor = roughnessFactor;
    renderer->model.normalScale = normalScale;
    renderer->model.occlusionStrength = occlusionStrength;
    renderer->model.pbrTextureSlotCount = (uint32_t)std::max(0, (int)pbrTextureSlotCount);
    renderer->model.uploadedPbrTextureCount = (uint32_t)std::max(0, (int)uploadedPbrTextureCount);
    renderer->model.skippedPbrTextureCount = (uint32_t)std::max(0, (int)skippedPbrTextureCount);
    renderer->model.pbrTextureFallbackCount = (uint32_t)std::max(0, (int)pbrTextureFallbackCount);
    renderer->model.materialSlotDiagnostics = diagChars ? diagChars : "[]";
    renderer->syncDiagnostics();
    renderer->updateReadyStatus();
    if (pbrChars) env->ReleaseStringUTFChars(pbrMapsStatus, pbrChars);
    if (mrChars) env->ReleaseStringUTFChars(metallicRoughnessStatus, mrChars);
    if (normalChars) env->ReleaseStringUTFChars(normalMapStatus, normalChars);
    if (normalAppliedChars) env->ReleaseStringUTFChars(normalMapAppliedStatus, normalAppliedChars);
    if (aoChars) env->ReleaseStringUTFChars(occlusionMapStatus, aoChars);
    if (tangentStatusChars) env->ReleaseStringUTFChars(tangentStatus, tangentStatusChars);
    if (tangentSourceChars) env->ReleaseStringUTFChars(tangentSource, tangentSourceChars);
    if (tangentFallbackChars) env->ReleaseStringUTFChars(tangentFallbackReason, tangentFallbackChars);
    if (tangentBuildModeChars) env->ReleaseStringUTFChars(tangentBuildMode, tangentBuildModeChars);
    if (allocationGuardChars) env->ReleaseStringUTFChars(renderLoopAllocationGuardStatus, allocationGuardChars);
    if (diagChars) env->ReleaseStringUTFChars(materialSlotDiagnostics, diagChars);
}
