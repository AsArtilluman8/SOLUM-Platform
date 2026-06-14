package com.solum.engine.environment;

public class WeatherRuntimeParameters {
    private String weatherPreset = "none";
    private String weatherLabel = "None";
    private float rainIntensity = 0.0f;
    private float snowIntensity = 0.0f;
    private float cloudCoverage = 0.0f;
    private float fogDensity = 0.0f;
    private float windIntensity = 0.0f;
    private float materialWetness = 0.0f;
    private float thunderIntensity = 0.0f;
    private float lightningChance = 0.0f;
    private float puddleAmount = 0.0f;

    public String getWeatherPreset() { return weatherPreset; }
    public String getWeatherLabel() { return weatherLabel; }
    public float getRainIntensity() { return rainIntensity; }
    public float getSnowIntensity() { return snowIntensity; }
    public float getCloudCoverage() { return cloudCoverage; }
    public float getFogDensity() { return fogDensity; }
    public float getWindIntensity() { return windIntensity; }
    public float getMaterialWetness() { return materialWetness; }
    public float getThunderIntensity() { return thunderIntensity; }
    public float getLightningChance() { return lightningChance; }
    public float getPuddleAmount() { return puddleAmount; }

    public void clear() {
        weatherPreset = "none";
        weatherLabel = "None";
        rainIntensity = 0.0f;
        snowIntensity = 0.0f;
        cloudCoverage = 0.0f;
        fogDensity = 0.0f;
        windIntensity = 0.0f;
        materialWetness = 0.0f;
        thunderIntensity = 0.0f;
        lightningChance = 0.0f;
        puddleAmount = 0.0f;
    }

    public void applyPreset(WeatherPreset preset) {
        if (preset == null) {
            clear();
            return;
        }
        weatherPreset = preset.getId();
        weatherLabel = preset.getLabel();
        cloudCoverage = clampWeather(preset.getCloudCoverage());
        rainIntensity = clampWeather(preset.getRainIntensity());
        snowIntensity = clampWeather(preset.getSnowIntensity());
        fogDensity = clampWeather(preset.getFogDensity());
        windIntensity = clampWeather(preset.getWindIntensity());
        materialWetness = EnvironmentSettings.clamp(preset.getMaterialWetness(), 0.0f, 1.0f);
        thunderIntensity = clampWeather(preset.getThunderIntensity());
        lightningChance = EnvironmentSettings.clamp(thunderIntensity / 10.0f, 0.0f, 1.0f);
        puddleAmount = EnvironmentSettings.clamp((rainIntensity / 10.0f) * materialWetness, 0.0f, 1.0f);
    }

    static float clampWeather(float value) {
        return EnvironmentSettings.clamp(value, 0.0f, 10.0f);
    }
}
