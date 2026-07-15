package com.solum.engine.environment.p63;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SolumEnvironmentState {
    public final SolumWeatherState weather = new SolumWeatherState();
    public final SolumAtmosphereState atmosphere = new SolumAtmosphereState();
    public final SolumCloudState clouds = new SolumCloudState();
    public final SolumFogState fog = new SolumFogState();
    public final SolumPrecipitationState precipitation = new SolumPrecipitationState();
    public final SolumWindState wind = new SolumWindState();
    public final SolumLightningState lightning = new SolumLightningState();
    public final SolumSurfaceWeatherState surface = new SolumSurfaceWeatherState();
    public final SolumEnvironmentAudioState audio = new SolumEnvironmentAudioState();
    public final SolumEnvironmentLightingState lighting = new SolumEnvironmentLightingState();
    public String quality = "Medium";
    public String requestedPreset = "Partly_Cloudy";
    public String activePreset = "Partly_Cloudy";
    public boolean weatherTransitionActive;
    public float weatherTransitionAlpha = 1.0f;
    public float weatherTransitionDuration = 4.0f;
    public float timeOfDay = 960.0f;
    public boolean cameraInside;
    public boolean cameraUnderRoof;
    public long frameRevision;
    public String packageStatus = "not_loaded";
    public String adapterStatus = "not_bound";
    public String stageStatus = "not_loaded";
    private final Map<String, EnvironmentFeatureStatus> featureStatus = new LinkedHashMap<>();

    public SolumEnvironmentState() {
        featureStatus.put("shared_core", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("13_presets", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("time_of_day", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("sun_directional_light", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("moon_directional_light", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("sun_disk", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("moon_phase_material", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("world_space_stars", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("atmosphere", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("clouds", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("fog", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("world_space_rain", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("world_space_snow", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("wind", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("lightning_transient_light", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("thunder", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("wetness", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("puddles", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("snow_cover", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("ice", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("prepared_ibl_reflections", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("ibl_transition", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("sky_weather_pbr_reflections", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("environment_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("rain_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("wind_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("snow_blizzard_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("sand_dust_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("thunder_audio", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("audio_crossfade", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("interior_audio_attenuation", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("sand_dust_world_space", EnvironmentFeatureStatus.PROTOTYPE);
        featureStatus.put("interior_exclusion", EnvironmentFeatureStatus.FUNCTIONAL);
        featureStatus.put("roof_occlusion_mask", EnvironmentFeatureStatus.FUNCTIONAL);
    }

    public Map<String, EnvironmentFeatureStatus> getFeatureStatus() { return Collections.unmodifiableMap(featureStatus); }
    public void setFeatureStatus(String key, EnvironmentFeatureStatus status) { if (key != null && status != null) featureStatus.put(key, status); }
}
