package com.solum.engine.skyweather;

public class WeatherActualState {
    private WeatherPreset preset = WeatherPreset.CLEAR;
    private float cloudCoverage;
    private float cloudDensity;
    private float cloudSpeed;
    private float cloudDirectionDeg;
    private float sunOcclusionByClouds;
    private float rainIntensity;
    private float snowIntensity;
    private float fogHazeIntensity;
    private float windIntensity;
    private float windDirectionDeg;
    private float wetnessAmount;
    private float snowAmount;
    private float auroraIntensity;
    private String visualStatus = "clear";

    public WeatherPreset getPreset() { return preset; }
    public void setPreset(WeatherPreset value) { preset = value == null ? WeatherPreset.CLEAR : value; }
    public float getCloudCoverage() { return cloudCoverage; }
    public void setCloudCoverage(float value) { cloudCoverage = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getCloudDensity() { return cloudDensity; }
    public void setCloudDensity(float value) { cloudDensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getCloudSpeed() { return cloudSpeed; }
    public void setCloudSpeed(float value) { cloudSpeed = SkySettings.clamp(value, 0.0f, 3.0f); }
    public float getCloudDirectionDeg() { return cloudDirectionDeg; }
    public void setCloudDirectionDeg(float value) { cloudDirectionDeg = WeatherSettings.normalizeDegrees(value); }
    public float getSunOcclusionByClouds() { return sunOcclusionByClouds; }
    public void setSunOcclusionByClouds(float value) { sunOcclusionByClouds = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getRainIntensity() { return rainIntensity; }
    public void setRainIntensity(float value) { rainIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getSnowIntensity() { return snowIntensity; }
    public void setSnowIntensity(float value) { snowIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getFogHazeIntensity() { return fogHazeIntensity; }
    public void setFogHazeIntensity(float value) { fogHazeIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getWindIntensity() { return windIntensity; }
    public void setWindIntensity(float value) { windIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getWindDirectionDeg() { return windDirectionDeg; }
    public void setWindDirectionDeg(float value) { windDirectionDeg = WeatherSettings.normalizeDegrees(value); }
    public float getWetnessAmount() { return wetnessAmount; }
    public void setWetnessAmount(float value) { wetnessAmount = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getSnowAmount() { return snowAmount; }
    public void setSnowAmount(float value) { snowAmount = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getAuroraIntensity() { return auroraIntensity; }
    public void setAuroraIntensity(float value) { auroraIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public String getVisualStatus() { return visualStatus; }
    public void setVisualStatus(String value) { visualStatus = value == null ? "" : value; }
}
