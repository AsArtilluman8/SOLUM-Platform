# NOTE-0009: P10B Runtime FPS + Render Diagnostics

## Decision

Before camera/material/glTF work, SOLUM needs a better diagnostics gate.

## Scope

- Android Choreographer frame loop.
- JNI `nativeRenderFrame`.
- Runtime FPS/frame-time estimate.
- `runtime_render_state.json`.
- `runtime_model_state.json`.
- `runtime_material_state.json`.
- Export script updated to include these files when accessible.

## Out of scope

- GPU timestamp queries.
- Perfetto/AGI integration.
- Final material system.
- glTF import.

## Known limits

Frame timing is Java/UI-frame based. GPU timestamps come later. The material report is intentionally a readiness state, not a fake material implementation.
