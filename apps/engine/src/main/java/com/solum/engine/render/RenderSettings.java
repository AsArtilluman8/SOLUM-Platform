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
    private float fogHeight = 0.0f;
    private float fogStart = 0.0f;
    private float fogEnd = 80.0f;
    private float fogColorRed = 0.55f;
    private float fogColorGreen = 0.65f;
    private float fogColorBlue = 0.75f;
    private float colorExposure = 0.0f;
    private float colorContrast = 1.0f;
    private float colorSaturation = 1.0f;
    private float colorTemperature = 0.0f;
    private float colorTint = 0.0f;
    private float sunIntensity = 2.5f;
    private float ambientIntensity = 1.0f;
    private float fillIntensity = 0.0f;
    private float backgroundIntensity = 0.14f;
    private float sunDirectionX = 0.70f;
    private float sunDirectionY = -0.35f;
    private float sunDirectionZ = -0.62f;
    private String lightingPreset = "SAFE_STUDIO";
    private String lightRig = "OFF";
    private float iblIntensity = 1.0f;
    private float iblRotation = 0.0f;
    private boolean skyboxEnabled = true;
    private boolean sunGlareEnabled = false;
    private float sunGlareStrength = 0.0f;
    private float sunGlareSize = 0.0f;
    private float modelScale = 1.0f;
    private float modelOffsetX = 0.0f;
    private float modelOffsetY = 0.0f;
    private float modelOffsetZ = 0.0f;
    private float modelRotationX = 0.0f;
    private float modelRotationY = 0.0f;
    private float modelRotationZ = 0.0f;
    private String cameraPreset = "ORBIT_MANUAL";

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
    public float getFogDistance() { return fogEnd; }
    public void setFogDistance(float value) { fogEnd = clamp(value, 1.0f, 500.0f); }
    public float getFogHeight() { return fogHeight; }
    public void setFogHeight(float value) { fogHeight = clamp(value, -100.0f, 100.0f); }
    public float getFogStart() { return fogStart; }
    public void setFogStart(float value) { fogStart = clamp(value, 0.0f, 500.0f); }
    public float getFogEnd() { return fogEnd; }
    public void setFogEnd(float value) { fogEnd = clamp(value, 1.0f, 500.0f); }
    public float getFogColorRed() { return fogColorRed; }
    public float getFogColorGreen() { return fogColorGreen; }
    public float getFogColorBlue() { return fogColorBlue; }
    public void setFogColorRgb(float red, float green, float blue) {
        fogColorRed = clamp(red, 0.0f, 1.0f);
        fogColorGreen = clamp(green, 0.0f, 1.0f);
        fogColorBlue = clamp(blue, 0.0f, 1.0f);
    }
    public float getColorExposure() { return colorExposure; }
    public void setColorExposure(float value) { colorExposure = clamp(value, -2.0f, 2.0f); }
    public float getColorContrast() { return colorContrast; }
    public void setColorContrast(float value) { colorContrast = clamp(value, 0.50f, 1.50f); }
    public float getColorSaturation() { return colorSaturation; }
    public void setColorSaturation(float value) { colorSaturation = clamp(value, 0.0f, 1.60f); }
    public float getColorTemperature() { return colorTemperature; }
    public void setColorTemperature(float value) { colorTemperature = clamp(value, -0.30f, 0.30f); }
    public float getColorTint() { return colorTint; }
    public void setColorTint(float value) { colorTint = clamp(value, -1.0f, 1.0f); }
    public float getSunIntensity() { return sunIntensity; }
    public void setSunIntensity(float value) { sunIntensity = clamp(value, 0.0f, 300.0f); }
    public float getAmbientIntensity() { return ambientIntensity; }
    public void setAmbientIntensity(float value) { ambientIntensity = clamp(value, 0.0f, 100.0f); }
    public float getFillIntensity() { return fillIntensity; }
    public void setFillIntensity(float value) { fillIntensity = clamp(value, 0.0f, 300.0f); }
    public float getBackgroundIntensity() { return backgroundIntensity; }
    public void setBackgroundIntensity(float value) { backgroundIntensity = clamp(value, 0.0f, 1.0f); }
    public float getSunDirectionX() { return sunDirectionX; }
    public float getSunDirectionY() { return sunDirectionY; }
    public float getSunDirectionZ() { return sunDirectionZ; }
    public void setSunDirection(float x, float y, float z) {
        sunDirectionX = clamp(x, -1.0f, 1.0f);
        sunDirectionY = clamp(y, -1.0f, 1.0f);
        sunDirectionZ = clamp(z, -1.0f, 1.0f);
    }
    public String getLightingPreset() { return lightingPreset; }
    public void setLightingPreset(String value) { lightingPreset = safe(value, "SAFE_STUDIO"); }
    public String getLightRig() { return lightRig; }
    public void setLightRig(String value) { lightRig = safe(value, "OFF"); }
    public float getIblIntensity() { return iblIntensity; }
    public void setIblIntensity(float value) { iblIntensity = clamp(value, 0.0f, 100.0f); }
    public float getIblRotation() { return iblRotation; }
    public void setIblRotation(float value) { iblRotation = clamp(value, 0.0f, 360.0f); }
    public boolean isSkyboxEnabled() { return skyboxEnabled; }
    public void setSkyboxEnabled(boolean value) { skyboxEnabled = value; }
    public boolean isSunGlareEnabled() { return sunGlareEnabled; }
    public void setSunGlareEnabled(boolean value) { sunGlareEnabled = value; }
    public float getSunGlareStrength() { return sunGlareStrength; }
    public void setSunGlareStrength(float value) { sunGlareStrength = clamp(value, 0.0f, 1.0f); }
    public float getSunGlareSize() { return sunGlareSize; }
    public void setSunGlareSize(float value) { sunGlareSize = clamp(value, 0.0f, 1.0f); }
    public float getModelScale() { return modelScale; }
    public void setModelScale(float value) { modelScale = clamp(value, 0.01f, 100.0f); }
    public float getModelOffsetX() { return modelOffsetX; }
    public float getModelOffsetY() { return modelOffsetY; }
    public float getModelOffsetZ() { return modelOffsetZ; }
    public void setModelOffset(float x, float y, float z) {
        modelOffsetX = clamp(x, -1000.0f, 1000.0f);
        modelOffsetY = clamp(y, -1000.0f, 1000.0f);
        modelOffsetZ = clamp(z, -1000.0f, 1000.0f);
    }
    public float getModelRotationX() { return modelRotationX; }
    public float getModelRotationY() { return modelRotationY; }
    public float getModelRotationZ() { return modelRotationZ; }
    public void setModelRotation(float x, float y, float z) {
        modelRotationX = clamp(x, -360.0f, 360.0f);
        modelRotationY = clamp(y, -360.0f, 360.0f);
        modelRotationZ = clamp(z, -360.0f, 360.0f);
    }
    public String getCameraPreset() { return cameraPreset; }
    public void setCameraPreset(String value) { cameraPreset = safe(value, "ORBIT_MANUAL"); }

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
