package com.solum.engine;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG_DIAG = "SOLUM_ENGINE_DIAG";
    private static final String PREFS_NAME = "solum_engine_diagnostics";
    private static final String PREF_TREE_URI = "diagnostics_tree_uri";
    private static final int REQUEST_CHOOSE_DIAGNOSTICS_TREE = 2202;

    private long nativeHandle = 0L;
    private TextView statusView;
    private TextView diagnosticsStatusView;
    private Button exportButton;
    private Button chooseFolderButton;
    private boolean nativeLoaded = false;
    private File cachedReportDir = null;
    private String cachedReportDirReason = "not_resolved";
    private String lastExportStatus = "not run";
    private String lastExportRoute = "not run";
    private String lastExportReason = "";
    private String lastExportPath = "";
    private String lastExportTimestamp = "";
    private float cameraYawDeg = 28.0f;
    private float cameraPitchDeg = -18.0f;
    private float cameraDistance = 4.2f;
    private float lastTouchX = 0.0f;
    private float lastTouchY = 0.0f;
    private float lastPinchDistance = 0.0f;
    private boolean pinchActive = false;

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);
    private static native String nativeGetRenderLabState(long handle);
    private static native void nativeSetCamera(long handle, float yawDeg, float pitchDeg, float distance);

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
        statusView = new TextView(this);
        statusView.setTextColor(Color.rgb(210, 245, 255));
        statusView.setTextSize(12f);
        statusView.setPadding(14, 10, 14, 10);
        statusView.setGravity(Gravity.START);
        statusView.setSingleLine(false);
        statusView.setMaxLines(12);
        statusView.setBackgroundColor(Color.argb(150, 3, 10, 12));
        statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: starting");
        diagnosticsStatusView = new TextView(this);
        diagnosticsStatusView.setTextColor(Color.rgb(232, 246, 255));
        diagnosticsStatusView.setTextSize(11f);
        diagnosticsStatusView.setPadding(14, 10, 14, 10);
        diagnosticsStatusView.setGravity(Gravity.START);
        diagnosticsStatusView.setSingleLine(false);
        diagnosticsStatusView.setMaxLines(7);
        diagnosticsStatusView.setBackgroundColor(Color.argb(165, 3, 10, 12));
        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP;
        root.addView(statusView, statusParams);
        chooseFolderButton = new Button(this);
        chooseFolderButton.setText("Choose Diagnostics Folder");
        chooseFolderButton.setAllCaps(false);
        chooseFolderButton.setOnClickListener(v -> chooseDiagnosticsFolder());
        FrameLayout.LayoutParams chooseParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        chooseParams.gravity = Gravity.BOTTOM | Gravity.START;
        chooseParams.setMargins(12, 12, 12, 36);
        root.addView(chooseFolderButton, chooseParams);
        exportButton = new Button(this);
        exportButton.setText("Export Engine Diagnostics");
        exportButton.setAllCaps(false);
        exportButton.setOnClickListener(v -> exportEngineDiagnosticsFromButton());
        FrameLayout.LayoutParams exportParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        exportParams.gravity = Gravity.BOTTOM | Gravity.END;
        exportParams.setMargins(12, 12, 12, 36);
        root.addView(exportButton, exportParams);
        Button zoomInButton = new Button(this);
        zoomInButton.setText("Zoom In");
        zoomInButton.setAllCaps(false);
        zoomInButton.setOnClickListener(v -> applyCamera(cameraYawDeg, cameraPitchDeg, cameraDistance - 0.35f));
        FrameLayout.LayoutParams zoomInParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        zoomInParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        zoomInParams.setMargins(12, 12, 12, 86);
        root.addView(zoomInButton, zoomInParams);
        Button zoomOutButton = new Button(this);
        zoomOutButton.setText("Zoom Out");
        zoomOutButton.setAllCaps(false);
        zoomOutButton.setOnClickListener(v -> applyCamera(cameraYawDeg, cameraPitchDeg, cameraDistance + 0.35f));
        FrameLayout.LayoutParams zoomOutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        zoomOutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        zoomOutParams.setMargins(12, 86, 12, 12);
        root.addView(zoomOutButton, zoomOutParams);
        FrameLayout.LayoutParams diagnosticsParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        diagnosticsParams.gravity = Gravity.BOTTOM;
        diagnosticsParams.setMargins(12, 12, 12, 118);
        root.addView(diagnosticsStatusView, diagnosticsParams);
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

    @Override protected void onDestroy() {
        try { if (nativeLoaded && nativeHandle != 0L) { nativeDestroy(nativeHandle); nativeHandle = 0L; } } catch (Throwable t) { writeCrashReport("native_destroy_failed", t); }
        super.onDestroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        try { nativeSurfaceCreated(nativeHandle, holder.getSurface(), getRuntimeReportDirPath()); updateStatus(); exportEngineDiagnostics("surface_created"); }
        catch (Throwable t) { writeCrashReport("surface_created_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: surface init failed\n" + shortThrowable(t)); }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height); updateStatus(); exportEngineDiagnostics("surface_changed"); }
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
        String next = pickValue(full, "Next: ");
        if (full.contains("Cube draw: OK")) status = "Scene01 Cube OK";
        else if (full.contains("Renderer core: OK")) status = "Renderer Core OK";
        else if (full.contains("Vertex buffer: OK")) status = "Vertex Buffer OK";
        else if (full.contains("Triangle draw: OK")) status = "Triangle OK";
        else if (full.contains("Render pass: clear color OK")) status = "Render Pass OK";
        else if (full.contains("Swapchain: created")) status = "Swapchain OK";
        else if (full.toLowerCase(Locale.US).contains("failed")) status = "Error";
        if (gpu.isEmpty()) gpu = "detecting";
        if (next.isEmpty()) next = "Material Constants / Asset Mesh Upload";
        String cube = full.contains("Cube draw: OK") ? "OK" : "pending";
        String depth = full.contains("Depth: OK") ? "OK" : "pending";
        String camera = full.contains("Camera: controls OK") ? "controls OK" : "pending";
        String material = full.contains("Material constants: OK") ? "OK" : "pending";
        String mesh = full.contains("Mesh layout: OK") ? "OK" : "pending";
        return "SOLUM Engine"
            + "\nVulkan: " + gpu
            + "\nRender Lab: Scene01 Foundation Cube"
            + "\nCube: " + cube
            + "\nDepth: " + depth
            + "\nCamera: " + camera
            + "\nMaterial constants: " + material
            + "\nMesh layout: " + mesh
            + "\nHint: Drag rotate / pinch zoom or buttons zoom"
            + "\nStatus: " + status
            + "\nNext: " + shorten(next, 48);
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
        exportButton.setEnabled(false);
        exportButton.setText("Exporting...");
        updateDiagnosticsStatusPanel();
        exportButton.post(() -> {
            ExportResult result = exportEngineDiagnostics("manual_button");
            lastExportStatus = result.ok ? "ok" : "failed";
            lastExportRoute = result.route;
            lastExportReason = result.reason;
            lastExportPath = result.actualRoot;
            lastExportTimestamp = result.timestamp;
            exportButton.setText(result.ok ? "Export OK" : "Export Failed");
            exportButton.setEnabled(true);
            updateDiagnosticsStatusPanel();
        });
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
            + "  \"currentScene\": \"scene01_foundation_cube\",\n"
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
            + "  \"files\": [\"engine_runtime_state.json\", \"engine_diagnostics_manifest.json\"],\n"
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
            + "  \"vertexLayout\": \"" + escape(jsonStringField(renderLab, "vertexLayout", "unknown")) + "\",\n"
            + "  \"vertexStrideBytes\": " + jsonNumberField(renderLab, "vertexStrideBytes", "0") + ",\n"
            + "  \"renderLab\": " + renderLab + "\n"
            + "}\n";
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
        Uri existing = findChild(resolver, parentDocumentUri, name);
        if (existing != null) return existing;
        Uri created = DocumentsContract.createDocument(resolver, parentDocumentUri, "application/json", name);
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
                    + "Last export timestamp: " + (lastExportTimestamp.isEmpty() ? "not run" : lastExportTimestamp)
            );
        });
    }

    private String getNativeStatusForExport() {
        if (!nativeLoaded || nativeHandle == 0L) return "native_not_loaded";
        try { return nativeGetStatus(nativeHandle); } catch (Throwable t) { return "native_status_failed: " + shortThrowable(t); }
    }

    private String getRenderLabStateForExport() {
        if (!nativeLoaded || nativeHandle == 0L) {
            return "{\"currentLabScene\":\"scene01_foundation_cube\",\"status\":\"native_not_loaded\"}";
        }
        try { return nativeGetRenderLabState(nativeHandle); }
        catch (Throwable t) { return "{\"currentLabScene\":\"scene01_foundation_cube\",\"status\":\"native_render_lab_state_failed\",\"reason\":\"" + escape(shortThrowable(t)) + "\"}"; }
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

    private static final class ExportResult {
        boolean ok = false;
        String route = "failed";
        String reason = "not_started";
        String actualRoot = "";
        String timestamp = "";
        Writer runtimeWriter;
        Writer manifestWriter;
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
