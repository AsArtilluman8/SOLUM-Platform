package com.solum.engine.environment.p63;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SolumEnvironmentPackage {
    public String schema = "solum.environment.runtime";
    public int schemaVersion = 1;
    public String packageId = "solum.native-environment.p63";
    public int deterministicSeed = 1597463007;
    public float initialTime = 960.0f;
    public float dawn = 600.0f;
    public float dusk = 1800.0f;
    public float previewDaySeconds = 240.0f;
    public float sunIntensity = 18.0f;
    public float sunDiskIntensity = 4.0f;
    public float moonIntensity = 0.15f;
    public float moonPhase = 0.62f;
    public float moonScale = 0.95f;
    public float starsIntensity = 0.75f;
    public float lightningFrequency = 14.0f;
    public float lightningSpawnPeriod = 2.0f;
    public float lightningDurationMin = 1.75f;
    public float lightningDurationMax = 2.2f;
    public float lightningLightIntensity = 5.0f;
    public float thunderDelayPerKm = 0.15f;
    public float wetCoverageSeconds = 20.0f;
    public float drySeconds = 90.0f;
    public float puddleCoverage = 0.28f;
    public float waterRoughness = 0.04f;
    private final List<SolumWeatherState> presets = new ArrayList<>();
    private final Map<String, SolumEnvironmentQuality> qualities = new LinkedHashMap<>();

    public List<SolumWeatherState> getPresets() { return Collections.unmodifiableList(presets); }
    public Map<String, SolumEnvironmentQuality> getQualities() { return Collections.unmodifiableMap(qualities); }
    public void addPreset(SolumWeatherState preset) { if (preset != null) presets.add(preset); }
    public void addQuality(SolumEnvironmentQuality quality) { if (quality != null) qualities.put(quality.name, quality); }

    public SolumWeatherState findPreset(String id) {
        if (id != null) for (SolumWeatherState preset : presets) if (id.equals(preset.id)) return preset;
        return presets.isEmpty() ? null : presets.get(0);
    }

    public SolumEnvironmentQuality findQuality(String name) {
        SolumEnvironmentQuality quality = qualities.get(name);
        if (quality == null) quality = qualities.get("Medium");
        return quality == null ? new SolumEnvironmentQuality("Medium", 1100, 8, 128, 0.86f) : quality;
    }

    public void validate() {
        if (!"solum.environment.runtime".equals(schema)) throw new IllegalArgumentException("environment_schema_mismatch");
        if (schemaVersion != 1) throw new IllegalArgumentException("environment_schema_version_unsupported");
        if (presets.size() != 13) throw new IllegalArgumentException("expected_13_weather_presets_actual_" + presets.size());
        if (qualities.size() < 4) throw new IllegalArgumentException("quality_tiers_missing");
        for (SolumWeatherState preset : presets) {
            if (preset.id == null || preset.id.trim().isEmpty()) throw new IllegalArgumentException("weather_preset_id_missing");
        }
    }
}
