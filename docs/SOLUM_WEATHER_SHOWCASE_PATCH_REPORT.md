# SOLUM Weather Showcase Patch Report

Patch: `P_UDW_Weather_Showcase_Camera_Cleanup_01`

## Inventory Summary

- `tools/solum_weather_fast_inventory.py` output:
  - weather related files: `21`
  - legacy candidates after isolation: `0`
  - current Filament candidates: `5`
  - recon output present: `true`
  - Unreal access: `OK`

## Result Scale

- `++++` = built and directly verified.
- `+++` = strong foundation, build/static proof only.
- `++` = partial, needs device/runtime visual test.
- `+` = weak hypothesis.
- `HOLD` = consciously deferred.
- `BLOCKED` = blocked.
- `FAKE-RISK` = dangerous to count as real.

## Status

- Camera gesture owner: `+++`
  - One-finger drag routes to camera orbit.
  - Pinch routes to camera distance.
  - Two-finger center movement pans camera target.
  - Object rotate is no longer primary camera UX.

- Camera self-check: `++`
  - Runtime fields added:
    - `cameraPositionBefore`
    - `cameraPositionAfter`
    - `cameraMatrixChangedOnGesture`
    - `lastGestureType`
  - Needs device gesture to prove `++++`.

- Touch selection: `++`
  - Tap near active GLB root selects `active_model`.
  - Filament pick is still attempted.
  - Mesh-level picking remains deferred.

- 3D RGB gizmo foundation: `++`
  - World-space X/Y/Z axes remain scene renderables.
  - `fakeOverlayUsed=false`.
  - Drag handles are `HOLD`.
  - If helper material package/model material is unavailable, gizmo reports `BLOCKED` instead of pretending.

- Weather showcase: `++`
  - Filament scene geometry:
    - sky dome;
    - sun/moon markers;
    - cloud layer;
    - rain/snow line particles;
    - wet floor overlay;
    - lightning bolt.
  - No post-process bloom/lens flare/god rays.
  - No 2D weather overlay.

- Asset/audio candidates: `HOLD`
  - Decoded PNG/WAV candidates exist locally but are not committed into app assets because they may be paid-asset-derived.
  - Audio playback is not wired; runtime status is `missing_assets` unless private assets are supplied later through the private asset pipeline.

- Legacy OpenGL ES cleanup: `+++`
  - Old weather Activity and weather_v* app asset trees isolated out of app source/assets.
  - Current Filament runtime preserved.

## Proof Fields

Runtime/debug now reports:

- `cameraStatus`
- `cameraPositionBefore`
- `cameraPositionAfter`
- `cameraMatrixChangedOnGesture`
- `lastGestureType`
- `selectedObjectName`
- `selectedObjectStatus`
- `gizmoVisible`
- `gizmoMode`
- `weatherPreset`
- `timeOfDay`
- `cloudCoverage/rain/snow/fog/wind/wetness/lightning`
- `legacyCleanupStatus`
- `environmentMode`
- `weatherSkyMode`
- `skyIs2DOverlay=false`
- `fakeOverlayUsed=false`

## Not Done

- `HOLD`: gizmo drag/hit handles.
- `HOLD`: grid editor.
- `HOLD`: selection outline/post-process.
- `HOLD`: UDS/UDW material graph reconstruction.
- `HOLD`: bloom/lens flare/god rays/exposure.
- `HOLD`: real weather audio playback.
- `HOLD`: private decoded texture/audio bundling until licensing/provenance is clean.
- `HOLD`: better puddles/ripples.
- `HOLD`: performance pass after device run.

## Fake Risk

- UDS/UDW channels without proof are marked `procedural fill`, not asset-derived.
- Weather geometry is a SOLUM runtime foundation, not a copied UDS system.
- Visual runtime needs device confirmation before any `++++` claim.
