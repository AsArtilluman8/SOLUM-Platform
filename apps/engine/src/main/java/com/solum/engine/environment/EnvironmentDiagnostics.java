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
