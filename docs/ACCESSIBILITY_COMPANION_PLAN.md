# ACCESSIBILITY_COMPANION_PLAN — future SOLUM companion app

Этот документ фиксирует будущий Android Accessibility companion для SOLUM.

P01G status: создан skeleton в `apps/solum-companion`. Реальные device actions не реализованы и входят в P01H.

## Что даст companion app

Companion app нужен, чтобы агент мог получать факты о SOLUM UI/runtime на телефоне без ручного копирования:

- screenshot текущего SOLUM экрана;
- UI tree для видимых controls;
- controlled launch / force-stop SOLUM apps;
- visual diagnostics pack для сравнения кадров;
- compact evidence для HTML dashboard и Telegram report.

Companion не заменяет diagnostics и не становится production renderer path.

## Screenshot route

Будущий route:

```text
request screenshot
↓
companion проверяет active package allowlist
↓
делает screenshot только SOLUM app
↓
сохраняет файл в SOLUMCreative diagnostics area
↓
bridge/MCP возвращает path
```

Expected output:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

## UI tree route

Будущий route:

```text
request UI tree
↓
companion проверяет active package allowlist
↓
читает AccessibilityNode tree
↓
redacts text if needed
↓
пишет structured JSON
```

Expected output:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
```

UI tree нужен для SOLUM editor controls, panels, errors и status overlay. Он не должен использоваться для чужих приложений.

## Launch / force-stop

Companion может позже дать controlled commands:

```text
launch SOLUM package
force-stop SOLUM package
open latest report path
```

Это должно работать только по SOLUM-only allowlist.

## SOLUM-only allowlist

Разрешены только пакеты SOLUM, например:

```text
com.solum.engine
com.solum.launcher
com.solum.assethub
com.solum.materialstudio
com.asart.solum
```

Любой другой package должен возвращать отказ:

```text
status=blocked
reason=package_not_allowlisted
```

## Запрет на Telegram UI automation

Companion не должен:

- открывать Telegram UI;
- нажимать кнопки Telegram;
- читать Telegram chat UI;
- отправлять сообщения через UI automation.

Telegram send остаётся только через explicit Bot API tool:

```text
tools/send_telegram_report.py
```

## Будущий visual diagnostics pack

Companion и renderer diagnostics позже должны сохранять pack:

```text
final.png
shadow_mask.png
normals.png
depth.png
frame_001.png
frame_002.png
diff.png
```

Назначение:

- `final.png` — итоговый вид;
- `shadow_mask.png` — shadow coverage/debug;
- `normals.png` — normal visualization;
- `depth.png` — depth visualization;
- `frame_001.png` / `frame_002.png` — сравнение соседних кадров;
- `diff.png` — visual regression diff.

Pack должен прикрепляться к diagnostics ZIP и показываться в HTML dashboard.

## Safety

Companion должен:

- быть выключен по умолчанию;
- требовать явного включения пользователем;
- работать только с SOLUM allowlist;
- писать outputs только в SOLUMCreative;
- не читать secrets;
- не менять Vulkan/render state;
- не делать destructive app/device actions.

## P01G skeleton

Добавленные файлы:

```text
apps/solum-companion/README.md
apps/solum-companion/AndroidManifest.xml
apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt
apps/solum-companion/src/main/res/xml/solum_accessibility_service.xml
```

AccessibilityService содержит только stubs:

```text
captureScreenshotStub()
dumpUiTreeStub()
writeActionLogStub()
launchSolumStub()
forceStopSolumStub()
```

Planned outputs:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

P01G не реализует taps outside allowlist и не выполняет real screenshot/UI tree capture.
