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

1. P53 Real Environment Assets + Sky Visual Layer + Smooth Weather Foundation.
2. P54 dedicated cloud shadows / rain / snow VFX / performance, if not completed in P53.
3. P55 Scene Workspace / multi-model scene.
4. P56 Asset Shelf.
5. P57 Transform Gizmo.
6. P58 Animation Preview.
7. Labs and package/agent systems later.

## Active near-term patch

Current near-term work:

```text
P53 — Real Environment Assets + Sky Visual Layer + Smooth Weather Foundation
```

Main goal:

```text
P51 = Environment API/time system. P52 = manifest/asset slots/fallback. P53 = real starter assets attempt, smooth sky visual layer, and cheap cloud/weather foundation. If `cmgen`/`toktx` are unavailable, do not fake KTX assets; keep fallback/status honest. P54 next = dedicated cloud shadows/rain/snow VFX/performance if not completed in P53.
```

## Important rules

- Read repo files before editing.
- Do not add new visual features before fixing current control truth.
- Do not add a huge API UI until existing controls work.
- Render Control Center is UI over the existing Render API; do not add new visual renderer features in P50.
- Sky / IBL has P51 Environment API controls, P52 asset status, and P53 smooth sky/weather foundation. Real HDRI/star KTX assets require verified conversion tools/licenses.
- True volumetric clouds are future, not P53.
- Cheap clouds use sun attenuation now and future scrolling noise/layer + projected soft cloud shadow masks.
- Skybox/IBL does not cast hard shadows; sun/moon directional lights do.
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
- `tools/env_asset_pack_build.py`
- `docs/design/SOLUM_SKY_WEATHER_FOUNDATION.md`

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
