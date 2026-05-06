# SOLUM agent tools

`tools/agent_tools/` содержит local agent-facing wrappers.

Первый wrapper:

```text
tools/agent_tools/solum_tool_bridge.py
```

Это CLI bridge. P01F MCP-style server layer находится отдельно:

```text
tools/mcp_server/solum_mcp_server.py
```

Он вызывает только этот bridge с `--json`.

## Commands

```bash
python3 tools/agent_tools/solum_tool_bridge.py --help
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run
python3 tools/agent_tools/solum_tool_bridge.py latest-paths
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --dry-run
```

Structured JSON для будущего MCP wrapper:

```bash
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py latest-paths --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run --json
```

JSON содержит:

```text
ok
command
dry_run
repo_root
branch/head where applicable
paths/statuses where applicable
planned_actions for dry-run
errors
```

`send-telegram-report --dry-run --json` не читает Telegram token и выводит только `token=not_read`.

## MCP-style wrapper

Команды:

```bash
python3 tools/mcp_server/solum_mcp_server.py --help
python3 tools/mcp_server/solum_mcp_server.py list-tools
python3 tools/mcp_server/solum_mcp_server.py call solum_print_status --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_latest_paths --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_generate_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_send_telegram_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_foundation_readiness --dry-run
```

Wrapper tools:

```text
solum_print_status
solum_latest_paths
solum_generate_report
solum_send_telegram_report
solum_foundation_readiness
```

Wrapper JSON contract:

```text
ok
tool
dry_run
result
errors
```

Real Telegram send через wrapper требует:

```bash
python3 tools/mcp_server/solum_mcp_server.py call solum_send_telegram_report --send --no-dry-run
```

## Real side effects

`generate-report` writes:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

`send-telegram-report --send` sends through:

```text
tools/send_telegram_report.py --send
```

It may read only:

```text
~/.solum/secrets/telegram.env
```

`foundation-readiness` runs:

```text
tools/check_foundation_readiness.sh
```

It runs `tools/agent_build_runner.sh` only when explicitly requested:

```bash
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --run-runner
```

## Safety

- no arbitrary shell;
- no token printing;
- no package installs;
- no runtime/Vulkan/Gradle/build-system edits;
- no Telegram UI automation;
- no runner unless `--run-runner` is passed.
- JSON output must not include secrets.
