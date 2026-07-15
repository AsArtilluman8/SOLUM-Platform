package com.solum.engine.environment.p63;

public final class SolumAtmosphereState {
    public float rayleigh = 1.0f;
    public float mie = 0.05f;
    public float mieAnisotropy = 0.76f;
    public float ozone = 0.3f;
    public float horizonScattering;
    public float twilight;
    public final float[] skyColor = {0.22f, 0.46f, 0.82f};
    public final float[] horizonColor = {0.62f, 0.72f, 0.86f};
}
