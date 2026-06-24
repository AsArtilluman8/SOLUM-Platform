# SOLUM SLPK Inspector MVP v1

Status: readable runtime debug/export layer.

## Goal

F5F avoids screenshot-based verification. The app can export a compact SLPK debug ZIP after the user presses `Load SLPK Sample`.

Runtime route already proven by F5E:

```text
.slpk asset → SolumPackageReaderMvp → GLB chunk → cache .glb → Filament/gltfio
```

F5F adds:

- `Export SLPK Debug ZIP` button in Debug tab;
- `/storage/emulated/0/Download/SOLUM_SLPK_DEBUG_LATEST.zip`;
- text report with route state;
- JSON summary with chunk/material/texture counts;
- bundled sample `.slpk` copied into the zip;
- extracted cached `.glb` copied into the zip when present.

## Manual test

1. Open APK.
2. Assets tab → `Load SLPK Sample`.
3. Debug tab → `Export SLPK Debug ZIP`.
4. Send `/storage/emulated/0/Download/SOLUM_SLPK_DEBUG_LATEST.zip`.

No need to send many screenshots.
