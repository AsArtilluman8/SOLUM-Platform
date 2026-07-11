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
| `Ultra_Dynamic_Sky.uasset` | physical EOF 4,407,808; declared `TotalHeaderSize` 15,199,227 | file ends before every export and before its own declared header boundary; complete original `.uasset` required | TRUNCATED |
| `Ultra_Dynamic_Weather.uasset` | physical EOF 6,007,808; declared `TotalHeaderSize` 9,207,558 | file ends before every export and before its own declared header boundary; complete original `.uasset` required | TRUNCATED |

## Implemented format boundaries

- Classic UE4/UE5 package summary, names, imports, exports and split `.uexp` resolution.
- Tagged property values with raw provenance for unsupported native structures.
- UE5 `FPackageTrailer` versions 0–2 and locally stored `FCompressedBuffer` payloads.
- Compression: None, LZ4 and external Oodle helper.
- UE5 new-format `FMeshDescription` fixed-extent attributes used by the verified fixture.
- Strict GLB 2.0 writing and chunk/buffer/index/geometry validation.
- Self-contained PNG/JPEG/WAV/Ogg discovery; strict PNG and WAV validators.
- Blueprint/K2 and Niagara serialized editor node/pin/link contracts.

## Not claimed

- Cooked `FStaticMeshLODResources` and every historical UE version.
- IoStore/Zen `.utoc/.ucas`, encrypted packages, virtualized remote payloads.
- Unversioned properties without matching engine version/schema (`.usmap`).
- BCn pixel decoding, streaming audio codecs, or material shader decompilation.
- Decompiled C++ from Blueprint/Niagara/MetaSound graphs.
