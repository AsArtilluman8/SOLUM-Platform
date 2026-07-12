# SOLUM UE Asset Truth Reader

Dependency-free Python 3.10+ inspector/extractor for classic UE4/UE5 `.uasset` and `.umap` packages.

## Commands

```bash
cd tools/ue_asset_tool
PYTHONPATH=src python3 -m ueassettool inspect Asset.uasset -o inspect.json
PYTHONPATH=src python3 -m ueassettool dump Asset.uasset --assets-only -o properties.json
PYTHONPATH=src python3 -m ueassettool graph Blueprint.uasset -o graph.json
PYTHONPATH=src python3 -m ueassettool bytecode Blueprint.uasset -o bytecode.json
PYTHONPATH=src python3 -m ueassettool extract Asset.uasset -d extracted -o manifest.json
PYTHONPATH=src python3 -m ueassettool verify Asset.uasset -o verify.json
PYTHONPATH=src python3 -m ueassettool export-mesh Mesh.uasset -o Mesh.glb --manifest mesh.json
PYTHONPATH=src python3 -m ueassettool export-texture Texture.uasset -o Texture.png --manifest texture.json
PYTHONPATH=src python3 -m ueassettool export-audio Sound.uasset -o Sound.wav --manifest audio.json
PYTHONPATH=src python3 -m ueassettool export-niagara System.uasset -o niagara.json
PYTHONPATH=src python3 -m ueassettool export-blueprint Blueprint.uasset -o blueprint.json
PYTHONPATH=src python3 -m ueassettool export-metasound Source.uasset -o metasound.json
```

The reader verifies package bounds, companions and UE5 package trailers; preserves unsupported native bytes; reconstructs serialized K2/Niagara node and pin links; decodes tagged properties and Niagara rich curves; and extracts bounded media payloads. K2 graph verification checks unique pin GUIDs, owning-node references, resolved targets and reciprocal links. The `bytecode` path follows UE 5.5 `UStruct::Serialize`, serialized `FProperty` fields, `FPropertyProxyArchive` field paths, `ScriptSerialization.h` expressions and the `UFunction` footer; it requires exact serialized-storage and loaded-VM byte counts and labels the result as Kismet bytecode, never decompiled C++.

UE5 editor `FMeshDescription` payloads can be exported to structurally checked GLB with positions, indices, normals, valid tangents, UVs, vertex colors and polygon-group primitives. `FEditorBulkData` metadata is decoded field-by-field and must match its payload by content identity and raw size. Editor TextureSource export resolves the declared `FCompressedBuffer`, checks its content hash/size and preserves PNG/JPEG or emits PNG for verified G8/BGRA8/RGBA8/G16/RGBA16 pixels. Legacy `FByteBulkData` metadata is decoded for editor SoundWave RawData and cross-checked against WAV channel/rate/sample/duration metadata. None/LZ4 are built in. Oodle requires an executable supplied through `UEASSET_OODLE_HELPER`; no GPL decoder is vendored into this Apache-2.0 repository.

## Truth policy

- Never emit placeholder GLB/FBX/PNG.
- Every output records source offsets, sizes, and SHA-256.
- Unsupported serialization stays raw/unknown.
- Unversioned packages require explicit version/schema support.
- Paid Marketplace assets and generated reports stay outside Git.

Validated locally against the maintainer-provided UDS sample set: package maps and package trailer, a real Nebula Sphere GLB (3,840 vertex instances / 1,280 triangles), a content-ID-bound 2048x2048 PNG with per-chunk CRC validation, and metadata-bound PCM WAV extraction. The Niagara contract for Dripping Curve verifies 3 emitter handles, 19 typed exposed parameters with decoded scalar defaults/object or data-interface bindings, and 273 graph nodes / 371 links. P56 complete Blueprints verify 12,149 Sky, 7,172 Weather and 901 Configuration Manager node pin streams with no duplicate IDs, owner mismatches, dangling links or asymmetric links. P57 additionally verifies all 991 serialized UFunctions (104,376 expressions): exact `UStruct` field boundaries, exact serialized script sizes, exact loaded VM sizes, in-range jumps, final `EX_EndOfScript`, and complete UFunction footers. The samples and derived full reports are intentionally not committed. Split packages remain `MISSING_INPUT` until their matching `.uexp`/`.ubulk` is present.
