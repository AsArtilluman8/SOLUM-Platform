# ADR-0003: Launcher-managed multi-APK ecosystem as future target

## Status

Accepted.

## Decision

Final SOLUM Platform target is launcher-managed multi-APK ecosystem.

SOLUM Launcher will manage:

- installed apps;
- app versions;
- manual updates;
- compatibility;
- diagnostics;
- projects;
- opening tools.

Apps are separate tools around SOLUM Engine.

## Why

SOLUM tools map to real game-dev roles:

- Material artist → Material Studio.
- VFX artist → VFX Studio.
- Animator → Motion Studio.
- World/level artist → World Studio.
- Sound designer → Sound Studio.
- Narrative designer → Quest/Dialogue Studio.
- Programmer/runtime → SOLUM Engine.

Separate tools provide isolation and professional workflows.

## Startup constraint

Do not start with 15 APK.

Start narrow:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

Launcher full update system comes later, when there are real apps to manage.

## Android install limitation

Normal Android launcher app cannot silently install/update APKs like Google Play.

Flow:

```text
Launcher sees update
↓
user presses Update
↓
Launcher downloads/opens APK
↓
Android system installer asks confirmation
↓
user confirms
```

No auto-update without user confirmation.

## Rejected

- One huge app forever.
- 15 APK immediately.
- Full Google Play-like silent updater.
- Launcher before core tools exist.
