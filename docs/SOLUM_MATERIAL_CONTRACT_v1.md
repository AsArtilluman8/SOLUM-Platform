# SOLUM Material Contract v1

Status: MVP contract. Not final ABI.

## Goal

SOLUM needs one stable material representation between imported assets and runtime rendering.

Source materials can come from:
- glTF / GLB PBR materials
- UE material instances and known texture slots
- future Solum material editor graphs
- future weather/UDS material modifiers

Runtime should not depend on raw source material graphs.

Pipeline:

```text
source material -> SolumMaterial -> Filament MaterialInstance
```

## MVP SolumMaterial fields

See generated `sample_solum_materials.json` for exact structure.

## Classification rules

- `alphaMode = BLEND` -> transparent
- `alphaMode = MASK` -> masked
- material name contains `glass`, `window`, `lens`, `crystal`, `visor` -> glass hint
- emissive texture or non-zero emissiveFactor -> emissive flag
- otherwise opaque

Glass hint does not force every alpha material to be glass. Foliage/cutout/leaf/grass must stay masked/opaque unless semantic material name says glass/window/lens/crystal/visor.

## UE material import policy MVP

UE material graph conversion is best-effort only.

Supported:
- known texture slots: BaseColor, Albedo, Diffuse, Normal, ORM, Occlusion, Roughness, Metallic, Emissive, Opacity
- scalar/vector parameters from material instances
- semantic name hints: glass/window/lens/crystal/foliage/leaf/grass/water

Unsupported in MVP:
- arbitrary UE material graph node compilation
- custom HLSL nodes
- material functions
- world-position tricks
- dynamic runtime parameter graphs

Unsupported nodes must create warnings, not fake correctness.

## Filament strategy MVP

Do not build the pipeline around custom `matc` per asset yet.

MVP uses stable built-in material templates:
- lit opaque
- lit masked
- lit transparent
- glass placeholder
- emissive

Cooker writes SolumMaterial params. Runtime creates Filament MaterialInstance and sets parameters.

Future:
- Solum material graph -> CI `matc` -> `FILM`/`FMAT` chunk if needed.
