# SOLUM Weather / Gizmo / Camera Roadmap

## Current Patch

`P_UDW_Weather_Showcase_Camera_Cleanup_01`

Focus:

- isolate old OpenGL ES weather;
- make camera gesture primary;
- add tap selection foundation;
- keep visible 3D RGB gizmo foundation;
- add UDS/UDW weather showcase without post-process;
- write proof/status reports.

## Next Recommended Patch

`P_UDW_Gizmo_Selection_Handles_02`

Scope:

- real gizmo axis hit testing;
- drag selected object through X/Y/Z handles;
- separate camera/gizmo InputRouter ownership;
- pivot anchor refinement;
- world/local mode UI;
- selection visual highlight without post-process first, then outline later.

## HOLD

- Grid editor.
- Selection outline/postprocess.
- Post-process bloom/lens flare/god rays/exposure.
- Deeper UAsset Blueprint pins and `LinkedTo` reconstruction.
- UDS/UDW material graph reconstruction.
- Weather audio extraction/playback.
- Better puddles/ripples.
- Weather performance pass.
- Moon/sun/star quality polish.
- True volumetric/fog later.

## Risk Controls

- Do not make camera drag rotate objects.
- Do not make gizmo a 2D overlay.
- Do not mark procedural defaults as asset-derived.
- Do not reintroduce OpenGL ES weather into app runtime.
