# NOTE-0013: P14 Native GLB Mesh Upload Pack

P14 renders real GLB mesh geometry through SOLUM's current Vulkan MeshResource path.

## Scope

- Python GLB/glTF mesh cache builder for diagnostics and first runtime bridge.
- `SOLMESH1` binary cache with expanded `Vertex3D` triangles.
- Native `MeshResource` loads `active_mesh_v1.bin` if available.
- Fallback cube remains if no cache exists.
- Materials/textures remain diagnostics-only.

## Not final yet

This is not the final cgltf native dependency path. It is the first real geometry upload path using a stable public asset cache so we can validate GLB mesh size, FPS, depth, and framing before texture/PBR.

## Next

P15: texture decode + GPU upload foundation.
