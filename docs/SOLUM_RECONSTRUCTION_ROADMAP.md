# SOLUM Reconstruction Roadmap

Patch: `P_Recon_Toolkit_Integration_Workspace_Fix_01`

## Current Proof

| Area | Score | Status |
|---|---:|---|
| Blueprint | +++ | asset-derived JSON exports and node tables exist |
| Texture | +++ | asset candidates only; full decode later |
| Material | ++ | candidates and refs; graph topology unknown |
| VFX | ++ | Niagara/particle candidates; topology unknown |
| Audio | ++ | Sound candidates; WAV/OGG decode blocked |
| Mesh | HOLD | out of scope for Patch 1 |
| Animation | HOLD | out of scope for Patch 1 |
| Weather logic | ++ | scalar Rain preset and function/node hints verified |
| Camera/Gizmo | ++ | code-level real camera/world geometry foundation; runtime manual check still needed |

## Labels

- asset-derived: UAssetAPI JSON, node tables, inventory, UDS Rain scalar values.
- reconstructed: SOLUM status model, weather proof channels, workspace diagnostics.
- procedural fill: workspace helper geometry only, not UDS weather visuals.
- unknown: Pins, LinkedTo, PinId, exact Blueprint execution flow, Niagara/material topology.
- blocked: `BLOCKED_UNREAL_REFERENCE_ACCESS` until GitHub/Epic source access is available.
- fake-risk: treating light toolkit output as full weather/VFX implementation.

## Patch 1 Acceptance Boundary

- Fix Camera Orbit, Object Rotate separation, selection root, world-space grid/floor/gizmo foundation.
- Add proof/status layer from Recon Toolkit Light v1.
- Do not render rain, snow, clouds, lightning, puddles, or audio.
- Do not import meshes/animations/raw UDS assets.

## Next Steps

1. Runtime manual test of touch orbit, object rotate, selection, grid/floor/gizmo visibility.
2. Add real gizmo hit testing and drag only after world-space picking is proven.
3. Add selective Blueprint pin/link extraction stage before claiming execution-flow reconstruction.
4. Add texture/material/audio decode stages separately with licensing and build proof.
