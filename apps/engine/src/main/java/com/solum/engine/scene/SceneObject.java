package com.solum.engine.scene;

public class SceneObject {
    private final String id;
    private final String name;
    private final String type;
    private final String source;
    private boolean selected;
    private final float[] transform;
    private String status;
    private int renderEntity;

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
        this.renderEntity = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean value) { selected = value; }
    public float[] getTransform() { return transform.clone(); }
    public float getPositionX() { return transform[0]; }
    public float getPositionY() { return transform[1]; }
    public float getPositionZ() { return transform[2]; }
    public float getRotationX() { return transform[3]; }
    public float getRotationY() { return transform[4]; }
    public float getRotationZ() { return transform[5]; }
    public float getScale() { return transform[6]; }
    public void setTransform(float positionX, float positionY, float positionZ,
            float rotationX, float rotationY, float rotationZ, float scale) {
        transform[0] = positionX;
        transform[1] = positionY;
        transform[2] = positionZ;
        transform[3] = rotationX;
        transform[4] = rotationY;
        transform[5] = rotationZ;
        transform[6] = scale;
        transform[7] = 1.0f;
        transform[8] = 1.0f;
        transform[9] = 1.0f;
    }
    public void resetTransform() {
        setTransform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
    }
    public int getRenderEntity() { return renderEntity; }
    public void setRenderEntity(int value) { renderEntity = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = safe(value, "unknown"); }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
