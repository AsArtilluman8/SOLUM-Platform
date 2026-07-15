package com.solum.engine.environment.p63;

public final class SolumLightningState {
    public boolean enabled;
    public boolean active;
    public float flash;
    public float distanceKm;
    public float strikeX;
    public float strikeZ;
    public float thunderDelaySeconds;
    public long eventIndex;
    public final float[] color = {0.495f, 0.613f, 1.0f};
    public float lightIntensity = 5.0f;
}
