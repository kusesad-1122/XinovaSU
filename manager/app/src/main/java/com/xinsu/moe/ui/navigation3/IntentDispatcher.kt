package com.xinsu.moe.ui.navigation3

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.xinsu.moe.Natives
import com.xinsu.moe.R
import com.xinsu.moe.data.script.ScriptRepository
import com.xinsu.moe.ksuApp
import com.xinsu.moe.ui.component.dialog.rememberConfirmDialog
import com.xinsu.moe.ui.screen.flash.FlashIt
import com.xinsu.moe.ui.screen.scriptrun.ScriptRunArg
import com.xinsu.moe.ui.util.DownloadService
import com.xinsu.moe.ui.util.getFileName

/**
 * Resolved intent action to execute after validation.
 */
private sealed interface PendingAction {
    /** Install module(s) from URI — triggered by DownloadService notification or ZIP file open. */
    data class InstallModule(
        val uri: Uri,
        val displayName: String,
        val requiresConfirmation: Boolean,
    ) : PendingAction {
        companion object {
            val InstallModuleSaver = listSaver<InstallModule?, Any>(
                save = { module ->
                    if (module == null) {
                        emptyList()
                    } else {
                        listOf(
                            module.uri,
                            module.displayName,
                            module.requiresConfirmation
                        )
                    }
                },
                restore = { list ->
                    if (list.isEmpty()) {
                        null
                    } else {
                        InstallModule(
                            uri = list[0] as Uri,
                            displayName = list[1] as String,
                            requiresConfirmation = list[2] as Boolean
                        )
                    }
                }
            )
        }
    }
}

fun getDisplayName(uri: Uri): String {
    return uri.getFileName(ksuApp) ?: uri.lastPathSegment ?: "Unknown"
}

/**
 * Resolve an intent snapshot into a [PendingAction].
 * Returns null if the intent carries no recognized action.
 */
private fun resolveIntent(intent: Intent): PendingAction? {
    // DownloadService notification: install module
    if (intent.action == DownloadService.ACTION_INSTALL_MODULE) {
        val uriString = intent.getStringExtra(DownloadService.EXTRA_MODULE_URI)
            ?: return null
        val uri = uriString.toUri()
        return PendingAction.InstallModule(
            uri = uri,
            displayName = getDisplayName(uri),
            requiresConfirmation = false,
        )
    }

    // File manager: open ZIP
    val viewUri = intent.data
    if (viewUri != null && viewUri.scheme == "content" && intent.type in ZIP_MIME_TYPES) {
        return PendingAction.InstallModule(
            uri = viewUri,
            displayName = getDisplayName(viewUri),
            requiresConfirmation = true,
        )
    }

    return null
}

/**
 * Fork-side intent dispatcher. The fork has no Channel<Intent> plumbing; it bumps
 * [intentState] in onNewIntent and re-reads activity.intent, so each snapshot is consumed once.
 */
@SuppressLint("StringFormatInvalid", "LocalContextGetResourceValueCall")
@Composable
fun IntentDispatcher(intentState: MutableStateFlow<Int>) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val isSafeMode = Natives.isSafeMode
    val isManager = Natives.isManager
    var pendingZipInstall by rememberSaveable(stateSaver = PendingAction.InstallModule.InstallModuleSaver) { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    // A one-shot script ZIP opens the run page, which also offers installing it as a module.
    val openModuleZip: (Uri) -> Unit = { uri ->
        scope.launch {
            val script = ScriptRepository.peekOneshot(uri)
            if (script != null) {
                navigator.push(Route.ScriptRun(ScriptRunArg(uri, offerInstall = true, displayName = script.name)))
            } else {
                navigator.push(Route.Flash(FlashIt.FlashModules(listOf(uri))))
            }
        }
    }

    val installDialog = rememberConfirmDialog(
        onConfirm = {
            pendingZipInstall?.let { action -> openModuleZip(action.uri) }
            pendingZipInstall = null
        },
        onDismiss = { pendingZipInstall = null }
    )

    val intentStateValue by intentState.collectAsStateWithLifecycle()
    // Not saveable: intentState itself restarts at 0 on activity recreation.
    var lastHandledIntentId by remember { mutableIntStateOf(-1) }
    LaunchedEffect(intentStateValue) {
        if (!isManager || intentStateValue == lastHandledIntentId) return@LaunchedEffect
        val intent = activity?.intent ?: return@LaunchedEffect
        val action = resolveIntent(intent) ?: return@LaunchedEffect

        when (action) {
            is PendingAction.InstallModule -> {
                if (isSafeMode) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.safe_mode_module_disabled),
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (action.requiresConfirmation) {
                    pendingZipInstall = action
                    installDialog.showConfirm(
                        title = resources.getString(R.string.module),
                        content = resources.getString(
                            R.string.module_install_prompt_with_name,
                            "\n${action.displayName}"
                        )
                    )
                } else {
                    openModuleZip(action.uri)
                }
            }
        }

        // Consume the snapshot so the next intentState bump does not replay it.
        if (intent.action == DownloadService.ACTION_INSTALL_MODULE) {
            intent.action = null
            intent.removeExtra(DownloadService.EXTRA_MODULE_URI)
            intent.removeExtra(DownloadService.EXTRA_DOWNLOAD_ID)
        } else {
            intent.data = null
            intent.type = null
        }
        lastHandledIntentId = intentStateValue
    }
}


