# P10–P13 Render / Camera / Material Block Status

This branch is intentionally not merged after every sub-patch.

## Current block goal

```text
3D object
+ FPS/runtime diagnostics
+ debug sheet
+ camera/depth correctness
+ glTF material mapping foundation
+ future glTF import/PBR slice
```

## Completed in this block

```text
P10  — 3D object/depth/MVP path
P10B — FPS + runtime render diagnostics
P11  — camera/depth/material diagnostics schema
P12A — Debug Sheet + diagnostics save workflow
P12B — glTF/GLB material mapping foundation
```

## Merge policy

Do not merge to main until the block proves useful as a vertical renderer foundation.

Required before final merge:

```text
build OK
APK OK
ZIP OK
3D object OK
FPS baseline OK
runtime diagnostics useful
material path not fake
clear next step toward glTF/PBR
```
