# SOLUM Cooker MVP v1

Status: MVP bridge layer.

## Goal

F5C connects two completed foundations:

- F5A: Solum Package `.slpk` reference writer/reader/validator.
- F5B: Solum Material Contract and glTF/GLB material converter.

The cooker MVP takes:

```text
GLB + SolumMaterial JSON
```

and writes:

```text
Solum Package .slpk
```

This is still a bridge format. It does not yet replace Filament `gltfio`.

## MVP package output

The cooker writes these chunks:

| Chunk | Meaning |
|---|---|
| `MANI` | package manifest counts |
| `SCNE` | one-object scene table |
| `GLB ` | raw GLB bridge blob |
| `MAT ` | canonical SolumMaterial JSON payload |
| `TEX ` | texture reference table extracted from materials |
| `GRPH` | empty graph placeholder |
| `DBGI` | cook debug info |
| `DEPS` | empty dependency table |

## What this patch does not do

This MVP intentionally does not add Android runtime reader, native mesh buffers, KTX2/ASTC texture transcoding, FBX/UE import, matc/filamat compilation, or material graph compilation.

## Next patch

F5D should integrate the package reader into Android/engine side enough to open `.slpk`, read stats, and prepare the GLB chunk route to Filament.
