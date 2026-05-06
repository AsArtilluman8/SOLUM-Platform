# MCP_LOCAL_TOOLS_BRIDGE — local tools bridge foundation

Этот документ фиксирует P01E/P01F: CLI bridge и локальный MCP-style wrapper layer.

## Зачем MCP нужен SOLUM

SOLUM строится как mobile-first Vulkan platform, где агенту нужны проверяемые локальные действия:

- собрать короткий human report;
- приложить HTML dashboard;
- проверить foundation readiness;
- найти latest report/diagnostics/build paths;
- позже запросить screenshot, UI tree и runtime diagnostics с телефона.

MCP нужен как стабильный контракт между агентом и локальными инструментами. Агент должен вызывать понятные commands, а не угадывать shell-пути каждый раз.

В P01E создан CLI bridge:

```text
tools/agent_tools/solum_tool_bridge.py
```

В P01F добавлен MCP-style wrapper:

```text
tools/mcp_server/solum_mcp_server.py
```

Он имеет explicit tool schema и вызывает только bridge с `--json`.

## Какие локальные tools будут обёрнуты

Bridge использует уже существующие project tools:

```text
tools/agent_telegram_report.py
tools/send_telegram_report.py
tools/check_foundation_readiness.sh
tools/agent_build_runner.sh
```

`tools/agent_build_runner.sh` разрешён только если пользователь или вызывающий tool явно передал:

```text
--run-runner
```

Без этого bridge не запускает runner.

## Команды bridge

```text
generate-report
send-telegram-report
foundation-readiness
latest-paths
print-status
```

Все команды поддерживают:

```text
--dry-run
--json
```

Dry run показывает план и не делает network calls, не пишет отчёт и не запускает runner.

## Structured JSON contract

Для будущего MCP wrapper bridge поддерживает structured JSON output:

```bash
python3 tools/agent_tools/solum_tool_bridge.py print-status --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py latest-paths --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py foundation-readiness --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py generate-report --dry-run --json
python3 tools/agent_tools/solum_tool_bridge.py send-telegram-report --dry-run --json
```

Базовая форма:

```json
{
  "ok": true,
  "command": "print-status",
  "dry_run": true,
  "repo_root": "/root/SOLUM-Platform",
  "branch": "patch/P01E-mcp-local-tools-bridge",
  "head": "2ed7317",
  "paths": [],
  "planned_actions": [],
  "errors": []
}
```

Правила JSON output:

- `ok` всегда показывает итог команды;
- `command` совпадает с bridge subcommand;
- `dry_run` показывает, были ли side effects отключены;
- `repo_root` всегда присутствует;
- `branch` / `head` присутствуют там, где они применимы;
- `paths` / `tools` содержат structured status для файлов;
- `planned_actions` заполняется для dry-run;
- `errors` всегда массив;
- secrets не выводятся;
- Telegram token не читается в `send-telegram-report --dry-run --json`.

Пример path status:

```json
{
  "path": "_work/agent_reports/latest/SOLUM_AGENT_REPORT.html",
  "exists": true,
  "kind": "file",
  "status": "file"
}
```

## Разрешённые действия

Bridge может:

- писать `_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt`;
- писать `_work/agent_reports/latest/SOLUM_AGENT_REPORT.html`;
- запускать `tools/agent_telegram_report.py`;
- запускать `tools/send_telegram_report.py --send`, если пользователь явно разрешил real Telegram send;
- читать только `~/.solum/secrets/telegram.env` через `tools/send_telegram_report.py`;
- запускать `tools/check_foundation_readiness.sh`;
- печатать latest paths для reports/diagnostics/build logs;
- запускать `tools/agent_build_runner.sh` только с явным `--run-runner`.

## Запрещённые действия

Bridge не должен:

- печатать `TELEGRAM_BOT_TOKEN`;
- читать `~/.ssh`, `~/.config`, token files, пароли или ключи;
- читать любые secrets кроме `~/.solum/secrets/telegram.env` для Telegram send;
- коммитить secrets;
- менять runtime/Vulkan/Gradle/build system;
- устанавливать пакеты;
- запускать `curl | bash`;
- писать в Download;
- автоматизировать Telegram UI;
- запускать `tools/agent_build_runner.sh` без `--run-runner`;
- делать `git push`, `git commit`, merge, rebase, force push или destructive git commands.

## Почему logcat не основной путь

`logcat` полезен для аварий и Android runtime bugs, но он не должен быть главным интерфейсом агента:

- поток шумный и зависит от устройства;
- ошибки легко потерять среди системных logs;
- нет стабильного schema/contract;
- сложно прикреплять к Telegram как compact summary;
- visual/runtime evidence лучше хранить в diagnostics/report files.

Основной путь SOLUM:

```text
structured diagnostics → latest reports → HTML dashboard → optional Telegram send
```

`logcat` остаётся fallback для runtime-debug задач, а не foundation API.

## MCP server wrapper

P01F превращает CLI commands в MCP-style tools:

```text
solum_print_status
solum_latest_paths
solum_generate_report
solum_send_telegram_report
solum_foundation_readiness
```

Wrapper:

- имеет explicit allowlist commands;
- не принимает arbitrary shell;
- redacting secrets by default;
- возвращает structured JSON:

```text
ok
tool
dry_run
result
errors
```

- хранит evidence paths через bridge output;
- отделяет dry-run от real side effects.

Проверки:

```bash
python3 tools/mcp_server/solum_mcp_server.py --help
python3 tools/mcp_server/solum_mcp_server.py list-tools
python3 tools/mcp_server/solum_mcp_server.py call solum_print_status --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_latest_paths --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_generate_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_send_telegram_report --dry-run
python3 tools/mcp_server/solum_mcp_server.py call solum_foundation_readiness --dry-run
```

Полный setup:

```text
docs/MCP_SERVER_SETUP.md
```

## Как позже подключить Accessibility companion

Accessibility companion должен быть отдельным Android companion app, который работает только с SOLUM package allowlist.

Он может дать:

- screenshot route;
- UI tree route;
- launch/force-stop route для SOLUM apps;
- visual diagnostics pack export;
- стабильный путь для агента без Telegram UI automation.

Подробный план зафиксирован в:

```text
docs/ACCESSIBILITY_COMPANION_PLAN.md
```
