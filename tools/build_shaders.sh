#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SHADER_DIR="$ROOT/engine-core/solum-vulkan-core/shaders"
OUT_DIR="$ROOT/engine-core/solum-vulkan-core/src/generated"
mkdir -p "$OUT_DIR"

find_shader_compiler() {
  if command -v glslc >/dev/null 2>&1; then
    echo "glslc"
    return 0
  fi
  if command -v glslangValidator >/dev/null 2>&1; then
    echo "glslangValidator"
    return 0
  fi
  return 1
}

emit_header() {
  local bin="$1"
  local symbol="$2"
  local out="$3"
  python3 - "$bin" "$symbol" "$out" <<'PY'
import pathlib
import struct
import sys

bin_path = pathlib.Path(sys.argv[1])
symbol = sys.argv[2]
out_path = pathlib.Path(sys.argv[3])
data = bin_path.read_bytes()
if len(data) % 4 != 0:
    raise SystemExit(f"SPIR-V byte size is not 4-byte aligned: {bin_path}")
words = struct.unpack("<" + "I" * (len(data) // 4), data)
with out_path.open("w", encoding="utf-8") as f:
    f.write("#pragma once\n")
    f.write("#include <cstddef>\n")
    f.write("#include <cstdint>\n\n")
    f.write(f"static const uint32_t {symbol}[] = {{\n")
    for i in range(0, len(words), 6):
        chunk = words[i:i+6]
        f.write("    " + ", ".join(f"0x{w:08x}" for w in chunk))
        if i + 6 < len(words):
            f.write(",")
        f.write("\n")
    f.write("};\n")
    f.write(f"static const size_t {symbol}_WORD_COUNT = sizeof({symbol}) / sizeof({symbol}[0]);\n")
PY
}

compile_one() {
  local compiler="$1"
  local stage="$2"
  local input="$3"
  local temp_spv="$4"
  local symbol="$5"
  local header="$6"

  if [ "$compiler" = "glslc" ]; then
    glslc -fshader-stage="$stage" "$input" -o "$temp_spv"
  else
    local glslang_stage="$stage"
    if [ "$stage" = "vert" ]; then glslang_stage="vert"; fi
    if [ "$stage" = "frag" ]; then glslang_stage="frag"; fi
    glslangValidator -V -S "$glslang_stage" "$input" -o "$temp_spv" >/dev/null
  fi
  emit_header "$temp_spv" "$symbol" "$header"
}

COMPILER="$(find_shader_compiler)" || {
  echo "SOLUM SHADER BUILD: FAILED"
  echo "Reason: no GLSL → SPIR-V compiler found. Install glslc or glslangValidator in Termux."
  echo "Expected one of: glslc, glslangValidator"
  exit 1
}

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

compile_one "$COMPILER" vert "$SHADER_DIR/triangle.vert" "$TMP_DIR/triangle.vert.spv" SOL_TRIANGLE_VERT_SPV "$OUT_DIR/solum_triangle_vert_spv.h"
compile_one "$COMPILER" frag "$SHADER_DIR/triangle.frag.glsl" "$TMP_DIR/triangle.frag.spv" SOL_TRIANGLE_FRAG_SPV "$OUT_DIR/solum_triangle_frag_spv.h"

echo "SOLUM SHADER BUILD: OK"
echo "Compiler: $COMPILER"
echo "Generated: $OUT_DIR/solum_triangle_vert_spv.h"
echo "Generated: $OUT_DIR/solum_triangle_frag_spv.h"
