package com.solum.engine.environment.p63;

public final class SolumCelestialSystem {
    private float dawn = 600.0f;
    private float dusk = 1800.0f;
    private float sunIntensity = 18.0f;
    private float sunDiskIntensity = 4.0f;
    private float moonIntensity = 0.15f;
    private float moonPhase = 0.62f;
    private float starsIntensity = 0.75f;

    public void configure(float dawn, float dusk, float sunIntensity, float sunDiskIntensity, float moonIntensity, float moonPhase, float starsIntensity) {
        this.dawn = dawn; this.dusk = dusk; this.sunIntensity = sunIntensity; this.sunDiskIntensity = sunDiskIntensity;
        this.moonIntensity = moonIntensity; this.moonPhase = moonPhase; this.starsIntensity = starsIntensity;
    }

    public void update(float time, SolumWeatherState weather, SolumEnvironmentLightingState out) {
        float span = Math.max(1.0f, dusk - dawn);
        double angle = (SolumTimeSystem.wrap(time) - dawn) / span * Math.PI;
        float sx = (float) Math.cos(angle) * 0.9f;
        float sy = (float) Math.sin(angle);
        float sz = (float) Math.sin(angle * 0.71) * 0.32f;
        normalize(out.sunDirection, sx, -sy, sz);
        normalize(out.moonDirection, -sx, sy, -sz);
        out.sunElevation = sy;
        out.moonElevation = -sy;
        float day = smooth(-0.08f, 0.16f, sy);
        float night = 1.0f - day;
        float twilight = (1.0f - smooth(0.05f, 0.34f, Math.abs(sy))) * smooth(-0.22f, 0.02f, sy);
        out.sunLux = day * sunIntensity * weather.lightingScale;
        out.moonLux = night * moonIntensity * weather.lightingScale;
        out.sunDiskBrightness = day * sunDiskIntensity * (1.0f - weather.cloudCoverage * 0.72f);
        out.moonDiskBrightness = night * (0.42f + moonPhase * 0.58f) * (1.0f - weather.cloudCoverage * 0.76f);
        out.moonPhase = moonPhase;
        out.starVisibility = night * starsIntensity * (1.0f - weather.cloudCoverage * 0.82f) * (1.0f - weather.fogDensity * 5.0f);
        out.ambientIntensity = weather.ambientScale * (0.22f + day * 0.98f);
        out.exposure = weather.exposure;
        float warm = Math.max(twilight, 1.0f - smooth(0.02f, 0.35f, sy));
        out.sunColor[0] = 1.0f; out.sunColor[1] = 0.96f - warm * 0.18f; out.sunColor[2] = 0.90f - warm * 0.34f;
    }

    private static float smooth(float a, float b, float value) {
        float t = Math.max(0.0f, Math.min(1.0f, (value - a) / Math.max(0.0001f, b - a)));
        return t * t * (3.0f - 2.0f * t);
    }

    private static void normalize(float[] out, float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length < 0.0001f) length = 1.0f;
        out[0] = x / length; out[1] = y / length; out[2] = z / length;
    }
}
