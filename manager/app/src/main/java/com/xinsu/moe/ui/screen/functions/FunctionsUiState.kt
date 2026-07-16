package com.xinsu.moe.ui.screen.functions

import androidx.compose.runtime.Immutable

@Immutable
data class FunctionsUiState(
    // bl-hide
    val blHideEnabled: Boolean = false,

    // Umount Service
    val umountAvailable: Boolean = false,
    val umountEnabled: Boolean = false,
    val umountPaths: String = "",

    // Kernel spoof (uts)
    val kernelSpoofEnabled: Boolean = false,
    val kernelSpoofRelease: String = "",
    val kernelSpoofVersion: String = "",

    // Path hide
    val pathHideEnabled: Boolean = false,
    val pathHidePaths: String = "",
    val pathHideUids: Set<Int> = emptySet(),

    // Net isolate
    val netIsolateEnabled: Boolean = false,
    val netIsolateUids: Set<Int> = emptySet(),
)

@Immutable
data class FunctionsScreenActions(
    val onBack: () -> Unit,

    val onSetBlHideEnabled: (Boolean) -> Unit,

    val onSetUmountEnabled: (Boolean) -> Unit,
    val onUmountPathsChange: (String) -> Unit,
    val onUmountSave: () -> Unit,

    val onSetKernelSpoofEnabled: (Boolean) -> Unit,
    val onKernelSpoofReleaseChange: (String) -> Unit,
    val onKernelSpoofVersionChange: (String) -> Unit,
    val onKernelSpoofSave: () -> Unit,
    val onKernelSpoofRestore: () -> Unit,

    val onSetPathHideEnabled: (Boolean) -> Unit,
    val onPathHidePathsChange: (String) -> Unit,
    val onPathHideSave: () -> Unit,
    val onPathHideUidToggle: (Int) -> Unit,

    val onSetNetIsolateEnabled: (Boolean) -> Unit,
    val onNetIsolateUidToggle: (Int) -> Unit,
)
