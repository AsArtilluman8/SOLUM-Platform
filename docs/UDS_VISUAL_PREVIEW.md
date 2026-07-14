# P62 UDS Visual Truth Preview

Локальный WebGL2 preview строится только из P60/P61 evidence. Нейтральная сцена не является DemoMap. UDS-система без полной source→browser связи остаётся заблокированной; VISUAL_HTML_GATE поэтому может честно быть `FAIL`, даже когда техническая валидация проходит.

Одна команда сборки, полной проверки и запуска:

```bash
python3 tools/uds_visual/serve_uds_preview.py --p60 <P60_TRUTH_ROOT> --p61 <P61_SCENE_TRUTH_ROOT>
```

Адрес: `http://127.0.0.1:8765/`.

Генерируются:

- `generated_local/uds_visual_preview/data/UDS_VISUAL_CONTRACT.json`;
- `generated_local/uds_visual_preview/data/UDS_VISUAL_ASSET_MANIFEST.json`;
- `generated_local/uds_visual_preview/data/UDS_VISUAL_EVIDENCE.json`;
- `generated_local/uds_visual_preview/data/UDS_VISUAL_CAPABILITIES.json`;
- `generated_local/uds_visual_preview/reports/UDS_VISUAL_BUILD_REPORT.json`;
- `generated_local/uds_visual_preview/reports/VISUAL_HTML_GATE.json`.

`generated_local` содержит локальные Marketplace-derived payloads и не коммитится. В Git находятся только инструменты, схемы, тесты и HTML/JS/shader templates.
