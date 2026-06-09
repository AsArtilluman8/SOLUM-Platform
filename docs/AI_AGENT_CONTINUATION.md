# AI Agent Continuation Guide

Status: public continuation guide for Codex and other code agents.

This file is the first stop for AI/code agents continuing work on SOLUM.

## Mandatory local gate

Every agent task should start with:

```bash
bash tools/agent_gate.sh
```

Manual bypass is allowed only if the tools are missing/broken or the task is explicitly about fixing the tools. If skipped, the final report must include `Tools skipped: reason`.

## Current project truth

SOLUM is an early-stage mobile-first game engine and creative toolchain. The current active foundation is Android + Filament renderer/editor work.

Do not claim planned systems are implemented.

## Current priority order

1. FPS Truth + Control Truth.
2. Mobile Performance Targets.
3. Render API Extraction.
4. Full Render Control Center.
5. Scene Workspace / multi-model scene.
6. Asset Shelf.
7. Transform Gizmo.
8. Animation Preview.
9. Scene Save/Load.
10. Labs and package/agent systems later.

## Active near-term patch

Current near-term work:

```text
P47B — Control Interaction Audit + FPS Truth v2
```

Main goal:

```text
FPS must not mislead.
Current buttons and sliders must work or report truth.
Quality profiles must apply real render state.
```

## Important rules

- Read repo files before editing.
- Do not add new visual features before fixing current control truth.
- Do not add a huge API UI until existing controls work.
- Do not use Java callback FPS as primary FPS.
- Do not fake applied state if a setting is requested-only or requires recreate.
- Keep public docs free of private user/device/account details.

## Key files

- `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java`
- `docs/audits/FILAMENT_API_SURFACE_AUDIT.md`
- `docs/audits/FILAMENT_RUNTIME_FORENSICS.md`
- `docs/design/SOLUM_CONTROL_TRUTH.md`
- `docs/design/SOLUM_RENDER_FOUNDATION_PLAN.md`
- `docs/design/SOLUM_MOBILE_PERFORMANCE_TARGETS.md`
- `docs/design/SOLUM_QUALITY_PROFILES.md`
- `docs/roadmap/SOLUM_ROADMAP.md`
- `BUILD.md`
- `docs/TESTING.md`
- `AGENTS.md`
- `solum_agent_state.json`
- `tools/agent_gate.sh`
- `tools/agent_context.sh`
- `tools/agent_repo_health.sh`
- `tools/agent_control_truth_static_check.py`
- `tools/agent_build_report.sh`

## Build command shape

```bash
bash tools/build_native_engine.sh
gradle --no-daemon -p "$PWD" clean assembleDebug
```

Expected debug APK:

```text
apps/engine/build/outputs/apk/debug/engine-debug.apk
```

## Required final report from agents

Every agent task should report:

- changed files;
- build result;
- APK path;
- commit SHA;
- what was fixed;
- what remains broken;
- what was verified;
- what was not verified;
- next recommended patch.
