# NOTE-0007: P09 Big Renderer Core Pack

## Purpose

P09 stops the tiny-patch chain and turns the current Vulkan proof path into a reusable renderer core.

The visual result intentionally stays the same: a compact HUD and the validation triangle. The architectural result changes: GPU resources are no longer random fields/functions inside one monolithic `solum_engine.cpp`.

## References

- ConfettiFX/The-Forge — REFERENCE_ONLY for renderer ownership, resource lifetime and future descriptor/barrier architecture.
- Khronos Vulkan Samples — REFERENCE_ONLY for buffer, pipeline, render pass and sync patterns.
- SaschaWillems Vulkan — REFERENCE_ONLY for practical mesh/pipeline draw structure.
- ARM Mali Vulkan best practices — REFERENCE_ONLY for mobile render pass and bandwidth constraints.
- Filament — REFERENCE_ONLY for future material/PBR architecture.

## Decision

Use `REFERENCE_ONLY` now and `ADAPTER LATER` if a Forge-inspired abstraction becomes useful.

Do not import The Forge as a dependency yet. The current Android/Termux Vulkan route is proven and must stay controllable.

## New architecture

```text
JNI bridge
↓
RendererCore
↓
RuntimeDiagnostics
↓
PipelineBundle
↓
MeshResource
↓
GpuBuffer
```

## Scope

- Split renderer internals into header-only internal modules under `src/solum/`.
- Keep a small `solum_engine.cpp` JNI bridge.
- Preserve Android Native Vulkan runtime.
- Preserve shader build path.
- Preserve compact HUD.
- Preserve visible validation triangle.
- Add `Renderer core: OK` runtime proof.

## Out of scope

- The Forge dependency.
- VMA/volk dependency.
- glTF import.
- PBR/material system.
- lighting/shadows/CSM.
- camera/orbit controls.

## Definition of Done

```text
build OK
APK opens
compact HUD says Renderer Core OK
triangle visible
SOLUM_RUNTIME_DIAGNOSTICS.zip generated
```

## Next

P10 should be a real Asset Mesh Upload Pack or a diagnostics/export improvement if runtime JSON remains inaccessible.
