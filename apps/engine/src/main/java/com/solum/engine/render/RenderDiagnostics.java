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
    private String ownershipSummary = "not_built";
    private String costCauseSummary = "estimated_cost not_runtime_measured needs_cost_probe_later";
    private String mobileSafetyStatus = "mobile_safe_unknown";
    private String primaryFrameMs = "waiting_for_samples";
    private String primaryFps = "waiting_for_samples";
    private String primaryFpsSource = "waiting_for_samples";
    private String javaCallbackFps = "debug_only_waiting_for_samples";
    private String frameMetricsTotalMs = "unavailable";
    private String frameMetricsGpuMs = "gpu_timing_unavailable_frame_metrics_only";
    private String frameMetricsDrawMs = "unavailable";
    private String frameMetricsSwapMs = "unavailable";
    private String p50FrameMs = "waiting_for_samples";
    private String p95FrameMs = "waiting_for_samples";
    private String worstFrameMs = "waiting_for_samples";
    private String jitterMs = "waiting_for_samples";
    private String fpsStability = "waiting_for_samples";
    private String fpsConfidence = "waiting_for_samples";
    private boolean timingDisagreement = false;
    private String timingDisagreementReason = "waiting_for_samples";
    private String fogRequested = "off";
    private String fogAppliedStatus = "activity_local";
    private String fogVisibilityConfidence = "not_verified";
    private String fogWarning = "not_verified";
    private final List<String> notExposedOrNotVerifiedItems = new ArrayList<>();
    private final List<String> enabledExpensiveFeatures = new ArrayList<>();
    private final List<String> recommendedActions = new ArrayList<>();

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
    public String getOwnershipSummary() { return ownershipSummary; }
    public void setOwnershipSummary(String value) { ownershipSummary = safe(value); }
    public String getCostCauseSummary() { return costCauseSummary; }
    public void setCostCauseSummary(String value) { costCauseSummary = safe(value); }
    public String getMobileSafetyStatus() { return mobileSafetyStatus; }
    public void setMobileSafetyStatus(String value) { mobileSafetyStatus = safe(value); }
    public String getPrimaryFrameMs() { return primaryFrameMs; }
    public String getPrimaryFps() { return primaryFps; }
    public String getPrimaryFpsSource() { return primaryFpsSource; }
    public String getJavaCallbackFps() { return javaCallbackFps; }
    public String getFrameMetricsTotalMs() { return frameMetricsTotalMs; }
    public String getFrameMetricsGpuMs() { return frameMetricsGpuMs; }
    public String getFrameMetricsDrawMs() { return frameMetricsDrawMs; }
    public String getFrameMetricsSwapMs() { return frameMetricsSwapMs; }
    public String getP50FrameMs() { return p50FrameMs; }
    public String getP95FrameMs() { return p95FrameMs; }
    public String getWorstFrameMs() { return worstFrameMs; }
    public String getJitterMs() { return jitterMs; }
    public String getFpsStability() { return fpsStability; }
    public String getFpsConfidence() { return fpsConfidence; }
    public boolean isTimingDisagreement() { return timingDisagreement; }
    public String getTimingDisagreementReason() { return timingDisagreementReason; }
    public String getFogRequested() { return fogRequested; }
    public String getFogAppliedStatus() { return fogAppliedStatus; }
    public String getFogVisibilityConfidence() { return fogVisibilityConfidence; }
    public String getFogWarning() { return fogWarning; }
    public void setFrameTiming(String primaryFrameMs, String primaryFps, String primaryFpsSource,
            String javaCallbackFps, String frameMetricsTotalMs, String frameMetricsGpuMs,
            String frameMetricsDrawMs, String frameMetricsSwapMs, String p50FrameMs,
            String p95FrameMs, String worstFrameMs, String jitterMs, String fpsStability,
            String fpsConfidence, boolean timingDisagreement, String timingDisagreementReason) {
        this.primaryFrameMs = safe(primaryFrameMs);
        this.primaryFps = safe(primaryFps);
        this.primaryFpsSource = safe(primaryFpsSource);
        this.javaCallbackFps = safe(javaCallbackFps);
        this.frameMetricsTotalMs = safe(frameMetricsTotalMs);
        this.frameMetricsGpuMs = safe(frameMetricsGpuMs);
        this.frameMetricsDrawMs = safe(frameMetricsDrawMs);
        this.frameMetricsSwapMs = safe(frameMetricsSwapMs);
        this.p50FrameMs = safe(p50FrameMs);
        this.p95FrameMs = safe(p95FrameMs);
        this.worstFrameMs = safe(worstFrameMs);
        this.jitterMs = safe(jitterMs);
        this.fpsStability = safe(fpsStability);
        this.fpsConfidence = safe(fpsConfidence);
        this.timingDisagreement = timingDisagreement;
        this.timingDisagreementReason = safe(timingDisagreementReason);
    }
    public void setFogDiagnostics(String requested, String appliedStatus, String visibilityConfidence, String warning) {
        fogRequested = safe(requested);
        fogAppliedStatus = safe(appliedStatus);
        fogVisibilityConfidence = safe(visibilityConfidence);
        fogWarning = safe(warning);
    }
    public void setCostDiagnostics(RenderCostDiagnostics cost) {
        enabledExpensiveFeatures.clear();
        recommendedActions.clear();
        if (cost == null) {
            costCauseSummary = "estimated_cost unknown not_runtime_measured needs_cost_probe_later";
            mobileSafetyStatus = "mobile_safe_unknown";
            return;
        }
        costCauseSummary = safe(cost.getCostCauseSummary());
        mobileSafetyStatus = safe(cost.getMobileSafetyStatus());
        enabledExpensiveFeatures.addAll(cost.getEnabledExpensiveFeatures());
        recommendedActions.addAll(cost.getRecommendedActions());
    }
    public List<String> getEnabledExpensiveFeatures() {
        return Collections.unmodifiableList(enabledExpensiveFeatures);
    }
    public List<String> getRecommendedActions() {
        return Collections.unmodifiableList(recommendedActions);
    }
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
