# UE Asset Truth Reader — verified fixture matrix

No fixture or derived full data dump is committed. Counts and hashes below come from local owner-provided files.

| Fixture | Package | Semantic result | Truth status |
|---|---|---|---|
| `Nebula_Sphere.uasset` | 8/8 exports, v2 trailer | `FMeshDescription` → GLB: 3,840 vertices, 1,280 triangles, normals, UV0, colors, one polygon group | VERIFIED |
| `Cloud_Wisps.uasset` | 3/3 exports | Oodle `FCompressedBuffer` → grayscale PNG 2048×2048; all PNG chunk CRCs valid | VERIFIED |
| `Dust_2.uasset` | 3/3 exports | RIFF/WAVE PCM16 mono 44.1 kHz, 28,792 bytes | VERIFIED |
| `Dripping_Curve.uasset` | 535/535 exports, package trailer | Niagara properties/curves/data interfaces plus 273 serialized graph nodes | VERIFIED/RAW_VERIFIED per export |
| `UDS_Close_Thunder.uasset` | 12/12 exports, package trailer | MetaSound properties/references: 12 exports, 25 imports; unknown frontend native bytes retained | RAW_VERIFIED |
| `DemoMap.umap` | 613/613 exports, package trailer | World/package/property contract | RAW_VERIFIED |
| `Dust.uasset` | 1,351/1,880 exports physically available | 529 declared payloads absent | TRUNCATED |
| `Configuration_Manager.uasset` (P56) | 1,278/1,278 exports | 901/901 K2 node pin streams, 2,755 unique pins, 991 links in 28 graphs; owner/GUID/reciprocity checks all zero-error | GRAPH VERIFIED; remaining native export tails RAW_VERIFIED |
| `Ultra_Dynamic_Sky.uasset` (P56, SHA-256 `9e4611e1…9c760`) | 13,298/13,298 exports | 12,149/12,149 K2 node pin streams, 37,657 unique pins, 12,436 links in 539 graphs; every target resolves and every link is reciprocal | GRAPH VERIFIED; remaining native export tails RAW_VERIFIED |
| `Ultra_Dynamic_Weather.uasset` (P56, SHA-256 `c5830ec5…6e9e`) | 8,045/8,045 exports | 7,172/7,172 K2 node pin streams, 21,878 unique pins, 7,307 links in 387 graphs; every target resolves and every link is reciprocal | GRAPH VERIFIED; remaining native export tails RAW_VERIFIED |

The earlier 4,407,808-byte Sky and 6,007,808-byte Weather uploads are still correctly classified as truncated. The P56 rows refer only to the complete replacements identified by their hashes.

## Implemented format boundaries

- Classic UE4/UE5 package summary, names, imports, exports and split `.uexp` resolution.
- Tagged property values with raw provenance for unsupported native structures.
- UE5 `FPackageTrailer` versions 0–2 and locally stored `FCompressedBuffer` payloads.
- UE5 persistent `FEditorBulkData` metadata, flags, GUID, 20-byte payload IoHash, size and conditional file offset. Mesh bulk metadata must consume exactly 68 bytes and match exactly one trailer entry by both IoHash and raw size.
- Compression: None, LZ4 and external Oodle helper.
- UE5 new-format `FMeshDescription` fixed-extent attributes used by the verified fixture.
- Strict GLB 2.0 writing and chunk/buffer/index/geometry validation.
- Self-contained PNG/JPEG/WAV/Ogg discovery; strict PNG and WAV validators.
- Blueprint/K2 and Niagara serialized editor node/pin/link contracts, including unique pin IDs, owning-node checks, target resolution and reciprocal-link validation.

## Not claimed

- Cooked `FStaticMeshLODResources` and every historical UE version.
- IoStore/Zen `.utoc/.ucas`, encrypted packages, virtualized remote payloads.
- Unversioned properties without matching engine version/schema (`.usmap`).
- BCn pixel decoding, streaming audio codecs, or material shader decompilation.
- Decompiled C++ from Blueprint/Niagara/MetaSound graphs.
