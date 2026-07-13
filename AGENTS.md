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

## Cortex bootstrap

Для любой нетривиальной задачи по SOLUM, UDS, renderer, assets, VFX, editor, diagnostics или architecture используй Cortex как проектный memory/router, если он доступен.

1. Найди Cortex через `CORTEX_ROOT`, соседний repo `../cortex-memory-ultimate` или известный Android/Termux workspace path.
2. Запусти один route-запрос:

```bash
python "$CORTEX_ROOT/scripts/cortex.py" route "<user request>" --json
```

3. Прочитай возвращённый project `state.md`, затем максимум два дополнительных memory/skill файла.
4. Для SOLUM загружай `PROJECTS/solum/state.md`; для UDS также `PROJECTS/uds/state.md`.
5. Не читай весь Cortex vault и не копируй полный чат в память.
6. Текущий repo, текущие artifacts и свежие tests важнее Cortex summary.
7. Если Cortex недоступен или сломан, не имитируй его использование. Укажи это в PRE-PATCH CHECK/RESULT и продолжай по локальным docs.

Разрешён read-only доступ к явно найденному Cortex repo для routing/memory/skills. Редактирование вне текущего repo запрещено без прямой cross-repo команды пользователя.

После проверенного этапа предлагай/update Cortex state только если результат подтверждён source evidence, tests и, когда разрешено, logical commit. Не сохраняй неподтверждённые гипотезы как durable truth.

## Mandatory Codex model decision

Перед тем как принять или сформировать нетривиальную Codex-задачу, прочитай:

```text
docs/CODEX_MODEL_SELECTION.md
```

Каждая задача обязана начинаться с:

```text
MODEL DECISION
- Recommended model:
- Reasoning effort:
- Why this configuration:
- Cheaper acceptable fallback:
- When to switch/restart with another model:
- Subagents/Ultra:
- Android/usage risk:
```

Правила выбора:

- Sol high/xhigh: architecture, unknown UE/binary layouts, renderer truth, difficult root cause, multi-system UDS/VFX/material work, final high-risk audit.
- Terra medium/high: bounded implementation, refactor, build repair, tests, UI/editor code по зрелому контракту.
- Luna low/medium: deterministic extraction, classification, fixtures, manifests, reports, repetitive conversions.
- Не назначай максимальную модель автоматически.
- Делай mixed work отдельными verified phases и меняй модель между фазами вручную.
- Не предполагай, что текущий Codex может сам выполнить `/model` внутри своей интерактивной сессии.
- Nested Codex, Ultra и subagents по умолчанию выключены на Android для memory-heavy parsing/builds и общих файлов.

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
7. docs/CODEX_MODEL_SELECTION.md

Для Vulkan/render/build/runtime задач также:
8. docs/RENDERING_TARGET_SPEC.md
9. docs/errors/ERROR_KNOWLEDGE_BASE.md
10. docs/BUILD_ENV_SPEC.md
11. docs/DIAGNOSTICS_SPEC.md
12. docs/PERFORMANCE_BUDGETS.md

Для SOLUM/UDS renderer, atmosphere, performance, asset format, cluster renderer, VFX или engine-roadmap задач также:
13. docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md

Перед изменениями сначала выведи PRE-PATCH CHECK:
Stage / Patch, Docs read, Cortex route/state, Model decision, Scope, Out of scope, Evidence plan, Risk.

Разрешено:
- читать/редактировать файлы внутри текущего repo;
- читать явно найденный Cortex repo только для routing/memory/skills;
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
- OpenGL/Canvas/fake renderer вместо Vulkan/Filament target.

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
- solum-vulkan-architecture-guard: Vulkan/Filament/render/material/shadow/engine architecture.
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
