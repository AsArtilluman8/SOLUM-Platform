package com.solum.engine.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenderOwnershipMap {
    public static final String RENDER_API = "render_api";
    public static final String ACTIVITY_LOCAL = "activity_local";
    public static final String SCENE_REGISTRY = "scene_registry";
    public static final String PLANNED = "planned";
    public static final String FILAMENT_CONTROLLER = "filament_controller";
    public static final String RENDER_ACTUAL_STATE = "render_actual_state";
    public static final String NOT_EXPOSED = "not_exposed";
    public static final String NOT_VERIFIED = "not_verified";
    public static final String CONTROLLER_OWNED = "controller_owned";
    public static final String PARTIAL = "partial";

    private final List<Entry> entries = new ArrayList<>();

    public RenderOwnershipMap() {
        addDefaults();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public String shortSummary() {
        int controller = 0;
        int activity = 0;
        int partial = 0;
        int notExposed = 0;
        int notVerified = 0;
        int planned = 0;
        for (Entry entry : entries) {
            if (CONTROLLER_OWNED.equals(entry.status)) controller++;
            else if (ACTIVITY_LOCAL.equals(entry.status)) activity++;
            else if (PARTIAL.equals(entry.status)) partial++;
            else if (NOT_EXPOSED.equals(entry.status)) notExposed++;
            else if (NOT_VERIFIED.equals(entry.status)) notVerified++;
            else if (PLANNED.equals(entry.status)) planned++;
        }
        return "controller=" + controller + " activity_local=" + activity + " partial=" + partial
            + " not_exposed=" + notExposed + " not_verified=" + notVerified + " planned=" + planned;
    }

    public List<String> notVerifiedOrNotExposedSummary() {
        List<String> out = new ArrayList<>();
        for (Entry entry : entries) {
            if (NOT_EXPOSED.equals(entry.status) || NOT_VERIFIED.equals(entry.status) || PARTIAL.equals(entry.status)) {
                out.add(entry.featureName + "=" + entry.status);
            }
        }
        return out;
    }

    private void addDefaults() {
        add("Quality profile", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("Render Scale", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("Dynamic Resolution", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("MSAA", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("FXAA", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("TAA", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("Dithering", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, CONTROLLER_OWNED);
        add("SSR", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("Refraction", "Render / quality", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("AO", "AO / Bloom / Shadows", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("Bloom", "AO / Bloom / Shadows", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("Shadows", "AO / Bloom / Shadows", RENDER_API, FILAMENT_CONTROLLER, RENDER_ACTUAL_STATE, PARTIAL);
        add("Fog", "Fog", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("ColorGrading", "Color", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("Lighting core", "Lighting", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("IBL", "IBL / sky", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("Skybox", "IBL / sky", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("Sun glare", "Sun glare", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("Camera/model transform", "Camera/model view", RENDER_API, SCENE_REGISTRY, ACTIVITY_LOCAL, PARTIAL);
        add("FPS timing", "Diagnostics", RENDER_API, ACTIVITY_LOCAL, ACTIVITY_LOCAL, PARTIAL);
        add("Config save/load", "Diagnostics", ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL, ACTIVITY_LOCAL);
        add("Model load/import", "Scene", ACTIVITY_LOCAL, ACTIVITY_LOCAL, SCENE_REGISTRY, PARTIAL);
        add("Material inspector", "Scene", PLANNED, ACTIVITY_LOCAL, NOT_VERIFIED, NOT_VERIFIED);
        add("Picking/select", "Scene", PLANNED, ACTIVITY_LOCAL, NOT_VERIFIED, PARTIAL);
    }

    private void add(String featureName, String category, String requestedOwner, String applyOwner,
            String actualStateOwner, String status) {
        entries.add(new Entry(featureName, category, requestedOwner, applyOwner, actualStateOwner, status));
    }

    public static class Entry {
        private final String featureName;
        private final String category;
        private final String requestedOwner;
        private final String applyOwner;
        private final String actualStateOwner;
        private final String status;

        Entry(String featureName, String category, String requestedOwner, String applyOwner,
                String actualStateOwner, String status) {
            this.featureName = featureName;
            this.category = category;
            this.requestedOwner = requestedOwner;
            this.applyOwner = applyOwner;
            this.actualStateOwner = actualStateOwner;
            this.status = status;
        }

        public String getFeatureName() { return featureName; }
        public String getCategory() { return category; }
        public String getRequestedOwner() { return requestedOwner; }
        public String getApplyOwner() { return applyOwner; }
        public String getActualStateOwner() { return actualStateOwner; }
        public String getStatus() { return status; }
    }
}
