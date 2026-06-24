# SOLUM GLB Import to SLPK MVP v1

Status: user-visible import gate before UDS.

## Goal

Use a real imported GLB, wrap it into SOLUM `.slpk` on Android, then load the GLB chunk through the existing Filament route.

Bridge mode:

```text
real GLB file -> Android SLPK cooker -> .slpk -> GLB chunk -> cache .glb -> gltfio
```

## Manual test

1. Open APK.
2. Import a real visible GLB with existing import flow.
3. Press `Cook Active GLB -> SLPK`.
4. Model should reload and stay visible.
5. Debug -> `Export SLPK Debug ZIP`.
6. Send `/storage/emulated/0/Download/SOLUM_SLPK_DEBUG_LATEST.zip`.

Note: Android MVP bridge writes runtime-readable SLPK. Full xxHash validation for Android-cooked packages can be added later if needed.
