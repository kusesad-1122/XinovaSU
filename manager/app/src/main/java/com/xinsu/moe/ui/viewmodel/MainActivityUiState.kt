package com.xinsu.moe.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.xinsu.moe.ui.UiMode
import com.xinsu.moe.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)
