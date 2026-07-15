package com.solum.engine.environment.p63;

import android.opengl.Matrix;

import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.LightManager;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Skybox;
import com.google.android.filament.Texture;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.utils.ModelViewer;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SolumFilamentEnvironmentAdapter {
    private static final float[][] CLOUD_POSITIONS = {{-9,10,-8},{-5,11,-11},{-1,9,-9},{4,10,-12},{9,11,-8},{-8,12,-16},{-3,10,-17},{2,12,-16},{7,9,-17},{-6,8,-5},{0,11,-5},{7,10,-4}};
    private static final float[][] RIPPLE_POSITIONS = {{-7,-1},{-5,4},{-2,1},{0,5},{2,-3},{7.5f,-3},{-8,6},{1,7}};
    private static final String[] CLOUD_NAMES = indexedNames("P63_CLOUD_", 12);
    private static final String[] STAR_NAMES = indexedNames("P63_STAR_GROUP_", 3);
    private static final String[] RAIN_NAMES = cellNames("P63_RAIN_CELL_");
    private static final String[] SNOW_NAMES = cellNames("P63_SNOW_CELL_");
    private static final String[] DUST_NAMES = cellNames("P63_DUST_CELL_");
    private static final String[] RIPPLE_NAMES = indexedNames("P63_RIPPLE_", 8);
    private static final int MOON_PHASE_STEPS = 33;
    private static final String[] MOON_PHASE_NAMES = phaseNames();
    public interface Host {
        void applyPreparedIbl(String slot, long revision, float intensity, float blend);
        void applyEnvironmentSkyColor(float red, float green, float blue, float lightningFlash);
        void onEnvironmentAdapterStatus(String status);
    }

    private final ModelViewer viewer;
    private final SolumEnvironmentController controller;
    private final SolumEnvironmentAudioSystem audio;
    private final Host host;
    private final Map<String, Integer> stageEntities = new LinkedHashMap<>();
    private final float[] transformScratch = new float[16];
    private final float[] billboardRightScratch = new float[3];
    private final float[] lastSkySunDirection = {Float.NaN, Float.NaN, Float.NaN};
    private int moonLightEntity;
    private int lightningLightEntity;
    private Texture celestialSkyTexture;
    private Skybox celestialSkybox;
    private long lastIblRevision = -1L;
    private float rainPhase;
    private float snowPhase;
    private float dustPhase;
    private float ripplePhase;
    private float materialClock;
    private String status = "created_not_bound";
    private String materialStatus = "not_applied";
    private boolean stageBound;
    private boolean celestialSkyVisible;
    private String celestialSkyStatus = "not_created";
    private int activeMoonPhaseIndex = -1;

    public SolumFilamentEnvironmentAdapter(ModelViewer viewer, SolumEnvironmentController controller,
                                            SolumEnvironmentAudioSystem audio, Host host) {
        if (viewer == null || controller == null) throw new IllegalArgumentException("filament_adapter_dependencies_missing");
        this.viewer = viewer; this.controller = controller; this.audio = audio; this.host = host;
        createLights();
    }

    public void bindStage(FilamentAsset asset) {
        stageEntities.clear(); stageBound = false;
        if (asset == null) { status = "stage_asset_missing_lights_fog_only"; notifyStatus(); return; }
        int p63Count = 0;
        for (int entity : asset.getRenderableEntities()) {
            String name = asset.getName(entity);
            if (name != null && !name.isEmpty()) {
                stageEntities.put(name, entity);
                if (name.startsWith("P63_")) p63Count++;
            }
        }
        boolean celestialStage = stageEntities.containsKey("P63_CELESTIAL_STAGE_ROOT")
            && stageEntities.containsKey("P63_SUN_DISK")
            && stageEntities.containsKey("P63_MOON_PHASE_00") && stageEntities.containsKey("P63_STAGE_GROUND");
        stageBound = celestialStage || p63Count >= 40;
        activeMoonPhaseIndex = -1;
        if (celestialStage) for (String name : MOON_PHASE_NAMES) setLayerVisible(name, false);
        status = stageBound ? "bound_p63_filament_stage_entities=" + p63Count : "asset_bound_without_p63_stage_entities=" + p63Count;
        SolumEnvironmentState state = controller.getState();
        state.stageStatus = status; state.adapterStatus = "filament_adapter_live";
        if (!controller.isCelestialOnlyMode()) {
            state.setFeatureStatus("world_space_rain", stageBound ? EnvironmentFeatureStatus.PROTOTYPE : EnvironmentFeatureStatus.PLACEHOLDER);
            state.setFeatureStatus("world_space_snow", stageBound ? EnvironmentFeatureStatus.PROTOTYPE : EnvironmentFeatureStatus.PLACEHOLDER);
        }
        notifyStatus();
    }

    public SolumEnvironmentState update(float deltaSeconds) {
        float dt = Math.max(0.0f, Math.min(0.1f, deltaSeconds));
        SolumEnvironmentState state = controller.update(dt);
        applyLights(state);
        if (controller.isCelestialOnlyMode()) disableFog(); else applyFog(state);
        if (host != null) {
            SolumCelestialControlState controls = controller.getCelestialControls();
            if (!controller.isCelestialOnlyMode() || controls.p63IblEnabled) {
                lastIblRevision = state.lighting.iblRevision;
                host.applyPreparedIbl(state.lighting.iblSlot, lastIblRevision, state.lighting.ambientIntensity, state.lighting.iblBlend);
            }
            host.applyEnvironmentSkyColor(state.atmosphere.skyColor[0], state.atmosphere.skyColor[1], state.atmosphere.skyColor[2], state.lightning.flash);
        }
        if (stageBound) applyStage(state, dt);
        if (audio != null) audio.update(state, dt);
        state.adapterStatus = status + " material=" + materialStatus;
        return state;
    }

    public void release() {
        Engine engine = viewer.getEngine();
        destroyCelestialSky(engine);
        destroyLight(engine, moonLightEntity); moonLightEntity = 0;
        destroyLight(engine, lightningLightEntity); lightningLightEntity = 0;
        stageEntities.clear(); stageBound = false; status = "released";
    }

    public String getStatus() { return status + " material=" + materialStatus + " sky=" + celestialSkyStatus
        + " moonPhaseNode=" + getActiveMoonPhaseNode() + " stageBound=" + stageBound; }

    public String getActiveMoonPhaseNode() {
        return activeMoonPhaseIndex < 0 ? "none" : MOON_PHASE_NAMES[activeMoonPhaseIndex];
    }

    private void createLights() {
        Engine engine = viewer.getEngine();
        moonLightEntity = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL).castShadows(false).direction(0.0f, 1.0f, 0.0f)
            .color(0.446f, 0.557f, 0.865f).intensity(0.0f).build(engine, moonLightEntity);
        viewer.getScene().addEntity(moonLightEntity);
        if (!controller.isCelestialOnlyMode()) {
            lightningLightEntity = EntityManager.get().create();
            new LightManager.Builder(LightManager.Type.POINT).castShadows(false).position(0.0f, 8.0f, -8.0f)
                .color(0.495f, 0.613f, 1.0f).intensity(0.0f).falloff(32.0f).build(engine, lightningLightEntity);
            viewer.getScene().addEntity(lightningLightEntity);
        }
    }

    private void applyLights(SolumEnvironmentState state) {
        try {
            LightManager manager = viewer.getEngine().getLightManager();
            int sun = manager.getInstance(viewer.getLight());
            if (sun != 0) {
                manager.setDirection(sun, state.lighting.sunDirection[0], state.lighting.sunDirection[1], state.lighting.sunDirection[2]);
                manager.setColor(sun, state.lighting.sunColor[0], state.lighting.sunColor[1], state.lighting.sunColor[2]);
                manager.setIntensity(sun, safeRange(state.lighting.sunLux, 0.0f, 50.0f));
                manager.setShadowCaster(sun, state.lighting.sunLux > 0.05f);
            }
            int moon = manager.getInstance(moonLightEntity);
            if (moon != 0) {
                manager.setDirection(moon, state.lighting.moonDirection[0], state.lighting.moonDirection[1], state.lighting.moonDirection[2]);
                manager.setColor(moon, state.lighting.moonColor[0], state.lighting.moonColor[1], state.lighting.moonColor[2]);
                manager.setIntensity(moon, safeRange(state.lighting.moonLux, 0.0f, 2.0f));
            }
            int lightning = manager.getInstance(lightningLightEntity);
            if (lightning != 0) {
                manager.setPosition(lightning, state.lightning.strikeX, 7.5f, state.lightning.strikeZ);
                manager.setIntensity(lightning, Math.max(0.0f, state.lighting.lightningLumens));
                manager.setFalloff(lightning, 30.0f + state.lightning.flash * 18.0f);
            }
        } catch (Throwable error) {
            status = "light_apply_failed_" + safe(error);
        }
    }

    private void applyFog(SolumEnvironmentState state) {
        try {
            View view = viewer.getView();
            View.FogOptions fog = view.getFogOptions();
            fog.enabled = state.fog.density > 0.006f;
            fog.density = state.fog.density;
            fog.distance = state.fog.distance;
            fog.cutOffDistance = Math.max(80.0f, state.fog.distance + 100.0f);
            fog.maximumOpacity = state.fog.maximumOpacity;
            fog.height = state.fog.height;
            fog.heightFalloff = state.fog.heightFalloff;
            fog.color[0] = state.fog.color[0]; fog.color[1] = state.fog.color[1]; fog.color[2] = state.fog.color[2];
            fog.fogColorFromIbl = true;
            view.setFogOptions(fog);
        } catch (Throwable error) {
            status = "fog_apply_failed_" + safe(error);
        }
    }

    private void disableFog() {
        try {
            View.FogOptions fog = viewer.getView().getFogOptions();
            if (fog.enabled) { fog.enabled = false; viewer.getView().setFogOptions(fog); }
        } catch (Throwable error) {
            status = "celestial_fog_disable_failed_" + safe(error);
        }
    }

    private void applyStage(SolumEnvironmentState state, float dt) {
        if (controller.isCelestialOnlyMode()) {
            applyAnalyticSky(state);
            applyCelestialGeometry(state);
            materialClock += dt;
            if (materialClock >= 0.12f) { materialClock = 0.0f; applyMaterials(state); }
            return;
        }
        rainPhase = wrap(rainPhase + dt * (5.5f + state.wind.speed * 5.0f), 12.0f);
        snowPhase = wrap(snowPhase + dt * (0.75f + state.wind.speed * 1.25f), 12.0f);
        dustPhase = wrap(dustPhase + dt * (0.65f + state.wind.speed * 2.6f), 12.0f);
        ripplePhase = wrap(ripplePhase + dt * (1.2f + state.weather.rain * 2.2f), 1.0f);
        applyCelestialGeometry(state);
        applyCloudGeometry(state);
        applyPrecipitationGeometry(state);
        applySurfaceGeometry(state);
        applyLightningGeometry(state);
        applyWindGeometry(state);
        materialClock += dt;
        if (materialClock >= 0.12f) { materialClock = 0.0f; applyMaterials(state); }
    }

    private void applyCelestialGeometry(SolumEnvironmentState state) {
        float radius = controller.isCelestialOnlyMode() ? SolumCelestialCoordinateSystem.SKY_RADIUS : 27.0f;
        SolumCelestialCoordinateSystem.positionRelativeToCamera(state.lighting.sunVisualPosition,
            controller.getCameraX(), controller.getCameraY(), controller.getCameraZ(),
            state.lighting.sunVisualDirection, radius);
        SolumCelestialCoordinateSystem.positionRelativeToCamera(state.lighting.moonVisualPosition,
            controller.getCameraX(), controller.getCameraY(), controller.getCameraZ(),
            state.lighting.moonVisualDirection, radius);
        float sunX = state.lighting.sunVisualPosition[0];
        float sunY = state.lighting.sunVisualPosition[1];
        float sunZ = state.lighting.sunVisualPosition[2];
        float moonX = state.lighting.moonVisualPosition[0];
        float moonY = state.lighting.moonVisualPosition[1];
        float moonZ = state.lighting.moonVisualPosition[2];
        SolumCelestialControlState controls = controller.getCelestialControls();
        float sunAngular = controller.isCelestialOnlyMode() ? controls.sunAngularSizeDegrees : 4.66f * controller.getSunDiskScale();
        float moonAngular = controller.isCelestialOnlyMode() ? controls.moonAngularSizeDegrees : 4.24f * controller.getMoonDiskScale();
        boolean sunVisible = state.lighting.sunAboveHorizon && state.lighting.sunDiskBrightness > 0.01f;
        boolean moonVisible = state.lighting.moonAboveHorizon && state.lighting.moonDiskBrightness > 0.01f;
        float sunScale = sunVisible ? radius * (float)Math.tan(Math.toRadians(sunAngular * 0.5f)) : 0.001f;
        float moonScale = moonVisible ? radius * (float)Math.tan(Math.toRadians(moonAngular * 0.5f)) : 0.001f;
        setBillboardTransform("P63_SUN_HALO", sunX, sunY, sunZ, state.lighting.sunVisualDirection,
            sunScale * (2.0f + controls.sunGlow * 2.5f + controls.sunEdgeSoftness * 0.8f),
            sunScale * (2.0f + controls.sunGlow * 2.5f + controls.sunEdgeSoftness * 0.8f), -0.045f);
        setBillboardTransform("P63_SUN_DISK", sunX, sunY, sunZ, state.lighting.sunVisualDirection,
            sunScale, sunScale, 0.0f);
        setBillboardTransform("P63_MOON_HALO", moonX, moonY, moonZ, state.lighting.moonVisualDirection,
            moonScale * (1.8f + controls.moonGlow * 2.0f + controls.moonEdgeSoftness * 0.6f),
            moonScale * (1.8f + controls.moonGlow * 2.0f + controls.moonEdgeSoftness * 0.6f), -0.040f);
        if (controller.isCelestialOnlyMode()) {
            int requestedPhase = Math.max(0, Math.min(MOON_PHASE_STEPS - 1,
                Math.round(state.lighting.moonPhase * (MOON_PHASE_STEPS - 1))));
            if (requestedPhase != activeMoonPhaseIndex) {
                if (activeMoonPhaseIndex >= 0) setLayerVisible(MOON_PHASE_NAMES[activeMoonPhaseIndex], false);
                activeMoonPhaseIndex = requestedPhase;
                setLayerVisible(MOON_PHASE_NAMES[activeMoonPhaseIndex], true);
            }
            setBillboardTransform(MOON_PHASE_NAMES[activeMoonPhaseIndex], moonX, moonY, moonZ,
                state.lighting.moonVisualDirection, moonScale, moonScale, 0.0f);
            return;
        }
        setBillboardTransform("P63_MOON_DISK", moonX, moonY, moonZ, state.lighting.moonVisualDirection,
            moonScale, moonScale, 0.0f);
        float phase = state.lighting.moonPhase;
        computeBillboardRight(state.lighting.moonVisualDirection, billboardRightScratch);
        float shadowOffset = phase * moonScale * 2.0f;
        float shadowScale = moonVisible && phase < 0.995f ? moonScale : 0.001f;
        setBillboardTransform("P63_MOON_SHADOW",
            moonX + billboardRightScratch[0] * shadowOffset,
            moonY + billboardRightScratch[1] * shadowOffset,
            moonZ + billboardRightScratch[2] * shadowOffset,
            state.lighting.moonVisualDirection, shadowScale, shadowScale, 0.035f);
        float starScale = Math.max(0.001f, state.lighting.starVisibility);
        float rotation = state.timeOfDay / 2400.0f * 360.0f;
        int visibleGroups = Math.max(0, Math.min(3, (int) Math.ceil(controller.getStarDensity() * 3.0f)));
        for (int i = 0; i < 3; i++) {
            float groupScale = i < visibleGroups ? starScale : 0.001f;
            setTransform(STAR_NAMES[i], 0, 0, 0, groupScale, groupScale, groupScale, rotation, 0, 0);
        }
    }

    private void applyCloudGeometry(SolumEnvironmentState state) {
        int requested = Math.min(state.clouds.visibleGroups, Math.max(0, (int) Math.ceil(state.clouds.coverage * 12.0f)));
        for (int i = 0; i < CLOUD_POSITIONS.length; i++) {
            boolean visible = i < requested && state.clouds.coverage > 0.015f;
            float scale = visible ? 0.52f + state.clouds.density * 0.78f : 0.001f;
            float x = wrapSigned(CLOUD_POSITIONS[i][0] + state.clouds.offsetX, 22.0f);
            float z = CLOUD_POSITIONS[i][2] + wrapSigned(state.clouds.offsetZ, 18.0f);
            setTransform(CLOUD_NAMES[i], x, CLOUD_POSITIONS[i][1], z, scale, 0.75f + state.clouds.thickness * 0.65f, scale, 0, 0, 0);
        }
    }

    private void applyPrecipitationGeometry(SolumEnvironmentState state) {
        int maxCells = state.precipitation.particleLimit <= 500 ? 9 : (state.precipitation.particleLimit <= 1200 ? 17 : 25);
        int rainCells = Math.min(maxCells, (int) Math.ceil(maxCells * state.weather.rain));
        int snowCells = Math.min(maxCells, (int) Math.ceil(maxCells * state.weather.snow));
        int dustCells = Math.min(maxCells, (int) Math.ceil(maxCells * state.weather.dust));
        for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
            int index = z * 5 + x;
            float worldX = controller.getCameraX() + (x - 2) * 3.0f;
            float worldZ = controller.getCameraZ() + (z - 2) * 3.0f;
            boolean blocked = controller.getOcclusion().blocksPrecipitation(worldX, 1.5f, worldZ);
            float rainVisible = !blocked && index < rainCells ? 1.0f : 0.001f;
            float snowVisible = !blocked && index < snowCells ? 1.0f : 0.001f;
            float dustVisible = !blocked && index < dustCells ? 1.0f : 0.001f;
            float windX = state.wind.x * 1.8f;
            float windZ = state.wind.z * 1.8f;
            setTransform(RAIN_NAMES[index], worldX + windX, 6.0f - rainPhase + (index % 3) * 4.0f, worldZ + windZ, rainVisible, rainVisible, rainVisible, 0, 0, -state.wind.x * 10.0f);
            setTransform(SNOW_NAMES[index], worldX + windX * 0.7f, 6.0f - snowPhase + (index % 3) * 4.0f, worldZ + windZ * 0.7f, snowVisible, snowVisible, snowVisible, state.wind.phase * 9.0f + index * 17.0f, 0, 0);
            setTransform(DUST_NAMES[index], worldX + dustPhase + windX, 3.5f + (index % 2), worldZ + windZ, dustVisible, dustVisible, dustVisible, state.wind.phase * 3.0f, 0, 0);
        }
    }

    private void applySurfaceGeometry(SolumEnvironmentState state) {
        for (int i = 0; i < RIPPLE_POSITIONS.length; i++) {
            boolean blocked = controller.getOcclusion().blocksPrecipitation(RIPPLE_POSITIONS[i][0], 0.1f, RIPPLE_POSITIONS[i][1]);
            float pulse = (!blocked && state.weather.rain > 0.04f) ? (0.12f + ripplePhase * 0.65f) : 0.001f;
            setTransform(RIPPLE_NAMES[i], RIPPLE_POSITIONS[i][0], 0.055f, RIPPLE_POSITIONS[i][1], pulse, 1.0f, pulse, i * 23.0f, 0, 0);
        }
    }

    private void applyLightningGeometry(SolumEnvironmentState state) {
        float visible = state.lightning.active && state.lightning.flash > 0.008f ? 1.0f : 0.001f;
        setTransform("P63_LIGHTNING_BOLT", state.lightning.strikeX, 0.0f, state.lightning.strikeZ, visible, visible, visible, 0, 0, 0);
    }

    private void applyWindGeometry(SolumEnvironmentState state) {
        float bend = state.wind.x * 16.0f + (float) Math.sin(state.wind.phase * 2.0f) * state.wind.gust * 11.0f;
        setTransform("P63_FLAG", -7.25f, 3.8f, 3.5f, 0.75f, 1.0f, 0.55f, 0, 90.0f, bend);
    }

    private void applyMaterials(SolumEnvironmentState state) {
        try {
            if (controller.isCelestialOnlyMode()) {
                SolumCelestialControlState controls = controller.getCelestialControls();
                float highlight = controls.highlightClampEnabled ? controls.highlightClamp : 1.0f;
                float sunVisual = Math.min(highlight, safeRange(state.lighting.sunDiskBrightness, 0.0f, 2.0f));
                float moonVisual = Math.min(highlight, safeRange(state.lighting.moonDiskBrightness, 0.0f, 2.0f));
                setMaterial4("P63_SUN_DISK", "baseColorFactor", controls.sunTint[0] * sunVisual,
                    controls.sunTint[1] * sunVisual, controls.sunTint[2] * sunVisual, Math.min(1.0f, sunVisual));
                String moonNode = getActiveMoonPhaseNode();
                setMaterial4(moonNode, "baseColorFactor", controls.moonTint[0] * moonVisual,
                    controls.moonTint[1] * moonVisual, controls.moonTint[2] * moonVisual, Math.min(1.0f, moonVisual));
                setMaterial3("P63_SUN_DISK", "emissiveFactor", controls.sunTint[0] * controls.sunEmissive,
                    controls.sunTint[1] * controls.sunEmissive, controls.sunTint[2] * controls.sunEmissive);
                setMaterial3(moonNode, "emissiveFactor", controls.moonTint[0] * controls.moonEmissive,
                    controls.moonTint[1] * controls.moonEmissive, controls.moonTint[2] * controls.moonEmissive);
                float sunHalo = controls.sunGlowEnabled && state.lighting.sunAboveHorizon ? controls.sunGlow : 0.0f;
                float moonHalo = controls.moonGlowEnabled && state.lighting.moonAboveHorizon ? controls.moonGlow : 0.0f;
                setMaterial4("P63_SUN_HALO", "baseColorFactor", controls.sunTint[0], controls.sunTint[1],
                    controls.sunTint[2], safeRange(sunHalo * 0.52f, 0.0f, 0.52f));
                setMaterial4("P63_MOON_HALO", "baseColorFactor", controls.moonTint[0], controls.moonTint[1],
                    controls.moonTint[2], safeRange(moonHalo * 0.38f, 0.0f, 0.38f));
                materialStatus = "p63_2a2_single_visible_moon_disc_analytic_terminator_no_occluder_safe_emissive_halos";
                return;
            }
            float wet = state.surface.wetness;
            setMaterial("P63_WET_SURFACE", "roughnessFactor", Math.max(0.04f, 0.78f - wet * 0.66f));
            setMaterial4("P63_WET_SURFACE", "baseColorFactor", 0.085f * (1.0f - wet * 0.38f), 0.09f * (1.0f - wet * 0.38f), 0.095f * (1.0f - wet * 0.38f), 1.0f);
            setMaterial("P63_PUDDLE", "roughnessFactor", Math.max(0.018f, 0.16f - state.surface.puddle * 0.13f));
            setMaterial4("P63_PUDDLE", "baseColorFactor", 0.035f, 0.09f, 0.13f, Math.max(0.02f, state.surface.puddle * 0.88f));
            float snow = state.surface.snowCover;
            setMaterial4("P63_SNOW_SURFACE", "baseColorFactor", 0.26f + snow * 0.62f, 0.28f + snow * 0.64f, 0.30f + snow * 0.67f, 1.0f);
            setMaterial("P63_SNOW_SURFACE", "roughnessFactor", 0.48f + snow * 0.36f);
            float ice = state.surface.ice;
            setMaterial4("P63_ICE_SURFACE", "baseColorFactor", 0.18f + ice * 0.24f, 0.25f + ice * 0.43f, 0.30f + ice * 0.48f, Math.max(0.08f, ice * 0.78f));
            setMaterial("P63_ICE_SURFACE", "roughnessFactor", Math.max(0.05f, 0.42f - ice * 0.33f));
            float flash = state.lightning.flash * 0.65f;
            setMaterial4("P63_CLOUD_0", "baseColorFactor", Math.min(1.0f,state.clouds.color[0]+flash), Math.min(1.0f,state.clouds.color[1]+flash), Math.min(1.0f,state.clouds.color[2]+flash), 0.38f + state.clouds.density * 0.46f);
            setMaterial4("P63_RAIN_CELL_0_0", "baseColorFactor", 0.42f, 0.62f, Math.min(1.0f, 0.82f + state.lightning.flash * 0.18f), 0.20f + state.weather.rain * 0.62f);
            setMaterial4("P63_SNOW_CELL_0_0", "baseColorFactor", 0.92f, 0.96f, 1.0f, 0.28f + state.weather.snow * 0.68f);
            setMaterial4("P63_DUST_CELL_0_0", "baseColorFactor", 0.62f, 0.38f, 0.18f, 0.12f + state.weather.dust * 0.48f);
            setMaterial4("P63_SUN_DISK", "baseColorFactor", 1.0f, 0.64f + state.lighting.sunElevation * 0.25f, 0.22f, Math.min(1.0f, state.lighting.sunDiskBrightness));
            setMaterial4("P63_MOON_DISK", "baseColorFactor", 0.72f, 0.78f, 0.90f, Math.min(1.0f, state.lighting.moonDiskBrightness));
            materialStatus = "live_pbr_parameters_wet_snow_ice_cloud_precipitation";
        } catch (Throwable error) {
            materialStatus = "material_parameter_apply_failed_" + safe(error);
        }
    }

    private void setMaterial(String name, String parameter, float value) {
        MaterialInstance material = firstMaterial(name);
        if (material != null) material.setParameter(parameter, value);
    }

    private void setMaterial4(String name, String parameter, float x, float y, float z, float w) {
        MaterialInstance material = firstMaterial(name);
        if (material != null) material.setParameter(parameter, x, y, z, w);
    }

    private void setMaterial3(String name, String parameter, float x, float y, float z) {
        MaterialInstance material = firstMaterial(name);
        if (material != null) material.setParameter(parameter, x, y, z);
    }

    private void setLayerVisible(String name, boolean visible) {
        Integer entity = stageEntities.get(name);
        if (entity == null) return;
        RenderableManager manager = viewer.getEngine().getRenderableManager();
        int instance = manager.getInstance(entity);
        if (instance != 0) manager.setLayerMask(instance, 0xFF, visible ? 0x01 : 0x00);
    }

    private MaterialInstance firstMaterial(String name) {
        Integer entity = stageEntities.get(name);
        if (entity == null) return null;
        RenderableManager manager = viewer.getEngine().getRenderableManager();
        int instance = manager.getInstance(entity);
        return instance == 0 || manager.getPrimitiveCount(instance) == 0 ? null : manager.getMaterialInstanceAt(instance, 0);
    }

    private void setTransform(String name, float tx, float ty, float tz, float sx, float sy, float sz, float rotationY, float rotationX, float rotationZ) {
        Integer entity = stageEntities.get(name); if (entity == null) return;
        TransformManager manager = viewer.getEngine().getTransformManager();
        int instance = manager.getInstance(entity); if (instance == 0) return;
        Matrix.setIdentityM(transformScratch, 0); Matrix.translateM(transformScratch, 0, tx, ty, tz);
        if (rotationY != 0.0f) Matrix.rotateM(transformScratch, 0, rotationY, 0, 1, 0);
        if (rotationX != 0.0f) Matrix.rotateM(transformScratch, 0, rotationX, 1, 0, 0);
        if (rotationZ != 0.0f) Matrix.rotateM(transformScratch, 0, rotationZ, 0, 0, 1);
        Matrix.scaleM(transformScratch, 0, sx, sy, sz);
        manager.setTransform(instance, transformScratch);
    }

    private void setBillboardTransform(String name, float tx, float ty, float tz, float[] bodyDirection,
                                       float scaleX, float scaleY, float towardCameraOffset) {
        Integer entity = stageEntities.get(name); if (entity == null) return;
        TransformManager manager = viewer.getEngine().getTransformManager();
        int instance = manager.getInstance(entity); if (instance == 0) return;
        float forwardX = -bodyDirection[0];
        float forwardY = -bodyDirection[1];
        float forwardZ = -bodyDirection[2];
        computeBillboardRight(bodyDirection, billboardRightScratch);
        float rightX = billboardRightScratch[0];
        float rightY = billboardRightScratch[1];
        float rightZ = billboardRightScratch[2];
        float upX = forwardY * rightZ - forwardZ * rightY;
        float upY = forwardZ * rightX - forwardX * rightZ;
        float upZ = forwardX * rightY - forwardY * rightX;
        transformScratch[0] = rightX * scaleX; transformScratch[1] = rightY * scaleX; transformScratch[2] = rightZ * scaleX; transformScratch[3] = 0.0f;
        transformScratch[4] = upX * scaleY; transformScratch[5] = upY * scaleY; transformScratch[6] = upZ * scaleY; transformScratch[7] = 0.0f;
        transformScratch[8] = forwardX; transformScratch[9] = forwardY; transformScratch[10] = forwardZ; transformScratch[11] = 0.0f;
        transformScratch[12] = tx + forwardX * towardCameraOffset;
        transformScratch[13] = ty + forwardY * towardCameraOffset;
        transformScratch[14] = tz + forwardZ * towardCameraOffset;
        transformScratch[15] = 1.0f;
        manager.setTransform(instance, transformScratch);
    }

    private static void computeBillboardRight(float[] bodyDirection, float[] out) {
        float forwardX = -bodyDirection[0];
        float forwardY = -bodyDirection[1];
        float forwardZ = -bodyDirection[2];
        float rightX = forwardZ;
        float rightY = 0.0f;
        float rightZ = -forwardX;
        float length = (float)Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (length < 0.001f) {
            rightX = -forwardY; rightY = forwardX; rightZ = 0.0f;
            length = (float)Math.sqrt(rightX * rightX + rightY * rightY);
        }
        if (length < 0.001f) { rightX = 1.0f; rightY = 0.0f; rightZ = 0.0f; length = 1.0f; }
        out[0] = rightX / length; out[1] = rightY / length; out[2] = rightZ / length;
    }

    private void applyAnalyticSky(SolumEnvironmentState state) {
        SolumCelestialControlState controls = controller.getCelestialControls();
        if (!controls.skyEnabled) {
            if (celestialSkyVisible) viewer.getScene().setSkybox(null);
            celestialSkyVisible = false;
            celestialSkyStatus = "disabled_user";
            return;
        }
        try {
            Engine engine = viewer.getEngine();
            if (celestialSkyTexture == null) {
                celestialSkyTexture = new Texture.Builder()
                    .width(SolumAnalyticSky.CUBEMAP_SIZE)
                    .height(SolumAnalyticSky.CUBEMAP_SIZE)
                    .levels(1)
                    .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
                    .format(Texture.InternalFormat.SRGB8_A8)
                    .build(engine);
            }
            float dot = state.lighting.sunVisualDirection[0] * lastSkySunDirection[0]
                + state.lighting.sunVisualDirection[1] * lastSkySunDirection[1]
                + state.lighting.sunVisualDirection[2] * lastSkySunDirection[2];
            if (!Float.isFinite(dot) || dot < 0.99995f || celestialSkybox == null) {
                ByteBuffer pixels = SolumAnalyticSky.createSrgbCubemap(state.lighting.sunVisualDirection);
                Texture.PixelBufferDescriptor descriptor = new Texture.PixelBufferDescriptor(
                    pixels, Texture.Format.RGBA, Texture.Type.UBYTE, 1);
                celestialSkyTexture.setImage(engine, 0, descriptor, SolumAnalyticSky.faceOffsets());
                System.arraycopy(state.lighting.sunVisualDirection, 0, lastSkySunDirection, 0, 3);
            }
            if (celestialSkybox == null) {
                celestialSkybox = new Skybox.Builder().environment(celestialSkyTexture).showSun(false).build(engine);
            }
            // Re-assert the celestial sky because legacy Render Control Center actions can
            // legitimately replace the Scene skybox while this bounded stage is active.
            viewer.getScene().setSkybox(celestialSkybox);
            celestialSkyVisible = true;
            celestialSkyStatus = "SOLUM_NATIVE_ANALYTIC_CUBEMAP_64_SRGB_no_hemisphere_seam";
        } catch (Throwable error) {
            controls.skyEnabled = false;
            controls.lastCelestialError = "sky_material_disabled_" + safe(error);
            celestialSkyStatus = "disabled_after_error_" + safe(error);
            celestialSkyVisible = false;
            try { viewer.getScene().setSkybox(null); } catch (Throwable ignored) { }
        }
    }

    private void destroyCelestialSky(Engine engine) {
        try { viewer.getScene().setSkybox(null); } catch (Throwable ignored) { }
        if (celestialSkybox != null) {
            try { engine.destroySkybox(celestialSkybox); } catch (Throwable ignored) { }
            celestialSkybox = null;
        }
        if (celestialSkyTexture != null) {
            try { engine.destroyTexture(celestialSkyTexture); } catch (Throwable ignored) { }
            celestialSkyTexture = null;
        }
        celestialSkyVisible = false;
    }

    private void destroyLight(Engine engine, int entity) {
        if (entity == 0) return;
        try { viewer.getScene().removeEntity(entity); engine.getLightManager().destroy(entity); EntityManager.get().destroy(entity); } catch (Throwable ignored) { }
    }

    private void notifyStatus() { if (host != null) host.onEnvironmentAdapterStatus(status); }
    private static String[] indexedNames(String prefix,int count){String[] out=new String[count];for(int i=0;i<count;i++)out[i]=prefix+i;return out;}
    private static String[] phaseNames(){String[] out=new String[MOON_PHASE_STEPS];for(int i=0;i<out.length;i++)out[i]=String.format(Locale.US,"P63_MOON_PHASE_%02d",i);return out;}
    private static String[] cellNames(String prefix){String[] out=new String[25];for(int z=0;z<5;z++)for(int x=0;x<5;x++)out[z*5+x]=prefix+x+"_"+z;return out;}
    private static float wrap(float value, float range) { float out=value%range;return out<0?out+range:out; }
    private static float wrapSigned(float value, float range) { float out=(value+range*0.5f)%range;if(out<0)out+=range;return out-range*0.5f; }
    private static float safeRange(float value, float min, float max) { return Float.isNaN(value) || Float.isInfinite(value) ? min : Math.max(min, Math.min(max, value)); }
    private static String safe(Throwable error) { return error == null ? "unknown" : error.getClass().getSimpleName() + "_" + String.valueOf(error.getMessage()).replace(' ', '_').toLowerCase(Locale.US); }
}
