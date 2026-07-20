package com.solum.engine.environment.p63;

/**
 * Render-thread input for the P63.3 single-pass analytic sky.
 *
 * <p>This deliberately contains only finite, sanitized values. The renderer owns the Filament
 * objects; this state owns no GPU resources and may be rebuilt without rebuilding the material.</p>
 */
public final class SolumAnalyticSkyState {
    public boolean analyticSky = true;
    public boolean analyticSun = true;
    public boolean analyticMoon = true;
    public boolean analyticStars = true;
    public boolean analyticClouds = true;
    public boolean legacyCelestialFallback = true;
    public boolean oldIbl = true;
    public boolean p63DynamicIbl = false;
    public boolean udsExactSunValues;

    public final float[] sunDirection = {0.0f, 1.0f, 0.0f};
    public final float[] moonDirection = {0.0f, 1.0f, 0.0f};
    public final float[] moonToSunDirection = {0.0f, 0.0f, -1.0f};
    public final float[] sunTint = {1.0f, 1.0f, 1.0f};
    public final float[] moonTint = {0.486328f, 0.574971f, 0.864583f};
    public final float[] starTint = {0.86f, 0.92f, 1.0f};
    public final float[] cloudArtTint = {1.0f, 1.0f, 1.0f};
    public final float[] skyArtTint = {1.0f, 1.0f, 1.0f};
    public final float[] auroraColor = {0.20f, 1.0f, 0.55f};

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

    public float sunDiscLuminanceNits = 35000.0f;
    public float sunEmissiveGain = 2.20f;
    public float sunAngularDiameterDegrees = SolumCelestialControlState.DEFAULT_SUN_ANGULAR_SIZE;
    public float sunHaloSize = 2.8f;
    public float sunHaloFalloff = 5.5f;
    public float sunBloomContribution = 0.35f;
    public float sunExposureWeight = 1.0f;
    public float sunLimbDarkening = SolumCelestialControlState.UDS_DEFAULT_SUN_SOFTNESS;
    public float sunDiscVisibility = 1.0f;
    public float sunRadiusRadians = (float)Math.toRadians(SolumCelestialControlState.DEFAULT_SUN_ANGULAR_SIZE);
    public String sunValuesStatus = "NOT_EVALUATED";

    public float moonPhaseAngleDegrees = 68.4f;
    public float moonVisualLuminanceNits = 2200.0f;
    public float moonEarthshine = 0.018f;
    public float moonNormalStrength = 0.32f;
    public float moonHalo = 0.05f;
    public float moonAngularDiameterDegrees = SolumCelestialControlState.DEFAULT_MOON_ANGULAR_SIZE;

    public float starDensity = 0.62f;
    public float starBrightness = 1.35f;
    public float starLimitingMagnitude = 6.0f;
    public float starSize = 1.05f;
    public float starTwinkle = 0.22f;
    public float milkyWayIntensity = 0.0f;
    public float milkyWaySaturation = 1.0f;
    public float siderealRotationDegrees;

    public float cloudCoverage = 0.28f;
    public float cloudDensity = 0.58f;
    public float cloudSoftness = 0.72f;
    public float cloudHeightKm = SolumCelestialControlState.UDS_CLOUD_BOTTOM_ALTITUDE_KM;
    public float cloudThicknessKm = SolumCelestialControlState.UDS_CLOUD_LAYER_HEIGHT_KM;
    public float cloudErosion = 0.42f;
    public float cloudWindSpeed = 0.35f;
    public float cloudEvolution = 0.70f;
    public float cloudSilverLining = 0.48f;
    public float cloudBrightness = 0.86f;
    public float cloudTypeIndex;
    public float cloudWindDirectionRadians = (float)Math.toRadians(-125.0);
    public float cloudShadowStrength = 0.72f;
    public float lightShaftStrength = 0.32f;
    public float sunLightLux = SolumCelestialControlState.UDS_DEFAULT_SUN_LIGHT_INTENSITY_LUX;
    public float moonLightLux = 0.15f;
    public float weatherRain;
    public float weatherSnow;
    public float weatherDust;
    public float lightningFlash;
    public boolean auroraEnabled;
    public float auroraIntensity = 0.85f;
    public float auroraScale = 4.6f;
    public float auroraSpeed = 0.18f;
    public float auroraShapeSpeed = 0.12f;
    public float auroraPower = 5.0f;
    public float auroraHorizonHeight = 0.48f;
    public float elapsedSeconds;
    public String cloudQuality = "Low";

    public String activeRenderer = "analytic_initializing";
    public String materialVariant = "analytic_sky_mobile_low";
    public String moonSource = "UNAVAILABLE";
    public String starSource = "UNAVAILABLE";
    public String cloudSource = "DISABLED_MISSING_EXACT_UDS_CLOUD_PAYLOAD";
    public String auroraSource = "UNAVAILABLE";
    public String resourceProvenance = "FILAMENT_ADAPTED+SOLUM_NATIVE";
    public String iblConsistency =
        "STATIC_PREPARED_OR_USER_IBL_SCALED_NOT_CAPTURED_FROM_ANALYTIC_SKY";
    public String lastSkyError = "none";
    public float oldIblIntensityScale = 1.0f;
    public int skyDrawCalls;
    public int materialBuildCount;
    public int materialRebuildCount;
    public long uniformUpdateCount;
}
