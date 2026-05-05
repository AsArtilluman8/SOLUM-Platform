# NOTE-0010: P13B Android GLB/GLTF Asset Import Picker

## Decision

Use Android file picker (`ACTION_OPEN_DOCUMENT`) for user-selected `.glb` / `.gltf` files.

Imported files are copied into SOLUM asset storage instead of being rendered directly from a temporary URI.

## Why this matters

The workflow must feel like an engine/editor:

```text
import once
↓
asset is registered
↓
reuse/swap/debug without searching again
```

## Scope

- Debug Sheet gets `Import GLB/GLTF`.
- App copies selected file to `SOLUMCreative/assets/models/imported` when writable.
- App writes `runtime_model_import_state.json`.
- App writes `imported_models_index.json` near imported assets.
- Export ZIP can include import state.

## Out of scope

- No GLB rendering yet.
- No PBR shader yet.
- No texture upload yet.
- No material preview yet.

## Next

P13C should parse the imported GLB/GLTF and report meshes/materials/textures before any PBR rendering.
