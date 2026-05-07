# SOLUM Companion

`apps/solum-companion` is the Android Accessibility companion for SOLUM device evidence routes.

P01H implements real route methods for status, screenshot, UI tree, action log and visual manifest. It is still intentionally narrow and does not implement tap/gesture automation.

P01H2 adds a normal launcher Activity so Android can open the installed companion APK from the app list.
P01H3 adds SAF folder permission for Android scoped storage so targetSdk 34 can write evidence files into the chosen `SOLUMCreative` folder.
P01I adds the real `Run Visual Diagnostics` launcher button. It bridges to the enabled Accessibility service, writes `action_log.json`, writes `ui_tree.json`, captures `final.png` through `AccessibilityService.takeScreenshot` on API >= 30, and updates `visual_diagnostics_manifest.json` through SAF.

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
com.solum.companion
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
Choose SOLUMCreative Output Folder
```

In the Android folder picker, select:

```text
/storage/emulated/0/SOLUMCreative
```

Then press:

```text
Test Write Evidence Files
```

Expected files:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

This manual test does not capture a screenshot.

Expected toast after SAF setup:

```text
Evidence files written via SAF
```

If no folder is selected, the app tries the legacy direct path as a fallback. On targetSdk 34 this can fail with:

```text
direct_public_storage_failed_choose_output_folder
```

In that case, choose the output folder through SAF and run the test again.

Use `Clear Output Folder Permission` only when you need to reset the persisted SAF permission.

## Run visual diagnostics

Before running visual diagnostics:

```text
Choose SOLUMCreative Output Folder
Enable SOLUM Accessibility Companion
Open an allowlisted SOLUM app, or keep SOLUM Companion open for self-test
Run Visual Diagnostics
```

Expected files through SAF:

```text
device_agent/latest/action_log.json
device_agent/latest/ui_tree.json
diagnostics/latest/final.png
diagnostics/latest/visual_diagnostics_manifest.json
```

The screen shows:

```text
Accessibility service: enabled / disabled / unknown
SAF output folder: configured / not configured
Last visual diagnostics: ok / partial / failed
```

Failure reasons are explicit:

```text
accessibility_service_not_connected
screenshot_api_unavailable
screenshot_failed
saf_not_configured
package_not_allowlisted
```

No taps, gestures, app launch automation, package force-stop, or Telegram UI automation are performed.

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
RUN_VISUAL_DIAGNOSTICS
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
