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
7. Для Vulkan/render задач:
   - `docs/RENDERING_TARGET_SPEC.md`
   - `docs/errors/ERROR_KNOWLEDGE_BASE.md`

После чтения этих файлов агент может предлагать план или патч.

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

### Patch roadmap

- `docs/PATCH_ROADMAP.md`

Порядок первых патчей и критерии завершения.

### Rendering target

- `docs/RENDERING_TARGET_SPEC.md`

Vulkan target, Android/Mali constraints, forbidden fallbacks, external repo reference rules.

### Asset formats

- `docs/ASSET_FORMAT_SPEC.md`

Folder-based asset layout, manifest v1, schema versioning, validator, sandbox import.

### Decisions / ADR

- `docs/decisions/ADR_0001_final_target_vulkan.md`
- `docs/decisions/ADR_0002_non_throwaway_architecture.md`
- `docs/decisions/ADR_0003_launcher_managed_multi_apk_future.md`
- `docs/decisions/ADR_0004_android_keystore_strategy.md`
- `docs/decisions/ADR_0005_use_proven_repositories_as_references.md`

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
