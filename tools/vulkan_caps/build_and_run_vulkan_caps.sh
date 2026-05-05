#!/usr/bin/env bash
set -u
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="${SOLUM_VULKAN_CAPS_OUT:-$SCRIPT_DIR/vulkan_caps.json}"
BIN="$SCRIPT_DIR/vulkan_caps"
SRC="$SCRIPT_DIR/vulkan_caps.c"

CC_BIN="${CC:-clang}"
CFLAGS="${CFLAGS:--O2 -Wall -Wextra}"
LDFLAGS="${LDFLAGS:--lvulkan}"

echo "SOLUM Vulkan Caps build"
echo "src=$SRC"
echo "bin=$BIN"
echo "out=$OUT"
echo "cc=$CC_BIN"

if ! command -v "$CC_BIN" >/dev/null 2>&1; then
  echo "Compiler not found: $CC_BIN" >&2
  exit 10
fi

if [ ! -f "$SRC" ]; then
  echo "Source not found: $SRC" >&2
  exit 11
fi

"$CC_BIN" $CFLAGS "$SRC" -o "$BIN" $LDFLAGS
"$BIN" "$OUT"
