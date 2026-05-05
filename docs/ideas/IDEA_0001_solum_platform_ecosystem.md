# IDEA-0001: SOLUM Platform ecosystem

## Raw idea

SOLUM should become not only one engine app, but a mobile game-dev ecosystem.

SOLUM Engine is the main Vulkan runtime. Around it there will be specialized apps/tools similar to professional game-dev team roles:

- Launcher
- Asset Hub
- AniStudio / Cutscene Studio
- Character Studio
- Motion Studio
- Material Studio
- VFX Studio
- Sound Studio
- World Studio
- UI Studio
- AI / Behavior Studio
- Quest / Dialogue Studio
- Mechanics Studio

## Modes for each app

Each tool should be able to work in three modes:

1. Standalone mode.
   Example: AniStudio creates anime video/MP4 without SOLUM game project.

2. SOLUM Project mode.
   Example: Cutscene Studio opens a SOLUM game project and uses its characters, sounds, assets and scenes.

3. Export mode.
   Example: export GLB/VRM/PNG/MP4/MP3/JSON/SOLUM formats, later Unreal-compatible exports where possible.

## Final target

Launcher-managed multi-APK ecosystem.

But start narrow:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

## Why valuable

This lets SOLUM become a platform where game-dev tasks are not rebuilt from scratch every time.

The user wants to spend less time fighting repeated systems and more time improving UX, graphics, story, lore, atmosphere and gameplay.

## Status

Accepted as final vision.

Implementation deferred and staged.
