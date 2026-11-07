package com.xinsu.moe.ui.webui.pm

import android.content.pm.ApplicationInfo
import android.webkit.JavascriptInterface
import androidx.core.content.pm.PackageInfoCompat
import com.xinsu.moe.data.model.AppInfo
import com.xinsu.moe.ui.webui.webUIApps
import org.json.JSONArray
import org.json.JSONObject

object KsuPm {
    private const val KSU_PM_VERSION = 1

    @JavascriptInterface
    fun version(): Int = KSU_PM_VERSION

    @JavascriptInterface
    fun listPackages(type: String): String {
        val packageNames = webUIApps()
            .filter { appInfo ->
                // Fork adaption of upstream's pseudo-app exclusion: the fork AppInfo
                // model has no such flag and getAppList() maps only real PackageInfo
                // records, so reject any malformed/pseudo record defensively.
                if (appInfo.packageInfo.applicationInfo == null) return@filter false
                val flags = appInfo.packageInfo.applicationInfo?.flags ?: 0
                when (type.lowercase()) {
                    "system" -> (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    "user" -> (flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    else -> true
                }
            }
            .map { it.packageName }
            .distinct()
            .sorted()

        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun getPackagesInfo(packageNamesJson: String): String {
        val packageNames = JSONArray(packageNamesJson)
        val appsByPackage = webUIApps()
            // Fork adaption of upstream's pseudo-app exclusion (missing_deps):
            // reject malformed records that lack a real ApplicationInfo.
            .filter { it.packageInfo.applicationInfo != null }
            .groupBy { it.packageName }
        val jsonArray = JSONArray()
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val matches = appsByPackage[pkgName]
            if (matches.isNullOrEmpty()) {
                val error = JSONObject()
                error.put("packageName", pkgName)
                error.put("error", "Package not found or inaccessible")
                jsonArray.put(error)
                continue
            }
            for (appInfo in matches.sortedBy { it.uid }) {
                jsonArray.put(buildInfo(appInfo))
            }
        }
        return jsonArray.toString()
    }

    private fun buildInfo(appInfo: AppInfo): JSONObject {
        val pkg = appInfo.packageInfo
        val app = pkg.applicationInfo
        return JSONObject().apply {
            put("packageName", pkg.packageName)
            put("versionName", pkg.versionName ?: "")
            put("versionCode", PackageInfoCompat.getLongVersionCode(pkg))
            put("appLabel", appInfo.label)
            put("isSystem", app?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: JSONObject.NULL)
            put("uid", app?.uid ?: JSONObject.NULL)
            put("userId", app?.let { it.uid / 100000 } ?: JSONObject.NULL)
        }
    }
}
