package com.solum.engine.environment.p63;

/**
 * Single source of truth for the bounded P63.3 analytic celestial experiment.
 * Values are sanitized at the boundary so renderer code never receives NaN or Infinity.
 */
public final class SolumCelestialControlState {
    public static final float DEFAULT_TIME = 960.0f;
    public static final float DEFAULT_SUN_ANGULAR_SIZE = 0.96f;
    public static final float DEFAULT_MOON_ANGULAR_SIZE = 1.02f;
    public static final float PHYSICAL_SUN_ANGULAR_DIAMETER_REFERENCE = 0.533f;
    public static final float PHYSICAL_MOON_ANGULAR_DIAMETER_REFERENCE = 0.518f;
    /** Exact Ultra_Dynamic_Sky CDO Bottom Altitude. */
    public static final float UDS_CLOUD_BOTTOM_ALTITUDE_KM = 0.60f;
    /** Exact UDS Volumetric Cloud Layer Height at Layer Height Scale=1 and cloud scale=1. */
    public static final float UDS_CLOUD_LAYER_HEIGHT_KM = 0.70f;
    /** Numeric input ceiling. The shader applies a finite HDR shoulder before framebuffer output. */
    public static final float SUN_EMISSIVE_GAIN_SAFETY_MAX = 10_000.0f;
    public static final float DEFAULT_CAMERA_ORBIT_SENSITIVITY = 0.002f;
    public static final float DEFAULT_CAMERA_ZOOM_SENSITIVITY = 0.021f;

    public boolean skyEnabled = true;
    public boolean sunEnabled = true;
    public boolean moonEnabled = true;
    public boolean timePaused = true;
    public boolean oldIblActive = true;
    public boolean p63IblEnabled = false;
    public boolean cloudsEnabled = true;
    public boolean starsEnabled = true;
    public boolean precipitationEnabled = true;
    public boolean surfaceWeatherEnabled = true;
    public boolean lightningEnabled = true;
    public boolean verifiedWeatherAudioEnabled = true;
    public boolean weatherDrivesSky = true;
    public boolean smartWeatherEnabled = false;
    public String climateProfile = SolumSeasonalWeatherPolicy.TEMPERATE_NORTH;
    public boolean proceduralAudioEnabled = false;
    public boolean analyticSky = true;
    public boolean analyticSun = true;
    public boolean analyticMoon = true;
    public boolean analyticStars = true;
    public boolean analyticClouds = true;
    public boolean legacyCelestialFallback = true;

    public float time = DEFAULT_TIME;
    public float timeSpeed = 1.0f;
    public float dayLengthMinutes = SolumTimeSystem.DEFAULT_DAY_LENGTH_MINUTES;

    public float sunLightLux = 25.0f;
    public float sunVisualBrightness = 1.0f;
    public float sunEmissive = 2.20f;
    public float sunEdgeSoftness = 0.72f;
    public float sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE;
    public float sunElevationOffsetDegrees = 0.0f;
    public final float[] sunTint = {1.0f, 1.0f, 1.0f};
    public float sunDiscLuminanceNits = 35_000.0f;
    public float sunHaloSize = 2.8f;
    public float sunHaloFalloff = 5.5f;
    public float sunBloomContribution = 0.35f;
    public float sunExposureWeight = 1.0f;
    public float sunLimbDarkening = 0.55f;
    public float sunDiscVisibility = 1.0f;
    /** Exact UDS default/manual Sun trajectory controls; separate from legacy art offsets. */
    public boolean udsExactSunTrajectory = true;
    public boolean udsDaylightSavingsTime = false;
    public float udsDawnTime = 600.0f;
    public float udsDuskTime = 1800.0f;
    public float udsSunPitchDegrees = 30.0f;
    public float udsSunYawDegrees = 0.0f;
    public float udsSunVerticalOffset = 0.0f;
    public float udsExtendDawnAndDusk = 0.0f;

    public float moonPhase = 0.62f;
    public float moonAngularSizeDegrees = DEFAULT_MOON_ANGULAR_SIZE;
    public float moonVisualBrightness = 0.82f;
    public float moonEmissive = 0.36f;
    public float moonEdgeSoftness = 0.68f;
    public float moonLightLux = 0.15f;
    public float moonElevationOffsetDegrees = 0.0f;
    /** Exact Ultra_Dynamic_Sky CDO Moon Material Color (linear). */
    public final float[] moonTint = {0.486328f, 0.574971f, 0.864583f};
    public float moonPhaseAngleDegrees = 68.4f;
    public float moonVisualLuminanceNits = 2_200.0f;
    public float moonEarthshine = 0.018f;
    public float moonNormalStrength = 0.32f;

    public boolean sunGlowEnabled = true;
    public boolean moonGlowEnabled = true;
    public boolean exposureCompensationEnabled = false;
    public boolean highlightClampEnabled = true;
    public boolean bloomLikeEnabled = true;
    public boolean lightShaftsEnabled = false;
    public boolean lensFlareEnabled = true;
    public boolean lensFlareStarburst = true;
    public float sunGlow = 0.48f;
    /** Exact Ultra_Dynamic_Sky CDO Moon Glow Intensity. */
    public float moonGlow = 0.05f;
    public float exposureCompensation = 0.0f;
    public float highlightClamp = 1.0f;
    public float bloomLikeResponse = 0.055f;
    public float lensFlareIntensity = 0.55f;
    public float lensFlareChromaticAberration = 0.004f;
    public float lensFlareGhostCount = 0.0f;
    public float lensFlareGhostSpacing = 0.58f;
    public float lensFlareHaloThickness = 0.0f;
    public float lensFlareHaloRadius = 0.34f;
    public String lensFlarePreset = "Clean";

    public float starDensity = 0.62f;
    public float starBrightness = 1.35f;
    public float starSize = 1.05f;
    public float starTwinkleAmount = 0.28f;
    public float starLimitingMagnitude = 6.0f;
    public float milkyWayIntensity = 0.0f;
    public float milkyWaySaturation = 1.0f;
    public float siderealRotationDegrees = 0.0f;
    public final float[] starTint = {0.82f, 0.90f, 1.0f};

    public float cloudCoverage = 0.28f;
    public float cloudDensity = 0.58f;
    public float cloudSoftness = 0.72f;
    /** Exact UDS Ultra_Dynamic_Sky CDO default: Cloud Speed=0.35. */
    public float cloudSpeed = 0.35f;
    public float cloudBrightness = 0.86f;
    public float cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
    public float cloudThicknessKm = UDS_CLOUD_LAYER_HEIGHT_KM;
    public float cloudErosion = 0.42f;
    /** Exact UDS Ultra_Dynamic_Sky CDO default: Formation Change Speed=0.7. */
    public float cloudEvolution = 0.70f;
    public float cloudSilverLining = 0.48f;
    public float cloudWindDirectionDegrees = -125.0f;
    public float cloudShadowStrength = 0.72f;
    public String cloudType = "Cumulus";
    public String cloudQuality = "Low";
    /** Neutral multiplier: Sun/Moon/sky lighting supplies cloud chroma. */
    public final float[] cloudTint = {1.0f, 1.0f, 1.0f};
    public String activeCloudPreset = "Light Clouds";
    public float lightShaftStrength = 0.32f;
    public boolean auroraEnabled = false;
    public float auroraIntensity = 0.85f;
    public float auroraScale = 4.6f;
    public float auroraSpeed = 0.18f;
    public float auroraShapeSpeed = 0.12f;
    public float auroraPower = 5.0f;
    public float auroraHorizonHeight = 0.48f;
    public final float[] auroraColor = {0.20f, 1.0f, 0.55f};

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
        precipitationEnabled = true; surfaceWeatherEnabled = true; lightningEnabled = true;
        verifiedWeatherAudioEnabled = true; weatherDrivesSky = true;
        smartWeatherEnabled = false;
        climateProfile = SolumSeasonalWeatherPolicy.TEMPERATE_NORTH;
        proceduralAudioEnabled = false; time = DEFAULT_TIME; timeSpeed = 1.0f;
        dayLengthMinutes = SolumTimeSystem.DEFAULT_DAY_LENGTH_MINUTES;
        resetSun(); resetMoon(); resetStars(); resetClouds(); resetPostProcess(); masterVolume = 0.45f; muted = false;
        cameraOrbitSensitivity = DEFAULT_CAMERA_ORBIT_SENSITIVITY;
        cameraZoomSensitivity = DEFAULT_CAMERA_ZOOM_SENSITIVITY;
        turbidity = 2.4f; rayleigh = 1.0f; mie = 1.0f; mieG = 0.76f; ozone = 1.0f;
        horizonHaze = 0.32f; nightFloor = 0.012f; sunsetSaturation = 1.08f;
        sunsetContrast = 1.05f; horizonWarmth = 0.18f; setSkyArtTint(1.0f, 1.0f, 1.0f);
        activeScenarioPreset = "Clear Noon";
        auroraEnabled = false; auroraIntensity = 0.85f; auroraScale = 4.6f; auroraSpeed = 0.18f;
        auroraShapeSpeed = 0.12f; auroraPower = 5.0f; auroraHorizonHeight = 0.48f;
        setAuroraColor(0.20f, 1.0f, 0.55f);
        activeSkySource = "FILAMENT_ADAPTED_ANALYTIC_SKY";
        lastCelestialError = "none";
    }

    public void resetSun() {
        sunEnabled = true; sunLightLux = 25.0f; sunVisualBrightness = 1.0f;
        sunEmissive = 2.20f; sunEdgeSoftness = 0.72f;
        sunDiscLuminanceNits = 35_000.0f; sunHaloSize = 2.8f; sunHaloFalloff = 5.5f;
        sunBloomContribution = 0.35f; sunExposureWeight = 1.0f; sunLimbDarkening = 0.55f;
        sunDiscVisibility = 1.0f;
        udsExactSunTrajectory = true; udsDaylightSavingsTime = false;
        udsDawnTime = 600.0f; udsDuskTime = 1800.0f;
        udsSunPitchDegrees = 30.0f; udsSunYawDegrees = 0.0f;
        udsSunVerticalOffset = 0.0f; udsExtendDawnAndDusk = 0.0f;
        sunAngularSizeDegrees = DEFAULT_SUN_ANGULAR_SIZE; sunElevationOffsetDegrees = 0.0f;
        sunGlowEnabled = true; sunGlow = 0.48f; bloomLikeEnabled = true; bloomLikeResponse = 0.055f;
        setSunTint(1.0f, 1.0f, 1.0f);
    }

    public void applySunPreset(String preset) {
        if ("Soft".equals(preset)) {
            sunLightLux = 6.0f; sunDiscLuminanceNits = 7_500.0f; sunEmissive = 1.15f; sunAngularSizeDegrees = 0.76f;
            sunHaloSize = 3.2f; sunHaloFalloff = 4.5f; sunBloomContribution = 0.16f;
            setSunTint(1.0f, 1.0f, 1.0f);
        } else if ("Physical Noon".equals(preset)) {
            sunLightLux = 50.0f; sunDiscLuminanceNits = 100_000.0f; sunEmissive = 1.55f; sunAngularSizeDegrees = 0.68f;
            sunHaloSize = 2.2f; sunHaloFalloff = 6.5f; sunBloomContribution = 0.42f;
            setSunTint(1.0f, 1.0f, 1.0f);
        } else if ("Golden Hour".equals(preset)) {
            sunLightLux = 12.0f; sunDiscLuminanceNits = 42_000.0f; sunEmissive = 1.45f; sunAngularSizeDegrees = 0.72f;
            sunHaloSize = 4.0f; sunHaloFalloff = 3.8f; sunBloomContribution = 0.38f;
            sunElevationOffsetDegrees = -4.0f; setSunTint(1.0f, 1.0f, 1.0f);
        } else if ("Overcast".equals(preset)) {
            sunLightLux = 3.0f; sunDiscLuminanceNits = 4_000.0f; sunEmissive = 0.95f; sunAngularSizeDegrees = 0.82f;
            sunHaloSize = 5.0f; sunHaloFalloff = 3.0f; sunBloomContribution = 0.08f;
            setSunTint(1.0f, 1.0f, 1.0f);
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
        moonVisualLuminanceNits = 2_200.0f; moonEarthshine = 0.018f; moonNormalStrength = 0.32f;
        moonLightLux = 0.15f; moonElevationOffsetDegrees = 0.0f;
        moonGlowEnabled = true; moonGlow = 0.05f;
        setMoonTint(0.486328f, 0.574971f, 0.864583f);
    }

    public void resetStars() {
        starsEnabled = true; starDensity = 0.62f; starBrightness = 1.35f;
        starSize = 1.05f; starTwinkleAmount = 0.28f; starLimitingMagnitude = 6.0f;
        milkyWayIntensity = 0.0f; milkyWaySaturation = 1.0f; siderealRotationDegrees = 0.0f;
        setStarTint(0.82f, 0.90f, 1.0f);
    }

    public void resetClouds() {
        cloudsEnabled = true; cloudCoverage = 0.28f; cloudDensity = 0.58f;
        cloudSoftness = 0.72f; cloudSpeed = 0.35f; cloudBrightness = 0.86f;
        cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
        cloudThicknessKm = UDS_CLOUD_LAYER_HEIGHT_KM; cloudErosion = 0.42f;
        cloudEvolution = 0.70f; cloudSilverLining = 0.48f; cloudQuality = "Low";
        cloudWindDirectionDegrees = -125.0f; cloudShadowStrength = 0.72f; cloudType = "Cumulus";
        activeCloudPreset = "Light Clouds";
        setCloudTint(1.0f, 1.0f, 1.0f);
    }

    public void applyCloudPreset(String preset) {
        if ("Clear".equals(preset)) {
            cloudsEnabled = false; cloudCoverage = 0.0f; cloudDensity = 0.35f; cloudSoftness = 0.80f;
            cloudSpeed = 0.16f; cloudEvolution = 0.20f; cloudBrightness = 1.0f;
            cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
            cloudThicknessKm = UDS_CLOUD_LAYER_HEIGHT_KM; setCloudTint(1.0f, 1.0f, 1.0f);
            cloudType = "Cumulus";
        } else if ("Partly Cloudy".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.48f; cloudDensity = 0.62f; cloudSoftness = 0.70f;
            cloudSpeed = 0.28f; cloudEvolution = 0.36f; cloudBrightness = 0.88f;
            cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
            cloudThicknessKm = 0.80f; setCloudTint(1.0f, 1.0f, 1.0f);
            cloudType = "Cumulus";
        } else if ("Cloudy".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.82f; cloudDensity = 0.78f; cloudSoftness = 0.62f;
            cloudSpeed = 0.20f; cloudEvolution = 0.24f; cloudBrightness = 0.76f;
            cloudHeightKm = 0.55f; cloudThicknessKm = 0.95f; setCloudTint(0.86f, 0.90f, 0.98f);
            cloudType = "Stratocumulus";
        } else if ("Sunset Clouds".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.55f; cloudDensity = 0.66f; cloudSoftness = 0.72f;
            cloudSpeed = 0.24f; cloudEvolution = 0.30f; cloudBrightness = 0.92f; time = 1800.0f;
            cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
            cloudThicknessKm = 0.78f; setCloudTint(1.0f, 1.0f, 1.0f);
            cloudType = "Cumulus";
        } else if ("Night Clouds".equals(preset)) {
            cloudsEnabled = true; cloudCoverage = 0.42f; cloudDensity = 0.70f; cloudSoftness = 0.68f;
            cloudSpeed = 0.18f; cloudEvolution = 0.22f; cloudBrightness = 0.44f; time = 0.0f;
            cloudHeightKm = UDS_CLOUD_BOTTOM_ALTITUDE_KM;
            cloudThicknessKm = 0.82f; setCloudTint(0.84f, 0.90f, 1.0f);
            cloudType = "Stratocumulus";
        } else {
            resetClouds();
            preset = "Light Clouds";
        }
        activeCloudPreset = preset;
        sanitize();
    }

    public void applyScenarioPreset(String preset) {
        timePaused = true;
        if (!"Aurora Night".equals(preset)) auroraEnabled = false;
        if ("Clear Noon".equals(preset)) { time = 1200.0f; applyCloudPreset("Clear"); applySunPreset("Physical Noon"); }
        else if ("Golden Hour".equals(preset)) { time = 1700.0f; applyCloudPreset("Light Clouds"); applySunPreset("Golden Hour"); }
        else if ("Sunset".equals(preset)) { time = 1800.0f; applyCloudPreset("Light Clouds"); applySunPreset("Golden Hour"); }
        else if ("Civil Twilight".equals(preset)) { time = 1900.0f; applyCloudPreset("Clear"); }
        else if ("Clear Midnight".equals(preset)) { time = 0.0f; applyCloudPreset("Clear"); }
        else if ("Quarter Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 90.0f; applyCloudPreset("Clear"); }
        else if ("Crescent Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 150.0f; applyCloudPreset("Clear"); }
        else if ("Full Moon".equals(preset)) { time = 0.0f; moonPhaseAngleDegrees = 0.0f; applyCloudPreset("Clear"); }
        else if ("Milky Way Night".equals(preset)) { time = 0.0f; milkyWayIntensity = 0.75f; starsEnabled = true; applyCloudPreset("Clear"); }
        else if ("Aurora Night".equals(preset)) {
            time = 0.0f; auroraEnabled = true; auroraIntensity = 0.90f; starsEnabled = true;
            applyCloudPreset("Light Clouds"); cloudCoverage = 0.18f; cloudDensity = 0.48f;
            setAuroraColor(0.20f, 1.0f, 0.55f);
        }
        else if ("Light Clouds".equals(preset) || "Partly Cloudy".equals(preset)
                || "Sunset Clouds".equals(preset) || "Night Clouds".equals(preset)) { applyCloudPreset(preset); }
        else if ("Overcast".equals(preset)) { applyCloudPreset("Cloudy"); applySunPreset("Overcast"); }
        activeScenarioPreset = preset;
        sanitize();
    }

    public void resetPostProcess() {
        sunGlowEnabled = true; moonGlowEnabled = true; exposureCompensationEnabled = false;
        highlightClampEnabled = true; bloomLikeEnabled = true; lightShaftsEnabled = false;
        lensFlareEnabled = true; lensFlareStarburst = true;
        sunGlow = 0.48f; moonGlow = 0.05f; exposureCompensation = 0.0f;
        highlightClamp = 1.0f; bloomLikeResponse = 0.055f;
        lensFlareIntensity = 0.55f; lensFlareChromaticAberration = 0.004f;
        lensFlareGhostCount = 0.0f; lensFlareGhostSpacing = 0.58f;
        lensFlareHaloThickness = 0.0f; lensFlareHaloRadius = 0.34f;
        lensFlarePreset = "Clean";
        postProcessStatus = "baseline_tone_mapping_highlight_clamp_state";
    }

    public void applyLensFlarePreset(String preset) {
        if ("Off".equals(preset)) {
            lensFlareEnabled = false; lensFlareStarburst = false; lensFlareIntensity = 0.0f;
        } else if ("Cinematic".equals(preset)) {
            lensFlareEnabled = true; lensFlareStarburst = true; lensFlareIntensity = 0.62f;
            lensFlareChromaticAberration = 0.008f; lensFlareGhostCount = 6.0f;
            lensFlareGhostSpacing = 0.62f; lensFlareHaloThickness = 0.085f; lensFlareHaloRadius = 0.39f;
        } else if ("Anamorphic".equals(preset)) {
            lensFlareEnabled = true; lensFlareStarburst = true; lensFlareIntensity = 0.52f;
            lensFlareChromaticAberration = 0.006f; lensFlareGhostCount = 3.0f;
            lensFlareGhostSpacing = 0.72f; lensFlareHaloThickness = 0.045f; lensFlareHaloRadius = 0.42f;
        } else {
            preset = "Clean"; lensFlareEnabled = true; lensFlareStarburst = true; lensFlareIntensity = 0.55f;
            lensFlareChromaticAberration = 0.004f; lensFlareGhostCount = 0.0f;
            lensFlareGhostSpacing = 0.58f; lensFlareHaloThickness = 0.0f; lensFlareHaloRadius = 0.34f;
        }
        lensFlarePreset = preset;
        sanitize();
    }

    public void setSunTint(float red, float green, float blue) {
        sunTint[0] = finiteClamp(red, 0.0f, 1.0f, 1.0f);
        sunTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.92f);
        sunTint[2] = finiteClamp(blue, 0.0f, 1.0f, 0.72f);
    }

    public void setMoonTint(float red, float green, float blue) {
        moonTint[0] = finiteClamp(red, 0.0f, 1.0f, 0.86f);
        moonTint[1] = finiteClamp(green, 0.0f, 1.0f, 0.89f);
        moonTint[2] = finiteClamp(blue, 0.0f, 1.0f, 0.95f);
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

    public void setAuroraColor(float red, float green, float blue) {
        auroraColor[0] = finiteClamp(red, 0.0f, 1.0f, 0.20f);
        auroraColor[1] = finiteClamp(green, 0.0f, 1.0f, 1.0f);
        auroraColor[2] = finiteClamp(blue, 0.0f, 1.0f, 0.55f);
    }

    public void sanitize() {
        time = finiteClamp(time, 0.0f, 2400.0f, DEFAULT_TIME);
        timeSpeed = finiteClamp(timeSpeed, 0.0f, 8.0f, 1.0f);
        dayLengthMinutes = finiteClamp(dayLengthMinutes, 1.0f, 1440.0f, SolumTimeSystem.DEFAULT_DAY_LENGTH_MINUTES);
        sunLightLux = finiteClamp(sunLightLux, 0.0f, SolumAnalyticSkyMaterial.SUN_LUX_SAFETY_MAX, 25.0f);
        sunVisualBrightness = finiteClamp(sunVisualBrightness, 0.0f, 2.0f, 1.0f);
        sunEmissive = finiteClamp(sunEmissive, 0.0f, SUN_EMISSIVE_GAIN_SAFETY_MAX, 2.20f);
        sunDiscLuminanceNits = finiteClamp(sunDiscLuminanceNits, 0.0f,
            SolumAnalyticSkyMaterial.SUN_LUMINANCE_SAFETY_MAX_NITS, 35_000.0f);
        sunHaloSize = finiteClamp(sunHaloSize, 0.1f, 64.0f, 2.8f);
        sunHaloFalloff = finiteClamp(sunHaloFalloff, 0.1f, 100.0f, 5.5f);
        sunBloomContribution = finiteClamp(sunBloomContribution, 0.0f, 32.0f, 0.35f);
        sunExposureWeight = finiteClamp(sunExposureWeight, 0.001f, 32.0f, 1.0f);
        sunLimbDarkening = finiteClamp(sunLimbDarkening, 0.0f, 8.0f, 0.55f);
        sunDiscVisibility = finiteClamp(sunDiscVisibility, 0.0f, 1.0f, 1.0f);
        sunEdgeSoftness = finiteClamp(sunEdgeSoftness, 0.0f, 1.0f, 0.72f);
        sunAngularSizeDegrees = finiteClamp(sunAngularSizeDegrees, 0.02f, 10.0f, DEFAULT_SUN_ANGULAR_SIZE);
        sunElevationOffsetDegrees = finiteClamp(sunElevationOffsetDegrees, -90.0f, 90.0f, 0.0f);
        udsDawnTime = finiteClamp(udsDawnTime, 0.0f, 2399.0f, 600.0f);
        udsDuskTime = finiteClamp(udsDuskTime, 1.0f, 2400.0f, 1800.0f);
        if (udsDawnTime >= udsDuskTime) { udsDawnTime = 600.0f; udsDuskTime = 1800.0f; }
        udsSunPitchDegrees = finiteClamp(udsSunPitchDegrees, -3600.0f, 3600.0f, 30.0f);
        udsSunYawDegrees = finiteClamp(udsSunYawDegrees, -3600.0f, 3600.0f, 0.0f);
        udsSunVerticalOffset = finiteClamp(udsSunVerticalOffset, -1.0f, 1.0f, 0.0f);
        udsExtendDawnAndDusk = finiteClamp(udsExtendDawnAndDusk, 0.0f, 5.0f, 0.0f);
        moonPhaseAngleDegrees = finiteClamp(moonPhaseAngleDegrees, 0.0f, 180.0f, 68.4f);
        moonPhase = 1.0f - moonPhaseAngleDegrees / 180.0f;
        moonAngularSizeDegrees = finiteClamp(moonAngularSizeDegrees, 0.02f, 10.0f, DEFAULT_MOON_ANGULAR_SIZE);
        moonVisualBrightness = finiteClamp(moonVisualBrightness, 0.0f, 2.0f, 0.82f);
        moonVisualLuminanceNits = finiteClamp(moonVisualLuminanceNits, 0.0f,
            SolumAnalyticSkyMaterial.MOON_LUMINANCE_SAFETY_MAX_NITS, 2_200.0f);
        moonEarthshine = finiteClamp(moonEarthshine, 0.0f, 1.0f, 0.018f);
        moonNormalStrength = finiteClamp(moonNormalStrength, 0.0f, 8.0f, 0.32f);
        moonEmissive = finiteClamp(moonEmissive, 0.0f, 1.5f, 0.36f);
        moonEdgeSoftness = finiteClamp(moonEdgeSoftness, 0.0f, 1.0f, 0.68f);
        moonLightLux = finiteClamp(moonLightLux, 0.0f, SolumAnalyticSkyMaterial.MOON_LUX_SAFETY_MAX, 0.15f);
        moonElevationOffsetDegrees = finiteClamp(moonElevationOffsetDegrees, -90.0f, 90.0f, 0.0f);
        sunGlow = finiteClamp(sunGlow, 0.0f, 1.0f, 0.48f);
        moonGlow = finiteClamp(moonGlow, 0.0f, 4.0f, 0.05f);
        exposureCompensation = finiteClamp(exposureCompensation, -1.0f, 1.0f, 0.0f);
        highlightClamp = finiteClamp(highlightClamp, 0.50f, 1.0f, 1.0f);
        bloomLikeResponse = finiteClamp(bloomLikeResponse, 0.0f, 0.25f, 0.055f);
        lensFlareIntensity = finiteClamp(lensFlareIntensity, 0.0f, 1.0f, 0.55f);
        lensFlareChromaticAberration = finiteClamp(lensFlareChromaticAberration, 0.0f, 0.05f, 0.004f);
        lensFlareGhostCount = finiteClamp(lensFlareGhostCount, 0.0f, 8.0f, 0.0f);
        lensFlareGhostSpacing = finiteClamp(lensFlareGhostSpacing, 0.0f, 0.95f, 0.58f);
        lensFlareHaloThickness = finiteClamp(lensFlareHaloThickness, 0.0f, 0.25f, 0.0f);
        lensFlareHaloRadius = finiteClamp(lensFlareHaloRadius, 0.0f, 0.5f, 0.34f);
        starDensity = finiteClamp(starDensity, 0.0f, 1.0f, 0.62f);
        starBrightness = finiteClamp(starBrightness, 0.0f, 32.0f, 1.35f);
        starSize = finiteClamp(starSize, 0.10f, 8.0f, 1.05f);
        starTwinkleAmount = finiteClamp(starTwinkleAmount, 0.0f, 4.0f, 0.28f);
        starLimitingMagnitude = finiteClamp(starLimitingMagnitude, 0.0f, 32.0f, 6.0f);
        milkyWayIntensity = finiteClamp(milkyWayIntensity, 0.0f, 16.0f, 0.0f);
        milkyWaySaturation = finiteClamp(milkyWaySaturation, 0.0f, 8.0f, 1.0f);
        siderealRotationDegrees = finiteClamp(siderealRotationDegrees, -3600.0f, 3600.0f, 0.0f);
        cloudCoverage = finiteClamp(cloudCoverage, 0.0f, 1.0f, 0.28f);
        cloudDensity = finiteClamp(cloudDensity, 0.0f, 1.0f, 0.58f);
        cloudSoftness = finiteClamp(cloudSoftness, 0.0f, 1.0f, 0.72f);
        cloudSpeed = finiteClamp(cloudSpeed, 0.0f, 20.0f, 0.35f);
        cloudBrightness = finiteClamp(cloudBrightness, 0.0f, 8.0f, 0.86f);
        cloudHeightKm = finiteClamp(cloudHeightKm, 0.1f, 100.0f, UDS_CLOUD_BOTTOM_ALTITUDE_KM);
        cloudThicknessKm = finiteClamp(cloudThicknessKm, 0.01f, 50.0f, UDS_CLOUD_LAYER_HEIGHT_KM);
        cloudErosion = finiteClamp(cloudErosion, 0.0f, 1.0f, 0.42f);
        cloudEvolution = finiteClamp(cloudEvolution, 0.0f, 20.0f, 0.70f);
        cloudSilverLining = finiteClamp(cloudSilverLining, 0.0f, 10.0f, 0.48f);
        cloudWindDirectionDegrees = finiteClamp(cloudWindDirectionDegrees, -3600.0f, 3600.0f, -125.0f);
        cloudShadowStrength = finiteClamp(cloudShadowStrength, 0.0f, 1.0f, 0.72f);
        lightShaftStrength = finiteClamp(lightShaftStrength, 0.0f, 2.0f, 0.32f);
        auroraIntensity = finiteClamp(auroraIntensity, 0.0f, 8.0f, 0.85f);
        auroraScale = finiteClamp(auroraScale, 0.1f, 64.0f, 4.6f);
        auroraSpeed = finiteClamp(auroraSpeed, -8.0f, 8.0f, 0.18f);
        auroraShapeSpeed = finiteClamp(auroraShapeSpeed, -8.0f, 8.0f, 0.12f);
        auroraPower = finiteClamp(auroraPower, 0.25f, 32.0f, 5.0f);
        auroraHorizonHeight = finiteClamp(auroraHorizonHeight, 0.08f, 0.85f, 0.48f);
        if (!"Cumulus".equals(cloudType) && !"Stratocumulus".equals(cloudType)
                && !"Cirrus".equals(cloudType) && !"Storm".equals(cloudType)) cloudType = "Cumulus";
        if (!"Low".equals(cloudQuality) && !"Medium".equals(cloudQuality)
                && !"High Experimental".equals(cloudQuality)) cloudQuality = "Low";
        climateProfile = SolumSeasonalWeatherPolicy.sanitizeProfile(climateProfile);
        turbidity = finiteClamp(turbidity, 0.1f, 50.0f, 2.4f);
        rayleigh = finiteClamp(rayleigh, 0.0f, 32.0f, 1.0f);
        mie = finiteClamp(mie, 0.0f, 32.0f, 1.0f);
        mieG = finiteClamp(mieG, -0.99f, 0.99f, 0.76f);
        ozone = finiteClamp(ozone, 0.0f, 32.0f, 1.0f);
        horizonHaze = finiteClamp(horizonHaze, 0.0f, 10.0f, 0.32f);
        nightFloor = finiteClamp(nightFloor, 0.0f, 2.0f, 0.012f);
        sunsetSaturation = finiteClamp(sunsetSaturation, 0.0f, 8.0f, 1.08f);
        sunsetContrast = finiteClamp(sunsetContrast, 0.05f, 8.0f, 1.05f);
        horizonWarmth = finiteClamp(horizonWarmth, 0.0f, 8.0f, 0.18f);
        masterVolume = finiteClamp(masterVolume, 0.0f, 1.0f, 0.45f);
        cameraOrbitSensitivity = finiteClamp(cameraOrbitSensitivity, 0.0002f, 0.050f, DEFAULT_CAMERA_ORBIT_SENSITIVITY);
        cameraZoomSensitivity = finiteClamp(cameraZoomSensitivity, 0.007f, 0.040f, DEFAULT_CAMERA_ZOOM_SENSITIVITY);
        setSunTint(sunTint[0], sunTint[1], sunTint[2]);
        setMoonTint(moonTint[0], moonTint[1], moonTint[2]);
        setStarTint(starTint[0], starTint[1], starTint[2]);
        setCloudTint(cloudTint[0], cloudTint[1], cloudTint[2]);
        setSkyArtTint(skyArtTint[0], skyArtTint[1], skyArtTint[2]);
        setAuroraColor(auroraColor[0], auroraColor[1], auroraColor[2]);
    }

    public static float finiteClamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
