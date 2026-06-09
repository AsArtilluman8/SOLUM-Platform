# SOLUM Render Control Center

Status: P50 UI foundation.

Render Control Center is a mobile UI over the existing render control surface. It does not add a renderer feature and must not pretend that planned systems are implemented.

## Purpose

P50 groups current render controls into phone-usable sections:

- Basic;
- Lighting;
- Sky / IBL;
- PostFX;
- Color / Fog;
- Debug.

The UI reads truth from:

- `RenderControlApi`;
- `RenderDiagnostics`;
- `RenderOwnershipMap`;
- `RenderFeatureDescriptor`;
- `RenderCostDiagnostics`;
- Activity-local status for controls that are not fully owned by the API yet.

## Sections

### Basic

Gameplay-safe controls:

- quality profile;
- compact FPS / frame ms status;
- render scale;
- dynamic resolution;
- MSAA;
- FXAA;
- TAA;
- dithering.

Labels must show requested/actual truth where relevant. Manual overrides must survive preset selection.

### Lighting

Controls and status:

- lighting preset;
- sun intensity;
- ambient / IBL intensity;
- fill intensity;
- background intensity;
- sun direction presets;
- light rig selector;
- light type/status summary.

Current color status is honest: sun/fill colors are fixed Activity-local values; ambient color is IBL/procedural and not exposed as a verified RGB control.

### Sky / IBL

P50 includes existing Sky / IBL controls and status:

- active IBL name/path;
- IBL intensity;
- IBL rotation;
- skybox visible on/off;
- IBL load/import/reload status.

Sky / IBL is foundation for future Sky / Sun / Time of Day work. Full Day/Night is planned for P51 and is not implemented in P50.

### PostFX

Controls and warnings:

- AO mode;
- Bloom mode;
- Bloom strength;
- Bloom highlight;
- SSR;
- Refraction;
- Sun glare;
- Shadow mode.

Cost warnings should come from descriptors/cost diagnostics where possible. Important mobile warnings:

- SSR is screenshot-only/gameplay unsafe until proven;
- MSAA 4x is high cost;
- AO strong/debug is expensive;
- Bloom high is expensive;
- TAA is not free and still confidence-pending.

### Color / Fog

Controls and status:

- color exposure;
- contrast;
- saturation;
- temperature;
- color mode/preset;
- fog mode;
- fog density;
- fog distance;
- fog height.

Fog visibility must be honest:

- fog off;
- visible likely;
- may be hidden by skybox/exposure;
- not verified.

### Debug

Debug is compact and on-demand:

- Copy Short Report;
- Export Full Report;
- Reset FPS/Jank Counters;
- FPS confidence/stability/timing disagreement compact text;
- ownership summary;
- cost cause summary;
- last report path/status.

Do not show full JSON live. Do not calculate full report every frame. Full report generation is button-driven.

## Mobile UX Rules

- Compact, collapsible, thumb-friendly.
- No giant always-open debug wall.
- Live HUD remains lightweight.
- One section is the focus at a time.
- Use short labels and requested/actual truth instead of long prose.
- Keep the viewport usable; Render Control Center is a bottom control surface, not a full-screen debug dump.

## Future Risks

Bad UI design here can create:

- duplicate owners for one render parameter;
- misleading labels that hide requested/actual mismatch;
- heavy live diagnostics every frame;
- controls that look final while the API reports `activity_local`, `partial`, `not_verified`, or `not_exposed`;
- expensive screenshot-only modes accidentally presented as gameplay-safe.

P50 must stay a safe foundation for later P51+ systems.
