package com.solum.engine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;

import com.google.android.filament.ColorGrading;
import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.LightManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Skybox;
import com.google.android.filament.Texture;
import com.google.android.filament.View.AmbientOcclusion;
import com.google.android.filament.View.AntiAliasing;
import com.google.android.filament.View.QualityLevel;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.utils.Float3;
import com.google.android.filament.utils.HDRLoader;
import com.google.android.filament.utils.IBLPrefilterContext;
import com.google.android.filament.utils.KTX1Loader;
import com.google.android.filament.utils.Manipulator;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.jvm.functions.Function1;

public class FilamentGlbPreviewActivity extends Activity {
    public static final String EXTRA_MODEL_PATH = "com.solum.engine.extra.MODEL_PATH";
    public static final String EXTRA_MODEL_NAME = "com.solum.engine.extra.MODEL_NAME";

    private static final String PREFS_NAME = "solum_engine_diagnostics";
    private static final String PREF_ACTIVE_MODEL_LOCAL_PATH = "active_model_local_path";
    private static final String PREF_ACTIVE_MODEL_PATH = "active_model_path";
    private static final String PREF_ACTIVE_MODEL_NAME = "active_model_name";
    private static final String PREF_ACTIVE_IBL_PATH = "active_ibl_path";
    private static final String PREF_ACTIVE_IBL_NAME = "active_ibl_name";
    private static final int REQUEST_IMPORT_MODEL = 4101;
    private static final int REQUEST_IMPORT_IBL = 4102;
    private static final long HUD_UPDATE_NS = 250_000_000L;

    private SurfaceView surfaceView;
    private ModelViewer modelViewer;
    private IndirectLight indirectLight;
    private Skybox skybox;
    private ColorGrading colorGrading;
    private final List<Texture> iblOwnedTextures = new ArrayList<>();
    private int fillLightEntity = 0;
    private TextView hudView;
    private TextView statusView;
    private Button qualityButton;
    private Button lightingButton;
    private Button iblButton;
    private Button advancedValuesButton;
    private Button aoButton;
    private Button bloomButton;
    private Button shadowsButton;
    private Button refractionButton;
    private LinearLayout advancedValuesPanel;
    private TextView sunSliderLabel;
    private TextView ambientSliderLabel;
    private TextView fillSliderLabel;
    private TextView exposureSliderLabel;
    private TextView backgroundSliderLabel;
    private EditText advancedSunField;
    private EditText advancedAmbientField;
    private EditText advancedFillField;
    private EditText advancedExposureField;
    private EditText advancedBackgroundField;
    private SeekBar sunSlider;
    private SeekBar ambientSlider;
    private SeekBar fillSlider;
    private SeekBar exposureSlider;
    private SeekBar backgroundSlider;
    private final Choreographer.FrameCallback frameCallback = this::doFrame;

    private FilamentQualityProfile qualityProfile = FilamentQualityProfile.MEDIUM;
    private LightingPreset lightingPreset = LightingPreset.SAFE_STUDIO;
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
    private String qualityFeatureStatus = "medium_mobile_safe_quality_only";
    private String environmentMode = "procedural_neutral_fallback";
    private String iblMode = "procedural_fallback";
    private String iblFile = "none";
    private String iblLoadStatus = "fallback_procedural";
    private String fallbackReason = "no_real_ibl_loaded";
    private String iblStatus = "fallback_no_hdr_asset";
    private String realIblReady = "false";
    private String skyboxReady = "true_procedural";
    private String indirectLightReady = "true_procedural";
    private String futureIblAssetPath = "none";
    private String modelSourcePath = "none";
    private String modelCopiedPath = "none";
    private String gltfioLoaded = "false";
    private String importCopyStatus = "not_run";
    private String permissionStatus = "not_checked";
    private String scanDownloadStatus = "not_run";
    private int scanCopiedCount = 0;
    private int scanSkippedCount = 0;
    private int scanFailedCount = 0;
    private String cameraStatus = "orbit_drag_pinch_zoom_unit_cube";
    private String lightingStatus = "not_applied";
    private String lastInputError = "none";
    private String refractionToggleAffectsTransmission = "false_screen_space_only_alpha_transmission_left_to_gltfio";
    private String legacyVulkanReturnStatus = "not_requested";
    private String actualAA = "FXAA";
    private int actualSampleCount = 2;
    private float dynamicMinScale = 0.72f;
    private float dynamicMaxScale = 0.95f;
    private boolean aoEnabled = false;
    private boolean bloomEnabled = false;
    private boolean shadowsEnabled = false;
    private boolean refractionEnabled = false;
    private boolean advancedValuesEnabled = false;
    private float sunLightIntensity = 6.0f;
    private float ambientFallbackIntensity = 3_000.0f;
    private float fillLightIntensity = 0.0f;
    private float exposure = 0.60f;
    private float backgroundBrightness = 0.16f;

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
        SAFE_STUDIO("Safe Studio", new float[] {-0.35f, -0.75f, -0.55f}, 6.0f, 3_000.0f, 0.0f, 0.60f, 0.16f, new float[] {0.70f, -0.35f, -0.62f}),
        BALANCED("Balanced", new float[] {-0.28f, -0.78f, -0.55f}, 9.0f, 4_500.0f, 0.0f, 0.70f, 0.18f, new float[] {0.66f, -0.38f, -0.65f}),
        BRIGHT_INSPECT("Bright Inspect", new float[] {-0.20f, -0.82f, -0.48f}, 14.0f, 6_000.0f, 0.0f, 0.68f, 0.22f, new float[] {0.62f, -0.42f, -0.66f}),
        CINEMATIC("Cinematic", new float[] {-0.62f, -0.62f, -0.48f}, 3.5f, 2_200.0f, 0.0f, 0.45f, 0.10f, new float[] {0.48f, -0.32f, -0.82f}),
        NEUTRAL("Neutral", new float[] {-0.35f, -0.82f, -0.45f}, 5.0f, 2_500.0f, 0.0f, 0.50f, 0.14f, new float[] {0.65f, -0.30f, -0.70f});

        final String label;
        final float[] sunDirection;
        final float sunIntensity;
        final float ambientFallbackIntensity;
        final float fillIntensity;
        final float exposure;
        final float backgroundBrightness;
        final float[] fillDirection;

        LightingPreset(String label, float[] sunDirection, float sunIntensity, float ambientFallbackIntensity, float fillIntensity, float exposure, float backgroundBrightness, float[] fillDirection) {
            this.label = label;
            this.sunDirection = sunDirection;
            this.sunIntensity = sunIntensity;
            this.ambientFallbackIntensity = ambientFallbackIntensity;
            this.fillIntensity = fillIntensity;
            this.exposure = exposure;
            this.backgroundBrightness = backgroundBrightness;
            this.fillDirection = fillDirection;
        }

        LightingPreset next() {
            if (this == SAFE_STUDIO) return BALANCED;
            if (this == BALANCED) return BRIGHT_INSPECT;
            if (this == BRIGHT_INSPECT) return CINEMATIC;
            if (this == CINEMATIC) return NEUTRAL;
            return SAFE_STUDIO;
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
        iblButton = button("IBL: Procedural fallback");
        iblButton.setOnClickListener(v -> cycleIblPreset());
        Button importModelButton = button("Import Model");
        importModelButton.setOnClickListener(v -> chooseModelForImport());
        Button importIblButton = button("Import IBL");
        importIblButton.setOnClickListener(v -> chooseIblForImport());
        Button scanDownloadButton = button("Scan Download");
        scanDownloadButton.setOnClickListener(v -> {
            scanDownloadForAssets("manual_button");
            updateIblButton();
            updateHud();
        });
        Button reloadButton = button("Reload");
        reloadButton.setOnClickListener(v -> loadModel());
        Button closeButton = button("Close Preview");
        closeButton.setOnClickListener(v -> closePreview());
        Button resetButton = button("Reset Safe Lighting");
        resetButton.setOnClickListener(v -> resetSafeLighting());
        advancedValuesButton = button("");
        advancedValuesButton.setOnClickListener(v -> {
            advancedValuesEnabled = !advancedValuesEnabled;
            updateAdvancedValuesVisibility();
            updateHud();
        });
        aoButton = button("");
        aoButton.setOnClickListener(v -> {
            aoEnabled = !aoEnabled;
            applyQualityProfile();
            updateHud();
        });
        bloomButton = button("");
        bloomButton.setOnClickListener(v -> {
            bloomEnabled = !bloomEnabled;
            applyQualityProfile();
            updateHud();
        });
        shadowsButton = button("");
        shadowsButton.setOnClickListener(v -> {
            shadowsEnabled = !shadowsEnabled;
            applyQualityProfile();
            applyLightingValues();
            updateHud();
        });
        refractionButton = button("");
        refractionButton.setOnClickListener(v -> {
            refractionEnabled = !refractionEnabled;
            applyQualityProfile();
            updateHud();
        });
        statusView = overlayText(10.0f, 28);
        controls.addView(importModelButton);
        controls.addView(importIblButton);
        controls.addView(scanDownloadButton);
        controls.addView(qualityButton);
        controls.addView(lightingButton);
        controls.addView(iblButton);
        controls.addView(resetButton);
        sunSlider = addLightingSlider(controls, "Sun", 0.0f, 20.0f, 0.5f, sunLightIntensity, v -> {
            sunLightIntensity = v;
            applyLightingValues();
        });
        ambientSlider = addLightingSlider(controls, "Ambient", 0.0f, 10_000.0f, 100.0f, ambientFallbackIntensity, v -> {
            ambientFallbackIntensity = v;
            applyLightingValues();
        });
        fillSlider = addLightingSlider(controls, "Fill", 0.0f, 20.0f, 0.5f, fillLightIntensity, v -> {
            fillLightIntensity = v;
            applyLightingValues();
        });
        exposureSlider = addLightingSlider(controls, "Exp", 0.30f, 2.00f, 0.01f, exposure, v -> {
            exposure = v;
            applyLightingValues();
        });
        backgroundSlider = addLightingSlider(controls, "BG", 0.05f, 0.45f, 0.01f, backgroundBrightness, v -> {
            backgroundBrightness = v;
            applyLightingValues();
        });
        controls.addView(advancedValuesButton);
        advancedValuesPanel = new LinearLayout(this);
        advancedValuesPanel.setOrientation(LinearLayout.VERTICAL);
        advancedValuesPanel.setPadding(0, dp(4), 0, dp(4));
        advancedValuesPanel.setVisibility(View.GONE);
        advancedSunField = addAdvancedField(advancedValuesPanel, "Sun", 0.0f, 300.0f, v -> sunLightIntensity = v);
        advancedAmbientField = addAdvancedField(advancedValuesPanel, "Ambient", 0.0f, 15_000.0f, v -> ambientFallbackIntensity = v);
        advancedFillField = addAdvancedField(advancedValuesPanel, "Fill", 0.0f, 300.0f, v -> fillLightIntensity = v);
        advancedExposureField = addAdvancedField(advancedValuesPanel, "Exposure", 0.30f, 2.00f, v -> exposure = v);
        advancedBackgroundField = addAdvancedField(advancedValuesPanel, "Background", 0.02f, 0.80f, v -> backgroundBrightness = v);
        controls.addView(advancedValuesPanel);
        controls.addView(aoButton);
        controls.addView(bloomButton);
        controls.addView(shadowsButton);
        controls.addView(refractionButton);
        controls.addView(reloadButton);
        controls.addView(closeButton);
        controls.addView(statusView);
        ScrollView controlScroll = new ScrollView(this);
        controlScroll.setFillViewport(false);
        controlScroll.addView(controls, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        int labHeight = Math.max(dp(260), Math.round(getResources().getDisplayMetrics().heightPixels * 0.40f));
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, labHeight);
        controlParams.gravity = Gravity.BOTTOM;
        controlParams.setMargins(dp(12), dp(12), dp(12), dp(28));
        root.addView(controlScroll, controlParams);
        setContentView(root);
        scanDownloadForAssets("startup");
        createViewer();
        restorePersistedIbl();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_MODEL) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                importCopyStatus = "model_picker_cancelled";
                updateHud();
                return;
            }
            importModelFromUri(data.getData());
            return;
        }
        if (requestCode == REQUEST_IMPORT_IBL) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                importCopyStatus = "ibl_picker_cancelled";
                updateHud();
                return;
            }
            importIblFromUri(data.getData());
        }
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
            applyLightingValues();
            updateAdvancedValuesVisibility();
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

    private void chooseModelForImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
            "model/gltf-binary",
            "model/gltf+json",
            "model/gltf-json",
            "application/octet-stream",
            "*/*"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_MODEL);
    }

    private void chooseIblForImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
            "image/vnd.radiance",
            "image/x-hdr",
            "image/ktx",
            "application/octet-stream",
            "*/*"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_IBL);
    }

    private void importModelFromUri(Uri uri) {
        try {
            persistReadPermission(uri);
            String sourceName = displayNameForUri(uri, "imported.glb");
            if (!isModelName(sourceName)) throw new IllegalArgumentException("unsupported_model_extension_glb_gltf_required");
            File out = copyUriToAssetFile(uri, sourceName, modelsDir());
            modelSourcePath = uri.toString();
            modelCopiedPath = out.getAbsolutePath();
            importCopyStatus = "model_import_copied";
            modelPath = out.getAbsolutePath();
            modelName = out.getName();
            persistActiveModel(out);
            loadModel();
        } catch (Throwable t) {
            importCopyStatus = "model_import_failed: " + shortMessage(t);
            loadStatus = importCopyStatus;
            updateHud();
        }
    }

    private void importIblFromUri(Uri uri) {
        try {
            persistReadPermission(uri);
            String sourceName = displayNameForUri(uri, "imported.hdr");
            if (!isIblName(sourceName)) throw new IllegalArgumentException("unsupported_ibl_extension_hdr_ktx_ktx1_exr_required");
            File out = copyUriToAssetFile(uri, sourceName, iblDir());
            importCopyStatus = "ibl_import_copied";
            loadIblFile(out, "picker_import");
        } catch (Throwable t) {
            String failedStatus = "ibl_import_failed: " + shortMessage(t);
            importCopyStatus = failedStatus;
            createEnvironmentFallback();
            iblLoadStatus = failedStatus;
            fallbackReason = "ibl_import_failed";
            updateHud();
        }
    }

    private void scanDownloadForAssets(String trigger) {
        scanCopiedCount = 0;
        scanSkippedCount = 0;
        scanFailedCount = 0;
        File download = new File("/storage/emulated/0/Download");
        if (!download.isDirectory()) {
            permissionStatus = "download_unavailable_or_permission_missing";
            scanDownloadStatus = trigger + ": failed_download_unavailable";
            return;
        }
        permissionStatus = download.canRead() ? "download_readable" : "download_permission_missing_picker_available";
        List<File> candidates = new ArrayList<>();
        collectDownloadCandidates(download, candidates, false);
        File solumIbl = new File(download, "solum_ibl_out");
        collectDownloadCandidates(solumIbl, candidates, true);
        for (File src : candidates) {
            try {
                File targetDir = isModelName(src.getName()) ? modelsDir() : iblDir();
                File target = new File(targetDir, safeFileName(src.getName()));
                if (target.isFile() && target.length() == src.length()) {
                    scanSkippedCount++;
                    continue;
                }
                copyFileToFile(src, uniqueFile(targetDir, target.getName()));
                scanCopiedCount++;
            } catch (Throwable ignored) {
                scanFailedCount++;
            }
        }
        scanDownloadStatus = trigger + ": copied=" + scanCopiedCount + " skipped=" + scanSkippedCount + " failed=" + scanFailedCount;
        File preferred = findPreferredIbl();
        if (preferred != null && "procedural_fallback".equals(iblMode)) {
            futureIblAssetPath = preferred.getAbsolutePath();
        }
    }

    private void collectDownloadCandidates(File dir, List<File> out, boolean recursive) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory() && recursive) {
                collectDownloadCandidates(file, out, true);
                continue;
            }
            if (!file.isFile()) continue;
            if (isModelName(file.getName()) || isIblName(file.getName())) out.add(file);
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
                gltfioLoaded = "false";
                updateHud();
                return;
            }
            File file = new File(modelPath);
            if (!file.isFile()) {
                loadStatus = "file_missing: " + modelPath;
                gltfioLoaded = "false";
                updateHud();
                return;
            }
            modelSourcePath = modelSourcePath.equals("none") ? file.getAbsolutePath() : modelSourcePath;
            modelCopiedPath = file.getAbsolutePath();
            ByteBuffer data = readFile(file);
            String lower = file.getName().toLowerCase(Locale.US);
            if (lower.endsWith(".glb")) {
                modelViewer.loadModelGlb(data);
            } else if (lower.endsWith(".gltf")) {
                File baseDir = file.getParentFile();
                modelViewer.loadModelGltf(data, (Function1<String, Buffer>) uri -> readSiblingResource(baseDir, uri));
            } else {
                loadStatus = "unsupported_extension_expected_glb_or_gltf";
                gltfioLoaded = "false";
                updateHud();
                return;
            }
            modelViewer.transformToUnitCube(new Float3(0.0f, 0.0f, 0.0f));
            cameraStatus = "orbit_drag_pinch_zoom_unit_cube_autofit";
            loadStatus = "ok_loaded_with_gltfio";
            gltfioLoaded = "true";
        } catch (Throwable t) {
            loadStatus = "load_error: " + shortMessage(t);
            gltfioLoaded = "false";
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
        if (fillLightEntity != 0) {
            if (modelViewer.getScene() != null) modelViewer.getScene().removeEntity(fillLightEntity);
            engine.getLightManager().destroy(fillLightEntity);
            EntityManager.get().destroy(fillLightEntity);
            fillLightEntity = 0;
        }
        destroyEnvironmentResources(engine);
        indirectLight = new IndirectLight.Builder()
            .intensity(ambientFallbackIntensity)
            .build(engine);
        skybox = new Skybox.Builder()
            .color(backgroundColor())
            .build(engine);
        modelViewer.getScene().setIndirectLight(indirectLight);
        modelViewer.getScene().setSkybox(skybox);
        iblMode = "procedural_fallback";
        iblFile = "none";
        iblLoadStatus = "fallback_procedural";
        environmentMode = "procedural_neutral_fallback";
        iblStatus = "fallback_procedural_indirect_light";
        realIblReady = "false";
        skyboxReady = "true_procedural";
        indirectLightReady = "true_procedural";
        fallbackReason = "no_real_ibl_loaded";
        fillLightEntity = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .castShadows(false)
            .direction(lightingPreset.fillDirection[0], lightingPreset.fillDirection[1], lightingPreset.fillDirection[2])
            .color(0.86f, 0.92f, 1.0f)
            .intensity(fillLightIntensity)
            .build(engine, fillLightEntity);
        modelViewer.getScene().addEntity(fillLightEntity);
    }

    private void loadIblFile(File file, String reason) {
        if (modelViewer == null) {
            iblLoadStatus = "viewer_not_ready";
            futureIblAssetPath = file == null ? "none" : file.getAbsolutePath();
            return;
        }
        if (file == null || !file.isFile()) {
            iblLoadStatus = "ibl_file_missing";
            fallbackReason = "ibl_file_missing";
            createEnvironmentFallback();
            return;
        }
        String lower = file.getName().toLowerCase(Locale.US);
        if (lower.endsWith(".exr")) {
            iblMode = "unsupported_exr";
            iblFile = file.getName();
            iblLoadStatus = "exr_imported_but_runtime_loader_not_supported";
            realIblReady = "false";
            skyboxReady = skybox == null ? "false" : skyboxReady;
            indirectLightReady = indirectLight == null ? "false" : indirectLightReady;
            fallbackReason = "exr_not_supported_by_filament_utils_runtime_loader";
            futureIblAssetPath = file.getAbsolutePath();
            persistActiveIbl(file);
            updateIblButton();
            updateHud();
            return;
        }
        try {
            if (lower.endsWith(".ktx") || lower.endsWith(".ktx1")) {
                loadKtxIbl(file, reason);
            } else if (lower.endsWith(".hdr")) {
                loadHdrIbl(file, reason);
            } else {
                iblLoadStatus = "unsupported_ibl_extension";
                fallbackReason = "unsupported_ibl_extension";
            }
            persistActiveIbl(file);
        } catch (Throwable t) {
            String failedStatus = "ibl_load_failed: " + shortMessage(t);
            realIblReady = "false";
            skyboxReady = "false";
            indirectLightReady = "false";
            createEnvironmentFallback();
            iblLoadStatus = failedStatus;
            fallbackReason = "ibl_loader_exception";
        }
        updateIblButton();
        updateHud();
    }

    private void loadKtxIbl(File file, String reason) throws Exception {
        Engine engine = modelViewer.getEngine();
        ByteBuffer data = readFile(file);
        KTX1Loader.Options options = new KTX1Loader.Options();
        options.setSrgb(false);
        KTX1Loader.IndirectLightBundle indirectBundle = KTX1Loader.INSTANCE.createIndirectLight(engine, data, options);
        data.rewind();
        KTX1Loader.SkyboxBundle skyboxBundle = KTX1Loader.INSTANCE.createSkybox(engine, data, options);
        if (indirectBundle == null || indirectBundle.getIndirectLight() == null) {
            throw new IllegalStateException("ktx_indirect_light_missing_spherical_harmonics");
        }
        if (skyboxBundle == null || skyboxBundle.getSkybox() == null) {
            throw new IllegalStateException("ktx_skybox_missing");
        }
        destroyEnvironmentResources(engine);
        indirectLight = indirectBundle.getIndirectLight();
        indirectLight.setIntensity(ambientFallbackIntensity);
        skybox = skyboxBundle.getSkybox();
        modelViewer.getScene().setIndirectLight(indirectLight);
        modelViewer.getScene().setSkybox(skybox);
        if (indirectBundle.getCubemap() != null) iblOwnedTextures.add(indirectBundle.getCubemap());
        if (skyboxBundle.getCubemap() != null) iblOwnedTextures.add(skyboxBundle.getCubemap());
        iblMode = "ktx1_real_ibl";
        iblFile = file.getName();
        iblLoadStatus = "ok_ktx1loader_" + reason;
        environmentMode = "real_ibl_ktx1";
        iblStatus = "ok_real_ibl_ktx1loader";
        realIblReady = "true";
        skyboxReady = "true";
        indirectLightReady = "true";
        fallbackReason = "none";
        futureIblAssetPath = file.getAbsolutePath();
    }

    private void loadHdrIbl(File file, String reason) throws Exception {
        Engine engine = modelViewer.getEngine();
        HDRLoader.Options options = new HDRLoader.Options();
        Texture hdrTexture = HDRLoader.INSTANCE.createTexture(engine, readFile(file), options);
        if (hdrTexture == null) throw new IllegalStateException("hdr_loader_returned_null_texture");
        IBLPrefilterContext context = null;
        IBLPrefilterContext.EquirectangularToCubemap equirect = null;
        IBLPrefilterContext.SpecularFilter specular = null;
        try {
            context = new IBLPrefilterContext(engine);
            equirect = new IBLPrefilterContext.EquirectangularToCubemap(context);
            Texture cubemap = equirect.run(hdrTexture);
            if (cubemap == null) throw new IllegalStateException("hdr_equirectangular_to_cubemap_failed");
            specular = new IBLPrefilterContext.SpecularFilter(context);
            Texture reflections = specular.run(cubemap);
            if (reflections == null) throw new IllegalStateException("hdr_specular_prefilter_failed");
            IndirectLight newIndirectLight = new IndirectLight.Builder()
                .reflections(reflections)
                .intensity(ambientFallbackIntensity)
                .build(engine);
            Skybox newSkybox = new Skybox.Builder()
                .environment(cubemap)
                .intensity(Math.max(1000.0f, ambientFallbackIntensity))
                .build(engine);
            destroyEnvironmentResources(engine);
            indirectLight = newIndirectLight;
            skybox = newSkybox;
            modelViewer.getScene().setIndirectLight(indirectLight);
            modelViewer.getScene().setSkybox(skybox);
            iblOwnedTextures.add(hdrTexture);
            iblOwnedTextures.add(cubemap);
            iblOwnedTextures.add(reflections);
            iblMode = "hdr_runtime_prefilter_real_ibl";
            iblFile = file.getName();
            iblLoadStatus = "ok_hdrloader_runtime_prefilter_" + reason;
            environmentMode = "real_ibl_hdr_runtime_prefilter";
            iblStatus = "ok_real_ibl_hdrloader_prefilter";
            realIblReady = "true";
            skyboxReady = "true";
            indirectLightReady = "true_reflections_only";
            fallbackReason = "none";
            futureIblAssetPath = file.getAbsolutePath();
        } catch (Throwable t) {
            if (!iblOwnedTextures.contains(hdrTexture)) engine.destroyTexture(hdrTexture);
            iblMode = "hdr_imported";
            iblFile = file.getName();
            iblLoadStatus = "hdr_imported_but_runtime_prefilter_not_available: " + shortMessage(t);
            realIblReady = "false";
            skyboxReady = skybox == null ? "false" : skyboxReady;
            indirectLightReady = indirectLight == null ? "false" : indirectLightReady;
            fallbackReason = "hdr_runtime_prefilter_failed";
            futureIblAssetPath = file.getAbsolutePath();
        } finally {
            if (specular != null) specular.destroy();
            if (equirect != null) equirect.destroy();
            if (context != null) context.destroy();
        }
    }

    private void destroyEnvironmentResources(Engine engine) {
        if (modelViewer != null && modelViewer.getScene() != null) {
            modelViewer.getScene().setIndirectLight(null);
            modelViewer.getScene().setSkybox(null);
        }
        if (indirectLight != null) {
            engine.destroyIndirectLight(indirectLight);
            indirectLight = null;
        }
        if (skybox != null) {
            engine.destroySkybox(skybox);
            skybox = null;
        }
        for (Texture texture : iblOwnedTextures) {
            if (texture != null) {
                try { engine.destroyTexture(texture); } catch (Throwable ignored) { }
            }
        }
        iblOwnedTextures.clear();
    }

    private void applyLightingPreset() {
        if (modelViewer == null) return;
        try {
            ambientFallbackIntensity = lightingPreset.ambientFallbackIntensity;
            sunLightIntensity = lightingPreset.sunIntensity;
            fillLightIntensity = lightingPreset.fillIntensity;
            exposure = lightingPreset.exposure;
            backgroundBrightness = lightingPreset.backgroundBrightness;
            applyLightingValues();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        if (lightingButton != null) lightingButton.setText("Lighting: " + lightingPreset.label);
    }

    private void resetSafeLighting() {
        lightingPreset = LightingPreset.SAFE_STUDIO;
        aoEnabled = false;
        bloomEnabled = false;
        shadowsEnabled = false;
        refractionEnabled = false;
        lastInputError = "none";
        applyLightingPreset();
        applyQualityProfile();
        updateHud();
    }

    private void applyLightingValues() {
        if (modelViewer == null) return;
        try {
            sunLightIntensity = clamp(sunLightIntensity, 0.0f, 300.0f);
            ambientFallbackIntensity = clamp(ambientFallbackIntensity, 0.0f, 15_000.0f);
            fillLightIntensity = clamp(fillLightIntensity, 0.0f, 300.0f);
            exposure = clamp(exposure, 0.30f, 2.00f);
            backgroundBrightness = clamp(backgroundBrightness, 0.02f, 0.80f);
            if (indirectLight != null) indirectLight.setIntensity(ambientFallbackIntensity);
            if (skybox != null && !realIblReady.equals("true")) skybox.setColor(backgroundColor());

            Engine engine = modelViewer.getEngine();
            LightManager lights = engine.getLightManager();
            int sunInstance = lights.getInstance(modelViewer.getLight());
            if (sunInstance != 0) {
                lights.setDirection(sunInstance, lightingPreset.sunDirection[0], lightingPreset.sunDirection[1], lightingPreset.sunDirection[2]);
                lights.setIntensity(sunInstance, sunLightIntensity);
                lights.setColor(sunInstance, 1.0f, 0.96f, 0.90f);
                lights.setShadowCaster(sunInstance, shadowsEnabled);
            }
            if (fillLightEntity != 0) {
                int fillInstance = lights.getInstance(fillLightEntity);
                if (fillInstance != 0) {
                    lights.setDirection(fillInstance, lightingPreset.fillDirection[0], lightingPreset.fillDirection[1], lightingPreset.fillDirection[2]);
                    lights.setIntensity(fillInstance, fillLightIntensity);
                    lights.setColor(fillInstance, 0.86f, 0.92f, 1.0f);
                    lights.setShadowCaster(fillInstance, false);
                }
            }
            modelViewer.getCamera().setExposure(exposure);
            Renderer.ClearOptions clear = modelViewer.getRenderer().getClearOptions();
            clear.clear = true;
            clear.discard = true;
            clear.clearColor = backgroundColor();
            modelViewer.getRenderer().setClearOptions(clear);
            lightingStatus = advancedValuesEnabled ? "live_values_applied_advanced_ranges" : "live_values_applied_safe_slider_ranges";
            if (realIblReady.equals("true")) iblStatus = "ok_real_ibl_intensity_controlled_by_ambient_slider";
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        updateLightingControlLabels();
        updateHud();
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
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                ao.enabled = aoEnabled;
                ao.quality = QualityLevel.LOW;
                ao.intensity = 0.12f;
                bloom.enabled = bloomEnabled;
                bloom.strength = bloomEnabled ? 0.03f : 0.0f;
                bloom.quality = QualityLevel.LOW;
                dynamic.enabled = true;
                dynamic.minScale = 0.58f;
                dynamic.maxScale = 0.82f;
                dynamic.quality = QualityLevel.LOW;
                renderQuality.hdrColorBuffer = QualityLevel.LOW;
                setActualQualityStatus("NONE", 1, 0.58f, 0.82f, "low_quality_only_dynamic_0_58_0_82");
            } else if (qualityProfile == FilamentQualityProfile.HIGH_PREVIEW) {
                view.setAntiAliasing(AntiAliasing.FXAA);
                view.setSampleCount(4);
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                ao.enabled = aoEnabled;
                ao.quality = QualityLevel.LOW;
                ao.intensity = 0.18f;
                bloom.enabled = bloomEnabled;
                bloom.strength = bloomEnabled ? 0.04f : 0.0f;
                bloom.quality = QualityLevel.LOW;
                dynamic.enabled = true;
                dynamic.minScale = 0.86f;
                dynamic.maxScale = 1.00f;
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.HIGH;
                setActualQualityStatus("FXAA", 4, 0.86f, 1.00f, "high_preview_quality_only_dynamic_0_86_1_00");
            } else {
                view.setAntiAliasing(AntiAliasing.FXAA);
                view.setSampleCount(2);
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                ao.enabled = aoEnabled;
                ao.quality = QualityLevel.LOW;
                ao.intensity = 0.15f;
                bloom.enabled = bloomEnabled;
                bloom.strength = bloomEnabled ? 0.035f : 0.0f;
                bloom.quality = QualityLevel.LOW;
                dynamic.enabled = true;
                dynamic.minScale = 0.72f;
                dynamic.maxScale = 0.95f;
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.MEDIUM;
                setActualQualityStatus("FXAA", 2, 0.72f, 0.95f, "medium_quality_only_dynamic_0_72_0_95");
            }
            view.setAmbientOcclusionOptions(ao);
            view.setBloomOptions(bloom);
            view.setDynamicResolutionOptions(dynamic);
            view.setRenderQuality(renderQuality);
            applyColorGrading();
            applyLightingValues();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            qualityFeatureStatus = "apply_failed";
        }
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
        updateToggleLabels();
    }

    private void setActualQualityStatus(String aa, int sampleCount, float minScale, float maxScale, String status) {
        actualAA = aa;
        actualSampleCount = sampleCount;
        dynamicMinScale = minScale;
        dynamicMaxScale = maxScale;
        qualityFeatureStatus = status + "_features_ao_" + aoEnabled + "_bloom_" + bloomEnabled + "_shadows_" + shadowsEnabled + "_refraction_" + refractionEnabled + "_brightness_unchanged";
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
                destroyEnvironmentResources(engine);
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

    private void restorePersistedIbl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String path = prefs.getString(PREF_ACTIVE_IBL_PATH, "");
        File preferred = path == null || path.isEmpty() ? findPreferredIbl() : new File(path);
        if (preferred != null && preferred.isFile()) loadIblFile(preferred, "startup_restore");
        else updateIblButton();
    }

    private void cycleIblPreset() {
        List<File> presets = new ArrayList<>();
        presets.add(null);
        File neutral = findFirstExistingIbl("studio_small_03");
        if (neutral != null) presets.add(neutral);
        File forest = findFirstExistingIbl("phalzer_forest_01");
        if (forest != null && !sameFile(neutral, forest)) presets.add(forest);
        for (File file : listIblFiles()) {
            boolean known = false;
            for (File preset : presets) if (sameFile(file, preset)) known = true;
            if (!known) presets.add(file);
        }
        int current = 0;
        for (int i = 0; i < presets.size(); i++) {
            File file = presets.get(i);
            if (file == null && "procedural_fallback".equals(iblMode)) current = i;
            else if (file != null && file.getName().equals(iblFile)) current = i;
        }
        File next = presets.get((current + 1) % presets.size());
        if (next == null) {
            createEnvironmentFallback();
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(PREF_ACTIVE_IBL_PATH).remove(PREF_ACTIVE_IBL_NAME).apply();
            updateIblButton();
            updateHud();
        } else {
            loadIblFile(next, "preset_cycle");
        }
    }

    private File findPreferredIbl() {
        File neutral = findFirstExistingIbl("studio_small_03");
        if (neutral != null) return neutral;
        File forest = findFirstExistingIbl("phalzer_forest_01");
        if (forest != null) return forest;
        List<File> all = listIblFiles();
        return all.isEmpty() ? null : all.get(0);
    }

    private File findFirstExistingIbl(String prefix) {
        for (File file : listIblFiles()) {
            String lower = file.getName().toLowerCase(Locale.US);
            if (lower.startsWith(prefix) && !lower.endsWith(".exr")) return file;
        }
        return null;
    }

    private List<File> listIblFiles() {
        List<File> files = new ArrayList<>();
        collectAssetFiles(iblDir(), files, false, true);
        return files;
    }

    private void collectAssetFiles(File dir, List<File> out, boolean models, boolean ibl) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.isFile()) continue;
            if ((models && isModelName(file.getName())) || (ibl && isIblName(file.getName()))) out.add(file);
        }
    }

    private File copyUriToAssetFile(Uri uri, String sourceName, File dir) throws Exception {
        File out = uniqueFile(dir, safeFileName(sourceName));
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream output = new FileOutputStream(out, false)) {
            if (in == null) throw new IllegalStateException("open_input_stream_failed");
            copyStream(in, output);
        }
        return out;
    }

    private void copyFileToFile(File src, File out) throws Exception {
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = new FileInputStream(src); OutputStream output = new FileOutputStream(out, false)) {
            copyStream(in, output);
        }
    }

    private void copyStream(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
    }

    private void persistReadPermission(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            permissionStatus = "picker_persisted_read_permission";
        } catch (Throwable t) {
            permissionStatus = "picker_read_permission_runtime_only";
        }
    }

    private void persistActiveModel(File file) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(PREF_ACTIVE_MODEL_PATH, file.getAbsolutePath())
            .putString(PREF_ACTIVE_MODEL_LOCAL_PATH, file.getAbsolutePath())
            .putString(PREF_ACTIVE_MODEL_NAME, file.getName())
            .apply();
    }

    private void persistActiveIbl(File file) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(PREF_ACTIVE_IBL_PATH, file.getAbsolutePath())
            .putString(PREF_ACTIVE_IBL_NAME, file.getName())
            .apply();
    }

    private File modelsDir() {
        File dir = new File(getFilesDir(), "solum/assets/models");
        dir.mkdirs();
        return dir;
    }

    private File iblDir() {
        File dir = new File(getFilesDir(), "solum/assets/ibl");
        dir.mkdirs();
        return dir;
    }

    private File uniqueFile(File dir, String fileName) {
        dir.mkdirs();
        File out = new File(dir, fileName);
        if (!out.exists()) return out;
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        for (int i = 1; i < 1000; i++) {
            out = new File(dir, base + "_" + i + ext);
            if (!out.exists()) return out;
        }
        return new File(dir, base + "_" + System.currentTimeMillis() + ext);
    }

    private String displayNameForUri(Uri uri, String fallback) {
        try (Cursor cursor = getContentResolver().query(uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Throwable ignored) { }
        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty() ? fallback : last;
    }

    private static boolean isModelName(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.US);
        return lower.endsWith(".glb") || lower.endsWith(".gltf");
    }

    private static boolean isIblName(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.US);
        return lower.endsWith(".hdr") || lower.endsWith(".ktx") || lower.endsWith(".ktx1") || lower.endsWith(".exr");
    }

    private static String safeFileName(String name) {
        String safe = name == null ? "imported_asset" : name.trim();
        if (safe.isEmpty()) safe = "imported_asset";
        return safe.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static boolean sameFile(File a, File b) {
        if (a == null || b == null) return false;
        return a.getAbsolutePath().equals(b.getAbsolutePath());
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
                + "\nRenderer: Filament"
                + "\nGLB loader: gltfio loaded=" + gltfioLoaded
                + "\nLoad: " + loadStatus
                + "\nSource: " + shorten(modelSourcePath, 72)
                + "\nCopied: " + shorten(modelCopiedPath, 72)
                + "\nCamera: " + cameraStatus
                + "\nLight preset: " + lightingPreset.label + " / " + lightingStatus
                + "\nSun/Ambient/Fill/Exp/BG: " + oneDecimal(sunLightIntensity) + " / " + noDecimal(ambientFallbackIntensity) + " / " + oneDecimal(fillLightIntensity) + " / " + twoDecimal(exposure) + " / " + twoDecimal(backgroundBrightness)
                + "\niblMode=" + iblMode + " iblFile=" + iblFile
                + "\niblLoadStatus=" + iblLoadStatus
                + "\nrealIblReady=" + realIblReady + " skyboxReady=" + skyboxReady + " indirectLightReady=" + indirectLightReady
                + "\nfallbackReason=" + fallbackReason
                + "\nenvironmentMode=" + environmentMode + " activeIbl=" + shorten(futureIblAssetPath, 72)
                + "\nQuality: " + qualityFeatureStatus
                + "\nactualAA=" + actualAA + " sampleCount=" + actualSampleCount + " dynamicResolution=" + twoDecimal(dynamicMinScale) + "-" + twoDecimal(dynamicMaxScale)
                + "\naoEnabled=" + aoEnabled + " bloomEnabled=" + bloomEnabled + " shadowsEnabled=" + shadowsEnabled + " refractionEnabled=" + refractionEnabled
                + "\nimportCopyStatus=" + importCopyStatus
                + "\nscanDownloadStatus=" + scanDownloadStatus + " copied/skipped/failed=" + scanCopiedCount + "/" + scanSkippedCount + "/" + scanFailedCount
                + "\npermissionStatus=" + permissionStatus
                + "\nrefractionToggleAffectsTransmission=" + refractionToggleAffectsTransmission
                + "\nadvancedValues=" + advancedValuesEnabled + " lastInputError=" + lastInputError
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

    private SeekBar addLightingSlider(LinearLayout parent, String label, float min, float max, float step, float value, SliderCallback callback) {
        TextView text = overlayText(10.0f, 1);
        text.setBackgroundColor(Color.TRANSPARENT);
        SeekBar slider = new SeekBar(this);
        slider.setMax(Math.max(1, Math.round((max - min) / step)));
        slider.setProgress(valueToProgress(value, min, max, step));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                callback.onValue(progressToValue(progress, min, max, step));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if ("Sun".equals(label)) sunSliderLabel = text;
        else if ("Ambient".equals(label)) ambientSliderLabel = text;
        else if ("Fill".equals(label)) fillSliderLabel = text;
        else if ("Exp".equals(label)) exposureSliderLabel = text;
        else if ("BG".equals(label)) backgroundSliderLabel = text;
        parent.addView(text);
        parent.addView(slider);
        setSliderLabel(text, label, value);
        return slider;
    }

    private void updateLightingControlLabels() {
        setSliderLabel(sunSliderLabel, "Sun", sunLightIntensity);
        setSliderLabel(ambientSliderLabel, realIblReady.equals("true") ? "IBL" : "Ambient", ambientFallbackIntensity);
        setSliderLabel(fillSliderLabel, "Fill", fillLightIntensity);
        setSliderLabel(exposureSliderLabel, "Exp", exposure);
        setSliderLabel(backgroundSliderLabel, "BG", backgroundBrightness);
        setSliderProgress(sunSlider, sunLightIntensity, 0.0f, 20.0f, 0.5f);
        setSliderProgress(ambientSlider, ambientFallbackIntensity, 0.0f, 10_000.0f, 100.0f);
        setSliderProgress(fillSlider, fillLightIntensity, 0.0f, 20.0f, 0.5f);
        setSliderProgress(exposureSlider, exposure, 0.30f, 2.00f, 0.01f);
        setSliderProgress(backgroundSlider, backgroundBrightness, 0.05f, 0.45f, 0.01f);
        updateAdvancedFieldValues();
        updateToggleLabels();
        if (lightingButton != null) lightingButton.setText("Lighting: " + lightingPreset.label);
        updateIblButton();
        if (advancedValuesButton != null) advancedValuesButton.setText("Advanced Values: " + (advancedValuesEnabled ? "On" : "Off"));
    }

    private void updateIblButton() {
        if (iblButton == null) return;
        if ("procedural_fallback".equals(iblMode)) {
            iblButton.setText("IBL: Procedural fallback");
        } else if ("unsupported_exr".equals(iblMode)) {
            iblButton.setText("IBL: EXR unsupported");
        } else {
            iblButton.setText("IBL: " + shorten(iblFile, 28));
        }
    }

    private void updateToggleLabels() {
        if (aoButton != null) aoButton.setText("AO " + (aoEnabled ? "On" : "Off"));
        if (bloomButton != null) bloomButton.setText("Bloom " + (bloomEnabled ? "On" : "Off"));
        if (shadowsButton != null) shadowsButton.setText("Shadows " + (shadowsEnabled ? "On" : "Off"));
        if (refractionButton != null) refractionButton.setText("Refraction " + (refractionEnabled ? "On" : "Off"));
    }

    private void setSliderLabel(TextView label, String name, float value) {
        if (label == null) return;
        if ("Exp".equals(name) || "BG".equals(name)) {
            label.setText(name + " " + twoDecimal(value));
        } else if ("Sun".equals(name) || "Fill".equals(name)) {
            label.setText(name + " " + oneDecimal(value));
        } else {
            label.setText(name + " " + noDecimal(value));
        }
    }

    private void setSliderProgress(SeekBar slider, float value, float min, float max, float step) {
        if (slider == null) return;
        int progress = valueToProgress(value, min, max, step);
        if (slider.getProgress() != progress) slider.setProgress(progress);
    }

    private EditText addAdvancedField(LinearLayout parent, String label, float min, float max, SliderCallback callback) {
        TextView fieldLabel = overlayText(10.0f, 1);
        fieldLabel.setBackgroundColor(Color.TRANSPARENT);
        fieldLabel.setText(label + " advanced " + oneDecimal(min) + "-" + oneDecimal(max));
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(label);
        field.setTextColor(Color.rgb(218, 248, 255));
        field.setHintTextColor(Color.rgb(130, 170, 178));
        field.setTextSize(12.0f);
        field.setSelectAllOnFocus(true);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        field.setBackgroundColor(Color.argb(220, 4, 24, 30));
        field.setPadding(dp(10), dp(4), dp(10), dp(4));
        field.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterPressed = event != null && event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_DONE || enterPressed) {
                applyAdvancedField(field, label, min, max, callback);
                field.clearFocus();
                return true;
            }
            return false;
        });
        field.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) applyAdvancedField(field, label, min, max, callback);
        });
        parent.addView(fieldLabel);
        parent.addView(field);
        return field;
    }

    private void applyAdvancedField(EditText field, String label, float min, float max, SliderCallback callback) {
        if (field == null) return;
        String raw = field.getText() == null ? "" : field.getText().toString().trim();
        if (raw.isEmpty()) {
            lastInputError = label + "_empty_kept_previous";
            updateAdvancedFieldValues();
            updateHud();
            return;
        }
        try {
            float parsed = Float.parseFloat(raw);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) throw new NumberFormatException("not_finite");
            float clamped = clamp(parsed, min, max);
            callback.onValue(clamped);
            lastInputError = parsed == clamped ? "none" : label + "_clamped_to_" + compactValue(clamped);
            applyLightingValues();
        } catch (Throwable ignored) {
            lastInputError = label + "_invalid_kept_previous";
            updateAdvancedFieldValues();
            updateHud();
        }
    }

    private void updateAdvancedValuesVisibility() {
        if (advancedValuesPanel != null) {
            advancedValuesPanel.setVisibility(advancedValuesEnabled ? View.VISIBLE : View.GONE);
        }
        if (advancedValuesButton != null) {
            advancedValuesButton.setText("Advanced Values: " + (advancedValuesEnabled ? "On" : "Off"));
        }
        updateAdvancedFieldValues();
    }

    private void updateAdvancedFieldValues() {
        setAdvancedFieldText(advancedSunField, oneDecimal(sunLightIntensity));
        setAdvancedFieldText(advancedAmbientField, noDecimal(ambientFallbackIntensity));
        setAdvancedFieldText(advancedFillField, oneDecimal(fillLightIntensity));
        setAdvancedFieldText(advancedExposureField, twoDecimal(exposure));
        setAdvancedFieldText(advancedBackgroundField, twoDecimal(backgroundBrightness));
    }

    private void setAdvancedFieldText(EditText field, String value) {
        if (field == null || field.hasFocus()) return;
        if (!value.equals(field.getText() == null ? "" : field.getText().toString())) {
            field.setText(value);
        }
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

    private float[] backgroundColor() {
        float bg = clamp(backgroundBrightness, 0.02f, 0.80f);
        return new float[] {bg * 0.80f, bg * 0.90f, bg, 1.0f};
    }

    private static int valueToProgress(float value, float min, float max, float step) {
        return Math.round((clamp(value, min, max) - min) / Math.max(0.0001f, step));
    }

    private static float progressToValue(int progress, float min, float max, float step) {
        return clamp(min + progress * step, min, max);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private interface SliderCallback {
        void onValue(float value);
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

    private static String compactValue(float value) {
        return Math.abs(value - Math.round(value)) < 0.0001f ? noDecimal(value) : twoDecimal(value);
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
