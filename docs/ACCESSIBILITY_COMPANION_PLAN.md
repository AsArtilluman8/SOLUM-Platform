# ACCESSIBILITY_COMPANION_PLAN — future SOLUM companion app

Этот документ фиксирует Android Accessibility companion для SOLUM.

P01H status: `apps/solum-companion` получил real route layer для status, screenshot, UI tree и action log. Follow-up подключил companion как отдельный Gradle Android app module. Tap/gesture automation, launch и force-stop остаются stub/future only.

P01H2 status: companion получает launcher Activity и ручной test screen, чтобы установленный APK был открываемым приложением, а не только Accessibility service.

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

## P01H2 launcher Activity

Launcher Activity:

```text
apps/solum-companion/src/main/java/com/solum/companion/MainActivity.kt
```

Manifest entry:

```text
android.intent.action.MAIN
android.intent.category.LAUNCHER
```

Экран делает только ручные действия:

- показывает `packageName`, `appVersion` и output paths;
- открывает Android Accessibility Settings;
- открывает App Details Settings для `com.solum.companion`;
- пишет manual evidence files без screenshot;
- открывает системный folder picker, если Android это поддерживает.

P01H2 не добавляет taps, gestures, renderer hooks или Telegram UI automation.

## P01H2 manual test flow

После установки APK:

```text
Open SOLUM Companion
↓
press Test Write Evidence Files
↓
check toast success/failure
↓
verify files
```

Expected files:

```text
/storage/emulated/0/SOLUMCreative/device_agent/latest/action_log.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/visual_diagnostics_manifest.json
```

Manual test пишет только JSON evidence. Screenshot route остаётся Accessibility route.

## TECNO/HiOS Restricted Settings blocker

Known blocker:

```text
Доступ к настройкам ограничен
```

Manual route:

```text
Settings -> Apps -> SOLUM Companion -> menu/dots -> Allow restricted settings
```

На некоторых TECNO/HiOS этот пункт может быть скрыт. Если пункта нет, используй adb/wireless debugging install route. Companion screen должен показывать эту инструкцию, потому что приложение не может программно обойти Android restricted settings policy.

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
settings.gradle
build.gradle
apps/solum-companion/README.md
apps/solum-companion/build.gradle
apps/solum-companion/AndroidManifest.xml
apps/solum-companion/src/main/java/com/solum/companion/MainActivity.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumAccessibilityService.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumCompanionCommand.kt
apps/solum-companion/src/main/java/com/solum/companion/SolumDeviceAgentState.kt
apps/solum-companion/src/main/res/xml/solum_accessibility_service.xml
```

Gradle module:

```text
:apps:solum-companion
```

Build command:

```text
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle :apps:solum-companion:assembleDebug
```

Successful APK output:

```text
apps/solum-companion/build/outputs/apk/debug/solum-companion-debug.apk
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

P01H2 не меняет Vulkan renderer/material logic.
