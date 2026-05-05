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

is_android_termux() {
  [ -d "/data/data/com.termux" ] || [ "$(uname -o 2>/dev/null || true)" = "Android" ]
}

is_executable_on_device() {
  local bin="$1"
  [ -x "$bin" ] || return 1
  "$bin" --version >/dev/null 2>&1
}

find_android_clang() {
  # On-device Termux cannot execute Android SDK NDK linux-x86_64 clang wrappers.
  # Prefer Termux clang++ first. It targets Android/aarch64 on the phone.
  if is_android_termux && command -v clang++ >/dev/null 2>&1; then
    command -v clang++
    return
  fi

  if command -v aarch64-linux-android26-clang++ >/dev/null 2>&1; then
    local cxx
    cxx="$(command -v aarch64-linux-android26-clang++)"
    if is_executable_on_device "$cxx"; then
      echo "$cxx"
      return
    fi
  fi

  # Desktop/CI fallback: SDK NDK host toolchain can be used only when executable.
  for ndk_root in "${ANDROID_NDK_HOME:-}" "${ANDROID_NDK_ROOT:-}"; do
    if [ -n "$ndk_root" ]; then
      local ndk_cxx="$ndk_root/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
      if is_executable_on_device "$ndk_cxx"; then
        echo "$ndk_cxx"
        return
      fi
    fi
  done

  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
  local found
  found="$(find "$sdk/ndk" -path '*/toolchains/llvm/prebuilt/*/bin/aarch64-linux-android26-clang++' 2>/dev/null | sort | tail -n 1 || true)"
  if [ -n "$found" ] && is_executable_on_device "$found"; then
    echo "$found"
    return
  fi

  if command -v clang++ >/dev/null 2>&1; then
    command -v clang++
    return
  fi

  return 1
}

CXX="$(find_android_clang)" || {
  echo "No executable Android/Termux clang++ found" | tee "$LOG"
  exit 1
}

{
  echo "SOLUM P04 native build"
  echo "ROOT=$ROOT"
  echo "SRC=$SRC"
  echo "OUT=$OUT"
  echo "CXX=$CXX"
  echo "UNAME=$(uname -a 2>/dev/null || true)"
  "$CXX" --version || true
  echo
  set -x
  "$CXX" -std=c++17 -O2 -fPIC -shared "$SRC" -o "$OUT" -landroid -lvulkan -llog
} > "$LOG" 2>&1

echo "Native build OK: $OUT"
echo "Log: $LOG"
