# VISUAL_DIAGNOSTICS_PACK — SOLUM visual evidence outputs

Этот документ фиксирует visual diagnostics pack для SOLUM.

## P01H status

P01H добавляет real companion route layer для:

- `final.png` через `AccessibilityService.takeScreenshot` на API >= 30;
- `ui_tree.json` через AccessibilityNode tree;
- `action_log.json`;
- `visual_diagnostics_manifest.json`.

Если screenshot недоступен, companion пишет `status=failed` и `reason` в manifest.

## P01I status

P01I подключает visual pack к реальной кнопке в SOLUM Companion:

```text
Run Visual Diagnostics
```

Кнопка:

- пишет `action_log.json`;
- пишет `ui_tree.json` из `rootInActiveWindow`;
- вызывает `AccessibilityService.takeScreenshot` на API >= 30;
- сохраняет `final.png` через SAF;
- обновляет `visual_diagnostics_manifest.json`.

Она не делает taps, gestures, launch/force-stop или arbitrary package automation.

## Output paths

Device agent outputs:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
```

Visual diagnostics outputs:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

SAF relative paths:

```text
device_agent/latest/action_log.json
device_agent/latest/ui_tree.json
diagnostics/latest/final.png
diagnostics/latest/visual_diagnostics_manifest.json
```

## P01H pack contents

Expected files:

```text
final.png
visual_diagnostics_manifest.json
ui_tree.json
action_log.json
```

Later flicker/visual regression tests may add:

```text
frame_001.png
frame_002.png
diff.png
```

Renderer-specific debug images may be specified separately when Vulkan diagnostics needs them.

## Safety

Visual evidence must be collected only for allowlisted SOLUM packages:

```text
com.solum.engine
com.solum.launcher
com.solum.assethub
com.solum.materialstudio
com.asart.solum
```

No Telegram UI automation, no secret reads, no arbitrary app automation.

Required failure reasons:

```text
accessibility_service_not_connected
screenshot_api_unavailable
screenshot_failed
saf_not_configured
package_not_allowlisted
```

Manifest fields:

```text
status
reason
activePackage
files.final
files.uiTree
files.actionLog
timestampUtc
```
