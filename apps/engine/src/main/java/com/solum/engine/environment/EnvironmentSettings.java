package com.solum.engine.environment;

public class EnvironmentSettings {
    private float timeOfDayHours = 12.0f;
    private float timeSpeed = 0.0f;
    private String environmentPreset = "NOON";
    private boolean sunEnabled = true;
    private float sunAzimuthDeg = 180.0f;
    private float sunElevationDeg = 70.0f;
    private float sunIntensityLux = 18.0f;
    private float sunColorTemperatureKelvin = 6000.0f;
    private boolean moonEnabled = true;
    private float moonAzimuthDeg = 0.0f;
    private float moonElevationDeg = -40.0f;
    private float moonIntensityLux = 0.3f;
    private float moonPhase = 0.5f;
    private String iblPreset = SkyIblPreset.DAY.name();
    private float iblStrength = 1.2f;
    private float iblRotationDeg = 0.0f;
    private String skyboxPreset = SkyIblPreset.DAY.name();
    private boolean skyboxVisible = true;
    private boolean starsEnabled = true;
    private float starsIntensity = 0.0f;
    private float cloudAmount = 0.0f;
    private String weatherPreset = "none";
    private final WeatherRuntimeParameters weather = new WeatherRuntimeParameters();
    private boolean fallbackAllowed = true;

    public float getTimeOfDayHours() { return timeOfDayHours; }
    public void setTimeOfDayHours(float value) { timeOfDayHours = wrapHours(value); }
    public float getTimeSpeed() { return timeSpeed; }
    public void setTimeSpeed(float value) { timeSpeed = clamp(value, -240.0f, 240.0f); }
    public String getEnvironmentPreset() { return environmentPreset; }
    public void setEnvironmentPreset(String value) { environmentPreset = safe(value, "CUSTOM"); }
    public boolean isSunEnabled() { return sunEnabled; }
    public void setSunEnabled(boolean value) { sunEnabled = value; }
    public float getSunAzimuthDeg() { return sunAzimuthDeg; }
    public void setSunAzimuthDeg(float value) { sunAzimuthDeg = normalizeDegrees(value); }
    public float getSunElevationDeg() { return sunElevationDeg; }
    public void setSunElevationDeg(float value) { sunElevationDeg = clamp(value, -90.0f, 90.0f); }
    public float getSunIntensityLux() { return sunIntensityLux; }
    public void setSunIntensityLux(float value) { sunIntensityLux = clamp(value, 0.0f, 300.0f); }
    public float getSunColorTemperatureKelvin() { return sunColorTemperatureKelvin; }
    public void setSunColorTemperatureKelvin(float value) { sunColorTemperatureKelvin = clamp(value, 1000.0f, 12000.0f); }
    public boolean isMoonEnabled() { return moonEnabled; }
    public void setMoonEnabled(boolean value) { moonEnabled = value; }
    public float getMoonAzimuthDeg() { return moonAzimuthDeg; }
    public void setMoonAzimuthDeg(float value) { moonAzimuthDeg = normalizeDegrees(value); }
    public float getMoonElevationDeg() { return moonElevationDeg; }
    public void setMoonElevationDeg(float value) { moonElevationDeg = clamp(value, -90.0f, 90.0f); }
    public float getMoonIntensityLux() { return moonIntensityLux; }
    public void setMoonIntensityLux(float value) { moonIntensityLux = clamp(value, 0.0f, 3.0f); }
    public float getMoonPhase() { return moonPhase; }
    public void setMoonPhase(float value) { moonPhase = clamp(value, 0.0f, 1.0f); }
    public String getIblPreset() { return iblPreset; }
    public void setIblPreset(String value) { iblPreset = safePreset(value); }
    public float getIblStrength() { return iblStrength; }
    public void setIblStrength(float value) { iblStrength = clamp(value, 0.0f, 10.0f); }
    public float getIblRotationDeg() { return iblRotationDeg; }
    public void setIblRotationDeg(float value) { iblRotationDeg = normalizeDegrees(value); }
    public String getSkyboxPreset() { return skyboxPreset; }
    public void setSkyboxPreset(String value) { skyboxPreset = safePreset(value); }
    public boolean isSkyboxVisible() { return skyboxVisible; }
    public void setSkyboxVisible(boolean value) { skyboxVisible = value; }
    public boolean isStarsEnabled() { return starsEnabled; }
    public void setStarsEnabled(boolean value) { starsEnabled = value; }
    public float getStarsIntensity() { return starsIntensity; }
    public void setStarsIntensity(float value) { starsIntensity = clamp(value, 0.0f, 1.0f); }
    public float getCloudAmount() { return cloudAmount; }
    public void setCloudAmount(float value) { cloudAmount = clamp(value, 0.0f, 1.0f); }
    public String getWeatherPreset() { return weatherPreset; }
    public WeatherRuntimeParameters getWeather() { return weather; }
    public void setWeatherPreset(String value) {
        String id = safe(value, "clear").toLowerCase();
        WeatherPreset preset = WeatherPreset.fromId(id);
        if (preset != null) {
            weatherPreset = preset.getId();
            weather.applyPreset(preset);
            cloudAmount = clamp(weather.getCloudCoverage() / 10.0f, 0.0f, 1.0f);
        } else {
            weatherPreset = "clear";
            weather.applyPreset(WeatherPreset.CLEAR);
            cloudAmount = clamp(weather.getCloudCoverage() / 10.0f, 0.0f, 1.0f);
        }
    }

    public void clearWeather() {
        weatherPreset = "clear";
        WeatherPreset clear = WeatherPreset.CLEAR;
        if (clear != null) {
            weather.applyPreset(clear);
            cloudAmount = clamp(weather.getCloudCoverage() / 10.0f, 0.0f, 1.0f);
        } else {
            weather.clear();
            cloudAmount = 0.0f;
        }
    }
    public boolean isFallbackAllowed() { return fallbackAllowed; }
    public void setFallbackAllowed(boolean value) { fallbackAllowed = value; }

    static float wrapHours(float value) {
        float wrapped = value % 24.0f;
        return wrapped < 0.0f ? wrapped + 24.0f : wrapped;
    }

    static float normalizeDegrees(float value) {
        float wrapped = value % 360.0f;
        return wrapped < 0.0f ? wrapped + 360.0f : wrapped;
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String safePreset(String value) {
        String raw = safe(value, SkyIblPreset.CURRENT.name()).toUpperCase();
        try {
            return SkyIblPreset.valueOf(raw).name();
        } catch (Throwable ignored) {
            return SkyIblPreset.CURRENT.name();
        }
    }
}
