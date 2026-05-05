# ANDROID_MODEL_IMPORT_WORKFLOW

## Goal

SOLUM must not force the user to manually place GLB files through Termux every time.

The app uses Android `ACTION_OPEN_DOCUMENT` to select a `.glb` or `.gltf` file from the phone file picker, then copies it into the SOLUM asset root.

## User flow

```text
long press HUD
↓
SOLUM Debug Sheet
↓
Import GLB/GLTF
↓
choose file from Android picker
↓
copy to SOLUMCreative/assets/models/imported
↓
write runtime_model_import_state.json
↓
export diagnostics ZIP
```

## Asset copy target

Preferred:

```text
/storage/emulated/0/SOLUMCreative/assets/models/imported/
```

Fallback:

```text
Android app external files / app private files
```

## Why copy instead of direct URI rendering

Direct `content://` URI rendering is fragile for native/Vulkan pipeline and future asset tools.

SOLUM should keep an engine-owned asset copy so later tools can re-open, validate, index and convert the asset without asking the user to find it again.

## Current status

P13B imports and persists model files as assets. It does not render imported GLB yet.

Next: glTF/GLB parser probe reads imported file and reports mesh/material/texture slots.
