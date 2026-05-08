# GLTF_COMPATIBILITY_MATRIX

P08 target: static multi-primitive GLB rendering with material slots and baseColor texture slots.

| Feature | Status | Patch target | Fallback behavior |
|---|---|---|---|
| GLB 2.0 binary container | supported | P06-P08 | invalid import state, cube fallback |
| Multiple meshes/primitives | partial | P08 | unsupported primitives skipped; all skipped uses fallback cube |
| Primitive mode TRIANGLES | supported | P08 | non-triangles skipped with reason |
| POSITION FLOAT VEC3 | supported | P08 | primitive skipped if missing/unsupported |
| NORMAL FLOAT VEC3 | supported | P08 | default normal if missing |
| TEXCOORD_0 FLOAT VEC2 | supported | P08 | default UV if missing |
| COLOR_0 FLOAT VEC3/VEC4 | partial | P08 | default white if missing |
| Indices UNSIGNED_SHORT | supported | P08 | draw expanded into shared index buffer |
| Indices UNSIGNED_INT | supported | P08 | draw expanded into shared index buffer |
| byteOffset + bufferView.byteOffset | supported | P08 | bounds error skips primitive |
| byteStride | supported | P08 | tightly packed if absent |
| Material baseColorFactor | supported | P08 | default white |
| Material baseColorTexture | partial | P08 | texture slot fallback/skipped diagnostics |
| alphaMode / alphaCutoff | diagnostics-only | P08 | metadata recorded; no blending yet |
| doubleSided | diagnostics-only | P08 | metadata recorded; no cull mode switch yet |
| metallicRoughness texture | not-supported | P09 | ignored with docs |
| normal/AO/emissive maps | not-supported | P09+ | ignored with docs |
| skinning/animation | not-supported | future | static mesh only |

Required P08 diagnostics include `modelRenderMode = multi_primitive_static`, primitive counts, material slot counts, texture slot counts, FPS fields, debug ZIP fields, `drawStatus`, `gpuUploadStatus`, and fallback cube state.
