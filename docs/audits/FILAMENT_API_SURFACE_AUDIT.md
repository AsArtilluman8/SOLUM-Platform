# Filament API Surface Audit — P45C

Status: audit/forensics patch. This table describes what is exposed or wired in the current Java Filament preview path. It is not a visual feature claim.

| Feature | Java API exposed | Native/C++ needed | Currently wired | UI exists | Works | Notes |
|---|---:|---:|---:|---:|---:|---|
| Renderer timing / FPS | Partial | No | Yes | Yes | Estimate | Choreographer/wall timing, rolling FPS and frame ms; not authoritative GPU truth. |
| GPU timing | Partial platform only | Later likely | Partial | Yes | Not proven | Filament Java does not expose renderer GPU timing here; Android FrameMetrics GPU duration may be unavailable or zero. |
| FrameMetrics | Yes | No | Partial | Debug text | Partial | Window FrameMetrics listener samples total/draw/swap/GPU if Android provides values. |
| dumpsys gfxinfo | External tool | No | Command documented | Debug text | Deferred | Requires ADB shell outside app: `dumpsys gfxinfo com.solum.engine framestats`. |
| Dynamic Resolution | Yes | No | Yes | Yes | Partial | Uses Filament dynamic resolution options where available; actual runtime scaling remains approximate from Java. |
| MSAA | Yes | No | Yes | Yes | Partial | Sample count is sanitized and applied through view/render options. |
| FXAA | Yes | No | Yes | Yes | Partial | Toggle exists; effective AA status reported. |
| TAA | Yes | No | Yes | Yes | Partial | Toggle/status exists; quality depends on Filament path and scene motion. |
| SSR | Yes | No | Yes | Yes | Partial/heavy | Wired as screen-space reflections; marked manual heavy mobile risk; GPU stalls need profiler. |
| Dithering | Yes | No | Yes | Yes | Partial | Status reported; exact visual impact depends on Filament output path. |
| ColorGrading | Yes | No | Yes | Yes | Partial | Tone/exposure/contrast/saturation style controls are wired. |
| LUT / palette | Not currently exposed | Later possible | No | Text only | No | Current Java path reports `not_exposed`; needs deeper API/native evaluation. |
| Bloom | Yes | No | Yes | Yes | Partial | Bloom mode/strength/highlight are wired; sliders now enable soft bloom when adjusted from off. |
| Fog | Yes | No | Yes | Yes | Partial | Fog controls/status are wired through scene/view options. |
| Vignette | Not exposed in current path | Later possible | No | No | No | Would need post-processing path/API support audit. |
| Lens flare / sun glare | Not as Filament lens flare | Later possible | Overlay/custom later | Yes | Partial visual overlay | Current sun glare is overlay/status, not a proven Filament lens flare pipeline. |
| God rays / light shafts | Not exposed | Yes/later custom | No | No | No | Deferred; requires render pipeline/post-process design, not P45C. |
| AO | Yes | No | Yes | Yes | Partial | AO mode/status wired; visual effect may be subtle or scene-dependent. |
| Shadows | Yes | No | Yes | Yes | Partial | Basic Filament shadow flags/settings are wired where available. |
| CSM / cascades | Not exposed in current Java path | Likely | No | Text only | No | Cascade count/splits are reported `not_exposed`. |
| Shadow bias/map/distance | Partial/not current | Likely for full control | Partial | Text only | Partial/no | Basic shadow type status exists; bias/map/distance controls remain `not_exposed`. |
| Sun light | Yes | No | Yes | Yes | Partial | Direction/intensity/preset wiring exists. |
| Point light | Yes | No | Yes | Yes | Partial | Additional light rig can create point lights; status reports active count. |
| Spot light | Yes | No | Yes | Yes | Partial | Additional light rig can create spot lights; no advanced shadow support. |
| IBL | Yes | No | Yes | Yes | Partial | KTX/HDR path and fallback status reported; EXR unsupported. |
| Skybox | Yes | No | Yes | Yes | Partial | Skybox visible/ready state wired; blur not exposed. |
| Material slots | Yes | No | Yes | Yes | Partial | Material count/selected material index status exists. |
| Material overrides | Partial | Later for full editor | Partial | Yes | Partial | Current material inspector applies limited overrides; not full material graph. |
| Glass / transmission / refraction | Partial | Later for full material control | Partial | Yes | Partial | Screen-space refraction toggle/status exists; transmission/refraction truth is limited by loaded material and API. |
| Animation playback | Yes via gltfio | No | Partial | Limited | Partial | Basic glTF animation support may exist through gltfio path; not a full animation editor. |
| Multi-model scene | API supports entities | Later for scene system | No/full deferred | No/full deferred | No | Current preview centers on active imported model, not a real multi-model scene editor. |
| Picking | Yes | No | Yes | Yes | Partial | Selected renderable/material index and pick depth/status are reported. |
| Config save/load | Android/JSON | No | Yes | Yes | Partial | SharedPreferences plus JSON config path/status; schema remains preview-specific. |

## Deferred Runtime Truth

`not_exposed` means the current Java preview does not have enough API surface or wiring to claim the feature is controllable or profiled. Deferred items should be handled by targeted Java FrameMetrics validation, `dumpsys gfxinfo`, Perfetto, AGI, or native/C++ renderer work later.
