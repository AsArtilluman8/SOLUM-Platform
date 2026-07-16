package com.solum.engine.environment.p63;

import android.content.res.AssetManager;

import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.Material;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Scene;
import com.google.android.filament.VertexBuffer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Locale;

/** One persistent full-screen triangle and one material instance for all P63.3 sky visuals. */
public final class SolumAnalyticSkyRenderer {
    private final Engine engine;
    private final Scene scene;
    private final AssetManager assets;
    private Material material;
    private MaterialInstance instance;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private SolumAnalyticSkyResources resources;
    private int entity;
    private int renderableInstance;
    private boolean active;
    private boolean available;
    private String lastError = "none";
    private int materialBuildCount;
    private int materialRebuildCount;
    private long uniformUpdateCount;

    public SolumAnalyticSkyRenderer(AssetManager assets, Engine engine, Scene scene) {
        if (assets == null || engine == null || scene == null) {
            throw new IllegalArgumentException("analytic_sky_renderer_dependency_missing");
        }
        this.assets = assets;
        this.engine = engine;
        this.scene = scene;
        initialize();
    }

    public boolean update(SolumAnalyticSkyState state) {
        if (state == null) return false;
        state.materialBuildCount = materialBuildCount;
        state.materialRebuildCount = materialRebuildCount;
        state.uniformUpdateCount = uniformUpdateCount;
        state.lastSkyError = lastError;
        state.moonSource = resources == null ? "UNAVAILABLE" : resources.moonSource;
        state.starSource = resources == null ? "SOLUM_NATIVE_PROCEDURAL" : resources.starSource;
        state.cloudSource = "SOLUM_NATIVE_PROCEDURAL_SPHERICAL_SHELL";
        state.resourceProvenance = (resources == null ? "UNAVAILABLE" : resources.moonSource)
            + ";" + (resources == null ? "SOLUM_NATIVE_PROCEDURAL" : resources.starSource)
            + ";FILAMENT_ADAPTED;SOLUM_NATIVE";
        state.materialVariant = variantName(state.cloudQuality);

        if (!available || !state.analyticSky) {
            setActive(false);
            state.activeRenderer = available ? "analytic_disabled" : "legacy_fallback_material_unavailable";
            state.skyDrawCalls = 0;
            return false;
        }
        try {
            applyUniforms(state);
            setActive(true);
            state.activeRenderer = "SOLUM_ANALYTIC_SKY_SINGLE_PASS";
            state.skyDrawCalls = 1;
            state.uniformUpdateCount = uniformUpdateCount;
            state.lastSkyError = "none";
            return true;
        } catch (Throwable error) {
            lastError = "uniform_update_failed_" + safe(error);
            available = false;
            setActive(false);
            state.lastSkyError = lastError;
            state.activeRenderer = "legacy_fallback_uniform_error";
            state.skyDrawCalls = 0;
            return false;
        }
    }

    public boolean isAvailable() { return available; }
    public boolean isActive() { return active; }
    public String getStatus() {
        return (active ? "active" : (available ? "ready_hidden" : "unavailable"))
            + " variant=mobile_single_package builds=" + materialBuildCount
            + " rebuilds=" + materialRebuildCount + " uniforms=" + uniformUpdateCount
            + " error=" + lastError;
    }

    public void release() {
        setActive(false);
        try { if (entity != 0) scene.removeEntity(entity); } catch (Throwable ignored) { }
        try { if (entity != 0) engine.getRenderableManager().destroy(entity); } catch (Throwable ignored) { }
        if (instance != null) {
            try { engine.destroyMaterialInstance(instance); } catch (Throwable ignored) { }
            instance = null;
        }
        if (material != null) {
            try { engine.destroyMaterial(material); } catch (Throwable ignored) { }
            material = null;
        }
        if (vertexBuffer != null) {
            try { engine.destroyVertexBuffer(vertexBuffer); } catch (Throwable ignored) { }
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            try { engine.destroyIndexBuffer(indexBuffer); } catch (Throwable ignored) { }
            indexBuffer = null;
        }
        if (resources != null) {
            resources.release();
            resources = null;
        }
        if (entity != 0) {
            try { EntityManager.get().destroy(entity); } catch (Throwable ignored) { }
            entity = 0;
        }
        renderableInstance = 0;
        available = false;
    }

    private void initialize() {
        try {
            ByteBuffer packageBuffer = readAsset(SolumAnalyticSkyMaterial.ASSET_PATH);
            material = new Material.Builder().payload(packageBuffer, packageBuffer.remaining()).build(engine);
            instance = material.createInstance();
            materialBuildCount = 1;
            resources = new SolumAnalyticSkyResources(assets, engine);
            instance.setParameter("moonAlbedo", resources.moonAlbedo, resources.moonSampler);
            instance.setParameter("moonNormal", resources.moonNormal, resources.moonSampler);
            instance.setParameter("realStars", resources.realStars, resources.starSampler);
            instance.setParameter("tilingStars", resources.tilingStars, resources.starSampler);

            FloatBuffer vertices = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(-1.0f).put(-1.0f).put(3.0f).put(-1.0f).put(-1.0f).put(3.0f).flip();
            ShortBuffer indices = ByteBuffer.allocateDirect(3 * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
            indices.put((short)0).put((short)1).put((short)2).flip();
            vertexBuffer = new VertexBuffer.Builder().vertexCount(3).bufferCount(1)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT2, 0, 8)
                .build(engine);
            vertexBuffer.setBufferAt(engine, 0, vertices);
            indexBuffer = new IndexBuffer.Builder().indexCount(3)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT).build(engine);
            indexBuffer.setBuffer(engine, indices);
            entity = EntityManager.get().create();
            new RenderableManager.Builder(1)
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
                .material(0, instance)
                .culling(false)
                .castShadows(false)
                .receiveShadows(false)
                .priority(7)
                .build(engine, entity);
            scene.addEntity(entity);
            renderableInstance = engine.getRenderableManager().getInstance(entity);
            setActive(false);
            available = renderableInstance != 0;
            lastError = available ? "none" : "renderable_instance_missing";
        } catch (Throwable error) {
            lastError = "material_initialize_failed_" + safe(error);
            available = false;
            releasePartialAfterFailure();
        }
    }

    private void applyUniforms(SolumAnalyticSkyState state) {
        instance.setParameter("sunDirection", state.sunDirection[0], state.sunDirection[1], state.sunDirection[2]);
        instance.setParameter("moonDirection", state.moonDirection[0], state.moonDirection[1], state.moonDirection[2]);
        instance.setParameter("moonToSunDirection", state.moonToSunDirection[0], state.moonToSunDirection[1], state.moonToSunDirection[2]);
        instance.setParameter("atmosphere0", state.turbidity, state.rayleigh, state.mie, state.mieG);
        instance.setParameter("atmosphere1", state.ozone, state.horizonHaze, state.nightFloor, state.horizonWarmth);
        instance.setParameter("atmosphereArt", state.sunsetSaturation, state.sunsetContrast, 0.0f, 0.0f);
        instance.setParameter("skyArtTint", state.skyArtTint[0], state.skyArtTint[1], state.skyArtTint[2]);

        float sunRadius = (float)Math.toRadians(state.sunAngularDiameterDegrees * 0.5f);
        instance.setParameter("sun0", sunRadius, state.sunDiscLuminanceNits, state.sunLimbDarkening,
            state.analyticSun ? 1.0f : 0.0f);
        instance.setParameter("sun1", state.sunHaloSize, state.sunHaloFalloff,
            state.sunBloomContribution, state.sunExposureWeight);
        instance.setParameter("sunTint", state.sunTint[0], state.sunTint[1], state.sunTint[2]);

        float moonRadius = (float)Math.toRadians(state.moonAngularDiameterDegrees * 0.5f);
        instance.setParameter("moon0", moonRadius, state.moonVisualLuminanceNits, state.moonEarthshine,
            state.analyticMoon ? 1.0f : 0.0f);
        instance.setParameter("moon1", state.moonNormalStrength, state.moonHalo,
            state.moonPhaseAngleDegrees, 0.0f);
        instance.setParameter("moonTint", state.moonTint[0], state.moonTint[1], state.moonTint[2]);

        instance.setParameter("stars0", state.starDensity, state.starBrightness,
            state.starLimitingMagnitude, state.starSize);
        instance.setParameter("stars1", state.starTwinkle, state.milkyWayIntensity,
            state.milkyWaySaturation, state.analyticStars ? 1.0f : 0.0f);
        instance.setParameter("starTint", state.starTint[0], state.starTint[1], state.starTint[2]);
        instance.setParameter("siderealRotation", state.siderealRotationDegrees);
        instance.setParameter("starTextureAvailable", resources.starTextureAvailable);

        instance.setParameter("cloud0", state.cloudCoverage, state.cloudDensity, state.cloudSoftness,
            state.analyticClouds ? 1.0f : 0.0f);
        instance.setParameter("cloud1", state.cloudHeightKm, state.cloudThicknessKm,
            state.cloudErosion, state.cloudWindSpeed);
        instance.setParameter("cloud2", state.cloudEvolution, state.cloudSilverLining,
            state.cloudBrightness, SolumAnalyticSkyMaterial.qualityIndex(state.cloudQuality));
        instance.setParameter("cloudArtTint", state.cloudArtTint[0], state.cloudArtTint[1], state.cloudArtTint[2]);
        instance.setParameter("elapsedSeconds", state.elapsedSeconds);
        uniformUpdateCount++;
    }

    private void setActive(boolean enabled) {
        active = enabled && available;
        if (renderableInstance == 0) return;
        try { engine.getRenderableManager().setLayerMask(renderableInstance, 0xff, active ? 0x01 : 0x00); }
        catch (Throwable ignored) { active = false; }
    }

    private ByteBuffer readAsset(String path) throws Exception {
        try (InputStream input = assets.open(path, AssetManager.ACCESS_STREAMING);
             ByteArrayOutputStream output = new ByteArrayOutputStream(192 * 1024)) {
            byte[] block = new byte[16 * 1024];
            int read;
            while ((read = input.read(block)) >= 0) {
                if (read > 0) output.write(block, 0, read);
                if (output.size() > 4 * 1024 * 1024) throw new IllegalArgumentException("analytic_sky_material_too_large");
            }
            byte[] bytes = output.toByteArray();
            if (bytes.length == 0) throw new IllegalArgumentException("analytic_sky_material_empty");
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
            buffer.put(bytes).flip();
            return buffer;
        }
    }

    private void releasePartialAfterFailure() {
        try { if (entity != 0) scene.removeEntity(entity); } catch (Throwable ignored) { }
        try { if (entity != 0) engine.getRenderableManager().destroy(entity); } catch (Throwable ignored) { }
        if (resources != null) { resources.release(); resources = null; }
        if (instance != null) { try { engine.destroyMaterialInstance(instance); } catch (Throwable ignored) { } instance = null; }
        if (material != null) { try { engine.destroyMaterial(material); } catch (Throwable ignored) { } material = null; }
        if (vertexBuffer != null) { try { engine.destroyVertexBuffer(vertexBuffer); } catch (Throwable ignored) { } vertexBuffer = null; }
        if (indexBuffer != null) { try { engine.destroyIndexBuffer(indexBuffer); } catch (Throwable ignored) { } indexBuffer = null; }
        if (entity != 0) { try { EntityManager.get().destroy(entity); } catch (Throwable ignored) { } entity = 0; }
        renderableInstance = 0;
    }

    private static String variantName(String quality) {
        if ("High Experimental".equals(quality)) return "analytic_sky_mobile_high_experimental";
        if ("Medium".equals(quality)) return "analytic_sky_mobile_medium";
        return "analytic_sky_mobile_low";
    }

    private static String safe(Throwable error) {
        String message = error == null ? "unknown" : String.valueOf(error.getMessage());
        return (error == null ? "Throwable" : error.getClass().getSimpleName()) + "_"
            + message.replace(' ', '_').toLowerCase(Locale.US);
    }
}
