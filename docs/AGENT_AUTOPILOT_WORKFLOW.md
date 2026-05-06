# AGENT_AUTOPILOT_WORKFLOW — supervised-autopilot-pr

Этот документ фиксирует режим работы агента, когда пользователь явно разрешил:

```text
Режим: supervised-autopilot-pr
```

## Назначение

`supervised-autopilot-pr` нужен для задач на несколько часов или для логических патчей, где агент должен довести работу до reviewable PR, а не остановиться после мелкой правки.

## Когда режим активен

Режим активен только если пользователь явно написал `supervised-autopilot-pr` или отдельно разрешил:

- создать branch;
- делать scoped edits;
- запустить разрешённые проверки;
- сделать один commit;
- push branch;
- создать PR.

Без такого разрешения действуют обычные правила `supervised-autopilot`: можно готовить изменения и отчёт, но нельзя автоматически делать commit, push или PR.

## Обязательный цикл

1. Прочитать обязательные docs проекта.
2. Прочитать релевантные skills.
3. Вывести `PRE-PATCH CHECK`.
4. Проверить `git status --short`.
5. Создать отдельную branch.
6. Внести только scoped edits.
7. Запустить `bash tools/agent_build_runner.sh`, если он применим к repo state.
8. Если проверка упала, читать `_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt`.
9. Исправлять только нужные файлы, максимум 3 цикла.
10. Проверить `git diff --stat` и `git diff`.
11. Написать report в `_work/agent_reports`.
12. Сделать один commit.
13. Push branch.
14. Создать PR в `main`.
15. В финальном `RESULT` дать changed/checks/output/known issues/next step.

## Git rules

Разрешено только в активном `supervised-autopilot-pr`:

- auto branch;
- один logical commit;
- push текущей feature/patch branch;
- create PR в `main`.

Запрещено без отдельной явной команды пользователя:

- merge main;
- auto-merge PR;
- force push;
- `git reset --hard`;
- `git clean`;
- rebase;
- менять branch вне текущей задачи.

## Scope control

Агент обязан держать патч в заявленных границах.

Если задача docs-only:

- не менять runtime;
- не менять build system;
- не менять Gradle;
- не менять Vulkan/render code;
- не менять roadmap без прямой команды.

Если задача runtime/build/Vulkan:

- читать релевантные specs;
- использовать build log или diagnostics как evidence;
- не заменять Vulkan target на OpenGL/Canvas/fake renderer.

## Long task rule

Если задача рассчитана на 2-3 часа или пользователь явно просит autopilot PR:

- агент должен работать до reviewable result;
- писать report;
- создавать PR, если это разрешено;
- не останавливаться после одной мелкой правки, если scope требует законченного workflow.

Остановиться можно только при stop condition.

## Stop conditions

Агент останавливается и пишет report, если:

- нужны секреты, токены или доступ к private config;
- нужна установка пакетов;
- нужен destructive git command;
- требуется merge/rebase без разрешения;
- scope начал затрагивать запрещённые файлы;
- 3 fix cycles не дали проходящей проверки;
- нужна ручная проверка на устройстве, которую агент не может выполнить.

## PR body

PR должен содержать:

- Scope;
- Out of scope;
- Changed files;
- User-visible result;
- Build/test evidence;
- Known issues;
- Next step.
