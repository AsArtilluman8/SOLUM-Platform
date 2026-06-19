#!/usr/bin/env bash
set -u

SRC_DIR="apps/engine/src/main/filament"
OUT_DIR="apps/engine/src/main/assets/materials"

is_executable_file() {
  [ -n "${1:-}" ] && [ -f "$1" ] && [ -x "$1" ]
}

find_matc() {
  if is_executable_file "${MATC_PATH:-}"; then
    printf '%s\n' "$MATC_PATH"
    return 0
  fi

  if command -v matc >/dev/null 2>&1; then
    command -v matc
    return 0
  fi

  local candidates=(
    "tools/matc"
    "tools/filament/bin/matc"
    "tools/filament/matc"
    "third_party/filament/bin/matc"
    "third_party/filament/matc"
  )
  local item
  for item in "${candidates[@]}"; do
    if is_executable_file "$item"; then
      printf '%s\n' "$item"
      return 0
    fi
  done

  local root
  for root in "$HOME/.gradle" "$HOME/.m2" "$PWD"; do
    [ -d "$root" ] || continue
    local found
    found="$(find "$root" -type f -name matc -perm -111 2>/dev/null | head -n 1)"
    if is_executable_file "$found"; then
      printf '%s\n' "$found"
      return 0
    fi
  done

  return 1
}

if ! RESOLVED_MATC="$(find_matc)"; then
  echo "MATC_FOUND=false"
  echo "MATC_PATH="
  echo "COMPILED_COUNT=0"
  echo "FAILED_COUNT=0"
  echo "MATC_NOT_FOUND"
  echo "Requirement: provide Filament matc on PATH, set MATC_PATH, or let GitHub Actions download the matching Filament release."
  echo "No .filamat files were generated."
  exit 2
fi

echo "MATC_FOUND=true"
echo "MATC_PATH=$RESOLVED_MATC"

if [ ! -d "$SRC_DIR" ]; then
  echo "COMPILED_COUNT=0"
  echo "FAILED_COUNT=0"
  echo "No material source directory: $SRC_DIR"
  exit 0
fi

mkdir -p "$OUT_DIR"

compiled=0
failed=0
found_sources=0

while IFS= read -r src; do
  [ -n "$src" ] || continue
  found_sources=$((found_sources + 1))
  name="$(basename "$src" .mat)"
  out="$OUT_DIR/$name.filamat"
  echo "compile $src -> $out"
  if "$RESOLVED_MATC" -o "$out" "$src"; then
    compiled=$((compiled + 1))
  else
    failed=$((failed + 1))
    rm -f "$out"
  fi
done <<EOF_MAT_FILES
$(find "$SRC_DIR" -type f -name '*.mat' | sort)
EOF_MAT_FILES

if [ "$found_sources" -eq 0 ]; then
  echo "No .mat files found under $SRC_DIR"
fi

echo "COMPILED_COUNT=$compiled"
echo "FAILED_COUNT=$failed"

if [ "$failed" -ne 0 ]; then
  exit 1
fi

exit 0
