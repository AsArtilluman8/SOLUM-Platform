# SOLUM SLPK GLB Route MVP v1

Status: first visual bridge route.

## Goal

F5E proves the cooked package can participate in the Android engine route:

```text
Solum .slpk asset
→ SolumPackageReaderMvp
→ find GLB chunk
→ extract GLB bytes to cache file
→ existing Filament GLB load path
```

This is not yet the final native MESH route. It is the safe bridge route.

## Manual APK test

1. Install and open APK.
2. Open the Assets tab.
3. Press `Load SLPK Sample`.
4. Expected:
   - model loads through existing Filament GLB path;
   - Debug tab shows `SLPK route: slpk_glb_extracted_to_cache`;
   - `GLB loader: gltfio loaded=true`.
