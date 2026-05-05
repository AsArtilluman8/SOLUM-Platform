# CURRENT_STAGE — SOLUM Platform

## Текущий статус

Репозиторий создан:

```text
AsArtilluman8/SOLUM-Platform
```

Проект находится на этапе:

```text
Stage 0 — Foundation documentation / project memory / architecture rules
```

Цель Stage 0 — не кодить движок, а зафиксировать фундамент, чтобы следующие чаты и агенты не уводили проект в сторону.

## Критерий завершения Stage 0

Stage 0 считается завершённым, когда в репозитории есть минимум эти файлы:

1. `docs/PROJECT_MEMORY_INDEX.md`
2. `docs/CURRENT_STAGE.md`
3. `docs/AGENT_RULES.md`
4. `docs/ARCHITECTURE_RULES.md`
5. `docs/UX_AND_WORKFLOW_RULES.md`
6. `docs/PATCH_ROADMAP.md`
7. `docs/RENDERING_TARGET_SPEC.md`
8. `docs/ASSET_FORMAT_SPEC.md`
9. `docs/errors/ERROR_KNOWLEDGE_BASE.md`

Дополнительные ADR/UX/ideas файлы желательны, но не должны превращать Stage 0 в бесконечную документацию.

## Текущий принятый старт

```text
Patch 01 — Repo Foundation
Patch 02 — Diagnostics v1 + Vulkan Capability Check
Patch 03 — Asset Schema v1 + Transaction Save for file writes
Patch 04 — Vulkan Foundation v1: swapchain + triangle
Patch 05 — Asset Hub v1
Patch 06 — Material Studio v1
Patch 07 — Launcher Foundation
```

## Почему старт узкий

Финальная цель — большая экосистема отдельных приложений. Но сразу строить 10–15 APK нельзя.

Риск:

```text
много APK → много сборок → много версий → много мест для ошибок → нет работающего ядра
```

Поэтому сначала строим корень:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → первый tool
```

## Что сейчас запрещено делать

Пока Stage 0/Patch 01 не закрыты, нельзя начинать:

- полноценный AniStudio;
- AI Behavior Studio;
- Quest/Dialogue Studio;
- Mechanics API implementation;
- полноценный Launcher с GitHub Releases;
- 15 отдельных APK;
- OpenGL preview как временную production-дорогу;
- blob shadows / fake renderer / low-poly target drift;
- node graph UI в первом tool;
- большие runtime/Vulkan патчи без diagnostics.

## Что делать дальше

Следующий реальный шаг после заполнения docs:

```text
Patch 01 — Repo Foundation
```

Он должен создать:

- базовую структуру папок;
- Gradle/Termux build skeleton позже, если будет решено кодить сразу;
- `tools/` skeleton;
- `SOLUMCreative` folder policy;
- project memory docs;
- patch naming policy.

## Источник правды

- GitHub repo — источник кода и долговременной памяти.
- Diagnostics ZIP — источник правды о запуске на телефоне.
- Build log — источник правды о сборке.
- Скриншоты — полезны, но не должны быть единственным способом диагностики.
