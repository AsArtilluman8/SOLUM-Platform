# SOLUM UE Asset Truth Reader

Strict Python 3.10+ inspector/extractor for classic UE4/UE5 `.uasset` and `.umap` packages.

The verified decoders work with the Python standard library. Optional permissively licensed accelerators make large UEDELTA sources and BLAKE3 checks faster:

```bash
python3 -m pip install -e '.[performance]'
```

Without that extra, the reader uses its bounded pure-Python paths and produces the same hash-verified result.

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
PYTHONPATH=src python3 -m ueassettool export-material Material.uasset -o material.json
PYTHONPATH=src python3 -m ueassettool export-curve Curve.uasset -o curve.json
```

The reader verifies package bounds, companions and UE5 package trailers; preserves unsupported native bytes; reconstructs serialized K2/Niagara node and pin links; decodes tagged properties and Niagara rich curves; and extracts bounded media payloads. K2 graph verification checks unique pin GUIDs, owning-node references, resolved targets and reciprocal links. The `bytecode` path follows UE 5.5 `UStruct::Serialize`, serialized `FProperty` fields, `FPropertyProxyArchive` field paths, `ScriptSerialization.h` expressions and the `UFunction` footer; it requires exact serialized-storage and loaded-VM byte counts and labels the result as Kismet bytecode, never decompiled C++.

UE5 editor `FMeshDescription` payloads can be exported to structurally checked GLB with positions, indices, normals, valid tangents, UVs, vertex colors and polygon-group primitives. Both package-trailer payloads and the verified UE5.0/5.1 disk-backed `FEditorBulkData` layout are resolved by exact content identity and raw size. GLB validation checks every view/accessor, declared min/max, attribute count, index range and triangle primitive.

Editor TextureSource export resolves either `FCompressedBuffer` or legacy `FByteBulkData`, checks its declared content identity/size, and preserves PNG/JPEG or emits PNG, Radiance HDR or lossless NPY depending on the verified source format and dimensionality. PNG CRCs and bounded IDAT scanlines are validated. The bounded `TSCF_UEDELTA` inverse covers the supplied one-mip G8/BGRA8/RGBA8/BGRE8 fixtures and accepts a predictor split only when reconstructed extrema and the hash-derived `TextureSource.Id` prove it. Legacy editor SoundWave RawData is cross-checked against WAV channel/rate/sample/duration metadata; bounded Ogg Vorbis/Opus containers require valid pages, sequences, EOS and CRCs. `FCompressedBuffer` header CRC and raw BLAKE3 are checked. None/LZ4 and legacy chunked zlib are built in. Oodle requires an executable supplied through `UEASSET_OODLE_HELPER`; no decoder is vendored into this repository.

Material, Material Function, Material Instance and Material Parameter Collection export follows serialized objects rather than shader output: package-index identity, direct `FExpressionInput` links, root material inputs, parameters and overrides, static switches/masks, function inputs/outputs and calls, object references and expression-collection membership are validated and emitted with byte provenance. It is a graph/parameter contract, not HLSL decompilation. Curve export decodes `CurveFloat`, `CurveVector` and `CurveLinearColor` rich-curve channels and keys with exact key offsets, interpolation/tangent modes and finite/sorted checks.

Niagara contracts include serialized script summaries, executable/VM bytecode hashes, compile hashes, parameter variables/default stores, function calls, system/emitter references, renderer/data-interface objects and exact editor graph links when present. Blueprint output remains an exact graph and Kismet bytecode contract, never invented C++.

## Truth policy

- Never emit placeholder GLB/FBX/PNG.
- Every output records source offsets, sizes, and SHA-256.
- Unsupported serialization stays raw/unknown.
- Unversioned packages require explicit version/schema support.
- Paid Marketplace assets and generated reports stay outside Git.

Validated locally against the maintainer-provided UDS sample set: package maps and package trailer, real GLB geometry, content-ID-bound PNGs and metadata-bound PCM WAV extraction. The Niagara contract for Dripping Curve verifies 3 emitter handles, 19 typed exposed parameters with decoded scalar defaults/object or data-interface bindings, and 273 graph nodes / 371 links. P56 complete Blueprints verify 12,149 Sky, 7,172 Weather and 901 Configuration Manager node pin streams with no duplicate IDs, owner mismatches, dangling links or asymmetric links. P57 additionally verifies all 991 serialized UFunctions (104,376 expressions): exact `UStruct` field boundaries, exact serialized script sizes, exact loaded VM sizes, in-range jumps, final `EX_EndOfScript`, and complete UFunction footers.

P58 verifies 20/20 Material and Material Function graphs containing 971 expression nodes, 970 serialized links, 115 parameters and 34 function calls with zero invalid links. Five Curve assets verify 115 rich-curve keys. Three additional editor meshes export as GLB; six SoundWave assets export as metadata-matched stereo PCM WAV; and three Texture2D sources export as verified PNG. Two of those textures exercise trailer-backed UEDELTA source data at 16384x128 and 4096x64. The samples and derived full reports are intentionally not committed. Split packages remain `MISSING_INPUT` until their matching `.uexp`/`.ubulk` is present.

P59 broadens the proof set to 341 packages. It verifies all 174 Material-family assets (including 59 Material Instances and three Material Parameter Collections), exports all 38 Texture2D/TextureCube/VolumeTexture sources as PNG/HDR/NPY, verifies 20 Niagara script summaries plus two Niagara Parameter Collections containing 106 byte-matched defaults, verifies graph or truthful data-only status for 23 Blueprint-family packages, and verifies eight curve contracts. All nine StaticMesh fixtures produce real GLB geometry (5,715 validated indices); two use the legacy disk-backed editor-bulk layout. Four short mono PCM SoundWave sources export as exact WAV, and the MetaSound fixture resolves 30 referenced audio assets. Paid samples and derived reports remain outside Git.

## P59 UDS canonical truth dataset

The P59 pipeline writes private generated data outside Git and refuses to build
the frontend unless `EXTRACTION_GATE.json` is `PASSED`:

```bash
python3 tools/ue_asset_tool/scripts/build_and_serve_uds_truth.py --extract --extract-only
python3 tools/ue_asset_tool/scripts/build_and_serve_uds_truth.py
```

The default URL is `http://127.0.0.1:8765/SOLUM_UDS_FINAL_TRUTH_HTML/`.
Stop the server with `Ctrl-C`. The canonical dataset and frontend remain
separate under `/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH` and
`/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH_HTML`.

The HTML application loads inventory, asset contracts, graphs and verified
media lazily. It does not synthesize shaders, particles, transforms, models,
textures, sound or UDS runtime state.
