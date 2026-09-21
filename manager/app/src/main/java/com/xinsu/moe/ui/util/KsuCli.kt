package com.xinsu.moe.ui.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.xinsu.moe.BuildConfig
import com.xinsu.moe.Natives
import com.xinsu.moe.ksuApp
import org.json.JSONArray
import java.io.File

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"

private fun getKsuDaemonPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libxnsusd.so"
}

data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
    constructor(result: Shell.Result, showReboot: Boolean) : this(result.code, result.err.joinToString("\n"), showReboot)
    constructor(result: Shell.Result) : this(result, result.isSuccess)
}

object KsuCli {
    // ⚠️ 降级 shell（sh / su -mm）绝不能被永久缓存。
    // 内核侧 throne_tracker 在 packages.list 半截解析时会把管理器身份"瞬时失效"，
    // 此时创建 root shell 会失败并 fallback 成非 root 的 sh；一旦把它缓存进单例，
    // root 就表现为"永久掉线"，只能杀掉管理器进程恢复（用户实测：重进才好）。
    // 所以：只有真正拿到 root 的 shell 才允许缓存，降级 shell 每次调用都重试。
    @Volatile private var cachedShell: Shell? = null
    @Volatile private var cachedGlobalMntShell: Shell? = null

    // 兼容旧引用点（SuperUserRepositoryImpl）
    val SHELL: Shell
        get() = getRootShell(false)
    val GLOBAL_MNT_SHELL: Shell
        get() = getRootShell(true)
}

fun getRootShell(globalMnt: Boolean = false): Shell {
    val cached = if (globalMnt) KsuCli.cachedGlobalMntShell else KsuCli.cachedShell
    if (cached != null && cached.isRoot) return cached
    return synchronized(KsuCli) {
        val again = if (globalMnt) KsuCli.cachedGlobalMntShell else KsuCli.cachedShell
        if (again != null && again.isRoot) {
            again
        } else {
            val shell = createRootShell(globalMnt)
            // 只有真拿到 root 才缓存；降级 shell 不缓存，让下一次调用自愈
            if (shell.isRoot) {
                if (globalMnt) KsuCli.cachedGlobalMntShell = shell else KsuCli.cachedShell = shell
            }
            shell
        }
    }
}

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
}

fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    return try {
        if (globalMnt) {
            builder.build(getKsuDaemonPath(), "debug", "su", "-g")
        } else {
            builder.build(getKsuDaemonPath(), "debug", "su")
        }
    } catch (e: Throwable) {
        Log.w(TAG, "ksu failed: ", e)
        try {
            if (globalMnt) {
                builder.build("su", "-mm")
            } else {
                builder.build("su")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "su failed: ", e)
            builder.build("sh")
        }
    }
}

fun execKsud(args: String, newShell: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "${getKsuDaemonPath()} $args")
    }
}

suspend fun getFeatureStatus(feature: String): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature check $feature").to(ArrayList<String>(), null).exec().out
    out.firstOrNull()?.trim().orEmpty()
}

suspend fun getFeaturePersistValue(feature: String): Long? = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature get --config $feature").to(ArrayList<String>(), null).exec().out
    val valueLine = out.firstOrNull { it.trim().startsWith("Value:") } ?: return@withContext null
    valueLine.substringAfter("Value:").trim().toLongOrNull()
}

fun install() {
    val start = SystemClock.elapsedRealtime()
    val libadbroot = File(ksuApp.applicationInfo.nativeLibraryDir, "libadbroot.so").absolutePath
    val result = execKsud("install --libadbroot $libadbroot", true)
    Log.w(TAG, "install result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
}

fun listModules(): String {
    val shell = getRootShell()

    val out = shell.newJob()
        .add("${getKsuDaemonPath()} module list").to(ArrayList(), null).exec().out
    return out.joinToString("\n").ifBlank { "[]" }
}

fun getModuleCount(): Int {
    val result = listModules()
    runCatching {
        val array = JSONArray(result)
        return array.length()
    }.getOrElse { return 0 }
}

fun getSuperuserCount(): Int {
    return Natives.getSuperuserCount()
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execKsud(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun undoUninstallModule(id: String): Boolean {
    val cmd = "module undo-uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "undo uninstall module $id result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

private fun flashWithIO(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    return withNewRootShell {
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }
}

fun flashModule(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver
    with(resolver.openInputStream(uri)) {
        val file = File(ksuApp.cacheDir, "module.zip")
        file.outputStream().use { output ->
            this?.copyTo(output)
        }
        val cmd = "module install ${file.absolutePath}"
        val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
        Log.i("XinovaSU", "install module $uri result: $result")

        file.delete()

        return FlashResult(result)
    }
}

fun runModuleAction(
    moduleId: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    val result = withNewRootShell(true) {
        newJob().add("${getKsuDaemonPath()} module action $moduleId")
            .to(stdoutCallback, stderrCallback).exec()
    }

    Log.i("XinovaSU", "Module runAction result: $result")

    return result.isSuccess
}

fun restoreBoot(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO("${getKsuDaemonPath()} boot-restore -f", onStdout, onStderr)
    return FlashResult(result)
}

fun uninstallPermanently(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO("${getKsuDaemonPath()} uninstall --package-name ${BuildConfig.APPLICATION_ID}", onStdout, onStderr)
    return FlashResult(result)
}

@Parcelize
sealed class LkmSelection : Parcelable {
    @Parcelize
    data class LkmUri(val uri: Uri) : LkmSelection()

    @Parcelize
    data class KmiString(val value: String) : LkmSelection()

    @Parcelize
    data object KmiNone : LkmSelection()
}

fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    partition: String?,
    allowShell: Boolean,
    enableAdb: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult {
    val resolver = ksuApp.contentResolver

    val bootFile = bootUri?.let { uri ->
        with(resolver.openInputStream(uri)) {
            val bootFile = File(ksuApp.cacheDir, "boot.img")
            bootFile.outputStream().use { output ->
                this?.copyTo(output)
            }

            bootFile
        }
    }

    var cmd = "boot-patch"

    cmd += if (bootFile == null) {
        // no boot.img, use -f to flash
        " -f"
    } else {
        " -b ${bootFile.absolutePath}"
    }

    if (allowShell) {
        cmd += " --allow-shell"
    }

    if (enableAdb) {
        cmd += " --enable-adbd"
    }

    if (ota) {
        cmd += " -u"
    }

    var lkmFile: File? = null
    when (lkm) {
        is LkmSelection.LkmUri -> {
            lkmFile = with(resolver.openInputStream(lkm.uri)) {
                val file = File(ksuApp.cacheDir, "xinovasu-tmp-lkm.ko")
                file.outputStream().use { output ->
                    this?.copyTo(output)
                }

                file
            }
            cmd += " -m ${lkmFile.absolutePath}"
        }

        is LkmSelection.KmiString -> {
            cmd += " --kmi ${lkm.value}"
        }

        LkmSelection.KmiNone -> {
            // do nothing
        }
    }

    // output dir
    if (bootFile != null) {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        cmd += " -o $downloadsDir"
    }

    partition?.let { part ->
        cmd += " --partition $part"
    }

    val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
    Log.i("XinovaSU", "install boot result: ${result.isSuccess}")

    bootFile?.delete()
    lkmFile?.delete()

    // if boot uri is empty, it is direct install, when success, we should show reboot button
    val showReboot = bootUri == null && result.isSuccess // we create a temporary val here, to avoid calc showReboot double
    if (showReboot) { // because we decide do not update xnsusd when startActivity
        install() // install xnsusd here
    }
    return FlashResult(result, showReboot)
}

fun reboot(reason: String = "") {
    if (reason == "soft_reboot") {
        execKsud("soft-reboot", true)
        return
    }
    val shell = getRootShell()
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        ShellUtils.fastCmd(shell, "/system/bin/input keyevent 26")
    }
    ShellUtils.fastCmd(shell, "/system/bin/svc power reboot $reason || /system/bin/reboot $reason")
}

fun rootAvailable(): Boolean {
    val shell = getRootShell()
    return shell.isRoot
}

suspend fun getCurrentKmi(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info current-kmi"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd")
}

suspend fun getSupportedKmis(): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info supported-kmis"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

suspend fun isAbDevice(): Boolean = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info is-ab-device"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim().toBoolean()
}

suspend fun getDefaultPartition(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    if (shell.isRoot) {
        val cmd = "boot-info default-partition"
        ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim()
    } else {
        if (!Os.uname().release.contains("android12-")) "init_boot" else "boot"
    }
}

suspend fun getSlotSuffix(ota: Boolean): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = if (ota) {
        "boot-info slot-suffix --ota"
    } else {
        "boot-info slot-suffix"
    }
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim()
}

suspend fun getAvailablePartitions(): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info available-partitions"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

fun hasMagisk(): Boolean {
    val shell = getRootShell(true)
    val result = shell.newJob().add("which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun isSepolicyValid(rules: String?): Boolean {
    if (rules == null) {
        return true
    }
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} sepolicy check '$rules'").to(ArrayList(), null)
            .exec()
    return result.isSuccess
}

fun getSepolicy(pkg: String): String {
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} profile get-sepolicy $pkg").to(ArrayList(), null)
            .exec()
    Log.i(TAG, "code: ${result.code}, out: ${result.out}, err: ${result.err}")
    return result.out.joinToString("\n")
}

fun setSepolicy(pkg: String, rules: String): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("${getKsuDaemonPath()} profile set-sepolicy $pkg '$rules'")
        .to(ArrayList(), null).exec()
    Log.i(TAG, "set sepolicy result: ${result.code}")
    return result.isSuccess
}

fun listAppProfileTemplates(): List<String> {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile list-templates").to(ArrayList(), null)
        .exec().out
}

fun getAppProfileTemplate(id: String): String {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile get-template '${id}'")
        .to(ArrayList(), null).exec().out.joinToString("\n")
}

fun setAppProfileTemplate(id: String, template: String): Boolean {
    val shell = getRootShell()
    val escapedTemplate = template.replace("\"", "\\\"")
    val cmd = """${getKsuDaemonPath()} profile set-template "$id" "$escapedTemplate'""""
    return shell.newJob().add(cmd)
        .to(ArrayList(), null).exec().isSuccess
}

fun deleteAppProfileTemplate(id: String): Boolean {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile delete-template '${id}'")
        .to(ArrayList(), null).exec().isSuccess
}

fun forceStopApp(packageName: String, userId: Int? = null) {
    val shell = getRootShell()
    val userArg = userId?.let { " --user $it" } ?: ""
    val result = shell.newJob().add("am force-stop$userArg $packageName").exec()
    Log.i(TAG, "force stop $packageName result: $result")
}

fun launchApp(packageName: String, userId: Int? = null) {
    val shell = getRootShell()
    val userArg = userId?.let { " --user $it" } ?: ""
    val result =
        shell.newJob()
            .add("cmd package resolve-activity --brief$userArg $packageName | tail -n 1 | xargs cmd activity start-activity$userArg -n")
            .exec()
    Log.i(TAG, "launch $packageName result: $result")
}

fun restartApp(packageName: String, userId: Int? = null) {
    forceStopApp(packageName, userId)
    launchApp(packageName, userId)
}

// ---------------------------------------------------------------------------
// Functions screen helpers (bl-hide / umount / kernel spoof / net-isolate / path-hide)
// These read config files under /data/adb/ksu via a root shell `cat`, and act
// through the ksud daemon via execKsud. Kept here because getKsuDaemonPath() is private.
// ---------------------------------------------------------------------------

private const val KSU_DIR = "/data/adb/ksu"
private const val BL_HIDE_MARKER = "$KSU_DIR/.bl_hide_enable"

private fun catLines(path: String): List<String> {
    val shell = getRootShell()
    return shell.newJob().add("cat '$path' 2>/dev/null").to(ArrayList<String>(), null).exec().out
}

// bl-hide -------------------------------------------------------------------

fun blHideIsEnabled(): Boolean {
    return ShellUtils.fastCmdResult(getRootShell(), "test -f '$BL_HIDE_MARKER'")
}

fun blHideSetEnabled(enabled: Boolean): Boolean {
    return if (enabled) {
        val ok = execKsud("bl-hide", true)
        ShellUtils.fastCmdResult(getRootShell(), "touch '$BL_HIDE_MARKER'")
        ok
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "rm -f '$BL_HIDE_MARKER'")
    }
}

// umount --------------------------------------------------------------------

fun umountReadPaths(): List<String> {
    return catLines("$KSU_DIR/umount_list")
        .map { it.substringBefore('\t').trim() }
        .filter { it.isNotBlank() }
}

fun umountSavePaths(paths: List<String>): Boolean {
    val args = paths.joinToString("") { " '${it}'" }
    return execKsud("kernel umount save$args", true)
}

// kernel spoof (uts) --------------------------------------------------------

data class UtsSpoofConfig(val enabled: Boolean, val release: String, val version: String)

fun utsSpoofRead(): UtsSpoofConfig {
    val lines = catLines("$KSU_DIR/uts_spoof.conf")
    return if (lines.isEmpty()) {
        UtsSpoofConfig(enabled = false, release = "", version = "")
    } else {
        UtsSpoofConfig(
            enabled = true,
            release = lines.getOrNull(0)?.trim().orEmpty(),
            version = lines.getOrNull(1)?.trim().orEmpty(),
        )
    }
}

fun utsSpoofSet(release: String, version: String): Boolean {
    return execKsud("kernel uts-spoof set '$release' '$version'", true)
}

fun utsSpoofReset(): Boolean {
    return execKsud("kernel uts-spoof reset", true)
}

// net-isolate ---------------------------------------------------------------

data class NetIsolateConfig(val enabled: Boolean, val uids: Set<Int>)

fun netIsolateRead(): NetIsolateConfig {
    val lines = catLines("$KSU_DIR/net_isolate.conf")
    if (lines.isEmpty()) return NetIsolateConfig(enabled = false, uids = emptySet())
    val enabled = lines.firstOrNull()?.trim() == "1"
    val uids = lines.drop(1).mapNotNull { it.trim().toIntOrNull() }.toSet()
    return NetIsolateConfig(enabled = enabled, uids = uids)
}

fun netIsolateSave(enabled: Boolean, uids: Set<Int>): Boolean {
    val cmd = "kernel net-isolate set" +
            (if (enabled) " --enabled" else "") +
            uids.joinToString("") { " $it" }
    return execKsud(cmd, true)
}

// path-hide -----------------------------------------------------------------

data class PathHideConfig(val enabled: Boolean, val paths: List<String>, val uids: Set<Int>)

fun pathHideRead(): PathHideConfig {
    val lines = catLines("$KSU_DIR/path_hide.conf")
    var enabled = false
    val paths = mutableListOf<String>()
    val uids = mutableSetOf<Int>()
    for (raw in lines) {
        val line = raw.trim()
        when {
            line.startsWith("enabled=") ->
                enabled = line.substringAfter("enabled=").trim() == "1"
            line.startsWith("path=") ->
                line.substringAfter("path=").trim().takeIf { it.isNotBlank() }?.let { paths.add(it) }
            line.startsWith("uid=") ->
                line.substringAfter("uid=").trim().toIntOrNull()?.let { uids.add(it) }
        }
    }
    return PathHideConfig(enabled = enabled, paths = paths, uids = uids)
}

fun pathHideSave(enabled: Boolean, paths: List<String>, uids: Set<Int>): Boolean {
    val cmd = "kernel path-hide set" +
            (if (enabled) " --enabled" else "") +
            paths.joinToString("") { " --path '$it'" } +
            uids.joinToString("") { " --uid $it" }
    return execKsud(cmd, true)
}

// cpu-spoof -----------------------------------------------------------------

data class CpuSpoofConfig(val enabled: Boolean, val template: String)

fun cpuSpoofRead(): CpuSpoofConfig {
    val lines = catLines("$KSU_DIR/cpu_spoof.conf")
    var enabled = false
    var template = ""
    for (raw in lines) {
        val line = raw.trim()
        when {
            line.startsWith("enabled=") -> enabled = line.substringAfter("enabled=").trim() == "1"
            line.startsWith("template=") -> template = line.substringAfter("template=").trim()
        }
    }
    return CpuSpoofConfig(enabled = enabled, template = template)
}

/// Enabling fetches the decoy material from the licensing server (which also
/// checks the internal allowlist), so it can legitimately fail — the caller
/// must surface that to the user instead of pretending the switch worked.
fun cpuSpoofEnable(template: String): Boolean =
    execKsud("kernel cpu-spoof enable $template", true)

fun cpuSpoofDisable(): Boolean =
    execKsud("kernel cpu-spoof disable", true)

// vpn-hide ------------------------------------------------------------------

data class VpnHideConfig(val enabled: Boolean, val uids: Set<Int>)

fun vpnHideRead(): VpnHideConfig {
    val lines = catLines("$KSU_DIR/vpn_hide.conf")
    if (lines.isEmpty()) return VpnHideConfig(enabled = false, uids = emptySet())
    val enabled = lines.firstOrNull()?.trim() == "1"
    val uids = lines.drop(1).mapNotNull { it.trim().toIntOrNull() }.toSet()
    return VpnHideConfig(enabled = enabled, uids = uids)
}

fun vpnHideSave(enabled: Boolean, uids: Set<Int>): Boolean {
    val cmd = "kernel vpn-hide set" +
            (if (enabled) " --enabled" else "") +
            uids.joinToString("") { " $it" }
    return execKsud(cmd, true)
}
