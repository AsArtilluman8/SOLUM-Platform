package com.solum.engine.environment;

public class EnvironmentSettings {
    public enum PrecipitationType {
        NONE,
        RAIN,
        SNOW
    }

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
    private float cloudCoverage = 0.0f;
    private float cloudDensity = 0.0f;
    private float cloudSpeed = 0.0f;
    private float cloudDirectionDeg = 90.0f;
    private float cloudShadowStrength = 0.0f;
    private float cloudShadowScale = 1.0f;
    private float cloudShadowSpeed = 0.0f;
    private PrecipitationType precipitationType = PrecipitationType.NONE;
    private float precipitationIntensity = 0.0f;
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
    public void setCloudAmount(float value) {
        cloudAmount = clamp(value, 0.0f, 1.0f);
        cloudCoverage = cloudAmount;
    }
    public float getCloudCoverage() { return cloudCoverage; }
    public void setCloudCoverage(float value) {
        cloudCoverage = clamp(value, 0.0f, 1.0f);
        cloudAmount = cloudCoverage;
    }
    public float getCloudDensity() { return cloudDensity; }
    public void setCloudDensity(float value) { cloudDensity = clamp(value, 0.0f, 1.0f); }
    public float getCloudSpeed() { return cloudSpeed; }
    public void setCloudSpeed(float value) { cloudSpeed = clamp(value, 0.0f, 8.0f); }
    public float getCloudDirectionDeg() { return cloudDirectionDeg; }
    public void setCloudDirectionDeg(float value) { cloudDirectionDeg = normalizeDegrees(value); }
    public float getCloudShadowStrength() { return cloudShadowStrength; }
    public void setCloudShadowStrength(float value) { cloudShadowStrength = clamp(value, 0.0f, 1.0f); }
    public float getCloudShadowScale() { return cloudShadowScale; }
    public void setCloudShadowScale(float value) { cloudShadowScale = clamp(value, 0.1f, 20.0f); }
    public float getCloudShadowSpeed() { return cloudShadowSpeed; }
    public void setCloudShadowSpeed(float value) { cloudShadowSpeed = clamp(value, 0.0f, 8.0f); }
    public PrecipitationType getPrecipitationType() { return precipitationType; }
    public void setPrecipitationType(PrecipitationType value) { precipitationType = value == null ? PrecipitationType.NONE : value; }
    public void setPrecipitationType(String value) {
        try {
            precipitationType = PrecipitationType.valueOf(safe(value, "NONE").toUpperCase());
        } catch (Throwable ignored) {
            precipitationType = PrecipitationType.NONE;
        }
    }
    public float getPrecipitationIntensity() { return precipitationIntensity; }
    public void setPrecipitationIntensity(float value) { precipitationIntensity = clamp(value, 0.0f, 1.0f); }
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
