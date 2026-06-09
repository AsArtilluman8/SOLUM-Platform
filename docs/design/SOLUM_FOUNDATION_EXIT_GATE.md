# SOLUM Foundation Exit Gate

Status: P46 foundation gate for leaving renderer foundation.

SOLUM can leave renderer foundation only when the renderer is understandable, auditable, and reusable by future tools. A powerful Activity-local preview is not enough.

## Foundation Is Usable When

1. Render API exists and is not only Activity code.
   - Render ownership, lifecycle, settings, and diagnostics must move toward reusable modules.

2. Diagnostics do not mislead the user.
   - Main HUD shows estimated visible FPS first.
   - Java callback FPS is debug-only.
   - GPU timing is not claimed unless measured.

3. Multi-model scene exists.
   - More than one model can be added, selected, transformed, saved, and unloaded.

4. Asset Shelf v1 exists.
   - Assets can be browsed, validated, previewed, and added to the scene without manual file hunting.

5. Gizmo / Transform Tool v1 exists.
   - Position, rotation, and scale are controlled by touch-first world/local gizmo, with numeric precision as secondary control.

6. Animation Preview v1 exists.
   - Loaded glTF/GLB clips can be listed, played, paused, scrubbed, and reported in diagnostics.

7. Scene save/load v1 exists.
   - Scene state persists separately from temporary render preview settings.

8. Render Control Center has Basic / Advanced / Debug structure.
   - Basic: safe visible controls.
   - Advanced: deeper quality and render settings.
   - Debug: callback FPS, FrameMetrics, profiler commands, not_exposed states.

9. Future Labs can reuse render/settings/diagnostics modules.
   - New labs must not copy-paste Activity render code.

## Future Module Path

1. Render Core
   - Engine/Renderer/View/Surface lifecycle.
   - Render settings model.
   - Frame timing diagnostics model.

2. Scene Workspace
   - Scene graph.
   - Selection state.
   - Object lifecycle.

3. Asset Shelf
   - Asset scan/import/validation.
   - Add asset to scene.
   - Asset unload/release rules.

4. Transform Gizmo
   - Touch-first position/rotation/scale.
   - World/local modes.
   - Snap and precision controls.

5. Animation Preview
   - Clip list.
   - Playback/scrub.
   - Skeleton/morph support audit.

6. PostProcess Studio
   - Color grading, bloom, fog, AA, output quality.

7. Light / Shadow Studio
   - Sun, point, spot, IBL, shadow controls, shadow diagnostics.

8. Material / Glass Studio
   - Material slots, runtime overrides, alpha/transmission/refraction truth.

9. Performance Profiler
   - HUD Light, FrameMetrics, gfxinfo, Perfetto/AGI workflow, native hooks if needed.

10. Later Labs
   - Material Lab.
   - VFX Lab.
   - Water Lab.
   - Physics Reaction Lab.

## Exit Rule

Renderer foundation is not finished while render truth, settings, assets, scene state, transform control, animation preview, and diagnostics remain trapped inside one Activity. P46 locks the diagnostics language and API surface so the next patches can extract reusable systems without adding more visual features first.
