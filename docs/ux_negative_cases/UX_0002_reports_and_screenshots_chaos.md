# UX-0002: Reports, screenshots and ZIP chaos

## Problem

Для диагностики приходилось отправлять много отдельных файлов:

- zip;
- build log;
- runtime log;
- screenshots;
- ещё один report;
- новый dump.

Потом становилось непонятно, какой файл актуальный.

## Why bad

- пользователь тратит время на поиск файлов;
- чат теряет контекст;
- анализ без полной картины превращается в угадайку;
- screenshots не должны быть единственным способом понять сцену/material/render state.

## Rule

У проекта всегда должен быть один актуальный diagnostics ZIP:

```text
SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
```

И один актуальный HTML report:

```text
SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
```

## Required solution

Diagnostics ZIP должен содержать:

- report.html;
- summary.txt;
- build_log.txt;
- runtime_log.txt;
- crash_log.txt;
- device_info.txt;
- git_info.txt;
- scene_state.json;
- asset_state.json;
- render_state.json;
- material_state.json;
- performance_history.json;

Скриншоты полезны, но должны быть внутри diagnostics ZIP, а не отдельным обязательным файлом.
