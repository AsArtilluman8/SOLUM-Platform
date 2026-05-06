# SOLUM agent tools

`tools/agent_tools/` содержит local agent-facing wrappers.

Первый wrapper:

```text
tools/agent_tools/solum_tool_bridge.py
```

Это CLI bridge, не полноценный MCP server.

## Commands

```bash
python3 tools/agent_tools/solum_tool_bridge.py --help
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run
python3 tools/agent_tools/solum_tool_bridge.py latest-paths
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --dry-run
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
