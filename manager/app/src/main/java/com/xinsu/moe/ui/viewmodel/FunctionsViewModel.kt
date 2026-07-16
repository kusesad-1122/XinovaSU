package com.xinsu.moe.ui.viewmodel

import android.system.Os
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.xinsu.moe.Natives
import com.xinsu.moe.ui.screen.functions.FunctionsUiState
import com.xinsu.moe.ui.util.blHideIsEnabled
import com.xinsu.moe.ui.util.blHideSetEnabled
import com.xinsu.moe.ui.util.execKsud
import com.xinsu.moe.ui.util.getFeatureStatus
import com.xinsu.moe.ui.util.netIsolateRead
import com.xinsu.moe.ui.util.netIsolateSave
import com.xinsu.moe.ui.util.pathHideRead
import com.xinsu.moe.ui.util.pathHideSave
import com.xinsu.moe.ui.util.umountReadPaths
import com.xinsu.moe.ui.util.umountSavePaths
import com.xinsu.moe.ui.util.utsSpoofRead
import com.xinsu.moe.ui.util.utsSpoofReset
import com.xinsu.moe.ui.util.utsSpoofSet

class FunctionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FunctionsUiState())
    val uiState: StateFlow<FunctionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val blHide = runCatching { blHideIsEnabled() }.getOrDefault(false)
            val umountAvailable = runCatching { getFeatureStatus("kernel_umount") == "supported" }.getOrDefault(false)
            val umountEnabled = runCatching { Natives.isKernelUmountEnabled() }.getOrDefault(false)
            val umountPaths = runCatching { umountReadPaths() }.getOrDefault(emptyList())
            val uts = runCatching { utsSpoofRead() }.getOrNull()
            val pathHide = runCatching { pathHideRead() }.getOrNull()
            val netIsolate = runCatching { netIsolateRead() }.getOrNull()

            _uiState.update {
                it.copy(
                    blHideEnabled = blHide,
                    umountAvailable = umountAvailable,
                    umountEnabled = umountEnabled,
                    umountPaths = umountPaths.joinToString("\n"),
                    kernelSpoofEnabled = uts?.enabled ?: false,
                    kernelSpoofRelease = uts?.release.orEmpty(),
                    kernelSpoofVersion = uts?.version.orEmpty(),
                    pathHideEnabled = pathHide?.enabled ?: false,
                    pathHidePaths = pathHide?.paths?.joinToString("\n").orEmpty(),
                    pathHideUids = pathHide?.uids ?: emptySet(),
                    netIsolateEnabled = netIsolate?.enabled ?: false,
                    netIsolateUids = netIsolate?.uids ?: emptySet(),
                )
            }
        }
    }

    // bl-hide -------------------------------------------------------------

    fun setBlHideEnabled(enabled: Boolean) {
        _uiState.update { it.copy(blHideEnabled = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            blHideSetEnabled(enabled)
        }
    }

    // umount --------------------------------------------------------------

    fun setUmountEnabled(enabled: Boolean) {
        _uiState.update { it.copy(umountEnabled = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            if (Natives.setKernelUmountEnabled(enabled)) {
                execKsud("feature save", true)
            }
        }
    }

    fun setUmountPaths(text: String) {
        _uiState.update { it.copy(umountPaths = text) }
    }

    fun saveUmountPaths() {
        val paths = splitLines(_uiState.value.umountPaths)
        viewModelScope.launch(Dispatchers.IO) {
            umountSavePaths(paths)
        }
    }

    // kernel spoof --------------------------------------------------------

    fun setKernelSpoofEnabled(enabled: Boolean) {
        _uiState.update { it.copy(kernelSpoofEnabled = enabled) }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                utsSpoofSet(state.kernelSpoofRelease, state.kernelSpoofVersion)
            } else {
                utsSpoofReset()
            }
        }
    }

    fun setKernelSpoofRelease(value: String) {
        _uiState.update { it.copy(kernelSpoofRelease = value) }
    }

    fun setKernelSpoofVersion(value: String) {
        _uiState.update { it.copy(kernelSpoofVersion = value) }
    }

    fun saveKernelSpoof() {
        _uiState.update { it.copy(kernelSpoofEnabled = true) }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            utsSpoofSet(state.kernelSpoofRelease, state.kernelSpoofVersion)
        }
    }

    fun restoreKernelSpoofDefaults() {
        val uname = Os.uname()
        _uiState.update {
            it.copy(
                kernelSpoofRelease = uname.release,
                kernelSpoofVersion = uname.version,
            )
        }
    }

    // path hide -----------------------------------------------------------

    fun setPathHideEnabled(enabled: Boolean) {
        _uiState.update { it.copy(pathHideEnabled = enabled) }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            pathHideSave(enabled, splitLines(state.pathHidePaths), state.pathHideUids)
        }
    }

    fun setPathHidePaths(text: String) {
        _uiState.update { it.copy(pathHidePaths = text) }
    }

    fun savePathHide() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            pathHideSave(state.pathHideEnabled, splitLines(state.pathHidePaths), state.pathHideUids)
        }
    }

    fun togglePathHideUid(uid: Int) {
        _uiState.update {
            val newSet = if (uid in it.pathHideUids) it.pathHideUids - uid else it.pathHideUids + uid
            it.copy(pathHideUids = newSet)
        }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            pathHideSave(state.pathHideEnabled, splitLines(state.pathHidePaths), state.pathHideUids)
        }
    }

    // net isolate ---------------------------------------------------------

    fun setNetIsolateEnabled(enabled: Boolean) {
        _uiState.update { it.copy(netIsolateEnabled = enabled) }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            netIsolateSave(enabled, state.netIsolateUids)
        }
    }

    fun toggleNetIsolateUid(uid: Int) {
        _uiState.update {
            val newSet = if (uid in it.netIsolateUids) it.netIsolateUids - uid else it.netIsolateUids + uid
            it.copy(netIsolateUids = newSet)
        }
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            netIsolateSave(state.netIsolateEnabled, state.netIsolateUids)
        }
    }

    private fun splitLines(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotBlank() }
}
