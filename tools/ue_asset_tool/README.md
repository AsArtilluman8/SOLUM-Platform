# SOLUM UE Asset Truth Reader

Dependency-free Python 3.10+ inspector/extractor for classic UE4/UE5 `.uasset` and `.umap` packages.

## Commands

```bash
cd tools/ue_asset_tool
PYTHONPATH=src python3 -m ueassettool inspect Asset.uasset -o inspect.json
PYTHONPATH=src python3 -m ueassettool dump Asset.uasset --assets-only -o properties.json
PYTHONPATH=src python3 -m ueassettool graph Blueprint.uasset -o graph.json
PYTHONPATH=src python3 -m ueassettool extract Asset.uasset -d extracted -o manifest.json
```

The reader verifies package bounds, preserves unsupported native bytes, reconstructs serialized K2 node/pin links, decodes tagged properties and Niagara rich curves, and extracts bounded RIFF or UE5 FCompressedBuffer payloads. None/LZ4 are built in. Oodle requires an executable supplied through `UEASSET_OODLE_HELPER`; no GPL decoder is vendored into this Apache-2.0 repository.

## Truth policy

- Never emit placeholder GLB/FBX/PNG.
- Every output records source offsets, sizes, and SHA-256.
- Unsupported serialization stays raw/unknown.
- Unversioned packages require explicit version/schema support.
- Paid Marketplace assets and generated reports stay outside Git.

Validated locally against the maintainer-provided UDS sample set: package maps, Blueprint graph links, Niagara curves, a 2048x2048 PNG inside Oodle FCompressedBuffer, and PCM WAV extraction. The samples themselves are intentionally not committed.
