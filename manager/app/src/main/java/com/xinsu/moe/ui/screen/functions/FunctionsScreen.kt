package com.xinsu.moe.ui.screen.functions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xinsu.moe.ui.LocalUiMode
import com.xinsu.moe.ui.UiMode
import com.xinsu.moe.ui.navigation3.LocalNavigator
import com.xinsu.moe.ui.viewmodel.FunctionsViewModel

@Composable
fun FunctionsScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<FunctionsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = FunctionsScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSetBlHideEnabled = viewModel::setBlHideEnabled,
        onSetUmountEnabled = viewModel::setUmountEnabled,
        onUmountPathsChange = viewModel::setUmountPaths,
        onUmountSave = viewModel::saveUmountPaths,
        onSetKernelSpoofEnabled = viewModel::setKernelSpoofEnabled,
        onKernelSpoofReleaseChange = viewModel::setKernelSpoofRelease,
        onKernelSpoofVersionChange = viewModel::setKernelSpoofVersion,
        onKernelSpoofSave = viewModel::saveKernelSpoof,
        onKernelSpoofRestore = viewModel::restoreKernelSpoofDefaults,
        onSetPathHideEnabled = viewModel::setPathHideEnabled,
        onPathHidePathsChange = viewModel::setPathHidePaths,
        onPathHideSave = viewModel::savePathHide,
        onPathHideUidToggle = viewModel::togglePathHideUid,
        onSetNetIsolateEnabled = viewModel::setNetIsolateEnabled,
        onNetIsolateUidToggle = viewModel::toggleNetIsolateUid,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> FunctionsMiuix(uiState, actions)
        UiMode.Material -> FunctionsMaterial(uiState, actions)
    }
}
