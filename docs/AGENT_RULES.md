# AGENT_RULES — правила для GPT / Claude / Codex

Этот файл обязателен для любого агента, который работает с SOLUM Platform.

## Язык и стиль ответа

- Отвечать на русском.
- Кратко, без воды.
- Не пугать масштабом.
- Не превращать ответ в учебник.
- Объяснения должны помогать созданию проекта, а не заменять разработку.
- Сложные термины объяснять простыми словами при первом упоминании.
- Английские термины использовать только когда нужно.

Пример:

Неправильно:

```text
Target system — система таргета.
```

Правильно:

```text
Target System — система выбора цели.
Она решает, на кого персонаж сейчас смотрит, атакует или фокусируется.
```

## Как объяснять алгоритмы

Алгоритмы писать как процесс, а не обрывками.

Пример:

```text
Игрок нажал кнопку атаки
↓
Combat system проверяет, можно ли атаковать сейчас
↓
если персонаж в cooldown — атака не запускается
↓
если можно — запускается анимация
↓
в нужный момент появляется hitbox
↓
если hitbox задел врага — враг получает урон
↓
VFX и звук показывают удар
```

## Перед работой с проектом

Агент обязан прочитать:

1. `docs/PROJECT_MEMORY_INDEX.md`
2. `docs/CURRENT_STAGE.md`
3. `docs/AGENT_RULES.md`
4. `docs/ARCHITECTURE_RULES.md`
5. `docs/UX_AND_WORKFLOW_RULES.md`
6. `docs/PATCH_ROADMAP.md`

Для Vulkan/render задач также:

7. `docs/RENDERING_TARGET_SPEC.md`
8. `docs/errors/ERROR_KNOWLEDGE_BASE.md`

Только после этого можно предлагать патч или план.

## GitHub и diagnostics

- Если GitHub repo подключён и актуален — читать код из GitHub.
- Full dump нужен только если GitHub недоступен, repo неполный или локальные файлы отличаются от repo.
- Runtime/FPS/Vulkan проблемы требуют latest diagnostics ZIP.
- Build проблемы требуют build log.
- Не гадать по runtime-проблемам без логов.

Формула:

```text
GitHub = источник кода.
Diagnostics ZIP = правда о запуске на телефоне.
Build log = правда о сборке.
```

## Патчи

Предпочтительно:

```text
один крупный проверяемый патч = один законченный вертикальный слой системы
```

Микропатчи допустимы только для:

- compile/build fix;
- one-line hotfix;
- emergency rollback;
- точечного исправления после diagnostics.

Формат ответа для патча:

```text
- Что делаем
- Зачем
- Что изменится для пользователя
- Один файл патча + одна команда apply/build
- Что проверить
- Если ошибка — какой latest diagnostics/log прислать
- Known issues
- Следующий шаг
```

## Запрещено

- Не утверждать “работает” без доказательства: build success, runtime log, diagnostics/report или явное подтверждение пользователя.
- Не использовать `/mnt/data` в командах для Termux.
- Не удалять файлы wildcard-командами без явного подтверждения пользователя.
- Не засорять общий Download.
- Не давать несколько раз patch-файл с тем же именем.
- Не строить fake/throwaway системы.
- Не заменять финальную цель костылями.
- Не делать OpenGL production preview для Vulkan-target Engine.
- Не делать blob shadows вместо shadow system.
- Не делать UI-кнопки вместо нормального инструмента/gizmo.
- Не создавать 15 APK на старте.
- Не начинать node graph в v1 tool без отдельной причины.

## Файлы и output

Все SOLUM output-файлы должны идти в controlled root:

Предпочтительно:

```text
/storage/emulated/0/SOLUMCreative/
```

Fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

Структура:

```text
SOLUMCreative/
  releases/latest/
  releases/archive/
  diagnostics/latest/
  diagnostics/archive/
  reports/latest/
  reports/archive/
  projects/
  assets/
  exports/
  temp/
```

Всегда должен быть latest-файл:

```text
SOLUM_LATEST_DIAGNOSTICS.zip
SOLUM_LATEST_REPORT.html
SOLUM_LATEST.apk
```

## Project memory

Если идея, ошибка, UX-боль, patch result или архитектурное решение могут повториться — сохранить в GitHub docs.

Не оставлять важную память только в чате.
