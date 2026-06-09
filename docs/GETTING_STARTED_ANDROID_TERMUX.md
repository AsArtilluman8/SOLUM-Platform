# Getting Started on Android / Termux

SOLUM is currently developed primarily on Android through Termux.

## Current build command

```bash
bash tools/build_native_engine.sh && \
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
gradle --no-daemon -p "$PWD" clean assembleDebug && \
mkdir -p /storage/emulated/0/Download/SOLUM_APK && \
cp apps/engine/build/outputs/apk/debug/engine-debug.apk \
/storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk
```

## Output APK

```text
/storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk
```
