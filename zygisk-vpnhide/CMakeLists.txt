cmake_minimum_required(VERSION 3.22)
project(vpnhide CXX)

# Standalone Zygisk module .so. Built once per ABI by the NDK toolchain (see
# .github/workflows/zygisk-vpnhide.yml); the resulting libvpnhide.so is renamed
# to <abi>.so and dropped into the module's zygisk/ directory.

add_library(vpnhide SHARED src/main.cpp)

target_compile_features(vpnhide PRIVATE cxx_std_20)
target_compile_options(vpnhide PRIVATE
        -Wall -Wextra
        -fvisibility=hidden -fvisibility-inlines-hidden
        -fno-exceptions -fno-rtti
        -Oz -ffunction-sections -fdata-sections)
target_link_options(vpnhide PRIVATE
        -Wl,--gc-sections -Wl,--strip-all -Wl,--hash-style=both)

target_link_libraries(vpnhide log)
