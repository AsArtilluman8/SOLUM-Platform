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

import com.google.android.filament.IndirectLight;
import com.google.android.filament.Skybox;
import com.google.android.filament.View.AntiAliasing;
import com.google.android.filament.View.QualityLevel;
import com.google.android.filament.Engine;
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
    private static final long FPS_WINDOW_NS = 1_000_000_000L;

    private SurfaceView surfaceView;
    private ModelViewer modelViewer;
    private TextView hudView;
    private TextView statusView;
    private Button qualityButton;
    private final Choreographer.FrameCallback frameCallback = this::doFrame;
    private QualityProfile qualityProfile = QualityProfile.MEDIUM;
    private boolean frameCallbackActive = false;
    private long fpsWindowStartNs = 0L;
    private long fpsWindowFrames = 0L;
    private float fps = 0.0f;
    private float frameMs = 0.0f;
    private String modelPath = "";
    private String modelName = "";
    private String loadStatus = "not_started";
    private final String iblStatus = "fallback_neutral_clear_color_plus_sun";

    private enum QualityProfile {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH_PREVIEW("High Preview");

        final String label;

        QualityProfile(String label) {
            this.label = label;
        }

        QualityProfile next() {
            if (this == LOW) return MEDIUM;
            if (this == MEDIUM) return HIGH_PREVIEW;
            return LOW;
        }
    }

    static {
        Utils.init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Button reloadButton = button("Reload");
        reloadButton.setOnClickListener(v -> loadModel());
        Button closeButton = button("Back to Vulkan");
        closeButton.setOnClickListener(v -> finish());
        statusView = overlayText(10.0f, 7);
        controls.addView(qualityButton);
        controls.addView(reloadButton);
        controls.addView(closeButton);
        controls.addView(statusView);
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        controlParams.gravity = Gravity.BOTTOM;
        controlParams.setMargins(dp(12), dp(12), dp(12), dp(28));
        root.addView(controls, controlParams);

        setContentView(root);

        UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        Manipulator manipulator = new Manipulator.Builder()
            .viewport(Math.max(1, surfaceView.getWidth()), Math.max(1, surfaceView.getHeight()))
            .targetPosition(0.0f, 0.0f, 0.0f)
            .orbitHomePosition(0.0f, 0.0f, 4.0f)
            .zoomSpeed(0.012f)
            .build(Manipulator.Mode.ORBIT);
        modelViewer = new ModelViewer(surfaceView, Engine.create(), uiHelper, manipulator);
        surfaceView.setOnTouchListener((view, event) -> {
            modelViewer.onTouchEvent(event);
            return true;
        });
        createFallbackLighting();
        applyQualityProfile();
        loadModel();
        updateHud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startFrames();
    }

    @Override
    protected void onPause() {
        stopFrames();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopFrames();
        if (modelViewer != null) {
            modelViewer.destroy();
            modelViewer = null;
        }
        super.onDestroy();
    }

    private void startFrames() {
        if (frameCallbackActive) return;
        frameCallbackActive = true;
        fpsWindowStartNs = 0L;
        fpsWindowFrames = 0L;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void stopFrames() {
        if (!frameCallbackActive) return;
        frameCallbackActive = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    private void doFrame(long frameTimeNanos) {
        if (!frameCallbackActive || modelViewer == null) return;
        Choreographer.getInstance().postFrameCallback(frameCallback);
        if (fpsWindowStartNs == 0L) fpsWindowStartNs = frameTimeNanos;
        fpsWindowFrames++;
        long elapsed = frameTimeNanos - fpsWindowStartNs;
        if (elapsed >= FPS_WINDOW_NS) {
            fps = fpsWindowFrames * 1_000_000_000.0f / Math.max(1L, elapsed);
            frameMs = fps > 0.01f ? 1000.0f / fps : 0.0f;
            fpsWindowStartNs = frameTimeNanos;
            fpsWindowFrames = 0L;
            updateHud();
        }
        try {
            if (modelViewer.getAnimator() != null && modelViewer.getAnimator().getAnimationCount() > 0) {
                float seconds = frameTimeNanos / 1_000_000_000.0f;
                modelViewer.getAnimator().applyAnimation(0, seconds);
                modelViewer.getAnimator().updateBoneMatrices();
            }
            modelViewer.render(frameTimeNanos);
        } catch (Throwable t) {
            loadStatus = "render_error: " + shortMessage(t);
            updateHud();
        }
    }

    private void loadModel() {
        if (modelViewer == null) return;
        try {
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

    private void createFallbackLighting() {
        if (modelViewer == null) return;
        IndirectLight indirect = new IndirectLight.Builder()
            .intensity(18_000.0f)
            .build(modelViewer.getEngine());
        modelViewer.getScene().setIndirectLight(indirect);
        modelViewer.getScene().setSkybox(new Skybox.Builder()
            .color(0.028f, 0.035f, 0.040f, 1.0f)
            .build(modelViewer.getEngine()));
    }

    private void applyQualityProfile() {
        if (modelViewer == null) return;
        com.google.android.filament.View view = modelViewer.getView();
        com.google.android.filament.View.AmbientOcclusionOptions ao = view.getAmbientOcclusionOptions();
        com.google.android.filament.View.BloomOptions bloom = view.getBloomOptions();
        com.google.android.filament.View.DynamicResolutionOptions dynamic = view.getDynamicResolutionOptions();
        com.google.android.filament.View.RenderQuality renderQuality = view.getRenderQuality();
        if (qualityProfile == QualityProfile.LOW) {
            view.setAntiAliasing(AntiAliasing.NONE);
            ao.enabled = false;
            bloom.enabled = false;
            dynamic.enabled = true;
            dynamic.quality = QualityLevel.LOW;
            renderQuality.hdrColorBuffer = QualityLevel.LOW;
        } else if (qualityProfile == QualityProfile.HIGH_PREVIEW) {
            view.setAntiAliasing(AntiAliasing.FXAA);
            ao.enabled = true;
            bloom.enabled = false;
            dynamic.enabled = true;
            dynamic.quality = QualityLevel.MEDIUM;
            renderQuality.hdrColorBuffer = QualityLevel.HIGH;
        } else {
            view.setAntiAliasing(AntiAliasing.FXAA);
            ao.enabled = false;
            bloom.enabled = false;
            dynamic.enabled = true;
            dynamic.quality = QualityLevel.MEDIUM;
            renderQuality.hdrColorBuffer = QualityLevel.MEDIUM;
        }
        view.setAmbientOcclusionOptions(ao);
        view.setBloomOptions(bloom);
        view.setDynamicResolutionOptions(dynamic);
        view.setRenderQuality(renderQuality);
        if (qualityButton != null) qualityButton.setText("Quality: " + qualityProfile.label);
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
            hudView.setText("Filament Preview  |  FPS " + oneDecimal(fps) + "  |  " + oneDecimal(frameMs)
                + " ms  |  " + qualityProfile.label);
        }
        if (statusView != null) {
            statusView.setText("Model: " + (modelName == null || modelName.isEmpty() ? "none" : modelName)
                + "\nPath: " + shorten(modelPath, 58)
                + "\nLoad: " + loadStatus
                + "\nCamera: orbit drag / pinch zoom"
                + "\nLight: Filament sun + neutral fallback"
                + "\nIBL: " + iblStatus);
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
