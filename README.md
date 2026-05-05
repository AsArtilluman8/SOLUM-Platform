# SOLUM Platform

**SOLUM Platform** — Android/Vulkan game-dev платформа и будущая экосистема мобильных инструментов для создания игры, ассетов, кат-сцен, материалов, VFX, анимаций, мира, механик и диагностики прямо на телефоне через Termux.

## Главная идея

SOLUM — не набор демо и не учебный проект. Это фундамент будущего мобильного движка и набора профессиональных инструментов.

- **SOLUM Engine** — главный Vulkan runtime и движок.
- **SOLUM Launcher** — будущий центр установки/обновления отдельных APK без автообновления.
- **Asset Hub** — общая библиотека ассетов.
- **Material Studio** — материалы, preview, PBR/toon параметры, позже node graph.
- **AniStudio / Cutscene Studio** — кат-сцены, видео, камера, персонажи, timeline.
- **Character Studio** — персонажи, лицо, тело, одежда, экспорт.
- **Motion Studio** — позы, клипы, анимации, motion tools.
- **VFX Studio** — эффекты, aura, trails, particles, sprite/Vulkan preview.
- **World Studio** — terrain, биомы, дороги, деревья, procedural rules.
- **Sound Studio** — звук, музыка, voice, timeline audio.
- **AI / Behavior Studio** — поведение NPC, behavior tree/state machine.
- **Quest / Dialogue Studio** — диалоги, задания, branching story.
- **Mechanics Studio** — reusable ARPG mechanics: dash, attacks, loot, skills.

Финальная модель: **launcher-managed multi-APK ecosystem**.

Стартовая стратегия: не строить сразу 15 APK. Идём узко:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

## Главные правила

1. **MVP может быть неполным, но не неправильным.**
2. Не делать throwaway/fake системы, которые потом нужно удалить.
3. Не заменять финальную цель костылями: blob shadows, OpenGL вместо Vulkan, low-poly вместо целевого качества, кнопки вместо нормального gizmo.
4. Большой патч должен закрывать цельный вертикальный слой системы.
5. Runtime/FPS/Vulkan/build проблемы требуют diagnostics ZIP или build log.
6. GitHub docs — долговременная память проекта.
7. Все output-файлы проекта должны идти в `SOLUMCreative/latest` и `archive`, а не засорять Download.
8. UI mobile-first: smart auto → visual control → compact precision → advanced override.

## С чего начинать новый чат/агент

Сначала читать:

1. `docs/PROJECT_MEMORY_INDEX.md`
2. `docs/CURRENT_STAGE.md`
3. `docs/AGENT_RULES.md`
4. `docs/ARCHITECTURE_RULES.md`
5. `docs/UX_AND_WORKFLOW_RULES.md`
6. `docs/PATCH_ROADMAP.md`
7. Для Vulkan/render задач: `docs/RENDERING_TARGET_SPEC.md` и `docs/errors/ERROR_KNOWLEDGE_BASE.md`

Только после этого предлагать патч или план.
