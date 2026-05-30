# SOLUM Repo Usage Map

Source: SOLUM_WORLD_REPOS.docx

## Renderer / PBR

- google/filament
  Primary renderer and PBR reference.
- SaschaWillems/Vulkan
  Vulkan examples and fallback reference.
- Vulkan-glTF-PBR
  PBR/glTF reference.
- ARM Vulkan Best Practice
  Mali-G57 optimization reference.

## IBL / Sky / Atmosphere

- KhronosGroup/glTF-IBL-Sampler
  Offline HDR to KTX2 cubemap, prefiltered mips, BRDF LUT.
- IBLBaker
  GUI IBL cubemap baking.
- diharaw/sky-models
  Bruneton / Preetham / Hosek-Wilkie comparison.
- andrewwillmott/sun-sky
  Preetham / Hosek-Wilkie sky and sun.
- wwwtyro/glsl-atmosphere
  Lightweight Rayleigh/Mie atmosphere.
- ebruneton/precomputed_atmospheric_scattering
  Physically accurate atmosphere reference.

## Shadows

- SaschaWillems/shadowmappingcascade
  CSM on Vulkan, split frustum, PCF.
- diharaw/cascaded-shadow-maps
  Stable CSM/PSSM reference.

## Terrain / World / Buildings

- TerraForge3D
  Node-based terrain, erosion, GLSL export.
- AntonHakansson/procedural-terrain
  Terrain with tessellation, CSM, PBR, SSR.
- fegennari/3DWorld
  Open-world terrain, buildings, vegetation, rain, day/night.
- pvallet/CGA_interpreter
  Procedural buildings with CGA grammar.

## Animation / Characters

- ozz-animation
  Main candidate for skeletal animation, blending, IK, foot IK.
- SaschaWillems/gltfskinning
  glTF GPU skinning example.
- MakeHuman
  Open source human generator.
- MPFB2
  Blender-based MakeHuman pipeline to glTF/FBX/USD.
- AI4Animation
  Locomotion research/reference.

## VFX

Final direction:
- SOLUM VFX Framework, Niagara-like mobile architecture.
- Filament is render adapter.
- Do not build primitive throwaway particles.

Repos:
- turanszkij/WickedEngine
  Main AAA GPU particles architecture reference: alive/dead lists, indirect draw, depth sort, force fields, skinned mesh emission.
- SaschaWillems/computeparticles
  Minimal Vulkan compute particle reference with SSBO.
- effekseer/Effekseer
  VFX editor/runtime candidate, authoring reference, not mandatory final runtime.
- AmanSachan1/Meteoros
  Volumetric clouds / Decima-like reference.
- Erkaman/glsl-godrays
  God rays reference.
- diharaw/volumetric-clouds
  Ray marching clouds reference.

## Water / Ocean

- kentril0/WaterSurfaceRendering
  Vulkan FFT water, Tessendorf, compute displacement/normal, PBR.
- deiss/fftocean
  FFT ocean reference.
- Themaister/GLFFT
  GPU FFT, FP16, mobile-friendly.
- ARM Ocean FFT sample
  ARM/Mali optimized ocean FFT reference.

## Vegetation / Hair

- nickmcd/VulkanGrassRendering
  Responsive Real-Time Grass / Ghost of Tsushima style.
- Vulkan-Forest-Rendering-Engine
  GPU instancing, billboard LOD, tree wind.
- AMD TressFX
  Hair/fur GPU simulation reference.

## Physics / Navigation / AI

- JoltPhysics
  Primary physics candidate.
- bullet3
  Backup/basic physics reference.
- recastnavigation
  Navmesh/pathfinding/crowd standard.

## Audio

- miniaudio
  Main small Android audio candidate.
- SoLoud
  Alternative richer audio engine.

## UI / Tools

- Dear ImGui
  Native tools/debug UI.
- imnodes
  Node editor for material/VFX graph.
- implot
  FPS/GPU/frame charts.
- KProgressHUD
  Android loading UI.

## Asset pipeline / optimization

- VulkanMemoryAllocator
  Vulkan memory reference if native passes stay.
- volk
  Vulkan function loader.
- meshoptimizer
  LOD, vertex cache, overdraw reduction.
- glslang
  GLSL to SPIR-V.
- SPIRV-Cross
  Shader reflection/conversion.
- SPIRV-Tools
  SPIR-V optimization/validation.
- KTX-Software
  KTX2 tools.
- basis_universal
  Basis/KTX2 compression.
- ARM astc-encoder
  ASTC compression for Mali.

## Math / ECS / tasks

- glm
  Math.
- FastNoiseLite
  Noise for terrain/clouds/material variation.
- stb
  Image/font loading.
- nlohmann/json
  Source/debug configs.
- tinygltf
  glTF loading/reference.
- taskflow
  Task graph for streaming/build systems.
- EnTT
  Main ECS candidate.
- flecs
  Rich ECS alternative.

## Profiling

- Tracy
  CPU/GPU frame profiler.
- Perfetto
  Android system/jank profiler.
- Android GPU Inspector
  GPU counters.
- ARM Mali Offline Compiler
  Shader cycle analysis for Mali-G57.
- Vulkan Validation Layers
  Vulkan validation for native paths.

## Mobile/Mali rules

- Prefer ASTC textures.
- Prefer FP16/mediump in particle/water shader math.
- Avoid bandwidth-heavy passes.
- MSAA can be cheap on tile GPUs if resolve is inside render pass.
- Use KTX2 streaming/LOD.
- SSR is expensive and must stay manual-only.
- Avoid heavy default settings.
