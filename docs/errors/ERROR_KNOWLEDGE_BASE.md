# ERROR_KNOWLEDGE_BASE — база ошибок SOLUM

Цель: не повторять одни и те же ошибки в Termux, Android, Vulkan, Gradle, NDK, asset pipeline и UI.

Если ошибка важная или повторяемая — записать её сюда.

## Формат записи ошибки

```markdown
## ERR-0000: Название ошибки

### Symptoms
Что видит пользователь / build / runtime.

### Context
Где произошло: patch, branch, module, файлы.

### Cause
Реальная причина.

### Fix
Как исправили.

### Prevention
Как не повторить.

### Related diagnostics
Путь к diagnostics archive / report / PR.
```

---

## ERR-0001: Download folder trash / duplicate patch files

### Symptoms

В Download копятся файлы:

```text
patch.py
patch (1).py
patch (2).py
app-debug.apk
report.zip
screenshots
```

### Cause

Патчи/отчёты/APK сохранялись прямо в Download без controlled folder, latest/archive и уникальных имён.

### Fix

Все SOLUM output files должны идти в:

```text
/storage/emulated/0/SOLUMCreative/
```

или fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

Патчи должны иметь уникальные имена с версией.

### Prevention

- Не давать patch-файл с тем же именем повторно.
- Не удалять wildcard-командами без подтверждения.
- Всегда latest/archive structure.

---

## ERR-0002: Runtime claim without diagnostics

### Symptoms

Агент пишет “исправлено/работает”, но нет:

- build success;
- runtime log;
- diagnostics ZIP;
- user confirmation.

### Cause

AI сделал вывод по коду или предположению, но не по факту запуска на телефоне.

### Fix

Всегда требовать proof:

```text
build log
runtime log
diagnostics ZIP
screenshot/user confirmation
```

### Prevention

В `AGENT_RULES.md` закреплено: не утверждать runtime success без доказательства.

---

## ERR-0003: Wrong direction fix / fake workaround

### Symptoms

Для сложной проблемы предлагается выключить систему или заменить её фейком.

Пример:

```text
тени мерцают → выключить тени
нужен CSM → сделать blob shadow
Vulkan сложно → сделать OpenGL preview
```

### Cause

Патч решает симптом, но уводит от финальной архитектуры.

### Fix

Использовать minimal final-system version, а не throwaway substitute.

### Prevention

См. `ARCHITECTURE_RULES.md`:

```text
MVP may be incomplete, but not incorrect.
```

---

## ERR-0004: Too many micro-patches

### Symptoms

Много маленьких патчей:

- одна кнопка;
- один slider;
- один debug label;
- мелкие UI изменения без цельного результата.

### Cause

Патчи делались слишком осторожно без diagnostics/test strategy.

### Fix

Крупные проверяемые vertical-system patches.

### Prevention

Микропатчи допустимы только для build/compile/hotfix после diagnostics.

---

## ERR-0005: UI button soup instead of tool

### Symptoms

Вместо нормального инструмента появляются кнопки:

```text
+X -X +Y -Y +Z -Z
```

или много debug/buttons поверх viewport.

### Cause

AI сделал технический UI вместо mobile editor UX.

### Fix

Использовать:

- on-object gizmo;
- context toolbar;
- bottom sheet;
- compact precision scrub controls;
- advanced hidden.

### Prevention

См. `UX_AND_WORKFLOW_RULES.md`.

---

## Future errors to add

Следующие категории должны пополняться по мере разработки:

- Gradle/Termux/AAPT2 errors.
- NDK/clang/CMake errors.
- Android storage/permission errors.
- Vulkan instance/device/swapchain errors.
- Descriptor/binding/pipeline layout errors.
- Shader compile/SPIR-V errors.
- Asset schema/validator/migration errors.
- APK signing/keystore errors.
- Runtime crash patterns.
- FPS/performance regressions.
