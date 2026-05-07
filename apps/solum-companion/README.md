# SOLUM Companion

`apps/solum-companion` is the Android Accessibility companion for SOLUM device evidence routes.

P01H implements real route methods for status, screenshot, UI tree, action log and visual manifest. It is still intentionally narrow and does not implement tap/gesture automation.

## Purpose

The companion will later provide controlled evidence for SOLUM apps only:

- screenshot capture;
- UI tree dump;
- action log writing;
- visual diagnostics manifest;
- controlled SOLUM launch later;
- controlled SOLUM force-stop later.

## Safety boundary

The companion must not:

- tap or gesture outside the SOLUM allowlist;
- automate Telegram UI;
- read secrets;
- change Vulkan/render/runtime state;
- install packages;
- write outside SOLUMCreative output paths.

Allowed packages:

```text
com.solum.engine
com.solum.launcher
com.solum.assethub
com.solum.materialstudio
com.asart.solum
```

Any non-allowlisted active package must return:

```text
status=blocked
reason=package_not_allowlisted
```

## Output paths

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

## P01H routes

Real:

```text
STATUS
CAPTURE_SCREENSHOT
DUMP_UI_TREE
WRITE_ACTION_LOG
BUILD_VISUAL_PACK
```

Stub/future only:

```text
LAUNCH_SOLUM_STUB
FORCE_STOP_SOLUM_STUB
```
