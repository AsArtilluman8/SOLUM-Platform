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

find_android_stub_lib_dir() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
  local api
  for api in 26 25 24 23 22 21; do
    local found
    found="$(find "$sdk/ndk" -path "*/toolchains/llvm/prebuilt/*/sysroot/usr/lib/aarch64-linux-android/$api/libvulkan.so" 2>/dev/null | sort | tail -n 1 || true)"
    if [ -n "$found" ]; then
      dirname "$found"
      return
    fi
  done
  return 1
}

copy_libcxx_shared_if_needed() {
  local candidate=""
  for p in \
    "/data/data/com.termux/files/usr/lib/libc++_shared.so" \
    "/data/data/com.termux/files/usr/lib/aarch64-linux-android/libc++_shared.so" \
    "${ANDROID_NDK_HOME:-}/sources/cxx-stl/llvm-libc++/libs/arm64-v8a/libc++_shared.so" \
    "${ANDROID_NDK_ROOT:-}/sources/cxx-stl/llvm-libc++/libs/arm64-v8a/libc++_shared.so"; do
    if [ -f "$p" ]; then
      candidate="$p"
      break
    fi
  done

  if [ -z "$candidate" ]; then
    candidate="$(find "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}/ndk" -path '*/sources/cxx-stl/llvm-libc++/libs/arm64-v8a/libc++_shared.so' 2>/dev/null | sort | tail -n 1 || true)"
  fi

  if [ -n "$candidate" ] && [ -f "$candidate" ]; then
    cp "$candidate" "$OUT_DIR/libc++_shared.so"
    echo "Packaged libc++_shared.so from: $candidate"
  else
    echo "WARNING: libc++_shared.so not found. APK may crash on native load if libsolum_engine.so needs it."
  fi
}

CXX="$(find_android_clang)" || {
  echo "No executable Android/Termux clang++ found" | tee "$LOG"
  exit 1
}
ANDROID_STUB_LIB_DIR="$(find_android_stub_lib_dir || true)"
if [ -z "$ANDROID_STUB_LIB_DIR" ]; then
  echo "No Android NDK Vulkan stub lib dir found. Refusing to link against Termux libvulkan.so.1." | tee "$LOG"
  exit 1
fi

{
  echo "SOLUM P04 native build"
  echo "ROOT=$ROOT"
  echo "SRC=$SRC"
  echo "OUT=$OUT"
  echo "CXX=$CXX"
  echo "ANDROID_STUB_LIB_DIR=$ANDROID_STUB_LIB_DIR"
  echo "UNAME=$(uname -a 2>/dev/null || true)"
  "$CXX" --version || true
  echo
  set -x
  if ! "$CXX" -std=c++17 -O2 -fPIC -shared "$SRC" -o "$OUT" -L"$ANDROID_STUB_LIB_DIR" -landroid -lvulkan -llog -static-libstdc++; then
    echo "static-libstdc++ build failed; retrying without it"
    "$CXX" -std=c++17 -O2 -fPIC -shared "$SRC" -o "$OUT" -L"$ANDROID_STUB_LIB_DIR" -landroid -lvulkan -llog
  fi
  set +x
  echo
  copy_libcxx_shared_if_needed
  echo
  echo "Runtime dynamic dependencies:"
  if command -v readelf >/dev/null 2>&1; then
    readelf -d "$OUT" | grep NEEDED || true
  elif command -v llvm-readelf >/dev/null 2>&1; then
    llvm-readelf -d "$OUT" | grep NEEDED || true
  else
    echo "readelf not available"
  fi
  if command -v readelf >/dev/null 2>&1 && readelf -d "$OUT" | grep -q 'libvulkan.so.1'; then
    echo "ERROR: libsolum_engine.so still depends on libvulkan.so.1. Android needs libvulkan.so."
    exit 1
  fi
  echo
  echo "Packaged jniLibs:"
  ls -la "$OUT_DIR"
} > "$LOG" 2>&1

echo "Native build OK: $OUT"
echo "Log: $LOG"
