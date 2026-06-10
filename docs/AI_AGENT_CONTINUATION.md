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

1. P52 Environment Asset Pipeline + HDRI/IBL/Skybox Starter Pack.
2. P53 Scene Workspace / multi-model scene.
3. P54 Asset Shelf.
4. P55 Transform Gizmo.
5. P56 Animation Preview.
6. P57 Scene Save/Load.
7. Labs and package/agent systems later.

## Active near-term patch

Current near-term work:

```text
P52 — Environment Asset Pipeline + HDRI/IBL/Skybox Starter Pack
```

Main goal:

```text
P51 created `EnvironmentApi`, time of day, sun/moon intent, IBL/skybox slots, stars placeholder state, fallback, and diagnostics. P52 adds source/license manifest, asset paths, validation tooling, and safe loader slots. Actual KTX assets remain P52B if `cmgen`/`toktx` are unavailable.
```

## Important rules

- Read repo files before editing.
- Do not add new visual features before fixing current control truth.
- Do not add a huge API UI until existing controls work.
- Render Control Center is UI over the existing Render API; do not add new visual renderer features in P50.
- Sky / IBL has P51 Environment API controls and P52 asset status; real HDRI/star KTX assets remain planned for P52B unless converted with verified tools/licenses.
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
- `docs/design/SOLUM_RENDER_CONTROL_CENTER.md`
- `docs/design/SOLUM_ENVIRONMENT_API.md`
- `docs/design/SOLUM_ENVIRONMENT_ASSET_PIPELINE.md`
- `docs/assets/SOLUM_ENVIRONMENT_ASSETS.md`
- `assets/env/ENVIRONMENT_ASSETS_MANIFEST.json`
- `tools/env_asset_manifest_check.py`

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
