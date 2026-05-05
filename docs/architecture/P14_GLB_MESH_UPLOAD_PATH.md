# P14_GLB_MESH_UPLOAD_PATH

```text
public GLB asset
↓
tools/build_solum_mesh_cache_from_gltf.py
↓
SOLMESH1 active_mesh_v1.bin
↓
MeshResource::createFromSolumMeshCache
↓
GpuBuffer vertex upload
↓
vkCmdDraw(vertexCount)
```

The current render uses per-primitive debug colors. glTF PBR materials and textures start in P15/P16.
