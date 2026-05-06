# HUMAN_REPORTS_SPEC — понятные отчёты агента

Этот документ фиксирует формат P01D: Telegram + HTML report pack для человека, а не для парсера.

## Цель

После scoped work агент должен оставить короткий отчёт, который можно быстро прочитать в Telegram и открыть как HTML-файл.

Основные файлы:

```text
_work/agent_reports/latest/SOLUM_TELEGRAM_REPORT.txt
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

## Telegram text report

Текстовый отчёт пишется на русском и содержит блоки:

```text
✅ SOLUM Agent Report

Патч: PXX — название
Статус: готово к review

Что сделал:
++ ...

Что проверил:
++ ...

Что не трогал:
-- ...

Проблемы:
!! ...

Нагрузка:
🟢 LOW / 🟡 MEDIUM / 🔴 HIGH

Следующий шаг:
-> ...

Файлы:
++ ...
```

## HTML dashboard

HTML-отчёт должен быть dashboard, а не копия TXT.

- крупный статус patch;
- блок `Простыми словами`;
- блоки действий, проверок, out of scope, проблем, next step и файлов;
- таблицу изменённых файлов;
- timeline `docs read -> patch -> checks -> PR -> Telegram`;
- visual cards: Build, Runtime, Vulkan, Telegram, Context Load;
- progress bar готовности;
- Debug / Metrics section;
- Context Load / Token Load Estimate;
- встроенную текстовую версию как secondary debug view.

HTML должен быть самодостаточным файлом без внешних CSS/JS и без secrets.

Подробный dashboard format:

```text
docs/AGENT_DASHBOARD_REPORTS.md
```

## Обозначения

```text
++ сделано / проверено / файл создан
-- не трогал / вне scope
!! проблема / ограничение / честный риск
-> следующий шаг
```

Эти маркеры нужны, чтобы отчёт легко читался в Telegram без сложной разметки.

## Context Load / Token Load Estimate

Отчёт показывает только примерную нагрузку:

```text
🟢 LOW
🟡 MEDIUM
🔴 HIGH
```

Запрещено писать точное число токенов. Точная метрика недоступна локальному project tool и не должна имитироваться.

## Optional metrics JSON

Если есть файл:

```text
_work/agent_reports/latest/SOLUM_AGENT_METRICS.json
```

HTML показывает supported runtime/visual/check metrics.

Если файла нет, HTML честно пишет:

```text
Метрики недоступны: runtime/visual diagnostics не запускались
```

Если поле равно `null`, HTML показывает `not_available`.

## Safety

- Не писать `TELEGRAM_BOT_TOKEN`.
- Не писать secrets в отчёты.
- Не читать secrets при генерации отчёта.
- Telegram send разрешён только отдельным tool и только после явного разрешения пользователя.
- Если HTML/TXT отсутствуют, отправитель должен написать это в summary и не падать только из-за отсутствия report-файлов.
