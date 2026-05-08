# TEXTURE_BINDING_FOUNDATION — P07

## P08 texture slots

P08 extends P07 single baseColor texture binding into a small texture slot system for multi-primitive static GLB rendering.

Rules:

- baseColor texture is resolved per material slot;
- native renderer can bind a texture descriptor per primitive draw range;
- `textureSlotLimit` is 8 in P08;
- overflow or unsupported texture decode increments skipped/fallback texture diagnostics;
- shader remains simple: `baseColorTexture * baseColorFactor * vertexColor/default`;
- metallicRoughness, normal, AO and emissive sampling are deferred to P09+.

Diagnostics:

```text
textureSlotCount
uploadedTextureCount
textureFallbackCount
skippedTextureCount
textureSlotLimit
```

## Supported now

- Active imported GLB, mesh 0 / primitive 0.
- `pbrMetallicRoughness.baseColorTexture.index`.
- `textures[index].source`.
- `images[source].bufferView`.
- Embedded GLB BIN image bytes through `bufferView.byteOffset` and `bufferView.byteLength`.
- MIME:
  - `image/png`;
  - `image/jpeg` when Android `BitmapFactory` decodes it.
- Android-side decode to ARGB pixels.
- JNI upload through `nativeUploadBaseColorTexture`.
- Vulkan RGBA8 `VkImage`, `VkDeviceMemory`, `VkImageView`, `VkSampler`.
- Minimal combined image sampler descriptor.
- Fragment shader `sampler2D` sampling with `TEXCOORD_0`.
- Fallback white/baseColor path when texture is missing or failed.

## Not supported yet

- normal map;
- metallicRoughness map;
- AO map;
- emissive map;
- PBR lighting;
- mipmaps;
- anisotropy;
- texture arrays;
- multi-material / multi-primitive texture binding;
- external image URI loading.

## Runtime truth

Diagnostics must distinguish mesh draw from texture status:

```text
gpuUploadStatus
drawStatus
meshDrawStatus
textureUploadStatus
baseColorTextureStatus
baseColorTextureName/source
baseColorTextureMimeType
textureWidth
textureHeight
textureBytes
textureFallbackUsed
fallbackCubeStatus
fallbackCubeVisible
```

Texture failure must not mark `drawStatus` failed when mesh draw still works.
