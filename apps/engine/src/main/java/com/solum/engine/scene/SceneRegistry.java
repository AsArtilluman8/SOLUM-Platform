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
        objects.clear();
        float[] transform = new float[] {offsetX, offsetY, offsetZ, rotateX, rotateY, rotateZ, scale, 1.0f, 1.0f, 1.0f};
        SceneObject object = new SceneObject("active_model", name, "gltf_model", source, true, transform, status);
        objects.add(object);
        selectedObjectId = object.getId();
        return object;
    }

    public List<SceneObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public SceneObject getSelectedObject() {
        for (SceneObject object : objects) {
            if (object.getId().equals(selectedObjectId)) return object;
        }
        return null;
    }

    public String getSelectedObjectId() {
        return selectedObjectId;
    }

    public String summary() {
        SceneObject selected = getSelectedObject();
        return "objects=" + objects.size() + " selected=" + (selected == null ? "none" : selected.getName());
    }
}
