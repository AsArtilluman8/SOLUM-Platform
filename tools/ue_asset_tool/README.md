# SOLUM UE Asset Truth Reader

Dependency-free Python 3.10+ inspector/extractor for classic UE4/UE5 `.uasset` and `.umap` packages.

## Commands

```bash
cd tools/ue_asset_tool
PYTHONPATH=src python3 -m ueassettool inspect Asset.uasset -o inspect.json
PYTHONPATH=src python3 -m ueassettool dump Asset.uasset --assets-only -o properties.json
PYTHONPATH=src python3 -m ueassettool graph Blueprint.uasset -o graph.json
PYTHONPATH=src python3 -m ueassettool extract Asset.uasset -d extracted -o manifest.json
PYTHONPATH=src python3 -m ueassettool verify Asset.uasset -o verify.json
PYTHONPATH=src python3 -m ueassettool export-mesh Mesh.uasset -o Mesh.glb --manifest mesh.json
PYTHONPATH=src python3 -m ueassettool export-texture Texture.uasset -o Texture.png --manifest texture.json
PYTHONPATH=src python3 -m ueassettool export-audio Sound.uasset -o Sound.wav --manifest audio.json
PYTHONPATH=src python3 -m ueassettool export-niagara System.uasset -o niagara.json
PYTHONPATH=src python3 -m ueassettool export-blueprint Blueprint.uasset -o blueprint.json
PYTHONPATH=src python3 -m ueassettool export-metasound Source.uasset -o metasound.json
```

The reader verifies package bounds, companions and UE5 package trailers; preserves unsupported native bytes; reconstructs serialized K2/Niagara node and pin links; decodes tagged properties and Niagara rich curves; and extracts bounded RIFF or UE5 FCompressedBuffer payloads. UE5 editor `FMeshDescription` payloads can be exported to structurally checked GLB with positions, indices, normals, valid tangents, UVs, vertex colors and polygon-group primitives. None/LZ4 are built in. Oodle requires an executable supplied through `UEASSET_OODLE_HELPER`; no GPL decoder is vendored into this Apache-2.0 repository.

## Truth policy

- Never emit placeholder GLB/FBX/PNG.
- Every output records source offsets, sizes, and SHA-256.
- Unsupported serialization stays raw/unknown.
- Unversioned packages require explicit version/schema support.
- Paid Marketplace assets and generated reports stay outside Git.

Validated locally against the maintainer-provided UDS sample set: package maps and package trailer, a real Nebula Sphere GLB (3,840 vertex instances / 1,280 triangles), Niagara curves/graph contract, a 2048x2048 PNG with per-chunk CRC validation, and PCM WAV extraction. The samples themselves are intentionally not committed. Split packages remain `MISSING_INPUT` until their matching `.uexp`/`.ubulk` is present.
