# DIAGNOSTICS_SPEC — диагностика SOLUM

Диагностика — источник правды о том, что реально происходит на телефоне.

Формула:

```text
GitHub = источник кода
Build log = правда о сборке
Diagnostics ZIP = правда о запуске на устройстве
```

## Главные правила

- Runtime/FPS/Vulkan проблемы нельзя чинить угадыванием.
- Нельзя утверждать “работает” без build/runtime evidence.
- Пользователь по умолчанию отправляет один файл: `SOLUM_LATEST_DIAGNOSTICS.zip`.
- Скриншот полезен, но не должен быть единственным источником диагностики.
- Диагностика не должна сама сильно просаживать FPS.

## Latest/archive layout

Preferred root:

```text
/storage/emulated/0/SOLUMCreative/
```

Fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

Diagnostics paths:

```text
SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
SOLUMCreative/diagnostics/archive/<timestamp>_<patch_or_stage>/
SOLUMCreative/reports/archive/<timestamp>_<patch_or_stage>/
```

## Required ZIP structure v1

```text
SOLUM_LATEST_DIAGNOSTICS.zip
  report.html
  summary.txt
  build_log.txt
  runtime_log.txt
  crash_log.txt
  device_info.json
  env_info.json
  git_state.json
  app_state.json
  storage_state.json
  vulkan_caps.json
  performance_history.json
  changed_files.txt
  known_issues.txt
```

Later when renderer/assets exist:

```text
  scene_state.json
  render_state.json
  material_state.json
  asset_state.json
  screenshots/
    viewport_main.png
    material_preview.png
    shadow_test.png
```

## report.html must explain

HTML report должен отвечать:

1. Что произошло?
2. Где ошибка?
3. Что изменилось недавно?
4. Какой файл/log отправить?
5. Что вероятнее всего сломано?
6. Что делать дальше?

Плохо:

```text
5000 строк build log без объяснения
```

Хорошо:

```text
Build failed
Cause: Android resource compiler failed
Likely fix: check aapt2 override
Related: ERR-000X
```

## device_info.json

Минимум:

```json
{
  "manufacturer": "TECNO",
  "model": "CI8n",
  "androidVersion": "13",
  "abi": "arm64-v8a",
  "totalRamMb": 0,
  "availableRamMb": 0,
  "thermalStatus": "unknown"
}
```

## env_info.json

Минимум:

```json
{
  "termux": true,
  "javaVersion": "...",
  "gradleVersion": "...",
  "androidSdkPath": "...",
  "ndkVersion": "...",
  "aapt2Path": "...",
  "clangVersion": "..."
}
```

## git_state.json

```json
{
  "repo": "AsArtilluman8/SOLUM-Platform",
  "branch": "...",
  "commit": "...",
  "dirty": false,
  "latestPatch": "..."
}
```

## vulkan_caps.json

Patch 02 должен добавить Vulkan capability dump без полноценного renderer.

Минимум:

```json
{
  "apiVersion": "...",
  "driverVersion": "...",
  "vendorID": "...",
  "deviceName": "...",
  "deviceType": "...",
  "features": {},
  "limits": {},
  "extensions": [],
  "missingCritical": []
}
```

Важно заранее знать:

- support Vulkan;
- available extensions;
- uniform/storage limits;
- push constant size;
- texture compression ASTC/ETC2;
- compute support;
- geometry/tessellation shader absence;
- timestamp query availability if possible.

## performance_history.json

Минимум:

```json
{
  "samples": [
    {
      "timestamp": "ISO-8601",
      "patch": "P02",
      "commit": "...",
      "scene": "diagnostics_smoke",
      "fpsAvg": 0,
      "frameMsAvg": 0,
      "cpuMsApprox": 0,
      "gpuMsApprox": null,
      "memoryMb": 0,
      "notes": ""
    }
  ]
}
```

## Diagnostics overhead rule

Диагностика не должна сама становиться причиной просадки FPS.

Правила:

- не собирать тяжёлые метрики каждый кадр;
- default sampling interval не чаще 1 раза в 1–5 секунд для heavy metrics;
- frame timing можно собирать лёгким rolling average;
- heavy GPU/debug dumps только по явной кнопке или в Debug mode;
- Diagnostics report должен указывать собственный overhead estimate, если возможно.

## Definition of Done for diagnostics patch

Patch считается готовым только если:

- создаётся latest ZIP;
- создаётся latest HTML;
- есть env/device/git info;
- есть readable summary;
- есть archive copy;
- ошибка сборки/запуска попадает в report;
- пользователь знает, какой один файл отправлять.
