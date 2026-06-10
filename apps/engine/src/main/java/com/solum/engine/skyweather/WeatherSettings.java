package com.solum.engine.skyweather;

public class WeatherSettings {
    private WeatherPreset preset = WeatherPreset.CLEAR;
    private float cloudCoverage = 0.0f;
    private float cloudDensity = 0.0f;
    private float cloudSpeed = 0.05f;
    private float cloudDirectionDeg = 90.0f;
    private float rainIntensity = 0.0f;
    private float snowIntensity = 0.0f;
    private float fogHazeIntensity = 0.0f;
    private float windIntensity = 0.1f;
    private float windDirectionDeg = 90.0f;
    private float wetnessAmount = 0.0f;
    private float snowAmount = 0.0f;
    private float auroraIntensity = 0.0f;

    public WeatherPreset getPreset() { return preset; }
    public void setPreset(WeatherPreset value) { preset = value == null ? WeatherPreset.CLEAR : value; }
    public float getCloudCoverage() { return cloudCoverage; }
    public void setCloudCoverage(float value) { cloudCoverage = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getCloudDensity() { return cloudDensity; }
    public void setCloudDensity(float value) { cloudDensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getCloudSpeed() { return cloudSpeed; }
    public void setCloudSpeed(float value) { cloudSpeed = SkySettings.clamp(value, 0.0f, 3.0f); }
    public float getCloudDirectionDeg() { return cloudDirectionDeg; }
    public void setCloudDirectionDeg(float value) { cloudDirectionDeg = normalizeDegrees(value); }
    public float getRainIntensity() { return rainIntensity; }
    public void setRainIntensity(float value) { rainIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getSnowIntensity() { return snowIntensity; }
    public void setSnowIntensity(float value) { snowIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getFogHazeIntensity() { return fogHazeIntensity; }
    public void setFogHazeIntensity(float value) { fogHazeIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getWindIntensity() { return windIntensity; }
    public void setWindIntensity(float value) { windIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getWindDirectionDeg() { return windDirectionDeg; }
    public void setWindDirectionDeg(float value) { windDirectionDeg = normalizeDegrees(value); }
    public float getWetnessAmount() { return wetnessAmount; }
    public void setWetnessAmount(float value) { wetnessAmount = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getSnowAmount() { return snowAmount; }
    public void setSnowAmount(float value) { snowAmount = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getAuroraIntensity() { return auroraIntensity; }
    public void setAuroraIntensity(float value) { auroraIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }

    static float normalizeDegrees(float value) {
        float wrapped = value % 360.0f;
        return wrapped < 0.0f ? wrapped + 360.0f : wrapped;
    }
}
