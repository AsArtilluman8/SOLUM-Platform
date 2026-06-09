package com.solum.engine.render;

import com.google.android.filament.View;
import com.google.android.filament.View.AmbientOcclusion;
import com.google.android.filament.View.AntiAliasing;
import com.google.android.filament.View.Dithering;
import com.google.android.filament.View.QualityLevel;
import com.google.android.filament.View.ShadowType;

import java.util.Locale;

public class FilamentRenderController implements RenderControlApi {
    private View view;
    private final RenderSettings settings = new RenderSettings();
    private final RenderActualState actualState = new RenderActualState();
    private final RenderDiagnostics diagnostics = new RenderDiagnostics();

    public FilamentRenderController(View view) {
        this.view = view;
    }

    public void setView(View view) {
        this.view = view;
    }

    @Override
    public RenderSettings getSettings() { return settings; }

    @Override
    public RenderActualState getActualState() { return actualState; }

    @Override
    public RenderDiagnostics getDiagnostics() { return diagnostics; }

    @Override
    public void apply() {
        diagnostics.clearNotExposedOrNotVerifiedItems();
        if (view == null) {
            diagnostics.setRenderTruthText("render_api_view_missing");
            diagnostics.addNotExposedOrNotVerifiedItem("filament_view_missing");
            return;
        }
        try {
            QualityLevel optionQuality = optionQuality();
            float minScale = dynamicMinScale();
            float maxScale = Math.max(minScale, settings.getRenderScale());

            view.setAntiAliasing(settings.isFxaa() ? AntiAliasing.FXAA : AntiAliasing.NONE);
            actualState.setFxaaApplyStatus(RenderActualState.APPLIED);

            view.setSampleCount(settings.getMsaaSampleCount());
            actualState.setActualMsaa(settings.getMsaaSampleCount());
            actualState.setMsaaApplyStatus("requested_" + settings.getMsaaSampleCount() + "x_actual_" + settings.getMsaaSampleCount() + "x_applied_live_not_runtime_verified");

            view.setAmbientOcclusion(aoEnabled() ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
            applyAo(optionQuality);

            boolean shadowsEnabled = !isMode("OFF", settings.getShadowsMode());
            view.setShadowingEnabled(shadowsEnabled);
            applyShadows(shadowsEnabled);

            view.setScreenSpaceRefractionEnabled(settings.isRefraction());
            actualState.setRefractionApplyStatus(settings.isRefraction() ? RenderActualState.APPLIED : "off");

            view.setPostProcessingEnabled(true);
            applyBloom(optionQuality);
            applyDynamicResolution(optionQuality, minScale, maxScale);
            applyRenderQuality();
            view.setDithering(settings.isDithering() ? Dithering.TEMPORAL : Dithering.NONE);
            actualState.setDitheringApplyStatus(settings.isDithering() ? "TEMPORAL" : "NONE");
            applyTaa();
            applySsr();

            actualState.setColorApplyStatus("activity_local_applied_by_FilamentGlbPreviewActivity");
            actualState.setFogApplyStatus("activity_local_applied_by_FilamentGlbPreviewActivity");
            diagnostics.addNotExposedOrNotVerifiedItem("color_grading_lifecycle_activity_local");
            diagnostics.addNotExposedOrNotVerifiedItem("fog_options_activity_local");
            diagnostics.addNotExposedOrNotVerifiedItem("performance_timing_activity_local");
            diagnostics.setRenderTruthText("render_api_applied profile=" + settings.getQualityProfileName()
                + " msaa=" + actualState.getActualMsaa() + " taa=" + actualState.isActualTaa()
                + " ao=" + actualState.getActualAo() + " bloom=" + actualState.getActualBloom());
        } catch (Throwable t) {
            actualState.setMsaaApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.setRenderTruthText("render_api_apply_failed: " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("render_api_apply_failed");
        }
    }

    @Override public void setQualityProfile(String profileName) { settings.setQualityProfileName(profileName); }
    @Override public void setRenderScale(float scale) { settings.setRenderScale(scale); }
    @Override public void setDynamicResolution(boolean enabled) { settings.setDynamicResolution(enabled); }
    @Override public void setMsaa(int sampleCount) { settings.setMsaaSampleCount(sampleCount); }
    @Override public void setFxaa(boolean enabled) { settings.setFxaa(enabled); }
    @Override public void setTaa(boolean enabled) { settings.setTaa(enabled); }
    @Override public void setDithering(boolean enabled) { settings.setDithering(enabled); }
    @Override public void setSsr(boolean enabled) { settings.setSsr(enabled); }
    @Override public void setRefraction(boolean enabled) { settings.setRefraction(enabled); }
    @Override public void setAoMode(String mode) { settings.setAoMode(mode); }
    @Override public void setBloomMode(String mode) { settings.setBloomMode(mode); }
    @Override public void setBloomStrength(float strength) { settings.setBloomStrength(strength); }
    @Override public void setBloomHighlight(float highlight) { settings.setBloomHighlight(highlight); }
    @Override public void setColorExposure(float exposure) { settings.setColorExposure(exposure); }
    @Override public void setColorContrast(float contrast) { settings.setColorContrast(contrast); }
    @Override public void setColorSaturation(float saturation) { settings.setColorSaturation(saturation); }
    @Override public void setColorTemperature(float temperature) { settings.setColorTemperature(temperature); }

    private void applyAo(QualityLevel quality) {
        try {
            View.AmbientOcclusionOptions ao = view.getAmbientOcclusionOptions();
            ao.enabled = aoEnabled();
            ao.quality = quality;
            ao.lowPassFilter = quality;
            ao.upsampling = quality;
            ao.resolution = isMode("LOW", settings.getQualityProfileName()) ? 0.5f : 1.0f;
            ao.bias = 0.0005f;
            ao.bilateralThreshold = 0.05f;
            if (isMode("OFF", settings.getAoMode())) {
                ao.radius = 0.0f;
                ao.intensity = 0.0f;
                ao.power = 0.0f;
            } else if (isMode("SOFT", settings.getAoMode())) {
                ao.radius = 0.35f;
                ao.intensity = 0.35f;
                ao.power = 0.8f;
            } else if (isMode("MEDIUM", settings.getAoMode())) {
                ao.radius = 0.55f;
                ao.intensity = 0.8f;
                ao.power = 1.2f;
            } else if (isMode("STRONG", settings.getAoMode())) {
                ao.radius = 0.8f;
                ao.intensity = 1.4f;
                ao.power = 1.8f;
            } else {
                ao.radius = 1.2f;
                ao.intensity = 3.0f;
                ao.power = 3.0f;
            }
            view.setAmbientOcclusionOptions(ao);
            actualState.setActualAo(settings.getAoMode());
            actualState.setAoApplyStatus(ao.enabled ? RenderActualState.APPLIED + "_not_visual_verified" : "off");
        } catch (Throwable t) {
            actualState.setAoApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("ao_apply_failed");
        }
    }

    private void applyBloom(QualityLevel quality) {
        try {
            View.BloomOptions bloom = view.getBloomOptions();
            boolean enabled = !isMode("OFF", settings.getBloomMode());
            bloom.enabled = enabled;
            bloom.quality = quality;
            bloom.threshold = true;
            bloom.levels = 6;
            bloom.resolution = 360;
            if (!enabled) {
                bloom.strength = 0.0f;
                bloom.highlight = 1000.0f;
            } else if (isMode("SOFT", settings.getBloomMode())) {
                bloom.strength = clamp(settings.getBloomStrength(), 0.0f, 0.08f);
                bloom.highlight = clamp(settings.getBloomHighlight(), 500.0f, 1200.0f);
            } else if (isMode("MEDIUM", settings.getBloomMode())) {
                bloom.strength = clamp(settings.getBloomStrength(), 0.0f, 0.14f);
                bloom.highlight = clamp(settings.getBloomHighlight(), 250.0f, 1200.0f);
            } else {
                bloom.strength = clamp(settings.getBloomStrength(), 0.0f, 0.25f);
                bloom.highlight = clamp(settings.getBloomHighlight(), 100.0f, 1200.0f);
            }
            view.setBloomOptions(bloom);
            actualState.setActualBloom(settings.getBloomMode());
            actualState.setActualBloomStrength(bloom.strength);
            actualState.setActualBloomHighlight(bloom.highlight);
            actualState.setBloomApplyStatus(enabled ? RenderActualState.APPLIED + "_not_visual_verified" : "off");
        } catch (Throwable t) {
            actualState.setBloomApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("bloom_apply_failed");
        }
    }

    private void applyDynamicResolution(QualityLevel quality, float minScale, float maxScale) {
        try {
            View.DynamicResolutionOptions dynamic = view.getDynamicResolutionOptions();
            dynamic.enabled = settings.isDynamicResolution();
            dynamic.minScale = minScale;
            dynamic.maxScale = maxScale;
            dynamic.quality = quality;
            view.setDynamicResolutionOptions(dynamic);
            actualState.setActualDynamicResolution(dynamic.enabled);
            actualState.setDynamicMinScale(minScale);
            actualState.setDynamicMaxScale(maxScale);
            actualState.setDynamicResolutionApplyStatus(settings.isDynamicResolution() == dynamic.enabled ? RenderActualState.APPLIED : RenderActualState.NOT_VERIFIED);
            actualState.setRenderScaleApplyStatus(RenderActualState.APPLIED);
        } catch (Throwable t) {
            actualState.setDynamicResolutionApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("dynamic_resolution_apply_failed");
        }
    }

    private void applyRenderQuality() {
        View.RenderQuality renderQuality = view.getRenderQuality();
        renderQuality.hdrColorBuffer = isMode("LOW", settings.getQualityProfileName()) ? QualityLevel.LOW
            : (isMode("MEDIUM", settings.getQualityProfileName()) ? QualityLevel.MEDIUM : QualityLevel.HIGH);
        view.setRenderQuality(renderQuality);
    }

    private void applyTaa() {
        try {
            View.TemporalAntiAliasingOptions taa = view.getTemporalAntiAliasingOptions();
            taa.enabled = settings.isTaa();
            taa.filterWidth = 1.0f;
            taa.feedback = isMode("ULTRA_PREVIEW", settings.getQualityProfileName()) ? 0.10f : 0.08f;
            taa.sharpness = isMode("LOW", settings.getQualityProfileName()) ? 0.0f : 0.25f;
            taa.filterHistory = true;
            taa.filterInput = true;
            taa.useYCoCg = true;
            taa.hdr = true;
            taa.preventFlickering = true;
            taa.historyReprojection = true;
            view.setTemporalAntiAliasingOptions(taa);
            actualState.setActualTaa(taa.enabled);
            actualState.setTaaApplyStatus(settings.isTaa() == taa.enabled ? RenderActualState.APPLIED + "_not_visual_verified" : RenderActualState.NOT_VERIFIED);
        } catch (Throwable t) {
            actualState.setActualTaa(false);
            actualState.setTaaApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("taa_apply_failed");
        }
    }

    private void applySsr() {
        try {
            View.ScreenSpaceReflectionsOptions ssr = view.getScreenSpaceReflectionsOptions();
            ssr.enabled = settings.isSsr();
            ssr.thickness = 0.08f;
            ssr.bias = 0.01f;
            ssr.maxDistance = isMode("ULTRA_PREVIEW", settings.getQualityProfileName()) ? 4.0f : 2.0f;
            ssr.stride = isMode("ULTRA_PREVIEW", settings.getQualityProfileName()) ? 1.0f : 2.0f;
            view.setScreenSpaceReflectionsOptions(ssr);
            actualState.setSsrApplyStatus(settings.isSsr() ? RenderActualState.APPLIED + "_manual_heavy_not_gpu_verified" : "off");
        } catch (Throwable t) {
            actualState.setSsrApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("ssr_apply_failed");
        }
    }

    private void applyShadows(boolean enabled) {
        try {
            if (!enabled) {
                actualState.setActualShadows("OFF");
                actualState.setShadowsApplyStatus("off");
                return;
            }
            if (isMode("MEDIUM", settings.getShadowsMode())) {
                view.setShadowType(ShadowType.DPCF);
            } else {
                view.setShadowType(ShadowType.PCF);
            }
            actualState.setActualShadows(settings.getShadowsMode());
            actualState.setShadowsApplyStatus(RenderActualState.APPLIED + "_filament_default_map_not_visual_verified");
        } catch (Throwable t) {
            actualState.setShadowsApplyStatus(RenderActualState.FAILED + ": " + shortMessage(t));
            diagnostics.addNotExposedOrNotVerifiedItem("shadow_type_apply_failed");
        }
    }

    private boolean aoEnabled() {
        return !isMode("OFF", settings.getAoMode());
    }

    private QualityLevel optionQuality() {
        return isMode("LOW", settings.getQualityProfileName()) ? QualityLevel.LOW : QualityLevel.MEDIUM;
    }

    private float dynamicMinScale() {
        if (!settings.isDynamicResolution()) return settings.getRenderScale();
        if (isMode("LOW", settings.getQualityProfileName())) return 0.58f;
        if (isMode("MEDIUM", settings.getQualityProfileName())) return 0.72f;
        return 0.86f;
    }

    private static boolean isMode(String expected, String actual) {
        return expected.equalsIgnoreCase(actual == null ? "" : actual.trim());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String shortMessage(Throwable t) {
        String msg = t == null ? "unknown" : t.getMessage();
        if (msg == null || msg.trim().isEmpty()) msg = t == null ? "unknown" : t.getClass().getSimpleName();
        return msg.replace('\n', ' ').replace('\r', ' ').toLowerCase(Locale.US);
    }
}
