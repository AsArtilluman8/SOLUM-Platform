# GLB_IMPORT_PIPELINE — GLB import and first primitive render

## Scope

P05 added the first real model import path for SOLUM Engine.
P06 adds first primitive CPU extraction, Vulkan GPU upload and draw.

Implemented:

- `Import GLB` button in `apps/engine`.
- Android file picker through `ACTION_OPEN_DOCUMENT`.
- MIME filter preference:
  - `model/gltf-binary`
  - `application/octet-stream`
  - `*/*` fallback.
- Copy into the asset library:

```text
/storage/emulated/0/SOLUMCreative/assets/models/imported/
```

- Copy route order:

```text
saf -> direct -> fallback -> failed
```

- `Scan Models` button.
- CPU GLB parser foundation.
- First primitive CPU extraction for active imported model.
- Vulkan model vertex/index buffer upload.
- Single primitive draw through the existing Vulkan renderer path.
- Model diagnostics JSON.
- Cube render remains the fallback when no supported model is active.

Not implemented:

- Texture binding.
- PBR lighting.
- Skeletal animation.

## Asset Folder

Preferred model import root:

```text
/storage/emulated/0/SOLUMCreative/assets/models/imported/
```

If Android blocks direct public writes, engine uses app-specific fallback and reports the exact route/reason in diagnostics.

## Parser Stages

The parser is CPU-only and has no external package dependency.

GLB container checks:

- magic must be `glTF`;
- version must be `2`;
- total length must match the file size;
- JSON chunk must exist;
- BIN chunk is detected when present.

JSON metadata read:

- scenes;
- nodes;
- meshes;
- primitives;
- accessors;
- bufferViews;
- buffers;
- materials;
- images;
- textures;
- samplers;
- skins;
- extensionsUsed;
- extensionsRequired.

Counts:

- meshCount;
- primitiveCount;
- nodeCount;
- sceneCount;
- materialCount;
- textureCount;
- imageCount;
- accessorCount;
- bufferViewCount;
- totalVertexCountEstimate;
- totalIndexCountEstimate.

Attributes detected:

- POSITION;
- NORMAL;
- TEXCOORD_0;
- COLOR_0;
- TANGENT;
- JOINTS_0;
- WEIGHTS_0.

If metadata cannot be read, fields stay `unknown/not_parsed` or zero and `reason` contains the exact failure.

## P06 First Primitive Extraction

Supported first primitive data:

- `POSITION`: FLOAT VEC3, required.
- `NORMAL`: FLOAT VEC3, optional.
- `TEXCOORD_0`: FLOAT VEC2, optional.
- `COLOR_0`: FLOAT VEC3/VEC4, optional, default white.
- indices: UNSIGNED_SHORT or UNSIGNED_INT.

Accessor handling:

- accessor `byteOffset`;
- bufferView `byteOffset`;
- bufferView `byteStride`;
- tightly packed fallback when stride is absent.

Unsupported primitive data does not claim success:

```text
gpuUploadStatus = failed
drawStatus = fallback
fallbackCubeVisible = true
reason = exact unsupported accessor/component/type
```

Model transform:

- CPU extracts first primitive vertex data.
- Bounds are measured from raw POSITION data.
- Vertices are centered and normalized before upload.
- `modelScale`, `modelBoundsMin`, `modelBoundsMax`, `modelBoundsCenter` are exported.
- Base color uses glTF `baseColorFactor`; texture sampling is deferred to P07.

## Diagnostics Files

P05 writes:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/model_import_state.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/asset_report.json
```

Both files include:

- importStatus;
- importRoute;
- sourceDisplayName;
- importedPath;
- activeModelPath;
- glbValid;
- glbVersion;
- fileSizeBytes;
- meshCount;
- primitiveCount;
- nodeCount;
- sceneCount;
- materialCount;
- textureCount;
- imageCount;
- accessorCount;
- bufferViewCount;
- totalVertexCountEstimate;
- totalIndexCountEstimate;
- attributesFound;
- extensionsUsed;
- extensionsRequired;
- hasSkinning;
- hasTangents;
- hasNormals;
- hasTexcoord0;
- gpuUploadStatus;
- drawStatus;
- uploadedVertexCount;
- uploadedIndexCount;
- fallbackCubeVisible;
- reason.

`engine_runtime_state.json` also includes:

- assetImportStatus;
- activeModelName;
- activeModelPath;
- activePrimitiveIndex;
- activeModelSummary;
- gpuUploadStatus;
- drawStatus;
- uploadedVertexCount;
- uploadedIndexCount;
- modelVertexLayout;
- modelBoundsMin;
- modelBoundsMax;
- modelBoundsCenter;
- modelScale;
- modelRenderMode = `first_primitive`;
- fallbackCubeVisible;
- reason.

## Runtime Truth

Scene label:

```text
Render Lab: Scene03 GLB Mesh Render Lab
```

The model is imported, parsed on CPU, extracted as first primitive and uploaded to Vulkan buffers when supported.
The visible Vulkan cube remains the fallback. A failed upload or unsupported accessor is reported honestly.

Next step:

```text
P07 Texture Binding Foundation + BaseColor Texture
```
