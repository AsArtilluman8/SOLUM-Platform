# NOTE-0009: P11 Camera + Depth + Material Diagnostics

## Decision

Do not start glTF/PBR materials until camera/depth/FPS diagnostics exist.

## Reference policy

- glTF 2.0: future material mapping source of truth.
- Filament: REFERENCE_ONLY for material correctness/color-space expectations.
- Khronos Vulkan Samples: REFERENCE_ONLY for camera/depth/pipeline patterns.
- ARM Mali: REFERENCE_ONLY for depth/bandwidth concerns.

## Scope

- Camera state baseline.
- Better material-readiness diagnostics.
- Render/model/material JSON files.
- FPS/frame time remains visible.

## Out of scope

- GLB importer.
- PBR shader.
- Texture loading.
- Normal/tangent reconstruction.

## Next

P12: glTF/GLB material import gate with real standard mapping, not fake baseColor-only material.
