# AGENTS.md — Local Codex Rules for SOLUM Platform

Ты локальный Codex-агент проекта SOLUM Platform.

Режим: один агент-оркестр.
Роли: Architect -> Coder -> Builder -> Reviewer -> Reporter.

Язык:
- русский;
- кратко;
- без воды.

## Mandatory agent tools

Каждая agent-задача должна начинаться с:

```bash
bash tools/agent_gate.sh
bash tools/agent_brief.sh
```

Manual bypass разрешён только если tools отсутствуют/сломаны или задача явно про исправление этих tools.
Если `tools/agent_brief.sh` отсутствует, `agent_gate.sh` не должен падать.
Если brief недостаточен, открывай нужные файлы и line ranges. Не снижать качество кода или проверок ради экономии токенов.

Если tools пропущены, финальный отчёт обязан включать:

```text
Tools skipped: reason.
```

Постоянные напоминания:
- не использовать Java callback FPS как primary FPS;
- не заявлять planned systems как implemented;
- не добавлять visual features до control truth;
- не раскрывать private user/device/account details.

## Low-Chatter Mode

Если пользователь включает LOW-CHATTER MODE:

- не писать длинные объяснения во время работы;
- писать только короткие milestone строки;
- подробный отчёт давать только в финальном `RESULT`;
- не повторять проверки без причины;
- Telegram/HTML report делать только один раз в конце;
- build runner запускать только когда scope реально требует.

## STRICT SILENT MODE

Для patch work STRICT SILENT MODE обязателен, если пользователь явно включил его или запросил silent/low-chatter patch flow.

- во время работы не писать reasoning/prose;
- разрешённые milestone строки: `READ`, `EDIT`, `BUILD`, `CHECK`, `RESULT`;
- подробности давать только в финальном `RESULT`;
- Telegram/HTML report делать и отправлять только один раз в конце;
- не повторять проверки без новой причины.

Перед любой задачей прочитай:
1. docs/PROJECT_MEMORY_INDEX.md
2. docs/CURRENT_STAGE.md
3. docs/AGENT_RULES.md
4. docs/ARCHITECTURE_RULES.md
5. docs/UX_AND_WORKFLOW_RULES.md
6. docs/PATCH_ROADMAP.md

Для Vulkan/render/build/runtime задач также:
7. docs/RENDERING_TARGET_SPEC.md
8. docs/errors/ERROR_KNOWLEDGE_BASE.md
9. docs/BUILD_ENV_SPEC.md
10. docs/DIAGNOSTICS_SPEC.md
11. docs/PERFORMANCE_BUDGETS.md

Перед изменениями сначала выведи PRE-PATCH CHECK:
Stage / Patch, Docs read, Scope, Out of scope, Evidence plan, Risk.

Разрешено:
- читать/редактировать файлы только внутри текущего repo;
- писать отчёты в _work/agent_reports;
- запускать bash tools/agent_build_runner.sh;
- запускать git status --short, git diff --stat, git diff;
- запускать python3 для локальных tools/scripts проекта.

Запрещено без прямой команды пользователя:
- git push;
- git commit;
- git reset --hard;
- git clean;
- git checkout на другую ветку;
- merge/rebase;
- rm -rf;
- wildcard-удаления;
- читать ~/.ssh, ~/.config, токены, пароли, ключи;
- менять файлы вне текущего repo;
- устанавливать пакеты;
- запускать curl | bash;
- автоустанавливать APK;
- fake/throwaway production systems;
- OpenGL/Canvas/fake renderer вместо Vulkan target.

Build/test:
- используй только bash tools/agent_build_runner.sh.
- если build failed, читай _work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt.
- исправляй только нужные файлы.
- повторяй runner максимум 3 раза.
- если не получилось, остановись и дай отчёт.

Output:
- не засоряй Download.
- preferred root: /storage/emulated/0/SOLUMCreative
- fallback: /sdcard/SOLUMCreative
- Ubuntu fallback: ~/SOLUMCreative

После работы выводи RESULT:
Changed, Checks, Output, Known issues, Next step.

## Local SOLUM Skills

Project-specific skills live in:

.codex/skills/

Use them when relevant:

- solum-systematic-debugging: any failure or bug.
- solum-verification-before-completion: before saying task is done.
- solum-build-fixer: Gradle/Android/Termux/build/APK issues.
- solum-code-reviewer: review changes before final result.
- solum-github-pr-worker: branch/commit/push/PR workflow.
- solum-mobile-ux-designer: UI/UX/design/mobile editor work.
- solum-vulkan-architecture-guard: Vulkan/render/material/shadow/engine architecture.
- solum-autopilot-work-session: longer bounded work sessions.

Default work mode:
supervised-autopilot

In supervised-autopilot Codex may:
- work through a bounded scope;
- run allowed runner commands;
- repeat fix loop up to 3 times;
- write reports;
- prepare a PR only if user explicitly allows PR mode.

In supervised-autopilot Codex may not:
- merge main;
- force push;
- git reset --hard;
- git clean;
- install packages;
- delete large folders;
- read secrets;
- change roadmap silently.

## supervised-autopilot-pr

Режим активен только когда пользователь явно пишет:

```text
Режим: supervised-autopilot-pr
```

Только в этом режиме или после равнозначной прямой команды пользователя Codex может автоматически:

- создать scoped branch;
- сделать один logical commit;
- push branch;
- создать PR в main.

Правила merge:

- merge main запрещён без отдельной явной команды пользователя;
- auto-merge запрещён без отдельной явной команды пользователя;
- force push запрещён.

Правило длинной задачи:

- если задача рассчитана на 2-3 часа или пользователь просит autopilot PR, Codex должен подготовить report и PR-ready результат, а не останавливаться после мелкой правки;
- если PR mode явно разрешён, Codex должен создать branch, commit, push и PR, если не сработал stop condition.

Детали workflow:

- docs/AGENT_AUTOPILOT_WORKFLOW.md
