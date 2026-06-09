# AI Agent Continuation Guide

Status: public continuation guide for Codex and other code agents.

This file is the first stop for AI/code agents continuing work on SOLUM.

## Mandatory local gate

Every agent task should start with:

```bash
bash tools/agent_gate.sh
bash tools/agent_brief.sh
```

Manual bypass is allowed only if the tools are missing/broken or the task is explicitly about fixing the tools. If skipped, the final report must include `Tools skipped: reason`.
If the generated brief is insufficient, open exact files and line ranges. Token optimization must not reduce code quality or verification.

## Current project truth

SOLUM is an early-stage mobile-first game engine and creative toolchain. The current active foundation is Android + Filament renderer/editor work.

Do not claim planned systems are implemented.

## Current priority order

1. P49 Render API Contract + Diagnostics On Demand + FPS Confidence.
2. P50 Full Render Control Center.
3. P51 Scene Workspace / multi-model scene.
4. P52 Asset Shelf.
5. P53 Transform Gizmo.
6. P54 Animation Preview.
7. P55 Scene Save/Load.
8. Labs and package/agent systems later.

## Active near-term patch

Current near-term work:

```text
P49 — Render API Contract + Diagnostics On Demand + FPS Confidence
```

Main goal:

```text
Render API must cover current render-related controls, diagnostics must be on-demand, and FPS must report frame ms, confidence, stability, and disagreement honestly.
```

## Important rules

- Read repo files before editing.
- Do not add new visual features before fixing current control truth.
- Do not add a huge API UI until existing controls work.
- Do not add a full Render Control Center before P50.
- Keep live HUD lightweight; full diagnostics run by button/copy/export only.
- Do not use Java callback FPS as primary FPS.
- Frame ms is primary truth. FPS is derived from frame ms.
- Do not fake applied state if a setting is requested-only or requires recreate.
- Cost diagnostics are estimated until a future cost probe/profiler exists.
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
- `tools/agent_brief.sh`
- `tools/agent_render_api_static_check.py`
- `apps/engine/src/main/java/com/solum/engine/render/`
- `apps/engine/src/main/java/com/solum/engine/scene/`
- `docs/design/SOLUM_DIAGNOSTICS_ON_DEMAND.md`
- `docs/design/SOLUM_MALI_RENDER_OPTIMIZATION_RULES.md`

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
