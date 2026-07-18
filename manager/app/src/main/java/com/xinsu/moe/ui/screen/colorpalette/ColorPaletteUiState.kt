package com.xinsu.moe.ui.screen.colorpalette

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.screen.home.HomeCardId
import com.xinsu.moe.ui.screen.home.HomeCardShape
import com.xinsu.moe.ui.screen.settings.SettingsUiState
import com.xinsu.moe.ui.theme.BackgroundStyle
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette

@Immutable
data class ColorPaletteUiState(
    val uiState: SettingsUiState,
    val currentColorMode: ColorMode,
    val currentPaletteStyle: PaletteStyle,
    val currentColorSpec: ColorSpec.SpecVersion,
    val currentThemePreset: KawaiiPalette,
    val currentBackgroundStyle: BackgroundStyle,
    val currentBackgroundImageUri: String,
    val currentBackgroundImageAlpha: Int,
    val currentBackgroundImageAlign: Int,
    val currentCardImageUri: String,
    val currentCardImageAlpha: Int,
    val currentCardImageAlign: Int,
    val currentCardOpacity: Int,
    val currentHomeCardOrder: List<HomeCardId>,
    val currentHomeCardShapes: Map<HomeCardId, HomeCardShape>,
    val currentCardsGlass: Boolean,
)

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetMiuixMonet: (Boolean) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (String) -> Unit,
    val onSetColorSpec: (String) -> Unit,
    val onSetThemePreset: (String) -> Unit,
    val onApplyThemeTemplate: (String) -> Unit,
    val onSetBackgroundStyle: (String) -> Unit,
    val onSetBackgroundImageUri: (String) -> Unit,
    val onSetBackgroundImageAlpha: (Int) -> Unit,
    val onSetBackgroundImageAlign: (Int) -> Unit,
    val onSetCardImageUri: (String) -> Unit,
    val onSetCardImageAlpha: (Int) -> Unit,
    val onSetCardImageAlign: (Int) -> Unit,
    val onSetCardOpacity: (Int) -> Unit,
    val onSetHomeCardOrder: (String) -> Unit,
    val onSetHomeCardShapes: (String) -> Unit,
    val onSetCardsGlass: (Boolean) -> Unit,
    val onExportTheme: (Uri) -> Unit,
    val onImportTheme: (Uri) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    val onSetEnablePredictiveBack: (Boolean) -> Unit,
    val onSetPageScale: (Float) -> Unit,
)
