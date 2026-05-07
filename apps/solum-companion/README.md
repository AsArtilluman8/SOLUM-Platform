# SOLUM Companion

`apps/solum-companion` is the Android Accessibility companion for SOLUM device evidence routes.

P01H implements real route methods for status, screenshot, UI tree, action log and visual manifest. It is still intentionally narrow and does not implement tap/gesture automation.

P01H2 adds a normal launcher Activity so Android can open the installed companion APK from the app list.

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

## Install and open

Build or copy the debug APK:

```text
apps/solum-companion/build/outputs/apk/debug/solum-companion-debug.apk
```

Install it through Android package installer, adb, or wireless debugging. After install, Android should show the app as:

```text
SOLUM Companion
```

Open it from the launcher or from Android's Open button on the app details screen.

## Manual evidence test

Inside `SOLUM Companion`, press:

```text
Test Write Evidence Files
```

Expected files:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

This manual test does not capture a screenshot.

If the write fails, check Android storage restrictions for this local APK route and use the toast/error text as evidence.

## Enable Accessibility

From the companion screen, press:

```text
Open Accessibility Settings
```

Then enable:

```text
SOLUM Accessibility Companion
```

The launcher Activity does not bypass Android Accessibility consent. It only gives a direct path and evidence buttons.

## Restricted Settings blocker

If Android shows:

```text
Доступ к настройкам ограничен
```

Use:

```text
Settings -> Apps -> SOLUM Companion -> menu/dots -> Allow restricted settings
```

On some TECNO/HiOS builds this menu item can be hidden. If it is not available, install through adb or wireless debugging, then return to Accessibility Settings.

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
