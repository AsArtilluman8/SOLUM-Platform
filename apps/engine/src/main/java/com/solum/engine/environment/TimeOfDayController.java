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
        float sunLux = sunLux(hours, sunElevation);
        float sunKelvin = sunKelvin(hours, sunElevation);
        if (settings.getSunIntensityLux() > 0.0f && !isAutoPreset(settings.getEnvironmentPreset())) {
            sunLux = settings.getSunIntensityLux();
            sunAzimuth = settings.getSunAzimuthDeg();
            sunElevation = settings.getSunElevationDeg();
            sunKelvin = settings.getSunColorTemperatureKelvin();
        }
        fillBody(sun, settings.isSunEnabled(), sunElevation > -1.0f && sunLux > 0.0f, sunAzimuth, sunElevation, sunLux, sunKelvin, 1.0f);
        sun.setStatus(sun.isEnabled() ? (sun.isVisible() ? "computed_time_of_day_directional" : "below_horizon") : "disabled");

        CelestialBodyState moon = actual.getMoon();
        float moonElevation = -sunElevation * 0.72f;
        float moonAzimuth = EnvironmentSettings.normalizeDegrees(sunAzimuth + 180.0f);
        float moonLux = moonElevation > 0.0f ? Math.max(0.1f, settings.getMoonIntensityLux()) : 0.0f;
        if (!isAutoPreset(settings.getEnvironmentPreset())) {
            moonAzimuth = settings.getMoonAzimuthDeg();
            moonElevation = settings.getMoonElevationDeg();
            moonLux = settings.getMoonIntensityLux();
        }
        fillBody(moon, settings.isMoonEnabled(), settings.isMoonEnabled() && moonElevation > 0.0f && moonLux > 0.0f,
            moonAzimuth, moonElevation, moonLux, 4200.0f, settings.getMoonPhase());
        moon.setStatus(moon.isVisible() ? "computed_placeholder_directional_not_rendered_in_p51" : "not_visible_or_disabled");

        float stars = settings.isStarsEnabled() ? nightFactor(hours) * settings.getStarsIntensity() : 0.0f;
        actual.setStarsVisibility(stars);
        actual.setAmbientIntensity(ambient(hours));
        actual.setBackgroundBrightness(background(hours));
        actual.setExposureHint(exposure(hours));

        String slot = slotForHours(hours);
        actual.setActiveIblPreset(SkyIblPreset.CURRENT.name().equals(settings.getIblPreset()) ? slot : settings.getIblPreset());
        actual.setActiveSkyboxPreset(SkyIblPreset.CURRENT.name().equals(settings.getSkyboxPreset()) ? slot : settings.getSkyboxPreset());
        actual.setFallbackActive(settings.isFallbackAllowed());
        actual.setApplyStatus("computed_not_renderer_verified");

        diagnostics.setTimeOfDayStatus("computed_non_astronomical_" + preset.toLowerCase());
        diagnostics.setSunStatus(sun.getStatus() + "_activity_local_when_applied");
        diagnostics.setMoonStatus(moon.getStatus());
        diagnostics.setStarsStatus(stars > 0.0f ? "placeholder_not_rendered_planned_p52_assets" : "off_or_daytime_placeholder");
        diagnostics.setCloudStatus(settings.getCloudAmount() > 0.0f ? "placeholder_only_no_weather_no_volumetric_clouds" : "off_placeholder");
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

    private static float sunLux(float h, float elevation) {
        if (elevation <= -1.0f) return 0.0f;
        float daylight = EnvironmentSettings.clamp(elevation / 72.0f, 0.0f, 1.0f);
        float warmBoost = ("DAWN".equals(presetForHours(h)) || "SUNSET".equals(presetForHours(h))) ? 0.45f : 1.0f;
        return EnvironmentSettings.clamp(18.0f * daylight * warmBoost, 0.0f, 18.0f);
    }

    private static float sunKelvin(float h, float elevation) {
        if (elevation < 8.0f || "DAWN".equals(presetForHours(h)) || "SUNSET".equals(presetForHours(h))) return 3600.0f;
        if (elevation < 28.0f) return 4500.0f;
        return 6200.0f;
    }

    private static float ambient(float h) {
        String p = presetForHours(h);
        if ("MIDNIGHT".equals(p) || "NIGHT".equals(p)) return 0.18f;
        if ("DAWN".equals(p) || "SUNSET".equals(p)) return 0.65f;
        return 1.25f;
    }

    private static float background(float h) {
        String p = presetForHours(h);
        if ("MIDNIGHT".equals(p) || "NIGHT".equals(p)) return 0.04f;
        if ("DAWN".equals(p) || "SUNSET".equals(p)) return 0.18f;
        return 0.28f;
    }

    private static float exposure(float h) {
        String p = presetForHours(h);
        if ("MIDNIGHT".equals(p) || "NIGHT".equals(p)) return 1.25f;
        if ("DAWN".equals(p) || "SUNSET".equals(p)) return 1.08f;
        return 1.0f;
    }

    private static float nightFactor(float h) {
        if (h >= 21.0f || h <= 4.5f) return 1.0f;
        if (h >= 19.0f) return (h - 19.0f) / 2.0f;
        if (h <= 6.0f) return (6.0f - h) / 1.5f;
        return 0.0f;
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
