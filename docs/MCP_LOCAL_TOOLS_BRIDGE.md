# MCP_LOCAL_TOOLS_BRIDGE — local tools bridge foundation

Этот документ фиксирует P01E: подготовку SOLUM к MCP/local tools bridge без запуска полноценного MCP server.

## Зачем MCP нужен SOLUM

SOLUM строится как mobile-first Vulkan platform, где агенту нужны проверяемые локальные действия:

- собрать короткий human report;
- приложить HTML dashboard;
- проверить foundation readiness;
- найти latest report/diagnostics/build paths;
- позже запросить screenshot, UI tree и runtime diagnostics с телефона.

MCP нужен как стабильный контракт между агентом и локальными инструментами. Агент должен вызывать понятные commands, а не угадывать shell-пути каждый раз.

В P01E создаётся только CLI bridge:

```text
tools/agent_tools/solum_tool_bridge.py
```

Это не MCP server. Это foundation, который позже можно завернуть в MCP tool schema.

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
```

Dry run показывает план и не делает network calls, не пишет отчёт и не запускает runner.

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

## Будущий MCP server

Следующий слой может превратить CLI commands в MCP tools:

```text
solum.generate_report
solum.send_telegram_report
solum.foundation_readiness
solum.latest_paths
solum.print_status
solum.collect_visual_diagnostics
```

MCP server должен:

- иметь explicit allowlist commands;
- не принимать arbitrary shell;
- redacting secrets by default;
- возвращать structured JSON;
- хранить evidence paths;
- отделять dry-run от real side effects.

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
