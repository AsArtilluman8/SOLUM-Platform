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

Если direct public storage недоступен Android app, engine пишет в app-specific fallback и manifest обязан указать exact reason.

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
