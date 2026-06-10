package com.solum.engine.skyweather;

import com.solum.engine.environment.CelestialBodyState;

public class SkyController {
    private final SkySettings settings = new SkySettings();
    private final SkyActualState actualState = new SkyActualState();
    private final SkyDiagnostics diagnostics = new SkyDiagnostics();
    private WeatherActualState weatherActualState;

    public SkyController() {
        apply();
    }

    public SkySettings getSettings() { return settings; }
    public SkyActualState getActualState() { return actualState; }
    public SkyDiagnostics getDiagnostics() { return diagnostics; }

    public void setWeatherActualState(WeatherActualState value) {
        weatherActualState = value;
        apply();
    }

    public void setTimeOfDay(float hours) {
        settings.setTimeOfDayHours(hours);
        apply();
    }

    public void apply() {
        float h = settings.getTimeOfDayHours();
        float cloudCoverage = weatherActualState == null ? 0.0f : weatherActualState.getCloudCoverage();
        float cloudDensity = weatherActualState == null ? 0.0f : weatherActualState.getCloudDensity();
        float occlusion = weatherActualState == null ? 0.0f : weatherActualState.getSunOcclusionByClouds();
        float haze = weatherActualState == null ? 0.0f : weatherActualState.getFogHazeIntensity();
        float aurora = weatherActualState == null ? 0.0f : weatherActualState.getAuroraIntensity();

        actualState.setTimeOfDayHours(h);
        actualState.setDayNightPhase(phaseForHours(h));
        float elevation = sunElevation(h);
        float azimuth = SkySettings.wrapHours(h) * 15.0f + 90.0f;
        float daylight = SkySettings.smoothstep(-4.0f, 22.0f, elevation);
        float sunLux = settings.isSunEnabled() && elevation > -2.0f ? 20.0f * daylight * (1.0f - occlusion * 0.82f) : 0.0f;
        fillBody(actualState.getSun(), settings.isSunEnabled(), sunLux > 0.01f, azimuth, elevation, sunLux, sunKelvin(h, elevation), 1.0f);
        actualState.getSun().setStatus(actualState.getSun().isVisible()
            ? "computed_directional_light_cloud_attenuated"
            : "below_horizon_or_disabled");

        float moonElevation = -elevation * 0.72f;
        float moonLux = settings.isMoonEnabled() && moonElevation > 0.0f ? SkySettings.lerp(0.05f, 0.35f, nightFactor(h)) : 0.0f;
        fillBody(actualState.getMoon(), settings.isMoonEnabled(), moonLux > 0.0f, azimuth + 180.0f, moonElevation, moonLux, 4300.0f, 0.5f);
        actualState.getMoon().setStatus(actualState.getMoon().isVisible()
            ? "computed_placeholder_no_visual_disk"
            : "not_visible_or_disabled");

        float stars = settings.isStarsEnabled() ? nightFactor(h) * (1.0f - cloudCoverage * 0.75f) : 0.0f;
        actualState.setStarsIntensity(stars);
        actualState.setSunOcclusionByClouds(occlusion);
        actualState.setAuroraIntensity(aurora * nightFactor(h));
        computeGradient(h, cloudCoverage, cloudDensity, haze);
        diagnostics.setSkySystemStatus("sky_core_live_phase_" + actualState.getDayNightPhase());
        diagnostics.setSunVisualStatus("real_directional_light_no_screen_space_disk");
        diagnostics.setMoonVisualStatus("placeholder_disabled_no_screen_space_disk");
        diagnostics.setStarsStatus(stars > 0.01f ? "state_live_texture_layer_deferred" : "off_or_cloud_occluded");
        diagnostics.setCloudVisualStatus(cloudCoverage > 0.01f
            ? "cheap_gradient_haze_and_sun_occlusion_no_volumetrics"
            : "off_clear_gradient");
        diagnostics.setFallbackStatus("public_procedural_gradient_fallback");
    }

    private void computeGradient(float h, float cloudCoverage, float cloudDensity, float haze) {
        String phase = phaseForHours(h);
        float[] zenith;
        float[] horizon;
        float[] ground;
        float brightness;
        if ("night".equals(phase) || "midnight".equals(phase)) {
            zenith = new float[] {0.015f, 0.025f, 0.070f};
            horizon = new float[] {0.055f, 0.075f, 0.120f};
            ground = new float[] {0.010f, 0.014f, 0.018f};
            brightness = 0.045f;
        } else if ("dawn".equals(phase) || "sunset".equals(phase)) {
            zenith = new float[] {0.115f, 0.170f, 0.300f};
            horizon = new float[] {0.820f, 0.420f, 0.210f};
            ground = new float[] {0.080f, 0.065f, 0.055f};
            brightness = 0.18f;
        } else {
            zenith = new float[] {0.120f, 0.310f, 0.700f};
            horizon = new float[] {0.520f, 0.690f, 0.880f};
            ground = new float[] {0.055f, 0.080f, 0.090f};
            brightness = 0.30f;
        }
        float gray = SkySettings.clamp(cloudCoverage * 0.42f + cloudDensity * 0.25f + haze * 0.22f, 0.0f, 0.78f);
        mixToCloud(zenith, gray);
        mixToCloud(horizon, gray * 0.78f);
        actualState.setZenithColor(zenith[0], zenith[1], zenith[2]);
        actualState.setHorizonColor(horizon[0], horizon[1], horizon[2]);
        actualState.setGroundColor(ground[0], ground[1], ground[2]);
        actualState.setSkyBrightness(SkySettings.clamp(brightness * settings.getSkyIntensity() * (1.0f - cloudDensity * 0.24f) + haze * 0.035f, 0.0f, 1.0f));
        actualState.setVisualStatus("renderer_owned_procedural_gradient_no_ui_overlay");
    }

    private static void mixToCloud(float[] color, float amount) {
        color[0] = SkySettings.lerp(color[0], 0.48f, amount);
        color[1] = SkySettings.lerp(color[1], 0.52f, amount);
        color[2] = SkySettings.lerp(color[2], 0.56f, amount);
    }

    private static String phaseForHours(float h) {
        if (h < 4.5f || h >= 23.0f) return "midnight";
        if (h < 8.0f) return "dawn";
        if (h < 16.5f) return "day";
        if (h < 20.0f) return "sunset";
        return "night";
    }

    private static float sunElevation(float h) {
        double t = (h - 6.0f) / 12.0f * Math.PI;
        return (float) (Math.sin(t) * 72.0f);
    }

    private static float sunKelvin(float h, float elevation) {
        if (elevation < 8.0f || "dawn".equals(phaseForHours(h)) || "sunset".equals(phaseForHours(h))) return 3600.0f;
        if (elevation < 28.0f) return 4700.0f;
        return 6200.0f;
    }

    private static float nightFactor(float h) {
        if (h >= 21.0f || h <= 4.5f) return 1.0f;
        if (h >= 19.0f) return SkySettings.smoothstep(19.0f, 21.0f, h);
        if (h <= 6.0f) return 1.0f - SkySettings.smoothstep(4.5f, 6.0f, h);
        return 0.0f;
    }

    private static void fillBody(CelestialBodyState body, boolean enabled, boolean visible, float azimuth, float elevation,
                                 float lux, float kelvin, float phase) {
        body.setEnabled(enabled);
        body.setVisible(visible);
        body.setAzimuthDeg(WeatherSettings.normalizeDegrees(azimuth));
        body.setElevationDeg(elevation);
        body.setIntensityLux(lux);
        body.setColorTemperatureKelvin(kelvin);
        body.setPhase(phase);
        double az = Math.toRadians(WeatherSettings.normalizeDegrees(azimuth));
        double el = Math.toRadians(elevation);
        body.setDirection((float) (Math.cos(el) * Math.sin(az)), (float) -Math.sin(el), (float) (Math.cos(el) * Math.cos(az)));
    }
}
