plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.paparazzi) apply false
}

val androidMinSdkVersion by extra(31)
val androidTargetSdkVersion by extra(37)
val androidCompileSdkVersion by extra(37)
val androidCompileSdkVersionMinor by extra(0)
val androidBuildToolsVersion by extra("37.0.0")
val androidCompileNdkVersion: String by extra(libs.versions.ndk.get())
val androidSourceCompatibility by extra(JavaVersion.VERSION_21)
val androidTargetCompatibility by extra(JavaVersion.VERSION_21)
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())

fun getGitCommitCount(): Int {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
    return process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
}

fun getGitDescribe(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
    return process.inputStream.bufferedReader().use { it.readText().trim() }
}

// ── versionCode 必须与 kernel/Kbuild 的 XNSU_VERSION 保持一致 ──────────────
//
// 两者都要满足 Natives.kt 的硬门槛：
//     const val MINIMAL_SUPPORTED_KERNEL = 32377
// 低于它管理器就拒绝工作（界面显示"未安装"）。KERNEL_SU_VERSION 走
// `kernel/include/xnsu.h: #define KERNEL_SU_VERSION XNSU_VERSION`，
// 所以这个公式在 Kbuild 与这里必须逐位相同。
//
// 历史沿革（踩过的坑）：原来是 `30000 + commitCount`，产出 30028 < 32377，
// 刷入后模块与 su 全部正常、管理器却显示"未安装"，
// 界面提示"当前 XinovaSU 版本过低，请升级至 32377 或以上"。
//
// 为什么不能继续用 `30000 + commitCount`：
// 官方 Releases 的历史 v3.4.1..v3.4.8 对应 32619..32637，按
// `major*10000 + git + 200`（Kbuild 里的注释）反推其 git version 是
// 2419..2437 —— 那是完整上游仓库的提交数。本仓库历史被压成单个根提交
// （`6c26d51 XinovaSU v3.4.8`），只有 30 个 commit，
// 所以 `30000 + n` 永远追不上门槛。
//
// 现在固定基数为 33000：既高于 32377 门槛，也给后续 commit 留出余量
// （要攒 2377 个 commit 才会掉回门槛以下，实际不可能触及）。
// major 位保留 3（对应 v3.x），便于将来升 v4 时自然进位到 40000+。
private const val VERSION_CODE_MAJOR = 3
private const val VERSION_CODE_BASE = 33000

fun getVersionCode(): Int {
    return VERSION_CODE_BASE + getGitCommitCount()
}

fun getVersionName(): String {
    return getGitDescribe()
}
