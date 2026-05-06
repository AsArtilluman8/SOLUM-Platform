# TELEGRAM_REPORTING — real Telegram send foundation

Этот документ фиксирует P01C: локальный агент может отправить короткий SOLUM report в Telegram через Telegram Bot API.

## Цель

Добавить реальную отправку уже подготовленного отчёта:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

Генератор отчёта остаётся отдельным tool:

```text
tools/agent_telegram_report.py
```

Отправитель:

```text
tools/send_telegram_report.py
```

## Secret source

Скрипт читает только:

```text
~/.solum/secrets/telegram.env
```

Разрешённые ключи:

```text
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
```

Поддерживается также форма:

```text
export TELEGRAM_BOT_TOKEN=...
export TELEGRAM_CHAT_ID=...
```

## Safety rules

- `TELEGRAM_BOT_TOKEN` никогда не печатать.
- Не читать `~/.ssh`, `~/.config`, другие env/secrets/token files.
- Не коммитить `~/.solum/secrets/telegram.env`.
- Не писать secrets в `_work/agent_reports`.
- Не менять runtime, Vulkan, Gradle или build system.
- Не устанавливать пакеты.

## Usage

Сначала создать локальный отчёт:

```bash
python3 tools/agent_telegram_report.py \
  --stage-patch "P01C — real Telegram send foundation" \
  --changed "tools/send_telegram_report.py;docs/TELEGRAM_REPORTING.md;docs/AGENT_LOCAL_TOOLS.md;docs/patch_history/PATCH_HISTORY.md" \
  --checks "python3 -m py_compile tools/send_telegram_report.py;python3 tools/send_telegram_report.py --dry-run;python3 tools/send_telegram_report.py --send" \
  --output "_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt" \
  --known-issues "No Android/runtime/Vulkan checks in this patch" \
  --next-step "Review PR"
```

Проверить без отправки:

```bash
python3 tools/send_telegram_report.py --dry-run
```

Отправить:

```bash
python3 tools/send_telegram_report.py --send
```

## Expected output

Dry run:

```text
dry_run=ok
telegram_bot_token=present_redacted
telegram_chat_id=present
report_file=_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

Send:

```text
send=success
message_id=...
```

## Failure policy

Если Telegram API, сеть или секреты недоступны, скрипт завершает работу с кодом `1` и печатает только безопасную ошибку без token value.
