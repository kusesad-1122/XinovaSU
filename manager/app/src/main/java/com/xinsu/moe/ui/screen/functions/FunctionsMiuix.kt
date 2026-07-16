package com.xinsu.moe.ui.screen.functions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.AppIconImage
import com.xinsu.moe.ui.theme.LocalEnableBlur
import com.xinsu.moe.ui.util.BlurredBar
import com.xinsu.moe.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun FunctionsMiuix(
    uiState: FunctionsUiState,
    actions: FunctionsScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    val apps by rememberInstalledApps()

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                SmallTopAppBar(
                    title = stringResource(R.string.functions),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    // 1. Hide service (bl-hide)
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_bl_hide),
                            summary = stringResource(R.string.functions_bl_hide_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.VisibilityOff,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.blHideEnabled,
                            onCheckedChange = actions.onSetBlHideEnabled
                        )
                    }

                    // 2. Umount service
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_umount),
                            summary = stringResource(R.string.functions_umount_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.FolderOff,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            enabled = uiState.umountAvailable,
                            checked = uiState.umountEnabled,
                            onCheckedChange = actions.onSetUmountEnabled
                        )
                        AnimatedVisibility(
                            visible = uiState.umountEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                TextField(
                                    value = uiState.umountPaths,
                                    onValueChange = actions.onUmountPathsChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(R.string.functions_umount_paths_label),
                                    maxLines = 6,
                                    keyboardOptions = KeyboardOptions.Default,
                                )
                                Spacer(Modifier.height(12.dp))
                                SaveButton(onClick = actions.onUmountSave)
                            }
                        }
                    }

                    // 3. Kernel spoof
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_kernel_spoof),
                            summary = stringResource(R.string.functions_kernel_spoof_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Memory,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.kernelSpoofEnabled,
                            onCheckedChange = actions.onSetKernelSpoofEnabled
                        )
                        AnimatedVisibility(
                            visible = uiState.kernelSpoofEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                TextField(
                                    value = uiState.kernelSpoofRelease,
                                    onValueChange = actions.onKernelSpoofReleaseChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(R.string.functions_kernel_spoof_release),
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(8.dp))
                                TextField(
                                    value = uiState.kernelSpoofVersion,
                                    onValueChange = actions.onKernelSpoofVersionChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(R.string.functions_kernel_spoof_version),
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        text = stringResource(R.string.functions_save),
                                        onClick = actions.onKernelSpoofSave,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    TextButton(
                                        text = stringResource(R.string.functions_kernel_spoof_restore),
                                        onClick = actions.onKernelSpoofRestore,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    // 4. Path hide
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_path_hide),
                            summary = stringResource(R.string.functions_path_hide_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.HideSource,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.pathHideEnabled,
                            onCheckedChange = actions.onSetPathHideEnabled
                        )
                        AnimatedVisibility(
                            visible = uiState.pathHideEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                TextField(
                                    value = uiState.pathHidePaths,
                                    onValueChange = actions.onPathHidePathsChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(R.string.functions_path_hide_paths_label),
                                    maxLines = 6,
                                    keyboardOptions = KeyboardOptions.Default,
                                )
                                Spacer(Modifier.height(12.dp))
                                SaveButton(onClick = actions.onPathHideSave)
                                Spacer(Modifier.height(12.dp))
                                AppMultiSelectSection(
                                    apps = apps,
                                    selectedUids = uiState.pathHideUids,
                                    onUidToggle = actions.onPathHideUidToggle,
                                )
                            }
                        }
                    }

                    // 5. Net isolate
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_net_isolate),
                            summary = stringResource(R.string.functions_net_isolate_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.WifiOff,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.netIsolateEnabled,
                            onCheckedChange = actions.onSetNetIsolateEnabled
                        )
                        AnimatedVisibility(
                            visible = uiState.netIsolateEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                AppMultiSelectSection(
                                    apps = apps,
                                    selectedUids = uiState.netIsolateUids,
                                    onUidToggle = actions.onNetIsolateUidToggle,
                                )
                            }
                        }
                    }

                    // 6. Vpn hide
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.functions_vpn_hide),
                            summary = stringResource(R.string.functions_vpn_hide_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.VpnKey,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.vpnHideEnabled,
                            onCheckedChange = actions.onSetVpnHideEnabled
                        )
                        AnimatedVisibility(
                            visible = uiState.vpnHideEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                AppMultiSelectSection(
                                    apps = apps,
                                    selectedUids = uiState.vpnHideUids,
                                    onUidToggle = actions.onVpnHideUidToggle,
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() + 12.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.functions_save),
        colors = ButtonDefaults.textButtonColorsPrimary(),
        onClick = onClick,
    )
}

@Composable
private fun AppMultiSelectSection(
    apps: List<InstalledAppEntry>,
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.functions_target_apps),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    if (selectedUids.isEmpty()) {
        Text(
            text = stringResource(R.string.functions_no_apps_selected),
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        Column {
            selectedUids.sorted().forEach { uid ->
                SelectedAppRow(
                    app = findAppByUid(apps, uid),
                    uid = uid,
                    onRemove = { onUidToggle(uid) },
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.functions_select_apps),
        onClick = { showPicker = true },
    )

    AppSelectDialog(
        show = showPicker,
        apps = apps,
        selectedUids = selectedUids,
        onUidToggle = onUidToggle,
        onDismissRequest = { showPicker = false },
    )
}

@Composable
private fun SelectedAppRow(
    app: InstalledAppEntry?,
    uid: Int,
    onRemove: () -> Unit,
) {
    BasicComponent(
        title = app?.label ?: "UID $uid",
        summary = app?.let { "${it.packageName} (UID $uid)" } ?: "UID $uid",
        insideMargin = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        startAction = {
            if (app != null) {
                AppIconImage(
                    applicationInfo = app.applicationInfo,
                    label = app.label,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .height(40.dp)
                        .width(40.dp)
                )
            }
        },
        endActions = {
            IconButton(onClick = onRemove) {
                Icon(
                    MiuixIcons.Close,
                    modifier = Modifier.height(16.dp).width(16.dp),
                    contentDescription = stringResource(R.string.functions_remove),
                    tint = colorScheme.onSurfaceVariantActions
                )
            }
        }
    )
}

@Composable
private fun AppSelectDialog(
    show: Boolean,
    apps: List<InstalledAppEntry>,
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    val filtered = remember(apps, query, showSystem) {
        apps.filter { showSystem || !it.isSystem }
            .filter {
                query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
    }

    OverlayDialog(
        show = show,
        title = stringResource(R.string.functions_select_apps),
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 24.dp),
        content = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    label = stringResource(R.string.functions_search_apps),
                    maxLines = 1,
                )
                CheckboxPreference(
                    title = stringResource(R.string.show_system_apps),
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    checked = showSystem,
                    onCheckedChange = { showSystem = it },
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it.uid }) { app ->
                        CheckboxPreference(
                            title = app.label,
                            summary = "${app.packageName} (UID ${app.uid})",
                            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            checkboxLocation = CheckboxLocation.End,
                            checked = app.uid in selectedUids,
                            onCheckedChange = { onUidToggle(app.uid) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(android.R.string.ok),
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}
