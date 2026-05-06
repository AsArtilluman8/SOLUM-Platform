# MCP_SERVER_SETUP — SOLUM local MCP wrapper

Этот документ фиксирует P01F: настоящий локальный MCP-style wrapper foundation без внешних Python-пакетов.

## Что это

`tools/mcp_server/solum_mcp_server.py` — локальный wrapper над SOLUM agent tools.

Он даёт:

- explicit tool schema;
- JSON output;
- минимальный stdio JSON-RPC loop для будущего подключения как MCP server;
- строгий allowlist;
- безопасный dry-run default.

Wrapper не является runtime/Vulkan частью проекта и не меняет Android build system.

## Главный safety принцип

MCP wrapper не принимает arbitrary shell command.

Единственный backend-вызов:

```bash
python3 tools/agent_tools/solum_tool_bridge.py <command> --json
```

Разрешённые bridge commands:

```text
print-status
latest-paths
generate-report
send-telegram-report
foundation-readiness
```

## Как запускать

Справка:

```bash
python3 tools/mcp_server/solum_mcp_server.py --help
```

Список tools и schema:

```bash
python3 tools/mcp_server/solum_mcp_server.py list-tools
```

Вызов tool:

```bash
python3 tools/mcp_server/solum_mcp_server.py call solum_print_status --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_latest_paths --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_generate_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_send_telegram_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_foundation_readiness --dry-run
```

Минимальный stdio mode:

```bash
python3 tools/mcp_server/solum_mcp_server.py serve-stdio
```

Поддержанные JSON-RPC methods:

```text
initialize
tools/list
tools/call
```

## Tools

### solum_print_status

Показывает repo/tool status через bridge.

Возвращает:

```text
ok
tool
dry_run
result
errors
```

### solum_latest_paths

Возвращает latest paths для reports, diagnostics, build logs.

### solum_generate_report

Создаёт или dry-run планирует TXT/HTML human report.

Допустимые args:

```text
dry_run
stage_patch
status
changed
checks
not_touched
problems
files
context_load
next_step
```

### solum_send_telegram_report

Dry-run по умолчанию.

Real send разрешён только если одновременно:

```text
send=true
dry_run=false
```

CLI пример:

```bash
python3 tools/mcp_server/solum_mcp_server.py call solum_send_telegram_report --send --no-dry-run
```

Token не печатается. Secret читает только существующий sender:

```text
tools/send_telegram_report.py
```

Разрешённый secret path:

```text
~/.solum/secrets/telegram.env
```

### solum_foundation_readiness

Запускает или dry-run планирует foundation readiness.

`tools/agent_build_runner.sh` можно вызвать только через явный аргумент:

```text
run_runner=true
```

## Как позже подключить к Codex или другому агенту

MCP config позже должен указывать команду:

```json
{
  "mcpServers": {
    "solum": {
      "command": "python3",
      "args": [
        "/root/SOLUM-Platform/tools/mcp_server/solum_mcp_server.py",
        "serve-stdio"
      ]
    }
  }
}
```

Для другого checkout нужно заменить абсолютный repo path.

## Termux/proot ограничения

- Запуск должен идти из repo root или с абсолютным path к server script.
- Python используется системный/local `python3`.
- Внешние Python packages не нужны.
- Network нужен только для real Telegram send.
- Android storage paths могут отсутствовать в proot; это должно возвращаться как `missing`, а не как ошибка server.
- Установка пакетов не входит в scope.

## Safety rules

Wrapper запрещает:

- arbitrary shell;
- чтение `~/.ssh`, `~/.config`, токенов, паролей, ключей;
- печать `TELEGRAM_BOT_TOKEN`;
- real Telegram send без `send=true` и `dry_run=false`;
- запись в Download;
- runtime/Vulkan/Gradle/build-system изменения;
- package install;
- `curl | bash`;
- merge/rebase/force push/reset/clean.

Wrapper разрешает:

- читать repo files через bridge-команды;
- писать `_work/agent_reports/latest` только через report tool;
- отправлять Telegram report только через `tools/send_telegram_report.py --send`;
- запускать foundation readiness через bridge;
- запускать build runner только при явном `run_runner=true`.
