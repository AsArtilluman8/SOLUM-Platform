package com.solum.engine.environment.p63;

/**
 * Exact UDS Sun value formulas and extracted FRichCurve data.
 *
 * <p>Sources: Current Sun Radius, Current Sun Light Intensity/Color, Current Sun Disk
 * Intensity/Color, Adjust Base Sun Light Intensity, Scaled Directional Balance, Sun Height and
 * the verified Sun_Disk_Color, Sun_Light_Color and Directional_Light_Intensity assets. Inputs are
 * kept in their UDS domains; no SOLUM-normalized cloud value is silently converted here.</p>
 */
public final class SolumUdsSunValues {
    public static final String CONTRACT_STATUS = "VERIFIED_UDS_SUN_VALUE_FORMULAS";
    public static final String CLEAR_RUNTIME_STATUS =
        "IMPLEMENTED_NOT_VISUALLY_VERIFIED_UDS_CLEAR_SKY_VALUES";
    public static final String CLOUD_RUNTIME_STATUS =
        "PARTIAL_UDS_CLOUD_VALUE_WRITERS_NOT_MAPPED";

    private static final SolumUdsRichCurve DIRECTIONAL_INTENSITY = curve(
        key(-0.6100000143051147, 1.0, -0.03837857395410538, -0.03837857395410538, 2),
        key(-0.06863726675510406, 0.44999995827674866, -1.8855160474777222, -1.8855160474777222, 2),
        key(0.011251073330640793, 3.4696359563213264e-8, -5.311466217041016, -5.311466217041016, 2));

    private static final SolumUdsRichCurve[] SUN_DISK_COLOR = {
        curve(key(0.4448404312133789, 0.0, 0.0, 0.0, 2),
            key(0.5, 0.5747108459472656, 0.0, 0.0, 2),
            key(0.5779003500938416, 0.5625, -0.3142126202583313, -0.3142126202583313, 2),
            key(0.7218588590621948, 0.5049999952316284, 0.0, 0.0, 0)),
        curve(key(0.4448404312133789, 0.0, 0.0, 0.0, 2),
            key(0.5, 0.018965275958180428, 0.5293965935707092, 0.5293965935707092, 2),
            key(0.5779003500938416, 0.2149738222360611, 1.904099464416504, 1.904099464416504, 2),
            key(0.7218588590621948, 0.44140660762786865, 0.0, 0.0, 0)),
        curve(key(0.4448404312133789, 0.0, 0.0, 0.0, 2),
            key(0.5, 0.0044427914544939995, 0.08196954429149628, 0.08196954429149628, 2),
            key(0.5779003500938416, 0.07871995121240616, 1.43039870262146, 1.43039870262146, 2),
            key(0.7218588590621948, 0.321789413690567, 0.0, 0.0, 0)),
        curve(key(0.0, 0.9694908857345581, 0.0, 0.0, 0),
            key(1.0, 0.9694908857345581, 0.0, 0.0, 0))
    };

    private static final SolumUdsRichCurve[] SUN_LIGHT_COLOR = {
        curve(key(0.4861408472061157, 0.8500000238418579, 0.0, 0.0, 0),
            key(0.5099999904632568, 0.8500000238418579, 0.0, 0.0, 0),
            key(0.550000011920929, 0.8500000238418579, 0.0, 0.0, 0),
            key(0.6100000143051147, 0.8500000238418579, 0.0, 0.0, 0),
            key(1.0000264644622803, 0.8500000238418579, 0.0, 0.0, 0)),
        curve(key(0.4861408472061157, 0.04769144579768181, 0.0, 0.0, 0),
            key(0.5099999904632568, 0.16025352478027344, 0.0, 0.0, 0),
            key(0.550000011920929, 0.4082125127315521, 0.0, 0.0, 0),
            key(0.6100000143051147, 0.6062620878219604, 0.0, 0.0, 0),
            key(1.0000264644622803, 0.836571216583252, 0.0, 0.0, 0)),
        curve(key(0.4861408472061157, 0.0, 0.0, 0.0, 0),
            key(0.5099999904632568, 0.01328125037252903, 0.0, 0.0, 0),
            key(0.550000011920929, 0.09264997392892838, 0.0, 0.0, 0),
            key(0.6100000143051147, 0.327604204416275, 0.0, 0.0, 0),
            key(1.0000264644622803, 0.7348958849906921, 0.0, 0.0, 0)),
        curve(key(0.0, 0.9694908857345581, 0.0, 0.0, 0),
            key(1.0, 0.9694908857345581, 0.0, 0.0, 0))
    };

    public static final class Inputs {
        public double cachedSunVectorZ;
        public boolean usingSkyAtmosphere;
        public boolean usingSpaceMode;
        public double sunLightIntensityLux = 5.0;
        public double sunDiskIntensity = 4.0;
        public double sunScaleDegrees = 1.2;
        public double scaleSunRadiusNearHorizon = 1.0;
        public double sunSoftness = 3.8;
        public double directionalBalance = 1.0;
        public double lightingBrightnessDay = 1.0;
        public double lightingBrightnessDawnDusk = 1.0;
        public double lightingBrightnessNight = 1.0;
        public double eclipsePercent = 1.0;
        public double saturation = 1.0;
        public double localCloudCoverage;
        public double fog = 1.0;
        public boolean applyFlatCloudiness;
        public boolean cloudPaintCanSubtractCoverage;
        public double cachedDirectionalLightDimming = 1.0;
        public double cachedDirectionalInscatteringMultiplier = 1.0;
        public double cachedInvertedGlobalOcclusion;
        public double sunLightIntensityMultiplierInInteriors = 1.0;
        public boolean fadeDownHighSunLightIntensityBelowHorizon = true;
        public final float[] sunLightColor = {1.0f, 1.0f, 1.0f, 1.0f};
        public final float[] sunDiskTint = {1.0f, 1.0f, 1.0f, 1.0f};
        public final float[] cachedSolarEclipseTint = {1.0f, 1.0f, 1.0f, 1.0f};
    }

    public static final class Output {
        public float sunHeight;
        public float currentSceneLightingBrightnessScale;
        public float scaledDirectionalBalance;
        public float currentSunRadiusRadians;
        public float currentSunLightIntensityLux;
        public float currentSunDiskIntensity;
        public float sunSoftness;
        public final float[] currentSunLightColor = new float[4];
        public final float[] currentSunDiskColor = new float[4];
        public String formulaStatus = "NOT_EVALUATED";
    }

    private SolumUdsSunValues() { }

    public static void evaluate(Inputs in, Output out) {
        if (in == null || out == null) throw new IllegalArgumentException("uds_sun_values_arguments_missing");
        double sunZ = finite(in.cachedSunVectorZ, 0.0);
        double eclipse = clamp(finite(in.eclipsePercent, 1.0), 0.0, 1.0);
        double sunHeight = sunZ * -0.5 + 0.5;
        double twilight = lerp(finite(in.lightingBrightnessDay, 1.0),
            finite(in.lightingBrightnessDawnDusk, 1.0),
            mapRangeClamped(sunHeight, 0.505, 0.635, 1.0, 0.0));
        double nightAlpha = mapRangeClamped(sunHeight, 0.5, 0.466, 1.0 - eclipse, 1.0);
        double sceneBrightness = lerp(twilight, finite(in.lightingBrightnessNight, 1.0), nightAlpha);
        double scaledBalance = sceneBrightness * finite(in.directionalBalance, 1.0);

        double sunScale = Math.toRadians(Math.max(0.0, finite(in.sunScaleDegrees, 1.2)));
        double horizonScale = finite(in.scaleSunRadiusNearHorizon, 1.0);
        double radius = horizonScale == 1.0 ? sunScale : sunScale * mapRangeClamped(
            sunZ, 0.0, -0.5, horizonScale, 1.0);

        double baseLightIntensity = Math.max(0.0, finite(in.sunLightIntensityLux, 5.0));
        double currentLight;
        if (in.usingSpaceMode) {
            currentLight = baseLightIntensity * eclipse;
        } else {
            double unscaled = in.usingSkyAtmosphere
                ? mapRangeClamped(sunZ, 0.157, 0.113, 0.0, baseLightIntensity)
                : mapRangeClamped(sunZ, 0.0, 0.15, baseLightIntensity, 0.0)
                    * DIRECTIONAL_INTENSITY.evaluate(sunZ);
            double cap = Math.min(safeDivide(5.0, baseLightIntensity), 1.0);
            double alpha = Math.pow(mapRangeClamped(sunZ, 0.0, 0.11, 1.0, 0.0), 8.0);
            double adjustBase = in.fadeDownHighSunLightIntensityBelowHorizon
                ? lerp(cap, 1.0, alpha) : 1.0;
            double interior = lerp(1.0, finite(in.sunLightIntensityMultiplierInInteriors, 1.0),
                finite(in.cachedInvertedGlobalOcclusion, 0.0));
            currentLight = unscaled * scaledBalance * eclipse
                * finite(in.cachedDirectionalLightDimming, 1.0)
                * finite(in.cachedDirectionalInscatteringMultiplier, 1.0)
                * interior * adjustBase;
        }

        color(in.sunLightColor, out.currentSunLightColor);
        if (!in.usingSkyAtmosphere && !in.usingSpaceMode) {
            for (int channel = 0; channel < 4; channel++) {
                out.currentSunLightColor[channel] *= SUN_LIGHT_COLOR[channel].evaluate(sunHeight);
            }
        }
        double saturation = Math.max(0.0, finite(in.saturation, 1.0));
        if (saturation != 1.0) desaturate(out.currentSunLightColor, 1.0 - saturation);
        else multiplyColor(out.currentSunLightColor, in.cachedSolarEclipseTint);

        double coverageA = in.applyFlatCloudiness ? 1.0 : 1.5;
        double coverageB = in.applyFlatCloudiness ? 1.8 : 2.4;
        double coverageOutB = in.applyFlatCloudiness ? 0.0
            : (in.cloudPaintCanSubtractCoverage ? 0.7 : 0.0);
        double cloud = Math.pow(mapRangeClamped(finite(in.localCloudCoverage, 0.0),
            coverageA, coverageB, 1.0, coverageOutB), 2.0);
        double fog = mapRangeClamped(finite(in.fog, 1.0), 6.0, 9.0, 1.0, 0.0);
        double currentDiskIntensity = Math.max(0.0, finite(in.sunDiskIntensity, 4.0))
            * 43.010753 * cloud * scaledBalance * baseLightIntensity * fog;

        if (in.usingSpaceMode) {
            double space = Math.max(0.0, finite(in.sunDiskIntensity, 4.0))
                * baseLightIntensity * 43.0;
            for (int channel = 0; channel < 4; channel++) out.currentSunDiskColor[channel] = (float)space;
        } else {
            double curveInput = lerp(0.5, sunHeight, eclipse);
            for (int channel = 0; channel < 4; channel++) {
                out.currentSunDiskColor[channel] = SUN_DISK_COLOR[channel].evaluate(curveInput)
                    * (float)currentDiskIntensity;
            }
        }
        double eclipseEdge = clamp(eclipse * 80.0, 0.0, 1.0)
            * (finite(in.sunSoftness, 3.8) / 4.0);
        for (int channel = 0; channel < 4; channel++) {
            out.currentSunDiskColor[channel] *= out.currentSunLightColor[channel]
                * finiteColor(in.cachedSolarEclipseTint[channel], 1.0f)
                * (float)eclipseEdge * finiteColor(in.sunDiskTint[channel], 1.0f);
        }

        out.sunHeight = (float)sunHeight;
        out.currentSceneLightingBrightnessScale = (float)sceneBrightness;
        out.scaledDirectionalBalance = (float)scaledBalance;
        out.currentSunRadiusRadians = (float)radius;
        out.currentSunLightIntensityLux = finiteFloat(currentLight);
        out.currentSunDiskIntensity = finiteFloat(currentDiskIntensity);
        out.sunSoftness = (float)finite(in.sunSoftness, 3.8);
        out.formulaStatus = CONTRACT_STATUS;
    }

    public static float directionalIntensityCurve(double input) {
        return DIRECTIONAL_INTENSITY.evaluate(input);
    }

    public static void sunDiskColorCurve(double input, float[] out) {
        requireColor(out);
        for (int channel = 0; channel < 4; channel++) out[channel] = SUN_DISK_COLOR[channel].evaluate(input);
    }

    public static void sunLightColorCurve(double input, float[] out) {
        requireColor(out);
        for (int channel = 0; channel < 4; channel++) out[channel] = SUN_LIGHT_COLOR[channel].evaluate(input);
    }

    private static SolumUdsRichCurve curve(SolumUdsRichCurve.Key... keys) {
        return new SolumUdsRichCurve(keys);
    }

    private static SolumUdsRichCurve.Key key(double time, double value, double arrive,
                                              double leave, int interpolation) {
        return new SolumUdsRichCurve.Key(time, value, arrive, leave, interpolation);
    }

    private static void color(float[] source, float[] out) {
        requireColor(source); requireColor(out);
        for (int channel = 0; channel < 4; channel++) out[channel] = finiteColor(source[channel], 1.0f);
    }

    private static void multiplyColor(float[] out, float[] multiplier) {
        requireColor(multiplier);
        for (int channel = 0; channel < 4; channel++) out[channel] *= finiteColor(multiplier[channel], 1.0f);
    }

    private static void desaturate(float[] color, double amount) {
        // UE FLinearColor::GetLuminance / Desaturate weights.
        float luminance = color[0] * 0.3f + color[1] * 0.59f + color[2] * 0.11f;
        for (int channel = 0; channel < 3; channel++) {
            color[channel] = (float)lerp(color[channel], luminance, amount);
        }
    }

    private static void requireColor(float[] color) {
        if (color == null || color.length < 4) throw new IllegalArgumentException("uds_sun_color_invalid");
    }

    private static float finiteColor(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float finiteFloat(double value) {
        if (!Double.isFinite(value)) return 0.0f;
        if (value > Float.MAX_VALUE) return Float.MAX_VALUE;
        if (value < -Float.MAX_VALUE) return -Float.MAX_VALUE;
        return (float)value;
    }

    private static double safeDivide(double numerator, double denominator) {
        return Math.abs(denominator) <= 1.0e-8 ? 0.0 : numerator / denominator;
    }

    private static double mapRangeClamped(double value, double inA, double inB,
                                           double outA, double outB) {
        if (inA == inB) return outA;
        return lerp(outA, outB, clamp((value - inA) / (inB - inA), 0.0, 1.0));
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
