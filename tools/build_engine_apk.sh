#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
LATEST_DIR="$SOLUM_ROOT/releases/latest"
ARCHIVE_DIR="$SOLUM_ROOT/releases/archive/$(date +%Y%m%d_%H%M%S)_P07A_engine"
REPORT_DIR="$SOLUM_ROOT/reports/latest"
mkdir -p "$LATEST_DIR" "$ARCHIVE_DIR" "$REPORT_DIR"

cd "$ROOT"

PATCH_LABEL="P07A Compact Runtime Debug + Evidence Workflow Cleanup"

print_header() {
  echo "============================================================"
  echo "SOLUM BUILD — $PATCH_LABEL"
  echo "ROOT: $ROOT"
  echo "OUT : $SOLUM_ROOT"
  echo "============================================================"
}

print_success() {
  echo
  echo "============================================================"
  echo "SOLUM BUILD RESULT: OK"
  echo "Patch: $PATCH_LABEL"
  echo "APK latest:  $LATEST_DIR/SOLUM_LATEST.apk"
  echo "APK archive: $ARCHIVE_DIR/SOLUM_Engine_P07A_debug.apk"
  echo "Native log:  $REPORT_DIR/P04_native_build.log"
  echo "Gradle log:  $REPORT_DIR/P04_gradle_build.log"
  echo
  echo "NEXT USER ACTION:"
  echo "1) Install/open: $LATEST_DIR/SOLUM_LATEST.apk"
  echo "2) Expected compact screen text:"
  echo "   - SOLUM Engine"
  echo "   - Vulkan: Mali-G57 MC2"
  echo "   - Status: Vertex Buffer OK"
  echo "   - Next: Mesh Foundation"
  echo "3) Expected visual result: orange triangle remains visible; long debug list is gone."
  echo
  echo "IF ERROR: send these logs:"
  echo "   $REPORT_DIR/P04_native_build.log"
  echo "   $REPORT_DIR/P04_gradle_build.log"
  echo "============================================================"
}

print_header

bash tools/build_native_engine.sh

GRADLE_CMD="./gradlew"
if [ ! -x "$GRADLE_CMD" ]; then
  if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
  else
    echo "SOLUM BUILD RESULT: FAILED"
    echo "Reason: Gradle not found. Install gradle or add Gradle wrapper later." | tee "$REPORT_DIR/P04_gradle_build.log"
    exit 1
  fi
fi

AAPT_ARG=""
if command -v aapt2 >/dev/null 2>&1; then
  AAPT_ARG="-Dandroid.aapt2FromMavenOverride=$(command -v aapt2)"
fi

if ! $GRADLE_CMD $AAPT_ARG :apps:engine:assembleDebug > "$REPORT_DIR/P04_gradle_build.log" 2>&1; then
  echo "SOLUM BUILD RESULT: FAILED"
  echo "Patch: $PATCH_LABEL"
  echo "Gradle failed. Log: $REPORT_DIR/P04_gradle_build.log"
  tail -n 80 "$REPORT_DIR/P04_gradle_build.log" || true
  exit 1
fi

APK="$ROOT/apps/engine/build/outputs/apk/debug/engine-debug.apk"
if [ ! -f "$APK" ]; then
  echo "SOLUM BUILD RESULT: FAILED"
  echo "APK not found: $APK" | tee -a "$REPORT_DIR/P04_gradle_build.log"
  exit 1
fi

cp "$APK" "$LATEST_DIR/SOLUM_LATEST.apk"
cp "$APK" "$ARCHIVE_DIR/SOLUM_Engine_P07A_debug.apk"

print_success
