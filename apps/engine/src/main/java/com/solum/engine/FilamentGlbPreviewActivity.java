package com.solum.engine;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.filament.ColorGrading;
import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.LightManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Skybox;
import com.google.android.filament.View.AmbientOcclusion;
import com.google.android.filament.View.AntiAliasing;
import com.google.android.filament.View.QualityLevel;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.utils.Float3;
import com.google.android.filament.utils.Manipulator;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Locale;

import kotlin.jvm.functions.Function1;

public class FilamentGlbPreviewActivity extends Activity {
    public static final String EXTRA_MODEL_PATH = "com.solum.engine.extra.MODEL_PATH";
    public static final String EXTRA_MODEL_NAME = "com.solum.engine.extra.MODEL_NAME";

    private static final String PREFS_NAME = "solum_engine_diagnostics";
    private static final String PREF_ACTIVE_MODEL_LOCAL_PATH = "active_model_local_path";
    private static final String PREF_ACTIVE_MODEL_PATH = "active_model_path";
    private static final long HUD_UPDATE_NS = 250_000_000L;

    private SurfaceView surfaceView;
    private ModelViewer modelViewer;
    private IndirectLight indirectLight;
    private Skybox skybox;
    private ColorGrading colorGrading;
    private int fillLightEntity = 0;
    private TextView hudView;
    private TextView statusView;
    private Button qualityButton;
    private Button lightingButton;
    private final Choreographer.FrameCallback frameCallback = this::doFrame;

    private FilamentQualityProfile qualityProfile = FilamentQualityProfile.MEDIUM;
    private LightingPreset lightingPreset = LightingPreset.STUDIO;
    private boolean frameCallbackActive = false;
    private boolean destroying = false;
    private boolean destroyed = false;
    private boolean returningToVulkan = false;
    private long lastFrameNs = 0L;
    private long lastHudUpdateNs = 0L;
    private float rollingFrameMs = 0.0f;
    private float rollingFps = 0.0f;
    private float rollingRenderCpuMs = 0.0f;
    private long liveFrameCounter = 0L;
    private String modelPath = "";
    private String modelName = "";
    private String loadStatus = "not_started";
    private String lifecycleStatus = "created";
    private String lastLifecycleError = "none";
    private String qualityFeatureStatus = "medium_mobile_safe";
    private String environmentMode = "procedural_neutral_fallback";
    private String iblStatus = "fallback_no_hdr_asset";
    private String realIblReady = "false";
    private String futureIblAssetPath = "none";
    private String cameraStatus = "orbit_drag_pinch_zoom_unit_cube";
    private String lightingStatus = "not_applied";
    private String legacyVulkanReturnStatus = "not_requested";
    private String actualAA = "FXAA";
    private int actualSampleCount = 2;
    private float dynamicMinScale = 0.72f;
    private float dynamicMaxScale = 0.95f;
    private boolean aoEnabled = false;
    private boolean bloomEnabled = false;
    private boolean shadowsEnabled = false;
    private boolean refractionEnabled = false;
    private float sunLightIntensity = 22_000.0f;
    private float indirectLightIntensity = 11_000.0f;
    private float fillLightIntensity = 3_500.0f;
    private float exposure = 0.92f;

    private enum FilamentQualityProfile {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH_PREVIEW("High Preview");

        final String label;

        FilamentQualityProfile(String label) {
            this.label = label;
        }

        FilamentQualityProfile next() {
            if (this == LOW) return MEDIUM;
            if (this == MEDIUM) return HIGH_PREVIEW;
            return LOW;
        }
    }

    private enum LightingPreset {
        STUDIO("Studio", new float[] {-0.35f, -0.75f, -0.55f}, 22_000.0f, 11_000.0f, 0.92f, new float[] {0.035f, 0.041f, 0.046f, 1.0f}, 3_500.0f, new float[] {0.70f, -0.35f, -0.62f}),
        BRIGHT("Bright", new float[] {-0.20f, -0.82f, -0.48f}, 30_000.0f, 15_000.0f, 1.00f, new float[] {0.045f, 0.052f, 0.058f, 1.0f}, 5_500.0f, new float[] {0.62f, -0.42f, -0.66f}),
        CINEMATIC("Cinematic", new float[] {-0.62f, -0.62f, -0.48f}, 15_000.0f, 8_000.0f, 0.82f, new float[] {0.022f, 0.026f, 0.033f, 1.0f}, 2_000.0f, new float[] {0.48f, -0.32f, -0.82f}),
        NEUTRAL("Neutral", new float[] {-0.35f, -0.82f, -0.45f}, 17_000.0f, 9_000.0f, 0.90f, new float[] {0.030f, 0.035f, 0.040f, 1.0f}, 2_200.0f, new float[] {0.65f, -0.30f, -0.70f});

        final String label;
        final float[] sunDirection;
        final float sunIntensity;
        final float indirectIntensity;
        final float exposure;
        final float[] skyColor;
        final float fillIntensity;
        final float[] fillDirection;

        LightingPreset(String label, float[] sunDirection, float sunIntensity, float indirectIntensity, float exposure, float[] skyColor, float fillIntensity, float[] fillDirection) {
            this.label = label;
            this.sunDirection = sunDirection;
            this.sunIntensity = sunIntensity;
            this.indirectIntensity = indirectIntensity;
            this.exposure = exposure;
            this.skyColor = skyColor;
            this.fillIntensity = fillIntensity;
            this.fillDirection = fillDirection;
        }

        LightingPreset next() {
            if (this == STUDIO) return BRIGHT;
            if (this == BRIGHT) return CINEMATIC;
            if (this == CINEMATIC) return NEUTRAL;
            return STUDIO;
        }
    }

    static {
        Utils.init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleStatus = "created";
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        modelPath = resolveModelPath();
        modelName = getIntent().getStringExtra(EXTRA_MODEL_NAME);
        if (modelName == null || modelName.isEmpty()) modelName = modelPath.isEmpty() ? "none" : new File(modelPath).getName();

        FrameLayout root = new FrameLayout(this);
        surfaceView = new SurfaceView(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        hudView = overlayText(11.0f, 3);
        FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hudParams.gravity = Gravity.TOP;
        root.addView(hudView, hudParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(10), dp(8), dp(10), dp(8));
        controls.setBackgroundColor(Color.argb(172, 4, 12, 16));
        qualityButton = button("Quality: " + qualityProfile.label);
        qualityButton.setOnClickListener(v -> {
            qualityProfile = qualityProfile.next();
            applyQualityProfile();
            updateHud();
        });
        lightingButton = button("Lighting: " + lightingPreset.label);
        lightingButton.setOnClickListener(v -> {
            lightingPreset = lightingPreset.next();
            applyLightingPreset();
            updateHud();
        });
        Button reloadButton = button("Reload");
        reloadButton.setOnClickListener(v -> loadModel());
        Button closeButton = button("Close Preview");
        closeButton.setOnClickListener(v -> closePreview());
        statusView = overlayText(10.0f, 12);
        controls.addView(qualityButton);
        controls.addView(lightingButton);
        controls.addView(reloadButton);
        controls.addView(closeButton);
        controls.addView(statusView);
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        controlParams.gravity = Gravity.BOTTOM;
        controlParams.setMargins(dp(12), dp(12), dp(12), dp(28));
        root.addView(controls, controlParams);

        setContentView(root);
        createViewer();
        loadModel();
        updateHud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!destroying && !destroyed && !returningToVulkan) startFrames();
    }

    @Override
    protected void onPause() {
        stopFrames("paused");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        closePreview();
    }

    @Override
    protected void onDestroy() {
        stopFrames(returningToVulkan ? "close_preview_destroyed" : "destroyed");
        releaseFilamentResources();
        super.onDestroy();
    }

    private void createViewer() {
        try {
            UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
            Manipulator manipulator = new Manipulator.Builder()
                .viewport(Math.max(1, surfaceView.getWidth()), Math.max(1, surfaceView.getHeight()))
                .targetPosition(0.0f, 0.0f, 0.0f)
                .orbitHomePosition(0.0f, 0.0f, 4.4f)
                .zoomSpeed(0.010f)
                .build(Manipulator.Mode.ORBIT);
            modelViewer = new ModelViewer(surfaceView, Engine.create(), uiHelper, manipulator);
            surfaceView.setOnTouchListener((view, event) -> {
                if (destroying || destroyed || modelViewer == null) return true;
                modelViewer.onTouchEvent(event);
                return true;
            });
            createEnvironmentFallback();
            applyQualityProfile();
            applyLightingPreset();
            lifecycleStatus = "viewer_created";
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lifecycleStatus = "create_failed";
        }
    }

    private void closePreview() {
        returningToVulkan = true;
        legacyVulkanReturnStatus = "known_legacy_return_untested_close_preview_finishes_filament_activity";
        lifecycleStatus = "close_preview_requested";
        stopFrames("close_preview_stop_frames");
        if (surfaceView != null) surfaceView.setOnTouchListener(null);
        releaseFilamentResources();
        lifecycleStatus = "close_preview_finish";
        finish();
    }

    private void startFrames() {
        if (frameCallbackActive || modelViewer == null) return;
        frameCallbackActive = true;
        lifecycleStatus = "running";
        lastFrameNs = 0L;
        lastHudUpdateNs = 0L;
        Choreographer.getInstance().postFrameCallback(frameCallback);
        updateHud();
    }

    private void stopFrames(String status) {
        if (frameCallbackActive) {
            frameCallbackActive = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
        lifecycleStatus = status;
        updateHud();
    }

    private void doFrame(long frameTimeNanos) {
        if (!frameCallbackActive || destroying || destroyed || modelViewer == null) return;
        try {
            updateFrameTiming(frameTimeNanos);
            if (modelViewer.getAnimator() != null && modelViewer.getAnimator().getAnimationCount() > 0) {
                float seconds = frameTimeNanos / 1_000_000_000.0f;
                modelViewer.getAnimator().applyAnimation(0, seconds);
                modelViewer.getAnimator().updateBoneMatrices();
            }
            long renderStartNs = System.nanoTime();
            modelViewer.render(frameTimeNanos);
            long renderEndNs = System.nanoTime();
            liveFrameCounter += 1L;
            updateRenderCpuTiming((renderEndNs - renderStartNs) / 1_000_000.0f);
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            loadStatus = "render_error: " + lastLifecycleError;
            stopFrames("render_error");
            return;
        }
        if (frameCallbackActive && !destroying && !destroyed) {
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    private void updateFrameTiming(long frameTimeNanos) {
        if (lastFrameNs > 0L) {
            float instantMs = (frameTimeNanos - lastFrameNs) / 1_000_000.0f;
            if (instantMs > 0.0f && instantMs < 250.0f) {
                rollingFrameMs = rollingFrameMs <= 0.0f ? instantMs : (rollingFrameMs * 0.82f + instantMs * 0.18f);
                rollingFps = 1000.0f / Math.max(1.0f, rollingFrameMs);
            }
        }
        lastFrameNs = frameTimeNanos;
        if (lastHudUpdateNs == 0L || frameTimeNanos - lastHudUpdateNs >= HUD_UPDATE_NS) {
            lastHudUpdateNs = frameTimeNanos;
            updateHud();
        }
    }

    private void updateRenderCpuTiming(float cpuMs) {
        if (cpuMs >= 0.0f && cpuMs < 250.0f) {
            rollingRenderCpuMs = rollingRenderCpuMs <= 0.0f ? cpuMs : (rollingRenderCpuMs * 0.82f + cpuMs * 0.18f);
        }
    }

    private void loadModel() {
        if (modelViewer == null) {
            loadStatus = "viewer_not_ready";
            updateHud();
            return;
        }
        try {
            modelViewer.destroyModel();
            if (modelPath == null || modelPath.isEmpty()) {
                loadStatus = "no_active_glb_or_gltf";
                updateHud();
                return;
            }
            File file = new File(modelPath);
            if (!file.isFile()) {
                loadStatus = "file_missing: " + modelPath;
                updateHud();
                return;
            }
            ByteBuffer data = readFile(file);
            String lower = file.getName().toLowerCase(Locale.US);
            if (lower.endsWith(".glb")) {
                modelViewer.loadModelGlb(data);
            } else if (lower.endsWith(".gltf")) {
                File baseDir = file.getParentFile();
                modelViewer.loadModelGltf(data, (Function1<String, Buffer>) uri -> readSiblingResource(baseDir, uri));
            } else {
                loadStatus = "unsupported_extension_expected_glb_or_gltf";
                updateHud();
                return;
            }
            modelViewer.transformToUnitCube(new Float3(0.0f, 0.0f, 0.0f));
            cameraStatus = "orbit_drag_pinch_zoom_unit_cube_autofit";
            loadStatus = "ok_loaded_with_gltfio";
        } catch (Throwable t) {
            loadStatus = "load_error: " + shortMessage(t);
        }
        updateHud();
    }

    private Buffer readSiblingResource(File baseDir, String uri) {
        try {
            if (baseDir == null || uri == null || uri.contains("..")) return null;
            File resource = new File(baseDir, uri);
            if (!resource.isFile()) return null;
            return readFile(resource);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void createEnvironmentFallback() {
        if (modelViewer == null) return;
        Engine engine = modelViewer.getEngine();
        indirectLight = new IndirectLight.Builder()
            .intensity(indirectLightIntensity)
            .build(engine);
        skybox = new Skybox.Builder()
            .color(lightingPreset.skyColor)
            .build(engine);
        modelViewer.getScene().setIndirectLight(indirectLight);
        modelViewer.getScene().setSkybox(skybox);
        fillLightEntity = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .castShadows(false)
            .direction(lightingPreset.fillDirection[0], lightingPreset.fillDirection[1], lightingPreset.fillDirection[2])
            .color(0.86f, 0.92f, 1.0f)
            .intensity(lightingPreset.fillIntensity)
            .build(engine, fillLightEntity);
        modelViewer.getScene().addEntity(fillLightEntity);
    }

    private void applyLightingPreset() {
        if (modelViewer == null) return;
        try {
            indirectLightIntensity = lightingPreset.indirectIntensity;
            sunLightIntensity = lightingPreset.sunIntensity;
            fillLightIntensity = lightingPreset.fillIntensity;
            exposure = lightingPreset.exposure;
            if (indirectLight != null) indirectLight.setIntensity(indirectLightIntensity);
            if (skybox != null) skybox.setColor(lightingPreset.skyColor);

            Engine engine = modelViewer.getEngine();
            LightManager lights = engine.getLightManager();
            int sunInstance = lights.getInstance(modelViewer.getLight());
            if (sunInstance != 0) {
                lights.setDirection(sunInstance, lightingPreset.sunDirection[0], lightingPreset.sunDirection[1], lightingPreset.sunDirection[2]);
                lights.setIntensity(sunInstance, lightingPreset.sunIntensity);
                lights.setColor(sunInstance, 1.0f, 0.96f, 0.90f);
                lights.setShadowCaster(sunInstance, shadowsEnabled);
            }
            if (fillLightEntity != 0) {
                int fillInstance = lights.getInstance(fillLightEntity);
                if (fillInstance != 0) {
                    lights.setDirection(fillInstance, lightingPreset.fillDirection[0], lightingPreset.fillDirection[1], lightingPreset.fillDirection[2]);
                    lights.setIntensity(fillInstance, lightingPreset.fillIntensity);
                    lights.setColor(fillInstance, 0.86f, 0.92f, 1.0f);
                    lights.setShadowCaster(fillInstance, false);
                }
            }
            modelViewer.getCamera().setExposure(exposure);
            Renderer.ClearOptions clear = modelViewer.getRenderer().getClearOptions();
            clear.clear = true;
            clear.discard = true;
            clear.clearColor = lightingPreset.skyColor;
            modelViewer.getRenderer().setClearOptions(clear);
            lightingStatus = "applied_mobile_safe_sun_fill_indirect_exposure_clear";
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        if (lightingButton != null) lightingButton.setText("Lighting: " + lightingPreset.label);
    }

    private void applyQualityProfile() {
        if (modelViewer == null) return;
        try {
            com.google.android.filament.View view = modelViewer.getView();
            com.google.android.filament.View.AmbientOcclusionOptions ao = view.getAmbientOcclusionOptions();
            com.google.android.filament.View.BloomOptions bloom = view.getBloomOptions();
            com.google.android.filament.View.DynamicResolutionOptions dynamic = view.getDynamicResolutionOptions();
            com.google.android.filament.View.RenderQuality renderQuality = view.getRenderQuality();
            if (qualityProfile == FilamentQualityProfile.LOW) {
                view.setAntiAliasing(AntiAliasing.NONE);
                view.setSampleCount(1);
                view.setAmbientOcclusion(AmbientOcclusion.NONE);
                view.setShadowingEnabled(false);
                view.setScreenSpaceRefractionEnabled(false);
                view.setPostProcessingEnabled(true);
                ao.enabled = false;
                bloom.enabled = false;
                dynamic.enabled = true;
                dynamic.minScale = 0.58f;
                dynamic.maxScale = 0.82f;
                dynamic.quality = QualityLevel.LOW;
                renderQuality.hdrColorBuffer = QualityLevel.LOW;
                setActualQualityStatus("NONE", 1, 0.58f, 0.82f, false, false, false, false, "low_mobile_safe_dynamic_0_58_0_82_no_aa_no_ao_no_bloom_no_shadows_no_refraction");
            } else if (qualityProfile == FilamentQualityProfile.HIGH_PREVIEW) {
                view.setAntiAliasing(AntiAliasing.FXAA);
                view.setSampleCount(4);
                view.setAmbientOcclusion(AmbientOcclusion.SSAO);
                view.setShadowingEnabled(false);
                view.setScreenSpaceRefractionEnabled(false);
                view.setPostProcessingEnabled(true);
                ao.enabled = true;
                ao.quality = QualityLevel.LOW;
                ao.intensity = 0.18f;
                bloom.enabled = false;
                bloom.strength = 0.0f;
                bloom.quality = QualityLevel.LOW;
                dynamic.enabled = true;
                dynamic.minScale = 0.86f;
                dynamic.maxScale = 1.00f;
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.HIGH;
                setActualQualityStatus("FXAA", 4, 0.86f, 1.00f, true, false, false, false, "high_preview_mobile_safe_dynamic_0_86_1_00_fxaa_msaa4_ssao_low_no_bloom_no_shadows_no_refraction");
            } else {
                view.setAntiAliasing(AntiAliasing.FXAA);
                view.setSampleCount(2);
                view.setAmbientOcclusion(AmbientOcclusion.NONE);
                view.setShadowingEnabled(false);
                view.setScreenSpaceRefractionEnabled(false);
                view.setPostProcessingEnabled(true);
                ao.enabled = false;
                bloom.enabled = false;
                dynamic.enabled = true;
                dynamic.minScale = 0.72f;
                dynamic.maxScale = 0.95f;
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.MEDIUM;
                setActualQualityStatus("FXAA", 2, 0.72f, 0.95f, false, false, false, false, "medium_mobile_safe_dynamic_0_72_0_95_fxaa_msaa2_no_ao_no_bloom_no_shadows_no_refraction");
            }
            view.setAmbientOcclusionOptions(ao);
            view.setBloomOptions(bloom);
            view.setDynamicResolutionOptions(dynamic);
            view.setRenderQuality(renderQuality);
            applyColorGrading();
            applyLightingPreset();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            qualityFeatureStatus = "apply_failed";
        }
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
    }

    private void setActualQualityStatus(String aa, int sampleCount, float minScale, float maxScale, boolean ao, boolean bloom, boolean shadows, boolean refraction, String status) {
        actualAA = aa;
        actualSampleCount = sampleCount;
        dynamicMinScale = minScale;
        dynamicMaxScale = maxScale;
        aoEnabled = ao;
        bloomEnabled = bloom;
        shadowsEnabled = shadows;
        refractionEnabled = refraction;
        qualityFeatureStatus = status;
    }

    private void applyColorGrading() {
        if (modelViewer == null) return;
        Engine engine = modelViewer.getEngine();
        if (colorGrading != null) {
            modelViewer.getView().setColorGrading(null);
            engine.destroyColorGrading(colorGrading);
            colorGrading = null;
        }
        float gradeExposure = 0.0f;
        colorGrading = new ColorGrading.Builder()
            .quality(qualityProfile == FilamentQualityProfile.HIGH_PREVIEW ? ColorGrading.QualityLevel.MEDIUM : ColorGrading.QualityLevel.LOW)
            .exposure(gradeExposure)
            .contrast(1.0f)
            .saturation(1.0f)
            .build(engine);
        modelViewer.getView().setColorGrading(colorGrading);
    }

    private void releaseFilamentResources() {
        if (destroyed || destroying) return;
        destroying = true;
        lifecycleStatus = returningToVulkan ? "close_preview_releasing_filament" : "destroyed";
        try {
            if (modelViewer != null) {
                Engine engine = modelViewer.getEngine();
                if (modelViewer.getScene() != null) {
                    if (fillLightEntity != 0) modelViewer.getScene().removeEntity(fillLightEntity);
                    modelViewer.getScene().setIndirectLight(null);
                    modelViewer.getScene().setSkybox(null);
                }
                if (colorGrading != null) {
                    modelViewer.getView().setColorGrading(null);
                    engine.destroyColorGrading(colorGrading);
                    colorGrading = null;
                }
                if (fillLightEntity != 0) {
                    engine.getLightManager().destroy(fillLightEntity);
                    EntityManager.get().destroy(fillLightEntity);
                    fillLightEntity = 0;
                }
                if (indirectLight != null) {
                    engine.destroyIndirectLight(indirectLight);
                    indirectLight = null;
                }
                if (skybox != null) {
                    engine.destroySkybox(skybox);
                    skybox = null;
                }
                modelViewer.destroyModel();
                modelViewer.destroy();
                modelViewer = null;
            }
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lifecycleStatus = "destroy_error";
        } finally {
            destroyed = true;
            destroying = false;
        }
    }

    private String resolveModelPath() {
        String explicit = getIntent().getStringExtra(EXTRA_MODEL_PATH);
        if (explicit != null && !explicit.isEmpty()) return explicit;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String local = prefs.getString(PREF_ACTIVE_MODEL_LOCAL_PATH, "");
        if (local != null && !local.isEmpty()) return local;
        String path = prefs.getString(PREF_ACTIVE_MODEL_PATH, "");
        return path == null ? "" : path;
    }

    private static ByteBuffer readFile(File file) throws Exception {
        long length = file.length();
        if (length <= 0L || length > Integer.MAX_VALUE) throw new IllegalArgumentException("invalid_file_size_" + length);
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) length);
        byte[] chunk = new byte[64 * 1024];
        try (FileInputStream input = new FileInputStream(file)) {
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.put(chunk, 0, read);
            }
        }
        buffer.flip();
        return buffer;
    }

    private void updateHud() {
        if (hudView != null) {
            hudView.setText("Filament Preview | FPS " + oneDecimal(rollingFps) + " | " + oneDecimal(rollingFrameMs)
                + " ms | CPU " + oneDecimal(rollingRenderCpuMs) + " ms | frame " + liveFrameCounter
                + " | " + qualityProfile.label + " | " + lightingPreset.label);
        }
        if (statusView != null) {
            statusView.setText("Model: " + (modelName == null || modelName.isEmpty() ? "none" : modelName)
                + "\nLoad: " + loadStatus
                + "\nCamera: " + cameraStatus
                + "\nLight preset: " + lightingPreset.label + " / " + lightingStatus
                + "\nExposure/sun/indirect/fill: " + oneDecimal(exposure) + " / " + noDecimal(sunLightIntensity) + " / " + noDecimal(indirectLightIntensity) + " / " + noDecimal(fillLightIntensity)
                + "\nIBL: " + iblStatus + " / " + environmentMode + " / realIblReady=" + realIblReady + " / future=" + futureIblAssetPath
                + "\nQuality: " + qualityFeatureStatus
                + "\nactualAA=" + actualAA + " sampleCount=" + actualSampleCount + " dynamicResolution=" + twoDecimal(dynamicMinScale) + "-" + twoDecimal(dynamicMaxScale)
                + "\naoEnabled=" + aoEnabled + " bloomEnabled=" + bloomEnabled + " shadowsEnabled=" + shadowsEnabled + " refractionEnabled=" + refractionEnabled
                + "\nLifecycle: " + lifecycleStatus
                + "\nlegacyVulkanReturnStatus: " + legacyVulkanReturnStatus
                + "\nlastLifecycleError: " + lastLifecycleError);
        }
    }

    private TextView overlayText(float sizeSp, int maxLines) {
        TextView view = new TextView(this);
        view.setTextColor(Color.rgb(220, 245, 250));
        view.setTextSize(sizeSp);
        view.setMaxLines(maxLines);
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackgroundColor(Color.argb(170, 3, 12, 17));
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.rgb(218, 248, 255));
        button.setTextSize(11.0f);
        button.setMinHeight(dp(48));
        button.setBackgroundColor(Color.argb(210, 4, 24, 30));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String noDecimal(float value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private static String twoDecimal(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String shorten(String value, int max) {
        if (value == null || value.isEmpty()) return "none";
        if (value.length() <= max) return value;
        return "..." + value.substring(value.length() - Math.max(4, max - 3));
    }

    private static String shortMessage(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isEmpty()) message = t.getClass().getSimpleName();
        return shorten(message, 96);
    }
}
