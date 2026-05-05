# NOTE-0010: P13 glTF Import Probe + Texture Slot Diagnostics

## Decision

P13 remains a probe/diagnostics layer. It does not implement a fake PBR material.

## References

- glTF 2.0 material model
- Google Filament glTF/PBR behavior as correctness reference
- Khronos Vulkan Samples for later descriptor/image upload path
- The Forge as renderer resource lifetime reference only

## Why probe first

Before rendering PBR, SOLUM must know whether the source model actually contains the material slots we expect.

If the model looks wrong later, diagnostics must answer:

```text
texture missing?
wrong color space?
normal map missing?
metallicRoughness missing?
alpha mode mismatch?
mesh lacks normals/tangents/uv?
```

P13 creates that visibility.
