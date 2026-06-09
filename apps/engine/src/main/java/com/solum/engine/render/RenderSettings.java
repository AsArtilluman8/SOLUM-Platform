package com.solum.engine.render;

public class RenderSettings {
    private String qualityProfileName = "MEDIUM";
    private float renderScale = 0.95f;
    private boolean dynamicResolution = true;
    private int msaaSampleCount = 2;
    private boolean fxaa = true;
    private boolean taa = false;
    private boolean dithering = true;
    private boolean ssr = false;
    private boolean refraction = true;
    private String aoMode = "OFF";
    private String bloomMode = "OFF";
    private float bloomStrength = 0.0f;
    private float bloomHighlight = 1000.0f;
    private String shadowsMode = "OFF";
    private String fogMode = "OFF";
    private float fogDensity = 0.0f;
    private float fogDistance = 80.0f;
    private float fogHeight = 0.0f;
    private float colorExposure = 0.0f;
    private float colorContrast = 1.0f;
    private float colorSaturation = 1.0f;
    private float colorTemperature = 0.0f;
    private float sunIntensity = 2.5f;
    private float ambientIntensity = 1.0f;
    private float fillIntensity = 0.0f;

    public String getQualityProfileName() { return qualityProfileName; }
    public void setQualityProfileName(String value) { qualityProfileName = safe(value, "MEDIUM"); }
    public float getRenderScale() { return renderScale; }
    public void setRenderScale(float value) { renderScale = clamp(value, 0.50f, 1.00f); }
    public boolean isDynamicResolution() { return dynamicResolution; }
    public void setDynamicResolution(boolean value) { dynamicResolution = value; }
    public int getMsaaSampleCount() { return msaaSampleCount; }
    public void setMsaaSampleCount(int value) { msaaSampleCount = sanitizeMsaa(value); }
    public boolean isFxaa() { return fxaa; }
    public void setFxaa(boolean value) { fxaa = value; }
    public boolean isTaa() { return taa; }
    public void setTaa(boolean value) { taa = value; }
    public boolean isDithering() { return dithering; }
    public void setDithering(boolean value) { dithering = value; }
    public boolean isSsr() { return ssr; }
    public void setSsr(boolean value) { ssr = value; }
    public boolean isRefraction() { return refraction; }
    public void setRefraction(boolean value) { refraction = value; }
    public String getAoMode() { return aoMode; }
    public void setAoMode(String value) { aoMode = safe(value, "OFF"); }
    public String getBloomMode() { return bloomMode; }
    public void setBloomMode(String value) { bloomMode = safe(value, "OFF"); }
    public float getBloomStrength() { return bloomStrength; }
    public void setBloomStrength(float value) { bloomStrength = clamp(value, 0.0f, 0.25f); }
    public float getBloomHighlight() { return bloomHighlight; }
    public void setBloomHighlight(float value) { bloomHighlight = clamp(value, 100.0f, 1200.0f); }
    public String getShadowsMode() { return shadowsMode; }
    public void setShadowsMode(String value) { shadowsMode = safe(value, "OFF"); }
    public String getFogMode() { return fogMode; }
    public void setFogMode(String value) { fogMode = safe(value, "OFF"); }
    public float getFogDensity() { return fogDensity; }
    public void setFogDensity(float value) { fogDensity = clamp(value, 0.0f, 0.08f); }
    public float getFogDistance() { return fogDistance; }
    public void setFogDistance(float value) { fogDistance = clamp(value, 1.0f, 500.0f); }
    public float getFogHeight() { return fogHeight; }
    public void setFogHeight(float value) { fogHeight = clamp(value, -100.0f, 100.0f); }
    public float getColorExposure() { return colorExposure; }
    public void setColorExposure(float value) { colorExposure = clamp(value, -2.0f, 2.0f); }
    public float getColorContrast() { return colorContrast; }
    public void setColorContrast(float value) { colorContrast = clamp(value, 0.50f, 1.50f); }
    public float getColorSaturation() { return colorSaturation; }
    public void setColorSaturation(float value) { colorSaturation = clamp(value, 0.0f, 1.60f); }
    public float getColorTemperature() { return colorTemperature; }
    public void setColorTemperature(float value) { colorTemperature = clamp(value, -0.30f, 0.30f); }
    public float getSunIntensity() { return sunIntensity; }
    public void setSunIntensity(float value) { sunIntensity = clamp(value, 0.0f, 300.0f); }
    public float getAmbientIntensity() { return ambientIntensity; }
    public void setAmbientIntensity(float value) { ambientIntensity = clamp(value, 0.0f, 100.0f); }
    public float getFillIntensity() { return fillIntensity; }
    public void setFillIntensity(float value) { fillIntensity = clamp(value, 0.0f, 300.0f); }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static int sanitizeMsaa(int value) {
        if (value <= 1) return 1;
        if (value <= 2) return 2;
        return 4;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
