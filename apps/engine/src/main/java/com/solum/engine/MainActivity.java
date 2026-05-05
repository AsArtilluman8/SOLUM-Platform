package com.solum.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Choreographer.FrameCallback {
    private long nativeHandle = 0L;
    private TextView statusView;
    private boolean nativeLoaded = false;
    private boolean surfaceReady = false;
    private boolean frameLoopRunning = false;
    private File cachedReportDir = null;

    private long lastFrameNanos = 0L;
    private long frameCount = 0L;
    private double fpsAvg = 0.0;
    private double frameMsAvg = 0.0;
    private double frameMsMin = 999999.0;
    private double frameMsMax = 0.0;
    private long lastReportNanos = 0L;

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native boolean nativeRenderFrame(long handle);
    private static native String nativeGetStatus(long handle);

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
        statusView.setMaxLines(5);
        statusView.setBackgroundColor(Color.argb(150, 3, 10, 12));
        statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: starting");
        statusView.setOnLongClickListener(v -> {
            showDebugSheet();
            return true;
        });

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP;
        root.addView(statusView, statusParams);
        setContentView(root);

        try {
            System.loadLibrary("solum_engine");
            nativeLoaded = true;
            nativeHandle = nativeCreate();
            statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: native ready");
            writeRuntimeNote("native_load_ok", "libsolum_engine loaded and native object created");
        } catch (Throwable t) {
            nativeLoaded = false;
            writeCrashReport("native_load_failed", t);
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: native load failed\n" + shortThrowable(t));
        }
    }

    @Override protected void onDestroy() {
        stopFrameLoop();
        try { if (nativeLoaded && nativeHandle != 0L) { nativeDestroy(nativeHandle); nativeHandle = 0L; } } catch (Throwable t) { writeCrashReport("native_destroy_failed", t); }
        super.onDestroy();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try {
            nativeSurfaceCreated(nativeHandle, holder.getSurface(), getRuntimeReportDirPath());
            surfaceReady = true;
            resetFrameStats();
            updateStatus();
            writeStaticModelAndMaterialState();
            startFrameLoop();
        } catch (Throwable t) {
            writeCrashReport("surface_created_failed", t);
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: surface init failed\n" + shortThrowable(t));
        }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try {
            nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height);
            surfaceReady = true;
            resetFrameStats();
            updateStatus();
            writeStaticModelAndMaterialState();
            startFrameLoop();
        } catch (Throwable t) {
            writeCrashReport("surface_changed_failed", t);
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: surface resize failed\n" + shortThrowable(t));
        }
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopFrameLoop();
        if (!nativeLoaded || nativeHandle == 0L) return;
        try { nativeSurfaceDestroyed(nativeHandle); updateStatus(); } catch (Throwable t) { writeCrashReport("surface_destroyed_failed", t); }
    }

    private void startFrameLoop() {
        if (frameLoopRunning) return;
        frameLoopRunning = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stopFrameLoop() {
        if (!frameLoopRunning) return;
        frameLoopRunning = false;
        try { Choreographer.getInstance().removeFrameCallback(this); } catch (Throwable ignored) { }
    }

    @Override public void doFrame(long frameTimeNanos) {
        if (!frameLoopRunning || !surfaceReady || !nativeLoaded || nativeHandle == 0L) return;
        try {
            boolean rendered = nativeRenderFrame(nativeHandle);
            updateFrameStats(frameTimeNanos, rendered);
            if (frameCount % 30 == 0 || frameTimeNanos - lastReportNanos > 1_000_000_000L) {
                writeRenderState(rendered);
                updateStatus();
                lastReportNanos = frameTimeNanos;
            }
        } catch (Throwable t) {
            writeCrashReport("native_render_frame_failed", t);
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: frame loop failed\n" + shortThrowable(t));
            stopFrameLoop();
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void resetFrameStats() {
        lastFrameNanos = 0L;
        frameCount = 0L;
        fpsAvg = 0.0;
        frameMsAvg = 0.0;
        frameMsMin = 999999.0;
        frameMsMax = 0.0;
        lastReportNanos = 0L;
    }

    private void updateFrameStats(long frameTimeNanos, boolean rendered) {
        if (!rendered) return;
        if (lastFrameNanos != 0L) {
            double frameMs = (frameTimeNanos - lastFrameNanos) / 1_000_000.0;
            if (frameMs > 0.001 && frameMs < 1000.0) {
                double fps = 1000.0 / frameMs;
                if (frameCount == 0) { fpsAvg = fps; frameMsAvg = frameMs; }
                else { fpsAvg = fpsAvg * 0.92 + fps * 0.08; frameMsAvg = frameMsAvg * 0.92 + frameMs * 0.08; }
                frameMsMin = Math.min(frameMsMin, frameMs);
                frameMsMax = Math.max(frameMsMax, frameMs);
                frameCount += 1;
            }
        }
        lastFrameNanos = frameTimeNanos;
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (nativeLoaded && nativeHandle != 0L) {
                try { statusView.setMaxLines(5); statusView.setText(compactStatus(nativeGetStatus(nativeHandle))); }
                catch (Throwable t) { writeCrashReport("native_status_failed", t); statusView.setMaxLines(8); statusView.setText("SOLUM Engine\nStatus: status call failed\n" + shortThrowable(t)); }
            }
        });
    }

    private String compactStatus(String full) {
        String gpu = pickValue(full, "GPU: ");
        String status = "running";
        String next = pickValue(full, "Next: ");
        if (full.contains("3D object: OK")) status = "3D Object OK";
        else if (full.contains("Camera Depth OK")) status = "Camera Depth OK";
        else if (full.contains("Renderer core: OK")) status = "Renderer Core OK";
        else if (full.contains("Vertex buffer: OK")) status = "Vertex Buffer OK";
        else if (full.contains("Triangle draw: OK")) status = "Triangle OK";
        else if (full.contains("Render pass: clear color OK")) status = "Render Pass OK";
        else if (full.contains("Swapchain: created")) status = "Swapchain OK";
        else if (full.toLowerCase(Locale.US).contains("failed")) status = "Error";
        if (gpu.isEmpty()) gpu = "detecting";
        if (next.isEmpty()) next = "Material Foundation";
        String fpsLine = frameCount > 2 ? String.format(Locale.US, "\nFPS: %.1f / %.2f ms", fpsAvg, frameMsAvg) : "";
        return "SOLUM Engine" + "\nVulkan: " + gpu + "\nStatus: " + status + fpsLine + "\nNext: " + shorten(next, 34);
    }

    private void writeRenderState(boolean rendered) {
        String json = "{\n"
                + "  \"schema\": \"solum.runtime_render_state\",\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"status\": \"" + (rendered ? "valid" : "render_failed") + "\",\n"
                + "  \"frameLoop\": true,\n"
                + "  \"frameCount\": " + frameCount + ",\n"
                + "  \"fpsAvg\": " + fmt(fpsAvg) + ",\n"
                + "  \"frameMsAvg\": " + fmt(frameMsAvg) + ",\n"
                + "  \"frameMsMin\": " + fmt(frameMsMin == 999999.0 ? 0.0 : frameMsMin) + ",\n"
                + "  \"frameMsMax\": " + fmt(frameMsMax) + ",\n"
                + "  \"visualSmokeTest\": \"3d_colored_cube_visible\",\n"
                + "  \"knownLimits\": [\"Java Choreographer FPS, not GPU timestamp\", \"No per-pass GPU timings yet\"]\n"
                + "}\n";
        writeFileToReportDir("runtime_render_state.json", json);
    }

    private void writeStaticModelAndMaterialState() {
        String model = "{\n"
                + "  \"schema\": \"solum.runtime_model_state\",\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"status\": \"validation_only\",\n"
                + "  \"mesh\": \"validation_cube\",\n"
                + "  \"vertexLayout\": \"position3_color3\",\n"
                + "  \"depthTest\": true,\n"
                + "  \"camera\": \"fixed_perspective_validation_camera\",\n"
                + "  \"nextRequired\": \"camera_depth_correctness_then_gltf_import_gate\"\n"
                + "}\n";
        String material = "{\n"
                + "  \"schema\": \"solum.runtime_material_state\",\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"status\": \"not_ready_for_gltf_pbr_yet\",\n"
                + "  \"currentPath\": \"vertex_color_validation_only\",\n"
                + "  \"requiredBeforePBR\": [\"gltf2_material_mapping\", \"srgb_linear_rules\", \"normal_tangent_space\", \"texture_slots\", \"alpha_mode\"],\n"
                + "  \"notAFinalMaterialSystem\": true\n"
                + "}\n";
        writeFileToReportDir("runtime_model_state.json", model);
        writeFileToReportDir("runtime_material_state.json", material);
    }

    private void writeFileToReportDir(String name, String content) {
        try {
            File dir = getReportDir();
            File out = new File(dir, name);
            try (FileWriter w = new FileWriter(out, false)) { w.write(content); }
        } catch (Throwable ignored) { }
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
    private String fmt(double v) { return String.format(Locale.US, "%.3f", v); }

    private void showDebugSheet() {
        final File dir = getReportDir();
        writeRuntimeNote("debug_sheet_opened", "Debug Sheet opened by long press on HUD");
        writeDiagnosticsExportRequest(dir, "debug_sheet_opened");

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 20, 24, 20);
        panel.setBackgroundColor(Color.rgb(4, 14, 18));

        TextView title = new TextView(this);
        title.setTextColor(Color.rgb(140, 235, 255));
        title.setTextSize(16f);
        title.setText("SOLUM Debug Sheet");
        panel.addView(title);

        TextView info = new TextView(this);
        info.setTextColor(Color.rgb(220, 245, 250));
        info.setTextSize(12f);
        info.setPadding(0, 12, 0, 12);
        String current = "";
        try {
            if (nativeLoaded && nativeHandle != 0L) {
                current = compactStatus(nativeGetStatus(nativeHandle));
            }
        } catch (Throwable ignored) {
            current = "status unavailable";
        }
        info.setText(current
                + "\nDiagnostics path:\n" + dir.getAbsolutePath()
                + "\n\nTermux ZIP exporter:\nbash tools/export_app_runtime_reports.sh");
        panel.addView(info);

        Button save = new Button(this);
        save.setText("Save runtime state");
        panel.addView(save);

        Button close = new Button(this);
        close.setText("Close");
        panel.addView(close);

        PopupWindow popup = new PopupWindow(
                panel,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        save.setOnClickListener(v -> {
            writeRuntimeNote("manual_runtime_saved", "Runtime state saved from Debug Sheet");
            writeDiagnosticsExportRequest(dir, "manual_runtime_saved");
            Toast.makeText(this, "Runtime diagnostics saved", Toast.LENGTH_SHORT).show();
        });

        close.setOnClickListener(v -> popup.dismiss());
        popup.showAtLocation(statusView, Gravity.BOTTOM, 0, 0);
    }

    private void writeDiagnosticsExportRequest(File dir, String reason) {
        try {
            File out = new File(dir, "diagnostics_export_request.json");
            try (FileWriter w = new FileWriter(out)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.diagnostics_export_request\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"reason\": \"" + escape(reason) + "\",\n");
                w.write("  \"nextTermuxAction\": \"bash tools/export_app_runtime_reports.sh\",\n");
                w.write("  \"reportDir\": \"" + escape(dir.getAbsolutePath()) + "\"\n");
                w.write("}\n");
            }
        } catch (Throwable ignored) {
        }
    }

    private String getRuntimeReportDirPath() { return getReportDir().getAbsolutePath(); }

    private File getReportDir() {
        if (cachedReportDir != null && cachedReportDir.exists()) return cachedReportDir;
        File solumCreative = new File("/storage/emulated/0/SOLUMCreative/diagnostics/latest");
        if (canWriteDirectory(solumCreative)) { cachedReportDir = solumCreative; return cachedReportDir; }
        File downloadCreative = new File("/storage/emulated/0/Download/SOLUMCreative/diagnostics/latest");
        if (canWriteDirectory(downloadCreative)) { cachedReportDir = downloadCreative; return cachedReportDir; }
        File externalBase = getExternalFilesDir(null);
        if (externalBase != null) {
            File externalDir = new File(externalBase, "solum_diagnostics");
            if (canWriteDirectory(externalDir)) { cachedReportDir = externalDir; return cachedReportDir; }
        }
        File appDir = new File(getFilesDir(), "solum_diagnostics");
        appDir.mkdirs();
        cachedReportDir = appDir;
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
        String json = "{\n"
                + "  \"schema\": \"solum.runtime_java_state\",\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"status\": \"" + escape(status) + "\",\n"
                + "  \"message\": \"" + escape(message) + "\",\n"
                + "  \"reportDir\": \"" + escape(getReportDir().getAbsolutePath()) + "\"\n"
                + "}\n";
        writeFileToReportDir("runtime_java_state.json", json);
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
