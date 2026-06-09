package com.solum.engine.render;

public interface RenderControlApi {
    RenderSettings getSettings();
    RenderActualState getActualState();
    RenderDiagnostics getDiagnostics();
    void apply();
    void setQualityProfile(String profileName);
    void setRenderScale(float scale);
    void setDynamicResolution(boolean enabled);
    void setMsaa(int sampleCount);
    void setFxaa(boolean enabled);
    void setTaa(boolean enabled);
    void setDithering(boolean enabled);
    void setSsr(boolean enabled);
    void setRefraction(boolean enabled);
    void setAoMode(String mode);
    void setBloomMode(String mode);
    void setBloomStrength(float strength);
    void setBloomHighlight(float highlight);
    void setColorExposure(float exposure);
    void setColorContrast(float contrast);
    void setColorSaturation(float saturation);
    void setColorTemperature(float temperature);
}
