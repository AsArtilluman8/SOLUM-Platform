# VISUAL_DIAGNOSTICS_PACK — SOLUM visual evidence outputs

Этот документ фиксирует visual diagnostics pack для SOLUM.

## P01H status

P01H добавляет real companion route layer для:

- `final.png` через `AccessibilityService.takeScreenshot` на API >= 30;
- `ui_tree.json` через AccessibilityNode tree;
- `action_log.json`;
- `visual_diagnostics_manifest.json`.

Если screenshot недоступен, companion пишет `status=failed` и `reason` в manifest.

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
