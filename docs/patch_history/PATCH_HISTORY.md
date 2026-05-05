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
