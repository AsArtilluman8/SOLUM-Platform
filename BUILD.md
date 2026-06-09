# Build SOLUM Platform

Status: public build guide.

SOLUM is an early-stage Android and Filament renderer/editor foundation. Build instructions should stay reproducible for contributors and AI/code agents.

## Requirements

Use a working Android build environment with:

- Java/JDK available to Gradle;
- Android SDK installed;
- Gradle available from the repository or environment;
- native build prerequisites required by `tools/build_native_engine.sh`.

## Debug APK build

From the repository root:

```bash
bash tools/build_native_engine.sh
gradle --no-daemon -p "$PWD" clean assembleDebug
```

Expected Gradle APK output:

```text
apps/engine/build/outputs/apk/debug/engine-debug.apk
```

Local developer scripts may copy the APK to a device-specific folder, but public docs should keep the canonical Gradle output path first.

## Build report template

When reporting a build, include:

- branch name;
- commit SHA;
- build command used;
- success or failure;
- APK path;
- first relevant compiler error if failed;
- changed files.

## Current limitations

- This is not yet a polished end-user SDK.
- Renderer/editor foundation is still being extracted from Activity-local code.
- Build documentation should be updated when the project gains a cleaner multi-host setup.
