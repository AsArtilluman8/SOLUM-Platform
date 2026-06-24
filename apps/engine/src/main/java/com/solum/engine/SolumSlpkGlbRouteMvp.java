package com.solum.engine;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * SLPK -> GLB bridge route MVP.
 */
public final class SolumSlpkGlbRouteMvp {
    public static final String SAMPLE_ASSET_PATH = "solum/f5e_sample_cooked_scene.slpk";

    public static final class RouteResult {
        public final boolean ok;
        public final String status;
        public final String packageName;
        public final int objectCount;
        public final int materialCount;
        public final int textureCount;
        public final int chunkCount;
        public final List<String> chunkTypes;
        public final long packageBytes;
        public final long glbBytes;
        public final File extractedGlbFile;
        public final String assetPath;

        RouteResult(boolean ok, String status, String packageName, int objectCount, int materialCount,
                    int textureCount, int chunkCount, List<String> chunkTypes, long packageBytes,
                    long glbBytes, File extractedGlbFile, String assetPath) {
            this.ok = ok;
            this.status = status;
            this.packageName = packageName;
            this.objectCount = objectCount;
            this.materialCount = materialCount;
            this.textureCount = textureCount;
            this.chunkCount = chunkCount;
            this.chunkTypes = chunkTypes;
            this.packageBytes = packageBytes;
            this.glbBytes = glbBytes;
            this.extractedGlbFile = extractedGlbFile;
            this.assetPath = assetPath;
        }

        public String debugLine() {
            return "ok=" + ok
                    + " status=" + status
                    + " package=" + packageName
                    + " objects=" + objectCount
                    + " materials=" + materialCount
                    + " textures=" + textureCount
                    + " chunks=" + chunkTypes
                    + " packageBytes=" + packageBytes
                    + " glbBytes=" + glbBytes
                    + " extracted=" + (extractedGlbFile == null ? "none" : extractedGlbFile.getAbsolutePath());
        }
    }

    private SolumSlpkGlbRouteMvp() { }

    public static RouteResult extractBundledSampleGlb(Context context) throws IOException {
        return extractAssetGlb(context, SAMPLE_ASSET_PATH, "f5e_route_model.glb");
    }

    public static RouteResult extractAssetGlb(Context context, String assetPath, String outputName) throws IOException {
        byte[] packageBytes = readAssetBytes(context, assetPath);
        SolumPackageReaderMvp reader = SolumPackageReaderMvp.open(packageBytes);
        SolumPackageReaderMvp.Summary summary = reader.fastSummary();

        if (!summary.hasGlbChunk || summary.glbBytes <= 0) {
            throw new IOException("ERR_SLPK_GLB_CHUNK_MISSING");
        }

        byte[] glbBytes = reader.readChunkBytes("GLB ");
        if (glbBytes.length < 4 || glbBytes[0] != 'g' || glbBytes[1] != 'l' || glbBytes[2] != 'T' || glbBytes[3] != 'F') {
            throw new IOException("ERR_SLPK_GLB_MAGIC_INVALID");
        }

        File dir = new File(context.getCacheDir(), "solum_slpk_route");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("ERR_SLPK_ROUTE_CACHE_UNAVAILABLE");
        }

        File out = new File(dir, outputName == null || outputName.isEmpty() ? "slpk_route_model.glb" : outputName);
        try (FileOutputStream stream = new FileOutputStream(out, false)) {
            stream.write(glbBytes);
            stream.flush();
        }

        return new RouteResult(
                true,
                "slpk_glb_extracted_to_cache",
                summary.packageName,
                summary.objectCount,
                summary.materialCount,
                summary.textureCount,
                summary.chunkCount,
                summary.chunkTypes,
                packageBytes.length,
                glbBytes.length,
                out,
                assetPath
        );
    }


    public static RouteResult extractFileGlb(Context context, File packageFile, String outputName) throws IOException {
        byte[] packageBytes = readFileBytes(packageFile);
        SolumPackageReaderMvp reader = SolumPackageReaderMvp.open(packageBytes);
        SolumPackageReaderMvp.Summary summary = reader.fastSummary();

        if (!summary.hasGlbChunk || summary.glbBytes <= 0) {
            throw new IOException("ERR_SLPK_GLB_CHUNK_MISSING");
        }

        byte[] glbBytes = reader.readChunkBytes("GLB ");
        if (glbBytes.length < 4 || glbBytes[0] != 'g' || glbBytes[1] != 'l' || glbBytes[2] != 'T' || glbBytes[3] != 'F') {
            throw new IOException("ERR_SLPK_GLB_MAGIC_INVALID");
        }

        File dir = new File(context.getCacheDir(), "solum_slpk_route");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("ERR_SLPK_ROUTE_CACHE_UNAVAILABLE");
        }

        File out = new File(dir, outputName == null || outputName.isEmpty() ? "slpk_route_model.glb" : outputName);
        try (FileOutputStream stream = new FileOutputStream(out, false)) {
            stream.write(glbBytes);
            stream.flush();
        }

        return new RouteResult(true, "slpk_glb_extracted_to_cache", summary.packageName,
                summary.objectCount, summary.materialCount, summary.textureCount, summary.chunkCount,
                summary.chunkTypes, packageBytes.length, glbBytes.length, out, packageFile.getAbsolutePath());
    }

    private static byte[] readFileBytes(File file) throws IOException {
        long n = file.length();
        if (n > Integer.MAX_VALUE) throw new IOException("ERR_FILE_TOO_LARGE_FOR_MVP: " + n);
        byte[] out = new byte[(int)n];
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            int off = 0;
            while (off < out.length) {
                int read = in.read(out, off, out.length - off);
                if (read < 0) break;
                off += read;
            }
            if (off != out.length) throw new IOException("ERR_SHORT_READ");
        }
        return out;
    }

    private static byte[] readAssetBytes(Context context, String path) throws IOException {
        try (java.io.InputStream input = context.getAssets().open(path);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
