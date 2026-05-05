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

## Pre-Patch Check

Перед каждым патчем агент обязан показать короткий PRE-PATCH CHECK.

Минимальный формат:

```text
PRE-PATCH CHECK

Stage / Patch:
- PXX — название

Docs read:
- PROJECT_MEMORY_INDEX.md
- CURRENT_STAGE.md
- AGENT_RULES.md
- ARCHITECTURE_RULES.md
- PATCH_ROADMAP.md
- relevant specs for this task

Scope:
- что входит

Out of scope:
- что НЕ входит

Evidence plan:
- build log / diagnostics ZIP / report / screenshot / PR diff

Risk:
- что может сломаться
```

Если патч касается сложной системы, PRE-PATCH CHECK обязан включать Research Gate.

Сложные системы:

- Vulkan;
- render;
- shadows / CSM;
- lighting;
- materials;
- water;
- terrain / vegetation;
- VFX;
- animation;
- assets/import/export;
- diagnostics/profiling;
- ECS/mechanics;
- editor/input/UX.

Для таких задач агент обязан прочитать:

- `docs/research/REPOSITORY_REFERENCE_CATALOG.md`
- `docs/research/RESEARCH_GATE_RULES.md`
- `docs/research/PATCH_RESEARCH_TEMPLATE.md`

Patch proposal без PRE-PATCH CHECK считается incomplete.

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

## Agent Mode GitHub rule

В Agent Mode для GitHub задач агент не должен использовать visual browser как основной способ редактирования repo.

Правильный приоритет:

```text
GitHub connector/tool
↓
branch
↓
batch commit
↓
single PR
↓
user review
```

Visual browser допустим только если GitHub tool/connector недоступен или пользователь явно просит работать через браузер.

Если Agent Mode открыл пустой Chromium и не может нормально работать с repo, агент должен остановиться и честно сообщить:

```text
Agent browser route не сработал.
Нужен GitHub tool или Termux script workflow.
```

Запрещено:

- пытаться кликать GitHub UI в браузере, если доступен GitHub connector/tool;
- создавать много отдельных write-actions для одной логической задачи;
- отправлять пользователю скриншоты пустого браузера как будто работа идёт нормально;
- завершать задачу ручным планом, если обещал выполнить GitHub update.

## GitHub write workflow

Для больших изменений запрещено создавать/обновлять много файлов по одному через отдельные `create_file` / `update_file` операции.

Правильный workflow для крупных docs/code патчей:

```text
create branch
↓
prepare all changes as one batch
↓
create one commit
↓
open one PR
↓
user reviews diff
↓
merge after approval
```

Правила:

- Крупный patch/docs/code change → branch + single batch commit + PR.
- Маленькая точечная правка → допустим прямой GitHub file update.
- Termux patch-файл → использовать только если GitHub недоступен, нужна локальная проверка или пользователь явно просит.
- Не создавать 10+ отдельных commits для одной логической задачи.
- PR body обязан содержать scope, changed files, checks, known issues и next step.

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
