# AGENT_LOCAL_TOOLS — local agent tools foundation

Этот документ фиксирует маленький слой local agent tools. Он не меняет runtime, Vulkan, Gradle или roadmap.

## Назначение

Local agent tools нужны, чтобы агент мог быстро оставить короткий проверяемый отчёт после scoped work.

Первый tool:

```text
tools/agent_telegram_report.py
```

Он создаёт локальный Telegram-ready текстовый отчёт:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

Второй tool:

```text
tools/send_telegram_report.py
```

Он отправляет уже созданный report в Telegram через Telegram Bot API.

Подробные правила:

```text
docs/TELEGRAM_REPORTING.md
```

## Разрешено

- писать только локальный текстовый report;
- использовать `_work/agent_reports/latest/`;
- включать `Stage / Patch`, `Changed`, `Checks`, `Output`, `Known issues`, `Next step`;
- запускать через `python3` как локальный project tool;
- для `tools/send_telegram_report.py` читать только `~/.solum/secrets/telegram.env`;
- отправлять содержимое `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt` в Telegram, если пользователь явно разрешил real send.

## Запрещено

- выводить `TELEGRAM_BOT_TOKEN`;
- читать любые secrets кроме явно разрешённого `~/.solum/secrets/telegram.env` для Telegram send;
- создавать или хранить токены внутри repo;
- чтение `~/.ssh`, `~/.config`, паролей, ключей;
- network calls без явного разрешения пользователя;
- изменение `tools/agent_build_runner.sh` без отдельного scope;
- изменение Gradle/Vulkan/runtime/build system;
- запись в Download.

## Usage: local report

```bash
python3 tools/agent_telegram_report.py \
  --stage-patch "P01B — Telegram report + local agent tools foundation" \
  --changed "tools/agent_telegram_report.py;docs/AGENT_LOCAL_TOOLS.md" \
  --checks "python3 tools/agent_telegram_report.py --help" \
  --output "_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt" \
  --known-issues "No Telegram send by design" \
  --next-step "Review PR"
```

## Формат отчёта

```text
SOLUM AGENT REPORT
Timestamp: ...
Stage / Patch: ...

Changed:
- ...

Checks:
- ...

Output:
- ...

Known issues:
- ...

Next step:
- ...
```

## Правило

`tools/agent_telegram_report.py` подготавливает текст для человека или отправителя. Он не читает secrets и не делает network calls.

`tools/send_telegram_report.py` является отдельной real Telegram send integration. Его можно запускать только когда пользователь явно разрешил Telegram send и доступ к `~/.solum/secrets/telegram.env`.
