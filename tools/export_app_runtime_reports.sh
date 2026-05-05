#!/usr/bin/env bash
set -euo pipefail

PKG="com.solum.engine"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
OUT_DIR="$SOLUM_ROOT/diagnostics/latest"
ARCHIVE_DIR="$SOLUM_ROOT/diagnostics/archive/$(date +%Y%m%d_%H%M%S)_P04_runtime"
EXT_DIR="/storage/emulated/0/Android/data/$PKG/files/solum_diagnostics"
mkdir -p "$OUT_DIR" "$ARCHIVE_DIR"

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

copy_from_external_dir() {
  if [ ! -d "$EXT_DIR" ]; then
    return 1
  fi

  local copied=0
  for name in runtime_java_state.json runtime_vulkan_caps.json; do
    if [ -f "$EXT_DIR/$name" ]; then
      cp "$EXT_DIR/$name" "$OUT_DIR/$name"
      cp "$EXT_DIR/$name" "$ARCHIVE_DIR/$name"
      echo "exported from external app storage: $name"
      copied=1
    fi
  done

  for f in "$EXT_DIR"/runtime_crash_*.txt; do
    [ -f "$f" ] || continue
    local name
    name="$(basename "$f")"
    cp "$f" "$OUT_DIR/$name"
    cp "$f" "$ARCHIVE_DIR/$name"
    echo "exported from external app storage: $name"
    copied=1
  done

  [ "$copied" -eq 1 ]
}

find_run_as() {
  if command -v run-as >/dev/null 2>&1; then
    command -v run-as
    return 0
  fi
  for p in /system/bin/run-as /apex/com.android.runtime/bin/run-as; do
    if [ -x "$p" ]; then
      echo "$p"
      return 0
    fi
  done
  return 1
}

copy_from_run_as() {
  local run_as
  run_as="$(find_run_as || true)"
  if [ -z "$run_as" ]; then
    echo "run-as not usable. Checked PATH, /system/bin/run-as, /apex/com.android.runtime/bin/run-as"
    return 1
  fi

  echo "Using run-as fallback: $run_as"

  if ! "$run_as" "$PKG" sh -c 'pwd >/dev/null' >/dev/null 2>&1; then
    echo "run-as failed for $PKG."
    return 1
  fi

  local copied=0
  for name in runtime_java_state.json runtime_vulkan_caps.json; do
    if "$run_as" "$PKG" sh -c "test -f files/solum_diagnostics/$name" >/dev/null 2>&1; then
      "$run_as" "$PKG" cat "files/solum_diagnostics/$name" > "$TMP_DIR/$name"
      cp "$TMP_DIR/$name" "$OUT_DIR/$name"
      cp "$TMP_DIR/$name" "$ARCHIVE_DIR/$name"
      echo "exported from app-private storage: $name"
      copied=1
    fi
  done

  local list
  list="$($run_as "$PKG" sh -c 'ls files/solum_diagnostics/runtime_crash_*.txt 2>/dev/null' 2>/dev/null || true)"
  if [ -n "$list" ]; then
    echo "$list" | while IFS= read -r path; do
      [ -z "$path" ] && continue
      local name
      name="$(basename "$path")"
      "$run_as" "$PKG" cat "$path" > "$TMP_DIR/$name"
      cp "$TMP_DIR/$name" "$OUT_DIR/$name"
      cp "$TMP_DIR/$name" "$ARCHIVE_DIR/$name"
      echo "exported from app-private storage: $name"
    done
    copied=1
  fi

  [ "$copied" -eq 1 ]
}

if copy_from_external_dir; then
  :
elif copy_from_run_as; then
  :
else
  echo "No runtime report files found."
  echo "Launch the APK first, then run this script again."
  echo "Expected external path: $EXT_DIR"
  echo "Expected private path: /data/user/0/$PKG/files/solum_diagnostics"
  exit 2
fi

if [ -z "$(find "$ARCHIVE_DIR" -type f 2>/dev/null | head -n 1)" ]; then
  echo "No runtime report files were copied."
  exit 2
fi

zip -qr "$OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip" "$ARCHIVE_DIR"
cp "$OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip" "$ARCHIVE_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip"

echo "Latest runtime reports: $OUT_DIR"
echo "Archive: $ARCHIVE_DIR"
echo "ZIP: $OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip"
