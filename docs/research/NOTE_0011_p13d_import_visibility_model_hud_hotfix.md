# NOTE-0011: P13D Import Visibility + Model HUD Hotfix

## Problem

User imported a GLB through the Android picker, but the diagnostics audit still reported:

```text
waiting_for_sample_gltf_or_glb
modelFilesFound: 0
```

This made it unclear whether the model was copied, where it was copied, or whether the audit searched the wrong path.

## Fix

- Persist last imported model name/path in Android SharedPreferences.
- Show active model name on the compact HUD.
- Debug Sheet shows model name and asset path.
- Save Runtime State no longer overwrites import state with `none`.
- Runtime writes `runtime_model_files.json` with the visible model directory listing.
- Audit tool can use runtime import state as fallback model source.

## Expected UX

```text
Import GLB/GLTF
↓
HUD shows Model: filename.glb
↓
Save runtime state
↓
ZIP contains runtime_model_import_state.json and runtime_model_files.json
↓
Audit can find the model
```
