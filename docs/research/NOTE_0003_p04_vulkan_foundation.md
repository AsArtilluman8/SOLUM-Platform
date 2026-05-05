# NOTE-0003: Patch P04 Vulkan Foundation v1

## Problem

Patch P02 proved that Termux Vulkan can report llvmpipe CPU. SOLUM needs an Android APK runtime path to query the real vendor Vulkan driver and start the future engine renderer.

## References studied

- `android/ndk-samples` — Android activity/surface/native lifecycle patterns.
- `KhronosGroup/Vulkan-Samples` — Vulkan instance/device/surface/swapchain initialization order.
- `ARM-software/vulkan_best_practice_for_mobile_developers` — Mali/tile-based constraints.
- `SaschaWillems/Vulkan` — practical Vulkan swapchain/triangle foundation reference.
- `VMA` / `volk` — future references, not dependencies in Patch P04.

## Adopted principles

- Android runtime Vulkan path is separate from Termux shell Vulkan path.
- Patch P04 creates Android APK + native C++ Vulkan module.
- First goal is instance/device/surface/swapchain and runtime caps report.
- Use manual prebuilt `.so` route for Termux reliability.
- Keep Vulkan system expandable; do not create OpenGL/Canvas fallback.

## Rejected parts

- OpenGL fallback.
- Canvas/bitmap renderer.
- The Forge import.
- Full render graph.
- PBR/materials/textures.
- CSM/shadows.
- VMA/volk dependency before Android Vulkan boot path works.

## SOLUM adaptation

Architecture:

```text
apps/engine Android Activity
↓
SurfaceView
↓
JNI
↓
engine-core/solum-vulkan-core C++
↓
VkInstance + VkPhysicalDevice + VkDevice + Android Surface + Swapchain
↓
runtime_vulkan_caps.json
```

## Diagnostics/tests

Build:

```bash
bash tools/build_engine_apk.sh
```

Expected:

```text
/storage/emulated/0/SOLUMCreative/releases/latest/SOLUM_LATEST.apk
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Runtime after APK launch:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/runtime_vulkan_caps.json
```

Expected user-visible status:

```text
Renderer path: Android Native Vulkan
GPU: Mali-G57 / ARM-class device
Swapchain: created
```

## Known limitation

Patch P04 creates swapchain and runtime report. Clear/triangle draw pass may be promoted to P04A if build/runtime foundation needs stabilization first.
