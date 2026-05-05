#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
LATEST_DIR="$SOLUM_ROOT/releases/latest"
ARCHIVE_DIR="$SOLUM_ROOT/releases/archive/$(date +%Y%m%d_%H%M%S)_P04_engine"
REPORT_DIR="$SOLUM_ROOT/reports/latest"
mkdir -p "$LATEST_DIR" "$ARCHIVE_DIR" "$REPORT_DIR"

cd "$ROOT"

bash tools/build_native_engine.sh

GRADLE_CMD="./gradlew"
if [ ! -x "$GRADLE_CMD" ]; then
  if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
  else
    echo "Gradle not found. Install gradle or add Gradle wrapper later." | tee "$REPORT_DIR/P04_gradle_build.log"
    exit 1
  fi
fi

AAPT_ARG=""
if command -v aapt2 >/dev/null 2>&1; then
  AAPT_ARG="-Dandroid.aapt2FromMavenOverride=$(command -v aapt2)"
fi

$GRADLE_CMD $AAPT_ARG :apps:engine:assembleDebug > "$REPORT_DIR/P04_gradle_build.log" 2>&1

APK="$ROOT/apps/engine/build/outputs/apk/debug/engine-debug.apk"
if [ ! -f "$APK" ]; then
  echo "APK not found: $APK" | tee -a "$REPORT_DIR/P04_gradle_build.log"
  exit 1
fi

cp "$APK" "$LATEST_DIR/SOLUM_LATEST.apk"
cp "$APK" "$ARCHIVE_DIR/SOLUM_Engine_P04_debug.apk"

echo "APK OK: $LATEST_DIR/SOLUM_LATEST.apk"
echo "Archive: $ARCHIVE_DIR/SOLUM_Engine_P04_debug.apk"
echo "Gradle log: $REPORT_DIR/P04_gradle_build.log"
