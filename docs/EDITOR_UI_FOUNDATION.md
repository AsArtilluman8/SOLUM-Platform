# EDITOR_UI_FOUNDATION — compact Engine UI foundation

## P06 Direction

P06 keeps the Vulkan viewport as the primary surface and replaces large floating buttons with compact translucent controls.

## Top HUD

The top HUD shows:

- SOLUM Engine / SOLUM V2;
- frame count;
- GPU name;
- Vulkan;
- current lab/scene;
- compact upload status.

## Compact Panels

The left rail owns panel visibility:

- Assets;
- Camera;
- Debug.

Panels are collapsible. Default state:

- Assets visible;
- Camera collapsed;
- Debug collapsed.

## Assets

Visible controls:

- Import GLB;
- Scan Models.

The active model and upload/draw status stay in the compact status panel.

## Camera

Visible controls when expanded:

- Zoom In;
- Zoom Out.

Drag/pinch viewport camera controls remain the primary interaction.

## Diagnostics

Visible controls when expanded:

- Choose Diagnostics Folder;
- Export Diagnostics.

Diagnostics text is kept inside the bottom dock, not as a full-screen overlay.

## Style Rules

- dark translucent panels;
- cyan/blue accent stroke;
- small readable text;
- no large gray buttons over the scene center;
- no new heavy UI framework;
- no image assets.

## Known Limits

This is a foundation only. It does not implement a full editor, scene hierarchy, material editor, animation editor or texture tool.
