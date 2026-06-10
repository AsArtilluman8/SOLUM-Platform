package com.solum.engine.skyweather;

import com.solum.engine.environment.CelestialBodyState;

public class SkyActualState {
    private float timeOfDayHours = 12.0f;
    private String dayNightPhase = "day";
    private final CelestialBodyState sun = new CelestialBodyState();
    private final CelestialBodyState moon = new CelestialBodyState();
    private float starsIntensity = 0.0f;
    private final float[] zenithColor = new float[] {0.10f, 0.16f, 0.26f};
    private final float[] horizonColor = new float[] {0.32f, 0.48f, 0.68f};
    private final float[] groundColor = new float[] {0.04f, 0.06f, 0.07f};
    private float skyBrightness = 0.28f;
    private float sunOcclusionByClouds = 0.0f;
    private float auroraIntensity = 0.0f;
    private String visualStatus = "procedural_gradient_renderer_owned";

    public float getTimeOfDayHours() { return timeOfDayHours; }
    public void setTimeOfDayHours(float value) { timeOfDayHours = SkySettings.wrapHours(value); }
    public String getDayNightPhase() { return dayNightPhase; }
    public void setDayNightPhase(String value) { dayNightPhase = value == null ? "" : value; }
    public CelestialBodyState getSun() { return sun; }
    public CelestialBodyState getMoon() { return moon; }
    public float getStarsIntensity() { return starsIntensity; }
    public void setStarsIntensity(float value) { starsIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float[] getZenithColor() { return zenithColor; }
    public float[] getHorizonColor() { return horizonColor; }
    public float[] getGroundColor() { return groundColor; }
    public void setZenithColor(float r, float g, float b) { setColor(zenithColor, r, g, b); }
    public void setHorizonColor(float r, float g, float b) { setColor(horizonColor, r, g, b); }
    public void setGroundColor(float r, float g, float b) { setColor(groundColor, r, g, b); }
    public float getSkyBrightness() { return skyBrightness; }
    public void setSkyBrightness(float value) { skyBrightness = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getSunOcclusionByClouds() { return sunOcclusionByClouds; }
    public void setSunOcclusionByClouds(float value) { sunOcclusionByClouds = SkySettings.clamp(value, 0.0f, 1.0f); }
    public float getAuroraIntensity() { return auroraIntensity; }
    public void setAuroraIntensity(float value) { auroraIntensity = SkySettings.clamp(value, 0.0f, 1.0f); }
    public String getVisualStatus() { return visualStatus; }
    public void setVisualStatus(String value) { visualStatus = value == null ? "" : value; }

    private static void setColor(float[] out, float r, float g, float b) {
        out[0] = SkySettings.clamp(r, 0.0f, 1.0f);
        out[1] = SkySettings.clamp(g, 0.0f, 1.0f);
        out[2] = SkySettings.clamp(b, 0.0f, 1.0f);
    }
}
