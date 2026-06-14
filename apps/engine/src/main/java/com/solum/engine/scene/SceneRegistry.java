package com.solum.engine.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneRegistry {
    private final List<SceneObject> objects = new ArrayList<>();
    private String selectedObjectId = "none";

    public void clear() {
        objects.clear();
        selectedObjectId = "none";
    }

    public SceneObject registerLoadedModel(String name, String source, float rotateX, float rotateY, float rotateZ,
            float scale, float offsetX, float offsetY, float offsetZ, String status) {
        SceneObject existing = findById("active_model");
        float[] transform = new float[] {offsetX, offsetY, offsetZ, rotateX, rotateY, rotateZ, scale, 1.0f, 1.0f, 1.0f};
        SceneObject object;
        if (existing == null) {
            object = new SceneObject("active_model", name, "gltf_model", source, true, transform, status);
            objects.add(0, object);
        } else {
            existing.setTransform(offsetX, offsetY, offsetZ, rotateX, rotateY, rotateZ, scale);
            existing.setStatus(status);
            object = existing;
        }
        selectedObjectId = object.getId();
        markSelected(selectedObjectId);
        return object;
    }

    public SceneObject registerObject(String id, String name, String type, String source, int renderEntity,
            float x, float y, float z, float rx, float ry, float rz, float scale, String status) {
        SceneObject existing = findById(id);
        SceneObject object = existing == null
            ? new SceneObject(id, name, type, source, false,
                new float[] {x, y, z, rx, ry, rz, scale, 1.0f, 1.0f, 1.0f}, status)
            : existing;
        object.setTransform(x, y, z, rx, ry, rz, scale);
        object.setRenderEntity(renderEntity);
        object.setStatus(status);
        if (existing == null) objects.add(object);
        if ("none".equals(selectedObjectId)) selectObject(object.getId());
        return object;
    }

    public List<SceneObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public SceneObject getSelectedObject() {
        return findById(selectedObjectId);
    }

    public SceneObject findById(String id) {
        for (SceneObject object : objects) {
            if (object.getId().equals(id)) return object;
        }
        return null;
    }

    public String getSelectedObjectId() {
        return selectedObjectId;
    }

    public SceneObject selectObject(String id) {
        SceneObject object = findById(id);
        if (object == null) return getSelectedObject();
        selectedObjectId = object.getId();
        markSelected(selectedObjectId);
        return object;
    }

    public SceneObject selectNext() {
        if (objects.isEmpty()) {
            selectedObjectId = "none";
            return null;
        }
        int selectedIndex = -1;
        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i).getId().equals(selectedObjectId)) {
                selectedIndex = i;
                break;
            }
        }
        SceneObject next = objects.get((selectedIndex + 1) % objects.size());
        return selectObject(next.getId());
    }

    public void updateSelectedTransform(float x, float y, float z, float rx, float ry, float rz, float scale) {
        SceneObject selected = getSelectedObject();
        if (selected != null) selected.setTransform(x, y, z, rx, ry, rz, scale);
    }

    private void markSelected(String id) {
        for (SceneObject object : objects) object.setSelected(object.getId().equals(id));
    }

    public String summary() {
        SceneObject selected = getSelectedObject();
        return "objects=" + objects.size() + " selected=" + (selected == null ? "none" : selected.getName());
    }
}
