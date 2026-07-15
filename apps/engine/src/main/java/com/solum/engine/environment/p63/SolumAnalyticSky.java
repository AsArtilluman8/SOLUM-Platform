package com.solum.engine.environment.p63;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** CPU-authored low-frequency cubemap for the mobile Filament skybox. */
public final class SolumAnalyticSky {
    public static final int CUBEMAP_SIZE = 64;
    public static final int FACE_COUNT = 6;

    private SolumAnalyticSky() { }

    public static ByteBuffer createSrgbCubemap(float[] sunDirection) {
        int faceBytes = CUBEMAP_SIZE * CUBEMAP_SIZE * 4;
        ByteBuffer pixels = ByteBuffer.allocateDirect(faceBytes * FACE_COUNT).order(ByteOrder.nativeOrder());
        float[] direction = new float[3];
        float[] linear = new float[3];
        for (int face = 0; face < FACE_COUNT; face++) {
            for (int y = 0; y < CUBEMAP_SIZE; y++) {
                float v = ((y + 0.5f) / CUBEMAP_SIZE) * 2.0f - 1.0f;
                for (int x = 0; x < CUBEMAP_SIZE; x++) {
                    float u = ((x + 0.5f) / CUBEMAP_SIZE) * 2.0f - 1.0f;
                    cubemapDirection(direction, face, u, v);
                    linearColor(direction, sunDirection, linear);
                    pixels.put(toSrgb8(linear[0]));
                    pixels.put(toSrgb8(linear[1]));
                    pixels.put(toSrgb8(linear[2]));
                    pixels.put((byte)255);
                }
            }
        }
        pixels.flip();
        return pixels;
    }

    public static int[] faceOffsets() {
        int faceBytes = CUBEMAP_SIZE * CUBEMAP_SIZE * 4;
        return new int[] {0, faceBytes, faceBytes * 2, faceBytes * 3, faceBytes * 4, faceBytes * 5};
    }

    public static void linearColor(float[] viewDirection, float[] sunDirection, float[] out) {
        float viewY = clamp(viewDirection[1], -1.0f, 1.0f);
        float sunY = clamp(sunDirection[1], -1.0f, 1.0f);
        float day = smoothStep(-0.10f, 0.16f, sunY);
        float twilight = smoothStep(-0.32f, 0.03f, sunY) * (1.0f - smoothStep(0.02f, 0.32f, sunY));
        float night = 1.0f - day;
        float horizon = (float)Math.exp(-Math.abs(viewY) * 6.5f);
        float upper = smoothStep(-0.04f, 0.88f, viewY);
        float lower = smoothStep(0.0f, 1.0f, -viewY);

        float[] zenithDay = {0.035f, 0.19f, 0.62f};
        float[] horizonDay = {0.48f, 0.66f, 0.88f};
        float[] zenithNight = {0.0035f, 0.008f, 0.030f};
        float[] horizonNight = {0.020f, 0.035f, 0.075f};
        float[] nadirDay = {0.055f, 0.095f, 0.155f};
        float[] nadirNight = {0.0025f, 0.005f, 0.016f};

        float mu = clamp(dot(viewDirection, sunDirection), -1.0f, 1.0f);
        float rayleighPhase = 0.75f * (1.0f + mu * mu);
        float g = 0.76f;
        float miePhase = (1.0f - g * g)
            / (float)Math.pow(Math.max(0.035f, 1.0f + g * g - 2.0f * g * mu), 1.5);
        float sunGlow = (float)Math.exp((mu - 1.0f) * 34.0f) * smoothStep(-0.10f, 0.04f, sunY);
        float twilightFacing = (float)Math.pow(Math.max(0.0f, mu), 4.0f) * twilight * horizon;

        for (int channel = 0; channel < 3; channel++) {
            float zenith = lerp(zenithNight[channel], zenithDay[channel], day);
            float horizonColor = lerp(horizonNight[channel], horizonDay[channel], day);
            float nadir = lerp(nadirNight[channel], nadirDay[channel], day);
            float upperAtmosphere = lerp(horizonColor, zenith, upper);
            float lowerAtmosphere = lerp(horizonColor, nadir, lower);
            float base = viewY >= 0.0f ? upperAtmosphere : lowerAtmosphere;
            float rayleighTint = channel == 2 ? 1.0f : (channel == 1 ? 0.64f : 0.34f);
            float mieTint = channel == 0 ? 1.0f : (channel == 1 ? 0.72f : 0.42f);
            float scatter = rayleighPhase * rayleighTint * (0.014f + day * 0.022f)
                + miePhase * mieTint * (0.0018f + twilight * 0.006f)
                + sunGlow * mieTint * 0.34f
                + twilightFacing * mieTint * 0.28f;
            out[channel] = clamp(base + scatter + horizon * horizonColor * 0.055f, 0.0015f, 1.0f);
        }
        if (night > 0.98f) {
            out[0] = Math.max(out[0], 0.0025f);
            out[1] = Math.max(out[1], 0.0050f);
            out[2] = Math.max(out[2], 0.0160f);
        }
    }

    private static void cubemapDirection(float[] out, int face, float u, float v) {
        switch (face) {
            case 0: set(out, 1.0f, -v, -u); break;   // +X
            case 1: set(out, -1.0f, -v, u); break;  // -X
            case 2: set(out, u, 1.0f, v); break;    // +Y
            case 3: set(out, u, -1.0f, -v); break;  // -Y
            case 4: set(out, u, -v, 1.0f); break;   // +Z
            default: set(out, -u, -v, -1.0f); break;// -Z
        }
    }

    private static void set(float[] out, float x, float y, float z) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        out[0] = x / length; out[1] = y / length; out[2] = z / length;
    }

    private static byte toSrgb8(float linear) {
        float srgb = linear <= 0.0031308f ? linear * 12.92f
            : 1.055f * (float)Math.pow(linear, 1.0 / 2.4) - 0.055f;
        return (byte)Math.round(clamp(srgb, 0.0f, 1.0f) * 255.0f);
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float smoothStep(float a, float b, float value) {
        float t = clamp((value - a) / Math.max(0.0001f, b - a), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
