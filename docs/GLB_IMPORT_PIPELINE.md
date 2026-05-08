# GLB_IMPORT_PIPELINE — P05 model import foundation

## Scope

P05 adds the first real model import path for SOLUM Engine.

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
- CPU-only GLB parser foundation.
- Model diagnostics JSON.
- Cube render remains the fallback/current render.

Not implemented in P05:

- GPU mesh upload.
- Model draw.
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
- gpuUploadStatus = `not_implemented`;
- drawStatus = `not_implemented`;
- reason.

`engine_runtime_state.json` also includes:

- assetImportStatus;
- activeModelName;
- activeModelSummary;
- gpuUploadStatus = `not_implemented`;
- drawStatus = `not_implemented`.

## Runtime Truth

Scene label:

```text
Render Lab: Scene02 Model Import Lab
```

The model is imported and parsed only on CPU in P05.

The visible Vulkan cube remains the current render fallback. A successful import is not a fake model render success.

Next step:

```text
P06 GLB Mesh GPU Upload + Single Primitive Render
```
