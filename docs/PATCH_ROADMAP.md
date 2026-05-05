# PATCH_ROADMAP — стартовый roadmap SOLUM Platform

Этот roadmap фиксирует порядок первых патчей. Он нужен, чтобы проект не ушёл в сторону и не начал строить 15 приложений до готового ядра.

## Стартовый принцип

Финальная цель большая:

```text
SOLUM Engine + launcher-managed multi-APK ecosystem
```

Но старт узкий:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

## Patch 01 — Repo Foundation

### Цель

Создать корень проекта и правила работы.

### Что входит

- README.
- `docs/` memory foundation.
- `PROJECT_MEMORY_INDEX.md`.
- `CURRENT_STAGE.md`.
- `AGENT_RULES.md`.
- `ARCHITECTURE_RULES.md`.
- `UX_AND_WORKFLOW_RULES.md`.
- `PATCH_ROADMAP.md`.
- `RENDERING_TARGET_SPEC.md`.
- `ASSET_FORMAT_SPEC.md`.
- `errors/ERROR_KNOWLEDGE_BASE.md`.
- `patch_history/PATCH_HISTORY.md`.
- `decisions/` ADR foundation.
- `ux_negative_cases/` memory.
- `ideas/` memory.
- folder structure skeleton:

```text
core/
engine-core/
apps/
tools/
```

### Что пользователь должен увидеть

В GitHub появляется понятная структура проекта и память проекта.

### Definition of Done

- Все обязательные docs созданы.
- README объясняет цель.
- Новый чат может начать с `PROJECT_MEMORY_INDEX.md`.

## Patch 02 — Diagnostics v1 + Vulkan Capability Check

### Цель

Создать “правду устройства” до сложного кода.

### Что входит

- `tools/collect_diagnostics.sh`.
- env report.
- build report placeholder.
- git state report.
- device info.
- storage paths report.
- latest/archive structure.
- HTML report v1.
- latest diagnostics ZIP.
- Vulkan capability check:
  - create VkInstance;
  - enumerate physical devices;
  - dump properties/features/limits/extensions;
  - write `vulkan_caps.json`.

### Что пользователь должен увидеть

- `SOLUM_LATEST_DIAGNOSTICS.zip`.
- `SOLUM_LATEST_REPORT.html`.
- `vulkan_caps.json` с GPU/device info.

### Почему Vulkan capability check здесь

Чтобы узнать возможности Mali/Android до полноценного Vulkan renderer.

## Patch 03 — Asset Schema v1 + Transaction Save

### Цель

Создать единый язык ассетов и безопасную запись файлов.

### Что входит

- folder-based asset layout.
- `asset_manifest.json` v1.
- `project_manifest.json` v1.
- schemaVersion.
- assetId.
- assetType.
- fileList.
- contentHashes.
- validationState.
- validator.
- import sandbox.
- transaction save:

```text
serialize temp → validate → backup → atomic replace → save_report.json
```

### Что пользователь должен увидеть

Asset validator умеет проверить тестовый asset folder и записать report.

## Patch 04 — Vulkan Foundation v1

### Цель

Доказать настоящий Vulkan production path на устройстве.

### Что входит

- Android app skeleton if not present.
- Vulkan instance/device/swapchain.
- clear screen.
- triangle.
- basic frame loop.
- frame timing approximation.
- Vulkan state in diagnostics.

### Что пользователь должен увидеть

APK показывает Vulkan triangle/clear screen, diagnostics подтверждает active Vulkan path.

### Запрещено

- OpenGL fallback.
- Fake renderer.
- Canvas/bitmap demo instead of Vulkan.

## Patch 05 — Asset Hub v1

### Цель

Первый реально полезный инструмент.

### Что входит

- Scan `SOLUMCreative/assets`.
- Asset cards.
- Preview image if exists.
- Fallback colored asset icon if no preview.
- asset type color.
- display name.
- schema version.
- validation status.
- details view.
- open diagnostics/report.

### Что пользователь должен увидеть

Asset Hub показывает assets, статусы, ошибки валидации и не требует ручного поиска файлов.

## Patch 06 — Material Studio v1

### Цель

Первый полноценный editor tool и реальный render/material vertical slice.

### Что входит

- Material asset type.
- Vulkan preview sphere/object.
- Basic material parameters:
  - base color;
  - roughness;
  - metallic;
  - normal slot placeholder if needed.
- live preview.
- save/validate material.
- diagnostics.
- no node graph in v1.

### Что пользователь должен увидеть

Материал создаётся, редактируется параметрами, preview обновляется live, asset сохраняется в schema v1.

## Patch 07 — Launcher Foundation

### Цель

Начать launcher-managed ecosystem без преждевременного overbuild.

### Что входит

- installed apps list placeholder/real check where possible.
- local update manifest.
- app cards.
- open app intent.
- open APK installer manually.
- version/compatibility display.
- diagnostics entry.

### Что пользователь должен увидеть

Launcher показывает приложения, версии, статусы и открывает install/update flow вручную.

## Что отложить

До готового core/render/asset foundation не делать:

- AniStudio full tool.
- Character Studio.
- Motion Studio.
- VFX Studio.
- Sound Studio.
- World Studio.
- AI Behavior Studio.
- Quest/Dialogue Studio.
- Mechanics API implementation.
- GitHub Releases updater.
- multi-APK expansion.

## Правила изменения roadmap

Roadmap можно менять, если:

1. diagnostics показала blocker;
2. external repo review дал лучшее решение;
3. device capability ограничивает план;
4. пользователь явно изменил приоритет.

Но нельзя менять roadmap так, чтобы уйти от:

- Vulkan target;
- non-throwaway architecture;
- diagnostics-first workflow;
- asset schema foundation;
- mobile-first UX.
