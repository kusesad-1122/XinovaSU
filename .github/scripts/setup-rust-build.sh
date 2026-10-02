#!/usr/bin/env bash
# Point cargo/cc/cxx at the Android NDK for one target triple.
#
# Usage: source setup-rust-build.sh <triple> <android-api-level>
#   e.g. source setup-rust-build.sh aarch64-linux-android 26
#
# Sourced (not executed) so the exported CC/CXX/AR/LINKER vars land in the
# calling shell. Mirrors upstream KernelSU's .github/scripts/setup-rust-build.sh.
#
# Deliberately does NOT call `set -euo pipefail`: this file is sourced, so
# those options would leak into and mutate the caller's shell state.

TRIPLE="$1"
ANDROID_SDK_LEVEL="$2"

: "${ANDROID_NDK_HOME:?ANDROID_NDK_HOME must be set (use nttld/setup-ndk and pass its ndk-path)}"

LLVM_PATH="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
LLVM_BIN="$LLVM_PATH/bin"
CLANG_PATH="$LLVM_BIN/${TRIPLE}${ANDROID_SDK_LEVEL}-clang"

if [ ! -x "$CLANG_PATH" ]; then
  echo "error: NDK clang not found at $CLANG_PATH" >&2
  echo "available:" >&2
  ls "$LLVM_BIN" 2>/dev/null | grep "^${TRIPLE}" >&2 || true
  exit 1
fi

# cargo env-var naming: dots -> underscores, uppercased.
UTRIPLE="$(echo "$TRIPLE" | sed 's/-/_/g')"
UUTRIPLE="$(echo "$UTRIPLE" | tr a-z A-Z)"

export "CC_$UTRIPLE=$CLANG_PATH"
export "CXX_$UTRIPLE=${CLANG_PATH}++"
export "AR_$UTRIPLE=$LLVM_BIN/llvm-ar"
export "CARGO_TARGET_${UUTRIPLE}_LINKER=$CLANG_PATH"
export "BINDGEN_EXTRA_CLANG_ARGS_$UTRIPLE=--sysroot=$LLVM_PATH/sysroot -I$LLVM_PATH/sysroot/usr/include/$TRIPLE"
