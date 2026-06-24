package com.solum.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SOLUM Package Reader MVP.
 *
 * Purpose:
 * - Android-side fast header/chunk-table reader for SLPK v1.
 * - No JSON runtime dependency.
 * - No Filament dependency.
 * - Does not yet load GLB to gltfio. That is the next route step.
 */
public final class SolumPackageReaderMvp {
    public static final int HEADER_SIZE = 64;
    public static final int CHUNK_ENTRY_SIZE = 32;
    public static final int ALIGNMENT = 64;
    public static final int CONTAINER_VERSION = 1;

    public static final int FLAG_HAS_STRING_POOL = 1;
    public static final int FLAG_REQUIRED = 1;

    private static final String MAGIC = "SLPK";

    private final byte[] data;
    private final Header header;
    private final List<Chunk> chunks;
    private final byte[] stringPool;

    public static final class Header {
        public final int headerSize;
        public final int containerVersion;
        public final int chunkEntrySize;
        public final int flags;
        public final int chunkCount;
        public final long chunkTableOffset;
        public final long stringPoolOffset;
        public final long stringPoolSize;
        public final long fileSize;
        public final long contentHash64;

        Header(int headerSize, int containerVersion, int chunkEntrySize, int flags, int chunkCount,
               long chunkTableOffset, long stringPoolOffset, long stringPoolSize, long fileSize,
               long contentHash64) {
            this.headerSize = headerSize;
            this.containerVersion = containerVersion;
            this.chunkEntrySize = chunkEntrySize;
            this.flags = flags;
            this.chunkCount = chunkCount;
            this.chunkTableOffset = chunkTableOffset;
            this.stringPoolOffset = stringPoolOffset;
            this.stringPoolSize = stringPoolSize;
            this.fileSize = fileSize;
            this.contentHash64 = contentHash64;
        }
    }

    public static final class Chunk {
        public final String type;
        public final int schemaVersion;
        public final int compression;
        public final int flags;
        public final long offset;
        public final long compressedSize;
        public final long uncompressedSize;
        public final long chunkHash32;
        public final long nameStringOffset;

        Chunk(String type, int schemaVersion, int compression, int flags, long offset,
              long compressedSize, long uncompressedSize, long chunkHash32, long nameStringOffset) {
            this.type = type;
            this.schemaVersion = schemaVersion;
            this.compression = compression;
            this.flags = flags;
            this.offset = offset;
            this.compressedSize = compressedSize;
            this.uncompressedSize = uncompressedSize;
            this.chunkHash32 = chunkHash32;
            this.nameStringOffset = nameStringOffset;
        }

        public boolean isRequired() {
            return (flags & FLAG_REQUIRED) != 0;
        }
    }

    public static final class Summary {
        public final String packageName;
        public final String author;
        public final int objectCount;
        public final int materialCount;
        public final int textureCount;
        public final int graphNodeCount;
        public final int graphLinkCount;
        public final int chunkCount;
        public final long fileSize;
        public final List<String> chunkTypes;
        public final boolean hasGlbChunk;
        public final long glbBytes;

        Summary(String packageName, String author, int objectCount, int materialCount, int textureCount,
                int graphNodeCount, int graphLinkCount, int chunkCount, long fileSize,
                List<String> chunkTypes, boolean hasGlbChunk, long glbBytes) {
            this.packageName = packageName;
            this.author = author;
            this.objectCount = objectCount;
            this.materialCount = materialCount;
            this.textureCount = textureCount;
            this.graphNodeCount = graphNodeCount;
            this.graphLinkCount = graphLinkCount;
            this.chunkCount = chunkCount;
            this.fileSize = fileSize;
            this.chunkTypes = Collections.unmodifiableList(chunkTypes);
            this.hasGlbChunk = hasGlbChunk;
            this.glbBytes = glbBytes;
        }

        public String toDebugLine() {
            return "SLPK " + objectCount + " objects, " + materialCount + " materials, "
                    + textureCount + " textures, chunks=" + chunkTypes
                    + ", GLB=" + (hasGlbChunk ? glbBytes + " bytes" : "none");
        }
    }

    private SolumPackageReaderMvp(byte[] data, Header header, List<Chunk> chunks, byte[] stringPool) {
        this.data = data;
        this.header = header;
        this.chunks = chunks;
        this.stringPool = stringPool;
    }

    public static SolumPackageReaderMvp open(File file) throws IOException {
        return open(readAllBytes(file));
    }

    public static SolumPackageReaderMvp open(byte[] bytes) throws IOException {
        if (bytes.length < HEADER_SIZE) {
            throw new IOException("ERR_TRUNCATED: file shorter than header");
        }

        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        byte[] magicBytes = new byte[4];
        b.get(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);
        if (!MAGIC.equals(magic)) {
            throw new IOException("ERR_BAD_MAGIC: " + magic);
        }

        int headerSize = u16(b);
        int version = u16(b);
        int entrySize = u16(b);
        int flags = u16(b);
        int chunkCount = b.getInt();
        long tableOffset = b.getLong();
        long stringPoolOffset = b.getLong();
        long stringPoolSize = b.getLong();
        long fileSize = b.getLong();
        long contentHash64 = b.getLong();
        b.getLong(); // reserved0

        if (headerSize != HEADER_SIZE) {
            throw new IOException("ERR_BAD_HEADER_SIZE: " + headerSize);
        }
        if (version > CONTAINER_VERSION) {
            throw new IOException("ERR_UNSUPPORTED_VERSION: " + version);
        }
        if (entrySize != CHUNK_ENTRY_SIZE) {
            throw new IOException("ERR_BAD_CHUNK_ENTRY_SIZE: " + entrySize);
        }
        if (fileSize != bytes.length) {
            throw new IOException("ERR_FILE_SIZE_MISMATCH: header=" + fileSize + " actual=" + bytes.length);
        }
        if (tableOffset != HEADER_SIZE) {
            throw new IOException("ERR_BAD_TABLE_OFFSET: " + tableOffset);
        }

        long tableEnd = tableOffset + (long) chunkCount * CHUNK_ENTRY_SIZE;
        if (tableEnd > bytes.length) {
            throw new IOException("ERR_TRUNCATED: chunk table out of file");
        }

        Header header = new Header(headerSize, version, entrySize, flags, chunkCount,
                tableOffset, stringPoolOffset, stringPoolSize, fileSize, contentHash64);

        List<Chunk> chunks = new ArrayList<>();
        boolean hasMani = false;
        boolean hasScne = false;

        for (int i = 0; i < chunkCount; i++) {
            int entryOffset = (int) tableOffset + i * CHUNK_ENTRY_SIZE;
            b.position(entryOffset);

            byte[] typeBytes = new byte[4];
            b.get(typeBytes);
            String type = new String(typeBytes, StandardCharsets.US_ASCII);

            int schema = u16(b);
            int compression = u8(b);
            int chunkFlags = u8(b);
            long offset = u32(b);
            long compressedSize = u32(b);
            long uncompressedSize = u32(b);
            long hash32 = u32(b);
            long nameStringOffset = u32(b);
            u32(b); // reserved

            if ((offset % ALIGNMENT) != 0) {
                throw new IOException("ERR_BAD_ALIGNMENT: " + type + " offset=" + offset);
            }
            if (offset + compressedSize > bytes.length) {
                throw new IOException("ERR_CHUNK_OOB: " + type + " offset=" + offset + " size=" + compressedSize);
            }
            if (compression != 0) {
                throw new IOException("ERR_UNSUPPORTED_COMPRESSION: " + type + " compression=" + compression);
            }

            if ("MANI".equals(type)) hasMani = true;
            if ("SCNE".equals(type)) hasScne = true;

            chunks.add(new Chunk(type, schema, compression, chunkFlags, offset, compressedSize,
                    uncompressedSize, hash32, nameStringOffset));
        }

        if (!hasMani) {
            throw new IOException("ERR_MISSING_CHUNK: MANI");
        }
        if (!hasScne) {
            throw new IOException("ERR_MISSING_CHUNK: SCNE");
        }

        if (stringPoolOffset + stringPoolSize > bytes.length) {
            throw new IOException("ERR_STRING_POOL_OOB");
        }

        byte[] pool = new byte[(int) stringPoolSize];
        System.arraycopy(bytes, (int) stringPoolOffset, pool, 0, (int) stringPoolSize);

        return new SolumPackageReaderMvp(bytes, header, chunks, pool);
    }

    public Header header() {
        return header;
    }

    public List<Chunk> chunks() {
        return Collections.unmodifiableList(chunks);
    }

    public Chunk findChunk(String type) {
        for (Chunk c : chunks) {
            if (c.type.equals(type)) {
                return c;
            }
        }
        return null;
    }

    public byte[] readChunkBytes(String type) throws IOException {
        Chunk c = findChunk(type);
        if (c == null) {
            throw new IOException("ERR_MISSING_CHUNK: " + type);
        }
        if (c.compressedSize > Integer.MAX_VALUE) {
            throw new IOException("ERR_CHUNK_TOO_LARGE: " + type);
        }
        byte[] out = new byte[(int) c.compressedSize];
        System.arraycopy(data, (int) c.offset, out, 0, out.length);
        return out;
    }

    public Summary fastSummary() throws IOException {
        Chunk mani = findChunk("MANI");
        if (mani == null) {
            throw new IOException("ERR_MISSING_CHUNK: MANI");
        }
        if (mani.compressedSize < 28) {
            throw new IOException("ERR_BAD_MANI_SIZE: " + mani.compressedSize);
        }

        ByteBuffer m = ByteBuffer.wrap(data, (int) mani.offset, (int) mani.compressedSize).order(ByteOrder.LITTLE_ENDIAN);

        long packageNameOff = u32(m);
        long authorOff = u32(m);
        int objectCount = m.getInt();
        int materialCount = m.getInt();
        int textureCount = m.getInt();
        int graphNodeCount = m.getInt();
        int graphLinkCount = m.getInt();

        ArrayList<String> types = new ArrayList<>();
        for (Chunk c : chunks) {
            types.add(c.type);
        }

        Chunk glb = findChunk("GLB ");
        boolean hasGlb = glb != null;

        return new Summary(
                readString((int) packageNameOff),
                readString((int) authorOff),
                objectCount,
                materialCount,
                textureCount,
                graphNodeCount,
                graphLinkCount,
                chunks.size(),
                header.fileSize,
                types,
                hasGlb,
                hasGlb ? glb.compressedSize : 0
        );
    }

    public String readString(int offset) {
        if (offset < 0 || offset >= stringPool.length) {
            return "";
        }
        int end = offset;
        while (end < stringPool.length && stringPool[end] != 0) {
            end++;
        }
        return new String(stringPool, offset, end - offset, StandardCharsets.UTF_8);
    }

    private static byte[] readAllBytes(File file) throws IOException {
        long n = file.length();
        if (n > Integer.MAX_VALUE) {
            throw new IOException("ERR_FILE_TOO_LARGE_FOR_MVP: " + n);
        }
        byte[] out = new byte[(int) n];
        try (FileInputStream in = new FileInputStream(file)) {
            int off = 0;
            while (off < out.length) {
                int r = in.read(out, off, out.length - off);
                if (r < 0) break;
                off += r;
            }
            if (off != out.length) {
                throw new IOException("ERR_SHORT_READ");
            }
        }
        return out;
    }

    private static int u8(ByteBuffer b) {
        return b.get() & 0xff;
    }

    private static int u16(ByteBuffer b) {
        return b.getShort() & 0xffff;
    }

    private static long u32(ByteBuffer b) {
        return b.getInt() & 0xffffffffL;
    }
}
