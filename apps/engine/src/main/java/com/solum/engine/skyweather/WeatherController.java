package com.solum.engine.skyweather;

public class WeatherController {
    private final WeatherSettings settings = new WeatherSettings();
    private final WeatherActualState actualState = new WeatherActualState();
    private final WeatherDiagnostics diagnostics = new WeatherDiagnostics();

    public WeatherController() {
        apply();
    }

    public WeatherSettings getSettings() { return settings; }
    public WeatherActualState getActualState() { return actualState; }
    public WeatherDiagnostics getDiagnostics() { return diagnostics; }

    public void setPreset(WeatherPreset preset) {
        settings.setPreset(preset);
        if (preset == WeatherPreset.CLEAR) {
            settings.setCloudCoverage(0.0f);
            settings.setCloudDensity(0.0f);
            settings.setRainIntensity(0.0f);
            settings.setSnowIntensity(0.0f);
            settings.setFogHazeIntensity(0.0f);
            settings.setWindIntensity(0.1f);
        } else if (preset == WeatherPreset.CLOUDY) {
            settings.setCloudCoverage(0.62f);
            settings.setCloudDensity(0.48f);
            settings.setRainIntensity(0.0f);
            settings.setSnowIntensity(0.0f);
            settings.setFogHazeIntensity(0.18f);
            settings.setWindIntensity(0.25f);
        } else if (preset == WeatherPreset.RAIN) {
            settings.setCloudCoverage(0.82f);
            settings.setCloudDensity(0.70f);
            settings.setRainIntensity(0.55f);
            settings.setSnowIntensity(0.0f);
            settings.setFogHazeIntensity(0.30f);
            settings.setWindIntensity(0.45f);
        } else if (preset == WeatherPreset.SNOW) {
            settings.setCloudCoverage(0.76f);
            settings.setCloudDensity(0.62f);
            settings.setRainIntensity(0.0f);
            settings.setSnowIntensity(0.50f);
            settings.setFogHazeIntensity(0.24f);
            settings.setWindIntensity(0.32f);
        } else if (preset == WeatherPreset.STORM) {
            settings.setCloudCoverage(0.95f);
            settings.setCloudDensity(0.88f);
            settings.setRainIntensity(0.82f);
            settings.setSnowIntensity(0.0f);
            settings.setFogHazeIntensity(0.38f);
            settings.setWindIntensity(0.78f);
        }
        apply();
    }

    public void apply() {
        actualState.setPreset(settings.getPreset());
        actualState.setCloudCoverage(settings.getCloudCoverage());
        actualState.setCloudDensity(settings.getCloudDensity());
        actualState.setCloudSpeed(settings.getCloudSpeed());
        actualState.setCloudDirectionDeg(settings.getCloudDirectionDeg());
        actualState.setRainIntensity(settings.getRainIntensity());
        actualState.setSnowIntensity(settings.getSnowIntensity());
        actualState.setFogHazeIntensity(settings.getFogHazeIntensity());
        actualState.setWindIntensity(settings.getWindIntensity());
        actualState.setWindDirectionDeg(settings.getWindDirectionDeg());
        actualState.setWetnessAmount(Math.max(settings.getWetnessAmount(), settings.getRainIntensity() * 0.85f));
        actualState.setSnowAmount(Math.max(settings.getSnowAmount(), settings.getSnowIntensity() * 0.80f));
        actualState.setAuroraIntensity(settings.getAuroraIntensity());
        float cloudBlock = SkySettings.smoothstep(0.12f, 0.95f, settings.getCloudCoverage() * 0.72f + settings.getCloudDensity() * 0.48f);
        actualState.setSunOcclusionByClouds(SkySettings.clamp(cloudBlock, 0.0f, 0.92f));
        actualState.setVisualStatus(settings.getPreset().name().toLowerCase()
            + "_clouds=" + compact(settings.getCloudCoverage())
            + "_rain=" + compact(settings.getRainIntensity())
            + "_snow=" + compact(settings.getSnowIntensity()));
        diagnostics.setWeatherSystemStatus("weather_core_live_preset_" + settings.getPreset().name().toLowerCase());
        diagnostics.setCloudVisualStatus(settings.getCloudCoverage() > 0.01f
            ? "procedural_state_drives_sky_gradient_and_sun_occlusion_no_volumetrics"
            : "off_clear");
        diagnostics.setRainStatus(settings.getRainIntensity() > 0.01f ? "parameter_live_particles_deferred_mobile_safe" : "off_particle_slot_placeholder");
        diagnostics.setSnowStatus(settings.getSnowIntensity() > 0.01f ? "parameter_live_particles_deferred_mobile_safe" : "off_particle_slot_placeholder");
        diagnostics.setFogHazeStatus(settings.getFogHazeIntensity() > 0.01f ? "parameter_live_filament_fog_bridge" : "off");
        diagnostics.setWetnessStatus("wetness=" + compact(actualState.getWetnessAmount()) + " snowAmount=" + compact(actualState.getSnowAmount()) + "_future_material_parameters");
    }

    private static String compact(float value) {
        return String.valueOf(Math.round(value * 100.0f) / 100.0f);
    }
}
