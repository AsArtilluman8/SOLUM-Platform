# NOTE-0014: P14A Mesh Cache Builder Strict Hotfix

P14A fixes the invalid Python syntax in `tools/build_solum_mesh_cache_from_gltf.py` and makes the mesh cache step strict.

## Problem

P14 reported `SyntaxError` while building `active_mesh_v1.bin`, but the APK build continued. That made P14 a partial/invalid patch: the runtime could still show the fallback cube.

## Fix

- Replace the mesh cache builder with a valid GLB/glTF parser.
- Run `python3 -m py_compile` before execution.
- Run mesh cache generation before APK build.
- Fail before APK build if `active_mesh_v1.bin` is missing.
- Write `runtime_mesh_cache_state.json` into diagnostics.

## Expected result

```text
SOLUM MESH CACHE: OK
active_mesh_v1.bin exists
vertexCount > 0
triangleCount > 0
APK build OK
```
