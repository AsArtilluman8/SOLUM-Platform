package com.solum.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private long nativeHandle = 0L;
    private TextView statusView;
    private boolean nativeLoaded = false;
    private File cachedReportDir = null;
    private String cachedReportDirReason = "not_resolved";

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);
    private static native String nativeGetRenderLabState(long handle);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            writeCrashReport("uncaught_exception", throwable);
            System.exit(10);
        });
        super.onCreate(savedInstanceState);
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        statusView = new TextView(this);
        statusView.setTextColor(Color.rgb(210, 245, 255));
        statusView.setTextSize(12f);
        statusView.setPadding(14, 10, 14, 10);
        statusView.setGravity(Gravity.START);
        statusView.setSingleLine(false);
        statusView.setMaxLines(4);
        statusView.setBackgroundColor(Color.argb(150, 3, 10, 12));
        statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: starting");
        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP;
        root.addView(statusView, statusParams);
        Button exportButton = new Button(this);
        exportButton.setText("Export Engine Diagnostics");
        exportButton.setAllCaps(false);
        exportButton.setOnClickListener(v -> exportEngineDiagnostics("manual_button"));
        FrameLayout.LayoutParams exportParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        exportParams.gravity = Gravity.BOTTOM | Gravity.END;
        exportParams.setMargins(12, 12, 12, 36);
        root.addView(exportButton, exportParams);
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
                try { statusView.setMaxLines(4); statusView.setText(compactStatus(nativeGetStatus(nativeHandle))); }
                catch (Throwable t) { writeCrashReport("native_status_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: status call failed\n" + shortThrowable(t)); }
            }
        });
    }

    private String compactStatus(String full) {
        String gpu = pickValue(full, "GPU: ");
        String status = "running";
        String next = pickValue(full, "Next: ");
        if (full.contains("Renderer core: OK")) status = "Renderer Core OK";
        else if (full.contains("Vertex buffer: OK")) status = "Vertex Buffer OK";
        else if (full.contains("Triangle draw: OK")) status = "Triangle OK";
        else if (full.contains("Render pass: clear color OK")) status = "Render Pass OK";
        else if (full.contains("Swapchain: created")) status = "Swapchain OK";
        else if (full.toLowerCase(Locale.US).contains("failed")) status = "Error";
        if (gpu.isEmpty()) gpu = "detecting";
        if (next.isEmpty()) next = "Asset Mesh Upload";
        return "SOLUM Engine" + "\nVulkan: " + gpu + "\nStatus: " + status + "\nNext: " + shorten(next, 34);
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

    private void exportEngineDiagnostics(String trigger) {
        try {
            File dir = getReportDir();
            String timestamp = timestampUtc();
            String nativeStatus = getNativeStatusForExport();
            String renderLab = getRenderLabStateForExport();
            String versionName = getVersionName();
            int versionCode = getVersionCode();
            String diagnosticsRoot = dir.getAbsolutePath();
            boolean publicStorage = diagnosticsRoot.startsWith("/storage/emulated/0/SOLUMCreative/");

            File runtime = new File(dir, "engine_runtime_state.json");
            try (FileWriter w = new FileWriter(runtime, false)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.engine_runtime_state\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"timestampUtc\": \"" + escape(timestamp) + "\",\n");
                w.write("  \"app\": \"engine\",\n");
                w.write("  \"packageName\": \"" + escape(getPackageName()) + "\",\n");
                w.write("  \"trigger\": \"" + escape(trigger) + "\",\n");
                w.write("  \"diagnosticsRoot\": \"" + escape(diagnosticsRoot) + "\",\n");
                w.write("  \"diagnosticsRootStatus\": \"" + escape(cachedReportDirReason) + "\",\n");
                w.write("  \"build\": { \"versionName\": \"" + escape(versionName) + "\", \"versionCode\": " + versionCode + " },\n");
                w.write("  \"backend\": { \"rendererPath\": \"Android Native Vulkan\", \"statusText\": \"" + escape(nativeStatus) + "\" },\n");
                w.write("  \"currentScene\": \"scene01_foundation_cube\",\n");
                w.write("  \"renderLab\": " + renderLab + "\n");
                w.write("}\n");
            }

            File manifest = new File(dir, "engine_diagnostics_manifest.json");
            try (FileWriter w = new FileWriter(manifest, false)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.engine_diagnostics_manifest\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"timestampUtc\": \"" + escape(timestamp) + "\",\n");
                w.write("  \"app\": \"engine\",\n");
                w.write("  \"packageName\": \"" + escape(getPackageName()) + "\",\n");
                w.write("  \"diagnosticsRoot\": \"" + escape(diagnosticsRoot) + "\",\n");
                w.write("  \"storage\": {\n");
                w.write("    \"publicRoot\": \"/storage/emulated/0/SOLUMCreative/diagnostics/latest\",\n");
                w.write("    \"actualRoot\": \"" + escape(diagnosticsRoot) + "\",\n");
                w.write("    \"directPublicStorage\": \"" + (publicStorage ? "ok" : "failed") + "\",\n");
                w.write("    \"reason\": \"" + escape(cachedReportDirReason) + "\"\n");
                w.write("  },\n");
                w.write("  \"files\": [\"engine_runtime_state.json\", \"engine_diagnostics_manifest.json\"],\n");
                w.write("  \"screenshot\": { \"status\": \"not_available\", \"reason\": \"renderer_readback_not_implemented\" },\n");
                w.write("  \"renderLab\": " + renderLab + "\n");
                w.write("}\n");
            }
            writeRuntimeNote("engine_diagnostics_exported", "Export Engine Diagnostics wrote engine_runtime_state.json and engine_diagnostics_manifest.json");
        } catch (Throwable t) {
            writeCrashReport("engine_diagnostics_export_failed", t);
        }
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
