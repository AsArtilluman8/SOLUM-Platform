package com.solum.engine.environment.p63;

/** Pure finite-value helpers shared by the analytic sky controller and renderer. */
public final class SolumAnalyticSkyMaterial {
    public static final String ASSET_PATH = "env/p63/analytic_sky_mobile.filamat";
    public static final float SUN_LUMINANCE_SAFETY_MAX_NITS = 1_000_000.0f;
    public static final float SUN_LUX_SAFETY_MAX = 1_000_000.0f;
    public static final float MOON_LUMINANCE_SAFETY_MAX_NITS = 100_000.0f;
    public static final float MOON_LUX_SAFETY_MAX = 100.0f;

    private SolumAnalyticSkyMaterial() { }

    public static float finite(float value, float min, float max, float fallback) {
        return SolumCelestialControlState.finiteClamp(value, min, max, fallback);
    }

    public static float qualityIndex(String quality) {
        if ("Medium".equals(quality)) return 1.0f;
        if ("High Experimental".equals(quality)) return 2.0f;
        return 0.0f;
    }

    /**
     * Builds the Moon-to-Sun direction for a continuous manual phase angle.
     * Zero degrees is full moon, 90 is quarter, and 180 is new moon.
     */
    public static void moonToSunDirection(float[] moonDirection, float phaseAngleDegrees, float[] out) {
        float mx = finite(moonDirection[0], -1.0f, 1.0f, 0.0f);
        float my = finite(moonDirection[1], -1.0f, 1.0f, 0.0f);
        float mz = finite(moonDirection[2], -1.0f, 1.0f, 1.0f);
        float length = (float)Math.sqrt(mx * mx + my * my + mz * mz);
        if (length < 1.0e-5f) { mx = 0.0f; my = 0.0f; mz = 1.0f; length = 1.0f; }
        mx /= length; my /= length; mz /= length;
        float rightX = mz;
        float rightY = 0.0f;
        float rightZ = -mx;
        float rightLength = (float)Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (rightLength < 1.0e-4f) {
            rightX = 1.0f; rightY = 0.0f; rightZ = 0.0f; rightLength = 1.0f;
        }
        rightX /= rightLength; rightY /= rightLength; rightZ /= rightLength;
        double angle = Math.toRadians(finite(phaseAngleDegrees, 0.0f, 180.0f, 68.4f));
        float cosine = (float)Math.cos(angle);
        float sine = (float)Math.sin(angle);
        out[0] = -mx * cosine + rightX * sine;
        out[1] = -my * cosine + rightY * sine;
        out[2] = -mz * cosine + rightZ * sine;
        float outLength = (float)Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
        if (outLength < 1.0e-5f || !Float.isFinite(outLength)) {
            out[0] = -mx; out[1] = -my; out[2] = -mz;
        } else {
            out[0] /= outLength; out[1] /= outLength; out[2] /= outLength;
        }
    }
}
