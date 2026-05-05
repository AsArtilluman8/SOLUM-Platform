# KNOWN ISSUE — P04 Runtime Report Export

## Status

P04 Vulkan Foundation is successful.

The APK starts on the phone and reaches real Android Native Vulkan:

```text
Renderer path: Android Native Vulkan
GPU: Mali-G57 MC2
Type: INTEGRATED_GPU
API: 1.1.177
Swapchain: created
```

## What works

- Android APK build works.
- Native `libsolum_engine.so` build works.
- `libc++_shared.so` is packaged into APK.
- Vulkan links against Android system `libvulkan.so`, not Termux `libvulkan.so.1`.
- APK sees real Mali-G57 MC2 through Android runtime.
- Swapchain creation works.

## What does not work yet

Runtime report export into this project folder is not solved yet:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/
```

The APK can show runtime state on screen, but report files are not yet reliably exported to SOLUMCreative.

## Why

Android 13 blocks simple file sharing between apps.

Current paths tested:

```text
/data/user/0/com.solum.engine/files/solum_diagnostics
/storage/emulated/0/Android/data/com.solum.engine/files/solum_diagnostics
/storage/emulated/0/SOLUMCreative/diagnostics/latest
```

Observed behavior:

- App-private storage is writable by APK but not readable by Termux.
- App external data path is shown by APK, but Termux cannot reliably read it on this device.
- `run-as` is not usable from Termux on this device.
- Direct SOLUMCreative write needs a proper Android-side export flow.

## Decision

Do not block P04 merge on runtime report export.

P04 proves the core Vulkan foundation. Runtime report export becomes a separate diagnostics task.

## Required follow-up

Create a later diagnostics patch:

```text
Diagnostics Export v2
↓
In-app Export Diagnostics action
↓
User chooses/approves SOLUMCreative folder
↓
APK writes runtime reports/ZIP there
↓
Termux and future agents read one latest ZIP
```

## Terms

`Swapchain` = set of images Vulkan presents to the screen.

`Mali-G57 MC2` = real phone GPU.

`llvmpipe` = CPU software renderer. It is not the final engine GPU path.

`run-as` = Android debug command for reading app-private files. It is not reliable on this phone from Termux.

`Scoped storage` = Android file access restriction. One app cannot freely read another app folder.

`SAF` = Storage Access Framework. Android system folder picker. It is the clean future way for the app to get permission to write SOLUM diagnostics into a selected folder.
