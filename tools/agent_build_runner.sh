#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TS="$(date +%Y%m%d_%H%M%S)"

if [ -d "/storage/emulated/0" ]; then
  OUTROOT="/storage/emulated/0/SOLUMCreative"
elif [ -d "/sdcard" ]; then
  OUTROOT="/sdcard/SOLUMCreative"
else
  OUTROOT="$HOME/SOLUMCreative"
fi

REPORT_LATEST="$OUTROOT/reports/latest"
DIAG_LATEST="$OUTROOT/diagnostics/latest"
RELEASE_LATEST="$OUTROOT/releases/latest"
APK_OUT_ROOT="/storage/emulated/0/Download/SOLUM_APK"

mkdir -p "$REPORT_LATEST" "$DIAG_LATEST" "$RELEASE_LATEST" "$APK_OUT_ROOT" _work/agent_reports/latest

FULL_LOG="$REPORT_LATEST/SOLUM_LATEST_BUILD_LOG.txt"
SHORT_LOG="$REPORT_LATEST/SOLUM_LATEST_BUILD_LOG_SHORT.txt"
LOCAL_FULL="_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG.txt"
LOCAL_SHORT="_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt"
ZIP_OUT="$DIAG_LATEST/SOLUM_LATEST_DIAGNOSTICS.zip"

: > "$FULL_LOG"
: > "$SHORT_LOG"
: > "$LOCAL_FULL"
: > "$LOCAL_SHORT"

log(){ echo "$*" | tee -a "$FULL_LOG" "$LOCAL_FULL"; }
short(){ echo "$*" | tee -a "$SHORT_LOG" "$LOCAL_SHORT"; }

append_short_from_full(){
  local pattern="${1:-}"
  local context="${2:-80}"
  local line=""
  local start=""
  local end=""

  line="$(grep -nEi "$pattern" "$FULL_LOG" 2>/dev/null | head -1 | cut -d: -f1 || true)"
  if [ -z "$line" ]; then
    return 1
  fi

  start=$((line - context))
  if [ "$start" -lt 1 ]; then
    start=1
  fi
  end=$((line + context))

  short "Gradle exception context: lines $start-$end around first match at line $line"
  sed -n "${start},${end}p" "$FULL_LOG" | tee -a "$SHORT_LOG" "$LOCAL_SHORT" >/dev/null || true
  return 0
}

path_state(){
  local label="$1"
  local value="$2"

  if [ -n "$value" ] && [ -d "$value" ]; then
    log "$label=$value [dir exists]"
  elif [ -n "$value" ]; then
    log "$label=$value [missing]"
  else
    log "$label= [empty]"
  fi
}

copy_named_apk(){
  local label="$1"
  local src="$2"
  local latest_name="$3"
  local download_name="$4"

  if [ -n "$src" ] && [ -f "$src" ]; then
    cp "$src" "$RELEASE_LATEST/$latest_name" 2>/dev/null || true
    cp "$src" "$APK_OUT_ROOT/$download_name" 2>/dev/null || true
    cp "$src" "$APK_OUT_ROOT/$latest_name" 2>/dev/null || true
    short "$label=$APK_OUT_ROOT/$download_name"
    short "${label}_ALIAS=$APK_OUT_ROOT/$latest_name"
  else
    short "$label=not found"
  fi
}

detect_android_sdk(){
  RESOLVED_ANDROID_SDK=""

  if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME:-}" ]; then
    RESOLVED_ANDROID_SDK="$ANDROID_HOME"
  elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT:-}" ]; then
    RESOLVED_ANDROID_SDK="$ANDROID_SDK_ROOT"
  elif [ -d "$HOME/android-sdk" ]; then
    RESOLVED_ANDROID_SDK="$HOME/android-sdk"
  elif [ -d "/data/data/com.termux/files/home/android-sdk" ]; then
    RESOLVED_ANDROID_SDK="/data/data/com.termux/files/home/android-sdk"
  fi
}

HAS_GRADLE_MARKERS=0
GRADLE_MARKERS_FOUND=""
for marker in settings.gradle settings.gradle.kts settings.gradle.dcl build.gradle build.gradle.kts build.gradle.dcl gradlew; do
  if [ -f "$marker" ]; then
    HAS_GRADLE_MARKERS=1
    if [ -n "$GRADLE_MARKERS_FOUND" ]; then
      GRADLE_MARKERS_FOUND="$GRADLE_MARKERS_FOUND,$marker"
    else
      GRADLE_MARKERS_FOUND="$marker"
    fi
  fi
done

log "SOLUM Agent Build Runner"
log "timestamp=$TS"
log "root=$ROOT"
log "outroot=$OUTROOT"

log ""
log "== Foundation readiness =="
if [ -x "tools/check_foundation_readiness.sh" ]; then
  set +e
  bash tools/check_foundation_readiness.sh 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL"
  FOUNDATION_CODE=${PIPESTATUS[0]}
  if [ "$FOUNDATION_CODE" -eq 0 ]; then
    short "FOUNDATION_READINESS=FOUNDATION_READY"
  else
    short "FOUNDATION_READINESS=FOUNDATION_NOT_READY"
    short "Foundation report=_work/agent_reports/latest/SOLUM_FOUNDATION_READINESS.txt"
  fi
else
  short "FOUNDATION_READINESS=not_available"
fi

log ""
log "== Git state =="
git branch --show-current 2>/dev/null | tee -a "$FULL_LOG" "$LOCAL_FULL" || true
git rev-parse --short HEAD 2>/dev/null | tee -a "$FULL_LOG" "$LOCAL_FULL" || true
git status --short 2>/dev/null | tee -a "$FULL_LOG" "$LOCAL_FULL" || true

log ""
log "== Environment =="
uname -a 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL" || true
java -version 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL" >/dev/null || true
if [ -x "./gradlew" ]; then
  ./gradlew --version 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL" >/dev/null || true
elif [ "$HAS_GRADLE_MARKERS" -eq 1 ] && command -v gradle >/dev/null 2>&1; then
  gradle --version 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL" >/dev/null || true
fi

path_state "ANDROID_HOME" "${ANDROID_HOME:-}"
path_state "ANDROID_SDK_ROOT" "${ANDROID_SDK_ROOT:-}"
path_state "HOME_ANDROID_SDK" "$HOME/android-sdk"
path_state "TERMUX_HOME_ANDROID_SDK" "/data/data/com.termux/files/home/android-sdk"

AAPT2_PATH="$(command -v aapt2 2>/dev/null || true)"
CLANG_PATH="$(command -v clang 2>/dev/null || true)"
if [ -n "$AAPT2_PATH" ]; then
  log "aapt2=$AAPT2_PATH"
else
  log "aapt2=not found"
fi
if [ -n "$CLANG_PATH" ]; then
  log "clang=$CLANG_PATH"
else
  log "clang=not found"
fi

detect_android_sdk
if [ -n "$RESOLVED_ANDROID_SDK" ]; then
  log "resolved_android_sdk=$RESOLVED_ANDROID_SDK"
  export ANDROID_HOME="$RESOLVED_ANDROID_SDK"
  export ANDROID_SDK_ROOT="$RESOLVED_ANDROID_SDK"
else
  log "resolved_android_sdk=not found"
fi

BUILD_CMD=""
log "gradle_markers=${GRADLE_MARKERS_FOUND:-none}"
GRADLE_VALIDATION_STATUS="not_checked"
GRADLE_VALIDATION_CODE=""

if [ -f "./gradlew" ]; then
  if [ ! -x "./gradlew" ]; then
    chmod +x ./gradlew 2>/dev/null || true
  fi
  BUILD_CMD="./gradlew assembleDebug"
  GRADLE_VALIDATION_STATUS="valid_gradlew"
elif [ "$HAS_GRADLE_MARKERS" -eq 1 ] && command -v gradle >/dev/null 2>&1; then
  log "gradle_project_validation=gradle -q projects"
  set +e
  gradle -q projects 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL"
  GRADLE_VALIDATION_CODE=${PIPESTATUS[0]}
  set -e

  if [ "$GRADLE_VALIDATION_CODE" -eq 0 ]; then
    BUILD_CMD="gradle assembleDebug"
    GRADLE_VALIDATION_STATUS="valid_global_gradle"
  else
    GRADLE_VALIDATION_STATUS="invalid_global_gradle"
  fi
elif [ "$HAS_GRADLE_MARKERS" -eq 1 ]; then
  GRADLE_VALIDATION_STATUS="gradle_command_missing"
fi

log ""
log "== Build =="

if [ -z "$BUILD_CMD" ]; then
  if [ "$HAS_GRADLE_MARKERS" -eq 1 ]; then
    RESULT="NO_VALID_GRADLE_BUILD"
    short "RESULT=$RESULT"
    short "Reason: Gradle markers found at repo root, but Gradle did not recognize a valid root project."
    short "Gradle markers=${GRADLE_MARKERS_FOUND:-none}"
    short "Gradle validation status=$GRADLE_VALIDATION_STATUS"
    if [ -n "$GRADLE_VALIDATION_CODE" ]; then
      short "Gradle validation exit code=$GRADLE_VALIDATION_CODE"
    fi
    short "This is not a runtime/code failure."
    append_short_from_full "Directory '.+' does not contain a Gradle build|FAILURE: Build failed with an exception|^\* What went wrong:|[A-Za-z0-9_.]+Exception" 80 || true
  else
    RESULT="NO_BUILD_SYSTEM_YET"
    short "RESULT=$RESULT"
    short "Reason: no Gradle project skeleton found at repo root."
    short "This is OK for Stage 0 docs/foundation."
  fi
else
  log "command=$BUILD_CMD"
  if [ -z "$RESOLVED_ANDROID_SDK" ]; then
    short "ANDROID_SDK_ENV=missing"
    short "Reason: ANDROID_HOME/ANDROID_SDK_ROOT are empty or invalid, and no SDK was found at $HOME/android-sdk or /data/data/com.termux/files/home/android-sdk."
    short "Gradle Android build may be impossible until Android SDK path is configured."
  else
    short "ANDROID_SDK_ENV=found"
    short "Resolved Android SDK=$RESOLVED_ANDROID_SDK"
  fi
  if [ -n "$AAPT2_PATH" ]; then
    short "aapt2=$AAPT2_PATH"
  else
    short "aapt2=not found"
  fi

  P63_TEST_CODE=0
  if [ -f "tools/p63_environment_tests.py" ]; then
    log "p63_test_command=python3 tools/p63_environment_tests.py"
    set +e
    python3 tools/p63_environment_tests.py 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL"
    P63_TEST_CODE=${PIPESTATUS[0]}
    set -e
    if [ "$P63_TEST_CODE" -eq 0 ]; then
      short "P63_ENVIRONMENT_TESTS=PASS"
    else
      short "P63_ENVIRONMENT_TESTS=FAILED"
    fi
  fi

  NATIVE_CODE=0
  if [ "$P63_TEST_CODE" -eq 0 ] && [ -x "tools/build_native_engine.sh" ]; then
    log "native_command=bash tools/build_native_engine.sh"
    set +e
    bash tools/build_native_engine.sh 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL"
    NATIVE_CODE=${PIPESTATUS[0]}
    set -e
    if [ "$NATIVE_CODE" -eq 0 ]; then
      short "NATIVE_ENGINE_BUILD=OK"
    else
      short "NATIVE_ENGINE_BUILD=FAILED"
      short "Native build log=$OUTROOT/reports/latest/P04_native_build.log"
    fi
  elif [ "$P63_TEST_CODE" -eq 0 ]; then
    short "NATIVE_ENGINE_BUILD=not_available"
  else
    NATIVE_CODE="$P63_TEST_CODE"
    short "NATIVE_ENGINE_BUILD=skipped_p63_tests_failed"
  fi

  set +e
  if [ "$NATIVE_CODE" -eq 0 ]; then
    bash -c "$BUILD_CMD" 2>&1 | tee -a "$FULL_LOG" "$LOCAL_FULL"
    CODE=${PIPESTATUS[0]}
  else
    CODE="$NATIVE_CODE"
  fi
  set -e

  if [ "$CODE" -eq 0 ]; then
    RESULT="BUILD_SUCCESS"
    short "RESULT=$RESULT"
    short "Command: $BUILD_CMD"

    ENGINE_FOUND_APK="$(find apps/engine/build/outputs/apk -type f -name "*.apk" 2>/dev/null | sort | head -1 || true)"
    COMPANION_FOUND_APK="$(find apps/solum-companion/build/outputs/apk -type f -name "*.apk" 2>/dev/null | sort | head -1 || true)"
    copy_named_apk "ENGINE_APK" "$ENGINE_FOUND_APK" "SOLUM_ENGINE_LATEST.apk" "SOLUM-engine-debug.apk"
    copy_named_apk "COMPANION_APK" "$COMPANION_FOUND_APK" "SOLUM_COMPANION_LATEST.apk" "SOLUM-companion-debug.apk"
    short "SOLUM_LATEST ambiguity fixed: use ENGINE_APK or COMPANION_APK, not a shared latest APK."
  else
    RESULT="BUILD_FAILED"
    short "RESULT=$RESULT"
    short "Command: $BUILD_CMD"
    short "Exit code: $CODE"
    if [ -z "$RESOLVED_ANDROID_SDK" ]; then
      short "Build preflight: Android SDK env/path is missing. This can make Gradle Android build impossible."
    fi
    if ! append_short_from_full "FAILURE: Build failed with an exception|Execution failed for task|^\* What went wrong:|^[[:space:]]*Caused by:|[A-Za-z0-9_.]+Exception" 80; then
      short "First meaningful errors:"
      grep -nEi "error:|exception|failed|unresolved reference|undefined reference|fatal|Execution failed" "$FULL_LOG" | head -40 | tee -a "$SHORT_LOG" "$LOCAL_SHORT" || true
    fi
  fi
fi

log ""
log "== Diff summary =="
git status --short 2>/dev/null | tee -a "$FULL_LOG" "$LOCAL_FULL" || true
git diff --stat 2>/dev/null | tee -a "$FULL_LOG" "$LOCAL_FULL" || true

zip -qr "$ZIP_OUT" "$REPORT_LATEST" _work/agent_reports/latest 2>/dev/null || true

short "Full log=$FULL_LOG"
short "Short log=$SHORT_LOG"
short "Local short log=$LOCAL_SHORT"
short "Diagnostics ZIP=$ZIP_OUT"

echo
echo "==== SOLUM AGENT RUNNER SUMMARY ===="
cat "$SHORT_LOG" 2>/dev/null || true
echo "===================================="
