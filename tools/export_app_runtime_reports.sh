#!/usr/bin/env bash
set -euo pipefail

PKG="com.solum.engine"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
OUT_DIR="$SOLUM_ROOT/diagnostics/latest"
REPORT_DIR="$SOLUM_ROOT/reports/latest"
ARCHIVE_DIR="$SOLUM_ROOT/diagnostics/archive/$(date +%Y%m%d_%H%M%S)_runtime"
ZIP_LATEST="$OUT_DIR/SOLUM_RUNTIME_DIAGNOSTICS.zip"
EXT_DIR="/storage/emulated/0/Android/data/$PKG/files/solum_diagnostics"
PUBLIC_ROOT_DIR="$SOLUM_ROOT/diagnostics/latest"
PUBLIC_DOWNLOAD_DIR="/storage/emulated/0/Download/SOLUMCreative/diagnostics/latest"
PRIVATE_DIR="/data/user/0/$PKG/files/solum_diagnostics"
mkdir -p "$OUT_DIR" "$ARCHIVE_DIR"

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

copy_if_exists() {
  local src="$1"
  local dst_name="$2"
  if [ -f "$src" ]; then
    cp "$src" "$TMP_DIR/$dst_name"
    return 0
  fi
  return 1
}

copy_runtime_from_public_dirs() {
  local copied=0
  for srcdir in "$PUBLIC_ROOT_DIR" "$PUBLIC_DOWNLOAD_DIR"; do
    [ -d "$srcdir" ] || continue
    for name in runtime_java_state.json runtime_vulkan_caps.json runtime_render_state.json runtime_model_state.json runtime_material_state.json imported_models_index.json runtime_model_import_state.json diagnostics_export_request.json runtime_latest_status.txt latest_status.txt; do
      if copy_if_exists "$srcdir/$name" "$name"; then copied=1; fi
    done
    for f in "$srcdir"/runtime_crash_*.txt; do
      [ -f "$f" ] || continue
      cp "$f" "$TMP_DIR/$(basename "$f")"
      copied=1
    done
  done
  [ "$copied" -eq 1 ]
}

copy_runtime_from_external_dir() {
  [ -d "$EXT_DIR" ] || return 1
  local copied=0
  for name in runtime_java_state.json runtime_vulkan_caps.json runtime_render_state.json runtime_model_state.json runtime_material_state.json diagnostics_export_request.json runtime_latest_status.txt latest_status.txt; do
    if copy_if_exists "$EXT_DIR/$name" "$name"; then copied=1; fi
  done
  for f in "$EXT_DIR"/runtime_crash_*.txt; do
    [ -f "$f" ] || continue
    cp "$f" "$TMP_DIR/$(basename "$f")"
    copied=1
  done
  [ "$copied" -eq 1 ]
}

find_run_as() {
  if command -v run-as >/dev/null 2>&1; then command -v run-as; return 0; fi
  for p in /system/bin/run-as /apex/com.android.runtime/bin/run-as; do
    if [ -x "$p" ]; then echo "$p"; return 0; fi
  done
  return 1
}

copy_runtime_from_run_as() {
  local run_as
  run_as="$(find_run_as || true)"
  [ -n "$run_as" ] || return 1
  "$run_as" "$PKG" sh -c 'pwd >/dev/null' >/dev/null 2>&1 || return 1

  local copied=0
  for name in runtime_java_state.json runtime_vulkan_caps.json runtime_render_state.json runtime_model_state.json runtime_material_state.json diagnostics_export_request.json runtime_latest_status.txt latest_status.txt; do
    if "$run_as" "$PKG" sh -c "test -f files/solum_diagnostics/$name" >/dev/null 2>&1; then
      "$run_as" "$PKG" cat "files/solum_diagnostics/$name" > "$TMP_DIR/$name"
      copied=1
    fi
  done

  local list
  list="$($run_as "$PKG" sh -c 'ls files/solum_diagnostics/runtime_crash_*.txt 2>/dev/null' 2>/dev/null || true)"
  if [ -n "$list" ]; then
    echo "$list" | while IFS= read -r path; do
      [ -z "$path" ] && continue
      "$run_as" "$PKG" cat "$path" > "$TMP_DIR/$(basename "$path")"
    done
    copied=1
  fi
  [ "$copied" -eq 1 ]
}

write_termux_state() {
  {
    echo "SOLUM Runtime Diagnostics Bundle"
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "package=$PKG"
    echo "repo=$ROOT"
    echo "solumRoot=$SOLUM_ROOT"
    echo "expectedRuntimeExternalPath=$EXT_DIR"
    echo "expectedRuntimePrivatePath=$PRIVATE_DIR"
    echo
    echo "Git:"
    cd "$ROOT"
    git branch --show-current 2>/dev/null || true
    git log -1 --oneline 2>/dev/null || true
    git status --short 2>/dev/null || true
  } > "$TMP_DIR/termux_state.txt"

  if [ -f "$REPORT_DIR/P04_native_build.log" ]; then cp "$REPORT_DIR/P04_native_build.log" "$TMP_DIR/P04_native_build.log"; fi
  if [ -f "$REPORT_DIR/P04_gradle_build.log" ]; then cp "$REPORT_DIR/P04_gradle_build.log" "$TMP_DIR/P04_gradle_build.log"; fi

  {
    echo "What to verify visually if runtime files are blocked by Android scoped storage:"
    echo "- Compact overlay: SOLUM Engine / Vulkan: Mali-G57 MC2 / Status: Vertex Buffer OK"
    echo "- Orange triangle visible"
    echo "- Long debug list hidden"
    echo
    echo "If runtime_java_state.json or runtime_vulkan_caps.json exists in this ZIP, prefer those over screenshot evidence."
  } > "$TMP_DIR/runtime_verification_guide.txt"
}

runtime_status="missing"
if copy_runtime_from_public_dirs; then
  runtime_status="copied_from_public_diagnostics"
elif copy_runtime_from_external_dir; then
  runtime_status="copied_from_external_app_storage"
elif copy_runtime_from_run_as; then
  runtime_status="copied_from_run_as"
else
  runtime_status="not_accessible_from_termux"
fi

write_termux_state

echo "runtimeExportStatus=$runtime_status" > "$TMP_DIR/runtime_export_status.txt"

rm -f "$ZIP_LATEST"
(cd "$TMP_DIR" && zip -qr "$ZIP_LATEST" .)
cp "$ZIP_LATEST" "$ARCHIVE_DIR/SOLUM_RUNTIME_DIAGNOSTICS.zip"
cp -r "$TMP_DIR"/* "$ARCHIVE_DIR"/ 2>/dev/null || true

cat <<EOF
============================================================
SOLUM RUNTIME DIAGNOSTICS: OK
ZIP latest:  $ZIP_LATEST
ZIP archive: $ARCHIVE_DIR/SOLUM_RUNTIME_DIAGNOSTICS.zip
Runtime export status: $runtime_status

SEND THIS ONE FILE:
$ZIP_LATEST

Inside ZIP:
- termux_state.txt
- runtime_export_status.txt
- runtime_verification_guide.txt
- P04_native_build.log if available
- P04_gradle_build.log if available
- runtime_java_state.json if Android allowed export
- runtime_vulkan_caps.json if Android allowed export
- runtime_render_state.json if available
- runtime_model_state.json if available
- runtime_material_state.json diagnostics_export_request.json if available
- runtime_crash_*.txt if present and accessible
============================================================
EOF
