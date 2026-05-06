# MCP_SERVER_SETUP — SOLUM local MCP wrapper

Этот документ фиксирует P01F: настоящий локальный MCP-style wrapper foundation без внешних Python-пакетов.

## Что это

`tools/mcp_server/solum_mcp_server.py` — локальный wrapper над SOLUM agent tools.

Он даёт:

- explicit tool schema;
- JSON output;
- MCP-compatible JSON-RPC 2.0 stdio foundation;
- строгий allowlist;
- безопасный dry-run default.

Это не packaged MCP SDK server. Внешний MCP SDK не используется, но stdio JSON-RPC contract совместим с базовым MCP handshake/tools flow.

Wrapper не является runtime/Vulkan частью проекта и не меняет Android build system.

## P01G companion MCP placeholders

P01G документирует future Accessibility companion route только как skeleton/stub. Эти MCP tools ещё не выполняют real device action и не добавлены в runtime bridge:

```text
solum_companion_status
solum_companion_screenshot
solum_companion_ui_tree
solum_companion_visual_pack
```

Planned behavior for P01H:

- `solum_companion_status` — проверить доступность companion service и активный allowlisted SOLUM package.
- `solum_companion_screenshot` — запросить screenshot только для SOLUM allowlist.
- `solum_companion_ui_tree` — запросить UI tree только для SOLUM allowlist.
- `solum_companion_visual_pack` — собрать visual diagnostics pack paths.

P01G не делает real screenshot, UI tree dump, launch, force-stop или tap automation.

Planned output paths:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

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

Пример MCP config:

```bash
python3 tools/mcp_server/solum_mcp_server.py print-config
```

Smoke-test JSON-RPC handler:

```bash
python3 tools/mcp_server/solum_mcp_server.py smoke-test
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

## JSON-RPC stdio contract

Все stdio responses сохраняют `id` из request.

Успешный ответ:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {}
}
```

Ошибка JSON-RPC method/protocol:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32601,
    "message": "method_not_found",
    "data": {
      "method": "missing/method"
    }
  }
}
```

### initialize

Request:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
```

Response result:

```json
{
  "protocolVersion": "2024-11-05",
  "serverInfo": {
    "name": "solum-local-mcp-wrapper",
    "version": "0.1.0"
  },
  "capabilities": {
    "tools": {}
  }
}
```

### tools/list

Request:

```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

Response result:

```json
{
  "tools": [
    {
      "name": "solum_print_status",
      "description": "Print repo/tool status through the SOLUM local bridge.",
      "inputSchema": {
        "type": "object",
        "properties": {
          "dry_run": {
            "type": "boolean",
            "default": true
          }
        },
        "additionalProperties": false
      }
    }
  ]
}
```

### tools/call

Request:

```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"solum_print_status","arguments":{"dry_run":true}}}
```

Response result:

```json
{
  "content": [
    {
      "type": "text",
      "text": "{\"ok\": true, \"tool\": \"solum_print_status\"}"
    }
  ],
  "isError": false
}
```

Tool-level error возвращается как MCP-style result, а не как JSON-RPC transport error:

```json
{
  "content": [
    {
      "type": "text",
      "text": "{\"ok\": false, \"error\": \"unknown_tool\"}"
    }
  ],
  "isError": true
}
```

Проверка через stdin:

```bash
printf '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}\n{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}\n{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"solum_print_status","arguments":{"dry_run":true}}}\n' | python3 tools/mcp_server/solum_mcp_server.py serve-stdio
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

Этот JSON можно вывести локально:

```bash
python3 tools/mcp_server/solum_mcp_server.py print-config
```

Для другого checkout нужно заменить абсолютный repo path.

## Termux/proot ограничения

- Запуск должен идти из repo root или с абсолютным path к server script.
- Python используется системный/local `python3`.
- Внешние Python packages не нужны.
- Нет external SDK packaging: это совместимый stdio JSON-RPC foundation.
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
