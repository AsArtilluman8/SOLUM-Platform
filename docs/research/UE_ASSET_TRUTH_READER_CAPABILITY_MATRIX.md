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
| 18 Material Functions + 2 Materials (P58) | 20/20 package graphs | 971 expression nodes, 970 direct serialized links, 115 parameters, 34 function calls; expression collections and all targets validated | GRAPH VERIFIED; two Material shader-resource tails RAW_VERIFIED |
| 5 Curve assets (P58) | `CurveFloat`, `CurveVector`, `CurveLinearColor` | 14 rich-curve channels and 115 exact 27-byte keys with modes, tangents, offsets and hashes | VERIFIED |
| `3DCells_128_Sheet.uasset` (P58) | v2 trailer, Oodle FCompressedBuffer | TextureSource `TSCF_UEDELTA` inverse → BGRA8 PNG 16384×128; content ID, raw size and serialized channel extrema match | VERIFIED |
| `3DCells_64_Sheet.uasset` (P58) | v2 trailer, Oodle FCompressedBuffer | TextureSource `TSCF_UEDELTA` inverse → BGRA8 PNG 4096×64; content ID, raw size and serialized channel extrema match | VERIFIED |
| 3 editor StaticMesh assets (P58) | v2 trailers | `FMeshDescription` → GLB: 96, 408 and 576 vertex instances; 32, 136 and 192 triangles | VERIFIED |
| 6 `CloseThunder_*` SoundWave assets (P58) | inline editor bulk | Six stereo PCM16 44.1 kHz WAV files, 1,656,380 frames / 37.560 s total | VERIFIED |
| 174 Material-family assets (P59) | 112 graph packages, 59 Material Instances, 3 Material Parameter Collections | Exact expression/root inputs or instance/collection parameter contracts; object targets, overrides, static parameters and membership validated | VERIFIED |
| 38 texture assets (P59) | Texture2D, TextureCube, VolumeTexture; trailer and legacy bulk | 32 PNG, 1 Radiance HDR and 5 lossless NPY outputs; source dimensions/bytes, UEDELTA extrema and hash-derived Source.Id validated | VERIFIED |
| 20 Niagara scripts + 2 parameter collections (P59) | editor script/graph/native data | 20 exact script summaries; VM/executable hashes, variables, calls and dependencies; 106 collection defaults matched by type/offset/bytes | VERIFIED sub-contracts; other native tails RAW_VERIFIED |
| 23 Blueprint-family packages (P59) | 18 graph-bearing, 5 data-only | Exact graph/links and Kismet bytecode where serialized; empty data-only packages are not presented as missing graphs | VERIFIED / UNSUPPORTED graph as applicable |
| 8 Curve assets (P59) | rich-curve channels/keys | Finite, sorted keys with modes, values, tangents, offsets and hashes | VERIFIED |
| 9 StaticMesh assets (P59) | 7 trailer-backed, 2 disk-backed editor bulk | Nine real GLBs; positions, indices, normals, tangents when valid, UV/color and sections; 5,715 indices pass accessor/range validation | VERIFIED |
| 4 SoundWave + 1 MetaSound assets (P59) | legacy editor bulk / reference graph | Four exact mono PCM16 WAV outputs; MetaSound resolves 30 referenced audio assets without claiming embedded samples | VERIFIED |

The earlier 4,407,808-byte Sky and 6,007,808-byte Weather uploads are still correctly classified as truncated. The P56 rows refer only to the complete replacements identified by their hashes.

## Implemented format boundaries

- Classic UE4/UE5 package summary, names, imports, exports and split `.uexp` resolution.
- Tagged property values with raw provenance for unsupported native structures.
- UE5 `FPackageTrailer` versions 0–2 and locally stored `FCompressedBuffer` payloads.
- UE5 persistent `FEditorBulkData` metadata, flags, GUID, 20-byte payload IoHash, size and conditional file offset. Mesh bulk metadata must consume exactly 68 bytes and match exactly one trailer entry by both IoHash and raw size.
- Compression: None, LZ4 and external Oodle helper.
- UE5 new-format `FMeshDescription` attributes used by the verified fixtures, from trailer-backed and proven disk-backed editor payloads.
- Strict GLB 2.0 writing plus chunk, buffer, buffer-view, accessor min/max, attribute-count, index-range and triangle validation.
- Exact UE `UStruct`/FProperty/UFunction serialization and recursive UE 5.5 Kismet EX_* bytecode with dual-size validation.
- Editor TextureSource FEditorBulkData/legacy-bulk identity and size resolution; strict PNG/JPEG/HDR/NPY validation.
- Restricted one-mip UE TextureSource `TSCF_UEDELTA` inverse for G8/BGRA8/RGBA8/BGRE8, with serialized `LayerColorInfo` extrema and hash-derived `TextureSource.Id` validation.
- Lossless TextureCube/VolumeTexture and integer/half/float source export to Radiance HDR or NPY when a faithful PNG representation is unavailable.
- Legacy FByteBulkData metadata/address resolution and SoundWave/WAV metadata cross-checks.
- Bounded Ogg Vorbis/Opus page, sequence, EOS, codec-header and CRC validation when editor RawData contains an Ogg container.
- Current Niagara variable/type-handle serialization, container delta records, emitter handles, script summaries/hashes/dependencies and typed parameter-store defaults/bindings.
- Blueprint/K2 and Niagara serialized editor node/pin/link contracts, including unique pin IDs, owning-node checks, target resolution and reciprocal-link validation.
- Serialized Material/Material Function expression graphs, exact native material input structs, parameters, function calls, object references and collection membership.
- Rich-curve contracts for `CurveFloat`, `CurveVector` and `CurveLinearColor`, including legacy pre-UE5 inner `FPropertyTag` array elements.

## Not claimed

- Cooked `FStaticMeshLODResources` and every historical UE version.
- IoStore/Zen `.utoc/.ucas`, encrypted packages, virtualized remote payloads.
- Unversioned properties without matching engine version/schema (`.usmap`).
- General multi-mip UEDELTA, cooked BCn platform-mip decoding, streaming audio codecs, or material shader/HLSL decompilation without matching fixtures.
- Decompiled C++ from Blueprint/Niagara/MetaSound graphs.
