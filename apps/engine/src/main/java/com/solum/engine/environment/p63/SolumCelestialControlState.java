package com.solum.engine.environment.p63;

/**
 * Single source of truth for the bounded P63.3 analytic celestial experiment.
 * Values are sanitized at the boundary so renderer code never receives NaN or Infinity.
 */
public final class SolumCelestialControlState {
    public static final float DEFAULT_TIME = 960.0f;
    public static final float DEFAULT_SUN_ANGULAR_SIZE = 0.53f;
    public static final float DEFAULT_MOON_ANGULAR_SIZE = 0.52f;
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
    public boolean analyticSky = true;
    public boolean analyticSun = true;
    public boolean analyticMoon = true;
    public boolean analyticStars = true;
    public boolean analyticClouds = true;
    public boolean legacyCelestialFallback = true;

    public float time = DEFAULT_TIME;
    public float timeSpeed = 1.0f;

    public float sunLightLux = 35_000.0f;
    public float sunVisualBrightness = 1.0f;
    public float sunEmissive = 1.10f;
    public float sunEdgeSoftness = 0.72f;
    public float sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE;
    public float sunElevationOffsetDegrees = 0.0f;
    public final float[] sunTint = {1.0f, 0.92f, 0.72f};
    public float sunDiscLuminanceNits = 35_000.0f;
    public float sunHaloSize = 2.8f;
    public float sunHaloFalloff = 5.5f;
    public float sunBloomContribution = 0.35f;
    public float sunExposureWeight = 1.0f;
    public float sunLimbDarkening = 0.55f;

    public float moonPhase = 0.62f;
    public float moonAngularSizeDegrees = DEFAULT_MOON_ANGULAR_SIZE;
    public float moonVisualBrightness = 0.82f;
    public float moonEmissive = 0.36f;
    public float moonEdgeSoftness = 0.68f;
    public float moonLightLux = 0.15f;
    public float moonElevationOffsetDegrees = 0.0f;
    public final float[] moonTint = {0.72f, 0.78f, 0.90f};
    public float moonPhaseAngleDegrees = 68.4f;
    public float moonVisualLuminanceNits = 2_200.0f;
    public float moonEarthshine = 0.035f;
    public float moonNormalStrength = 0.32f;

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
    public float starLimitingMagnitude = 6.0f;
    public float milkyWayIntensity = 0.0f;
    public float milkyWaySaturation = 1.0f;
    public float siderealRotationDegrees = 0.0f;
    public final float[] starTint = {0.82f, 0.90f, 1.0f};

    public float cloudCoverage = 0.28f;
    public float cloudDensity = 0.58f;
    public float cloudSoftness = 0.72f;
    public float cloudSpeed = 0.22f;
    public float cloudBrightness = 0.86f;
    public float cloudHeightKm = 2.2f;
    public float cloudThicknessKm = 1.4f;
    public float cloudErosion = 0.42f;
    public float cloudEvolution = 0.12f;
    public float cloudSilverLining = 0.48f;
    public String cloudQuality = "Low";
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
    public float turbidity = 2.4f;
    public float rayleigh = 1.0f;
    public float mie = 1.0f;
    public float mieG = 0.76f;
    public float ozone = 1.0f;
    public float horizonHaze = 0.32f;
    public float nightFloor = 0.012f;
    public float sunsetSaturation = 1.08f;
    public float sunsetContrast = 1.05f;
    public float horizonWarmth = 0.18f;
    public final float[] skyArtTint = {1.0f, 1.0f, 1.0f};
    public String activeScenarioPreset = "Clear Noon";

    public void reset() {
        skyEnabled = true; sunEnabled = true; moonEnabled = true; timePaused = true;
        oldIblActive = true; p63IblEnabled = false; cloudsEnabled = true; starsEnabled = true;
        analyticSky = true; analyticSun = true; analyticMoon = true; analyticStars = true;
        analyticClouds = true; legacyCelestialFallback = true;
        precipitationEnabled = false; surfaceWeatherEnabled = false; lightningEnabled = false;
        proceduralAudioEnabled = false; time = DEFAULT_TIME; timeSpeed = 1.0f;
        resetSun(); resetMoon(); resetStars(); resetClouds(); resetPostProcess(); masterVolume = 0.45f; muted = false;
        cameraOrbitSensitivity = DEFAULT_CAMERA_ORBIT_SENSITIVITY;
        cameraZoomSensitivity = DEFAULT_CAMERA_ZOOM_SENSITIVITY;
        turbidity = 2.4f; rayleigh = 1.0f; mie = 1.0f; mieG = 0.76f; ozone = 1.0f;
        horizonHaze = 0.32f; nightFloor = 0.012f; sunsetSaturation = 1.08f;
        sunsetContrast = 1.05f; horizonWarmth = 0.18f; setSkyArtTint(1.0f, 1.0f, 1.0f);
        activeScenarioPreset = "Clear Noon";
        activeSkySource = "FILAMENT_ADAPTED_ANALYTIC_SKY";
        lastCelestialError = "none";
    }

    public void resetSun() {
        sunEnabled = true; sunLightLux = 35_000.0f; sunVisualBrightness = 1.0f;
        sunEmissive = 1.10f; sunEdgeSoftness = 0.72f;
        sunDiscLuminanceNits = 35_000.0f; sunHaloSize = 2.8f; sunHaloFalloff = 5.5f;
        sunBloomContribution = 0.35f; sunExposureWeight = 1.0f; sunLimbDarkening = 0.55f;
        sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE; sunElevationOffsetDegrees = 0.0f;
        sunGlowEnabled = true; sunGlow = 0.48f; bloomLikeEnabled = true; bloomLikeResponse = 0.040f;
        setSunTint(1.0f, 0.92f, 0.72f);
    }

    public void applySunPreset(String preset) {
        if ("Soft".equals(preset)) {
            sunLightLux = 8_000.0f; sunDiscLuminanceNits = 7_500.0f; sunAngularSizeDegrees = 0.62f;
            sunHaloSize = 3.2f; sunHaloFalloff = 4.5f; sunBloomContribution = 0.16f;
            setSunTint(1.0f, 0.88f, 0.70f);
        } else if ("Physical Noon".equals(preset)) {
            sunLightLux = 110_000.0f; sunDiscLuminanceNits = 100_000.0f; sunAngularSizeDegrees = 0.53f;
            sunHaloSize = 2.2f; sunHaloFalloff = 6.5f; sunBloomContribution = 0.42f;
            setSunTint(1.0f, 0.96f, 0.88f);
        } else if ("Golden Hour".equals(preset)) {
            sunLightLux = 18_000.0f; sunDiscLuminanceNits = 42_000.0f; sunAngularSizeDegrees = 0.58f;
            sunHaloSize = 4.0f; sunHaloFalloff = 3.8f; sunBloomContribution = 0.38f;
            sunElevationOffsetDegrees = -4.0f; setSunTint(1.0f, 0.52f, 0.22f);
        } else if ("Overcast".equals(preset)) {
            sunLightLux = 6_000.0f; sunDiscLuminanceNits = 4_000.0f; sunAngularSizeDegrees = 0.72f;
            sunHaloSize = 5.0f; sunHaloFalloff = 3.0f; sunBloomContribution = 0.08f;
            setSunTint(0.94f, 0.96f, 1.0f);
        } else if ("Custom".equals(preset)) {
            sanitize(); return;
        } else {
            resetSun();
        }
        sunGlowEnabled = true; bloomLikeEnabled = bloomLikeResponse > 0.0f;
        sanitize();
    }

    public void resetMoon() {
        moonEnabled = true; moonPhase = 0.62f; moonPhaseAngleDegrees = 68.4f;
        moonAngularSizeDegrees = DEFAULT_MOON_ANGULAR_SIZE;
        moonVisualBrightness = 0.82f; moonEmissive = 0.36f; moonEdgeSoftness = 0.68f;
        moonVisualLuminanceNits = 2_200.0f; moonEarthshine = 0.035f; moonNormalStrength = 0.32f;
        moonLightLux = 0.15f; moonElevationOffsetDegrees = 0.0f;
        moonGlowEnabled = true; moonGlow = 0.24f;
        setMoonTint(0.72f, 0.78f, 0.90f);
    }

    public void resetStars() {
        starsEnabled = true; starDensity = 0.72f; starBrightness = 0.88f;
        starSize = 1.0f; starTwinkleAmount = 0.28f; starLimitingMagnitude = 6.0f;
        milkyWayIntensity = 0.0f; milkyWaySaturation = 1.0f; siderealRotationDegrees = 0.0f;
        setStarTint(0.82f, 0.90f, 1.0f);
    }

    public void resetClouds() {
        cloudsEnabled = true; cloudCoverage = 0.28f; cloudDensity = 0.58f;
        cloudSoftness = 0.72f; cloudSpeed = 0.22f; cloudBrightness = 0.86f;
        cloudHeightKm = 2.2f; cloudThicknessKm = 1.4f; cloudErosion = 0.42f;
        cloudEvolution = 0.12f; cloudSilverLining = 0.48f; cloudQuality = "Low";
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
            cloudSpeed = 0.24f; cloudBrightness = 0.92f; time = 1800.0f;
            setCloudTint(1.0f, 0.96f, 0.92f);
        } else if ("Night Clouds".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.42f; cloudDensity = 0.70f; cloudSoftness = 0.68f;
            cloudSpeed = 0.18f; cloudBrightness = 0.52f; time = 0.0f;
            setCloudTint(0.82f, 0.88f, 1.0f);
        } else {
            resetClouds();
            preset = "Light Clouds";
        }
        activeCloudPreset = preset;
        sanitize();
    }

    public void applyScenarioPreset(String preset) {
        if ("Clear Noon".equals(preset)) { time = 1200.0f; applyCloudPreset("Clear"); applySunPreset("Physical Noon"); }
        else if ("Golden Hour".equals(preset)) { time = 1700.0f; applyCloudPreset("Light Clouds"); applySunPreset("Golden Hour"); }
        else if ("Sunset".equals(preset)) { time = 1800.0f; applyCloudPreset("Light Clouds"); applySunPreset("Golden Hour"); }
        else if ("Civil Twilight".equals(preset)) { time = 1900.0f; applyCloudPreset("Clear"); }
        else if ("Clear Midnight".equals(preset)) { time = 0.0f; applyCloudPreset("Clear"); }
        else if ("Quarter Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 90.0f; applyCloudPreset("Clear"); }
        else if ("Crescent Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 150.0f; applyCloudPreset("Clear"); }
        else if ("Full Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 0.0f; applyCloudPreset("Clear"); }
        else if ("Milky Way Night".equals(preset)) { time = 0.0f; milkyWayIntensity = 0.75f; starsEnabled = true; applyCloudPreset("Clear"); }
        else if ("Light Clouds".equals(preset) || "Partly Cloudy".equals(preset)
                || "Sunset Clouds".equals(preset) || "Night Clouds".equals(preset)) { applyCloudPreset(preset); }
        else if ("Overcast".equals(preset)) { applyCloudPreset("Cloudy"); applySunPreset("Overcast"); }
        activeScenarioPreset = preset;
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

    public void setSkyArtTint(float red, float green, float blue) {
        skyArtTint[0] = finiteClamp(red, 0.0f, 2.0f, 1.0f);
        skyArtTint[1] = finiteClamp(green, 0.0f, 2.0f, 1.0f);
        skyArtTint[2] = finiteClamp(blue, 0.0f, 2.0f, 1.0f);
    }

    public void sanitize() {
        time = finiteClamp(time, 0.0f, 2400.0f, DEFAULT_TIME);
        timeSpeed = finiteClamp(timeSpeed, 0.0f, 8.0f, 1.0f);
        sunLightLux = finiteClamp(sunLightLux, 0.0f, SolumAnalyticSkyMaterial.SUN_LUX_SAFETY_MAX, 35_000.0f);
        sunVisualBrightness = finiteClamp(sunVisualBrightness, 0.0f, 2.0f, 1.0f);
        sunEmissive = finiteClamp(sunEmissive, 0.0f, SolumAnalyticSkyMaterial.SUN_LUMINANCE_SAFETY_MAX_NITS, 1.10f);
        sunDiscLuminanceNits = finiteClamp(sunDiscLuminanceNits, 0.0f,
            SolumAnalyticSkyMaterial.SUN_LUMINANCE_SAFETY_MAX_NITS, 35_000.0f);
        sunHaloSize = finiteClamp(sunHaloSize, 0.1f, 16.0f, 2.8f);
        sunHaloFalloff = finiteClamp(sunHaloFalloff, 0.5f, 20.0f, 5.5f);
        sunBloomContribution = finiteClamp(sunBloomContribution, 0.0f, 4.0f, 0.35f);
        sunExposureWeight = finiteClamp(sunExposureWeight, 0.01f, 8.0f, 1.0f);
        sunLimbDarkening = finiteClamp(sunLimbDarkening, 0.0f, 2.0f, 0.55f);
        sunEdgeSoftness = finiteClamp(sunEdgeSoftness, 0.0f, 1.0f, 0.72f);
        sunAngularSizeDegrees = finiteClamp(sunAngularSizeDegrees, 0.10f, 2.0f, DEFAULT_SUN_ANGULAR_SIZE);
        sunElevationOffsetDegrees = finiteClamp(sunElevationOffsetDegrees, -15.0f, 15.0f, 0.0f);
        moonPhaseAngleDegrees = finiteClamp(moonPhaseAngleDegrees, 0.0f, 180.0f, 68.4f);
        moonPhase = 1.0f - moonPhaseAngleDegrees / 180.0f;
        moonAngularSizeDegrees = finiteClamp(moonAngularSizeDegrees, 0.10f, 2.0f, DEFAULT_MOON_ANGULAR_SIZE);
        moonVisualBrightness = finiteClamp(moonVisualBrightness, 0.0f, 2.0f, 0.82f);
        moonVisualLuminanceNits = finiteClamp(moonVisualLuminanceNits, 0.0f,
            SolumAnalyticSkyMaterial.MOON_LUMINANCE_SAFETY_MAX_NITS, 2_200.0f);
        moonEarthshine = finiteClamp(moonEarthshine, 0.0f, 0.25f, 0.035f);
        moonNormalStrength = finiteClamp(moonNormalStrength, 0.0f, 2.0f, 0.32f);
        moonEmissive = finiteClamp(moonEmissive, 0.0f, 1.5f, 0.36f);
        moonEdgeSoftness = finiteClamp(moonEdgeSoftness, 0.0f, 1.0f, 0.68f);
        moonLightLux = finiteClamp(moonLightLux, 0.0f, SolumAnalyticSkyMaterial.MOON_LUX_SAFETY_MAX, 0.15f);
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
        starLimitingMagnitude = finiteClamp(starLimitingMagnitude, 0.0f, 8.0f, 6.0f);
        milkyWayIntensity = finiteClamp(milkyWayIntensity, 0.0f, 4.0f, 0.0f);
        milkyWaySaturation = finiteClamp(milkyWaySaturation, 0.0f, 2.0f, 1.0f);
        siderealRotationDegrees = finiteClamp(siderealRotationDegrees, -3600.0f, 3600.0f, 0.0f);
        cloudCoverage = finiteClamp(cloudCoverage, 0.0f, 1.0f, 0.28f);
        cloudDensity = finiteClamp(cloudDensity, 0.0f, 1.0f, 0.58f);
        cloudSoftness = finiteClamp(cloudSoftness, 0.0f, 1.0f, 0.72f);
        cloudSpeed = finiteClamp(cloudSpeed, 0.0f, 2.0f, 0.22f);
        cloudBrightness = finiteClamp(cloudBrightness, 0.0f, 1.5f, 0.86f);
        cloudHeightKm = finiteClamp(cloudHeightKm, 0.5f, 16.0f, 2.2f);
        cloudThicknessKm = finiteClamp(cloudThicknessKm, 0.1f, 8.0f, 1.4f);
        cloudErosion = finiteClamp(cloudErosion, 0.0f, 1.0f, 0.42f);
        cloudEvolution = finiteClamp(cloudEvolution, 0.0f, 2.0f, 0.12f);
        cloudSilverLining = finiteClamp(cloudSilverLining, 0.0f, 2.0f, 0.48f);
        if (!"Low".equals(cloudQuality) && !"Medium".equals(cloudQuality)
                && !"High Experimental".equals(cloudQuality)) cloudQuality = "Low";
        turbidity = finiteClamp(turbidity, 1.0f, 12.0f, 2.4f);
        rayleigh = finiteClamp(rayleigh, 0.0f, 4.0f, 1.0f);
        mie = finiteClamp(mie, 0.0f, 4.0f, 1.0f);
        mieG = finiteClamp(mieG, 0.0f, 0.95f, 0.76f);
        ozone = finiteClamp(ozone, 0.0f, 4.0f, 1.0f);
        horizonHaze = finiteClamp(horizonHaze, 0.0f, 2.0f, 0.32f);
        nightFloor = finiteClamp(nightFloor, 0.0f, 0.2f, 0.012f);
        sunsetSaturation = finiteClamp(sunsetSaturation, 0.0f, 2.0f, 1.08f);
        sunsetContrast = finiteClamp(sunsetContrast, 0.5f, 2.0f, 1.05f);
        horizonWarmth = finiteClamp(horizonWarmth, 0.0f, 2.0f, 0.18f);
        masterVolume = finiteClamp(masterVolume, 0.0f, 1.0f, 0.45f);
        cameraOrbitSensitivity = finiteClamp(cameraOrbitSensitivity, 0.003f, 0.020f, DEFAULT_CAMERA_ORBIT_SENSITIVITY);
        cameraZoomSensitivity = finiteClamp(cameraZoomSensitivity, 0.007f, 0.040f, DEFAULT_CAMERA_ZOOM_SENSITIVITY);
        setSunTint(sunTint[0], sunTint[1], sunTint[2]);
        setMoonTint(moonTint[0], moonTint[1], moonTint[2]);
        setStarTint(starTint[0], starTint[1], starTint[2]);
        setCloudTint(cloudTint[0], cloudTint[1], cloudTint[2]);
        setSkyArtTint(skyArtTint[0], skyArtTint[1], skyArtTint[2]);
    }

    public static float finiteClamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
