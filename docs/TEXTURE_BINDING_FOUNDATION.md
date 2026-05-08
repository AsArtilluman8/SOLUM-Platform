# TEXTURE_BINDING_FOUNDATION — P07

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
