package com.solum.engine.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenderDiagnostics {
    private String renderTruthText = "render_api_not_applied";
    private String performanceSummary = "activity_local";
    private String timingSourceSummary = "activity_local";
    private String presetMismatchStatus = "activity_local";
    private String manualOverrideStatus = "activity_local";
    private final List<String> notExposedOrNotVerifiedItems = new ArrayList<>();

    public String getRenderTruthText() { return renderTruthText; }
    public void setRenderTruthText(String value) { renderTruthText = safe(value); }
    public String getPerformanceSummary() { return performanceSummary; }
    public void setPerformanceSummary(String value) { performanceSummary = safe(value); }
    public String getTimingSourceSummary() { return timingSourceSummary; }
    public void setTimingSourceSummary(String value) { timingSourceSummary = safe(value); }
    public String getPresetMismatchStatus() { return presetMismatchStatus; }
    public void setPresetMismatchStatus(String value) { presetMismatchStatus = safe(value); }
    public String getManualOverrideStatus() { return manualOverrideStatus; }
    public void setManualOverrideStatus(String value) { manualOverrideStatus = safe(value); }
    public List<String> getNotExposedOrNotVerifiedItems() {
        return Collections.unmodifiableList(notExposedOrNotVerifiedItems);
    }
    public void clearNotExposedOrNotVerifiedItems() { notExposedOrNotVerifiedItems.clear(); }
    public void addNotExposedOrNotVerifiedItem(String value) {
        if (value != null && !value.trim().isEmpty()) notExposedOrNotVerifiedItems.add(value.trim());
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }
}
