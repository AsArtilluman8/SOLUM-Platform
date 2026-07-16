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

    public final float[] sunDirection = {0.0f, 1.0f, 0.0f};
    public final float[] moonDirection = {0.0f, 1.0f, 0.0f};
    public final float[] moonToSunDirection = {0.0f, 0.0f, -1.0f};
    public final float[] sunTint = {1.0f, 0.96f, 0.88f};
    public final float[] moonTint = {0.78f, 0.84f, 0.96f};
    public final float[] starTint = {0.86f, 0.92f, 1.0f};
    public final float[] cloudArtTint = {1.0f, 1.0f, 1.0f};
    public final float[] skyArtTint = {1.0f, 1.0f, 1.0f};

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
    public float sunAngularDiameterDegrees = 0.53f;
    public float sunHaloSize = 2.8f;
    public float sunHaloFalloff = 5.5f;
    public float sunBloomContribution = 0.35f;
    public float sunExposureWeight = 1.0f;
    public float sunLimbDarkening = 0.55f;

    public float moonPhaseAngleDegrees = 68.4f;
    public float moonVisualLuminanceNits = 2200.0f;
    public float moonEarthshine = 0.035f;
    public float moonNormalStrength = 0.32f;
    public float moonHalo = 0.24f;
    public float moonAngularDiameterDegrees = 0.52f;

    public float starDensity = 0.72f;
    public float starBrightness = 1.0f;
    public float starLimitingMagnitude = 6.0f;
    public float starSize = 1.0f;
    public float starTwinkle = 0.22f;
    public float milkyWayIntensity = 0.0f;
    public float milkyWaySaturation = 1.0f;
    public float siderealRotationDegrees;

    public float cloudCoverage = 0.28f;
    public float cloudDensity = 0.58f;
    public float cloudSoftness = 0.72f;
    public float cloudHeightKm = 2.2f;
    public float cloudThicknessKm = 1.4f;
    public float cloudErosion = 0.42f;
    public float cloudWindSpeed = 0.22f;
    public float cloudEvolution = 0.12f;
    public float cloudSilverLining = 0.48f;
    public float cloudBrightness = 0.86f;
    public float elapsedSeconds;
    public String cloudQuality = "Low";

    public String activeRenderer = "analytic_initializing";
    public String materialVariant = "analytic_sky_mobile_low";
    public String moonSource = "UNAVAILABLE";
    public String starSource = "SOLUM_NATIVE_PROCEDURAL";
    public String cloudSource = "SOLUM_NATIVE_PROCEDURAL_SPHERICAL_SHELL";
    public String resourceProvenance = "FILAMENT_ADAPTED+SOLUM_NATIVE";
    public String lastSkyError = "none";
    public int skyDrawCalls;
    public int materialBuildCount;
    public int materialRebuildCount;
    public long uniformUpdateCount;
}
