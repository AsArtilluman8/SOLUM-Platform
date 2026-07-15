# P62B SOLUM Native Environment HTML

P62B — независимая мобильная WebGL2-система окружения SOLUM. UDS/UDW используются как референс поведения и источник подтверждённых параметров, но Blueprint, Niagara VM, Material Graph и MetaSound в браузере не исполняются. Visual parity с UDS не заявляется.

Одна команда сборки, проверки детерминизма и локального запуска:

```bash
python3 tools/uds_visual/serve_solum_environment.py
```

Адрес: `http://127.0.0.1:8765/`.

Команда по умолчанию читает локальные truth roots:

- `/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH`;
- `/mnt/shared/Download/SOLUM_UDS_P61_SCENE_TRUTH`.

Их можно переопределить аргументами `--p60`, `--p61`, `--output`, `--bind`, `--port`.

Runtime HTML читает только компактный пакет `data/solum_environment_package.json`, локальные шейдеры и четыре вручную включаемых WAV. Отчёты P60/P61 и абсолютные пути в runtime не входят. CDN и сетевые зависимости отсутствуют.

Реализованы 13 погодных presets, день/ночь, солнце, луна, звёзды, атмосфера, облака, туман, дождь, снег, пыль, ветер, молния, влажность, wetness и плавные переходы. Источник каждого значения отмечен как `UDS_VERIFIED`, `UDS_DERIVED_MAPPING`, `SOLUM_NATIVE`, `UNKNOWN` или `UNAVAILABLE`.

Извлечённые Oodle/legacy-текстуры не активируются: при недоступном browser-decodable payload используются явно помеченные процедурные `SOLUM_NATIVE` fallback. Автоматическое сопоставление MetaSound отсутствует; проверенные WAV доступны только вручную. Thunder binding остаётся `UNKNOWN`, поэтому автоматический гром не включается.

Генерируются:

- `generated_local/uds_visual_preview/data/solum_environment_package.json`;
- `generated_local/uds_visual_preview/index.html` и локальные CSS/JS/shaders/audio;
- `generated_local/uds_visual_preview/reports/SOLUM_ENVIRONMENT_BUILD_REPORT.json`;
- `generated_local/uds_visual_preview/reports/SOLUM_ENVIRONMENT_VALIDATION.json`.

`generated_local` содержит локальные Marketplace-derived payloads и не коммитится. В Git находятся инструменты, схемы, тесты и HTML/JS/shader templates.
