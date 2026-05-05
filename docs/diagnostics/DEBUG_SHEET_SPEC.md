# DEBUG_SHEET_SPEC — SOLUM Runtime Debug Sheet

## Purpose

Normal viewport must stay clean. Debug data must be visible only on demand.

## UX rule

```text
compact HUD
↓
long press HUD
↓
Debug Sheet opens from bottom
↓
user can save runtime state / see diagnostics path
↓
sheet closes
```

## v1 scope

- Long press compact HUD.
- Bottom Debug Sheet.
- Shows runtime diagnostics path.
- Shows current compact status.
- Writes `runtime_java_state.json`.
- Writes `diagnostics_export_request.json`.

## Out of scope

- No permanent debug buttons on viewport.
- No full editor Debug panel yet.
- No in-app ZIP creation yet.
- No Termux command execution from APK.

## Future v2

- Share diagnostics ZIP.
- SAF export.
- Runtime render graph view.
- FPS history.
- Material/texture table.
