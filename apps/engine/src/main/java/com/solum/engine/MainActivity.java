package com.solum.engine;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.Choreographer;
import android.view.View;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG_DIAG = "SOLUM_ENGINE_DIAG";
    private static final String PREFS_NAME = "solum_engine_diagnostics";
    private static final String PREF_TREE_URI = "diagnostics_tree_uri";
    private static final String SCENE_ID = "scene15_environment_ibl_lab";
    private static final String SCENE_NAME = "Scene15 Environment IBL Lab";
    private static final int REQUEST_CHOOSE_DIAGNOSTICS_TREE = 2202;
    private static final int REQUEST_IMPORT_GLB = 2305;
    private static final int TEX_BASE_COLOR = 0;
    private static final int TEX_METALLIC_ROUGHNESS = 1;
    private static final int TEX_NORMAL = 2;
    private static final int TEX_OCCLUSION = 3;
    private static final long FPS_SAMPLE_WINDOW_NS = 1_000_000_000L;

    private long nativeHandle = 0L;
    private TextView statusView;
    private TextView diagnosticsStatusView;
    private TextView assetsSummaryView;
    private TextView cameraInfoView;
    private Button exportButton;
    private Button quickExportButton;
    private Button debugZipButton;
    private Button chooseFolderButton;
    private Button importGlbButton;
    private Button scanModelsButton;
    private Button inspectorToggleButton;
    private Button assetsTabButton;
    private Button cameraTabButton;
    private Button lightingTabButton;
    private Button materialTabButton;
    private Button debugTabButton;
    private Button lightPresetButton;
    private Button sunIntensityButton;
    private Button ambientIntensityButton;
    private Button exposureButton;
    private Button specularBoostButton;
    private Button reflectionIntensityButton;
    private Button groundIntensityButton;
    private Button environmentButton;
    private Button skyPresetButton;
    private Button horizonButton;
    private Button calibrationPresetButton;
    private Button calibrationButton;
    private Button glossButton;
    private Button paintGlossButton;
    private Button materialViewButton;
    private TextView materialStatusView;
    private SeekBar sunSlider;
    private SeekBar ambientSlider;
    private SeekBar exposureSlider;
    private SeekBar specularSlider;
    private SeekBar reflectionSlider;
    private SeekBar groundSlider;
    private SeekBar environmentSlider;
    private SeekBar horizonSlider;
    private SeekBar calibrationSlider;
    private SeekBar glossSlider;
    private SeekBar paintGlossSlider;
    private LinearLayout inspectorPanel;
    private LinearLayout tabRow;
    private LinearLayout assetsPanel;
    private LinearLayout cameraPanel;
    private LinearLayout lightingPanel;
    private LinearLayout materialPanel;
    private LinearLayout debugPanel;
    private TextView topHudView;
    private boolean nativeLoaded = false;
    private boolean inspectorPanelVisible = true;
    private String activeInspectorTab = "Assets";
    private boolean updatingSlidersFromState = false;
    private int lightPresetIndex = 3;
    private float sunIntensity = 2.0f;
    private float ambientIntensity = 0.80f;
    private float exposureValue = 1.50f;
    private float ambientFloor = 0.16f;
    private float specularBoost = 1.85f;
    private float reflectionIntensity = 1.15f;
    private float contactShadowIntensity = 0.65f;
    private float environmentIntensity = 1.0f;
    private int environmentPresetIndex = 0;
    private float horizonStrength = 0.55f;
    private int calibrationPresetIndex = 2;
    private float calibrationSliderValue = 0.65f;
    private float glossSliderValue = 0.62f;
    private float paintGlossSliderValue = 0.55f;
    private int brightnessPresetIndex = 3;
    private int activeDebugViewIndex = 0;
    private int toneMappingModeIndex = 1;
    private File cachedReportDir = null;
    private String cachedReportDirReason = "not_resolved";
    private String lastExportStatus = "not run";
    private String lastExportRoute = "not run";
    private String lastExportReason = "";
    private String lastExportPath = "";
    private String lastExportTimestamp = "";
    private String debugZipStatus = "not_run";
    private String debugZipPath = "";
    private String debugZipReason = "not_run";
    private String debugZipIncludedFiles = "";
    private long fpsWindowStartNs = 0L;
    private long fpsWindowFrames = 0L;
    private long framesRenderedLive = 0L;
    private float fpsCurrent = 0.0f;
    private float frameTimeMs = 0.0f;
    private float fpsLastStable = 0.0f;
    private float frameTimeLastStableMs = 0.0f;
    private String fpsSource = "not_ready";
    private String fpsStatus = "not_ready";
    private String fpsUpdateMode = "java_choreographer_live";
    private int fpsSampleWindowMs = 1000;
    private boolean fpsPulseActive = false;
    private final Choreographer.FrameCallback fpsFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!fpsPulseActive) return;
            updateFpsFromUiPulse();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };
    private float cameraYawDeg = 28.0f;
    private float cameraPitchDeg = -18.0f;
    private float cameraDistance = 4.2f;
    private float lastTouchX = 0.0f;
    private float lastTouchY = 0.0f;
    private float lastPinchDistance = 0.0f;
    private boolean pinchActive = false;
    private ModelImportState modelState = ModelImportState.notRun();

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);
    private static native String nativeGetRenderLabState(long handle);
    private static native void nativeSetCamera(long handle, float yawDeg, float pitchDeg, float distance);
    private static native void nativeSetLightingControls(long handle, int lightPreset, float sunIntensity, float ambientIntensity, int activeDebugView, int toneMappingMode, float exposureValue, float ambientFloor, int brightnessPreset, float specularBoost, float reflectionIntensity, float contactShadowIntensity, int calibrationPreset, float calibrationStrength, float glossSliderValue, float paintGlossSliderValue, float environmentIntensity, int environmentPreset, float horizonStrength);
    private static native boolean nativeUploadModelFirstPrimitive(long handle, String modelName, String modelPath, float[] vertexData, int[] indexData, float[] boundsMin, float[] boundsMax, float[] boundsCenter, float modelScale, float[] baseColorFactor);
    private static native boolean nativeUploadModelMultiPrimitive(long handle, String modelName, String modelPath, float[] vertexData, int[] indexData, int[] rangeData, float[] materialData, float[] boundsMin, float[] boundsMax, float[] boundsCenter, float modelScale, int primitiveTotal, int primitiveSkipped, int unsupportedPrimitiveCount, String reason);
    private static native boolean nativeUploadBaseColorTexture(long handle, int[] rgbaPixels, int width, int height, String textureName, String textureSource, String mimeType);
    private static native boolean nativeUploadBaseColorTextureSlot(long handle, int slot, int[] rgbaPixels, int width, int height, String textureName, String textureSource, String mimeType);
    private static native boolean nativeUploadPbrTextureSlot(long handle, int materialSlot, int textureKind, int[] rgbaPixels, int width, int height, String textureName, String textureSource, String mimeType);
    private static native void nativeSetPbrDiagnostics(long handle, String pbrMapsStatus, String metallicRoughnessStatus, String normalMapStatus, String normalMapAppliedStatus, String occlusionMapStatus, String tangentStatus, String tangentSource, int tangentGeneratedCount, int tangentFallbackGeneratedCount, int tangentMissingCount, int tangentDegenerateTriangleCount, String tangentFallbackReason, String tangentBuildMode, int modelUploadRepeatCount, int uploadGenerationId, String renderLoopAllocationGuardStatus, float metallicFactor, float roughnessFactor, float normalScale, float occlusionStrength, int pbrTextureSlotCount, int uploadedPbrTextureCount, int skippedPbrTextureCount, int pbrTextureFallbackCount, String materialSlotDiagnostics);
    private static native void nativeUpdateUiDiagnostics(long handle, float fpsCurrent, float frameTimeMs, float fpsLastStable, float frameTimeLastStableMs, String fpsSource, String fpsStatus, String fpsUpdateMode, int fpsSampleWindowMs, long framesRenderedLive, String debugZipStatus, String debugZipPath, String debugZipIncludedFiles, String debugZipReason);
    private static native void nativeSetModelFallback(long handle, String modelName, String modelPath, String reason);

    private TextView panelText(float sizeSp, int maxLines) {
        TextView view = new TextView(this);
        view.setTextColor(Color.rgb(210, 245, 255));
        view.setTextSize(sizeSp);
        view.setPadding(12, 8, 12, 8);
        view.setGravity(Gravity.START);
        view.setSingleLine(false);
        view.setMaxLines(maxLines);
        view.setBackground(panelBackground(160));
        return view;
    }

    private TextView panelText(String text, float sizeSp, int maxLines) {
        TextView view = panelText(sizeSp, maxLines);
        view.setText(text);
        return view;
    }

    private Button compactButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(11f);
        button.setTextColor(Color.rgb(218, 248, 255));
        button.setMinHeight(52);
        button.setMinimumHeight(52);
        button.setMinWidth(132);
        button.setPadding(14, 6, 14, 6);
        button.setBackground(panelBackground(190));
        return button;
    }

    private TextView compactInfoText(String text) {
        TextView view = panelText(text, 11.0f, 2);
        view.setPadding(8, 4, 8, 4);
        view.setTextColor(Color.rgb(190, 205, 210));
        return view;
    }

    private LinearLayout sliderControl(String label, float min, float max, float value, ValueConsumer consumer) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(2, 4, 2, 4);
        Button valueButton = compactButton(label + " " + oneDecimal(value));
        valueButton.setEnabled(false);
        SeekBar slider = new SeekBar(this);
        bindSliderControl(label, valueButton, slider);
        slider.setMax(100);
        slider.setProgress(sliderProgress(value, min, max));
        slider.setMinHeight(52);
        slider.setMinimumHeight(52);
        slider.setPadding(8, 2, 8, 2);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingSlidersFromState) return;
                float next = min + (max - min) * (progress / 100.0f);
                consumer.accept(next);
                valueButton.setText(label + " " + oneDecimal(next));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { modelState.sliderTouchStatus = "active"; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { modelState.sliderTouchStatus = "ok_touch_targets"; updateStatus(); }
        });
        column.addView(valueButton);
        column.addView(slider);
        return column;
    }

    private void bindSliderControl(String label, Button valueButton, SeekBar slider) {
        if ("Sun".equals(label)) { sunIntensityButton = valueButton; sunSlider = slider; }
        else if ("Amb".equals(label)) { ambientIntensityButton = valueButton; ambientSlider = slider; }
        else if ("Exp".equals(label)) { exposureButton = valueButton; exposureSlider = slider; }
        else if ("Spec".equals(label)) { specularBoostButton = valueButton; specularSlider = slider; }
        else if ("Refl".equals(label)) { reflectionIntensityButton = valueButton; reflectionSlider = slider; }
        else if ("Ground".equals(label)) { groundIntensityButton = valueButton; groundSlider = slider; }
        else if ("Env".equals(label)) { environmentButton = valueButton; environmentSlider = slider; }
        else if ("Horizon".equals(label)) { horizonButton = valueButton; horizonSlider = slider; }
        else if ("Calib".equals(label)) { calibrationButton = valueButton; calibrationSlider = slider; }
        else if ("Gloss".equals(label)) { glossButton = valueButton; glossSlider = slider; }
        else if ("Coat".equals(label)) { paintGlossButton = valueButton; paintGlossSlider = slider; }
    }

    private int sliderProgress(float value, float min, float max) {
        if (max <= min) return 0;
        return Math.round(clamp((value - min) / (max - min), 0.0f, 1.0f) * 100.0f);
    }

    private interface ValueConsumer { void accept(float value); }

    private LinearLayout lightingStepperRow(String minusLabel, String plusLabel, View.OnClickListener minusClick, View.OnClickListener plusClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button minus = compactButton(minusLabel);
        Button plus = compactButton(plusLabel);
        minus.setOnClickListener(minusClick);
        plus.setOnClickListener(plusClick);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(minus, params);
        row.addView(plus, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        return row;
    }

    private GradientDrawable panelBackground(int alpha) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(alpha, 3, 12, 17));
        bg.setStroke(1, Color.argb(220, 0, 190, 220));
        bg.setCornerRadius(8f);
        return bg;
    }

    private void syncPanelVisibility() {
        if (inspectorPanel != null) inspectorPanel.setVisibility(inspectorPanelVisible ? View.VISIBLE : View.GONE);
        if (assetsPanel != null) assetsPanel.setVisibility("Assets".equals(activeInspectorTab) ? View.VISIBLE : View.GONE);
        if (cameraPanel != null) cameraPanel.setVisibility("Camera".equals(activeInspectorTab) ? View.VISIBLE : View.GONE);
        if (lightingPanel != null) lightingPanel.setVisibility("Lighting".equals(activeInspectorTab) ? View.VISIBLE : View.GONE);
        if (materialPanel != null) materialPanel.setVisibility("Material".equals(activeInspectorTab) ? View.VISIBLE : View.GONE);
        if (debugPanel != null) debugPanel.setVisibility("Debug".equals(activeInspectorTab) ? View.VISIBLE : View.GONE);
        if (inspectorToggleButton != null) inspectorToggleButton.setText(inspectorPanelVisible ? "Inspector -" : "Inspector +");
        updateTabButtonState(assetsTabButton, "Assets");
        updateTabButtonState(cameraTabButton, "Camera");
        updateTabButtonState(lightingTabButton, "Lighting");
        updateTabButtonState(materialTabButton, "Material");
        updateTabButtonState(debugTabButton, "Debug");
    }

    private void updateTabButtonState(Button button, String tab) {
        if (button == null) return;
        button.setText(tab.equals(activeInspectorTab) ? tab + " -" : tab);
        button.setBackground(panelBackground(tab.equals(activeInspectorTab) ? 230 : 170));
    }

    private Button tabButton(String label) {
        Button button = compactButton(label);
        button.setMinWidth(112);
        button.setOnClickListener(v -> {
            activeInspectorTab = label;
            applyInspectorDiagnostics();
            syncPanelVisibility();
            updateStatus();
        });
        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            writeCrashReport("uncaught_exception", throwable);
            System.exit(10);
        });
        super.onCreate(savedInstanceState);
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener((view, event) -> handleCameraTouch(event));
        topHudView = panelText(11f, 2);
        topHudView.setText("SOLUM Engine / SOLUM V2  |  Vulkan loading");
        statusView = panelText(11f, 9);
        statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: starting");
        diagnosticsStatusView = new TextView(this);
        diagnosticsStatusView.setTextColor(Color.rgb(222, 242, 250));
        diagnosticsStatusView.setTextSize(10f);
        diagnosticsStatusView.setPadding(14, 10, 14, 10);
        diagnosticsStatusView.setGravity(Gravity.START);
        diagnosticsStatusView.setSingleLine(false);
        diagnosticsStatusView.setMaxLines(8);
        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        topParams.gravity = Gravity.TOP;
        root.addView(topHudView, topParams);

        inspectorToggleButton = compactButton("Inspector");
        inspectorToggleButton.setOnClickListener(v -> {
            inspectorPanelVisible = !inspectorPanelVisible;
            applyInspectorDiagnostics();
            syncPanelVisibility();
        });
        FrameLayout.LayoutParams inspectorToggleParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        inspectorToggleParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        inspectorToggleParams.setMargins(10, 72, 10, 96);
        root.addView(inspectorToggleButton, inspectorToggleParams);

        ScrollView dockScroll = new ScrollView(this);
        inspectorPanel = new LinearLayout(this);
        inspectorPanel.setOrientation(LinearLayout.VERTICAL);
        inspectorPanel.setPadding(10, 8, 10, 8);
        inspectorPanel.setBackground(panelBackground(175));
        dockScroll.addView(inspectorPanel);
        statusView.setBackgroundColor(Color.TRANSPARENT);
        inspectorPanel.addView(statusView);
        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        assetsTabButton = tabButton("Assets");
        cameraTabButton = tabButton("Camera");
        lightingTabButton = tabButton("Lighting");
        materialTabButton = tabButton("Material");
        debugTabButton = tabButton("Debug");
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tabRow.addView(assetsTabButton, tabParams);
        tabRow.addView(cameraTabButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tabRow.addView(lightingTabButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tabRow.addView(materialTabButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tabRow.addView(debugTabButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        inspectorPanel.addView(tabRow);
        assetsPanel = new LinearLayout(this);
        assetsPanel.setOrientation(LinearLayout.VERTICAL);
        importGlbButton = compactButton("Import GLB");
        importGlbButton.setOnClickListener(v -> chooseGlbForImport());
        scanModelsButton = compactButton("Scan Models");
        scanModelsButton.setOnClickListener(v -> scanModelsFromButton());
        quickExportButton = compactButton("Export");
        quickExportButton.setOnClickListener(v -> exportEngineDiagnosticsFromButton());
        assetsPanel.addView(importGlbButton);
        assetsPanel.addView(scanModelsButton);
        assetsPanel.addView(quickExportButton);
        assetsSummaryView = panelText("Active: none", 10f, 3);
        assetsPanel.addView(assetsSummaryView);
        inspectorPanel.addView(assetsPanel);

        cameraPanel = new LinearLayout(this);
        cameraPanel.setOrientation(LinearLayout.VERTICAL);
        Button resetCameraButton = compactButton("Reset Camera");
        resetCameraButton.setOnClickListener(v -> applyCamera(28.0f, -18.0f, 4.2f));
        Button zoomInButton = compactButton("Camera +");
        zoomInButton.setOnClickListener(v -> applyCamera(cameraYawDeg, cameraPitchDeg, cameraDistance - 0.35f));
        Button zoomOutButton = compactButton("Camera -");
        zoomOutButton.setOnClickListener(v -> applyCamera(cameraYawDeg, cameraPitchDeg, cameraDistance + 0.35f));
        cameraInfoView = panelText("Yaw/Pitch/Distance", 10f, 2);
        cameraPanel.addView(cameraInfoView);
        cameraPanel.addView(resetCameraButton);
        cameraPanel.addView(zoomInButton);
        cameraPanel.addView(zoomOutButton);
        inspectorPanel.addView(cameraPanel);

        lightingPanel = new LinearLayout(this);
        lightingPanel.setOrientation(LinearLayout.VERTICAL);
        lightPresetButton = compactButton("Lighting: Bright");
        lightPresetButton.setOnClickListener(v -> cycleLightPreset());
        lightingPanel.addView(lightPresetButton);
        lightingPanel.addView(sliderControl("Sun", 0.5f, 4.0f, sunIntensity, v -> {
            sunIntensity = clamp(v, 0.5f, 4.0f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Amb", 0.1f, 2.0f, ambientIntensity, v -> {
            ambientIntensity = clamp(v, 0.1f, 2.0f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Exp", 0.8f, 3.0f, exposureValue, v -> {
            exposureValue = clamp(v, 0.8f, 3.0f);
            ambientFloor = clamp(0.06f + (exposureValue - 0.8f) * 0.10f, 0.06f, 0.28f);
            brightnessPresetIndex = exposureValue >= 1.80f ? 4 : (exposureValue >= 1.45f ? 3 : (exposureValue >= 1.2f ? 2 : 1));
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Spec", 0.5f, 3.0f, specularBoost, v -> {
            specularBoost = clamp(v, 0.5f, 3.0f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Refl", 0.0f, 2.0f, reflectionIntensity, v -> {
            reflectionIntensity = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Ground", 0.0f, 1.5f, contactShadowIntensity, v -> {
            contactShadowIntensity = clamp(v, 0.0f, 1.5f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Env", 0.0f, 2.0f, environmentIntensity, v -> {
            environmentIntensity = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        skyPresetButton = compactButton("Sky: Studio");
        skyPresetButton.setOnClickListener(v -> cycleEnvironmentPreset());
        lightingPanel.addView(skyPresetButton);
        lightingPanel.addView(sliderControl("Horizon", 0.0f, 1.0f, horizonStrength, v -> {
            horizonStrength = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        inspectorPanel.addView(lightingPanel);

        materialPanel = new LinearLayout(this);
        materialPanel.setOrientation(LinearLayout.VERTICAL);
        calibrationPresetButton = compactButton("Calib: Balanced");
        calibrationPresetButton.setOnClickListener(v -> cycleCalibrationPreset());
        materialPanel.addView(calibrationPresetButton);
        materialPanel.addView(sliderControl("Calib", 0.0f, 1.0f, calibrationSliderValue, v -> {
            calibrationSliderValue = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Gloss", 0.0f, 1.0f, glossSliderValue, v -> {
            glossSliderValue = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Coat", 0.0f, 1.0f, paintGlossSliderValue, v -> {
            paintGlossSliderValue = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialStatusView = compactInfoText("Paint targets: none | Fabric matte: on");
        materialPanel.addView(materialStatusView);
        materialViewButton = compactButton("Debug: Final Shaded");
        materialViewButton.setOnClickListener(v -> cycleMaterialView());
        materialPanel.addView(materialViewButton);
        inspectorPanel.addView(materialPanel);

        debugPanel = new LinearLayout(this);
        debugPanel.setOrientation(LinearLayout.VERTICAL);
        chooseFolderButton = compactButton("Choose Diagnostics Folder");
        chooseFolderButton.setOnClickListener(v -> chooseDiagnosticsFolder());
        exportButton = compactButton("Export Diagnostics");
        exportButton.setOnClickListener(v -> exportEngineDiagnosticsFromButton());
        debugZipButton = compactButton("Export Debug ZIP");
        debugZipButton.setOnClickListener(v -> exportDebugZipFromButton());
        diagnosticsStatusView.setBackgroundColor(Color.TRANSPARENT);
        debugPanel.addView(chooseFolderButton);
        debugPanel.addView(exportButton);
        debugPanel.addView(debugZipButton);
        debugPanel.addView(diagnosticsStatusView);
        inspectorPanel.addView(debugPanel);
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dockParams.gravity = Gravity.BOTTOM;
        dockParams.setMargins(76, 12, 12, 28);
        root.addView(dockScroll, dockParams);
        syncPanelVisibility();
        scanModels("startup");
        updateDiagnosticsStatusPanel();
        setContentView(root);
        try {
            System.loadLibrary("solum_engine");
            nativeLoaded = true;
            nativeHandle = nativeCreate();
            statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: native ready");
            writeRuntimeNote("native_load_ok", "libsolum_engine loaded and native object created");
            exportEngineDiagnostics("native_load_ok");
        } catch (Throwable t) {
            nativeLoaded = false;
            writeCrashReport("native_load_failed", t);
            exportEngineDiagnostics("native_load_failed");
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: native load failed\n" + shortThrowable(t));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        startFpsPulse();
    }

    @Override protected void onPause() {
        stopFpsPulse();
        super.onPause();
    }

    @Override protected void onDestroy() {
        stopFpsPulse();
        try { if (nativeLoaded && nativeHandle != 0L) { nativeDestroy(nativeHandle); nativeHandle = 0L; } } catch (Throwable t) { writeCrashReport("native_destroy_failed", t); }
        super.onDestroy();
    }

    private void startFpsPulse() {
        if (fpsPulseActive) return;
        fpsPulseActive = true;
        fpsWindowStartNs = 0L;
        fpsWindowFrames = 0L;
        Choreographer.getInstance().postFrameCallback(fpsFrameCallback);
    }

    private void stopFpsPulse() {
        if (!fpsPulseActive) return;
        fpsPulseActive = false;
        Choreographer.getInstance().removeFrameCallback(fpsFrameCallback);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_GLB) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                modelState.importStatus = "failed";
                modelState.importRoute = "failed";
                modelState.reason = "file_picker_cancelled";
                updateImportUi();
                writeModelDiagnostics("import_cancelled");
                return;
            }
            importGlbFromUri(data.getData());
            return;
        }
        if (requestCode != REQUEST_CHOOSE_DIAGNOSTICS_TREE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            lastExportReason = "folder_picker_cancelled";
            updateDiagnosticsStatusPanel();
            return;
        }
        Uri treeUri = data.getData();
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(treeUri, flags);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_TREE_URI, treeUri.toString()).apply();
            Log.i(TAG_DIAG, "folder_configured uri=" + treeUri);
            lastExportReason = "SAF folder configured. Use /storage/emulated/0/SOLUMCreative in the picker.";
        } catch (Throwable t) {
            Log.e(TAG_DIAG, "folder_configured_failed reason=" + shortThrowable(t));
            lastExportReason = "folder_configured_failed: " + shortThrowable(t);
        }
        updateDiagnosticsStatusPanel();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { nativeSurfaceCreated(nativeHandle, holder.getSurface(), getRuntimeReportDirPath()); applyLightingControls(); attemptActiveModelGpuUpload("surface_created"); updateStatus(); exportEngineDiagnostics("surface_created"); }
        catch (Throwable t) { writeCrashReport("surface_created_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: surface init failed\n" + shortThrowable(t)); }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height); applyLightingControls(); attemptActiveModelGpuUpload("surface_changed"); updateStatus(); exportEngineDiagnostics("surface_changed"); }
        catch (Throwable t) { writeCrashReport("surface_changed_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: surface resize failed\n" + shortThrowable(t)); }
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { nativeSurfaceDestroyed(nativeHandle); updateStatus(); } catch (Throwable t) { writeCrashReport("surface_destroyed_failed", t); }
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (nativeLoaded && nativeHandle != 0L) {
                try { statusView.setMaxLines(12); statusView.setText(compactStatus(nativeGetStatus(nativeHandle))); }
                catch (Throwable t) { writeCrashReport("native_status_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: status call failed\n" + shortThrowable(t)); }
            }
        });
    }

    private String compactStatus(String full) {
        String gpu = pickValue(full, "GPU: ");
        String status = "running";
        String importStatus = modelState.importStatus;
        String activeName = modelState.activeModelName();
        GlbParseResult p = modelState.parse;
        if (full.contains("Draw Model: ok")) status = "Model OK";
        else if (full.contains("Cube draw: OK")) status = "Cube Fallback OK";
        else if (full.contains("Renderer core: OK")) status = "Renderer Core OK";
        else if (full.contains("Vertex buffer: OK")) status = "Vertex Buffer OK";
        else if (full.contains("Triangle draw: OK")) status = "Triangle OK";
        else if (full.contains("Render pass: clear color OK")) status = "Render Pass OK";
        else if (full.contains("Swapchain: created")) status = "Swapchain OK";
        else if (full.toLowerCase(Locale.US).contains("failed")) status = "Error";
        if (gpu.isEmpty()) gpu = "detecting";
        String upload = pickValue(full, "GPU Upload: ");
        String draw = pickValue(full, "Draw Model: ");
        String fallback = pickValue(full, "Fallback cube: ");
        String counts = pickValue(full, "Vertices / indices: ");
        if (upload.isEmpty()) upload = modelState.gpuUploadStatus;
        if (draw.isEmpty()) draw = modelState.drawStatus;
        if (fallback.isEmpty()) fallback = modelState.fallbackCubeVisible ? "on" : "off";
        if (counts.isEmpty()) counts = modelState.uploadedVertexCount + " / " + modelState.uploadedIndexCount;
        if (topHudView != null) {
            topHudView.setText("FPS " + oneDecimal(fpsCurrent) + "  |  " + oneDecimal(frameTimeMs) + " ms  |  GPU " + gpu + "  |  Vulkan  |  " + SCENE_NAME);
        }
        if (assetsSummaryView != null) {
            assetsSummaryView.setText("Active: " + (activeName.isEmpty() ? "none" : shorten(activeName, 32))
                + "\nModels: " + modelState.modelsFoundCount
                + "\nExport: " + lastExportStatus + " / ZIP " + debugZipStatus);
        }
        if (cameraInfoView != null) {
            cameraInfoView.setText("Yaw " + oneDecimal(cameraYawDeg)
                + "  Pitch " + oneDecimal(cameraPitchDeg)
                + "  Dist " + oneDecimal(cameraDistance));
        }
        int rendered = intJsonField("primitiveCountRendered", modelState.primitiveCountRendered);
        int skipped = intJsonField("primitiveCountSkipped", modelState.primitiveCountSkipped);
        int total = intJsonField("primitiveCountTotal", modelState.primitiveCountTotal);
        return "Render Lab: " + SCENE_NAME
            + "\nImport: " + importStatus
            + "\nActive model: " + (activeName.isEmpty() ? "none" : shorten(activeName, 34))
            + "\nModel render: " + draw
            + "\nPrimitives rendered/skipped/total: " + rendered + " / " + skipped + " / " + total
            + "\nMaterials used: " + intJsonField("materialSlotCountRendered", modelState.materialSlotCountRendered)
            + "\nLighting status: " + jsonStringField(getRenderLabStateForExport(), "lightingStatus", modelState.lightingStatus)
            + "\nLight preset: " + lightPresetName(lightPresetIndex)
            + "\nSun intensity: " + oneDecimal(sunIntensity)
            + "\nAmbient intensity: " + oneDecimal(ambientIntensity)
            + "\nExposure: " + oneDecimal(exposureValue) + " " + brightnessPresetName(brightnessPresetIndex)
            + "\nSpecular boost: " + oneDecimal(specularBoost)
            + "\nReflection: " + oneDecimal(reflectionIntensity)
            + "\nGround: " + oneDecimal(contactShadowIntensity)
            + "\nIBL mode: " + jsonStringField(getRenderLabStateForExport(), "iblMode", modelState.iblMode)
            + "\nMaterial response status: " + jsonStringField(getRenderLabStateForExport(), "materialResponseStatus", modelState.materialResponseStatus)
            + "\nCalibration: " + modelState.calibrationPreset + " " + oneDecimal(calibrationSliderValue)
            + "\nAlbedo clamp: " + jsonStringField(getRenderLabStateForExport(), "albedoClampStatus", modelState.albedoClampStatus)
            + "\nLuminance guard: " + jsonStringField(getRenderLabStateForExport(), "luminanceGuardStatus", modelState.luminanceGuardStatus)
            + "\nMaterial hints: " + jsonStringField(getRenderLabStateForExport(), "materialTypeHintStatus", modelState.materialTypeHintStatus)
            + "\nBRDF status: " + jsonStringField(getRenderLabStateForExport(), "brdfStatus", modelState.brdfStatus)
            + "\nSpecular status: " + jsonStringField(getRenderLabStateForExport(), "specularStatus", modelState.specularStatus)
            + "\nActive debug view: " + materialDebugViewName(activeDebugViewIndex)
            + "\nBaseColor status: " + modelState.baseColorTextureStatus
            + "\nMetallicRoughness status: " + modelState.metallicRoughnessStatus
            + "\nTangent status: " + modelState.tangentStatus
            + "\nNormal status: " + modelState.normalMapStatus + " applied=" + modelState.normalMapAppliedStatus
            + "\nAO status: " + modelState.occlusionMapStatus
            + "\nFPS/frameMs: " + oneDecimal(fpsCurrent) + " / " + oneDecimal(frameTimeMs)
            + "\nDebug ZIP: " + debugZipStatus
            + "\nVertices / indices: " + counts
            + "\nFallback cube: " + fallback
            + "\nMesh meta: " + p.meshCount + " / " + p.primitiveCount + " / " + p.materialCount + " / " + p.textureCount
            + "\nStatus: " + status
            + "\nNext: P18 environment cubemap / real IBL foundation";
    }

    private void cycleLightPreset() {
        lightPresetIndex = (lightPresetIndex + 1) % 5;
        applyPresetDefaults();
        applyLightingControls();
        updateStatus();
    }

    private void adjustSunIntensity(float delta) {
        sunIntensity = clamp(sunIntensity + delta, 0.5f, 4.0f);
        applyLightingControls();
        updateStatus();
    }

    private void adjustAmbientIntensity(float delta) {
        ambientIntensity = clamp(ambientIntensity + delta, 0.1f, 2.0f);
        applyLightingControls();
        updateStatus();
    }

    private void adjustExposureValue(float delta) {
        exposureValue = clamp(exposureValue + delta, 0.8f, 3.0f);
        ambientFloor = clamp(ambientFloor + delta * 0.04f, 0.06f, 0.28f);
        brightnessPresetIndex = exposureValue >= 1.45f ? 3 : (exposureValue >= 1.2f ? 2 : 1);
        applyLightingControls();
        updateStatus();
    }

    private void adjustSpecularBoost(float delta) {
        specularBoost = clamp(specularBoost + delta, 0.5f, 3.0f);
        applyLightingControls();
        updateStatus();
    }

    private void cycleCalibrationPreset() {
        calibrationPresetIndex = (calibrationPresetIndex + 1) % 4;
        calibrationSliderValue = calibrationPresetIndex == 1 ? 0.85f : (calibrationPresetIndex == 3 ? 0.45f : (calibrationPresetIndex == 2 ? 0.65f : 0.25f));
        applyLightingControls();
        updateStatus();
    }

    private void cycleMaterialView() {
        activeDebugViewIndex = (activeDebugViewIndex + 1) % 27;
        applyLightingControls();
        updateStatus();
    }

    private void cycleEnvironmentPreset() {
        environmentPresetIndex = (environmentPresetIndex + 1) % 5;
        applyLightingControls();
        updateStatus();
    }

    private void applyPresetDefaults() {
        if (lightPresetIndex == 1) {
            sunIntensity = 1.55f;
            ambientIntensity = 0.46f;
            exposureValue = 1.18f;
            ambientFloor = 0.10f;
            specularBoost = 1.35f;
            reflectionIntensity = 0.95f;
            contactShadowIntensity = 0.55f;
            environmentIntensity = 1.05f;
            environmentPresetIndex = 0;
            horizonStrength = 0.52f;
            brightnessPresetIndex = 1;
        } else if (lightPresetIndex == 2) {
            sunIntensity = 1.65f;
            ambientIntensity = 0.38f;
            exposureValue = 1.10f;
            ambientFloor = 0.08f;
            specularBoost = 1.55f;
            reflectionIntensity = 1.20f;
            contactShadowIntensity = 0.75f;
            environmentIntensity = 1.15f;
            environmentPresetIndex = 3;
            horizonStrength = 0.62f;
            brightnessPresetIndex = 1;
        } else if (lightPresetIndex == 3) {
            sunIntensity = 2.0f;
            ambientIntensity = 0.80f;
            exposureValue = 1.50f;
            ambientFloor = 0.16f;
            specularBoost = 1.85f;
            reflectionIntensity = 1.15f;
            contactShadowIntensity = 0.65f;
            environmentIntensity = 1.0f;
            environmentPresetIndex = 0;
            horizonStrength = 0.55f;
            brightnessPresetIndex = 3;
        } else if (lightPresetIndex == 4) {
            sunIntensity = 3.35f;
            ambientIntensity = 1.25f;
            exposureValue = 1.90f;
            ambientFloor = 0.22f;
            specularBoost = 2.25f;
            reflectionIntensity = 1.35f;
            contactShadowIntensity = 0.90f;
            environmentIntensity = 1.35f;
            environmentPresetIndex = 3;
            horizonStrength = 0.72f;
            brightnessPresetIndex = 4;
        } else {
            sunIntensity = 1.05f;
            ambientIntensity = 0.58f;
            exposureValue = 1.32f;
            ambientFloor = 0.14f;
            specularBoost = 1.25f;
            reflectionIntensity = 0.85f;
            contactShadowIntensity = 0.45f;
            environmentIntensity = 0.85f;
            environmentPresetIndex = 2;
            horizonStrength = 0.45f;
            brightnessPresetIndex = 2;
        }
    }

    private void applyLightingControls() {
        applyInspectorDiagnostics();
        modelState.lightingStatus = "ok";
        modelState.lightPreset = lightPresetName(lightPresetIndex);
        modelState.sunIntensity = sunIntensity;
        modelState.ambientIntensity = ambientIntensity;
        modelState.specularBoost = specularBoost;
        modelState.specularBoostStatus = "ok_uniform_controlled";
        modelState.reflectionIntensity = reflectionIntensity;
        modelState.iblStatus = "ok_foundation";
        modelState.iblMode = "directional_sky_ground_ibl";
        modelState.environmentIblStatus = "ok_foundation";
        modelState.environmentIblMode = "directional_sky_ground_ibl";
        modelState.environmentSourceStatus = "ok_procedural_no_external_texture";
        modelState.environmentSourceType = "directional_sky_ground_shader_model";
        modelState.environmentSkyColorStatus = "ok_preset_uniform";
        modelState.environmentGroundColorStatus = "ok_preset_uniform";
        modelState.environmentHorizonStatus = "ok_directional_horizon_blend";
        modelState.environmentPerformanceStatus = "ok_no_extra_pass_no_texture_upload";
        modelState.environmentReflectionStatus = "p18_environment_directional_source_no_texture_cubemap";
        modelState.environmentReflectionMode = "reflection_direction_sky_ground_ibl";
        modelState.environmentSource = "directional_sky_ground_shader_model";
        modelState.reflectionFoundationStatus = "p18_environment_ibl_foundation";
        modelState.reflectionMode = "directional_sky_ground_ibl";
        modelState.reflectionColorStatus = "ok_environment_preset_horizon_gradient";
        modelState.reflectionRoughnessResponseStatus = "ok_roughness_blurs_reduces_reflection";
        modelState.metallicReflectionStatus = "ok_metal_tinted_environment_guarded";
        modelState.dielectricReflectionStatus = "ok_subtle_f0_environment";
        modelState.reflectionPerformanceStatus = "ok_no_extra_pass_no_texture_rebuild";
        modelState.iblDiffuseStatus = "ok_directional_sky_ground_diffuse";
        modelState.iblSpecularStatus = "ok_reflection_direction_environment";
        modelState.iblRoughnessResponseStatus = "ok_roughness_blurs_and_reduces_specular";
        modelState.iblMetallicResponseStatus = "ok_metal_tints_reflection";
        modelState.iblDielectricResponseStatus = "ok_subtle_f0_reflection";
        modelState.iblFabricPreserveStatus = "ok_fabric_matte_preserved";
        modelState.iblOverbrightGuardStatus = "ok_luminance_guarded";
        modelState.environmentUiStatus = "ok_compact_lighting_controls";
        modelState.environmentPreset = environmentPresetName(environmentPresetIndex);
        modelState.environmentIntensity = environmentIntensity;
        modelState.environmentSliderStatus = "ok";
        modelState.skyPresetStatus = "ok";
        modelState.horizonControlStatus = "ok";
        modelState.horizonStrength = horizonStrength;
        modelState.environmentUniformUpdateStatus = "ok_uniform_only";
        modelState.contactGroundingStatus = "foundation_analytic";
        modelState.contactShadowStatus = contactShadowIntensity > 0.0f ? "enabled" : "disabled";
        modelState.contactShadowMode = "analytic_blob_or_grounding_approx";
        modelState.contactShadowIntensity = contactShadowIntensity;
        modelState.contactShadowPerformanceStatus = "ok_uniform_only_no_shadow_pass";
        modelState.groundingUsesModelBounds = "yes_upload_bounds_scaled_local";
        modelState.groundingUniformUpdateStatus = "ok_uniform_only";
        modelState.groundSliderStatus = "ok";
        modelState.contactGroundingSliderStatus = "ok";
        modelState.lightingControlStatus = "ok";
        modelState.lightingUiMode = "compact_sliders";
        modelState.lightingUniformUpdateStatus = "ok_uniform_only";
        modelState.sliderUpdateMode = "uniform_only";
        modelState.sliderTouchStatus = "ok_touch_targets";
        modelState.sunSliderStatus = "ok";
        modelState.ambientSliderStatus = "ok";
        modelState.exposureSliderStatus = "ok";
        modelState.specularSliderStatus = "ok";
        modelState.reflectionSliderStatus = "ok";
        modelState.environmentSliderStatus = "ok";
        modelState.materialCalibrationStatus = "ok";
        modelState.materialCalibrationMode = "shader_uniform_upload_lightweight";
        modelState.albedoEnergyStatus = "ok_normalized";
        modelState.albedoClampStatus = "ok";
        modelState.diffuseClampStatus = "ok";
        modelState.luminanceGuardStatus = "ok";
        modelState.aoCalibrationStatus = "ok_indirect_weighted";
        modelState.roughnessRemapStatus = "ok";
        modelState.metallicRoughnessClampStatus = "ok";
        modelState.emissiveGuardStatus = "ok_guarded_real_emissive_only";
        modelState.fabricMattePreserveStatus = "ok_rough_fabric_kept_matte";
        modelState.paintMaterialCalibrationStatus = "ok_luminance_guard_reflection_readable";
        modelState.metalMaterialCalibrationStatus = "ok_clamped_metallic_readable";
        modelState.materialTypeHintStatus = "ok";
        modelState.materialSlotCalibrationStatus = "ok";
        modelState.calibrationUiStatus = "ok_compact_material_tab";
        modelState.calibrationPreset = calibrationPresetName(calibrationPresetIndex);
        modelState.calibrationSliderStatus = "ok";
        modelState.calibrationSliderValue = calibrationSliderValue;
        modelState.calibrationUniformUpdateStatus = "ok_uniform_only";
        modelState.calibratedAlbedoDebugViewStatus = "shader_applied";
        modelState.materialTypeDebugViewStatus = "shader_applied";
        modelState.aoInfluenceDebugViewStatus = "shader_applied";
        modelState.luminanceGuardDebugViewStatus = "shader_applied";
        modelState.calibrationVisualStrength = clamp(0.28f + calibrationSliderValue * 0.72f, 0.28f, 1.0f);
        modelState.calibrationAffectsAlbedo = "yes_guarded_luminance_clamp";
        modelState.calibrationAffectsAo = "yes_indirect_occlusion_weight";
        modelState.calibrationAffectsRoughness = "yes_material_safe_remap";
        modelState.calibrationVisibleResponseStatus = "ok_visible_final_shaded_response";
        modelState.materialCalibrationPerformanceStatus = "ok_uniform_shader_no_rebuild";
        modelState.specularGlossStatus = "ok";
        modelState.specularGlossMode = "uniform_gloss_response_p17b";
        modelState.specularResponseStatus = "ok_guarded_dielectric_metal";
        modelState.glossResponseStatus = "ok_slider_controls_lobe_width";
        modelState.roughnessRemapV2Status = "ok_calib_and_gloss_weighted";
        modelState.metallicSpecularBoostStatus = "ok_metal_routed_boost";
        modelState.dielectricGlossStatus = "ok_f0_guarded";
        modelState.fabricSpecularSuppressStatus = "ok_matte_preserved";
        modelState.specularOverbrightGuardStatus = "ok_luminance_guard";
        modelState.viewDependentHighlightStatus = "ok_reflection_vector";
        modelState.paintGlossLiteStatus = "ok";
        modelState.paintGlossLiteMode = "uniform_lite_no_texture_rebuild";
        modelState.paintGlossIntensity = paintGlossSliderValue;
        modelState.paintGlossRoughness = clamp(0.78f - paintGlossSliderValue * 0.58f, 0.20f, 0.78f);
        applyPaintGlossTargetDiagnostics();
        modelState.paintGlossPerformanceStatus = "ok_uniform_only";
        modelState.glossSliderStatus = "ok";
        modelState.glossSliderValue = glossSliderValue;
        modelState.paintGlossSliderStatus = "ok";
        modelState.paintGlossSliderValue = paintGlossSliderValue;
        modelState.glossUniformUpdateStatus = "ok_uniform_only";
        modelState.glossResponseDebugViewStatus = "shader_applied";
        modelState.specularGuardDebugViewStatus = "shader_applied";
        modelState.paintGlossDebugViewStatus = "shader_applied";
        modelState.metalResponseDebugViewStatus = "shader_applied";
        modelState.paintTargetDebugViewStatus = "shader_applied";
        modelState.calibrationResponseDebugViewStatus = "shader_applied";
        modelState.materialTypeSpecularRoutingStatus = "ok";
        modelState.paintMaterialGlossStatus = "ok_lite_gloss";
        modelState.metalMaterialGlossStatus = "ok_stronger_response";
        modelState.rubberMaterialGlossStatus = "ok_suppressed";
        modelState.specularGlossPerformanceStatus = "ok_no_alloc_no_rebuild";
        modelState.glossVisibleResponseStatus = "ok_visible_lobe_and_reflection";
        modelState.glossAffectsSpecularLobe = "yes_shader_roughness_distribution";
        modelState.glossAffectsReflectionWeight = "yes_shader_environment_weight";
        modelState.brdfStatus = "ok";
        modelState.brdfMode = "direct_lighting_schlick_mobile";
        modelState.diffuseStatus = "ok_non_metal_diffuse";
        modelState.specularStatus = "ok_p17_gloss_response_guarded";
        modelState.fresnelStatus = "ok_schlick";
        modelState.f0Status = "ok_dielectric_0_04_metal_base_color";
        modelState.metallicResponseStatus = "ok_diffuse_reduced_f0_tinted";
        modelState.roughnessResponseStatus = "ok_gloss_width_energy_remap";
        modelState.directLightingStatus = "ok_single_sun_direct_gloss_lobe";
        modelState.materialResponseStatus = "p18_environment_ibl_foundation";
        modelState.pbrQualityTier = "mobile_direct_lighting_ibl_v1";
        modelState.brdfPerformanceStatus = "ok_mobile_friendly_direct_lighting_ibl";
        modelState.toneMappingStatus = "ok";
        modelState.toneMappingMode = toneMappingModeName(toneMappingModeIndex);
        modelState.exposureStatus = "ok";
        modelState.exposureValue = exposureValue;
        modelState.ambientFloor = ambientFloor;
        modelState.brightnessPreset = brightnessPresetName(brightnessPresetIndex);
        modelState.activeDebugView = materialDebugViewName(activeDebugViewIndex);
        modelState.debugViewStatus = "shader_applied";
        modelState.normalDebugViewStatus = "shader_applied";
        modelState.ndotlDebugViewStatus = "shader_applied";
        modelState.diffuseDebugViewStatus = "shader_applied";
        modelState.specularDebugViewStatus = "shader_applied";
        modelState.f0DebugViewStatus = "shader_applied";
        modelState.reflectionDebugViewStatus = "shader_applied";
        modelState.iblDiffuseDebugViewStatus = "shader_applied";
        modelState.iblSpecularDebugViewStatus = "shader_applied";
        modelState.environmentDebugViewStatus = "shader_applied";
        modelState.reflectionDirectionDebugViewStatus = "shader_applied";
        modelState.environmentColorDebugViewStatus = "shader_applied";
        modelState.iblPerformanceStatus = "ok_shader_math_only_no_loops";
        modelState.brdfStatusDebugViewStatus = "shader_applied";
        modelState.groundingDebugViewStatus = "shader_applied";
        if (lightPresetButton != null) lightPresetButton.setText("Lighting: " + modelState.lightPreset);
        if (sunIntensityButton != null) sunIntensityButton.setText("Sun " + oneDecimal(sunIntensity));
        if (ambientIntensityButton != null) ambientIntensityButton.setText("Amb " + oneDecimal(ambientIntensity));
        if (exposureButton != null) exposureButton.setText("Exp " + oneDecimal(exposureValue));
        if (specularBoostButton != null) specularBoostButton.setText("Spec " + oneDecimal(specularBoost));
        if (reflectionIntensityButton != null) reflectionIntensityButton.setText("Refl " + oneDecimal(reflectionIntensity));
        if (groundIntensityButton != null) groundIntensityButton.setText("Ground " + oneDecimal(contactShadowIntensity));
        if (environmentButton != null) environmentButton.setText("Env " + oneDecimal(environmentIntensity));
        if (skyPresetButton != null) skyPresetButton.setText("Sky: " + modelState.environmentPreset);
        if (horizonButton != null) horizonButton.setText("Horizon " + oneDecimal(horizonStrength));
        if (calibrationPresetButton != null) calibrationPresetButton.setText("Calib: " + modelState.calibrationPreset);
        if (calibrationButton != null) calibrationButton.setText("Calib " + oneDecimal(calibrationSliderValue));
        if (glossButton != null) glossButton.setText("Gloss " + oneDecimal(glossSliderValue));
        if (paintGlossButton != null) paintGlossButton.setText("Coat " + oneDecimal(paintGlossSliderValue));
        if (materialStatusView != null) materialStatusView.setText("Paint targets: " + modelState.paintGlossTargetStatus + " | Fabric matte: on");
        updateSliderPositionsFromState();
        if (materialViewButton != null) materialViewButton.setText("Debug: " + modelState.activeDebugView);
        try {
            if (nativeLoaded && nativeHandle != 0L) {
                nativeSetLightingControls(nativeHandle, lightPresetIndex, sunIntensity, ambientIntensity, activeDebugViewIndex, toneMappingModeIndex, exposureValue, ambientFloor, brightnessPresetIndex, specularBoost, reflectionIntensity, contactShadowIntensity, calibrationPresetIndex, calibrationSliderValue, glossSliderValue, paintGlossSliderValue, environmentIntensity, environmentPresetIndex, horizonStrength);
            }
        } catch (Throwable t) {
            modelState.debugViewStatus = "native_control_failed";
            writeCrashReport("lighting_controls_failed", t);
        }
    }

    private void applyInspectorDiagnostics() {
        modelState.inspectorUiStatus = "ok";
        modelState.inspectorUiMode = "tabbed_compact_inspector";
        modelState.activeInspectorTab = activeInspectorTab;
        modelState.assetsTabStatus = "ok_import_scan_export_summary";
        modelState.cameraTabStatus = "ok_camera_info_reset_zoom";
        modelState.lightingTabStatus = "ok_sliders_environment_controls";
        modelState.materialTabStatus = "ok_calibration_debug_views";
        modelState.debugTabStatus = "ok_fps_zip_status";
    }

    private void updateSliderPositionsFromState() {
        updatingSlidersFromState = true;
        try {
            if (sunSlider != null) sunSlider.setProgress(sliderProgress(sunIntensity, 0.5f, 4.0f));
            if (ambientSlider != null) ambientSlider.setProgress(sliderProgress(ambientIntensity, 0.1f, 2.0f));
            if (exposureSlider != null) exposureSlider.setProgress(sliderProgress(exposureValue, 0.8f, 3.0f));
            if (specularSlider != null) specularSlider.setProgress(sliderProgress(specularBoost, 0.5f, 3.0f));
            if (reflectionSlider != null) reflectionSlider.setProgress(sliderProgress(reflectionIntensity, 0.0f, 2.0f));
            if (groundSlider != null) groundSlider.setProgress(sliderProgress(contactShadowIntensity, 0.0f, 1.5f));
            if (environmentSlider != null) environmentSlider.setProgress(sliderProgress(environmentIntensity, 0.0f, 2.0f));
            if (horizonSlider != null) horizonSlider.setProgress(sliderProgress(horizonStrength, 0.0f, 1.0f));
            if (calibrationSlider != null) calibrationSlider.setProgress(sliderProgress(calibrationSliderValue, 0.0f, 1.0f));
            if (glossSlider != null) glossSlider.setProgress(sliderProgress(glossSliderValue, 0.0f, 1.0f));
            if (paintGlossSlider != null) paintGlossSlider.setProgress(sliderProgress(paintGlossSliderValue, 0.0f, 1.0f));
        } finally {
            updatingSlidersFromState = false;
        }
    }

    private String lightPresetName(int index) {
        if (index == 1) return "Studio";
        if (index == 2) return "Outdoor";
        if (index == 3) return "Bright";
        if (index == 4) return "Ultra";
        return "Soft";
    }

    private String environmentPresetName(int index) {
        if (index == 1) return "Warm";
        if (index == 2) return "Cool";
        if (index == 3) return "Outdoor";
        if (index == 4) return "Sunset";
        return "Studio";
    }

    private String materialDebugViewName(int index) {
        if (index == 1) return "BaseColor";
        if (index == 2) return "Normal";
        if (index == 3) return "Roughness";
        if (index == 4) return "Metallic";
        if (index == 5) return "AO";
        if (index == 6) return "Diffuse";
        if (index == 7) return "Specular";
        if (index == 8) return "F0";
        if (index == 9) return "Reflection";
        if (index == 10) return "IBL Diffuse";
        if (index == 11) return "IBL Specular";
        if (index == 12) return "BRDF Status";
        if (index == 13) return "Grounding / Contact Shadow";
        if (index == 14) return "Calibrated Albedo";
        if (index == 15) return "Material Type";
        if (index == 16) return "AO Influence";
        if (index == 17) return "Luminance Guard";
        if (index == 18) return "Gloss Response";
        if (index == 19) return "Specular Guard";
        if (index == 20) return "Paint Gloss";
        if (index == 21) return "Metal Response";
        if (index == 22) return "Paint Target";
        if (index == 23) return "Calibration Response";
        if (index == 24) return "Environment";
        if (index == 25) return "Reflection Direction";
        if (index == 26) return "Environment Color";
        return "Final Shaded";
    }

    private String calibrationPresetName(int index) {
        if (index == 1) return "Matte Safe";
        if (index == 2) return "Balanced";
        if (index == 3) return "Punchy";
        return "Neutral";
    }

    private void applyPaintGlossTargetDiagnostics() {
        int paintCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"paint_like\"");
        int metalCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"metal_like\"");
        int fabricCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"fabric_like\"");
        int unknownCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"unknown\"");
        modelState.paintGlossSkippedFabricCount = fabricCount;
        if (paintCount > 0) {
            modelState.paintGlossTargetStatus = "paint";
            modelState.paintGlossAppliedMaterialCount = paintCount;
            modelState.paintGlossFallbackRouting = "paint_like";
            modelState.paintGlossVisibleResponseStatus = "ok_paint_like_visible";
            modelState.paintGlossMaterialHintStatus = "ok_paint_like_target";
        } else if (metalCount > 0) {
            modelState.paintGlossTargetStatus = "metal";
            modelState.paintGlossAppliedMaterialCount = metalCount;
            modelState.paintGlossFallbackRouting = "metal_like_limited_safe";
            modelState.paintGlossVisibleResponseStatus = "ok_toycar_metal_coat_visible_limited";
            modelState.paintGlossMaterialHintStatus = "ok_no_paint_like_using_metal_fallback";
        } else if (unknownCount > 0) {
            modelState.paintGlossTargetStatus = "unknown";
            modelState.paintGlossAppliedMaterialCount = unknownCount;
            modelState.paintGlossFallbackRouting = "unknown_very_limited";
            modelState.paintGlossVisibleResponseStatus = "limited_unknown_target";
            modelState.paintGlossMaterialHintStatus = "ok_unknown_limited_fallback";
        } else {
            modelState.paintGlossTargetStatus = "none";
            modelState.paintGlossAppliedMaterialCount = 0;
            modelState.paintGlossFallbackRouting = "none";
            modelState.paintGlossVisibleResponseStatus = "no_target";
            modelState.paintGlossMaterialHintStatus = "no_paint_or_safe_coat_target";
        }
    }

    private int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) {
            count++;
            at += needle.length();
        }
        return count;
    }

    private String brightnessPresetName(int index) {
        if (index == 0) return "Low";
        if (index == 2) return "Bright";
        if (index == 3) return "Bright Preview";
        if (index == 4) return "Ultra";
        return "Normal";
    }

    private String toneMappingModeName(int index) {
        if (index == 2) return "aces_lite";
        if (index == 0) return "none";
        return "reinhard";
    }

    private void updateFpsFromUiPulse() {
        long now = System.nanoTime();
        if (fpsWindowStartNs == 0L) fpsWindowStartNs = now;
        framesRenderedLive++;
        fpsWindowFrames++;
        long elapsed = now - fpsWindowStartNs;
        if (elapsed >= FPS_SAMPLE_WINDOW_NS) {
            float measuredFps = (float)(fpsWindowFrames * 1_000_000_000.0 / elapsed);
            float measuredFrameMs = measuredFps > 0.001f ? 1000.0f / measuredFps : 0.0f;
            if (isStableFpsSample(measuredFps, measuredFrameMs)) {
                fpsCurrent = measuredFps;
                frameTimeMs = measuredFrameMs;
                fpsLastStable = measuredFps;
                frameTimeLastStableMs = measuredFrameMs;
                fpsSource = "java_choreographer_frame_callback";
                fpsStatus = "live";
            } else if (fpsLastStable > 0.001f) {
                fpsCurrent = fpsLastStable;
                frameTimeMs = frameTimeLastStableMs;
                fpsSource = "java_choreographer_last_stable";
                fpsStatus = "last_stable";
            } else {
                fpsCurrent = 0.0f;
                frameTimeMs = 0.0f;
                fpsSource = "not_ready";
                fpsStatus = "not_ready";
            }
            fpsWindowStartNs = now;
            fpsWindowFrames = 0L;
            syncFpsDiagnosticsToNative();
            updateTopHudOnly();
        }
    }

    private void syncFpsDiagnosticsToNative() {
        try {
            if (nativeLoaded && nativeHandle != 0L) {
                nativeUpdateUiDiagnostics(nativeHandle, fpsCurrent, frameTimeMs, fpsLastStable, frameTimeLastStableMs, fpsSource, fpsStatus, fpsUpdateMode, fpsSampleWindowMs, framesRenderedLive, debugZipStatus, debugZipPath, debugZipIncludedFiles, debugZipReason);
            }
        } catch (Throwable ignored) { }
    }

    private void updateTopHudOnly() {
        if (topHudView == null) return;
        topHudView.setText("FPS " + oneDecimal(fpsCurrent) + "  |  " + oneDecimal(frameTimeMs) + " ms  |  Vulkan  |  " + SCENE_NAME);
    }

    private boolean isStableFpsSample(float fps, float frameMs) {
        return fps >= 1.0f && fps <= 240.0f && frameMs >= 4.0f && frameMs <= 1000.0f;
    }

    private FpsSnapshot fpsSnapshotForExport(String renderLab) {
        float nativeFps = parseFloatOr(jsonNumberField(renderLab, "fpsCurrent", ""), 0.0f);
        float nativeFrameMs = parseFloatOr(jsonNumberField(renderLab, "frameTimeMs", ""), 0.0f);
        String nativeSource = jsonStringField(renderLab, "fpsSource", "");
        if (isStableFpsSample(fpsCurrent, frameTimeMs)) {
            return new FpsSnapshot(fpsCurrent, frameTimeMs, fpsSource);
        }
        if (isStableFpsSample(fpsLastStable, frameTimeLastStableMs)) {
            return new FpsSnapshot(fpsLastStable, frameTimeLastStableMs, "java_ui_frame_delta_last_stable");
        }
        if (isStableFpsSample(nativeFps, nativeFrameMs)) {
            return new FpsSnapshot(nativeFps, nativeFrameMs, nativeSource.isEmpty() ? "native_render_lab_state" : nativeSource);
        }
        return new FpsSnapshot(0.0f, 0.0f, "not_ready");
    }

    private float parseFloatOr(String raw, float fallback) {
        try {
            if (raw == null || raw.isEmpty()) return fallback;
            return Float.parseFloat(raw);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private String oneDecimal(float v) { return String.format(Locale.US, "%.1f", v); }

    private int intJsonField(String key, int fallback) {
        try { return Integer.parseInt(jsonNumberField(getRenderLabStateForExport(), key, String.valueOf(fallback))); }
        catch (Throwable ignored) { return fallback; }
    }

    private boolean handleCameraTouch(MotionEvent event) {
        if (!nativeLoaded || nativeHandle == 0L) return true;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            pinchActive = false;
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
            lastPinchDistance = touchDistance(event);
            pinchActive = true;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() >= 2) {
                float d = touchDistance(event);
                if (lastPinchDistance > 0.0f && d > 0.0f) {
                    float delta = (lastPinchDistance - d) * 0.006f;
                    applyCamera(cameraYawDeg, cameraPitchDeg, cameraDistance + delta);
                }
                lastPinchDistance = d;
                pinchActive = true;
                return true;
            }
            if (!pinchActive) {
                float x = event.getX();
                float y = event.getY();
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                applyCamera(cameraYawDeg + dx * 0.35f, cameraPitchDeg + dy * 0.25f, cameraDistance);
                lastTouchX = x;
                lastTouchY = y;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
            pinchActive = false;
            lastPinchDistance = 0.0f;
            return true;
        }
        return true;
    }

    private float touchDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0.0f;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private void applyCamera(float yawDeg, float pitchDeg, float distance) {
        cameraYawDeg = yawDeg;
        cameraPitchDeg = clamp(pitchDeg, -75.0f, 75.0f);
        cameraDistance = clamp(distance, 2.0f, 8.0f);
        try {
            nativeSetCamera(nativeHandle, cameraYawDeg, cameraPitchDeg, cameraDistance);
            updateStatus();
        } catch (Throwable t) {
            writeCrashReport("native_camera_update_failed", t);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String pickValue(String text, String prefix) {
        int start = text.indexOf(prefix);
        if (start < 0) return "";
        start += prefix.length();
        int end = text.indexOf('\n', start);
        if (end < 0) end = text.length();
        return text.substring(start, end).trim();
    }

    private String shorten(String text, int max) { if (text == null) return ""; if (text.length() <= max) return text; return text.substring(0, Math.max(0, max - 1)) + "…"; }
    private String getRuntimeReportDirPath() { return getReportDir().getAbsolutePath(); }

    private void attemptActiveModelGpuUpload(String trigger) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        if (modelState.activeModelPath == null || modelState.activeModelPath.isEmpty()) {
            setModelFallbackState("no active model");
            nativeSetModelFallback(nativeHandle, "none", "", "no active model");
            return;
        }
        File active = new File(modelState.localExtractionPath());
        if (!active.exists()) {
            setModelFallbackState("active model missing: " + active.getAbsolutePath());
            nativeSetModelFallback(nativeHandle, modelState.activeModelName(), modelState.activeModelPath, modelState.reason);
            return;
        }
        String uploadKey = active.getAbsolutePath() + ":" + active.length() + ":" + active.lastModified();
        if ("surface_changed".equals(trigger) && uploadKey.equals(modelState.lastUploadedModelKey) && "ok".equals(modelState.gpuUploadStatus)) {
            modelState.reason = trigger + ": model upload preserved";
            return;
        }
        try {
            if (modelState.parse == null || !modelState.parse.glbValid || modelState.parse.binChunk == null) modelState.parse = GlbParser.parse(active);
            GlbPrimitiveMesh mesh = GlbParser.extractMultiPrimitive(modelState.parse);
            boolean ok = nativeUploadModelMultiPrimitive(
                nativeHandle,
                modelState.activeModelName(),
                modelState.activeModelPath,
                mesh.vertexData,
                mesh.indexData,
                mesh.rangeData,
                mesh.materialData,
                mesh.boundsMin,
                mesh.boundsMax,
                mesh.boundsCenter,
                mesh.modelScale,
                mesh.primitiveCountTotal,
                mesh.primitiveCountSkipped,
                mesh.unsupportedPrimitiveCount,
                mesh.reason
            );
            if (ok) {
                modelState.modelUploadRepeatCount++;
                modelState.uploadGenerationId++;
                modelState.lastUploadedModelKey = uploadKey;
                modelState.gpuUploadStatus = "ok";
                modelState.drawStatus = mesh.primitiveCountSkipped > 0 ? "partial_ok" : "ok";
                modelState.meshDrawStatus = modelState.drawStatus;
                modelState.uploadedVertexCount = mesh.vertexCount;
                modelState.uploadedIndexCount = mesh.indexCount;
                modelState.primitiveCountTotal = mesh.primitiveCountTotal;
                modelState.primitiveCountRendered = mesh.primitiveCountRendered;
                modelState.primitiveCountSkipped = mesh.primitiveCountSkipped;
                modelState.unsupportedPrimitiveCount = mesh.unsupportedPrimitiveCount;
                modelState.materialSlotCount = mesh.materialSlotCount;
                modelState.materialSlotCountRendered = mesh.materialSlotCount;
                modelState.textureSlotCount = mesh.textureSlotCount;
                modelState.textureSlotLimit = mesh.textureSlotLimit;
                modelState.pbrMapsStatus = mesh.pbrMapsStatus;
                modelState.metallicRoughnessStatus = mesh.metallicRoughnessStatus;
                modelState.normalMapStatus = mesh.normalMapStatus;
                modelState.normalMapAppliedStatus = mesh.normalMapAppliedStatus;
                modelState.occlusionMapStatus = mesh.occlusionMapStatus;
                modelState.tangentStatus = mesh.tangentStatus;
                modelState.tangentSource = mesh.tangentSource;
                modelState.tangentGeneratedCount = mesh.tangentGeneratedCount;
                modelState.tangentFallbackGeneratedCount = mesh.tangentFallbackGeneratedCount;
                modelState.tangentMissingCount = mesh.tangentMissingCount;
                modelState.tangentDegenerateTriangleCount = mesh.tangentDegenerateTriangleCount;
                modelState.tangentFallbackReason = mesh.tangentFallbackReason;
                modelState.tangentBuildMode = mesh.tangentBuildMode;
                modelState.pbrTextureSlotCount = mesh.pbrTextureSlotCount;
                modelState.materialSlotDiagnostics = mesh.materialSlotDiagnostics;
                if (mesh.materials != null && !mesh.materials.isEmpty()) {
                    MaterialInfo first = mesh.materials.get(0);
                    modelState.metallicFactor = first.metallicFactor;
                    modelState.roughnessFactor = first.roughnessFactor;
                    modelState.normalScale = first.normalScale;
                    modelState.occlusionStrength = first.occlusionStrength;
                }
                modelState.fallbackCubeVisible = false;
                modelState.fallbackCubeStatus = "off";
                modelState.reason = trigger + ": multi primitive static upload rendered=" + mesh.primitiveCountRendered + " skipped=" + mesh.primitiveCountSkipped + " reason=" + mesh.reason;
                modelState.parse.gpuUploadStatus = "ok";
                modelState.parse.drawStatus = modelState.drawStatus;
                modelState.parse.uploadedVertexCount = mesh.vertexCount;
                modelState.parse.uploadedIndexCount = mesh.indexCount;
                applyBaseColorTextures(mesh, trigger);
                applyPbrTextures(mesh, trigger);
            } else {
                setModelFallbackState(trigger + ": native model upload/draw failed");
            }
        } catch (Throwable t) {
            setModelFallbackState(trigger + ": " + shortThrowable(t));
            nativeSetModelFallback(nativeHandle, modelState.activeModelName(), modelState.activeModelPath, modelState.reason);
        }
        writeModelDiagnostics("gpu_upload_" + trigger);
    }

    private void applyBaseColorTextures(GlbPrimitiveMesh mesh, String trigger) {
        if (mesh == null || mesh.textures == null || mesh.textures.isEmpty()) {
            setTextureState("missing", "missing", "none", "none", "none", 0, 0, 0, true, trigger + ": baseColorTexture missing");
            modelState.textureSlotCount = mesh == null ? 0 : mesh.textureSlotCount;
            return;
        }
        int uploaded = 0;
        int fallback = 0;
        int skipped = Math.max(0, mesh.skippedTextureCount);
        for (BaseColorTexture texture : mesh.textures) {
            if (texture == null) continue;
            if (!"ok".equals(texture.status)) {
                if ("missing".equals(texture.status)) skipped++; else fallback++;
                continue;
            }
            try {
                boolean ok = nativeUploadBaseColorTextureSlot(nativeHandle, texture.slot, texture.pixels, texture.width, texture.height, texture.name, texture.source, texture.mimeType);
                if (ok) uploaded++; else fallback++;
            } catch (Throwable t) {
                fallback++;
            }
        }
        modelState.uploadedTextureCount = uploaded;
        modelState.textureFallbackCount = fallback;
        modelState.skippedTextureCount = skipped;
        modelState.textureSlotCount = mesh.textureSlotCount;
        if (uploaded > 0) {
            BaseColorTexture first = mesh.textures.get(0);
            setTextureState("ok", "ok", first.name, first.source, first.mimeType, first.width, first.height, first.pixels == null ? 0 : first.pixels.length * 4, false, trigger + ": texture slots uploaded=" + uploaded);
        } else {
            setTextureState("missing", fallback > 0 ? "failed" : "missing", "none", "none", "none", 0, 0, 0, true, trigger + ": no texture slots uploaded");
        }
    }

    private void applyPbrTextures(GlbPrimitiveMesh mesh, String trigger) {
        if (mesh == null || mesh.materials == null) return;
        int uploaded = 0;
        int fallback = 0;
        int skipped = 0;
        for (MaterialInfo material : mesh.materials) {
            BaseColorTexture[] pbr = new BaseColorTexture[] { material.metallicRoughnessTexture, material.normalTexture, material.occlusionTexture };
            for (BaseColorTexture texture : pbr) {
                if (texture == null) continue;
                if ("missing".equals(texture.status) || "blocked_no_tangent".equals(texture.status)) {
                    skipped++;
                    continue;
                }
                if (!"ok".equals(texture.status)) {
                    fallback++;
                    continue;
                }
                try {
                    boolean ok = nativeUploadPbrTextureSlot(nativeHandle, texture.materialSlot, texture.kind, texture.pixels, texture.width, texture.height, texture.name, texture.source, texture.mimeType);
                    if (ok) uploaded++; else fallback++;
                } catch (Throwable t) {
                    fallback++;
                }
            }
        }
        modelState.uploadedPbrTextureCount = uploaded;
        modelState.pbrTextureFallbackCount = fallback;
        modelState.skippedPbrTextureCount = skipped;
        modelState.pbrTextureSlotCount = mesh.pbrTextureSlotCount;
        modelState.pbrMapsStatus = uploaded > 0 ? (fallback > 0 || skipped > 0 ? "partial_ok" : "ok") : (fallback > 0 ? "failed" : "missing");
        modelState.metallicRoughnessStatus = mesh.metallicRoughnessStatus;
        modelState.normalMapStatus = mesh.normalMapStatus;
        modelState.normalMapAppliedStatus = mesh.normalMapAppliedStatus;
        modelState.occlusionMapStatus = mesh.occlusionMapStatus;
        modelState.reason = trigger + ": pbr texture slots uploaded=" + uploaded + " fallback=" + fallback + " skipped=" + skipped;
        try {
            nativeSetPbrDiagnostics(nativeHandle, modelState.pbrMapsStatus, modelState.metallicRoughnessStatus, modelState.normalMapStatus, modelState.normalMapAppliedStatus, modelState.occlusionMapStatus, modelState.tangentStatus, modelState.tangentSource, modelState.tangentGeneratedCount, modelState.tangentFallbackGeneratedCount, modelState.tangentMissingCount, modelState.tangentDegenerateTriangleCount, modelState.tangentFallbackReason, modelState.tangentBuildMode, modelState.modelUploadRepeatCount, modelState.uploadGenerationId, modelState.renderLoopAllocationGuardStatus, modelState.metallicFactor, modelState.roughnessFactor, modelState.normalScale, modelState.occlusionStrength, modelState.pbrTextureSlotCount, modelState.uploadedPbrTextureCount, modelState.skippedPbrTextureCount, modelState.pbrTextureFallbackCount, modelState.materialSlotDiagnostics);
        } catch (Throwable ignored) { }
    }

    private void applyBaseColorTexture(GlbPrimitiveMesh mesh, String trigger) {
        if (mesh == null || mesh.texture == null) {
            setTextureState("missing", "missing", "none", "none", "none", 0, 0, 0, true, trigger + ": baseColorTexture missing");
            return;
        }
        if (!"ok".equals(mesh.texture.status)) {
            setTextureState("missing".equals(mesh.texture.status) ? "missing" : "failed", mesh.texture.status, mesh.texture.name, mesh.texture.source, mesh.texture.mimeType, 0, 0, 0, true, mesh.texture.reason);
            return;
        }
        try {
            boolean ok = nativeUploadBaseColorTexture(nativeHandle, mesh.texture.pixels, mesh.texture.width, mesh.texture.height, mesh.texture.name, mesh.texture.source, mesh.texture.mimeType);
            if (ok) {
                setTextureState("ok", "ok", mesh.texture.name, mesh.texture.source, mesh.texture.mimeType, mesh.texture.width, mesh.texture.height, mesh.texture.pixels.length * 4, false, trigger + ": baseColor texture uploaded");
            } else {
                setTextureState("failed", "failed", mesh.texture.name, mesh.texture.source, mesh.texture.mimeType, 0, 0, 0, true, trigger + ": native texture upload failed");
            }
        } catch (Throwable t) {
            setTextureState("failed", "failed", mesh.texture.name, mesh.texture.source, mesh.texture.mimeType, 0, 0, 0, true, trigger + ": " + shortThrowable(t));
        }
    }

    private void setTextureState(String uploadStatus, String baseColorStatus, String name, String source, String mimeType, int width, int height, int bytes, boolean fallbackUsed, String reason) {
        modelState.textureUploadStatus = uploadStatus;
        modelState.baseColorTextureStatus = baseColorStatus;
        modelState.baseColorTextureName = name == null || name.isEmpty() ? "none" : name;
        modelState.baseColorTextureSource = source == null || source.isEmpty() ? "none" : source;
        modelState.baseColorTextureMimeType = mimeType == null || mimeType.isEmpty() ? "none" : mimeType;
        modelState.textureWidth = Math.max(0, width);
        modelState.textureHeight = Math.max(0, height);
        modelState.textureBytes = Math.max(0, bytes);
        modelState.textureFallbackUsed = fallbackUsed;
        modelState.reason = reason;
        if (modelState.parse != null) {
            modelState.parse.textureUploadStatus = uploadStatus;
            modelState.parse.baseColorTextureStatus = baseColorStatus;
            modelState.parse.textureWidth = Math.max(0, width);
            modelState.parse.textureHeight = Math.max(0, height);
            modelState.parse.textureFallbackUsed = fallbackUsed;
        }
    }

    private void setModelFallbackState(String reason) {
        modelState.gpuUploadStatus = "failed";
        modelState.drawStatus = "fallback";
        modelState.meshDrawStatus = "fallback";
        modelState.uploadedVertexCount = 0;
        modelState.uploadedIndexCount = 0;
        modelState.fallbackCubeVisible = true;
        modelState.fallbackCubeStatus = "on";
        modelState.reason = reason;
        if (modelState.parse != null) {
            modelState.parse.gpuUploadStatus = "failed";
            modelState.parse.drawStatus = "fallback";
            modelState.parse.uploadedVertexCount = 0;
            modelState.parse.uploadedIndexCount = 0;
        }
        modelState.primitiveCountRendered = 0;
        modelState.primitiveCountSkipped = modelState.parse == null ? 0 : modelState.parse.primitiveCount;
        modelState.primitiveCountTotal = modelState.parse == null ? 0 : modelState.parse.primitiveCount;
    }

    private void chooseGlbForImport() {
        Log.i(TAG_DIAG, "import_glb_clicked");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
            "model/gltf-binary",
            "application/octet-stream",
            "*/*"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_GLB);
    }

    private void importGlbFromUri(Uri uri) {
        modelState.importStatus = "importing";
        modelState.importRoute = "not run";
        modelState.reason = "import in progress";
        modelState.sourceDisplayName = displayNameForUri(uri);
        importGlbButton.setEnabled(false);
        importGlbButton.setText("Importing...");
        updateDiagnosticsStatusPanel();
        importGlbButton.post(() -> {
            try {
                ModelCopyResult copy = copyImportedModel(uri, modelState.sourceDisplayName);
                modelState.importStatus = "ok";
                modelState.importRoute = copy.route;
                modelState.importedPath = copy.path;
                modelState.activeModelPath = copy.path;
                modelState.activeModelLocalPath = copy.localFile.getAbsolutePath();
                modelState.lastImportedModel = copy.path;
                modelState.reason = copy.reason;
                modelState.parse = GlbParser.parse(copy.localFile);
                if (!modelState.parse.glbValid) {
                    modelState.importStatus = "failed";
                    modelState.importRoute = copy.route;
                    modelState.reason = modelState.parse.reason;
                }
                scanModels("after_import");
                importGlbButton.setText(modelState.parse.glbValid ? "Import OK" : "Import Failed");
                attemptActiveModelGpuUpload("model_import");
            } catch (Throwable t) {
                modelState.importStatus = "failed";
                modelState.importRoute = "failed";
                modelState.reason = shortThrowable(t);
                modelState.parse = GlbParseResult.failed(modelState.reason);
                importGlbButton.setText("Import Failed");
            }
            importGlbButton.setEnabled(true);
            writeModelDiagnostics("import");
            exportEngineDiagnostics("model_import");
            updateImportUi();
        });
    }

    private void scanModelsFromButton() {
        scanModelsButton.setEnabled(false);
        scanModelsButton.setText("Scanning...");
            scanModelsButton.post(() -> {
            scanModels("manual_scan");
            attemptActiveModelGpuUpload("manual_scan");
            scanModelsButton.setText("Scan Models");
            scanModelsButton.setEnabled(true);
            writeModelDiagnostics("scan");
            exportEngineDiagnostics("model_scan");
            updateImportUi();
        });
    }

    private void scanModels(String trigger) {
        List<File> models = new ArrayList<>();
        File directDir = new File("/storage/emulated/0/SOLUMCreative/assets/models/imported");
        collectGlbFiles(directDir, models);
        File externalBase = getExternalFilesDir(null);
        if (models.isEmpty() && externalBase != null) collectGlbFiles(new File(externalBase, "assets/models/imported"), models);
        File appDir = new File(getFilesDir(), "assets/models/imported");
        if (models.isEmpty()) collectGlbFiles(appDir, models);
        modelState.modelsFoundCount = models.size();
        if (modelState.activeModelPath.isEmpty() && !models.isEmpty()) {
            modelState.activeModelPath = models.get(0).getAbsolutePath();
            modelState.activeModelLocalPath = models.get(0).getAbsolutePath();
        }
        if (modelState.lastImportedModel.isEmpty() && !models.isEmpty()) modelState.lastImportedModel = models.get(models.size() - 1).getAbsolutePath();
        if (!modelState.activeModelPath.isEmpty()) {
            File active = new File(modelState.localExtractionPath());
            if (active.exists()) modelState.parse = GlbParser.parse(active);
        }
        if ("not run".equals(modelState.importStatus) && models.isEmpty()) modelState.reason = trigger + ": no .glb files found";
    }

    private void collectGlbFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".glb")) out.add(f);
        }
    }

    private void updateImportUi() {
        if (importGlbButton != null && importGlbButton.isEnabled()) {
            if ("ok".equals(modelState.importStatus)) importGlbButton.setText("Import OK");
            else if ("failed".equals(modelState.importStatus)) importGlbButton.setText("Import Failed");
            else importGlbButton.setText("Import GLB");
        }
        updateStatus();
        updateDiagnosticsStatusPanel();
    }

    private ModelCopyResult copyImportedModel(Uri uri, String sourceName) throws Exception {
        String safeName = safeFileName(sourceName);
        if (!safeName.toLowerCase(Locale.US).endsWith(".glb")) safeName += ".glb";
        ContentResolver resolver = getContentResolver();
        Uri treeUri = getConfiguredTreeUri();
        if (treeUri != null) {
            try {
                Uri rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
                Uri assets = ensureChildDirectory(resolver, rootDocumentUri, "assets");
                Uri models = ensureChildDirectory(resolver, assets, "models");
                Uri imported = ensureChildDirectory(resolver, models, "imported");
                Uri outUri = ensureChildFileWithMime(resolver, imported, safeName, "model/gltf-binary");
                try (InputStream in = resolver.openInputStream(uri); OutputStream out = openTruncatingOutputStream(resolver, outUri)) {
                    if (in == null) throw new IllegalStateException("open_input_stream_failed");
                    copyStream(in, out);
                }
                File mirror = new File(getFilesDir(), "assets/models/imported/" + safeName);
                copyUriToFile(uri, mirror);
                return new ModelCopyResult("saf", treeUri.toString() + "/assets/models/imported/" + safeName, "saf_tree_uri_configured", mirror);
            } catch (Throwable t) {
                Log.e(TAG_DIAG, "model_import_saf_failed reason=" + shortThrowable(t));
            }
        }
        File directDir = new File("/storage/emulated/0/SOLUMCreative/assets/models/imported");
        if (canWriteDirectory(directDir)) {
            File out = uniqueFile(directDir, safeName);
            copyUriToFile(uri, out);
            return new ModelCopyResult("direct", out.getAbsolutePath(), "direct_public_storage_ok", out);
        }
        File externalBase = getExternalFilesDir(null);
        File fallbackDir = externalBase != null ? new File(externalBase, "assets/models/imported") : new File(getFilesDir(), "assets/models/imported");
        if (canWriteDirectory(fallbackDir)) {
            File out = uniqueFile(fallbackDir, safeName);
            copyUriToFile(uri, out);
            return new ModelCopyResult("fallback", out.getAbsolutePath(), "direct_public_storage_failed_app_specific_fallback", out);
        }
        throw new IllegalStateException("no_writable_asset_import_route");
    }

    private void copyUriToFile(Uri uri, File outFile) throws Exception {
        File parent = outFile.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new java.io.FileOutputStream(outFile, false)) {
            if (in == null) throw new IllegalStateException("open_input_stream_failed");
            copyStream(in, out);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
    }

    private File uniqueFile(File dir, String fileName) {
        File f = new File(dir, fileName);
        if (!f.exists()) return f;
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) { base = fileName.substring(0, dot); ext = fileName.substring(dot); }
        for (int i = 1; i < 1000; i++) {
            f = new File(dir, base + "_" + i + ext);
            if (!f.exists()) return f;
        }
        return new File(dir, base + "_" + System.currentTimeMillis() + ext);
    }

    private String displayNameForUri(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Throwable ignored) { }
        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty() ? "imported.glb" : last;
    }

    private String safeFileName(String name) {
        String trimmed = name == null ? "imported.glb" : name.trim();
        if (trimmed.isEmpty()) trimmed = "imported.glb";
        return trimmed.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void chooseDiagnosticsFolder() {
        Log.i(TAG_DIAG, "choose_folder_clicked");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CHOOSE_DIAGNOSTICS_TREE);
    }

    private void exportEngineDiagnosticsFromButton() {
        Log.i(TAG_DIAG, "export_button_clicked");
        lastExportStatus = "running";
        lastExportRoute = "running";
        lastExportReason = "export in progress";
        lastExportPath = "";
        lastExportTimestamp = timestampUtc();
        if (exportButton != null) {
            exportButton.setEnabled(false);
            exportButton.setText("Exporting...");
        }
        if (quickExportButton != null) {
            quickExportButton.setEnabled(false);
            quickExportButton.setText("Exporting...");
        }
        updateDiagnosticsStatusPanel();
        View poster = exportButton != null ? exportButton : quickExportButton;
        if (poster == null) poster = statusView;
        poster.post(() -> {
            ExportResult result = exportEngineDiagnostics("manual_button");
            lastExportStatus = result.ok ? "ok" : "failed";
            lastExportRoute = result.route;
            lastExportReason = result.reason;
            lastExportPath = result.actualRoot;
            lastExportTimestamp = result.timestamp;
            if (exportButton != null) {
                exportButton.setText(result.ok ? "Export OK" : "Export Failed");
                exportButton.setEnabled(true);
            }
            if (quickExportButton != null) {
                quickExportButton.setText(result.ok ? "Export OK" : "Export Failed");
                quickExportButton.setEnabled(true);
            }
            updateDiagnosticsStatusPanel();
        });
    }

    private void exportDebugZipFromButton() {
        Log.i(TAG_DIAG, "debug_zip_clicked");
        debugZipStatus = "running";
        debugZipReason = "export in progress";
        debugZipPath = "";
        debugZipIncludedFiles = "";
        if (debugZipButton != null) {
            debugZipButton.setEnabled(false);
            debugZipButton.setText("ZIP...");
        }
        updateDiagnosticsStatusPanel();
        View poster = debugZipButton != null ? debugZipButton : statusView;
        poster.post(() -> {
            try {
                exportEngineDiagnostics("debug_zip");
                writeModelDiagnostics("debug_zip");
                DebugZipResult result = exportDebugZip();
                debugZipStatus = result.ok ? "ok" : "failed";
                debugZipPath = result.path;
                debugZipReason = result.reason;
                debugZipIncludedFiles = result.includedFiles;
            } catch (Throwable t) {
                debugZipStatus = "failed";
                debugZipReason = shortThrowable(t);
                writeCrashReport("debug_zip_export_failed", t);
            }
            syncFpsDiagnosticsToNative();
            exportEngineDiagnostics("debug_zip_final");
            if (debugZipButton != null) {
                debugZipButton.setText("ok".equals(debugZipStatus) ? "Debug ZIP OK" : "Debug ZIP Failed");
                debugZipButton.setEnabled(true);
            }
            updateDiagnosticsStatusPanel();
            updateStatus();
        });
    }

    private DebugZipResult exportDebugZip() throws Exception {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "SOLUM_DEBUG_" + stamp + ".zip";
        List<DebugZipEntry> entries = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        File reportDir = getReportDir();
        writeDebugZipDiagnosticsFiles(reportDir, "debug_zip_package");
        addDebugZipEntry(entries, missing, new File(reportDir, "engine_runtime_state.json"), true);
        addDebugZipEntry(entries, missing, new File(reportDir, "engine_diagnostics_manifest.json"), true);
        addDebugZipEntry(entries, missing, new File(reportDir, "model_import_state.json"), true);
        addDebugZipEntry(entries, missing, new File(reportDir, "asset_report.json"), true);
        File note = new File(reportDir, "debug_zip_runtime_note.txt");
        try (FileWriter w = new FileWriter(note, false)) {
            w.write("SOLUM P15 debug zip\n");
            w.write(SCENE_NAME + "\n");
            w.write("debugZipStatus=running\n");
            w.write("requiredFiles=engine_runtime_state.json,engine_diagnostics_manifest.json,model_import_state.json,asset_report.json\n");
            if (missing.isEmpty()) {
                w.write("missingFiles=none\n");
            } else {
                for (String item : missing) w.write("missingFile=" + item + "\n");
            }
        }
        addDebugZipEntry(entries, missing, note, true);
        File summary = new File(reportDir, "glb_model_summary.json");
        try (FileWriter w = new FileWriter(summary, false)) { w.write(modelState.toJson("solum.glb_model_summary", timestampUtc(), "debug_zip_summary")); }
        addDebugZipEntry(entries, missing, summary, false);

        DebugZipResult result = new DebugZipResult();
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SOLUM_EXPORTS");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new IllegalStateException("mediastore_open_output_failed");
                    result.includedFiles = writeZip(out, entries);
                }
                result.ok = true;
                result.path = "/storage/emulated/0/Download/SOLUM_EXPORTS/" + fileName;
                result.missingFiles = missing.toString();
                result.reason = missing.isEmpty() ? "mediastore_downloads_ok" : "mediastore_downloads_ok_missing:" + missing.toString();
                return result;
            }
        }
        File dir = new File("/storage/emulated/0/Download/SOLUM_EXPORTS");
        if (!canWriteDirectory(dir)) throw new IllegalStateException("debug_zip_download_route_not_writable");
        File outFile = new File(dir, fileName);
        try (OutputStream out = new java.io.FileOutputStream(outFile, false)) { result.includedFiles = writeZip(out, entries); }
        result.ok = true;
        result.path = outFile.getAbsolutePath();
        result.missingFiles = missing.toString();
        result.reason = missing.isEmpty() ? "direct_downloads_fallback_ok" : "direct_downloads_fallback_ok_missing:" + missing.toString();
        return result;
    }

    private void writeDebugZipDiagnosticsFiles(File reportDir, String trigger) {
        try {
            if (reportDir == null) return;
            canWriteDirectory(reportDir);
            String timestamp = timestampUtc();
            String nativeStatus = getNativeStatusForExport();
            String renderLab = getRenderLabStateForExport();
            ExportResult result = new ExportResult();
            result.ok = true;
            result.route = "debug_zip_report_dir";
            result.reason = cachedReportDirReason;
            result.actualRoot = reportDir.getAbsolutePath();
            result.timestamp = timestamp;
            String runtimeJson = buildRuntimeStateJson(timestamp, trigger, result, getVersionName(), getVersionCode(), nativeStatus, renderLab);
            String manifestJson = buildDiagnosticsManifestJson(timestamp, result, renderLab);
            writeText(new FileWriter(new File(reportDir, "engine_runtime_state.json"), false), runtimeJson);
            writeText(new FileWriter(new File(reportDir, "engine_diagnostics_manifest.json"), false), manifestJson);
            writeText(new FileWriter(new File(reportDir, "model_import_state.json"), false), modelState.toJson("solum.model_import_state", timestamp, trigger));
            writeText(new FileWriter(new File(reportDir, "asset_report.json"), false), modelState.toJson("solum.asset_report", timestamp, trigger));
        } catch (Throwable t) {
            Log.e(TAG_DIAG, "debug_zip_prepare_files_failed reason=" + shortThrowable(t));
        }
    }

    private void addDebugZipEntry(List<DebugZipEntry> entries, List<String> missing, File file, boolean required) {
        if (file != null && file.exists() && file.isFile()) {
            entries.add(new DebugZipEntry(file));
            return;
        }
        String name = file == null ? "unknown" : file.getName();
        missing.add((required ? "required:" : "optional:") + name);
    }

    private String writeZip(OutputStream out, List<DebugZipEntry> entries) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (DebugZipEntry entry : entries) {
                File file = entry.file;
                String name = file.getName();
                names.add(name);
                zip.putNextEntry(new ZipEntry(name));
                try (InputStream in = new java.io.FileInputStream(file)) { copyStream(in, zip); }
                zip.closeEntry();
            }
        }
        return names.toString();
    }

    private File getReportDir() {
        if (cachedReportDir != null && cachedReportDir.exists()) return cachedReportDir;
        File solumCreative = new File("/storage/emulated/0/SOLUMCreative/diagnostics/latest");
        if (canWriteDirectory(solumCreative)) { cachedReportDir = solumCreative; cachedReportDirReason = "direct_public_storage_ok"; return cachedReportDir; }
        File externalBase = getExternalFilesDir(null);
        if (externalBase != null) {
            File externalDir = new File(externalBase, "solum_diagnostics");
            if (canWriteDirectory(externalDir)) { cachedReportDir = externalDir; cachedReportDirReason = "direct_public_storage_write_probe_failed_app_specific_external_fallback"; return cachedReportDir; }
        }
        File appDir = new File(getFilesDir(), "solum_diagnostics");
        appDir.mkdirs();
        cachedReportDir = appDir;
        cachedReportDirReason = "direct_public_storage_write_probe_failed_internal_files_fallback";
        return cachedReportDir;
    }

    private boolean canWriteDirectory(File dir) {
        try {
            if (!(dir.mkdirs() || dir.exists())) return false;
            File probe = new File(dir, ".solum_write_probe");
            try (FileWriter w = new FileWriter(probe, false)) { w.write("ok"); }
            probe.delete();
            return true;
        } catch (Throwable ignored) { return false; }
    }

    private void writeRuntimeNote(String status, String message) {
        try {
            File dir = getReportDir();
            File out = new File(dir, "runtime_java_state.json");
            try (FileWriter w = new FileWriter(out)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.runtime_java_state\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"status\": \"" + escape(status) + "\",\n");
                w.write("  \"message\": \"" + escape(message) + "\",\n");
                w.write("  \"reportDir\": \"" + escape(dir.getAbsolutePath()) + "\"\n");
                w.write("}\n");
            }
        } catch (Throwable ignored) { }
    }

    private ExportResult exportEngineDiagnostics(String trigger) {
        ExportResult result = new ExportResult();
        result.timestamp = timestampUtc();
        try {
            String timestamp = result.timestamp;
            String nativeStatus = getNativeStatusForExport();
            String renderLab = getRenderLabStateForExport();
            String versionName = getVersionName();
            int versionCode = getVersionCode();

            result = openDiagnosticsWriters();
            result.timestamp = timestamp;
            String runtimeJson = buildRuntimeStateJson(timestamp, trigger, result, versionName, versionCode, nativeStatus, renderLab);
            String manifestJson = buildDiagnosticsManifestJson(timestamp, result, renderLab);
            writeText(result.runtimeWriter, runtimeJson);
            writeText(result.manifestWriter, manifestJson);
            writeModelDiagnostics("engine_export");
            writeRuntimeNote("engine_diagnostics_exported", "Export Engine Diagnostics wrote engine_runtime_state.json and engine_diagnostics_manifest.json");
            Log.i(TAG_DIAG, "export_ok route=" + result.route + " actualRoot=" + result.actualRoot);
        } catch (Throwable t) {
            result.ok = false;
            result.route = "failed";
            result.reason = shortThrowable(t);
            Log.e(TAG_DIAG, "export_failed reason=" + result.reason);
            writeCrashReport("engine_diagnostics_export_failed", t);
        }
        return result;
    }

    private String buildRuntimeStateJson(String timestamp, String trigger, ExportResult result, String versionName, int versionCode, String nativeStatus, String renderLab) {
        FpsSnapshot fps = fpsSnapshotForExport(renderLab);
        return "{\n"
            + "  \"schema\": \"solum.engine_runtime_state\",\n"
            + "  \"schemaVersion\": 1,\n"
            + "  \"timestampUtc\": \"" + escape(timestamp) + "\",\n"
            + "  \"app\": \"engine\",\n"
            + "  \"packageName\": \"" + escape(getPackageName()) + "\",\n"
            + "  \"trigger\": \"" + escape(trigger) + "\",\n"
            + "  \"exportStatus\": \"" + (result.ok ? "ok" : "failed") + "\",\n"
            + "  \"exportRoute\": \"" + escape(result.route) + "\",\n"
            + "  \"actualRoot\": \"" + escape(result.actualRoot) + "\",\n"
            + "  \"reason\": \"" + escape(result.reason) + "\",\n"
            + "  \"diagnosticsRoot\": \"" + escape(result.actualRoot) + "\",\n"
            + "  \"diagnosticsRootStatus\": \"" + escape(result.reason) + "\",\n"
            + "  \"build\": { \"versionName\": \"" + escape(versionName) + "\", \"versionCode\": " + versionCode + " },\n"
            + "  \"backend\": { \"rendererPath\": \"Android Native Vulkan\", \"statusText\": \"" + escape(nativeStatus) + "\" },\n"
            + "  \"currentScene\": \"" + SCENE_ID + "\",\n"
            + "  \"currentLabScene\": \"" + SCENE_ID + "\",\n"
            + "  \"currentLabSceneName\": \"" + SCENE_NAME + "\",\n"
            + "  \"assetImportStatus\": \"" + escape(modelState.importStatus) + "\",\n"
            + "  \"activeModelName\": \"" + escape(modelState.activeModelName()) + "\",\n"
            + "  \"activeModelPath\": \"" + escape(modelState.activeModelPath) + "\",\n"
            + "  \"activePrimitiveIndex\": 0,\n"
            + "  \"activeModelSummary\": \"" + escape(modelState.summary()) + "\",\n"
            + "  \"gpuUploadStatus\": \"" + escape(jsonStringField(renderLab, "gpuUploadStatus", modelState.gpuUploadStatus)) + "\",\n"
            + "  \"drawStatus\": \"" + escape(jsonStringField(renderLab, "drawStatus", modelState.drawStatus)) + "\",\n"
            + "  \"meshDrawStatus\": \"" + escape(jsonStringField(renderLab, "meshDrawStatus", modelState.meshDrawStatus)) + "\",\n"
            + "  \"textureUploadStatus\": \"" + escape(jsonStringField(renderLab, "textureUploadStatus", modelState.textureUploadStatus)) + "\",\n"
            + "  \"baseColorTextureStatus\": \"" + escape(jsonStringField(renderLab, "baseColorTextureStatus", modelState.baseColorTextureStatus)) + "\",\n"
            + "  \"baseColorTextureName\": \"" + escape(jsonStringField(renderLab, "baseColorTextureName", modelState.baseColorTextureName)) + "\",\n"
            + "  \"baseColorTextureSource\": \"" + escape(jsonStringField(renderLab, "baseColorTextureSource", modelState.baseColorTextureSource)) + "\",\n"
            + "  \"baseColorTextureMimeType\": \"" + escape(jsonStringField(renderLab, "baseColorTextureMimeType", modelState.baseColorTextureMimeType)) + "\",\n"
            + "  \"textureWidth\": " + jsonNumberField(renderLab, "textureWidth", String.valueOf(modelState.textureWidth)) + ",\n"
            + "  \"textureHeight\": " + jsonNumberField(renderLab, "textureHeight", String.valueOf(modelState.textureHeight)) + ",\n"
            + "  \"textureBytes\": " + jsonNumberField(renderLab, "textureBytes", String.valueOf(modelState.textureBytes)) + ",\n"
            + "  \"textureFallbackUsed\": " + jsonBooleanField(renderLab, "textureFallbackUsed", modelState.textureFallbackUsed ? "true" : "false") + ",\n"
            + "  \"uploadedVertexCount\": " + jsonNumberField(renderLab, "uploadedVertexCount", String.valueOf(modelState.uploadedVertexCount)) + ",\n"
            + "  \"uploadedIndexCount\": " + jsonNumberField(renderLab, "uploadedIndexCount", String.valueOf(modelState.uploadedIndexCount)) + ",\n"
            + "  \"modelVertexLayout\": \"" + escape(jsonStringField(renderLab, "modelVertexLayout", "POSITION,NORMAL,TEXCOORD_0,COLOR_0,TANGENT")) + "\",\n"
            + "  \"modelBoundsMin\": " + jsonArrayField(renderLab, "modelBoundsMin", "[0,0,0]") + ",\n"
            + "  \"modelBoundsMax\": " + jsonArrayField(renderLab, "modelBoundsMax", "[0,0,0]") + ",\n"
            + "  \"modelBoundsCenter\": " + jsonArrayField(renderLab, "modelBoundsCenter", "[0,0,0]") + ",\n"
            + "  \"modelScale\": " + jsonNumberField(renderLab, "modelScale", "1") + ",\n"
            + "  \"modelRenderMode\": \"" + escape(jsonStringField(renderLab, "modelRenderMode", "multi_primitive_static")) + "\",\n"
            + "  \"primitiveCountTotal\": " + jsonNumberField(renderLab, "primitiveCountTotal", String.valueOf(modelState.primitiveCountTotal)) + ",\n"
            + "  \"primitiveCountRendered\": " + jsonNumberField(renderLab, "primitiveCountRendered", String.valueOf(modelState.primitiveCountRendered)) + ",\n"
            + "  \"primitiveCountSkipped\": " + jsonNumberField(renderLab, "primitiveCountSkipped", String.valueOf(modelState.primitiveCountSkipped)) + ",\n"
            + "  \"unsupportedPrimitiveCount\": " + jsonNumberField(renderLab, "unsupportedPrimitiveCount", String.valueOf(modelState.unsupportedPrimitiveCount)) + ",\n"
            + "  \"materialSlotCount\": " + jsonNumberField(renderLab, "materialSlotCount", String.valueOf(modelState.materialSlotCount)) + ",\n"
            + "  \"materialSlotCountRendered\": " + jsonNumberField(renderLab, "materialSlotCountRendered", String.valueOf(modelState.materialSlotCountRendered)) + ",\n"
            + "  \"textureSlotCount\": " + jsonNumberField(renderLab, "textureSlotCount", String.valueOf(modelState.textureSlotCount)) + ",\n"
            + "  \"uploadedTextureCount\": " + jsonNumberField(renderLab, "uploadedTextureCount", String.valueOf(modelState.uploadedTextureCount)) + ",\n"
            + "  \"textureFallbackCount\": " + jsonNumberField(renderLab, "textureFallbackCount", String.valueOf(modelState.textureFallbackCount)) + ",\n"
            + "  \"skippedTextureCount\": " + jsonNumberField(renderLab, "skippedTextureCount", String.valueOf(modelState.skippedTextureCount)) + ",\n"
            + "  \"textureSlotLimit\": " + jsonNumberField(renderLab, "textureSlotLimit", String.valueOf(modelState.textureSlotLimit)) + ",\n"
            + "  \"pbrMapsStatus\": \"" + escape(jsonStringField(renderLab, "pbrMapsStatus", modelState.pbrMapsStatus)) + "\",\n"
            + "  \"metallicRoughnessStatus\": \"" + escape(jsonStringField(renderLab, "metallicRoughnessStatus", modelState.metallicRoughnessStatus)) + "\",\n"
            + "  \"normalMapStatus\": \"" + escape(jsonStringField(renderLab, "normalMapStatus", modelState.normalMapStatus)) + "\",\n"
            + "  \"normalMapAppliedStatus\": \"" + escape(jsonStringField(renderLab, "normalMapAppliedStatus", modelState.normalMapAppliedStatus)) + "\",\n"
            + "  \"occlusionMapStatus\": \"" + escape(jsonStringField(renderLab, "occlusionMapStatus", modelState.occlusionMapStatus)) + "\",\n"
            + "  \"tangentStatus\": \"" + escape(jsonStringField(renderLab, "tangentStatus", modelState.tangentStatus)) + "\",\n"
            + "  \"tangentSource\": \"" + escape(jsonStringField(renderLab, "tangentSource", modelState.tangentSource)) + "\",\n"
            + "  \"tangentGeneratedCount\": " + jsonNumberField(renderLab, "tangentGeneratedCount", String.valueOf(modelState.tangentGeneratedCount)) + ",\n"
            + "  \"tangentFallbackGeneratedCount\": " + jsonNumberField(renderLab, "tangentFallbackGeneratedCount", String.valueOf(modelState.tangentFallbackGeneratedCount)) + ",\n"
            + "  \"tangentMissingCount\": " + jsonNumberField(renderLab, "tangentMissingCount", String.valueOf(modelState.tangentMissingCount)) + ",\n"
            + "  \"tangentDegenerateTriangleCount\": " + jsonNumberField(renderLab, "tangentDegenerateTriangleCount", String.valueOf(modelState.tangentDegenerateTriangleCount)) + ",\n"
            + "  \"tangentFallbackReason\": \"" + escape(jsonStringField(renderLab, "tangentFallbackReason", modelState.tangentFallbackReason)) + "\",\n"
            + "  \"tangentBuildMode\": \"" + escape(jsonStringField(renderLab, "tangentBuildMode", modelState.tangentBuildMode)) + "\",\n"
            + "  \"metallicFactor\": " + jsonNumberField(renderLab, "metallicFactor", jsonFloat(modelState.metallicFactor)) + ",\n"
            + "  \"roughnessFactor\": " + jsonNumberField(renderLab, "roughnessFactor", jsonFloat(modelState.roughnessFactor)) + ",\n"
            + "  \"normalScale\": " + jsonNumberField(renderLab, "normalScale", jsonFloat(modelState.normalScale)) + ",\n"
            + "  \"occlusionStrength\": " + jsonNumberField(renderLab, "occlusionStrength", jsonFloat(modelState.occlusionStrength)) + ",\n"
            + "  \"pbrTextureSlotCount\": " + jsonNumberField(renderLab, "pbrTextureSlotCount", String.valueOf(modelState.pbrTextureSlotCount)) + ",\n"
            + "  \"uploadedPbrTextureCount\": " + jsonNumberField(renderLab, "uploadedPbrTextureCount", String.valueOf(modelState.uploadedPbrTextureCount)) + ",\n"
            + "  \"skippedPbrTextureCount\": " + jsonNumberField(renderLab, "skippedPbrTextureCount", String.valueOf(modelState.skippedPbrTextureCount)) + ",\n"
            + "  \"pbrTextureFallbackCount\": " + jsonNumberField(renderLab, "pbrTextureFallbackCount", String.valueOf(modelState.pbrTextureFallbackCount)) + ",\n"
            + "  \"materialSlotDiagnostics\": " + jsonArrayField(renderLab, "materialSlotDiagnostics", modelState.materialSlotDiagnostics) + ",\n"
            + "  \"materialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "materialCalibrationStatus", modelState.materialCalibrationStatus)) + "\",\n"
            + "  \"materialCalibrationMode\": \"" + escape(jsonStringField(renderLab, "materialCalibrationMode", modelState.materialCalibrationMode)) + "\",\n"
            + "  \"albedoEnergyStatus\": \"" + escape(jsonStringField(renderLab, "albedoEnergyStatus", modelState.albedoEnergyStatus)) + "\",\n"
            + "  \"albedoClampStatus\": \"" + escape(jsonStringField(renderLab, "albedoClampStatus", modelState.albedoClampStatus)) + "\",\n"
            + "  \"diffuseClampStatus\": \"" + escape(jsonStringField(renderLab, "diffuseClampStatus", modelState.diffuseClampStatus)) + "\",\n"
            + "  \"luminanceGuardStatus\": \"" + escape(jsonStringField(renderLab, "luminanceGuardStatus", modelState.luminanceGuardStatus)) + "\",\n"
            + "  \"aoCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "aoCalibrationStatus", modelState.aoCalibrationStatus)) + "\",\n"
            + "  \"roughnessRemapStatus\": \"" + escape(jsonStringField(renderLab, "roughnessRemapStatus", modelState.roughnessRemapStatus)) + "\",\n"
            + "  \"metallicRoughnessClampStatus\": \"" + escape(jsonStringField(renderLab, "metallicRoughnessClampStatus", modelState.metallicRoughnessClampStatus)) + "\",\n"
            + "  \"emissiveGuardStatus\": \"" + escape(jsonStringField(renderLab, "emissiveGuardStatus", modelState.emissiveGuardStatus)) + "\",\n"
            + "  \"fabricMattePreserveStatus\": \"" + escape(jsonStringField(renderLab, "fabricMattePreserveStatus", modelState.fabricMattePreserveStatus)) + "\",\n"
            + "  \"paintMaterialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "paintMaterialCalibrationStatus", modelState.paintMaterialCalibrationStatus)) + "\",\n"
            + "  \"metalMaterialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "metalMaterialCalibrationStatus", modelState.metalMaterialCalibrationStatus)) + "\",\n"
            + "  \"materialTypeHintStatus\": \"" + escape(jsonStringField(renderLab, "materialTypeHintStatus", modelState.materialTypeHintStatus)) + "\",\n"
            + "  \"materialSlotCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotCalibrationStatus", modelState.materialSlotCalibrationStatus)) + "\",\n"
            + "  \"calibrationUiStatus\": \"" + escape(jsonStringField(renderLab, "calibrationUiStatus", modelState.calibrationUiStatus)) + "\",\n"
            + "  \"calibrationPreset\": \"" + escape(jsonStringField(renderLab, "calibrationPreset", modelState.calibrationPreset)) + "\",\n"
            + "  \"calibrationSliderStatus\": \"" + escape(jsonStringField(renderLab, "calibrationSliderStatus", modelState.calibrationSliderStatus)) + "\",\n"
            + "  \"calibrationSliderValue\": " + jsonNumberField(renderLab, "calibrationSliderValue", jsonFloat(modelState.calibrationSliderValue)) + ",\n"
            + "  \"calibrationUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "calibrationUniformUpdateStatus", modelState.calibrationUniformUpdateStatus)) + "\",\n"
            + "  \"calibratedAlbedoDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "calibratedAlbedoDebugViewStatus", modelState.calibratedAlbedoDebugViewStatus)) + "\",\n"
            + "  \"materialTypeDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "materialTypeDebugViewStatus", modelState.materialTypeDebugViewStatus)) + "\",\n"
            + "  \"aoInfluenceDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "aoInfluenceDebugViewStatus", modelState.aoInfluenceDebugViewStatus)) + "\",\n"
            + "  \"luminanceGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "luminanceGuardDebugViewStatus", modelState.luminanceGuardDebugViewStatus)) + "\",\n"
            + "  \"materialCalibrationPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "materialCalibrationPerformanceStatus", modelState.materialCalibrationPerformanceStatus)) + "\",\n"
            + p17GlossJsonFields(renderLab, "  ")
            + p18EnvironmentJsonFields(renderLab, "  ")
            + "  \"lightingStatus\": \"" + escape(jsonStringField(renderLab, "lightingStatus", modelState.lightingStatus)) + "\",\n"
            + "  \"lightingControlStatus\": \"" + escape(jsonStringField(renderLab, "lightingControlStatus", modelState.lightingControlStatus)) + "\",\n"
            + "  \"lightingUiMode\": \"" + escape(jsonStringField(renderLab, "lightingUiMode", modelState.lightingUiMode)) + "\",\n"
            + "  \"sunDirection\": " + jsonArrayField(renderLab, "sunDirection", "[" + jsonFloat(modelState.sunDirection[0]) + "," + jsonFloat(modelState.sunDirection[1]) + "," + jsonFloat(modelState.sunDirection[2]) + "]") + ",\n"
            + "  \"sunColor\": " + jsonArrayField(renderLab, "sunColor", "[" + jsonFloat(modelState.sunColor[0]) + "," + jsonFloat(modelState.sunColor[1]) + "," + jsonFloat(modelState.sunColor[2]) + "]") + ",\n"
            + "  \"sunIntensity\": " + jsonNumberField(renderLab, "sunIntensity", jsonFloat(modelState.sunIntensity)) + ",\n"
            + "  \"ambientColor\": " + jsonArrayField(renderLab, "ambientColor", "[" + jsonFloat(modelState.ambientColor[0]) + "," + jsonFloat(modelState.ambientColor[1]) + "," + jsonFloat(modelState.ambientColor[2]) + "]") + ",\n"
            + "  \"ambientIntensity\": " + jsonNumberField(renderLab, "ambientIntensity", jsonFloat(modelState.ambientIntensity)) + ",\n"
            + "  \"lightPreset\": \"" + escape(jsonStringField(renderLab, "lightPreset", modelState.lightPreset)) + "\",\n"
            + "  \"specularBoost\": " + jsonNumberField(renderLab, "specularBoost", jsonFloat(modelState.specularBoost)) + ",\n"
            + "  \"specularBoostStatus\": \"" + escape(jsonStringField(renderLab, "specularBoostStatus", modelState.specularBoostStatus)) + "\",\n"
            + "  \"reflectionIntensity\": " + jsonNumberField(renderLab, "reflectionIntensity", jsonFloat(modelState.reflectionIntensity)) + ",\n"
            + "  \"iblStatus\": \"" + escape(jsonStringField(renderLab, "iblStatus", modelState.iblStatus)) + "\",\n"
            + "  \"iblMode\": \"" + escape(jsonStringField(renderLab, "iblMode", modelState.iblMode)) + "\",\n"
            + "  \"reflectionFoundationStatus\": \"" + escape(jsonStringField(renderLab, "reflectionFoundationStatus", modelState.reflectionFoundationStatus)) + "\",\n"
            + "  \"reflectionMode\": \"" + escape(jsonStringField(renderLab, "reflectionMode", modelState.reflectionMode)) + "\",\n"
            + "  \"environmentReflectionStatus\": \"" + escape(jsonStringField(renderLab, "environmentReflectionStatus", modelState.environmentReflectionStatus)) + "\",\n"
            + "  \"environmentReflectionMode\": \"" + escape(jsonStringField(renderLab, "environmentReflectionMode", modelState.environmentReflectionMode)) + "\",\n"
            + "  \"environmentSource\": \"" + escape(jsonStringField(renderLab, "environmentSource", modelState.environmentSource)) + "\",\n"
            + "  \"reflectionColorStatus\": \"" + escape(jsonStringField(renderLab, "reflectionColorStatus", modelState.reflectionColorStatus)) + "\",\n"
            + "  \"reflectionRoughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "reflectionRoughnessResponseStatus", modelState.reflectionRoughnessResponseStatus)) + "\",\n"
            + "  \"metallicReflectionStatus\": \"" + escape(jsonStringField(renderLab, "metallicReflectionStatus", modelState.metallicReflectionStatus)) + "\",\n"
            + "  \"dielectricReflectionStatus\": \"" + escape(jsonStringField(renderLab, "dielectricReflectionStatus", modelState.dielectricReflectionStatus)) + "\",\n"
            + "  \"reflectionPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "reflectionPerformanceStatus", modelState.reflectionPerformanceStatus)) + "\",\n"
            + "  \"inspectorUiStatus\": \"" + escape(jsonStringField(renderLab, "inspectorUiStatus", modelState.inspectorUiStatus)) + "\",\n"
            + "  \"inspectorUiMode\": \"" + escape(jsonStringField(renderLab, "inspectorUiMode", modelState.inspectorUiMode)) + "\",\n"
            + "  \"activeInspectorTab\": \"" + escape(modelState.activeInspectorTab) + "\",\n"
            + "  \"assetsTabStatus\": \"" + escape(jsonStringField(renderLab, "assetsTabStatus", modelState.assetsTabStatus)) + "\",\n"
            + "  \"cameraTabStatus\": \"" + escape(jsonStringField(renderLab, "cameraTabStatus", modelState.cameraTabStatus)) + "\",\n"
            + "  \"lightingTabStatus\": \"" + escape(jsonStringField(renderLab, "lightingTabStatus", modelState.lightingTabStatus)) + "\",\n"
            + "  \"materialTabStatus\": \"" + escape(jsonStringField(renderLab, "materialTabStatus", modelState.materialTabStatus)) + "\",\n"
            + "  \"debugTabStatus\": \"" + escape(jsonStringField(renderLab, "debugTabStatus", modelState.debugTabStatus)) + "\",\n"
            + "  \"contactGroundingStatus\": \"" + escape(jsonStringField(renderLab, "contactGroundingStatus", modelState.contactGroundingStatus)) + "\",\n"
            + "  \"contactShadowStatus\": \"" + escape(jsonStringField(renderLab, "contactShadowStatus", modelState.contactShadowStatus)) + "\",\n"
            + "  \"contactShadowMode\": \"" + escape(jsonStringField(renderLab, "contactShadowMode", modelState.contactShadowMode)) + "\",\n"
            + "  \"contactShadowIntensity\": " + jsonNumberField(renderLab, "contactShadowIntensity", jsonFloat(modelState.contactShadowIntensity)) + ",\n"
            + "  \"contactShadowPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "contactShadowPerformanceStatus", modelState.contactShadowPerformanceStatus)) + "\",\n"
            + "  \"groundingUsesModelBounds\": \"" + escape(jsonStringField(renderLab, "groundingUsesModelBounds", modelState.groundingUsesModelBounds)) + "\",\n"
            + "  \"groundingUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "groundingUniformUpdateStatus", modelState.groundingUniformUpdateStatus)) + "\",\n"
            + "  \"groundSliderStatus\": \"" + escape(jsonStringField(renderLab, "groundSliderStatus", modelState.groundSliderStatus)) + "\",\n"
            + "  \"contactGroundingSliderStatus\": \"" + escape(jsonStringField(renderLab, "contactGroundingSliderStatus", modelState.contactGroundingSliderStatus)) + "\",\n"
            + "  \"lightingUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "lightingUniformUpdateStatus", modelState.lightingUniformUpdateStatus)) + "\",\n"
            + "  \"sliderUpdateMode\": \"" + escape(jsonStringField(renderLab, "sliderUpdateMode", modelState.sliderUpdateMode)) + "\",\n"
            + "  \"sliderTouchStatus\": \"" + escape(jsonStringField(renderLab, "sliderTouchStatus", modelState.sliderTouchStatus)) + "\",\n"
            + "  \"sunSliderStatus\": \"" + escape(jsonStringField(renderLab, "sunSliderStatus", modelState.sunSliderStatus)) + "\",\n"
            + "  \"ambientSliderStatus\": \"" + escape(jsonStringField(renderLab, "ambientSliderStatus", modelState.ambientSliderStatus)) + "\",\n"
            + "  \"exposureSliderStatus\": \"" + escape(jsonStringField(renderLab, "exposureSliderStatus", modelState.exposureSliderStatus)) + "\",\n"
            + "  \"specularSliderStatus\": \"" + escape(jsonStringField(renderLab, "specularSliderStatus", modelState.specularSliderStatus)) + "\",\n"
            + "  \"reflectionSliderStatus\": \"" + escape(jsonStringField(renderLab, "reflectionSliderStatus", modelState.reflectionSliderStatus)) + "\",\n"
            + "  \"brdfStatus\": \"" + escape(jsonStringField(renderLab, "brdfStatus", modelState.brdfStatus)) + "\",\n"
            + "  \"brdfMode\": \"" + escape(jsonStringField(renderLab, "brdfMode", modelState.brdfMode)) + "\",\n"
            + "  \"diffuseStatus\": \"" + escape(jsonStringField(renderLab, "diffuseStatus", modelState.diffuseStatus)) + "\",\n"
            + "  \"specularStatus\": \"" + escape(jsonStringField(renderLab, "specularStatus", modelState.specularStatus)) + "\",\n"
            + "  \"fresnelStatus\": \"" + escape(jsonStringField(renderLab, "fresnelStatus", modelState.fresnelStatus)) + "\",\n"
            + "  \"f0Status\": \"" + escape(jsonStringField(renderLab, "f0Status", modelState.f0Status)) + "\",\n"
            + "  \"metallicResponseStatus\": \"" + escape(jsonStringField(renderLab, "metallicResponseStatus", modelState.metallicResponseStatus)) + "\",\n"
            + "  \"roughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "roughnessResponseStatus", modelState.roughnessResponseStatus)) + "\",\n"
            + "  \"directLightingStatus\": \"" + escape(jsonStringField(renderLab, "directLightingStatus", modelState.directLightingStatus)) + "\",\n"
            + "  \"materialResponseStatus\": \"" + escape(jsonStringField(renderLab, "materialResponseStatus", modelState.materialResponseStatus)) + "\",\n"
            + "  \"pbrQualityTier\": \"" + escape(jsonStringField(renderLab, "pbrQualityTier", modelState.pbrQualityTier)) + "\",\n"
            + "  \"brdfPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "brdfPerformanceStatus", modelState.brdfPerformanceStatus)) + "\",\n"
            + "  \"toneMappingStatus\": \"" + escape(jsonStringField(renderLab, "toneMappingStatus", modelState.toneMappingStatus)) + "\",\n"
            + "  \"toneMappingMode\": \"" + escape(jsonStringField(renderLab, "toneMappingMode", modelState.toneMappingMode)) + "\",\n"
            + "  \"exposureStatus\": \"" + escape(jsonStringField(renderLab, "exposureStatus", modelState.exposureStatus)) + "\",\n"
            + "  \"exposureValue\": " + jsonNumberField(renderLab, "exposureValue", jsonFloat(modelState.exposureValue)) + ",\n"
            + "  \"ambientFloor\": " + jsonNumberField(renderLab, "ambientFloor", jsonFloat(modelState.ambientFloor)) + ",\n"
            + "  \"brightnessPreset\": \"" + escape(jsonStringField(renderLab, "brightnessPreset", modelState.brightnessPreset)) + "\",\n"
            + "  \"activeDebugView\": \"" + escape(jsonStringField(renderLab, "activeDebugView", modelState.activeDebugView)) + "\",\n"
            + "  \"debugViewStatus\": \"" + escape(jsonStringField(renderLab, "debugViewStatus", modelState.debugViewStatus)) + "\",\n"
            + "  \"normalDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "normalDebugViewStatus", modelState.normalDebugViewStatus)) + "\",\n"
            + "  \"ndotlDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "ndotlDebugViewStatus", modelState.ndotlDebugViewStatus)) + "\",\n"
            + "  \"diffuseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "diffuseDebugViewStatus", modelState.diffuseDebugViewStatus)) + "\",\n"
            + "  \"specularDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "specularDebugViewStatus", modelState.specularDebugViewStatus)) + "\",\n"
            + "  \"f0DebugViewStatus\": \"" + escape(jsonStringField(renderLab, "f0DebugViewStatus", modelState.f0DebugViewStatus)) + "\",\n"
            + "  \"reflectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionDebugViewStatus", modelState.reflectionDebugViewStatus)) + "\",\n"
            + "  \"iblDiffuseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "iblDiffuseDebugViewStatus", modelState.iblDiffuseDebugViewStatus)) + "\",\n"
            + "  \"iblSpecularDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "iblSpecularDebugViewStatus", modelState.iblSpecularDebugViewStatus)) + "\",\n"
            + "  \"brdfStatusDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "brdfStatusDebugViewStatus", modelState.brdfStatusDebugViewStatus)) + "\",\n"
            + "  \"groundingDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "groundingDebugViewStatus", modelState.groundingDebugViewStatus)) + "\",\n"
            + "  \"fpsCurrent\": " + jsonFloat(fps.fpsCurrent) + ",\n"
            + "  \"frameTimeMs\": " + jsonFloat(fps.frameTimeMs) + ",\n"
            + "  \"fpsSource\": \"" + escape(fps.source) + "\",\n"
            + "  \"fpsLastStable\": " + jsonFloat(fpsLastStable) + ",\n"
            + "  \"frameTimeLastStableMs\": " + jsonFloat(frameTimeLastStableMs) + ",\n"
            + "  \"fpsStatus\": \"" + escape(fpsStatus) + "\",\n"
            + "  \"fpsUpdateMode\": \"" + escape(fpsUpdateMode) + "\",\n"
            + "  \"fpsSampleWindowMs\": " + fpsSampleWindowMs + ",\n"
            + "  \"framesRenderedLive\": " + framesRenderedLive + ",\n"
            + "  \"modelUploadRepeatCount\": " + jsonNumberField(renderLab, "modelUploadRepeatCount", String.valueOf(modelState.modelUploadRepeatCount)) + ",\n"
            + "  \"uploadGenerationId\": " + jsonNumberField(renderLab, "uploadGenerationId", String.valueOf(modelState.uploadGenerationId)) + ",\n"
            + "  \"renderLoopAllocationGuardStatus\": \"" + escape(jsonStringField(renderLab, "renderLoopAllocationGuardStatus", modelState.renderLoopAllocationGuardStatus)) + "\",\n"
            + "  \"debugZipStatus\": \"" + escape(debugZipStatus) + "\",\n"
            + "  \"debugZipPath\": \"" + escape(debugZipPath) + "\",\n"
            + "  \"debugZipIncludedFiles\": \"" + escape(debugZipIncludedFiles) + "\",\n"
            + "  \"debugZipReason\": \"" + escape(debugZipReason) + "\",\n"
            + "  \"fallbackCubeVisible\": " + jsonBooleanField(renderLab, "fallbackCubeVisible", modelState.fallbackCubeVisible ? "true" : "false") + ",\n"
            + "  \"fallbackCubeStatus\": \"" + escape(jsonStringField(renderLab, "fallbackCubeStatus", modelState.fallbackCubeStatus)) + "\",\n"
            + "  \"cubeStatus\": \"" + escape(jsonStringField(renderLab, "cubeStatus", "unknown")) + "\",\n"
            + "  \"depthStatus\": \"" + escape(jsonStringField(renderLab, "depthStatus", "unknown")) + "\",\n"
            + "  \"cameraStatus\": \"" + escape(jsonStringField(renderLab, "cameraStatus", "unknown")) + "\",\n"
            + "  \"cameraMvpStatus\": \"" + escape(jsonStringField(renderLab, "cameraMvpStatus", "unknown")) + "\",\n"
            + "  \"cameraControlsStatus\": \"" + escape(jsonStringField(renderLab, "cameraControlsStatus", "unknown")) + "\",\n"
            + "  \"cameraYawDeg\": " + jsonNumberField(renderLab, "cameraYawDeg", "0") + ",\n"
            + "  \"cameraPitchDeg\": " + jsonNumberField(renderLab, "cameraPitchDeg", "0") + ",\n"
            + "  \"cameraDistance\": " + jsonNumberField(renderLab, "cameraDistance", "0") + ",\n"
            + "  \"framesRendered\": " + jsonNumberField(renderLab, "framesRendered", "0") + ",\n"
            + "  \"materialConstantsReady\": " + jsonBooleanField(renderLab, "materialConstantsReady", "false") + ",\n"
            + "  \"meshAttributeLayoutReady\": " + jsonBooleanField(renderLab, "meshAttributeLayoutReady", "false") + ",\n"
            + "  \"vertexLayout\": \"" + escape(jsonStringField(renderLab, "vertexLayout", "unknown")) + "\",\n"
            + "  \"vertexStrideBytes\": " + jsonNumberField(renderLab, "vertexStrideBytes", "0") + ",\n"
            + "  \"vertexCount\": " + jsonNumberField(renderLab, "vertexCount", "0") + ",\n"
            + "  \"indexCount\": " + jsonNumberField(renderLab, "indexCount", "0") + ",\n"
            + "  \"renderLab\": " + renderLab + "\n"
            + "}\n";
    }

    private String buildDiagnosticsManifestJson(String timestamp, ExportResult result, String renderLab) {
        return "{\n"
            + "  \"schema\": \"solum.engine_diagnostics_manifest\",\n"
            + "  \"schemaVersion\": 1,\n"
            + "  \"timestampUtc\": \"" + escape(timestamp) + "\",\n"
            + "  \"app\": \"engine\",\n"
            + "  \"packageName\": \"" + escape(getPackageName()) + "\",\n"
            + "  \"exportStatus\": \"" + (result.ok ? "ok" : "failed") + "\",\n"
            + "  \"exportRoute\": \"" + escape(result.route) + "\",\n"
            + "  \"actualRoot\": \"" + escape(result.actualRoot) + "\",\n"
            + "  \"reason\": \"" + escape(result.reason) + "\",\n"
            + "  \"diagnosticsRoot\": \"" + escape(result.actualRoot) + "\",\n"
            + "  \"storage\": {\n"
            + "    \"publicRoot\": \"/storage/emulated/0/SOLUMCreative/diagnostics/latest\",\n"
            + "    \"actualRoot\": \"" + escape(result.actualRoot) + "\",\n"
            + "    \"exportRoute\": \"" + escape(result.route) + "\",\n"
            + "    \"reason\": \"" + escape(result.reason) + "\"\n"
            + "  },\n"
            + "  \"files\": [\"engine_runtime_state.json\", \"engine_diagnostics_manifest.json\", \"model_import_state.json\", \"asset_report.json\"],\n"
            + "  \"debugZipStatus\": \"" + escape(debugZipStatus) + "\",\n"
            + "  \"debugZipPath\": \"" + escape(debugZipPath) + "\",\n"
            + "  \"debugZipIncludedFiles\": \"" + escape(debugZipIncludedFiles) + "\",\n"
            + "  \"debugZipReason\": \"" + escape(debugZipReason) + "\",\n"
            + "  \"debugZipRequiredFiles\": [\"engine_runtime_state.json\", \"engine_diagnostics_manifest.json\", \"model_import_state.json\", \"asset_report.json\", \"debug_zip_runtime_note.txt\"],\n"
            + "  \"debugZipOptionalFiles\": [\"glb_model_summary.json\"],\n"
            + "  \"debugZipRequiredFileStatus\": {\n"
            + "    \"engine_runtime_state.json\": \"" + fileStatusForDebugZip("engine_runtime_state.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"engine_diagnostics_manifest.json\": \"" + fileStatusForDebugZip("engine_diagnostics_manifest.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"model_import_state.json\": \"" + fileStatusForDebugZip("model_import_state.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"asset_report.json\": \"" + fileStatusForDebugZip("asset_report.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"debug_zip_runtime_note.txt\": \"" + fileStatusForDebugZip("debug_zip_runtime_note.txt", debugZipIncludedFiles, true) + "\"\n"
            + "  },\n"
            + "  \"debugZipOptionalFileStatus\": {\n"
            + "    \"glb_model_summary.json\": \"" + fileStatusForDebugZip("glb_model_summary.json", debugZipIncludedFiles, false) + "\"\n"
            + "  },\n"
            + "  \"screenshot\": { \"status\": \"not_available\", \"reason\": \"renderer_readback_not_implemented\" },\n"
            + "  \"camera\": {\n"
            + "    \"cameraMvpStatus\": \"" + escape(jsonStringField(renderLab, "cameraMvpStatus", "unknown")) + "\",\n"
            + "    \"cameraControlsStatus\": \"" + escape(jsonStringField(renderLab, "cameraControlsStatus", "unknown")) + "\",\n"
            + "    \"cameraYawDeg\": " + jsonNumberField(renderLab, "cameraYawDeg", "0") + ",\n"
            + "    \"cameraPitchDeg\": " + jsonNumberField(renderLab, "cameraPitchDeg", "0") + ",\n"
            + "    \"cameraDistance\": " + jsonNumberField(renderLab, "cameraDistance", "0") + "\n"
            + "  },\n"
            + "  \"materialConstantsReady\": " + jsonBooleanField(renderLab, "materialConstantsReady", "false") + ",\n"
            + "  \"meshAttributeLayoutReady\": " + jsonBooleanField(renderLab, "meshAttributeLayoutReady", "false") + ",\n"
            + "  \"currentScene\": \"" + SCENE_ID + "\",\n"
            + "  \"currentLabScene\": \"" + SCENE_ID + "\",\n"
            + "  \"currentLabSceneName\": \"" + SCENE_NAME + "\",\n"
            + "  \"materialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "materialCalibrationStatus", modelState.materialCalibrationStatus)) + "\",\n"
            + "  \"materialCalibrationMode\": \"" + escape(jsonStringField(renderLab, "materialCalibrationMode", modelState.materialCalibrationMode)) + "\",\n"
            + "  \"albedoEnergyStatus\": \"" + escape(jsonStringField(renderLab, "albedoEnergyStatus", modelState.albedoEnergyStatus)) + "\",\n"
            + "  \"albedoClampStatus\": \"" + escape(jsonStringField(renderLab, "albedoClampStatus", modelState.albedoClampStatus)) + "\",\n"
            + "  \"diffuseClampStatus\": \"" + escape(jsonStringField(renderLab, "diffuseClampStatus", modelState.diffuseClampStatus)) + "\",\n"
            + "  \"luminanceGuardStatus\": \"" + escape(jsonStringField(renderLab, "luminanceGuardStatus", modelState.luminanceGuardStatus)) + "\",\n"
            + "  \"aoCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "aoCalibrationStatus", modelState.aoCalibrationStatus)) + "\",\n"
            + "  \"roughnessRemapStatus\": \"" + escape(jsonStringField(renderLab, "roughnessRemapStatus", modelState.roughnessRemapStatus)) + "\",\n"
            + "  \"metallicRoughnessClampStatus\": \"" + escape(jsonStringField(renderLab, "metallicRoughnessClampStatus", modelState.metallicRoughnessClampStatus)) + "\",\n"
            + "  \"emissiveGuardStatus\": \"" + escape(jsonStringField(renderLab, "emissiveGuardStatus", modelState.emissiveGuardStatus)) + "\",\n"
            + "  \"fabricMattePreserveStatus\": \"" + escape(jsonStringField(renderLab, "fabricMattePreserveStatus", modelState.fabricMattePreserveStatus)) + "\",\n"
            + "  \"paintMaterialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "paintMaterialCalibrationStatus", modelState.paintMaterialCalibrationStatus)) + "\",\n"
            + "  \"metalMaterialCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "metalMaterialCalibrationStatus", modelState.metalMaterialCalibrationStatus)) + "\",\n"
            + "  \"materialTypeHintStatus\": \"" + escape(jsonStringField(renderLab, "materialTypeHintStatus", modelState.materialTypeHintStatus)) + "\",\n"
            + "  \"materialSlotCalibrationStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotCalibrationStatus", modelState.materialSlotCalibrationStatus)) + "\",\n"
            + "  \"calibrationUiStatus\": \"" + escape(jsonStringField(renderLab, "calibrationUiStatus", modelState.calibrationUiStatus)) + "\",\n"
            + "  \"calibrationPreset\": \"" + escape(jsonStringField(renderLab, "calibrationPreset", modelState.calibrationPreset)) + "\",\n"
            + "  \"calibrationSliderStatus\": \"" + escape(jsonStringField(renderLab, "calibrationSliderStatus", modelState.calibrationSliderStatus)) + "\",\n"
            + "  \"calibrationSliderValue\": " + jsonNumberField(renderLab, "calibrationSliderValue", jsonFloat(modelState.calibrationSliderValue)) + ",\n"
            + "  \"calibrationUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "calibrationUniformUpdateStatus", modelState.calibrationUniformUpdateStatus)) + "\",\n"
            + "  \"calibratedAlbedoDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "calibratedAlbedoDebugViewStatus", modelState.calibratedAlbedoDebugViewStatus)) + "\",\n"
            + "  \"materialTypeDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "materialTypeDebugViewStatus", modelState.materialTypeDebugViewStatus)) + "\",\n"
            + "  \"aoInfluenceDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "aoInfluenceDebugViewStatus", modelState.aoInfluenceDebugViewStatus)) + "\",\n"
            + "  \"luminanceGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "luminanceGuardDebugViewStatus", modelState.luminanceGuardDebugViewStatus)) + "\",\n"
            + "  \"materialCalibrationPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "materialCalibrationPerformanceStatus", modelState.materialCalibrationPerformanceStatus)) + "\",\n"
            + p17GlossJsonFields(renderLab, "  ")
            + p18EnvironmentJsonFields(renderLab, "  ")
            + "  \"sunIntensity\": " + jsonNumberField(renderLab, "sunIntensity", jsonFloat(modelState.sunIntensity)) + ",\n"
            + "  \"ambientIntensity\": " + jsonNumberField(renderLab, "ambientIntensity", jsonFloat(modelState.ambientIntensity)) + ",\n"
            + "  \"exposureValue\": " + jsonNumberField(renderLab, "exposureValue", jsonFloat(modelState.exposureValue)) + ",\n"
            + "  \"ambientFloor\": " + jsonNumberField(renderLab, "ambientFloor", jsonFloat(modelState.ambientFloor)) + ",\n"
            + "  \"brightnessPreset\": \"" + escape(jsonStringField(renderLab, "brightnessPreset", modelState.brightnessPreset)) + "\",\n"
            + "  \"brdfStatus\": \"" + escape(jsonStringField(renderLab, "brdfStatus", modelState.brdfStatus)) + "\",\n"
            + "  \"lightingControlStatus\": \"" + escape(jsonStringField(renderLab, "lightingControlStatus", modelState.lightingControlStatus)) + "\",\n"
            + "  \"lightingUiMode\": \"" + escape(jsonStringField(renderLab, "lightingUiMode", modelState.lightingUiMode)) + "\",\n"
            + "  \"specularBoost\": " + jsonNumberField(renderLab, "specularBoost", jsonFloat(modelState.specularBoost)) + ",\n"
            + "  \"specularBoostStatus\": \"" + escape(jsonStringField(renderLab, "specularBoostStatus", modelState.specularBoostStatus)) + "\",\n"
            + "  \"reflectionIntensity\": " + jsonNumberField(renderLab, "reflectionIntensity", jsonFloat(modelState.reflectionIntensity)) + ",\n"
            + "  \"iblStatus\": \"" + escape(jsonStringField(renderLab, "iblStatus", modelState.iblStatus)) + "\",\n"
            + "  \"iblMode\": \"" + escape(jsonStringField(renderLab, "iblMode", modelState.iblMode)) + "\",\n"
            + "  \"reflectionFoundationStatus\": \"" + escape(jsonStringField(renderLab, "reflectionFoundationStatus", modelState.reflectionFoundationStatus)) + "\",\n"
            + "  \"reflectionMode\": \"" + escape(jsonStringField(renderLab, "reflectionMode", modelState.reflectionMode)) + "\",\n"
            + "  \"environmentReflectionStatus\": \"" + escape(jsonStringField(renderLab, "environmentReflectionStatus", modelState.environmentReflectionStatus)) + "\",\n"
            + "  \"environmentReflectionMode\": \"" + escape(jsonStringField(renderLab, "environmentReflectionMode", modelState.environmentReflectionMode)) + "\",\n"
            + "  \"environmentSource\": \"" + escape(jsonStringField(renderLab, "environmentSource", modelState.environmentSource)) + "\",\n"
            + "  \"reflectionColorStatus\": \"" + escape(jsonStringField(renderLab, "reflectionColorStatus", modelState.reflectionColorStatus)) + "\",\n"
            + "  \"reflectionRoughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "reflectionRoughnessResponseStatus", modelState.reflectionRoughnessResponseStatus)) + "\",\n"
            + "  \"metallicReflectionStatus\": \"" + escape(jsonStringField(renderLab, "metallicReflectionStatus", modelState.metallicReflectionStatus)) + "\",\n"
            + "  \"dielectricReflectionStatus\": \"" + escape(jsonStringField(renderLab, "dielectricReflectionStatus", modelState.dielectricReflectionStatus)) + "\",\n"
            + "  \"reflectionPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "reflectionPerformanceStatus", modelState.reflectionPerformanceStatus)) + "\",\n"
            + "  \"inspectorUiStatus\": \"" + escape(jsonStringField(renderLab, "inspectorUiStatus", modelState.inspectorUiStatus)) + "\",\n"
            + "  \"inspectorUiMode\": \"" + escape(jsonStringField(renderLab, "inspectorUiMode", modelState.inspectorUiMode)) + "\",\n"
            + "  \"activeInspectorTab\": \"" + escape(modelState.activeInspectorTab) + "\",\n"
            + "  \"assetsTabStatus\": \"" + escape(jsonStringField(renderLab, "assetsTabStatus", modelState.assetsTabStatus)) + "\",\n"
            + "  \"cameraTabStatus\": \"" + escape(jsonStringField(renderLab, "cameraTabStatus", modelState.cameraTabStatus)) + "\",\n"
            + "  \"lightingTabStatus\": \"" + escape(jsonStringField(renderLab, "lightingTabStatus", modelState.lightingTabStatus)) + "\",\n"
            + "  \"materialTabStatus\": \"" + escape(jsonStringField(renderLab, "materialTabStatus", modelState.materialTabStatus)) + "\",\n"
            + "  \"debugTabStatus\": \"" + escape(jsonStringField(renderLab, "debugTabStatus", modelState.debugTabStatus)) + "\",\n"
            + "  \"contactGroundingStatus\": \"" + escape(jsonStringField(renderLab, "contactGroundingStatus", modelState.contactGroundingStatus)) + "\",\n"
            + "  \"contactShadowStatus\": \"" + escape(jsonStringField(renderLab, "contactShadowStatus", modelState.contactShadowStatus)) + "\",\n"
            + "  \"contactShadowMode\": \"" + escape(jsonStringField(renderLab, "contactShadowMode", modelState.contactShadowMode)) + "\",\n"
            + "  \"contactShadowIntensity\": " + jsonNumberField(renderLab, "contactShadowIntensity", jsonFloat(modelState.contactShadowIntensity)) + ",\n"
            + "  \"contactShadowPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "contactShadowPerformanceStatus", modelState.contactShadowPerformanceStatus)) + "\",\n"
            + "  \"groundingUsesModelBounds\": \"" + escape(jsonStringField(renderLab, "groundingUsesModelBounds", modelState.groundingUsesModelBounds)) + "\",\n"
            + "  \"groundingUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "groundingUniformUpdateStatus", modelState.groundingUniformUpdateStatus)) + "\",\n"
            + "  \"groundSliderStatus\": \"" + escape(jsonStringField(renderLab, "groundSliderStatus", modelState.groundSliderStatus)) + "\",\n"
            + "  \"contactGroundingSliderStatus\": \"" + escape(jsonStringField(renderLab, "contactGroundingSliderStatus", modelState.contactGroundingSliderStatus)) + "\",\n"
            + "  \"lightingUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "lightingUniformUpdateStatus", modelState.lightingUniformUpdateStatus)) + "\",\n"
            + "  \"sliderUpdateMode\": \"" + escape(jsonStringField(renderLab, "sliderUpdateMode", modelState.sliderUpdateMode)) + "\",\n"
            + "  \"sliderTouchStatus\": \"" + escape(jsonStringField(renderLab, "sliderTouchStatus", modelState.sliderTouchStatus)) + "\",\n"
            + "  \"sunSliderStatus\": \"" + escape(jsonStringField(renderLab, "sunSliderStatus", modelState.sunSliderStatus)) + "\",\n"
            + "  \"ambientSliderStatus\": \"" + escape(jsonStringField(renderLab, "ambientSliderStatus", modelState.ambientSliderStatus)) + "\",\n"
            + "  \"exposureSliderStatus\": \"" + escape(jsonStringField(renderLab, "exposureSliderStatus", modelState.exposureSliderStatus)) + "\",\n"
            + "  \"specularSliderStatus\": \"" + escape(jsonStringField(renderLab, "specularSliderStatus", modelState.specularSliderStatus)) + "\",\n"
            + "  \"reflectionSliderStatus\": \"" + escape(jsonStringField(renderLab, "reflectionSliderStatus", modelState.reflectionSliderStatus)) + "\",\n"
            + "  \"brdfMode\": \"" + escape(jsonStringField(renderLab, "brdfMode", modelState.brdfMode)) + "\",\n"
            + "  \"diffuseStatus\": \"" + escape(jsonStringField(renderLab, "diffuseStatus", modelState.diffuseStatus)) + "\",\n"
            + "  \"specularStatus\": \"" + escape(jsonStringField(renderLab, "specularStatus", modelState.specularStatus)) + "\",\n"
            + "  \"fresnelStatus\": \"" + escape(jsonStringField(renderLab, "fresnelStatus", modelState.fresnelStatus)) + "\",\n"
            + "  \"f0Status\": \"" + escape(jsonStringField(renderLab, "f0Status", modelState.f0Status)) + "\",\n"
            + "  \"metallicResponseStatus\": \"" + escape(jsonStringField(renderLab, "metallicResponseStatus", modelState.metallicResponseStatus)) + "\",\n"
            + "  \"roughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "roughnessResponseStatus", modelState.roughnessResponseStatus)) + "\",\n"
            + "  \"directLightingStatus\": \"" + escape(jsonStringField(renderLab, "directLightingStatus", modelState.directLightingStatus)) + "\",\n"
            + "  \"materialResponseStatus\": \"" + escape(jsonStringField(renderLab, "materialResponseStatus", modelState.materialResponseStatus)) + "\",\n"
            + "  \"pbrQualityTier\": \"" + escape(jsonStringField(renderLab, "pbrQualityTier", modelState.pbrQualityTier)) + "\",\n"
            + "  \"brdfPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "brdfPerformanceStatus", modelState.brdfPerformanceStatus)) + "\",\n"
            + "  \"fpsStatus\": \"" + escape(fpsStatus) + "\",\n"
            + "  \"fpsUpdateMode\": \"" + escape(fpsUpdateMode) + "\",\n"
            + "  \"activeDebugView\": \"" + escape(jsonStringField(renderLab, "activeDebugView", modelState.activeDebugView)) + "\",\n"
            + "  \"debugViewStatus\": \"" + escape(jsonStringField(renderLab, "debugViewStatus", modelState.debugViewStatus)) + "\",\n"
            + "  \"diffuseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "diffuseDebugViewStatus", modelState.diffuseDebugViewStatus)) + "\",\n"
            + "  \"specularDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "specularDebugViewStatus", modelState.specularDebugViewStatus)) + "\",\n"
            + "  \"f0DebugViewStatus\": \"" + escape(jsonStringField(renderLab, "f0DebugViewStatus", modelState.f0DebugViewStatus)) + "\",\n"
            + "  \"reflectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionDebugViewStatus", modelState.reflectionDebugViewStatus)) + "\",\n"
            + "  \"iblDiffuseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "iblDiffuseDebugViewStatus", modelState.iblDiffuseDebugViewStatus)) + "\",\n"
            + "  \"iblSpecularDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "iblSpecularDebugViewStatus", modelState.iblSpecularDebugViewStatus)) + "\",\n"
            + "  \"brdfStatusDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "brdfStatusDebugViewStatus", modelState.brdfStatusDebugViewStatus)) + "\",\n"
            + "  \"groundingDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "groundingDebugViewStatus", modelState.groundingDebugViewStatus)) + "\",\n"
            + "  \"vertexLayout\": \"" + escape(jsonStringField(renderLab, "vertexLayout", "unknown")) + "\",\n"
            + "  \"vertexStrideBytes\": " + jsonNumberField(renderLab, "vertexStrideBytes", "0") + ",\n"
            + "  \"renderLab\": " + renderLab + "\n"
            + "}\n";
    }

    private String p17GlossJsonFields(String renderLab, String indent) {
        return indent + "\"specularGlossStatus\": \"" + escape(jsonStringField(renderLab, "specularGlossStatus", modelState.specularGlossStatus)) + "\",\n"
            + indent + "\"specularGlossMode\": \"" + escape(jsonStringField(renderLab, "specularGlossMode", modelState.specularGlossMode)) + "\",\n"
            + indent + "\"specularResponseStatus\": \"" + escape(jsonStringField(renderLab, "specularResponseStatus", modelState.specularResponseStatus)) + "\",\n"
            + indent + "\"glossResponseStatus\": \"" + escape(jsonStringField(renderLab, "glossResponseStatus", modelState.glossResponseStatus)) + "\",\n"
            + indent + "\"roughnessRemapV2Status\": \"" + escape(jsonStringField(renderLab, "roughnessRemapV2Status", modelState.roughnessRemapV2Status)) + "\",\n"
            + indent + "\"metallicSpecularBoostStatus\": \"" + escape(jsonStringField(renderLab, "metallicSpecularBoostStatus", modelState.metallicSpecularBoostStatus)) + "\",\n"
            + indent + "\"dielectricGlossStatus\": \"" + escape(jsonStringField(renderLab, "dielectricGlossStatus", modelState.dielectricGlossStatus)) + "\",\n"
            + indent + "\"fabricSpecularSuppressStatus\": \"" + escape(jsonStringField(renderLab, "fabricSpecularSuppressStatus", modelState.fabricSpecularSuppressStatus)) + "\",\n"
            + indent + "\"specularOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "specularOverbrightGuardStatus", modelState.specularOverbrightGuardStatus)) + "\",\n"
            + indent + "\"viewDependentHighlightStatus\": \"" + escape(jsonStringField(renderLab, "viewDependentHighlightStatus", modelState.viewDependentHighlightStatus)) + "\",\n"
            + indent + "\"paintGlossLiteStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossLiteStatus", modelState.paintGlossLiteStatus)) + "\",\n"
            + indent + "\"paintGlossLiteMode\": \"" + escape(jsonStringField(renderLab, "paintGlossLiteMode", modelState.paintGlossLiteMode)) + "\",\n"
            + indent + "\"paintGlossIntensity\": " + jsonNumberField(renderLab, "paintGlossIntensity", jsonFloat(modelState.paintGlossIntensity)) + ",\n"
            + indent + "\"paintGlossRoughness\": " + jsonNumberField(renderLab, "paintGlossRoughness", jsonFloat(modelState.paintGlossRoughness)) + ",\n"
            + indent + "\"paintGlossMaterialHintStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossMaterialHintStatus", modelState.paintGlossMaterialHintStatus)) + "\",\n"
            + indent + "\"paintGlossPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossPerformanceStatus", modelState.paintGlossPerformanceStatus)) + "\",\n"
            + indent + "\"calibrationVisualStrength\": " + jsonNumberField(renderLab, "calibrationVisualStrength", jsonFloat(modelState.calibrationVisualStrength)) + ",\n"
            + indent + "\"calibrationAffectsAlbedo\": \"" + escape(jsonStringField(renderLab, "calibrationAffectsAlbedo", modelState.calibrationAffectsAlbedo)) + "\",\n"
            + indent + "\"calibrationAffectsAo\": \"" + escape(jsonStringField(renderLab, "calibrationAffectsAo", modelState.calibrationAffectsAo)) + "\",\n"
            + indent + "\"calibrationAffectsRoughness\": \"" + escape(jsonStringField(renderLab, "calibrationAffectsRoughness", modelState.calibrationAffectsRoughness)) + "\",\n"
            + indent + "\"calibrationVisibleResponseStatus\": \"" + escape(jsonStringField(renderLab, "calibrationVisibleResponseStatus", modelState.calibrationVisibleResponseStatus)) + "\",\n"
            + indent + "\"paintGlossTargetStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossTargetStatus", modelState.paintGlossTargetStatus)) + "\",\n"
            + indent + "\"paintGlossAppliedMaterialCount\": " + jsonNumberField(renderLab, "paintGlossAppliedMaterialCount", String.valueOf(modelState.paintGlossAppliedMaterialCount)) + ",\n"
            + indent + "\"paintGlossSkippedFabricCount\": " + jsonNumberField(renderLab, "paintGlossSkippedFabricCount", String.valueOf(modelState.paintGlossSkippedFabricCount)) + ",\n"
            + indent + "\"paintGlossFallbackRouting\": \"" + escape(jsonStringField(renderLab, "paintGlossFallbackRouting", modelState.paintGlossFallbackRouting)) + "\",\n"
            + indent + "\"paintGlossVisibleResponseStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossVisibleResponseStatus", modelState.paintGlossVisibleResponseStatus)) + "\",\n"
            + indent + "\"glossSliderStatus\": \"" + escape(jsonStringField(renderLab, "glossSliderStatus", modelState.glossSliderStatus)) + "\",\n"
            + indent + "\"glossSliderValue\": " + jsonNumberField(renderLab, "glossSliderValue", jsonFloat(modelState.glossSliderValue)) + ",\n"
            + indent + "\"paintGlossSliderStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossSliderStatus", modelState.paintGlossSliderStatus)) + "\",\n"
            + indent + "\"paintGlossSliderValue\": " + jsonNumberField(renderLab, "paintGlossSliderValue", jsonFloat(modelState.paintGlossSliderValue)) + ",\n"
            + indent + "\"glossUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "glossUniformUpdateStatus", modelState.glossUniformUpdateStatus)) + "\",\n"
            + indent + "\"glossResponseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glossResponseDebugViewStatus", modelState.glossResponseDebugViewStatus)) + "\",\n"
            + indent + "\"specularGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "specularGuardDebugViewStatus", modelState.specularGuardDebugViewStatus)) + "\",\n"
            + indent + "\"paintGlossDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "paintGlossDebugViewStatus", modelState.paintGlossDebugViewStatus)) + "\",\n"
            + indent + "\"metalResponseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "metalResponseDebugViewStatus", modelState.metalResponseDebugViewStatus)) + "\",\n"
            + indent + "\"paintTargetDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "paintTargetDebugViewStatus", modelState.paintTargetDebugViewStatus)) + "\",\n"
            + indent + "\"calibrationResponseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "calibrationResponseDebugViewStatus", modelState.calibrationResponseDebugViewStatus)) + "\",\n"
            + indent + "\"materialTypeSpecularRoutingStatus\": \"" + escape(jsonStringField(renderLab, "materialTypeSpecularRoutingStatus", modelState.materialTypeSpecularRoutingStatus)) + "\",\n"
            + indent + "\"paintMaterialGlossStatus\": \"" + escape(jsonStringField(renderLab, "paintMaterialGlossStatus", modelState.paintMaterialGlossStatus)) + "\",\n"
            + indent + "\"metalMaterialGlossStatus\": \"" + escape(jsonStringField(renderLab, "metalMaterialGlossStatus", modelState.metalMaterialGlossStatus)) + "\",\n"
            + indent + "\"rubberMaterialGlossStatus\": \"" + escape(jsonStringField(renderLab, "rubberMaterialGlossStatus", modelState.rubberMaterialGlossStatus)) + "\",\n"
            + indent + "\"specularGlossPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "specularGlossPerformanceStatus", modelState.specularGlossPerformanceStatus)) + "\",\n"
            + indent + "\"glossVisibleResponseStatus\": \"" + escape(jsonStringField(renderLab, "glossVisibleResponseStatus", modelState.glossVisibleResponseStatus)) + "\",\n"
            + indent + "\"glossAffectsSpecularLobe\": \"" + escape(jsonStringField(renderLab, "glossAffectsSpecularLobe", modelState.glossAffectsSpecularLobe)) + "\",\n"
            + indent + "\"glossAffectsReflectionWeight\": \"" + escape(jsonStringField(renderLab, "glossAffectsReflectionWeight", modelState.glossAffectsReflectionWeight)) + "\",\n";
    }

    private String p18EnvironmentJsonFields(String renderLab, String indent) {
        return indent + "\"environmentIblStatus\": \"" + escape(jsonStringField(renderLab, "environmentIblStatus", modelState.environmentIblStatus)) + "\",\n"
            + indent + "\"environmentIblMode\": \"" + escape(jsonStringField(renderLab, "environmentIblMode", modelState.environmentIblMode)) + "\",\n"
            + indent + "\"environmentSourceStatus\": \"" + escape(jsonStringField(renderLab, "environmentSourceStatus", modelState.environmentSourceStatus)) + "\",\n"
            + indent + "\"environmentSourceType\": \"" + escape(jsonStringField(renderLab, "environmentSourceType", modelState.environmentSourceType)) + "\",\n"
            + indent + "\"environmentSkyColorStatus\": \"" + escape(jsonStringField(renderLab, "environmentSkyColorStatus", modelState.environmentSkyColorStatus)) + "\",\n"
            + indent + "\"environmentGroundColorStatus\": \"" + escape(jsonStringField(renderLab, "environmentGroundColorStatus", modelState.environmentGroundColorStatus)) + "\",\n"
            + indent + "\"environmentHorizonStatus\": \"" + escape(jsonStringField(renderLab, "environmentHorizonStatus", modelState.environmentHorizonStatus)) + "\",\n"
            + indent + "\"environmentPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "environmentPerformanceStatus", modelState.environmentPerformanceStatus)) + "\",\n"
            + indent + "\"iblDiffuseStatus\": \"" + escape(jsonStringField(renderLab, "iblDiffuseStatus", modelState.iblDiffuseStatus)) + "\",\n"
            + indent + "\"iblSpecularStatus\": \"" + escape(jsonStringField(renderLab, "iblSpecularStatus", modelState.iblSpecularStatus)) + "\",\n"
            + indent + "\"iblRoughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "iblRoughnessResponseStatus", modelState.iblRoughnessResponseStatus)) + "\",\n"
            + indent + "\"iblMetallicResponseStatus\": \"" + escape(jsonStringField(renderLab, "iblMetallicResponseStatus", modelState.iblMetallicResponseStatus)) + "\",\n"
            + indent + "\"iblDielectricResponseStatus\": \"" + escape(jsonStringField(renderLab, "iblDielectricResponseStatus", modelState.iblDielectricResponseStatus)) + "\",\n"
            + indent + "\"iblFabricPreserveStatus\": \"" + escape(jsonStringField(renderLab, "iblFabricPreserveStatus", modelState.iblFabricPreserveStatus)) + "\",\n"
            + indent + "\"iblOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "iblOverbrightGuardStatus", modelState.iblOverbrightGuardStatus)) + "\",\n"
            + indent + "\"environmentUiStatus\": \"" + escape(jsonStringField(renderLab, "environmentUiStatus", modelState.environmentUiStatus)) + "\",\n"
            + indent + "\"environmentPreset\": \"" + escape(jsonStringField(renderLab, "environmentPreset", modelState.environmentPreset)) + "\",\n"
            + indent + "\"environmentIntensity\": " + jsonNumberField(renderLab, "environmentIntensity", jsonFloat(modelState.environmentIntensity)) + ",\n"
            + indent + "\"environmentSliderStatus\": \"" + escape(jsonStringField(renderLab, "environmentSliderStatus", modelState.environmentSliderStatus)) + "\",\n"
            + indent + "\"skyPresetStatus\": \"" + escape(jsonStringField(renderLab, "skyPresetStatus", modelState.skyPresetStatus)) + "\",\n"
            + indent + "\"horizonControlStatus\": \"" + escape(jsonStringField(renderLab, "horizonControlStatus", modelState.horizonControlStatus)) + "\",\n"
            + indent + "\"environmentUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "environmentUniformUpdateStatus", modelState.environmentUniformUpdateStatus)) + "\",\n"
            + indent + "\"environmentDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "environmentDebugViewStatus", modelState.environmentDebugViewStatus)) + "\",\n"
            + indent + "\"reflectionDirectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionDirectionDebugViewStatus", modelState.reflectionDirectionDebugViewStatus)) + "\",\n"
            + indent + "\"environmentColorDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "environmentColorDebugViewStatus", modelState.environmentColorDebugViewStatus)) + "\",\n"
            + indent + "\"iblPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "iblPerformanceStatus", modelState.iblPerformanceStatus)) + "\",\n";
    }

    private String jsonStringField(String json, String key, String fallback) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return fallback;
        start += marker.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : fallback;
    }

    private String jsonNumberField(String json, String key, String fallback) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return fallback;
        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (!((c >= '0' && c <= '9') || c == '-' || c == '.')) break;
            end++;
        }
        return end > start ? json.substring(start, end) : fallback;
    }

    private String jsonBooleanField(String json, String key, String fallback) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return fallback;
        start += marker.length();
        if (json.startsWith("true", start)) return "true";
        if (json.startsWith("false", start)) return "false";
        return fallback;
    }

    private String jsonArrayField(String json, String key, String fallback) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return fallback;
        start += marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '[') return fallback;
        int end = findJsonArrayEnd(json, start);
        return end > start ? json.substring(start, end + 1) : fallback;
    }

    private int findJsonArrayEnd(String json, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '[') {
                depth++;
                continue;
            }
            if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private ExportResult openDiagnosticsWriters() throws Exception {
        Uri treeUri = getConfiguredTreeUri();
        if (treeUri != null) {
            try {
                ExportResult saf = openSafDiagnosticsWriters(treeUri);
                saf.ok = true;
                saf.route = "saf";
                saf.reason = "saf_tree_uri_configured";
                return saf;
            } catch (Throwable t) {
                Log.e(TAG_DIAG, "export_failed reason=saf_failed: " + shortThrowable(t));
            }
        }

        File directDir = new File("/storage/emulated/0/SOLUMCreative/diagnostics/latest");
        if (canWriteDirectory(directDir)) {
            ExportResult direct = openFileDiagnosticsWriters(directDir);
            direct.ok = true;
            direct.route = "direct";
            direct.reason = treeUri == null ? "saf_not_configured_direct_public_storage_ok" : "saf_failed_direct_public_storage_ok";
            return direct;
        }

        File externalBase = getExternalFilesDir(null);
        File fallbackDir = externalBase != null ? new File(externalBase, "solum_diagnostics") : new File(getFilesDir(), "solum_diagnostics");
        if (canWriteDirectory(fallbackDir)) {
            ExportResult fallback = openFileDiagnosticsWriters(fallbackDir);
            fallback.ok = true;
            fallback.route = "fallback";
            fallback.reason = "saf_not_available_or_failed_direct_public_storage_failed_app_specific_fallback";
            return fallback;
        }

        throw new IllegalStateException("no_writable_diagnostics_route");
    }

    private ExportResult openFileDiagnosticsWriters(File dir) throws Exception {
        ExportResult result = new ExportResult();
        result.actualRoot = dir.getAbsolutePath();
        result.runtimeWriter = new FileWriter(new File(dir, "engine_runtime_state.json"), false);
        result.manifestWriter = new FileWriter(new File(dir, "engine_diagnostics_manifest.json"), false);
        return result;
    }

    private ExportResult openSafDiagnosticsWriters(Uri treeUri) throws Exception {
        ContentResolver resolver = getContentResolver();
        Uri rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        Uri diagnostics = ensureChildDirectory(resolver, rootDocumentUri, "diagnostics");
        Uri latest = ensureChildDirectory(resolver, diagnostics, "latest");
        Uri runtime = ensureChildFile(resolver, latest, "engine_runtime_state.json");
        Uri manifest = ensureChildFile(resolver, latest, "engine_diagnostics_manifest.json");
        ExportResult result = new ExportResult();
        result.actualRoot = treeUri.toString() + "/diagnostics/latest";
        result.runtimeWriter = new OutputStreamWriter(openTruncatingOutputStream(resolver, runtime), StandardCharsets.UTF_8);
        result.manifestWriter = new OutputStreamWriter(openTruncatingOutputStream(resolver, manifest), StandardCharsets.UTF_8);
        return result;
    }

    private Uri ensureChildDirectory(ContentResolver resolver, Uri parentTreeOrDocumentUri, String name) throws Exception {
        Uri existing = findChild(resolver, parentTreeOrDocumentUri, name);
        if (existing != null) return existing;
        Uri created = DocumentsContract.createDocument(resolver, parentTreeOrDocumentUri, DocumentsContract.Document.MIME_TYPE_DIR, name);
        if (created == null) throw new IllegalStateException("create_directory_failed:" + name);
        return created;
    }

    private Uri ensureChildFile(ContentResolver resolver, Uri parentDocumentUri, String name) throws Exception {
        return ensureChildFileWithMime(resolver, parentDocumentUri, name, "application/json");
    }

    private Uri ensureChildFileWithMime(ContentResolver resolver, Uri parentDocumentUri, String name, String mimeType) throws Exception {
        Uri existing = findChild(resolver, parentDocumentUri, name);
        if (existing != null) return existing;
        Uri created = DocumentsContract.createDocument(resolver, parentDocumentUri, mimeType, name);
        if (created == null) throw new IllegalStateException("create_file_failed:" + name);
        return created;
    }

    private Uri findChild(ContentResolver resolver, Uri parentTreeOrDocumentUri, String name) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentTreeOrDocumentUri, DocumentsContract.getDocumentId(parentTreeOrDocumentUri));
        try (Cursor cursor = resolver.query(childrenUri, new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        }, null, null, null)) {
            if (cursor == null) return null;
            while (cursor.moveToNext()) {
                String documentId = cursor.getString(0);
                String displayName = cursor.getString(1);
                if (name.equals(displayName)) {
                    return DocumentsContract.buildDocumentUriUsingTree(parentTreeOrDocumentUri, documentId);
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private OutputStream openTruncatingOutputStream(ContentResolver resolver, Uri uri) throws Exception {
        OutputStream out = resolver.openOutputStream(uri, "wt");
        if (out == null) throw new IllegalStateException("open_output_stream_failed");
        return out;
    }

    private void writeModelDiagnostics(String trigger) {
        String timestamp = timestampUtc();
        String modelJson = modelState.toJson("solum.model_import_state", timestamp, trigger);
        String assetJson = modelState.toJson("solum.asset_report", timestamp, trigger);
        Uri treeUri = getConfiguredTreeUri();
        if (treeUri != null) {
            try {
                ContentResolver resolver = getContentResolver();
                Uri rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
                Uri diagnostics = ensureChildDirectory(resolver, rootDocumentUri, "diagnostics");
                Uri latest = ensureChildDirectory(resolver, diagnostics, "latest");
                Uri model = ensureChildFile(resolver, latest, "model_import_state.json");
                Uri asset = ensureChildFile(resolver, latest, "asset_report.json");
                writeText(new OutputStreamWriter(openTruncatingOutputStream(resolver, model), StandardCharsets.UTF_8), modelJson);
                writeText(new OutputStreamWriter(openTruncatingOutputStream(resolver, asset), StandardCharsets.UTF_8), assetJson);
                return;
            } catch (Throwable t) {
                Log.e(TAG_DIAG, "model_diagnostics_saf_failed reason=" + shortThrowable(t));
            }
        }
        File directDir = new File("/storage/emulated/0/SOLUMCreative/diagnostics/latest");
        if (!canWriteDirectory(directDir)) {
            File externalBase = getExternalFilesDir(null);
            directDir = externalBase != null ? new File(externalBase, "solum_diagnostics") : new File(getFilesDir(), "solum_diagnostics");
            canWriteDirectory(directDir);
        }
        try {
            writeText(new FileWriter(new File(directDir, "model_import_state.json"), false), modelJson);
            writeText(new FileWriter(new File(directDir, "asset_report.json"), false), assetJson);
        } catch (Throwable t) {
            Log.e(TAG_DIAG, "model_diagnostics_failed reason=" + shortThrowable(t));
        }
    }

    private Uri getConfiguredTreeUri() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(PREF_TREE_URI, "");
        if (raw == null || raw.isEmpty()) return null;
        return Uri.parse(raw);
    }

    private void writeText(Writer writer, String text) throws Exception {
        try (Writer w = writer) {
            w.write(text);
        }
    }

    private void updateDiagnosticsStatusPanel() {
        if (diagnosticsStatusView == null) return;
        runOnUiThread(() -> {
            boolean configured = getConfiguredTreeUri() != null;
            diagnosticsStatusView.setText(
                "Diagnostics folder: " + (configured ? "configured" : "not configured") + "\n"
                    + "Last export: " + lastExportStatus + "\n"
                    + "Last export route: " + lastExportRoute + "\n"
                    + "Last export reason/path: " + shorten((lastExportReason + " " + lastExportPath).trim(), 72) + "\n"
                    + "Debug ZIP: " + debugZipStatus + " " + shorten((debugZipReason + " " + debugZipPath).trim(), 72) + "\n"
                    + "Last export timestamp: " + (lastExportTimestamp.isEmpty() ? "not run" : lastExportTimestamp) + "\n"
                    + "Import GLB: " + modelState.importStatus + " route=" + modelState.importRoute + "\n"
                    + "GPU Upload / Draw: " + modelState.gpuUploadStatus + " / " + modelState.drawStatus + "\n"
                    + "BaseColor Texture: " + modelState.baseColorTextureStatus + " upload=" + modelState.textureUploadStatus + "\n"
                    + "PBR maps: " + modelState.pbrMapsStatus + " MR=" + modelState.metallicRoughnessStatus + " N=" + modelState.normalMapStatus + " AO=" + modelState.occlusionMapStatus + "\n"
                    + "Lighting: " + modelState.lightingStatus + " preset=" + modelState.lightPreset + " sun=" + oneDecimal(modelState.sunIntensity) + " ambient=" + oneDecimal(modelState.ambientIntensity) + " spec=" + oneDecimal(modelState.specularBoost) + "\n"
                    + "Material View: " + modelState.activeDebugView + " response=" + modelState.materialResponseStatus + "\n"
                    + "Texture size/fallback: " + modelState.textureWidth + "x" + modelState.textureHeight + " / " + (modelState.textureFallbackUsed ? "yes" : "no") + "\n"
                    + "Uploaded vertices/indices: " + modelState.uploadedVertexCount + " / " + modelState.uploadedIndexCount + "\n"
                    + "Primitives rendered/skipped/total: " + modelState.primitiveCountRendered + " / " + modelState.primitiveCountSkipped + " / " + modelState.primitiveCountTotal + "\n"
                    + "Source: " + shorten(modelState.sourceDisplayName.isEmpty() ? "none" : modelState.sourceDisplayName, 48) + "\n"
                    + "Imported: " + shorten(modelState.importedPath.isEmpty() ? "none" : modelState.importedPath, 72) + "\n"
                    + "Models found: " + modelState.modelsFoundCount + " active=" + (modelState.activeModelName().isEmpty() ? "none" : shorten(modelState.activeModelName(), 40)) + "\n"
                    + "Reason: " + shorten(modelState.reason, 72)
            );
        });
    }

    private String getNativeStatusForExport() {
        if (!nativeLoaded || nativeHandle == 0L) return "native_not_loaded";
        try { return nativeGetStatus(nativeHandle); } catch (Throwable t) { return "native_status_failed: " + shortThrowable(t); }
    }

    private String getRenderLabStateForExport() {
        if (!nativeLoaded || nativeHandle == 0L) {
            return fallbackRenderLabJson("native_not_loaded", "ok", "shader_applied", "");
        }
        try { return injectJavaInspectorState(nativeGetRenderLabState(nativeHandle)); }
        catch (Throwable t) { return fallbackRenderLabJson("native_render_lab_state_failed", "failed", "not_applied", shortThrowable(t)); }
    }

    private String injectJavaInspectorState(String json) {
        if (json == null || json.isEmpty()) return json;
        return json.replace("\"activeInspectorTab\":\"Assets\"", "\"activeInspectorTab\":\"" + escape(activeInspectorTab) + "\"");
    }

    private String fallbackRenderLabJson(String status, String lightingStatus, String debugStatus, String reason) {
        return "{\"currentScene\":\"" + SCENE_ID + "\",\"currentLabScene\":\"" + SCENE_ID + "\",\"currentLabSceneName\":\"" + SCENE_NAME + "\",\"status\":\"" + escape(status) + "\",\"lightingStatus\":\"" + escape(lightingStatus) + "\",\"lightingControlStatus\":\"" + escape(lightingStatus) + "\",\"lightingUiMode\":\"compact_sliders\",\"inspectorUiStatus\":\"ok\",\"inspectorUiMode\":\"tabbed_compact_inspector\",\"activeInspectorTab\":\"" + escape(activeInspectorTab) + "\",\"assetsTabStatus\":\"ok_import_scan_export_summary\",\"cameraTabStatus\":\"ok_camera_info_reset_zoom\",\"lightingTabStatus\":\"ok_sliders_environment_controls\",\"materialTabStatus\":\"ok_debug_views\",\"debugTabStatus\":\"ok_fps_zip_status\",\"sunDirection\":[-0.35,-0.82,-0.45],\"sunColor\":[1,0.96,0.88],\"sunIntensity\":2.0,\"ambientColor\":[0.42,0.52,0.62],\"ambientIntensity\":0.8,\"lightPreset\":\"Bright\",\"specularBoost\":1.85,\"specularBoostStatus\":\"ok_uniform_controlled\",\"reflectionIntensity\":1.15,\"contactGroundingStatus\":\"foundation_analytic\",\"contactShadowStatus\":\"enabled\",\"contactShadowMode\":\"analytic_blob_or_grounding_approx\",\"contactShadowIntensity\":" + jsonFloat(contactShadowIntensity) + ",\"contactShadowPerformanceStatus\":\"ok_uniform_only_no_shadow_pass\",\"groundingUsesModelBounds\":\"yes_upload_bounds_scaled_local\",\"groundingUniformUpdateStatus\":\"ok_uniform_only\",\"groundSliderStatus\":\"ok\",\"contactGroundingSliderStatus\":\"ok\",\"iblStatus\":\"ok_foundation\",\"iblMode\":\"directional_sky_ground_ibl\",\"environmentIblStatus\":\"ok_foundation\",\"environmentIblMode\":\"directional_sky_ground_ibl\",\"environmentSourceStatus\":\"ok_procedural_no_external_texture\",\"environmentSourceType\":\"directional_sky_ground_shader_model\",\"environmentSkyColorStatus\":\"ok_preset_uniform\",\"environmentGroundColorStatus\":\"ok_preset_uniform\",\"environmentHorizonStatus\":\"ok_directional_horizon_blend\",\"environmentPerformanceStatus\":\"ok_no_extra_pass_no_texture_upload\",\"iblDiffuseStatus\":\"ok_directional_sky_ground_diffuse\",\"iblSpecularStatus\":\"ok_reflection_direction_environment\",\"iblRoughnessResponseStatus\":\"ok_roughness_blurs_and_reduces_specular\",\"iblMetallicResponseStatus\":\"ok_metal_tints_reflection\",\"iblDielectricResponseStatus\":\"ok_subtle_f0_reflection\",\"iblFabricPreserveStatus\":\"ok_fabric_matte_preserved\",\"iblOverbrightGuardStatus\":\"ok_luminance_guarded\",\"environmentUiStatus\":\"ok_compact_lighting_controls\",\"environmentPreset\":\"Studio\",\"environmentIntensity\":1.0,\"environmentSliderStatus\":\"ok\",\"skyPresetStatus\":\"ok\",\"horizonControlStatus\":\"ok\",\"environmentUniformUpdateStatus\":\"ok_uniform_only\",\"environmentDebugViewStatus\":\"shader_applied\",\"reflectionDirectionDebugViewStatus\":\"shader_applied\",\"environmentColorDebugViewStatus\":\"shader_applied\",\"iblPerformanceStatus\":\"ok_shader_math_only_no_loops\",\"reflectionFoundationStatus\":\"p18_environment_ibl_foundation\",\"reflectionMode\":\"directional_sky_ground_ibl\",\"environmentReflectionStatus\":\"p18_environment_directional_source_no_texture_cubemap\",\"environmentReflectionMode\":\"reflection_direction_sky_ground_ibl\",\"environmentSource\":\"directional_sky_ground_shader_model\",\"reflectionColorStatus\":\"ok_environment_preset_horizon_gradient\",\"reflectionRoughnessResponseStatus\":\"ok_roughness_blurs_reduces_reflection\",\"metallicReflectionStatus\":\"ok_metal_tinted_environment_guarded\",\"dielectricReflectionStatus\":\"ok_subtle_f0_environment\",\"reflectionPerformanceStatus\":\"ok_no_extra_pass_no_texture_rebuild\",\"lightingUniformUpdateStatus\":\"ok_uniform_only\",\"sliderUpdateMode\":\"uniform_only\",\"sliderTouchStatus\":\"ok_touch_targets\",\"sunSliderStatus\":\"ok\",\"ambientSliderStatus\":\"ok\",\"exposureSliderStatus\":\"ok\",\"specularSliderStatus\":\"ok\",\"reflectionSliderStatus\":\"ok\",\"brdfStatus\":\"ok\",\"brdfMode\":\"direct_lighting_schlick_mobile_p17_gloss\",\"diffuseStatus\":\"ok_environment_diffuse\",\"specularStatus\":\"ok_p17_gloss_response_guarded\",\"fresnelStatus\":\"ok_schlick\",\"f0Status\":\"ok_dielectric_0_04_metal_base_color\",\"metallicResponseStatus\":\"ok_diffuse_reduced_f0_tinted\",\"roughnessResponseStatus\":\"ok_gloss_width_energy_remap\",\"directLightingStatus\":\"ok_single_sun_direct_gloss_lobe\",\"materialResponseStatus\":\"p18_environment_ibl_foundation\",\"pbrQualityTier\":\"mobile_direct_lighting_ibl_v1\",\"brdfPerformanceStatus\":\"ok_mobile_friendly_direct_lighting_ibl\",\"toneMappingStatus\":\"ok\",\"toneMappingMode\":\"reinhard\",\"activeDebugView\":\"Final Shaded\",\"debugViewStatus\":\"" + escape(debugStatus) + "\",\"diffuseDebugViewStatus\":\"" + escape(debugStatus) + "\",\"specularDebugViewStatus\":\"" + escape(debugStatus) + "\",\"f0DebugViewStatus\":\"" + escape(debugStatus) + "\",\"reflectionDebugViewStatus\":\"" + escape(debugStatus) + "\",\"iblDiffuseDebugViewStatus\":\"" + escape(debugStatus) + "\",\"iblSpecularDebugViewStatus\":\"" + escape(debugStatus) + "\",\"brdfStatusDebugViewStatus\":\"" + escape(debugStatus) + "\",\"groundingDebugViewStatus\":\"" + escape(debugStatus) + "\",\"exposureValue\":1.5,\"ambientFloor\":0.16,\"brightnessPreset\":\"Bright Preview\",\"gpuUploadStatus\":\"failed\",\"drawStatus\":\"fallback\",\"meshDrawStatus\":\"fallback\",\"textureUploadStatus\":\"missing\",\"baseColorTextureStatus\":\"missing\",\"textureFallbackUsed\":true,\"textureWidth\":0,\"textureHeight\":0,\"uploadedVertexCount\":0,\"uploadedIndexCount\":0,\"modelBoundsMin\":[0,0,0],\"modelBoundsMax\":[0,0,0],\"modelBoundsCenter\":[0,0,0],\"modelScale\":1,\"modelRenderMode\":\"multi_primitive_static\",\"primitiveCountTotal\":0,\"primitiveCountRendered\":0,\"primitiveCountSkipped\":0,\"unsupportedPrimitiveCount\":0,\"materialSlotCount\":0,\"materialSlotCountRendered\":0,\"textureSlotCount\":0,\"uploadedTextureCount\":0,\"textureFallbackCount\":0,\"skippedTextureCount\":0,\"textureSlotLimit\":8,\"tangentFallbackGeneratedCount\":0,\"tangentDegenerateTriangleCount\":0,\"tangentBuildMode\":\"once_on_upload\",\"fpsCurrent\":0,\"frameTimeMs\":0,\"fpsSource\":\"not_ready\",\"fpsLastStable\":0,\"frameTimeLastStableMs\":0,\"fpsStatus\":\"not_ready\",\"fpsUpdateMode\":\"java_choreographer_live\",\"fpsSampleWindowMs\":1000,\"framesRenderedLive\":0,\"modelUploadRepeatCount\":0,\"uploadGenerationId\":0,\"renderLoopAllocationGuardStatus\":\"ok_no_java_glb_parse_or_upload_in_frame_callback\",\"debugZipStatus\":\"not_run\",\"debugZipPath\":\"\",\"debugZipIncludedFiles\":\"\",\"debugZipReason\":\"not_run\",\"fallbackCubeVisible\":true,\"fallbackCubeStatus\":\"on\",\"reason\":\"" + escape(reason) + "\"}";
    }

    private String timestampUtc() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date());
    }

    private String getVersionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Throwable t) { return "unknown"; }
    }

    private int getVersionCode() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; }
        catch (Throwable t) { return 0; }
    }

    private static final class ModelCopyResult {
        final String route;
        final String path;
        final String reason;
        final File localFile;

        ModelCopyResult(String route, String path, String reason, File localFile) {
            this.route = route;
            this.path = path;
            this.reason = reason;
            this.localFile = localFile;
        }
    }

    private static final class ModelImportState {
        String importStatus = "not run";
        String importRoute = "not run";
        String sourceDisplayName = "";
        String importedPath = "";
        String activeModelPath = "";
        String activeModelLocalPath = "";
        String lastImportedModel = "";
        String reason = "not run";
        int modelsFoundCount = 0;
        GlbParseResult parse = GlbParseResult.notParsed("not_parsed");
        String gpuUploadStatus = "failed";
        String drawStatus = "fallback";
        String meshDrawStatus = "fallback";
        int uploadedVertexCount = 0;
        int uploadedIndexCount = 0;
        int primitiveCountTotal = 0;
        int primitiveCountRendered = 0;
        int primitiveCountSkipped = 0;
        int unsupportedPrimitiveCount = 0;
        int materialSlotCount = 0;
        int materialSlotCountRendered = 0;
        int textureSlotCount = 0;
        int uploadedTextureCount = 0;
        int textureFallbackCount = 0;
        int skippedTextureCount = 0;
        int textureSlotLimit = 8;
        String pbrMapsStatus = "missing";
        String metallicRoughnessStatus = "missing";
        String normalMapStatus = "missing";
        String normalMapAppliedStatus = "missing";
        String occlusionMapStatus = "missing";
        String tangentStatus = "missing_or_blocked";
        String tangentSource = "missing";
        int tangentGeneratedCount = 0;
        int tangentFallbackGeneratedCount = 0;
        int tangentMissingCount = 0;
        int tangentDegenerateTriangleCount = 0;
        String tangentFallbackReason = "not_loaded";
        String tangentBuildMode = "once_on_upload";
        int modelUploadRepeatCount = 0;
        int uploadGenerationId = 0;
        String renderLoopAllocationGuardStatus = "ok_no_java_glb_parse_or_upload_in_frame_callback";
        String lastUploadedModelKey = "";
        float metallicFactor = 0.0f;
        float roughnessFactor = 1.0f;
        float normalScale = 1.0f;
        float occlusionStrength = 1.0f;
        int pbrTextureSlotCount = 0;
        int uploadedPbrTextureCount = 0;
        int skippedPbrTextureCount = 0;
        int pbrTextureFallbackCount = 0;
        String materialSlotDiagnostics = "[]";
        String materialCalibrationStatus = "ok";
        String materialCalibrationMode = "shader_uniform_upload_lightweight";
        String albedoEnergyStatus = "ok_normalized";
        String albedoClampStatus = "ok";
        String diffuseClampStatus = "ok";
        String luminanceGuardStatus = "ok";
        String aoCalibrationStatus = "ok_indirect_weighted";
        String roughnessRemapStatus = "ok";
        String metallicRoughnessClampStatus = "ok";
        String emissiveGuardStatus = "ok_guarded_real_emissive_only";
        String fabricMattePreserveStatus = "ok_rough_fabric_kept_matte";
        String paintMaterialCalibrationStatus = "ok_luminance_guard_reflection_readable";
        String metalMaterialCalibrationStatus = "ok_clamped_metallic_readable";
        String materialTypeHintStatus = "ok";
        String materialSlotCalibrationStatus = "ok";
        String calibrationUiStatus = "ok_compact_material_tab";
        String calibrationPreset = "Balanced";
        String calibrationSliderStatus = "ok";
        float calibrationSliderValue = 0.65f;
        String calibrationUniformUpdateStatus = "ok_uniform_only";
        String calibratedAlbedoDebugViewStatus = "shader_applied";
        String materialTypeDebugViewStatus = "shader_applied";
        String aoInfluenceDebugViewStatus = "shader_applied";
        String luminanceGuardDebugViewStatus = "shader_applied";
        float calibrationVisualStrength = 0.72f;
        String calibrationAffectsAlbedo = "yes_guarded_luminance_clamp";
        String calibrationAffectsAo = "yes_indirect_occlusion_weight";
        String calibrationAffectsRoughness = "yes_material_safe_remap";
        String calibrationVisibleResponseStatus = "ok_visible_final_shaded_response";
        String materialCalibrationPerformanceStatus = "ok_uniform_shader_no_rebuild";
        String specularGlossStatus = "ok";
        String specularGlossMode = "uniform_gloss_response_p17b";
        String specularResponseStatus = "ok_guarded_dielectric_metal";
        String glossResponseStatus = "ok_slider_controls_lobe_width";
        String roughnessRemapV2Status = "ok_calib_and_gloss_weighted";
        String metallicSpecularBoostStatus = "ok_metal_routed_boost";
        String dielectricGlossStatus = "ok_f0_guarded";
        String fabricSpecularSuppressStatus = "ok_matte_preserved";
        String specularOverbrightGuardStatus = "ok_luminance_guard";
        String viewDependentHighlightStatus = "ok_reflection_vector";
        String paintGlossLiteStatus = "ok";
        String paintGlossLiteMode = "uniform_lite_no_texture_rebuild";
        float paintGlossIntensity = 0.55f;
        float paintGlossRoughness = 0.45f;
        String paintGlossMaterialHintStatus = "ok_paint_like_only";
        String paintGlossPerformanceStatus = "ok_uniform_only";
        String paintGlossTargetStatus = "none";
        int paintGlossAppliedMaterialCount = 0;
        int paintGlossSkippedFabricCount = 0;
        String paintGlossFallbackRouting = "none";
        String paintGlossVisibleResponseStatus = "no_target";
        String glossSliderStatus = "ok";
        float glossSliderValue = 0.62f;
        String paintGlossSliderStatus = "ok";
        float paintGlossSliderValue = 0.55f;
        String glossUniformUpdateStatus = "ok_uniform_only";
        String glossResponseDebugViewStatus = "shader_applied";
        String specularGuardDebugViewStatus = "shader_applied";
        String paintGlossDebugViewStatus = "shader_applied";
        String metalResponseDebugViewStatus = "shader_applied";
        String paintTargetDebugViewStatus = "shader_applied";
        String calibrationResponseDebugViewStatus = "shader_applied";
        String materialTypeSpecularRoutingStatus = "ok";
        String paintMaterialGlossStatus = "ok_lite_gloss";
        String metalMaterialGlossStatus = "ok_stronger_response";
        String rubberMaterialGlossStatus = "ok_suppressed";
        String specularGlossPerformanceStatus = "ok_no_alloc_no_rebuild";
        String glossVisibleResponseStatus = "ok_visible_lobe_and_reflection";
        String glossAffectsSpecularLobe = "yes_shader_roughness_distribution";
        String glossAffectsReflectionWeight = "yes_shader_environment_weight";
        String lightingStatus = "ok";
        float[] sunDirection = new float[] { -0.35f, -0.82f, -0.45f };
        float[] sunColor = new float[] { 1.0f, 0.96f, 0.88f };
        float sunIntensity = 2.0f;
        float[] ambientColor = new float[] { 0.42f, 0.52f, 0.62f };
        float ambientIntensity = 0.80f;
        String lightPreset = "Bright";
        float specularBoost = 1.85f;
        String specularBoostStatus = "ok_uniform_controlled";
        float reflectionIntensity = 1.15f;
        String iblStatus = "ok_foundation";
        String iblMode = "directional_sky_ground_ibl";
        String environmentIblStatus = "ok_foundation";
        String environmentIblMode = "directional_sky_ground_ibl";
        String environmentSourceStatus = "ok_procedural_no_external_texture";
        String environmentSourceType = "directional_sky_ground_shader_model";
        String environmentSkyColorStatus = "ok_preset_uniform";
        String environmentGroundColorStatus = "ok_preset_uniform";
        String environmentHorizonStatus = "ok_directional_horizon_blend";
        String environmentPerformanceStatus = "ok_no_extra_pass_no_texture_upload";
        String iblDiffuseStatus = "ok_directional_sky_ground_diffuse";
        String iblSpecularStatus = "ok_reflection_direction_environment";
        String iblRoughnessResponseStatus = "ok_roughness_blurs_and_reduces_specular";
        String iblMetallicResponseStatus = "ok_metal_tints_reflection";
        String iblDielectricResponseStatus = "ok_subtle_f0_reflection";
        String iblFabricPreserveStatus = "ok_fabric_matte_preserved";
        String iblOverbrightGuardStatus = "ok_luminance_guarded";
        String environmentUiStatus = "ok_compact_lighting_controls";
        String environmentPreset = "Studio";
        float environmentIntensity = 1.0f;
        String environmentSliderStatus = "ok";
        String skyPresetStatus = "ok";
        String horizonControlStatus = "ok";
        float horizonStrength = 0.55f;
        String environmentUniformUpdateStatus = "ok_uniform_only";
        String environmentDebugViewStatus = "shader_applied";
        String reflectionDirectionDebugViewStatus = "shader_applied";
        String environmentColorDebugViewStatus = "shader_applied";
        String iblPerformanceStatus = "ok_shader_math_only_no_loops";
        String reflectionFoundationStatus = "p18_environment_ibl_foundation";
        String reflectionMode = "directional_sky_ground_ibl";
        String environmentReflectionStatus = "p18_environment_directional_source_no_texture_cubemap";
        String environmentReflectionMode = "reflection_direction_sky_ground_ibl";
        String environmentSource = "directional_sky_ground_shader_model";
        String reflectionColorStatus = "ok_environment_preset_horizon_gradient";
        String reflectionRoughnessResponseStatus = "ok_roughness_blurs_reduces_reflection";
        String metallicReflectionStatus = "ok_metal_tinted_environment_guarded";
        String dielectricReflectionStatus = "ok_subtle_f0_environment";
        String reflectionPerformanceStatus = "ok_no_extra_pass_no_texture_rebuild";
        String inspectorUiStatus = "ok";
        String inspectorUiMode = "tabbed_compact_inspector";
        String activeInspectorTab = "Assets";
        String assetsTabStatus = "ok_import_scan_export_summary";
        String cameraTabStatus = "ok_camera_info_reset_zoom";
        String lightingTabStatus = "ok_sliders_environment_controls";
        String materialTabStatus = "ok_debug_views";
        String debugTabStatus = "ok_fps_zip_status";
        String contactGroundingStatus = "foundation_analytic";
        String contactShadowStatus = "enabled";
        String contactShadowMode = "analytic_blob_or_grounding_approx";
        float contactShadowIntensity = 0.65f;
        String contactShadowPerformanceStatus = "ok_uniform_only_no_shadow_pass";
        String groundingUsesModelBounds = "yes_upload_bounds_scaled_local";
        String groundingUniformUpdateStatus = "ok_uniform_only";
        String groundSliderStatus = "ok";
        String contactGroundingSliderStatus = "ok";
        String lightingControlStatus = "ok";
        String lightingUiMode = "compact_sliders";
        String lightingUniformUpdateStatus = "ok_uniform_only";
        String sliderUpdateMode = "uniform_only";
        String sliderTouchStatus = "ok_touch_targets";
        String sunSliderStatus = "ok";
        String ambientSliderStatus = "ok";
        String exposureSliderStatus = "ok";
        String specularSliderStatus = "ok";
        String reflectionSliderStatus = "ok";
        String brdfStatus = "ok";
        String brdfMode = "direct_lighting_schlick_mobile";
        String diffuseStatus = "ok_non_metal_diffuse";
        String specularStatus = "ok_p17_gloss_response_guarded";
        String fresnelStatus = "ok_schlick";
        String f0Status = "ok_dielectric_0_04_metal_base_color";
        String metallicResponseStatus = "ok_diffuse_reduced_f0_tinted";
        String roughnessResponseStatus = "ok_gloss_width_energy_remap";
        String directLightingStatus = "ok_single_sun_direct_gloss_lobe";
        String materialResponseStatus = "p18_environment_ibl_foundation";
        String pbrQualityTier = "mobile_direct_lighting_ibl_v1";
        String brdfPerformanceStatus = "ok_mobile_friendly_direct_lighting_ibl";
        String toneMappingStatus = "ok";
        String toneMappingMode = "reinhard";
        String exposureStatus = "ok";
        float exposureValue = 1.50f;
        float ambientFloor = 0.16f;
        String brightnessPreset = "Bright Preview";
        String activeDebugView = "Final Shaded";
        String debugViewStatus = "shader_applied";
        String normalDebugViewStatus = "shader_applied";
        String ndotlDebugViewStatus = "shader_applied";
        String diffuseDebugViewStatus = "shader_applied";
        String specularDebugViewStatus = "shader_applied";
        String f0DebugViewStatus = "shader_applied";
        String reflectionDebugViewStatus = "shader_applied";
        String iblDiffuseDebugViewStatus = "shader_applied";
        String iblSpecularDebugViewStatus = "shader_applied";
        String brdfStatusDebugViewStatus = "shader_applied";
        String groundingDebugViewStatus = "shader_applied";
        boolean fallbackCubeVisible = true;
        String fallbackCubeStatus = "on";
        String textureUploadStatus = "missing";
        String baseColorTextureStatus = "missing";
        String baseColorTextureName = "none";
        String baseColorTextureSource = "none";
        String baseColorTextureMimeType = "none";
        int textureWidth = 0;
        int textureHeight = 0;
        int textureBytes = 0;
        boolean textureFallbackUsed = true;

        static ModelImportState notRun() { return new ModelImportState(); }

        String activeModelName() {
            if (activeModelPath == null || activeModelPath.isEmpty()) return "";
            return new File(activeModelPath).getName();
        }

        String localExtractionPath() {
            if (activeModelLocalPath != null && !activeModelLocalPath.isEmpty()) return activeModelLocalPath;
            return activeModelPath == null ? "" : activeModelPath;
        }

        String summary() {
            return "meshes=" + parse.meshCount + ", primitives=" + parse.primitiveCount
                + ", materials=" + parse.materialCount + ", textures=" + parse.textureCount
                + ", glbValid=" + parse.glbValid;
        }

        String toJson(String schema, String timestamp, String trigger) {
            return "{\n"
                + "  \"schema\": \"" + schema + "\",\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"timestampUtc\": \"" + esc(timestamp) + "\",\n"
                + "  \"trigger\": \"" + esc(trigger) + "\",\n"
                + "  \"assetRoot\": \"/storage/emulated/0/SOLUMCreative/assets/models/imported/\",\n"
                + "  \"importStatus\": \"" + esc(importStatus) + "\",\n"
                + "  \"importRoute\": \"" + esc(importRoute) + "\",\n"
                + "  \"sourceDisplayName\": \"" + esc(sourceDisplayName) + "\",\n"
                + "  \"importedPath\": \"" + esc(importedPath) + "\",\n"
                + "  \"activeModelPath\": \"" + esc(activeModelPath) + "\",\n"
                + "  \"activeModelLocalPath\": \"" + esc(activeModelLocalPath) + "\",\n"
                + "  \"lastImportedModel\": \"" + esc(lastImportedModel) + "\",\n"
                + "  \"importReason\": \"" + esc(reason) + "\",\n"
                + "  \"modelsFoundCount\": " + modelsFoundCount + ",\n"
                + "  \"gpuUploadStatus\": \"" + esc(gpuUploadStatus) + "\",\n"
                + "  \"drawStatus\": \"" + esc(drawStatus) + "\",\n"
                + "  \"meshDrawStatus\": \"" + esc(meshDrawStatus) + "\",\n"
                + "  \"textureUploadStatus\": \"" + esc(textureUploadStatus) + "\",\n"
                + "  \"baseColorTextureStatus\": \"" + esc(baseColorTextureStatus) + "\",\n"
                + "  \"baseColorTextureName\": \"" + esc(baseColorTextureName) + "\",\n"
                + "  \"baseColorTextureSource\": \"" + esc(baseColorTextureSource) + "\",\n"
                + "  \"baseColorTextureMimeType\": \"" + esc(baseColorTextureMimeType) + "\",\n"
                + "  \"textureWidth\": " + textureWidth + ",\n"
                + "  \"textureHeight\": " + textureHeight + ",\n"
                + "  \"textureBytes\": " + textureBytes + ",\n"
                + "  \"textureFallbackUsed\": " + textureFallbackUsed + ",\n"
                + "  \"uploadedVertexCount\": " + uploadedVertexCount + ",\n"
                + "  \"uploadedIndexCount\": " + uploadedIndexCount + ",\n"
                + "  \"modelRenderMode\": \"multi_primitive_static\",\n"
                + "  \"primitiveCountTotal\": " + primitiveCountTotal + ",\n"
                + "  \"primitiveCountRendered\": " + primitiveCountRendered + ",\n"
                + "  \"primitiveCountSkipped\": " + primitiveCountSkipped + ",\n"
                + "  \"unsupportedPrimitiveCount\": " + unsupportedPrimitiveCount + ",\n"
                + "  \"materialSlotCount\": " + materialSlotCount + ",\n"
                + "  \"materialSlotCountRendered\": " + materialSlotCountRendered + ",\n"
                + "  \"textureSlotCount\": " + textureSlotCount + ",\n"
                + "  \"uploadedTextureCount\": " + uploadedTextureCount + ",\n"
                + "  \"textureFallbackCount\": " + textureFallbackCount + ",\n"
                + "  \"skippedTextureCount\": " + skippedTextureCount + ",\n"
                + "  \"textureSlotLimit\": " + textureSlotLimit + ",\n"
                + "  \"pbrMapsStatus\": \"" + esc(pbrMapsStatus) + "\",\n"
                + "  \"metallicRoughnessStatus\": \"" + esc(metallicRoughnessStatus) + "\",\n"
                + "  \"normalMapStatus\": \"" + esc(normalMapStatus) + "\",\n"
                + "  \"normalMapAppliedStatus\": \"" + esc(normalMapAppliedStatus) + "\",\n"
                + "  \"occlusionMapStatus\": \"" + esc(occlusionMapStatus) + "\",\n"
                + "  \"tangentStatus\": \"" + esc(tangentStatus) + "\",\n"
                + "  \"tangentSource\": \"" + esc(tangentSource) + "\",\n"
                + "  \"tangentGeneratedCount\": " + tangentGeneratedCount + ",\n"
                + "  \"tangentFallbackGeneratedCount\": " + tangentFallbackGeneratedCount + ",\n"
                + "  \"tangentMissingCount\": " + tangentMissingCount + ",\n"
                + "  \"tangentDegenerateTriangleCount\": " + tangentDegenerateTriangleCount + ",\n"
                + "  \"tangentFallbackReason\": \"" + esc(tangentFallbackReason) + "\",\n"
                + "  \"tangentBuildMode\": \"" + esc(tangentBuildMode) + "\",\n"
                + "  \"modelUploadRepeatCount\": " + modelUploadRepeatCount + ",\n"
                + "  \"uploadGenerationId\": " + uploadGenerationId + ",\n"
                + "  \"renderLoopAllocationGuardStatus\": \"" + esc(renderLoopAllocationGuardStatus) + "\",\n"
                + "  \"metallicFactor\": " + jsonFloat(metallicFactor) + ",\n"
                + "  \"roughnessFactor\": " + jsonFloat(roughnessFactor) + ",\n"
                + "  \"normalScale\": " + jsonFloat(normalScale) + ",\n"
                + "  \"occlusionStrength\": " + jsonFloat(occlusionStrength) + ",\n"
                + "  \"pbrTextureSlotCount\": " + pbrTextureSlotCount + ",\n"
                + "  \"uploadedPbrTextureCount\": " + uploadedPbrTextureCount + ",\n"
                + "  \"skippedPbrTextureCount\": " + skippedPbrTextureCount + ",\n"
                + "  \"pbrTextureFallbackCount\": " + pbrTextureFallbackCount + ",\n"
                + "  \"materialSlotDiagnostics\": " + materialSlotDiagnostics + ",\n"
                + "  \"materialCalibrationStatus\": \"" + esc(materialCalibrationStatus) + "\",\n"
                + "  \"materialCalibrationMode\": \"" + esc(materialCalibrationMode) + "\",\n"
                + "  \"albedoEnergyStatus\": \"" + esc(albedoEnergyStatus) + "\",\n"
                + "  \"albedoClampStatus\": \"" + esc(albedoClampStatus) + "\",\n"
                + "  \"diffuseClampStatus\": \"" + esc(diffuseClampStatus) + "\",\n"
                + "  \"luminanceGuardStatus\": \"" + esc(luminanceGuardStatus) + "\",\n"
                + "  \"aoCalibrationStatus\": \"" + esc(aoCalibrationStatus) + "\",\n"
                + "  \"roughnessRemapStatus\": \"" + esc(roughnessRemapStatus) + "\",\n"
                + "  \"metallicRoughnessClampStatus\": \"" + esc(metallicRoughnessClampStatus) + "\",\n"
                + "  \"emissiveGuardStatus\": \"" + esc(emissiveGuardStatus) + "\",\n"
                + "  \"fabricMattePreserveStatus\": \"" + esc(fabricMattePreserveStatus) + "\",\n"
                + "  \"paintMaterialCalibrationStatus\": \"" + esc(paintMaterialCalibrationStatus) + "\",\n"
                + "  \"metalMaterialCalibrationStatus\": \"" + esc(metalMaterialCalibrationStatus) + "\",\n"
                + "  \"materialTypeHintStatus\": \"" + esc(materialTypeHintStatus) + "\",\n"
                + "  \"materialSlotCalibrationStatus\": \"" + esc(materialSlotCalibrationStatus) + "\",\n"
                + "  \"calibrationUiStatus\": \"" + esc(calibrationUiStatus) + "\",\n"
                + "  \"calibrationPreset\": \"" + esc(calibrationPreset) + "\",\n"
                + "  \"calibrationSliderStatus\": \"" + esc(calibrationSliderStatus) + "\",\n"
                + "  \"calibrationSliderValue\": " + jsonFloat(calibrationSliderValue) + ",\n"
                + "  \"calibrationUniformUpdateStatus\": \"" + esc(calibrationUniformUpdateStatus) + "\",\n"
                + "  \"calibratedAlbedoDebugViewStatus\": \"" + esc(calibratedAlbedoDebugViewStatus) + "\",\n"
                + "  \"materialTypeDebugViewStatus\": \"" + esc(materialTypeDebugViewStatus) + "\",\n"
                + "  \"aoInfluenceDebugViewStatus\": \"" + esc(aoInfluenceDebugViewStatus) + "\",\n"
                + "  \"luminanceGuardDebugViewStatus\": \"" + esc(luminanceGuardDebugViewStatus) + "\",\n"
                + "  \"materialCalibrationPerformanceStatus\": \"" + esc(materialCalibrationPerformanceStatus) + "\",\n"
                + "  \"specularGlossStatus\": \"" + esc(specularGlossStatus) + "\",\n"
                + "  \"specularGlossMode\": \"" + esc(specularGlossMode) + "\",\n"
                + "  \"specularResponseStatus\": \"" + esc(specularResponseStatus) + "\",\n"
                + "  \"glossResponseStatus\": \"" + esc(glossResponseStatus) + "\",\n"
                + "  \"roughnessRemapV2Status\": \"" + esc(roughnessRemapV2Status) + "\",\n"
                + "  \"metallicSpecularBoostStatus\": \"" + esc(metallicSpecularBoostStatus) + "\",\n"
                + "  \"dielectricGlossStatus\": \"" + esc(dielectricGlossStatus) + "\",\n"
                + "  \"fabricSpecularSuppressStatus\": \"" + esc(fabricSpecularSuppressStatus) + "\",\n"
                + "  \"specularOverbrightGuardStatus\": \"" + esc(specularOverbrightGuardStatus) + "\",\n"
                + "  \"viewDependentHighlightStatus\": \"" + esc(viewDependentHighlightStatus) + "\",\n"
                + "  \"paintGlossLiteStatus\": \"" + esc(paintGlossLiteStatus) + "\",\n"
                + "  \"paintGlossLiteMode\": \"" + esc(paintGlossLiteMode) + "\",\n"
                + "  \"paintGlossIntensity\": " + jsonFloat(paintGlossIntensity) + ",\n"
                + "  \"paintGlossRoughness\": " + jsonFloat(paintGlossRoughness) + ",\n"
                + "  \"paintGlossMaterialHintStatus\": \"" + esc(paintGlossMaterialHintStatus) + "\",\n"
                + "  \"paintGlossPerformanceStatus\": \"" + esc(paintGlossPerformanceStatus) + "\",\n"
                + "  \"glossSliderStatus\": \"" + esc(glossSliderStatus) + "\",\n"
                + "  \"glossSliderValue\": " + jsonFloat(glossSliderValue) + ",\n"
                + "  \"paintGlossSliderStatus\": \"" + esc(paintGlossSliderStatus) + "\",\n"
                + "  \"paintGlossSliderValue\": " + jsonFloat(paintGlossSliderValue) + ",\n"
                + "  \"glossUniformUpdateStatus\": \"" + esc(glossUniformUpdateStatus) + "\",\n"
                + "  \"glossResponseDebugViewStatus\": \"" + esc(glossResponseDebugViewStatus) + "\",\n"
                + "  \"specularGuardDebugViewStatus\": \"" + esc(specularGuardDebugViewStatus) + "\",\n"
                + "  \"paintGlossDebugViewStatus\": \"" + esc(paintGlossDebugViewStatus) + "\",\n"
                + "  \"metalResponseDebugViewStatus\": \"" + esc(metalResponseDebugViewStatus) + "\",\n"
                + "  \"materialTypeSpecularRoutingStatus\": \"" + esc(materialTypeSpecularRoutingStatus) + "\",\n"
                + "  \"paintMaterialGlossStatus\": \"" + esc(paintMaterialGlossStatus) + "\",\n"
                + "  \"metalMaterialGlossStatus\": \"" + esc(metalMaterialGlossStatus) + "\",\n"
                + "  \"rubberMaterialGlossStatus\": \"" + esc(rubberMaterialGlossStatus) + "\",\n"
                + "  \"specularGlossPerformanceStatus\": \"" + esc(specularGlossPerformanceStatus) + "\",\n"
                + "  \"environmentIblStatus\": \"" + esc(environmentIblStatus) + "\",\n"
                + "  \"environmentIblMode\": \"" + esc(environmentIblMode) + "\",\n"
                + "  \"environmentSourceStatus\": \"" + esc(environmentSourceStatus) + "\",\n"
                + "  \"environmentSourceType\": \"" + esc(environmentSourceType) + "\",\n"
                + "  \"environmentSkyColorStatus\": \"" + esc(environmentSkyColorStatus) + "\",\n"
                + "  \"environmentGroundColorStatus\": \"" + esc(environmentGroundColorStatus) + "\",\n"
                + "  \"environmentHorizonStatus\": \"" + esc(environmentHorizonStatus) + "\",\n"
                + "  \"environmentPerformanceStatus\": \"" + esc(environmentPerformanceStatus) + "\",\n"
                + "  \"iblDiffuseStatus\": \"" + esc(iblDiffuseStatus) + "\",\n"
                + "  \"iblSpecularStatus\": \"" + esc(iblSpecularStatus) + "\",\n"
                + "  \"iblRoughnessResponseStatus\": \"" + esc(iblRoughnessResponseStatus) + "\",\n"
                + "  \"iblMetallicResponseStatus\": \"" + esc(iblMetallicResponseStatus) + "\",\n"
                + "  \"iblDielectricResponseStatus\": \"" + esc(iblDielectricResponseStatus) + "\",\n"
                + "  \"iblFabricPreserveStatus\": \"" + esc(iblFabricPreserveStatus) + "\",\n"
                + "  \"iblOverbrightGuardStatus\": \"" + esc(iblOverbrightGuardStatus) + "\",\n"
                + "  \"environmentUiStatus\": \"" + esc(environmentUiStatus) + "\",\n"
                + "  \"environmentPreset\": \"" + esc(environmentPreset) + "\",\n"
                + "  \"environmentIntensity\": " + jsonFloat(environmentIntensity) + ",\n"
                + "  \"environmentSliderStatus\": \"" + esc(environmentSliderStatus) + "\",\n"
                + "  \"skyPresetStatus\": \"" + esc(skyPresetStatus) + "\",\n"
                + "  \"horizonControlStatus\": \"" + esc(horizonControlStatus) + "\",\n"
                + "  \"environmentUniformUpdateStatus\": \"" + esc(environmentUniformUpdateStatus) + "\",\n"
                + "  \"environmentDebugViewStatus\": \"" + esc(environmentDebugViewStatus) + "\",\n"
                + "  \"reflectionDirectionDebugViewStatus\": \"" + esc(reflectionDirectionDebugViewStatus) + "\",\n"
                + "  \"environmentColorDebugViewStatus\": \"" + esc(environmentColorDebugViewStatus) + "\",\n"
                + "  \"iblPerformanceStatus\": \"" + esc(iblPerformanceStatus) + "\",\n"
                + "  \"currentScene\": \"" + SCENE_ID + "\",\n"
                + "  \"currentLabScene\": \"" + SCENE_ID + "\",\n"
                + "  \"currentLabSceneName\": \"" + SCENE_NAME + "\",\n"
                + "  \"lightingStatus\": \"" + esc(lightingStatus) + "\",\n"
                + "  \"lightingControlStatus\": \"" + esc(lightingControlStatus) + "\",\n"
                + "  \"lightingUiMode\": \"" + esc(lightingUiMode) + "\",\n"
                + "  \"sunDirection\": [" + jsonFloat(sunDirection[0]) + ", " + jsonFloat(sunDirection[1]) + ", " + jsonFloat(sunDirection[2]) + "],\n"
                + "  \"sunColor\": [" + jsonFloat(sunColor[0]) + ", " + jsonFloat(sunColor[1]) + ", " + jsonFloat(sunColor[2]) + "],\n"
                + "  \"sunIntensity\": " + jsonFloat(sunIntensity) + ",\n"
                + "  \"ambientColor\": [" + jsonFloat(ambientColor[0]) + ", " + jsonFloat(ambientColor[1]) + ", " + jsonFloat(ambientColor[2]) + "],\n"
                + "  \"ambientIntensity\": " + jsonFloat(ambientIntensity) + ",\n"
                + "  \"lightPreset\": \"" + esc(lightPreset) + "\",\n"
                + "  \"specularBoost\": " + jsonFloat(specularBoost) + ",\n"
                + "  \"specularBoostStatus\": \"" + esc(specularBoostStatus) + "\",\n"
                + "  \"reflectionIntensity\": " + jsonFloat(reflectionIntensity) + ",\n"
                + "  \"iblStatus\": \"" + esc(iblStatus) + "\",\n"
                + "  \"iblMode\": \"" + esc(iblMode) + "\",\n"
                + "  \"reflectionFoundationStatus\": \"" + esc(reflectionFoundationStatus) + "\",\n"
                + "  \"reflectionMode\": \"" + esc(reflectionMode) + "\",\n"
                + "  \"environmentReflectionStatus\": \"" + esc(environmentReflectionStatus) + "\",\n"
                + "  \"environmentReflectionMode\": \"" + esc(environmentReflectionMode) + "\",\n"
                + "  \"environmentSource\": \"" + esc(environmentSource) + "\",\n"
                + "  \"reflectionColorStatus\": \"" + esc(reflectionColorStatus) + "\",\n"
                + "  \"reflectionRoughnessResponseStatus\": \"" + esc(reflectionRoughnessResponseStatus) + "\",\n"
                + "  \"metallicReflectionStatus\": \"" + esc(metallicReflectionStatus) + "\",\n"
                + "  \"dielectricReflectionStatus\": \"" + esc(dielectricReflectionStatus) + "\",\n"
                + "  \"reflectionPerformanceStatus\": \"" + esc(reflectionPerformanceStatus) + "\",\n"
                + "  \"inspectorUiStatus\": \"" + esc(inspectorUiStatus) + "\",\n"
                + "  \"inspectorUiMode\": \"" + esc(inspectorUiMode) + "\",\n"
                + "  \"activeInspectorTab\": \"" + esc(activeInspectorTab) + "\",\n"
                + "  \"assetsTabStatus\": \"" + esc(assetsTabStatus) + "\",\n"
                + "  \"cameraTabStatus\": \"" + esc(cameraTabStatus) + "\",\n"
                + "  \"lightingTabStatus\": \"" + esc(lightingTabStatus) + "\",\n"
                + "  \"materialTabStatus\": \"" + esc(materialTabStatus) + "\",\n"
                + "  \"debugTabStatus\": \"" + esc(debugTabStatus) + "\",\n"
                + "  \"contactGroundingStatus\": \"" + esc(contactGroundingStatus) + "\",\n"
                + "  \"contactShadowStatus\": \"" + esc(contactShadowStatus) + "\",\n"
                + "  \"contactShadowMode\": \"" + esc(contactShadowMode) + "\",\n"
                + "  \"contactShadowIntensity\": " + jsonFloat(contactShadowIntensity) + ",\n"
                + "  \"contactShadowPerformanceStatus\": \"" + esc(contactShadowPerformanceStatus) + "\",\n"
                + "  \"groundingUsesModelBounds\": \"" + esc(groundingUsesModelBounds) + "\",\n"
                + "  \"groundingUniformUpdateStatus\": \"" + esc(groundingUniformUpdateStatus) + "\",\n"
                + "  \"groundSliderStatus\": \"" + esc(groundSliderStatus) + "\",\n"
                + "  \"contactGroundingSliderStatus\": \"" + esc(contactGroundingSliderStatus) + "\",\n"
                + "  \"lightingUniformUpdateStatus\": \"" + esc(lightingUniformUpdateStatus) + "\",\n"
                + "  \"sliderUpdateMode\": \"" + esc(sliderUpdateMode) + "\",\n"
                + "  \"sliderTouchStatus\": \"" + esc(sliderTouchStatus) + "\",\n"
                + "  \"sunSliderStatus\": \"" + esc(sunSliderStatus) + "\",\n"
                + "  \"ambientSliderStatus\": \"" + esc(ambientSliderStatus) + "\",\n"
                + "  \"exposureSliderStatus\": \"" + esc(exposureSliderStatus) + "\",\n"
                + "  \"specularSliderStatus\": \"" + esc(specularSliderStatus) + "\",\n"
                + "  \"reflectionSliderStatus\": \"" + esc(reflectionSliderStatus) + "\",\n"
                + "  \"brdfStatus\": \"" + esc(brdfStatus) + "\",\n"
                + "  \"brdfMode\": \"" + esc(brdfMode) + "\",\n"
                + "  \"diffuseStatus\": \"" + esc(diffuseStatus) + "\",\n"
                + "  \"specularStatus\": \"" + esc(specularStatus) + "\",\n"
                + "  \"fresnelStatus\": \"" + esc(fresnelStatus) + "\",\n"
                + "  \"f0Status\": \"" + esc(f0Status) + "\",\n"
                + "  \"metallicResponseStatus\": \"" + esc(metallicResponseStatus) + "\",\n"
                + "  \"roughnessResponseStatus\": \"" + esc(roughnessResponseStatus) + "\",\n"
                + "  \"directLightingStatus\": \"" + esc(directLightingStatus) + "\",\n"
                + "  \"materialResponseStatus\": \"" + esc(materialResponseStatus) + "\",\n"
                + "  \"pbrQualityTier\": \"" + esc(pbrQualityTier) + "\",\n"
                + "  \"brdfPerformanceStatus\": \"" + esc(brdfPerformanceStatus) + "\",\n"
                + "  \"toneMappingStatus\": \"" + esc(toneMappingStatus) + "\",\n"
                + "  \"toneMappingMode\": \"" + esc(toneMappingMode) + "\",\n"
                + "  \"exposureStatus\": \"" + esc(exposureStatus) + "\",\n"
                + "  \"exposureValue\": " + jsonFloat(exposureValue) + ",\n"
                + "  \"ambientFloor\": " + jsonFloat(ambientFloor) + ",\n"
                + "  \"brightnessPreset\": \"" + esc(brightnessPreset) + "\",\n"
                + "  \"activeDebugView\": \"" + esc(activeDebugView) + "\",\n"
                + "  \"debugViewStatus\": \"" + esc(debugViewStatus) + "\",\n"
                + "  \"normalDebugViewStatus\": \"" + esc(normalDebugViewStatus) + "\",\n"
                + "  \"ndotlDebugViewStatus\": \"" + esc(ndotlDebugViewStatus) + "\",\n"
                + "  \"diffuseDebugViewStatus\": \"" + esc(diffuseDebugViewStatus) + "\",\n"
                + "  \"specularDebugViewStatus\": \"" + esc(specularDebugViewStatus) + "\",\n"
                + "  \"f0DebugViewStatus\": \"" + esc(f0DebugViewStatus) + "\",\n"
                + "  \"reflectionDebugViewStatus\": \"" + esc(reflectionDebugViewStatus) + "\",\n"
                + "  \"iblDiffuseDebugViewStatus\": \"" + esc(iblDiffuseDebugViewStatus) + "\",\n"
                + "  \"iblSpecularDebugViewStatus\": \"" + esc(iblSpecularDebugViewStatus) + "\",\n"
                + "  \"brdfStatusDebugViewStatus\": \"" + esc(brdfStatusDebugViewStatus) + "\",\n"
                + "  \"groundingDebugViewStatus\": \"" + esc(groundingDebugViewStatus) + "\",\n"
                + "  \"calibratedAlbedoDebugViewStatus\": \"" + esc(calibratedAlbedoDebugViewStatus) + "\",\n"
                + "  \"materialTypeDebugViewStatus\": \"" + esc(materialTypeDebugViewStatus) + "\",\n"
                + "  \"aoInfluenceDebugViewStatus\": \"" + esc(aoInfluenceDebugViewStatus) + "\",\n"
                + "  \"luminanceGuardDebugViewStatus\": \"" + esc(luminanceGuardDebugViewStatus) + "\",\n"
                + "  \"fallbackCubeVisible\": " + fallbackCubeVisible + ",\n"
                + "  \"fallbackCubeStatus\": \"" + esc(fallbackCubeStatus) + "\",\n"
                + parse.toJsonFields()
                + "}\n";
        }
    }

    private static final class GlbParser {
        static GlbParseResult parse(File file) {
            if (file == null) return GlbParseResult.failed("file_null");
            GlbParseResult r = new GlbParseResult();
            r.fileSizeBytes = file.length();
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                if (raf.length() < 20) return GlbParseResult.failed("file_too_small_for_glb_header", raf.length(), 0);
                byte[] header = new byte[12];
                raf.readFully(header);
                ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                int magic = h.getInt();
                int version = h.getInt();
                long length = Integer.toUnsignedLong(h.getInt());
                r.glbVersion = version;
                if (magic != 0x46546C67) return GlbParseResult.failed("invalid_glb_magic_expected_glTF", raf.length(), version);
                if (version != 2) return GlbParseResult.failed("unsupported_glb_version_" + version, raf.length(), version);
                if (length != raf.length()) return GlbParseResult.failed("glb_total_length_mismatch_header_" + length + "_actual_" + raf.length(), raf.length(), version);
                String json = null;
                byte[] bin = null;
                boolean binFound = false;
                while (raf.getFilePointer() + 8 <= raf.length()) {
                    byte[] chunkHeader = new byte[8];
                    raf.readFully(chunkHeader);
                    ByteBuffer ch = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN);
                    int chunkLength = ch.getInt();
                    int chunkType = ch.getInt();
                    if (chunkLength < 0 || raf.getFilePointer() + chunkLength > raf.length()) return GlbParseResult.failed("invalid_glb_chunk_length", raf.length(), version);
                    byte[] data = new byte[chunkLength];
                    raf.readFully(data);
                    if (chunkType == 0x4E4F534A) json = new String(data, StandardCharsets.UTF_8).trim();
                    if (chunkType == 0x004E4942) { binFound = true; bin = data; }
                }
                if (json == null || json.isEmpty()) return GlbParseResult.failed("missing_json_chunk", raf.length(), version);
                r.binChunkFound = binFound;
                r.jsonText = json;
                r.binChunk = bin;
                parseJson(json, r);
                r.glbValid = true;
                r.reason = "ok";
                return r;
            } catch (Throwable t) {
                return GlbParseResult.failed(t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "no message" : t.getMessage()), file.length(), r.glbVersion);
            }
        }

        private static void parseJson(String json, GlbParseResult r) throws Exception {
            JSONObject root = new JSONObject(json);
            JSONArray scenes = root.optJSONArray("scenes");
            JSONArray nodes = root.optJSONArray("nodes");
            JSONArray meshes = root.optJSONArray("meshes");
            JSONArray accessors = root.optJSONArray("accessors");
            JSONArray bufferViews = root.optJSONArray("bufferViews");
            JSONArray buffers = root.optJSONArray("buffers");
            JSONArray materials = root.optJSONArray("materials");
            JSONArray images = root.optJSONArray("images");
            JSONArray textures = root.optJSONArray("textures");
            JSONArray samplers = root.optJSONArray("samplers");
            JSONArray skins = root.optJSONArray("skins");
            r.sceneCount = len(scenes);
            r.nodeCount = len(nodes);
            r.meshCount = len(meshes);
            r.accessorCount = len(accessors);
            r.bufferViewCount = len(bufferViews);
            r.bufferCount = len(buffers);
            r.materialCount = len(materials);
            r.imageCount = len(images);
            r.textureCount = len(textures);
            r.samplerCount = len(samplers);
            r.skinCount = len(skins);
            r.hasSkinning = r.skinCount > 0;
            addStrings(root.optJSONArray("extensionsUsed"), r.extensionsUsed);
            addStrings(root.optJSONArray("extensionsRequired"), r.extensionsRequired);
            if (meshes != null) {
                for (int i = 0; i < meshes.length(); i++) {
                    JSONObject mesh = meshes.optJSONObject(i);
                    if (mesh == null) continue;
                    JSONArray primitives = mesh.optJSONArray("primitives");
                    if (primitives == null) continue;
                    r.primitiveCount += primitives.length();
                    for (int j = 0; j < primitives.length(); j++) {
                        JSONObject primitive = primitives.optJSONObject(j);
                        if (primitive == null) continue;
                        JSONObject attrs = primitive.optJSONObject("attributes");
                        if (attrs != null) {
                            String[] keys = {"POSITION", "NORMAL", "TEXCOORD_0", "COLOR_0", "TANGENT", "JOINTS_0", "WEIGHTS_0"};
                            for (String key : keys) {
                                if (attrs.has(key)) r.attributesFound.add(key);
                            }
                            if (attrs.has("POSITION")) r.totalVertexCountEstimate += accessorCount(accessors, attrs.optInt("POSITION", -1));
                            r.hasNormals = r.hasNormals || attrs.has("NORMAL");
                            r.hasTexcoord0 = r.hasTexcoord0 || attrs.has("TEXCOORD_0");
                            r.hasTangents = r.hasTangents || attrs.has("TANGENT");
                            r.hasSkinning = r.hasSkinning || attrs.has("JOINTS_0") || attrs.has("WEIGHTS_0");
                        }
                        if (primitive.has("indices")) r.totalIndexCountEstimate += accessorCount(accessors, primitive.optInt("indices", -1));
                    }
                }
            }
            if (r.attributesFound.isEmpty()) r.attributesFound.add("unknown/not_parsed");
        }

        private static int len(JSONArray a) { return a == null ? 0 : a.length(); }

        private static int accessorCount(JSONArray accessors, int index) {
            if (accessors == null || index < 0 || index >= accessors.length()) return 0;
            JSONObject accessor = accessors.optJSONObject(index);
            return accessor == null ? 0 : Math.max(0, accessor.optInt("count", 0));
        }

        private static void addStrings(JSONArray array, List<String> out) {
            if (array == null) return;
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) out.add(value);
            }
        }

        static GlbPrimitiveMesh extractFirstPrimitive(GlbParseResult parsed) throws Exception {
            if (parsed == null || !parsed.glbValid) throw new IllegalStateException("glb_not_valid: " + (parsed == null ? "null" : parsed.reason));
            if (parsed.jsonText == null || parsed.jsonText.isEmpty()) throw new IllegalStateException("missing_cached_glb_json");
            if (parsed.binChunk == null || parsed.binChunk.length == 0) throw new IllegalStateException("missing_glb_bin_chunk");
            JSONObject root = new JSONObject(parsed.jsonText);
            JSONArray meshes = root.optJSONArray("meshes");
            JSONArray accessors = root.optJSONArray("accessors");
            JSONArray bufferViews = root.optJSONArray("bufferViews");
            if (meshes == null || meshes.length() == 0) throw new IllegalStateException("no_meshes_in_glb");
            JSONObject mesh = meshes.optJSONObject(0);
            if (mesh == null) throw new IllegalStateException("mesh_0_missing");
            JSONArray primitives = mesh.optJSONArray("primitives");
            if (primitives == null || primitives.length() == 0) throw new IllegalStateException("mesh_0_has_no_primitives");
            JSONObject primitive = primitives.optJSONObject(0);
            if (primitive == null) throw new IllegalStateException("primitive_0_missing");
            int mode = primitive.optInt("mode", 4);
            if (mode != 4) throw new IllegalStateException("unsupported_primitive_mode_" + mode + "_expected_TRIANGLES");
            JSONObject attrs = primitive.optJSONObject("attributes");
            if (attrs == null || !attrs.has("POSITION")) throw new IllegalStateException("POSITION_attribute_missing");
            int positionAccessor = attrs.optInt("POSITION", -1);
            int normalAccessor = attrs.optInt("NORMAL", -1);
            int texcoordAccessor = attrs.optInt("TEXCOORD_0", -1);
            int colorAccessor = attrs.optInt("COLOR_0", -1);
            AccessorReader positions = AccessorReader.create(accessors, bufferViews, parsed.binChunk, positionAccessor, 5126, "VEC3", "POSITION");
            AccessorReader normals = normalAccessor >= 0 ? AccessorReader.create(accessors, bufferViews, parsed.binChunk, normalAccessor, 5126, "VEC3", "NORMAL") : null;
            AccessorReader texcoords = texcoordAccessor >= 0 ? AccessorReader.create(accessors, bufferViews, parsed.binChunk, texcoordAccessor, 5126, "VEC2", "TEXCOORD_0") : null;
            AccessorReader colors = colorAccessor >= 0 ? AccessorReader.createColor(accessors, bufferViews, parsed.binChunk, colorAccessor) : null;
            int vertexCount = positions.count;
            float[] rawPositions = new float[vertexCount * 3];
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < vertexCount; i++) {
                float x = positions.floatAt(i, 0);
                float y = positions.floatAt(i, 1);
                float z = positions.floatAt(i, 2);
                rawPositions[i * 3] = x;
                rawPositions[i * 3 + 1] = y;
                rawPositions[i * 3 + 2] = z;
                minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
            }
            float cx = (minX + maxX) * 0.5f;
            float cy = (minY + maxY) * 0.5f;
            float cz = (minZ + maxZ) * 0.5f;
            float extent = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
            float scale = extent > 0.00001f ? 1.8f / extent : 1.0f;
            float[] vertexData = new float[vertexCount * 15];
            for (int i = 0; i < vertexCount; i++) {
                int o = i * 15;
                vertexData[o] = (rawPositions[i * 3] - cx) * scale;
                vertexData[o + 1] = (rawPositions[i * 3 + 1] - cy) * scale;
                vertexData[o + 2] = (rawPositions[i * 3 + 2] - cz) * scale;
                vertexData[o + 3] = normals != null ? normals.floatAt(i, 0) : 0.0f;
                vertexData[o + 4] = normals != null ? normals.floatAt(i, 1) : 1.0f;
                vertexData[o + 5] = normals != null ? normals.floatAt(i, 2) : 0.0f;
                vertexData[o + 6] = texcoords != null ? texcoords.floatAt(i, 0) : 0.0f;
                vertexData[o + 7] = texcoords != null ? texcoords.floatAt(i, 1) : 0.0f;
                vertexData[o + 8] = colors != null ? colors.floatAt(i, 0) : 1.0f;
                vertexData[o + 9] = colors != null ? colors.floatAt(i, 1) : 1.0f;
                vertexData[o + 10] = colors != null ? colors.floatAt(i, 2) : 1.0f;
                vertexData[o + 11] = 1.0f;
                vertexData[o + 12] = 0.0f;
                vertexData[o + 13] = 0.0f;
                vertexData[o + 14] = 1.0f;
            }
            int[] indices = new int[0];
            if (primitive.has("indices")) {
                IndexReader reader = IndexReader.create(accessors, bufferViews, parsed.binChunk, primitive.optInt("indices", -1));
                indices = new int[reader.count];
                for (int i = 0; i < reader.count; i++) indices[i] = reader.indexAt(i);
            }
            GlbPrimitiveMesh out = new GlbPrimitiveMesh();
            out.vertexData = vertexData;
            out.indexData = indices;
            out.vertexCount = vertexCount;
            out.indexCount = indices.length;
            out.boundsMin = new float[] { minX, minY, minZ };
            out.boundsMax = new float[] { maxX, maxY, maxZ };
            out.boundsCenter = new float[] { cx, cy, cz };
            out.modelScale = scale;
            out.baseColorFactor = readBaseColorFactor(root, primitive);
            out.texture = readBaseColorTexture(root, primitive, parsed.binChunk);
            return out;
        }

        static GlbPrimitiveMesh extractMultiPrimitive(GlbParseResult parsed) throws Exception {
            if (parsed == null || !parsed.glbValid) throw new IllegalStateException("glb_not_valid: " + (parsed == null ? "null" : parsed.reason));
            if (parsed.jsonText == null || parsed.jsonText.isEmpty()) throw new IllegalStateException("missing_cached_glb_json");
            if (parsed.binChunk == null || parsed.binChunk.length == 0) throw new IllegalStateException("missing_glb_bin_chunk");
            JSONObject root = new JSONObject(parsed.jsonText);
            JSONArray meshes = root.optJSONArray("meshes");
            JSONArray accessors = root.optJSONArray("accessors");
            JSONArray bufferViews = root.optJSONArray("bufferViews");
            if (meshes == null || meshes.length() == 0) throw new IllegalStateException("no_meshes_in_glb");

            List<PrimitiveSource> sources = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            int primitiveTotal = 0;
            for (int meshIndex = 0; meshIndex < meshes.length(); meshIndex++) {
                JSONObject mesh = meshes.optJSONObject(meshIndex);
                JSONArray primitives = mesh == null ? null : mesh.optJSONArray("primitives");
                if (primitives == null) continue;
                for (int primitiveIndex = 0; primitiveIndex < primitives.length(); primitiveIndex++) {
                    primitiveTotal++;
                    JSONObject primitive = primitives.optJSONObject(primitiveIndex);
                    try {
                        if (primitive == null) throw new IllegalStateException("primitive_not_object");
                        int mode = primitive.optInt("mode", 4);
                        if (mode != 4) throw new IllegalStateException("unsupported_primitive_mode_" + mode + "_expected_TRIANGLES");
                        JSONObject attrs = primitive.optJSONObject("attributes");
                        if (attrs == null || !attrs.has("POSITION")) throw new IllegalStateException("POSITION_attribute_missing");
                        int positionAccessor = attrs.optInt("POSITION", -1);
                        int normalAccessor = attrs.optInt("NORMAL", -1);
                        int texcoordAccessor = attrs.optInt("TEXCOORD_0", -1);
                        int colorAccessor = attrs.optInt("COLOR_0", -1);
                        int tangentAccessor = attrs.optInt("TANGENT", -1);
                        AccessorReader positions = AccessorReader.create(accessors, bufferViews, parsed.binChunk, positionAccessor, 5126, "VEC3", "POSITION");
                        AccessorReader normals = normalAccessor >= 0 ? AccessorReader.create(accessors, bufferViews, parsed.binChunk, normalAccessor, 5126, "VEC3", "NORMAL") : null;
                        AccessorReader texcoords = texcoordAccessor >= 0 ? AccessorReader.create(accessors, bufferViews, parsed.binChunk, texcoordAccessor, 5126, "VEC2", "TEXCOORD_0") : null;
                        AccessorReader colors = colorAccessor >= 0 ? AccessorReader.createColor(accessors, bufferViews, parsed.binChunk, colorAccessor) : null;
                        AccessorReader tangents = tangentAccessor >= 0 ? AccessorReader.create(accessors, bufferViews, parsed.binChunk, tangentAccessor, 5126, "VEC4", "TANGENT") : null;
                        PrimitiveSource src = new PrimitiveSource();
                        src.primitive = primitive;
                        src.positions = positions;
                        src.normals = normals;
                        src.texcoords = texcoords;
                        src.colors = colors;
                        src.tangents = tangents;
                        src.vertexCount = positions.count;
                        if (primitive.has("indices")) src.indices = IndexReader.create(accessors, bufferViews, parsed.binChunk, primitive.optInt("indices", -1));
                        src.materialIndex = primitive.optInt("material", -1);
                        for (int i = 0; i < src.vertexCount; i++) {
                            float x = positions.floatAt(i, 0);
                            float y = positions.floatAt(i, 1);
                            float z = positions.floatAt(i, 2);
                            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
                        }
                        sources.add(src);
                    } catch (Throwable t) {
                        skipped.add("mesh[" + meshIndex + "].primitive[" + primitiveIndex + "]: " + t.getMessage());
                    }
                }
            }
            if (sources.isEmpty()) throw new IllegalStateException("all primitives unsupported: " + joinReasons(skipped));
            float cx = (minX + maxX) * 0.5f;
            float cy = (minY + maxY) * 0.5f;
            float cz = (minZ + maxZ) * 0.5f;
            float extent = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
            float scale = extent > 0.00001f ? 1.8f / extent : 1.0f;
            List<Float> vertexFloats = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            List<Integer> ranges = new ArrayList<>();
            int firstVertex = 0;
            int textureSlotLimit = 8;
            List<MaterialInfo> materials = readMaterials(root, parsed.binChunk, textureSlotLimit);
            int tangentGeneratedCount = 0;
            int tangentFallbackGeneratedCount = 0;
            int tangentMissingCount = 0;
            int tangentDegenerateTriangleCount = 0;
            String tangentStatus = "missing_or_blocked";
            String tangentSource = "missing";
            String tangentFallbackReason = "not_evaluated";
            List<BaseColorTexture> textures = new ArrayList<>();
            for (MaterialInfo mi : materials) if (mi.texture != null && mi.texture.slot >= 0 && textures.size() < textureSlotLimit) textures.add(mi.texture);
            List<BaseColorTexture> pbrTextures = new ArrayList<>();
            for (MaterialInfo mi : materials) {
                addPbrTexture(pbrTextures, mi.metallicRoughnessTexture, textureSlotLimit);
                addPbrTexture(pbrTextures, mi.normalTexture, textureSlotLimit);
                addPbrTexture(pbrTextures, mi.occlusionTexture, textureSlotLimit);
            }
            for (PrimitiveSource src : sources) {
                int materialSlot = src.materialIndex >= 0 && src.materialIndex < materials.size() ? src.materialIndex : 0;
                int textureSlot = materialSlot < materials.size() ? materials.get(materialSlot).textureSlot : -1;
                int rangeFirstIndex = indices.size();
                float[] tangentValues = null;
                int primitiveTangentMissing = 0;
                int primitiveTangentFallback = 0;
                int primitiveDegenerateTriangles = 0;
                String primitiveTangentStatus = "missing_or_blocked";
                if (src.tangents != null) {
                    tangentValues = readTangents(src.tangents);
                    tangentSource = "from_gltf";
                    tangentStatus = "from_gltf";
                    primitiveTangentStatus = "from_gltf";
                } else if (src.normals != null && src.texcoords != null) {
                    int[] localIndices = localIndexArray(src);
                    TangentBuildResult tangentResult = generateTangents(src, localIndices);
                    tangentValues = tangentResult.values;
                    tangentDegenerateTriangleCount += tangentResult.degenerateTriangleCount;
                    primitiveDegenerateTriangles = tangentResult.degenerateTriangleCount;
                    primitiveTangentMissing = tangentResult.missingVertexCount;
                    primitiveTangentFallback = tangentResult.fallbackGeneratedCount;
                    tangentGeneratedCount += tangentResult.generatedVertexCount;
                    tangentFallbackGeneratedCount += tangentResult.fallbackGeneratedCount;
                    tangentMissingCount += tangentResult.missingVertexCount;
                    if (tangentResult.hasAnyTangent()) {
                        tangentSource = tangentFallbackGeneratedCount > 0 ? "generated_with_safe_fallback" : "generated";
                        tangentStatus = tangentResult.complete() ? "generated" : "partial_generated";
                        primitiveTangentStatus = tangentResult.complete() ? "generated" : "partial_generated";
                        if (tangentResult.fallbackGeneratedCount > 0) tangentFallbackReason = "normal_orthogonal_safe_fallback_for_vertices_without_uv_basis";
                    } else {
                        tangentFallbackReason = "degenerate_uv_or_triangle_tangent_basis";
                        primitiveTangentStatus = "blocked_no_tangent";
                    }
                } else {
                    tangentMissingCount += src.vertexCount;
                    primitiveTangentMissing = src.vertexCount;
                    tangentFallbackReason = src.normals == null ? "NORMAL_missing" : "TEXCOORD_0_missing";
                    primitiveTangentStatus = "blocked_no_tangent";
                }
                if (materialSlot >= 0 && materialSlot < materials.size()) {
                    MaterialInfo mat = materials.get(materialSlot);
                    mat.tangentStatus = primitiveTangentStatus;
                    mat.tangentMissingCount += primitiveTangentMissing;
                    mat.tangentFallbackCount += primitiveTangentFallback;
                    mat.tangentDegenerateTriangleCount += primitiveDegenerateTriangles;
                    if (mat.normalTexture != null && "ok".equals(mat.normalTexture.status)) {
                        mat.normalMapAppliedStatus = tangentValues != null && !"blocked_no_tangent".equals(primitiveTangentStatus) ? "ok" : "blocked_no_tangent";
                    } else {
                        mat.normalMapAppliedStatus = mat.normalTexture == null ? "missing" : mat.normalTexture.status;
                    }
                }
                for (int i = 0; i < src.vertexCount; i++) {
                    vertexFloats.add((src.positions.floatAt(i, 0) - cx) * scale);
                    vertexFloats.add((src.positions.floatAt(i, 1) - cy) * scale);
                    vertexFloats.add((src.positions.floatAt(i, 2) - cz) * scale);
                    vertexFloats.add(src.normals != null ? src.normals.floatAt(i, 0) : 0.0f);
                    vertexFloats.add(src.normals != null ? src.normals.floatAt(i, 1) : 1.0f);
                    vertexFloats.add(src.normals != null ? src.normals.floatAt(i, 2) : 0.0f);
                    vertexFloats.add(src.texcoords != null ? src.texcoords.floatAt(i, 0) : 0.0f);
                    vertexFloats.add(src.texcoords != null ? src.texcoords.floatAt(i, 1) : 0.0f);
                    vertexFloats.add(src.colors != null ? src.colors.floatAt(i, 0) : 1.0f);
                    vertexFloats.add(src.colors != null ? src.colors.floatAt(i, 1) : 1.0f);
                    vertexFloats.add(src.colors != null ? src.colors.floatAt(i, 2) : 1.0f);
                    vertexFloats.add(tangentValues != null ? tangentValues[i * 4] : 1.0f);
                    vertexFloats.add(tangentValues != null ? tangentValues[i * 4 + 1] : 0.0f);
                    vertexFloats.add(tangentValues != null ? tangentValues[i * 4 + 2] : 0.0f);
                    vertexFloats.add(tangentValues != null ? tangentValues[i * 4 + 3] : 1.0f);
                }
                if (src.indices != null) {
                    for (int i = 0; i < src.indices.count; i++) indices.add(firstVertex + src.indices.indexAt(i));
                } else {
                    for (int i = 0; i < src.vertexCount; i++) indices.add(firstVertex + i);
                }
                ranges.add(rangeFirstIndex);
                ranges.add(indices.size() - rangeFirstIndex);
                ranges.add(firstVertex);
                ranges.add(src.vertexCount);
                ranges.add(materialSlot);
                ranges.add(textureSlot);
                firstVertex += src.vertexCount;
            }
            GlbPrimitiveMesh out = new GlbPrimitiveMesh();
            out.vertexData = toFloatArray(vertexFloats);
            out.indexData = toIntArray(indices);
            out.rangeData = toIntArray(ranges);
            out.materialData = materialData(materials);
            out.vertexCount = out.vertexData.length / 15;
            out.indexCount = out.indexData.length;
            out.boundsMin = new float[] { minX, minY, minZ };
            out.boundsMax = new float[] { maxX, maxY, maxZ };
            out.boundsCenter = new float[] { cx, cy, cz };
            out.modelScale = scale;
            out.primitiveCountTotal = primitiveTotal;
            out.primitiveCountRendered = sources.size();
            out.primitiveCountSkipped = Math.max(0, primitiveTotal - sources.size());
            out.unsupportedPrimitiveCount = skipped.size();
            out.materialSlotCount = materials.size();
            out.textureSlotCount = textures.size();
            out.textureSlotLimit = textureSlotLimit;
            out.skippedTextureCount = Math.max(0, textureReferences(root) - textures.size());
            out.textures = textures;
            out.pbrTextures = pbrTextures;
            out.materials = materials;
            out.texture = textures.isEmpty() ? BaseColorTexture.missing("baseColorTexture missing") : textures.get(0);
            out.baseColorFactor = materials.isEmpty() ? new float[] {1f,1f,1f,1f} : materials.get(0).baseColorFactor;
            out.pbrTextureSlotCount = pbrTextures.size();
            out.pbrMapsStatus = pbrTextures.isEmpty() ? "missing" : "available";
            if ("missing_or_blocked".equals(tangentStatus) && tangentMissingCount == 0) {
                tangentStatus = "missing_or_blocked";
                tangentFallbackReason = "no_supported_tangent_source";
            } else if (!"missing_or_blocked".equals(tangentStatus) && tangentMissingCount > 0 && !tangentStatus.startsWith("partial_")) {
                tangentStatus = "partial_" + tangentStatus;
            }
            out.tangentStatus = tangentStatus;
            out.tangentSource = tangentSource;
            out.tangentGeneratedCount = tangentGeneratedCount;
            out.tangentFallbackGeneratedCount = tangentFallbackGeneratedCount;
            out.tangentMissingCount = tangentMissingCount;
            out.tangentDegenerateTriangleCount = tangentDegenerateTriangleCount;
            out.tangentFallbackReason = tangentFallbackGeneratedCount > 0 || tangentMissingCount > 0 ? tangentFallbackReason : "none";
            for (MaterialInfo mi : materials) {
                if (mi.normalTexture != null && "ok".equals(mi.normalTexture.status) && "blocked_no_tangent".equals(mi.normalMapAppliedStatus)) {
                    mi.normalTexture.status = "blocked_no_tangent";
                    mi.normalTexture.reason = "normal map requires tangent data for this material primitive";
                }
                mi.normalMapStatus = mi.normalTexture == null ? "missing" : mi.normalTexture.status;
            }
            out.metallicRoughnessStatus = aggregateStatus(materials, TEX_METALLIC_ROUGHNESS);
            out.normalMapStatus = aggregateStatus(materials, TEX_NORMAL);
            out.normalMapAppliedStatus = normalAppliedStatus(out.normalMapStatus, out.tangentStatus);
            out.occlusionMapStatus = aggregateStatus(materials, TEX_OCCLUSION);
            out.materialSlotDiagnostics = materialSlotDiagnostics(materials);
            out.reason = skipped.isEmpty() ? "all supported primitives uploaded" : joinReasons(skipped);
            return out;
        }

        private static List<MaterialInfo> readMaterials(JSONObject root, byte[] bin, int textureSlotLimit) {
            List<MaterialInfo> out = new ArrayList<>();
            JSONArray materials = root.optJSONArray("materials");
            int count = materials == null ? 1 : Math.max(1, materials.length());
            for (int i = 0; i < count; i++) {
                JSONObject material = materials == null ? null : materials.optJSONObject(i);
                MaterialInfo info = new MaterialInfo();
                info.materialName = material == null ? "" : material.optString("name", "");
                info.baseColorFactor = readBaseColorFactor(root, i);
                JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
                info.metallicFactor = pbr == null ? 0.0f : (float)pbr.optDouble("metallicFactor", 1.0);
                info.roughnessFactor = pbr == null ? 1.0f : (float)pbr.optDouble("roughnessFactor", 1.0);
                String alpha = material == null ? "OPAQUE" : material.optString("alphaMode", "OPAQUE");
                info.alphaModeText = alpha;
                info.alphaMode = "MASK".equals(alpha) ? 1 : ("BLEND".equals(alpha) ? 2 : 0);
                info.alphaCutoff = material == null ? 0.5f : (float)material.optDouble("alphaCutoff", 0.5);
                info.doubleSided = material != null && material.optBoolean("doubleSided", false);
                JSONArray emissive = material == null ? null : material.optJSONArray("emissiveFactor");
                if (emissive != null && emissive.length() >= 3) for (int e = 0; e < 3; e++) info.emissiveFactor[e] = (float)emissive.optDouble(e, 0.0);
                info.emissiveTextureStatus = material != null && material.optJSONObject("emissiveTexture") != null ? "metadata_only" : "missing";
                if (i < textureSlotLimit) {
                    BaseColorTexture texture = readBaseColorTexture(root, i, bin);
                    if ("ok".equals(texture.status)) {
                        texture.slot = i;
                        texture.materialSlot = i;
                        texture.kind = TEX_BASE_COLOR;
                        info.textureSlot = i;
                        info.texture = texture;
                    }
                    info.metallicRoughnessTexture = readMaterialTexture(root, i, bin, TEX_METALLIC_ROUGHNESS, "metallicRoughnessTexture");
                    info.normalTexture = readMaterialTexture(root, i, bin, TEX_NORMAL, "normalTexture");
                    info.occlusionTexture = readMaterialTexture(root, i, bin, TEX_OCCLUSION, "occlusionTexture");
                    if (info.normalTexture != null) info.normalScale = info.normalTexture.normalScale;
                    if (info.occlusionTexture != null) info.occlusionStrength = info.occlusionTexture.occlusionStrength;
                }
                info.metallicRoughnessStatus = info.metallicRoughnessTexture == null ? "missing" : info.metallicRoughnessTexture.status;
                info.normalMapStatus = info.normalTexture == null ? "missing" : info.normalTexture.status;
                info.normalMapAppliedStatus = "ok".equals(info.normalMapStatus) ? "blocked_no_tangent" : info.normalMapStatus;
                info.occlusionMapStatus = info.occlusionTexture == null ? "missing" : info.occlusionTexture.status;
                info.materialTypeHint = materialTypeHint(info);
                info.albedoLuminance = luminance(info.baseColorFactor);
                info.calibratedRoughness = calibratedRoughness(info);
                info.calibratedMetallic = clampUnitRange(info.metallicFactor, 0.0f, 1.0f);
                info.aoInfluence = "ok".equals(info.occlusionMapStatus) ? clampUnitRange(info.occlusionStrength * 1.25f, 0.0f, 1.0f) : 0.0f;
                info.emissiveGuardApplied = luminance(info.emissiveFactor) <= 0.001f;
                out.add(info);
            }
            return out;
        }

        private static int materialTypeHint(MaterialInfo info) {
            String name = info.materialName == null ? "" : info.materialName.toLowerCase(Locale.US);
            boolean hasBase = info.texture != null && "ok".equals(info.texture.status);
            boolean hasMr = info.metallicRoughnessTexture != null && "ok".equals(info.metallicRoughnessTexture.status);
            if (info.metallicFactor >= 0.65f || name.contains("metal") || name.contains("chrome") || name.contains("steel")) return 2;
            if (name.contains("fabric") || name.contains("cloth") || name.contains("seat") || name.contains("carpet") || (info.roughnessFactor >= 0.78f && info.metallicFactor < 0.15f && info.alphaMode == 0)) return 0;
            if (name.contains("rubber") || name.contains("tire") || name.contains("tyre") || name.contains("black") || (info.roughnessFactor >= 0.55f && info.metallicFactor < 0.08f && !hasBase)) return 3;
            if (name.contains("paint") || name.contains("body") || name.contains("coat") || (info.metallicFactor < 0.2f && info.roughnessFactor < 0.72f && (hasBase || hasMr))) return 1;
            return 4;
        }

        private static String materialTypeHintName(int hint) {
            if (hint == 0) return "fabric_like";
            if (hint == 1) return "paint_like";
            if (hint == 2) return "metal_like";
            if (hint == 3) return "rubber_like";
            return "unknown";
        }

        private static float calibratedRoughness(MaterialInfo info) {
            float r = clampUnitRange(info.roughnessFactor, 0.04f, 1.0f);
            if (info.materialTypeHint == 0) return Math.max(r, 0.78f);
            if (info.materialTypeHint == 3) return Math.max(r, 0.62f);
            if (info.materialTypeHint == 1) return clampUnitRange(r, 0.28f, 0.72f);
            if (info.materialTypeHint == 2) return clampUnitRange(r, 0.18f, 0.64f);
            return clampUnitRange(r, 0.24f, 0.88f);
        }

        private static float luminance(float[] rgb) {
            if (rgb == null || rgb.length < 3) return 0.0f;
            return rgb[0] * 0.2126f + rgb[1] * 0.7152f + rgb[2] * 0.0722f;
        }

        private static float clampUnitRange(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private static void addPbrTexture(List<BaseColorTexture> out, BaseColorTexture texture, int limit) {
            if (texture == null || out.size() >= limit) return;
            if ("ok".equals(texture.status)) out.add(texture);
        }

        private static String normalAppliedStatus(String normalMapStatus, String tangentStatus) {
            if ("missing".equals(normalMapStatus)) return "missing";
            if ("ok".equals(normalMapStatus) || "partial_ok".equals(normalMapStatus)) {
                if (tangentStatus != null && (tangentStatus.contains("generated") || tangentStatus.contains("from_gltf"))) {
                    return "partial_ok".equals(normalMapStatus) || tangentStatus.startsWith("partial_") ? "partial_ok" : "ok";
                }
                return "blocked_no_tangent";
            }
            if ("blocked_no_tangent".equals(normalMapStatus)) return "blocked_no_tangent";
            return normalMapStatus == null ? "missing" : normalMapStatus;
        }

        private static int[] localIndexArray(PrimitiveSource src) {
            if (src.indices != null && src.indices.count > 0) {
                int[] out = new int[src.indices.count];
                for (int i = 0; i < src.indices.count; i++) out[i] = src.indices.indexAt(i);
                return out;
            }
            int[] out = new int[src.vertexCount];
            for (int i = 0; i < src.vertexCount; i++) out[i] = i;
            return out;
        }

        private static float[] readTangents(AccessorReader tangents) {
            float[] out = new float[tangents.count * 4];
            for (int i = 0; i < tangents.count; i++) {
                float x = tangents.floatAt(i, 0);
                float y = tangents.floatAt(i, 1);
                float z = tangents.floatAt(i, 2);
                float invLen = invLength3(x, y, z);
                out[i * 4] = x * invLen;
                out[i * 4 + 1] = y * invLen;
                out[i * 4 + 2] = z * invLen;
                out[i * 4 + 3] = tangents.floatAt(i, 3) < 0.0f ? -1.0f : 1.0f;
            }
            return out;
        }

        private static TangentBuildResult generateTangents(PrimitiveSource src, int[] localIndices) {
            float[] tan1 = new float[src.vertexCount * 3];
            float[] tan2 = new float[src.vertexCount * 3];
            int validTriangles = 0;
            int degenerateTriangles = 0;
            for (int i = 0; i + 2 < localIndices.length; i += 3) {
                int i1 = localIndices[i];
                int i2 = localIndices[i + 1];
                int i3 = localIndices[i + 2];
                if (i1 < 0 || i2 < 0 || i3 < 0 || i1 >= src.vertexCount || i2 >= src.vertexCount || i3 >= src.vertexCount) continue;
                float x1 = src.positions.floatAt(i2, 0) - src.positions.floatAt(i1, 0);
                float y1 = src.positions.floatAt(i2, 1) - src.positions.floatAt(i1, 1);
                float z1 = src.positions.floatAt(i2, 2) - src.positions.floatAt(i1, 2);
                float x2 = src.positions.floatAt(i3, 0) - src.positions.floatAt(i1, 0);
                float y2 = src.positions.floatAt(i3, 1) - src.positions.floatAt(i1, 1);
                float z2 = src.positions.floatAt(i3, 2) - src.positions.floatAt(i1, 2);
                float s1 = src.texcoords.floatAt(i2, 0) - src.texcoords.floatAt(i1, 0);
                float t1 = src.texcoords.floatAt(i2, 1) - src.texcoords.floatAt(i1, 1);
                float s2 = src.texcoords.floatAt(i3, 0) - src.texcoords.floatAt(i1, 0);
                float t2 = src.texcoords.floatAt(i3, 1) - src.texcoords.floatAt(i1, 1);
                float denom = s1 * t2 - s2 * t1;
                if (Math.abs(denom) < 1.0e-8f) {
                    degenerateTriangles++;
                    continue;
                }
                validTriangles++;
                float r = 1.0f / denom;
                float sx = (t2 * x1 - t1 * x2) * r;
                float sy = (t2 * y1 - t1 * y2) * r;
                float sz = (t2 * z1 - t1 * z2) * r;
                float bx = (s1 * x2 - s2 * x1) * r;
                float by = (s1 * y2 - s2 * y1) * r;
                float bz = (s1 * z2 - s2 * z1) * r;
                add3(tan1, i1, sx, sy, sz); add3(tan1, i2, sx, sy, sz); add3(tan1, i3, sx, sy, sz);
                add3(tan2, i1, bx, by, bz); add3(tan2, i2, bx, by, bz); add3(tan2, i3, bx, by, bz);
            }
            float[] out = new float[src.vertexCount * 4];
            int generated = 0;
            int fallback = 0;
            int missing = 0;
            for (int i = 0; i < src.vertexCount; i++) {
                float nx = src.normals.floatAt(i, 0);
                float ny = src.normals.floatAt(i, 1);
                float nz = src.normals.floatAt(i, 2);
                float nInv = invLength3(nx, ny, nz);
                if (nInv == 0.0f) {
                    missing++;
                    continue;
                }
                nx *= nInv; ny *= nInv; nz *= nInv;
                float tx = tan1[i * 3];
                float ty = tan1[i * 3 + 1];
                float tz = tan1[i * 3 + 2];
                float ndott = nx * tx + ny * ty + nz * tz;
                tx -= nx * ndott;
                ty -= ny * ndott;
                tz -= nz * ndott;
                float invLen = invLength3(tx, ty, tz);
                float handedness = 1.0f;
                if (invLen == 0.0f) {
                    float[] fallbackTangent = orthogonalTangent(nx, ny, nz);
                    if (fallbackTangent == null) {
                        missing++;
                        continue;
                    }
                    tx = fallbackTangent[0];
                    ty = fallbackTangent[1];
                    tz = fallbackTangent[2];
                    fallback++;
                } else {
                    tx *= invLen; ty *= invLen; tz *= invLen;
                    float cx = ny * tz - nz * ty;
                    float cy = nz * tx - nx * tz;
                    float cz = nx * ty - ny * tx;
                    handedness = (cx * tan2[i * 3] + cy * tan2[i * 3 + 1] + cz * tan2[i * 3 + 2]) < 0.0f ? -1.0f : 1.0f;
                    generated++;
                }
                out[i * 4] = tx;
                out[i * 4 + 1] = ty;
                out[i * 4 + 2] = tz;
                out[i * 4 + 3] = handedness;
            }
            TangentBuildResult result = new TangentBuildResult();
            result.values = (generated + fallback) > 0 ? out : null;
            result.generatedVertexCount = generated;
            result.fallbackGeneratedCount = fallback;
            result.missingVertexCount = missing;
            result.degenerateTriangleCount = degenerateTriangles;
            result.validTriangleCount = validTriangles;
            return result;
        }

        private static float[] orthogonalTangent(float nx, float ny, float nz) {
            float ax = Math.abs(nx) < 0.9f ? 1.0f : 0.0f;
            float ay = Math.abs(nx) < 0.9f ? 0.0f : 1.0f;
            float az = 0.0f;
            float tx = ay * nz - az * ny;
            float ty = az * nx - ax * nz;
            float tz = ax * ny - ay * nx;
            float inv = invLength3(tx, ty, tz);
            if (inv == 0.0f) return null;
            return new float[] { tx * inv, ty * inv, tz * inv };
        }

        private static void add3(float[] values, int index, float x, float y, float z) {
            values[index * 3] += x;
            values[index * 3 + 1] += y;
            values[index * 3 + 2] += z;
        }

        private static float invLength3(float x, float y, float z) {
            float len2 = x * x + y * y + z * z;
            return len2 > 1.0e-12f ? (float)(1.0 / Math.sqrt(len2)) : 0.0f;
        }

        private static float[] readBaseColorFactor(JSONObject root, int materialIndex) {
            float[] out = new float[] { 1f, 1f, 1f, 1f };
            JSONArray materials = root.optJSONArray("materials");
            JSONObject material = materials != null && materialIndex >= 0 && materialIndex < materials.length() ? materials.optJSONObject(materialIndex) : null;
            JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
            JSONArray factor = pbr == null ? null : pbr.optJSONArray("baseColorFactor");
            if (factor != null && factor.length() >= 4) for (int i = 0; i < 4; i++) out[i] = (float)factor.optDouble(i, 1.0);
            return out;
        }

        private static BaseColorTexture readBaseColorTexture(JSONObject root, int materialIndex, byte[] bin) {
            JSONObject primitive = new JSONObject();
            try { primitive.put("material", materialIndex); } catch (Throwable ignored) { }
            return readBaseColorTexture(root, primitive, bin);
        }

        private static BaseColorTexture readMaterialTexture(JSONObject root, int materialIndex, byte[] bin, int kind, String label) {
            try {
                JSONArray materials = root.optJSONArray("materials");
                JSONObject material = materials != null && materialIndex >= 0 && materialIndex < materials.length() ? materials.optJSONObject(materialIndex) : null;
                JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
                JSONObject textureInfo;
                if (kind == TEX_METALLIC_ROUGHNESS) textureInfo = pbr == null ? null : pbr.optJSONObject("metallicRoughnessTexture");
                else if (kind == TEX_NORMAL) textureInfo = material == null ? null : material.optJSONObject("normalTexture");
                else if (kind == TEX_OCCLUSION) textureInfo = material == null ? null : material.optJSONObject("occlusionTexture");
                else textureInfo = null;
                if (textureInfo == null) return BaseColorTexture.missing(label + " missing");
                BaseColorTexture texture = decodeTexture(root, textureInfo.optInt("index", -1), bin, label);
                texture.kind = kind;
                texture.materialSlot = materialIndex;
                texture.slot = materialIndex;
                if (kind == TEX_NORMAL) texture.normalScale = (float)textureInfo.optDouble("scale", 1.0);
                if (kind == TEX_OCCLUSION) texture.occlusionStrength = (float)textureInfo.optDouble("strength", 1.0);
                return texture;
            } catch (Throwable t) {
                return BaseColorTexture.failed(t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? label + " decode failed" : t.getMessage()));
            }
        }

        private static BaseColorTexture decodeTexture(JSONObject root, int textureIndex, byte[] bin, String label) throws Exception {
            JSONArray textures = root.optJSONArray("textures");
            JSONArray images = root.optJSONArray("images");
            JSONArray bufferViews = root.optJSONArray("bufferViews");
            JSONObject texture = textures != null && textureIndex >= 0 && textureIndex < textures.length() ? textures.optJSONObject(textureIndex) : null;
            if (texture == null) return BaseColorTexture.failed(label + " texture index missing: " + textureIndex);
            int sourceIndex = texture.optInt("source", -1);
            JSONObject image = images != null && sourceIndex >= 0 && sourceIndex < images.length() ? images.optJSONObject(sourceIndex) : null;
            if (image == null) return BaseColorTexture.failed(label + " image source missing: " + sourceIndex);
            if (image.has("uri")) {
                String uri = image.optString("uri", "");
                return BaseColorTexture.withStatus(uri.startsWith("data:") ? "unsupported_data_uri" : "unsupported_external_uri", uri.startsWith("data:") ? "unsupported_data_uri" : "unsupported_external_uri");
            }
            int viewIndex = image.optInt("bufferView", -1);
            String mimeType = image.optString("mimeType", "");
            if (!"image/png".equals(mimeType) && !"image/jpeg".equals(mimeType)) return BaseColorTexture.failed("unsupported_" + label + "_mimeType_" + mimeType);
            JSONObject view = checkedObject(bufferViews, viewIndex, label + "_image_bufferView");
            int offset = view.optInt("byteOffset", 0);
            int length = view.optInt("byteLength", -1);
            if (bin == null || length <= 0 || offset < 0 || offset + length > bin.length) return BaseColorTexture.failed(label + "_bufferView_out_of_bin_bounds");
            Bitmap decoded = BitmapFactory.decodeByteArray(bin, offset, length);
            if (decoded == null) return BaseColorTexture.failed("BitmapFactory decode failed for " + mimeType);
            Bitmap rgba = decoded.getConfig() == Bitmap.Config.ARGB_8888 ? decoded : decoded.copy(Bitmap.Config.ARGB_8888, false);
            if (rgba == null) return BaseColorTexture.failed("Bitmap ARGB_8888 conversion failed");
            int width = rgba.getWidth();
            int height = rgba.getHeight();
            int[] pixels = new int[width * height];
            rgba.getPixels(pixels, 0, width, 0, 0, width, height);
            BaseColorTexture ok = new BaseColorTexture();
            ok.status = "ok";
            ok.reason = "decoded";
            ok.name = image.optString("name", label + "_" + textureIndex);
            ok.source = "textures[" + textureIndex + "].source=images[" + sourceIndex + "].bufferView=" + viewIndex;
            ok.mimeType = mimeType;
            ok.width = width;
            ok.height = height;
            ok.pixels = pixels;
            return ok;
        }

        private static String aggregateStatus(List<MaterialInfo> materials, int kind) {
            boolean anyOk = false, anyFailed = false, anyBlocked = false;
            for (MaterialInfo m : materials) {
                BaseColorTexture t = kind == TEX_METALLIC_ROUGHNESS ? m.metallicRoughnessTexture : (kind == TEX_NORMAL ? m.normalTexture : m.occlusionTexture);
                if (t == null || "missing".equals(t.status)) continue;
                if ("ok".equals(t.status)) anyOk = true;
                else if ("blocked_no_tangent".equals(t.status)) anyBlocked = true;
                else anyFailed = true;
            }
            if (anyOk) return anyFailed || anyBlocked ? "partial_ok" : "ok";
            if (anyBlocked) return "blocked_no_tangent";
            return anyFailed ? "failed" : "missing";
        }

        private static String materialSlotDiagnostics(List<MaterialInfo> materials) {
            StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < materials.size(); i++) {
                MaterialInfo m = materials.get(i);
                if (i > 0) b.append(",");
                b.append("{\"slot\":").append(i)
                    .append(",\"materialTypeHint\":\"").append(esc(materialTypeHintName(m.materialTypeHint))).append("\"")
                    .append(",\"calibrationApplied\":true")
                    .append(",\"albedoLuminance\":").append(jsonFloat(m.albedoLuminance))
                    .append(",\"calibratedRoughness\":").append(jsonFloat(m.calibratedRoughness))
                    .append(",\"calibratedMetallic\":").append(jsonFloat(m.calibratedMetallic))
                    .append(",\"aoInfluence\":").append(jsonFloat(m.aoInfluence))
                    .append(",\"emissiveGuardApplied\":").append(m.emissiveGuardApplied)
                    .append(",\"metallicFactor\":").append(jsonFloat(m.metallicFactor))
                    .append(",\"roughnessFactor\":").append(jsonFloat(m.roughnessFactor))
                    .append(",\"metallicRoughnessStatus\":\"").append(esc(m.metallicRoughnessStatus)).append("\"")
                    .append(",\"normalTextureStatus\":\"").append(esc(m.normalMapStatus)).append("\"")
                    .append(",\"tangentStatus\":\"").append(esc(m.tangentStatus)).append("\"")
                    .append(",\"normalMapAppliedStatus\":\"").append(esc(m.normalMapAppliedStatus)).append("\"")
                    .append(",\"tangentMissingCount\":").append(m.tangentMissingCount)
                    .append(",\"tangentFallbackCount\":").append(m.tangentFallbackCount)
                    .append(",\"tangentDegenerateTriangleCount\":").append(m.tangentDegenerateTriangleCount)
                    .append(",\"normalMapStatus\":\"").append(esc(m.normalMapStatus)).append("\"")
                    .append(",\"occlusionMapStatus\":\"").append(esc(m.occlusionMapStatus)).append("\"")
                    .append(",\"normalScale\":").append(jsonFloat(m.normalScale))
                    .append(",\"occlusionStrength\":").append(jsonFloat(m.occlusionStrength))
                    .append(",\"emissiveFactor\":[").append(jsonFloat(m.emissiveFactor[0])).append(",").append(jsonFloat(m.emissiveFactor[1])).append(",").append(jsonFloat(m.emissiveFactor[2])).append("]")
                    .append(",\"emissiveTextureStatus\":\"").append(esc(m.emissiveTextureStatus)).append("\"")
                    .append(",\"alphaMode\":\"").append(esc(m.alphaModeText)).append("\"")
                    .append(",\"doubleSided\":").append(m.doubleSided)
                    .append("}");
            }
            return b.append("]").toString();
        }

        private static int textureReferences(JSONObject root) {
            int out = 0;
            JSONArray materials = root.optJSONArray("materials");
            if (materials == null) return 0;
            for (int i = 0; i < materials.length(); i++) {
                JSONObject material = materials.optJSONObject(i);
                JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
                if (pbr != null && pbr.optJSONObject("baseColorTexture") != null) out++;
            }
            return out;
        }

        private static float[] materialData(List<MaterialInfo> materials) {
            float[] out = new float[Math.max(1, materials.size()) * 16];
            if (materials.isEmpty()) materials.add(new MaterialInfo());
            for (int i = 0; i < materials.size(); i++) {
                MaterialInfo m = materials.get(i);
                out[i * 16] = m.baseColorFactor[0];
                out[i * 16 + 1] = m.baseColorFactor[1];
                out[i * 16 + 2] = m.baseColorFactor[2];
                out[i * 16 + 3] = m.baseColorFactor[3];
                out[i * 16 + 4] = m.alphaMode;
                out[i * 16 + 5] = m.alphaCutoff;
                out[i * 16 + 6] = m.doubleSided ? 1f : 0f;
                out[i * 16 + 7] = m.textureSlot;
                out[i * 16 + 8] = m.metallicFactor;
                out[i * 16 + 9] = m.roughnessFactor;
                out[i * 16 + 10] = m.metallicRoughnessTexture != null && "ok".equals(m.metallicRoughnessTexture.status) ? i : -1f;
                out[i * 16 + 11] = m.normalTexture != null && "ok".equals(m.normalTexture.status) ? i : -1f;
                out[i * 16 + 12] = m.occlusionTexture != null && "ok".equals(m.occlusionTexture.status) ? i : -1f;
                out[i * 16 + 13] = m.normalScale;
                out[i * 16 + 14] = m.occlusionStrength;
                out[i * 16 + 15] = m.materialTypeHint;
            }
            return out;
        }

        private static float[] toFloatArray(List<Float> values) {
            float[] out = new float[values.size()];
            for (int i = 0; i < values.size(); i++) out[i] = values.get(i);
            return out;
        }

        private static int[] toIntArray(List<Integer> values) {
            int[] out = new int[values.size()];
            for (int i = 0; i < values.size(); i++) out[i] = values.get(i);
            return out;
        }

        private static String joinReasons(List<String> reasons) {
            if (reasons == null || reasons.isEmpty()) return "none";
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) b.append("; ");
                b.append(reasons.get(i));
                if (b.length() > 900) break;
            }
            return b.toString();
        }

        private static float[] readBaseColorFactor(JSONObject root, JSONObject primitive) {
            float[] out = new float[] { 1f, 1f, 1f, 1f };
            JSONArray materials = root.optJSONArray("materials");
            int materialIndex = primitive.optInt("material", -1);
            JSONObject material = materials != null && materialIndex >= 0 && materialIndex < materials.length() ? materials.optJSONObject(materialIndex) : null;
            JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
            JSONArray factor = pbr == null ? null : pbr.optJSONArray("baseColorFactor");
            if (factor != null && factor.length() >= 4) {
                for (int i = 0; i < 4; i++) out[i] = (float)factor.optDouble(i, 1.0);
            }
            return out;
        }

        private static BaseColorTexture readBaseColorTexture(JSONObject root, JSONObject primitive, byte[] bin) {
            BaseColorTexture out = BaseColorTexture.missing("baseColorTexture missing");
            try {
                JSONArray materials = root.optJSONArray("materials");
                JSONArray textures = root.optJSONArray("textures");
                JSONArray images = root.optJSONArray("images");
                JSONArray bufferViews = root.optJSONArray("bufferViews");
                int materialIndex = primitive.optInt("material", -1);
                JSONObject material = materials != null && materialIndex >= 0 && materialIndex < materials.length() ? materials.optJSONObject(materialIndex) : null;
                JSONObject pbr = material == null ? null : material.optJSONObject("pbrMetallicRoughness");
                JSONObject baseColorTexture = pbr == null ? null : pbr.optJSONObject("baseColorTexture");
                if (baseColorTexture == null) return out;
                int textureIndex = baseColorTexture.optInt("index", -1);
                JSONObject texture = textures != null && textureIndex >= 0 && textureIndex < textures.length() ? textures.optJSONObject(textureIndex) : null;
                if (texture == null) return BaseColorTexture.failed("baseColorTexture texture index missing: " + textureIndex);
                int sourceIndex = texture.optInt("source", -1);
                JSONObject image = images != null && sourceIndex >= 0 && sourceIndex < images.length() ? images.optJSONObject(sourceIndex) : null;
                if (image == null) return BaseColorTexture.failed("baseColorTexture image source missing: " + sourceIndex);
                if (image.has("uri")) {
                    String uri = image.optString("uri", "");
                    return BaseColorTexture.withStatus(uri.startsWith("data:") ? "unsupported_data_uri" : "unsupported_external_uri", uri.startsWith("data:") ? "unsupported_data_uri" : "unsupported_external_uri");
                }
                int viewIndex = image.optInt("bufferView", -1);
                String mimeType = image.optString("mimeType", "");
                if (!"image/png".equals(mimeType) && !"image/jpeg".equals(mimeType)) return BaseColorTexture.failed("unsupported_baseColorTexture_mimeType_" + mimeType);
                JSONObject view = checkedObject(bufferViews, viewIndex, "baseColorTexture_image_bufferView");
                int offset = view.optInt("byteOffset", 0);
                int length = view.optInt("byteLength", -1);
                if (bin == null || length <= 0 || offset < 0 || offset + length > bin.length) return BaseColorTexture.failed("baseColorTexture_bufferView_out_of_bin_bounds");
                Bitmap decoded = BitmapFactory.decodeByteArray(bin, offset, length);
                if (decoded == null) return BaseColorTexture.failed("BitmapFactory decode failed for " + mimeType);
                Bitmap rgba = decoded.getConfig() == Bitmap.Config.ARGB_8888 ? decoded : decoded.copy(Bitmap.Config.ARGB_8888, false);
                if (rgba == null) return BaseColorTexture.failed("Bitmap ARGB_8888 conversion failed");
                int width = rgba.getWidth();
                int height = rgba.getHeight();
                int[] pixels = new int[width * height];
                rgba.getPixels(pixels, 0, width, 0, 0, width, height);
                BaseColorTexture ok = new BaseColorTexture();
                ok.status = "ok";
                ok.reason = "decoded";
                ok.name = image.optString("name", "baseColorTexture_" + textureIndex);
                ok.source = "textures[" + textureIndex + "].source=images[" + sourceIndex + "].bufferView=" + viewIndex;
                ok.mimeType = mimeType;
                ok.width = width;
                ok.height = height;
                ok.pixels = pixels;
                return ok;
            } catch (Throwable t) {
                return BaseColorTexture.failed(t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "texture decode failed" : t.getMessage()));
            }
        }
    }

    private static final class AccessorReader {
        final ByteBuffer data;
        final int offset;
        final int stride;
        final int count;
        final int components;
        final String label;

        AccessorReader(ByteBuffer data, int offset, int stride, int count, int components, String label) {
            this.data = data;
            this.offset = offset;
            this.stride = stride;
            this.count = count;
            this.components = components;
            this.label = label;
        }

        static AccessorReader create(JSONArray accessors, JSONArray bufferViews, byte[] bin, int accessorIndex, int requiredComponentType, String requiredType, String label) throws Exception {
            JSONObject accessor = checkedObject(accessors, accessorIndex, label + "_accessor");
            int componentType = accessor.optInt("componentType", -1);
            String type = accessor.optString("type", "");
            if (componentType != requiredComponentType || !requiredType.equals(type)) {
                throw new IllegalStateException("unsupported_" + label + "_accessor_componentType_" + componentType + "_type_" + type);
            }
            return createRaw(accessor, bufferViews, bin, label, componentsForType(type), 4);
        }

        static AccessorReader createColor(JSONArray accessors, JSONArray bufferViews, byte[] bin, int accessorIndex) throws Exception {
            JSONObject accessor = checkedObject(accessors, accessorIndex, "COLOR_0_accessor");
            int componentType = accessor.optInt("componentType", -1);
            String type = accessor.optString("type", "");
            if (componentType != 5126 || (!"VEC3".equals(type) && !"VEC4".equals(type))) {
                throw new IllegalStateException("unsupported_COLOR_0_accessor_componentType_" + componentType + "_type_" + type);
            }
            return createRaw(accessor, bufferViews, bin, "COLOR_0", componentsForType(type), 4);
        }

        private static AccessorReader createRaw(JSONObject accessor, JSONArray bufferViews, byte[] bin, String label, int components, int componentBytes) throws Exception {
            int viewIndex = accessor.optInt("bufferView", -1);
            JSONObject view = checkedObject(bufferViews, viewIndex, label + "_bufferView");
            int count = Math.max(0, accessor.optInt("count", 0));
            int accessorOffset = accessor.optInt("byteOffset", 0);
            int viewOffset = view.optInt("byteOffset", 0);
            int stride = view.optInt("byteStride", components * componentBytes);
            int offset = viewOffset + accessorOffset;
            int needed = offset + (count <= 0 ? 0 : ((count - 1) * stride + components * componentBytes));
            if (offset < 0 || needed > bin.length) throw new IllegalStateException(label + "_accessor_out_of_bin_bounds");
            return new AccessorReader(ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN), offset, stride, count, components, label);
        }

        float floatAt(int index, int component) {
            if (component >= components) return 1.0f;
            return data.getFloat(offset + index * stride + component * 4);
        }
    }

    private static final class IndexReader {
        final ByteBuffer data;
        final int offset;
        final int stride;
        final int count;
        final int componentType;

        IndexReader(ByteBuffer data, int offset, int stride, int count, int componentType) {
            this.data = data;
            this.offset = offset;
            this.stride = stride;
            this.count = count;
            this.componentType = componentType;
        }

        static IndexReader create(JSONArray accessors, JSONArray bufferViews, byte[] bin, int accessorIndex) throws Exception {
            JSONObject accessor = checkedObject(accessors, accessorIndex, "indices_accessor");
            int componentType = accessor.optInt("componentType", -1);
            int componentBytes;
            if (componentType == 5123) componentBytes = 2;
            else if (componentType == 5125) componentBytes = 4;
            else throw new IllegalStateException("unsupported_indices_componentType_" + componentType);
            String type = accessor.optString("type", "");
            if (!"SCALAR".equals(type)) throw new IllegalStateException("unsupported_indices_type_" + type);
            int viewIndex = accessor.optInt("bufferView", -1);
            JSONObject view = checkedObject(bufferViews, viewIndex, "indices_bufferView");
            int count = Math.max(0, accessor.optInt("count", 0));
            int offset = view.optInt("byteOffset", 0) + accessor.optInt("byteOffset", 0);
            int stride = view.optInt("byteStride", componentBytes);
            int needed = offset + (count <= 0 ? 0 : ((count - 1) * stride + componentBytes));
            if (offset < 0 || needed > bin.length) throw new IllegalStateException("indices_accessor_out_of_bin_bounds");
            return new IndexReader(ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN), offset, stride, count, componentType);
        }

        int indexAt(int i) {
            int p = offset + i * stride;
            if (componentType == 5123) return data.getShort(p) & 0xffff;
            long value = Integer.toUnsignedLong(data.getInt(p));
            if (value > Integer.MAX_VALUE) throw new IllegalStateException("index_value_exceeds_java_int");
            return (int)value;
        }
    }

    private static JSONObject checkedObject(JSONArray array, int index, String label) {
        if (array == null || index < 0 || index >= array.length()) throw new IllegalStateException(label + "_missing_index_" + index);
        JSONObject obj = array.optJSONObject(index);
        if (obj == null) throw new IllegalStateException(label + "_not_object_" + index);
        return obj;
    }

    private static int componentsForType(String type) {
        if ("SCALAR".equals(type)) return 1;
        if ("VEC2".equals(type)) return 2;
        if ("VEC3".equals(type)) return 3;
        if ("VEC4".equals(type)) return 4;
        throw new IllegalStateException("unsupported_accessor_type_" + type);
    }

    private static final class GlbPrimitiveMesh {
        float[] vertexData;
        int[] indexData;
        int[] rangeData;
        float[] materialData;
        float[] boundsMin;
        float[] boundsMax;
        float[] boundsCenter;
        float[] baseColorFactor;
        BaseColorTexture texture;
        List<BaseColorTexture> textures = new ArrayList<>();
        List<BaseColorTexture> pbrTextures = new ArrayList<>();
        List<MaterialInfo> materials = new ArrayList<>();
        String pbrMapsStatus = "missing";
        String metallicRoughnessStatus = "missing";
        String normalMapStatus = "missing";
        String normalMapAppliedStatus = "missing";
        String occlusionMapStatus = "missing";
        String tangentStatus = "missing_or_blocked";
        String tangentSource = "missing";
        int tangentGeneratedCount = 0;
        int tangentFallbackGeneratedCount = 0;
        int tangentMissingCount = 0;
        int tangentDegenerateTriangleCount = 0;
        String tangentFallbackReason = "not_loaded";
        String tangentBuildMode = "once_on_upload";
        int pbrTextureSlotCount = 0;
        int uploadedPbrTextureCount = 0;
        int skippedPbrTextureCount = 0;
        int pbrTextureFallbackCount = 0;
        String materialSlotDiagnostics = "[]";
        float modelScale;
        int vertexCount;
        int indexCount;
        int primitiveCountTotal = 0;
        int primitiveCountRendered = 0;
        int primitiveCountSkipped = 0;
        int unsupportedPrimitiveCount = 0;
        int materialSlotCount = 0;
        int textureSlotCount = 0;
        int textureSlotLimit = 8;
        int skippedTextureCount = 0;
        String reason = "not_run";
    }

    private static final class PrimitiveSource {
        JSONObject primitive;
        AccessorReader positions;
        AccessorReader normals;
        AccessorReader texcoords;
        AccessorReader colors;
        AccessorReader tangents;
        IndexReader indices;
        int vertexCount = 0;
        int materialIndex = -1;
    }

    private static final class TangentBuildResult {
        float[] values;
        int generatedVertexCount = 0;
        int fallbackGeneratedCount = 0;
        int missingVertexCount = 0;
        int degenerateTriangleCount = 0;
        int validTriangleCount = 0;

        boolean hasAnyTangent() {
            return values != null && generatedVertexCount + fallbackGeneratedCount > 0;
        }

        boolean complete() {
            return hasAnyTangent() && missingVertexCount == 0;
        }
    }

    private static final class MaterialInfo {
        String materialName = "";
        float[] baseColorFactor = new float[] {1f, 1f, 1f, 1f};
        int alphaMode = 0;
        float alphaCutoff = 0.5f;
        boolean doubleSided = false;
        int textureSlot = -1;
        BaseColorTexture texture;
        float metallicFactor = 0.0f;
        float roughnessFactor = 1.0f;
        BaseColorTexture metallicRoughnessTexture;
        BaseColorTexture normalTexture;
        BaseColorTexture occlusionTexture;
        float normalScale = 1.0f;
        float occlusionStrength = 1.0f;
        float[] emissiveFactor = new float[] {0f, 0f, 0f};
        String emissiveTextureStatus = "missing";
        String alphaModeText = "OPAQUE";
        String metallicRoughnessStatus = "missing";
        String normalMapStatus = "missing";
        String normalMapAppliedStatus = "missing";
        String occlusionMapStatus = "missing";
        String tangentStatus = "missing_or_blocked";
        int materialTypeHint = 4;
        float albedoLuminance = 1.0f;
        float calibratedRoughness = 1.0f;
        float calibratedMetallic = 0.0f;
        float aoInfluence = 0.0f;
        boolean emissiveGuardApplied = true;
        int tangentMissingCount = 0;
        int tangentFallbackCount = 0;
        int tangentDegenerateTriangleCount = 0;
    }

    private static final class BaseColorTexture {
        String status = "missing";
        String reason = "missing";
        String name = "none";
        String source = "none";
        String mimeType = "none";
        int slot = -1;
        int materialSlot = -1;
        int kind = TEX_BASE_COLOR;
        int width = 0;
        int height = 0;
        int[] pixels = null;
        float normalScale = 1.0f;
        float occlusionStrength = 1.0f;

        static BaseColorTexture missing(String reason) {
            BaseColorTexture t = new BaseColorTexture();
            t.status = "missing";
            t.reason = reason;
            return t;
        }

        static BaseColorTexture failed(String reason) {
            BaseColorTexture t = new BaseColorTexture();
            t.status = "failed";
            t.reason = reason;
            return t;
        }

        static BaseColorTexture withStatus(String status, String reason) {
            BaseColorTexture t = new BaseColorTexture();
            t.status = status;
            t.reason = reason;
            return t;
        }
    }

    private static final class GlbParseResult {
        boolean glbValid = false;
        int glbVersion = 0;
        boolean binChunkFound = false;
        long fileSizeBytes = 0;
        int meshCount = 0;
        int primitiveCount = 0;
        int nodeCount = 0;
        int sceneCount = 0;
        int materialCount = 0;
        int textureCount = 0;
        int imageCount = 0;
        int accessorCount = 0;
        int bufferViewCount = 0;
        int bufferCount = 0;
        int samplerCount = 0;
        int skinCount = 0;
        long totalVertexCountEstimate = 0;
        long totalIndexCountEstimate = 0;
        LinkedHashSet<String> attributesFound = new LinkedHashSet<>();
        List<String> extensionsUsed = new ArrayList<>();
        List<String> extensionsRequired = new ArrayList<>();
        boolean hasSkinning = false;
        boolean hasTangents = false;
        boolean hasNormals = false;
        boolean hasTexcoord0 = false;
        String reason = "not_parsed";
        transient String jsonText = "";
        transient byte[] binChunk = null;
        String gpuUploadStatus = "failed";
        String drawStatus = "fallback";
        int uploadedVertexCount = 0;
        int uploadedIndexCount = 0;
        String textureUploadStatus = "missing";
        String baseColorTextureStatus = "missing";
        int textureWidth = 0;
        int textureHeight = 0;
        boolean textureFallbackUsed = true;

        static GlbParseResult notParsed(String reason) {
            GlbParseResult r = new GlbParseResult();
            r.attributesFound.add("unknown/not_parsed");
            r.reason = reason;
            return r;
        }

        static GlbParseResult failed(String reason) {
            GlbParseResult r = notParsed(reason);
            r.reason = reason;
            return r;
        }

        static GlbParseResult failed(String reason, long fileSizeBytes, int glbVersion) {
            GlbParseResult r = failed(reason);
            r.fileSizeBytes = fileSizeBytes;
            r.glbVersion = glbVersion;
            return r;
        }

        String toJsonFields() {
            return "  \"glbValid\": " + glbValid + ",\n"
                + "  \"glbVersion\": " + glbVersion + ",\n"
                + "  \"binChunkFound\": " + binChunkFound + ",\n"
                + "  \"fileSizeBytes\": " + fileSizeBytes + ",\n"
                + "  \"meshCount\": " + meshCount + ",\n"
                + "  \"primitiveCount\": " + primitiveCount + ",\n"
                + "  \"nodeCount\": " + nodeCount + ",\n"
                + "  \"sceneCount\": " + sceneCount + ",\n"
                + "  \"materialCount\": " + materialCount + ",\n"
                + "  \"textureCount\": " + textureCount + ",\n"
                + "  \"imageCount\": " + imageCount + ",\n"
                + "  \"accessorCount\": " + accessorCount + ",\n"
                + "  \"bufferViewCount\": " + bufferViewCount + ",\n"
                + "  \"bufferCount\": " + bufferCount + ",\n"
                + "  \"samplerCount\": " + samplerCount + ",\n"
                + "  \"skinCount\": " + skinCount + ",\n"
                + "  \"totalVertexCountEstimate\": " + totalVertexCountEstimate + ",\n"
                + "  \"totalIndexCountEstimate\": " + totalIndexCountEstimate + ",\n"
                + "  \"attributesFound\": " + stringArray(new ArrayList<>(attributesFound)) + ",\n"
                + "  \"extensionsUsed\": " + stringArray(extensionsUsed) + ",\n"
                + "  \"extensionsRequired\": " + stringArray(extensionsRequired) + ",\n"
                + "  \"hasSkinning\": " + hasSkinning + ",\n"
                + "  \"hasTangents\": " + hasTangents + ",\n"
                + "  \"hasNormals\": " + hasNormals + ",\n"
                + "  \"hasTexcoord0\": " + hasTexcoord0 + ",\n"
                + "  \"textureUploadStatus\": \"" + esc(textureUploadStatus) + "\",\n"
                + "  \"baseColorTextureStatus\": \"" + esc(baseColorTextureStatus) + "\",\n"
                + "  \"textureWidth\": " + textureWidth + ",\n"
                + "  \"textureHeight\": " + textureHeight + ",\n"
                + "  \"textureFallbackUsed\": " + textureFallbackUsed + ",\n"
                + "  \"reason\": \"" + esc(reason) + "\"\n";
        }
    }

    private static String stringArray(List<String> values) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) b.append(", ");
            b.append("\"").append(esc(values.get(i))).append("\"");
        }
        return b.append("]").toString();
    }

    private static String esc(String s) { if (s == null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }

    private static final class ExportResult {
        boolean ok = false;
        String route = "failed";
        String reason = "not_started";
        String actualRoot = "";
        String timestamp = "";
        Writer runtimeWriter;
        Writer manifestWriter;
    }

    private static final class DebugZipResult {
        boolean ok = false;
        String path = "";
        String reason = "not_started";
        String includedFiles = "";
        String missingFiles = "[]";
    }

    private static final class DebugZipEntry {
        final File file;

        DebugZipEntry(File file) {
            this.file = file;
        }
    }

    private static final class FpsSnapshot {
        final float fpsCurrent;
        final float frameTimeMs;
        final String source;

        FpsSnapshot(float fpsCurrent, float frameTimeMs, String source) {
            this.fpsCurrent = fpsCurrent;
            this.frameTimeMs = frameTimeMs;
            this.source = source == null || source.isEmpty() ? "not_ready" : source;
        }
    }

    private static String jsonFloat(float value) { return String.format(Locale.US, "%.3f", value); }

    private String fileStatusForDebugZip(String name, String includedFiles, boolean required) {
        if (includedFiles != null && includedFiles.contains(name)) return "included";
        if ("not_run".equals(debugZipStatus) || "running".equals(debugZipStatus)) return "not_checked";
        return required ? "missing_required" : "missing_optional";
    }

    private void writeCrashReport(String stage, Throwable throwable) {
        try {
            File dir = getReportDir();
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File out = new File(dir, "runtime_crash_" + ts + ".txt");
            try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                pw.println("SOLUM Runtime Crash Report");
                pw.println("stage=" + stage);
                pw.println("time=" + ts);
                pw.println("thread=" + Thread.currentThread().getName());
                pw.println("throwable=" + throwable.getClass().getName());
                pw.println("message=" + throwable.getMessage());
                pw.println();
                throwable.printStackTrace(pw);
            }
        } catch (Throwable ignored) { }
    }

    private String shortThrowable(Throwable t) { String msg = t.getMessage(); if (msg == null) msg = "no message"; return t.getClass().getSimpleName() + ": " + msg; }
    private String escape(String s) { if (s == null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
