# SOLUM Project Memory Index

Этот файл — главный вход в память проекта. Любой новый чат, GPT, Claude, Codex или другой агент должен начинать отсюда.

## Как читать проект перед работой

Обязательный порядок:

1. `docs/CURRENT_STAGE.md`
2. `docs/AGENT_RULES.md`
3. `docs/ARCHITECTURE_RULES.md`
4. `docs/UX_AND_WORKFLOW_RULES.md`
5. `docs/PATCH_ROADMAP.md`
6. `docs/ASSET_FORMAT_SPEC.md`
7. `docs/DIAGNOSTICS_SPEC.md`
8. `docs/BUILD_ENV_SPEC.md`
9. `docs/PERFORMANCE_BUDGETS.md`

Для UI/editor/input задач дополнительно:

- `docs/DESIGN_TOKENS.md`
- `docs/INPUT_AND_GESTURE_ARCHITECTURE.md`
- `docs/EDITOR_CORE_COMPONENTS.md`

Для Vulkan/render/resource задач дополнительно:

- `docs/RENDERING_TARGET_SPEC.md`
- `docs/RENDER_RESOURCE_LIFETIME_RULES.md`
- `docs/VISUAL_QA_SPEC.md`
- `docs/errors/ERROR_KNOWLEDGE_BASE.md`
- `docs/research/REPOSITORY_REFERENCE_CATALOG.md`

Для Studio app задач дополнительно:

- `docs/STUDIO_APP_MODEL.md`

Для сложных систем обязательно:

- `docs/research/RESEARCH_GATE_RULES.md`
- `docs/research/PATCH_RESEARCH_TEMPLATE.md`

Сложные системы: Vulkan/render/shadows/materials/water/terrain/vegetation/VFX/animation/assets/ECS/mechanics/diagnostics/profiling.

Для задач с внешними repo/dependencies:

- `docs/DEPENDENCY_AND_LICENSE_POLICY.md`

Для GitHub/PR workflow:

- `docs/GITHUB_WORKFLOW.md`

После чтения релевантных файлов агент может предлагать план или патч.

## Главная цель

SOLUM Platform — Android/Vulkan AAA-like game-dev платформа на телефоне через Termux.

SOLUM Engine — главный Vulkan runtime.

Вокруг него будущая экосистема отдельных приложений-инструментов:

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

Финальная модель: launcher-managed multi-APK ecosystem.

Стартовая стратегия: не строить всё сразу. Узкий порядок:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

## Project memory areas

### Current status

- `docs/CURRENT_STAGE.md`

Текущий этап, что уже решено, что делать дальше, чего не трогать.

### Agent rules

- `docs/AGENT_RULES.md`

Как должен работать AI: язык, стиль, диагностика, патчи, GitHub, запреты.

### Architecture rules

- `docs/ARCHITECTURE_RULES.md`

Non-throwaway архитектура, финальный target, module boundaries, startup strategy.

### UX/workflow rules

- `docs/UX_AND_WORKFLOW_RULES.md`

Mobile-first editor UX, панели, кнопки, gizmo, bottom sheet, цвета, debug, workflow.

### Design tokens

- `docs/DESIGN_TOKENS.md`

Цвета, spacing, typography, panel states, button types, safe zones.

### Input and gestures

- `docs/INPUT_AND_GESTURE_ARCHITECTURE.md`

Single-owner input rule, InputRouter, viewport/gizmo/panel/nav zones, Android picker gesture isolation.

### Editor core components

- `docs/EDITOR_CORE_COMPONENTS.md`

SceneHierarchy, Inspector, AssetBrowser, ViewportSelection, Gizmo, Undo/Redo, SaveValidation, no fake tabs.

### Visual QA

- `docs/VISUAL_QA_SPEC.md`

Benchmark screenshots, before/after comparison, known visual bug list, visual regression checklist.

### Render resource lifetime

- `docs/RENDER_RESOURCE_LIFETIME_RULES.md`

Render thread owns GPU resources, command queue upload flow, no random resource mutation from UI thread.

### Studio app model

- `docs/STUDIO_APP_MODEL.md`

Standalone/project/export modes, mixed 3D/2D/2.5D/sprite/VFX/audio scene model, AI/OpenCV as assist layer.

### Research gate

- `docs/research/RESEARCH_GATE_RULES.md`
- `docs/research/PATCH_RESEARCH_TEMPLATE.md`

Обязательная сверка с проверенными repo/docs перед сложными системами.

### Device and stack

- `docs/DEVICE_AND_STACK_PROFILE.md`

Target device, stack, storage and Termux constraints.

### Editor tooling reference

- `docs/EDITOR_TOOLING_ROADMAP_REFERENCE.md`

Scene hierarchy, inspector, gizmo, asset browser, build panel, save/load, profiler as future credibility components.

### Gameplay mechanics reference

- `docs/GAMEPLAY_MECHANICS_LIBRARY_REFERENCE.md`

Future Action RPG mechanics knowledge base. Документировать сейчас, реализовывать позже.

### Patch roadmap

- `docs/PATCH_ROADMAP.md`

Порядок первых патчей и критерии завершения.

### Foundation readiness

- `docs/FOUNDATION_READINESS.md`

Patch 01 continuation: проверка repo/build/tools foundation без runtime/Vulkan изменений.

### Diagnostics

- `docs/DIAGNOSTICS_SPEC.md`

Структура latest diagnostics ZIP, HTML report, device/env/git/vulkan/performance state.

### Build environment

- `docs/BUILD_ENV_SPEC.md`

Termux/Android SDK/NDK/Gradle/aapt2/logs/module build rules.

### Performance budgets

- `docs/PERFORMANCE_BUDGETS.md`

FPS tiers, frame budget, diagnostics overhead limits, regression snapshots.

### Rendering target

- `docs/RENDERING_TARGET_SPEC.md`

Vulkan target, Android/Mali constraints, forbidden fallbacks, external repo reference rules.

### Asset formats

- `docs/ASSET_FORMAT_SPEC.md`

Folder-based asset layout, manifest v1, schema versioning, validator, sandbox import.

### Dependency/license policy

- `docs/DEPENDENCY_AND_LICENSE_POLICY.md`

Reference vs dependency, license checks, external code rules.

### GitHub workflow

- `docs/GITHUB_WORKFLOW.md`

Branch naming, PR body, direct update rules, patch complete checklist.

### Repository references

- `docs/research/REPOSITORY_REFERENCE_CATALOG.md`

Проверенные repo/docs для Vulkan, The Forge, Filament, SaschaWillems, ARM Mali, materials, shadows, water, terrain, animation, VFX, ECS, diagnostics, UI references.

### Decisions / ADR

- `docs/decisions/ADR_0001_final_target_vulkan.md`
- `docs/decisions/ADR_0002_non_throwaway_architecture.md`
- `docs/decisions/ADR_0003_launcher_managed_multi_apk_future.md`
- `docs/decisions/ADR_0004_android_keystore_strategy.md`
- `docs/decisions/ADR_0005_use_proven_repositories_as_references.md`
- `docs/decisions/ADR_0006_android_storage_and_inter_app_communication.md`

ADR = Architecture Decision Record. Это записи, почему принято решение и что запрещено.

### Negative UX cases

- `docs/ux_negative_cases/UX_0001_download_trash.md`
- `docs/ux_negative_cases/UX_0002_reports_and_screenshots_chaos.md`
- `docs/ux_negative_cases/UX_0003_too_much_text_and_terms.md`
- `docs/ux_negative_cases/UX_0004_micro_patches.md`
- `docs/ux_negative_cases/UX_0005_bad_mobile_editor_ui.md`

Это память боли пользователя. Если проблема может повториться — она должна быть записана.

### Errors

- `docs/errors/ERROR_KNOWLEDGE_BASE.md`

Повторяющиеся build/runtime/Vulkan/Termux ошибки и их решения.

### Patch history

- `docs/patch_history/PATCH_HISTORY.md`

История патчей, результат, диагностика, known issues, следующий шаг.

### Ideas

- `docs/ideas/IDEA_0001_solum_platform_ecosystem.md`
- `docs/ideas/IDEA_0002_mechanics_api.md`
- `docs/ideas/IDEA_0003_project_memory_palace.md`

Идеи, которые не нужно сразу кодить, но нельзя потерять.

## Главное правило памяти

Если идея, ошибка, UX-боль, архитектурное решение или результат патча могут повториться — они должны попасть в GitHub docs.

Чат не является долговременной памятью. GitHub docs — источник правды.
