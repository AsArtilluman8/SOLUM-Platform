# AGENT_DASHBOARD_REPORTS — HTML dashboard отчёты агента

Этот документ фиксирует доработку P01D: HTML report должен быть dashboard, а не копия Telegram TXT.

## Назначение

Dashboard нужен для быстрого review после agent work:

- крупно показывает статус патча;
- объясняет результат простыми словами;
- отделяет сделанное, проверки, out of scope, проблемы и next step;
- показывает timeline этапов;
- показывает visual cards: Build, Runtime, Vulkan, Telegram, Context Load;
- показывает progress bar готовности;
- показывает Debug / Metrics.

Основной файл:

```text
_work/agent_reports/latest/SOLUM_AGENT_REPORT.html
```

## Metrics JSON

Dashboard опционально читает:

```text
_work/agent_reports/latest/SOLUM_AGENT_METRICS.json
```

Поддерживаемые поля:

```json
{
  "fps_current": null,
  "fps_previous": null,
  "fps_delta": null,
  "quality_score": null,
  "material_score": null,
  "shadow_score": null,
  "visual_status": "not_tested",
  "changed_files_count": 7,
  "checks_passed": 5,
  "checks_failed": 0
}
```

Правила:

- `fps_*` показываются как runtime metrics.
- `quality_score`, `material_score`, `shadow_score` показываются horizontal bars.
- `visual_status` показывает статус визуальной проверки.
- `changed_files_count` помогает сверить размер patch.
- `checks_passed` и `checks_failed` управляют checks progress bar.

## Если runtime/visual данных нет

Если metrics JSON отсутствует, dashboard обязан честно писать:

```text
Метрики недоступны: runtime/visual diagnostics не запускались
```

Если отдельное поле равно `null`, dashboard показывает:

```text
not_available
```

Нельзя придумывать FPS, visual score или screenshot status.

## Диаграммы

Разрешены только self-contained элементы:

- CSS progress bars;
- horizontal bars;
- таблицы;
- cards;
- timeline.

Запрещено:

- внешние JS/CSS;
- CDN;
- package install;
- подмена runtime/Vulkan проверки fake visual status.

## Screenshots later

Позже screenshots можно подключить как отдельный блок:

```text
_work/agent_reports/latest/screenshots/
```

Правила для будущего подключения:

- thumbnail в HTML только из локального report bundle;
- явно подписывать viewport/device/build;
- если screenshot не создан, писать `not_available`;
- не считать screenshot заменой diagnostics ZIP или runtime log.
