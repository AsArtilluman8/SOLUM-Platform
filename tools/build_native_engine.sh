#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/engine-core/solum-vulkan-core/src/solum_engine.cpp"
OUT_DIR="$ROOT/apps/engine/src/main/jniLibs/arm64-v8a"
OUT="$OUT_DIR/libsolum_engine.so"
LOG_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$LOG_ROOT")" ]; then
  LOG_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi
LOG_DIR="$LOG_ROOT/reports/latest"
LOG="$LOG_DIR/P04_native_build.log"

mkdir -p "$OUT_DIR" "$LOG_DIR"

find_ndk_clang() {
  if [ -n "${ANDROID_NDK_HOME:-}" ] && [ -x "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++" ]; then
    echo "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
    return
  fi
  if [ -n "${ANDROID_NDK_ROOT:-}" ] && [ -x "$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++" ]; then
    echo "$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
    return
  fi
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
  local found
  found="$(find "$sdk/ndk" -path '*/toolchains/llvm/prebuilt/*/bin/aarch64-linux-android26-clang++' 2>/dev/null | sort | tail -n 1 || true)"
  if [ -n "$found" ]; then
    echo "$found"
    return
  fi
  if command -v aarch64-linux-android26-clang++ >/dev/null 2>&1; then
    command -v aarch64-linux-android26-clang++
    return
  fi
  if command -v clang++ >/dev/null 2>&1; then
    command -v clang++
    return
  fi
  return 1
}

CXX="$(find_ndk_clang)" || {
  echo "No Android/Termux clang++ found" | tee "$LOG"
  exit 1
}

{
  echo "SOLUM P04 native build"
  echo "ROOT=$ROOT"
  echo "SRC=$SRC"
  echo "OUT=$OUT"
  echo "CXX=$CXX"
  "$CXX" --version || true
  echo
  set -x
  "$CXX" -std=c++17 -O2 -fPIC -shared "$SRC" -o "$OUT" -landroid -lvulkan -llog
} > "$LOG" 2>&1

echo "Native build OK: $OUT"
echo "Log: $LOG"
