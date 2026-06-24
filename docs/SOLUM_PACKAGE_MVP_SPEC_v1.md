# SOLUM Package MVP Spec v1

Status: MVP reference spec. Not final ABI.

## Goal

SOLUM runtime should not parse JSON as its main runtime format.

Source formats:
- GLB/GLTF
- FBX
- PNG/JPG/TGA
- WAV/OGG
- editor node graphs
- material graphs
- UI animation graphs
- future UDS/weather graphs

are imported by Solum Cooker and converted into a cooked Solum package.

Runtime package:
- fast to open
- low allocations
- validates safely
- readable through tools/UI/solumdump
- does not require Filament fork

## File layout

```text
HEADER 64 bytes
CHUNK_TABLE n * 32 bytes
padding to 64
CHUNKS aligned 64 bytes
STRING_POOL
```

## Header v1

64 bytes, little endian.

```c
struct SolumPackageHeaderV1 {
    char     magic[4];              // "SLPK"
    uint16_t header_size;           // 64
    uint16_t container_version;     // 1
    uint16_t chunk_entry_size;      // 32
    uint16_t flags;                 // bit0 = has string pool
    uint32_t chunk_count;
    uint64_t chunk_table_offset;    // normally 64
    uint64_t string_pool_offset;
    uint64_t string_pool_size;
    uint64_t file_size;
    uint64_t content_hash64;        // xxHash64 of bytes from chunk_table_offset to EOF
    uint64_t reserved0;             // zero / future extension
};
```

## Chunk entry v1

32 bytes.

```c
struct SolumChunkEntryV1 {
    char     type[4];               // "MANI", "SCNE", "GLB ", "MESH", etc.
    uint16_t schema_version;
    uint8_t  compression;           // 0 none, 1 lz4, 2 zstd, 3 zlib legacy-read-only
    uint8_t  flags;                 // bit0 REQUIRED
    uint32_t offset;                // v1 <=4GB package
    uint32_t compressed_size;
    uint32_t uncompressed_size;
    uint32_t chunk_hash32;          // xxHash32 of payload bytes
    uint32_t name_string_offset;    // optional name in centralized string pool
    uint32_t reserved0;
};
```

## Chunk types

| Type | Meaning | Required |
|---|---|---|
| `MANI` | manifest/package metadata not duplicated in header | yes |
| `SCNE` | scene/object table | yes |
| `GLB ` | raw GLB bridge chunk for v1 | no |
| `MESH` | future native Solum mesh buffers | no |
| `MAT ` | material params/tables | no |
| `TEX ` | texture/KTX metadata/blob | no |
| `AUD ` | audio blob | no |
| `GRPH` | graph index + payload blob | no |
| `ANIM` | animation clips | no |
| `DBGI` | debug/source/cook info | no |
| `DEPS` | dependency table, empty in MVP | no |

## Versioning

- `container_version > supported`: hard error.
- Required chunk with `schema_version > known`: hard error.
- Optional unknown chunk: skip + warning.
- Optional unsupported schema: skip + warning.
- Chunk type names must be mapped through constants/registry, not scattered string comparisons.

## Alignment

- Every chunk payload starts at offset divisible by 64.
- Future native MESH internal arrays:
  - vertex data align 64
  - index data align 4 or 16
  - skin weights align 16

## String pool

- Centralized UTF-8 null-terminated string pool.
- References use uint32 offsets into the pool.
- Cooker deduplicates strings.
- Runtime keeps pool alive for package lifetime.
- Runtime should prefer numeric ids; name lookup table can be added later.

## Compression MVP

The enum exists, but MVP writer uses compression `none`.

Later:
- LZ4 for hot/low-latency metadata
- ZSTD for cold/debug chunks
- KTX2/ASTC/ETC2 for textures
- meshoptimizer compression for future MESH data

## MVP test gate

Minimum tests:
1. valid package opens
2. bad magic gives error
3. truncated file gives error
4. missing required SCNE gives error
5. chunk out of bounds gives error
6. bad alignment gives error
7. chunk hash mismatch gives error
8. unsupported container version gives error
9. unknown optional chunk skips with warning
10. unsupported required schema gives error
11. dump summary equals expected
12. string pool dedup works
13. graph payload roundtrip counts
14. quick summary read speed
