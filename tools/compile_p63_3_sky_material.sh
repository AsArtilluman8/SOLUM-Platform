#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="$ROOT/apps/engine/src/main/materials/p63_3_analytic_sky.mat"
OUTPUT="$ROOT/apps/engine/src/main/assets/env/p63/analytic_sky_mobile.filamat"
WORK="$ROOT/_work/p63_3_material/analytic_sky_mobile_all.filamat"

MATC_BIN="${MATC:-}"
if [ -z "$MATC_BIN" ] && command -v matc >/dev/null 2>&1; then
  MATC_BIN="$(command -v matc)"
fi
if [ -z "$MATC_BIN" ] && [ -x "$ROOT/_work/p63_3_matc_arm/filament/bin/matc" ]; then
  MATC_BIN="$ROOT/_work/p63_3_matc_arm/filament/bin/matc"
fi

if [ -z "$MATC_BIN" ] || [ ! -x "$MATC_BIN" ]; then
  if [ -s "$OUTPUT" ]; then
    echo "P63_3_MATC=CHECKED_IN_PACKAGE tool_unavailable"
    exit 0
  fi
  echo "P63_3_MATC=FAILED matching Filament 1.71.x matc unavailable" >&2
  exit 1
fi

VERSION="$($MATC_BIN --version 2>/dev/null || true)"
if [ "$VERSION" != "71" ]; then
  echo "P63_3_MATC=FAILED expected_material_version_71 actual=$VERSION" >&2
  exit 1
fi

mkdir -p "$(dirname "$WORK")" "$(dirname "$OUTPUT")"
"$MATC_BIN" -p mobile -a all -o "$WORK" "$SOURCE"
cp "$WORK" "$OUTPUT"
echo "P63_3_MATC=PASS version=$VERSION profile=mobile api=all bytes=$(wc -c < "$OUTPUT")"
sha256sum "$OUTPUT"
