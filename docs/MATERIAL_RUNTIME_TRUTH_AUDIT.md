# Material Runtime Truth Audit — P29A

## 1. Goal

Find every current source of truth for Material/Glass runtime parameters before another visual glass patch. The target map is:

```text
UI -> Java -> JNI -> C++ runtime state -> PushConstants -> Shader -> Diagnostics
```

This audit is not a visual fix. It does not prove glass is visually correct.

## 2. Current Problem

P28 glass fixes cannot be trusted as final visual evidence because glass state is split across UI fields, Java diagnostics, JNI arguments, C++ `MaterialConstants`, per-slot `MaterialSlotState`, push constants, shader branches, and multiple diagnostics reports.

The user can see one value on a slider while:

- `materialSlotDiagnostics` still reports legacy per-material glass values.
- `syncGlassState()` reports CPU-estimated final alpha/RGB values.
- `recordFrame()` may override selected-slot values when routing an active glass slot.
- `triangle.frag.glsl` may use a different branch depending on `materialTypeHint`, `materialPresetHint`, `glassEnabled`, and `glassRenderMode`.
- Debug views show branch/input diagnostics, not final visible pixels.

Result: P28 can say state/formula is updated while final screen output remains unproven.

## 3. Parameter Truth Table

| Parameter | UI | Java | JNI | C++ Runtime | PushConstants | Shader | Diagnostics | Final Screen Impact | Conflict Risk |
|---|---|---|---|---|---|---|---|---|---|
| `glassEnabled` | `Glass Enable` button, `glassEnableButton` | `boolean glassEnabled`; toggled by `toggleGlassEnabled()`, also set by `cycleGlassTintPreset()`, `applyGlassPresetValues()`, `assignSelectedAsGlass()` | `nativeSetLightingControls(..., int glassEnabled, ...)` | `material.glassEnabled`; `syncGlassState()` uses it for `transparentAllowed`; `recordFrame()` may zero it for non-active slots | `pc.material.glassEnabled`; set to live value for active glass pass, forced `0` for non-active/selected opaque pass | `glassActive = materialRoleIsGlass && pc.glassEnabled != 0`; `transparentGlassRequested` also requires it | `glassEnabledControlsShaderBranch`, `glassMaterialStatus`, `transparentGlassPassStatus`, `material.Glass` summary fields | Yes, but only when material role resolves to Glass and routing allows it | High |
| `glassRenderMode` | `Glass Render: Fake Safe/Transparent v1/Auto` | `int glassRenderModeIndex`; `cycleGlassRenderMode()` | `nativeSetLightingControls(..., int glassRenderMode, ...)` | `material.glassRenderMode`; `syncGlassState()` treats `1` as Transparent v1 and `2` Auto as transparent only when motion scale > 0.62 | `pc.material.glassRenderMode`; active glass pass forced to `1`, non-active forced `0` | `transparentGlassRequested` requires `pc.glassRenderMode == 1`; debug views 66/67/72 show mode | `glassRenderModeStatus`, `activeGlassRenderMode`, `glassAutoFallbackStatus`, `glassMotionFallbackStatus` | Yes, controls transparent vs fake/inactive route | High |
| `glassOpacity` | `Glass Opacity` slider/button | `float glassOpacity`; preset/tint functions can overwrite it | `nativeSetLightingControls(..., float glassOpacity, ...)` | `material.glassOpacity`; `syncGlassState()` also computes CPU effective alpha | `pc.material.glassOpacity` from `material`, unless slot branch disables glass | `glassOpacityInput`; contributes to `centerAlpha`, `transparentGlassAlpha`, ambient scaling, debug view 53 | `glassOpacity`, `glassOpacityValue`, `glassLiveOpacity`, `glassShaderOpacityInput`, `glassFinalCenterAlpha`, `glassEffectiveCenterAlpha`; legacy `materialSlotDiagnostics.legacyGlassOpacityApplied` | Yes for active glass; no for non-glass slots | High |
| `glassTintPreset / glassTintColor` | `Glass Tint` button | `int glassTintPresetIndex`; `glassTintColorStatus()` duplicates C++/shader mapping | `nativeSetLightingControls(..., int glassTintPreset, ...)` | `material.glassTintPreset`; `glassTintColorString()` duplicates shader mapping | `pc.material.glassTintPreset` | `glassTintColor(pc.glassTintPreset)` and `glassPresetParams(pc.glassTintPreset)`; affects tint, preset reflection input | `glassTintColor`, `glassFinalTintColor`, `activeGlassTintPreset`; legacy `materialSlotDiagnostics.legacyGlassTintApplied` | Yes, but shader also has its own hardcoded tint/preset tables | Medium |
| `glassClarity` | `Glass Clarity` slider/button | `float glassClarity`; overwritten by glass presets | `nativeSetLightingControls(..., float glassClarity, ...)` | `material.glassClarity`; CPU effective alpha estimate uses it | `pc.material.glassClarity` | `glassClarityInput`; affects center alpha, haze, spec/glint, ambient, debug view 77 | `glassClarityValue`, `glassLiveClarity`, `glassShaderClarityInput`, `glassEffectiveCenterAlpha`; `material.glassClarity` in runtime JSON | Yes for active glass | Medium |
| `glassThickness` | `Glass Thickness` slider/button | `float glassThickness`; overwritten by glass presets | `nativeSetLightingControls(..., float glassThickness, ...)` | `material.glassThickness`; CPU effective edge estimate uses it | `pc.material.glassThickness` | `glassThicknessInput`; affects edge alpha, thickness tint, debug views 60/76 | `glassThicknessValue`, `glassLiveThickness`, `glassShaderThicknessInput`, `glassEffectiveEdgeAlpha`; `material.glassThickness` in runtime JSON | Yes for active glass edges | Medium |
| `glassRoughness` | `Glass Rough` slider/button | `float glassRoughness`; overwritten by glass presets | `nativeSetLightingControls(..., float glassRoughness, ...)` | `material.glassRoughness`; selected glass preset may also set selected slot roughness | `pc.material.glassRoughness` | `glassRoughInput`; used when `glassActive` to mix final roughness | `glassRoughnessValue`, `glassRoughnessResponseStatus`; legacy `materialSlotDiagnostics.legacyGlassRoughnessApplied` | Yes for active glass reflection/specular response | Medium |
| `glassEdge` | `Glass Edge` slider/button | `float glassEdge`; overwritten by glass presets | `nativeSetLightingControls(..., float glassEdge, ...)` | `material.glassEdge`; CPU effective edge estimate approximates it | `pc.material.glassEdge` | `glassEdgeInput`; affects Fresnel, reflection, edge alpha, debug views 54/59/75 | `glassEdgeValue`, `glassEffectiveEdgeAlpha`, `glassEdgeReflectionStatus` | Yes for active glass edge/rim | Medium |
| `selectedMaterialSlot` | `Slot -`, `Slot +`, `Select Glass Candidate` | `int selectedMaterialSlot`; `selectedMaterialJson()` clamps using diagnostics length | `nativeSetLightingControls(..., int selectedMaterialSlot, ...)` | `RendererCore::selectedMaterialSlot`; `syncMaterialSlotEditorState()`; `recordFrame()` applies selected overrides to selected slot or active glass | Not a direct shader field, but determines which slot gets selected overrides before push | Indirect through per-draw `pc.material.*` values | `selectedMaterialSlot`, `selectedSlotDisplay`, `selectedVsActiveGlassSlotStatus`, status text | Yes for selected slot overrides; can be unrelated to active glass slot | High |
| `activeTransparentGlassSlots` | No direct full-list UI; summary in Material status | Java computes `activeGlassSlots` in `applyGlassDiagnostics()` from diagnostics roles/manual override | No direct list argument; C++ recomputes list from runtime slots and manual override | `activeTransparentGlassSlots[4]`, `activeTransparentGlassSlotCount`, legacy `activeTransparentGlassSlot` | Not a list in push constants; per-draw routing decides active slot; `activeGlassSlotCount` pushed | Shader only sees `activeGlassSlotCount`, not list; role forced to Glass for active draw | `glassActiveSlotList`, `activeGlassSlotDisplay`, `transparentGlassSlotsRendered`, legacy `activeTransparentGlassSlot` | Yes, decides which primitives are skipped in opaque pass and drawn in transparent pass | High |
| `activeTransparentGlassSlotCount` | Read-only Material status | Java `activeGlassSlots.size()` | No direct arg | `activeTransparentGlassSlotCount` in C++ | `pc.material.activeGlassSlotCount` | Used for layer compensation via `glassLayerComp` | `glassActiveSlotCount`, `transparentGlassSlotsRendered`, `transparentGlassSlotLimit`, `activeGlassSlotCount` | Yes, changes alpha/RGB compensation | Medium |
| `materialPresetHint` | `Preset` cycle + `Apply Preset`; Glass preset sets index 6 | `activeMaterialPresetIndex`; `applyActiveMaterialPreset()` mutates many sliders; GLB import sets per-slot preset hint | `nativeSetLightingControls(..., int materialPreset, ...)`; material data carries per-slot hint | `material.materialPresetHint`; `slot.materialPresetHint`; activeMaterialPreset mirrors it | `pc.material.materialPresetHint`; per-slot hint loaded, selected/active glass can override to 6 | `materialRoleIsGlass = hint == 6 || pc.materialPresetHint == 6`; `clearcoatMaterialWeight()` also checks preset 1 | `activeMaterialPreset`, `materialPresetAppliedStatus`, `materialSlotDiagnostics.materialPresetHint-ish via type`; no single final shader truth field | Yes; can activate glass shader even when `materialTypeHint` is not Glass | High |
| `materialTypeHint` | No direct slider; shown in status/debug | Derived from glTF metadata/name/factors in parser and diagnostics role code | Passed in `materialData` slot array, not lighting JNI | `MaterialSlotState.materialTypeHint`; `material.materialTypeHint` defaults/first slot | `pc.material.materialTypeHint`; active glass pass forced to `6` | `hint`; controls material role, roughness remap, debug colors, glass branch | `materialSlotDiagnostics.materialTypeHint`, `selectedMaterialTypeHint`, role diagnostics | Yes; primary role input, can be overridden by active glass pass | High |
| `alphaMode / alphaCutoff` | `Alpha Cutoff` slider; alpha debug button | `alphaCutoffValue`; `materialSlotDiagnostics.alphaMode`; selected slot seeding can overwrite cutoff | `nativeSetLightingControls(..., float alphaCutoffValue, ...)`; material data carries per-slot alpha mode/cutoff | `slot.alphaMode`, `slot.alphaCutoff`, `material.alphaCutoff`; active glass pass forces `alphaMode = 0` | `pc.material.alphaMode`, `pc.material.alphaCutoff` | Early discard only if not transparent glass and `alphaMode == MASK`; transparent glass bypasses discard | `alphaModeSupportStatus`, `alphaCutoffValue`, `material.alphaMode` hardcoded as `"OPAQUE"` in runtime material object | Yes for cutout; active glass deliberately bypasses alpha discard | Medium |
| `materialSlotDiagnostics` glass fields | Not direct controls; Java parses them for slot UI and role selection | `modelState.materialSlotDiagnostics`; parsed by `selectedMaterialJson()`, role scoring, seeding overrides | `nativeSetPbrDiagnostics(..., String materialSlotDiagnostics)` | `model.materialSlotDiagnostics` string; C++ parses it by substring for names/extensions | No direct push, except it affects role/list selection before push | No direct shader variable | `materialSlotDiagnostics[]` includes `glassAppliedStatus`, `legacyGlassOpacityApplied`, `legacyGlassTintApplied`, `legacyGlassRoughnessApplied`, `glassEdgeReflectionApplied`, `glassGuardApplied` | Unclear/indirect: it can influence role routing but the glass fields are not shader truth | High |

Safety summary:

| Parameter | Safe now? | Reason |
|---|---|---|
| `glassEnabled` | No | Diagnostics can report active glass from role/preset/slots while shader still requires enabled branch. |
| `glassRenderMode` | No | Auto/Fake/Transparent share wording and Auto can route to fake fallback. |
| `glassOpacity` | No | UI live value, legacy material report, CPU estimates, and shader formula can diverge. |
| `glassTintPreset / glassTintColor` | Partial | Live value reaches shader, but tint tables are duplicated in Java/C++/GLSL. |
| `glassClarity` | Partial | Live value reaches shader, but CPU alpha estimates are separate. |
| `glassThickness` | Partial | Live value reaches shader, but CPU edge estimates are separate. |
| `glassRoughness` | Partial | Live value reaches shader, but selected-slot roughness overrides and legacy reports can confuse it. |
| `glassEdge` | Partial | Live value reaches shader, but diagnostics estimate edge alpha separately. |
| `selectedMaterialSlot` | No | Selection and active transparent glass routing are separate concepts but still interact in override logic. |
| `activeTransparentGlassSlots` | Partial | C++ routing uses list, but Java and legacy single-slot diagnostics duplicate the meaning. |
| `activeTransparentGlassSlotCount` | Partial | It reaches shader as layer count, but Java/C++ can compute it separately. |
| `materialPresetHint` | No | Preset can activate Glass role and mutate sliders. |
| `materialTypeHint` | No | Java and C++ duplicate role scoring and active glass can force hint 6. |
| `alphaMode / alphaCutoff` | Partial | Cutout is preserved, but active glass forces alpha mode and runtime material JSON writes opaque. |
| `materialSlotDiagnostics` glass fields | No | Metadata/legacy report, not shader truth. |

## 4. Conflicting Truths Found

1. UI/live glass values differ from `materialSlotDiagnostics`.
   - UI live values are fields such as `glassOpacity`, `glassClarity`, `glassThickness` in `apps/engine/src/main/java/com/solum/engine/MainActivity.java` around lines 211-218 and sliders around 692-710.
   - `materialSlotDiagnostics()` emits legacy constants like `legacyGlassOpacityApplied: 0.44`, `legacyGlassTintApplied: clear_default`, and `legacyGlassRoughnessApplied` around lines 6960-7017.
   - These diagnostics are static material reports, not the values pushed to the shader.

2. Preset can overwrite live slider values after the user changed them.
   - `applyActiveMaterialPreset()` calls `applyGlassPresetValues()` for preset 6 in `MainActivity.java` around 1032-1045.
   - `cycleGlassTintPreset()` also calls `applyGlassPresetValues()` around 1079-1084.
   - `applyGlassPresetValues()` changes opacity, edge, roughness, clarity, thickness, and reflection intensity together around 1094-1104.

3. Selected slot overrides can replace source material values and can affect active glass.
   - In `engine-core/solum-vulkan-core/src/solum/renderer_core.hpp` around 1736-1848, `recordFrame()` applies selected override values when `range.materialSlot == selectedMaterialSlot || rangeActiveGlass`.
   - For active glass, `materialPresetHint` and `materialTypeHint` are forced to Glass, `glassRenderMode` is forced to Transparent v1, and alpha mode is forced opaque.

4. Glass Enabled can be bypassed by role/hint diagnostics as an "active" status.
   - C++ `syncGlassState()` around `renderer_core.hpp` 879-927 and Java `applyGlassDiagnostics()` around `MainActivity.java` 1779-1815 mark glass state active if enabled, selected glass, preset 6, or glass slots exist.
   - The shader still requires `pc.glassEnabled != 0` for `glassActive` in `triangle.frag.glsl` around 299-324, so diagnostics can look active while shader glass branch is off.

5. Debug views are not final shader output proof.
   - Debug views 52-79 return early and show masks, opacity, Fresnel, tint, routing, alpha, and composite approximations.
   - They do not prove final blended framebuffer pixels.

6. CPU diagnostics compute alpha/RGB separately from shader.
   - Java sets `glassFinalCenterAlpha = clamp(glassOpacity, 0, 1)` and `glassFinalEdgeAlpha = clamp(0.06 + glassEdge * 0.20 + glassThickness * 0.08, ...)` around `MainActivity.java` 1917-1919.
   - C++ computes a newer effective alpha estimate using center alpha, edge alpha, and layer compensation around `renderer_core.hpp` 1034-1049.
   - Shader computes `centerAlpha`, `edgeAlpha`, `transparentGlassAlpha`, and `transparentGlassRgb` independently in `triangle.frag.glsl` around 417-450.

7. Fake glass path still exists beside Transparent v1.
   - Render mode 0 is `Fake Safe`.
   - Auto can fall back to fake safe when motion guard blocks transparent routing.
   - Diagnostics still report `fakeGlassFallbackStatus`, `glassStillFakeTransparencyStatus`, `glassTransparencyMode`, and P24/P25/P26 fake probe/polish statuses.

8. Active glass slots and selected slot are mixed by meaning.
   - UI selects one `selectedMaterialSlot`.
   - Transparent routing can use all detected glass slots or a manual override.
   - `activeTransparentGlassSlot` is legacy single-slot diagnostic, while actual routing uses `activeTransparentGlassSlots[4]`.

9. `materialSlotDiagnostics` is not shader truth.
   - It is generated on Java import and passed as a JSON string for reporting/role scoring.
   - Shader gets `PushConstants`, not `materialSlotDiagnostics`.
   - P28I already marks `legacyGlassMaterialReportNotShaderTruth = yes`.

10. P25/P26 fake glass fields remain beside P27/P28 transparent glass fields.
    - Examples: `glassPolishMode = single_pass_fake_glass_polish`, `glassReflectionSourceStatus = p24_fake_cubemap_probe`, `transparentGlassFallbackStatus`, `fakeGlassFallbackStatus`.
    - These can make Auto/Fake/Transparent read as one system when they are separate routes.

## 5. Fake / Legacy Glass Layers

- P25 fake glass:
  - Single-pass/fake opacity terminology remains in defaults and diagnostics.
  - Legacy per-slot fields still say glass was "applied" in material reports.

- P26 fake polish:
  - P24/P26 procedural reflection probe and fake polish statuses remain.
  - They are shader math/probe approximations, not real cubemap/refraction/mirror proof.

- P27 transparent pass:
  - Adds transparent pipeline and draw order, but initially used selected-slot semantics.
  - Current code uses a slot list, while `activeTransparentGlassSlot` remains as legacy diagnostic.

- P28 visual attempts:
  - Adds clarity/thickness/final composite diagnostics and formula status fields.
  - These fields still report formula/state, not pixel proof.

Current intersection:

```text
Fake Safe mode
Auto fallback/motion guard
Transparent v1 active slot list
Legacy materialSlotDiagnostics glass fields
P24/P25/P26 reflection/glass polish status fields
P28 CPU estimates
Shader final branch
```

These must not be treated as one truth source.

## 6. Debug / Diagnostics Problems

- `glassVisualQualityStatus`, `p28VisualGlassStatus`, `glassFinalShadedFormulaStatus`, and similar fields are not visual proof.
- `debugViewStatus = shader_applied` only means a debug branch exists/was selected.
- `materialSlotDiagnostics` glass fields are legacy import/material reports, not final shader inputs.
- CPU effective alpha fields are estimates and can diverge from GLSL formulas.
- `runtime_vulkan_caps.json.material.alphaMode` writes `"OPAQUE"` even though per-slot alpha modes exist and active glass forces alpha mode during draw.
- Java and C++ both implement material role/glass candidate scoring; drift between them would change UI selection vs render routing.
- `Auto` mode can become fake fallback due to motion scale, while UI still reads like a glass render mode.
- No pixel readback exists; diagnostics explicitly report screenshot/readback unavailable.

## 7. Required Cleanup Plan — P29B

Remove or demote:

- Remove legacy per-slot `legacyGlassOpacityApplied`, `legacyGlassTintApplied`, `legacyGlassRoughnessApplied`, and `glassAppliedStatus` from any "shader truth" interpretation.
- Remove `ok` wording from glass visual diagnostics that do not have pixel/manual proof.
- Remove duplicate CPU final alpha formulas or mark them as estimates only.

Rename:

- `activeTransparentGlassSlot` -> `legacyActiveTransparentGlassSlot`.
- `glassFinalCenterAlpha` / `glassFinalEdgeAlpha` -> `glassCpuEstimatedCenterAlpha` / `glassCpuEstimatedEdgeAlpha` unless generated from the exact shader formula.
- `glassPresetMixRemovedStatus` -> `glassShaderPresetMixStatus`.
- `glassVisualQualityStatus` -> `glassVisualVerificationStatus`.

Single source:

- UI live material controls should flow to one Java state object.
- JNI should pass that state without preset or diagnostics side effects.
- C++ `MaterialConstants` should be the runtime source for global live controls.
- Per-draw push constants should be the only shader truth.
- `materialSlotDiagnostics` should be metadata/report only.

Legacy to keep with explicit label:

- P24 fake cubemap/procedural probe fields can remain as reflection approximation diagnostics.
- P25/P26 fake glass fallback can remain only as `legacyFakeGlassFallback`, never as proof for Transparent v1.
- Single active glass slot can remain only as a legacy display alias of the first list item.

Make diagnostics honest:

- Add `shaderTruthSource = push_constants_per_draw`.
- Add `materialSlotDiagnosticsTruth = metadata_only_not_shader_input`.
- Add `debugViewsAreVisualProof = false`.
- Add `pixelReadbackStatus = not_implemented`.
- Add `glassRoute = fake_safe | transparent_v1 | auto_fallback`.
- Add `glassRouteReason` with exact branch reason.

## 8. Required Glass Proof Plan — P29C

After P29B cleanup:

- Build a simple transparent proof path with one known Glass role and one non-glass object behind it.
- Add final alpha debug that reports the final shader alpha used for blended output.
- Add final RGB debug that reports the final shader RGB before blending.
- Verify opacity values `0.0`, `0.2`, and `0.6`.
- Proof must use Transparent v1, not fake fallback.
- Do not use `materialSlotDiagnostics` as proof.
- Do not write `visual ok` without pixel readback or explicit manual screenshot proof.

## 9. Acceptance Rules For Future Material Patches

- One UI parameter must have one path to shader.
- Diagnostics must not write `visual ok` without pixel readback or manual proof.
- Presets may fill sliders, but must not secretly modify values after live slider edits.
- Fake path is not the main material/glass path.
- Before visual polish, run a truth audit.
- `materialSlotDiagnostics` is metadata/report only.
- Debug views are diagnostics, not final screen proof.
- Selected slot and active transparent glass slot list must stay separate in naming and diagnostics.
- Any CPU estimate must be named as an estimate.
