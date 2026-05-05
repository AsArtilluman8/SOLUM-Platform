# PATCH_HISTORY — история патчей SOLUM

Этот файл фиксирует историю патчей, результаты, ошибки, диагностику и следующие шаги.

## Формат записи

```markdown
## Patch PXX — Название

### Goal
Что должен был закрыть патч.

### Scope
Что входит.

### Changed files/modules
Список модулей/файлов.

### Build result
SUCCESS / FAILED / NOT TESTED

### Runtime result
SUCCESS / FAILED / NOT TESTED

### Diagnostics
Путь к latest/archive ZIP/report.

### User-visible result
Что пользователь должен увидеть.

### Known issues
Что осталось.

### Lessons
Что запомнить в future patches.

### Next
Следующий шаг.
```

---

## Patch P01 — Repository / Documentation Foundation

### Goal

Создать начальную память проекта и зафиксировать правила, которые были обсуждены до начала кода.

### Scope

- README.
- Project memory index.
- Current stage.
- Agent rules.
- Architecture rules.
- UX/workflow rules.
- Patch roadmap.
- Rendering target spec.
- Asset format spec.
- Error knowledge base.
- ADR foundation.
- UX negative cases.
- Ideas foundation.
- Repository folder skeleton placeholders.

### Build result

NOT TESTED — documentation-only patch.

### Runtime result

NOT TESTED — documentation-only patch.

### User-visible result

GitHub repo becomes a structured project memory instead of empty repository.

### Known issues

- No build system yet.
- No Android app yet.
- No diagnostics script yet.
- No Vulkan capability check yet.

### Lessons

Start narrow. Do not build multi-APK suite before core/diagnostics/asset/Vulkan foundation.

### Next

Patch P02 — Diagnostics v1 + Vulkan Capability Check.

---

## Patch P02 — Diagnostics v1 + Vulkan Capability Check

### Goal

Create the first real diagnostics layer and collect device/env/git/storage/Vulkan capability facts before renderer work.

### Scope

- `tools/collect_diagnostics.sh`.
- `tools/report_builder.py`.
- `tools/vulkan_caps/vulkan_caps.c`.
- `tools/vulkan_caps/build_and_run_vulkan_caps.sh`.
- `tools/vulkan_caps/README.md`.
- Latest/archive diagnostics layout.
- `SOLUM_LATEST_DIAGNOSTICS.zip` output.
- `SOLUM_LATEST_REPORT.html` output.
- `vulkan_caps.json` schema.
- `performance_history.json` v1 placeholder.

### Build result

NOT TESTED in GitHub environment — must be run on Termux/Android target device.

### Runtime result

NOT TESTED in GitHub environment — no Android app/runtime in Patch P02.

### Diagnostics

Expected after user runs:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/SOLUM_LATEST_DIAGNOSTICS.zip
/storage/emulated/0/SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
```

Fallback root:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

### User-visible result

User runs one command and gets one diagnostics ZIP + one HTML report.

### Known issues

- Vulkan caps may fail from Termux shell on some devices.
- If Vulkan caps fails, report must show honest `status=failed` and include build log.
- No Vulkan swapchain, renderer, triangle, material or shadow system in this patch by design.

### Lessons

Diagnostics must be useful but low-overhead. Patch P02 establishes facts before graphics systems.

### Next

After user provides diagnostics ZIP: fix P02 if needed, then Patch P03 — Asset Schema v1 + Transaction Save.

---

## Patch P03 — Asset Schema v1 + Transaction Save

### Goal

Create the first real SOLUM asset format foundation and safe file write path.

### Scope

- `schemas/asset_manifest.schema.json`.
- `schemas/project_manifest.schema.json`.
- `tools/transaction_save.py`.
- `tools/asset_validator.py`.
- `tools/create_sample_asset.py`.
- `docs/research/NOTE_0002_p03_asset_schema_transaction_save.md`.

### Build result

NOT TESTED in GitHub environment — Python/Termux tool patch.

### Runtime result

NOT TESTED in GitHub environment — no Android runtime.

### Diagnostics

Verified locally by user in Termux:

```text
Sample asset created: /storage/emulated/0/SOLUMCreative/assets/materials/sample_material
Status: valid
Report: /storage/emulated/0/SOLUMCreative/assets/materials/sample_material/validation_report.json
```

### User-visible result

User can create a sample material asset folder and validate it.

### Known issues

- No Asset Hub UI yet.
- No material preview yet.
- No glTF import yet.
- No zip/bundle format yet.

### Lessons

Asset format must exist before UI tools. Transaction save prevents corrupted files and gives future editors safe write behavior.

### Next

Patch P04 — Vulkan Foundation v1 or Patch P03A fix if validation fails on device.

---

## Patch P04 — Vulkan Foundation v1

### Goal

Create the first Android APK runtime path for SOLUM Engine and verify Android Native Vulkan separately from Termux llvmpipe.

### Scope

- Root Gradle settings and Android application module.
- `apps/engine` Android Activity and SurfaceView.
- Manual prebuilt `.so` route for Termux.
- Native C++ Vulkan foundation.
- VkInstance.
- Android surface.
- physical device selection.
- VkDevice.
- swapchain creation.
- runtime Vulkan capability report.
- APK output script.

### Build result

NOT TESTED in GitHub environment — must be built in Termux.

### Runtime result

NOT TESTED in GitHub environment — must run APK on target Android phone.

### Diagnostics

Expected build outputs:

```text
/storage/emulated/0/SOLUMCreative/releases/latest/SOLUM_LATEST.apk
/storage/emulated/0/SOLUMCreative/reports/latest/P04_native_build.log
/storage/emulated/0/SOLUMCreative/reports/latest/P04_gradle_build.log
```

Expected runtime output after launching APK:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/runtime_vulkan_caps.json
```

### User-visible result

APK opens `SOLUM Engine` and shows runtime status overlay:

```text
Renderer path: Android Native Vulkan
GPU: ...
Swapchain: created
```

### Known issues

- P04 may only create swapchain/runtime report. Clear/triangle draw pass can move to P04A after build/runtime foundation is verified.
- Manual prebuilt `.so` route is chosen for Termux reliability.
- Real Mali-G57 appears only through Android APK runtime path, not Termux shell Vulkan.

### Lessons

Do not start PBR/materials/shadows before Android Vulkan runtime path is proven.

### Next

Run `bash tools/build_engine_apk.sh`, install/open APK manually, then send diagnostics ZIP/logs if build or runtime fails.
