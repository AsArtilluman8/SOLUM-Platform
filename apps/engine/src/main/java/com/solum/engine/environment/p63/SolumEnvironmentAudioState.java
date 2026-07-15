package com.solum.engine.environment.p63;

public final class SolumEnvironmentAudioState {
    public float masterVolume = 0.45f;
    public boolean muted;
    public float rainGain;
    public float windGain;
    public float snowGain;
    public float sandGain;
    public float interiorAttenuation = 1.0f;
    public float lowPassMix;
    public float thunderPendingSeconds = -1.0f;
    public String activeProfile = "calm";
    public String provenance = "SOLUM_NATIVE procedural loops; optional P62B verified hit WAVs";
}
