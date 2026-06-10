package com.solum.engine.environment;

import com.solum.engine.render.RenderControlApi;

public class EnvironmentController implements EnvironmentApi {
    private final EnvironmentSettings settings = new EnvironmentSettings();
    private final EnvironmentActualState actualState = new EnvironmentActualState();
    private final EnvironmentDiagnostics diagnostics = new EnvironmentDiagnostics();
    private final TimeOfDayController timeOfDayController = new TimeOfDayController();
    private RenderControlApi renderControlApi;

    public EnvironmentController(RenderControlApi renderControlApi) {
        this.renderControlApi = renderControlApi;
        settings.setEnvironmentPreset("AUTO");
        settings.setIblPreset(SkyIblPreset.CURRENT.name());
        settings.setSkyboxPreset(SkyIblPreset.CURRENT.name());
        settings.setStarsIntensity(1.0f);
        apply();
    }

    public void setRenderControlApi(RenderControlApi api) {
        renderControlApi = api;
    }

    @Override public EnvironmentSettings getSettings() { return settings; }
    @Override public EnvironmentActualState getActualState() { return actualState; }
    @Override public EnvironmentDiagnostics getDiagnostics() { return diagnostics; }
    @Override public void setTimeOfDay(float hours) { settings.setTimeOfDayHours(hours); settings.setEnvironmentPreset("AUTO"); apply(); }
    @Override public void setTimeSpeed(float multiplier) { settings.setTimeSpeed(multiplier); }
    @Override public void setEnvironmentPreset(String preset) {
        String p = EnvironmentSettings.safe(preset, "AUTO").toUpperCase();
        if ("DAWN".equals(p)) settings.setTimeOfDayHours(6.0f);
        else if ("NOON".equals(p)) settings.setTimeOfDayHours(12.0f);
        else if ("SUNSET".equals(p)) settings.setTimeOfDayHours(18.5f);
        else if ("NIGHT".equals(p)) settings.setTimeOfDayHours(22.0f);
        else if ("MIDNIGHT".equals(p)) settings.setTimeOfDayHours(0.0f);
        settings.setEnvironmentPreset(p);
        apply();
    }
    @Override public void setSunEnabled(boolean enabled) { settings.setSunEnabled(enabled); apply(); }
    @Override public void setSunAzimuth(float degrees) { settings.setSunAzimuthDeg(degrees); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setSunElevation(float degrees) { settings.setSunElevationDeg(degrees); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setSunIntensityLux(float lux) { settings.setSunIntensityLux(lux); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setSunColorTemperatureKelvin(float kelvin) { settings.setSunColorTemperatureKelvin(kelvin); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setMoonEnabled(boolean enabled) { settings.setMoonEnabled(enabled); apply(); }
    @Override public void setMoonAzimuth(float degrees) { settings.setMoonAzimuthDeg(degrees); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setMoonElevation(float degrees) { settings.setMoonElevationDeg(degrees); settings.setEnvironmentPreset("CUSTOM"); apply(); }
    @Override public void setMoonIntensityLux(float lux) { settings.setMoonIntensityLux(lux); apply(); }
    @Override public void setMoonPhase(float value) { settings.setMoonPhase(value); apply(); }
    @Override public void setIblPreset(String preset) { settings.setIblPreset(preset); apply(); }
    @Override public void setIblStrength(float strength) { settings.setIblStrength(strength); apply(); }
    @Override public void setIblRotation(float degrees) { settings.setIblRotationDeg(degrees); apply(); }
    @Override public void setSkyboxPreset(String preset) { settings.setSkyboxPreset(preset); apply(); }
    @Override public void setSkyboxVisible(boolean visible) { settings.setSkyboxVisible(visible); apply(); }
    @Override public void setStarsEnabled(boolean enabled) { settings.setStarsEnabled(enabled); apply(); }
    @Override public void setStarsIntensity(float value) { settings.setStarsIntensity(value); apply(); }
    @Override public void setCloudAmount(float value) { settings.setCloudAmount(value); apply(); }
    @Override public void setCloudCoverage(float value) { settings.setCloudCoverage(value); apply(); }
    @Override public void setCloudDensity(float value) { settings.setCloudDensity(value); apply(); }
    @Override public void setCloudSpeed(float value) { settings.setCloudSpeed(value); apply(); }
    @Override public void setCloudDirectionDeg(float degrees) { settings.setCloudDirectionDeg(degrees); apply(); }
    @Override public void setCloudShadowStrength(float value) { settings.setCloudShadowStrength(value); apply(); }
    @Override public void setCloudShadowScale(float value) { settings.setCloudShadowScale(value); apply(); }
    @Override public void setCloudShadowSpeed(float value) { settings.setCloudShadowSpeed(value); apply(); }
    @Override public void setPrecipitationType(String type) { settings.setPrecipitationType(type); apply(); }
    @Override public void setPrecipitationIntensity(float value) { settings.setPrecipitationIntensity(value); apply(); }

    @Override
    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0.0f || settings.getTimeSpeed() == 0.0f) return;
        settings.setTimeOfDayHours(settings.getTimeOfDayHours() + (deltaSeconds / 3600.0f) * settings.getTimeSpeed());
        apply();
    }

    @Override
    public void apply() {
        diagnostics.resetNotImplementedYet();
        timeOfDayController.compute(settings, actualState, diagnostics);
        if (renderControlApi != null) {
            renderControlApi.setSunIntensity(actualState.getSun().isVisible() ? actualState.getSun().getIntensityLux() : 0.0f);
            renderControlApi.setSunDirection(actualState.getSun().getDirectionX(), actualState.getSun().getDirectionY(), actualState.getSun().getDirectionZ());
            renderControlApi.setAmbientIntensity(actualState.getAmbientIntensity());
            renderControlApi.setIblIntensity(actualState.getAmbientIntensity() * settings.getIblStrength());
            renderControlApi.setIblRotation(settings.getIblRotationDeg());
            renderControlApi.setBackgroundIntensity(actualState.getBackgroundBrightness());
            renderControlApi.setSkyboxEnabled(settings.isSkyboxVisible());
            renderControlApi.setLightingPreset("ENVIRONMENT_" + actualState.getActiveEnvironmentPreset());
            diagnostics.setLastApplyStatus("render_api_requested_activity_local_apply_required");
        } else {
            diagnostics.setLastApplyStatus("render_api_missing_state_computed_only");
        }
        diagnostics.setEnvironmentTruthText("environment_api_live timeOfDay=" + actualState.getActiveTimeOfDayHours()
            + " preset=" + actualState.getActiveEnvironmentPreset()
            + " sun=" + actualState.getSun().getStatus()
            + " moon=" + actualState.getMoon().getStatus()
            + " smoothBlendStatus=" + diagnostics.getSmoothBlendStatus()
            + " cloudCoverage=" + actualState.getCloudCoverage()
            + " cloudDensity=" + actualState.getCloudDensity()
            + " sunOcclusion=" + actualState.getSunOcclusion());
        diagnostics.setIblSlotStatus("slot_ready_asset_missing active=" + actualState.getActiveIblPreset() + " paths=assets/env/*_ibl.ktx planned_p52_assets");
        diagnostics.setSkyboxSlotStatus("slot_ready_asset_missing active=" + actualState.getActiveSkyboxPreset() + " paths=assets/env/*_skybox.ktx planned_p52_assets");
        diagnostics.setFallbackStatus(settings.isFallbackAllowed() ? "missing_asset_fallback_allowed_current_or_neutral_background" : "fallback_disabled");
        diagnostics.addNotImplementedYet("p53_real_hdri_ibl_assets_blocked_if_cmgen_toktx_unavailable");
        diagnostics.addNotImplementedYet("stars_milkyway_asset_optional_missing_placeholder");
        diagnostics.addNotImplementedYet("moon_visual_world_space_sky_disk_not_implemented");
        diagnostics.addNotImplementedYet("cloud_shadow_mask_projected_soft_mask_planned_p54");
        diagnostics.addNotImplementedYet("rain_snow_particles_vfx_planned_p54");
        diagnostics.addNotImplementedYet("true_volumetric_clouds_future_not_p53");
        actualState.setApplyStatus(diagnostics.getLastApplyStatus());
    }
}
