package com.solum.engine.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnvironmentDiagnostics {
    private String environmentTruthText = "environment_api_created_not_applied";
    private String environmentOwnershipSummary = "EnvironmentApi owns time/sun/moon/stars/slots; RenderControlApi owns render quality/postfx";
    private String timeOfDayStatus = "not_applied";
    private String sunStatus = "not_applied";
    private String moonStatus = "placeholder_not_rendered";
    private String iblSlotStatus = "slot_ready_asset_missing";
    private String skyboxSlotStatus = "slot_ready_asset_missing";
    private String starsStatus = "placeholder_not_rendered_planned_p52_assets";
    private String cloudStatus = "placeholder_only_no_weather_no_volumetric_clouds";
    private String sunOcclusionStatus = "clear_no_cloud_attenuation";
    private String cloudShadowStatus = "planned_projected_mask_not_active";
    private String precipitationStatus = "none";
    private String volumetricCloudsStatus = "not_implemented_mobile_future";
    private String timeBlendPhase = "noon";
    private float sunCurveT = 1.0f;
    private float nightCurveT = 0.0f;
    private float starsCurveT = 0.0f;
    private String smoothBlendStatus = "skyboxBlendStatus=discrete_preset_switch_light_blend_smooth";
    private String skyboxBlendStatus = "discrete_preset_switch_light_blend_smooth";
    private String sunDiskStatus = "not_applied";
    private String moonDiskStatus = "placeholder_not_applied";
    private String fallbackStatus = "fallback_allowed_neutral";
    private String mobileSafetyStatus = "lightweight_state_based_no_heavy_diagnostics_per_frame";
    private String lastApplyStatus = "not_applied";
    private final List<String> notImplementedYet = new ArrayList<>();

    public String getEnvironmentTruthText() { return environmentTruthText; }
    public void setEnvironmentTruthText(String value) { environmentTruthText = safe(value); }
    public String getEnvironmentOwnershipSummary() { return environmentOwnershipSummary; }
    public void setEnvironmentOwnershipSummary(String value) { environmentOwnershipSummary = safe(value); }
    public String getTimeOfDayStatus() { return timeOfDayStatus; }
    public void setTimeOfDayStatus(String value) { timeOfDayStatus = safe(value); }
    public String getSunStatus() { return sunStatus; }
    public void setSunStatus(String value) { sunStatus = safe(value); }
    public String getMoonStatus() { return moonStatus; }
    public void setMoonStatus(String value) { moonStatus = safe(value); }
    public String getIblSlotStatus() { return iblSlotStatus; }
    public void setIblSlotStatus(String value) { iblSlotStatus = safe(value); }
    public String getSkyboxSlotStatus() { return skyboxSlotStatus; }
    public void setSkyboxSlotStatus(String value) { skyboxSlotStatus = safe(value); }
    public String getStarsStatus() { return starsStatus; }
    public void setStarsStatus(String value) { starsStatus = safe(value); }
    public String getCloudStatus() { return cloudStatus; }
    public void setCloudStatus(String value) { cloudStatus = safe(value); }
    public String getSunOcclusionStatus() { return sunOcclusionStatus; }
    public void setSunOcclusionStatus(String value) { sunOcclusionStatus = safe(value); }
    public String getCloudShadowStatus() { return cloudShadowStatus; }
    public void setCloudShadowStatus(String value) { cloudShadowStatus = safe(value); }
    public String getPrecipitationStatus() { return precipitationStatus; }
    public void setPrecipitationStatus(String value) { precipitationStatus = safe(value); }
    public String getVolumetricCloudsStatus() { return volumetricCloudsStatus; }
    public void setVolumetricCloudsStatus(String value) { volumetricCloudsStatus = safe(value); }
    public String getTimeBlendPhase() { return timeBlendPhase; }
    public void setTimeBlendPhase(String value) { timeBlendPhase = safe(value); }
    public float getSunCurveT() { return sunCurveT; }
    public void setSunCurveT(float value) { sunCurveT = EnvironmentSettings.clamp(value, 0.0f, 1.0f); }
    public float getNightCurveT() { return nightCurveT; }
    public void setNightCurveT(float value) { nightCurveT = EnvironmentSettings.clamp(value, 0.0f, 1.0f); }
    public float getStarsCurveT() { return starsCurveT; }
    public void setStarsCurveT(float value) { starsCurveT = EnvironmentSettings.clamp(value, 0.0f, 1.0f); }
    public String getSmoothBlendStatus() { return smoothBlendStatus; }
    public void setSmoothBlendStatus(String value) { smoothBlendStatus = safe(value); }
    public String getSkyboxBlendStatus() { return skyboxBlendStatus; }
    public void setSkyboxBlendStatus(String value) { skyboxBlendStatus = safe(value); }
    public String getSunDiskStatus() { return sunDiskStatus; }
    public void setSunDiskStatus(String value) { sunDiskStatus = safe(value); }
    public String getMoonDiskStatus() { return moonDiskStatus; }
    public void setMoonDiskStatus(String value) { moonDiskStatus = safe(value); }
    public String getFallbackStatus() { return fallbackStatus; }
    public void setFallbackStatus(String value) { fallbackStatus = safe(value); }
    public String getMobileSafetyStatus() { return mobileSafetyStatus; }
    public void setMobileSafetyStatus(String value) { mobileSafetyStatus = safe(value); }
    public String getLastApplyStatus() { return lastApplyStatus; }
    public void setLastApplyStatus(String value) { lastApplyStatus = safe(value); }
    public List<String> getNotImplementedYet() { return Collections.unmodifiableList(notImplementedYet); }

    public void resetNotImplementedYet() {
        notImplementedYet.clear();
    }

    public void addNotImplementedYet(String value) {
        if (value != null && !value.trim().isEmpty() && !notImplementedYet.contains(value.trim())) {
            notImplementedYet.add(value.trim());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
