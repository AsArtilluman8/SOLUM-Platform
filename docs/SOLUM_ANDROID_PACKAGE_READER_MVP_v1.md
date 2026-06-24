# SOLUM Android Package Reader MVP v1

Status: Android-side foundation.

## Goal

F5D adds the first Android/engine-side `.slpk` reader foundation.

It does not yet connect GLB chunk to Filament `gltfio`. It only proves that Android app code can:

- open SLPK bytes;
- parse 64-byte header;
- parse 32-byte chunk table;
- read centralized string pool;
- read fast MANI summary;
- find `GLB ` chunk;
- extract chunk bytes for the next route step.

## Added class

`SolumPackageReaderMvp.java`

The class has no JSON dependency and no Filament dependency.

## Next step

F5E should route:

```text
SolumPackageReaderMvp.findChunk("GLB ")
SolumPackageReaderMvp.readChunkBytes("GLB ")
→ Filament/gltfio memory load route
```
