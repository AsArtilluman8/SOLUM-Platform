package com.solum.engine.environment.p63;

/**
 * Single source of truth for the bounded P63.2B celestial experiment.
 * Values are sanitized at the boundary so renderer code never receives NaN or Infinity.
 */
public final class SolumCelestialControlState {
    public static final float DEFAULT_TIME = 960.0f;
    public static final float DEFAULT_SUN_ANGULAR_SIZE = 0.85f;
    public static final float DEFAULT_MOON_ANGULAR_SIZE = 0.90f;
    public static final float DEFAULT_CAMERA_ORBIT_SENSITIVITY = 0.010f;
    public static final float DEFAULT_CAMERA_ZOOM_SENSITIVITY = 0.021f;

    public boolean skyEnabled = true;
    public boolean sunEnabled = true;
    public boolean moonEnabled = true;
    public boolean timePaused = true;
    public boolean oldIblActive = true;
    public boolean p63IblEnabled = false;
    public boolean cloudsEnabled = true;
    public boolean starsEnabled = true;
    public boolean precipitationEnabled = false;
    public boolean surfaceWeatherEnabled = false;
    public boolean lightningEnabled = false;
    public boolean proceduralAudioEnabled = false;

    public float time = DEFAULT_TIME;
    public float timeSpeed = 1.0f;

    public float sunLightLux = 18.0f;
    public float sunVisualBrightness = 1.0f;
    public float sunEmissive = 1.10f;
    public float sunEdgeSoftness = 0.72f;
    public float sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE;
    public float sunElevationOffsetDegrees = 0.0f;
    public final float[] sunTint = {1.0f, 0.92f, 0.72f};

    public float moonPhase = 0.62f;
    public float moonAngularSizeDegrees = DEFAULT_MOON_ANGULAR_SIZE;
    public float moonVisualBrightness = 0.82f;
    public float moonEmissive = 0.36f;
    public float moonEdgeSoftness = 0.68f;
    public float moonLightLux = 0.15f;
    public float moonElevationOffsetDegrees = 0.0f;
    public final float[] moonTint = {0.72f, 0.78f, 0.90f};

    public boolean sunGlowEnabled = true;
    public boolean moonGlowEnabled = true;
    public boolean exposureCompensationEnabled = false;
    public boolean highlightClampEnabled = true;
    public boolean bloomLikeEnabled = true;
    public boolean lightShaftsEnabled = false;
    public float sunGlow = 0.48f;
    public float moonGlow = 0.24f;
    public float exposureCompensation = 0.0f;
    public float highlightClamp = 1.0f;
    public float bloomLikeResponse = 0.040f;

    public float starDensity = 0.72f;
    public float starBrightness = 0.88f;
    public float starSize = 1.0f;
    public float starTwinkleAmount = 0.28f;
    public final float[] starTint = {0.82f, 0.90f, 1.0f};

    public float cloudCoverage = 0.28f;
    public float cloudDensity = 0.58f;
    public float cloudSoftness = 0.72f;
    public float cloudSpeed = 0.22f;
    public float cloudBrightness = 0.86f;
    public final float[] cloudTint = {0.92f, 0.95f, 1.0f};
    public String activeCloudPreset = "Light Clouds";

    public float masterVolume = 0.45f;
    public boolean muted = false;
    public float cameraOrbitSensitivity = DEFAULT_CAMERA_ORBIT_SENSITIVITY;
    public float cameraZoomSensitivity = DEFAULT_CAMERA_ZOOM_SENSITIVITY;

    public String activeSkySource = "SOLUM_NATIVE_RAYLEIGH_MIE_DAY";
    public String activeMoonTexture = "UNAVAILABLE";
    public String moonProvenance = "UNAVAILABLE";
    public String postProcessStatus = "baseline_tone_mapping_highlight_clamp_state";
    public String lastCelestialError = "none";

    public void reset() {
        skyEnabled = true; sunEnabled = true; moonEnabled = true; timePaused = true;
        oldIblActive = true; p63IblEnabled = false; cloudsEnabled = true; starsEnabled = true;
        precipitationEnabled = false; surfaceWeatherEnabled = false; lightningEnabled = false;
        proceduralAudioEnabled = false; time = DEFAULT_TIME; timeSpeed = 1.0f;
        resetSun(); resetMoon(); resetStars(); resetClouds(); resetPostProcess(); masterVolume = 0.45f; muted = false;
        cameraOrbitSensitivity = DEFAULT_CAMERA_ORBIT_SENSITIVITY;
        cameraZoomSensitivity = DEFAULT_CAMERA_ZOOM_SENSITIVITY;
        activeSkySource = "SOLUM_NATIVE_RAYLEIGH_MIE_DAY";
        lastCelestialError = "none";
    }

    public void resetSun() {
        sunEnabled = true; sunLightLux = 18.0f; sunVisualBrightness = 1.0f;
        sunEmissive = 1.10f; sunEdgeSoftness = 0.72f;
        sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE; sunElevationOffsetDegrees = 0.0f;
        sunGlowEnabled = true; sunGlow = 0.48f; bloomLikeEnabled = true; bloomLikeResponse = 0.040f;
        setSunTint(1.0f, 0.92f, 0.72f);
    }

    public void applySunPreset(String preset) {
        if ("Soft".equals(preset)) {
            sunLightLux = 7.0f; sunVisualBrightness = 0.70f; sunAngularSizeDegrees = 0.65f;
            sunEmissive = 0.45f; sunGlow = 0.22f; bloomLikeResponse = 0.015f;
            setSunTint(1.0f, 0.88f, 0.70f);
        } else if ("Bright".equals(preset)) {
            sunLightLux = 35.0f; sunVisualBrightness = 1.35f; sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE;
            sunEmissive = 1.10f; sunGlow = 0.50f; bloomLikeResponse = 0.060f;
            setSunTint(1.0f, 0.96f, 0.88f);
        } else if ("Sunset".equals(preset)) {
            sunLightLux = 12.0f; sunVisualBrightness = 1.15f; sunAngularSizeDegrees = 0.72f;
            sunEmissive = 0.82f; sunGlow = 0.42f; bloomLikeResponse = 0.035f;
            sunElevationOffsetDegrees = -4.0f; setSunTint(1.0f, 0.52f, 0.22f);
        } else {
            resetSun();
        }
        sunGlowEnabled = true; bloomLikeEnabled = bloomLikeResponse > 0.0f;
        sanitize();
    }

    public void resetMoon() {
        moonEnabled = true; moonPhase = 0.62f; moonAngularSizeDegrees = DEFAULT_MOON_ANGULAR_SIZE;
        moonVisualBrightness = 0.82f; moonEmissive = 0.36f; moonEdgeSoftness = 0.68f;
        moonLightLux = 0.15f; moonElevationOffsetDegrees = 0.0f;
        moonGlowEnabled = true; moonGlow = 0.24f;
        setMoonTint(0.72f, 0.78f, 0.90f);
    }

    public void resetStars() {
        starsEnabled = true; starDensity = 0.72f; starBrightness = 0.88f;
        starSize = 1.0f; starTwinkleAmount = 0.28f;
        setStarTint(0.82f, 0.90f, 1.0f);
    }

    public void resetClouds() {
        cloudsEnabled = true; cloudCoverage = 0.28f; cloudDensity = 0.58f;
        cloudSoftness = 0.72f; cloudSpeed = 0.22f; cloudBrightness = 0.86f;
        activeCloudPreset = "Light Clouds";
        setCloudTint(0.92f, 0.95f, 1.0f);
    }

    public void applyCloudPreset(String preset) {
        if ("Clear".equals(preset)) {
            cloudsEnabled = false; cloudCoverage = 0.0f; cloudDensity = 0.35f; cloudSoftness = 0.80f;
            cloudSpeed = 0.16f; cloudBrightness = 1.0f; setCloudTint(0.95f, 0.97f, 1.0f);
        } else if ("Partly Cloudy".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.48f; cloudDensity = 0.62f; cloudSoftness = 0.70f;
            cloudSpeed = 0.28f; cloudBrightness = 0.84f; setCloudTint(0.90f, 0.93f, 1.0f);
        } else if ("Cloudy".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.82f; cloudDensity = 0.78f; cloudSoftness = 0.62f;
            cloudSpeed = 0.20f; cloudBrightness = 0.70f; setCloudTint(0.78f, 0.82f, 0.90f);
        } else if ("Sunset Clouds".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.55f; cloudDensity = 0.66f; cloudSoftness = 0.72f;
            cloudSpeed = 0.24f; cloudBrightness = 0.92f; setCloudTint(1.0f, 0.60f, 0.38f);
        } else if ("Night Clouds".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.42f; cloudDensity = 0.70f; cloudSoftness = 0.68f;
            cloudSpeed = 0.18f; cloudBrightness = 0.42f; setCloudTint(0.34f, 0.44f, 0.68f);
        } else {
            resetClouds();
            preset = "Light Clouds";
        }
        activeCloudPreset = preset;
        sanitize();
    }

    public void resetPostProcess() {
        sunGlowEnabled = true; moonGlowEnabled = true; exposureCompensationEnabled = false;
        highlightClampEnabled = true; bloomLikeEnabled = true; lightShaftsEnabled = false;
        sunGlow = 0.48f; moonGlow = 0.24f; exposureCompensation = 0.0f;
        highlightClamp = 1.0f; bloomLikeResponse = 0.040f;
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

    public void setStarTint(float red, float green, float blue) {
        starTint[0] = finiteClamp(red, 0.0f, 1.0f, 0.82f);
        starTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.90f);
        starTint[2] = finiteClamp(blue, 0.0f, 1.0f, 1.0f);
    }

    public void setCloudTint(float red, float green, float blue) {
        cloudTint[0] = finiteClamp(red, 0.0f, 1.0f, 0.92f);
        cloudTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.95f);
        cloudTint[2] = finiteClamp(blue, 0.0f, 1.0f, 1.0f);
    }

    public void sanitize() {
        time = finiteClamp(time, 0.0f, 2400.0f, DEFAULT_TIME);
        timeSpeed = finiteClamp(timeSpeed, 0.0f, 8.0f, 1.0f);
        sunLightLux = finiteClamp(sunLightLux, 0.0f, 50.0f, 18.0f);
        sunVisualBrightness = finiteClamp(sunVisualBrightness, 0.0f, 2.0f, 1.0f);
        sunEmissive = finiteClamp(sunEmissive, 0.0f, 2.0f, 1.10f);
        sunEdgeSoftness = finiteClamp(sunEdgeSoftness, 0.0f, 1.0f, 0.72f);
        sunAngularSizeDegrees = finiteClamp(sunAngularSizeDegrees, 0.10f, 2.0f, DEFAULT_SUN_ANGULAR_SIZE);
        sunElevationOffsetDegrees = finiteClamp(sunElevationOffsetDegrees, -15.0f, 15.0f, 0.0f);
        moonPhase = finiteClamp(moonPhase, 0.0f, 1.0f, 0.62f);
        moonAngularSizeDegrees = finiteClamp(moonAngularSizeDegrees, 0.10f, 2.0f, DEFAULT_MOON_ANGULAR_SIZE);
        moonVisualBrightness = finiteClamp(moonVisualBrightness, 0.0f, 2.0f, 0.82f);
        moonEmissive = finiteClamp(moonEmissive, 0.0f, 1.5f, 0.36f);
        moonEdgeSoftness = finiteClamp(moonEdgeSoftness, 0.0f, 1.0f, 0.68f);
        moonLightLux = finiteClamp(moonLightLux, 0.0f, 2.0f, 0.15f);
        moonElevationOffsetDegrees = finiteClamp(moonElevationOffsetDegrees, -15.0f, 15.0f, 0.0f);
        sunGlow = finiteClamp(sunGlow, 0.0f, 1.0f, 0.48f);
        moonGlow = finiteClamp(moonGlow, 0.0f, 1.0f, 0.24f);
        exposureCompensation = finiteClamp(exposureCompensation, -1.0f, 1.0f, 0.0f);
        highlightClamp = finiteClamp(highlightClamp, 0.50f, 1.0f, 1.0f);
        bloomLikeResponse = finiteClamp(bloomLikeResponse, 0.0f, 0.12f, 0.040f);
        starDensity = finiteClamp(starDensity, 0.0f, 1.0f, 0.72f);
        starBrightness = finiteClamp(starBrightness, 0.0f, 2.0f, 0.88f);
        starSize = finiteClamp(starSize, 0.50f, 1.80f, 1.0f);
        starTwinkleAmount = finiteClamp(starTwinkleAmount, 0.0f, 1.0f, 0.28f);
        cloudCoverage = finiteClamp(cloudCoverage, 0.0f, 1.0f, 0.28f);
        cloudDensity = finiteClamp(cloudDensity, 0.0f, 1.0f, 0.58f);
        cloudSoftness = finiteClamp(cloudSoftness, 0.0f, 1.0f, 0.72f);
        cloudSpeed = finiteClamp(cloudSpeed, 0.0f, 2.0f, 0.22f);
        cloudBrightness = finiteClamp(cloudBrightness, 0.0f, 1.5f, 0.86f);
        masterVolume = finiteClamp(masterVolume, 0.0f, 1.0f, 0.45f);
        cameraOrbitSensitivity = finiteClamp(cameraOrbitSensitivity, 0.003f, 0.020f, DEFAULT_CAMERA_ORBIT_SENSITIVITY);
        cameraZoomSensitivity = finiteClamp(cameraZoomSensitivity, 0.007f, 0.040f, DEFAULT_CAMERA_ZOOM_SENSITIVITY);
        setSunTint(sunTint[0], sunTint[1], sunTint[2]);
        setMoonTint(moonTint[0], moonTint[1], moonTint[2]);
        setStarTint(starTint[0], starTint[1], starTint[2]);
        setCloudTint(cloudTint[0], cloudTint[1], cloudTint[2]);
    }

    public static float finiteClamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
