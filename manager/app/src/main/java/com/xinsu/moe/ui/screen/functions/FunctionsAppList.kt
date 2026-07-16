package com.xinsu.moe.ui.screen.functions

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.xinsu.moe.data.repository.SuperUserRepositoryImpl

/**
 * A single installed app entry, de-duplicated by uid, used by the app multi-select
 * pickers for the path-hide and net-isolate cards.
 */
data class InstalledAppEntry(
    val uid: Int,
    val packageName: String,
    val label: String,
    val applicationInfo: ApplicationInfo,
    val isSystem: Boolean,
)

/**
 * Loads the installed application list off the main thread. Returns an empty list until
 * loading completes. Apps are de-duplicated by uid and sorted by label.
 *
 * The list is sourced from the root-side [com.xinsu.moe.ui.KsuService] (via
 * [SuperUserRepositoryImpl]) rather than the in-process PackageManager: since Android 11,
 * `getInstalledApplications` is subject to package-visibility filtering and would only
 * return this app plus a handful of visible packages. The root service enumerates every
 * package across all users, which is what these UID pickers need.
 */
@Composable
fun rememberInstalledApps(): State<List<InstalledAppEntry>> {
    return produceState<List<InstalledAppEntry>>(emptyList()) {
        value = runCatching {
            SuperUserRepositoryImpl().getAppList().getOrNull()
                ?.first
                ?.mapNotNull { appInfo ->
                    val ai = appInfo.packageInfo.applicationInfo ?: return@mapNotNull null
                    InstalledAppEntry(
                        uid = appInfo.uid,
                        packageName = appInfo.packageName,
                        label = appInfo.label,
                        applicationInfo = ai,
                        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    )
                }
                ?.distinctBy { it.uid }
                ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
}

fun findAppByUid(apps: List<InstalledAppEntry>, uid: Int): InstalledAppEntry? =
    apps.firstOrNull { it.uid == uid }
