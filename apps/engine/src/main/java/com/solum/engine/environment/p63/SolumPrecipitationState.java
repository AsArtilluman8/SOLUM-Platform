package com.solum.engine.environment.p63;

public final class SolumPrecipitationState {
    public float rain;
    public float snow;
    public float dust;
    public int rainParticles;
    public int snowParticles;
    public int dustParticles;
    public int particleLimit = 1100;
    public float windTiltX;
    public float windTiltZ;
    public boolean worldSpace = true;
    public boolean screenSpaceBranch = false;
    public int blockedCells;
    public int exposedCells;
    public String occlusionStatus = "explicit_volumes_plus_roof_mask";
}
