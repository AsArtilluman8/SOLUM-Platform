# BUILD_ENV_SPEC — Termux / Android build environment

Этот файл фиксирует правила сборки SOLUM на телефоне через Termux.

## Цель

Сборка должна быть воспроизводимой, диагностируемой и не зависеть от угадывания окружения.

## Главные правила

- Не использовать `/mnt/data` в командах для пользователя.
- Сборка должна писать short и full log.
- Build script должен собирать конкретный модуль, а не весь monorepo без причины.
- Build report должен сохранять env snapshot.
- Gradle/NDK/SDK paths не должны быть захардкожены без проверки.
- Любая build error должна попадать в diagnostics/report.

## Expected Android/Termux paths

Ожидаемые пути могут отличаться, поэтому build script обязан проверять наличие:

```text
$HOME/android-sdk/
$ANDROID_HOME
$ANDROID_SDK_ROOT
/data/data/com.termux/files/usr/bin/aapt2
/data/data/com.termux/files/usr/bin/clang
/data/data/com.termux/files/usr/bin/clang++
```

## Java / Gradle

Build report должен сохранять:

```text
java -version
gradle --version
./gradlew --version
```

Gradle memory для телефона:

```properties
org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
```

Если устройство/проект позволяет, можно увеличить heap, но default должен быть mobile-safe.

## AAPT2 override

В Termux часто нужен local aapt2 override:

```text
-Dandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Build scripts должны проверять наличие Termux `aapt2` и явно писать в report, какой aapt2 используется.

## Module-only build

Нельзя всегда собирать весь monorepo.

Правильный формат:

```text
tools/build_app.sh engine
tools/build_app.sh asset-hub
tools/build_app.sh material-studio
```

Алгоритм:

```text
user selects module
↓
build script validates env
↓
builds only target module
↓
writes logs
↓
copies APK/report to SOLUMCreative/latest
↓
archives previous outputs
```

## Logs

Required outputs:

```text
SOLUMCreative/reports/latest/SOLUM_LATEST_BUILD_LOG.txt
SOLUMCreative/reports/latest/SOLUM_LATEST_BUILD_LOG_SHORT.txt
SOLUMCreative/reports/latest/SOLUM_LATEST_REPORT.html
```

Short log должен содержать:

- module;
- branch/commit;
- build result;
- first meaningful error;
- output APK path if success.

Full log содержит полный Gradle/NDK output.

## APK output

APK должен попадать в:

```text
SOLUMCreative/releases/latest/<app-name>-debug.apk
```

И в archive:

```text
SOLUMCreative/releases/archive/<timestamp>_<patch>/<app-name>-debug.apk
```

## No automatic install by default

Build script не должен устанавливать APK без явной просьбы.

Allowed later:

```text
tools/build_app.sh engine --install
```

Default:

```text
build only
```

## Native/Vulkan build notes

Для Vulkan/native задач report должен сохранять:

- NDK version;
- clang version;
- CMake version if used;
- shader compiler availability if used;
- ABI list;
- minSdk/targetSdk/compileSdk;
- Vulkan headers/source paths.

## Failure handling

Если build failed:

```text
write full log
↓
extract first meaningful error
↓
create report.html
↓
update diagnostics latest
↓
print exact path to latest log/ZIP
```

Нельзя говорить “почти собралось” без логов.

## Future CI note

GitHub Actions может быть добавлен позже, но source of truth для runtime на телефоне остаётся diagnostics ZIP с устройства.
