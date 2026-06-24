package com.solum.engine;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Android-side SLPK cooker MVP.
 * Bridge only: existing GLB file -> SLPK with GLB chunk.
 */
public final class SolumAndroidPackageCookerMvp {
    private static final int HEADER_SIZE = 64;
    private static final int CHUNK_SIZE = 32;
    private static final int ALIGN = 64;
    private static final int FLAG_HAS_STRING_POOL = 1;
    private static final int FLAG_REQUIRED = 1;

    private SolumAndroidPackageCookerMvp() { }

    public static final class CookResult {
        public final File sourceGlb;
        public final File slpkFile;
        public final long sourceGlbBytes;
        public final long slpkBytes;
        public final String status;

        CookResult(File sourceGlb, File slpkFile, long sourceGlbBytes, long slpkBytes, String status) {
            this.sourceGlb = sourceGlb;
            this.slpkFile = slpkFile;
            this.sourceGlbBytes = sourceGlbBytes;
            this.slpkBytes = slpkBytes;
            this.status = status;
        }
    }

    private static final class StringPool {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Map<String, Integer> map = new LinkedHashMap<>();

        StringPool() {
            add("");
        }

        int add(String value) {
            if (value == null) value = "";
            Integer old = map.get(value);
            if (old != null) return old;
            int off = bytes.size();
            try {
                bytes.write(value.getBytes(StandardCharsets.UTF_8));
                bytes.write(0);
            } catch (IOException ignored) { }
            map.put(value, off);
            return off;
        }

        byte[] data() {
            return bytes.toByteArray();
        }
    }

    private static final class Chunk {
        final String type;
        final int flags;
        final byte[] data;
        final int nameOff;
        int offset;

        Chunk(String type, int flags, byte[] data, int nameOff) {
            this.type = type;
            this.flags = flags;
            this.data = data == null ? new byte[0] : data;
            this.nameOff = nameOff;
        }
    }

    public static CookResult cookGlbFile(Context context, File glbFile) throws IOException {
        if (glbFile == null || !glbFile.isFile()) throw new IOException("ERR_SOURCE_GLB_MISSING");
        String lower = glbFile.getName().toLowerCase(java.util.Locale.US);
        if (!lower.endsWith(".glb")) throw new IOException("ERR_SOURCE_NOT_GLB: " + glbFile.getName());

        byte[] glb = readFileBytes(glbFile);
        if (glb.length < 4 || glb[0] != 'g' || glb[1] != 'l' || glb[2] != 'T' || glb[3] != 'F') {
            throw new IOException("ERR_GLB_MAGIC_INVALID");
        }

        File outDir = new File("/storage/emulated/0/Download/SOLUM/packages");
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            outDir = new File(context.getFilesDir(), "solum_packages");
            if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IOException("ERR_PACKAGE_DIR_UNAVAILABLE");
        }

        String base = glbFile.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replaceAll("[^A-Za-z0-9_\\-]+", "_");
        if (base.isEmpty()) base = "imported_model";

        File slpk = new File(outDir, base + ".slpk");
        byte[] pkg = buildPackage(base, glbFile, glb);
        try (FileOutputStream stream = new FileOutputStream(slpk, false)) {
            stream.write(pkg);
            stream.flush();
        }

        return new CookResult(glbFile, slpk, glb.length, pkg.length, "android_glb_wrapped_to_slpk_mvp");
    }

    private static byte[] buildPackage(String packageName, File sourceGlb, byte[] glb) throws IOException {
        StringPool pool = new StringPool();
        int packageNameOff = pool.add(packageName);
        int authorOff = pool.add("SOLUM_ANDROID_COOKER_MVP");
        int sceneNameOff = pool.add("scene_one_android_imported_glb");
        int glbNameOff = pool.add(sourceGlb.getName());
        int matNameOff = pool.add("android_empty_material_payload");
        int texNameOff = pool.add("android_empty_texture_refs");
        int graphNameOff = pool.add("empty_graph");
        int dbgiNameOff = pool.add("android_cook_debug_info");
        int depsNameOff = pool.add("dependencies_empty");

        ByteBuffer mani = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN);
        mani.putInt(packageNameOff).putInt(authorOff).putInt(1).putInt(0).putInt(0).putInt(0).putInt(0);

        ByteBuffer scne = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
        scne.putInt(1);
        scne.putInt(pool.add("ImportedGLBRoot")).putInt(0).putInt(0).putInt(0);
        scne.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(1f).putFloat(1f).putFloat(1f);
        scne.putInt(0);

        byte[] mat = "{\"schema\":\"SolumAndroidMaterialPayload.v1\",\"materials\":[]}".getBytes(StandardCharsets.UTF_8);
        ByteBuffer tex = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        tex.putInt(0);
        ByteBuffer grph = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        grph.putShort((short)0).putShort((short)0).putInt(0).putInt(0).putInt(24).putInt(24).putInt(24);
        byte[] dbgi = ("{\"schema\":\"SolumAndroidCookDebug.v1\",\"sourceGlb\":\""
                + escapeJson(sourceGlb.getAbsolutePath()) + "\",\"note\":\"Android MVP GLB wrapper\"}")
                .getBytes(StandardCharsets.UTF_8);
        ByteBuffer deps = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        deps.putInt(0);

        ArrayList<Chunk> chunks = new ArrayList<>();
        chunks.add(new Chunk("MANI", FLAG_REQUIRED, mani.array(), packageNameOff));
        chunks.add(new Chunk("SCNE", FLAG_REQUIRED, scne.array(), sceneNameOff));
        chunks.add(new Chunk("GLB ", 0, glb, glbNameOff));
        chunks.add(new Chunk("MAT ", 0, mat, matNameOff));
        chunks.add(new Chunk("TEX ", 0, tex.array(), texNameOff));
        chunks.add(new Chunk("GRPH", 0, grph.array(), graphNameOff));
        chunks.add(new Chunk("DBGI", 0, dbgi, dbgiNameOff));
        chunks.add(new Chunk("DEPS", 0, deps.array(), depsNameOff));

        int dataStart = align(HEADER_SIZE + chunks.size() * CHUNK_SIZE);
        int offset = dataStart;
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        for (Chunk c : chunks) {
            int pad = offset - (dataStart + payload.size());
            writeZeros(payload, pad);
            c.offset = offset;
            payload.write(c.data);
            offset = align(offset + c.data.length);
        }

        int poolOffset = align(dataStart + payload.size());
        writeZeros(payload, poolOffset - (dataStart + payload.size()));
        byte[] poolBytes = pool.data();
        payload.write(poolBytes);
        int fileSize = poolOffset + poolBytes.length;

        ByteArrayOutputStream table = new ByteArrayOutputStream();
        for (Chunk c : chunks) {
            ByteBuffer e = ByteBuffer.allocate(CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            e.put(c.type.getBytes(StandardCharsets.US_ASCII));
            e.putShort((short)1);
            e.put((byte)0);
            e.put((byte)c.flags);
            e.putInt(c.offset);
            e.putInt(c.data.length);
            e.putInt(c.data.length);
            e.putInt(0); // chunk_hash32 omitted in Android MVP bridge; app reader does not validate hashes.
            e.putInt(c.nameOff);
            e.putInt(0);
            table.write(e.array());
        }

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(table.toByteArray());
        writeZeros(body, dataStart - HEADER_SIZE - table.size());
        body.write(payload.toByteArray());

        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        header.put("SLPK".getBytes(StandardCharsets.US_ASCII));
        header.putShort((short)HEADER_SIZE);
        header.putShort((short)1);
        header.putShort((short)CHUNK_SIZE);
        header.putShort((short)FLAG_HAS_STRING_POOL);
        header.putInt(chunks.size());
        header.putLong(HEADER_SIZE);
        header.putLong(poolOffset);
        header.putLong(poolBytes.length);
        header.putLong(fileSize);
        header.putLong(0); // content_hash64 omitted in Android MVP bridge; app reader does not validate hashes.
        header.putLong(0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.array());
        out.write(body.toByteArray());
        return out.toByteArray();
    }

    private static int align(int n) {
        return ((n + ALIGN - 1) / ALIGN) * ALIGN;
    }

    private static void writeZeros(ByteArrayOutputStream out, int count) {
        for (int i = 0; i < count; i++) out.write(0);
    }

    private static byte[] readFileBytes(File file) throws IOException {
        long n = file.length();
        if (n > Integer.MAX_VALUE) throw new IOException("ERR_GLB_TOO_LARGE_FOR_MVP: " + n);
        byte[] out = new byte[(int)n];
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            int off = 0;
            while (off < out.length) {
                int r = in.read(out, off, out.length - off);
                if (r < 0) break;
                off += r;
            }
            if (off != out.length) throw new IOException("ERR_SHORT_READ");
        }
        return out;
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
