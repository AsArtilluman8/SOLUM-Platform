# VISUAL_QA_SPEC — visual benchmark and regression rules

Этот файл фиксирует правило: нельзя оценивать render/UI патч только по словам “стало лучше”. Нужны эталонные кадры, сравнение и список визуальных багов.

## Цель

Защитить SOLUM от ситуации:

```text
FPS стал лучше
↓
но картинка стала хуже
↓
агент пишет “оптимизация успешна”
```

Visual QA = проверка, что пользователь реально видит улучшение, а не деградацию.

## Visual benchmark pack

Перед крупными render/material/light/shadow/UI/world патчами должен быть benchmark pack:

```text
SOLUMCreative/visual_benchmarks/latest/
  benchmark_manifest.json
  01_main_view.png
  02_close_object.png
  03_shadow_contact.png
  04_material_preview.png
  05_ui_editor.png
  06_asset_card.png
  07_world_view.png
  08_cinematic_view.png
```

Не все кадры обязательны на раннем этапе. Но когда соответствующая система появляется, кадр добавляется.

## benchmark_manifest.json

```json
{
  "schema": "solum.visual_benchmark",
  "schemaVersion": 1,
  "patch": "PXX",
  "commit": "...",
  "device": "...",
  "resolution": "...",
  "renderer": "Vulkan",
  "qualityPreset": "balanced",
  "screenshots": [
    {
      "id": "main_view",
      "path": "01_main_view.png",
      "description": "Main editor viewport"
    }
  ]
}
```

## Before / after flow

```text
сделать baseline screenshots
↓
применить patch
↓
сделать after screenshots
↓
сравнить
↓
записать visual_result.md
↓
если картинка стала хуже — patch не считается успешным без объяснения
```

## visual_result.md

Минимум:

```markdown
# Visual Result — PXX

## Improved
- ...

## Regressed
- ...

## Same
- ...

## Known visual issues
- ...

## Decision
Accepted / Needs fix / Reverted
```

## Known visual bug list

В каждом render-heavy этапе должен быть список известных визуальных багов:

- z-fighting;
- shadow acne;
- peter-panning;
- jagged shadows;
- shimmer/flicker;
- object floating/sinking;
- material too plastic;
- water sorting issues;
- UI overlap;
- text clipping;
- preview not updating live.

## Visual QA rules

- Render optimization не считается успешной, если качество упало без явного решения.
- UI patch не считается успешным без screenshot или user-visible description.
- Material/light/shadow patch должен иметь visual before/after when possible.
- Screenshot полезен, но должен быть частью diagnostics/visual benchmark folder, а не хаосом в Download.

## Not required yet

На ранних этапах можно начать с HTML report + manual screenshots. Pixel-perfect automated image diff не обязателен в v1.

Позже можно добавить:

- perceptual diff;
- histogram comparison;
- overlay comparison;
- side-by-side HTML viewer.
