# 本地构建入口。注意：管理器的 APK 只能编链接用，KSU 环境本机没有
# JDK/Android SDK 时请走 GitHub Actions（.github/workflows/build-manager.yml）。

alias bk := build_xnsusd
alias bm := build_manager

# userspace/ksud 的 package name 是 xnsusd（目录叫 ksud 只是历史遗留），
# 所以 cargo 产出的文件名是 xnsusd 而非 ksud。设备上仍然通过
# /data/adb/ksud 兼容链接访问，见 userspace/ksud/src/assets.rs。
build_xnsusd:
    cross build --package xnsusd --target aarch64-linux-android --release

# 产物的命名契约（三处必须一致，否则装上去用不了）：
#   1. userspace/ksud/Cargo.toml 的 package name = "xnsusd"  -> 产物 xnsusd
#   2. 本步骤重命名为 libxnsusd.so 放进 jniLibs
#   3. manager 的 KsuCli.getKsuDaemonPath() 读 nativeLibraryDir/libxnsusd.so
# 之前这里写的是 libksud.so，与 (3) 不一致 —— 编译能过，装上后 xnsusd 缺失。
build_manager: build_xnsusd
    cp target/aarch64-linux-android/release/xnsusd manager/app/src/main/jniLibs/arm64-v8a/libxnsusd.so
    cd manager && ./gradlew aDebug

clippy:
    cargo fmt
    cross clippy --package xnsusd --target aarch64-linux-android --release
