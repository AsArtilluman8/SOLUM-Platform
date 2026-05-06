# CODEX_LAUNCH_MODES — безопасный запуск Codex для SOLUM

Этот документ фиксирует режимы запуска локального Codex-агента для SOLUM Platform.

## Обычный безопасный режим

Использовать для чтения, планирования, docs-only задач и небольших проверок.

Правила:

- работать только внутри repo;
- перед патчем читать project memory docs;
- перед изменениями показывать `PRE-PATCH CHECK`;
- не читать secrets без явного разрешения;
- не делать destructive git operations.

## Рабочий Termux/proot режим

Для scoped work внутри локального repo можно запускать:

```bash
codex -s danger-full-access -a never
```

Этот режим разрешён только внутри:

```text
~/SOLUM-Platform
```

Он нужен для локальной разработки, где агент должен читать/редактировать repo-файлы и запускать разрешённые project tools.

## Запреты

Даже в рабочем Termux/proot режиме запрещено без отдельной прямой команды пользователя:

- merge main;
- `git reset --hard`;
- `git clean`;
- force push;
- `rm -rf`;
- wildcard deletion;
- чтение secrets, кроме явно разрешённого файла для конкретной задачи;
- вывод токенов в лог;
- package install;
- `curl | bash`;
- изменение runtime/Vulkan/Gradle/build system вне scope.

## Telegram secrets

Для Telegram send разрешён только этот файл и только после явного разрешения:

```text
~/.solum/secrets/telegram.env
```

Запрещено коммитить этот файл или печатать `TELEGRAM_BOT_TOKEN`.
