package com.solum.engine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.Matrix;
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
import android.widget.HorizontalScrollView;
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
import com.google.android.filament.Material;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Skybox;
import com.google.android.filament.Texture;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View.AmbientOcclusion;
import com.google.android.filament.View.AntiAliasing;
import com.google.android.filament.View.QualityLevel;
import com.google.android.filament.View.ShadowType;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.FilamentAsset;
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
import org.json.JSONObject;

public class FilamentGlbPreviewActivity extends Activity {
    public static final String EXTRA_MODEL_PATH = "com.solum.engine.extra.MODEL_PATH";
    public static final String EXTRA_MODEL_NAME = "com.solum.engine.extra.MODEL_NAME";

    private static final String PREFS_NAME = "solum_engine_diagnostics";
    private static final String PREF_ACTIVE_MODEL_LOCAL_PATH = "active_model_local_path";
    private static final String PREF_ACTIVE_MODEL_PATH = "active_model_path";
    private static final String PREF_ACTIVE_MODEL_NAME = "active_model_name";
    private static final String PREF_ACTIVE_IBL_PATH = "active_ibl_path";
    private static final String PREF_ACTIVE_IBL_NAME = "active_ibl_name";
    private static final String PREF_FILAMENT_LIGHTING_PRESET = "filament_lighting_preset";
    private static final String PREF_FILAMENT_QUALITY_PROFILE = "filament_quality_profile";
    private static final String PREF_FILAMENT_ACTIVE_TAB = "filament_active_tab";
    private static final String PREF_FILAMENT_PANEL_COLLAPSED = "filament_panel_collapsed";
    private static final String PREF_FILAMENT_SUN = "filament_sun";
    private static final String PREF_FILAMENT_AMBIENT = "filament_ambient_user";
    private static final String PREF_FILAMENT_FILL = "filament_fill";
    private static final String PREF_FILAMENT_EXPOSURE = "filament_exposure";
    private static final String PREF_FILAMENT_BG = "filament_bg";
    private static final String PREF_FILAMENT_AO_MODE = "filament_ao_mode";
    private static final String PREF_FILAMENT_BLOOM_MODE = "filament_bloom_mode";
    private static final String PREF_FILAMENT_SHADOW_MODE = "filament_shadow_mode";
    private static final String PREF_FILAMENT_REFRACTION_MODE = "filament_refraction_mode";
    private static final String PREF_FILAMENT_RENDER_SCALE = "filament_render_scale";
    private static final String PREF_FILAMENT_DYNAMIC_RESOLUTION = "filament_dynamic_resolution";
    private static final String PREF_FILAMENT_MSAA_SAMPLES = "filament_msaa_samples";
    private static final String PREF_FILAMENT_SKYBOX_VISIBLE = "filament_skybox_visible";
    private static final String PREF_FILAMENT_IBL_ROTATION = "filament_ibl_rotation";
    private static final String PREF_FILAMENT_SUN_AZIMUTH = "filament_sun_azimuth";
    private static final String PREF_FILAMENT_SUN_ELEVATION = "filament_sun_elevation";
    private static final String PREF_FILAMENT_MODEL_RX = "filament_model_rx";
    private static final String PREF_FILAMENT_MODEL_RY = "filament_model_ry";
    private static final String PREF_FILAMENT_MODEL_RZ = "filament_model_rz";
    private static final String PREF_FILAMENT_MODEL_SCALE = "filament_model_scale";
    private static final String PREF_FILAMENT_MODEL_OX = "filament_model_ox";
    private static final String PREF_FILAMENT_MODEL_OY = "filament_model_oy";
    private static final String PREF_FILAMENT_MODEL_OZ = "filament_model_oz";
    private static final String PREF_FILAMENT_CAMERA_DISTANCE = "filament_camera_distance";
    private static final String PREF_FILAMENT_CAMERA_TARGET_X = "filament_camera_target_x";
    private static final String PREF_FILAMENT_CAMERA_TARGET_Y = "filament_camera_target_y";
    private static final String PREF_FILAMENT_CAMERA_TARGET_Z = "filament_camera_target_z";
    private static final String PREF_FILAMENT_CAMERA_FOV = "filament_camera_fov";
    private static final String CONFIG_FILE_NAME = "filament_render_config.json";
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
    private Button collapseButton;
    private Button assetsTabButton;
    private Button lightingTabButton;
    private Button renderTabButton;
    private Button iblTabButton;
    private Button shadowsTabButton;
    private Button cameraTabButton;
    private Button modelTabButton;
    private Button glassTabButton;
    private Button configTabButton;
    private Button qualityTabButton;
    private Button materialTabButton;
    private Button debugTabButton;
    private LinearLayout workspacePanel;
    private LinearLayout tabRow;
    private LinearLayout assetsPanel;
    private LinearLayout lightingPanel;
    private LinearLayout renderPanel;
    private LinearLayout iblPanel;
    private LinearLayout shadowsPanel;
    private LinearLayout cameraPanel;
    private LinearLayout modelPanel;
    private LinearLayout glassPanel;
    private LinearLayout configPanel;
    private LinearLayout qualityPanel;
    private LinearLayout materialPanel;
    private LinearLayout debugPanel;
    private ScrollView workspaceScroll;
    private LinearLayout advancedValuesPanel;
    private TextView assetsSummaryView;
    private TextView materialSummaryView;
    private TextView qualitySummaryView;
    private TextView iblSummaryView;
    private TextView shadowSummaryView;
    private TextView cameraSummaryView;
    private TextView modelSummaryView;
    private TextView glassSummaryView;
    private TextView configSummaryView;
    private TextView debugSummaryView;
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
    private AoMode aoMode = AoMode.OFF;
    private BloomMode bloomMode = BloomMode.OFF;
    private ShadowMode shadowMode = ShadowMode.OFF;
    private RefractionMode refractionMode = RefractionMode.OFF;
    private WorkspaceTab activeTab = WorkspaceTab.ASSETS;
    private boolean panelCollapsed = true;
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
    private String aoActualStatus = "not_applied";
    private String aoActuallyApplied = "false";
    private String aoTypeStatus = "NONE";
    private String aoLikelyInvisibleReason = "ao_off";
    private String shadowActualStatus = "not_applied";
    private String shadowsActuallyApplied = "false";
    private String shadowTypeStatus = "none";
    private String bloomActualStatus = "not_applied";
    private String refractionActualStatus = "not_applied";
    private String refractionActuallyApplied = "false";
    private String materialInspectorStatus = "not_loaded";
    private int materialCount = 0;
    private int selectedMaterialIndex = 0;
    private int actualSampleCount = 2;
    private float dynamicMinScale = 0.72f;
    private float dynamicMaxScale = 0.95f;
    private boolean aoEnabled = false;
    private boolean bloomEnabled = false;
    private boolean shadowsEnabled = false;
    private boolean refractionEnabled = false;
    private boolean advancedValuesEnabled = false;
    private boolean dynamicResolutionEnabled = true;
    private boolean ditheringEnabled = true;
    private boolean skyboxVisible = true;
    private float sunLightIntensity = 2.5f;
    private float ambientUserIntensity = 1.0f;
    private float ambientFallbackIntensity = 1.0f;
    private float fillLightIntensity = 0.0f;
    private float exposure = 1.0f;
    private float backgroundBrightness = 0.14f;
    private float renderScale = 0.95f;
    private float iblRotation = 0.0f;
    private float sunAzimuth = -145.0f;
    private float sunElevation = 45.0f;
    private float modelRotationX = 0.0f;
    private float modelRotationY = 0.0f;
    private float modelRotationZ = 0.0f;
    private float modelScale = 1.0f;
    private float modelOffsetX = 0.0f;
    private float modelOffsetY = 0.0f;
    private float modelOffsetZ = 0.0f;
    private float cameraDistance = 4.4f;
    private float cameraTargetX = 0.0f;
    private float cameraTargetY = 0.0f;
    private float cameraTargetZ = 0.0f;
    private float cameraFov = 45.0f;
    private String configStatus = "not_loaded";
    private String modelTransformStatus = "not_applied";
    private String transformTargetStatus = "none";
    private String cameraApplyStatus = "not_applied";

    private enum FilamentQualityProfile {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH_PREVIEW("High Preview"),
        ULTRA_PREVIEW("Ultra Preview");

        final String label;

        FilamentQualityProfile(String label) {
            this.label = label;
        }

        FilamentQualityProfile next() {
            if (this == LOW) return MEDIUM;
            if (this == MEDIUM) return HIGH_PREVIEW;
            if (this == HIGH_PREVIEW) return ULTRA_PREVIEW;
            return LOW;
        }
    }

    private enum WorkspaceTab {
        ASSETS("Assets"),
        RENDER("Render"),
        LIGHTING("Lighting"),
        IBL("IBL"),
        SHADOWS("Shadows"),
        CAMERA("Camera"),
        MODEL("Model"),
        MATERIAL("Material"),
        GLASS("Glass"),
        CONFIG("Config"),
        DEBUG("Debug");

        final String label;

        WorkspaceTab(String label) {
            this.label = label;
        }
    }

    private enum AoMode {
        OFF("AO Off"),
        SOFT("AO Soft"),
        MEDIUM("AO Medium"),
        STRONG("AO Strong"),
        DEBUG_MAX("AO Debug Max");

        final String label;

        AoMode(String label) {
            this.label = label;
        }

        AoMode next() {
            int next = (ordinal() + 1) % values().length;
            return values()[next];
        }
    }

    private enum BloomMode {
        OFF("Bloom Off"),
        SOFT("Bloom Soft"),
        MEDIUM("Bloom Medium"),
        DEBUG("Bloom Debug");

        final String label;

        BloomMode(String label) {
            this.label = label;
        }

        BloomMode next() {
            int next = (ordinal() + 1) % values().length;
            return values()[next];
        }
    }

    private enum ShadowMode {
        OFF("Shadows Off"),
        SOFT("Shadows Soft"),
        MEDIUM("Shadows Medium"),
        SHARP("Shadows Sharp");

        final String label;

        ShadowMode(String label) {
            this.label = label;
        }

        ShadowMode next() {
            int next = (ordinal() + 1) % values().length;
            return values()[next];
        }
    }

    private enum RefractionMode {
        OFF("Refraction Off"),
        ON("Refraction On"),
        DEBUG("Refraction Debug");

        final String label;

        RefractionMode(String label) {
            this.label = label;
        }

        RefractionMode next() {
            int next = (ordinal() + 1) % values().length;
            return values()[next];
        }
    }

    private enum LightingPreset {
        SAFE_STUDIO("Safe Studio", new float[] {-0.35f, -0.75f, -0.55f}, 2.5f, 1.0f, 0.0f, 1.0f, 0.14f, new float[] {0.70f, -0.35f, -0.62f}, RefractionMode.OFF),
        BALANCED("Balanced", new float[] {-0.28f, -0.78f, -0.55f}, 3.0f, 1.0f, 0.0f, 1.0f, 0.16f, new float[] {0.66f, -0.38f, -0.65f}, RefractionMode.OFF),
        CINEMATIC_FOREST("Cinematic Forest", new float[] {-0.62f, -0.62f, -0.48f}, 2.0f, 1.0f, 0.0f, 0.95f, 0.12f, new float[] {0.48f, -0.32f, -0.82f}, RefractionMode.OFF),
        GLASS_PREVIEW("Glass Preview", new float[] {-0.35f, -0.82f, -0.45f}, 2.0f, 1.0f, 0.0f, 1.0f, 0.12f, new float[] {0.65f, -0.30f, -0.70f}, RefractionMode.ON),
        CHARACTER_PREVIEW("Character Preview", new float[] {-0.30f, -0.78f, -0.54f}, 2.5f, 1.0f, 0.0f, 1.0f, 0.16f, new float[] {0.68f, -0.35f, -0.64f}, RefractionMode.OFF),
        DARK_INSPECT("Dark Inspect", new float[] {-0.48f, -0.70f, -0.52f}, 1.0f, 0.7f, 0.0f, 0.8f, 0.08f, new float[] {0.50f, -0.30f, -0.80f}, RefractionMode.OFF),
        BRIGHT_INSPECT("Bright Inspect", new float[] {-0.20f, -0.82f, -0.48f}, 4.0f, 1.2f, 0.0f, 1.1f, 0.20f, new float[] {0.62f, -0.42f, -0.66f}, RefractionMode.OFF);

        final String label;
        final float[] sunDirection;
        final float sunIntensity;
        final float ambientFallbackIntensity;
        final float fillIntensity;
        final float exposure;
        final float backgroundBrightness;
        final float[] fillDirection;
        final RefractionMode refractionMode;

        LightingPreset(String label, float[] sunDirection, float sunIntensity, float ambientFallbackIntensity, float fillIntensity, float exposure, float backgroundBrightness, float[] fillDirection, RefractionMode refractionMode) {
            this.label = label;
            this.sunDirection = sunDirection;
            this.sunIntensity = sunIntensity;
            this.ambientFallbackIntensity = ambientFallbackIntensity;
            this.fillIntensity = fillIntensity;
            this.exposure = exposure;
            this.backgroundBrightness = backgroundBrightness;
            this.fillDirection = fillDirection;
            this.refractionMode = refractionMode;
        }

        LightingPreset next() {
            if (this == SAFE_STUDIO) return BALANCED;
            if (this == BALANCED) return CINEMATIC_FOREST;
            if (this == CINEMATIC_FOREST) return GLASS_PREVIEW;
            if (this == GLASS_PREVIEW) return CHARACTER_PREVIEW;
            if (this == CHARACTER_PREVIEW) return DARK_INSPECT;
            if (this == DARK_INSPECT) return BRIGHT_INSPECT;
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

        restoreWorkspaceSettings();

        workspacePanel = new LinearLayout(this);
        workspacePanel.setOrientation(LinearLayout.VERTICAL);
        workspacePanel.setPadding(dp(10), dp(8), dp(10), dp(8));
        workspacePanel.setBackgroundColor(Color.argb(172, 4, 12, 16));
        collapseButton = button(panelCollapsed ? "Expand" : "Collapse");
        collapseButton.setOnClickListener(v -> {
            panelCollapsed = !panelCollapsed;
            persistWorkspaceSettings();
            syncWorkspaceUi();
            updateHud();
        });

        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        assetsTabButton = tabButton("Assets", WorkspaceTab.ASSETS);
        renderTabButton = tabButton("Render", WorkspaceTab.RENDER);
        lightingTabButton = tabButton("Lighting", WorkspaceTab.LIGHTING);
        iblTabButton = tabButton("IBL", WorkspaceTab.IBL);
        shadowsTabButton = tabButton("Shadows", WorkspaceTab.SHADOWS);
        cameraTabButton = tabButton("Camera", WorkspaceTab.CAMERA);
        modelTabButton = tabButton("Model", WorkspaceTab.MODEL);
        materialTabButton = tabButton("Material", WorkspaceTab.MATERIAL);
        glassTabButton = tabButton("Glass", WorkspaceTab.GLASS);
        configTabButton = tabButton("Config", WorkspaceTab.CONFIG);
        debugTabButton = tabButton("Debug", WorkspaceTab.DEBUG);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT);
        tabRow.addView(assetsTabButton, tabParams);
        tabRow.addView(renderTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(lightingTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(iblTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(shadowsTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(cameraTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(modelTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(materialTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(glassTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(configTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(debugTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.addView(tabRow, new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT));

        workspacePanel.addView(collapseButton);
        workspacePanel.addView(tabScroll);

        assetsPanel = new LinearLayout(this);
        assetsPanel.setOrientation(LinearLayout.VERTICAL);
        renderPanel = new LinearLayout(this);
        renderPanel.setOrientation(LinearLayout.VERTICAL);
        lightingPanel = new LinearLayout(this);
        lightingPanel.setOrientation(LinearLayout.VERTICAL);
        iblPanel = new LinearLayout(this);
        iblPanel.setOrientation(LinearLayout.VERTICAL);
        shadowsPanel = new LinearLayout(this);
        shadowsPanel.setOrientation(LinearLayout.VERTICAL);
        cameraPanel = new LinearLayout(this);
        cameraPanel.setOrientation(LinearLayout.VERTICAL);
        modelPanel = new LinearLayout(this);
        modelPanel.setOrientation(LinearLayout.VERTICAL);
        qualityPanel = new LinearLayout(this);
        qualityPanel.setOrientation(LinearLayout.VERTICAL);
        materialPanel = new LinearLayout(this);
        materialPanel.setOrientation(LinearLayout.VERTICAL);
        glassPanel = new LinearLayout(this);
        glassPanel.setOrientation(LinearLayout.VERTICAL);
        configPanel = new LinearLayout(this);
        configPanel.setOrientation(LinearLayout.VERTICAL);
        debugPanel = new LinearLayout(this);
        debugPanel.setOrientation(LinearLayout.VERTICAL);

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
        Button reloadButton = button("Reload Model");
        reloadButton.setOnClickListener(v -> loadModel());
        assetsSummaryView = overlayText(10.0f, 6);
        assetsSummaryView.setBackgroundColor(Color.TRANSPARENT);
        assetsPanel.addView(importModelButton);
        assetsPanel.addView(importIblButton);
        assetsPanel.addView(scanDownloadButton);
        assetsPanel.addView(reloadButton);
        assetsPanel.addView(assetsSummaryView);

        qualityButton = button("Quality: " + qualityProfile.label);
        qualityButton.setOnClickListener(v -> {
            qualityProfile = qualityProfile.next();
            persistWorkspaceSettings();
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
            aoMode = aoMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            updateHud();
        });
        bloomButton = button("");
        bloomButton.setOnClickListener(v -> {
            bloomMode = bloomMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            updateHud();
        });
        shadowsButton = button("");
        shadowsButton.setOnClickListener(v -> {
            shadowMode = shadowMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            applyLightingValues();
            updateHud();
        });
        refractionButton = button("");
        refractionButton.setOnClickListener(v -> {
            refractionMode = refractionMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            updateHud();
        });
        lightingPanel.addView(lightingButton);
        lightingPanel.addView(resetButton);
        sunSlider = addLightingSlider(lightingPanel, "Sun", 0.0f, 20.0f, 0.5f, sunLightIntensity, v -> {
            sunLightIntensity = v;
            applyLightingValues();
        });
        ambientSlider = addLightingSlider(lightingPanel, "Ambient", 0.0f, 10.0f, 0.1f, ambientUserIntensity, v -> {
            ambientUserIntensity = v;
            applyLightingValues();
        });
        fillSlider = addLightingSlider(lightingPanel, "Fill", 0.0f, 10.0f, 0.1f, fillLightIntensity, v -> {
            fillLightIntensity = v;
            applyLightingValues();
        });
        addLightingSlider(lightingPanel, "Az", -180.0f, 180.0f, 1.0f, sunAzimuth, v -> {
            sunAzimuth = v;
            applyLightingValues();
        });
        addLightingSlider(lightingPanel, "El", 0.0f, 89.0f, 1.0f, sunElevation, v -> {
            sunElevation = v;
            applyLightingValues();
        });
        exposureSlider = addLightingSlider(lightingPanel, "Exp", 0.30f, 2.50f, 0.01f, exposure, v -> {
            exposure = v;
            applyLightingValues();
        });
        backgroundSlider = addLightingSlider(lightingPanel, "BG", 0.0f, 0.80f, 0.01f, backgroundBrightness, v -> {
            backgroundBrightness = v;
            applyLightingValues();
        });
        lightingPanel.addView(advancedValuesButton);
        advancedValuesPanel = new LinearLayout(this);
        advancedValuesPanel.setOrientation(LinearLayout.VERTICAL);
        advancedValuesPanel.setPadding(0, dp(4), 0, dp(4));
        advancedValuesPanel.setVisibility(View.GONE);
        advancedSunField = addAdvancedField(advancedValuesPanel, "Sun", 0.0f, 300.0f, v -> sunLightIntensity = v);
        advancedAmbientField = addAdvancedField(advancedValuesPanel, "Ambient", 0.0f, 100.0f, v -> ambientUserIntensity = v);
        advancedFillField = addAdvancedField(advancedValuesPanel, "Fill", 0.0f, 300.0f, v -> fillLightIntensity = v);
        advancedExposureField = addAdvancedField(advancedValuesPanel, "Exposure", 0.10f, 5.00f, v -> exposure = v);
        advancedBackgroundField = addAdvancedField(advancedValuesPanel, "Background", 0.0f, 1.0f, v -> backgroundBrightness = v);
        lightingPanel.addView(advancedValuesPanel);

        qualitySummaryView = overlayText(10.0f, 12);
        qualitySummaryView.setBackgroundColor(Color.TRANSPARENT);
        renderPanel.addView(qualityButton);
        renderPanel.addView(button("Dynamic Resolution: On", v -> {
            dynamicResolutionEnabled = !dynamicResolutionEnabled;
            applyQualityProfile();
            updateHud();
        }));
        renderPanel.addView(button("MSAA Cycle 1/2/4", v -> {
            actualSampleCount = actualSampleCount == 1 ? 2 : (actualSampleCount == 2 ? 4 : 1);
            applyQualityProfile();
            updateHud();
        }));
        addLightingSlider(renderPanel, "Scale", 0.50f, 1.00f, 0.01f, renderScale, v -> {
            renderScale = v;
            applyQualityProfile();
        });
        renderPanel.addView(aoButton);
        renderPanel.addView(bloomButton);
        renderPanel.addView(refractionButton);
        renderPanel.addView(qualitySummaryView);

        iblSummaryView = overlayText(10.0f, 12);
        iblSummaryView.setBackgroundColor(Color.TRANSPARENT);
        iblPanel.addView(iblButton);
        iblPanel.addView(button("Skybox On/Off", v -> {
            skyboxVisible = !skyboxVisible;
            applySkyboxVisibility();
            updateHud();
        }));
        addLightingSlider(iblPanel, "IBL", 0.0f, 10.0f, 0.1f, ambientUserIntensity, v -> {
            ambientUserIntensity = v;
            applyLightingValues();
        });
        addLightingSlider(iblPanel, "Rot", 0.0f, 360.0f, 1.0f, iblRotation, v -> {
            iblRotation = v;
            applyIblRotation();
        });
        iblPanel.addView(iblSummaryView);

        shadowSummaryView = overlayText(10.0f, 12);
        shadowSummaryView.setBackgroundColor(Color.TRANSPARENT);
        shadowsPanel.addView(shadowsButton);
        shadowsPanel.addView(shadowSummaryView);

        buildCameraPanel();
        buildModelPanel();

        materialSummaryView = overlayText(10.0f, 12);
        materialSummaryView.setBackgroundColor(Color.TRANSPARENT);
        materialPanel.addView(materialSummaryView);

        glassSummaryView = overlayText(10.0f, 10);
        glassSummaryView.setBackgroundColor(Color.TRANSPARENT);
        glassPanel.addView(button("Refraction On/Off", v -> {
            refractionMode = refractionMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            updateHud();
        }));
        glassPanel.addView(glassSummaryView);

        configSummaryView = overlayText(10.0f, 10);
        configSummaryView.setBackgroundColor(Color.TRANSPARENT);
        configPanel.addView(button("Save Config", v -> saveConfig(configFile(), "save_config")));
        configPanel.addView(button("Load Config", v -> loadConfig(configFile(), "load_config")));
        configPanel.addView(button("Save As Default", v -> saveConfig(configFile(), "save_as_default")));
        configPanel.addView(button("Reset Safe Defaults", v -> resetSafeDefaults()));
        configPanel.addView(button("Export Config", v -> saveConfig(configFile(), "export_config")));
        configPanel.addView(button("Import Config", v -> loadConfig(configFile(), "import_config")));
        configPanel.addView(configSummaryView);

        statusView = overlayText(10.0f, 28);
        debugSummaryView = statusView;
        Button closeButton = button("Close Preview");
        closeButton.setOnClickListener(v -> closePreview());
        debugPanel.addView(closeButton);
        debugPanel.addView(statusView);

        workspacePanel.addView(assetsPanel);
        workspacePanel.addView(renderPanel);
        workspacePanel.addView(lightingPanel);
        workspacePanel.addView(iblPanel);
        workspacePanel.addView(shadowsPanel);
        workspacePanel.addView(cameraPanel);
        workspacePanel.addView(modelPanel);
        workspacePanel.addView(materialPanel);
        workspacePanel.addView(glassPanel);
        workspacePanel.addView(configPanel);
        workspacePanel.addView(debugPanel);
        ScrollView controlScroll = new ScrollView(this);
        workspaceScroll = controlScroll;
        controlScroll.setFillViewport(false);
        controlScroll.addView(workspacePanel, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        int labHeight = panelCollapsed ? dp(136) : Math.max(dp(260), Math.round(getResources().getDisplayMetrics().heightPixels * 0.40f));
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, labHeight);
        controlParams.gravity = Gravity.BOTTOM;
        controlParams.setMargins(dp(12), dp(12), dp(12), dp(28));
        root.addView(controlScroll, controlParams);
        setContentView(root);
        syncWorkspaceUi();
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
        persistWorkspaceSettings();
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
            applyCameraControls();
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

    private Button tabButton(String label, WorkspaceTab tab) {
        Button button = button(label);
        button.setMinHeight(dp(42));
        button.setTextSize(10.0f);
        button.setOnClickListener(v -> {
            activeTab = tab;
            panelCollapsed = false;
            persistWorkspaceSettings();
            syncWorkspaceUi();
            updateHud();
        });
        return button;
    }

    private void syncWorkspaceUi() {
        if (collapseButton != null) collapseButton.setText(panelCollapsed ? "Expand" : "Collapse");
        if (tabRow != null) tabRow.setVisibility(panelCollapsed ? View.GONE : View.VISIBLE);
        setPanelVisible(assetsPanel, activeTab == WorkspaceTab.ASSETS && !panelCollapsed);
        setPanelVisible(renderPanel, activeTab == WorkspaceTab.RENDER && !panelCollapsed);
        setPanelVisible(lightingPanel, activeTab == WorkspaceTab.LIGHTING && !panelCollapsed);
        setPanelVisible(iblPanel, activeTab == WorkspaceTab.IBL && !panelCollapsed);
        setPanelVisible(shadowsPanel, activeTab == WorkspaceTab.SHADOWS && !panelCollapsed);
        setPanelVisible(cameraPanel, activeTab == WorkspaceTab.CAMERA && !panelCollapsed);
        setPanelVisible(modelPanel, activeTab == WorkspaceTab.MODEL && !panelCollapsed);
        setPanelVisible(qualityPanel, false);
        setPanelVisible(materialPanel, activeTab == WorkspaceTab.MATERIAL && !panelCollapsed);
        setPanelVisible(glassPanel, activeTab == WorkspaceTab.GLASS && !panelCollapsed);
        setPanelVisible(configPanel, activeTab == WorkspaceTab.CONFIG && !panelCollapsed);
        setPanelVisible(debugPanel, activeTab == WorkspaceTab.DEBUG && !panelCollapsed);
        updateTabState(assetsTabButton, WorkspaceTab.ASSETS);
        updateTabState(renderTabButton, WorkspaceTab.RENDER);
        updateTabState(lightingTabButton, WorkspaceTab.LIGHTING);
        updateTabState(iblTabButton, WorkspaceTab.IBL);
        updateTabState(shadowsTabButton, WorkspaceTab.SHADOWS);
        updateTabState(cameraTabButton, WorkspaceTab.CAMERA);
        updateTabState(modelTabButton, WorkspaceTab.MODEL);
        updateTabState(qualityTabButton, WorkspaceTab.RENDER);
        updateTabState(materialTabButton, WorkspaceTab.MATERIAL);
        updateTabState(glassTabButton, WorkspaceTab.GLASS);
        updateTabState(configTabButton, WorkspaceTab.CONFIG);
        updateTabState(debugTabButton, WorkspaceTab.DEBUG);
        if (activeTab == WorkspaceTab.MATERIAL && !panelCollapsed) updateMaterialInspector();
        if (workspaceScroll != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) workspaceScroll.getLayoutParams();
            if (params != null) {
                params.height = panelCollapsed ? dp(74) : Math.max(dp(260), Math.round(getResources().getDisplayMetrics().heightPixels * 0.42f));
                workspaceScroll.setLayoutParams(params);
            }
        }
    }

    private void setPanelVisible(View panel, boolean visible) {
        if (panel != null) panel.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateTabState(Button button, WorkspaceTab tab) {
        if (button == null) return;
        button.setText((activeTab == tab ? "* " : "") + tab.label);
        button.setAlpha(activeTab == tab ? 1.0f : 0.72f);
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
            applyModelTransform();
            applyCameraControls();
            cameraStatus = "orbit_drag_pinch_zoom_unit_cube_autofit_with_ui_pan";
            loadStatus = "ok_loaded_with_gltfio";
            gltfioLoaded = "true";
            updateMaterialInspector();
            applyRenderableShadowMode();
        } catch (Throwable t) {
            loadStatus = "load_error: " + shortMessage(t);
            gltfioLoaded = "false";
            materialInspectorStatus = "not_available_model_load_failed";
        }
        updateHud();
    }

    private void applyRenderableShadowMode() {
        if (modelViewer == null || modelViewer.getAsset() == null) return;
        try {
            RenderableManager renderables = modelViewer.getEngine().getRenderableManager();
            for (int entity : modelViewer.getAsset().getRenderableEntities()) {
                int instance = renderables.getInstance(entity);
                if (instance == 0) continue;
                renderables.setCastShadows(instance, shadowMode != ShadowMode.OFF);
                renderables.setReceiveShadows(instance, shadowMode != ShadowMode.OFF);
            }
        } catch (Throwable t) {
            shadowActualStatus = "renderable_shadow_flags_failed: " + shortMessage(t);
        }
    }

    private void updateMaterialInspector() {
        materialCount = 0;
        materialInspectorStatus = "not_loaded";
        if (modelViewer == null || modelViewer.getAsset() == null) return;
        try {
            FilamentAsset asset = modelViewer.getAsset();
            RenderableManager renderables = modelViewer.getEngine().getRenderableManager();
            StringBuilder detail = new StringBuilder();
            int entityCount = 0;
            for (int entity : asset.getRenderableEntities()) {
                int instance = renderables.getInstance(entity);
                if (instance == 0) continue;
                entityCount++;
                int primitiveCount = renderables.getPrimitiveCount(instance);
                materialCount += primitiveCount;
                if (detail.length() < 900) {
                    String entityName = asset.getName(entity);
                    detail.append(entityCount).append(". ")
                        .append(entityName == null || entityName.isEmpty() ? "entity_" + entity : entityName)
                        .append(" primitives=").append(primitiveCount).append("\n");
                    for (int i = 0; i < primitiveCount && detail.length() < 900; i++) {
                        MaterialInstance mi = renderables.getMaterialInstanceAt(instance, i);
                        Material material = mi == null ? null : mi.getMaterial();
                        detail.append("  [").append(i).append("] ")
                            .append(mi == null ? "materialInstance=none" : "mi=" + safeText(mi.getName()))
                            .append(" material=").append(material == null ? "none" : safeText(material.getName()))
                            .append(" doubleSided=").append(mi != null && mi.isDoubleSided())
                            .append(" alpha=").append(mi == null ? "unknown" : mi.getTransparencyMode())
                            .append(" culling=").append(mi == null ? "unknown" : mi.getCullingMode())
                            .append("\n");
                    }
                }
            }
            materialInspectorStatus = materialCount > 0 ? "ok_read_only_filament_material_instances" : "limited_by_gltfio_api_no_material_instances";
            if (materialSummaryView != null) {
                materialSummaryView.setText("materialInspectorStatus=" + materialInspectorStatus
                    + "\nmaterialCount=" + materialCount
                    + "\nselectedMaterialIndex=" + selectedMaterialIndex
                    + "\nbaseColor/metallic/roughness/texture flags=limited_by_gltfio_java_api"
                    + "\n" + detail);
            }
        } catch (Throwable t) {
            materialInspectorStatus = "limited_by_gltfio_api: " + shortMessage(t);
        }
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
        applySkyboxVisibility();
        applyIblRotation();
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
        applySkyboxVisibility();
        applyIblRotation();
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
            applySkyboxVisibility();
            applyIblRotation();
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
            ambientUserIntensity = lightingPreset.ambientFallbackIntensity;
            ambientFallbackIntensity = ambientToInternal(ambientUserIntensity);
            sunLightIntensity = lightingPreset.sunIntensity;
            fillLightIntensity = lightingPreset.fillIntensity;
            exposure = lightingPreset.exposure;
            backgroundBrightness = lightingPreset.backgroundBrightness;
            refractionMode = lightingPreset.refractionMode;
            persistWorkspaceSettings();
            applyQualityProfile();
            applyLightingValues();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        if (lightingButton != null) lightingButton.setText("Lighting: " + lightingPreset.label);
    }

    private void resetSafeLighting() {
        lightingPreset = LightingPreset.SAFE_STUDIO;
        aoMode = AoMode.OFF;
        bloomMode = BloomMode.OFF;
        shadowMode = ShadowMode.OFF;
        refractionMode = RefractionMode.OFF;
        lastInputError = "none";
        applyLightingPreset();
        applyQualityProfile();
        updateHud();
    }

    private void applyLightingValues() {
        if (modelViewer == null) return;
        try {
            sunLightIntensity = clamp(sunLightIntensity, 0.0f, 300.0f);
            ambientUserIntensity = clamp(ambientUserIntensity, 0.0f, 100.0f);
            ambientFallbackIntensity = ambientToInternal(ambientUserIntensity);
            fillLightIntensity = clamp(fillLightIntensity, 0.0f, 300.0f);
            exposure = clamp(exposure, 0.10f, 5.00f);
            backgroundBrightness = clamp(backgroundBrightness, 0.0f, 1.0f);
            if (indirectLight != null) indirectLight.setIntensity(ambientFallbackIntensity);
            if (skybox != null && !realIblReady.equals("true")) skybox.setColor(backgroundColor());

            Engine engine = modelViewer.getEngine();
            LightManager lights = engine.getLightManager();
            int sunInstance = lights.getInstance(modelViewer.getLight());
            if (sunInstance != 0) {
                float[] sunDirection = sunDirectionFromAngles();
                lights.setDirection(sunInstance, sunDirection[0], sunDirection[1], sunDirection[2]);
                lights.setIntensity(sunInstance, sunLightIntensity);
                lights.setColor(sunInstance, 1.0f, 0.96f, 0.90f);
                lights.setShadowCaster(sunInstance, shadowMode != ShadowMode.OFF);
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
            applyRenderableShadowMode();
            Renderer.ClearOptions clear = modelViewer.getRenderer().getClearOptions();
            clear.clear = true;
            clear.discard = true;
            clear.clearColor = backgroundColor();
            modelViewer.getRenderer().setClearOptions(clear);
            lightingStatus = advancedValuesEnabled ? "live_values_applied_advanced_ranges" : "live_values_applied_safe_slider_ranges";
            if (realIblReady.equals("true")) iblStatus = "ok_real_ibl_intensity_controlled_by_ambient_slider";
            persistWorkspaceSettings();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        updateLightingControlLabels();
        updateHud();
    }

    private float[] sunDirectionFromAngles() {
        double az = Math.toRadians(sunAzimuth);
        double el = Math.toRadians(sunElevation);
        float x = (float) (Math.cos(el) * Math.sin(az));
        float y = (float) -Math.sin(el);
        float z = (float) (Math.cos(el) * Math.cos(az));
        return new float[] {x, y, z};
    }

    private void applyQualityProfile() {
        if (modelViewer == null) return;
        try {
            com.google.android.filament.View view = modelViewer.getView();
            com.google.android.filament.View.AmbientOcclusionOptions ao = view.getAmbientOcclusionOptions();
            com.google.android.filament.View.BloomOptions bloom = view.getBloomOptions();
            com.google.android.filament.View.DynamicResolutionOptions dynamic = view.getDynamicResolutionOptions();
            com.google.android.filament.View.RenderQuality renderQuality = view.getRenderQuality();
            aoEnabled = aoMode != AoMode.OFF;
            bloomEnabled = bloomMode != BloomMode.OFF;
            shadowsEnabled = shadowMode != ShadowMode.OFF;
            refractionEnabled = refractionMode != RefractionMode.OFF;
            if (qualityProfile == FilamentQualityProfile.LOW) {
                view.setAntiAliasing(AntiAliasing.NONE);
                view.setSampleCount(actualSampleCount);
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                applyAoOptions(ao, QualityLevel.LOW);
                applyBloomOptions(bloom, QualityLevel.LOW);
                applyShadowOptions(view);
                dynamic.enabled = dynamicResolutionEnabled;
                dynamic.minScale = 0.58f;
                dynamic.maxScale = Math.max(dynamic.minScale, Math.min(0.82f, renderScale));
                dynamic.quality = QualityLevel.LOW;
                renderQuality.hdrColorBuffer = QualityLevel.LOW;
                setActualQualityStatus("NONE", actualSampleCount, 0.58f, dynamic.maxScale, "low_quality_only_dynamic_safe");
            } else if (qualityProfile == FilamentQualityProfile.HIGH_PREVIEW || qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW) {
                view.setAntiAliasing(AntiAliasing.FXAA);
                if (qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW && actualSampleCount < 4) actualSampleCount = 4;
                view.setSampleCount(actualSampleCount);
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                applyAoOptions(ao, QualityLevel.MEDIUM);
                applyBloomOptions(bloom, QualityLevel.MEDIUM);
                applyShadowOptions(view);
                dynamic.enabled = dynamicResolutionEnabled;
                dynamic.minScale = qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW ? 1.00f : 0.86f;
                dynamic.maxScale = qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW ? 1.00f : Math.max(dynamic.minScale, renderScale);
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.HIGH;
                setActualQualityStatus("FXAA", actualSampleCount, dynamic.minScale, dynamic.maxScale, qualityProfile.name().toLowerCase(Locale.US) + "_quality_only");
            } else {
                view.setAntiAliasing(AntiAliasing.FXAA);
                view.setSampleCount(actualSampleCount);
                view.setAmbientOcclusion(aoEnabled ? AmbientOcclusion.SSAO : AmbientOcclusion.NONE);
                view.setShadowingEnabled(shadowsEnabled);
                view.setScreenSpaceRefractionEnabled(refractionEnabled);
                view.setPostProcessingEnabled(true);
                applyAoOptions(ao, QualityLevel.LOW);
                applyBloomOptions(bloom, QualityLevel.LOW);
                applyShadowOptions(view);
                dynamic.enabled = dynamicResolutionEnabled;
                dynamic.minScale = 0.72f;
                dynamic.maxScale = Math.max(dynamic.minScale, renderScale);
                dynamic.quality = QualityLevel.MEDIUM;
                renderQuality.hdrColorBuffer = QualityLevel.MEDIUM;
                setActualQualityStatus("FXAA", actualSampleCount, 0.72f, dynamic.maxScale, "medium_quality_only_dynamic_safe");
            }
            view.setAmbientOcclusionOptions(ao);
            view.setBloomOptions(bloom);
            view.setDynamicResolutionOptions(dynamic);
            view.setRenderQuality(renderQuality);
            applyColorGrading();
            applyLightingValues();
            persistWorkspaceSettings();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            qualityFeatureStatus = "apply_failed";
        }
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
        updateToggleLabels();
    }

    private void applyAoOptions(com.google.android.filament.View.AmbientOcclusionOptions ao, QualityLevel quality) {
        ao.enabled = aoEnabled;
        ao.quality = quality;
        ao.lowPassFilter = quality;
        ao.upsampling = quality;
        ao.resolution = qualityProfile == FilamentQualityProfile.LOW ? 0.5f : 1.0f;
        ao.bias = 0.0005f;
        ao.bilateralThreshold = 0.05f;
        aoLikelyInvisibleReason = aoEnabled ? "none" : "ao_off";
        if (aoMode == AoMode.OFF) {
            ao.radius = 0.0f;
            ao.intensity = 0.0f;
            ao.power = 0.0f;
        } else if (aoMode == AoMode.SOFT) {
            ao.radius = 0.35f;
            ao.intensity = 0.35f;
            ao.power = 0.8f;
        } else if (aoMode == AoMode.MEDIUM) {
            ao.radius = 0.55f;
            ao.intensity = 0.8f;
            ao.power = 1.2f;
        } else if (aoMode == AoMode.STRONG) {
            ao.radius = 0.8f;
            ao.intensity = 1.4f;
            ao.power = 1.8f;
        } else {
            ao.radius = 1.2f;
            ao.intensity = 3.0f;
            ao.power = 3.0f;
            aoLikelyInvisibleReason = "none_if_scene_has_depth_contact_areas";
        }
        aoActualStatus = "mode=" + aoMode.name() + " radius=" + twoDecimal(ao.radius) + " intensity=" + twoDecimal(ao.intensity) + " power=" + twoDecimal(ao.power);
        aoActuallyApplied = aoEnabled ? "true_view_ssao_options_set" : "false_disabled";
        aoTypeStatus = aoEnabled ? "SSAO" : "NONE";
    }

    private void applyBloomOptions(com.google.android.filament.View.BloomOptions bloom, QualityLevel quality) {
        bloom.enabled = bloomEnabled;
        bloom.quality = quality;
        bloom.threshold = true;
        bloom.levels = 6;
        bloom.resolution = 360;
        if (bloomMode == BloomMode.OFF) {
            bloom.strength = 0.0f;
            bloom.highlight = 1000.0f;
        } else if (bloomMode == BloomMode.SOFT) {
            bloom.strength = 0.025f;
            bloom.highlight = 800.0f;
        } else if (bloomMode == BloomMode.MEDIUM) {
            bloom.strength = 0.05f;
            bloom.highlight = 500.0f;
        } else {
            bloom.strength = 0.12f;
            bloom.highlight = 250.0f;
        }
        bloomActualStatus = "mode=" + bloomMode.name() + " enabled=" + bloom.enabled + " strength=" + twoDecimal(bloom.strength);
    }

    private void applyShadowOptions(com.google.android.filament.View view) {
        try {
            if (shadowMode == ShadowMode.OFF) {
                shadowTypeStatus = "none";
                shadowsActuallyApplied = "false_disabled";
            } else if (shadowMode == ShadowMode.SOFT) {
                view.setShadowType(ShadowType.PCF);
                shadowTypeStatus = "PCF_soft";
                shadowsActuallyApplied = "true_view_shadowing_and_sun_cast_shadows";
            } else if (shadowMode == ShadowMode.MEDIUM) {
                view.setShadowType(ShadowType.DPCF);
                shadowTypeStatus = "DPCF_medium";
                shadowsActuallyApplied = "true_view_shadowing_and_sun_cast_shadows";
            } else {
                view.setShadowType(ShadowType.PCF);
                shadowTypeStatus = "PCF_sharp";
                shadowsActuallyApplied = "true_view_shadowing_and_sun_cast_shadows";
            }
            shadowActualStatus = "mode=" + shadowMode.name() + " enabled=" + shadowsEnabled + " type=" + shadowTypeStatus + " mapSize=filament_default cascades=filament_default";
        } catch (Throwable t) {
            shadowActualStatus = "shadow_type_apply_failed: " + shortMessage(t);
            shadowsActuallyApplied = shadowsEnabled ? "partial_view_shadowing_only" : "false_disabled";
        }
        refractionActualStatus = "mode=" + refractionMode.name() + " screenSpaceRefraction=" + refractionEnabled;
        refractionActuallyApplied = refractionEnabled ? "true_view_screen_space_refraction_enabled" : "false_disabled";
    }

    private void setActualQualityStatus(String aa, int sampleCount, float minScale, float maxScale, String status) {
        actualAA = aa;
        actualSampleCount = sampleCount;
        dynamicMinScale = minScale;
        dynamicMaxScale = maxScale;
        qualityFeatureStatus = status + "_aoMode_" + aoMode.name() + "_bloomMode_" + bloomMode.name() + "_shadowsMode_" + shadowMode.name() + "_refractionMode_" + refractionMode.name();
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
        if (local != null && !local.isEmpty() && new File(local).isFile()) return local;
        String path = prefs.getString(PREF_ACTIVE_MODEL_PATH, "");
        if (path != null && !path.isEmpty() && new File(path).isFile()) return path;
        File latest = findLatestModel();
        return latest == null ? "" : latest.getAbsolutePath();
    }

    private File findLatestModel() {
        List<File> files = new ArrayList<>();
        collectAssetFiles(modelsDir(), files, true, false);
        File latest = null;
        for (File file : files) {
            if (latest == null || file.lastModified() > latest.lastModified()) latest = file;
        }
        return latest;
    }

    private void restoreWorkspaceSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        qualityProfile = enumPref(prefs, PREF_FILAMENT_QUALITY_PROFILE, FilamentQualityProfile.MEDIUM);
        lightingPreset = enumPref(prefs, PREF_FILAMENT_LIGHTING_PRESET, LightingPreset.SAFE_STUDIO);
        activeTab = enumPref(prefs, PREF_FILAMENT_ACTIVE_TAB, WorkspaceTab.ASSETS);
        aoMode = enumPref(prefs, PREF_FILAMENT_AO_MODE, AoMode.OFF);
        bloomMode = enumPref(prefs, PREF_FILAMENT_BLOOM_MODE, BloomMode.OFF);
        shadowMode = enumPref(prefs, PREF_FILAMENT_SHADOW_MODE, ShadowMode.OFF);
        refractionMode = enumPref(prefs, PREF_FILAMENT_REFRACTION_MODE, RefractionMode.OFF);
        panelCollapsed = prefs.getBoolean(PREF_FILAMENT_PANEL_COLLAPSED, true);
        sunLightIntensity = prefs.getFloat(PREF_FILAMENT_SUN, lightingPreset.sunIntensity);
        ambientUserIntensity = prefs.getFloat(PREF_FILAMENT_AMBIENT, lightingPreset.ambientFallbackIntensity);
        fillLightIntensity = prefs.getFloat(PREF_FILAMENT_FILL, lightingPreset.fillIntensity);
        exposure = prefs.getFloat(PREF_FILAMENT_EXPOSURE, lightingPreset.exposure);
        backgroundBrightness = prefs.getFloat(PREF_FILAMENT_BG, lightingPreset.backgroundBrightness);
        renderScale = prefs.getFloat(PREF_FILAMENT_RENDER_SCALE, 0.95f);
        dynamicResolutionEnabled = prefs.getBoolean(PREF_FILAMENT_DYNAMIC_RESOLUTION, true);
        actualSampleCount = prefs.getInt(PREF_FILAMENT_MSAA_SAMPLES, 2);
        skyboxVisible = prefs.getBoolean(PREF_FILAMENT_SKYBOX_VISIBLE, true);
        iblRotation = prefs.getFloat(PREF_FILAMENT_IBL_ROTATION, 0.0f);
        sunAzimuth = prefs.getFloat(PREF_FILAMENT_SUN_AZIMUTH, -145.0f);
        sunElevation = prefs.getFloat(PREF_FILAMENT_SUN_ELEVATION, 45.0f);
        modelRotationX = prefs.getFloat(PREF_FILAMENT_MODEL_RX, 0.0f);
        modelRotationY = prefs.getFloat(PREF_FILAMENT_MODEL_RY, 0.0f);
        modelRotationZ = prefs.getFloat(PREF_FILAMENT_MODEL_RZ, 0.0f);
        modelScale = prefs.getFloat(PREF_FILAMENT_MODEL_SCALE, 1.0f);
        modelOffsetX = prefs.getFloat(PREF_FILAMENT_MODEL_OX, 0.0f);
        modelOffsetY = prefs.getFloat(PREF_FILAMENT_MODEL_OY, 0.0f);
        modelOffsetZ = prefs.getFloat(PREF_FILAMENT_MODEL_OZ, 0.0f);
        cameraDistance = prefs.getFloat(PREF_FILAMENT_CAMERA_DISTANCE, 4.4f);
        cameraTargetX = prefs.getFloat(PREF_FILAMENT_CAMERA_TARGET_X, 0.0f);
        cameraTargetY = prefs.getFloat(PREF_FILAMENT_CAMERA_TARGET_Y, 0.0f);
        cameraTargetZ = prefs.getFloat(PREF_FILAMENT_CAMERA_TARGET_Z, 0.0f);
        cameraFov = prefs.getFloat(PREF_FILAMENT_CAMERA_FOV, 45.0f);
        ambientFallbackIntensity = ambientToInternal(ambientUserIntensity);
    }

    private void persistWorkspaceSettings() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(PREF_FILAMENT_QUALITY_PROFILE, qualityProfile.name())
            .putString(PREF_FILAMENT_LIGHTING_PRESET, lightingPreset.name())
            .putString(PREF_FILAMENT_ACTIVE_TAB, activeTab.name())
            .putString(PREF_FILAMENT_AO_MODE, aoMode.name())
            .putString(PREF_FILAMENT_BLOOM_MODE, bloomMode.name())
            .putString(PREF_FILAMENT_SHADOW_MODE, shadowMode.name())
            .putString(PREF_FILAMENT_REFRACTION_MODE, refractionMode.name())
            .putBoolean(PREF_FILAMENT_PANEL_COLLAPSED, panelCollapsed)
            .putFloat(PREF_FILAMENT_SUN, sunLightIntensity)
            .putFloat(PREF_FILAMENT_AMBIENT, ambientUserIntensity)
            .putFloat(PREF_FILAMENT_FILL, fillLightIntensity)
            .putFloat(PREF_FILAMENT_EXPOSURE, exposure)
            .putFloat(PREF_FILAMENT_BG, backgroundBrightness)
            .putFloat(PREF_FILAMENT_RENDER_SCALE, renderScale)
            .putBoolean(PREF_FILAMENT_DYNAMIC_RESOLUTION, dynamicResolutionEnabled)
            .putInt(PREF_FILAMENT_MSAA_SAMPLES, actualSampleCount)
            .putBoolean(PREF_FILAMENT_SKYBOX_VISIBLE, skyboxVisible)
            .putFloat(PREF_FILAMENT_IBL_ROTATION, iblRotation)
            .putFloat(PREF_FILAMENT_SUN_AZIMUTH, sunAzimuth)
            .putFloat(PREF_FILAMENT_SUN_ELEVATION, sunElevation)
            .putFloat(PREF_FILAMENT_MODEL_RX, modelRotationX)
            .putFloat(PREF_FILAMENT_MODEL_RY, modelRotationY)
            .putFloat(PREF_FILAMENT_MODEL_RZ, modelRotationZ)
            .putFloat(PREF_FILAMENT_MODEL_SCALE, modelScale)
            .putFloat(PREF_FILAMENT_MODEL_OX, modelOffsetX)
            .putFloat(PREF_FILAMENT_MODEL_OY, modelOffsetY)
            .putFloat(PREF_FILAMENT_MODEL_OZ, modelOffsetZ)
            .putFloat(PREF_FILAMENT_CAMERA_DISTANCE, cameraDistance)
            .putFloat(PREF_FILAMENT_CAMERA_TARGET_X, cameraTargetX)
            .putFloat(PREF_FILAMENT_CAMERA_TARGET_Y, cameraTargetY)
            .putFloat(PREF_FILAMENT_CAMERA_TARGET_Z, cameraTargetZ)
            .putFloat(PREF_FILAMENT_CAMERA_FOV, cameraFov)
            .apply();
    }

    private static <T extends Enum<T>> T enumPref(SharedPreferences prefs, String key, T fallback) {
        String raw = prefs.getString(key, fallback.name());
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), raw);
        } catch (Throwable ignored) {
            return fallback;
        }
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

    private void buildCameraPanel() {
        cameraSummaryView = overlayText(10.0f, 12);
        cameraSummaryView.setBackgroundColor(Color.TRANSPARENT);
        cameraPanel.addView(button("Reset Camera", v -> resetCameraControls()));
        cameraPanel.addView(button("Fit Model", v -> fitModelCamera()));
        addLightingSlider(cameraPanel, "Dist", 1.0f, 12.0f, 0.1f, cameraDistance, v -> {
            cameraDistance = v;
            applyCameraControls();
        });
        addLightingSlider(cameraPanel, "Pan X", -3.0f, 3.0f, 0.05f, cameraTargetX, v -> {
            cameraTargetX = v;
            applyCameraControls();
        });
        addLightingSlider(cameraPanel, "Pan Y", -3.0f, 3.0f, 0.05f, cameraTargetY, v -> {
            cameraTargetY = v;
            applyCameraControls();
        });
        addLightingSlider(cameraPanel, "Target Z", -3.0f, 3.0f, 0.05f, cameraTargetZ, v -> {
            cameraTargetZ = v;
            applyCameraControls();
        });
        addLightingSlider(cameraPanel, "FOV", 20.0f, 80.0f, 1.0f, cameraFov, v -> {
            cameraFov = v;
            applyCameraControls();
        });
        cameraPanel.addView(cameraSummaryView);
    }

    private void buildModelPanel() {
        modelSummaryView = overlayText(10.0f, 12);
        modelSummaryView.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout rxRow = row();
        rxRow.addView(button("Rotate X -90", v -> nudgeModelRotation(-90.0f, 0.0f, 0.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        rxRow.addView(button("Rotate X +90", v -> nudgeModelRotation(90.0f, 0.0f, 0.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        modelPanel.addView(rxRow);
        LinearLayout ryRow = row();
        ryRow.addView(button("Rotate Y -90", v -> nudgeModelRotation(0.0f, -90.0f, 0.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        ryRow.addView(button("Rotate Y +90", v -> nudgeModelRotation(0.0f, 90.0f, 0.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        modelPanel.addView(ryRow);
        LinearLayout rzRow = row();
        rzRow.addView(button("Rotate Z -90", v -> nudgeModelRotation(0.0f, 0.0f, -90.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        rzRow.addView(button("Rotate Z +90", v -> nudgeModelRotation(0.0f, 0.0f, 90.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        modelPanel.addView(rzRow);
        addLightingSlider(modelPanel, "Rot X", -180.0f, 180.0f, 1.0f, modelRotationX, v -> {
            modelRotationX = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Rot Y", -180.0f, 180.0f, 1.0f, modelRotationY, v -> {
            modelRotationY = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Rot Z", -180.0f, 180.0f, 1.0f, modelRotationZ, v -> {
            modelRotationZ = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Scale", 0.10f, 10.0f, 0.05f, modelScale, v -> {
            modelScale = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Off X", -3.0f, 3.0f, 0.05f, modelOffsetX, v -> {
            modelOffsetX = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Off Y", -3.0f, 3.0f, 0.05f, modelOffsetY, v -> {
            modelOffsetY = v;
            applyModelTransform();
        });
        addLightingSlider(modelPanel, "Off Z", -3.0f, 3.0f, 0.05f, modelOffsetZ, v -> {
            modelOffsetZ = v;
            applyModelTransform();
        });
        modelPanel.addView(button("Reset Model Transform", v -> resetModelTransform()));
        modelPanel.addView(button("Auto Fit after Transform", v -> {
            applyModelTransform();
            fitModelCamera();
        }));
        modelPanel.addView(modelSummaryView);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void nudgeModelRotation(float dx, float dy, float dz) {
        modelRotationX = wrapDegrees(modelRotationX + dx);
        modelRotationY = wrapDegrees(modelRotationY + dy);
        modelRotationZ = wrapDegrees(modelRotationZ + dz);
        applyModelTransform();
    }

    private void resetModelTransform() {
        modelRotationX = 0.0f;
        modelRotationY = 0.0f;
        modelRotationZ = 0.0f;
        modelScale = 1.0f;
        modelOffsetX = 0.0f;
        modelOffsetY = 0.0f;
        modelOffsetZ = 0.0f;
        applyModelTransform();
    }

    private void applyModelTransform() {
        if (modelViewer == null || modelViewer.getAsset() == null) {
            modelTransformStatus = "false_no_loaded_asset";
            return;
        }
        try {
            int root = modelViewer.getAsset().getRoot();
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(root);
            if (instance == 0) {
                modelTransformStatus = "false_root_transform_instance_missing";
                transformTargetStatus = "rootEntity_missing_transform";
                return;
            }
            float[] m = new float[16];
            Matrix.setIdentityM(m, 0);
            Matrix.translateM(m, 0, modelOffsetX, modelOffsetY, modelOffsetZ);
            Matrix.rotateM(m, 0, modelRotationZ, 0.0f, 0.0f, 1.0f);
            Matrix.rotateM(m, 0, modelRotationY, 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(m, 0, modelRotationX, 1.0f, 0.0f, 0.0f);
            Matrix.scaleM(m, 0, modelScale, modelScale, modelScale);
            tm.setTransform(instance, m);
            modelTransformStatus = "true";
            transformTargetStatus = "rootEntity";
            persistWorkspaceSettings();
        } catch (Throwable t) {
            modelTransformStatus = "false_apply_failed: " + shortMessage(t);
            transformTargetStatus = "rootEntity_failed";
        }
        updateHud();
    }

    private void resetCameraControls() {
        cameraDistance = 4.4f;
        cameraTargetX = 0.0f;
        cameraTargetY = 0.0f;
        cameraTargetZ = 0.0f;
        cameraFov = 45.0f;
        applyCameraControls();
    }

    private void fitModelCamera() {
        if (modelViewer != null) {
            try {
                modelViewer.transformToUnitCube(new Float3(0.0f, 0.0f, 0.0f));
                applyModelTransform();
            } catch (Throwable ignored) { }
        }
        cameraDistance = 4.4f;
        applyCameraControls();
    }

    private void applyCameraControls() {
        if (modelViewer == null) return;
        try {
            float distance = clamp(cameraDistance, 1.0f, 30.0f);
            double aspect = surfaceView == null || surfaceView.getHeight() <= 0 ? 1.0 : Math.max(0.1, (double) surfaceView.getWidth() / (double) surfaceView.getHeight());
            modelViewer.getCamera().setProjection(cameraFov, aspect, 0.05, 250.0, com.google.android.filament.Camera.Fov.VERTICAL);
            modelViewer.getCamera().lookAt(cameraTargetX, cameraTargetY, cameraTargetZ + distance, cameraTargetX, cameraTargetY, cameraTargetZ, 0.0f, 1.0f, 0.0f);
            cameraApplyStatus = "applied_lookAt_target_offset";
            cameraStatus = "distance=" + twoDecimal(cameraDistance) + " target=" + twoDecimal(cameraTargetX) + "/" + twoDecimal(cameraTargetY) + "/" + twoDecimal(cameraTargetZ) + " fov=" + oneDecimal(cameraFov) + " nearFar=0.05/250";
            persistWorkspaceSettings();
        } catch (Throwable t) {
            cameraApplyStatus = "apply_failed: " + shortMessage(t);
        }
        updateHud();
    }

    private void applySkyboxVisibility() {
        if (modelViewer == null || modelViewer.getScene() == null) return;
        try {
            modelViewer.getScene().setSkybox(skyboxVisible ? skybox : null);
            skyboxReady = skyboxVisible ? (skybox == null ? "false" : skyboxReady) : "hidden_user_toggle";
            persistWorkspaceSettings();
        } catch (Throwable t) {
            skyboxReady = "toggle_failed: " + shortMessage(t);
        }
    }

    private void applyIblRotation() {
        try {
            if (indirectLight != null) {
                float[] m = new float[9];
                double radians = Math.toRadians(iblRotation);
                float c = (float) Math.cos(radians);
                float s = (float) Math.sin(radians);
                m[0] = c; m[1] = 0.0f; m[2] = -s;
                m[3] = 0.0f; m[4] = 1.0f; m[5] = 0.0f;
                m[6] = s; m[7] = 0.0f; m[8] = c;
                indirectLight.getClass().getMethod("setRotation", float[].class).invoke(indirectLight, (Object) m);
            }
            persistWorkspaceSettings();
        } catch (Throwable t) {
            iblStatus = "iblRotation_not_exposed_or_failed: " + shortMessage(t);
        }
        updateHud();
    }

    private void resetSafeDefaults() {
        qualityProfile = FilamentQualityProfile.MEDIUM;
        lightingPreset = LightingPreset.SAFE_STUDIO;
        aoMode = AoMode.OFF;
        bloomMode = BloomMode.OFF;
        shadowMode = ShadowMode.OFF;
        refractionMode = RefractionMode.OFF;
        dynamicResolutionEnabled = true;
        renderScale = 0.95f;
        actualSampleCount = 2;
        skyboxVisible = true;
        iblRotation = 0.0f;
        sunLightIntensity = 2.5f;
        ambientUserIntensity = 1.0f;
        fillLightIntensity = 0.0f;
        exposure = 1.0f;
        backgroundBrightness = 0.14f;
        resetModelTransform();
        resetCameraControls();
        applyQualityProfile();
        applyLightingValues();
        configStatus = "safe_defaults_reset";
        updateHud();
    }

    private File configFile() {
        File dir = new File("/storage/emulated/0/Download/SOLUM/config");
        if (!dir.mkdirs() && !dir.isDirectory()) dir = new File(getFilesDir(), "solum/config");
        dir.mkdirs();
        return new File(dir, CONFIG_FILE_NAME);
    }

    private void saveConfig(File file, String reason) {
        try {
            JSONObject json = new JSONObject();
            json.put("schema", "solum_filament_render_config");
            json.put("schemaVersion", 1);
            json.put("qualityProfile", qualityProfile.name());
            json.put("renderScale", renderScale);
            json.put("dynamicResolutionEnabled", dynamicResolutionEnabled);
            json.put("aaMode", actualAA);
            json.put("msaaSamples", actualSampleCount);
            json.put("taaSupported", "not_exposed");
            json.put("taaEnabled", false);
            json.put("fxaaEnabled", "FXAA".equals(actualAA));
            json.put("ditheringEnabled", ditheringEnabled);
            json.put("lightingPreset", lightingPreset.name());
            json.put("sunIntensity", sunLightIntensity);
            json.put("sunAzimuth", sunAzimuth);
            json.put("sunElevation", sunElevation);
            json.put("iblStrength", ambientUserIntensity);
            json.put("iblRotation", iblRotation);
            json.put("fillIntensity", fillLightIntensity);
            json.put("exposure", exposure);
            json.put("background", backgroundBrightness);
            json.put("skyboxVisible", skyboxVisible);
            json.put("aoMode", aoMode.name());
            json.put("bloomMode", bloomMode.name());
            json.put("shadowMode", shadowMode.name());
            json.put("refractionMode", refractionMode.name());
            json.put("cameraDistance", cameraDistance);
            json.put("cameraTargetX", cameraTargetX);
            json.put("cameraTargetY", cameraTargetY);
            json.put("cameraTargetZ", cameraTargetZ);
            json.put("fov", cameraFov);
            json.put("modelRotateX", modelRotationX);
            json.put("modelRotateY", modelRotationY);
            json.put("modelRotateZ", modelRotationZ);
            json.put("modelScale", modelScale);
            json.put("modelOffsetX", modelOffsetX);
            json.put("modelOffsetY", modelOffsetY);
            json.put("modelOffsetZ", modelOffsetZ);
            json.put("panelCollapsed", panelCollapsed);
            json.put("lastSelectedTab", activeTab.name());
            json.put("activeIblPath", futureIblAssetPath);
            json.put("activeIblName", iblFile);
            json.put("activeModelPath", modelCopiedPath);
            json.put("activeModelName", modelName);
            writeText(file, json.toString(2));
            persistWorkspaceSettings();
            configStatus = reason + "_ok: " + file.getAbsolutePath();
        } catch (Throwable t) {
            configStatus = reason + "_failed: " + shortMessage(t);
        }
        updateHud();
    }

    private void loadConfig(File file, String reason) {
        try {
            if (file == null || !file.isFile()) throw new IllegalStateException("config_missing");
            JSONObject json = new JSONObject(readText(file));
            qualityProfile = enumValue(FilamentQualityProfile.class, json.optString("qualityProfile"), FilamentQualityProfile.MEDIUM);
            lightingPreset = enumValue(LightingPreset.class, json.optString("lightingPreset"), LightingPreset.SAFE_STUDIO);
            aoMode = enumValue(AoMode.class, json.optString("aoMode"), AoMode.OFF);
            bloomMode = enumValue(BloomMode.class, json.optString("bloomMode"), BloomMode.OFF);
            shadowMode = enumValue(ShadowMode.class, json.optString("shadowMode"), ShadowMode.OFF);
            refractionMode = enumValue(RefractionMode.class, json.optString("refractionMode"), RefractionMode.OFF);
            renderScale = (float) json.optDouble("renderScale", renderScale);
            dynamicResolutionEnabled = json.optBoolean("dynamicResolutionEnabled", dynamicResolutionEnabled);
            actualSampleCount = json.optInt("msaaSamples", actualSampleCount);
            ditheringEnabled = json.optBoolean("ditheringEnabled", ditheringEnabled);
            sunLightIntensity = (float) json.optDouble("sunIntensity", sunLightIntensity);
            sunAzimuth = (float) json.optDouble("sunAzimuth", sunAzimuth);
            sunElevation = (float) json.optDouble("sunElevation", sunElevation);
            ambientUserIntensity = (float) json.optDouble("iblStrength", ambientUserIntensity);
            iblRotation = (float) json.optDouble("iblRotation", iblRotation);
            fillLightIntensity = (float) json.optDouble("fillIntensity", fillLightIntensity);
            exposure = (float) json.optDouble("exposure", exposure);
            backgroundBrightness = (float) json.optDouble("background", backgroundBrightness);
            skyboxVisible = json.optBoolean("skyboxVisible", skyboxVisible);
            cameraDistance = (float) json.optDouble("cameraDistance", cameraDistance);
            cameraTargetX = (float) json.optDouble("cameraTargetX", cameraTargetX);
            cameraTargetY = (float) json.optDouble("cameraTargetY", cameraTargetY);
            cameraTargetZ = (float) json.optDouble("cameraTargetZ", cameraTargetZ);
            cameraFov = (float) json.optDouble("fov", cameraFov);
            modelRotationX = (float) json.optDouble("modelRotateX", modelRotationX);
            modelRotationY = (float) json.optDouble("modelRotateY", modelRotationY);
            modelRotationZ = (float) json.optDouble("modelRotateZ", modelRotationZ);
            modelScale = (float) json.optDouble("modelScale", modelScale);
            modelOffsetX = (float) json.optDouble("modelOffsetX", modelOffsetX);
            modelOffsetY = (float) json.optDouble("modelOffsetY", modelOffsetY);
            modelOffsetZ = (float) json.optDouble("modelOffsetZ", modelOffsetZ);
            panelCollapsed = json.optBoolean("panelCollapsed", panelCollapsed);
            activeTab = enumValue(WorkspaceTab.class, json.optString("lastSelectedTab"), activeTab);
            String loadedModel = json.optString("activeModelPath", "");
            if (!loadedModel.isEmpty() && new File(loadedModel).isFile()) {
                modelPath = loadedModel;
                modelName = json.optString("activeModelName", new File(loadedModel).getName());
                loadModel();
            }
            String loadedIbl = json.optString("activeIblPath", "");
            if (!loadedIbl.isEmpty() && new File(loadedIbl).isFile()) loadIblFile(new File(loadedIbl), "config_load");
            applyQualityProfile();
            applyLightingValues();
            applyIblRotation();
            applySkyboxVisibility();
            applyModelTransform();
            applyCameraControls();
            syncWorkspaceUi();
            configStatus = reason + "_ok: " + file.getAbsolutePath();
        } catch (Throwable t) {
            configStatus = reason + "_failed: " + shortMessage(t);
        }
        updateHud();
    }

    private void writeText(File file, String text) throws Exception {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(file, false)) {
            out.write(text.getBytes("UTF-8"));
        }
    }

    private String readText(File file) throws Exception {
        ByteBuffer data = readFile(file);
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        return new String(bytes, "UTF-8");
    }

    private static <T extends Enum<T>> T enumValue(Class<T> cls, String raw, T fallback) {
        try {
            if (raw == null || raw.isEmpty()) return fallback;
            return Enum.valueOf(cls, raw);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private void updateHud() {
        if (hudView != null) {
            hudView.setText("Renderer: Filament | GLB loader: gltfio | FPS " + oneDecimal(rollingFps) + " | " + oneDecimal(rollingFrameMs)
                + " ms | CPU " + oneDecimal(rollingRenderCpuMs) + " ms | frame " + liveFrameCounter
                + " | " + qualityProfile.label + " | IBL " + iblFile + " | " + lightingPreset.label);
        }
        if (assetsSummaryView != null) {
            assetsSummaryView.setText("Active model: " + (modelName == null || modelName.isEmpty() ? "none" : modelName)
                + "\nActive IBL: " + iblFile
                + "\nLoad: " + loadStatus
                + "\nIBL: " + iblLoadStatus
                + "\nScan: " + scanDownloadStatus
                + "\nCopy/import: " + importCopyStatus);
        }
        if (qualitySummaryView != null) {
            qualitySummaryView.setText("actualAA=" + actualAA + " sampleCount=" + actualSampleCount
                + "\ndynamicResolutionEnabled=" + dynamicResolutionEnabled
                + "\nrenderScale=" + twoDecimal(renderScale) + " dynamicScale=" + twoDecimal(dynamicMinScale) + "-" + twoDecimal(dynamicMaxScale)
                + "\ntaaSupported=not_exposed taaEnabled=false fxaaEnabled=" + "FXAA".equals(actualAA)
                + "\nditheringEnabled=" + ditheringEnabled + " anisotropicFiltering=not_exposed textureLodBias=not_exposed"
                + "\naoMode=" + aoMode.name() + " aoApplied=" + aoActuallyApplied
                + "\nshadowsMode=" + shadowMode.name() + " shadowsApplied=" + shadowsActuallyApplied
                + "\nbloom=" + bloomActualStatus
                + "\nrefraction=" + refractionActualStatus);
        }
        if (iblSummaryView != null) {
            iblSummaryView.setText("activeIblName=" + iblFile
                + "\nactiveIblPath=" + shorten(futureIblAssetPath, 72)
                + "\nrealIblReady=" + realIblReady + " indirectLightReady=" + indirectLightReady + " skyboxReady=" + skyboxReady
                + "\nhdrLoaded=" + iblMode.startsWith("hdr") + " ktxLoaded=" + iblMode.startsWith("ktx") + " exrUnsupported=" + "unsupported_exr".equals(iblMode)
                + "\niblIntensityUser=" + twoDecimal(ambientUserIntensity) + " iblIntensityInternal=" + twoDecimal(ambientFallbackIntensity)
                + "\niblRotation=" + oneDecimal(iblRotation) + " skyboxVisible=" + skyboxVisible
                + "\nskyboxBlur=not_exposed backgroundBrightness=" + twoDecimal(backgroundBrightness)
                + "\nstatus=" + iblLoadStatus);
        }
        if (shadowSummaryView != null) {
            shadowSummaryView.setText("shadowsSupported=true"
                + "\nshadowsEnabled=" + shadowsEnabled + " shadowMode=" + shadowMode.name()
                + "\nshadowType=" + shadowTypeStatus
                + "\nshadowMapSize=not_exposed cascadeStatus=not_exposed cascadeCount=not_exposed"
                + "\ncontactShadowsSupported=not_exposed contactShadowsEnabled=false"
                + "\nshadowStrength=not_exposed shadowSoftness=not_exposed shadowBiasStatus=not_exposed"
                + "\ncastReceiveFlags=applied_to_renderables_if_loaded"
                + "\nstatus=" + shadowActualStatus);
        }
        if (cameraSummaryView != null) {
            cameraSummaryView.setText("cameraDistance=" + twoDecimal(cameraDistance)
                + "\ncameraTargetX/Y/Z=" + twoDecimal(cameraTargetX) + "/" + twoDecimal(cameraTargetY) + "/" + twoDecimal(cameraTargetZ)
                + "\ncameraPanX/Y=" + twoDecimal(cameraTargetX) + "/" + twoDecimal(cameraTargetY)
                + "\nfov=" + oneDecimal(cameraFov) + " nearFar=0.05/250"
                + "\norbitSensitivity=filament_manipulator_default zoomSensitivity=0.010"
                + "\nfitStatus=" + cameraApplyStatus);
        }
        if (modelSummaryView != null) {
            modelSummaryView.setText("modelRotationX/Y/Z=" + oneDecimal(modelRotationX) + "/" + oneDecimal(modelRotationY) + "/" + oneDecimal(modelRotationZ)
                + "\nmodelScale=" + twoDecimal(modelScale)
                + "\nmodelOffsetX/Y/Z=" + twoDecimal(modelOffsetX) + "/" + twoDecimal(modelOffsetY) + "/" + twoDecimal(modelOffsetZ)
                + "\ntransformApplied=" + modelTransformStatus
                + "\ntransformTarget=" + transformTargetStatus
                + "\nno_vertex_bake=true no_model_specific_hack=true");
        }
        if (glassSummaryView != null) {
            glassSummaryView.setText("Author Material Mode=default"
                + "\nGlass override=disabled_by_default"
                + "\nrefractionSupported=true_screen_space_view_api"
                + "\nrefractionEnabled=" + refractionEnabled
                + "\nrefractionActuallyApplied=" + refractionActuallyApplied
                + "\ntransparentObjectCount=not_exposed"
                + "\nglassMaterialCount=not_exposed"
                + "\nalpha/transmission status=limited_by_gltfio_java_api");
        }
        if (configSummaryView != null) {
            configSummaryView.setText("configPath=" + configFile().getAbsolutePath()
                + "\nSharedPreferences backup=" + PREFS_NAME
                + "\nconfigStatus=" + configStatus
                + "\nmissing paths on load=skip_no_crash"
                + "\nlastSelectedTab=" + activeTab.name() + " panelCollapsed=" + panelCollapsed);
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
                + "\nSun/Ambient/Fill/Exp/BG: " + oneDecimal(sunLightIntensity) + " / " + oneDecimal(ambientUserIntensity) + " / " + oneDecimal(fillLightIntensity) + " / " + twoDecimal(exposure) + " / " + twoDecimal(backgroundBrightness)
                + "\nambientUser=" + oneDecimal(ambientUserIntensity) + " ambientInternal=" + oneDecimal(ambientFallbackIntensity)
                + "\niblMode=" + iblMode + " iblFile=" + iblFile
                + "\niblLoadStatus=" + iblLoadStatus
                + "\nrealIblReady=" + realIblReady + " skyboxReady=" + skyboxReady + " indirectLightReady=" + indirectLightReady
                + "\nhdrLoaded=" + iblMode.startsWith("hdr") + " ktxLoaded=" + iblMode.startsWith("ktx") + " exrUnsupported=" + "unsupported_exr".equals(iblMode)
                + "\nfallbackReason=" + fallbackReason
                + "\nenvironmentMode=" + environmentMode + " activeIbl=" + shorten(futureIblAssetPath, 72)
                + "\nQuality: " + qualityFeatureStatus
                + "\nactualAA=" + actualAA + " sampleCount=" + actualSampleCount + " dynamicResolutionEnabled=" + dynamicResolutionEnabled + " dynamicResolution=" + twoDecimal(dynamicMinScale) + "-" + twoDecimal(dynamicMaxScale)
                + "\ntaaSupported=not_exposed taaEnabled=false fxaaSupported=true dithering=" + ditheringEnabled
                + "\naoMode=" + aoMode.name() + " aoEnabled=" + aoEnabled + " aoApplied=" + aoActuallyApplied + " aoType=" + aoTypeStatus
                + "\naoStatus=" + aoActualStatus + " aoLikelyInvisibleReason=" + aoLikelyInvisibleReason
                + "\nshadowsMode=" + shadowMode.name() + " shadowsEnabled=" + shadowsEnabled + " shadowsApplied=" + shadowsActuallyApplied
                + "\nshadowStatus=" + shadowActualStatus + " shadowType=" + shadowTypeStatus
                + "\nbloomMode=" + bloomMode.name() + " " + bloomActualStatus
                + "\nrefractionMode=" + refractionMode.name() + " refractionEnabled=" + refractionEnabled + " refractionApplied=" + refractionActuallyApplied
                + "\nmodelTransform=" + modelTransformStatus + " target=" + transformTargetStatus + " rx/ry/rz=" + oneDecimal(modelRotationX) + "/" + oneDecimal(modelRotationY) + "/" + oneDecimal(modelRotationZ) + " scale=" + twoDecimal(modelScale)
                + "\ncameraControls=" + cameraStatus + " apply=" + cameraApplyStatus
                + "\nLegacy Vulkan=deprecated/hidden normal_ui_route=false"
                + "\nconfigStatus=" + configStatus + " path=" + configFile().getAbsolutePath()
                + "\nmaterialInspectorStatus=" + materialInspectorStatus + " materialCount=" + materialCount
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
        setSliderLabel(ambientSliderLabel, realIblReady.equals("true") ? "IBL" : "Ambient", ambientUserIntensity);
        setSliderLabel(fillSliderLabel, "Fill", fillLightIntensity);
        setSliderLabel(exposureSliderLabel, "Exp", exposure);
        setSliderLabel(backgroundSliderLabel, "BG", backgroundBrightness);
        setSliderProgress(sunSlider, sunLightIntensity, 0.0f, 20.0f, 0.5f);
        setSliderProgress(ambientSlider, ambientUserIntensity, 0.0f, 10.0f, 0.1f);
        setSliderProgress(fillSlider, fillLightIntensity, 0.0f, 10.0f, 0.1f);
        setSliderProgress(exposureSlider, exposure, 0.30f, 2.50f, 0.01f);
        setSliderProgress(backgroundSlider, backgroundBrightness, 0.0f, 0.80f, 0.01f);
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
        if (aoButton != null) aoButton.setText(aoMode.label);
        if (bloomButton != null) bloomButton.setText(bloomMode.label);
        if (shadowsButton != null) shadowsButton.setText(shadowMode.label);
        if (refractionButton != null) refractionButton.setText(refractionMode.label);
    }

    private void setSliderLabel(TextView label, String name, float value) {
        if (label == null) return;
        if ("Exp".equals(name) || "BG".equals(name) || "Ambient".equals(name) || "IBL".equals(name) || "Fill".equals(name)) {
            label.setText(name + " " + twoDecimal(value));
        } else if ("Sun".equals(name)) {
            label.setText(name + " " + oneDecimal(value));
        } else {
            label.setText(name + " " + oneDecimal(value));
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
        setAdvancedFieldText(advancedAmbientField, oneDecimal(ambientUserIntensity));
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

    private Button button(String label, View.OnClickListener listener) {
        Button button = button(label);
        button.setOnClickListener(listener);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float[] backgroundColor() {
        float bg = clamp(backgroundBrightness, 0.0f, 1.0f);
        return new float[] {bg * 0.80f, bg * 0.90f, bg, 1.0f};
    }

    private float ambientToInternal(float userValue) {
        return clamp(userValue, 0.0f, 100.0f);
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

    private static float wrapDegrees(float value) {
        float out = value;
        while (out > 180.0f) out -= 360.0f;
        while (out < -180.0f) out += 360.0f;
        return out;
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

    private static String safeText(String value) {
        if (value == null || value.isEmpty()) return "none";
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
