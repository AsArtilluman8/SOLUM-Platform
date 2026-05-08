# EDITOR_UI_FOUNDATION — compact Engine UI foundation

## P07 Direction

P07 keeps the Vulkan viewport as the primary surface, preserves compact translucent controls and makes diagnostics export reachable even when the Debug panel is hidden.

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
- Export.

Panels are collapsible. Default state:

- Assets visible;
- Camera collapsed;
- Debug collapsed.
- Export visible as a compact dock action.

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

Compact dock:

- Export Diagnostics stays directly visible as `Export` outside the expanded Debug panel.
- Debug details remain inside the collapsed/expanded Debug panel.

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
