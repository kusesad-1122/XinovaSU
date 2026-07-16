package com.xinsu.moe.ui.screen.functions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.AppIconImage
import com.xinsu.moe.ui.component.material.ExpressiveSwitch
import com.xinsu.moe.ui.component.profile.dialogs.MultiSelectDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FunctionsMaterial(
    uiState: FunctionsUiState,
    actions: FunctionsScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val apps by rememberInstalledApps()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.functions)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // 1. Hide service
            item {
                FunctionCard(
                    icon = Icons.Filled.VisibilityOff,
                    title = stringResource(R.string.functions_bl_hide),
                    summary = stringResource(R.string.functions_bl_hide_summary),
                    checked = uiState.blHideEnabled,
                    onCheckedChange = actions.onSetBlHideEnabled,
                )
            }

            // 2. Umount service
            item {
                FunctionCard(
                    icon = Icons.Filled.FolderOff,
                    title = stringResource(R.string.functions_umount),
                    summary = stringResource(R.string.functions_umount_summary),
                    checked = uiState.umountEnabled,
                    onCheckedChange = actions.onSetUmountEnabled,
                    switchEnabled = uiState.umountAvailable,
                ) {
                    OutlinedTextField(
                        value = uiState.umountPaths,
                        onValueChange = actions.onUmountPathsChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text(stringResource(R.string.functions_umount_paths_label)) },
                        placeholder = { Text(stringResource(R.string.functions_umount_paths_placeholder)) },
                        minLines = 4,
                        maxLines = Int.MAX_VALUE,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = actions.onUmountSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.functions_save))
                    }
                }
            }

            // 3. Kernel spoof
            item {
                FunctionCard(
                    icon = Icons.Filled.Memory,
                    title = stringResource(R.string.functions_kernel_spoof),
                    summary = stringResource(R.string.functions_kernel_spoof_summary),
                    checked = uiState.kernelSpoofEnabled,
                    onCheckedChange = actions.onSetKernelSpoofEnabled,
                ) {
                    OutlinedTextField(
                        value = uiState.kernelSpoofRelease,
                        onValueChange = actions.onKernelSpoofReleaseChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.functions_kernel_spoof_release)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.kernelSpoofVersion,
                        onValueChange = actions.onKernelSpoofVersionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.functions_kernel_spoof_version)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = actions.onKernelSpoofSave, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.functions_save))
                        }
                        OutlinedButton(onClick = actions.onKernelSpoofRestore, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.functions_kernel_spoof_restore))
                        }
                    }
                }
            }

            // 4. Path hide
            item {
                FunctionCard(
                    icon = Icons.Filled.HideSource,
                    title = stringResource(R.string.functions_path_hide),
                    summary = stringResource(R.string.functions_path_hide_summary),
                    checked = uiState.pathHideEnabled,
                    onCheckedChange = actions.onSetPathHideEnabled,
                ) {
                    OutlinedTextField(
                        value = uiState.pathHidePaths,
                        onValueChange = actions.onPathHidePathsChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text(stringResource(R.string.functions_path_hide_paths_label)) },
                        placeholder = { Text(stringResource(R.string.functions_path_hide_paths_placeholder)) },
                        minLines = 4,
                        maxLines = Int.MAX_VALUE,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = actions.onPathHideSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.functions_save))
                    }
                    Spacer(Modifier.height(12.dp))
                    MaterialAppMultiSelect(
                        apps = apps,
                        selectedUids = uiState.pathHideUids,
                        onUidToggle = actions.onPathHideUidToggle,
                    )
                }
            }

            // 5. Net isolate
            item {
                FunctionCard(
                    icon = Icons.Filled.WifiOff,
                    title = stringResource(R.string.functions_net_isolate),
                    summary = stringResource(R.string.functions_net_isolate_summary),
                    checked = uiState.netIsolateEnabled,
                    onCheckedChange = actions.onSetNetIsolateEnabled,
                ) {
                    MaterialAppMultiSelect(
                        apps = apps,
                        selectedUids = uiState.netIsolateUids,
                        onUidToggle = actions.onNetIsolateUidToggle,
                    )
                }
            }

            // 6. Vpn hide
            item {
                FunctionCard(
                    icon = Icons.Filled.VpnKey,
                    title = stringResource(R.string.functions_vpn_hide),
                    summary = stringResource(R.string.functions_vpn_hide_summary),
                    checked = uiState.vpnHideEnabled,
                    onCheckedChange = actions.onSetVpnHideEnabled,
                ) {
                    MaterialAppMultiSelect(
                        apps = apps,
                        selectedUids = uiState.vpnHideUids,
                        onUidToggle = actions.onVpnHideUidToggle,
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FunctionCard(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ExpressiveSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = switchEnabled,
                )
            }

            if (content != null) {
                AnimatedVisibility(visible = checked) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaterialAppMultiSelect(
    apps: List<InstalledAppEntry>,
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.functions_target_apps),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )

    if (selectedUids.isEmpty()) {
        Text(
            text = stringResource(R.string.functions_no_apps_selected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        Column {
            selectedUids.sorted().forEach { uid ->
                MaterialSelectedAppRow(
                    app = findAppByUid(apps, uid),
                    uid = uid,
                    onRemove = { onUidToggle(uid) },
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.functions_select_apps))
    }

    if (showPicker) {
        MultiSelectDialog(
            title = stringResource(R.string.functions_select_apps),
            items = apps,
            selectedItems = apps.filter { it.uid in selectedUids }.toSet(),
            itemTitle = { it.label },
            itemSubtitle = { "${it.packageName} (UID ${it.uid})" },
            onSelectionChange = { newSelection ->
                val newUids = newSelection.map { it.uid }.toSet()
                val currentInDialog = apps.filter { it.uid in selectedUids }.map { it.uid }.toSet()
                (newUids - currentInDialog).forEach(onUidToggle)
                (currentInDialog - newUids).forEach(onUidToggle)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun MaterialSelectedAppRow(
    app: InstalledAppEntry?,
    uid: Int,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) {
            AppIconImage(
                applicationInfo = app.applicationInfo,
                label = app.label,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app?.label ?: "UID $uid",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app?.let { "${it.packageName} (UID $uid)" } ?: "UID $uid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.functions_remove),
            )
        }
    }
}
