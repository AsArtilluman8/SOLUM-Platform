#!/usr/bin/env bash
set -euo pipefail

PKG="com.solum.engine"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
OUT_DIR="$SOLUM_ROOT/diagnostics/latest"
ARCHIVE_DIR="$SOLUM_ROOT/diagnostics/archive/$(date +%Y%m%d_%H%M%S)_P04_runtime"
mkdir -p "$OUT_DIR" "$ARCHIVE_DIR"

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

copy_one() {
  local name="$1"
  if run-as "$PKG" sh -c "test -f files/solum_diagnostics/$name" >/dev/null 2>&1; then
    run-as "$PKG" cat "files/solum_diagnostics/$name" > "$TMP_DIR/$name"
    cp "$TMP_DIR/$name" "$OUT_DIR/$name"
    cp "$TMP_DIR/$name" "$ARCHIVE_DIR/$name"
    echo "exported: $name"
  fi
}

copy_glob_runtime_crashes() {
  local list
  list="$(run-as "$PKG" sh -c 'ls files/solum_diagnostics/runtime_crash_*.txt 2>/dev/null' 2>/dev/null || true)"
  if [ -z "$list" ]; then
    return
  fi
  echo "$list" | while IFS= read -r path; do
    [ -z "$path" ] && continue
    local name
    name="$(basename "$path")"
    run-as "$PKG" cat "$path" > "$TMP_DIR/$name"
    cp "$TMP_DIR/$name" "$OUT_DIR/$name"
    cp "$TMP_DIR/$name" "$ARCHIVE_DIR/$name"
    echo "exported: $name"
  done
}

if ! command -v run-as >/dev/null 2>&1; then
  echo "run-as not found on this device. Runtime reports stay in app-private storage."
  exit 1
fi

if ! run-as "$PKG" sh -c 'pwd >/dev/null' >/dev/null 2>&1; then
  echo "run-as failed for $PKG. Install debug APK built by this repo and launch it once."
  exit 1
fi

copy_one runtime_java_state.json
copy_one runtime_vulkan_caps.json
copy_glob_runtime_crashes

if [ -z "$(find "$ARCHIVE_DIR" -type f 2>/dev/null | head -n 1)" ]; then
  echo "No runtime report files found. Launch the APK first, then run this script again."
  exit 2
fi

zip -qr "$OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip" "$ARCHIVE_DIR"
cp "$OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip" "$ARCHIVE_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip"

echo "Latest runtime reports: $OUT_DIR"
echo "Archive: $ARCHIVE_DIR"
echo "ZIP: $OUT_DIR/SOLUM_LATEST_RUNTIME_REPORTS.zip"
