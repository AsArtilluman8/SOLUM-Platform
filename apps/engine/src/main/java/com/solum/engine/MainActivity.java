package com.solum.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private long nativeHandle = 0L;
    private TextView statusView;
    private boolean nativeLoaded = false;

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
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
        statusView.setPadding(18, 18, 18, 18);
        statusView.setBackgroundColor(Color.argb(180, 3, 10, 12));
        statusView.setText("SOLUM Engine\nLoading native Vulkan module...\nReport path: " + getRuntimeReportDirPath());

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(statusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        setContentView(root);

        try {
            System.loadLibrary("solum_engine");
            nativeLoaded = true;
            nativeHandle = nativeCreate();
            statusView.setText("SOLUM Engine\nNative module loaded. Waiting for Vulkan surface...\nReport path: " + getRuntimeReportDirPath());
            writeRuntimeNote("native_load_ok", "libsolum_engine loaded and native object created");
        } catch (Throwable t) {
            nativeLoaded = false;
            writeCrashReport("native_load_failed", t);
            statusView.setText("SOLUM Engine\nNative load failed. Crash report written.\n" + shortThrowable(t) + "\nReport path: " + getRuntimeReportDirPath());
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (nativeLoaded && nativeHandle != 0L) {
                nativeDestroy(nativeHandle);
                nativeHandle = 0L;
            }
        } catch (Throwable t) {
            writeCrashReport("native_destroy_failed", t);
        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try {
            nativeSurfaceCreated(nativeHandle, holder.getSurface(), getRuntimeReportDirPath());
            updateStatus();
        } catch (Throwable t) {
            writeCrashReport("surface_created_failed", t);
            statusView.setText("SOLUM Engine\nSurface init failed. Report written.\n" + shortThrowable(t) + "\nReport path: " + getRuntimeReportDirPath());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try {
            nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height);
            updateStatus();
        } catch (Throwable t) {
            writeCrashReport("surface_changed_failed", t);
            statusView.setText("SOLUM Engine\nSurface resize failed. Report written.\n" + shortThrowable(t) + "\nReport path: " + getRuntimeReportDirPath());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!nativeLoaded || nativeHandle == 0L) return;
        try {
            nativeSurfaceDestroyed(nativeHandle);
            updateStatus();
        } catch (Throwable t) {
            writeCrashReport("surface_destroyed_failed", t);
        }
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (nativeLoaded && nativeHandle != 0L) {
                try {
                    statusView.setText(nativeGetStatus(nativeHandle) + "\nReport path: " + getRuntimeReportDirPath());
                } catch (Throwable t) {
                    writeCrashReport("native_status_failed", t);
                    statusView.setText("SOLUM Engine\nStatus call failed. Report written.\n" + shortThrowable(t) + "\nReport path: " + getRuntimeReportDirPath());
                }
            }
        });
    }

    private String getRuntimeReportDirPath() {
        return getReportDir().getAbsolutePath();
    }

    private File getReportDir() {
        File externalBase = getExternalFilesDir(null);
        if (externalBase != null) {
            File externalDir = new File(externalBase, "solum_diagnostics");
            if (externalDir.mkdirs() || externalDir.exists()) {
                return externalDir;
            }
        }
        File appDir = new File(getFilesDir(), "solum_diagnostics");
        appDir.mkdirs();
        return appDir;
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
        } catch (Throwable ignored) {
        }
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
        } catch (Throwable ignored) {
        }
    }

    private String shortThrowable(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) msg = "no message";
        return t.getClass().getSimpleName() + ": " + msg;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
