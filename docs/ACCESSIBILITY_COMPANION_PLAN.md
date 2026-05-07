# ACCESSIBILITY_COMPANION_PLAN — future SOLUM companion app

Этот документ фиксирует Android Accessibility companion для SOLUM.

P01H status: `apps/solum-companion` получил real route layer для status, screenshot, UI tree и action log. Tap/gesture automation, launch и force-stop остаются stub/future only.

## Что даст companion app

Companion app нужен, чтобы агент мог получать факты о SOLUM UI/runtime на телефоне без ручного копирования:

- screenshot текущего SOLUM экрана;
- UI tree для видимых controls;
- controlled launch / force-stop SOLUM apps later;
- visual diagnostics pack для сравнения кадров;
- compact evidence для HTML dashboard и Telegram report.

Companion не заменяет diagnostics и не становится production renderer path.

## Screenshot route

P01H route:

```text
request screenshot
↓
companion проверяет active package allowlist
↓
если API >= 30, вызывает AccessibilityService.takeScreenshot
↓
если screenshot недоступен, пишет честный failed status и reason
↓
делает screenshot только для SOLUM app
↓
сохраняет файл в SOLUMCreative diagnostics area
↓
пишет visual diagnostics manifest
```

Expected output:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

## UI tree route

P01H route:

```text
request UI tree
↓
companion проверяет active package allowlist
↓
читает AccessibilityNode tree
↓
пишет structured JSON
```

Expected output:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
```

UI tree нужен для SOLUM editor controls, panels, errors и status overlay. Он не должен использоваться для чужих приложений.

## Action log route

P01H пишет журнал команд и событий:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
```

Журнал содержит:

- timestamp UTC;
- command;
- status;
- reason при отказе/ошибке;
- active package info;
- output path, если команда пишет файл.

## Status route

P01H status JSON строится из active package tracking:

```text
status=ready
```

только если текущий active package входит в SOLUM allowlist.

Любой другой package возвращает:

```text
status=blocked
reason=package_not_allowlisted
```

## Launch / force-stop

Launch / force-stop остаются future/stub only:

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

## Как вручную включить Accessibility Service

На устройстве Android:

```text
Settings
↓
Accessibility
↓
Installed apps / Downloaded apps
↓
SOLUM Accessibility Companion
↓
Enable
```

После включения открой один из allowlisted SOLUM apps, затем запроси route через будущий bridge/MCP или локальный Android entrypoint.

## Как проверить output files

После capture/dump проверь:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

Если active package не из allowlist, JSON должен содержать:

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

## Visual diagnostics pack

P01H companion пишет:

```text
final.png
visual_diagnostics_manifest.json
ui_tree.json
action_log.json
```

Later Vulkan/render diagnostics may add:

```text
frame_001.png
frame_002.png
diff.png
```

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

## P01H implementation

Companion files:

```text
apps/solum-companion/README.md
apps/solum-companion/AndroidManifest.xml
apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumDeviceAgentState.kt
apps/solum-companion/src/main/res/xml/solum_accessibility_service.xml
```

Real in P01H:

```text
buildStatusJson()
captureScreenshot()
dumpUiTree()
writeActionLog()
buildVisualPack()
```

Stub only after P01H:

```text
launchSolumStub()
forceStopSolumStub()
```

Output paths:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/device_agent/latest/ui_tree.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/final.png
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

P01H не реализует taps, gestures, arbitrary package automation или Telegram UI automation.
