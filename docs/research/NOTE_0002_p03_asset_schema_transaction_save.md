# NOTE-0002: Patch P03 Asset Schema v1 + Transaction Save

## Problem

SOLUM needs one shared asset language before Asset Hub, Material Studio, Vulkan preview, and future multi-APK tools.

Without this, each app would write its own random JSON and later migrations would become painful.

## References studied

- SOLUM `ASSET_FORMAT_SPEC.md`.
- glTF 2.0 structure principles.
- tinygltf parser discipline.
- KTX2/BasisU future texture pipeline ideas.

## Adopted principles

- Asset is a folder plus `asset_manifest.json`.
- Every asset has `schema`, `schemaVersion`, `assetId`, `assetType`.
- All files are listed in `fileList`.
- All files have `sha256` hashes.
- Validator writes machine-readable `validation_report.json`.
- Save path uses transaction flow: `temp → backup → atomic replace → save_report.json`.

## Rejected parts

- Zip bundle as primary edit format in v1.
- Full dependency graph.
- Migration engine.
- Asset Hub UI.
- Material Studio UI.
- glTF import.

## SOLUM adaptation

Patch P03 creates Python tools that work directly in Termux:

- `tools/create_sample_asset.py`
- `tools/asset_validator.py`
- `tools/transaction_save.py`

This is not throwaway. These tools become the foundation for Asset Hub and future Studio apps.

## Diagnostics/tests

Run:

```bash
python3 tools/create_sample_asset.py
python3 tools/asset_validator.py /storage/emulated/0/SOLUMCreative/assets/materials/sample_material
```

Expected:

- `validation_report.json`
- `save_report.json`
- status = `valid`
