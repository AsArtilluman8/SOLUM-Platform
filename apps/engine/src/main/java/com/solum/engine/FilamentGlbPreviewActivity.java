package com.solum.engine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Choreographer;
import android.view.FrameMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.SurfaceView;
import android.view.Window;
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
import com.google.android.filament.View.Dithering;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private static final String PREF_FILAMENT_COLOR_MODE = "filament_color_mode";
    private static final String PREF_FILAMENT_COLOR_EXPOSURE = "filament_color_exposure";
    private static final String PREF_FILAMENT_COLOR_CONTRAST = "filament_color_contrast";
    private static final String PREF_FILAMENT_COLOR_SATURATION = "filament_color_saturation";
    private static final String PREF_FILAMENT_COLOR_TEMPERATURE = "filament_color_temperature";
    private static final String PREF_FILAMENT_FOG_MODE = "filament_fog_mode";
    private static final String PREF_FILAMENT_LIGHT_RIG = "filament_light_rig";
    private static final String PREF_FILAMENT_BLOOM_STRENGTH = "filament_bloom_strength";
    private static final String PREF_FILAMENT_BLOOM_HIGHLIGHT = "filament_bloom_highlight";
    private static final String PREF_FILAMENT_RENDER_SCALE = "filament_render_scale";
    private static final String PREF_FILAMENT_DYNAMIC_RESOLUTION = "filament_dynamic_resolution";
    private static final String PREF_FILAMENT_MSAA_SAMPLES = "filament_msaa_samples";
    private static final String PREF_FILAMENT_FXAA_ENABLED = "filament_fxaa_enabled";
    private static final String PREF_FILAMENT_TAA_ENABLED = "filament_taa_enabled";
    private static final String PREF_FILAMENT_SSR_ENABLED = "filament_ssr_enabled";
    private static final String PREF_FILAMENT_DITHERING_ENABLED = "filament_dithering_enabled";
    private static final String PREF_FILAMENT_FOG_DENSITY = "filament_fog_density";
    private static final String PREF_FILAMENT_FOG_DISTANCE = "filament_fog_distance";
    private static final String PREF_FILAMENT_FOG_HEIGHT = "filament_fog_height";
    private static final String PREF_FILAMENT_SUN_GLARE_MODE = "filament_sun_glare_mode";
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
    private static final String PREF_FILAMENT_CONFIG_JSON = "filament_config_json";
    private static final String PREF_FILAMENT_DEFAULT_CONFIG_JSON = "filament_default_config_json";
    private static final String CONFIG_FILE_NAME = "filament_render_config.json";
    private static final int CONFIG_SCHEMA_VERSION = 5;
    private static final int REQUEST_IMPORT_MODEL = 4101;
    private static final int REQUEST_IMPORT_IBL = 4102;
    private static final long HUD_UPDATE_NS = 250_000_000L;
    private static final String GFXINFO_FRAMESTATS_COMMAND = "adb shell dumpsys gfxinfo com.solum.engine framestats";

    private SurfaceView surfaceView;
    private SunGlareOverlayView sunGlareOverlayView;
    private ModelViewer modelViewer;
    private IndirectLight indirectLight;
    private Skybox skybox;
    private ColorGrading colorGrading;
    private final List<Texture> iblOwnedTextures = new ArrayList<>();
    private int fillLightEntity = 0;
    private int pointLightEntity = 0;
    private int spotLightEntity = 0;
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
    private Button dynamicResolutionButton;
    private Button msaaButton;
    private Button fxaaButton;
    private Button ditheringButton;
    private Button taaButton;
    private Button ssrButton;
    private Button colorModeButton;
    private Button resetColorButton;
    private Button fogButton;
    private Button sunGlareButton;
    private Button lightRigButton;
    private Button skyboxButton;
    private Button collapseButton;
    private Button assetsTabButton;
    private Button lightingTabButton;
    private Button renderTabButton;
    private Button iblTabButton;
    private Button shadowsTabButton;
    private Button cameraTabButton;
    private Button modelTabButton;
    private Button colorTabButton;
    private Button fogTabButton;
    private Button lightsTabButton;
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
    private LinearLayout colorPanel;
    private LinearLayout fogPanel;
    private LinearLayout lightsPanel;
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
    private TextView colorSummaryView;
    private TextView fogSummaryView;
    private TextView lightsSummaryView;
    private TextView configSummaryView;
    private TextView debugSummaryView;
    private TextView lastActionStatusView;
    private TextView sunSliderLabel;
    private TextView ambientSliderLabel;
    private TextView fillSliderLabel;
    private TextView exposureSliderLabel;
    private TextView backgroundSliderLabel;
    private final List<SliderBinding> sliderBindings = new ArrayList<>();
    private HandlerThread frameMetricsThread;
    private Handler frameMetricsHandler;
    private Window.OnFrameMetricsAvailableListener frameMetricsListener;
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
    private ColorMode colorMode = ColorMode.NEUTRAL;
    private FogMode fogMode = FogMode.OFF;
    private SunGlareMode sunGlareMode = SunGlareMode.OFF;
    private LightRig lightRig = LightRig.OFF;
    private AoMode aoMode = AoMode.OFF;
    private BloomMode bloomMode = BloomMode.OFF;
    private ShadowMode shadowMode = ShadowMode.OFF;
    private RefractionMode refractionMode = RefractionMode.ON;
    private WorkspaceTab activeTab = WorkspaceTab.ASSETS;
    private boolean panelCollapsed = true;
    private boolean frameCallbackActive = false;
    private boolean destroying = false;
    private boolean destroyed = false;
    private long lastFrameNs = 0L;
    private long lastFrameWallNs = 0L;
    private long lastHudUpdateNs = 0L;
    private float rollingFrameMs = 0.0f;
    private float rollingFps = 0.0f;
    private float visibleSmoothFps = 0.0f;
    private float rollingRenderCpuMs = 0.0f;
    private long liveFrameCounter = 0L;
    private final float[] frameWindowMs = new float[180];
    private int frameWindowIndex = 0;
    private int frameWindowCount = 0;
    private float avgFrameMs = 0.0f;
    private float minFrameMs = 0.0f;
    private float maxFrameMs = 0.0f;
    private float p95FrameMs = 0.0f;
    private float worstFrameMs = 0.0f;
    private long jankFrameCounter = 0L;
    private long slowFrameCounter = 0L;
    private long ssrSlowFrameBaseline = 0L;
    private long ssrJankFrameBaseline = 0L;
    private String timingSourceStatus = "wall_clock_frame_interval_cpu_approx";
    private String frameMetricsStatus = "not_started";
    private long frameMetricsSampleCount = 0L;
    private float frameMetricsTotalMs = 0.0f;
    private float frameMetricsGpuMs = 0.0f;
    private float frameMetricsSwapMs = 0.0f;
    private float frameMetricsDrawMs = 0.0f;
    private long frameMetricsSlowCount = 0L;
    private long frameMetricsJankCount = 0L;
    private String frameBudgetStatus = "waiting_for_samples";
    private String smoothnessStatus = "waiting_for_samples";
    private String ssrPerformanceWarning = "off";
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
    private boolean fxaaEnabled = true;
    private boolean taaEnabled = false;
    private boolean ssrEnabled = false;
    private boolean ditheringEnabled = true;
    private boolean skyboxVisible = true;
    private float sunLightIntensity = 2.5f;
    private float ambientUserIntensity = 1.0f;
    private float ambientFallbackIntensity = 1.0f;
    private float fillLightIntensity = 0.0f;
    private float exposure = 1.0f;
    private float backgroundBrightness = 0.14f;
    private float renderScale = 0.95f;
    private float bloomStrength = 0.025f;
    private float bloomHighlight = 800.0f;
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
    private float colorExposure = 0.0f;
    private float colorContrast = 1.0f;
    private float colorSaturation = 1.0f;
    private float colorTemperature = 0.0f;
    private float fogDensity = 0.0f;
    private float fogDistance = 80.0f;
    private float fogHeight = 0.0f;
    private String configStatus = "not_loaded";
    private String lastActionStatus = "created";
    private String configPrivateSaved = "false";
    private String configExportSaved = "false";
    private String lastConfigError = "none";
    private String lastConfigLoadSource = "none";
    private String lastConfigSaveTimestamp = "none";
    private int loadedConfigVersion = 0;
    private String modelTransformStatus = "not_applied";
    private String transformTargetStatus = "none";
    private String cameraApplyStatus = "not_applied";
    private String colorGradingStatus = "not_applied";
    private String toneMapperStatus = "not_applied";
    private String fogStatus = "not_applied";
    private String sunGlareStatus = "off_mobile_safe_default";
    private String taaStatus = "off_default";
    private String ssrStatus = "off_default";
    private String guardBandStatus = "not_applied";
    private String ditheringStatus = "on_default";
    private String pickingStatus = "deferred_no_pick_yet";
    private String selectedRenderable = "none";
    private String selectedMaterialIndexStatus = "none";
    private float pickDepth = -1.0f;
    private String lightRigStatus = "off";

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
        COLOR("Color"),
        FOG("Fog"),
        LIGHTING("Lighting"),
        LIGHTS("Lights"),
        IBL("IBL"),
        SHADOWS("Shadows"),
        CAMERA("Camera"),
        MODEL("Model"),
        MATERIAL("Material"),
        CONFIG("Config"),
        DEBUG("Debug");

        final String label;

        WorkspaceTab(String label) {
            this.label = label;
        }
    }

    private enum ColorMode {
        NEUTRAL("Neutral", com.google.android.filament.ColorGrading.ToneMapping.ACES, 1.00f, 1.00f, 0.0f),
        PBR_NEUTRAL("PBR Neutral", com.google.android.filament.ColorGrading.ToneMapping.ACES_LEGACY, 1.00f, 1.00f, 0.0f),
        FILMIC("Filmic", com.google.android.filament.ColorGrading.ToneMapping.FILMIC, 1.03f, 1.02f, 0.0f),
        CINEMATIC("Cinematic", com.google.android.filament.ColorGrading.ToneMapping.FILMIC, 1.06f, 1.04f, 0.03f),
        PRODUCT("Product", com.google.android.filament.ColorGrading.ToneMapping.ACES, 1.04f, 1.03f, 0.0f),
        CHARACTER("Character", com.google.android.filament.ColorGrading.ToneMapping.ACES, 1.02f, 1.02f, 0.02f),
        NIGHT("Night", com.google.android.filament.ColorGrading.ToneMapping.ACES, 0.96f, 0.90f, -0.05f);

        final String label;
        final com.google.android.filament.ColorGrading.ToneMapping toneMapping;
        final float contrast;
        final float saturation;
        final float temperature;

        ColorMode(String label, com.google.android.filament.ColorGrading.ToneMapping toneMapping, float contrast, float saturation, float temperature) {
            this.label = label;
            this.toneMapping = toneMapping;
            this.contrast = contrast;
            this.saturation = saturation;
            this.temperature = temperature;
        }

        ColorMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum FogMode {
        OFF("Fog Off", 0.0f, 100.0f, 0.0f),
        SOFT_DEPTH("Soft Depth", 0.018f, 75.0f, 0.0f),
        FOREST_HAZE("Forest Haze", 0.030f, 55.0f, -0.2f),
        NIGHT_MIST("Night Mist", 0.038f, 42.0f, -0.4f),
        CINEMATIC_LOW("Cinematic Low", 0.020f, 65.0f, -0.8f);

        final String label;
        final float density;
        final float distance;
        final float height;

        FogMode(String label, float density, float distance, float height) {
            this.label = label;
            this.density = density;
            this.distance = distance;
            this.height = height;
        }

        FogMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum SunGlareMode {
        OFF("Sun Glare Off"),
        SUBTLE("Sun Glare Subtle"),
        MEDIUM("Sun Glare Medium");

        final String label;

        SunGlareMode(String label) {
            this.label = label;
        }

        SunGlareMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum LightRig {
        OFF("Rig Off"),
        STUDIO_KEY("Studio Key"),
        RIM_LIGHT("Rim Light"),
        PRODUCT_LIGHT("Product Light"),
        NIGHT_LAMP("Night Lamp"),
        MAGIC_PREVIEW("Magic Preview Light");

        final String label;

        LightRig(String label) {
            this.label = label;
        }

        LightRig next() {
            return values()[(ordinal() + 1) % values().length];
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
        SOFT("Bloom Low"),
        MEDIUM("Bloom Medium"),
        HIGH("Bloom High");

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
        SOFT("Shadows Soft Low"),
        MEDIUM("Shadows Medium"),
        SHARP("Shadows Sharp Inspect");

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
        ON("Refraction On");

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
        SAFE_STUDIO("Safe Studio", -145.0f, 45.0f, 2.5f, 1.0f, 0.0f, 1.0f, 0.14f, new float[] {0.70f, -0.35f, -0.62f}, RefractionMode.ON),
        BALANCED("Balanced", -130.0f, 48.0f, 3.0f, 1.0f, 0.2f, 1.0f, 0.16f, new float[] {0.66f, -0.38f, -0.65f}, RefractionMode.ON),
        DARK_INSPECT("Dark Inspect", -155.0f, 38.0f, 1.2f, 0.7f, 0.0f, 0.8f, 0.08f, new float[] {0.50f, -0.30f, -0.80f}, RefractionMode.ON),
        CHARACTER_PREVIEW("Character Preview", -135.0f, 48.0f, 2.8f, 1.0f, 0.35f, 1.0f, 0.16f, new float[] {0.68f, -0.35f, -0.64f}, RefractionMode.ON),
        PRODUCT_PREVIEW("Product Preview", -120.0f, 55.0f, 3.5f, 1.2f, 0.25f, 1.05f, 0.18f, new float[] {0.62f, -0.42f, -0.66f}, RefractionMode.ON),
        REFRACTION_PREVIEW("Refraction Preview", -145.0f, 50.0f, 2.0f, 1.3f, 0.15f, 1.0f, 0.12f, new float[] {0.65f, -0.30f, -0.70f}, RefractionMode.ON),
        CINEMATIC_FOREST("Cinematic Forest", -220.0f, 35.0f, 2.0f, 1.0f, 0.0f, 0.95f, 0.12f, new float[] {0.48f, -0.32f, -0.82f}, RefractionMode.ON),
        OUTDOOR_SOFT("Outdoor Soft", -105.0f, 55.0f, 4.0f, 1.2f, 0.15f, 1.05f, 0.20f, new float[] {0.62f, -0.42f, -0.66f}, RefractionMode.ON),
        NIGHT_INSPECT("Night Inspect", -175.0f, 24.0f, 0.8f, 0.45f, 0.2f, 0.75f, 0.04f, new float[] {0.50f, -0.30f, -0.80f}, RefractionMode.ON);

        final String label;
        final float sunAzimuth;
        final float sunElevation;
        final float sunIntensity;
        final float ambientFallbackIntensity;
        final float fillIntensity;
        final float exposure;
        final float backgroundBrightness;
        final float[] fillDirection;
        final RefractionMode refractionMode;

        LightingPreset(String label, float sunAzimuth, float sunElevation, float sunIntensity, float ambientFallbackIntensity, float fillIntensity, float exposure, float backgroundBrightness, float[] fillDirection, RefractionMode refractionMode) {
            this.label = label;
            this.sunAzimuth = sunAzimuth;
            this.sunElevation = sunElevation;
            this.sunIntensity = sunIntensity;
            this.ambientFallbackIntensity = ambientFallbackIntensity;
            this.fillIntensity = fillIntensity;
            this.exposure = exposure;
            this.backgroundBrightness = backgroundBrightness;
            this.fillDirection = fillDirection;
            this.refractionMode = refractionMode;
        }

        LightingPreset next() {
            return values()[(ordinal() + 1) % values().length];
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
        sunGlareOverlayView = new SunGlareOverlayView(this);
        sunGlareOverlayView.setVisibility(View.GONE);
        root.addView(sunGlareOverlayView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

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
            refreshUiNow();
        });

        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        assetsTabButton = tabButton("Assets", WorkspaceTab.ASSETS);
        renderTabButton = tabButton("Render", WorkspaceTab.RENDER);
        colorTabButton = tabButton("Color", WorkspaceTab.COLOR);
        fogTabButton = tabButton("Fog", WorkspaceTab.FOG);
        lightingTabButton = tabButton("Lighting", WorkspaceTab.LIGHTING);
        lightsTabButton = tabButton("Lights", WorkspaceTab.LIGHTS);
        iblTabButton = tabButton("IBL", WorkspaceTab.IBL);
        shadowsTabButton = tabButton("Shadows", WorkspaceTab.SHADOWS);
        cameraTabButton = tabButton("Camera", WorkspaceTab.CAMERA);
        modelTabButton = tabButton("Model", WorkspaceTab.MODEL);
        materialTabButton = tabButton("Material", WorkspaceTab.MATERIAL);
        configTabButton = tabButton("Config", WorkspaceTab.CONFIG);
        debugTabButton = tabButton("Debug", WorkspaceTab.DEBUG);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT);
        tabRow.addView(assetsTabButton, tabParams);
        tabRow.addView(renderTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(colorTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(fogTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(lightingTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(lightsTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(iblTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(shadowsTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(cameraTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(modelTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(materialTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(configTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        tabRow.addView(debugTabButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.addView(tabRow, new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT));

        workspacePanel.addView(collapseButton);
        lastActionStatusView = overlayText(10.0f, 1);
        lastActionStatusView.setBackgroundColor(Color.argb(130, 4, 24, 30));
        workspacePanel.addView(lastActionStatusView);
        workspacePanel.addView(tabScroll);

        assetsPanel = new LinearLayout(this);
        assetsPanel.setOrientation(LinearLayout.VERTICAL);
        renderPanel = new LinearLayout(this);
        renderPanel.setOrientation(LinearLayout.VERTICAL);
        colorPanel = new LinearLayout(this);
        colorPanel.setOrientation(LinearLayout.VERTICAL);
        fogPanel = new LinearLayout(this);
        fogPanel.setOrientation(LinearLayout.VERTICAL);
        lightingPanel = new LinearLayout(this);
        lightingPanel.setOrientation(LinearLayout.VERTICAL);
        lightsPanel = new LinearLayout(this);
        lightsPanel.setOrientation(LinearLayout.VERTICAL);
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
            refreshUiNow();
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
            ssrEnabled = false;
            persistWorkspaceSettings();
            applyQualityProfile();
            setLastAction("quality_" + qualityProfile.name().toLowerCase(Locale.US) + "_ssr_forced_off");
            refreshUiNow();
        });
        lightingButton = button("Lighting: " + lightingPreset.label);
        lightingButton.setOnClickListener(v -> {
            lightingPreset = lightingPreset.next();
            applyLightingPreset();
            refreshUiNow();
        });
        iblButton = button("IBL: Procedural fallback");
        iblButton.setOnClickListener(v -> cycleIblPreset());
        Button resetButton = button("Reset Safe Lighting");
        resetButton.setOnClickListener(v -> resetSafeLighting());
        advancedValuesButton = button("");
        advancedValuesButton.setOnClickListener(v -> {
            advancedValuesEnabled = !advancedValuesEnabled;
            updateAdvancedValuesVisibility();
            refreshUiNow();
        });
        aoButton = button("");
        aoButton.setOnClickListener(v -> {
            aoMode = aoMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            setLastAction("ao_" + aoMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        bloomButton = button("");
        bloomButton.setOnClickListener(v -> {
            bloomMode = bloomMode.next();
            bloomStrength = defaultBloomStrength(bloomMode);
            bloomHighlight = defaultBloomHighlight(bloomMode);
            persistWorkspaceSettings();
            applyQualityProfile();
            setLastAction("bloom_" + bloomMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        shadowsButton = button("");
        shadowsButton.setOnClickListener(v -> {
            shadowMode = shadowMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            applyLightingValues();
            setLastAction("shadows_" + shadowMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        refractionButton = button("");
        refractionButton.setOnClickListener(v -> {
            refractionMode = refractionMode.next();
            persistWorkspaceSettings();
            applyQualityProfile();
            setLastAction("refraction_" + refractionMode.name().toLowerCase(Locale.US));
            refreshUiNow();
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
        addLightingSlider(lightingPanel, "Sun Azimuth", 0.0f, 360.0f, 1.0f, normalizedAzimuth(sunAzimuth), v -> {
            sunAzimuth = v;
            applyLightingValues();
        });
        addLightingSlider(lightingPanel, "Sun Elevation", -20.0f, 90.0f, 1.0f, sunElevation, v -> {
            sunElevation = v;
            applyLightingValues();
        });
        LinearLayout dirRow1 = row();
        dirRow1.addView(button("Front", v -> setSunDirection("front", 180.0f, 35.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        dirRow1.addView(button("Front Left", v -> setSunDirection("front_left", 225.0f, 40.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        dirRow1.addView(button("Front Right", v -> setSunDirection("front_right", 135.0f, 40.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        lightingPanel.addView(dirRow1);
        LinearLayout dirRow2 = row();
        dirRow2.addView(button("Top Soft", v -> setSunDirection("top_soft", 180.0f, 82.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        dirRow2.addView(button("Side", v -> setSunDirection("side", 90.0f, 30.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        dirRow2.addView(button("Back Rim", v -> setSunDirection("back_rim", 0.0f, 28.0f)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        lightingPanel.addView(dirRow2);
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
        dynamicResolutionButton = button("", v -> {
            dynamicResolutionEnabled = !dynamicResolutionEnabled;
            applyQualityProfile();
            setLastAction("dynamic_resolution_" + (dynamicResolutionEnabled ? "on" : "off"));
            refreshUiNow();
        });
        renderPanel.addView(dynamicResolutionButton);
        msaaButton = button("", v -> {
            actualSampleCount = actualSampleCount == 1 ? 2 : (actualSampleCount == 2 ? 4 : 1);
            applyQualityProfile();
            setLastAction("msaa_" + actualSampleCount + "x");
            refreshUiNow();
        });
        renderPanel.addView(msaaButton);
        fxaaButton = button("", v -> {
            fxaaEnabled = !fxaaEnabled;
            applyQualityProfile();
            setLastAction("fxaa_" + (fxaaEnabled ? "on" : "off"));
            refreshUiNow();
        });
        renderPanel.addView(fxaaButton);
        ditheringButton = button("", v -> {
            ditheringEnabled = !ditheringEnabled;
            applyQualityProfile();
            setLastAction("dithering_" + (ditheringEnabled ? "on" : "off"));
            refreshUiNow();
        });
        renderPanel.addView(ditheringButton);
        taaButton = button("", v -> {
            taaEnabled = !taaEnabled;
            applyQualityProfile();
            setLastAction("taa_" + (taaEnabled ? "on" : "off"));
            refreshUiNow();
        });
        renderPanel.addView(taaButton);
        ssrButton = button("", v -> {
            ssrEnabled = !ssrEnabled;
            resetSsrPerformanceBaseline();
            applyQualityProfile();
            setLastAction("ssr_" + (ssrEnabled ? "on" : "off"));
            refreshUiNow();
        });
        renderPanel.addView(ssrButton);
        sunGlareButton = button("", v -> {
            sunGlareMode = sunGlareMode.next();
            applySunGlareOverlay();
            persistWorkspaceSettings();
            setLastAction("sun_glare_" + sunGlareMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        renderPanel.addView(sunGlareButton);
        addLightingSlider(renderPanel, "Render Scale", 0.50f, 1.00f, 0.01f, renderScale, v -> {
            renderScale = v;
            applyQualityProfile();
        });
        renderPanel.addView(aoButton);
        renderPanel.addView(bloomButton);
        addLightingSlider(renderPanel, "Bloom Strength", 0.0f, 0.25f, 0.005f, bloomStrength, v -> {
            bloomStrength = v;
            if (bloomStrength > 0.0f && bloomMode == BloomMode.OFF) bloomMode = BloomMode.SOFT;
            applyQualityProfile();
            refreshUiNow();
        });
        addLightingSlider(renderPanel, "Bloom Highlight", 100.0f, 1200.0f, 10.0f, bloomHighlight, v -> {
            bloomHighlight = v;
            if (bloomMode == BloomMode.OFF) bloomMode = BloomMode.SOFT;
            applyQualityProfile();
            refreshUiNow();
        });
        renderPanel.addView(refractionButton);
        renderPanel.addView(qualitySummaryView);

        colorSummaryView = overlayText(10.0f, 12);
        colorSummaryView.setBackgroundColor(Color.TRANSPARENT);
        colorModeButton = button("", v -> {
            colorMode = colorMode.next();
            applyColorModeDefaults();
            applyColorGrading();
            persistWorkspaceSettings();
            setLastAction("color_" + colorMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        colorPanel.addView(colorModeButton);
        resetColorButton = button("Reset Color Grading", v -> {
            colorMode = ColorMode.NEUTRAL;
            applyColorModeDefaults();
            applyColorGrading();
            persistWorkspaceSettings();
            setLastAction("color_reset_neutral");
            refreshUiNow();
        });
        colorPanel.addView(resetColorButton);
        addLightingSlider(colorPanel, "Color Exposure", -2.0f, 2.0f, 0.05f, colorExposure, v -> {
            colorExposure = v;
            applyColorGrading();
            persistWorkspaceSettings();
            refreshUiNow();
        });
        addLightingSlider(colorPanel, "Color Contrast", 0.50f, 1.50f, 0.01f, colorContrast, v -> {
            colorContrast = v;
            applyColorGrading();
            persistWorkspaceSettings();
            refreshUiNow();
        });
        addLightingSlider(colorPanel, "Color Saturation", 0.0f, 1.60f, 0.01f, colorSaturation, v -> {
            colorSaturation = v;
            applyColorGrading();
            persistWorkspaceSettings();
            refreshUiNow();
        });
        addLightingSlider(colorPanel, "Color Temperature", -0.30f, 0.30f, 0.01f, colorTemperature, v -> {
            colorTemperature = v;
            applyColorGrading();
            persistWorkspaceSettings();
            refreshUiNow();
        });
        colorPanel.addView(colorSummaryView);

        fogSummaryView = overlayText(10.0f, 12);
        fogSummaryView.setBackgroundColor(Color.TRANSPARENT);
        fogButton = button("", v -> {
            fogMode = fogMode.next();
            fogDensity = fogMode.density;
            fogDistance = fogMode.distance;
            fogHeight = fogMode.height;
            applyFogOptions();
            persistWorkspaceSettings();
            setLastAction("fog_" + fogMode.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        fogPanel.addView(fogButton);
        addLightingSlider(fogPanel, "Fog Density", 0.0f, 0.08f, 0.001f, fogDensity, v -> {
            fogDensity = v;
            applyFogOptions();
        });
        addLightingSlider(fogPanel, "Fog Distance", 10.0f, 160.0f, 1.0f, fogDistance, v -> {
            fogDistance = v;
            applyFogOptions();
        });
        addLightingSlider(fogPanel, "Fog Height", -5.0f, 5.0f, 0.1f, fogHeight, v -> {
            fogHeight = v;
            applyFogOptions();
        });
        fogPanel.addView(fogSummaryView);

        iblSummaryView = overlayText(10.0f, 12);
        iblSummaryView.setBackgroundColor(Color.TRANSPARENT);
        iblPanel.addView(iblButton);
        skyboxButton = button("", v -> {
            skyboxVisible = !skyboxVisible;
            applySkyboxVisibility();
            setLastAction("skybox_" + (skyboxVisible ? "on" : "off"));
            refreshUiNow();
        });
        iblPanel.addView(skyboxButton);
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

        lightsSummaryView = overlayText(10.0f, 12);
        lightsSummaryView.setBackgroundColor(Color.TRANSPARENT);
        lightRigButton = button("", v -> {
            lightRig = lightRig.next();
            applyLightRig();
            persistWorkspaceSettings();
            setLastAction("light_rig_" + lightRig.name().toLowerCase(Locale.US));
            refreshUiNow();
        });
        lightsPanel.addView(lightRigButton);
        lightsPanel.addView(lightsSummaryView);

        buildCameraPanel();
        buildModelPanel();

        materialSummaryView = overlayText(10.0f, 12);
        materialSummaryView.setBackgroundColor(Color.TRANSPARENT);
        materialPanel.addView(materialSummaryView);

        configSummaryView = overlayText(10.0f, 10);
        configSummaryView.setBackgroundColor(Color.TRANSPARENT);
        configPanel.addView(button("Save Config", v -> saveConfig(configFile(), "save_config")));
        configPanel.addView(button("Load Config", v -> loadConfig(configFile(), "load_config")));
        configPanel.addView(button("Save As Default", v -> saveConfig(configFile(), "save_as_default")));
        configPanel.addView(button("Reset Safe Defaults", v -> resetSafeDefaults()));
        configPanel.addView(button("Export Config", v -> saveConfig(configFile(), "export_config")));
        configPanel.addView(button("Import Config", v -> loadConfig(configFile(), "import_config")));
        configPanel.addView(configSummaryView);

        statusView = overlayText(10.0f, 220);
        debugSummaryView = statusView;
        Button closeButton = button("Close Preview");
        closeButton.setOnClickListener(v -> closePreview());
        Button resetFrameCountersButton = button("Reset FPS/Jank Counters");
        resetFrameCountersButton.setOnClickListener(v -> {
            resetFrameCounters();
            setLastAction("fps_jank_counters_reset");
            refreshUiNow();
        });
        debugPanel.addView(closeButton);
        debugPanel.addView(resetFrameCountersButton);
        debugPanel.addView(statusView);

        workspacePanel.addView(assetsPanel);
        workspacePanel.addView(renderPanel);
        workspacePanel.addView(colorPanel);
        workspacePanel.addView(fogPanel);
        workspacePanel.addView(lightingPanel);
        workspacePanel.addView(lightsPanel);
        workspacePanel.addView(iblPanel);
        workspacePanel.addView(shadowsPanel);
        workspacePanel.addView(cameraPanel);
        workspacePanel.addView(modelPanel);
        workspacePanel.addView(materialPanel);
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
        startFrameMetricsDiagnostics();
        syncWorkspaceUi();
        scanDownloadForAssets("startup");
        createViewer();
        restorePersistedIbl();
        loadModel();
        refreshUiNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!destroying && !destroyed) startFrames();
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
                setLastAction("model_picker_cancelled");
                refreshUiNow();
                return;
            }
            importModelFromUri(data.getData());
            return;
        }
        if (requestCode == REQUEST_IMPORT_IBL) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                importCopyStatus = "ibl_picker_cancelled";
                setLastAction("ibl_picker_cancelled");
                refreshUiNow();
                return;
            }
            importIblFromUri(data.getData());
        }
    }

    @Override
    protected void onDestroy() {
        stopFrames("destroyed");
        stopFrameMetricsDiagnostics();
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
                if (event.getAction() == MotionEvent.ACTION_UP) requestPick(event);
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
        legacyVulkanReturnStatus = "deprecated_hidden_close_preview_finishes_filament_only";
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
        lastFrameWallNs = 0L;
        lastHudUpdateNs = 0L;
        Choreographer.getInstance().postFrameCallback(frameCallback);
        refreshUiNow();
    }

    private void stopFrames(String status) {
        if (frameCallbackActive) {
            frameCallbackActive = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
        lifecycleStatus = status;
        refreshUiNow();
    }

    private void doFrame(long frameTimeNanos) {
        if (!frameCallbackActive || destroying || destroyed || modelViewer == null) return;
        try {
            long frameStartWallNs = System.nanoTime();
            updateFrameTiming(frameTimeNanos, frameStartWallNs);
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

    private void updateFrameTiming(long frameTimeNanos, long frameWallNs) {
        if (lastFrameWallNs > 0L) {
            float instantMs = (frameWallNs - lastFrameWallNs) / 1_000_000.0f;
            if (instantMs > 0.0f && instantMs < 250.0f) {
                rollingFrameMs = rollingFrameMs <= 0.0f ? instantMs : (rollingFrameMs * 0.70f + instantMs * 0.30f);
                rollingFps = 1000.0f / Math.max(1.0f, rollingFrameMs);
                recordFrameSample(instantMs);
            }
        }
        lastFrameNs = frameTimeNanos;
        lastFrameWallNs = frameWallNs;
        if (lastHudUpdateNs == 0L || frameTimeNanos - lastHudUpdateNs >= HUD_UPDATE_NS) {
            lastHudUpdateNs = frameTimeNanos;
            refreshUiNow();
        }
    }

    private void updateRenderCpuTiming(float cpuMs) {
        if (cpuMs >= 0.0f && cpuMs < 250.0f) {
            rollingRenderCpuMs = rollingRenderCpuMs <= 0.0f ? cpuMs : (rollingRenderCpuMs * 0.82f + cpuMs * 0.18f);
        }
    }

    private void startFrameMetricsDiagnostics() {
        try {
            frameMetricsThread = new HandlerThread("solum-frame-metrics");
            frameMetricsThread.start();
            frameMetricsHandler = new Handler(frameMetricsThread.getLooper());
            frameMetricsListener = (window, frameMetrics, dropCountSinceLastInvocation) -> {
                try {
                    recordAndroidFrameMetrics(frameMetrics);
                } catch (Throwable t) {
                    frameMetricsStatus = "listener_failed: " + shortMessage(t);
                }
            };
            getWindow().addOnFrameMetricsAvailableListener(frameMetricsListener, frameMetricsHandler);
            frameMetricsStatus = "android_window_frame_metrics_enabled";
        } catch (Throwable t) {
            frameMetricsStatus = "not_available_or_failed: " + shortMessage(t);
            stopFrameMetricsDiagnostics();
        }
    }

    private void stopFrameMetricsDiagnostics() {
        try {
            if (frameMetricsListener != null) getWindow().removeOnFrameMetricsAvailableListener(frameMetricsListener);
        } catch (Throwable ignored) { }
        frameMetricsListener = null;
        frameMetricsHandler = null;
        if (frameMetricsThread != null) {
            try { frameMetricsThread.quitSafely(); } catch (Throwable ignored) { }
            frameMetricsThread = null;
        }
    }

    private void recordAndroidFrameMetrics(FrameMetrics metrics) {
        if (metrics == null) return;
        float totalMs = nanosToMs(metrics.getMetric(FrameMetrics.TOTAL_DURATION));
        float gpuMs = nanosToMs(metrics.getMetric(FrameMetrics.GPU_DURATION));
        float swapMs = nanosToMs(metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION));
        float drawMs = nanosToMs(metrics.getMetric(FrameMetrics.DRAW_DURATION));
        if (totalMs > 0.0f && totalMs < 1000.0f) {
            frameMetricsTotalMs = smoothMetric(frameMetricsTotalMs, totalMs, 0.25f);
            if (totalMs > 22.2f) frameMetricsSlowCount++;
            if (totalMs > 33.3f) frameMetricsJankCount++;
        }
        if (gpuMs > 0.0f && gpuMs < 1000.0f) frameMetricsGpuMs = smoothMetric(frameMetricsGpuMs, gpuMs, 0.25f);
        if (swapMs > 0.0f && swapMs < 1000.0f) frameMetricsSwapMs = smoothMetric(frameMetricsSwapMs, swapMs, 0.25f);
        if (drawMs > 0.0f && drawMs < 1000.0f) frameMetricsDrawMs = smoothMetric(frameMetricsDrawMs, drawMs, 0.25f);
        frameMetricsSampleCount++;
        frameMetricsStatus = gpuMs > 0.0f ? "android_frame_metrics_gpu_duration_available" : "android_frame_metrics_gpu_duration_unavailable";
    }

    private static float nanosToMs(long nanos) {
        return nanos <= 0L ? 0.0f : nanos / 1_000_000.0f;
    }

    private static float smoothMetric(float previous, float value, float alpha) {
        return previous <= 0.0f ? value : previous * (1.0f - alpha) + value * alpha;
    }

    private void recordFrameSample(float frameMs) {
        frameWindowMs[frameWindowIndex] = frameMs;
        frameWindowIndex = (frameWindowIndex + 1) % frameWindowMs.length;
        if (frameWindowCount < frameWindowMs.length) frameWindowCount++;
        if (worstFrameMs <= 0.0f || frameMs > worstFrameMs) worstFrameMs = frameMs;
        if (frameMs > 33.3f) jankFrameCounter++;
        if (frameMs > 22.2f) slowFrameCounter++;
        computeFrameStats();
    }

    private void computeFrameStats() {
        if (frameWindowCount <= 0) {
            avgFrameMs = 0.0f;
            minFrameMs = 0.0f;
            maxFrameMs = 0.0f;
            p95FrameMs = 0.0f;
            frameBudgetStatus = "waiting_for_samples";
            smoothnessStatus = "waiting_for_samples";
            visibleSmoothFps = 0.0f;
            return;
        }
        float[] samples = Arrays.copyOf(frameWindowMs, frameWindowCount);
        float sum = 0.0f;
        minFrameMs = samples[0];
        maxFrameMs = samples[0];
        for (float sample : samples) {
            sum += sample;
            if (sample < minFrameMs) minFrameMs = sample;
            if (sample > maxFrameMs) maxFrameMs = sample;
        }
        avgFrameMs = sum / frameWindowCount;
        Arrays.sort(samples);
        int p95Index = Math.min(samples.length - 1, Math.max(0, (int) Math.ceil(samples.length * 0.95f) - 1));
        p95FrameMs = samples[p95Index];
        visibleSmoothFps = 1000.0f / Math.max(1.0f, p95FrameMs);
        if (p95FrameMs <= 16.6f) frameBudgetStatus = "within_60fps_budget_16.6ms";
        else if (p95FrameMs <= 22.2f) frameBudgetStatus = "within_45fps_budget_22.2ms";
        else if (p95FrameMs <= 33.3f) frameBudgetStatus = "within_30fps_budget_33.3ms";
        else frameBudgetStatus = "over_30fps_budget_jank_risk";
        updateSsrPerformanceWarning();
        smoothnessStatus = "target=" + presetTargetFps() + "fps javaCallback=" + oneDecimal(rollingFps) + "fps estimatedVisible=" + oneDecimal(visibleSmoothFps) + "fps p95=" + oneDecimal(p95FrameMs) + "ms";
    }

    private void updateSsrPerformanceWarning() {
        if (!ssrEnabled) {
            ssrPerformanceWarning = "off";
            ssrSlowFrameBaseline = slowFrameCounter;
            ssrJankFrameBaseline = jankFrameCounter;
            return;
        }
        long slowDelta = Math.max(0L, slowFrameCounter - ssrSlowFrameBaseline);
        long jankDelta = Math.max(0L, jankFrameCounter - ssrJankFrameBaseline);
        if (jankDelta > 0L || slowDelta >= 6L || visibleSmoothFps < 25.0f) {
            ssrPerformanceWarning = "WARNING_ssr_manual_heavy_slowDelta=" + slowDelta + "_jankDelta=" + jankDelta + "_visibleP95Fps=" + oneDecimal(visibleSmoothFps);
        } else {
            ssrPerformanceWarning = "enabled_monitoring_slowDelta=" + slowDelta + "_jankDelta=" + jankDelta;
        }
    }

    private void resetFrameCounters() {
        Arrays.fill(frameWindowMs, 0.0f);
        frameWindowIndex = 0;
        frameWindowCount = 0;
        rollingFrameMs = 0.0f;
        rollingFps = 0.0f;
        visibleSmoothFps = 0.0f;
        rollingRenderCpuMs = 0.0f;
        avgFrameMs = 0.0f;
        minFrameMs = 0.0f;
        maxFrameMs = 0.0f;
        p95FrameMs = 0.0f;
        worstFrameMs = 0.0f;
        jankFrameCounter = 0L;
        slowFrameCounter = 0L;
        ssrSlowFrameBaseline = 0L;
        ssrJankFrameBaseline = 0L;
        ssrPerformanceWarning = ssrEnabled ? "enabled_after_counter_reset" : "off";
        frameBudgetStatus = "reset_waiting_for_samples";
        smoothnessStatus = "reset_waiting_for_samples";
    }

    private int presetTargetFps() {
        if (qualityProfile == FilamentQualityProfile.LOW) return 60;
        if (qualityProfile == FilamentQualityProfile.MEDIUM) return 45;
        if (qualityProfile == FilamentQualityProfile.HIGH_PREVIEW) return 45;
        return 30;
    }

    private void requestPick(MotionEvent event) {
        if (modelViewer == null || event == null) return;
        try {
            int x = Math.max(0, Math.round(event.getX()));
            int y = Math.max(0, Math.round(event.getY()));
            modelViewer.getView().setTransparentPickingEnabled(true);
            modelViewer.getView().pick(x, y, this, result -> {
                selectedRenderable = result.renderable == 0 ? "none" : String.valueOf(result.renderable);
                pickDepth = result.depth;
                selectedMaterialIndexStatus = selectedRenderable.equals("none") ? "none" : "deferred_material_slot_mapping_limited_by_gltfio_java_api";
                pickingStatus = selectedRenderable.equals("none") ? "ok_no_renderable_at_tap" : "ok_renderable_selected";
                refreshUiNow();
            });
            pickingStatus = "pick_requested";
        } catch (Throwable t) {
            pickingStatus = "not_exposed_or_failed: " + shortMessage(t);
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
            refreshUiNow();
        });
        return button;
    }

    private void syncWorkspaceUi() {
        if (collapseButton != null) collapseButton.setText(panelCollapsed ? "Expand" : "Collapse");
        if (tabRow != null) tabRow.setVisibility(panelCollapsed ? View.GONE : View.VISIBLE);
        setPanelVisible(assetsPanel, activeTab == WorkspaceTab.ASSETS && !panelCollapsed);
        setPanelVisible(renderPanel, activeTab == WorkspaceTab.RENDER && !panelCollapsed);
        setPanelVisible(colorPanel, activeTab == WorkspaceTab.COLOR && !panelCollapsed);
        setPanelVisible(fogPanel, activeTab == WorkspaceTab.FOG && !panelCollapsed);
        setPanelVisible(lightingPanel, activeTab == WorkspaceTab.LIGHTING && !panelCollapsed);
        setPanelVisible(lightsPanel, activeTab == WorkspaceTab.LIGHTS && !panelCollapsed);
        setPanelVisible(iblPanel, activeTab == WorkspaceTab.IBL && !panelCollapsed);
        setPanelVisible(shadowsPanel, activeTab == WorkspaceTab.SHADOWS && !panelCollapsed);
        setPanelVisible(cameraPanel, activeTab == WorkspaceTab.CAMERA && !panelCollapsed);
        setPanelVisible(modelPanel, activeTab == WorkspaceTab.MODEL && !panelCollapsed);
        setPanelVisible(qualityPanel, false);
        setPanelVisible(materialPanel, activeTab == WorkspaceTab.MATERIAL && !panelCollapsed);
        setPanelVisible(configPanel, activeTab == WorkspaceTab.CONFIG && !panelCollapsed);
        setPanelVisible(debugPanel, activeTab == WorkspaceTab.DEBUG && !panelCollapsed);
        updateTabState(assetsTabButton, WorkspaceTab.ASSETS);
        updateTabState(renderTabButton, WorkspaceTab.RENDER);
        updateTabState(colorTabButton, WorkspaceTab.COLOR);
        updateTabState(fogTabButton, WorkspaceTab.FOG);
        updateTabState(lightingTabButton, WorkspaceTab.LIGHTING);
        updateTabState(lightsTabButton, WorkspaceTab.LIGHTS);
        updateTabState(iblTabButton, WorkspaceTab.IBL);
        updateTabState(shadowsTabButton, WorkspaceTab.SHADOWS);
        updateTabState(cameraTabButton, WorkspaceTab.CAMERA);
        updateTabState(modelTabButton, WorkspaceTab.MODEL);
        updateTabState(qualityTabButton, WorkspaceTab.RENDER);
        updateTabState(materialTabButton, WorkspaceTab.MATERIAL);
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
            setLastAction("imported_model_ok");
            modelPath = out.getAbsolutePath();
            modelName = out.getName();
            persistActiveModel(out);
            loadModel();
        } catch (Throwable t) {
            importCopyStatus = "model_import_failed: " + shortMessage(t);
            setLastAction("imported_model_failed");
            loadStatus = importCopyStatus;
            refreshUiNow();
        }
    }

    private void importIblFromUri(Uri uri) {
        try {
            persistReadPermission(uri);
            String sourceName = displayNameForUri(uri, "imported.hdr");
            if (!isIblName(sourceName)) throw new IllegalArgumentException("unsupported_ibl_extension_hdr_ktx_ktx1_exr_required");
            File out = copyUriToAssetFile(uri, sourceName, iblDir());
            importCopyStatus = "ibl_import_copied";
            setLastAction("imported_ibl_ok");
            loadIblFile(out, "picker_import");
        } catch (Throwable t) {
            String failedStatus = "ibl_import_failed: " + shortMessage(t);
            importCopyStatus = failedStatus;
            setLastAction("imported_ibl_failed");
            createEnvironmentFallback();
            iblLoadStatus = failedStatus;
            fallbackReason = "ibl_import_failed";
            refreshUiNow();
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
            refreshUiNow();
            return;
        }
        try {
            modelViewer.destroyModel();
            if (modelPath == null || modelPath.isEmpty()) {
                loadStatus = "no_active_glb_or_gltf";
                gltfioLoaded = "false";
                refreshUiNow();
                return;
            }
            File file = new File(modelPath);
            if (!file.isFile()) {
                loadStatus = "file_missing: " + modelPath;
                gltfioLoaded = "false";
                refreshUiNow();
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
                refreshUiNow();
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
        refreshUiNow();
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
        destroyAdditionalLights(engine);
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
        applyLightRig();
    }

    private void destroyAdditionalLights(Engine engine) {
        if (engine == null) return;
        int[] entities = new int[] {pointLightEntity, spotLightEntity};
        for (int entity : entities) {
            if (entity == 0) continue;
            try {
                if (modelViewer != null && modelViewer.getScene() != null) modelViewer.getScene().removeEntity(entity);
                engine.getLightManager().destroy(entity);
                EntityManager.get().destroy(entity);
            } catch (Throwable ignored) { }
        }
        pointLightEntity = 0;
        spotLightEntity = 0;
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
            refreshUiNow();
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
        refreshUiNow();
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
            sunAzimuth = lightingPreset.sunAzimuth;
            sunElevation = lightingPreset.sunElevation;
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
        setLastAction("lighting_preset_" + lightingPreset.name().toLowerCase(Locale.US));
    }

    private void resetSafeLighting() {
        lightingPreset = LightingPreset.SAFE_STUDIO;
        aoMode = AoMode.OFF;
        bloomMode = BloomMode.OFF;
        bloomStrength = defaultBloomStrength(bloomMode);
        bloomHighlight = defaultBloomHighlight(bloomMode);
        shadowMode = ShadowMode.OFF;
        refractionMode = RefractionMode.ON;
        lastInputError = "none";
        applyLightingPreset();
        applyQualityProfile();
        refreshUiNow();
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
            applyLightRig();
            modelViewer.getCamera().setExposure(exposure);
            applyRenderableShadowMode();
            Renderer.ClearOptions clear = modelViewer.getRenderer().getClearOptions();
            clear.clear = true;
            clear.discard = true;
            clear.clearColor = backgroundColor();
            modelViewer.getRenderer().setClearOptions(clear);
            applySunGlareOverlay();
            lightingStatus = advancedValuesEnabled ? "live_values_applied_advanced_ranges" : "live_values_applied_safe_slider_ranges";
            if (realIblReady.equals("true")) iblStatus = "ok_real_ibl_intensity_controlled_by_ambient_slider";
            persistWorkspaceSettings();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            lightingStatus = "apply_failed";
        }
        updateLightingControlLabels();
        refreshUiNow();
    }

    private float[] sunDirectionFromAngles() {
        double az = Math.toRadians(normalizedAzimuth(sunAzimuth));
        double el = Math.toRadians(sunElevation);
        float x = (float) (Math.cos(el) * Math.sin(az));
        float y = (float) -Math.sin(el);
        float z = (float) (Math.cos(el) * Math.cos(az));
        return new float[] {x, y, z};
    }

    private void setSunDirection(String label, float azimuth, float elevation) {
        sunAzimuth = normalizedAzimuth(azimuth);
        sunElevation = clamp(elevation, -20.0f, 90.0f);
        applyLightingValues();
        setLastAction("sun_direction_" + label);
    }

    private void applyQualityProfile() {
        if (modelViewer == null) return;
        try {
            com.google.android.filament.View view = modelViewer.getView();
            com.google.android.filament.View.AmbientOcclusionOptions ao = view.getAmbientOcclusionOptions();
            com.google.android.filament.View.BloomOptions bloom = view.getBloomOptions();
            com.google.android.filament.View.DynamicResolutionOptions dynamic = view.getDynamicResolutionOptions();
            com.google.android.filament.View.RenderQuality renderQuality = view.getRenderQuality();
            actualSampleCount = sanitizeMsaa(actualSampleCount);
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
                view.setAntiAliasing(fxaaEnabled ? AntiAliasing.FXAA : AntiAliasing.NONE);
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
                setActualQualityStatus(fxaaEnabled ? "FXAA" : "NONE", actualSampleCount, dynamic.minScale, dynamic.maxScale, qualityProfile.name().toLowerCase(Locale.US) + "_quality_only");
            } else {
                view.setAntiAliasing(fxaaEnabled ? AntiAliasing.FXAA : AntiAliasing.NONE);
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
                setActualQualityStatus(fxaaEnabled ? "FXAA" : "NONE", actualSampleCount, 0.72f, dynamic.maxScale, "medium_quality_only_dynamic_safe");
            }
            view.setAmbientOcclusionOptions(ao);
            view.setBloomOptions(bloom);
            view.setDynamicResolutionOptions(dynamic);
            view.setRenderQuality(renderQuality);
            view.setDithering(ditheringEnabled ? Dithering.TEMPORAL : Dithering.NONE);
            ditheringStatus = ditheringEnabled ? "TEMPORAL" : "NONE";
            applyTemporalAaOptions(view);
            applyScreenSpaceReflectionsOptions(view);
            applyGuardBandOptions(view);
            applySunGlareOverlay();
            applyFogOptions();
            applyColorGrading();
            applyLightingValues();
            persistWorkspaceSettings();
        } catch (Throwable t) {
            lastLifecycleError = shortMessage(t);
            qualityFeatureStatus = "apply_failed";
        }
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
        updateAllSliderLabels();
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
            bloom.strength = clamp(bloomStrength, 0.0f, 0.08f);
            bloom.highlight = clamp(bloomHighlight, 500.0f, 1200.0f);
        } else if (bloomMode == BloomMode.MEDIUM) {
            bloom.strength = clamp(bloomStrength, 0.0f, 0.14f);
            bloom.highlight = clamp(bloomHighlight, 250.0f, 1200.0f);
        } else {
            bloom.strength = clamp(bloomStrength, 0.0f, 0.25f);
            bloom.highlight = clamp(bloomHighlight, 100.0f, 1200.0f);
        }
        if (bloomMode != BloomMode.OFF) {
            bloomStrength = bloom.strength;
            bloomHighlight = bloom.highlight;
        }
        bloomActualStatus = "mode=" + bloomMode.name() + " enabled=" + bloom.enabled + " strength=" + threeDecimal(bloom.strength)
            + " highlight=" + oneDecimal(bloom.highlight) + " thresholdBoolean=" + bloom.threshold + " dirt=not_exposed softness=not_exposed";
    }

    private float defaultBloomStrength(BloomMode mode) {
        if (mode == BloomMode.SOFT) return 0.025f;
        if (mode == BloomMode.MEDIUM) return 0.05f;
        if (mode == BloomMode.HIGH) return 0.12f;
        return 0.0f;
    }

    private float defaultBloomHighlight(BloomMode mode) {
        if (mode == BloomMode.SOFT) return 800.0f;
        if (mode == BloomMode.MEDIUM) return 500.0f;
        if (mode == BloomMode.HIGH) return 250.0f;
        return 1000.0f;
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

    private void applyTemporalAaOptions(com.google.android.filament.View view) {
        try {
            com.google.android.filament.View.TemporalAntiAliasingOptions taa = view.getTemporalAntiAliasingOptions();
            taa.enabled = taaEnabled;
            taa.filterWidth = 1.0f;
            taa.feedback = qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW ? 0.10f : 0.08f;
            taa.sharpness = qualityProfile == FilamentQualityProfile.LOW ? 0.0f : 0.25f;
            taa.filterHistory = true;
            taa.filterInput = true;
            taa.useYCoCg = true;
            taa.hdr = true;
            taa.preventFlickering = true;
            taa.historyReprojection = true;
            view.setTemporalAntiAliasingOptions(taa);
            taaStatus = taaEnabled
                ? "enabled feedback=" + twoDecimal(taa.feedback) + " filterWidth=" + twoDecimal(taa.filterWidth)
                : "off_mobile_safe_default";
        } catch (Throwable t) {
            taaEnabled = false;
            taaStatus = "not_exposed_or_failed: " + shortMessage(t);
        }
    }

    private void applyScreenSpaceReflectionsOptions(com.google.android.filament.View view) {
        try {
            com.google.android.filament.View.ScreenSpaceReflectionsOptions ssr = view.getScreenSpaceReflectionsOptions();
            ssr.enabled = ssrEnabled;
            ssr.thickness = 0.08f;
            ssr.bias = 0.01f;
            ssr.maxDistance = qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW ? 4.0f : 2.0f;
            ssr.stride = qualityProfile == FilamentQualityProfile.ULTRA_PREVIEW ? 1.0f : 2.0f;
            view.setScreenSpaceReflectionsOptions(ssr);
            ssrStatus = ssrEnabled
                ? "enabled maxDistance=" + oneDecimal(ssr.maxDistance) + " stride=" + oneDecimal(ssr.stride)
                : "off_mobile_safe_default";
        } catch (Throwable t) {
            ssrEnabled = false;
            ssrStatus = "not_exposed_or_failed: " + shortMessage(t);
        }
    }

    private void applyGuardBandOptions(com.google.android.filament.View view) {
        try {
            com.google.android.filament.View.GuardBandOptions guardBand = view.getGuardBandOptions();
            guardBand.enabled = qualityProfile != FilamentQualityProfile.LOW;
            view.setGuardBandOptions(guardBand);
            guardBandStatus = guardBand.enabled ? "enabled" : "disabled_low_quality";
        } catch (Throwable t) {
            guardBandStatus = "not_exposed_or_failed: " + shortMessage(t);
        }
    }

    private void applySunGlareOverlay() {
        if (sunGlareOverlayView == null) {
            sunGlareStatus = "deferred_overlay_not_created";
            return;
        }
        boolean enabled = sunGlareMode != SunGlareMode.OFF && qualityProfile != FilamentQualityProfile.LOW && sunElevation > 2.0f && sunLightIntensity > 0.1f;
        if (!enabled) {
            sunGlareOverlayView.setVisibility(View.GONE);
            if (sunGlareMode == SunGlareMode.OFF) sunGlareStatus = "off_mobile_safe_default";
            else if (qualityProfile == FilamentQualityProfile.LOW) sunGlareStatus = "disabled_low_quality";
            else sunGlareStatus = "off_sun_below_horizon_or_zero_intensity";
            return;
        }
        float az = normalizedAzimuth(sunAzimuth);
        float x = 0.5f + (float) Math.sin(Math.toRadians(az)) * 0.34f;
        float y = 0.58f - clamp(sunElevation, 0.0f, 90.0f) / 90.0f * 0.46f;
        float alpha = sunGlareMode == SunGlareMode.SUBTLE ? 0.10f : 0.17f;
        float radius = sunGlareMode == SunGlareMode.SUBTLE ? 0.18f : 0.25f;
        sunGlareOverlayView.configure(clamp(x, 0.08f, 0.92f), clamp(y, 0.06f, 0.78f), radius, alpha);
        sunGlareOverlayView.setVisibility(View.VISIBLE);
        sunGlareStatus = "applied_overlay_" + sunGlareMode.name().toLowerCase(Locale.US) + "_screen_space_no_ssr";
    }

    private void applyColorGrading() {
        if (modelViewer == null) return;
        Engine engine = modelViewer.getEngine();
        if (colorGrading != null) {
            modelViewer.getView().setColorGrading(null);
            engine.destroyColorGrading(colorGrading);
            colorGrading = null;
        }
        colorContrast = colorMode.contrast;
        colorSaturation = colorMode.saturation;
        colorTemperature = colorMode.temperature;
        float tint = 0.0f;
        colorGrading = new ColorGrading.Builder()
            .quality(qualityProfile == FilamentQualityProfile.HIGH_PREVIEW ? ColorGrading.QualityLevel.MEDIUM : ColorGrading.QualityLevel.LOW)
            .toneMapping(colorMode.toneMapping)
            .exposure(colorExposure)
            .whiteBalance(colorTemperature, tint)
            .contrast(colorContrast)
            .saturation(colorSaturation)
            .build(engine);
        modelViewer.getView().setColorGrading(colorGrading);
        toneMapperStatus = colorMode.toneMapping.name();
        colorGradingStatus = "applied_" + colorMode.name().toLowerCase(Locale.US)
            + "_manual_exposure_contrast_saturation_temperature";
        if (colorModeButton != null) colorModeButton.setText("Color Mode: " + colorMode.label);
    }

    private void applyColorModeDefaults() {
        colorExposure = 0.0f;
        colorContrast = colorMode.contrast;
        colorSaturation = colorMode.saturation;
        colorTemperature = colorMode.temperature;
    }

    private void applyFogOptions() {
        if (modelViewer == null) return;
        try {
            com.google.android.filament.View view = modelViewer.getView();
            com.google.android.filament.View.FogOptions fog = view.getFogOptions();
            boolean enabled = fogMode != FogMode.OFF && fogDensity > 0.0f;
            fog.enabled = enabled;
            fog.density = enabled ? clamp(fogDensity, 0.0f, 0.08f) : 0.0f;
            fog.distance = clamp(fogDistance, 10.0f, 160.0f);
            fog.cutOffDistance = Math.max(fog.distance + 80.0f, 120.0f);
            fog.maximumOpacity = enabled ? 0.72f : 0.0f;
            fog.height = fogHeight;
            fog.heightFalloff = 0.18f;
            fog.color = colorMode == ColorMode.NIGHT
                ? new float[] {0.08f, 0.10f, 0.16f}
                : new float[] {0.62f, 0.68f, 0.72f};
            fog.fogColorFromIbl = realIblReady.equals("true");
            view.setFogOptions(fog);
            fogStatus = enabled
                ? "enabled density=" + threeDecimal(fog.density) + " distance=" + oneDecimal(fog.distance) + " height=" + oneDecimal(fog.height)
                : "off_mobile_safe_default";
            if (fogButton != null) fogButton.setText(fogMode.label);
        } catch (Throwable t) {
            fogStatus = "not_exposed_or_failed: " + shortMessage(t);
        }
    }

    private void applyLightRig() {
        if (modelViewer == null || modelViewer.getScene() == null) return;
        try {
            Engine engine = modelViewer.getEngine();
            destroyAdditionalLights(engine);
            if (lightRig == LightRig.OFF) {
                lightRigStatus = "off_mobile_safe_default";
                if (lightRigButton != null) lightRigButton.setText(lightRig.label);
                return;
            }
            float pointIntensity = 0.0f;
            float spotIntensity = 0.0f;
            float[] pointColor = new float[] {1.0f, 0.92f, 0.82f};
            float[] spotColor = new float[] {0.72f, 0.82f, 1.0f};
            if (lightRig == LightRig.STUDIO_KEY) { pointIntensity = 2000.0f; spotIntensity = 0.0f; }
            else if (lightRig == LightRig.RIM_LIGHT) { pointIntensity = 0.0f; spotIntensity = 1600.0f; }
            else if (lightRig == LightRig.PRODUCT_LIGHT) { pointIntensity = 2600.0f; spotIntensity = 1200.0f; }
            else if (lightRig == LightRig.NIGHT_LAMP) { pointIntensity = 850.0f; spotIntensity = 0.0f; pointColor = new float[] {1.0f, 0.70f, 0.42f}; }
            else if (lightRig == LightRig.MAGIC_PREVIEW) { pointIntensity = 1200.0f; spotIntensity = 1400.0f; pointColor = new float[] {0.62f, 0.72f, 1.0f}; spotColor = new float[] {0.86f, 0.55f, 1.0f}; }
            if (pointIntensity > 0.0f) {
                pointLightEntity = EntityManager.get().create();
                new LightManager.Builder(LightManager.Type.POINT)
                    .position(1.8f, 1.4f, 2.2f)
                    .color(pointColor[0], pointColor[1], pointColor[2])
                    .intensity(pointIntensity)
                    .falloff(4.0f)
                    .castShadows(false)
                    .build(engine, pointLightEntity);
                modelViewer.getScene().addEntity(pointLightEntity);
            }
            if (spotIntensity > 0.0f) {
                spotLightEntity = EntityManager.get().create();
                new LightManager.Builder(LightManager.Type.SPOT)
                    .position(-1.6f, 1.6f, 2.4f)
                    .direction(0.45f, -0.35f, -0.82f)
                    .color(spotColor[0], spotColor[1], spotColor[2])
                    .intensity(spotIntensity)
                    .falloff(5.0f)
                    .spotLightCone(0.35f, 0.75f)
                    .castShadows(false)
                    .build(engine, spotLightEntity);
                modelViewer.getScene().addEntity(spotLightEntity);
            }
            lightRigStatus = "active=" + lightRig.name() + " point=" + (pointLightEntity != 0) + " spot=" + (spotLightEntity != 0) + " shadows=false";
            if (lightRigButton != null) lightRigButton.setText("Light Rig: " + lightRig.label);
        } catch (Throwable t) {
            lightRigStatus = "failed: " + shortMessage(t);
        }
    }

    private void releaseFilamentResources() {
        if (destroyed || destroying) return;
        destroying = true;
        lifecycleStatus = "destroyed";
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
                destroyAdditionalLights(engine);
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
        refractionMode = enumPref(prefs, PREF_FILAMENT_REFRACTION_MODE, RefractionMode.ON);
        colorMode = enumPref(prefs, PREF_FILAMENT_COLOR_MODE, ColorMode.NEUTRAL);
        fogMode = enumPref(prefs, PREF_FILAMENT_FOG_MODE, FogMode.OFF);
        sunGlareMode = enumPref(prefs, PREF_FILAMENT_SUN_GLARE_MODE, SunGlareMode.OFF);
        lightRig = enumPref(prefs, PREF_FILAMENT_LIGHT_RIG, LightRig.OFF);
        panelCollapsed = prefs.getBoolean(PREF_FILAMENT_PANEL_COLLAPSED, true);
        sunLightIntensity = prefs.getFloat(PREF_FILAMENT_SUN, lightingPreset.sunIntensity);
        ambientUserIntensity = prefs.getFloat(PREF_FILAMENT_AMBIENT, lightingPreset.ambientFallbackIntensity);
        fillLightIntensity = prefs.getFloat(PREF_FILAMENT_FILL, lightingPreset.fillIntensity);
        exposure = prefs.getFloat(PREF_FILAMENT_EXPOSURE, lightingPreset.exposure);
        backgroundBrightness = prefs.getFloat(PREF_FILAMENT_BG, lightingPreset.backgroundBrightness);
        renderScale = prefs.getFloat(PREF_FILAMENT_RENDER_SCALE, 0.95f);
        bloomStrength = prefs.getFloat(PREF_FILAMENT_BLOOM_STRENGTH, defaultBloomStrength(bloomMode));
        bloomHighlight = prefs.getFloat(PREF_FILAMENT_BLOOM_HIGHLIGHT, defaultBloomHighlight(bloomMode));
        dynamicResolutionEnabled = prefs.getBoolean(PREF_FILAMENT_DYNAMIC_RESOLUTION, true);
        actualSampleCount = prefs.getInt(PREF_FILAMENT_MSAA_SAMPLES, 2);
        fxaaEnabled = prefs.getBoolean(PREF_FILAMENT_FXAA_ENABLED, true);
        taaEnabled = prefs.getBoolean(PREF_FILAMENT_TAA_ENABLED, false);
        ssrEnabled = prefs.getBoolean(PREF_FILAMENT_SSR_ENABLED, false);
        ditheringEnabled = prefs.getBoolean(PREF_FILAMENT_DITHERING_ENABLED, true);
        fogDensity = prefs.getFloat(PREF_FILAMENT_FOG_DENSITY, fogMode.density);
        fogDistance = prefs.getFloat(PREF_FILAMENT_FOG_DISTANCE, fogMode.distance);
        fogHeight = prefs.getFloat(PREF_FILAMENT_FOG_HEIGHT, fogMode.height);
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
        colorExposure = prefs.getFloat(PREF_FILAMENT_COLOR_EXPOSURE, 0.0f);
        colorContrast = prefs.getFloat(PREF_FILAMENT_COLOR_CONTRAST, colorMode.contrast);
        colorSaturation = prefs.getFloat(PREF_FILAMENT_COLOR_SATURATION, colorMode.saturation);
        colorTemperature = prefs.getFloat(PREF_FILAMENT_COLOR_TEMPERATURE, colorMode.temperature);
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
            .putString(PREF_FILAMENT_COLOR_MODE, colorMode.name())
            .putString(PREF_FILAMENT_FOG_MODE, fogMode.name())
            .putString(PREF_FILAMENT_SUN_GLARE_MODE, sunGlareMode.name())
            .putString(PREF_FILAMENT_LIGHT_RIG, lightRig.name())
            .putBoolean(PREF_FILAMENT_PANEL_COLLAPSED, panelCollapsed)
            .putFloat(PREF_FILAMENT_SUN, sunLightIntensity)
            .putFloat(PREF_FILAMENT_AMBIENT, ambientUserIntensity)
            .putFloat(PREF_FILAMENT_FILL, fillLightIntensity)
            .putFloat(PREF_FILAMENT_EXPOSURE, exposure)
            .putFloat(PREF_FILAMENT_BG, backgroundBrightness)
            .putFloat(PREF_FILAMENT_RENDER_SCALE, renderScale)
            .putFloat(PREF_FILAMENT_BLOOM_STRENGTH, bloomStrength)
            .putFloat(PREF_FILAMENT_BLOOM_HIGHLIGHT, bloomHighlight)
            .putBoolean(PREF_FILAMENT_DYNAMIC_RESOLUTION, dynamicResolutionEnabled)
            .putInt(PREF_FILAMENT_MSAA_SAMPLES, actualSampleCount)
            .putBoolean(PREF_FILAMENT_FXAA_ENABLED, fxaaEnabled)
            .putBoolean(PREF_FILAMENT_TAA_ENABLED, taaEnabled)
            .putBoolean(PREF_FILAMENT_SSR_ENABLED, ssrEnabled)
            .putBoolean(PREF_FILAMENT_DITHERING_ENABLED, ditheringEnabled)
            .putFloat(PREF_FILAMENT_FOG_DENSITY, fogDensity)
            .putFloat(PREF_FILAMENT_FOG_DISTANCE, fogDistance)
            .putFloat(PREF_FILAMENT_FOG_HEIGHT, fogHeight)
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
            .putFloat(PREF_FILAMENT_COLOR_EXPOSURE, colorExposure)
            .putFloat(PREF_FILAMENT_COLOR_CONTRAST, colorContrast)
            .putFloat(PREF_FILAMENT_COLOR_SATURATION, colorSaturation)
            .putFloat(PREF_FILAMENT_COLOR_TEMPERATURE, colorTemperature)
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
            refreshUiNow();
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
        updateAllSliderLabels();
        refreshUiNow();
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
        updateAllSliderLabels();
        refreshUiNow();
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
        refreshUiNow();
    }

    private void resetSafeDefaults() {
        qualityProfile = FilamentQualityProfile.MEDIUM;
        lightingPreset = LightingPreset.SAFE_STUDIO;
        aoMode = AoMode.OFF;
        bloomMode = BloomMode.OFF;
        shadowMode = ShadowMode.OFF;
        refractionMode = RefractionMode.ON;
        colorMode = ColorMode.NEUTRAL;
        applyColorModeDefaults();
        fogMode = FogMode.OFF;
        lightRig = LightRig.OFF;
        sunGlareMode = SunGlareMode.OFF;
        dynamicResolutionEnabled = true;
        renderScale = 0.95f;
        actualSampleCount = 2;
        fxaaEnabled = true;
        taaEnabled = false;
        ssrEnabled = false;
        ditheringEnabled = true;
        fogDensity = 0.0f;
        fogDistance = 100.0f;
        fogHeight = 0.0f;
        skyboxVisible = true;
        iblRotation = 0.0f;
        sunLightIntensity = 2.5f;
        sunAzimuth = -145.0f;
        sunElevation = 45.0f;
        ambientUserIntensity = 1.0f;
        fillLightIntensity = 0.0f;
        exposure = 1.0f;
        backgroundBrightness = 0.14f;
        resetModelTransform();
        resetCameraControls();
        applyQualityProfile();
        applyLightingValues();
        applyFogOptions();
        applyLightRig();
        configStatus = "safe_defaults_reset";
        setLastAction("safe_defaults_reset");
        refreshUiNow();
    }

    private File configFile() {
        File dir = new File("/storage/emulated/0/Download/SOLUM/config");
        dir.mkdirs();
        return new File(dir, CONFIG_FILE_NAME);
    }

    private void saveConfig(File file, String reason) {
        JSONObject json = buildConfigJson();
        String text = json.toString();
        boolean privateOk = false;
        boolean exportOk = false;
        String error = "none";
        try {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_FILAMENT_CONFIG_JSON, text);
            if ("save_as_default".equals(reason)) editor.putString(PREF_FILAMENT_DEFAULT_CONFIG_JSON, text);
            privateOk = editor.commit();
        } catch (Throwable t) {
            error = shortMessage(t);
        }
        try {
            writeText(file, text);
            exportOk = true;
        } catch (Throwable t) {
            if ("none".equals(error)) error = shortMessage(t);
        }
        persistWorkspaceSettings();
        configPrivateSaved = String.valueOf(privateOk);
        configExportSaved = String.valueOf(exportOk);
        lastConfigError = error;
        lastConfigSaveTimestamp = nowTimestamp();
        if (privateOk && exportOk) configStatus = reason + "_ok";
        else if (privateOk) configStatus = reason + "_private_ok_export_failed";
        else configStatus = reason + "_failed";
        setLastAction(configStatus);
        refreshUiNow();
    }

    private void loadConfig(File file, String reason) {
        String text = null;
        String source = "none";
        try {
            if (file != null && file.isFile()) {
                text = readText(file);
                source = "download_json";
            } else {
                text = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_FILAMENT_CONFIG_JSON, "");
                source = "shared_preferences";
            }
            if (text == null || text.trim().isEmpty()) throw new IllegalStateException("config_missing");
            JSONObject json;
            try {
                json = new JSONObject(text);
            } catch (Throwable parseDownload) {
                if (!"download_json".equals(source)) throw parseDownload;
                text = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_FILAMENT_CONFIG_JSON, "");
                source = "shared_preferences_after_download_invalid";
                if (text == null || text.trim().isEmpty()) throw parseDownload;
                json = new JSONObject(text);
            }
            applyConfigJson(json);
            lastConfigLoadSource = source;
            lastConfigError = "none";
            configStatus = reason + "_ok";
            setLastAction(configStatus);
        } catch (Throwable t) {
            lastConfigLoadSource = source;
            lastConfigError = shortMessage(t);
            configStatus = reason + "_failed";
            setLastAction(configStatus);
        }
        refreshUiNow();
    }

    private JSONObject buildConfigJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("schema", "solum_filament_render_config");
            json.put("schemaVersion", CONFIG_SCHEMA_VERSION);
            json.put("qualityProfile", qualityProfile.name());
            json.put("renderScale", renderScale);
            json.put("dynamicResolutionEnabled", dynamicResolutionEnabled);
            json.put("dynamicMinScale", dynamicMinScale);
            json.put("dynamicMaxScale", dynamicMaxScale);
            json.put("aaMode", actualAA);
            json.put("msaaSamples", actualSampleCount);
            json.put("taaSupported", true);
            json.put("taaEnabled", taaEnabled);
            json.put("taaStatus", taaStatus);
            json.put("ssrSupported", true);
            json.put("ssrEnabled", ssrEnabled);
            json.put("ssrStatus", ssrStatus);
            json.put("guardBandStatus", guardBandStatus);
            json.put("fxaaEnabled", fxaaEnabled);
            json.put("ditheringEnabled", ditheringEnabled);
            json.put("ditheringStatus", ditheringStatus);
            json.put("colorMode", colorMode.name());
            json.put("toneMapper", toneMapperStatus);
            json.put("colorExposure", colorExposure);
            json.put("colorContrast", colorContrast);
            json.put("colorSaturation", colorSaturation);
            json.put("colorTemperature", colorTemperature);
            json.put("fogMode", fogMode.name());
            json.put("fogDensity", fogDensity);
            json.put("fogDistance", fogDistance);
            json.put("fogHeight", fogHeight);
            json.put("fogStatus", fogStatus);
            json.put("sunGlareMode", sunGlareMode.name());
            json.put("sunGlareStatus", sunGlareStatus);
            json.put("lightRig", lightRig.name());
            json.put("lightRigStatus", lightRigStatus);
            json.put("lightingPreset", lightingPreset.name());
            json.put("sunIntensity", sunLightIntensity);
            json.put("sunAzimuth", normalizedAzimuth(sunAzimuth));
            json.put("sunElevation", sunElevation);
            json.put("iblStrength", ambientUserIntensity);
            json.put("iblRotation", iblRotation);
            json.put("fillIntensity", fillLightIntensity);
            json.put("exposure", exposure);
            json.put("background", backgroundBrightness);
            json.put("skyboxVisible", skyboxVisible);
            json.put("aoMode", aoMode.name());
            json.put("aoStatus", aoActualStatus);
            json.put("bloomMode", bloomMode.name());
            json.put("bloomStrength", bloomStrength);
            json.put("bloomHighlight", bloomHighlight);
            json.put("bloomStatus", bloomActualStatus);
            json.put("shadowMode", shadowMode.name());
            json.put("shadowStatus", shadowActualStatus);
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
        } catch (Throwable ignored) { }
        return json;
    }

    private void applyConfigJson(JSONObject json) {
        loadedConfigVersion = json.optInt("schemaVersion", 0);
        qualityProfile = enumValue(FilamentQualityProfile.class, json.optString("qualityProfile"), FilamentQualityProfile.MEDIUM);
        lightingPreset = enumValue(LightingPreset.class, json.optString("lightingPreset"), LightingPreset.SAFE_STUDIO);
        aoMode = enumValue(AoMode.class, json.optString("aoMode"), AoMode.OFF);
        bloomMode = enumValue(BloomMode.class, json.optString("bloomMode"), BloomMode.OFF);
        shadowMode = enumValue(ShadowMode.class, json.optString("shadowMode"), ShadowMode.OFF);
        refractionMode = enumValue(RefractionMode.class, json.optString("refractionMode"), RefractionMode.ON);
        colorMode = enumValue(ColorMode.class, json.optString("colorMode"), ColorMode.NEUTRAL);
        fogMode = enumValue(FogMode.class, json.optString("fogMode"), FogMode.OFF);
        sunGlareMode = enumValue(SunGlareMode.class, json.optString("sunGlareMode"), SunGlareMode.OFF);
        lightRig = enumValue(LightRig.class, json.optString("lightRig"), LightRig.OFF);
        renderScale = (float) json.optDouble("renderScale", renderScale);
        bloomStrength = (float) json.optDouble("bloomStrength", bloomStrength);
        bloomHighlight = (float) json.optDouble("bloomHighlight", bloomHighlight);
        dynamicResolutionEnabled = json.optBoolean("dynamicResolutionEnabled", dynamicResolutionEnabled);
        actualSampleCount = sanitizeMsaa(json.optInt("msaaSamples", actualSampleCount));
        fxaaEnabled = json.optBoolean("fxaaEnabled", fxaaEnabled);
        taaEnabled = json.optBoolean("taaEnabled", taaEnabled);
        ssrEnabled = json.optBoolean("ssrEnabled", ssrEnabled);
        ditheringEnabled = json.optBoolean("ditheringEnabled", ditheringEnabled);
        fogDensity = (float) json.optDouble("fogDensity", fogDensity);
        fogDistance = (float) json.optDouble("fogDistance", fogDistance);
        fogHeight = (float) json.optDouble("fogHeight", fogHeight);
        colorExposure = (float) json.optDouble("colorExposure", colorExposure);
        colorContrast = (float) json.optDouble("colorContrast", colorContrast);
        colorSaturation = (float) json.optDouble("colorSaturation", colorSaturation);
        colorTemperature = (float) json.optDouble("colorTemperature", colorTemperature);
        sunLightIntensity = (float) json.optDouble("sunIntensity", sunLightIntensity);
        sunAzimuth = normalizedAzimuth((float) json.optDouble("sunAzimuth", sunAzimuth));
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
        applyFogOptions();
        applyLightRig();
        applyIblRotation();
        applySkyboxVisibility();
        applyModelTransform();
        applyCameraControls();
        persistWorkspaceSettings();
        syncWorkspaceUi();
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
            hudView.setText("Renderer: Filament | GLB: gltfio | FPS " + estimatedVisibleFpsHud()
                + " | frame " + oneDecimal(estimatedVisibleFrameMs()) + " ms"
                + " | p95 " + oneDecimal(p95FrameMs) + " ms"
                + " | " + renderHealthLabel()
                + " | " + qualityProfile.label
                + mainHudExpensiveFeatureLabel());
        }
        if (lastActionStatusView != null) lastActionStatusView.setText("status: " + lastActionStatus);
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
                + "\ntaaSupported=true taaEnabled=" + taaEnabled + " taaStatus=" + taaStatus
                + "\nssrSupported=true ssrEnabled=" + ssrEnabled + " ssrStatus=" + ssrStatus + (ssrEnabled ? " WARNING_manual_heavy_mobile" : "")
                + "\nssrPerformanceWarning=" + ssrPerformanceWarning
                + "\ngpuTimingStatus=" + gpuTimingStatus()
                + "\nandroidFrameMetrics=" + frameMetricsStatus + " samples=" + frameMetricsSampleCount + " totalMs=" + oneDecimal(frameMetricsTotalMs) + " gpuMs=" + gpuMetricLabel()
                + "\nfxaaSupported=true fxaaToggle=" + fxaaEnabled + " fxaaActuallyActive=" + "FXAA".equals(actualAA)
                + "\nditheringStatus=" + ditheringStatus + " guardBandStatus=" + guardBandStatus
                + "\nsunGlareMode=" + sunGlareMode.name() + " sunGlareStatus=" + sunGlareStatus
                + "\nvisibleEstimate=" + visualSmoothnessLabel() + " targetFps=" + presetTargetFps()
                + "\nframeMs current=" + oneDecimal(rollingFrameMs) + " avg=" + oneDecimal(avgFrameMs) + " min=" + oneDecimal(minFrameMs) + " max=" + oneDecimal(maxFrameMs) + " p95=" + oneDecimal(p95FrameMs) + " worst=" + oneDecimal(worstFrameMs)
                + "\nframeStatus=" + renderHealthLabel() + " frameBudget=" + frameBudgetStatus + " slow=" + slowFrameCounter + " jank=" + jankFrameCounter
                + "\nanisotropicFiltering=not_exposed textureLodBias=not_exposed"
                + "\naoMode=" + aoMode.name() + " aoApplied=" + aoActuallyApplied
                + "\nshadowsMode=" + shadowMode.name() + " shadowsApplied=" + shadowsActuallyApplied
                + "\nbloom=" + bloomActualStatus
                + "\nrefraction=" + refractionActualStatus);
        }
        if (colorSummaryView != null) {
            colorSummaryView.setText("colorGradingSupported=true"
                + "\ncolorMode=" + colorMode.label
                + "\ntoneMapper=" + toneMapperStatus
                + "\nexposure=" + twoDecimal(colorExposure)
                + "\ncontrast=" + twoDecimal(colorContrast) + " saturation=" + twoDecimal(colorSaturation)
                + "\ntemperature=" + twoDecimal(colorTemperature) + " whitePoint=not_exposed"
                + "\nLUT/palette=not_exposed_by_current_java_api"
                + "\nhighlightProtection=not_exposed"
                + "\ncolorGradingStatus=" + colorGradingStatus);
        }
        if (fogSummaryView != null) {
            fogSummaryView.setText("fogSupported=true"
                + "\nfogEnabled=" + (fogMode != FogMode.OFF && fogDensity > 0.0f)
                + "\nfogMode=" + fogMode.label
                + "\nfogDensity=" + threeDecimal(fogDensity)
                + "\nfogDistance=" + oneDecimal(fogDistance)
                + "\nfogHeight=" + oneDecimal(fogHeight)
                + "\nfogFromIbl=" + realIblReady
                + "\nfogStatus=" + fogStatus);
        }
        if (lightsSummaryView != null) {
            lightsSummaryView.setText("pointLightSupported=true"
                + "\nspotLightSupported=true"
                + "\nadditionalLightsEnabled=" + (lightRig != LightRig.OFF)
                + "\nactiveLightRig=" + lightRig.label
                + "\nlightCount=" + activeLightCount()
                + "\nfalloffSupported=true spotConeSupported=true"
                + "\nadditionalLightShadows=false_mobile_safe"
                + "\nlightRigStatus=" + lightRigStatus);
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
                + "\nshadowMapSizeStatus=not_exposed csmStatus=not_exposed cascadeCountStatus=not_exposed"
                + "\ncontactShadowsSupported=not_exposed contactShadowsEnabled=false"
                + "\nshadowStrength=not_exposed shadowSoftness=not_exposed shadowDistance=not_exposed shadowBiasStatus=not_exposed"
                + "\ncastReceiveFlags=applied_to_renderables_if_loaded"
                + "\nactualFilamentShadowSettingsSummary=" + shadowActualStatus);
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
        if (configSummaryView != null) {
            configSummaryView.setText("configPath=" + configFile().getAbsolutePath()
                + "\nSharedPreferences backup=" + PREFS_NAME
                + "\nconfigStatus=" + configStatus
                + "\nschemaVersion=" + CONFIG_SCHEMA_VERSION + " loadedConfigVersion=" + loadedConfigVersion
                + "\nconfigPrivateSaved=" + configPrivateSaved + " configExportSaved=" + configExportSaved
                + "\nlastConfigError=" + lastConfigError
                + "\nlastConfigLoadSource=" + lastConfigLoadSource
                + "\nlastConfigSaveTimestamp=" + lastConfigSaveTimestamp
                + "\ncurrentDiffersFromSaved=" + currentConfigDiffersFromSaved()
                + "\nmissing paths on load=skip_no_crash"
                + "\nlastSelectedTab=" + activeTab.name() + " panelCollapsed=" + panelCollapsed);
        }
        if (statusView != null) {
            statusView.setText(capabilityStatusTable()
                + "\n\nPerformance:"
                + "\nEstimated visible FPS: " + estimatedVisibleFpsHud()
                + "\nJava callback FPS: " + oneDecimal(rollingFps)
                + "\nCurrent frame ms: " + oneDecimal(rollingFrameMs)
                + "\nAvg frame ms: " + oneDecimal(avgFrameMs)
                + "\np95 frame ms: " + oneDecimal(p95FrameMs)
                + "\nWorst frame ms: " + oneDecimal(worstFrameMs)
                + "\nJank count: " + jankFrameCounter
                + "\nSlow count: " + slowFrameCounter
                + "\nRender status: " + renderHealthLabel()
                + "\nCurrent quality profile: " + qualityProfile.label
                + "\nRender CPU submit approx ms: " + oneDecimal(rollingRenderCpuMs)
                + "\n\nTiming sources:"
                + "\nChoreographer/wall interval: " + timingSourceStatus
                + "\nAndroid FrameMetrics status: " + frameMetricsStatus + " samples=" + frameMetricsSampleCount
                + "\nFrameMetrics total/draw/swap/GPU ms: " + oneDecimal(frameMetricsTotalMs) + "/" + oneDecimal(frameMetricsDrawMs) + "/" + oneDecimal(frameMetricsSwapMs) + "/" + gpuMetricLabel()
                + "\nGPU timing status: " + gpuTimingStatus()
                + "\ngfxinfo command: " + GFXINFO_FRAMESTATS_COMMAND
                + "\nPerfetto/AGI status: external_deferred"
                + "\n\nExpensive feature warnings:"
                + expensiveFeatureWarningsDebug()
                + "\n\nRuntime state:"
                + "\nModel: " + (modelName == null || modelName.isEmpty() ? "none" : modelName)
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
                + "\nactualAA=" + actualAA + " actualMSAA=" + actualSampleCount + "x actualDynamicResolution=" + dynamicResolutionEnabled + " actualRenderScale=" + twoDecimal(renderScale) + " dynamicScale=" + twoDecimal(dynamicMinScale) + "-" + twoDecimal(dynamicMaxScale)
                + "\ntaaSupported=true taaEnabled=" + taaEnabled + " taaStatus=" + taaStatus
                + "\nssrSupported=true ssrEnabled=" + ssrEnabled + " ssrStatus=" + ssrStatus + (ssrEnabled ? " WARNING_manual_heavy_mobile" : "")
                + "\nssrPerformanceWarning=" + ssrPerformanceWarning
                + "\nditheringStatus=" + ditheringStatus + " guardBandStatus=" + guardBandStatus + " fxaaSupported=true"
                + "\ncolorGradingSupported=true colorMode=" + colorMode.name() + " toneMapper=" + toneMapperStatus + " colorGradingStatus=" + colorGradingStatus
                + "\nfogSupported=true fogMode=" + fogMode.name() + " fogStatus=" + fogStatus
                + "\nsunGlareMode=" + sunGlareMode.name() + " sunGlareStatus=" + sunGlareStatus
                + "\nlightCount=" + activeLightCount() + " pointLightSupported=true spotLightSupported=true activeLightRig=" + lightRig.name()
                + "\naoMode=" + aoMode.name() + " aoEnabled=" + aoEnabled + " aoApplied=" + aoActuallyApplied + " aoType=" + aoTypeStatus
                + "\naoStatus=" + aoActualStatus + " aoLikelyInvisibleReason=" + aoLikelyInvisibleReason
                + "\nshadowsMode=" + shadowMode.name() + " shadowsEnabled=" + shadowsEnabled + " shadowsApplied=" + shadowsActuallyApplied
                + "\nshadowStatus=" + shadowActualStatus + " shadowType=" + shadowTypeStatus
                + "\nshadowMapSizeStatus=not_exposed contactShadowsSupported=not_exposed csmStatus=not_exposed cascadeCountStatus=not_exposed shadowBiasStatus=not_exposed"
                + "\nbloomMode=" + bloomMode.name() + " " + bloomActualStatus
                + "\nrefractionSupported=true_screen_space_view_api refractionMode=" + refractionMode.name() + " refractionEnabled=" + refractionEnabled + " refractionActuallyApplied=" + refractionActuallyApplied
                + "\nmodelTransform=" + modelTransformStatus + " target=" + transformTargetStatus + " rx/ry/rz=" + oneDecimal(modelRotationX) + "/" + oneDecimal(modelRotationY) + "/" + oneDecimal(modelRotationZ) + " scale=" + twoDecimal(modelScale)
                + "\ncameraControls=" + cameraStatus + " apply=" + cameraApplyStatus
                + "\npickingSupported=true selectedRenderable=" + selectedRenderable + " selectedMaterialIndex=" + selectedMaterialIndexStatus + " pickDepth=" + oneDecimal(pickDepth) + " pickStatus=" + pickingStatus
                + "\nLegacy Vulkan=removed_from_normal_flow/deprecated/build_required_only normal_ui_route=false"
                + "\nconfigPath=" + configFile().getAbsolutePath()
                + "\nconfigPrivateSaved=" + configPrivateSaved + " configExportSaved=" + configExportSaved + " lastConfigError=" + lastConfigError
                + "\nlastConfigLoadSource=" + lastConfigLoadSource + " lastConfigSaveTimestamp=" + lastConfigSaveTimestamp + " loadedConfigVersion=" + loadedConfigVersion
                + "\nconfigStatus=" + configStatus + " lastActionStatus=" + lastActionStatus
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

    private void refreshUiNow() {
        updateAllSliderLabels();
        updateToggleLabels();
        updateIblButton();
        if (lightingButton != null) lightingButton.setText("Lighting: " + lightingPreset.label);
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
        if (colorModeButton != null) colorModeButton.setText("Color Mode: " + colorMode.label);
        if (fogButton != null) fogButton.setText(fogMode.label);
        updateHud();
    }

    private void resetSsrPerformanceBaseline() {
        ssrSlowFrameBaseline = slowFrameCounter;
        ssrJankFrameBaseline = jankFrameCounter;
        ssrPerformanceWarning = ssrEnabled ? "enabled_monitoring_from_now" : "off";
    }

    private String visualSmoothnessLabel() {
        if (frameWindowCount <= 0) return "Estimated visible FPS: waiting_for_samples";
        if (ssrEnabled && p95FrameMs <= 22.2f) {
            return "Estimated visible FPS: " + oneDecimal(visibleSmoothFps) + " (SSR enabled; GPU not measured)";
        }
        return "Estimated visible FPS: " + oneDecimal(visibleSmoothFps);
    }

    private String gpuTimingStatus() {
        if (frameMetricsGpuMs > 0.0f) return "android_frame_metrics_gpu_duration_estimate_ms=" + oneDecimal(frameMetricsGpuMs);
        return "GPU timing unavailable in current Filament Java path; use FrameMetrics validation, gfxinfo, Perfetto, AGI, or native hooks";
    }

    private String gpuTimingWarningShort() {
        if (ssrEnabled && frameMetricsGpuMs <= 0.0f) return "estimated FPS; GPU timing not exposed; SSR may feel lower";
        if (frameMetricsGpuMs <= 0.0f) return "estimated FPS; GPU timing not exposed";
        return "GPU " + oneDecimal(frameMetricsGpuMs) + "ms";
    }

    private String gpuMetricLabel() {
        return frameMetricsGpuMs > 0.0f ? oneDecimal(frameMetricsGpuMs) : "unavailable";
    }

    private String estimatedVisibleFpsHud() {
        if (frameWindowCount <= 0) return "sampling";
        return String.valueOf(Math.max(1, Math.round(visibleSmoothFps)));
    }

    private float estimatedVisibleFrameMs() {
        if (frameWindowCount > 0 && p95FrameMs > 0.0f) return p95FrameMs;
        return rollingFrameMs;
    }

    private String renderHealthLabel() {
        if (frameWindowCount <= 0) return "SAMPLING";
        if (p95FrameMs > 33.3f || worstFrameMs > 66.6f) return "BAD";
        if (p95FrameMs > 22.2f || jankFrameCounter > 0L) return "JANK";
        if (p95FrameMs > 16.6f || slowFrameCounter > 0L) return "OK";
        return "GOOD";
    }

    private String mainHudExpensiveFeatureLabel() {
        String warnings = expensiveFeatureWarningsCsv();
        return warnings.isEmpty() ? "" : " | costly: " + warnings;
    }

    private String expensiveFeatureWarningsCsv() {
        List<String> warnings = new ArrayList<>();
        if (ssrEnabled) warnings.add("SSR");
        if (aoMode == AoMode.DEBUG_MAX) warnings.add("AO Debug Max");
        if (bloomMode == BloomMode.HIGH) warnings.add("Bloom High");
        if (taaEnabled) warnings.add("TAA");
        if (actualSampleCount >= 4) warnings.add("MSAA " + actualSampleCount + "x");
        return joinLabels(warnings, ", ");
    }

    private String expensiveFeatureWarningsDebug() {
        return "\nSSR: " + (ssrEnabled ? ssrPerformanceWarning : "off")
            + "\nAO Debug Max: " + (aoMode == AoMode.DEBUG_MAX ? "enabled_expensive" : "off")
            + "\nBloom High: " + (bloomMode == BloomMode.HIGH ? "enabled_expensive" : "off")
            + "\nTAA: " + (taaEnabled ? "enabled_medium_to_expensive" : "off")
            + "\nHigh MSAA: " + (actualSampleCount >= 4 ? actualSampleCount + "x_enabled_medium_to_expensive" : "off")
            + "\nFuture volumetric/god rays: deferred";
    }

    private static String joinLabels(List<String> labels, String separator) {
        if (labels == null || labels.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) builder.append(separator);
            builder.append(labels.get(i));
        }
        return builder.toString();
    }

    private boolean currentConfigDiffersFromSaved() {
        try {
            String saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_FILAMENT_CONFIG_JSON, "");
            return saved == null || saved.isEmpty() || !saved.equals(buildConfigJson().toString());
        } catch (Throwable ignored) {
            return true;
        }
    }

    private String capabilityStatusTable() {
        return "Capability status"
            + "\nFilament renderer: supported/active"
            + "\nGLB/glTF loaded: " + ("true".equals(gltfioLoaded) ? "applied" : "off")
            + "\nIBL: " + ("true".equals(realIblReady) ? "applied" : "fallback")
            + "\nColorGrading: " + statusKind(colorGradingStatus)
            + "\nFog: " + statusKind(fogStatus)
            + "\nBloom: " + (bloomEnabled ? "applied" : "off")
            + "\nAO: " + (aoEnabled ? "applied" : "off")
            + "\nShadows: " + (shadowsEnabled ? "applied" : "off")
            + "\nRefraction: " + (refractionEnabled ? "applied" : "off")
            + "\nTAA: " + (taaEnabled ? statusKind(taaStatus) : "off")
            + "\nSSR: " + (ssrEnabled ? "applied_manual_warning" : "off")
            + "\nDithering: " + ("NONE".equals(ditheringStatus) ? "off" : "applied")
            + "\nDynamic Resolution: " + (dynamicResolutionEnabled ? "applied" : "off")
            + "\nMSAA: applied_" + actualSampleCount + "x"
            + "\nLight Rig: " + (lightRig == LightRig.OFF ? "off" : statusKind(lightRigStatus))
            + "\nSun glare: " + statusKind(sunGlareStatus)
            + "\nMaterial Inspector: " + statusKind(materialInspectorStatus)
            + "\nPicking: " + statusKind(pickingStatus)
            + "\nCSM/cascades: not_exposed"
            + "\nShadow map/bias/distance: not_exposed"
            + "\nOld Vulkan normal flow: removed";
    }

    private String statusKind(String status) {
        if (status == null || status.isEmpty()) return "deferred";
        if (status.contains("not_applied")) return "off";
        if (status.contains("not_exposed")) return "not_exposed";
        if (status.contains("failed") || status.contains("error")) return "failed";
        if (status.startsWith("off") || status.contains("disabled")) return "off";
        if (status.contains("fallback")) return "fallback";
        if (status.contains("deferred") || status.contains("limited")) return "deferred";
        if (status.contains("applied") || status.contains("enabled") || status.contains("active") || status.contains("ok_")) return "applied";
        return status;
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
        SliderBinding binding = new SliderBinding(text, slider, label, min, max, step);
        sliderBindings.add(binding);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                float newValue = progressToValue(progress, min, max, step);
                setSliderLabel(text, label, newValue);
                callback.onValue(newValue);
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
        updateAllSliderLabels();
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
        if (dynamicResolutionButton != null) dynamicResolutionButton.setText("Dynamic Resolution: " + (dynamicResolutionEnabled ? "On" : "Off"));
        if (msaaButton != null) msaaButton.setText("MSAA: " + actualSampleCount + "x");
        if (fxaaButton != null) fxaaButton.setText("FXAA: " + (fxaaEnabled ? "On" : "Off"));
        if (ditheringButton != null) ditheringButton.setText("Dithering: " + (ditheringEnabled ? "On" : "Off"));
        if (taaButton != null) taaButton.setText("TAA: " + (taaEnabled ? "On" : "Off"));
        if (ssrButton != null) ssrButton.setText("SSR: " + (ssrEnabled ? "On" : "Off"));
        if (sunGlareButton != null) sunGlareButton.setText(sunGlareMode.label);
        if (colorModeButton != null) colorModeButton.setText("Color Mode: " + colorMode.label);
        if (fogButton != null) fogButton.setText(fogMode.label);
        if (lightRigButton != null) lightRigButton.setText("Light Rig: " + lightRig.label);
        if (skyboxButton != null) skyboxButton.setText("Skybox: " + (skyboxVisible ? "On" : "Off"));
        if (lastActionStatusView != null) lastActionStatusView.setText("status: " + lastActionStatus);
    }

    private void updateAllSliderLabels() {
        for (SliderBinding binding : sliderBindings) {
            float value = currentSliderValue(binding.label);
            setSliderLabel(binding.text, binding.label, value);
            setSliderProgress(binding.slider, value, binding.min, binding.max, binding.step);
        }
    }

    private void setSliderLabel(TextView label, String name, float value) {
        if (label == null) return;
        if ("Fog Density".equals(name)) {
            label.setText(name + " " + threeDecimal(value));
        } else if ("Exp".equals(name) || "BG".equals(name) || "Ambient".equals(name) || "IBL".equals(name) || "Fill".equals(name)
            || "Exposure".equals(name) || "Background".equals(name) || name.contains("Scale") || name.contains("Offset")
            || name.startsWith("Pan") || name.startsWith("Target") || name.contains("Distance")
            || name.startsWith("Color ") || "Bloom Strength".equals(name)) {
            label.setText(name + " " + twoDecimal(value));
        } else if ("Bloom Highlight".equals(name)) {
            label.setText(name + " " + oneDecimal(value));
        } else if ("Sun".equals(name)) {
            label.setText(name + " " + oneDecimal(value));
        } else {
            label.setText(name + " " + oneDecimal(value));
        }
    }

    private float currentSliderValue(String label) {
        if ("Sun".equals(label)) return sunLightIntensity;
        if ("Ambient".equals(label) || "IBL".equals(label)) return ambientUserIntensity;
        if ("Fill".equals(label)) return fillLightIntensity;
        if ("Exp".equals(label) || "Exposure".equals(label)) return exposure;
        if ("BG".equals(label) || "Background".equals(label)) return backgroundBrightness;
        if ("Sun Azimuth".equals(label)) return normalizedAzimuth(sunAzimuth);
        if ("Sun Elevation".equals(label)) return sunElevation;
        if ("Render Scale".equals(label)) return renderScale;
        if ("Bloom Strength".equals(label)) return bloomStrength;
        if ("Bloom Highlight".equals(label)) return bloomHighlight;
        if ("Color Exposure".equals(label)) return colorExposure;
        if ("Color Contrast".equals(label)) return colorContrast;
        if ("Color Saturation".equals(label)) return colorSaturation;
        if ("Color Temperature".equals(label)) return colorTemperature;
        if ("Fog Density".equals(label)) return fogDensity;
        if ("Fog Distance".equals(label)) return fogDistance;
        if ("Fog Height".equals(label)) return fogHeight;
        if ("Rot".equals(label)) return iblRotation;
        if ("Dist".equals(label) || "Camera Distance".equals(label)) return cameraDistance;
        if ("Pan X".equals(label)) return cameraTargetX;
        if ("Pan Y".equals(label)) return cameraTargetY;
        if ("Target Z".equals(label)) return cameraTargetZ;
        if ("FOV".equals(label)) return cameraFov;
        if ("Rot X".equals(label)) return modelRotationX;
        if ("Rot Y".equals(label)) return modelRotationY;
        if ("Rot Z".equals(label)) return modelRotationZ;
        if ("Scale".equals(label)) return modelScale;
        if ("Off X".equals(label) || "Offset X".equals(label)) return modelOffsetX;
        if ("Off Y".equals(label) || "Offset Y".equals(label)) return modelOffsetY;
        if ("Off Z".equals(label) || "Offset Z".equals(label)) return modelOffsetZ;
        return 0.0f;
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
            refreshUiNow();
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
            refreshUiNow();
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

    private static float normalizedAzimuth(float value) {
        float out = value % 360.0f;
        if (out < 0.0f) out += 360.0f;
        return out;
    }

    private static int sanitizeMsaa(int value) {
        if (value <= 1) return 1;
        if (value <= 2) return 2;
        return 4;
    }

    private int activeLightCount() {
        int count = 1; // ModelViewer sun light.
        if (fillLightEntity != 0) count++;
        if (pointLightEntity != 0) count++;
        if (spotLightEntity != 0) count++;
        return count;
    }

    private void setLastAction(String status) {
        lastActionStatus = status == null || status.isEmpty() ? "none" : status;
        if (lastActionStatusView != null) lastActionStatusView.setText("status: " + lastActionStatus);
    }

    private static String nowTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    private static final class SunGlareOverlayView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float centerX = 0.5f;
        private float centerY = 0.18f;
        private float radius = 0.18f;
        private float alpha = 0.10f;

        SunGlareOverlayView(android.content.Context context) {
            super(context);
            setWillNotDraw(false);
            setClickable(false);
        }

        void configure(float x, float y, float radius, float alpha) {
            this.centerX = x;
            this.centerY = y;
            this.radius = radius;
            this.alpha = alpha;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0 || alpha <= 0.0f) return;
            float px = width * centerX;
            float py = height * centerY;
            float pr = Math.max(width, height) * radius;
            int inner = Color.argb(Math.round(alpha * 255.0f), 255, 238, 184);
            int mid = Color.argb(Math.round(alpha * 96.0f), 255, 210, 128);
            int outer = Color.argb(0, 255, 210, 128);
            paint.setShader(new RadialGradient(px, py, pr, new int[] {inner, mid, outer}, new float[] {0.0f, 0.32f, 1.0f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(px, py, pr, paint);
            paint.setShader(null);
        }
    }

    private interface SliderCallback {
        void onValue(float value);
    }

    private static final class SliderBinding {
        final TextView text;
        final SeekBar slider;
        final String label;
        final float min;
        final float max;
        final float step;

        SliderBinding(TextView text, SeekBar slider, String label, float min, float max, float step) {
            this.text = text;
            this.slider = slider;
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
        }
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

    private static String threeDecimal(float value) {
        return String.format(Locale.US, "%.3f", value);
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
