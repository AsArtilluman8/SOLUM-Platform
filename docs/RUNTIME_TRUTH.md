# RUNTIME_TRUTH — SOLUM runtime paths

Этот документ фиксирует, где находится runtime truth для Vulkan/render работы.

## Apps

```text
main runtime app: apps/engine
companion app: apps/solum-companion
```

`apps/engine` — главный SOLUM Engine runtime. Renderer diagnostics должны идти из engine.

`apps/solum-companion` — device evidence companion. Он помогает собрать visual pack, но не является источником renderer truth.

## APK output

APK output root:

```text
/storage/emulated/0/Download/SOLUM_APK
```

Explicit APK paths:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM-engine-debug.apk
/storage/emulated/0/Download/SOLUM_APK/SOLUM-companion-debug.apk
```

Aliases:

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk
/storage/emulated/0/Download/SOLUM_APK/SOLUM_COMPANION_LATEST.apk
```

Нельзя использовать один общий `SOLUM_LATEST.apk`, потому что он скрывает, какой APK реально собран.

## Diagnostics root

Preferred diagnostics root:

```text
/storage/emulated/0/SOLUMCreative
```

Engine diagnostics latest:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest
```

Engine asset import root:

```text
/storage/emulated/0/SOLUMCreative/assets/models/imported/
```

Engine app exposes:

```text
Choose Diagnostics Folder
Export Engine Diagnostics
```

Preferred route:

```text
ACTION_OPEN_DOCUMENT_TREE -> user chooses /storage/emulated/0/SOLUMCreative -> persisted SAF read/write permission
```

Export writes:

```text
diagnostics/latest/engine_runtime_state.json
diagnostics/latest/engine_diagnostics_manifest.json
diagnostics/latest/model_import_state.json
diagnostics/latest/asset_report.json
```

Route order:

```text
saf -> direct public path -> app-specific fallback -> failed
```

Manifest must state:

```text
exportStatus
exportRoute
actualRoot
reason
screenshot.status = not_available
screenshot.reason = renderer_readback_not_implemented
```

Engine screen must show visible diagnostics status. Toast is not enough.

P05 model import status must show:

```text
Import GLB
Scan Models
Import: OK/FAILED/not run
Active model: name or none
GPU Upload: not implemented
Draw Model: not implemented
```

If direct public storage is blocked by Android storage rules, engine writes app-specific fallback and manifest/status panel must show the exact route and reason.

## Visual pack

Companion visual pack paths:

```text
diagnostics/latest/final.png
device_agent/latest/ui_tree.json
device_agent/latest/action_log.json
diagnostics/latest/visual_diagnostics_manifest.json
```

Visual pack полезен для UI evidence. Для Vulkan/render truth основным источником остаются engine-native diagnostics:

```text
diagnostics/latest/engine_runtime_state.json
diagnostics/latest/engine_diagnostics_manifest.json
```
