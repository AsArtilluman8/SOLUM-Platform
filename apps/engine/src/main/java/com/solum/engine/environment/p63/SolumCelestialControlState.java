package com.solum.engine.environment.p63;

/**
 * Single source of truth for the bounded P63.2A celestial experiment.
 * Values are sanitized at the boundary so renderer code never receives NaN or Infinity.
 */
public final class SolumCelestialControlState {
    public static final float DEFAULT_TIME = 960.0f;

    public boolean skyEnabled = true;
    public boolean sunEnabled = true;
    public boolean moonEnabled = true;
    public boolean timePaused = true;
    public boolean oldIblActive = true;
    public boolean p63IblEnabled = false;
    public boolean cloudsEnabled = false;
    public boolean starsEnabled = false;
    public boolean precipitationEnabled = false;
    public boolean surfaceWeatherEnabled = false;
    public boolean lightningEnabled = false;
    public boolean proceduralAudioEnabled = false;

    public float time = DEFAULT_TIME;
    public float timeSpeed = 1.0f;

    public float sunLightLux = 18.0f;
    public float sunVisualBrightness = 1.0f;
    public float sunAngularSizeDegrees = 0.53f;
    public float sunElevationOffsetDegrees = 0.0f;
    public final float[] sunTint = {1.0f, 0.92f, 0.72f};

    public float moonPhase = 0.62f;
    public float moonAngularSizeDegrees = 0.52f;
    public float moonVisualBrightness = 0.75f;
    public float moonLightLux = 0.15f;
    public final float[] moonTint = {0.72f, 0.78f, 0.90f};

    public boolean sunGlowEnabled = true;
    public boolean moonGlowEnabled = true;
    public boolean exposureCompensationEnabled = false;
    public boolean highlightClampEnabled = true;
    public boolean bloomLikeEnabled = false;
    public boolean lightShaftsEnabled = false;
    public float sunGlow = 0.35f;
    public float moonGlow = 0.16f;
    public float exposureCompensation = 0.0f;
    public float highlightClamp = 1.0f;
    public float bloomLikeResponse = 0.0f;

    public float masterVolume = 0.45f;
    public boolean muted = false;

    public String activeSkySource = "SOLUM_NATIVE_RAYLEIGH_MIE_DAY";
    public String activeMoonTexture = "UNAVAILABLE";
    public String moonProvenance = "UNAVAILABLE";
    public String postProcessStatus = "baseline_tone_mapping_highlight_clamp_state";
    public String lastCelestialError = "none";

    public void reset() {
        skyEnabled = true; sunEnabled = true; moonEnabled = true; timePaused = true;
        oldIblActive = true; p63IblEnabled = false; cloudsEnabled = false; starsEnabled = false;
        precipitationEnabled = false; surfaceWeatherEnabled = false; lightningEnabled = false;
        proceduralAudioEnabled = false; time = DEFAULT_TIME; timeSpeed = 1.0f;
        resetSun(); resetMoon(); resetPostProcess(); masterVolume = 0.45f; muted = false;
        activeSkySource = "SOLUM_NATIVE_RAYLEIGH_MIE_DAY";
        lastCelestialError = "none";
    }

    public void resetSun() {
        sunEnabled = true; sunLightLux = 18.0f; sunVisualBrightness = 1.0f;
        sunAngularSizeDegrees = 0.53f; sunElevationOffsetDegrees = 0.0f;
        setSunTint(1.0f, 0.92f, 0.72f);
    }

    public void applySunPreset(String preset) {
        if ("Soft".equals(preset)) {
            sunLightLux = 7.0f; sunVisualBrightness = 0.70f; sunAngularSizeDegrees = 0.65f;
            setSunTint(1.0f, 0.88f, 0.70f);
        } else if ("Bright".equals(preset)) {
            sunLightLux = 35.0f; sunVisualBrightness = 1.35f; sunAngularSizeDegrees = 0.53f;
            setSunTint(1.0f, 0.96f, 0.88f);
        } else if ("Sunset".equals(preset)) {
            sunLightLux = 12.0f; sunVisualBrightness = 1.15f; sunAngularSizeDegrees = 0.72f;
            sunElevationOffsetDegrees = -4.0f; setSunTint(1.0f, 0.52f, 0.22f);
        } else {
            resetSun();
        }
        sanitize();
    }

    public void resetMoon() {
        moonEnabled = true; moonPhase = 0.62f; moonAngularSizeDegrees = 0.52f;
        moonVisualBrightness = 0.75f; moonLightLux = 0.15f;
        setMoonTint(0.72f, 0.78f, 0.90f);
    }

    public void resetPostProcess() {
        sunGlowEnabled = true; moonGlowEnabled = true; exposureCompensationEnabled = false;
        highlightClampEnabled = true; bloomLikeEnabled = false; lightShaftsEnabled = false;
        sunGlow = 0.35f; moonGlow = 0.16f; exposureCompensation = 0.0f;
        highlightClamp = 1.0f; bloomLikeResponse = 0.0f;
        postProcessStatus = "baseline_tone_mapping_highlight_clamp_state";
    }

    public void setSunTint(float red, float green, float blue) {
        sunTint[0] = finiteClamp(red, 0.0f, 1.0f, 1.0f);
        sunTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.92f);
        sunTint[2] = finiteClamp(blue, 0.0f, 1.0f, 0.72f);
    }

    public void setMoonTint(float red, float green, float blue) {
        moonTint[0] = finiteClamp(red, 0.0f, 1.0f, 0.72f);
        moonTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.78f);
        moonTint[2] = finiteClamp(blue, 0.0f, 1.0f, 0.90f);
    }

    public void sanitize() {
        time = finiteClamp(time, 0.0f, 2400.0f, DEFAULT_TIME);
        timeSpeed = finiteClamp(timeSpeed, 0.0f, 8.0f, 1.0f);
        sunLightLux = finiteClamp(sunLightLux, 0.0f, 50.0f, 18.0f);
        sunVisualBrightness = finiteClamp(sunVisualBrightness, 0.0f, 2.0f, 1.0f);
        sunAngularSizeDegrees = finiteClamp(sunAngularSizeDegrees, 0.10f, 2.0f, 0.53f);
        sunElevationOffsetDegrees = finiteClamp(sunElevationOffsetDegrees, -15.0f, 15.0f, 0.0f);
        moonPhase = finiteClamp(moonPhase, 0.0f, 1.0f, 0.62f);
        moonAngularSizeDegrees = finiteClamp(moonAngularSizeDegrees, 0.10f, 2.0f, 0.52f);
        moonVisualBrightness = finiteClamp(moonVisualBrightness, 0.0f, 2.0f, 0.75f);
        moonLightLux = finiteClamp(moonLightLux, 0.0f, 2.0f, 0.15f);
        sunGlow = finiteClamp(sunGlow, 0.0f, 1.0f, 0.35f);
        moonGlow = finiteClamp(moonGlow, 0.0f, 1.0f, 0.16f);
        exposureCompensation = finiteClamp(exposureCompensation, -1.0f, 1.0f, 0.0f);
        highlightClamp = finiteClamp(highlightClamp, 0.50f, 1.0f, 1.0f);
        bloomLikeResponse = finiteClamp(bloomLikeResponse, 0.0f, 0.12f, 0.0f);
        masterVolume = finiteClamp(masterVolume, 0.0f, 1.0f, 0.45f);
        setSunTint(sunTint[0], sunTint[1], sunTint[2]);
        setMoonTint(moonTint[0], moonTint[1], moonTint[2]);
    }

    public static float finiteClamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
