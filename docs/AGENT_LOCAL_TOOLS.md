# AGENT_LOCAL_TOOLS — local agent tools foundation

Этот документ фиксирует маленький слой локальных agent tools. Он не меняет runtime, Vulkan, Gradle или roadmap.

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

## Разрешено

- писать только локальный текстовый report;
- использовать `_work/agent_reports/latest/`;
- включать `Stage / Patch`, `Changed`, `Checks`, `Output`, `Known issues`, `Next step`;
- запускать через `python3` как локальный project tool.

## Запрещено

- Telegram Bot API;
- отправка сообщений;
- чтение, создание или хранение токенов;
- чтение `~/.ssh`, `~/.config`, паролей, ключей;
- network calls;
- изменение `tools/agent_build_runner.sh` без отдельного scope;
- изменение Gradle/Vulkan/runtime/build system;
- запись в Download.

## Usage

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

Этот tool подготавливает текст для человека. Он не является интеграцией с Telegram.
