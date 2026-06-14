package com.solum.engine.environment;

public interface EnvironmentApi {
    EnvironmentSettings getSettings();
    EnvironmentActualState getActualState();
    EnvironmentDiagnostics getDiagnostics();
    void setTimeOfDay(float hours);
    void setTimeSpeed(float multiplier);
    void setEnvironmentPreset(String preset);
    void setSunEnabled(boolean enabled);
    void setSunAzimuth(float degrees);
    void setSunElevation(float degrees);
    void setSunIntensityLux(float lux);
    void setSunColorTemperatureKelvin(float kelvin);
    void setMoonEnabled(boolean enabled);
    void setMoonAzimuth(float degrees);
    void setMoonElevation(float degrees);
    void setMoonIntensityLux(float lux);
    void setMoonPhase(float value);
    void setIblPreset(String preset);
    void setIblStrength(float strength);
    void setIblRotation(float degrees);
    void setSkyboxPreset(String preset);
    void setSkyboxVisible(boolean visible);
    void setStarsEnabled(boolean enabled);
    void setStarsIntensity(float value);
    void setCloudAmount(float value);
    void setWeatherPreset(String preset);
    void apply();
    void update(float deltaSeconds);
}
