# VISUAL_DIAGNOSTICS_PACK — SOLUM visual evidence outputs

Этот документ фиксирует future visual diagnostics pack для SOLUM. В P01G это только contract/skeleton, без real device action.

## P01G status

P01G добавляет companion skeleton и документирует стабильные output paths.

Не реализовано в P01G:

- real screenshot capture;
- real UI tree dump;
- real visual diff;
- renderer/Vulkan diagnostics integration;
- HTML dashboard generation.

Real screenshot/UI tree/device action входит в P01H.

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

## Future pack contents

Expected future files:

```text
final.png
visual_diagnostics_manifest.json
ui_tree.json
action_log.json
```

Later Vulkan/render diagnostics may add:

```text
shadow_mask.png
normals.png
depth.png
frame_001.png
frame_002.png
diff.png
```

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
