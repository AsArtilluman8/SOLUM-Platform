package com.solum.engine.scene;

public class SceneObject {
    private final String id;
    private final String name;
    private final String type;
    private final String source;
    private boolean selected;
    private final float[] transform;
    private String status;

    public SceneObject(String id, String name, String type, String source, boolean selected, float[] transform, String status) {
        this.id = safe(id, "object_0");
        this.name = safe(name, "Unnamed");
        this.type = safe(type, "unknown");
        this.source = safe(source, "none");
        this.selected = selected;
        this.transform = transform == null || transform.length != 10
            ? new float[] {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f}
            : transform.clone();
        this.status = safe(status, "registered");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean value) { selected = value; }
    public float[] getTransform() { return transform.clone(); }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = safe(value, "unknown"); }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
