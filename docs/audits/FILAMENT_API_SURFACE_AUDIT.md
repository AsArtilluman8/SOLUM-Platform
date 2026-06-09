# Filament / SOLUM API Surface Audit - P46

Status: render diagnostics and API surface lock.

This audit is based on the current project state:

- Gradle dependencies: `filament-android:1.71.4`, `gltfio-android:1.71.4`, `filament-utils-android:1.71.4`.
- Current imports and calls in `FilamentGlbPreviewActivity`.
- Current SOLUM UI wiring and diagnostics strings.
- Known exposed Java API surface visible from this code path.

This is not a marketing list. It is not a claim that every listed feature works on device. If an API or runtime behavior is not verified, it is marked `unknown_needs_verification` or `not_verified_on_device`.

## Strict Status Values

Java API status: `exposed`, `partially_exposed`, `not_exposed`, `unknown_needs_verification`.

C++ / native status: `not_needed_for_basic`, `likely_needed_for_full_control`, `required`, `unknown_needs_verification`.

Current SOLUM wiring: `wired`, `partially_wired`, `not_wired`, `fake_or_overlay`, `docs_only`.

Current UI status: `user_ui`, `debug_only`, `hidden`, `not_present`, `misleading_needs_fix`.

Runtime status: `works`, `partially_works`, `not_verified_on_device`, `not_working`, `not_available`, `deferred`.

Performance risk: `cheap`, `medium`, `expensive`, `screenshot_only`, `unknown_needs_measurement`.

## API Surface Table

| Feature | Purpose / what it gives visually or technically | Filament Java API status | Filament C++ / native status | Current SOLUM wiring | Current UI status | Runtime status on device | Performance risk on Mali-G57 | Final SOLUM module | Next action |
|---|---|---|---|---|---|---|---|---|---|
| Engine | Owns Filament objects and resource lifetime | exposed | not_needed_for_basic | partially_wired | hidden | partially_works | cheap | Render Core | Move ownership out of Activity later. |
| Renderer | Submits frames to Filament | exposed | not_needed_for_basic | partially_wired | hidden | partially_works | cheap | Render Core | Wrap in Render Core API. |
| View | Holds render options, post-process, quality | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Keep Basic/Advanced/Debug split. |
| Scene | Holds lights, skybox, renderables | exposed | not_needed_for_basic | partially_wired | hidden | partially_works | medium | Scene Workspace | Create real scene model. |
| Camera | Projection/orbit target | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Scene Workspace | Separate camera controller. |
| Surface/SwapChain | Android surface connection through ModelViewer/UiHelper | partially_exposed | likely_needed_for_full_control | partially_wired | hidden | partially_works | medium | Render Core | Hide behind render surface lifecycle. |
| Render loop | Choreographer callback plus ModelViewer render | exposed | likely_needed_for_full_control | wired | debug_only | partially_works | cheap | Render Core | Split callback timing from visible smoothness. |
| Dynamic resolution | Adjusts internal render scale under load | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Add runtime measurement later. |
| Render scale | User-facing quality/cost knob | partially_exposed | likely_needed_for_full_control | wired | user_ui | partially_works | medium | Render Control Center | Keep diagnostics explicit. |
| Viewport | Where scene is displayed | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Scene Workspace | Keep viewport clear of debug noise. |
| Clear color/background | Procedural fallback scene background | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Render Control Center | Retain as basic control. |
| Skybox | Environment/background | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Add asset-backed skybox validation. |
| Choreographer callback FPS | Measures Java callback cadence | exposed | not_needed_for_basic | wired | debug_only | works | cheap | Performance Profiler | Keep out of main HUD. |
| Wall frame interval | Java-side elapsed time between callbacks | exposed | not_needed_for_basic | wired | debug_only | works | cheap | Performance Profiler | Label as estimate only. |
| Android FrameMetrics | Platform frame timing | exposed | not_needed_for_basic | partially_wired | debug_only | not_verified_on_device | cheap | Performance Profiler | Validate samples on device. |
| GPU_DURATION | Android-reported GPU duration if available | partially_exposed | likely_needed_for_full_control | partially_wired | debug_only | not_verified_on_device | unknown_needs_measurement | Performance Profiler | Treat zero/unavailable as not exposed. |
| SWAP_BUFFERS_DURATION | Platform swap timing | exposed | not_needed_for_basic | partially_wired | debug_only | not_verified_on_device | cheap | Performance Profiler | Compare with gfxinfo. |
| DRAW_DURATION | Platform draw traversal timing | exposed | not_needed_for_basic | partially_wired | debug_only | not_verified_on_device | cheap | Performance Profiler | Compare with visible stalls. |
| dumpsys gfxinfo framestats | External Android frame stats | not_exposed | not_needed_for_basic | docs_only | debug_only | deferred | cheap | Performance Profiler | Capture through ADB/manual workflow. |
| Perfetto | System trace and scheduling/GPU counters where available | not_exposed | not_needed_for_basic | docs_only | debug_only | deferred | unknown_needs_measurement | Performance Profiler | Add profiler workflow doc. |
| Android GPU Inspector | GPU frame capture/counters | not_exposed | not_needed_for_basic | docs_only | debug_only | deferred | unknown_needs_measurement | Performance Profiler | Use for real GPU bottleneck proof. |
| Native/C++ GPU timestamps | GPU timestamp query path if supported | not_exposed | required | docs_only | not_present | deferred | medium | Performance Profiler | Research after Render Core exists. |
| Per-pass GPU timing | Identifies expensive pass | not_exposed | required | docs_only | not_present | deferred | unknown_needs_measurement | Performance Profiler | Needs native/profiler integration. |
| CPU render submit timing | Java wall timing around render submit | exposed | not_needed_for_basic | wired | debug_only | partially_works | cheap | Performance Profiler | Keep approximate label. |
| Jank/slow-frame counters | User-visible smoothness signal | exposed | not_needed_for_basic | wired | debug_only | works | cheap | Performance Profiler | Keep main HUD status derived from p95/jank. |
| MSAA | Hardware multisampling | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Cap high samples on Mali. |
| FXAA | Low-cost post AA | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Render Control Center | Keep basic toggle. |
| TAA | Temporal anti-aliasing | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Mark cost in diagnostics. |
| Dithering | Reduces banding | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Render Control Center | Keep status. |
| Sample count | Controls MSAA samples | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Validate actual sample cost. |
| Guard band | Reduces edge artifacts for post/AA path | partially_exposed | unknown_needs_verification | partially_wired | debug_only | not_verified_on_device | unknown_needs_measurement | Render Control Center | Verify Java API behavior. |
| Dynamic resolution quality | Quality levels/min/max scale | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Render Control Center | Profile scaling on device. |
| ColorGrading | Tone/color output pipeline | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | PostProcess Studio | Move presets into module later. |
| Tone mapping | ACES/Filmic/etc output transform | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | PostProcess Studio | Keep preset truth text. |
| Exposure | Brightness/color grade control | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | PostProcess Studio | Keep as basic control. |
| Contrast | Color grade contrast | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | PostProcess Studio | Keep as advanced control. |
| Saturation | Color intensity | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | PostProcess Studio | Keep as advanced control. |
| Temperature/tint | Warm/cool grading | partially_exposed | likely_needed_for_full_control | partially_wired | user_ui | partially_works | cheap | PostProcess Studio | Verify exact Filament parameter mapping. |
| LUT | Lookup-table color transform | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | medium | PostProcess Studio | Verify Java API before claiming. |
| Color palette | Artistic palette mapping | not_exposed | likely_needed_for_full_control | docs_only | not_present | deferred | medium | PostProcess Studio | Defer to custom post/material pipeline. |
| Vignette | Darkens image edges | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | cheap | PostProcess Studio | Verify API surface later. |
| Sharpen | Post-process detail sharpening | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | medium | PostProcess Studio | Verify API and cost. |
| Film grain | Adds noise texture look | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | cheap | PostProcess Studio | Verify before UI. |
| Chromatic aberration | Lens color fringe effect | not_exposed | likely_needed_for_full_control | not_wired | not_present | deferred | medium | PostProcess Studio | Defer/custom post only. |
| Bloom | Bright highlight glow | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | PostProcess Studio | Keep cost warning for high. |
| Bloom strength | Bloom intensity | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | PostProcess Studio | Wired; sliders enable Bloom Soft. |
| Bloom threshold/highlight | Controls highlight pickup | partially_exposed | likely_needed_for_full_control | wired | user_ui | partially_works | medium | PostProcess Studio | Verify exact threshold semantics. |
| Bloom dirt/softness | Lens dirt/softness controls | partially_exposed | likely_needed_for_full_control | not_wired | debug_only | deferred | medium | PostProcess Studio | Do not claim exposed until verified. |
| Sun glare | Visible sun glare indicator | not_exposed | likely_needed_for_full_control | fake_or_overlay | user_ui | partially_works | screenshot_only | PostProcess Studio | Keep labeled overlay, not Filament lens flare. |
| Lens flare | Optical flare pipeline | not_exposed | likely_needed_for_full_control | not_wired | not_present | deferred | expensive | PostProcess Studio | Defer. |
| God rays / light shafts | Volumetric/screen-space light shafts | not_exposed | required | docs_only | not_present | deferred | expensive | VFX Lab later | Do not add before profiler. |
| Screen-space overlays | 2D diagnostic/visual overlay | exposed | not_needed_for_basic | wired | user_ui | partially_works | screenshot_only | Render Control Center | Keep separate from render truth. |
| Fog | Distance/height atmosphere | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Keep as simple atmosphere control. |
| Haze | Artistic fog preset | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Keep preset-based. |
| Density | Fog thickness | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Keep advanced numeric. |
| Distance/falloff | Fog depth relationship | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Verify visual response. |
| Height fog | Fog by height | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Verify device output. |
| Sky/IBL relation | Fog/sky lighting coherence | partially_exposed | likely_needed_for_full_control | partially_wired | debug_only | not_verified_on_device | medium | Light Studio | Needs scene presets. |
| Volumetric fog | True volumetric atmosphere | not_exposed | required | docs_only | not_present | deferred | expensive | VFX Lab later | Defer. |
| Atmospheric scattering | Physical sky/atmosphere | unknown_needs_verification | likely_needed_for_full_control | docs_only | not_present | deferred | expensive | VFX Lab later | Research later. |
| Directional/sun light | Main directional lighting | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Light Studio | Move out of Activity. |
| Point light | Local omni light | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Add scene object ownership later. |
| Spot light | Cone light | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Add gizmo/control later. |
| Light color | Light tint | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | cheap | Light Studio | Promote to structured UI later. |
| Light intensity | Brightness | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Light Studio | Keep clamped. |
| Light range | Falloff range | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | medium | Light Studio | Expose in advanced later. |
| Spot cone | Spot inner/outer cone | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | medium | Light Studio | Expose later. |
| Light rig presets | Quick multi-light setups | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Convert to scene preset asset later. |
| Area lights | Broad source lighting | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | medium | Light Studio | Verify Java API. |
| IBL intensity | Environment light strength | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Light Studio | Keep status. |
| IBL rotation | Rotates environment | partially_exposed | likely_needed_for_full_control | partially_wired | user_ui | partially_works | medium | Light Studio | Verify exact transform path. |
| Skybox visibility | Show/hide background skybox | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Light Studio | Keep. |
| Shadows on/off | Enables shadowing path | exposed | not_needed_for_basic | wired | user_ui | partially_works | expensive | Shadow Studio | Needs profiler before expanding. |
| Shadow type | PCF/DPCF style selection | exposed | not_needed_for_basic | wired | user_ui | partially_works | expensive | Shadow Studio | Keep status explicit. |
| Shadow quality | Overall shadow quality | partially_exposed | likely_needed_for_full_control | partially_wired | user_ui | not_verified_on_device | expensive | Shadow Studio | Verify actual map/quality control. |
| Shadow bias | Acne/peter-panning control | unknown_needs_verification | likely_needed_for_full_control | docs_only | debug_only | deferred | medium | Shadow Studio | Verify API before UI. |
| Shadow map size | Resolution/cost | unknown_needs_verification | likely_needed_for_full_control | docs_only | debug_only | deferred | expensive | Shadow Studio | Needs API/profiler proof. |
| Shadow distance | Far shadow range | unknown_needs_verification | likely_needed_for_full_control | docs_only | debug_only | deferred | expensive | Shadow Studio | Defer. |
| Soft shadows | Filtered shadow look | partially_exposed | likely_needed_for_full_control | partially_wired | user_ui | partially_works | expensive | Shadow Studio | Keep mobile-safe modes. |
| Contact shadows | Contact detail | unknown_needs_verification | likely_needed_for_full_control | docs_only | debug_only | deferred | expensive | Shadow Studio | Verify Java API. |
| CSM/cascades | Large outdoor directional shadows | not_exposed | required | docs_only | debug_only | deferred | expensive | Shadow Studio | Native/render architecture later. |
| Cascade splits | CSM distribution | not_exposed | required | docs_only | not_present | deferred | expensive | Shadow Studio | Defer. |
| Per-light shadow support | Shadows for specific lights | partially_exposed | likely_needed_for_full_control | partially_wired | debug_only | not_verified_on_device | expensive | Shadow Studio | Audit per light type. |
| Shadow debug | Visualize shadow maps/cascades | not_exposed | required | docs_only | not_present | deferred | screenshot_only | Shadow Studio | Add after Shadow Studio. |
| SSR | Screen-space reflections | exposed | not_needed_for_basic | wired | user_ui | partially_works | expensive | PostProcess Studio | Keep heavy warning, needs profiler. |
| Screen-space refraction | Refraction from screen data | exposed | not_needed_for_basic | wired | user_ui | partially_works | expensive | Glass Studio | Keep truth text. |
| Transmission | Glass material light transmission | partially_exposed | likely_needed_for_full_control | partially_wired | debug_only | not_verified_on_device | expensive | Glass Studio | Verify glTF/material support. |
| Alpha blending | Transparent materials | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | medium | Glass Studio | Needs material editor ownership. |
| Double-sided glass | Two-sided transparent material | partially_exposed | likely_needed_for_full_control | docs_only | not_present | deferred | medium | Glass Studio | Verify material instance control. |
| Transparent sorting | Correct transparent order | partially_exposed | likely_needed_for_full_control | docs_only | not_present | deferred | medium | Glass Studio | Needs scene tests. |
| Rough glass | Blurred/rough refraction | unknown_needs_verification | likely_needed_for_full_control | docs_only | not_present | deferred | expensive | Glass Studio | Defer. |
| Fake reflection | Overlay/cubemap approximation | not_exposed | likely_needed_for_full_control | not_wired | not_present | deferred | screenshot_only | Glass Studio | Avoid fake production claim. |
| Cubemap/IBL reflection | PBR environment reflection | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Glass Studio | Validate with known assets. |
| Real reflection probes | Scene-local reflections | unknown_needs_verification | required | docs_only | not_present | deferred | expensive | Glass Studio | Research later. |
| Material slots | Per-renderable material slots | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Material Studio | Build real inspector model. |
| Material instances | Runtime material parameter object | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Material Studio | Move overrides into Material Studio. |
| BaseColor | Surface color | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Material Studio | Verify per material type. |
| Metallic | Metalness parameter | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Material Studio | Verify parameter names. |
| Roughness | Surface roughness | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Material Studio | Verify parameter names. |
| Normal | Normal map slot | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Material Studio | Add asset-backed slot later. |
| ORM | Occlusion/roughness/metallic texture | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Material Studio | Add after asset schema. |
| Emissive | Self-lit material output | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Material Studio | Verify glTF path. |
| Alpha mode | Opaque/mask/blend | exposed | not_needed_for_basic | partially_wired | debug_only | not_verified_on_device | medium | Material Studio | Add explicit UI later. |
| Double-sided | Render both sides | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Material Studio | Verify material control. |
| Material overrides | Runtime parameter overrides | partially_exposed | likely_needed_for_full_control | partially_wired | user_ui | partially_works | medium | Material Studio | Keep limited and honest. |
| Material preset | Saved material style preset | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Material Studio | Needs SOLUM schema. |
| Material graph/node system | Full node material authoring | not_exposed | required | docs_only | not_present | deferred | expensive | Material Studio | Later only. |
| Custom material package/matc | Compiled Filament material packages | partially_exposed | required | not_wired | not_present | deferred | medium | Material Studio | Needs build/toolchain audit. |
| GLB loading | Import binary glTF models | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Asset Shelf | Keep gltfio status. |
| glTF loading | Import text glTF models | exposed | not_needed_for_basic | partially_wired | user_ui | not_verified_on_device | medium | Asset Shelf | Verify file types. |
| Texture loading | Load referenced textures | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | medium | Asset Shelf | Add asset validation. |
| HDR loading | Load HDR environment | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Asset Shelf | Keep fallback status. |
| KTX loading | Load KTX cubemap/IBL | exposed | not_needed_for_basic | wired | user_ui | partially_works | medium | Asset Shelf | Prefer mobile-ready assets later. |
| IBL prefilter | Convert HDR to IBL | exposed | likely_needed_for_full_control | partially_wired | debug_only | not_verified_on_device | expensive | Asset Shelf | Measure cost and cache outputs. |
| EXR support status | EXR environment input | not_exposed | likely_needed_for_full_control | docs_only | debug_only | not_available | unknown_needs_measurement | Asset Shelf | Keep unsupported unless dependency chosen. |
| Animation loading | glTF animations | exposed | not_needed_for_basic | partially_wired | hidden | not_verified_on_device | medium | Animation Preview | Add player UI later. |
| Morph targets | Blendshape animation | unknown_needs_verification | likely_needed_for_full_control | not_wired | not_present | deferred | medium | Animation Preview | Verify gltfio API. |
| Skinning/skeleton | Skeletal mesh animation | exposed | not_needed_for_basic | partially_wired | hidden | not_verified_on_device | medium | Animation Preview | Add animation smoke test. |
| Multiple assets in scene | More than one model/renderable asset | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Scene Workspace | Required for foundation exit. |
| Asset unload/release | Avoid leaks and stale GPU resources | exposed | not_needed_for_basic | partially_wired | debug_only | partially_works | cheap | Asset Shelf | Centralize lifetime. |
| Picking/selecting renderables | Tap/select object/material | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Scene Workspace | Build selection model. |
| Multi-model scene | Real scene with multiple objects | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Scene Workspace | Required before Labs. |
| Object list | Hierarchy/list of scene objects | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Scene Workspace | Build SOLUM scene model. |
| Selected object | Current object selection state | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Scene Workspace | Promote from renderable debug state. |
| Transform position/rotation/scale | Object editing transform | exposed | not_needed_for_basic | partially_wired | user_ui | partially_works | cheap | Transform Gizmo | Replace sliders with gizmo later. |
| World/local gizmo | Direct touch transform control | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Transform Gizmo | Required for foundation exit. |
| Scene save/load | Persist scene graph | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Scene Workspace | Required for foundation exit. |
| Asset shelf | Add/reuse assets | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Asset Shelf | Required for foundation exit. |
| Drag/drop/add to scene | Put assets into scene | not_exposed | not_needed_for_basic | not_wired | not_present | deferred | cheap | Asset Shelf | Build after scene model. |
| Animation preview | Play clips in preview | exposed | not_needed_for_basic | not_wired | not_present | deferred | medium | Animation Preview | Required for foundation exit. |
| Config save/load | Save preview settings | exposed | not_needed_for_basic | wired | user_ui | partially_works | cheap | Render Control Center | Split render config from scene save. |

## Lock Summary

Java-exposed and already useful now:

- Engine/Renderer/View/Scene/Camera basics through Filament/ModelViewer.
- View quality controls: dynamic resolution, MSAA, FXAA, TAA, dithering, AO, SSR, bloom, fog, color grading, shadow toggles.
- Light creation for sun/point/spot and basic IBL/skybox.
- glTF/GLB loading through `gltfio`.
- Android timing sources: Choreographer and FrameMetrics listener.

Requires C++/native or external profiler for trustworthy/full control:

- Real GPU timing, per-pass timing, GPU timestamp queries.
- CSM/cascade controls and full shadow debug.
- Custom post effects such as god rays, volumetric fog, advanced lens flare, custom color palette, material graph pipeline.
- Native material tooling/matc integration if SOLUM moves beyond runtime parameter overrides.

Not exposed or not wired yet:

- Multi-model scene, object list, asset shelf, world/local gizmo, scene save/load, animation preview UI.
- LUT/palette, vignette/sharpen/film grain/chromatic aberration until verified.
- Reflection probes, rough glass, transparent sorting controls.

Deferred:

- Perfetto/AGI workflow integration.
- Native profiler hooks.
- VFX Lab, Water Lab, Physics Reaction Lab.

## P47B Control Truth Update

Current Activity-level control status after P47B:

| Control | Current status | Notes |
|---|---|---|
| MSAA | applied_live_not_runtime_profiling_verified | UI tracks `requestedMSAA`, `actualMSAA`, and `msaaApplyStatus`. Java API `View.setSampleCount()` is called live. Runtime/device visual proof is still required. |
| Dynamic Resolution | applied_live_not_runtime_profiling_verified | UI tracks `requestedDynamicResolution`, `actualDynamicResolution`, and apply status after `View.setDynamicResolutionOptions()`. |
| AO | applied_live_not_visual_verified | `AmbientOcclusionOptions` are applied; visual strength depends on scene depth/contact areas. |
| Bloom | applied_live_not_visual_verified | `BloomOptions` are rebuilt/applied; high mode marked expensive. |
| SSR | applied_live_expensive_not_gpu_verified | `ScreenSpaceReflectionsOptions` are applied; GPU cost requires FrameMetrics/profiler proof. |
| TAA | applied_live_or_failed_truthful | UI tracks `requestedTAA`, `actualTAA`, `taaApplyStatus`; failures are reported without flipping requested state. |
| Shadows | partially_applied_not_full_shadow_system | View shadowing and shadow type are applied; map size, CSM, contact shadows, bias and distance remain not exposed. |
| Color | applied_live_rebuilds_colorgrading | Color sliders update requested values and rebuild `ColorGrading`; preset changes set defaults only when the preset button is used. |
| Render scale | applied_to_dynamic_resolution_range | Controls Dynamic Resolution max scale; exact internal scaler behavior still requires runtime measurement. |
| FXAA | applied_live | Uses `View.setAntiAliasing(FXAA/NONE)`. |
| Dithering | applied_live | Uses `View.setDithering(TEMPORAL/NONE)`. |
| Fog | applied_live_not_visual_verified | `FogOptions` are applied; runtime scene proof still required. |
| Sun/Ambient/Fill/Background | applied_live | Light, IBL intensity, camera exposure, and clear color are updated live. |
| IBL controls | partially_applied | Intensity/skybox visibility apply; rotation path remains partially verified by current Java path. |
| Light rig | applied_live_scene_lights | Point/spot lights are recreated as Activity-owned preview lights; not a reusable scene system yet. |
| Sun glare | overlay_only_not_filament_lens_flare | Explicitly screen-space overlay, not a Filament lens flare feature. |
| Material inspector | requested/current_partial | Existing material parameter inspection remains Activity-local and not a full Material Studio. |
| Picking/select | partially_applied | Pick requests and selected renderable diagnostics exist; full scene selection model deferred. |
| Config save/load | applied_with_safety_normalization | Schema v6 stores requested/actual truth fields; Low/Medium profile loads normalize stale expensive values. |
