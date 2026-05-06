# TELEGRAM_REPORTING — real Telegram send foundation

Этот документ фиксирует Telegram send foundation: локальный агент может отправить короткий SOLUM report в Telegram через Telegram Bot API.

## Цель

Добавить реальную отправку уже подготовленных отчётов:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

HTML-файл является dashboard: cards, timeline, progress bars и Debug / Metrics.

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
  --stage-patch "P01D — понятные отчёты + HTML" \
  --changed "Переделал HTML в dashboard;Добавил metrics JSON support;Добавил отправку HTML dashboard файлом в Telegram" \
  --checks "Python syntax — OK;Dry run — OK" \
  --not-touched "Android runtime;Vulkan;Gradle/build system" \
  --problems "FPS/visual данные not_available, потому что runtime/visual diagnostics не запускались;Точные токены недоступны, используется примерная оценка" \
  --files "_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt;_work/agent_reports/latest/SOLUM_AGENT_REPORT.html" \
  --context-load MEDIUM \
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
html_report=present path=_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
txt_report=present path=_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

Send:

```text
send=success
message_id=...
document=success path=_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
document=success path=_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
```

Telegram summary должен быть коротким. Если HTML есть, summary пишет:

```text
++ HTML dashboard attached
```

## Failure policy

Если Telegram API, сеть или секреты недоступны, скрипт завершает работу с кодом `1` и печатает только безопасную ошибку без token value.

Если HTML/TXT report-файлов нет, скрипт не падает только из-за этого: он отправляет summary с `!!` проблемой о недостающих файлах.
