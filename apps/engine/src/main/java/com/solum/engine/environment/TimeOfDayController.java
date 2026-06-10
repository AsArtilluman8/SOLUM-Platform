package com.solum.engine.environment;

public class TimeOfDayController {
    public void compute(EnvironmentSettings settings, EnvironmentActualState actual, EnvironmentDiagnostics diagnostics) {
        float hours = EnvironmentSettings.wrapHours(settings.getTimeOfDayHours());
        String preset = presetForHours(hours);
        actual.setActiveTimeOfDayHours(hours);
        actual.setActiveEnvironmentPreset(preset);

        CelestialBodyState sun = actual.getSun();
        sun.setEnabled(settings.isSunEnabled());
        float sunElevation = sunElevation(hours);
        float sunAzimuth = EnvironmentSettings.normalizeDegrees(90.0f + hours * 15.0f);
        float sunCurveT = sunCurveT(sunElevation);
        float nightCurveT = nightCurveT(hours, sunElevation);
        float starsCurveT = starsCurveT(hours, sunElevation);
        float sunOcclusion = sunOcclusion(settings.getCloudCoverage(), settings.getCloudDensity());
        float sunLux = sunLux(hours, sunElevation, sunCurveT) * sunOcclusion;
        float sunKelvin = sunKelvin(hours, sunElevation);
        if (settings.getSunIntensityLux() > 0.0f && !isAutoPreset(settings.getEnvironmentPreset())) {
            sunLux = settings.getSunIntensityLux() * sunOcclusion;
            sunAzimuth = settings.getSunAzimuthDeg();
            sunElevation = settings.getSunElevationDeg();
            sunKelvin = settings.getSunColorTemperatureKelvin();
        }
        fillBody(sun, settings.isSunEnabled(), sunElevation > -1.0f && sunLux > 0.0f, sunAzimuth, sunElevation, sunLux, sunKelvin, 1.0f);
        sun.setStatus(sun.isEnabled() ? (sun.isVisible() ? "computed_time_of_day_directional" : "below_horizon") : "disabled");

        CelestialBodyState moon = actual.getMoon();
        float moonElevation = -sunElevation * 0.72f;
        float moonAzimuth = EnvironmentSettings.normalizeDegrees(sunAzimuth + 180.0f);
        float moonLux = moonElevation > 0.0f ? Math.max(0.1f, settings.getMoonIntensityLux()) * smoothstep(0.0f, 24.0f, moonElevation) : 0.0f;
        if (!isAutoPreset(settings.getEnvironmentPreset())) {
            moonAzimuth = settings.getMoonAzimuthDeg();
            moonElevation = settings.getMoonElevationDeg();
            moonLux = settings.getMoonIntensityLux();
        }
        fillBody(moon, settings.isMoonEnabled(), settings.isMoonEnabled() && moonElevation > 0.0f && moonLux > 0.0f,
            moonAzimuth, moonElevation, moonLux, 4200.0f, settings.getMoonPhase());
        moon.setStatus(moon.isVisible() ? "computed_placeholder_directional_not_rendered_in_p51" : "not_visible_or_disabled");

        float stars = settings.isStarsEnabled() ? starsCurveT * settings.getStarsIntensity() : 0.0f;
        actual.setStarsVisibility(stars);
        actual.setAmbientIntensity(ambient(hours, sunCurveT, nightCurveT, settings.getCloudCoverage(), settings.getCloudDensity()));
        actual.setBackgroundBrightness(background(hours, sunCurveT, nightCurveT));
        actual.setExposureHint(exposure(sunCurveT, nightCurveT));
        actual.setCloudCoverage(settings.getCloudCoverage());
        actual.setCloudDensity(settings.getCloudDensity());
        actual.setSunOcclusion(sunOcclusion);
        actual.setCloudShadowStatus(settings.getCloudShadowStrength() > 0.0f
            ? "cloud_shadow_mask=planned_not_active shadow_strength_not_exposed_cloud_occlusion_light_only"
            : "cloud_shadow_mask=planned_not_active");
        actual.setPrecipitationStatus(precipitationStatus(settings));

        String slot = slotForHours(hours);
        actual.setActiveIblPreset(SkyIblPreset.CURRENT.name().equals(settings.getIblPreset()) ? slot : settings.getIblPreset());
        actual.setActiveSkyboxPreset(SkyIblPreset.CURRENT.name().equals(settings.getSkyboxPreset()) ? slot : settings.getSkyboxPreset());
        actual.setFallbackActive(settings.isFallbackAllowed());
        actual.setApplyStatus("computed_not_renderer_verified");

        diagnostics.setTimeOfDayStatus("computed_non_astronomical_" + preset.toLowerCase());
        diagnostics.setSunStatus(sun.getStatus() + "_activity_local_when_applied");
        diagnostics.setMoonStatus(moon.isVisible() ? "moon_directional_state_computed_visual_world_space_not_implemented" : moon.getStatus());
        diagnostics.setStarsStatus(stars > 0.0f ? "stars_asset_missing_placeholder intensity_smooth" : "off_or_daytime_placeholder");
        diagnostics.setCloudStatus(settings.getCloudCoverage() > 0.0f || settings.getCloudDensity() > 0.0f
            ? "cheap_cloud_foundation_controls_only_no_volumetric cloudCoverage=" + settings.getCloudCoverage() + " cloudDensity=" + settings.getCloudDensity()
            : "off_placeholder_no_volumetric_clouds");
        diagnostics.setSunOcclusionStatus("sunOcclusion=" + sunOcclusion + " clear_1_cloudy_0_4_to_0_8_light_attenuation");
        diagnostics.setCloudShadowStatus(actual.getCloudShadowStatus());
        diagnostics.setPrecipitationStatus(actual.getPrecipitationStatus());
        diagnostics.setVolumetricCloudsStatus("not_implemented_mobile_future");
        diagnostics.setTimeBlendPhase(timeBlendPhase(hours, sunCurveT, nightCurveT));
        diagnostics.setSunCurveT(sunCurveT);
        diagnostics.setNightCurveT(nightCurveT);
        diagnostics.setStarsCurveT(starsCurveT);
        diagnostics.setSmoothBlendStatus("smoothstep_lerp_sun_moon_ambient_exposure_stars; skyboxBlendStatus=discrete_preset_switch_light_blend_smooth");
        diagnostics.setSkyboxBlendStatus("discrete_preset_switch_light_blend_smooth");
    }

    private static boolean isAutoPreset(String value) {
        return value == null || value.equals("AUTO") || value.equals("DAWN") || value.equals("NOON")
            || value.equals("SUNSET") || value.equals("NIGHT") || value.equals("MIDNIGHT");
    }

    private static String presetForHours(float h) {
        if (h < 4.5f || h >= 23.0f) return "MIDNIGHT";
        if (h < 8.0f) return "DAWN";
        if (h < 16.5f) return "NOON";
        if (h < 20.0f) return "SUNSET";
        return "NIGHT";
    }

    private static String slotForHours(float h) {
        String preset = presetForHours(h);
        if ("DAWN".equals(preset) || "SUNSET".equals(preset)) return SkyIblPreset.SUNSET.name();
        if ("NIGHT".equals(preset) || "MIDNIGHT".equals(preset)) return SkyIblPreset.NIGHT.name();
        return SkyIblPreset.DAY.name();
    }

    private static float sunElevation(float h) {
        double t = (h - 6.0f) / 12.0f * Math.PI;
        return (float) (Math.sin(t) * 72.0f);
    }

    private static float sunLux(float h, float elevation, float curveT) {
        if (elevation <= -1.0f) return 0.0f;
        float warmBoost = ("DAWN".equals(presetForHours(h)) || "SUNSET".equals(presetForHours(h))) ? 0.45f : 1.0f;
        return EnvironmentSettings.clamp(18.0f * curveT * warmBoost, 0.0f, 18.0f);
    }

    private static float sunKelvin(float h, float elevation) {
        float low = smoothstep(-4.0f, 12.0f, elevation);
        float high = smoothstep(12.0f, 46.0f, elevation);
        return lerp(3200.0f, lerp(4500.0f, 6200.0f, high), low);
    }

    private static float ambient(float h, float sunCurveT, float nightCurveT, float cloudCoverage, float cloudDensity) {
        float base = lerp(0.18f, 1.25f, sunCurveT);
        float twilight = ("DAWN".equals(presetForHours(h)) || "SUNSET".equals(presetForHours(h))) ? 0.20f : 0.0f;
        float cloudDiffuseBoost = cloudCoverage * cloudDensity * 0.18f;
        float cloudSunDimming = cloudCoverage * cloudDensity * 0.22f * sunCurveT;
        return EnvironmentSettings.clamp(base + twilight + cloudDiffuseBoost - cloudSunDimming - nightCurveT * 0.04f, 0.08f, 1.35f);
    }

    private static float background(float h, float sunCurveT, float nightCurveT) {
        float twilight = ("DAWN".equals(presetForHours(h)) || "SUNSET".equals(presetForHours(h))) ? 0.06f : 0.0f;
        return EnvironmentSettings.clamp(lerp(0.035f, 0.28f, sunCurveT) + twilight - nightCurveT * 0.01f, 0.02f, 0.32f);
    }

    private static float exposure(float sunCurveT, float nightCurveT) {
        return EnvironmentSettings.clamp(lerp(1.22f, 1.0f, sunCurveT) + nightCurveT * 0.03f, 0.92f, 1.28f);
    }

    private static float nightFactor(float h) {
        if (h >= 21.0f || h <= 4.5f) return 1.0f;
        if (h >= 19.0f) return (h - 19.0f) / 2.0f;
        if (h <= 6.0f) return (6.0f - h) / 1.5f;
        return 0.0f;
    }

    private static float sunCurveT(float elevation) {
        return smoothstep(-4.0f, 32.0f, elevation);
    }

    private static float nightCurveT(float h, float elevation) {
        return Math.max(1.0f - smoothstep(-10.0f, 6.0f, elevation), nightFactor(h) * 0.8f);
    }

    private static float starsCurveT(float h, float elevation) {
        return EnvironmentSettings.clamp(nightFactor(h) * (1.0f - smoothstep(-8.0f, 5.0f, elevation)), 0.0f, 1.0f);
    }

    private static float sunOcclusion(float coverage, float density) {
        float cloud = EnvironmentSettings.clamp(coverage * (0.35f + density * 0.65f), 0.0f, 1.0f);
        return EnvironmentSettings.clamp(1.0f - cloud * 0.60f, 0.40f, 1.0f);
    }

    private static String precipitationStatus(EnvironmentSettings settings) {
        if (settings.getPrecipitationType() == EnvironmentSettings.PrecipitationType.NONE || settings.getPrecipitationIntensity() <= 0.0f) {
            return "none";
        }
        return settings.getPrecipitationType().name().toLowerCase() + "_placeholder_no_particles intensity=" + settings.getPrecipitationIntensity();
    }

    private static String timeBlendPhase(float h, float sunCurveT, float nightCurveT) {
        String preset = presetForHours(h).toLowerCase();
        return preset + "_smooth sunCurveT=" + sunCurveT + " nightCurveT=" + nightCurveT;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = EnvironmentSettings.clamp((x - edge0) / Math.max(0.0001f, edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * EnvironmentSettings.clamp(t, 0.0f, 1.0f);
    }

    private static void fillBody(CelestialBodyState body, boolean enabled, boolean visible, float azimuth, float elevation,
                                 float lux, float kelvin, float phase) {
        body.setEnabled(enabled);
        body.setVisible(visible);
        body.setAzimuthDeg(azimuth);
        body.setElevationDeg(elevation);
        body.setIntensityLux(lux);
        body.setColorTemperatureKelvin(kelvin);
        body.setPhase(phase);
        double az = Math.toRadians(azimuth);
        double el = Math.toRadians(elevation);
        float x = (float) (Math.cos(el) * Math.sin(az));
        float y = (float) -Math.sin(el);
        float z = (float) (Math.cos(el) * Math.cos(az));
        body.setDirection(x, y, z);
    }
}
