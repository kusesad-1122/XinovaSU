package com.xinsu.moe.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xinsu.moe.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

// Live "tech" readouts for the home screen — battery power/health, CPU model/temp/freq, RAM,
// uptime. Battery comes from the standard BatteryManager + sticky ACTION_BATTERY_CHANGED (no
// root); CPU temperature / frequency are read directly from sysfs (shown as N/A when a device
// keeps them private). Everything is best-effort: any unreadable field degrades to a sentinel
// (-1 / 0 / "") rather than failing.
@Immutable
data class HardwareStats(
    val socModel: String,
    val cpuCores: Int,
    val cpuFreqMhz: Int,       // max current across cores, -1 if unknown
    val cpuTempC: Float,       // -1f if unknown
    val batteryLevel: Int,     // %
    val charging: Boolean,
    val powerWatts: Float,     // |current| * voltage
    val currentMa: Int,        // signed
    val voltageV: Float,
    val batteryTempC: Float,
    val batteryHealth: Int,    // BatteryManager.BATTERY_HEALTH_*
    val ramUsedMb: Int,
    val ramTotalMb: Int,
    val uptimeMs: Long,
)

@Composable
fun rememberHardwareStats(active: Boolean): HardwareStats? {
    val context = LocalContext.current
    val stats by produceState<HardwareStats?>(initialValue = null, active) {
        if (!active) return@produceState
        while (isActive) {
            value = withContext(Dispatchers.IO) { readHardwareStats(context) }
            delay(2000)
        }
    }
    return stats
}

private fun readHardwareStats(context: Context): HardwareStats {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val sticky = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()

    val level = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }.getOrDefault(-1)
    val currentMicroAmp = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrDefault(0)
    val currentMa = currentMicroAmp / 1000

    val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val voltageV = (sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1).let {
        if (it > 0) it / 1000f else 0f
    }
    val batteryTempC = (sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE).let {
        if (it != Int.MIN_VALUE) it / 10f else -1f
    }
    val health = sticky?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
    val powerWatts = abs(currentMa / 1000f) * voltageV

    val (usedMb, totalMb) = readMemoryMb(context)

    return HardwareStats(
        socModel = readSocModel(),
        cpuCores = Runtime.getRuntime().availableProcessors(),
        cpuFreqMhz = readMaxCpuFreqMhz(),
        cpuTempC = readCpuTempC(),
        batteryLevel = level,
        charging = charging,
        powerWatts = powerWatts,
        currentMa = currentMa,
        voltageV = voltageV,
        batteryTempC = batteryTempC,
        batteryHealth = health,
        ramUsedMb = usedMb,
        ramTotalMb = totalMb,
        uptimeMs = SystemClock.elapsedRealtime(),
    )
}

private fun readSocModel(): String {
    val model = Build.SOC_MODEL
    val manufacturer = Build.SOC_MANUFACTURER
    val parts = listOf(manufacturer, model)
        .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
    return if (parts.isNotEmpty()) parts.joinToString(" ") else Build.HARDWARE
}

private fun readMaxCpuFreqMhz(): Int {
    var maxKhz = -1
    val cores = Runtime.getRuntime().availableProcessors()
    for (i in 0 until cores) {
        val khz = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            .takeIf { it.canRead() }
            ?.runCatching { readText().trim().toInt() }
            ?.getOrNull() ?: continue
        if (khz > maxKhz) maxKhz = khz
    }
    return if (maxKhz > 0) maxKhz / 1000 else -1
}

private fun readCpuTempC(): Float {
    val base = File("/sys/class/thermal")
    val zones = base.listFiles { f -> f.name.startsWith("thermal_zone") }?.sortedBy { it.name }
        ?: return -1f
    var fallback = -1f
    for (zone in zones) {
        val type = File(zone, "type").takeIf { it.canRead() }?.runCatching { readText().trim() }?.getOrNull().orEmpty()
        val raw = File(zone, "temp").takeIf { it.canRead() }?.runCatching { readText().trim().toLong() }?.getOrNull()
            ?: continue
        val celsius = if (raw > 1000) raw / 1000f else raw.toFloat()
        if (celsius <= 0f || celsius > 200f) continue
        val t = type.lowercase()
        if (t.contains("cpu") || t.contains("soc") || t.contains("tsens") || t.contains("mtktscpu") || t.contains("bigcore")) {
            return celsius
        }
        if (fallback < 0f) fallback = celsius
    }
    return fallback
}

private fun readMemoryMb(context: Context): Pair<Int, Int> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0 to 0
    val info = ActivityManager.MemoryInfo()
    runCatching { am.getMemoryInfo(info) }
    val totalMb = (info.totalMem / (1024 * 1024)).toInt()
    val usedMb = ((info.totalMem - info.availMem) / (1024 * 1024)).toInt()
    return usedMb to totalMb
}

// Flattens the stats into localized (label, value) tiles for the home "system monitor" card.
@Composable
fun hardwareTiles(stats: HardwareStats): List<Pair<String, String>> {
    fun temp(c: Float) = if (c >= 0f) "%.0f°C".format(c) else "N/A"
    val health = when (stats.batteryHealth) {
        BatteryManager.BATTERY_HEALTH_GOOD -> stringResource(R.string.home_hw_health_good)
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> stringResource(R.string.home_hw_health_overheat)
        BatteryManager.BATTERY_HEALTH_DEAD -> stringResource(R.string.home_hw_health_dead)
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> stringResource(R.string.home_hw_health_overvoltage)
        BatteryManager.BATTERY_HEALTH_COLD -> stringResource(R.string.home_hw_health_cold)
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> stringResource(R.string.home_hw_health_failure)
        else -> stringResource(R.string.home_hw_health_unknown)
    }
    val ram = if (stats.ramTotalMb > 0) "${stats.ramUsedMb} / ${stats.ramTotalMb} MB" else "N/A"
    return listOf(
        stringResource(R.string.home_hw_soc) to stats.socModel.ifBlank { "N/A" },
        stringResource(R.string.home_hw_cpu_temp) to temp(stats.cpuTempC),
        stringResource(R.string.home_hw_cpu_freq) to if (stats.cpuFreqMhz > 0) "${stats.cpuFreqMhz} MHz" else "N/A",
        stringResource(R.string.home_hw_cores) to "${stats.cpuCores}",
        stringResource(R.string.home_hw_battery) to if (stats.batteryLevel >= 0) "${stats.batteryLevel}%" else "N/A",
        stringResource(R.string.home_hw_power) to if (stats.powerWatts > 0.05f) "%.1f W".format(stats.powerWatts) else "—",
        stringResource(R.string.home_hw_current) to "${stats.currentMa} mA",
        stringResource(R.string.home_hw_voltage) to if (stats.voltageV > 0f) "%.2f V".format(stats.voltageV) else "N/A",
        stringResource(R.string.home_hw_battery_temp) to temp(stats.batteryTempC),
        stringResource(R.string.home_hw_health) to health,
        stringResource(R.string.home_hw_ram) to ram,
    )
}
