# GLB_IMPORT_PIPELINE — GLB import and first primitive render

## Scope

P05 added the first real model import path for SOLUM Engine.
P06 adds first primitive CPU extraction, Vulkan GPU upload and draw.
P07 adds baseColor texture extraction, Android decode, Vulkan upload and shader sampling.

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
- Embedded GLB baseColorTexture extraction from `image.bufferView`.
- Android `BitmapFactory` decode for `image/png` and `image/jpeg`.
- Vulkan RGBA8 image/imageView/sampler upload for one active first primitive baseColor texture.
- Fragment shader sampling through `sampler2D` and `TEXCOORD_0`.
- Single primitive draw through the existing Vulkan renderer path.
- Model diagnostics JSON.
- Cube render remains the fallback when no supported model is active.

Not implemented yet:

- PBR lighting.
- normal, metallicRoughness, AO, emissive texture sampling.
- mipmaps, anisotropy, texture arrays, multi-material binding.
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
- Base color uses glTF `baseColorFactor`.
- If the first primitive material has `pbrMetallicRoughness.baseColorTexture.index`, P07 resolves:
  - `textures[index].source`;
  - `images[source].bufferView`;
  - `images[source].mimeType`;
  - `bufferViews[bufferView].byteOffset`;
  - `bufferViews[bufferView].byteLength`;
  - GLB BIN chunk bytes.
- Supported embedded MIME now:
  - `image/png`;
  - `image/jpeg` when Android `BitmapFactory` decodes it.
- Missing texture reports `baseColorTextureStatus = missing` and keeps white/baseColor fallback.
- Decode/upload failure reports `baseColorTextureStatus = failed` / `textureUploadStatus = failed` with reason and keeps mesh draw alive.

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
- meshDrawStatus;
- textureUploadStatus;
- baseColorTextureStatus;
- textureWidth;
- textureHeight;
- textureFallbackUsed;
- uploadedVertexCount;
- uploadedIndexCount;
- fallbackCubeVisible;
- fallbackCubeStatus;
- reason.

`engine_runtime_state.json` also includes:

- assetImportStatus;
- activeModelName;
- activeModelPath;
- activePrimitiveIndex;
- activeModelSummary;
- gpuUploadStatus;
- drawStatus;
- meshDrawStatus;
- textureUploadStatus;
- baseColorTextureStatus;
- baseColorTextureName/source;
- baseColorTextureMimeType;
- textureWidth;
- textureHeight;
- textureBytes;
- textureFallbackUsed;
- uploadedVertexCount;
- uploadedIndexCount;
- modelVertexLayout;
- modelBoundsMin;
- modelBoundsMax;
- modelBoundsCenter;
- modelScale;
- modelRenderMode = `first_primitive`;
- fallbackCubeVisible;
- fallbackCubeStatus;
- reason.

## Runtime Truth

Scene label:

```text
Render Lab: Scene04 Texture Binding Lab
```

The model is imported, parsed on CPU, extracted as first primitive, uploaded to Vulkan buffers and optionally rendered with one baseColor texture when supported.
The visible Vulkan cube remains the mesh fallback. White/baseColor remains the material fallback. A failed texture decode/upload does not turn a successful mesh draw into `drawStatus=failed`.

Next step:

```text
P08 PBR Material Maps Foundation: metallicRoughness + normal + AO
```
