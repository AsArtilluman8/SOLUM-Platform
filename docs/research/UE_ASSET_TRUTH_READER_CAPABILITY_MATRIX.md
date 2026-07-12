# UE Asset Truth Reader — verified fixture matrix

No fixture or derived full data dump is committed. Counts and hashes below come from local owner-provided files.

| Fixture | Package | Semantic result | Truth status |
|---|---|---|---|
| `Nebula_Sphere.uasset` | 8/8 exports, v2 trailer | `FMeshDescription` → GLB: 3,840 vertices, 1,280 triangles, normals, UV0, colors, one polygon group | VERIFIED |
| `Cloud_Wisps.uasset` | 3/3 exports | Exact TextureSource strip/native suffix + 56-byte `FEditorBulkData`; content ID/raw size-bound Oodle `FCompressedBuffer` → grayscale PNG 2048×2048; all chunk CRCs valid | VERIFIED |
| `Dust_2.uasset` | 3/3 exports | Exact SoundWave native suffix + 20-byte `FByteBulkData`; RIFF/WAVE PCM16 mono 44.1 kHz, 14,374 frames / 28,792 bytes | VERIFIED |
| `Dripping_Curve.uasset` | 535/535 exports, package trailer | 3 emitter handles; 19 typed exposed parameters/defaults/bindings; curves/data interfaces; 273 nodes, 1,051 pins and 371 reciprocal graph links | VERIFIED/RAW_VERIFIED per export |
| `UDS_Close_Thunder.uasset` | 12/12 exports, package trailer | MetaSound properties/references: six external SoundWave assets identified; no embedded waveform claimed; unknown frontend native bytes retained | RAW_VERIFIED |
| `DemoMap.umap` | 613/613 exports, package trailer | World/package/property contract | RAW_VERIFIED |
| `Dust.uasset` | 1,351/1,880 exports physically available | 529 declared payloads absent | TRUNCATED |
| `Configuration_Manager.uasset` (P56) | 1,278/1,278 exports | Existing 901-node graph verification plus 34/34 UFunctions and 5,286 exact serialized EX_* expressions | GRAPH + BYTECODE VERIFIED; other native tails RAW_VERIFIED |
| `Ultra_Dynamic_Sky.uasset` (P56, SHA-256 `9e4611e1…9c760`) | 13,298/13,298 exports | Existing 12,149-node graph verification plus 538/538 UFunctions and 61,297 exact serialized EX_* expressions | GRAPH + BYTECODE VERIFIED; other native tails RAW_VERIFIED |
| `Ultra_Dynamic_Weather.uasset` (P56, SHA-256 `c5830ec5…6e9e`) | 8,045/8,045 exports | Existing 7,172-node graph verification plus 419/419 UFunctions and 37,793 exact serialized EX_* expressions | GRAPH + BYTECODE VERIFIED; other native tails RAW_VERIFIED |

The earlier 4,407,808-byte Sky and 6,007,808-byte Weather uploads are still correctly classified as truncated. The P56 rows refer only to the complete replacements identified by their hashes.

## Implemented format boundaries

- Classic UE4/UE5 package summary, names, imports, exports and split `.uexp` resolution.
- Tagged property values with raw provenance for unsupported native structures.
- UE5 `FPackageTrailer` versions 0–2 and locally stored `FCompressedBuffer` payloads.
- UE5 persistent `FEditorBulkData` metadata, flags, GUID, 20-byte payload IoHash, size and conditional file offset. Mesh bulk metadata must consume exactly 68 bytes and match exactly one trailer entry by both IoHash and raw size.
- Compression: None, LZ4 and external Oodle helper.
- UE5 new-format `FMeshDescription` fixed-extent attributes used by the verified fixture.
- Strict GLB 2.0 writing and chunk/buffer/index/geometry validation.
- Exact UE `UStruct`/FProperty/UFunction serialization and recursive UE 5.5 Kismet EX_* bytecode with dual-size validation.
- Editor TextureSource FEditorBulkData identity/size resolution and strict PNG/JPEG validation.
- Legacy FByteBulkData metadata/address resolution and SoundWave/WAV metadata cross-checks.
- Current Niagara variable/type-handle serialization, container delta records, emitter handles and typed parameter-store defaults/bindings.
- Blueprint/K2 and Niagara serialized editor node/pin/link contracts, including unique pin IDs, owning-node checks, target resolution and reciprocal-link validation.

## Not claimed

- Cooked `FStaticMeshLODResources` and every historical UE version.
- IoStore/Zen `.utoc/.ucas`, encrypted packages, virtualized remote payloads.
- Unversioned properties without matching engine version/schema (`.usmap`).
- Cooked BCn platform-mip decoding, streaming audio codecs, or material shader decompilation without matching cooked fixtures.
- Decompiled C++ from Blueprint/Niagara/MetaSound graphs.
