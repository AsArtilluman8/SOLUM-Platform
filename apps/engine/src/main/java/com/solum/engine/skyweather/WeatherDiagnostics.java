package com.solum.engine.skyweather;

public class WeatherDiagnostics {
    private String weatherSystemStatus = "created_not_applied";
    private String cloudVisualStatus = "state_only_no_heavy_volumetrics";
    private String rainStatus = "off_particle_slot_placeholder";
    private String snowStatus = "off_particle_slot_placeholder";
    private String fogHazeStatus = "parameter_drives_filament_fog_when_applied";
    private String cloudShadowMaskStatus = "planned_not_implemented";
    private String wetnessStatus = "material_parameter_placeholder";
    private String privateAssetsStatus = "private_assets_disabled";

    public String getWeatherSystemStatus() { return weatherSystemStatus; }
    public void setWeatherSystemStatus(String value) { weatherSystemStatus = safe(value); }
    public String getCloudVisualStatus() { return cloudVisualStatus; }
    public void setCloudVisualStatus(String value) { cloudVisualStatus = safe(value); }
    public String getRainStatus() { return rainStatus; }
    public void setRainStatus(String value) { rainStatus = safe(value); }
    public String getSnowStatus() { return snowStatus; }
    public void setSnowStatus(String value) { snowStatus = safe(value); }
    public String getFogHazeStatus() { return fogHazeStatus; }
    public void setFogHazeStatus(String value) { fogHazeStatus = safe(value); }
    public String getCloudShadowMaskStatus() { return cloudShadowMaskStatus; }
    public void setCloudShadowMaskStatus(String value) { cloudShadowMaskStatus = safe(value); }
    public String getWetnessStatus() { return wetnessStatus; }
    public void setWetnessStatus(String value) { wetnessStatus = safe(value); }
    public String getPrivateAssetsStatus() { return privateAssetsStatus; }
    public void setPrivateAssetsStatus(String value) { privateAssetsStatus = safe(value); }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
