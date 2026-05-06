# AGENT_LOCAL_TOOLS — local agent tools foundation

Этот документ фиксирует маленький слой local agent tools. Он не меняет runtime, Vulkan, Gradle или roadmap.

## Назначение

Local agent tools нужны, чтобы агент мог быстро оставить короткий проверяемый отчёт после scoped work.

Первый tool:

```text
tools/agent_telegram_report.py
```

Он создаёт локальный human-friendly отчёт:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

Второй tool:

```text
tools/send_telegram_report.py
```

Он отправляет короткий русский summary в Telegram через Telegram Bot API и прикрепляет отчёты файлами.

P01E добавляет bridge foundation:

```text
tools/agent_tools/solum_tool_bridge.py
```

Это локальный CLI bridge, пока не полноценный MCP server. Он даёт стабильные команды для будущего MCP слоя:

```text
generate-report
send-telegram-report
foundation-readiness
latest-paths
print-status
```

Подробный контракт:

```text
docs/MCP_LOCAL_TOOLS_BRIDGE.md
docs/ACCESSIBILITY_COMPANION_PLAN.md
tools/agent_tools/README.md
```

Подробные правила:

```text
docs/TELEGRAM_REPORTING.md
docs/HUMAN_REPORTS_SPEC.md
docs/AGENT_DASHBOARD_REPORTS.md
```

## Разрешено

- писать локальный текстовый report и локальный HTML report;
- использовать `_work/agent_reports/latest/`;
- включать `Что сделал`, `Что проверил`, `Что не трогал`, `Проблемы`, `Следующий шаг`, `Файлы`;
- использовать маркеры `++`, `--`, `!!`, `->`;
- показывать только примерный Context Load / Token Load Estimate: `🟢 LOW`, `🟡 MEDIUM`, `🔴 HIGH`;
- запускать через `python3` как локальный project tool;
- для `tools/send_telegram_report.py` читать только `~/.solum/secrets/telegram.env`;
- отправлять summary через `sendMessage`, если пользователь явно разрешил real send;
- прикреплять `_work/agent_reports/latest/SOLUM_AGENT_REPORT.html` через `sendDocument`;
- прикреплять `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt` через `sendDocument`, если файл есть;
- не падать только из-за отсутствия HTML/TXT report-файлов, а писать это в summary.
- использовать optional metrics JSON `_work/agent_reports/latest/SOLUM_AGENT_METRICS.json`;
- при отсутствии runtime/visual metrics честно писать `not_available`.
- запускать `tools/agent_tools/solum_tool_bridge.py` как allowlisted CLI wrapper;
- использовать bridge `--dry-run` без network calls и без записи отчётов;
- запускать `tools/check_foundation_readiness.sh` через bridge;
- запускать `tools/agent_build_runner.sh` через bridge только если явно передан `--run-runner`.

## Запрещено

- выводить `TELEGRAM_BOT_TOKEN`;
- читать любые secrets кроме явно разрешённого `~/.solum/secrets/telegram.env` для Telegram send;
- создавать или хранить токены внутри repo;
- чтение `~/.ssh`, `~/.config`, паролей, ключей;
- network calls без явного разрешения пользователя;
- изменение `tools/agent_build_runner.sh` без отдельного scope;
- изменение Gradle/Vulkan/runtime/build system;
- запись в Download.
- arbitrary shell через bridge;
- запуск `tools/agent_build_runner.sh` через bridge без `--run-runner`;
- Telegram UI automation.

## Usage: local report

```bash
python3 tools/agent_telegram_report.py \
  --stage-patch "P01D — понятные отчёты + HTML" \
  --changed "Добавил русский понятный отчёт;Добавил HTML-отчёт" \
  --checks "Python syntax — OK;Dry run — OK" \
  --not-touched "Android runtime;Vulkan;Gradle/build system" \
  --problems "Точные токены недоступны, используется примерная оценка" \
  --files "_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt;_work/agent_reports/latest/SOLUM_AGENT_REPORT.html" \
  --metrics "_work/agent_reports/latest/SOLUM_AGENT_METRICS.json" \
  --context-load MEDIUM \
  --next-step "Review PR"
```

## Usage: bridge

```bash
python3 tools/agent_tools/solum_tool_bridge.py --help
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run
python3 tools/agent_tools/solum_tool_bridge.py latest-paths
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py generate-report
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --send
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --run-runner
```

`send-telegram-report --dry-run` на уровне bridge не читает token. Real send должен идти только после явного разрешения пользователя:

```bash
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --send
```

## Формат отчёта

```text
✅ SOLUM Agent Report

Патч: ...
Статус: ...

Что сделал:
++ ...

Что проверил:
++ ...

Что не трогал:
-- ...

Проблемы:
!! ...

Нагрузка:
🟡 MEDIUM

Следующий шаг:
-> ...
```

## Правило

`tools/agent_telegram_report.py` подготавливает TXT/HTML для человека или отправителя. Он не читает secrets и не делает network calls.

`tools/send_telegram_report.py` является отдельной real Telegram send integration. Его можно запускать только когда пользователь явно разрешил Telegram send и доступ к `~/.solum/secrets/telegram.env`.

Подробный формат human-friendly отчёта описан в `docs/HUMAN_REPORTS_SPEC.md`.

Dashboard HTML описан в `docs/AGENT_DASHBOARD_REPORTS.md`.

MCP/local bridge foundation описан в `docs/MCP_LOCAL_TOOLS_BRIDGE.md`.

Accessibility companion plan описан в `docs/ACCESSIBILITY_COMPANION_PLAN.md`.
