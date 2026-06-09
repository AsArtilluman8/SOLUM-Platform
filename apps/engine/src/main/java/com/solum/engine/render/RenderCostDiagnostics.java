package com.solum.engine.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenderCostDiagnostics {
    private final String costCauseSummary;
    private final List<String> enabledExpensiveFeatures;
    private final List<String> recommendedActions;
    private final String mobileSafetyStatus;

    public RenderCostDiagnostics(String costCauseSummary, List<String> enabledExpensiveFeatures,
            List<String> recommendedActions, String mobileSafetyStatus) {
        this.costCauseSummary = safe(costCauseSummary, "estimated_cost none not_runtime_measured needs_cost_probe_later");
        this.enabledExpensiveFeatures = Collections.unmodifiableList(new ArrayList<>(enabledExpensiveFeatures));
        this.recommendedActions = Collections.unmodifiableList(new ArrayList<>(recommendedActions));
        this.mobileSafetyStatus = safe(mobileSafetyStatus, "mobile_safe_unknown");
    }

    public String getCostCauseSummary() { return costCauseSummary; }
    public List<String> getEnabledExpensiveFeatures() { return enabledExpensiveFeatures; }
    public List<String> getRecommendedActions() { return recommendedActions; }
    public String getMobileSafetyStatus() { return mobileSafetyStatus; }

    public static RenderCostDiagnostics fromSettings(RenderSettings settings) {
        List<String> expensive = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        String safety = "mobile_safe_medium";
        if (settings == null) {
            actions.add("settings_missing");
            return new RenderCostDiagnostics("estimated_cost unknown settings_missing not_runtime_measured needs_cost_probe_later", expensive, actions, "mobile_safe_unknown");
        }
        if (settings.isSsr()) {
            expensive.add("SSR=high_to_extreme_estimated_cost_gameplay_unsafe_on_mali_until_proven");
            actions.add("disable_ssr_for_gameplay_or_run_future_cost_probe");
            safety = "mobile_safe_low";
        }
        if (settings.getMsaaSampleCount() >= 4) {
            expensive.add("MSAA_4x=high_estimated_cost");
            actions.add("try_msaa_2x_or_fxaa");
            safety = lowerSafety(safety, "mobile_safe_low");
        }
        String ao = settings.getAoMode();
        if ("STRONG".equalsIgnoreCase(ao) || "DEBUG_MAX".equalsIgnoreCase(ao)) {
            expensive.add("AO_" + ao + "=medium_to_high_estimated_cost");
            actions.add("use_ao_soft_or_medium_on_phone");
        }
        if ("HIGH".equalsIgnoreCase(settings.getBloomMode())) {
            expensive.add("Bloom_High=medium_to_high_estimated_cost");
            actions.add("use_bloom_soft_or_medium");
        }
        if (settings.isTaa()) {
            expensive.add("TAA=medium_unknown_estimated_cost_not_free");
            actions.add("compare_taa_off_with_frame_ms");
        }
        if (settings.getRenderScale() >= 1.0f) {
            expensive.add("RenderScale_1_0=high_estimated_cost_on_high_res_phone");
            actions.add("enable_dynamic_resolution_or_lower_render_scale");
        }
        if (!settings.isDynamicResolution()) {
            expensive.add("DynamicResolution_off=warning_high_resolution_risk");
            actions.add("enable_dynamic_resolution_for_phone_preview");
        }
        if (settings.getAmbientIntensity() > 5.0f || settings.getBackgroundIntensity() > 0.7f) {
            expensive.add("IBL_or_background_high=medium_unknown_estimated_cost");
            actions.add("keep_ibl_background_moderate_until_runtime_probe");
        }
        if (expensive.isEmpty()) {
            actions.add("no_obvious_expensive_feature_enabled");
        }
        String summary = "estimated_cost not_runtime_measured needs_cost_probe_later features=" + (expensive.isEmpty() ? "none" : expensive.size());
        return new RenderCostDiagnostics(summary, expensive, actions, safety);
    }

    private static String lowerSafety(String current, String candidate) {
        if ("mobile_safe_low".equals(current) || "mobile_safe_low".equals(candidate)) return "mobile_safe_low";
        return candidate;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
