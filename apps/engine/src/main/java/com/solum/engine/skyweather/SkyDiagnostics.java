package com.solum.engine.skyweather;

public class SkyDiagnostics {
    private String skySystemStatus = "created_not_applied";
    private String sunVisualStatus = "renderer_light_only_no_screen_overlay";
    private String moonVisualStatus = "placeholder_disabled_no_screen_overlay";
    private String starsStatus = "intensity_state_only_no_texture";
    private String cloudVisualStatus = "weather_state_only_no_volumetric_clouds";
    private String fallbackStatus = "procedural_gradient_fallback_available";
    private String privateAssetsStatus = "private_assets_disabled";
    private String paidAssetsTrackedStatus = "false_static_gate_required";
    private String generatedAssetStatus = "none";

    public String getSkySystemStatus() { return skySystemStatus; }
    public void setSkySystemStatus(String value) { skySystemStatus = safe(value); }
    public String getSunVisualStatus() { return sunVisualStatus; }
    public void setSunVisualStatus(String value) { sunVisualStatus = safe(value); }
    public String getMoonVisualStatus() { return moonVisualStatus; }
    public void setMoonVisualStatus(String value) { moonVisualStatus = safe(value); }
    public String getStarsStatus() { return starsStatus; }
    public void setStarsStatus(String value) { starsStatus = safe(value); }
    public String getCloudVisualStatus() { return cloudVisualStatus; }
    public void setCloudVisualStatus(String value) { cloudVisualStatus = safe(value); }
    public String getFallbackStatus() { return fallbackStatus; }
    public void setFallbackStatus(String value) { fallbackStatus = safe(value); }
    public String getPrivateAssetsStatus() { return privateAssetsStatus; }
    public void setPrivateAssetsStatus(String value) { privateAssetsStatus = safe(value); }
    public String getPaidAssetsTrackedStatus() { return paidAssetsTrackedStatus; }
    public void setPaidAssetsTrackedStatus(String value) { paidAssetsTrackedStatus = safe(value); }
    public String getGeneratedAssetStatus() { return generatedAssetStatus; }
    public void setGeneratedAssetStatus(String value) { generatedAssetStatus = safe(value); }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
