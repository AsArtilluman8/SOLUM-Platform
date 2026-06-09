#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

OUT="_build/agent"
mkdir -p "$OUT"
FULL="$OUT/build_full.log"
SHORT="$OUT/build_short.log"
SUMMARY="$OUT/build_summary.json"
EXPECTED_APK="apps/engine/build/outputs/apk/debug/engine-debug.apk"
BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"
COMMIT_BEFORE="$(git rev-parse HEAD 2>/dev/null || echo unknown)"

: > "$FULL"
: > "$SHORT"

log_step() {
  echo "$*" | tee -a "$FULL" "$SHORT"
}

run_step() {
  label="$1"
  shift
  log_step "STEP=$label"
  echo "COMMAND=$*" >> "$FULL"
  "$@" >> "$FULL" 2>&1
}

BUILD_SUCCESS=false
FIRST_ERROR=""

run_step "native_engine" bash tools/build_native_engine.sh
NATIVE_CODE=$?
if [ "$NATIVE_CODE" -eq 0 ]; then
  run_step "gradle_clean_assembleDebug" gradle --no-daemon -p "$PWD" clean assembleDebug
  GRADLE_CODE=$?
else
  GRADLE_CODE=99
fi

if [ "$NATIVE_CODE" -eq 0 ] && [ "$GRADLE_CODE" -eq 0 ]; then
  BUILD_SUCCESS=true
  log_step "BUILD_SUCCESS=true"
else
  BUILD_SUCCESS=false
  log_step "BUILD_SUCCESS=false"
  FIRST_ERROR="$(grep -nE "error:|FAILED|Exception|What went wrong|No such file|not found" "$FULL" | head -20 | sed 's/"/'\''/g' || true)"
  {
    echo "FIRST_ERROR_EXCERPT:"
    printf '%s\n' "$FIRST_ERROR"
  } >> "$SHORT"
fi

APK_EXISTS=false
if [ -f "$EXPECTED_APK" ]; then
  APK_EXISTS=true
fi
log_step "EXPECTED_APK=$EXPECTED_APK"
log_step "APK_EXISTS=$APK_EXISTS"

if command -v python3 >/dev/null 2>&1; then
  python3 - "$SUMMARY" "$BUILD_SUCCESS" "$BRANCH" "$COMMIT_BEFORE" "$EXPECTED_APK" "$APK_EXISTS" "$FIRST_ERROR" <<'PY'
import json, sys
summary, success, branch, commit, apk, apk_exists, first_error = sys.argv[1:8]
data = {
    "build_success": success == "true",
    "branch": branch,
    "commit_sha_before": commit,
    "command_steps": [
        ["bash", "tools/build_native_engine.sh"],
        ["gradle", "--no-daemon", "-p", "$PWD", "clean", "assembleDebug"]
    ],
    "expected_apk": apk,
    "apk_exists": apk_exists == "true",
    "first_error_excerpt": first_error,
    "agent_tools_used": True
}
with open(summary, "w", encoding="utf-8") as fh:
    json.dump(data, fh, indent=2)
    fh.write("\n")
PY
else
  cat > "$SUMMARY" <<EOF
{
  "build_success": $BUILD_SUCCESS,
  "branch": "$BRANCH",
  "commit_sha_before": "$COMMIT_BEFORE",
  "command_steps": [["bash","tools/build_native_engine.sh"],["gradle","--no-daemon","-p","$PWD","clean","assembleDebug"]],
  "expected_apk": "$EXPECTED_APK",
  "apk_exists": $APK_EXISTS,
  "first_error_excerpt": "",
  "agent_tools_used": true
}
EOF
fi

echo "build_full_log=$FULL"
echo "build_short_log=$SHORT"
echo "build_summary=$SUMMARY"

if [ "$BUILD_SUCCESS" = true ]; then
  exit 0
fi
exit 1
