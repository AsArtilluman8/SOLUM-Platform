# SOLUM Companion

`apps/solum-companion` is the P01G skeleton for the future Android Accessibility companion.

It is intentionally not connected to the Gradle build system in P01G. Real device actions are deferred to P01H.

## Purpose

The companion will later provide controlled evidence for SOLUM apps only:

- screenshot capture;
- UI tree dump;
- action log writing;
- controlled SOLUM launch;
- controlled SOLUM force-stop.

## Safety boundary

P01G contains stubs only. It must not:

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

## Planned output paths

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```
