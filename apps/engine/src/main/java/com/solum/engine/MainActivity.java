package com.solum.engine;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int REQ_IMPORT_MODEL = 13013;

    private long nativeHandle = 0L;
    private TextView statusView;
    private TextView debugText;
    private LinearLayout debugSheet;
    private boolean nativeLoaded = false;
    private File cachedReportDir = null;
    private File cachedModelDir = null;
    private String lastImportedModelPath = "none";
    private String lastImportedModelName = "none";

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);
    private static native void nativeRenderFrame(long handle);

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
        statusView.setMaxLines(6);
        statusView.setBackgroundColor(Color.argb(150, 3, 10, 12));
        statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: starting\nModel: none\nFPS: waiting");
        statusView.setOnLongClickListener(v -> { toggleDebugSheet(); return true; });

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.TOP;
        root.addView(statusView, statusParams);

        debugSheet = makeDebugSheet();
        FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        sheetParams.gravity = Gravity.BOTTOM;
        root.addView(debugSheet, sheetParams);

        setContentView(root);
        loadPersistedModelState();

        try {
            System.loadLibrary("solum_engine");
            nativeLoaded = true;
            nativeHandle = nativeCreate();
            statusView.setText("SOLUM Engine\nVulkan: loading\nStatus: native ready\nModel: none\nFPS: waiting");
            writeRuntimeNote("native_load_ok", "libsolum_engine loaded and native object created");
        } catch (Throwable t) {
            nativeLoaded = false;
            writeCrashReport("native_load_failed", t);
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: native load failed\n" + shortThrowable(t));
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
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: surface init failed\n" + shortThrowable(t));
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
            statusView.setMaxLines(8);
            statusView.setText("SOLUM Engine\nStatus: surface resize failed\n" + shortThrowable(t));
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

    private LinearLayout makeDebugSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(16, 14, 16, 16);
        sheet.setBackgroundColor(Color.argb(220, 4, 10, 12));
        sheet.setVisibility(LinearLayout.GONE);

        TextView title = new TextView(this);
        title.setTextColor(Color.rgb(230, 250, 255));
        title.setTextSize(15f);
        title.setText("SOLUM Debug Sheet");
        sheet.addView(title);

        debugText = new TextView(this);
        debugText.setTextColor(Color.rgb(190, 230, 240));
        debugText.setTextSize(12f);
        debugText.setText("Long press HUD to show/hide.\nImport GLB/GLTF keeps a local SOLUM asset copy.");
        sheet.addView(debugText);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button save = new Button(this);
        save.setText("Save runtime state");
        save.setOnClickListener(v -> saveRuntimeStateFromUi());
        row.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button importModel = new Button(this);
        importModel.setText("Import GLB/GLTF");
        importModel.setOnClickListener(v -> openModelPicker());
        row.addView(importModel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        sheet.addView(row);
        return sheet;
    }

    private void toggleDebugSheet() {
        if (debugSheet == null) return;
        debugSheet.setVisibility(debugSheet.getVisibility() == LinearLayout.VISIBLE ? LinearLayout.GONE : LinearLayout.VISIBLE);
        refreshDebugText("Debug Sheet ready.");
    }

    private void refreshDebugText(String message) {
        if (debugText == null) return;
        debugText.setText(message
                + "\nRuntime: " + getRuntimeReportDirPath()
                + "\nAssets: " + getModelAssetDir().getAbsolutePath()
                + "\nLast model: " + lastImportedModelName
                + "\nLast path: " + lastImportedModelPath);
    }

    private void openModelPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQ_IMPORT_MODEL);
        } catch (Throwable t) {
            writeCrashReport("open_model_picker_failed", t);
            refreshDebugText("Open model picker failed: " + shortThrowable(t));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT_MODEL && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importSelectedModel(data.getData(), data.getFlags());
        }
    }

    private void importSelectedModel(Uri uri, int flags) {
        try {
            final int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri, takeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) {
            }

            String displayName = sanitizeFileName(resolveDisplayName(uri));
            if (!(displayName.toLowerCase(Locale.US).endsWith(".glb") || displayName.toLowerCase(Locale.US).endsWith(".gltf"))) {
                displayName = displayName + ".glb";
            }

            File outDir = getModelAssetDir();
            File out = uniqueFile(outDir, displayName);
            long bytes = copyUriToFile(uri, out);
            lastImportedModelPath = out.getAbsolutePath();
            lastImportedModelName = displayName;
            persistModelState();
            writeModelImportState("imported", displayName, uri.toString(), out.getAbsolutePath(), bytes, "ready_for_probe_not_rendered_yet");
            writeImportedModelDirListing("after_import");
            refreshDebugText("Model imported: " + displayName + " (" + bytes + " bytes)\nSaved as asset: " + out.getAbsolutePath());
            updateStatus();
        } catch (Throwable t) {
            writeCrashReport("model_import_failed", t);
            writeModelImportState("failed", "unknown", uri.toString(), "none", 0, shortThrowable(t));
            refreshDebugText("Model import failed: " + shortThrowable(t));
        }
    }

    private long copyUriToFile(Uri uri, File out) throws Exception {
        long total = 0;
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IllegalStateException("openInputStream returned null");
            byte[] buffer = new byte[1024 * 64];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                os.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    private String resolveDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Throwable ignored) {
        }
        if (name == null || name.trim().isEmpty()) name = "imported_model.glb";
        return name;
    }

    private String sanitizeFileName(String input) {
        String safe = input == null ? "imported_model.glb" : input.trim();
        safe = safe.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isEmpty()) safe = "imported_model.glb";
        return safe;
    }

    private File uniqueFile(File dir, String name) {
        File base = new File(dir, name);
        if (!base.exists()) return base;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; i < 1000; i++) {
            File f = new File(dir, stem + "_" + i + ext);
            if (!f.exists()) return f;
        }
        return new File(dir, stem + "_" + System.currentTimeMillis() + ext);
    }

    private void saveRuntimeStateFromUi() {
        writeRuntimeNote("debug_sheet_save_requested", "User tapped Save runtime state");
        writeDiagnosticsExportRequest("debug_sheet_button");
        File modelFile = new File(lastImportedModelPath == null ? "none" : lastImportedModelPath);
        long modelBytes = modelFile.exists() ? modelFile.length() : 0L;
        writeModelImportState("last_known", lastImportedModelName, "persisted_or_last_import", lastImportedModelPath, modelBytes, "debug_sheet_save");
        writeImportedModelDirListing("debug_sheet_save");
        refreshDebugText("Runtime state saved. Model: " + lastImportedModelName + "\nExport ZIP from Termux when needed.");
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (nativeLoaded && nativeHandle != 0L) {
                try {
                    statusView.setMaxLines(6);
                    statusView.setText(compactStatus(nativeGetStatus(nativeHandle)));
                } catch (Throwable t) {
                    writeCrashReport("native_status_failed", t);
                    statusView.setMaxLines(8);
                    statusView.setText("SOLUM Engine\nStatus: status call failed\n" + shortThrowable(t));
                }
            }
        });
    }

    private String compactStatus(String full) {
        String gpu = pickValue(full, "GPU: ");
        String status = "running";
        String next = pickValue(full, "Next: ");
        String fps = pickValue(full, "FPS: ");
        if (full.contains("Camera depth: OK")) status = "Camera Depth OK";
        else if (full.contains("3D object: OK")) status = "3D Object OK";
        else if (full.contains("Renderer core: OK")) status = "Renderer Core OK";
        else if (full.contains("Vertex buffer: OK")) status = "Vertex Buffer OK";
        else if (full.contains("Triangle draw: OK")) status = "Triangle OK";
        else if (full.toLowerCase(Locale.US).contains("failed")) status = "Error";
        if (gpu.isEmpty()) gpu = "detecting";
        if (next.isEmpty()) next = "glTF Material Import";
        if (fps.isEmpty()) fps = "collecting";
        return "SOLUM Engine" + "\nVulkan: " + gpu + "\nStatus: " + status + "\nModel: " + shorten(lastImportedModelName, 34) + "\nFPS: " + shorten(fps, 34) + "\nNext: " + shorten(next, 34);
    }

    private String pickValue(String text, String prefix) {
        int start = text.indexOf(prefix);
        if (start < 0) return "";
        start += prefix.length();
        int end = text.indexOf('\n', start);
        if (end < 0) end = text.length();
        return text.substring(start, end).trim();
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String getRuntimeReportDirPath() { return getReportDir().getAbsolutePath(); }

    private File getReportDir() {
        if (cachedReportDir != null && cachedReportDir.exists()) return cachedReportDir;
        File solumCreative = new File("/storage/emulated/0/SOLUMCreative/diagnostics/latest");
        if (canWriteDirectory(solumCreative)) { cachedReportDir = solumCreative; return cachedReportDir; }
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

    private File getModelAssetDir() {
        if (cachedModelDir != null && cachedModelDir.exists()) return cachedModelDir;
        File publicDir = new File("/storage/emulated/0/SOLUMCreative/assets/models/imported");
        if (canWriteDirectory(publicDir)) { cachedModelDir = publicDir; return cachedModelDir; }
        File externalBase = getExternalFilesDir(null);
        if (externalBase != null) {
            File externalDir = new File(externalBase, "assets/models/imported");
            if (canWriteDirectory(externalDir)) { cachedModelDir = externalDir; return cachedModelDir; }
        }
        File appDir = new File(getFilesDir(), "assets/models/imported");
        appDir.mkdirs();
        cachedModelDir = appDir;
        return cachedModelDir;
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
                w.write("  \"schemaVersion\": 2,\n");
                w.write("  \"status\": \"" + escape(status) + "\",\n");
                w.write("  \"message\": \"" + escape(message) + "\",\n");
                w.write("  \"reportDir\": \"" + escape(dir.getAbsolutePath()) + "\",\n");
                w.write("  \"assetModelDir\": \"" + escape(getModelAssetDir().getAbsolutePath()) + "\",\n");
                w.write("  \"lastImportedModelPath\": \"" + escape(lastImportedModelPath) + "\",\n");
                w.write("  \"lastImportedModelName\": \"" + escape(lastImportedModelName) + "\"\n");
                w.write("}\n");
            }
        } catch (Throwable ignored) { }
    }

    private void writeDiagnosticsExportRequest(String source) {
        try {
            File out = new File(getReportDir(), "diagnostics_export_request.json");
            try (FileWriter w = new FileWriter(out)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.diagnostics_export_request\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"source\": \"" + escape(source) + "\",\n");
                w.write("  \"time\": \"" + escape(now()) + "\"\n");
                w.write("}\n");
            }
        } catch (Throwable ignored) { }
    }

    private void writeModelImportState(String status, String displayName, String sourceUri, String path, long sizeBytes, String note) {
        try {
            File diag = new File(getReportDir(), "runtime_model_import_state.json");
            writeModelImportJson(diag, status, displayName, sourceUri, path, sizeBytes, note);
            File index = new File(getModelAssetDir(), "imported_models_index.json");
            writeModelImportJson(index, status, displayName, sourceUri, path, sizeBytes, note);
        } catch (Throwable ignored) { }
    }

    private void writeModelImportJson(File out, String status, String displayName, String sourceUri, String path, long sizeBytes, String note) throws Exception {
        try (FileWriter w = new FileWriter(out)) {
            w.write("{\n");
            w.write("  \"schema\": \"solum.model_import_state\",\n");
            w.write("  \"schemaVersion\": 1,\n");
            w.write("  \"status\": \"" + escape(status) + "\",\n");
            w.write("  \"displayName\": \"" + escape(displayName) + "\",\n");
            w.write("  \"sourceUri\": \"" + escape(sourceUri) + "\",\n");
            w.write("  \"assetPath\": \"" + escape(path) + "\",\n");
            w.write("  \"sizeBytes\": " + sizeBytes + ",\n");
            w.write("  \"note\": \"" + escape(note) + "\",\n");
            w.write("  \"importedAt\": \"" + escape(now()) + "\"\n");
            w.write("}\n");
        }
    }


    private void loadPersistedModelState() {
        try {
            SharedPreferences sp = getSharedPreferences("solum_imports", MODE_PRIVATE);
            lastImportedModelPath = sp.getString("lastImportedModelPath", "none");
            lastImportedModelName = sp.getString("lastImportedModelName", "none");
            writeImportedModelDirListing("startup");
        } catch (Throwable ignored) { }
    }

    private void persistModelState() {
        try {
            getSharedPreferences("solum_imports", MODE_PRIVATE)
                    .edit()
                    .putString("lastImportedModelPath", lastImportedModelPath)
                    .putString("lastImportedModelName", lastImportedModelName)
                    .apply();
        } catch (Throwable ignored) { }
    }

    private void writeImportedModelDirListing(String source) {
        try {
            File dir = getModelAssetDir();
            File out = new File(getReportDir(), "runtime_model_files.json");
            File[] files = dir.listFiles();
            try (FileWriter w = new FileWriter(out)) {
                w.write("{\n");
                w.write("  \"schema\": \"solum.runtime_model_files\",\n");
                w.write("  \"schemaVersion\": 1,\n");
                w.write("  \"source\": \"" + escape(source) + "\",\n");
                w.write("  \"modelDir\": \"" + escape(dir.getAbsolutePath()) + "\",\n");
                w.write("  \"lastImportedModelName\": \"" + escape(lastImportedModelName) + "\",\n");
                w.write("  \"lastImportedModelPath\": \"" + escape(lastImportedModelPath) + "\",\n");
                w.write("  \"files\": [\n");
                boolean first = true;
                if (files != null) {
                    for (File f : files) {
                        String n = f.getName().toLowerCase(Locale.US);
                        if (!(n.endsWith(".glb") || n.endsWith(".gltf"))) continue;
                        if (!first) w.write(",\n");
                        first = false;
                        w.write("    { \"name\": \"" + escape(f.getName()) + "\", \"path\": \"" + escape(f.getAbsolutePath()) + "\", \"sizeBytes\": " + f.length() + " }");
                    }
                }
                w.write("\n  ]\n");
                w.write("}\n");
            }
        } catch (Throwable ignored) { }
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

    private String now() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()); }
    private String shortThrowable(Throwable t) { String msg = t.getMessage(); if (msg == null) msg = "no message"; return t.getClass().getSimpleName() + ": " + msg; }
    private String escape(String s) { if (s == null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
