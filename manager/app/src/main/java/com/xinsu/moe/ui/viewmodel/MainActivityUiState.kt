package com.xinsu.moe.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.xinsu.moe.ui.UiMode
import com.xinsu.moe.ui.screen.home.HomeCardId
import com.xinsu.moe.ui.screen.home.HomeCardShape
import com.xinsu.moe.ui.theme.AppSettings
import com.xinsu.moe.ui.theme.BackgroundStyle

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val backgroundStyle: BackgroundStyle,
    val backgroundImageUri: String,
    val backgroundImageAlpha: Int,
    val backgroundImageAlign: Int,
    val cardImageUri: String,
    val cardImageAlpha: Int,
    val cardImageAlign: Int,
    val cardOpacity: Int,
    val homeCardOrder: List<HomeCardId>,
    val homeCardShapes: Map<HomeCardId, HomeCardShape>,
    val cardsGlass: Boolean,
    val uiMode: UiMode,
)
