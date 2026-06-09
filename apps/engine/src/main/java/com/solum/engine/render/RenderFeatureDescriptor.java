package com.solum.engine.render;

public class RenderFeatureDescriptor {
    private final String id;
    private final String label;
    private final String category;
    private final String mobileCost;
    private final String mobileSafe;
    private final String status;
    private final String userWarning;

    public RenderFeatureDescriptor(String id, String label, String category, String mobileCost,
            String mobileSafe, String status, String userWarning) {
        this.id = safe(id, "unknown");
        this.label = safe(label, "Unknown");
        this.category = safe(category, "debug");
        this.mobileCost = safe(mobileCost, "unknown");
        this.mobileSafe = safe(mobileSafe, "unknown");
        this.status = safe(status, "not_verified");
        this.userWarning = safe(userWarning, "");
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getCategory() { return category; }
    public String getMobileCost() { return mobileCost; }
    public String getMobileSafe() { return mobileSafe; }
    public String getStatus() { return status; }
    public String getUserWarning() { return userWarning; }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
