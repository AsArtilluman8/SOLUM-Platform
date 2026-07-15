package com.solum.engine.environment.p63;

public final class SolumEnvironmentQuality {
    public String name;
    public int particleLimit;
    public int cloudGroups;
    public int starCount;
    public float renderScale;

    public SolumEnvironmentQuality(String name, int particleLimit, int cloudGroups, int starCount, float renderScale) {
        this.name = name;
        this.particleLimit = particleLimit;
        this.cloudGroups = cloudGroups;
        this.starCount = starCount;
        this.renderScale = renderScale;
    }
}
