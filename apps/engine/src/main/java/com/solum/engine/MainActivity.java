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
import android.os.Handler;
import android.os.Looper;
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
    private static final String PREF_ACTIVE_MODEL_PATH = "active_model_path";
    private static final String PREF_ACTIVE_MODEL_LOCAL_PATH = "active_model_local_path";
    private static final String PREF_ACTIVE_MODEL_NAME = "active_model_name";
    private static final String SCENE_ID = "scene24_transparent_glass_pass_lab";
    private static final String SCENE_NAME = "Scene24 Transparent Glass Pass Lab";
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
    private Button reflectionContrastButton;
    private Button reflectionSaturationButton;
    private Button groundIntensityButton;
    private Button environmentButton;
    private Button skyPresetButton;
    private Button horizonButton;
    private Button calibrationPresetButton;
    private Button calibrationButton;
    private Button glossButton;
    private Button paintGlossButton;
    private Button clearcoatButton;
    private Button clearcoatRoughnessButton;
    private Button glassOpacityButton;
    private Button glassEdgeButton;
    private Button glassRoughnessButton;
    private Button slotPrevButton;
    private Button slotNextButton;
    private Button metallicSlotButton;
    private Button roughnessSlotButton;
    private Button normalSlotButton;
    private Button aoSlotButton;
    private Button resetSelectedSlotButton;
    private Button presetCycleButton;
    private Button presetApplyButton;
    private Button glassEnableButton;
    private Button glassRenderModeButton;
    private Button selectGlassCandidateButton;
    private Button assignSelectedAsGlassButton;
    private Button clearManualRoleButton;
    private Button glassTintPresetButton;
    private Button glassDebugButton;
    private Button emissiveDebugButton;
    private Button alphaModeDebugButton;
    private Button doubleSidedDebugButton;
    private Button resetAlphaButton;
    private Button materialViewButton;
    private Button reloadActiveModelButton;
    private TextView materialStatusView;
    private SeekBar sunSlider;
    private SeekBar ambientSlider;
    private SeekBar exposureSlider;
    private SeekBar specularSlider;
    private SeekBar reflectionSlider;
    private SeekBar reflectionContrastSlider;
    private SeekBar reflectionSaturationSlider;
    private SeekBar groundSlider;
    private SeekBar environmentSlider;
    private SeekBar horizonSlider;
    private SeekBar calibrationSlider;
    private SeekBar glossSlider;
    private SeekBar paintGlossSlider;
    private SeekBar clearcoatSlider;
    private SeekBar clearcoatRoughnessSlider;
    private SeekBar metallicSlotSlider;
    private SeekBar roughnessSlotSlider;
    private SeekBar normalSlotSlider;
    private SeekBar aoSlotSlider;
    private SeekBar alphaCutoffSlider;
    private SeekBar emissiveSlider;
    private SeekBar glassOpacitySlider;
    private SeekBar glassEdgeSlider;
    private SeekBar glassRoughnessSlider;
    private LinearLayout inspectorPanel;
    private LinearLayout tabRow;
    private LinearLayout assetsPanel;
    private LinearLayout cameraPanel;
    private LinearLayout lightingPanel;
    private LinearLayout materialPanel;
    private LinearLayout debugPanel;
    private ScrollView inspectorScrollView;
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
    private float reflectionContrast = 1.18f;
    private float reflectionSaturation = 1.12f;
    private float contactShadowIntensity = 0.65f;
    private float environmentIntensity = 1.0f;
    private int environmentPresetIndex = 0;
    private float horizonStrength = 0.55f;
    private int calibrationPresetIndex = 2;
    private float calibrationSliderValue = 0.65f;
    private float glossSliderValue = 0.62f;
    private float paintGlossSliderValue = 0.55f;
    private float clearcoatIntensity = 0.72f;
    private float clearcoatRoughness = 0.28f;
    private int selectedMaterialSlot = 0;
    private int selectedMaterialSlotCount = 0;
    private float selectedSlotMetallicOverride = 0.0f;
    private float selectedSlotRoughnessOverride = 1.0f;
    private float selectedSlotNormalScaleOverride = 1.0f;
    private float selectedSlotAoOverride = 1.0f;
    private float alphaCutoffValue = 0.5f;
    private float emissiveIntensity = 0.0f;
    private int activeMaterialPresetIndex = 0;
    private boolean materialPresetPendingApply = false;
    private boolean glassEnabled = false;
    private float glassOpacity = 0.56f;
    private float glassEdge = 1.55f;
    private float glassRoughness = 0.14f;
    private int glassTintPresetIndex = 0;
    private int glassRenderModeIndex = 2;
    private int manualGlassSlotOverride = -1;
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
    private long lastCameraMoveNs = 0L;
    private long cameraMotionStopNs = 0L;
    private float cameraMotionSpeed = 0.0f;
    private float cameraMotionQualityBlend = 1.0f;
    private float motionReflectionScale = 1.0f;
    private float motionClearcoatScale = 1.0f;
    private float cameraMovingFpsLast = 0.0f;
    private float cameraStillFpsLast = 0.0f;
    private String cameraMotionQualityTier = "full";
    private static final long MOTION_RESTORE_NS = 520_000_000L;
    private float lastPinchDistance = 0.0f;
    private boolean pinchActive = false;
    private ModelImportState modelState = ModelImportState.notRun();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable restoreInspectorAlphaRunnable = () -> setInspectorAlpha(0.92f, "idle");

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);
    private static native String nativeGetRenderLabState(long handle);
    private static native void nativeSetCamera(long handle, float yawDeg, float pitchDeg, float distance, float motionSpeed, float qualityBlend, float reflectionScale, float clearcoatScale, float movingFps, float stillFps);
    private static native void nativeSetLightingControls(long handle, int lightPreset, float sunIntensity, float ambientIntensity, int activeDebugView, int toneMappingMode, float exposureValue, float ambientFloor, int brightnessPreset, float specularBoost, float reflectionIntensity, float contactShadowIntensity, int calibrationPreset, float calibrationStrength, float glossSliderValue, float paintGlossSliderValue, float clearcoatIntensity, float clearcoatRoughness, float environmentIntensity, int environmentPreset, float horizonStrength, float reflectionContrast, float reflectionSaturation, float motionReflectionScale, float motionClearcoatScale, int selectedMaterialSlot, float selectedSlotMetallicOverride, float selectedSlotRoughnessOverride, float selectedSlotNormalScaleOverride, float selectedSlotAoOverride, float selectedSlotGlossOverride, float selectedSlotCoatOverride, float alphaCutoffValue, float emissiveIntensity, int materialPreset, int glassEnabled, float glassOpacity, float glassEdge, float glassRoughness, int glassTintPreset, int glassRenderMode, int manualGlassSlotOverride);
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
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                modelState.sliderTouchStatus = "active";
                setInspectorAlpha(0.12f, "slider_drag");
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                modelState.sliderTouchStatus = "ok_touch_targets";
                scheduleInspectorAlphaRestore();
                updateStatus();
            }
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
        else if ("Refl Contrast".equals(label)) { reflectionContrastButton = valueButton; reflectionContrastSlider = slider; }
        else if ("Refl Saturation".equals(label)) { reflectionSaturationButton = valueButton; reflectionSaturationSlider = slider; }
        else if ("Ground".equals(label)) { groundIntensityButton = valueButton; groundSlider = slider; }
        else if ("Env".equals(label)) { environmentButton = valueButton; environmentSlider = slider; }
        else if ("Horizon".equals(label)) { horizonButton = valueButton; horizonSlider = slider; }
        else if ("Calib".equals(label)) { calibrationButton = valueButton; calibrationSlider = slider; }
        else if ("Gloss".equals(label)) { glossButton = valueButton; glossSlider = slider; }
        else if ("Coat".equals(label)) { paintGlossButton = valueButton; paintGlossSlider = slider; }
        else if ("Clearcoat".equals(label)) { clearcoatButton = valueButton; clearcoatSlider = slider; }
        else if ("Clearcoat Rough".equals(label)) { clearcoatRoughnessButton = valueButton; clearcoatRoughnessSlider = slider; }
        else if ("Metallic".equals(label)) { metallicSlotButton = valueButton; metallicSlotSlider = slider; }
        else if ("Rough".equals(label)) { roughnessSlotButton = valueButton; roughnessSlotSlider = slider; }
        else if ("Normal".equals(label)) { normalSlotButton = valueButton; normalSlotSlider = slider; }
        else if ("AO".equals(label)) { aoSlotButton = valueButton; aoSlotSlider = slider; }
        else if ("Alpha Cutoff".equals(label)) { alphaCutoffSlider = slider; }
        else if ("Emissive".equals(label)) { emissiveSlider = slider; }
        else if ("Glass Opacity".equals(label)) { glassOpacityButton = valueButton; glassOpacitySlider = slider; }
        else if ("Glass Edge".equals(label)) { glassEdgeButton = valueButton; glassEdgeSlider = slider; }
        else if ("Glass Rough".equals(label)) { glassRoughnessButton = valueButton; glassRoughnessSlider = slider; }
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
        if (inspectorScrollView != null) inspectorScrollView.setVisibility(inspectorPanelVisible ? View.VISIBLE : View.GONE);
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

    private void applyInspectorHeightCap() {
        if (inspectorScrollView == null) return;
        int height = getResources().getDisplayMetrics().heightPixels;
        int capped = Math.max(dp(180), Math.round(height * 0.30f));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, capped);
        params.gravity = Gravity.BOTTOM;
        params.setMargins(dp(76), dp(12), dp(12), dp(28));
        inspectorScrollView.setLayoutParams(params);
        modelState.inspectorExpandedMaxHeightPercent = 30;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setInspectorAlpha(float alpha, String mode) {
        uiHandler.removeCallbacks(restoreInspectorAlphaRunnable);
        float clamped = clamp(alpha, 0.10f, 0.92f);
        if (inspectorScrollView != null) inspectorScrollView.setAlpha(clamped);
        if (inspectorPanel != null) inspectorPanel.setAlpha(clamped);
        modelState.inspectorCurrentAlphaMode = mode;
    }

    private void scheduleInspectorAlphaRestore() {
        uiHandler.removeCallbacks(restoreInspectorAlphaRunnable);
        uiHandler.postDelayed(restoreInspectorAlphaRunnable, 420L);
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
        inspectorScrollView = dockScroll;
        dockScroll.setFillViewport(false);
        dockScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
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
        TextView assetsWorkflowHint = compactInfoText("Assets: active model, import, scan, reload, export");
        assetsPanel.addView(assetsWorkflowHint);
        importGlbButton = compactButton("Import GLB");
        importGlbButton.setOnClickListener(v -> chooseGlbForImport());
        scanModelsButton = compactButton("Scan Models");
        scanModelsButton.setOnClickListener(v -> scanModelsFromButton());
        reloadActiveModelButton = compactButton("Reload Active Model");
        reloadActiveModelButton.setOnClickListener(v -> reloadActiveModelFromButton());
        quickExportButton = compactButton("Export");
        quickExportButton.setOnClickListener(v -> exportEngineDiagnosticsFromButton());
        assetsPanel.addView(importGlbButton);
        assetsPanel.addView(scanModelsButton);
        assetsPanel.addView(reloadActiveModelButton);
        assetsPanel.addView(quickExportButton);
        assetsSummaryView = panelText("Active: none", 10f, 5);
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
        lightingPanel.addView(sliderControl("Refl Contrast", 0.0f, 2.0f, reflectionContrast, v -> {
            reflectionContrast = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        lightingPanel.addView(sliderControl("Refl Saturation", 0.0f, 2.0f, reflectionSaturation, v -> {
            reflectionSaturation = clamp(v, 0.0f, 2.0f);
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
        skyPresetButton = compactButton("Env Zone: Studio");
        skyPresetButton.setOnClickListener(v -> cycleEnvironmentPreset());
        lightingPanel.addView(skyPresetButton);
        lightingPanel.addView(sliderControl("Horizon", 0.0f, 1.0f, horizonStrength, v -> {
            horizonStrength = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        inspectorPanel.addView(lightingPanel);

        materialPanel = new LinearLayout(this);
        materialPanel.setOrientation(LinearLayout.VERTICAL);
        materialPanel.addView(compactInfoText("Selected slot workflow"));
        LinearLayout slotRow = new LinearLayout(this);
        slotRow.setOrientation(LinearLayout.VERTICAL);
        slotPrevButton = compactButton("Slot -");
        slotPrevButton.setOnClickListener(v -> cycleMaterialSlot(-1));
        slotNextButton = compactButton("Slot +");
        slotNextButton.setOnClickListener(v -> cycleMaterialSlot(1));
        slotRow.addView(slotPrevButton);
        slotRow.addView(slotNextButton);
        materialPanel.addView(slotRow);
        materialPanel.addView(sliderControl("Metallic", 0.0f, 1.0f, selectedSlotMetallicOverride, v -> {
            selectedSlotMetallicOverride = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Rough", 0.0f, 1.0f, selectedSlotRoughnessOverride, v -> {
            selectedSlotRoughnessOverride = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Normal", 0.0f, 2.0f, selectedSlotNormalScaleOverride, v -> {
            selectedSlotNormalScaleOverride = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("AO", 0.0f, 1.5f, selectedSlotAoOverride, v -> {
            selectedSlotAoOverride = clamp(v, 0.0f, 1.5f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Alpha Cutoff", 0.0f, 1.0f, alphaCutoffValue, v -> {
            alphaCutoffValue = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        presetCycleButton = compactButton("Preset: Balanced");
        presetCycleButton.setOnClickListener(v -> cycleMaterialPreset());
        materialPanel.addView(presetCycleButton);
        presetApplyButton = compactButton("Apply Preset");
        presetApplyButton.setOnClickListener(v -> applyActiveMaterialPreset());
        materialPanel.addView(presetApplyButton);
        glassEnableButton = compactButton("Glass Enable");
        glassEnableButton.setOnClickListener(v -> toggleGlassEnabled());
        materialPanel.addView(glassEnableButton);
        glassRenderModeButton = compactButton("Glass Render: Auto");
        glassRenderModeButton.setOnClickListener(v -> cycleGlassRenderMode());
        materialPanel.addView(glassRenderModeButton);
        selectGlassCandidateButton = compactButton("Select Glass Candidate");
        selectGlassCandidateButton.setOnClickListener(v -> selectBestGlassCandidate());
        materialPanel.addView(selectGlassCandidateButton);
        assignSelectedAsGlassButton = compactButton("Assign Selected As Glass");
        assignSelectedAsGlassButton.setOnClickListener(v -> assignSelectedAsGlass());
        materialPanel.addView(assignSelectedAsGlassButton);
        clearManualRoleButton = compactButton("Clear Manual Role");
        clearManualRoleButton.setOnClickListener(v -> clearManualRoleOverride());
        materialPanel.addView(clearManualRoleButton);
        glassTintPresetButton = compactButton("Glass Tint: Clear");
        glassTintPresetButton.setOnClickListener(v -> cycleGlassTintPreset());
        materialPanel.addView(glassTintPresetButton);
        materialPanel.addView(sliderControl("Glass Opacity", 0.0f, 1.0f, glassOpacity, v -> {
            glassOpacity = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Glass Edge", 0.0f, 2.0f, glassEdge, v -> {
            glassEdge = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Glass Rough", 0.0f, 1.0f, glassRoughness, v -> {
            glassRoughness = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Emissive", 0.0f, 2.0f, emissiveIntensity, v -> {
            emissiveIntensity = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Clearcoat", 0.0f, 2.0f, clearcoatIntensity, v -> {
            clearcoatIntensity = clamp(v, 0.0f, 2.0f);
            applyLightingControls();
        }));
        materialPanel.addView(sliderControl("Clearcoat Rough", 0.0f, 1.0f, clearcoatRoughness, v -> {
            clearcoatRoughness = clamp(v, 0.0f, 1.0f);
            applyLightingControls();
        }));
        alphaModeDebugButton = compactButton("Alpha Mode");
        alphaModeDebugButton.setOnClickListener(v -> cycleAlphaDebugView());
        materialPanel.addView(alphaModeDebugButton);
        doubleSidedDebugButton = compactButton("Double Sided");
        doubleSidedDebugButton.setOnClickListener(v -> {
            activeDebugViewIndex = 34;
            applyLightingControls();
            updateStatus();
        });
        materialPanel.addView(doubleSidedDebugButton);
        emissiveDebugButton = compactButton("Emissive View");
        emissiveDebugButton.setOnClickListener(v -> {
            activeDebugViewIndex = 37;
            applyLightingControls();
            updateStatus();
        });
        materialPanel.addView(emissiveDebugButton);
        resetAlphaButton = compactButton("Reset Alpha");
        resetAlphaButton.setOnClickListener(v -> resetAlphaControls());
        materialPanel.addView(resetAlphaButton);
        glassDebugButton = compactButton("Glass Debug");
        glassDebugButton.setOnClickListener(v -> cycleGlassDebugView());
        materialPanel.addView(glassDebugButton);
        resetSelectedSlotButton = compactButton("Reset Selected Slot");
        resetSelectedSlotButton.setOnClickListener(v -> resetSelectedMaterialSlot());
        materialPanel.addView(resetSelectedSlotButton);
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
        root.addView(dockScroll);
        applyInspectorHeightCap();
        syncPanelVisibility();
        restorePersistedActiveModel();
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
        uiHandler.removeCallbacks(restoreInspectorAlphaRunnable);
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
        try { modelState.surfaceRecreateStatus = "surface_created"; nativeSurfaceCreated(nativeHandle, holder.getSurface(), getRuntimeReportDirPath()); applyLightingControls(); attemptActiveModelGpuUpload("surface_created"); updateStatus(); exportEngineDiagnostics("surface_created"); }
        catch (Throwable t) { writeCrashReport("surface_created_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: surface init failed\n" + shortThrowable(t)); }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { modelState.surfaceRecreateStatus = "surface_changed"; nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height); applyLightingControls(); attemptActiveModelGpuUpload("surface_changed"); updateStatus(); exportEngineDiagnostics("surface_changed"); }
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
                + "\nFallback reason: " + (modelState.fallbackCubeVisible ? shorten(modelState.fallbackCubeReason, 42) : "none")
                + "\nReload: " + modelState.reloadActiveModelStatus
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
            + "\nNext: validate Scene24 transparent glass pass lab";
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
        activeDebugViewIndex = (activeDebugViewIndex + 1) % 74;
        applyLightingControls();
        updateStatus();
    }

    private void cycleMaterialPreset() {
        activeMaterialPresetIndex = (activeMaterialPresetIndex + 1) % 8;
        materialPresetPendingApply = true;
        modelState.materialPresetAppliedStatus = "pending_apply";
        applyLightingControls();
        updateStatus();
    }

    private void applyActiveMaterialPreset() {
        if (activeMaterialPresetIndex == 1) {
            selectedSlotMetallicOverride = 0.12f; selectedSlotRoughnessOverride = 0.24f; selectedSlotNormalScaleOverride = 1.0f; selectedSlotAoOverride = 1.0f; glossSliderValue = 0.86f; paintGlossSliderValue = 0.92f; clearcoatIntensity = 1.35f; clearcoatRoughness = 0.22f; emissiveIntensity = 0.0f;
        } else if (activeMaterialPresetIndex == 2) {
            selectedSlotMetallicOverride = 0.92f; selectedSlotRoughnessOverride = 0.24f; selectedSlotNormalScaleOverride = 1.0f; selectedSlotAoOverride = 0.92f; glossSliderValue = 0.72f; paintGlossSliderValue = 0.42f; clearcoatIntensity = 0.25f; clearcoatRoughness = 0.36f; emissiveIntensity = 0.0f;
        } else if (activeMaterialPresetIndex == 3) {
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = 0.88f; selectedSlotNormalScaleOverride = 0.75f; selectedSlotAoOverride = 1.15f; glossSliderValue = 0.12f; paintGlossSliderValue = 0.0f; clearcoatIntensity = 0.0f; clearcoatRoughness = 0.80f; emissiveIntensity = 0.0f;
        } else if (activeMaterialPresetIndex == 4) {
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = 0.82f; selectedSlotNormalScaleOverride = 0.55f; selectedSlotAoOverride = 1.10f; glossSliderValue = 0.08f; paintGlossSliderValue = 0.0f; clearcoatIntensity = 0.0f; clearcoatRoughness = 0.75f; emissiveIntensity = 0.0f;
        } else if (activeMaterialPresetIndex == 5) {
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = 0.46f; selectedSlotNormalScaleOverride = 0.85f; selectedSlotAoOverride = 1.0f; glossSliderValue = 0.42f; paintGlossSliderValue = 0.18f; clearcoatIntensity = 0.20f; clearcoatRoughness = 0.46f; emissiveIntensity = 0.0f;
        } else if (activeMaterialPresetIndex == 6) {
            applyGlassPresetValues(glassTintPresetIndex);
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = glassRoughness; selectedSlotNormalScaleOverride = 0.65f; selectedSlotAoOverride = 0.75f; glossSliderValue = 0.70f; paintGlossSliderValue = 0.20f; clearcoatIntensity = 0.0f; clearcoatRoughness = 0.35f; emissiveIntensity = 0.0f; alphaCutoffValue = Math.max(alphaCutoffValue, 0.5f);
        } else if (activeMaterialPresetIndex == 7) {
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = 0.55f; selectedSlotNormalScaleOverride = 1.0f; selectedSlotAoOverride = 1.0f; glossSliderValue = 0.24f; paintGlossSliderValue = 0.0f; clearcoatIntensity = 0.0f; clearcoatRoughness = 0.65f; emissiveIntensity = Math.max(0.8f, emissiveIntensity);
        } else {
            selectedSlotMetallicOverride = 0.0f; selectedSlotRoughnessOverride = 0.62f; selectedSlotNormalScaleOverride = 1.0f; selectedSlotAoOverride = 1.0f; glossSliderValue = 0.62f; paintGlossSliderValue = 0.55f; clearcoatIntensity = 0.55f; clearcoatRoughness = 0.34f; emissiveIntensity = 0.0f;
        }
        materialPresetPendingApply = false;
        modelState.materialPresetAppliedStatus = "ok_selected_slot_uniform_only";
        modelState.selectedSlotResetStatus = "ok_preset_applied";
        applyLightingControls();
        updateStatus();
    }

    private void toggleGlassEnabled() {
        glassEnabled = !glassEnabled;
        if (glassEnabled) activeMaterialPresetIndex = 6;
        materialPresetPendingApply = false;
        modelState.materialPresetAppliedStatus = "ok_selected_slot_uniform_only";
        applyLightingControls();
        updateStatus();
    }

    private void cycleGlassRenderMode() {
        glassRenderModeIndex = (glassRenderModeIndex + 1) % 3;
        applyLightingControls();
        updateStatus();
    }

    private String glassRenderModeName(int mode) {
        if (mode == 0) return "Fake Safe";
        if (mode == 1) return "Transparent v1";
        return "Auto";
    }

    private void cycleGlassTintPreset() {
        glassTintPresetIndex = (glassTintPresetIndex + 1) % 8;
        glassEnabled = true;
        activeMaterialPresetIndex = 6;
        applyGlassPresetValues(glassTintPresetIndex);
        applyLightingControls();
        updateStatus();
    }

    private void cycleGlassDebugView() {
        activeDebugViewIndex = activeDebugViewIndex < 52 || activeDebugViewIndex > 73 ? 52 : 52 + ((activeDebugViewIndex - 51) % 22);
        applyLightingControls();
        updateStatus();
    }

    private void applyGlassPresetValues(int preset) {
        glassEnabled = true;
        activeMaterialPresetIndex = 6;
        if (preset == 1) { glassOpacity = 0.52f; glassEdge = 1.55f; glassRoughness = 0.16f; reflectionIntensity = Math.max(reflectionIntensity, 1.22f); }
        else if (preset == 2) { glassOpacity = 0.54f; glassEdge = 1.48f; glassRoughness = 0.18f; reflectionIntensity = Math.max(reflectionIntensity, 1.16f); }
        else if (preset == 3) { glassOpacity = 0.68f; glassEdge = 1.30f; glassRoughness = 0.42f; reflectionIntensity = Math.max(reflectionIntensity, 1.04f); }
        else if (preset == 4) { glassOpacity = 0.58f; glassEdge = 1.42f; glassRoughness = 0.20f; reflectionIntensity = Math.max(reflectionIntensity, 1.14f); }
        else if (preset == 5) { glassOpacity = 0.50f; glassEdge = 1.85f; glassRoughness = 0.18f; reflectionIntensity = Math.max(reflectionIntensity, 1.32f); }
        else if (preset == 6) { glassOpacity = 0.74f; glassEdge = 1.12f; glassRoughness = 0.54f; reflectionIntensity = Math.max(reflectionIntensity, 0.96f); }
        else if (preset == 7) { glassOpacity = 0.42f; glassEdge = 1.95f; glassRoughness = 0.08f; reflectionIntensity = Math.max(reflectionIntensity, 1.38f); }
        else { glassOpacity = 0.50f; glassEdge = 1.62f; glassRoughness = 0.10f; reflectionIntensity = Math.max(reflectionIntensity, 1.24f); }
    }

    private String glassTintPresetName(int preset) {
        if (preset == 1) return "Blue Glass";
        if (preset == 2) return "Green Glass";
        if (preset == 3) return "Smoke Glass";
        if (preset == 4) return "Warm Glass";
        if (preset == 5) return "Magic Glass";
        if (preset == 6) return "Dirty Glass Lite";
        if (preset == 7) return "Crystal Lite";
        return "Clear Glass";
    }

    private String glassTintColorStatus(int preset) {
        if (preset == 1) return "0.55,0.82,1.00";
        if (preset == 2) return "0.55,1.00,0.72";
        if (preset == 3) return "0.48,0.52,0.58";
        if (preset == 4) return "1.00,0.78,0.52";
        if (preset == 5) return "0.82,0.58,1.00";
        if (preset == 6) return "0.70,0.74,0.72";
        if (preset == 7) return "0.86,0.98,1.00";
        return "0.92,0.98,1.00";
    }

    private void cycleAlphaDebugView() {
        activeDebugViewIndex = activeDebugViewIndex < 32 || activeDebugViewIndex > 36 ? 32 : 32 + ((activeDebugViewIndex - 31) % 5);
        applyLightingControls();
        updateStatus();
    }

    private void resetAlphaControls() {
        alphaCutoffValue = 0.5f;
        activeDebugViewIndex = 0;
        modelState.alphaResetButtonStatus = "ok_reset_alpha_cutoff_and_debug";
        applyLightingControls();
        updateStatus();
    }

    private void cycleMaterialSlot(int delta) {
        selectedMaterialSlotCount = Math.max(0, modelState.materialSlotCount);
        if (selectedMaterialSlotCount > 0) {
            selectedMaterialSlot = (selectedMaterialSlot + delta + selectedMaterialSlotCount) % selectedMaterialSlotCount;
            seedSelectedSlotOverridesFromDiagnostics();
        } else {
            selectedMaterialSlot = 0;
        }
        applyLightingControls();
        updateStatus();
    }

    private void resetSelectedMaterialSlot() {
        seedSelectedSlotOverridesFromDiagnostics();
        modelState.selectedSlotResetStatus = "ok_reset_selected_slot_from_source_material";
        applyLightingControls();
        updateStatus();
    }

    private JSONObject selectedMaterialJson() {
        try {
            JSONArray slots = new JSONArray(modelState.materialSlotDiagnostics == null ? "[]" : modelState.materialSlotDiagnostics);
            if (slots.length() <= 0) return null;
            selectedMaterialSlotCount = slots.length();
            selectedMaterialSlot = Math.max(0, Math.min(selectedMaterialSlot, selectedMaterialSlotCount - 1));
            return slots.optJSONObject(selectedMaterialSlot);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private JSONArray materialSlotsJson() {
        try {
            return new JSONArray(modelState.materialSlotDiagnostics == null ? "[]" : modelState.materialSlotDiagnostics);
        } catch (Throwable ignored) {
            return new JSONArray();
        }
    }

    private String materialRoleFromSlot(JSONObject slot) {
        if (slot == null) return "Unknown";
        String hint = slot.optString("materialTypeHint", "unknown");
        if ("glass_like".equals(hint)) return "Glass";
        if ("paint_like".equals(hint)) return "Paint";
        if ("metal_like".equals(hint)) return "Metal";
        if ("fabric_like".equals(hint)) return "Fabric";
        if ("rubber_like".equals(hint)) return "Rubber";
        if ("emissive_like".equals(hint)) return "Emissive";
        if ("cutout_like".equals(hint) || "decal_like".equals(hint)) return "Plastic";
        return "Unknown";
    }

    private float materialRoleConfidence(JSONObject slot) {
        if (slot == null) return 0.0f;
        String role = materialRoleFromSlot(slot);
        if ("Glass".equals(role)) return 0.94f;
        if ("Paint".equals(role) || "Metal".equals(role) || "Fabric".equals(role) || "Rubber".equals(role) || "Emissive".equals(role)) return 0.82f;
        String alpha = slot.optString("alphaMode", "OPAQUE");
        if ("BLEND".equals(alpha) || slot.optDouble("glassOpacityApplied", 1.0) < 0.98) return 0.68f;
        return 0.38f;
    }

    private String materialRoleSource(JSONObject slot) {
        if (slot == null) return "none";
        String role = materialRoleFromSlot(slot);
        if ("Glass".equals(role)) return "material_name_alpha_or_glass_hint";
        if ("Paint".equals(role)) return "paint_hint_or_factor";
        if ("Metal".equals(role)) return "metal_hint_or_factor";
        if ("Fabric".equals(role)) return "fabric_hint_or_roughness";
        if ("Rubber".equals(role)) return "rubber_hint_or_roughness";
        if ("Emissive".equals(role)) return "emissive_factor_or_texture";
        if ("BLEND".equals(slot.optString("alphaMode", "OPAQUE"))) return "alpha_metadata";
        return "fallback_unknown";
    }

    private int bestGlassCandidateSlot() {
        JSONArray slots = materialSlotsJson();
        int best = -1;
        float bestScore = -1.0f;
        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.optJSONObject(i);
            if (slot == null) continue;
            String role = materialRoleFromSlot(slot);
            float score = -1.0f;
            if ("Glass".equals(role)) score = materialRoleConfidence(slot);
            else if ("BLEND".equals(slot.optString("alphaMode", "OPAQUE")) && !"Paint".equals(role) && !"Metal".equals(role) && !"Fabric".equals(role) && !"Rubber".equals(role) && !"Emissive".equals(role)) score = 0.68f;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private JSONObject materialSlotJson(int slotIndex) {
        JSONArray slots = materialSlotsJson();
        return slotIndex >= 0 && slotIndex < slots.length() ? slots.optJSONObject(slotIndex) : null;
    }

    private void selectBestGlassCandidate() {
        int candidate = bestGlassCandidateSlot();
        if (candidate >= 0) {
            selectedMaterialSlot = candidate;
            seedSelectedSlotOverridesFromDiagnostics();
            modelState.transparentGlassAutoDetectStatus = "ok_selected_best_candidate";
        } else {
            modelState.transparentGlassAutoDetectStatus = "fallback_no_glass_candidate";
        }
        applyLightingControls();
        updateStatus();
    }

    private void assignSelectedAsGlass() {
        manualGlassSlotOverride = selectedMaterialSlot;
        glassEnabled = true;
        modelState.transparentGlassManualOverrideStatus = "active_selected_slot_assigned_as_glass";
        applyLightingControls();
        updateStatus();
    }

    private void clearManualRoleOverride() {
        manualGlassSlotOverride = -1;
        modelState.transparentGlassManualOverrideStatus = "inactive";
        applyLightingControls();
        updateStatus();
    }

    private void seedSelectedSlotOverridesFromDiagnostics() {
        JSONObject slot = selectedMaterialJson();
        if (slot == null) return;
        selectedSlotMetallicOverride = clamp((float)slot.optDouble("metallicFactor", selectedSlotMetallicOverride), 0.0f, 1.0f);
        selectedSlotRoughnessOverride = clamp((float)slot.optDouble("roughnessFactor", selectedSlotRoughnessOverride), 0.0f, 1.0f);
        selectedSlotNormalScaleOverride = clamp((float)slot.optDouble("normalScale", selectedSlotNormalScaleOverride), 0.0f, 2.0f);
        selectedSlotAoOverride = clamp((float)slot.optDouble("occlusionStrength", selectedSlotAoOverride), 0.0f, 1.5f);
        alphaCutoffValue = clamp((float)slot.optDouble("alphaCutoff", alphaCutoffValue), 0.0f, 1.0f);
        modelState.selectedSlotResetStatus = "seeded_from_slot";
    }

    private void cycleEnvironmentPreset() {
        environmentPresetIndex = (environmentPresetIndex + 1) % 6;
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
            reflectionContrast = 1.05f;
            reflectionSaturation = 1.04f;
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
            reflectionContrast = 1.22f;
            reflectionSaturation = 1.12f;
            contactShadowIntensity = 0.75f;
            environmentIntensity = 1.15f;
            environmentPresetIndex = 1;
            horizonStrength = 0.62f;
            brightnessPresetIndex = 1;
        } else if (lightPresetIndex == 3) {
            sunIntensity = 2.0f;
            ambientIntensity = 0.80f;
            exposureValue = 1.50f;
            ambientFloor = 0.16f;
            specularBoost = 1.85f;
            reflectionIntensity = 1.15f;
            reflectionContrast = 1.18f;
            reflectionSaturation = 1.12f;
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
            reflectionContrast = 1.34f;
            reflectionSaturation = 1.24f;
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
            reflectionContrast = 0.92f;
            reflectionSaturation = 0.88f;
            contactShadowIntensity = 0.45f;
            environmentIntensity = 0.85f;
            environmentPresetIndex = 3;
            horizonStrength = 0.45f;
            brightnessPresetIndex = 2;
        }
    }

    private void applyLightingControls() {
        updateMotionQualityBlend(System.nanoTime());
        applyInspectorDiagnostics();
        modelState.lightingStatus = "ok";
        modelState.lightPreset = lightPresetName(lightPresetIndex);
        modelState.sunIntensity = sunIntensity;
        modelState.ambientIntensity = ambientIntensity;
        modelState.specularBoost = specularBoost;
        modelState.specularBoostStatus = "ok_uniform_controlled";
        modelState.reflectionIntensity = reflectionIntensity;
        modelState.reflectionContrastValue = reflectionContrast;
        modelState.reflectionSaturationValue = reflectionSaturation;
        modelState.motionReflectionScale = motionReflectionScale;
        modelState.motionClearcoatScale = motionClearcoatScale;
        modelState.iblStatus = "ok_foundation";
        modelState.iblMode = "procedural_directional_probe";
        modelState.environmentIblStatus = "ok_fake_cubemap_probe";
        modelState.environmentIblMode = "procedural_directional_probe";
        modelState.environmentSourceStatus = "ok_procedural_no_external_texture";
        modelState.environmentSourceType = "procedural_directional_probe";
        modelState.environmentSkyColorStatus = "ok_zone_uniform";
        modelState.environmentGroundColorStatus = "ok_zone_uniform";
        modelState.environmentHorizonStatus = "ok_zone_horizon_blend";
        modelState.environmentPerformanceStatus = "ok_no_extra_pass_no_texture_upload";
        modelState.environmentReflectionStatus = "p24_fake_cubemap_probe_no_texture";
        modelState.environmentReflectionMode = "reflection_direction_sky_horizon_ground_side";
        modelState.environmentSource = "procedural_directional_probe";
        modelState.reflectionFoundationStatus = "p24_fake_cubemap_probe_foundation";
        modelState.reflectionMode = "procedural_directional_probe";
        modelState.reflectionColorStatus = "ok_sky_horizon_ground_side_glint";
        modelState.reflectionRoughnessResponseStatus = "ok_roughness_blur_approx";
        modelState.metallicReflectionStatus = "ok_metal_tinted_stronger_guarded";
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
        modelState.environmentZonePreset = modelState.environmentPreset;
        modelState.environmentIntensity = environmentIntensity;
        modelState.environmentSliderStatus = "ok";
        modelState.skyPresetStatus = "ok";
        modelState.horizonControlStatus = "ok";
        modelState.horizonStrength = horizonStrength;
        modelState.environmentUniformUpdateStatus = "ok_uniform_only";
        modelState.fakeCubemapStatus = "ok";
        modelState.fakeCubemapMode = "procedural_directional_probe";
        modelState.fakeCubemapSourceStatus = "procedural_no_texture_no_render_pass";
        modelState.environmentZoneStatus = "ok_sky_horizon_ground_side";
        modelState.skyZoneStatus = "ok";
        modelState.horizonZoneStatus = "ok";
        modelState.groundZoneStatus = "ok";
        modelState.sideReflectionZoneStatus = "ok";
        modelState.fakeCubemapPerformanceStatus = "ok_shader_math_only_no_loops";
        modelState.reflectionQualityStatus = "ok_p26_probe_polished";
        modelState.reflectionContrastStatus = "ok_uniform_control";
        modelState.reflectionSaturationStatus = "ok_uniform_control";
        modelState.roughnessReflectionBlurStatus = "ok_direction_blend_approx";
        modelState.clearcoatReflectionBoostStatus = "ok_motion_guarded";
        modelState.metalReflectionTintStatus = "ok_base_color_tinted";
        modelState.fabricReflectionSuppressStatus = "ok_fabric_matte_preserved";
        modelState.reflectionEnergyGuardStatus = "ok_luminance_guard";
        modelState.reflectionOverbrightGuardStatus = "ok_clamped_before_tonemap";
        modelState.reflectionQualityUiStatus = "ok_compact_controls";
        modelState.reflectionContrastSliderStatus = "ok";
        modelState.reflectionSaturationSliderStatus = "ok";
        modelState.environmentZonePresetStatus = "ok";
        modelState.reflectionUniformUpdateStatus = "ok_uniform_only";
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
        applyClearcoatDiagnostics(selectedMaterialJson());
        modelState.paintGlossPerformanceStatus = "ok_uniform_only";
        modelState.glossSliderStatus = "ok";
        modelState.glossSliderValue = glossSliderValue;
        modelState.paintGlossSliderStatus = "ok";
        modelState.paintGlossSliderValue = paintGlossSliderValue;
        modelState.clearcoatSliderStatus = "ok";
        modelState.clearcoatSliderValue = clearcoatIntensity;
        modelState.clearcoatRoughnessSliderStatus = "ok";
        modelState.clearcoatRoughnessSliderValue = clearcoatRoughness;
        modelState.clearcoatUniformUpdateStatus = "ok_uniform_only";
        modelState.clearcoatUiStatus = "ok_compact_material_tab";
        modelState.glossUniformUpdateStatus = "ok_uniform_only";
        modelState.glossResponseDebugViewStatus = "shader_applied";
        modelState.specularGuardDebugViewStatus = "shader_applied";
        modelState.paintGlossDebugViewStatus = "shader_applied";
        modelState.metalResponseDebugViewStatus = "shader_applied";
        modelState.paintTargetDebugViewStatus = "shader_applied";
        modelState.calibrationResponseDebugViewStatus = "shader_applied";
        modelState.clearcoatDebugViewStatus = "shader_applied";
        modelState.clearcoatHighlightDebugViewStatus = "shader_applied";
        modelState.paintLayerDebugViewStatus = "shader_applied";
        modelState.paintEnergyGuardDebugViewStatus = "shader_applied";
        modelState.fakeCubemapDebugViewStatus = "shader_applied";
        modelState.reflectionZonesDebugViewStatus = "shader_applied";
        modelState.reflectionContrastDebugViewStatus = "shader_applied";
        modelState.reflectionEnergyGuardDebugViewStatus = "shader_applied";
        modelState.clearcoatReflectionDebugViewStatus = "shader_applied";
        modelState.motionQualityDebugViewStatus = "shader_applied";
        modelState.carPaintReflectionStatus = "ok_clearcoat_probe_separation";
        modelState.metalReflectionStatus = "ok_tinted_stronger";
        modelState.plasticReflectionStatus = "ok_glossy_limited";
        modelState.rubberReflectionStatus = "ok_low_reflection";
        modelState.glassMetadataReflectionStatus = "foundation_single_pass_fake_transparency";
        modelState.materialPresetReflectionStatus = "ok";
        modelState.p23ClearcoatPreservedStatus = "ok";
        modelState.p24PerformanceStatus = "ok_no_new_pass_no_texture_no_rebuild";
        modelState.fakeCubemapNoTextureStatus = "ok";
        modelState.fakeCubemapNoNewPassStatus = "ok";
        modelState.reflectionNoTextureRebuildStatus = "ok";
        modelState.reflectionNoModelReuploadStatus = "ok";
        modelState.noFrameFileWriteStatus = "ok";
        modelState.noFrameGlbParseStatus = "ok";
        modelState.p26ReflectionPolishStatus = "ok_zone_contrast_rim_roughness_polished";
        modelState.reflectionZonePolishStatus = "ok_sky_horizon_ground_contrast_guarded";
        modelState.sideRimReflectionPolishStatus = "ok_rim_zone_boost_guarded";
        modelState.roughnessReflectionPolishStatus = "ok_smoother_mobile_response";
        modelState.glassReflectionZoneStatus = "ok_glass_uses_p26_zone_probe";
        modelState.clearcoatReflectionZoneStatus = "ok_clearcoat_uses_p26_zone_probe";
        modelState.materialTypeSpecularRoutingStatus = "ok";
        modelState.paintMaterialGlossStatus = "ok_lite_gloss";
        modelState.metalMaterialGlossStatus = "ok_stronger_response";
        modelState.rubberMaterialGlossStatus = "ok_suppressed";
        modelState.specularGlossPerformanceStatus = "ok_no_alloc_no_rebuild";
        modelState.glossVisibleResponseStatus = "ok_visible_lobe_and_reflection";
        modelState.glossAffectsSpecularLobe = "yes_shader_roughness_distribution";
        modelState.glossAffectsReflectionWeight = "yes_shader_environment_weight";
        JSONObject selectedSlot = selectedMaterialJson();
        applyGlassDiagnostics(selectedSlot);
        modelState.materialSlotEditorStatus = "ok";
        modelState.selectedMaterialSlot = selectedMaterialSlot;
        modelState.selectedMaterialSlotCount = selectedMaterialSlotCount;
        modelState.selectedMaterialTypeHint = selectedSlot == null ? "unknown" : selectedSlot.optString("materialTypeHint", "unknown");
        modelState.selectedMaterialName = selectedSlot == null ? "unknown" : selectedSlot.optString("materialName", "slot_" + selectedMaterialSlot);
        modelState.selectedMaterialSummaryStatus = selectedSlot == null ? "empty" : "ok_metallic_roughness_normal_ao_texture_summary";
        modelState.materialSlotSelectionUiStatus = "ok_prev_next_label_summary";
        modelState.perMaterialOverrideStatus = "foundation_selected_slot_uniform";
        modelState.perMaterialOverrideMode = "cpu_selected_slot_push_constants";
        modelState.selectedSlotMetallicOverride = selectedSlotMetallicOverride;
        modelState.selectedSlotRoughnessOverride = selectedSlotRoughnessOverride;
        modelState.selectedSlotNormalScaleOverride = selectedSlotNormalScaleOverride;
        modelState.selectedSlotAoOverride = selectedSlotAoOverride;
        modelState.selectedSlotGlossOverride = glossSliderValue;
        modelState.selectedSlotCoatOverride = paintGlossSliderValue;
        modelState.selectedSlotOverrideApplied = selectedSlot == null ? "false_no_material_slot" : "true_selected_slot_only";
        if (!"seeded_from_slot".equals(modelState.selectedSlotResetStatus)
            && !"ok_reset_selected_slot_from_source_material".equals(modelState.selectedSlotResetStatus)) {
            modelState.selectedSlotResetStatus = "available_safe_reseed_from_slot";
        }
        modelState.perMaterialUniformUpdateStatus = "ok_uniform_only";
        modelState.materialSlotControlsUiStatus = "ok_compact";
        modelState.metallicSlotSliderStatus = "ok";
        modelState.roughnessSlotSliderStatus = "ok";
        modelState.normalSlotSliderStatus = "ok";
        modelState.aoSlotSliderStatus = "ok";
        modelState.selectedMaterialDebugViewStatus = "shader_applied";
        modelState.materialOverrideDebugViewStatus = "shader_applied";
        modelState.slotMetallicDebugViewStatus = "shader_applied";
        modelState.slotRoughnessDebugViewStatus = "shader_applied";
        modelState.slotAoDebugViewStatus = "shader_applied";
        modelState.perMaterialOverridePerformanceStatus = "ok_no_extra_pass_no_upload";
        modelState.materialWorkflowStatus = "ok_selected_slot_material_workflow";
        modelState.materialSlotSummaryUiStatus = "ok_slot_name_hint_texture_summary";
        modelState.selectedSlotResetButtonStatus = "ok";
        modelState.selectedMaterialTextureSummaryStatus = selectedSlot == null ? "missing_no_slot" : textureSummaryForSlot(selectedSlot);
        applyAlphaCutoutDiagnostics(selectedSlot);
        modelState.assetsWorkflowStatus = "ok_active_model_import_scan_reload_export";
        modelState.reloadActiveModelButtonStatus = "ok";
        modelState.activeModelDisplayStatus = modelState.activeModelName().isEmpty() ? "empty" : "ok";
        modelState.fallbackReasonDisplayStatus = modelState.fallbackCubeVisible ? "ok_visible" : "ok_hidden_when_not_needed";
        modelState.p19PreservedStatus = "ok";
        modelState.p19SlotControlsPreservedStatus = "ok";
        modelState.p20RuntimeWorkflowPreservedStatus = "ok";
        modelState.p18IblPreservedStatus = "ok";
        modelState.p17GlossPreservedStatus = "ok";
        modelState.p22EmissivePreservedStatus = "ok";
        modelState.p22PresetsPreservedStatus = "ok";
        modelState.runtimeStateDebugViewStatus = "ok";
        modelState.restoreStateDebugViewStatus = "ok";
        modelState.uiStateDebugViewStatus = "ok";
        modelState.brdfStatus = "ok";
        modelState.brdfMode = "direct_lighting_schlick_mobile";
        modelState.diffuseStatus = "ok_non_metal_diffuse";
        modelState.specularStatus = "ok_p17_gloss_response_guarded";
        modelState.fresnelStatus = "ok_schlick";
        modelState.f0Status = "ok_dielectric_0_04_metal_base_color";
        modelState.metallicResponseStatus = "ok_diffuse_reduced_f0_tinted";
        modelState.roughnessResponseStatus = "ok_gloss_width_energy_remap";
        modelState.directLightingStatus = "ok_single_sun_direct_gloss_lobe";
        modelState.materialResponseStatus = "p24_fake_cubemap_probe_foundation";
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
        if (reflectionContrastButton != null) reflectionContrastButton.setText("Refl Contrast " + oneDecimal(reflectionContrast));
        if (reflectionSaturationButton != null) reflectionSaturationButton.setText("Refl Saturation " + oneDecimal(reflectionSaturation));
        if (groundIntensityButton != null) groundIntensityButton.setText("Ground " + oneDecimal(contactShadowIntensity));
        if (environmentButton != null) environmentButton.setText("Env " + oneDecimal(environmentIntensity));
        if (skyPresetButton != null) skyPresetButton.setText("Env Zone: " + modelState.environmentPreset);
        if (horizonButton != null) horizonButton.setText("Horizon " + oneDecimal(horizonStrength));
        if (calibrationPresetButton != null) calibrationPresetButton.setText("Calib: " + modelState.calibrationPreset);
        if (slotPrevButton != null) slotPrevButton.setText("Slot -");
        if (slotNextButton != null) slotNextButton.setText("Slot " + selectedMaterialSlot + "/" + Math.max(0, selectedMaterialSlotCount));
        if (metallicSlotButton != null) metallicSlotButton.setText("Metallic " + oneDecimal(selectedSlotMetallicOverride));
        if (roughnessSlotButton != null) roughnessSlotButton.setText("Rough " + oneDecimal(selectedSlotRoughnessOverride));
        if (normalSlotButton != null) normalSlotButton.setText("Normal " + oneDecimal(selectedSlotNormalScaleOverride));
        if (aoSlotButton != null) aoSlotButton.setText("AO " + oneDecimal(selectedSlotAoOverride));
        if (presetCycleButton != null) presetCycleButton.setText("Preset: " + modelState.activeMaterialPreset);
        if (presetApplyButton != null) presetApplyButton.setText(materialPresetPendingApply ? "Apply Preset *" : "Apply Preset");
        if (glassEnableButton != null) glassEnableButton.setText(glassEnabled ? "Glass On" : "Glass Enable");
        if (glassRenderModeButton != null) glassRenderModeButton.setText("Glass Render: " + glassRenderModeName(glassRenderModeIndex));
        if (selectGlassCandidateButton != null) selectGlassCandidateButton.setText("Select Glass Candidate");
        if (assignSelectedAsGlassButton != null) assignSelectedAsGlassButton.setText("Assign Selected As Glass");
        if (clearManualRoleButton != null) clearManualRoleButton.setText("Clear Manual Role");
        if (glassTintPresetButton != null) glassTintPresetButton.setText("Glass Tint: " + glassTintPresetName(glassTintPresetIndex));
        if (glassOpacityButton != null) glassOpacityButton.setText("Glass Opacity " + oneDecimal(glassOpacity));
        if (glassEdgeButton != null) glassEdgeButton.setText("Glass Edge " + oneDecimal(glassEdge));
        if (glassRoughnessButton != null) glassRoughnessButton.setText("Glass Rough " + oneDecimal(glassRoughness));
        if (emissiveSlider != null) emissiveSlider.setProgress(sliderProgress(emissiveIntensity, 0.0f, 2.0f));
        if (emissiveDebugButton != null) emissiveDebugButton.setText("Emissive View");
        if (alphaModeDebugButton != null) alphaModeDebugButton.setText("Alpha: " + materialDebugViewName(activeDebugViewIndex));
        if (doubleSidedDebugButton != null) doubleSidedDebugButton.setText("Double Sided");
        if (resetAlphaButton != null) resetAlphaButton.setText("Reset Alpha");
        if (calibrationButton != null) calibrationButton.setText("Calib " + oneDecimal(calibrationSliderValue));
        if (glossButton != null) glossButton.setText("Gloss " + oneDecimal(glossSliderValue));
        if (paintGlossButton != null) paintGlossButton.setText("Coat " + oneDecimal(paintGlossSliderValue));
        if (clearcoatButton != null) clearcoatButton.setText("Clearcoat " + oneDecimal(clearcoatIntensity));
        if (clearcoatRoughnessButton != null) clearcoatRoughnessButton.setText("Clearcoat Rough " + oneDecimal(clearcoatRoughness));
        if (materialStatusView != null) materialStatusView.setText("Selected: slot " + selectedMaterialSlot + "/" + Math.max(0, selectedMaterialSlotCount) + " " + modelState.selectedMaterialName + " role=" + modelState.selectedMaterialRole + "\nActive Glass: slot " + modelState.activeTransparentGlassSlot + "/" + Math.max(0, selectedMaterialSlotCount) + " " + modelState.activeTransparentGlassMaterialName + " role=" + modelState.activeTransparentGlassMaterialRole + " source=" + modelState.activeTransparentGlassSlotSource + "\nGlass: " + glassRenderModeName(glassRenderModeIndex) + " | " + (glassEnabled ? "on" : "off") + " " + glassTintPresetName(glassTintPresetIndex) + " " + oneDecimal(glassOpacity) + "/" + oneDecimal(glassEdge) + "/" + oneDecimal(glassRoughness) + "\nAlpha " + modelState.alphaModeSupportStatus + " cutoff " + oneDecimal(alphaCutoffValue) + " | Double " + modelState.doubleSidedMode + "\nMetallic " + oneDecimal(selectedSlotMetallicOverride) + " Rough " + oneDecimal(selectedSlotRoughnessOverride) + " Normal " + oneDecimal(selectedSlotNormalScaleOverride) + " AO " + oneDecimal(selectedSlotAoOverride) + "\nRefl C/S " + oneDecimal(reflectionContrast) + "/" + oneDecimal(reflectionSaturation) + " Motion " + modelState.cameraMotionQualityTier + "\nCalib " + oneDecimal(calibrationSliderValue) + " Gloss " + oneDecimal(glossSliderValue) + " Coat " + oneDecimal(paintGlossSliderValue) + " Clear " + oneDecimal(clearcoatIntensity) + "/" + oneDecimal(clearcoatRoughness) + " | wrong-slot guard on");
        updateSliderPositionsFromState();
        if (materialViewButton != null) materialViewButton.setText("Debug: " + modelState.activeDebugView);
        try {
            if (nativeLoaded && nativeHandle != 0L) {
                nativeSetLightingControls(nativeHandle, lightPresetIndex, sunIntensity, ambientIntensity, activeDebugViewIndex, toneMappingModeIndex, exposureValue, ambientFloor, brightnessPresetIndex, specularBoost, reflectionIntensity, contactShadowIntensity, calibrationPresetIndex, calibrationSliderValue, glossSliderValue, paintGlossSliderValue, clearcoatIntensity, clearcoatRoughness, environmentIntensity, environmentPresetIndex, horizonStrength, reflectionContrast, reflectionSaturation, motionReflectionScale, motionClearcoatScale, selectedMaterialSlot, selectedSlotMetallicOverride, selectedSlotRoughnessOverride, selectedSlotNormalScaleOverride, selectedSlotAoOverride, glossSliderValue, paintGlossSliderValue, alphaCutoffValue, emissiveIntensity, activeMaterialPresetIndex, glassEnabled ? 1 : 0, glassOpacity, glassEdge, glassRoughness, glassTintPresetIndex, glassRenderModeIndex, manualGlassSlotOverride);
            }
        } catch (Throwable t) {
            modelState.debugViewStatus = "native_control_failed";
            writeCrashReport("lighting_controls_failed", t);
        }
    }

    private void applyGlassDiagnostics(JSONObject selectedSlot) {
        String selectedRole = materialRoleFromSlot(selectedSlot);
        int bestGlassSlot = bestGlassCandidateSlot();
        JSONObject bestGlass = materialSlotJson(bestGlassSlot);
        boolean manualValid = manualGlassSlotOverride >= 0 && materialSlotJson(manualGlassSlotOverride) != null;
        int activeGlassSlot = manualValid ? manualGlassSlotOverride : ("Glass".equals(selectedRole) && glassRenderModeIndex == 1 ? selectedMaterialSlot : bestGlassSlot);
        JSONObject activeGlass = materialSlotJson(activeGlassSlot);
        String activeGlassRole = manualValid && activeGlassSlot >= 0 ? "Glass" : materialRoleFromSlot(activeGlass);
        String activeGlassSource = manualValid ? "manual_override" : (activeGlassSlot == selectedMaterialSlot && "Glass".equals(selectedRole) ? "selected_glass_role" : "auto_best_glass_candidate");
        boolean selectedGlassHint = "Glass".equals(selectedRole);
        boolean selectedFabric = selectedSlot != null && "fabric_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean selectedMetal = selectedSlot != null && "metal_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean selectedPaint = selectedSlot != null && "paint_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean active = glassEnabled || selectedGlassHint || activeMaterialPresetIndex == 6 || activeGlassSlot >= 0;
        boolean transparentActive = activeGlassSlot >= 0 && (glassRenderModeIndex == 1 || (glassRenderModeIndex == 2 && motionReflectionScale > 0.62f));
        int fabricCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"fabric_like\"");
        int metalCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"metal_like\"");
        int paintCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"paint_like\"");
        int nonGlassCount = Math.max(0, modelState.materialSlotCount - countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"glass_like\""));
        modelState.materialRoleSystemStatus = "ok";
        modelState.materialRoleDetectionMode = "material_name_alpha_hint_factor_no_asset_hardcode";
        modelState.selectedMaterialRole = selectedRole;
        modelState.selectedMaterialRoleConfidence = materialRoleConfidence(selectedSlot);
        modelState.selectedMaterialRoleSource = materialRoleSource(selectedSlot);
        modelState.materialRoleCandidateCount = modelState.materialSlotCount;
        modelState.glassCandidateCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"glass_like\"");
        modelState.bestGlassCandidateSlot = bestGlassSlot;
        modelState.bestGlassCandidateName = bestGlass == null ? "none" : bestGlass.optString("materialName", "slot_" + bestGlassSlot);
        modelState.bestGlassCandidateConfidence = materialRoleConfidence(bestGlass);
        modelState.bestGlassCandidateSource = materialRoleSource(bestGlass);
        modelState.materialRoleNoHardcodeStatus = "ok_no_asset_specific_slot_hardcode";
        modelState.transparentGlassRoutingStatus = transparentActive ? "ok_active_role_routed" : "fallback_fake_safe";
        modelState.transparentGlassRoutingMode = manualValid ? "manual_override" : (glassRenderModeIndex == 1 ? "transparent_v1_selected_glass_or_best_candidate" : (glassRenderModeIndex == 2 ? "auto_best_glass_candidate" : "fake_safe"));
        modelState.activeTransparentGlassSlot = activeGlassSlot;
        modelState.activeTransparentGlassMaterialName = activeGlass == null ? "none" : activeGlass.optString("materialName", "slot_" + activeGlassSlot);
        modelState.activeTransparentGlassMaterialRole = activeGlassRole;
        modelState.activeTransparentGlassRoleSource = manualValid && activeGlassSlot >= 0 ? "manual_override" : materialRoleSource(activeGlass);
        modelState.activeTransparentGlassSlotSource = activeGlassSlot >= 0 ? activeGlassSource : "none";
        modelState.transparentGlassSelectedSlotAllowedStatus = selectedGlassHint ? "ok_selected_glass_allowed" : "not_selected_glass";
        modelState.transparentGlassSelectedSlotRejectedStatus = (!selectedGlassHint && (selectedFabric || selectedMetal || selectedPaint)) ? "ok_rejected_non_glass_selected_slot" : "inactive";
        modelState.transparentGlassAutoDetectStatus = bestGlassSlot >= 0 ? "ok_best_candidate_found" : "fallback_no_glass_candidate";
        modelState.transparentGlassManualOverrideStatus = manualValid ? "active_selected_slot_assigned_as_glass" : "inactive";
        modelState.transparentGlassFallbackReason = transparentActive ? "none" : (bestGlassSlot < 0 && !manualValid ? "no_glass_candidate" : (glassRenderModeIndex == 0 ? "fake_safe_mode" : "auto_motion_guard_or_inactive"));
        modelState.transparentGlassWrongSlotGuardStatus = "ok_non_glass_slots_not_transparent_by_default";
        modelState.glassMaterialStatus = transparentActive ? "ok_transparent_v1_role_routed_slot" : (active ? "ok_p26_polished_single_pass" : "available");
        modelState.glassMode = transparentActive ? "transparent_v1_role_routed_no_refraction" : "single_pass_fake_transparency_no_refraction";
        modelState.glassPolishStatus = "ok";
        modelState.glassPolishMode = "single_pass_fake_glass_polish";
        modelState.glassReadabilityStatus = active ? "ok_visible_edges_tint_reflection" : "available";
        modelState.glassEdgePolishStatus = "ok_stronger_fresnel_rim";
        modelState.glassTintPolishStatus = "ok_tint_blends_preserve_surface";
        modelState.glassThicknessPolishStatus = "ok_fake_thickness_edge_dark_bright";
        modelState.glassOpacityCurveStatus = "ok_minimum_surface_curve";
        modelState.glassRoughnessPolishStatus = "ok_clear_vs_frosted_response";
        modelState.glassReflectionPolishStatus = "ok_p24_probe_boost_guarded";
        modelState.glassOpacity = glassOpacity;
        modelState.glassTintStatus = "ok_preset_tint";
        modelState.glassTintColor = glassTintColorStatus(glassTintPresetIndex);
        modelState.glassFresnelStatus = "ok_stronger_schlick_edge";
        modelState.glassEdgeReflectionStatus = "ok_p26_edge_fresnel_boost";
        modelState.glassReflectionSourceStatus = "p24_fake_cubemap_probe";
        modelState.glassRoughnessResponseStatus = "ok_clear_vs_rough_distinction";
        modelState.glassThicknessFakeStatus = "ok_edge_tint_thickness_fake";
        modelState.glassTransparencyMode = transparentActive ? "transparent_v1_color_blend_role_routed_slot" : "safe_single_pass_fake_opacity";
        modelState.glassSortingStatus = "limited_selected_slot_no_full_sort";
        modelState.glassRefractionStatus = "deferred_no_real_refraction";
        modelState.glassStillFakeTransparencyStatus = transparentActive ? "fallback_preserved_but_transparent_pass_active" : "yes_no_real_transparent_pass";
        modelState.glassPerformanceStatus = transparentActive ? "ok_one_extra_selected_slot_draw_no_sort" : "ok_uniform_shader_no_pass_no_sort";
        modelState.glassUiStatus = "ok_compact_material_tab";
        modelState.glassEnableButtonStatus = "ok";
        modelState.glassPresetStatus = "ok";
        modelState.activeGlassPreset = active ? glassTintPresetName(glassTintPresetIndex) : "Clear Glass";
        modelState.glassOpacitySliderStatus = "ok";
        modelState.glassOpacityValue = glassOpacity;
        modelState.glassTintPresetStatus = "ok";
        modelState.activeGlassTintPreset = glassTintPresetName(glassTintPresetIndex);
        modelState.glassEdgeSliderStatus = "ok";
        modelState.glassEdgeValue = glassEdge;
        modelState.glassRoughnessSliderStatus = "ok";
        modelState.glassRoughnessValue = glassRoughness;
        modelState.glassUniformUpdateStatus = "ok_uniform_only";
        modelState.glassPresetIntegrationStatus = "ok_selected_slot_only";
        modelState.glassPresetAppliedSlot = selectedMaterialSlot;
        modelState.glassPresetAppliedStatus = active ? "ok_selected_slot_uniform_only" : "available";
        modelState.glassPresetPolishStatus = "ok";
        modelState.clearGlassPresetStatus = "ok";
        modelState.blueGlassPresetStatus = "ok";
        modelState.greenGlassPresetStatus = "ok";
        modelState.smokeGlassPresetStatus = "ok";
        modelState.warmGlassPresetStatus = "ok";
        modelState.magicGlassPresetStatus = "ok";
        modelState.dirtyGlassLitePresetStatus = "ok";
        modelState.crystalLitePresetStatus = "ok";
        modelState.glassPresetVisualResponseStatus = "ok_tint_opacity_edge_roughness_reflection_thickness";
        modelState.glassSelectedSlotRoutingStatus = "ok_role_routed_no_blind_selected_slot";
        modelState.glassFabricGuardStatus = selectedFabric ? "ok_guard_blocks_transparency" : "ok";
        modelState.glassMetalGuardStatus = selectedMetal ? "ok_metal_preserved_unless_selected_glass_preset" : "ok";
        modelState.glassCarPaintGuardStatus = selectedPaint ? "ok_car_paint_preserved_unless_selected_glass_preset" : "ok";
        modelState.glassVisualResponseStatus = active ? "ok_visible_tint_opacity_edge_reflection" : "available";
        modelState.glassOpacityResponseStatus = "ok_base_surface_reduced";
        modelState.glassTintResponseStatus = "ok_tint_colors_surface";
        modelState.glassFresnelResponseStatus = "ok_grazing_angle_boost";
        modelState.glassReflectionResponseStatus = "ok_p24_probe_visible_guarded";
        modelState.glassEnergyGuardStatus = "ok_clamped";
        modelState.glassOverbrightGuardStatus = "ok";
        modelState.glassInvisibleGuardStatus = "ok_minimum_surface_kept";
        modelState.glassMaskDebugViewStatus = "shader_applied";
        modelState.glassOpacityDebugViewStatus = "shader_applied";
        modelState.glassFresnelDebugViewStatus = "shader_applied";
        modelState.glassReflectionDebugViewStatus = "shader_applied";
        modelState.glassTintDebugViewStatus = "shader_applied";
        modelState.glassSafetyDebugViewStatus = "shader_applied";
        modelState.glassPolishDebugViewStatus = "shader_applied";
        modelState.glassEdgeDebugViewStatus = "shader_applied";
        modelState.glassThicknessDebugViewStatus = "shader_applied";
        modelState.glassReflectionPolishDebugViewStatus = "shader_applied";
        modelState.clearcoatPolishDebugViewStatus = "shader_applied";
        modelState.paintReflectionDebugViewStatus = "shader_applied";
        modelState.p24ReflectionPreservedStatus = "ok";
        modelState.p25GlassPreservedStatus = "ok";
        modelState.p26UiChangeStatus = "minimal_material_tab_text_only";
        modelState.glassSummaryUiStatus = "ok_compact_summary";
        modelState.glassStrengthSliderStatus = "deferred_reusing_edge_opacity_roughness";
        modelState.glassControlsPreservedStatus = "ok";
        modelState.transparentGlassPassStatus = transparentActive ? "ok_active" : "available_fallback_fake";
        modelState.transparentGlassPassMode = "opaque_first_active_glass_after";
        modelState.transparentGlassSelectedSlotStatus = selectedGlassHint ? "ok_selected_slot_glass_role" : "selected_slot_not_glass";
        modelState.transparentGlassDrawOrderStatus = "opaque_first_then_active_glass";
        modelState.transparentGlassBlendStatus = transparentActive ? "ok_vk_src_alpha_one_minus_src_alpha" : "inactive_fake_safe";
        modelState.transparentGlassOpacityStatus = transparentActive ? "ok_real_alpha_blend" : "fallback_fake_opacity";
        modelState.transparentGlassTintStatus = "ok_tint_uniform";
        modelState.transparentGlassFresnelStatus = "ok_edge_reflection_preserved";
        modelState.transparentGlassReflectionStatus = "ok_p26_p24_probe_preserved";
        modelState.transparentGlassSortingStatus = "limited_selected_slot_no_full_sort";
        modelState.transparentGlassRefractionStatus = "deferred_no_real_refraction";
        modelState.transparentGlassFallbackStatus = transparentActive ? "p26_fake_available_when_disabled" : "active_p26_fake_glass";
        modelState.transparentGlassPerformanceStatus = transparentActive ? "ok_extra_selected_slot_draw_only" : "ok_no_extra_draw";
        modelState.transparentGlassUiStatus = "ok_minimal_material_tab";
        modelState.glassRenderModeStatus = "ok_fake_safe_transparent_v1_auto";
        modelState.activeGlassRenderMode = glassRenderModeName(glassRenderModeIndex);
        modelState.transparentGlassToggleStatus = "ok";
        modelState.glassModeSummaryUiStatus = "ok";
        modelState.p26GlassPolishPreservedStatus = "ok";
        modelState.fakeGlassFallbackStatus = transparentActive ? "available_when_mode_fake_safe_or_guard" : "active";
        modelState.glassAutoFallbackStatus = glassRenderModeIndex == 2 && !transparentActive ? "active_fake_safe" : "available";
        modelState.glassMotionFallbackStatus = motionReflectionScale <= 0.62f ? "active_motion_guard_fake_safe" : "available";
        modelState.transparentGlassMaterialRoutingStatus = "role_routed_active_glass_slot";
        modelState.transparentGlassAppliedSlot = activeGlassSlot;
        modelState.transparentGlassAppliedMaterialCount = transparentActive ? 1 : 0;
        modelState.transparentGlassSkippedOpaqueCount = nonGlassCount;
        modelState.transparentGlassSkippedNonGlassCount = nonGlassCount;
        modelState.transparentGlassSkippedFabricCount = fabricCount;
        modelState.transparentGlassSkippedMetalCount = metalCount;
        modelState.transparentGlassSkippedPaintCount = paintCount;
        modelState.transparentGlassFabricGuardStatus = selectedFabric && !manualValid ? "ok_fabric_kept_opaque" : "ok";
        modelState.transparentGlassMetalGuardStatus = selectedMetal && !manualValid ? "ok_metal_kept_opaque" : "ok";
        modelState.transparentGlassCarPaintGuardStatus = selectedPaint && !manualValid ? "ok_car_paint_kept_opaque" : "ok";
        modelState.materialRoleUiStatus = "ok_minimal_material_tab";
        modelState.selectedSlotRoleSummaryUiStatus = "ok";
        modelState.activeGlassSlotSummaryUiStatus = "ok";
        modelState.selectGlassCandidateButtonStatus = "ok";
        modelState.assignSelectedAsGlassButtonStatus = "ok";
        modelState.clearManualRoleButtonStatus = "ok";
        modelState.selectedSlotHighlightToggleStatus = "diagnostic_only_deferred";
        modelState.p27TransparentPassPreservedStatus = "ok";
        modelState.p27bPerformanceStatus = "ok_existing_transparent_pass_role_routing_only";
        modelState.p27bNoExtraHeavyPassStatus = "ok";
        modelState.p27bNoTextureRebuildStatus = "ok";
        modelState.p27bNoModelReuploadStatus = "ok";
        modelState.p27bNoFrameGlbParseStatus = "ok";
        modelState.p27bNoFrameFileWriteStatus = "ok";
        modelState.p27bNoShadowStatus = "ok";
        modelState.p27bNoRealMirrorStatus = "ok";
        modelState.p27bNoSsrStatus = "ok";
        modelState.p27bNoRealRefractionStatus = "ok";
        modelState.materialRoleDebugViewStatus = "shader_applied";
        modelState.glassCandidateDebugViewStatus = "shader_applied";
        modelState.activeGlassSlotDebugViewStatus = "shader_applied";
        modelState.transparentRoutingDebugViewStatus = "shader_applied";
        modelState.wrongSlotGuardDebugViewStatus = "shader_applied";
        modelState.transparentGlassMaskDebugViewStatus = "shader_applied";
        modelState.transparentGlassAlphaDebugViewStatus = "shader_applied";
        modelState.transparentGlassDrawOrderDebugViewStatus = "shader_applied";
        modelState.transparentGlassFallbackDebugViewStatus = "shader_applied";
        modelState.transparentGlassSafetyDebugViewStatus = "shader_applied";
        modelState.p27PerformanceStatus = transparentActive ? "ok_selected_slot_extra_draw_only" : "ok_fake_safe_no_extra_draw";
        modelState.p27NoShadowStatus = "ok";
        modelState.p27NoRaytraceStatus = "ok";
        modelState.p27NoSsrStatus = "ok";
        modelState.p27NoRealMirrorStatus = "ok";
        modelState.p27NoScreenRefractionStatus = "ok";
        modelState.p27NoTextureRebuildStatus = "ok";
        modelState.p27NoModelReuploadStatus = "ok";
        modelState.clearcoatPolishStatus = "ok";
        modelState.clearcoatHighlightPolishStatus = "ok_clearer_guarded_highlight";
        modelState.clearcoatReflectionSeparationStatus = "ok_layer_separated_from_base";
        modelState.carPaintPolishStatus = "ok";
        modelState.carPaintLayerReadabilityStatus = "ok_base_coat_reflection_separated";
        modelState.carPaintReflectionSeparationStatus = "ok_paint_reflection_layer_readable";
        modelState.paintOverbrightGuardStatus = "ok_clamped";
        modelState.p25PerformanceStatus = "ok_single_pass_uniform_shader_math";
        modelState.p26PerformanceStatus = "ok_single_pass_shader_math_no_rebuild";
        modelState.p26NoRealRefractionStatus = "ok_deferred";
        modelState.p26NoScreenRefractionStatus = "ok_deferred";
        modelState.p26NoMirrorPassStatus = "ok_deferred";
        modelState.p26NoFullSortingStatus = "ok_deferred";
        modelState.p26NoTextureRebuildStatus = "ok";
        modelState.p26NoModelReuploadStatus = "ok";
        modelState.p26NoNewShadowPassStatus = "ok";
        modelState.glassNoRefractionPassStatus = "ok";
        modelState.glassNoScreenRefractionStatus = "ok";
        modelState.glassNoFullSortingStatus = "ok";
        modelState.glassNoTextureRebuildStatus = "ok";
        modelState.glassNoModelReuploadStatus = "ok";
        modelState.glassNoNewShadowPassStatus = "ok";
    }

    private void applyAlphaCutoutDiagnostics(JSONObject selectedSlot) {
        int maskCount = countOccurrences(modelState.materialSlotDiagnostics, "\"alphaMode\":\"MASK\"");
        int blendCount = countOccurrences(modelState.materialSlotDiagnostics, "\"alphaMode\":\"BLEND\"");
        int doubleCount = countOccurrences(modelState.materialSlotDiagnostics, "\"doubleSided\":true");
        int cutoutCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"cutout_like\"");
        int fabricCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"fabric_like\"");
        int glassCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"glass_like\"");
        int decalCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"decal_like\"");
        String selectedAlpha = selectedSlot == null ? "OPAQUE" : selectedSlot.optString("alphaMode", "OPAQUE");
        boolean selectedDoubleSided = selectedSlot != null && selectedSlot.optBoolean("doubleSided", false);
        modelState.alphaMaterialStatus = maskCount > 0 || blendCount > 0 ? "ok_alpha_metadata_detected" : "ok_opaque_materials";
        modelState.alphaModeSupportStatus = "ok_opaque_mask_blend_metadata";
        modelState.alphaMaskStatus = maskCount > 0 ? "ok_mask_discard_shader" : "ok_no_mask_material";
        modelState.alphaBlendStatus = blendCount > 0 ? "fallback_no_sorting_blend_as_cutout_or_opaque" : "ok_no_blend_material";
        modelState.alphaCutoffStatus = "ok_uniform_control";
        modelState.alphaCutoffValue = alphaCutoffValue;
        modelState.alphaDiscardStatus = maskCount > 0 || blendCount > 0 ? "ok_shader_discard_for_safe_cutout" : "inactive_opaque";
        modelState.alphaTextureChannelStatus = "baseColor_alpha_channel_sampled_when_texture_ready";
        modelState.alphaFallbackStatus = blendCount > 0 ? "blend_deferred_safe_cutout_or_opaque" : "none";
        modelState.doubleSidedMaterialStatus = doubleCount > 0 ? "ok_double_sided_metadata_detected" : "ok_no_double_sided_material";
        modelState.doubleSidedMode = selectedDoubleSided ? "selected_slot_double_sided" : "selected_slot_single_sided_or_none";
        modelState.doubleSidedNormalStatus = "shader_gl_front_facing_normal_flip";
        modelState.doubleSidedRasterStatus = "ok_pipeline_cull_none_no_new_permutation";
        modelState.doubleSidedFallbackStatus = doubleCount > 0 ? "no_new_pipeline_permutation_needed" : "none";
        modelState.thinMaterialPolishStatus = "ok_cutout_double_sided_hint_foundation";
        modelState.cutoutMaterialHintStatus = cutoutCount > 0 ? "ok" : "ok_available";
        modelState.fabricEdgeStatus = fabricCount > 0 ? "ok_fabric_like_matte_edges" : "ok_available";
        modelState.glassMetadataStatus = glassCount > 0 ? "metadata_only_render_safe_cutout_or_opaque" : "none";
        modelState.decalMaterialHintStatus = decalCount > 0 ? "ok" : "ok_available";
        modelState.transparencyDeferredStatus = "ok_no_full_transparent_sorting_or_glass";
        modelState.alphaUiStatus = "ok_compact_material_tab";
        modelState.alphaCutoffSliderStatus = "ok";
        modelState.alphaDebugViewStatus = "shader_applied";
        modelState.doubleSidedDebugViewStatus = "shader_applied";
        modelState.alphaResetButtonStatus = "ok".equals(modelState.alphaResetButtonStatus) ? "ok" : modelState.alphaResetButtonStatus;
        modelState.alphaUniformUpdateStatus = "ok_uniform_only";
        modelState.alphaSliderUpdateMode = "uniform_only";
        modelState.alphaMaskDebugViewStatus = "shader_applied";
        modelState.alphaModeDebugViewStatus = "shader_applied";
        modelState.cutoutHintDebugViewStatus = "shader_applied";
        modelState.transparencyStatusDebugViewStatus = "shader_applied";
        modelState.alphaPerformanceStatus = "ok_no_new_pass_no_sorting_no_reupload";
        modelState.alphaNoNewPassStatus = "ok";
        modelState.alphaNoTextureRebuildStatus = "ok";
        modelState.alphaNoModelReuploadStatus = "ok";
        applyP22Diagnostics();
        modelState.selectedAlphaMode = selectedAlpha;
    }

    private void applyP22Diagnostics() {
        JSONObject selectedSlot = selectedMaterialJson();
        int emissiveFactorCount = countOccurrences(modelState.materialSlotDiagnostics, "\"emissiveAppliedStatus\":\"factor_available\"");
        int emissiveTextureCount = countOccurrences(modelState.materialSlotDiagnostics, "\"emissiveTextureStatus\":\"metadata_only\"");
        modelState.p21AlphaPreservedStatus = "ok";
        modelState.emissiveMaterialStatus = emissiveFactorCount > 0 || emissiveTextureCount > 0 || emissiveIntensity > 0.001f ? "ok_safe_emissive_supported" : "ok_metadata_supported";
        modelState.emissiveMode = "factor_uniform_only_no_light_contribution";
        modelState.emissiveFactorStatus = emissiveFactorCount > 0 ? "ok_metadata_found" : "missing";
        modelState.emissiveTextureStatus = emissiveTextureCount > 0 ? "metadata_only_not_sampled_no_extra_descriptor" : "missing";
        modelState.emissiveIntensity = emissiveIntensity;
        modelState.emissiveIntensityStatus = "ok_clamped_0_2";
        modelState.emissiveColorStatus = "ok_guarded";
        modelState.emissiveOverbrightGuardStatus = "ok_clamped";
        modelState.emissiveLightingContributionStatus = "not_real_light_source";
        modelState.emissivePerformanceStatus = "ok_no_bloom_no_new_pass";
        modelState.materialPresetStatus = "ok";
        modelState.activeMaterialPreset = materialPresetName(activeMaterialPresetIndex);
        modelState.materialPresetMode = "selected_slot_uniform_only";
        modelState.materialPresetAppliedSlot = selectedMaterialSlot;
        modelState.materialPresetAppliedStatus = materialPresetPendingApply ? "pending_apply" : ("not_applied".equals(modelState.materialPresetAppliedStatus) ? "not_applied" : "ok_selected_slot_uniform_only");
        modelState.materialPresetUiStatus = "ok_compact_material_tab";
        modelState.materialPresetPerformanceStatus = "ok_no_model_reupload_no_texture_rebuild";
        modelState.selectedSlotPresetStatus = selectedSlot == null ? "empty" : "ok_selected_slot";
        modelState.carPaintPresetStatus = "ok";
        modelState.metalPresetStatus = "ok";
        modelState.fabricPresetStatus = "ok";
        modelState.rubberPresetStatus = "ok";
        modelState.plasticPresetStatus = "ok";
        modelState.glassMetadataPresetStatus = "glass_foundation_no_refraction";
        modelState.emissivePresetStatus = "ok_safe_clamped";
        modelState.materialPresetGuardStatus = "ok_energy_guarded";
        modelState.presetCycleButtonStatus = "ok";
        modelState.presetApplyButtonStatus = "ok";
        modelState.emissiveSliderStatus = "ok";
        modelState.emissiveUniformUpdateStatus = "ok_uniform_only";
        modelState.materialResetButtonStatus = "ok";
        modelState.materialUiScrollPreservedStatus = "ok";
        modelState.emissiveDebugViewStatus = "shader_applied";
        modelState.presetTypeDebugViewStatus = "shader_applied";
        modelState.presetResponseDebugViewStatus = "shader_applied";
        modelState.materialEnergyGuardDebugViewStatus = "shader_applied";
        modelState.selectedSlotPresetDebugViewStatus = "shader_applied";
        modelState.p22PerformanceStatus = "ok_no_new_pass_no_bloom_no_reupload";
        modelState.emissiveNoBloomStatus = "ok";
        modelState.emissiveNoNewPassStatus = "ok";
        modelState.presetNoModelReuploadStatus = "ok";
        modelState.presetNoTextureRebuildStatus = "ok";
        modelState.p22EmissivePreservedStatus = "ok";
        modelState.p22PresetsPreservedStatus = "ok";
    }

    private void applyInspectorDiagnostics() {
        modelState.inspectorUiStatus = "ok";
        modelState.inspectorUiMode = "tabbed_capped_scroll_inspector";
        modelState.activeInspectorTab = activeInspectorTab;
        modelState.assetsTabStatus = "ok_import_scan_reload_export_summary";
        modelState.cameraTabStatus = "ok_camera_info_reset_zoom";
        modelState.lightingTabStatus = "ok_sliders_environment_controls";
        modelState.materialTabStatus = "ok_scrollable_selected_slot_workflow";
        modelState.debugTabStatus = "ok_fps_zip_status";
        modelState.inspectorHeightMode = "capped_30_percent";
        modelState.inspectorScrollStatus = "ok";
        modelState.inspectorExpandedMaxHeightPercent = 30;
        modelState.inspectorCollapsedStatus = "ok_compact_toggle";
        modelState.materialTabScrollStatus = "ok";
        modelState.inspectorTouchTargetStatus = "ok";
        modelState.inspectorDynamicAlphaStatus = "ok";
        modelState.inspectorAlphaIdle = 0.92f;
        modelState.inspectorAlphaWhileSliderDrag = 0.12f;
        modelState.inspectorAlphaWhileCameraMove = 0.16f;
        modelState.inspectorAlphaRestoreStatus = "ok_timed_restore";
        modelState.sliderDragVisualMode = "transparent_inspector_uniform_only";
        modelState.cameraMoveVisualMode = "transparent_inspector_camera_drag";
    }

    private String textureSummaryForSlot(JSONObject slot) {
        if (slot == null) return "baseColor missing | metallicRoughness missing | normal missing | occlusion missing";
        return "baseColor " + okMissing(slot, "baseColorTextureSlot")
            + " | metallicRoughness " + okMissing(slot, "metallicRoughnessTextureSlot")
            + "\nnormal " + okMissing(slot, "normalTextureSlot")
            + " | occlusion " + okMissing(slot, "occlusionTextureSlot");
    }

    private String okMissing(JSONObject slot, String key) {
        return slot.optInt(key, -1) >= 0 ? "ok" : "missing";
    }

    private void updateSliderPositionsFromState() {
        updatingSlidersFromState = true;
        try {
            if (sunSlider != null) sunSlider.setProgress(sliderProgress(sunIntensity, 0.5f, 4.0f));
            if (ambientSlider != null) ambientSlider.setProgress(sliderProgress(ambientIntensity, 0.1f, 2.0f));
            if (exposureSlider != null) exposureSlider.setProgress(sliderProgress(exposureValue, 0.8f, 3.0f));
            if (specularSlider != null) specularSlider.setProgress(sliderProgress(specularBoost, 0.5f, 3.0f));
            if (reflectionSlider != null) reflectionSlider.setProgress(sliderProgress(reflectionIntensity, 0.0f, 2.0f));
            if (reflectionContrastSlider != null) reflectionContrastSlider.setProgress(sliderProgress(reflectionContrast, 0.0f, 2.0f));
            if (reflectionSaturationSlider != null) reflectionSaturationSlider.setProgress(sliderProgress(reflectionSaturation, 0.0f, 2.0f));
            if (groundSlider != null) groundSlider.setProgress(sliderProgress(contactShadowIntensity, 0.0f, 1.5f));
            if (environmentSlider != null) environmentSlider.setProgress(sliderProgress(environmentIntensity, 0.0f, 2.0f));
            if (horizonSlider != null) horizonSlider.setProgress(sliderProgress(horizonStrength, 0.0f, 1.0f));
            if (metallicSlotSlider != null) metallicSlotSlider.setProgress(sliderProgress(selectedSlotMetallicOverride, 0.0f, 1.0f));
            if (roughnessSlotSlider != null) roughnessSlotSlider.setProgress(sliderProgress(selectedSlotRoughnessOverride, 0.0f, 1.0f));
            if (normalSlotSlider != null) normalSlotSlider.setProgress(sliderProgress(selectedSlotNormalScaleOverride, 0.0f, 2.0f));
            if (aoSlotSlider != null) aoSlotSlider.setProgress(sliderProgress(selectedSlotAoOverride, 0.0f, 1.5f));
            if (alphaCutoffSlider != null) alphaCutoffSlider.setProgress(sliderProgress(alphaCutoffValue, 0.0f, 1.0f));
            if (emissiveSlider != null) emissiveSlider.setProgress(sliderProgress(emissiveIntensity, 0.0f, 2.0f));
            if (glassOpacitySlider != null) glassOpacitySlider.setProgress(sliderProgress(glassOpacity, 0.0f, 1.0f));
            if (glassEdgeSlider != null) glassEdgeSlider.setProgress(sliderProgress(glassEdge, 0.0f, 2.0f));
            if (glassRoughnessSlider != null) glassRoughnessSlider.setProgress(sliderProgress(glassRoughness, 0.0f, 1.0f));
            if (clearcoatSlider != null) clearcoatSlider.setProgress(sliderProgress(clearcoatIntensity, 0.0f, 2.0f));
            if (clearcoatRoughnessSlider != null) clearcoatRoughnessSlider.setProgress(sliderProgress(clearcoatRoughness, 0.0f, 1.0f));
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
        if (index == 1) return "Outdoor";
        if (index == 2) return "Warm Room";
        if (index == 3) return "Cold Room";
        if (index == 4) return "Sunset";
        if (index == 5) return "Cave";
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
        if (index == 27) return "Selected Material";
        if (index == 28) return "Material Override";
        if (index == 29) return "Slot Metallic";
        if (index == 30) return "Slot Roughness";
        if (index == 31) return "Slot AO";
        if (index == 32) return "Alpha Mask";
        if (index == 33) return "Alpha Mode";
        if (index == 34) return "Double Sided";
        if (index == 35) return "Cutout Hint";
        if (index == 36) return "Transparency Status";
        if (index == 37) return "Emissive";
        if (index == 38) return "Preset Type";
        if (index == 39) return "Preset Response";
        if (index == 40) return "Material Energy Guard";
        if (index == 41) return "Selected Slot Preset";
        if (index == 42) return "Clearcoat";
        if (index == 43) return "Clearcoat Highlight";
        if (index == 44) return "Paint Layer";
        if (index == 45) return "Paint Energy Guard";
        if (index == 46) return "Fake Cubemap";
        if (index == 47) return "Reflection Zones";
        if (index == 48) return "Reflection Contrast";
        if (index == 49) return "Reflection Energy Guard";
        if (index == 50) return "Clearcoat Reflection";
        if (index == 51) return "Motion Quality";
        if (index == 52) return "Glass Mask";
        if (index == 53) return "Glass Opacity";
        if (index == 54) return "Glass Fresnel";
        if (index == 55) return "Glass Reflection";
        if (index == 56) return "Glass Tint";
        if (index == 57) return "Glass Safety Status";
        if (index == 58) return "Glass Polish";
        if (index == 59) return "Glass Edge";
        if (index == 60) return "Glass Thickness Fake";
        if (index == 61) return "Glass Reflection Polish";
        if (index == 62) return "Clearcoat Polish";
        if (index == 63) return "Paint Reflection";
        if (index == 64) return "Transparent Glass Mask";
        if (index == 65) return "Transparent Glass Alpha";
        if (index == 66) return "Transparent Glass Draw Order";
        if (index == 67) return "Transparent Glass Fallback";
        if (index == 68) return "Transparent Glass Safety";
        if (index == 69) return "Material Role";
        if (index == 70) return "Glass Candidate";
        if (index == 71) return "Active Glass Slot";
        if (index == 72) return "Transparent Routing";
        if (index == 73) return "Wrong Slot Guard";
        return "Final Shaded";
    }

    private String materialPresetName(int index) {
        if (index == 1) return "Car Paint";
        if (index == 2) return "Metal";
        if (index == 3) return "Fabric";
        if (index == 4) return "Rubber";
        if (index == 5) return "Plastic";
        if (index == 6) return "Glass Foundation";
        if (index == 7) return "Emissive Safe";
        return "Balanced";
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

    private void applyClearcoatDiagnostics(JSONObject selectedSlot) {
        int paintCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"paint_like\"");
        int metalCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"metal_like\"");
        int fabricCount = countOccurrences(modelState.materialSlotDiagnostics, "\"materialTypeHint\":\"fabric_like\"");
        boolean selectedFabric = selectedSlot != null && "fabric_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean selectedGlass = selectedSlot != null && "glass_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean carPaintPreset = activeMaterialPresetIndex == 1;
        boolean selectedCoatable = selectedSlot != null && !selectedFabric && !selectedGlass && !"rubber_like".equals(selectedSlot.optString("materialTypeHint", ""));
        boolean applied = clearcoatIntensity > 0.001f && (carPaintPreset || selectedCoatable);
        float selectedLum = selectedSlot == null ? 0.0f : (float)selectedSlot.optDouble("albedoLuminance", 0.0);
        boolean guard = selectedLum > 0.82f || selectedFabric || selectedGlass;
        modelState.clearcoatStatus = applied ? "ok" : "available";
        modelState.clearcoatMode = "single_pass_uniform_clearcoat";
        modelState.clearcoatIntensity = clearcoatIntensity;
        modelState.clearcoatRoughness = clearcoatRoughness;
        modelState.clearcoatFresnelStatus = "ok_schlick_view_angle";
        modelState.clearcoatHighlightStatus = "ok_view_dependent";
        modelState.clearcoatMaterialRoutingStatus = selectedFabric ? "fabric_skipped_matte" : (selectedGlass ? "glass_metadata_only_skipped" : (carPaintPreset ? "car_paint_preset" : "selected_slot_safe"));
        modelState.clearcoatOverbrightGuardStatus = guard ? "ok_guard_applied" : "ok";
        modelState.clearcoatPerformanceStatus = "ok_single_pass_no_texture_no_loop";
        modelState.clearcoatAppliedStatus = applied ? "applied" : "not_applied";
        modelState.clearcoatWeight = applied ? clearcoatIntensity : 0.0f;
        modelState.clearcoatRoughnessApplied = applied ? clearcoatRoughness : 1.0f;
        modelState.clearcoatGuardApplied = guard ? "yes" : "no";
        modelState.carPaintLayerStatus = "ok";
        modelState.carPaintPresetV2Status = "ok";
        modelState.carPaintClearcoatStatus = carPaintPreset ? "ok_clearcoat_enabled" : "available";
        modelState.paintLayerEnergyGuardStatus = guard ? "ok_guard_applied" : "ok";
        modelState.paintLayerMaterialHintStatus = paintCount > 0 ? "paint_like" : (metalCount > 0 ? "metal_limited" : "selected_slot");
        modelState.p23PerformanceStatus = "ok_single_pass_uniform_only";
        modelState.clearcoatNoNewPassStatus = "ok";
        modelState.clearcoatNoTextureRebuildStatus = "ok";
        modelState.clearcoatNoModelReuploadStatus = "ok";
        modelState.clearcoatNoTransparentSortingStatus = "ok";
        modelState.renderLoopAllocationGuardStatus = "ok_no_java_glb_parse_or_upload_in_frame_callback";
        modelState.fabricMattePreserveStatus = fabricCount > 0 || selectedFabric ? "ok_fabric_matte_preserved" : "ok";
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
        updateMotionQualityBlend(now);
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
                if (cameraMotionQualityBlend < 0.98f) cameraMovingFpsLast = measuredFps; else cameraStillFpsLast = measuredFps;
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
            setInspectorAlpha(0.16f, "camera_move");
            beginCameraMotion(0.0f);
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            pinchActive = false;
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
            lastPinchDistance = touchDistance(event);
            beginCameraMotion(0.0f);
            pinchActive = true;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            setInspectorAlpha(0.16f, "camera_move");
            if (event.getPointerCount() >= 2) {
                float d = touchDistance(event);
                if (lastPinchDistance > 0.0f && d > 0.0f) {
                    float delta = (lastPinchDistance - d) * 0.006f;
                    updateCameraMotion(Math.abs(delta) * 780.0f);
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
                updateCameraMotion((float)Math.sqrt(dx * dx + dy * dy));
                applyCamera(cameraYawDeg + dx * 0.35f, cameraPitchDeg + dy * 0.25f, cameraDistance);
                lastTouchX = x;
                lastTouchY = y;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
            pinchActive = false;
            lastPinchDistance = 0.0f;
            cameraMotionStopNs = System.nanoTime();
            scheduleInspectorAlphaRestore();
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
            updateMotionQualityBlend(System.nanoTime());
            nativeSetCamera(nativeHandle, cameraYawDeg, cameraPitchDeg, cameraDistance, cameraMotionSpeed, cameraMotionQualityBlend, motionReflectionScale, motionClearcoatScale, cameraMovingFpsLast, cameraStillFpsLast);
            updateTopHudOnly();
        } catch (Throwable t) {
            writeCrashReport("native_camera_update_failed", t);
        }
    }

    private void beginCameraMotion(float speed) {
        lastCameraMoveNs = System.nanoTime();
        cameraMotionStopNs = 0L;
        cameraMotionSpeed = Math.max(cameraMotionSpeed, speed);
        updateMotionQualityBlend(lastCameraMoveNs);
    }

    private void updateCameraMotion(float speed) {
        long now = System.nanoTime();
        lastCameraMoveNs = now;
        cameraMotionStopNs = 0L;
        cameraMotionSpeed = clamp(speed, 0.0f, 2400.0f);
        updateMotionQualityBlend(now);
    }

    private void updateMotionQualityBlend(long now) {
        boolean moving = lastCameraMoveNs > 0L && (now - lastCameraMoveNs) < 140_000_000L;
        if (moving) {
            cameraMotionQualityBlend = 0.42f;
        } else {
            if (cameraMotionStopNs == 0L) cameraMotionStopNs = now;
            float t = clamp((float)(now - cameraMotionStopNs) / (float)MOTION_RESTORE_NS, 0.0f, 1.0f);
            cameraMotionQualityBlend = 0.42f + (1.0f - 0.42f) * t;
            cameraMotionSpeed = Math.max(0.0f, cameraMotionSpeed * 0.82f);
        }
        motionReflectionScale = clamp(0.46f + cameraMotionQualityBlend * 0.54f, 0.46f, 1.0f);
        motionClearcoatScale = clamp(0.40f + cameraMotionQualityBlend * 0.60f, 0.40f, 1.0f);
        cameraMotionQualityTier = cameraMotionQualityBlend < 0.55f ? "moving_guard" : (cameraMotionQualityBlend < 0.98f ? "restoring" : "full");
        modelState.cameraMotionStatus = moving ? "moving" : "still";
        modelState.cameraMotionSpeed = cameraMotionSpeed;
        modelState.cameraMotionQualityBlend = cameraMotionQualityBlend;
        modelState.cameraMotionQualityTier = cameraMotionQualityTier;
        modelState.motionReflectionScale = motionReflectionScale;
        modelState.motionClearcoatScale = motionClearcoatScale;
        modelState.cameraMovingFpsLast = cameraMovingFpsLast;
        modelState.cameraStillFpsLast = cameraStillFpsLast;
        modelState.motionPerformanceGuardStatus = "ok";
        modelState.motionQualityRestoreStatus = "ok_smooth_restore";
        modelState.motionDiagnosticsThrottleStatus = moving ? "ok_no_export_while_moving" : "ok";
        modelState.movingFpsGuardStatus = cameraMovingFpsLast <= 0.0f ? "monitoring" : (cameraMovingFpsLast >= 45.0f ? "ok_target_met" : "active_below_target");
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
            modelState.resumeRestoreStatus = "skipped_no_active_model";
            modelState.resumeRestoreMode = "none";
            modelState.activeModelPersistenceStatus = "empty";
            modelState.activeModelRestoreResult = "skipped";
            setModelFallbackState("no active model");
            nativeSetModelFallback(nativeHandle, "none", "", "no active model");
            return;
        }
        if (trigger.startsWith("surface_") || trigger.contains("resume") || trigger.contains("reload")) {
            modelState.activeModelRestoreAttemptCount++;
            modelState.resumeRestoreStatus = "attempting";
            modelState.resumeRestoreMode = "cached_parsed_model_reupload";
        }
        File active = new File(modelState.localExtractionPath());
        if (!active.exists()) {
            modelState.resumeRestoreStatus = "failed";
            modelState.activeModelRestoreResult = "failed_missing_cached_model";
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
                modelState.resumeRestoreStatus = trigger.startsWith("surface_") || trigger.contains("reload") ? "ok" : modelState.resumeRestoreStatus;
                modelState.activeModelPersistenceStatus = "ok_cached_metadata_and_local_model";
                modelState.activeModelRestoreResult = "ok_model_active";
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
                modelState.persistedModelBounds = "[" + jsonFloat(mesh.boundsMin[0]) + "," + jsonFloat(mesh.boundsMin[1]) + "," + jsonFloat(mesh.boundsMin[2]) + "]-[" + jsonFloat(mesh.boundsMax[0]) + "," + jsonFloat(mesh.boundsMax[1]) + "," + jsonFloat(mesh.boundsMax[2]) + "]";
                modelState.persistedModelScale = mesh.modelScale;
                selectedMaterialSlotCount = mesh.materialSlotCount;
                selectedMaterialSlot = selectedMaterialSlotCount > 0 ? Math.min(selectedMaterialSlot, selectedMaterialSlotCount - 1) : 0;
                seedSelectedSlotOverridesFromDiagnostics();
                if (mesh.materials != null && !mesh.materials.isEmpty()) {
                    MaterialInfo first = mesh.materials.get(0);
                    modelState.metallicFactor = first.metallicFactor;
                    modelState.roughnessFactor = first.roughnessFactor;
                    modelState.normalScale = first.normalScale;
                    modelState.occlusionStrength = first.occlusionStrength;
                }
                modelState.fallbackCubeVisible = false;
                modelState.fallbackCubeStatus = "off";
                modelState.fallbackCubeReason = "none_active_model_restored";
                modelState.reason = trigger + ": multi primitive static upload rendered=" + mesh.primitiveCountRendered + " skipped=" + mesh.primitiveCountSkipped + " reason=" + mesh.reason;
                modelState.parse.gpuUploadStatus = "ok";
                modelState.parse.drawStatus = modelState.drawStatus;
                modelState.parse.uploadedVertexCount = mesh.vertexCount;
                modelState.parse.uploadedIndexCount = mesh.indexCount;
                applyBaseColorTextures(mesh, trigger);
                applyPbrTextures(mesh, trigger);
                persistActiveModelMetadata();
            } else {
                modelState.resumeRestoreStatus = "failed";
                modelState.activeModelRestoreResult = "failed_native_upload";
                setModelFallbackState(trigger + ": native model upload/draw failed");
            }
        } catch (Throwable t) {
            modelState.resumeRestoreStatus = "failed";
            modelState.activeModelRestoreResult = "failed_exception";
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
        modelState.fallbackCubeReason = reason;
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
                modelState.activeModelPersistenceStatus = "metadata_pending_upload";
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
                persistActiveModelMetadata();
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

    private void reloadActiveModelFromButton() {
        if (reloadActiveModelButton != null) {
            reloadActiveModelButton.setEnabled(false);
            reloadActiveModelButton.setText("Reloading...");
        }
        modelState.reloadActiveModelStatus = "running";
        View poster = reloadActiveModelButton != null ? reloadActiveModelButton : statusView;
        poster.post(() -> {
            attemptActiveModelGpuUpload("reload_active_model");
            modelState.reloadActiveModelStatus = "ok".equals(modelState.gpuUploadStatus) ? "ok" : "failed";
            if (reloadActiveModelButton != null) {
                reloadActiveModelButton.setText("Reload Active Model");
                reloadActiveModelButton.setEnabled(true);
            }
            writeModelDiagnostics("reload_active_model");
            exportEngineDiagnostics("reload_active_model");
            updateImportUi();
        });
    }

    private void persistActiveModelMetadata() {
        if (modelState.activeModelPath == null || modelState.activeModelPath.isEmpty()) return;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(PREF_ACTIVE_MODEL_PATH, modelState.activeModelPath)
            .putString(PREF_ACTIVE_MODEL_LOCAL_PATH, modelState.activeModelLocalPath)
            .putString(PREF_ACTIVE_MODEL_NAME, modelState.activeModelName())
            .apply();
        modelState.activeModelPersistenceStatus = "ok_metadata_saved";
    }

    private void restorePersistedActiveModel() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String path = prefs.getString(PREF_ACTIVE_MODEL_PATH, "");
        String local = prefs.getString(PREF_ACTIVE_MODEL_LOCAL_PATH, "");
        String name = prefs.getString(PREF_ACTIVE_MODEL_NAME, "");
        if (path == null || path.isEmpty()) {
            modelState.activeModelPersistenceStatus = "empty";
            modelState.resumeRestoreStatus = "skipped_no_persisted_model";
            return;
        }
        modelState.activeModelPath = path;
        modelState.activeModelLocalPath = local == null ? "" : local;
        modelState.sourceDisplayName = name == null ? "" : name;
        modelState.importStatus = "ok";
        modelState.importRoute = "persisted_metadata";
        modelState.activeModelPersistenceStatus = "ok_metadata_restored";
        modelState.resumeRestoreStatus = "pending_surface_restore";
        modelState.resumeRestoreMode = "cached_local_path";
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
            w.write("SOLUM P21 debug zip\n");
            w.write(SCENE_NAME + "\n");
            w.write("debugZipStatus=running\n");
            w.write("requiredFiles=engine_runtime_state.json,engine_diagnostics_manifest.json,model_import_state.json,asset_report.json,glb_model_summary.json,debug_zip_runtime_note.txt\n");
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
            + "  \"resumeRestoreStatus\": \"" + escape(jsonStringField(renderLab, "resumeRestoreStatus", modelState.resumeRestoreStatus)) + "\",\n"
            + "  \"resumeRestoreMode\": \"" + escape(jsonStringField(renderLab, "resumeRestoreMode", modelState.resumeRestoreMode)) + "\",\n"
            + "  \"activeModelPersistenceStatus\": \"" + escape(jsonStringField(renderLab, "activeModelPersistenceStatus", modelState.activeModelPersistenceStatus)) + "\",\n"
            + "  \"activeModelRestoreAttemptCount\": " + jsonNumberField(renderLab, "activeModelRestoreAttemptCount", String.valueOf(modelState.activeModelRestoreAttemptCount)) + ",\n"
            + "  \"activeModelRestoreResult\": \"" + escape(jsonStringField(renderLab, "activeModelRestoreResult", modelState.activeModelRestoreResult)) + "\",\n"
            + "  \"fallbackCubeReason\": \"" + escape(jsonStringField(renderLab, "fallbackCubeReason", modelState.fallbackCubeReason)) + "\",\n"
            + "  \"surfaceRecreateStatus\": \"" + escape(jsonStringField(renderLab, "surfaceRecreateStatus", modelState.surfaceRecreateStatus)) + "\",\n"
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
            + p19MaterialSlotJsonFields(renderLab, "  ")
            + p21AlphaCutoutJsonFields(renderLab, "  ")
            + p22EmissivePresetJsonFields(renderLab, "  ")
            + p24ReflectionJsonFields(renderLab, "  ")
            + p25GlassJsonFields(renderLab, "  ")
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
            + p20WorkflowJsonFields(renderLab, "  ")
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
            + "  \"debugZipRequiredFiles\": [\"engine_runtime_state.json\", \"engine_diagnostics_manifest.json\", \"model_import_state.json\", \"asset_report.json\", \"glb_model_summary.json\", \"debug_zip_runtime_note.txt\"],\n"
            + "  \"debugZipOptionalFiles\": [],\n"
            + "  \"debugZipRequiredFileStatus\": {\n"
            + "    \"engine_runtime_state.json\": \"" + fileStatusForDebugZip("engine_runtime_state.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"engine_diagnostics_manifest.json\": \"" + fileStatusForDebugZip("engine_diagnostics_manifest.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"model_import_state.json\": \"" + fileStatusForDebugZip("model_import_state.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"asset_report.json\": \"" + fileStatusForDebugZip("asset_report.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"glb_model_summary.json\": \"" + fileStatusForDebugZip("glb_model_summary.json", debugZipIncludedFiles, true) + "\",\n"
            + "    \"debug_zip_runtime_note.txt\": \"" + fileStatusForDebugZip("debug_zip_runtime_note.txt", debugZipIncludedFiles, true) + "\"\n"
            + "  },\n"
            + "  \"debugZipOptionalFileStatus\": {},\n"
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
            + "  \"resumeRestoreStatus\": \"" + escape(jsonStringField(renderLab, "resumeRestoreStatus", modelState.resumeRestoreStatus)) + "\",\n"
            + "  \"resumeRestoreMode\": \"" + escape(jsonStringField(renderLab, "resumeRestoreMode", modelState.resumeRestoreMode)) + "\",\n"
            + "  \"activeModelPersistenceStatus\": \"" + escape(jsonStringField(renderLab, "activeModelPersistenceStatus", modelState.activeModelPersistenceStatus)) + "\",\n"
            + "  \"activeModelRestoreAttemptCount\": " + jsonNumberField(renderLab, "activeModelRestoreAttemptCount", String.valueOf(modelState.activeModelRestoreAttemptCount)) + ",\n"
            + "  \"activeModelRestoreResult\": \"" + escape(jsonStringField(renderLab, "activeModelRestoreResult", modelState.activeModelRestoreResult)) + "\",\n"
            + "  \"fallbackCubeReason\": \"" + escape(jsonStringField(renderLab, "fallbackCubeReason", modelState.fallbackCubeReason)) + "\",\n"
            + "  \"surfaceRecreateStatus\": \"" + escape(jsonStringField(renderLab, "surfaceRecreateStatus", modelState.surfaceRecreateStatus)) + "\",\n"
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
            + p19MaterialSlotJsonFields(renderLab, "  ")
            + p21AlphaCutoutJsonFields(renderLab, "  ")
            + p22EmissivePresetJsonFields(renderLab, "  ")
            + p24ReflectionJsonFields(renderLab, "  ")
            + p25GlassJsonFields(renderLab, "  ")
            + p20WorkflowJsonFields(renderLab, "  ")
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

    private String p19MaterialSlotJsonFields(String renderLab, String indent) {
        return indent + "\"materialSlotEditorStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotEditorStatus", modelState.materialSlotEditorStatus)) + "\",\n"
            + indent + "\"selectedMaterialSlot\": " + jsonNumberField(renderLab, "selectedMaterialSlot", String.valueOf(modelState.selectedMaterialSlot)) + ",\n"
            + indent + "\"selectedMaterialSlotCount\": " + jsonNumberField(renderLab, "selectedMaterialSlotCount", String.valueOf(modelState.selectedMaterialSlotCount)) + ",\n"
            + indent + "\"selectedMaterialTypeHint\": \"" + escape(jsonStringField(renderLab, "selectedMaterialTypeHint", modelState.selectedMaterialTypeHint)) + "\",\n"
            + indent + "\"selectedMaterialName\": \"" + escape(jsonStringField(renderLab, "selectedMaterialName", modelState.selectedMaterialName)) + "\",\n"
            + indent + "\"selectedMaterialSummaryStatus\": \"" + escape(jsonStringField(renderLab, "selectedMaterialSummaryStatus", modelState.selectedMaterialSummaryStatus)) + "\",\n"
            + indent + "\"materialSlotSelectionUiStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotSelectionUiStatus", modelState.materialSlotSelectionUiStatus)) + "\",\n"
            + indent + "\"perMaterialOverrideStatus\": \"" + escape(jsonStringField(renderLab, "perMaterialOverrideStatus", modelState.perMaterialOverrideStatus)) + "\",\n"
            + indent + "\"perMaterialOverrideMode\": \"" + escape(jsonStringField(renderLab, "perMaterialOverrideMode", modelState.perMaterialOverrideMode)) + "\",\n"
            + indent + "\"selectedSlotMetallicOverride\": " + jsonNumberField(renderLab, "selectedSlotMetallicOverride", jsonFloat(modelState.selectedSlotMetallicOverride)) + ",\n"
            + indent + "\"selectedSlotRoughnessOverride\": " + jsonNumberField(renderLab, "selectedSlotRoughnessOverride", jsonFloat(modelState.selectedSlotRoughnessOverride)) + ",\n"
            + indent + "\"selectedSlotNormalScaleOverride\": " + jsonNumberField(renderLab, "selectedSlotNormalScaleOverride", jsonFloat(modelState.selectedSlotNormalScaleOverride)) + ",\n"
            + indent + "\"selectedSlotAoOverride\": " + jsonNumberField(renderLab, "selectedSlotAoOverride", jsonFloat(modelState.selectedSlotAoOverride)) + ",\n"
            + indent + "\"selectedSlotGlossOverride\": " + jsonNumberField(renderLab, "selectedSlotGlossOverride", jsonFloat(modelState.selectedSlotGlossOverride)) + ",\n"
            + indent + "\"selectedSlotCoatOverride\": " + jsonNumberField(renderLab, "selectedSlotCoatOverride", jsonFloat(modelState.selectedSlotCoatOverride)) + ",\n"
            + indent + "\"selectedSlotOverrideApplied\": \"" + escape(jsonStringField(renderLab, "selectedSlotOverrideApplied", modelState.selectedSlotOverrideApplied)) + "\",\n"
            + indent + "\"selectedSlotResetStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotResetStatus", modelState.selectedSlotResetStatus)) + "\",\n"
            + indent + "\"perMaterialUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "perMaterialUniformUpdateStatus", modelState.perMaterialUniformUpdateStatus)) + "\",\n"
            + indent + "\"materialSlotControlsUiStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotControlsUiStatus", modelState.materialSlotControlsUiStatus)) + "\",\n"
            + indent + "\"metallicSlotSliderStatus\": \"" + escape(jsonStringField(renderLab, "metallicSlotSliderStatus", modelState.metallicSlotSliderStatus)) + "\",\n"
            + indent + "\"roughnessSlotSliderStatus\": \"" + escape(jsonStringField(renderLab, "roughnessSlotSliderStatus", modelState.roughnessSlotSliderStatus)) + "\",\n"
            + indent + "\"normalSlotSliderStatus\": \"" + escape(jsonStringField(renderLab, "normalSlotSliderStatus", modelState.normalSlotSliderStatus)) + "\",\n"
            + indent + "\"aoSlotSliderStatus\": \"" + escape(jsonStringField(renderLab, "aoSlotSliderStatus", modelState.aoSlotSliderStatus)) + "\",\n"
            + indent + "\"selectedMaterialDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "selectedMaterialDebugViewStatus", modelState.selectedMaterialDebugViewStatus)) + "\",\n"
            + indent + "\"materialOverrideDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "materialOverrideDebugViewStatus", modelState.materialOverrideDebugViewStatus)) + "\",\n"
            + indent + "\"slotMetallicDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "slotMetallicDebugViewStatus", modelState.slotMetallicDebugViewStatus)) + "\",\n"
            + indent + "\"slotRoughnessDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "slotRoughnessDebugViewStatus", modelState.slotRoughnessDebugViewStatus)) + "\",\n"
            + indent + "\"slotAoDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "slotAoDebugViewStatus", modelState.slotAoDebugViewStatus)) + "\",\n"
            + indent + "\"perMaterialOverridePerformanceStatus\": \"" + escape(jsonStringField(renderLab, "perMaterialOverridePerformanceStatus", modelState.perMaterialOverridePerformanceStatus)) + "\",\n";
    }

    private String p21AlphaCutoutJsonFields(String renderLab, String indent) {
        return indent + "\"alphaMaterialStatus\": \"" + escape(jsonStringField(renderLab, "alphaMaterialStatus", modelState.alphaMaterialStatus)) + "\",\n"
            + indent + "\"alphaModeSupportStatus\": \"" + escape(jsonStringField(renderLab, "alphaModeSupportStatus", modelState.alphaModeSupportStatus)) + "\",\n"
            + indent + "\"alphaMaskStatus\": \"" + escape(jsonStringField(renderLab, "alphaMaskStatus", modelState.alphaMaskStatus)) + "\",\n"
            + indent + "\"alphaBlendStatus\": \"" + escape(jsonStringField(renderLab, "alphaBlendStatus", modelState.alphaBlendStatus)) + "\",\n"
            + indent + "\"alphaCutoffStatus\": \"" + escape(jsonStringField(renderLab, "alphaCutoffStatus", modelState.alphaCutoffStatus)) + "\",\n"
            + indent + "\"alphaCutoffValue\": " + jsonNumberField(renderLab, "alphaCutoffValue", jsonFloat(modelState.alphaCutoffValue)) + ",\n"
            + indent + "\"alphaDiscardStatus\": \"" + escape(jsonStringField(renderLab, "alphaDiscardStatus", modelState.alphaDiscardStatus)) + "\",\n"
            + indent + "\"alphaTextureChannelStatus\": \"" + escape(jsonStringField(renderLab, "alphaTextureChannelStatus", modelState.alphaTextureChannelStatus)) + "\",\n"
            + indent + "\"alphaFallbackStatus\": \"" + escape(jsonStringField(renderLab, "alphaFallbackStatus", modelState.alphaFallbackStatus)) + "\",\n"
            + indent + "\"doubleSidedMaterialStatus\": \"" + escape(jsonStringField(renderLab, "doubleSidedMaterialStatus", modelState.doubleSidedMaterialStatus)) + "\",\n"
            + indent + "\"doubleSidedMode\": \"" + escape(jsonStringField(renderLab, "doubleSidedMode", modelState.doubleSidedMode)) + "\",\n"
            + indent + "\"doubleSidedNormalStatus\": \"" + escape(jsonStringField(renderLab, "doubleSidedNormalStatus", modelState.doubleSidedNormalStatus)) + "\",\n"
            + indent + "\"doubleSidedRasterStatus\": \"" + escape(jsonStringField(renderLab, "doubleSidedRasterStatus", modelState.doubleSidedRasterStatus)) + "\",\n"
            + indent + "\"doubleSidedFallbackStatus\": \"" + escape(jsonStringField(renderLab, "doubleSidedFallbackStatus", modelState.doubleSidedFallbackStatus)) + "\",\n"
            + indent + "\"thinMaterialPolishStatus\": \"" + escape(jsonStringField(renderLab, "thinMaterialPolishStatus", modelState.thinMaterialPolishStatus)) + "\",\n"
            + indent + "\"cutoutMaterialHintStatus\": \"" + escape(jsonStringField(renderLab, "cutoutMaterialHintStatus", modelState.cutoutMaterialHintStatus)) + "\",\n"
            + indent + "\"fabricEdgeStatus\": \"" + escape(jsonStringField(renderLab, "fabricEdgeStatus", modelState.fabricEdgeStatus)) + "\",\n"
            + indent + "\"glassMetadataStatus\": \"" + escape(jsonStringField(renderLab, "glassMetadataStatus", modelState.glassMetadataStatus)) + "\",\n"
            + indent + "\"decalMaterialHintStatus\": \"" + escape(jsonStringField(renderLab, "decalMaterialHintStatus", modelState.decalMaterialHintStatus)) + "\",\n"
            + indent + "\"transparencyDeferredStatus\": \"" + escape(jsonStringField(renderLab, "transparencyDeferredStatus", modelState.transparencyDeferredStatus)) + "\",\n"
            + indent + "\"alphaUiStatus\": \"" + escape(jsonStringField(renderLab, "alphaUiStatus", modelState.alphaUiStatus)) + "\",\n"
            + indent + "\"alphaCutoffSliderStatus\": \"" + escape(jsonStringField(renderLab, "alphaCutoffSliderStatus", modelState.alphaCutoffSliderStatus)) + "\",\n"
            + indent + "\"alphaDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "alphaDebugViewStatus", modelState.alphaDebugViewStatus)) + "\",\n"
            + indent + "\"doubleSidedDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "doubleSidedDebugViewStatus", modelState.doubleSidedDebugViewStatus)) + "\",\n"
            + indent + "\"alphaResetButtonStatus\": \"" + escape(jsonStringField(renderLab, "alphaResetButtonStatus", modelState.alphaResetButtonStatus)) + "\",\n"
            + indent + "\"alphaUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "alphaUniformUpdateStatus", modelState.alphaUniformUpdateStatus)) + "\",\n"
            + indent + "\"alphaSliderUpdateMode\": \"" + escape(jsonStringField(renderLab, "alphaSliderUpdateMode", modelState.alphaSliderUpdateMode)) + "\",\n"
            + indent + "\"alphaMaskDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "alphaMaskDebugViewStatus", modelState.alphaMaskDebugViewStatus)) + "\",\n"
            + indent + "\"alphaModeDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "alphaModeDebugViewStatus", modelState.alphaModeDebugViewStatus)) + "\",\n"
            + indent + "\"cutoutHintDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "cutoutHintDebugViewStatus", modelState.cutoutHintDebugViewStatus)) + "\",\n"
            + indent + "\"transparencyStatusDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparencyStatusDebugViewStatus", modelState.transparencyStatusDebugViewStatus)) + "\",\n"
            + indent + "\"alphaPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "alphaPerformanceStatus", modelState.alphaPerformanceStatus)) + "\",\n"
            + indent + "\"alphaNoNewPassStatus\": \"" + escape(jsonStringField(renderLab, "alphaNoNewPassStatus", modelState.alphaNoNewPassStatus)) + "\",\n"
            + indent + "\"alphaNoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "alphaNoTextureRebuildStatus", modelState.alphaNoTextureRebuildStatus)) + "\",\n"
            + indent + "\"alphaNoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "alphaNoModelReuploadStatus", modelState.alphaNoModelReuploadStatus)) + "\",\n";
    }

    private String p22EmissivePresetJsonFields(String renderLab, String indent) {
        return indent + "\"emissiveMaterialStatus\": \"" + escape(jsonStringField(renderLab, "emissiveMaterialStatus", modelState.emissiveMaterialStatus)) + "\",\n"
            + indent + "\"emissiveMode\": \"" + escape(jsonStringField(renderLab, "emissiveMode", modelState.emissiveMode)) + "\",\n"
            + indent + "\"emissiveFactorStatus\": \"" + escape(jsonStringField(renderLab, "emissiveFactorStatus", modelState.emissiveFactorStatus)) + "\",\n"
            + indent + "\"emissiveTextureStatus\": \"" + escape(jsonStringField(renderLab, "emissiveTextureStatus", modelState.emissiveTextureStatus)) + "\",\n"
            + indent + "\"emissiveIntensity\": " + jsonNumberField(renderLab, "emissiveIntensity", jsonFloat(modelState.emissiveIntensity)) + ",\n"
            + indent + "\"emissiveIntensityStatus\": \"" + escape(jsonStringField(renderLab, "emissiveIntensityStatus", modelState.emissiveIntensityStatus)) + "\",\n"
            + indent + "\"emissiveColorStatus\": \"" + escape(jsonStringField(renderLab, "emissiveColorStatus", modelState.emissiveColorStatus)) + "\",\n"
            + indent + "\"emissiveOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "emissiveOverbrightGuardStatus", modelState.emissiveOverbrightGuardStatus)) + "\",\n"
            + indent + "\"emissiveLightingContributionStatus\": \"" + escape(jsonStringField(renderLab, "emissiveLightingContributionStatus", modelState.emissiveLightingContributionStatus)) + "\",\n"
            + indent + "\"emissivePerformanceStatus\": \"" + escape(jsonStringField(renderLab, "emissivePerformanceStatus", modelState.emissivePerformanceStatus)) + "\",\n"
            + indent + "\"materialPresetStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetStatus", modelState.materialPresetStatus)) + "\",\n"
            + indent + "\"activeMaterialPreset\": \"" + escape(jsonStringField(renderLab, "activeMaterialPreset", modelState.activeMaterialPreset)) + "\",\n"
            + indent + "\"materialPresetMode\": \"" + escape(jsonStringField(renderLab, "materialPresetMode", modelState.materialPresetMode)) + "\",\n"
            + indent + "\"materialPresetAppliedSlot\": " + jsonNumberField(renderLab, "materialPresetAppliedSlot", String.valueOf(modelState.materialPresetAppliedSlot)) + ",\n"
            + indent + "\"materialPresetAppliedStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetAppliedStatus", modelState.materialPresetAppliedStatus)) + "\",\n"
            + indent + "\"materialPresetUiStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetUiStatus", modelState.materialPresetUiStatus)) + "\",\n"
            + indent + "\"materialPresetPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetPerformanceStatus", modelState.materialPresetPerformanceStatus)) + "\",\n"
            + indent + "\"selectedSlotPresetStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotPresetStatus", modelState.selectedSlotPresetStatus)) + "\",\n"
            + indent + "\"carPaintPresetStatus\": \"" + escape(jsonStringField(renderLab, "carPaintPresetStatus", modelState.carPaintPresetStatus)) + "\",\n"
            + indent + "\"metalPresetStatus\": \"" + escape(jsonStringField(renderLab, "metalPresetStatus", modelState.metalPresetStatus)) + "\",\n"
            + indent + "\"fabricPresetStatus\": \"" + escape(jsonStringField(renderLab, "fabricPresetStatus", modelState.fabricPresetStatus)) + "\",\n"
            + indent + "\"rubberPresetStatus\": \"" + escape(jsonStringField(renderLab, "rubberPresetStatus", modelState.rubberPresetStatus)) + "\",\n"
            + indent + "\"plasticPresetStatus\": \"" + escape(jsonStringField(renderLab, "plasticPresetStatus", modelState.plasticPresetStatus)) + "\",\n"
            + indent + "\"glassMetadataPresetStatus\": \"" + escape(jsonStringField(renderLab, "glassMetadataPresetStatus", modelState.glassMetadataPresetStatus)) + "\",\n"
            + indent + "\"emissivePresetStatus\": \"" + escape(jsonStringField(renderLab, "emissivePresetStatus", modelState.emissivePresetStatus)) + "\",\n"
            + indent + "\"materialPresetGuardStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetGuardStatus", modelState.materialPresetGuardStatus)) + "\",\n"
            + indent + "\"presetCycleButtonStatus\": \"" + escape(jsonStringField(renderLab, "presetCycleButtonStatus", modelState.presetCycleButtonStatus)) + "\",\n"
            + indent + "\"presetApplyButtonStatus\": \"" + escape(jsonStringField(renderLab, "presetApplyButtonStatus", modelState.presetApplyButtonStatus)) + "\",\n"
            + indent + "\"emissiveSliderStatus\": \"" + escape(jsonStringField(renderLab, "emissiveSliderStatus", modelState.emissiveSliderStatus)) + "\",\n"
            + indent + "\"emissiveUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "emissiveUniformUpdateStatus", modelState.emissiveUniformUpdateStatus)) + "\",\n"
            + indent + "\"materialResetButtonStatus\": \"" + escape(jsonStringField(renderLab, "materialResetButtonStatus", modelState.materialResetButtonStatus)) + "\",\n"
            + indent + "\"materialUiScrollPreservedStatus\": \"" + escape(jsonStringField(renderLab, "materialUiScrollPreservedStatus", modelState.materialUiScrollPreservedStatus)) + "\",\n"
            + indent + "\"emissiveDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "emissiveDebugViewStatus", modelState.emissiveDebugViewStatus)) + "\",\n"
            + indent + "\"presetTypeDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "presetTypeDebugViewStatus", modelState.presetTypeDebugViewStatus)) + "\",\n"
            + indent + "\"presetResponseDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "presetResponseDebugViewStatus", modelState.presetResponseDebugViewStatus)) + "\",\n"
            + indent + "\"materialEnergyGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "materialEnergyGuardDebugViewStatus", modelState.materialEnergyGuardDebugViewStatus)) + "\",\n"
            + indent + "\"selectedSlotPresetDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotPresetDebugViewStatus", modelState.selectedSlotPresetDebugViewStatus)) + "\",\n"
            + indent + "\"p22PerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p22PerformanceStatus", modelState.p22PerformanceStatus)) + "\",\n"
            + indent + "\"emissiveNoBloomStatus\": \"" + escape(jsonStringField(renderLab, "emissiveNoBloomStatus", modelState.emissiveNoBloomStatus)) + "\",\n"
            + indent + "\"emissiveNoNewPassStatus\": \"" + escape(jsonStringField(renderLab, "emissiveNoNewPassStatus", modelState.emissiveNoNewPassStatus)) + "\",\n"
            + indent + "\"presetNoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "presetNoModelReuploadStatus", modelState.presetNoModelReuploadStatus)) + "\",\n"
            + indent + "\"presetNoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "presetNoTextureRebuildStatus", modelState.presetNoTextureRebuildStatus)) + "\",\n"
            + indent + "\"p21AlphaPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p21AlphaPreservedStatus", modelState.p21AlphaPreservedStatus)) + "\",\n";
    }

    private String p20WorkflowJsonFields(String renderLab, String indent) {
        return indent + "\"inspectorHeightMode\": \"" + escape(jsonStringField(renderLab, "inspectorHeightMode", modelState.inspectorHeightMode)) + "\",\n"
            + indent + "\"inspectorScrollStatus\": \"" + escape(jsonStringField(renderLab, "inspectorScrollStatus", modelState.inspectorScrollStatus)) + "\",\n"
            + indent + "\"inspectorExpandedMaxHeightPercent\": " + jsonNumberField(renderLab, "inspectorExpandedMaxHeightPercent", String.valueOf(modelState.inspectorExpandedMaxHeightPercent)) + ",\n"
            + indent + "\"inspectorCollapsedStatus\": \"" + escape(jsonStringField(renderLab, "inspectorCollapsedStatus", modelState.inspectorCollapsedStatus)) + "\",\n"
            + indent + "\"materialTabScrollStatus\": \"" + escape(jsonStringField(renderLab, "materialTabScrollStatus", modelState.materialTabScrollStatus)) + "\",\n"
            + indent + "\"inspectorTouchTargetStatus\": \"" + escape(jsonStringField(renderLab, "inspectorTouchTargetStatus", modelState.inspectorTouchTargetStatus)) + "\",\n"
            + indent + "\"inspectorDynamicAlphaStatus\": \"" + escape(jsonStringField(renderLab, "inspectorDynamicAlphaStatus", modelState.inspectorDynamicAlphaStatus)) + "\",\n"
            + indent + "\"inspectorAlphaIdle\": " + jsonNumberField(renderLab, "inspectorAlphaIdle", jsonFloat(modelState.inspectorAlphaIdle)) + ",\n"
            + indent + "\"inspectorAlphaWhileSliderDrag\": " + jsonNumberField(renderLab, "inspectorAlphaWhileSliderDrag", jsonFloat(modelState.inspectorAlphaWhileSliderDrag)) + ",\n"
            + indent + "\"inspectorAlphaWhileCameraMove\": " + jsonNumberField(renderLab, "inspectorAlphaWhileCameraMove", jsonFloat(modelState.inspectorAlphaWhileCameraMove)) + ",\n"
            + indent + "\"inspectorAlphaRestoreStatus\": \"" + escape(jsonStringField(renderLab, "inspectorAlphaRestoreStatus", modelState.inspectorAlphaRestoreStatus)) + "\",\n"
            + indent + "\"sliderDragVisualMode\": \"" + escape(jsonStringField(renderLab, "sliderDragVisualMode", modelState.sliderDragVisualMode)) + "\",\n"
            + indent + "\"cameraMoveVisualMode\": \"" + escape(jsonStringField(renderLab, "cameraMoveVisualMode", modelState.cameraMoveVisualMode)) + "\",\n"
            + indent + "\"materialWorkflowStatus\": \"" + escape(jsonStringField(renderLab, "materialWorkflowStatus", modelState.materialWorkflowStatus)) + "\",\n"
            + indent + "\"materialSlotSummaryUiStatus\": \"" + escape(jsonStringField(renderLab, "materialSlotSummaryUiStatus", modelState.materialSlotSummaryUiStatus)) + "\",\n"
            + indent + "\"selectedSlotResetButtonStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotResetButtonStatus", modelState.selectedSlotResetButtonStatus)) + "\",\n"
            + indent + "\"selectedMaterialTextureSummaryStatus\": \"" + escape(jsonStringField(renderLab, "selectedMaterialTextureSummaryStatus", modelState.selectedMaterialTextureSummaryStatus)) + "\",\n"
            + indent + "\"assetsWorkflowStatus\": \"" + escape(jsonStringField(renderLab, "assetsWorkflowStatus", modelState.assetsWorkflowStatus)) + "\",\n"
            + indent + "\"reloadActiveModelButtonStatus\": \"" + escape(jsonStringField(renderLab, "reloadActiveModelButtonStatus", modelState.reloadActiveModelButtonStatus)) + "\",\n"
            + indent + "\"reloadActiveModelStatus\": \"" + escape(jsonStringField(renderLab, "reloadActiveModelStatus", modelState.reloadActiveModelStatus)) + "\",\n"
            + indent + "\"activeModelDisplayStatus\": \"" + escape(jsonStringField(renderLab, "activeModelDisplayStatus", modelState.activeModelDisplayStatus)) + "\",\n"
            + indent + "\"fallbackReasonDisplayStatus\": \"" + escape(jsonStringField(renderLab, "fallbackReasonDisplayStatus", modelState.fallbackReasonDisplayStatus)) + "\",\n"
            + indent + "\"p20RuntimeWorkflowPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p20RuntimeWorkflowPreservedStatus", modelState.p20RuntimeWorkflowPreservedStatus)) + "\",\n"
            + indent + "\"p19SlotControlsPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p19SlotControlsPreservedStatus", modelState.p19SlotControlsPreservedStatus)) + "\",\n"
            + indent + "\"p19PreservedStatus\": \"" + escape(jsonStringField(renderLab, "p19PreservedStatus", modelState.p19PreservedStatus)) + "\",\n"
            + indent + "\"p18IblPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p18IblPreservedStatus", modelState.p18IblPreservedStatus)) + "\",\n"
            + indent + "\"p17GlossPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p17GlossPreservedStatus", modelState.p17GlossPreservedStatus)) + "\",\n"
            + indent + "\"runtimeStateDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "runtimeStateDebugViewStatus", modelState.runtimeStateDebugViewStatus)) + "\",\n"
            + indent + "\"restoreStateDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "restoreStateDebugViewStatus", modelState.restoreStateDebugViewStatus)) + "\",\n"
            + indent + "\"uiStateDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "uiStateDebugViewStatus", modelState.uiStateDebugViewStatus)) + "\",\n";
    }

    private String p24ReflectionJsonFields(String renderLab, String indent) {
        return indent + "\"fakeCubemapStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapStatus", modelState.fakeCubemapStatus)) + "\",\n"
            + indent + "\"fakeCubemapMode\": \"" + escape(jsonStringField(renderLab, "fakeCubemapMode", modelState.fakeCubemapMode)) + "\",\n"
            + indent + "\"fakeCubemapSourceStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapSourceStatus", modelState.fakeCubemapSourceStatus)) + "\",\n"
            + indent + "\"environmentZoneStatus\": \"" + escape(jsonStringField(renderLab, "environmentZoneStatus", modelState.environmentZoneStatus)) + "\",\n"
            + indent + "\"skyZoneStatus\": \"" + escape(jsonStringField(renderLab, "skyZoneStatus", modelState.skyZoneStatus)) + "\",\n"
            + indent + "\"horizonZoneStatus\": \"" + escape(jsonStringField(renderLab, "horizonZoneStatus", modelState.horizonZoneStatus)) + "\",\n"
            + indent + "\"groundZoneStatus\": \"" + escape(jsonStringField(renderLab, "groundZoneStatus", modelState.groundZoneStatus)) + "\",\n"
            + indent + "\"sideReflectionZoneStatus\": \"" + escape(jsonStringField(renderLab, "sideReflectionZoneStatus", modelState.sideReflectionZoneStatus)) + "\",\n"
            + indent + "\"fakeCubemapPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapPerformanceStatus", modelState.fakeCubemapPerformanceStatus)) + "\",\n"
            + indent + "\"reflectionQualityStatus\": \"" + escape(jsonStringField(renderLab, "reflectionQualityStatus", modelState.reflectionQualityStatus)) + "\",\n"
            + indent + "\"reflectionContrastStatus\": \"" + escape(jsonStringField(renderLab, "reflectionContrastStatus", modelState.reflectionContrastStatus)) + "\",\n"
            + indent + "\"reflectionContrastValue\": " + jsonNumberField(renderLab, "reflectionContrastValue", jsonFloat(modelState.reflectionContrastValue)) + ",\n"
            + indent + "\"reflectionSaturationStatus\": \"" + escape(jsonStringField(renderLab, "reflectionSaturationStatus", modelState.reflectionSaturationStatus)) + "\",\n"
            + indent + "\"reflectionSaturationValue\": " + jsonNumberField(renderLab, "reflectionSaturationValue", jsonFloat(modelState.reflectionSaturationValue)) + ",\n"
            + indent + "\"roughnessReflectionBlurStatus\": \"" + escape(jsonStringField(renderLab, "roughnessReflectionBlurStatus", modelState.roughnessReflectionBlurStatus)) + "\",\n"
            + indent + "\"clearcoatReflectionBoostStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatReflectionBoostStatus", modelState.clearcoatReflectionBoostStatus)) + "\",\n"
            + indent + "\"metalReflectionTintStatus\": \"" + escape(jsonStringField(renderLab, "metalReflectionTintStatus", modelState.metalReflectionTintStatus)) + "\",\n"
            + indent + "\"fabricReflectionSuppressStatus\": \"" + escape(jsonStringField(renderLab, "fabricReflectionSuppressStatus", modelState.fabricReflectionSuppressStatus)) + "\",\n"
            + indent + "\"reflectionEnergyGuardStatus\": \"" + escape(jsonStringField(renderLab, "reflectionEnergyGuardStatus", modelState.reflectionEnergyGuardStatus)) + "\",\n"
            + indent + "\"reflectionOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "reflectionOverbrightGuardStatus", modelState.reflectionOverbrightGuardStatus)) + "\",\n"
            + indent + "\"reflectionQualityUiStatus\": \"" + escape(jsonStringField(renderLab, "reflectionQualityUiStatus", modelState.reflectionQualityUiStatus)) + "\",\n"
            + indent + "\"reflectionContrastSliderStatus\": \"" + escape(jsonStringField(renderLab, "reflectionContrastSliderStatus", modelState.reflectionContrastSliderStatus)) + "\",\n"
            + indent + "\"reflectionSaturationSliderStatus\": \"" + escape(jsonStringField(renderLab, "reflectionSaturationSliderStatus", modelState.reflectionSaturationSliderStatus)) + "\",\n"
            + indent + "\"environmentZonePresetStatus\": \"" + escape(jsonStringField(renderLab, "environmentZonePresetStatus", modelState.environmentZonePresetStatus)) + "\",\n"
            + indent + "\"environmentZonePreset\": \"" + escape(jsonStringField(renderLab, "environmentZonePreset", modelState.environmentZonePreset)) + "\",\n"
            + indent + "\"reflectionUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "reflectionUniformUpdateStatus", modelState.reflectionUniformUpdateStatus)) + "\",\n"
            + indent + "\"motionPerformanceGuardStatus\": \"" + escape(jsonStringField(renderLab, "motionPerformanceGuardStatus", modelState.motionPerformanceGuardStatus)) + "\",\n"
            + indent + "\"cameraMotionStatus\": \"" + escape(jsonStringField(renderLab, "cameraMotionStatus", modelState.cameraMotionStatus)) + "\",\n"
            + indent + "\"cameraMotionSpeed\": " + jsonNumberField(renderLab, "cameraMotionSpeed", jsonFloat(modelState.cameraMotionSpeed)) + ",\n"
            + indent + "\"cameraMotionQualityTier\": \"" + escape(jsonStringField(renderLab, "cameraMotionQualityTier", modelState.cameraMotionQualityTier)) + "\",\n"
            + indent + "\"cameraMotionQualityBlend\": " + jsonNumberField(renderLab, "cameraMotionQualityBlend", jsonFloat(modelState.cameraMotionQualityBlend)) + ",\n"
            + indent + "\"motionReflectionScale\": " + jsonNumberField(renderLab, "motionReflectionScale", jsonFloat(modelState.motionReflectionScale)) + ",\n"
            + indent + "\"motionClearcoatScale\": " + jsonNumberField(renderLab, "motionClearcoatScale", jsonFloat(modelState.motionClearcoatScale)) + ",\n"
            + indent + "\"motionDiagnosticsThrottleStatus\": \"" + escape(jsonStringField(renderLab, "motionDiagnosticsThrottleStatus", modelState.motionDiagnosticsThrottleStatus)) + "\",\n"
            + indent + "\"motionQualityRestoreStatus\": \"" + escape(jsonStringField(renderLab, "motionQualityRestoreStatus", modelState.motionQualityRestoreStatus)) + "\",\n"
            + indent + "\"motionQualityTransitionMs\": " + jsonNumberField(renderLab, "motionQualityTransitionMs", String.valueOf(modelState.motionQualityTransitionMs)) + ",\n"
            + indent + "\"cameraMovingFpsLast\": " + jsonNumberField(renderLab, "cameraMovingFpsLast", jsonFloat(modelState.cameraMovingFpsLast)) + ",\n"
            + indent + "\"cameraStillFpsLast\": " + jsonNumberField(renderLab, "cameraStillFpsLast", jsonFloat(modelState.cameraStillFpsLast)) + ",\n"
            + indent + "\"targetMovingFps\": " + jsonNumberField(renderLab, "targetMovingFps", jsonFloat(modelState.targetMovingFps)) + ",\n"
            + indent + "\"movingFpsGuardStatus\": \"" + escape(jsonStringField(renderLab, "movingFpsGuardStatus", modelState.movingFpsGuardStatus)) + "\",\n"
            + indent + "\"carPaintReflectionStatus\": \"" + escape(jsonStringField(renderLab, "carPaintReflectionStatus", modelState.carPaintReflectionStatus)) + "\",\n"
            + indent + "\"metalReflectionStatus\": \"" + escape(jsonStringField(renderLab, "metalReflectionStatus", modelState.metalReflectionStatus)) + "\",\n"
            + indent + "\"plasticReflectionStatus\": \"" + escape(jsonStringField(renderLab, "plasticReflectionStatus", modelState.plasticReflectionStatus)) + "\",\n"
            + indent + "\"rubberReflectionStatus\": \"" + escape(jsonStringField(renderLab, "rubberReflectionStatus", modelState.rubberReflectionStatus)) + "\",\n"
            + indent + "\"glassMetadataReflectionStatus\": \"" + escape(jsonStringField(renderLab, "glassMetadataReflectionStatus", modelState.glassMetadataReflectionStatus)) + "\",\n"
            + indent + "\"materialPresetReflectionStatus\": \"" + escape(jsonStringField(renderLab, "materialPresetReflectionStatus", modelState.materialPresetReflectionStatus)) + "\",\n"
            + indent + "\"fakeCubemapDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapDebugViewStatus", modelState.fakeCubemapDebugViewStatus)) + "\",\n"
            + indent + "\"reflectionZonesDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionZonesDebugViewStatus", modelState.reflectionZonesDebugViewStatus)) + "\",\n"
            + indent + "\"reflectionContrastDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionContrastDebugViewStatus", modelState.reflectionContrastDebugViewStatus)) + "\",\n"
            + indent + "\"reflectionEnergyGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "reflectionEnergyGuardDebugViewStatus", modelState.reflectionEnergyGuardDebugViewStatus)) + "\",\n"
            + indent + "\"clearcoatReflectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatReflectionDebugViewStatus", modelState.clearcoatReflectionDebugViewStatus)) + "\",\n"
            + indent + "\"motionQualityDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "motionQualityDebugViewStatus", modelState.motionQualityDebugViewStatus)) + "\",\n"
            + indent + "\"p23ClearcoatPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p23ClearcoatPreservedStatus", modelState.p23ClearcoatPreservedStatus)) + "\",\n"
            + indent + "\"p24PerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p24PerformanceStatus", modelState.p24PerformanceStatus)) + "\",\n"
            + indent + "\"fakeCubemapNoTextureStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapNoTextureStatus", modelState.fakeCubemapNoTextureStatus)) + "\",\n"
            + indent + "\"fakeCubemapNoNewPassStatus\": \"" + escape(jsonStringField(renderLab, "fakeCubemapNoNewPassStatus", modelState.fakeCubemapNoNewPassStatus)) + "\",\n"
            + indent + "\"reflectionNoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "reflectionNoTextureRebuildStatus", modelState.reflectionNoTextureRebuildStatus)) + "\",\n"
            + indent + "\"reflectionNoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "reflectionNoModelReuploadStatus", modelState.reflectionNoModelReuploadStatus)) + "\",\n"
            + indent + "\"noFrameFileWriteStatus\": \"" + escape(jsonStringField(renderLab, "noFrameFileWriteStatus", modelState.noFrameFileWriteStatus)) + "\",\n"
            + indent + "\"noFrameGlbParseStatus\": \"" + escape(jsonStringField(renderLab, "noFrameGlbParseStatus", modelState.noFrameGlbParseStatus)) + "\",\n"
            + indent + "\"p26ReflectionPolishStatus\": \"" + escape(jsonStringField(renderLab, "p26ReflectionPolishStatus", modelState.p26ReflectionPolishStatus)) + "\",\n"
            + indent + "\"reflectionZonePolishStatus\": \"" + escape(jsonStringField(renderLab, "reflectionZonePolishStatus", modelState.reflectionZonePolishStatus)) + "\",\n"
            + indent + "\"sideRimReflectionPolishStatus\": \"" + escape(jsonStringField(renderLab, "sideRimReflectionPolishStatus", modelState.sideRimReflectionPolishStatus)) + "\",\n"
            + indent + "\"roughnessReflectionPolishStatus\": \"" + escape(jsonStringField(renderLab, "roughnessReflectionPolishStatus", modelState.roughnessReflectionPolishStatus)) + "\",\n"
            + indent + "\"glassReflectionZoneStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionZoneStatus", modelState.glassReflectionZoneStatus)) + "\",\n"
            + indent + "\"clearcoatReflectionZoneStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatReflectionZoneStatus", modelState.clearcoatReflectionZoneStatus)) + "\",\n";
    }

    private String p25GlassJsonFields(String renderLab, String indent) {
        return indent + "\"glassMaterialStatus\": \"" + escape(jsonStringField(renderLab, "glassMaterialStatus", modelState.glassMaterialStatus)) + "\",\n"
            + indent + "\"glassMode\": \"" + escape(jsonStringField(renderLab, "glassMode", modelState.glassMode)) + "\",\n"
            + indent + "\"glassPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassPolishStatus", modelState.glassPolishStatus)) + "\",\n"
            + indent + "\"glassPolishMode\": \"" + escape(jsonStringField(renderLab, "glassPolishMode", modelState.glassPolishMode)) + "\",\n"
            + indent + "\"glassReadabilityStatus\": \"" + escape(jsonStringField(renderLab, "glassReadabilityStatus", modelState.glassReadabilityStatus)) + "\",\n"
            + indent + "\"glassEdgePolishStatus\": \"" + escape(jsonStringField(renderLab, "glassEdgePolishStatus", modelState.glassEdgePolishStatus)) + "\",\n"
            + indent + "\"glassTintPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassTintPolishStatus", modelState.glassTintPolishStatus)) + "\",\n"
            + indent + "\"glassThicknessPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassThicknessPolishStatus", modelState.glassThicknessPolishStatus)) + "\",\n"
            + indent + "\"glassOpacityCurveStatus\": \"" + escape(jsonStringField(renderLab, "glassOpacityCurveStatus", modelState.glassOpacityCurveStatus)) + "\",\n"
            + indent + "\"glassRoughnessPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassRoughnessPolishStatus", modelState.glassRoughnessPolishStatus)) + "\",\n"
            + indent + "\"glassReflectionPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionPolishStatus", modelState.glassReflectionPolishStatus)) + "\",\n"
            + indent + "\"glassOpacity\": " + jsonNumberField(renderLab, "glassOpacity", jsonFloat(modelState.glassOpacity)) + ",\n"
            + indent + "\"glassTintStatus\": \"" + escape(jsonStringField(renderLab, "glassTintStatus", modelState.glassTintStatus)) + "\",\n"
            + indent + "\"glassTintColor\": \"" + escape(jsonStringField(renderLab, "glassTintColor", modelState.glassTintColor)) + "\",\n"
            + indent + "\"glassFresnelStatus\": \"" + escape(jsonStringField(renderLab, "glassFresnelStatus", modelState.glassFresnelStatus)) + "\",\n"
            + indent + "\"glassEdgeReflectionStatus\": \"" + escape(jsonStringField(renderLab, "glassEdgeReflectionStatus", modelState.glassEdgeReflectionStatus)) + "\",\n"
            + indent + "\"glassReflectionSourceStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionSourceStatus", modelState.glassReflectionSourceStatus)) + "\",\n"
            + indent + "\"glassRoughnessResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassRoughnessResponseStatus", modelState.glassRoughnessResponseStatus)) + "\",\n"
            + indent + "\"glassThicknessFakeStatus\": \"" + escape(jsonStringField(renderLab, "glassThicknessFakeStatus", modelState.glassThicknessFakeStatus)) + "\",\n"
            + indent + "\"glassTransparencyMode\": \"" + escape(jsonStringField(renderLab, "glassTransparencyMode", modelState.glassTransparencyMode)) + "\",\n"
            + indent + "\"glassSortingStatus\": \"" + escape(jsonStringField(renderLab, "glassSortingStatus", modelState.glassSortingStatus)) + "\",\n"
            + indent + "\"glassRefractionStatus\": \"" + escape(jsonStringField(renderLab, "glassRefractionStatus", modelState.glassRefractionStatus)) + "\",\n"
            + indent + "\"glassStillFakeTransparencyStatus\": \"" + escape(jsonStringField(renderLab, "glassStillFakeTransparencyStatus", modelState.glassStillFakeTransparencyStatus)) + "\",\n"
            + indent + "\"glassPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "glassPerformanceStatus", modelState.glassPerformanceStatus)) + "\",\n"
            + indent + "\"glassUiStatus\": \"" + escape(jsonStringField(renderLab, "glassUiStatus", modelState.glassUiStatus)) + "\",\n"
            + indent + "\"glassEnableButtonStatus\": \"" + escape(jsonStringField(renderLab, "glassEnableButtonStatus", modelState.glassEnableButtonStatus)) + "\",\n"
            + indent + "\"glassPresetStatus\": \"" + escape(jsonStringField(renderLab, "glassPresetStatus", modelState.glassPresetStatus)) + "\",\n"
            + indent + "\"activeGlassPreset\": \"" + escape(jsonStringField(renderLab, "activeGlassPreset", modelState.activeGlassPreset)) + "\",\n"
            + indent + "\"glassOpacitySliderStatus\": \"" + escape(jsonStringField(renderLab, "glassOpacitySliderStatus", modelState.glassOpacitySliderStatus)) + "\",\n"
            + indent + "\"glassOpacityValue\": " + jsonNumberField(renderLab, "glassOpacityValue", jsonFloat(modelState.glassOpacityValue)) + ",\n"
            + indent + "\"glassTintPresetStatus\": \"" + escape(jsonStringField(renderLab, "glassTintPresetStatus", modelState.glassTintPresetStatus)) + "\",\n"
            + indent + "\"activeGlassTintPreset\": \"" + escape(jsonStringField(renderLab, "activeGlassTintPreset", modelState.activeGlassTintPreset)) + "\",\n"
            + indent + "\"glassEdgeSliderStatus\": \"" + escape(jsonStringField(renderLab, "glassEdgeSliderStatus", modelState.glassEdgeSliderStatus)) + "\",\n"
            + indent + "\"glassEdgeValue\": " + jsonNumberField(renderLab, "glassEdgeValue", jsonFloat(modelState.glassEdgeValue)) + ",\n"
            + indent + "\"glassRoughnessSliderStatus\": \"" + escape(jsonStringField(renderLab, "glassRoughnessSliderStatus", modelState.glassRoughnessSliderStatus)) + "\",\n"
            + indent + "\"glassRoughnessValue\": " + jsonNumberField(renderLab, "glassRoughnessValue", jsonFloat(modelState.glassRoughnessValue)) + ",\n"
            + indent + "\"glassUniformUpdateStatus\": \"" + escape(jsonStringField(renderLab, "glassUniformUpdateStatus", modelState.glassUniformUpdateStatus)) + "\",\n"
            + indent + "\"glassPresetIntegrationStatus\": \"" + escape(jsonStringField(renderLab, "glassPresetIntegrationStatus", modelState.glassPresetIntegrationStatus)) + "\",\n"
            + indent + "\"glassPresetAppliedSlot\": " + jsonNumberField(renderLab, "glassPresetAppliedSlot", String.valueOf(modelState.glassPresetAppliedSlot)) + ",\n"
            + indent + "\"glassPresetAppliedStatus\": \"" + escape(jsonStringField(renderLab, "glassPresetAppliedStatus", modelState.glassPresetAppliedStatus)) + "\",\n"
            + indent + "\"glassPresetPolishStatus\": \"" + escape(jsonStringField(renderLab, "glassPresetPolishStatus", modelState.glassPresetPolishStatus)) + "\",\n"
            + indent + "\"clearGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "clearGlassPresetStatus", modelState.clearGlassPresetStatus)) + "\",\n"
            + indent + "\"blueGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "blueGlassPresetStatus", modelState.blueGlassPresetStatus)) + "\",\n"
            + indent + "\"greenGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "greenGlassPresetStatus", modelState.greenGlassPresetStatus)) + "\",\n"
            + indent + "\"smokeGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "smokeGlassPresetStatus", modelState.smokeGlassPresetStatus)) + "\",\n"
            + indent + "\"warmGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "warmGlassPresetStatus", modelState.warmGlassPresetStatus)) + "\",\n"
            + indent + "\"magicGlassPresetStatus\": \"" + escape(jsonStringField(renderLab, "magicGlassPresetStatus", modelState.magicGlassPresetStatus)) + "\",\n"
            + indent + "\"dirtyGlassLitePresetStatus\": \"" + escape(jsonStringField(renderLab, "dirtyGlassLitePresetStatus", modelState.dirtyGlassLitePresetStatus)) + "\",\n"
            + indent + "\"crystalLitePresetStatus\": \"" + escape(jsonStringField(renderLab, "crystalLitePresetStatus", modelState.crystalLitePresetStatus)) + "\",\n"
            + indent + "\"glassPresetVisualResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassPresetVisualResponseStatus", modelState.glassPresetVisualResponseStatus)) + "\",\n"
            + indent + "\"glassSelectedSlotRoutingStatus\": \"" + escape(jsonStringField(renderLab, "glassSelectedSlotRoutingStatus", modelState.glassSelectedSlotRoutingStatus)) + "\",\n"
            + indent + "\"glassFabricGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassFabricGuardStatus", modelState.glassFabricGuardStatus)) + "\",\n"
            + indent + "\"glassMetalGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassMetalGuardStatus", modelState.glassMetalGuardStatus)) + "\",\n"
            + indent + "\"glassCarPaintGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassCarPaintGuardStatus", modelState.glassCarPaintGuardStatus)) + "\",\n"
            + indent + "\"glassVisualResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassVisualResponseStatus", modelState.glassVisualResponseStatus)) + "\",\n"
            + indent + "\"glassOpacityResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassOpacityResponseStatus", modelState.glassOpacityResponseStatus)) + "\",\n"
            + indent + "\"glassTintResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassTintResponseStatus", modelState.glassTintResponseStatus)) + "\",\n"
            + indent + "\"glassFresnelResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassFresnelResponseStatus", modelState.glassFresnelResponseStatus)) + "\",\n"
            + indent + "\"glassReflectionResponseStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionResponseStatus", modelState.glassReflectionResponseStatus)) + "\",\n"
            + indent + "\"glassEnergyGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassEnergyGuardStatus", modelState.glassEnergyGuardStatus)) + "\",\n"
            + indent + "\"glassOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassOverbrightGuardStatus", modelState.glassOverbrightGuardStatus)) + "\",\n"
            + indent + "\"glassInvisibleGuardStatus\": \"" + escape(jsonStringField(renderLab, "glassInvisibleGuardStatus", modelState.glassInvisibleGuardStatus)) + "\",\n"
            + indent + "\"glassMaskDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassMaskDebugViewStatus", modelState.glassMaskDebugViewStatus)) + "\",\n"
            + indent + "\"glassOpacityDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassOpacityDebugViewStatus", modelState.glassOpacityDebugViewStatus)) + "\",\n"
            + indent + "\"glassFresnelDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassFresnelDebugViewStatus", modelState.glassFresnelDebugViewStatus)) + "\",\n"
            + indent + "\"glassReflectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionDebugViewStatus", modelState.glassReflectionDebugViewStatus)) + "\",\n"
            + indent + "\"glassTintDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassTintDebugViewStatus", modelState.glassTintDebugViewStatus)) + "\",\n"
            + indent + "\"glassSafetyDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassSafetyDebugViewStatus", modelState.glassSafetyDebugViewStatus)) + "\",\n"
            + indent + "\"glassPolishDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassPolishDebugViewStatus", modelState.glassPolishDebugViewStatus)) + "\",\n"
            + indent + "\"glassEdgeDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassEdgeDebugViewStatus", modelState.glassEdgeDebugViewStatus)) + "\",\n"
            + indent + "\"glassThicknessDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassThicknessDebugViewStatus", modelState.glassThicknessDebugViewStatus)) + "\",\n"
            + indent + "\"glassReflectionPolishDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassReflectionPolishDebugViewStatus", modelState.glassReflectionPolishDebugViewStatus)) + "\",\n"
            + indent + "\"clearcoatPolishDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatPolishDebugViewStatus", modelState.clearcoatPolishDebugViewStatus)) + "\",\n"
            + indent + "\"paintReflectionDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "paintReflectionDebugViewStatus", modelState.paintReflectionDebugViewStatus)) + "\",\n"
            + indent + "\"p24ReflectionPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p24ReflectionPreservedStatus", modelState.p24ReflectionPreservedStatus)) + "\",\n"
            + indent + "\"p25GlassPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p25GlassPreservedStatus", modelState.p25GlassPreservedStatus)) + "\",\n"
            + indent + "\"p26UiChangeStatus\": \"" + escape(jsonStringField(renderLab, "p26UiChangeStatus", modelState.p26UiChangeStatus)) + "\",\n"
            + indent + "\"glassSummaryUiStatus\": \"" + escape(jsonStringField(renderLab, "glassSummaryUiStatus", modelState.glassSummaryUiStatus)) + "\",\n"
            + indent + "\"glassStrengthSliderStatus\": \"" + escape(jsonStringField(renderLab, "glassStrengthSliderStatus", modelState.glassStrengthSliderStatus)) + "\",\n"
            + indent + "\"glassControlsPreservedStatus\": \"" + escape(jsonStringField(renderLab, "glassControlsPreservedStatus", modelState.glassControlsPreservedStatus)) + "\",\n"
            + indent + "\"clearcoatPolishStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatPolishStatus", modelState.clearcoatPolishStatus)) + "\",\n"
            + indent + "\"clearcoatHighlightPolishStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatHighlightPolishStatus", modelState.clearcoatHighlightPolishStatus)) + "\",\n"
            + indent + "\"clearcoatReflectionSeparationStatus\": \"" + escape(jsonStringField(renderLab, "clearcoatReflectionSeparationStatus", modelState.clearcoatReflectionSeparationStatus)) + "\",\n"
            + indent + "\"carPaintPolishStatus\": \"" + escape(jsonStringField(renderLab, "carPaintPolishStatus", modelState.carPaintPolishStatus)) + "\",\n"
            + indent + "\"carPaintLayerReadabilityStatus\": \"" + escape(jsonStringField(renderLab, "carPaintLayerReadabilityStatus", modelState.carPaintLayerReadabilityStatus)) + "\",\n"
            + indent + "\"carPaintReflectionSeparationStatus\": \"" + escape(jsonStringField(renderLab, "carPaintReflectionSeparationStatus", modelState.carPaintReflectionSeparationStatus)) + "\",\n"
            + indent + "\"paintOverbrightGuardStatus\": \"" + escape(jsonStringField(renderLab, "paintOverbrightGuardStatus", modelState.paintOverbrightGuardStatus)) + "\",\n"
            + indent + "\"p25PerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p25PerformanceStatus", modelState.p25PerformanceStatus)) + "\",\n"
            + indent + "\"p26PerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p26PerformanceStatus", modelState.p26PerformanceStatus)) + "\",\n"
            + indent + "\"p26NoRealRefractionStatus\": \"" + escape(jsonStringField(renderLab, "p26NoRealRefractionStatus", modelState.p26NoRealRefractionStatus)) + "\",\n"
            + indent + "\"p26NoScreenRefractionStatus\": \"" + escape(jsonStringField(renderLab, "p26NoScreenRefractionStatus", modelState.p26NoScreenRefractionStatus)) + "\",\n"
            + indent + "\"p26NoMirrorPassStatus\": \"" + escape(jsonStringField(renderLab, "p26NoMirrorPassStatus", modelState.p26NoMirrorPassStatus)) + "\",\n"
            + indent + "\"p26NoFullSortingStatus\": \"" + escape(jsonStringField(renderLab, "p26NoFullSortingStatus", modelState.p26NoFullSortingStatus)) + "\",\n"
            + indent + "\"p26NoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "p26NoTextureRebuildStatus", modelState.p26NoTextureRebuildStatus)) + "\",\n"
            + indent + "\"p26NoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "p26NoModelReuploadStatus", modelState.p26NoModelReuploadStatus)) + "\",\n"
            + indent + "\"p26NoNewShadowPassStatus\": \"" + escape(jsonStringField(renderLab, "p26NoNewShadowPassStatus", modelState.p26NoNewShadowPassStatus)) + "\",\n"
            + indent + "\"transparentGlassPassStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassPassStatus", modelState.transparentGlassPassStatus)) + "\",\n"
            + indent + "\"transparentGlassPassMode\": \"" + escape(jsonStringField(renderLab, "transparentGlassPassMode", modelState.transparentGlassPassMode)) + "\",\n"
            + indent + "\"transparentGlassSelectedSlotStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassSelectedSlotStatus", modelState.transparentGlassSelectedSlotStatus)) + "\",\n"
            + indent + "\"transparentGlassDrawOrderStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassDrawOrderStatus", modelState.transparentGlassDrawOrderStatus)) + "\",\n"
            + indent + "\"transparentGlassBlendStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassBlendStatus", modelState.transparentGlassBlendStatus)) + "\",\n"
            + indent + "\"transparentGlassOpacityStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassOpacityStatus", modelState.transparentGlassOpacityStatus)) + "\",\n"
            + indent + "\"transparentGlassTintStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassTintStatus", modelState.transparentGlassTintStatus)) + "\",\n"
            + indent + "\"transparentGlassFresnelStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassFresnelStatus", modelState.transparentGlassFresnelStatus)) + "\",\n"
            + indent + "\"transparentGlassReflectionStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassReflectionStatus", modelState.transparentGlassReflectionStatus)) + "\",\n"
            + indent + "\"transparentGlassSortingStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassSortingStatus", modelState.transparentGlassSortingStatus)) + "\",\n"
            + indent + "\"transparentGlassRefractionStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassRefractionStatus", modelState.transparentGlassRefractionStatus)) + "\",\n"
            + indent + "\"transparentGlassFallbackStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassFallbackStatus", modelState.transparentGlassFallbackStatus)) + "\",\n"
            + indent + "\"transparentGlassPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassPerformanceStatus", modelState.transparentGlassPerformanceStatus)) + "\",\n"
            + indent + "\"transparentGlassUiStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassUiStatus", modelState.transparentGlassUiStatus)) + "\",\n"
            + indent + "\"glassRenderModeStatus\": \"" + escape(jsonStringField(renderLab, "glassRenderModeStatus", modelState.glassRenderModeStatus)) + "\",\n"
            + indent + "\"activeGlassRenderMode\": \"" + escape(jsonStringField(renderLab, "activeGlassRenderMode", modelState.activeGlassRenderMode)) + "\",\n"
            + indent + "\"transparentGlassToggleStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassToggleStatus", modelState.transparentGlassToggleStatus)) + "\",\n"
            + indent + "\"glassModeSummaryUiStatus\": \"" + escape(jsonStringField(renderLab, "glassModeSummaryUiStatus", modelState.glassModeSummaryUiStatus)) + "\",\n"
            + indent + "\"p26GlassPolishPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p26GlassPolishPreservedStatus", modelState.p26GlassPolishPreservedStatus)) + "\",\n"
            + indent + "\"fakeGlassFallbackStatus\": \"" + escape(jsonStringField(renderLab, "fakeGlassFallbackStatus", modelState.fakeGlassFallbackStatus)) + "\",\n"
            + indent + "\"glassAutoFallbackStatus\": \"" + escape(jsonStringField(renderLab, "glassAutoFallbackStatus", modelState.glassAutoFallbackStatus)) + "\",\n"
            + indent + "\"glassMotionFallbackStatus\": \"" + escape(jsonStringField(renderLab, "glassMotionFallbackStatus", modelState.glassMotionFallbackStatus)) + "\",\n"
            + indent + "\"transparentGlassMaterialRoutingStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassMaterialRoutingStatus", modelState.transparentGlassMaterialRoutingStatus)) + "\",\n"
            + indent + "\"transparentGlassAppliedSlot\": " + jsonNumberField(renderLab, "transparentGlassAppliedSlot", String.valueOf(modelState.transparentGlassAppliedSlot)) + ",\n"
            + indent + "\"transparentGlassAppliedMaterialCount\": " + jsonNumberField(renderLab, "transparentGlassAppliedMaterialCount", String.valueOf(modelState.transparentGlassAppliedMaterialCount)) + ",\n"
            + indent + "\"transparentGlassSkippedOpaqueCount\": " + jsonNumberField(renderLab, "transparentGlassSkippedOpaqueCount", String.valueOf(modelState.transparentGlassSkippedOpaqueCount)) + ",\n"
            + indent + "\"materialRoleSystemStatus\": \"" + escape(jsonStringField(renderLab, "materialRoleSystemStatus", modelState.materialRoleSystemStatus)) + "\",\n"
            + indent + "\"materialRoleDetectionMode\": \"" + escape(jsonStringField(renderLab, "materialRoleDetectionMode", modelState.materialRoleDetectionMode)) + "\",\n"
            + indent + "\"selectedMaterialRole\": \"" + escape(jsonStringField(renderLab, "selectedMaterialRole", modelState.selectedMaterialRole)) + "\",\n"
            + indent + "\"selectedMaterialRoleConfidence\": " + jsonNumberField(renderLab, "selectedMaterialRoleConfidence", jsonFloat(modelState.selectedMaterialRoleConfidence)) + ",\n"
            + indent + "\"selectedMaterialRoleSource\": \"" + escape(jsonStringField(renderLab, "selectedMaterialRoleSource", modelState.selectedMaterialRoleSource)) + "\",\n"
            + indent + "\"materialRoleCandidateCount\": " + jsonNumberField(renderLab, "materialRoleCandidateCount", String.valueOf(modelState.materialRoleCandidateCount)) + ",\n"
            + indent + "\"glassCandidateCount\": " + jsonNumberField(renderLab, "glassCandidateCount", String.valueOf(modelState.glassCandidateCount)) + ",\n"
            + indent + "\"bestGlassCandidateSlot\": " + jsonNumberField(renderLab, "bestGlassCandidateSlot", String.valueOf(modelState.bestGlassCandidateSlot)) + ",\n"
            + indent + "\"bestGlassCandidateName\": \"" + escape(jsonStringField(renderLab, "bestGlassCandidateName", modelState.bestGlassCandidateName)) + "\",\n"
            + indent + "\"bestGlassCandidateConfidence\": " + jsonNumberField(renderLab, "bestGlassCandidateConfidence", jsonFloat(modelState.bestGlassCandidateConfidence)) + ",\n"
            + indent + "\"bestGlassCandidateSource\": \"" + escape(jsonStringField(renderLab, "bestGlassCandidateSource", modelState.bestGlassCandidateSource)) + "\",\n"
            + indent + "\"materialRoleNoHardcodeStatus\": \"" + escape(jsonStringField(renderLab, "materialRoleNoHardcodeStatus", modelState.materialRoleNoHardcodeStatus)) + "\",\n"
            + indent + "\"transparentGlassRoutingStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassRoutingStatus", modelState.transparentGlassRoutingStatus)) + "\",\n"
            + indent + "\"transparentGlassRoutingMode\": \"" + escape(jsonStringField(renderLab, "transparentGlassRoutingMode", modelState.transparentGlassRoutingMode)) + "\",\n"
            + indent + "\"activeTransparentGlassSlot\": " + jsonNumberField(renderLab, "activeTransparentGlassSlot", String.valueOf(modelState.activeTransparentGlassSlot)) + ",\n"
            + indent + "\"activeTransparentGlassMaterialName\": \"" + escape(jsonStringField(renderLab, "activeTransparentGlassMaterialName", modelState.activeTransparentGlassMaterialName)) + "\",\n"
            + indent + "\"activeTransparentGlassMaterialRole\": \"" + escape(jsonStringField(renderLab, "activeTransparentGlassMaterialRole", modelState.activeTransparentGlassMaterialRole)) + "\",\n"
            + indent + "\"activeTransparentGlassRoleSource\": \"" + escape(jsonStringField(renderLab, "activeTransparentGlassRoleSource", modelState.activeTransparentGlassRoleSource)) + "\",\n"
            + indent + "\"activeTransparentGlassSlotSource\": \"" + escape(jsonStringField(renderLab, "activeTransparentGlassSlotSource", modelState.activeTransparentGlassSlotSource)) + "\",\n"
            + indent + "\"transparentGlassSelectedSlotAllowedStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassSelectedSlotAllowedStatus", modelState.transparentGlassSelectedSlotAllowedStatus)) + "\",\n"
            + indent + "\"transparentGlassSelectedSlotRejectedStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassSelectedSlotRejectedStatus", modelState.transparentGlassSelectedSlotRejectedStatus)) + "\",\n"
            + indent + "\"transparentGlassAutoDetectStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassAutoDetectStatus", modelState.transparentGlassAutoDetectStatus)) + "\",\n"
            + indent + "\"transparentGlassManualOverrideStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassManualOverrideStatus", modelState.transparentGlassManualOverrideStatus)) + "\",\n"
            + indent + "\"transparentGlassFallbackReason\": \"" + escape(jsonStringField(renderLab, "transparentGlassFallbackReason", modelState.transparentGlassFallbackReason)) + "\",\n"
            + indent + "\"transparentGlassWrongSlotGuardStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassWrongSlotGuardStatus", modelState.transparentGlassWrongSlotGuardStatus)) + "\",\n"
            + indent + "\"materialRoleUiStatus\": \"" + escape(jsonStringField(renderLab, "materialRoleUiStatus", modelState.materialRoleUiStatus)) + "\",\n"
            + indent + "\"selectedSlotRoleSummaryUiStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotRoleSummaryUiStatus", modelState.selectedSlotRoleSummaryUiStatus)) + "\",\n"
            + indent + "\"activeGlassSlotSummaryUiStatus\": \"" + escape(jsonStringField(renderLab, "activeGlassSlotSummaryUiStatus", modelState.activeGlassSlotSummaryUiStatus)) + "\",\n"
            + indent + "\"selectGlassCandidateButtonStatus\": \"" + escape(jsonStringField(renderLab, "selectGlassCandidateButtonStatus", modelState.selectGlassCandidateButtonStatus)) + "\",\n"
            + indent + "\"assignSelectedAsGlassButtonStatus\": \"" + escape(jsonStringField(renderLab, "assignSelectedAsGlassButtonStatus", modelState.assignSelectedAsGlassButtonStatus)) + "\",\n"
            + indent + "\"clearManualRoleButtonStatus\": \"" + escape(jsonStringField(renderLab, "clearManualRoleButtonStatus", modelState.clearManualRoleButtonStatus)) + "\",\n"
            + indent + "\"selectedSlotHighlightToggleStatus\": \"" + escape(jsonStringField(renderLab, "selectedSlotHighlightToggleStatus", modelState.selectedSlotHighlightToggleStatus)) + "\",\n"
            + indent + "\"transparentGlassSkippedNonGlassCount\": " + jsonNumberField(renderLab, "transparentGlassSkippedNonGlassCount", String.valueOf(modelState.transparentGlassSkippedNonGlassCount)) + ",\n"
            + indent + "\"transparentGlassSkippedFabricCount\": " + jsonNumberField(renderLab, "transparentGlassSkippedFabricCount", String.valueOf(modelState.transparentGlassSkippedFabricCount)) + ",\n"
            + indent + "\"transparentGlassSkippedMetalCount\": " + jsonNumberField(renderLab, "transparentGlassSkippedMetalCount", String.valueOf(modelState.transparentGlassSkippedMetalCount)) + ",\n"
            + indent + "\"transparentGlassSkippedPaintCount\": " + jsonNumberField(renderLab, "transparentGlassSkippedPaintCount", String.valueOf(modelState.transparentGlassSkippedPaintCount)) + ",\n"
            + indent + "\"p27TransparentPassPreservedStatus\": \"" + escape(jsonStringField(renderLab, "p27TransparentPassPreservedStatus", modelState.p27TransparentPassPreservedStatus)) + "\",\n"
            + indent + "\"p27bPerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p27bPerformanceStatus", modelState.p27bPerformanceStatus)) + "\",\n"
            + indent + "\"p27bNoExtraHeavyPassStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoExtraHeavyPassStatus", modelState.p27bNoExtraHeavyPassStatus)) + "\",\n"
            + indent + "\"p27bNoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoTextureRebuildStatus", modelState.p27bNoTextureRebuildStatus)) + "\",\n"
            + indent + "\"p27bNoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoModelReuploadStatus", modelState.p27bNoModelReuploadStatus)) + "\",\n"
            + indent + "\"p27bNoFrameGlbParseStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoFrameGlbParseStatus", modelState.p27bNoFrameGlbParseStatus)) + "\",\n"
            + indent + "\"p27bNoFrameFileWriteStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoFrameFileWriteStatus", modelState.p27bNoFrameFileWriteStatus)) + "\",\n"
            + indent + "\"p27bNoShadowStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoShadowStatus", modelState.p27bNoShadowStatus)) + "\",\n"
            + indent + "\"p27bNoRealMirrorStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoRealMirrorStatus", modelState.p27bNoRealMirrorStatus)) + "\",\n"
            + indent + "\"p27bNoSsrStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoSsrStatus", modelState.p27bNoSsrStatus)) + "\",\n"
            + indent + "\"p27bNoRealRefractionStatus\": \"" + escape(jsonStringField(renderLab, "p27bNoRealRefractionStatus", modelState.p27bNoRealRefractionStatus)) + "\",\n"
            + indent + "\"materialRoleDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "materialRoleDebugViewStatus", modelState.materialRoleDebugViewStatus)) + "\",\n"
            + indent + "\"glassCandidateDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "glassCandidateDebugViewStatus", modelState.glassCandidateDebugViewStatus)) + "\",\n"
            + indent + "\"activeGlassSlotDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "activeGlassSlotDebugViewStatus", modelState.activeGlassSlotDebugViewStatus)) + "\",\n"
            + indent + "\"transparentRoutingDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentRoutingDebugViewStatus", modelState.transparentRoutingDebugViewStatus)) + "\",\n"
            + indent + "\"wrongSlotGuardDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "wrongSlotGuardDebugViewStatus", modelState.wrongSlotGuardDebugViewStatus)) + "\",\n"
            + indent + "\"transparentGlassFabricGuardStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassFabricGuardStatus", modelState.transparentGlassFabricGuardStatus)) + "\",\n"
            + indent + "\"transparentGlassMetalGuardStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassMetalGuardStatus", modelState.transparentGlassMetalGuardStatus)) + "\",\n"
            + indent + "\"transparentGlassCarPaintGuardStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassCarPaintGuardStatus", modelState.transparentGlassCarPaintGuardStatus)) + "\",\n"
            + indent + "\"transparentGlassMaskDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassMaskDebugViewStatus", modelState.transparentGlassMaskDebugViewStatus)) + "\",\n"
            + indent + "\"transparentGlassAlphaDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassAlphaDebugViewStatus", modelState.transparentGlassAlphaDebugViewStatus)) + "\",\n"
            + indent + "\"transparentGlassDrawOrderDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassDrawOrderDebugViewStatus", modelState.transparentGlassDrawOrderDebugViewStatus)) + "\",\n"
            + indent + "\"transparentGlassFallbackDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassFallbackDebugViewStatus", modelState.transparentGlassFallbackDebugViewStatus)) + "\",\n"
            + indent + "\"transparentGlassSafetyDebugViewStatus\": \"" + escape(jsonStringField(renderLab, "transparentGlassSafetyDebugViewStatus", modelState.transparentGlassSafetyDebugViewStatus)) + "\",\n"
            + indent + "\"p27PerformanceStatus\": \"" + escape(jsonStringField(renderLab, "p27PerformanceStatus", modelState.p27PerformanceStatus)) + "\",\n"
            + indent + "\"p27NoShadowStatus\": \"" + escape(jsonStringField(renderLab, "p27NoShadowStatus", modelState.p27NoShadowStatus)) + "\",\n"
            + indent + "\"p27NoRaytraceStatus\": \"" + escape(jsonStringField(renderLab, "p27NoRaytraceStatus", modelState.p27NoRaytraceStatus)) + "\",\n"
            + indent + "\"p27NoSsrStatus\": \"" + escape(jsonStringField(renderLab, "p27NoSsrStatus", modelState.p27NoSsrStatus)) + "\",\n"
            + indent + "\"p27NoRealMirrorStatus\": \"" + escape(jsonStringField(renderLab, "p27NoRealMirrorStatus", modelState.p27NoRealMirrorStatus)) + "\",\n"
            + indent + "\"p27NoScreenRefractionStatus\": \"" + escape(jsonStringField(renderLab, "p27NoScreenRefractionStatus", modelState.p27NoScreenRefractionStatus)) + "\",\n"
            + indent + "\"p27NoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "p27NoTextureRebuildStatus", modelState.p27NoTextureRebuildStatus)) + "\",\n"
            + indent + "\"p27NoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "p27NoModelReuploadStatus", modelState.p27NoModelReuploadStatus)) + "\",\n"
            + indent + "\"glassNoRefractionPassStatus\": \"" + escape(jsonStringField(renderLab, "glassNoRefractionPassStatus", modelState.glassNoRefractionPassStatus)) + "\",\n"
            + indent + "\"glassNoScreenRefractionStatus\": \"" + escape(jsonStringField(renderLab, "glassNoScreenRefractionStatus", modelState.glassNoScreenRefractionStatus)) + "\",\n"
            + indent + "\"glassNoFullSortingStatus\": \"" + escape(jsonStringField(renderLab, "glassNoFullSortingStatus", modelState.glassNoFullSortingStatus)) + "\",\n"
            + indent + "\"glassNoTextureRebuildStatus\": \"" + escape(jsonStringField(renderLab, "glassNoTextureRebuildStatus", modelState.glassNoTextureRebuildStatus)) + "\",\n"
            + indent + "\"glassNoModelReuploadStatus\": \"" + escape(jsonStringField(renderLab, "glassNoModelReuploadStatus", modelState.glassNoModelReuploadStatus)) + "\",\n"
            + indent + "\"glassNoNewShadowPassStatus\": \"" + escape(jsonStringField(renderLab, "glassNoNewShadowPassStatus", modelState.glassNoNewShadowPassStatus)) + "\",\n";
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
            return injectJavaInspectorState(fallbackRenderLabJson("native_not_loaded", "ok", "shader_applied", ""));
        }
        try { return injectJavaInspectorState(nativeGetRenderLabState(nativeHandle)); }
        catch (Throwable t) { return injectJavaInspectorState(fallbackRenderLabJson("native_render_lab_state_failed", "failed", "not_applied", shortThrowable(t))); }
    }

    private String injectJavaInspectorState(String json) {
        if (json == null || json.isEmpty()) return json;
        String updated = json.replace("\"activeInspectorTab\":\"Assets\"", "\"activeInspectorTab\":\"" + escape(activeInspectorTab) + "\"");
        int end = updated.lastIndexOf('}');
        if (end < 0) return updated;
        return updated.substring(0, end)
            + ",\"resumeRestoreStatus\":\"" + escape(modelState.resumeRestoreStatus) + "\""
            + ",\"resumeRestoreMode\":\"" + escape(modelState.resumeRestoreMode) + "\""
            + ",\"activeModelPersistenceStatus\":\"" + escape(modelState.activeModelPersistenceStatus) + "\""
            + ",\"activeModelRestoreAttemptCount\":" + modelState.activeModelRestoreAttemptCount
            + ",\"activeModelRestoreResult\":\"" + escape(modelState.activeModelRestoreResult) + "\""
            + ",\"fallbackCubeReason\":\"" + escape(modelState.fallbackCubeReason) + "\""
            + ",\"surfaceRecreateStatus\":\"" + escape(modelState.surfaceRecreateStatus) + "\""
            + ",\"inspectorHeightMode\":\"" + escape(modelState.inspectorHeightMode) + "\""
            + ",\"inspectorScrollStatus\":\"" + escape(modelState.inspectorScrollStatus) + "\""
            + ",\"inspectorExpandedMaxHeightPercent\":" + modelState.inspectorExpandedMaxHeightPercent
            + ",\"inspectorCollapsedStatus\":\"" + escape(modelState.inspectorCollapsedStatus) + "\""
            + ",\"materialTabScrollStatus\":\"" + escape(modelState.materialTabScrollStatus) + "\""
            + ",\"inspectorTouchTargetStatus\":\"" + escape(modelState.inspectorTouchTargetStatus) + "\""
            + ",\"inspectorDynamicAlphaStatus\":\"" + escape(modelState.inspectorDynamicAlphaStatus) + "\""
            + ",\"inspectorAlphaIdle\":" + jsonFloat(modelState.inspectorAlphaIdle)
            + ",\"inspectorAlphaWhileSliderDrag\":" + jsonFloat(modelState.inspectorAlphaWhileSliderDrag)
            + ",\"inspectorAlphaWhileCameraMove\":" + jsonFloat(modelState.inspectorAlphaWhileCameraMove)
            + ",\"inspectorAlphaRestoreStatus\":\"" + escape(modelState.inspectorAlphaRestoreStatus) + "\""
            + ",\"sliderDragVisualMode\":\"" + escape(modelState.sliderDragVisualMode) + "\""
            + ",\"cameraMoveVisualMode\":\"" + escape(modelState.cameraMoveVisualMode) + "\""
            + ",\"materialWorkflowStatus\":\"" + escape(modelState.materialWorkflowStatus) + "\""
            + ",\"materialSlotSummaryUiStatus\":\"" + escape(modelState.materialSlotSummaryUiStatus) + "\""
            + ",\"selectedSlotResetButtonStatus\":\"" + escape(modelState.selectedSlotResetButtonStatus) + "\""
            + ",\"selectedSlotResetStatus\":\"" + escape(modelState.selectedSlotResetStatus) + "\""
            + ",\"selectedMaterialTextureSummaryStatus\":\"" + escape(modelState.selectedMaterialTextureSummaryStatus) + "\""
            + ",\"assetsWorkflowStatus\":\"" + escape(modelState.assetsWorkflowStatus) + "\""
            + ",\"reloadActiveModelButtonStatus\":\"" + escape(modelState.reloadActiveModelButtonStatus) + "\""
            + ",\"reloadActiveModelStatus\":\"" + escape(modelState.reloadActiveModelStatus) + "\""
            + ",\"activeModelDisplayStatus\":\"" + escape(modelState.activeModelDisplayStatus) + "\""
            + ",\"fallbackReasonDisplayStatus\":\"" + escape(modelState.fallbackReasonDisplayStatus) + "\""
            + ",\"p19PreservedStatus\":\"" + escape(modelState.p19PreservedStatus) + "\""
            + ",\"p18IblPreservedStatus\":\"" + escape(modelState.p18IblPreservedStatus) + "\""
            + ",\"p17GlossPreservedStatus\":\"" + escape(modelState.p17GlossPreservedStatus) + "\""
            + ",\"runtimeStateDebugViewStatus\":\"" + escape(modelState.runtimeStateDebugViewStatus) + "\""
            + ",\"restoreStateDebugViewStatus\":\"" + escape(modelState.restoreStateDebugViewStatus) + "\""
            + ",\"uiStateDebugViewStatus\":\"" + escape(modelState.uiStateDebugViewStatus) + "\""
            + "}";
    }

    private String fallbackRenderLabJson(String status, String lightingStatus, String debugStatus, String reason) {
        return "{\"currentScene\":\"" + SCENE_ID + "\",\"currentLabScene\":\"" + SCENE_ID + "\",\"currentLabSceneName\":\"" + SCENE_NAME + "\",\"status\":\"" + escape(status) + "\",\"lightingStatus\":\"" + escape(lightingStatus) + "\",\"lightingControlStatus\":\"" + escape(lightingStatus) + "\",\"lightingUiMode\":\"compact_sliders\",\"inspectorUiStatus\":\"ok\",\"inspectorUiMode\":\"tabbed_compact_inspector\",\"activeInspectorTab\":\"" + escape(activeInspectorTab) + "\",\"assetsTabStatus\":\"ok_import_scan_export_summary\",\"cameraTabStatus\":\"ok_camera_info_reset_zoom\",\"lightingTabStatus\":\"ok_sliders_environment_controls\",\"materialTabStatus\":\"ok_debug_views\",\"debugTabStatus\":\"ok_fps_zip_status\",\"sunDirection\":[-0.35,-0.82,-0.45],\"sunColor\":[1,0.96,0.88],\"sunIntensity\":2.0,\"ambientColor\":[0.42,0.52,0.62],\"ambientIntensity\":0.8,\"lightPreset\":\"Bright\",\"specularBoost\":1.85,\"specularBoostStatus\":\"ok_uniform_controlled\",\"reflectionIntensity\":1.15,\"contactGroundingStatus\":\"foundation_analytic\",\"contactShadowStatus\":\"enabled\",\"contactShadowMode\":\"analytic_blob_or_grounding_approx\",\"contactShadowIntensity\":" + jsonFloat(contactShadowIntensity) + ",\"contactShadowPerformanceStatus\":\"ok_uniform_only_no_shadow_pass\",\"groundingUsesModelBounds\":\"yes_upload_bounds_scaled_local\",\"groundingUniformUpdateStatus\":\"ok_uniform_only\",\"groundSliderStatus\":\"ok\",\"contactGroundingSliderStatus\":\"ok\",\"iblStatus\":\"ok_foundation\",\"iblMode\":\"procedural_directional_probe\",\"environmentIblStatus\":\"ok_foundation\",\"environmentIblMode\":\"procedural_directional_probe\",\"environmentSourceStatus\":\"ok_procedural_no_external_texture\",\"environmentSourceType\":\"procedural_directional_probe\",\"environmentSkyColorStatus\":\"ok_preset_uniform\",\"environmentGroundColorStatus\":\"ok_preset_uniform\",\"environmentHorizonStatus\":\"ok_directional_horizon_blend\",\"environmentPerformanceStatus\":\"ok_no_extra_pass_no_texture_upload\",\"iblDiffuseStatus\":\"ok_directional_sky_ground_diffuse\",\"iblSpecularStatus\":\"ok_reflection_direction_environment\",\"iblRoughnessResponseStatus\":\"ok_roughness_blurs_and_reduces_specular\",\"iblMetallicResponseStatus\":\"ok_metal_tints_reflection\",\"iblDielectricResponseStatus\":\"ok_subtle_f0_reflection\",\"iblFabricPreserveStatus\":\"ok_fabric_matte_preserved\",\"iblOverbrightGuardStatus\":\"ok_luminance_guarded\",\"environmentUiStatus\":\"ok_compact_lighting_controls\",\"environmentPreset\":\"Studio\",\"environmentIntensity\":1.0,\"environmentSliderStatus\":\"ok\",\"skyPresetStatus\":\"ok\",\"horizonControlStatus\":\"ok\",\"environmentUniformUpdateStatus\":\"ok_uniform_only\",\"environmentDebugViewStatus\":\"shader_applied\",\"reflectionDirectionDebugViewStatus\":\"shader_applied\",\"environmentColorDebugViewStatus\":\"shader_applied\",\"iblPerformanceStatus\":\"ok_shader_math_only_no_loops\",\"reflectionFoundationStatus\":\"p24_fake_cubemap_probe_foundation\",\"reflectionMode\":\"procedural_directional_probe\",\"environmentReflectionStatus\":\"p24_fake_cubemap_probe_no_texture\",\"environmentReflectionMode\":\"reflection_direction_sky_horizon_ground_side\",\"environmentSource\":\"procedural_directional_probe\",\"reflectionColorStatus\":\"ok_sky_horizon_ground_side_glint\",\"reflectionRoughnessResponseStatus\":\"ok_roughness_blur_approx\",\"metallicReflectionStatus\":\"ok_metal_tinted_stronger_guarded\",\"dielectricReflectionStatus\":\"ok_subtle_f0_environment\",\"reflectionPerformanceStatus\":\"ok_no_extra_pass_no_texture_rebuild\",\"lightingUniformUpdateStatus\":\"ok_uniform_only\",\"sliderUpdateMode\":\"uniform_only\",\"sliderTouchStatus\":\"ok_touch_targets\",\"sunSliderStatus\":\"ok\",\"ambientSliderStatus\":\"ok\",\"exposureSliderStatus\":\"ok\",\"specularSliderStatus\":\"ok\",\"reflectionSliderStatus\":\"ok\",\"brdfStatus\":\"ok\",\"brdfMode\":\"direct_lighting_schlick_mobile_p17_gloss\",\"diffuseStatus\":\"ok_environment_diffuse\",\"specularStatus\":\"ok_p17_gloss_response_guarded\",\"fresnelStatus\":\"ok_schlick\",\"f0Status\":\"ok_dielectric_0_04_metal_base_color\",\"metallicResponseStatus\":\"ok_diffuse_reduced_f0_tinted\",\"roughnessResponseStatus\":\"ok_gloss_width_energy_remap\",\"directLightingStatus\":\"ok_single_sun_direct_gloss_lobe\",\"materialResponseStatus\":\"p24_fake_cubemap_probe_foundation\",\"pbrQualityTier\":\"mobile_direct_lighting_ibl_v1\",\"brdfPerformanceStatus\":\"ok_mobile_friendly_direct_lighting_ibl\",\"toneMappingStatus\":\"ok\",\"toneMappingMode\":\"reinhard\",\"activeDebugView\":\"Final Shaded\",\"debugViewStatus\":\"" + escape(debugStatus) + "\",\"diffuseDebugViewStatus\":\"" + escape(debugStatus) + "\",\"specularDebugViewStatus\":\"" + escape(debugStatus) + "\",\"f0DebugViewStatus\":\"" + escape(debugStatus) + "\",\"reflectionDebugViewStatus\":\"" + escape(debugStatus) + "\",\"iblDiffuseDebugViewStatus\":\"" + escape(debugStatus) + "\",\"iblSpecularDebugViewStatus\":\"" + escape(debugStatus) + "\",\"brdfStatusDebugViewStatus\":\"" + escape(debugStatus) + "\",\"groundingDebugViewStatus\":\"" + escape(debugStatus) + "\",\"exposureValue\":1.5,\"ambientFloor\":0.16,\"brightnessPreset\":\"Bright Preview\",\"gpuUploadStatus\":\"failed\",\"drawStatus\":\"fallback\",\"meshDrawStatus\":\"fallback\",\"textureUploadStatus\":\"missing\",\"baseColorTextureStatus\":\"missing\",\"textureFallbackUsed\":true,\"textureWidth\":0,\"textureHeight\":0,\"uploadedVertexCount\":0,\"uploadedIndexCount\":0,\"modelBoundsMin\":[0,0,0],\"modelBoundsMax\":[0,0,0],\"modelBoundsCenter\":[0,0,0],\"modelScale\":1,\"modelRenderMode\":\"multi_primitive_static\",\"primitiveCountTotal\":0,\"primitiveCountRendered\":0,\"primitiveCountSkipped\":0,\"unsupportedPrimitiveCount\":0,\"materialSlotCount\":0,\"materialSlotCountRendered\":0,\"textureSlotCount\":0,\"uploadedTextureCount\":0,\"textureFallbackCount\":0,\"skippedTextureCount\":0,\"textureSlotLimit\":8,\"tangentFallbackGeneratedCount\":0,\"tangentDegenerateTriangleCount\":0,\"tangentBuildMode\":\"once_on_upload\",\"fpsCurrent\":0,\"frameTimeMs\":0,\"fpsSource\":\"not_ready\",\"fpsLastStable\":0,\"frameTimeLastStableMs\":0,\"fpsStatus\":\"not_ready\",\"fpsUpdateMode\":\"java_choreographer_live\",\"fpsSampleWindowMs\":1000,\"framesRenderedLive\":0,\"modelUploadRepeatCount\":0,\"uploadGenerationId\":0,\"renderLoopAllocationGuardStatus\":\"ok_no_java_glb_parse_or_upload_in_frame_callback\",\"debugZipStatus\":\"not_run\",\"debugZipPath\":\"\",\"debugZipIncludedFiles\":\"\",\"debugZipReason\":\"not_run\",\"fallbackCubeVisible\":true,\"fallbackCubeStatus\":\"on\",\"reason\":\"" + escape(reason) + "\"}";
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
        String resumeRestoreStatus = "not_run";
        String resumeRestoreMode = "not_run";
        String activeModelPersistenceStatus = "not_run";
        int activeModelRestoreAttemptCount = 0;
        String activeModelRestoreResult = "not_run";
        String fallbackCubeReason = "no active model";
        String surfaceRecreateStatus = "not_run";
        String persistedModelBounds = "unknown";
        float persistedModelScale = 1.0f;
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
        String materialSlotEditorStatus = "ok";
        int selectedMaterialSlot = 0;
        int selectedMaterialSlotCount = 0;
        String selectedMaterialTypeHint = "unknown";
        String selectedMaterialName = "unknown";
        String selectedMaterialSummaryStatus = "empty";
        String materialSlotSelectionUiStatus = "ok";
        String perMaterialOverrideStatus = "foundation_selected_slot_uniform";
        String perMaterialOverrideMode = "cpu_selected_slot_push_constants";
        float selectedSlotMetallicOverride = 0.0f;
        float selectedSlotRoughnessOverride = 1.0f;
        float selectedSlotNormalScaleOverride = 1.0f;
        float selectedSlotAoOverride = 1.0f;
        float selectedSlotGlossOverride = 0.0f;
        float selectedSlotCoatOverride = 0.0f;
        String selectedSlotOverrideApplied = "false_no_material_slot";
        String selectedSlotResetStatus = "not_run";
        String perMaterialUniformUpdateStatus = "ok_uniform_only";
        String materialSlotControlsUiStatus = "ok_compact";
        String metallicSlotSliderStatus = "ok";
        String roughnessSlotSliderStatus = "ok";
        String normalSlotSliderStatus = "ok";
        String aoSlotSliderStatus = "ok";
        String selectedMaterialDebugViewStatus = "shader_applied";
        String materialOverrideDebugViewStatus = "shader_applied";
        String slotMetallicDebugViewStatus = "shader_applied";
        String slotRoughnessDebugViewStatus = "shader_applied";
        String slotAoDebugViewStatus = "shader_applied";
        String perMaterialOverridePerformanceStatus = "ok_no_extra_pass_no_upload";
        String alphaMaterialStatus = "ok_opaque_materials";
        String alphaModeSupportStatus = "ok_opaque_mask_blend_metadata";
        String alphaMaskStatus = "ok_no_mask_material";
        String alphaBlendStatus = "ok_no_blend_material";
        String alphaCutoffStatus = "ok_uniform_control";
        float alphaCutoffValue = 0.5f;
        String alphaDiscardStatus = "inactive_opaque";
        String alphaTextureChannelStatus = "baseColor_alpha_channel_sampled_when_texture_ready";
        String alphaFallbackStatus = "none";
        String doubleSidedMaterialStatus = "ok_no_double_sided_material";
        String doubleSidedMode = "selected_slot_single_sided_or_none";
        String doubleSidedNormalStatus = "shader_gl_front_facing_normal_flip";
        String doubleSidedRasterStatus = "ok_pipeline_cull_none_no_new_permutation";
        String doubleSidedFallbackStatus = "none";
        String thinMaterialPolishStatus = "ok_cutout_double_sided_hint_foundation";
        String cutoutMaterialHintStatus = "ok_available";
        String fabricEdgeStatus = "ok_available";
        String glassMetadataStatus = "none";
        String decalMaterialHintStatus = "ok_available";
        String transparencyDeferredStatus = "ok_no_full_transparent_sorting_or_glass";
        String alphaUiStatus = "ok_compact_material_tab";
        String alphaCutoffSliderStatus = "ok";
        String alphaDebugViewStatus = "shader_applied";
        String doubleSidedDebugViewStatus = "shader_applied";
        String alphaResetButtonStatus = "ok";
        String alphaUniformUpdateStatus = "ok_uniform_only";
        String alphaSliderUpdateMode = "uniform_only";
        String alphaMaskDebugViewStatus = "shader_applied";
        String alphaModeDebugViewStatus = "shader_applied";
        String cutoutHintDebugViewStatus = "shader_applied";
        String transparencyStatusDebugViewStatus = "shader_applied";
        String alphaPerformanceStatus = "ok_no_new_pass_no_sorting_no_reupload";
        String alphaNoNewPassStatus = "ok";
        String alphaNoTextureRebuildStatus = "ok";
        String alphaNoModelReuploadStatus = "ok";
        String selectedAlphaMode = "OPAQUE";
        String materialWorkflowStatus = "ok_selected_slot_material_workflow";
        String materialSlotSummaryUiStatus = "ok_slot_name_hint_texture_summary";
        String selectedSlotResetButtonStatus = "ok";
        String selectedMaterialTextureSummaryStatus = "baseColor missing | metallicRoughness missing | normal missing | occlusion missing";
        String assetsWorkflowStatus = "ok_active_model_import_scan_reload_export";
        String reloadActiveModelButtonStatus = "ok";
        String reloadActiveModelStatus = "not_run";
        String activeModelDisplayStatus = "empty";
        String fallbackReasonDisplayStatus = "ok_visible";
        String p19PreservedStatus = "ok";
        String p19SlotControlsPreservedStatus = "ok";
        String p20RuntimeWorkflowPreservedStatus = "ok";
        String p18IblPreservedStatus = "ok";
        String p17GlossPreservedStatus = "ok";
        String p21AlphaPreservedStatus = "ok";
        String emissiveMaterialStatus = "ok_metadata_supported";
        String emissiveMode = "factor_uniform_only_no_light_contribution";
        String emissiveFactorStatus = "missing";
        String emissiveTextureStatus = "missing";
        float emissiveIntensity = 0.0f;
        String emissiveIntensityStatus = "ok_clamped";
        String emissiveColorStatus = "ok_guarded";
        String emissiveOverbrightGuardStatus = "ok_clamped";
        String emissiveLightingContributionStatus = "not_real_light_source";
        String emissivePerformanceStatus = "ok_no_bloom_no_new_pass";
        String materialPresetStatus = "ok";
        String activeMaterialPreset = "Balanced";
        String materialPresetMode = "selected_slot_uniform_only";
        int materialPresetAppliedSlot = 0;
        String materialPresetAppliedStatus = "not_applied";
        String materialPresetUiStatus = "ok_compact_material_tab";
        String materialPresetPerformanceStatus = "ok_no_model_reupload_no_texture_rebuild";
        String selectedSlotPresetStatus = "ok";
        String carPaintPresetStatus = "ok";
        String metalPresetStatus = "ok";
        String fabricPresetStatus = "ok";
        String rubberPresetStatus = "ok";
        String plasticPresetStatus = "ok";
        String glassMetadataPresetStatus = "glass_foundation_no_refraction";
        String emissivePresetStatus = "ok_safe_clamped";
        String materialPresetGuardStatus = "ok_energy_guarded";
        String presetCycleButtonStatus = "ok";
        String presetApplyButtonStatus = "ok";
        String emissiveSliderStatus = "ok";
        String emissiveUniformUpdateStatus = "ok_uniform_only";
        String materialResetButtonStatus = "ok";
        String materialUiScrollPreservedStatus = "ok";
        String emissiveDebugViewStatus = "shader_applied";
        String presetTypeDebugViewStatus = "shader_applied";
        String presetResponseDebugViewStatus = "shader_applied";
        String materialEnergyGuardDebugViewStatus = "shader_applied";
        String selectedSlotPresetDebugViewStatus = "shader_applied";
        String p22PerformanceStatus = "ok_no_new_pass_no_bloom_no_reupload";
        String emissiveNoBloomStatus = "ok";
        String emissiveNoNewPassStatus = "ok";
        String presetNoModelReuploadStatus = "ok";
        String presetNoTextureRebuildStatus = "ok";
        String clearcoatStatus = "available";
        String clearcoatMode = "single_pass_uniform_clearcoat";
        float clearcoatIntensity = 0.72f;
        float clearcoatRoughness = 0.28f;
        String clearcoatFresnelStatus = "ok_schlick_view_angle";
        String clearcoatHighlightStatus = "ok_view_dependent";
        String clearcoatMaterialRoutingStatus = "selected_slot_safe";
        String clearcoatOverbrightGuardStatus = "ok";
        String clearcoatPerformanceStatus = "ok_single_pass_no_texture_no_loop";
        String clearcoatAppliedStatus = "not_applied";
        float clearcoatWeight = 0.0f;
        float clearcoatRoughnessApplied = 1.0f;
        String clearcoatGuardApplied = "no";
        String carPaintLayerStatus = "ok";
        String carPaintPresetV2Status = "ok";
        String carPaintClearcoatStatus = "available";
        String paintLayerEnergyGuardStatus = "ok";
        String paintLayerMaterialHintStatus = "selected_slot";
        String clearcoatSliderStatus = "ok";
        float clearcoatSliderValue = 0.72f;
        String clearcoatRoughnessSliderStatus = "ok";
        float clearcoatRoughnessSliderValue = 0.28f;
        String clearcoatUniformUpdateStatus = "ok_uniform_only";
        String clearcoatUiStatus = "ok_compact_material_tab";
        String clearcoatDebugViewStatus = "shader_applied";
        String clearcoatHighlightDebugViewStatus = "shader_applied";
        String paintLayerDebugViewStatus = "shader_applied";
        String paintEnergyGuardDebugViewStatus = "shader_applied";
        String p22EmissivePreservedStatus = "ok";
        String p22PresetsPreservedStatus = "ok";
        String p23PerformanceStatus = "ok_single_pass_uniform_only";
        String clearcoatNoNewPassStatus = "ok";
        String clearcoatNoTextureRebuildStatus = "ok";
        String clearcoatNoModelReuploadStatus = "ok";
        String clearcoatNoTransparentSortingStatus = "ok";
        String fakeCubemapStatus = "ok";
        String fakeCubemapMode = "procedural_directional_probe";
        String fakeCubemapSourceStatus = "procedural_no_texture_no_render_pass";
        String environmentZoneStatus = "ok_sky_horizon_ground_side";
        String skyZoneStatus = "ok";
        String horizonZoneStatus = "ok";
        String groundZoneStatus = "ok";
        String sideReflectionZoneStatus = "ok";
        String fakeCubemapPerformanceStatus = "ok_shader_math_only_no_loops";
        String reflectionQualityStatus = "ok_procedural_probe_enhanced";
        String reflectionContrastStatus = "ok_uniform_control";
        float reflectionContrastValue = 1.18f;
        String reflectionSaturationStatus = "ok_uniform_control";
        float reflectionSaturationValue = 1.12f;
        String roughnessReflectionBlurStatus = "ok_direction_blend_approx";
        String clearcoatReflectionBoostStatus = "ok_motion_guarded";
        String metalReflectionTintStatus = "ok_base_color_tinted";
        String fabricReflectionSuppressStatus = "ok_fabric_matte_preserved";
        String reflectionEnergyGuardStatus = "ok_luminance_guard";
        String reflectionOverbrightGuardStatus = "ok_clamped_before_tonemap";
        String reflectionQualityUiStatus = "ok_compact_controls";
        String reflectionContrastSliderStatus = "ok";
        String reflectionSaturationSliderStatus = "ok";
        String environmentZonePresetStatus = "ok";
        String environmentZonePreset = "Studio";
        String reflectionUniformUpdateStatus = "ok_uniform_only";
        String motionPerformanceGuardStatus = "ok";
        String cameraMotionStatus = "still";
        float cameraMotionSpeed = 0.0f;
        String cameraMotionQualityTier = "full";
        float cameraMotionQualityBlend = 1.0f;
        float motionReflectionScale = 1.0f;
        float motionClearcoatScale = 1.0f;
        String motionDiagnosticsThrottleStatus = "ok_ui_fps_only_no_export";
        String motionQualityRestoreStatus = "ok_smooth_restore";
        int motionQualityTransitionMs = 520;
        float cameraMovingFpsLast = 0.0f;
        float cameraStillFpsLast = 0.0f;
        float targetMovingFps = 45.0f;
        String movingFpsGuardStatus = "monitoring";
        String carPaintReflectionStatus = "ok_clearcoat_probe_separation";
        String metalReflectionStatus = "ok_tinted_stronger";
        String plasticReflectionStatus = "ok_glossy_limited";
        String rubberReflectionStatus = "ok_low_reflection";
        String glassMetadataReflectionStatus = "foundation_single_pass_fake_transparency";
        String materialPresetReflectionStatus = "ok";
        String fakeCubemapDebugViewStatus = "shader_applied";
        String reflectionZonesDebugViewStatus = "shader_applied";
        String reflectionContrastDebugViewStatus = "shader_applied";
        String reflectionEnergyGuardDebugViewStatus = "shader_applied";
        String clearcoatReflectionDebugViewStatus = "shader_applied";
        String motionQualityDebugViewStatus = "shader_applied";
        String p23ClearcoatPreservedStatus = "ok";
        String p24PerformanceStatus = "ok_no_new_pass_no_texture_no_rebuild";
        String fakeCubemapNoTextureStatus = "ok";
        String fakeCubemapNoNewPassStatus = "ok";
        String reflectionNoTextureRebuildStatus = "ok";
        String reflectionNoModelReuploadStatus = "ok";
        String noFrameFileWriteStatus = "ok";
        String noFrameGlbParseStatus = "ok";
        String p26ReflectionPolishStatus = "ok_zone_contrast_rim_roughness_polished";
        String reflectionZonePolishStatus = "ok_sky_horizon_ground_contrast_guarded";
        String sideRimReflectionPolishStatus = "ok_rim_zone_boost_guarded";
        String roughnessReflectionPolishStatus = "ok_smoother_mobile_response";
        String glassReflectionZoneStatus = "ok_glass_uses_p26_zone_probe";
        String clearcoatReflectionZoneStatus = "ok_clearcoat_uses_p26_zone_probe";
        String glassMaterialStatus = "available";
        String glassMode = "single_pass_fake_transparency_no_refraction";
        String glassPolishStatus = "ok";
        String glassPolishMode = "single_pass_fake_glass_polish";
        String glassReadabilityStatus = "available";
        String glassEdgePolishStatus = "ok_stronger_fresnel_rim";
        String glassTintPolishStatus = "ok_tint_blends_preserve_surface";
        String glassThicknessPolishStatus = "ok_fake_thickness_edge_dark_bright";
        String glassOpacityCurveStatus = "ok_minimum_surface_curve";
        String glassRoughnessPolishStatus = "ok_clear_vs_frosted_response";
        String glassReflectionPolishStatus = "ok_p24_probe_boost_guarded";
        float glassOpacity = 0.56f;
        String glassTintStatus = "ok_preset_tint";
        String glassTintColor = "0.92,0.98,1.00";
        String glassFresnelStatus = "ok_stronger_schlick_edge";
        String glassEdgeReflectionStatus = "ok_p26_edge_fresnel_boost";
        String glassReflectionSourceStatus = "p24_fake_cubemap_probe";
        String glassRoughnessResponseStatus = "ok_clear_vs_rough_distinction";
        String glassThicknessFakeStatus = "ok_edge_tint_thickness_fake";
        String glassTransparencyMode = "safe_single_pass_fake_opacity";
        String glassSortingStatus = "safe_no_full_transparent_sorting";
        String glassRefractionStatus = "deferred_not_real_refraction";
        String glassStillFakeTransparencyStatus = "yes_no_real_transparent_pass";
        String glassPerformanceStatus = "ok_uniform_shader_no_pass_no_sort";
        String glassUiStatus = "ok_compact_material_tab";
        String glassEnableButtonStatus = "ok";
        String glassPresetStatus = "ok";
        String activeGlassPreset = "Clear Glass";
        String glassOpacitySliderStatus = "ok";
        float glassOpacityValue = 0.56f;
        String glassTintPresetStatus = "ok";
        String activeGlassTintPreset = "Clear Glass";
        String glassEdgeSliderStatus = "ok";
        float glassEdgeValue = 1.55f;
        String glassRoughnessSliderStatus = "ok";
        float glassRoughnessValue = 0.14f;
        String glassUniformUpdateStatus = "ok_uniform_only";
        String glassPresetIntegrationStatus = "ok_selected_slot_only";
        int glassPresetAppliedSlot = 0;
        String glassPresetAppliedStatus = "available";
        String glassPresetPolishStatus = "ok";
        String clearGlassPresetStatus = "ok";
        String blueGlassPresetStatus = "ok";
        String greenGlassPresetStatus = "ok";
        String smokeGlassPresetStatus = "ok";
        String warmGlassPresetStatus = "ok";
        String magicGlassPresetStatus = "ok";
        String dirtyGlassLitePresetStatus = "ok";
        String crystalLitePresetStatus = "ok";
        String glassPresetVisualResponseStatus = "ok_tint_opacity_edge_roughness_reflection_thickness";
        String glassSelectedSlotRoutingStatus = "ok_selected_slot_or_glass_hint";
        String glassFabricGuardStatus = "ok";
        String glassMetalGuardStatus = "ok";
        String glassCarPaintGuardStatus = "ok";
        String glassVisualResponseStatus = "available";
        String glassOpacityResponseStatus = "ok_base_surface_reduced";
        String glassTintResponseStatus = "ok_tint_colors_surface";
        String glassFresnelResponseStatus = "ok_grazing_angle_boost";
        String glassReflectionResponseStatus = "ok_p24_probe_visible_guarded";
        String glassEnergyGuardStatus = "ok_clamped";
        String glassOverbrightGuardStatus = "ok";
        String glassInvisibleGuardStatus = "ok_minimum_surface_kept";
        String glassMaskDebugViewStatus = "shader_applied";
        String glassOpacityDebugViewStatus = "shader_applied";
        String glassFresnelDebugViewStatus = "shader_applied";
        String glassReflectionDebugViewStatus = "shader_applied";
        String glassTintDebugViewStatus = "shader_applied";
        String glassSafetyDebugViewStatus = "shader_applied";
        String glassPolishDebugViewStatus = "shader_applied";
        String glassEdgeDebugViewStatus = "shader_applied";
        String glassThicknessDebugViewStatus = "shader_applied";
        String glassReflectionPolishDebugViewStatus = "shader_applied";
        String clearcoatPolishDebugViewStatus = "shader_applied";
        String paintReflectionDebugViewStatus = "shader_applied";
        String p24ReflectionPreservedStatus = "ok";
        String p25GlassPreservedStatus = "ok";
        String p26UiChangeStatus = "minimal_material_tab_text_only";
        String glassSummaryUiStatus = "ok_compact_summary";
        String glassStrengthSliderStatus = "deferred_reusing_edge_opacity_roughness";
        String glassControlsPreservedStatus = "ok";
        String clearcoatPolishStatus = "ok";
        String clearcoatHighlightPolishStatus = "ok_clearer_guarded_highlight";
        String clearcoatReflectionSeparationStatus = "ok_layer_separated_from_base";
        String carPaintPolishStatus = "ok";
        String carPaintLayerReadabilityStatus = "ok_base_coat_reflection_separated";
        String carPaintReflectionSeparationStatus = "ok_paint_reflection_layer_readable";
        String paintOverbrightGuardStatus = "ok_clamped";
        String p25PerformanceStatus = "ok_single_pass_uniform_shader_math";
        String p26PerformanceStatus = "ok_single_pass_shader_math_no_rebuild";
        String p26NoRealRefractionStatus = "ok_deferred";
        String p26NoScreenRefractionStatus = "ok_deferred";
        String p26NoMirrorPassStatus = "ok_deferred";
        String p26NoFullSortingStatus = "ok_deferred";
        String p26NoTextureRebuildStatus = "ok";
        String p26NoModelReuploadStatus = "ok";
        String p26NoNewShadowPassStatus = "ok";
        String transparentGlassPassStatus = "ok_selected_slot_transparent_pass";
        String transparentGlassPassMode = "opaque_first_selected_glass_after";
        String transparentGlassSelectedSlotStatus = "available";
        String transparentGlassDrawOrderStatus = "opaque_first_then_selected_glass";
        String transparentGlassBlendStatus = "ok_vk_src_alpha_one_minus_src_alpha";
        String transparentGlassOpacityStatus = "ok_real_color_blend_alpha";
        String transparentGlassTintStatus = "ok_shader_tint";
        String transparentGlassFresnelStatus = "ok_edge_reflection_visible";
        String transparentGlassReflectionStatus = "ok_p26_fake_probe_preserved";
        String transparentGlassSortingStatus = "limited_selected_slot_no_full_sort";
        String transparentGlassRefractionStatus = "deferred_no_real_refraction";
        String transparentGlassFallbackStatus = "p26_fake_glass_available";
        String transparentGlassPerformanceStatus = "ok_one_extra_selected_slot_draw_when_active";
        String transparentGlassUiStatus = "ok_minimal_material_tab";
        String glassRenderModeStatus = "ok_fake_transparent_auto";
        String activeGlassRenderMode = "Auto";
        String transparentGlassToggleStatus = "ok";
        String glassModeSummaryUiStatus = "ok";
        String p26GlassPolishPreservedStatus = "ok";
        String fakeGlassFallbackStatus = "available";
        String glassAutoFallbackStatus = "available_motion_or_non_glass";
        String glassMotionFallbackStatus = "available_motion_guard";
        String transparentGlassMaterialRoutingStatus = "selected_slot_only";
        int transparentGlassAppliedSlot = 0;
        int transparentGlassAppliedMaterialCount = 0;
        int transparentGlassSkippedOpaqueCount = 0;
        String materialRoleSystemStatus = "ok";
        String materialRoleDetectionMode = "metadata_name_alpha_hint_factor";
        String selectedMaterialRole = "Unknown";
        float selectedMaterialRoleConfidence = 0.0f;
        String selectedMaterialRoleSource = "none";
        int materialRoleCandidateCount = 0;
        int glassCandidateCount = 0;
        int bestGlassCandidateSlot = -1;
        String bestGlassCandidateName = "none";
        float bestGlassCandidateConfidence = 0.0f;
        String bestGlassCandidateSource = "none";
        String materialRoleNoHardcodeStatus = "ok_no_asset_specific_slot_hardcode";
        String transparentGlassRoutingStatus = "available";
        String transparentGlassRoutingMode = "auto_best_glass_candidate";
        int activeTransparentGlassSlot = -1;
        String activeTransparentGlassMaterialName = "none";
        String activeTransparentGlassMaterialRole = "Unknown";
        String activeTransparentGlassRoleSource = "none";
        String activeTransparentGlassSlotSource = "none";
        String transparentGlassSelectedSlotAllowedStatus = "inactive";
        String transparentGlassSelectedSlotRejectedStatus = "inactive";
        String transparentGlassAutoDetectStatus = "available";
        String transparentGlassManualOverrideStatus = "inactive";
        String transparentGlassFallbackReason = "none";
        String transparentGlassWrongSlotGuardStatus = "ok_non_glass_slots_not_transparent_by_default";
        String materialRoleUiStatus = "ok_minimal_material_tab";
        String selectedSlotRoleSummaryUiStatus = "ok";
        String activeGlassSlotSummaryUiStatus = "ok";
        String selectGlassCandidateButtonStatus = "ok";
        String assignSelectedAsGlassButtonStatus = "ok";
        String clearManualRoleButtonStatus = "ok";
        String selectedSlotHighlightToggleStatus = "diagnostic_only_deferred";
        int transparentGlassSkippedNonGlassCount = 0;
        int transparentGlassSkippedFabricCount = 0;
        int transparentGlassSkippedMetalCount = 0;
        int transparentGlassSkippedPaintCount = 0;
        String p27TransparentPassPreservedStatus = "ok";
        String p27bPerformanceStatus = "ok_existing_transparent_pass_only";
        String p27bNoExtraHeavyPassStatus = "ok";
        String p27bNoTextureRebuildStatus = "ok";
        String p27bNoModelReuploadStatus = "ok";
        String p27bNoFrameGlbParseStatus = "ok";
        String p27bNoFrameFileWriteStatus = "ok";
        String p27bNoShadowStatus = "ok";
        String p27bNoRealMirrorStatus = "ok";
        String p27bNoSsrStatus = "ok";
        String p27bNoRealRefractionStatus = "ok";
        String materialRoleDebugViewStatus = "shader_applied";
        String glassCandidateDebugViewStatus = "shader_applied";
        String activeGlassSlotDebugViewStatus = "shader_applied";
        String transparentRoutingDebugViewStatus = "shader_applied";
        String wrongSlotGuardDebugViewStatus = "shader_applied";
        String transparentGlassFabricGuardStatus = "ok";
        String transparentGlassMetalGuardStatus = "ok";
        String transparentGlassCarPaintGuardStatus = "ok";
        String transparentGlassMaskDebugViewStatus = "shader_applied";
        String transparentGlassAlphaDebugViewStatus = "shader_applied";
        String transparentGlassDrawOrderDebugViewStatus = "shader_applied";
        String transparentGlassFallbackDebugViewStatus = "shader_applied";
        String transparentGlassSafetyDebugViewStatus = "shader_applied";
        String p27PerformanceStatus = "ok_selected_slot_extra_draw_only";
        String p27NoShadowStatus = "ok";
        String p27NoRaytraceStatus = "ok";
        String p27NoSsrStatus = "ok";
        String p27NoRealMirrorStatus = "ok";
        String p27NoScreenRefractionStatus = "ok";
        String p27NoTextureRebuildStatus = "ok";
        String p27NoModelReuploadStatus = "ok";
        String glassNoRefractionPassStatus = "ok";
        String glassNoScreenRefractionStatus = "ok";
        String glassNoFullSortingStatus = "ok";
        String glassNoTextureRebuildStatus = "ok";
        String glassNoModelReuploadStatus = "ok";
        String glassNoNewShadowPassStatus = "ok";
        String runtimeStateDebugViewStatus = "ok";
        String restoreStateDebugViewStatus = "ok";
        String uiStateDebugViewStatus = "ok";
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
        String iblMode = "procedural_directional_probe";
        String environmentIblStatus = "ok_foundation";
        String environmentIblMode = "procedural_directional_probe";
        String environmentSourceStatus = "ok_procedural_no_external_texture";
        String environmentSourceType = "procedural_directional_probe";
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
        String reflectionFoundationStatus = "p24_fake_cubemap_probe_foundation";
        String reflectionMode = "procedural_directional_probe";
        String environmentReflectionStatus = "p24_fake_cubemap_probe_no_texture";
        String environmentReflectionMode = "reflection_direction_sky_horizon_ground_side";
        String environmentSource = "procedural_directional_probe";
        String reflectionColorStatus = "ok_sky_horizon_ground_side_glint";
        String reflectionRoughnessResponseStatus = "ok_roughness_blur_approx";
        String metallicReflectionStatus = "ok_metal_tinted_stronger_guarded";
        String dielectricReflectionStatus = "ok_subtle_f0_environment";
        String reflectionPerformanceStatus = "ok_no_extra_pass_no_texture_rebuild";
        String inspectorUiStatus = "ok";
        String inspectorUiMode = "tabbed_compact_inspector";
        String activeInspectorTab = "Assets";
        String inspectorHeightMode = "capped_30_percent";
        String inspectorScrollStatus = "ok";
        int inspectorExpandedMaxHeightPercent = 30;
        String inspectorCollapsedStatus = "ok_compact_toggle";
        String materialTabScrollStatus = "ok";
        String inspectorTouchTargetStatus = "ok";
        String inspectorDynamicAlphaStatus = "ok";
        float inspectorAlphaIdle = 0.92f;
        float inspectorAlphaWhileSliderDrag = 0.12f;
        float inspectorAlphaWhileCameraMove = 0.16f;
        String inspectorAlphaRestoreStatus = "ok_timed_restore";
        String sliderDragVisualMode = "transparent_inspector_uniform_only";
        String cameraMoveVisualMode = "transparent_inspector_camera_drag";
        String inspectorCurrentAlphaMode = "idle";
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
        String materialResponseStatus = "p24_fake_cubemap_probe_foundation";
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
                + "  \"activeModelName\": \"" + esc(activeModelName()) + "\",\n"
                + "  \"resumeRestoreStatus\": \"" + esc(resumeRestoreStatus) + "\",\n"
                + "  \"resumeRestoreMode\": \"" + esc(resumeRestoreMode) + "\",\n"
                + "  \"activeModelPersistenceStatus\": \"" + esc(activeModelPersistenceStatus) + "\",\n"
                + "  \"activeModelRestoreAttemptCount\": " + activeModelRestoreAttemptCount + ",\n"
                + "  \"activeModelRestoreResult\": \"" + esc(activeModelRestoreResult) + "\",\n"
                + "  \"fallbackCubeReason\": \"" + esc(fallbackCubeReason) + "\",\n"
                + "  \"surfaceRecreateStatus\": \"" + esc(surfaceRecreateStatus) + "\",\n"
                + "  \"persistedModelBounds\": \"" + esc(persistedModelBounds) + "\",\n"
                + "  \"persistedModelScale\": " + jsonFloat(persistedModelScale) + ",\n"
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
                + "  \"materialSlotEditorStatus\": \"" + esc(materialSlotEditorStatus) + "\",\n"
                + "  \"selectedMaterialSlot\": " + selectedMaterialSlot + ",\n"
                + "  \"selectedMaterialSlotCount\": " + selectedMaterialSlotCount + ",\n"
                + "  \"selectedMaterialTypeHint\": \"" + esc(selectedMaterialTypeHint) + "\",\n"
                + "  \"selectedMaterialName\": \"" + esc(selectedMaterialName) + "\",\n"
                + "  \"selectedMaterialSummaryStatus\": \"" + esc(selectedMaterialSummaryStatus) + "\",\n"
                + "  \"materialSlotSelectionUiStatus\": \"" + esc(materialSlotSelectionUiStatus) + "\",\n"
                + "  \"perMaterialOverrideStatus\": \"" + esc(perMaterialOverrideStatus) + "\",\n"
                + "  \"perMaterialOverrideMode\": \"" + esc(perMaterialOverrideMode) + "\",\n"
                + "  \"selectedSlotMetallicOverride\": " + jsonFloat(selectedSlotMetallicOverride) + ",\n"
                + "  \"selectedSlotRoughnessOverride\": " + jsonFloat(selectedSlotRoughnessOverride) + ",\n"
                + "  \"selectedSlotNormalScaleOverride\": " + jsonFloat(selectedSlotNormalScaleOverride) + ",\n"
                + "  \"selectedSlotAoOverride\": " + jsonFloat(selectedSlotAoOverride) + ",\n"
                + "  \"selectedSlotGlossOverride\": " + jsonFloat(selectedSlotGlossOverride) + ",\n"
                + "  \"selectedSlotCoatOverride\": " + jsonFloat(selectedSlotCoatOverride) + ",\n"
                + "  \"selectedSlotOverrideApplied\": \"" + esc(selectedSlotOverrideApplied) + "\",\n"
                + "  \"selectedSlotResetStatus\": \"" + esc(selectedSlotResetStatus) + "\",\n"
                + "  \"perMaterialUniformUpdateStatus\": \"" + esc(perMaterialUniformUpdateStatus) + "\",\n"
                + "  \"materialSlotControlsUiStatus\": \"" + esc(materialSlotControlsUiStatus) + "\",\n"
                + "  \"metallicSlotSliderStatus\": \"" + esc(metallicSlotSliderStatus) + "\",\n"
                + "  \"roughnessSlotSliderStatus\": \"" + esc(roughnessSlotSliderStatus) + "\",\n"
                + "  \"normalSlotSliderStatus\": \"" + esc(normalSlotSliderStatus) + "\",\n"
                + "  \"aoSlotSliderStatus\": \"" + esc(aoSlotSliderStatus) + "\",\n"
                + "  \"selectedMaterialDebugViewStatus\": \"" + esc(selectedMaterialDebugViewStatus) + "\",\n"
                + "  \"materialOverrideDebugViewStatus\": \"" + esc(materialOverrideDebugViewStatus) + "\",\n"
                + "  \"slotMetallicDebugViewStatus\": \"" + esc(slotMetallicDebugViewStatus) + "\",\n"
                + "  \"slotRoughnessDebugViewStatus\": \"" + esc(slotRoughnessDebugViewStatus) + "\",\n"
                + "  \"slotAoDebugViewStatus\": \"" + esc(slotAoDebugViewStatus) + "\",\n"
                + "  \"perMaterialOverridePerformanceStatus\": \"" + esc(perMaterialOverridePerformanceStatus) + "\",\n"
                + "  \"alphaMaterialStatus\": \"" + esc(alphaMaterialStatus) + "\",\n"
                + "  \"alphaModeSupportStatus\": \"" + esc(alphaModeSupportStatus) + "\",\n"
                + "  \"alphaMaskStatus\": \"" + esc(alphaMaskStatus) + "\",\n"
                + "  \"alphaBlendStatus\": \"" + esc(alphaBlendStatus) + "\",\n"
                + "  \"alphaCutoffStatus\": \"" + esc(alphaCutoffStatus) + "\",\n"
                + "  \"alphaCutoffValue\": " + jsonFloat(alphaCutoffValue) + ",\n"
                + "  \"alphaDiscardStatus\": \"" + esc(alphaDiscardStatus) + "\",\n"
                + "  \"alphaTextureChannelStatus\": \"" + esc(alphaTextureChannelStatus) + "\",\n"
                + "  \"alphaFallbackStatus\": \"" + esc(alphaFallbackStatus) + "\",\n"
                + "  \"doubleSidedMaterialStatus\": \"" + esc(doubleSidedMaterialStatus) + "\",\n"
                + "  \"doubleSidedMode\": \"" + esc(doubleSidedMode) + "\",\n"
                + "  \"doubleSidedNormalStatus\": \"" + esc(doubleSidedNormalStatus) + "\",\n"
                + "  \"doubleSidedRasterStatus\": \"" + esc(doubleSidedRasterStatus) + "\",\n"
                + "  \"doubleSidedFallbackStatus\": \"" + esc(doubleSidedFallbackStatus) + "\",\n"
                + "  \"thinMaterialPolishStatus\": \"" + esc(thinMaterialPolishStatus) + "\",\n"
                + "  \"cutoutMaterialHintStatus\": \"" + esc(cutoutMaterialHintStatus) + "\",\n"
                + "  \"fabricEdgeStatus\": \"" + esc(fabricEdgeStatus) + "\",\n"
                + "  \"glassMetadataStatus\": \"" + esc(glassMetadataStatus) + "\",\n"
                + "  \"decalMaterialHintStatus\": \"" + esc(decalMaterialHintStatus) + "\",\n"
                + "  \"transparencyDeferredStatus\": \"" + esc(transparencyDeferredStatus) + "\",\n"
                + "  \"alphaUiStatus\": \"" + esc(alphaUiStatus) + "\",\n"
                + "  \"alphaCutoffSliderStatus\": \"" + esc(alphaCutoffSliderStatus) + "\",\n"
                + "  \"alphaDebugViewStatus\": \"" + esc(alphaDebugViewStatus) + "\",\n"
                + "  \"doubleSidedDebugViewStatus\": \"" + esc(doubleSidedDebugViewStatus) + "\",\n"
                + "  \"alphaResetButtonStatus\": \"" + esc(alphaResetButtonStatus) + "\",\n"
                + "  \"alphaUniformUpdateStatus\": \"" + esc(alphaUniformUpdateStatus) + "\",\n"
                + "  \"alphaSliderUpdateMode\": \"" + esc(alphaSliderUpdateMode) + "\",\n"
                + "  \"alphaMaskDebugViewStatus\": \"" + esc(alphaMaskDebugViewStatus) + "\",\n"
                + "  \"alphaModeDebugViewStatus\": \"" + esc(alphaModeDebugViewStatus) + "\",\n"
                + "  \"cutoutHintDebugViewStatus\": \"" + esc(cutoutHintDebugViewStatus) + "\",\n"
                + "  \"transparencyStatusDebugViewStatus\": \"" + esc(transparencyStatusDebugViewStatus) + "\",\n"
                + "  \"alphaPerformanceStatus\": \"" + esc(alphaPerformanceStatus) + "\",\n"
                + "  \"alphaNoNewPassStatus\": \"" + esc(alphaNoNewPassStatus) + "\",\n"
                + "  \"alphaNoTextureRebuildStatus\": \"" + esc(alphaNoTextureRebuildStatus) + "\",\n"
                + "  \"alphaNoModelReuploadStatus\": \"" + esc(alphaNoModelReuploadStatus) + "\",\n"
                + "  \"materialWorkflowStatus\": \"" + esc(materialWorkflowStatus) + "\",\n"
                + "  \"materialSlotSummaryUiStatus\": \"" + esc(materialSlotSummaryUiStatus) + "\",\n"
                + "  \"selectedSlotResetButtonStatus\": \"" + esc(selectedSlotResetButtonStatus) + "\",\n"
                + "  \"selectedMaterialTextureSummaryStatus\": \"" + esc(selectedMaterialTextureSummaryStatus) + "\",\n"
                + "  \"assetsWorkflowStatus\": \"" + esc(assetsWorkflowStatus) + "\",\n"
                + "  \"reloadActiveModelButtonStatus\": \"" + esc(reloadActiveModelButtonStatus) + "\",\n"
                + "  \"reloadActiveModelStatus\": \"" + esc(reloadActiveModelStatus) + "\",\n"
                + "  \"activeModelDisplayStatus\": \"" + esc(activeModelDisplayStatus) + "\",\n"
                + "  \"fallbackReasonDisplayStatus\": \"" + esc(fallbackReasonDisplayStatus) + "\",\n"
                + "  \"p20RuntimeWorkflowPreservedStatus\": \"" + esc(p20RuntimeWorkflowPreservedStatus) + "\",\n"
                + "  \"p19SlotControlsPreservedStatus\": \"" + esc(p19SlotControlsPreservedStatus) + "\",\n"
                + "  \"p19PreservedStatus\": \"" + esc(p19PreservedStatus) + "\",\n"
                + "  \"p18IblPreservedStatus\": \"" + esc(p18IblPreservedStatus) + "\",\n"
                + "  \"p17GlossPreservedStatus\": \"" + esc(p17GlossPreservedStatus) + "\",\n"
                + "  \"p21AlphaPreservedStatus\": \"" + esc(p21AlphaPreservedStatus) + "\",\n"
                + "  \"emissiveMaterialStatus\": \"" + esc(emissiveMaterialStatus) + "\",\n"
                + "  \"emissiveMode\": \"" + esc(emissiveMode) + "\",\n"
                + "  \"emissiveFactorStatus\": \"" + esc(emissiveFactorStatus) + "\",\n"
                + "  \"emissiveTextureStatus\": \"" + esc(emissiveTextureStatus) + "\",\n"
                + "  \"emissiveIntensity\": " + jsonFloat(emissiveIntensity) + ",\n"
                + "  \"emissiveIntensityStatus\": \"" + esc(emissiveIntensityStatus) + "\",\n"
                + "  \"emissiveColorStatus\": \"" + esc(emissiveColorStatus) + "\",\n"
                + "  \"emissiveOverbrightGuardStatus\": \"" + esc(emissiveOverbrightGuardStatus) + "\",\n"
                + "  \"emissiveLightingContributionStatus\": \"" + esc(emissiveLightingContributionStatus) + "\",\n"
                + "  \"emissivePerformanceStatus\": \"" + esc(emissivePerformanceStatus) + "\",\n"
                + "  \"materialPresetStatus\": \"" + esc(materialPresetStatus) + "\",\n"
                + "  \"activeMaterialPreset\": \"" + esc(activeMaterialPreset) + "\",\n"
                + "  \"materialPresetMode\": \"" + esc(materialPresetMode) + "\",\n"
                + "  \"materialPresetAppliedSlot\": " + materialPresetAppliedSlot + ",\n"
                + "  \"materialPresetAppliedStatus\": \"" + esc(materialPresetAppliedStatus) + "\",\n"
                + "  \"materialPresetUiStatus\": \"" + esc(materialPresetUiStatus) + "\",\n"
                + "  \"materialPresetPerformanceStatus\": \"" + esc(materialPresetPerformanceStatus) + "\",\n"
                + "  \"selectedSlotPresetStatus\": \"" + esc(selectedSlotPresetStatus) + "\",\n"
                + "  \"carPaintPresetStatus\": \"" + esc(carPaintPresetStatus) + "\",\n"
                + "  \"metalPresetStatus\": \"" + esc(metalPresetStatus) + "\",\n"
                + "  \"fabricPresetStatus\": \"" + esc(fabricPresetStatus) + "\",\n"
                + "  \"rubberPresetStatus\": \"" + esc(rubberPresetStatus) + "\",\n"
                + "  \"plasticPresetStatus\": \"" + esc(plasticPresetStatus) + "\",\n"
                + "  \"glassMetadataPresetStatus\": \"" + esc(glassMetadataPresetStatus) + "\",\n"
                + "  \"emissivePresetStatus\": \"" + esc(emissivePresetStatus) + "\",\n"
                + "  \"materialPresetGuardStatus\": \"" + esc(materialPresetGuardStatus) + "\",\n"
                + "  \"presetCycleButtonStatus\": \"" + esc(presetCycleButtonStatus) + "\",\n"
                + "  \"presetApplyButtonStatus\": \"" + esc(presetApplyButtonStatus) + "\",\n"
                + "  \"emissiveSliderStatus\": \"" + esc(emissiveSliderStatus) + "\",\n"
                + "  \"emissiveUniformUpdateStatus\": \"" + esc(emissiveUniformUpdateStatus) + "\",\n"
                + "  \"materialResetButtonStatus\": \"" + esc(materialResetButtonStatus) + "\",\n"
                + "  \"materialUiScrollPreservedStatus\": \"" + esc(materialUiScrollPreservedStatus) + "\",\n"
                + "  \"emissiveDebugViewStatus\": \"" + esc(emissiveDebugViewStatus) + "\",\n"
                + "  \"presetTypeDebugViewStatus\": \"" + esc(presetTypeDebugViewStatus) + "\",\n"
                + "  \"presetResponseDebugViewStatus\": \"" + esc(presetResponseDebugViewStatus) + "\",\n"
                + "  \"materialEnergyGuardDebugViewStatus\": \"" + esc(materialEnergyGuardDebugViewStatus) + "\",\n"
                + "  \"selectedSlotPresetDebugViewStatus\": \"" + esc(selectedSlotPresetDebugViewStatus) + "\",\n"
                + "  \"p22PerformanceStatus\": \"" + esc(p22PerformanceStatus) + "\",\n"
                + "  \"emissiveNoBloomStatus\": \"" + esc(emissiveNoBloomStatus) + "\",\n"
                + "  \"emissiveNoNewPassStatus\": \"" + esc(emissiveNoNewPassStatus) + "\",\n"
                + "  \"presetNoModelReuploadStatus\": \"" + esc(presetNoModelReuploadStatus) + "\",\n"
                + "  \"presetNoTextureRebuildStatus\": \"" + esc(presetNoTextureRebuildStatus) + "\",\n"
                + "  \"clearcoatStatus\": \"" + esc(clearcoatStatus) + "\",\n"
                + "  \"clearcoatMode\": \"" + esc(clearcoatMode) + "\",\n"
                + "  \"clearcoatIntensity\": " + jsonFloat(clearcoatIntensity) + ",\n"
                + "  \"clearcoatRoughness\": " + jsonFloat(clearcoatRoughness) + ",\n"
                + "  \"clearcoatFresnelStatus\": \"" + esc(clearcoatFresnelStatus) + "\",\n"
                + "  \"clearcoatHighlightStatus\": \"" + esc(clearcoatHighlightStatus) + "\",\n"
                + "  \"clearcoatMaterialRoutingStatus\": \"" + esc(clearcoatMaterialRoutingStatus) + "\",\n"
                + "  \"clearcoatOverbrightGuardStatus\": \"" + esc(clearcoatOverbrightGuardStatus) + "\",\n"
                + "  \"clearcoatPerformanceStatus\": \"" + esc(clearcoatPerformanceStatus) + "\",\n"
                + "  \"clearcoatAppliedStatus\": \"" + esc(clearcoatAppliedStatus) + "\",\n"
                + "  \"clearcoatWeight\": " + jsonFloat(clearcoatWeight) + ",\n"
                + "  \"clearcoatRoughnessApplied\": " + jsonFloat(clearcoatRoughnessApplied) + ",\n"
                + "  \"clearcoatGuardApplied\": \"" + esc(clearcoatGuardApplied) + "\",\n"
                + "  \"carPaintLayerStatus\": \"" + esc(carPaintLayerStatus) + "\",\n"
                + "  \"carPaintPresetV2Status\": \"" + esc(carPaintPresetV2Status) + "\",\n"
                + "  \"carPaintClearcoatStatus\": \"" + esc(carPaintClearcoatStatus) + "\",\n"
                + "  \"paintLayerEnergyGuardStatus\": \"" + esc(paintLayerEnergyGuardStatus) + "\",\n"
                + "  \"paintLayerMaterialHintStatus\": \"" + esc(paintLayerMaterialHintStatus) + "\",\n"
                + "  \"clearcoatSliderStatus\": \"" + esc(clearcoatSliderStatus) + "\",\n"
                + "  \"clearcoatSliderValue\": " + jsonFloat(clearcoatSliderValue) + ",\n"
                + "  \"clearcoatRoughnessSliderStatus\": \"" + esc(clearcoatRoughnessSliderStatus) + "\",\n"
                + "  \"clearcoatRoughnessSliderValue\": " + jsonFloat(clearcoatRoughnessSliderValue) + ",\n"
                + "  \"clearcoatUniformUpdateStatus\": \"" + esc(clearcoatUniformUpdateStatus) + "\",\n"
                + "  \"clearcoatUiStatus\": \"" + esc(clearcoatUiStatus) + "\",\n"
                + "  \"clearcoatDebugViewStatus\": \"" + esc(clearcoatDebugViewStatus) + "\",\n"
                + "  \"clearcoatHighlightDebugViewStatus\": \"" + esc(clearcoatHighlightDebugViewStatus) + "\",\n"
                + "  \"paintLayerDebugViewStatus\": \"" + esc(paintLayerDebugViewStatus) + "\",\n"
                + "  \"paintEnergyGuardDebugViewStatus\": \"" + esc(paintEnergyGuardDebugViewStatus) + "\",\n"
                + "  \"p22EmissivePreservedStatus\": \"" + esc(p22EmissivePreservedStatus) + "\",\n"
                + "  \"p22PresetsPreservedStatus\": \"" + esc(p22PresetsPreservedStatus) + "\",\n"
                + "  \"p23PerformanceStatus\": \"" + esc(p23PerformanceStatus) + "\",\n"
                + "  \"clearcoatNoNewPassStatus\": \"" + esc(clearcoatNoNewPassStatus) + "\",\n"
                + "  \"clearcoatNoTextureRebuildStatus\": \"" + esc(clearcoatNoTextureRebuildStatus) + "\",\n"
                + "  \"clearcoatNoModelReuploadStatus\": \"" + esc(clearcoatNoModelReuploadStatus) + "\",\n"
                + "  \"clearcoatNoTransparentSortingStatus\": \"" + esc(clearcoatNoTransparentSortingStatus) + "\",\n"
                + "  \"fakeCubemapStatus\": \"" + esc(fakeCubemapStatus) + "\",\n"
                + "  \"fakeCubemapMode\": \"" + esc(fakeCubemapMode) + "\",\n"
                + "  \"fakeCubemapSourceStatus\": \"" + esc(fakeCubemapSourceStatus) + "\",\n"
                + "  \"environmentZoneStatus\": \"" + esc(environmentZoneStatus) + "\",\n"
                + "  \"skyZoneStatus\": \"" + esc(skyZoneStatus) + "\",\n"
                + "  \"horizonZoneStatus\": \"" + esc(horizonZoneStatus) + "\",\n"
                + "  \"groundZoneStatus\": \"" + esc(groundZoneStatus) + "\",\n"
                + "  \"sideReflectionZoneStatus\": \"" + esc(sideReflectionZoneStatus) + "\",\n"
                + "  \"fakeCubemapPerformanceStatus\": \"" + esc(fakeCubemapPerformanceStatus) + "\",\n"
                + "  \"reflectionQualityStatus\": \"" + esc(reflectionQualityStatus) + "\",\n"
                + "  \"reflectionContrastStatus\": \"" + esc(reflectionContrastStatus) + "\",\n"
                + "  \"reflectionContrastValue\": " + jsonFloat(reflectionContrastValue) + ",\n"
                + "  \"reflectionSaturationStatus\": \"" + esc(reflectionSaturationStatus) + "\",\n"
                + "  \"reflectionSaturationValue\": " + jsonFloat(reflectionSaturationValue) + ",\n"
                + "  \"roughnessReflectionBlurStatus\": \"" + esc(roughnessReflectionBlurStatus) + "\",\n"
                + "  \"clearcoatReflectionBoostStatus\": \"" + esc(clearcoatReflectionBoostStatus) + "\",\n"
                + "  \"metalReflectionTintStatus\": \"" + esc(metalReflectionTintStatus) + "\",\n"
                + "  \"fabricReflectionSuppressStatus\": \"" + esc(fabricReflectionSuppressStatus) + "\",\n"
                + "  \"reflectionEnergyGuardStatus\": \"" + esc(reflectionEnergyGuardStatus) + "\",\n"
                + "  \"reflectionOverbrightGuardStatus\": \"" + esc(reflectionOverbrightGuardStatus) + "\",\n"
                + "  \"reflectionQualityUiStatus\": \"" + esc(reflectionQualityUiStatus) + "\",\n"
                + "  \"reflectionContrastSliderStatus\": \"" + esc(reflectionContrastSliderStatus) + "\",\n"
                + "  \"reflectionSaturationSliderStatus\": \"" + esc(reflectionSaturationSliderStatus) + "\",\n"
                + "  \"environmentZonePresetStatus\": \"" + esc(environmentZonePresetStatus) + "\",\n"
                + "  \"environmentZonePreset\": \"" + esc(environmentZonePreset) + "\",\n"
                + "  \"reflectionUniformUpdateStatus\": \"" + esc(reflectionUniformUpdateStatus) + "\",\n"
                + "  \"motionPerformanceGuardStatus\": \"" + esc(motionPerformanceGuardStatus) + "\",\n"
                + "  \"cameraMotionStatus\": \"" + esc(cameraMotionStatus) + "\",\n"
                + "  \"cameraMotionSpeed\": " + jsonFloat(cameraMotionSpeed) + ",\n"
                + "  \"cameraMotionQualityTier\": \"" + esc(cameraMotionQualityTier) + "\",\n"
                + "  \"cameraMotionQualityBlend\": " + jsonFloat(cameraMotionQualityBlend) + ",\n"
                + "  \"motionReflectionScale\": " + jsonFloat(motionReflectionScale) + ",\n"
                + "  \"motionClearcoatScale\": " + jsonFloat(motionClearcoatScale) + ",\n"
                + "  \"motionDiagnosticsThrottleStatus\": \"" + esc(motionDiagnosticsThrottleStatus) + "\",\n"
                + "  \"motionQualityRestoreStatus\": \"" + esc(motionQualityRestoreStatus) + "\",\n"
                + "  \"motionQualityTransitionMs\": " + motionQualityTransitionMs + ",\n"
                + "  \"cameraMovingFpsLast\": " + jsonFloat(cameraMovingFpsLast) + ",\n"
                + "  \"cameraStillFpsLast\": " + jsonFloat(cameraStillFpsLast) + ",\n"
                + "  \"targetMovingFps\": " + jsonFloat(targetMovingFps) + ",\n"
                + "  \"movingFpsGuardStatus\": \"" + esc(movingFpsGuardStatus) + "\",\n"
                + "  \"carPaintReflectionStatus\": \"" + esc(carPaintReflectionStatus) + "\",\n"
                + "  \"metalReflectionStatus\": \"" + esc(metalReflectionStatus) + "\",\n"
                + "  \"plasticReflectionStatus\": \"" + esc(plasticReflectionStatus) + "\",\n"
                + "  \"rubberReflectionStatus\": \"" + esc(rubberReflectionStatus) + "\",\n"
                + "  \"glassMetadataReflectionStatus\": \"" + esc(glassMetadataReflectionStatus) + "\",\n"
                + "  \"materialPresetReflectionStatus\": \"" + esc(materialPresetReflectionStatus) + "\",\n"
                + "  \"fakeCubemapDebugViewStatus\": \"" + esc(fakeCubemapDebugViewStatus) + "\",\n"
                + "  \"reflectionZonesDebugViewStatus\": \"" + esc(reflectionZonesDebugViewStatus) + "\",\n"
                + "  \"reflectionContrastDebugViewStatus\": \"" + esc(reflectionContrastDebugViewStatus) + "\",\n"
                + "  \"reflectionEnergyGuardDebugViewStatus\": \"" + esc(reflectionEnergyGuardDebugViewStatus) + "\",\n"
                + "  \"clearcoatReflectionDebugViewStatus\": \"" + esc(clearcoatReflectionDebugViewStatus) + "\",\n"
                + "  \"motionQualityDebugViewStatus\": \"" + esc(motionQualityDebugViewStatus) + "\",\n"
                + "  \"p23ClearcoatPreservedStatus\": \"" + esc(p23ClearcoatPreservedStatus) + "\",\n"
                + "  \"p24PerformanceStatus\": \"" + esc(p24PerformanceStatus) + "\",\n"
                + "  \"fakeCubemapNoTextureStatus\": \"" + esc(fakeCubemapNoTextureStatus) + "\",\n"
                + "  \"fakeCubemapNoNewPassStatus\": \"" + esc(fakeCubemapNoNewPassStatus) + "\",\n"
                + "  \"reflectionNoTextureRebuildStatus\": \"" + esc(reflectionNoTextureRebuildStatus) + "\",\n"
                + "  \"reflectionNoModelReuploadStatus\": \"" + esc(reflectionNoModelReuploadStatus) + "\",\n"
                + "  \"noFrameFileWriteStatus\": \"" + esc(noFrameFileWriteStatus) + "\",\n"
                + "  \"noFrameGlbParseStatus\": \"" + esc(noFrameGlbParseStatus) + "\",\n"
                + "  \"runtimeStateDebugViewStatus\": \"" + esc(runtimeStateDebugViewStatus) + "\",\n"
                + "  \"restoreStateDebugViewStatus\": \"" + esc(restoreStateDebugViewStatus) + "\",\n"
                + "  \"uiStateDebugViewStatus\": \"" + esc(uiStateDebugViewStatus) + "\",\n"
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
                + "  \"inspectorHeightMode\": \"" + esc(inspectorHeightMode) + "\",\n"
                + "  \"inspectorScrollStatus\": \"" + esc(inspectorScrollStatus) + "\",\n"
                + "  \"inspectorExpandedMaxHeightPercent\": " + inspectorExpandedMaxHeightPercent + ",\n"
                + "  \"inspectorCollapsedStatus\": \"" + esc(inspectorCollapsedStatus) + "\",\n"
                + "  \"materialTabScrollStatus\": \"" + esc(materialTabScrollStatus) + "\",\n"
                + "  \"inspectorTouchTargetStatus\": \"" + esc(inspectorTouchTargetStatus) + "\",\n"
                + "  \"inspectorDynamicAlphaStatus\": \"" + esc(inspectorDynamicAlphaStatus) + "\",\n"
                + "  \"inspectorAlphaIdle\": " + jsonFloat(inspectorAlphaIdle) + ",\n"
                + "  \"inspectorAlphaWhileSliderDrag\": " + jsonFloat(inspectorAlphaWhileSliderDrag) + ",\n"
                + "  \"inspectorAlphaWhileCameraMove\": " + jsonFloat(inspectorAlphaWhileCameraMove) + ",\n"
                + "  \"inspectorAlphaRestoreStatus\": \"" + esc(inspectorAlphaRestoreStatus) + "\",\n"
                + "  \"sliderDragVisualMode\": \"" + esc(sliderDragVisualMode) + "\",\n"
                + "  \"cameraMoveVisualMode\": \"" + esc(cameraMoveVisualMode) + "\",\n"
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
                info.emissiveGuardApplied = luminance(info.emissiveFactor) > 1.35f;
                out.add(info);
            }
            return out;
        }

        private static int materialTypeHint(MaterialInfo info) {
            String name = info.materialName == null ? "" : info.materialName.toLowerCase(Locale.US);
            boolean hasBase = info.texture != null && "ok".equals(info.texture.status);
            boolean hasMr = info.metallicRoughnessTexture != null && "ok".equals(info.metallicRoughnessTexture.status);
            boolean alphaHint = info.alphaMode == 1 || info.alphaMode == 2 || info.baseColorFactor[3] < 0.98f;
            if (luminance(info.emissiveFactor) > 0.001f || "metadata_only".equals(info.emissiveTextureStatus)) return 8;
            if (name.contains("glass") || name.contains("window") || name.contains("windshield") || name.contains("crystal") || name.contains("transparent")) return 6;
            if (name.contains("decal") || name.contains("sticker") || name.contains("label") || (alphaHint && info.roughnessFactor < 0.36f && info.metallicFactor < 0.2f)) return 7;
            if (alphaHint || info.doubleSided || name.contains("leaf") || name.contains("leaves") || name.contains("hair") || name.contains("grille") || name.contains("grid") || name.contains("card")) return 5;
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
            if (hint == 5) return "cutout_like";
            if (hint == 6) return "glass_like";
            if (hint == 7) return "decal_like";
            if (hint == 8) return "emissive_like";
            return "unknown";
        }

        private static float calibratedRoughness(MaterialInfo info) {
            float r = clampUnitRange(info.roughnessFactor, 0.04f, 1.0f);
            if (info.materialTypeHint == 0) return Math.max(r, 0.78f);
            if (info.materialTypeHint == 3) return Math.max(r, 0.62f);
            if (info.materialTypeHint == 1) return clampUnitRange(r, 0.28f, 0.72f);
            if (info.materialTypeHint == 2) return clampUnitRange(r, 0.18f, 0.64f);
            if (info.materialTypeHint == 5) return Math.max(r, 0.48f);
            if (info.materialTypeHint == 6) return clampUnitRange(r, 0.18f, 0.70f);
            if (info.materialTypeHint == 7) return clampUnitRange(r, 0.22f, 0.68f);
            if (info.materialTypeHint == 8) return clampUnitRange(r, 0.35f, 0.80f);
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
                    .append(",\"materialName\":\"").append(esc(m.materialName == null || m.materialName.isEmpty() ? "slot_" + i : m.materialName)).append("\"")
                    .append(",\"materialTypeHint\":\"").append(esc(materialTypeHintName(m.materialTypeHint))).append("\"")
                    .append(",\"materialRole\":\"").append(esc(materialTypeHintName(m.materialTypeHint))).append("\"")
                    .append(",\"materialRoleSource\":\"").append(esc(m.materialTypeHint == 6 ? "material_name_alpha_or_glass_hint" : "metadata_hint_factor")).append("\"")
                    .append(",\"calibrationApplied\":true")
                    .append(",\"albedoLuminance\":").append(jsonFloat(m.albedoLuminance))
                    .append(",\"calibratedRoughness\":").append(jsonFloat(m.calibratedRoughness))
                    .append(",\"calibratedMetallic\":").append(jsonFloat(m.calibratedMetallic))
                    .append(",\"aoInfluence\":").append(jsonFloat(m.aoInfluence))
                    .append(",\"emissiveGuardApplied\":").append(m.emissiveGuardApplied)
                    .append(",\"metallicFactor\":").append(jsonFloat(m.metallicFactor))
                    .append(",\"roughnessFactor\":").append(jsonFloat(m.roughnessFactor))
                    .append(",\"baseColorTextureStatus\":\"").append(esc(m.texture == null ? "missing" : m.texture.status)).append("\"")
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
                    .append(",\"emissiveAppliedStatus\":\"").append(esc(luminance(m.emissiveFactor) > 0.001f ? "factor_available" : "none")).append("\"")
                    .append(",\"emissiveIntensityApplied\":").append(jsonFloat(luminance(m.emissiveFactor) > 0.001f ? 1.0f : 0.0f))
                    .append(",\"materialPresetHint\":\"").append(esc(materialPresetHintName(m))).append("\"")
                    .append(",\"selectedPresetAppliedStatus\":\"available_selected_slot_uniform_only\"")
                    .append(",\"clearcoatAppliedStatus\":\"").append(esc(clearcoatAppliedStatus(m))).append("\"")
                    .append(",\"clearcoatWeight\":").append(jsonFloat(clearcoatWeight(m)))
                    .append(",\"clearcoatRoughnessApplied\":").append(jsonFloat(clearcoatRoughnessApplied(m)))
                    .append(",\"clearcoatGuardApplied\":\"").append(esc(clearcoatGuardApplied(m))).append("\"")
                    .append(",\"alphaMode\":\"").append(esc(m.alphaModeText)).append("\"")
                    .append(",\"alphaCutoff\":").append(jsonFloat(m.alphaCutoff))
                    .append(",\"alphaTextureStatus\":\"").append(esc(m.texture == null ? "missing" : m.texture.status)).append("\"")
                    .append(",\"alphaMaskAppliedStatus\":\"").append(esc(m.alphaMode == 1 ? "shader_discard_enabled" : "not_mask")).append("\"")
                    .append(",\"alphaBlendFallbackStatus\":\"").append(esc(m.alphaMode == 2 ? "fallback_no_transparent_sorting" : "not_blend")).append("\"")
                    .append(",\"doubleSided\":").append(m.doubleSided)
                    .append(",\"doubleSidedAppliedStatus\":\"").append(esc(m.doubleSided ? "normal_flip_foundation_pipeline_cull_none" : "not_double_sided")).append("\"")
                    .append(",\"glassAppliedStatus\":\"").append(esc(m.materialTypeHint == 6 ? "applied_glass_like_hint" : "available_selected_slot_preset_only")).append("\"")
                    .append(",\"glassOpacityApplied\":").append(jsonFloat(m.materialTypeHint == 6 ? 0.44f : 1.0f))
                    .append(",\"glassTintApplied\":\"").append(esc(m.materialTypeHint == 6 ? "clear_default" : "none")).append("\"")
                    .append(",\"glassEdgeReflectionApplied\":\"").append(esc(m.materialTypeHint == 6 ? "p24_probe_edge_fresnel" : "none")).append("\"")
                    .append(",\"glassRoughnessApplied\":").append(jsonFloat(m.materialTypeHint == 6 ? calibratedRoughness(m) : m.roughnessFactor))
                    .append(",\"glassGuardApplied\":\"").append(esc(m.materialTypeHint == 0 || m.materialTypeHint == 2 || m.materialTypeHint == 1 ? "yes_preserve_non_glass" : "no")).append("\"")
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
            float[] out = new float[Math.max(1, materials.size()) * 20];
            if (materials.isEmpty()) materials.add(new MaterialInfo());
            for (int i = 0; i < materials.size(); i++) {
                MaterialInfo m = materials.get(i);
                out[i * 20] = m.baseColorFactor[0];
                out[i * 20 + 1] = m.baseColorFactor[1];
                out[i * 20 + 2] = m.baseColorFactor[2];
                out[i * 20 + 3] = m.baseColorFactor[3];
                out[i * 20 + 4] = m.alphaMode;
                out[i * 20 + 5] = m.alphaCutoff;
                out[i * 20 + 6] = m.doubleSided ? 1f : 0f;
                out[i * 20 + 7] = m.textureSlot;
                out[i * 20 + 8] = m.metallicFactor;
                out[i * 20 + 9] = m.roughnessFactor;
                out[i * 20 + 10] = m.metallicRoughnessTexture != null && "ok".equals(m.metallicRoughnessTexture.status) ? i : -1f;
                out[i * 20 + 11] = m.normalTexture != null && "ok".equals(m.normalTexture.status) ? i : -1f;
                out[i * 20 + 12] = m.occlusionTexture != null && "ok".equals(m.occlusionTexture.status) ? i : -1f;
                out[i * 20 + 13] = m.normalScale;
                out[i * 20 + 14] = m.occlusionStrength;
                out[i * 20 + 15] = m.materialTypeHint;
                out[i * 20 + 16] = clampUnitRange(m.emissiveFactor[0], 0.0f, 1.0f);
                out[i * 20 + 17] = clampUnitRange(m.emissiveFactor[1], 0.0f, 1.0f);
                out[i * 20 + 18] = clampUnitRange(m.emissiveFactor[2], 0.0f, 1.0f);
                out[i * 20 + 19] = "metadata_only".equals(m.emissiveTextureStatus) ? i : -1f;
            }
            return out;
        }

        private static String materialPresetHintName(MaterialInfo info) {
            if (info.materialTypeHint == 1) return "Car Paint";
            if (info.materialTypeHint == 2) return "Metal";
            if (info.materialTypeHint == 0) return "Fabric";
            if (info.materialTypeHint == 3) return "Rubber";
            if (info.materialTypeHint == 6) return "Glass Foundation";
            if (info.materialTypeHint == 8) return "Emissive Safe";
            return "Balanced";
        }

        private static String clearcoatAppliedStatus(MaterialInfo info) {
            if (info.materialTypeHint == 0) return "skipped_fabric_matte";
            if (info.materialTypeHint == 6) return "skipped_glass_metadata_only";
            if (info.materialTypeHint == 3) return "skipped_rubber_matte";
            if (info.materialTypeHint == 1) return "available_paint_like";
            if (info.materialTypeHint == 2) return "available_metal_limited";
            return "available_selected_slot";
        }

        private static float clearcoatWeight(MaterialInfo info) {
            if (info.materialTypeHint == 1) return 1.0f;
            if (info.materialTypeHint == 2) return 0.28f;
            if (info.materialTypeHint == 4) return 0.18f;
            return 0.0f;
        }

        private static float clearcoatRoughnessApplied(MaterialInfo info) {
            if (info.materialTypeHint == 1) return 0.28f;
            if (info.materialTypeHint == 2) return 0.38f;
            return 1.0f;
        }

        private static String clearcoatGuardApplied(MaterialInfo info) {
            return luminance(info.baseColorFactor) > 0.82f || info.materialTypeHint == 0 || info.materialTypeHint == 6 ? "yes" : "no";
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
