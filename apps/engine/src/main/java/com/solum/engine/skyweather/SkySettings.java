package com.solum.engine.skyweather;

public class SkySettings {
    private float timeOfDayHours = 12.0f;
    private boolean sunEnabled = true;
    private boolean moonEnabled = true;
    private boolean starsEnabled = true;
    private float skyIntensity = 1.0f;

    public float getTimeOfDayHours() { return timeOfDayHours; }
    public void setTimeOfDayHours(float value) { timeOfDayHours = wrapHours(value); }
    public boolean isSunEnabled() { return sunEnabled; }
    public void setSunEnabled(boolean value) { sunEnabled = value; }
    public boolean isMoonEnabled() { return moonEnabled; }
    public void setMoonEnabled(boolean value) { moonEnabled = value; }
    public boolean isStarsEnabled() { return starsEnabled; }
    public void setStarsEnabled(boolean value) { starsEnabled = value; }
    public float getSkyIntensity() { return skyIntensity; }
    public void setSkyIntensity(float value) { skyIntensity = clamp(value, 0.0f, 3.0f); }

    static float wrapHours(float value) {
        float wrapped = value % 24.0f;
        return wrapped < 0.0f ? wrapped + 24.0f : wrapped;
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0.0f, 1.0f);
    }

    static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / Math.max(0.0001f, edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
