package com.solum.engine.environment;

public class EnvironmentActualState {
    private float activeTimeOfDayHours = 12.0f;
    private String activeEnvironmentPreset = "NOON";
    private final CelestialBodyState sun = new CelestialBodyState();
    private final CelestialBodyState moon = new CelestialBodyState();
    private String activeIblPreset = SkyIblPreset.DAY.name();
    private String activeSkyboxPreset = SkyIblPreset.DAY.name();
    private float starsVisibility = 0.0f;
    private float backgroundBrightness = 0.16f;
    private float ambientIntensity = 1.2f;
    private float exposureHint = 1.0f;
    private boolean fallbackActive = true;
    private IblMode iblMode = IblMode.PROCEDURAL_APPROX;
    private String skyMode = "PROCEDURAL_SKY_PASS";
    private String sunMode = "PROCEDURAL_DIRECTIONAL_LIGHT";
    private String moonMode = "PROCEDURAL_DIRECTIONAL_FALLBACK_NOT_RENDERED";
    private String starsMode = "PROCEDURAL_FALLBACK_NOT_RENDERED";
    private boolean fakeOverlayUsed = false;
    private final WeatherRuntimeParameters weather = new WeatherRuntimeParameters();
    private WeatherVfxRecipe weatherVfxRecipe = WeatherVfxRecipe.rainFallback(weather);
    private String applyStatus = "not_applied";

    public float getActiveTimeOfDayHours() { return activeTimeOfDayHours; }
    public void setActiveTimeOfDayHours(float value) { activeTimeOfDayHours = EnvironmentSettings.wrapHours(value); }
    public String getActiveEnvironmentPreset() { return activeEnvironmentPreset; }
    public void setActiveEnvironmentPreset(String value) { activeEnvironmentPreset = EnvironmentSettings.safe(value, "CUSTOM"); }
    public CelestialBodyState getSun() { return sun; }
    public CelestialBodyState getMoon() { return moon; }
    public String getActiveIblPreset() { return activeIblPreset; }
    public void setActiveIblPreset(String value) { activeIblPreset = EnvironmentSettings.safe(value, SkyIblPreset.CURRENT.name()); }
    public String getActiveSkyboxPreset() { return activeSkyboxPreset; }
    public void setActiveSkyboxPreset(String value) { activeSkyboxPreset = EnvironmentSettings.safe(value, SkyIblPreset.CURRENT.name()); }
    public float getStarsVisibility() { return starsVisibility; }
    public void setStarsVisibility(float value) { starsVisibility = EnvironmentSettings.clamp(value, 0.0f, 1.0f); }
    public float getBackgroundBrightness() { return backgroundBrightness; }
    public void setBackgroundBrightness(float value) { backgroundBrightness = EnvironmentSettings.clamp(value, 0.0f, 1.0f); }
    public float getAmbientIntensity() { return ambientIntensity; }
    public void setAmbientIntensity(float value) { ambientIntensity = EnvironmentSettings.clamp(value, 0.0f, 10.0f); }
    public float getExposureHint() { return exposureHint; }
    public void setExposureHint(float value) { exposureHint = EnvironmentSettings.clamp(value, 0.1f, 5.0f); }
    public boolean isFallbackActive() { return fallbackActive; }
    public void setFallbackActive(boolean value) { fallbackActive = value; }
    public IblMode getIblMode() { return iblMode; }
    public void setIblMode(IblMode value) { iblMode = value == null ? IblMode.NONE : value; }
    public String getSkyMode() { return skyMode; }
    public void setSkyMode(String value) { skyMode = value == null ? "" : value; }
    public String getSunMode() { return sunMode; }
    public void setSunMode(String value) { sunMode = value == null ? "" : value; }
    public String getMoonMode() { return moonMode; }
    public void setMoonMode(String value) { moonMode = value == null ? "" : value; }
    public String getStarsMode() { return starsMode; }
    public void setStarsMode(String value) { starsMode = value == null ? "" : value; }
    public boolean isFakeOverlayUsed() { return fakeOverlayUsed; }
    public void setFakeOverlayUsed(boolean value) { fakeOverlayUsed = value; }
    public WeatherRuntimeParameters getWeather() { return weather; }
    public void setWeatherFrom(WeatherRuntimeParameters value) {
        WeatherPreset preset = value == null ? WeatherPreset.CLEAR : WeatherPreset.fromId(value.getWeatherPreset());
        weather.applyPreset(preset == null ? WeatherPreset.CLEAR : preset);
        weatherVfxRecipe = WeatherVfxRecipe.rainFallback(weather);
    }
    public WeatherVfxRecipe getWeatherVfxRecipe() { return weatherVfxRecipe; }
    public String getApplyStatus() { return applyStatus; }
    public void setApplyStatus(String value) { applyStatus = value == null ? "" : value; }
}
