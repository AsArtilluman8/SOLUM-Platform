# P10B_RUNTIME_RENDER_DIAGNOSTICS

## Goal

P10B adds runtime FPS and render-state diagnostics before glTF/material work.

## Why

Materials and imported models are hard to debug from screenshots only. Before glTF/PBR work the diagnostics ZIP must expose:

- frame count;
- FPS estimate;
- average/min/max frame time;
- current model state;
- current material state;
- known limits of the measurement path.

## Current limitation

FPS is measured from Android Choreographer frame callbacks. This is not GPU timestamp profiling yet. It is enough to catch obvious performance regressions and verify that the renderer is no longer a one-frame static proof.

## Runtime files

```text
runtime_render_state.json
runtime_model_state.json
runtime_material_state.json
runtime_java_state.json
runtime_vulkan_caps.json
```

## Material status

P10B does not add a final material system. It explicitly reports:

```text
not_ready_for_gltf_pbr_yet
```

This prevents fake material work from being treated as final.
