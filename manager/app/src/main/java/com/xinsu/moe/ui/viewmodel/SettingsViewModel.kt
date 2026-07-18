package com.xinsu.moe.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.system.OsConstants
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.xinsu.moe.Natives
import com.xinsu.moe.R
import com.xinsu.moe.data.repository.SettingsRepository
import com.xinsu.moe.data.repository.SettingsRepositoryImpl
import com.xinsu.moe.ksuApp
import com.xinsu.moe.ui.screen.settings.SettingsUiState
import com.xinsu.moe.ui.theme.BuiltInThemes
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.ThemeBundle
import com.xinsu.moe.ui.theme.toApplicationPlan

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val checkUpdate = repo.checkUpdate
            val checkModuleUpdate = repo.checkModuleUpdate
            val themeMode = repo.themeMode
            val miuixMonet = repo.miuixMonet
            val keyColor = repo.keyColor
            val enablePredictiveBack = repo.enablePredictiveBack
            val enableBlur = repo.enableBlur
            val enableFloatingBottomBar = repo.enableFloatingBottomBar
            val enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur
            val pageScale = repo.pageScale
            val enableWebDebugging = repo.enableWebDebugging
            val colorStyle = repo.colorStyle
            val colorSpec = repo.colorSpec
            val themePreset = repo.themePreset
            val backgroundStyle = repo.backgroundStyle
            val backgroundImageUri = repo.backgroundImageUri
            val backgroundImageAlpha = repo.backgroundImageAlpha
            val backgroundImageAlign = repo.backgroundImageAlign
            val cardImageUri = repo.cardImageUri
            val cardImageAlpha = repo.cardImageAlpha
            val cardImageAlign = repo.cardImageAlign
            val cardOpacity = repo.cardOpacity
            val homeCardOrder = repo.homeCardOrder
            val homeCardShapes = repo.homeCardShapes
            val cardsGlass = repo.cardsGlass
            val isLkmMode = repo.isLkmMode()

            // Async loading for natives/features
            val suCompatStatus = repo.getSuCompatStatus()
            val suCompatPersistValue = repo.getSuCompatPersistValue()
            val isSuEnabled = repo.isSuEnabled()

            val suCompatMode = if (suCompatPersistValue == 0L) 2 else if (!isSuEnabled) 1 else 0

            val kernelUmountStatus = repo.getKernelUmountStatus()
            val isKernelUmountEnabled = repo.isKernelUmountEnabled()
            val selinuxHideStatus = repo.getSelinuxHideStatus()
            val isSelinuxHideEnabled = repo.isSelinuxHideEnabled()
            val sulogStatus = repo.getSulogStatus()
            val isSulogEnabled = repo.getSulogPersistValue() == 1L
            val adbRootStatus = repo.getAdbRootStatus()
            val isAdbRootEnabled = repo.getAdbRootPersistValue() == 1L
            val isDefaultUmountModules = repo.isDefaultUmountModules()
            val uiMode = repo.uiMode
            val autoJailbreak = repo.autoJailbreak
            val isLateLoadMode = Natives.isLateLoadMode

            _uiState.update {
                it.copy(
                    uiMode = uiMode,
                    checkUpdate = checkUpdate,
                    checkModuleUpdate = checkModuleUpdate,
                    themeMode = themeMode,
                    miuixMonet = miuixMonet,
                    keyColor = keyColor,
                    enablePredictiveBack = enablePredictiveBack,
                    enableBlur = enableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    pageScale = pageScale,
                    enableWebDebugging = enableWebDebugging,
                    colorStyle = colorStyle,
                    colorSpec = colorSpec,
                    themePreset = themePreset,
                    backgroundStyle = backgroundStyle,
                    backgroundImageUri = backgroundImageUri,
                    backgroundImageAlpha = backgroundImageAlpha,
                    backgroundImageAlign = backgroundImageAlign,
                    cardImageUri = cardImageUri,
                    cardImageAlpha = cardImageAlpha,
                    cardImageAlign = cardImageAlign,
                    cardOpacity = cardOpacity,
                    homeCardOrder = homeCardOrder,
                    homeCardShapes = homeCardShapes,
                    cardsGlass = cardsGlass,
                    suCompatStatus = suCompatStatus,
                    suCompatMode = suCompatMode,
                    isSuEnabled = isSuEnabled,
                    adbRootStatus = adbRootStatus,
                    isAdbRootEnabled = isAdbRootEnabled,
                    kernelUmountStatus = kernelUmountStatus,
                    isKernelUmountEnabled = isKernelUmountEnabled,
                    selinuxHideStatus = selinuxHideStatus,
                    isSelinuxHideEnabled = isSelinuxHideEnabled,
                    sulogStatus = sulogStatus,
                    isSulogEnabled = isSulogEnabled,
                    isDefaultUmountModules = isDefaultUmountModules,
                    isLkmMode = isLkmMode,
                    autoJailbreak = autoJailbreak,
                    isLateLoadMode = isLateLoadMode,
                )
            }
        }
    }

    fun setCheckUpdate(enabled: Boolean) {
        repo.checkUpdate = enabled
        _uiState.update { it.copy(checkUpdate = enabled) }
    }

    fun setUiMode(mode: String) {
        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode

        val newThemeMode = when (oldMode) {
            "material" if mode == "miuix" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
                if (repo.miuixMonet && !colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toMonetMode()
                } else if (!repo.miuixMonet && colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toNonMonetMode()
                } else baseMode
            }

            "miuix" if mode == "material" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                if (colorMode.isMonet) {
                    colorMode.toNonMonetMode()
                } else currentThemeMode
            }

            else -> currentThemeMode
        }

        repo.uiMode = mode
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(uiMode = mode, themeMode = newThemeMode) }
    }

    fun setCheckModuleUpdate(enabled: Boolean) {
        repo.checkModuleUpdate = enabled
        _uiState.update { it.copy(checkModuleUpdate = enabled) }
    }

    fun setThemeMode(mode: Int) {
        val currentUiMode = repo.uiMode
        val effectiveMode = if (currentUiMode == "miuix" && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode) }
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value) }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(miuixMonet = enabled, themeMode = newThemeMode) }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun setThemePreset(preset: String) {
        repo.themePreset = preset
        _uiState.update { it.copy(themePreset = preset) }
    }

    fun setBackgroundStyle(style: String) {
        repo.backgroundStyle = style
        _uiState.update { it.copy(backgroundStyle = style) }
    }

    // Apply a complete built-in theme: palette + its designed-for background (+ light/dark it was
    // designed for), in one tap.
    fun applyThemeTemplate(id: String) {
        val template = BuiltInThemes.byId(id) ?: return
        val plan = template.toApplicationPlan()
        val artworkUri = plan.artworkResourceName?.let { resourceName ->
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(ksuApp.packageName)
                .appendPath("drawable")
                .appendPath(resourceName.substringBeforeLast('.'))
                .build()
                .toString()
        }

        repo.themePreset = plan.themePreset
        repo.backgroundStyle = plan.backgroundStyle
        plan.preferredColorMode?.let { repo.themeMode = it }
        artworkUri?.let {
            repo.backgroundImageUri = it
            repo.backgroundImageAlpha = plan.backgroundImageAlpha ?: 100
        }
        plan.backgroundImageAlign?.let { repo.backgroundImageAlign = it }
        plan.cardOpacity?.let { repo.cardOpacity = it }
        _uiState.update {
            it.copy(
                themePreset = plan.themePreset,
                backgroundStyle = plan.backgroundStyle,
                themeMode = plan.preferredColorMode ?: it.themeMode,
                backgroundImageUri = artworkUri ?: it.backgroundImageUri,
                backgroundImageAlpha = plan.backgroundImageAlpha ?: it.backgroundImageAlpha,
                backgroundImageAlign = plan.backgroundImageAlign ?: it.backgroundImageAlign,
                cardOpacity = plan.cardOpacity ?: it.cardOpacity,
            )
        }
    }

    fun exportTheme(uri: Uri) {
        viewModelScope.launch {
            val preset = repo.themePreset
            val name = if (preset.isNotEmpty() && preset != "None") preset else "XinovaSU"
            val result = ThemeBundle.export(ksuApp, repo, uri, name)
            val msg = if (result.isSuccess) R.string.theme_export_success else R.string.theme_export_failed
            Toast.makeText(ksuApp, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun importTheme(uri: Uri) {
        viewModelScope.launch {
            ThemeBundle.import(ksuApp, repo, uri)
                .onSuccess { name ->
                    refresh()
                    Toast.makeText(
                        ksuApp,
                        ksuApp.getString(R.string.theme_import_success, name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure {
                    Toast.makeText(ksuApp, R.string.theme_import_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun setBackgroundImageUri(uri: String) {
        repo.backgroundImageUri = uri
        _uiState.update { it.copy(backgroundImageUri = uri) }
    }

    // The card illustration is applied live through the MainActivity prefs-change listener
    // (LocalCardImageUri); the uiState copy only backs the theme screen's clear button.
    fun setCardImageUri(uri: String) {
        repo.cardImageUri = uri
        _uiState.update { it.copy(cardImageUri = uri) }
    }

    fun setCardOpacity(opacity: Int) {
        val v = opacity.coerceIn(0, 100)
        repo.cardOpacity = v
        _uiState.update { it.copy(cardOpacity = v) }
    }

    // Home card order/shapes apply live through the MainActivity prefs-change listener
    // (LocalHomeCardOrder / LocalHomeCardShapes); the uiState copy backs the layout editor.
    fun setHomeCardOrder(order: String) {
        repo.homeCardOrder = order
        _uiState.update { it.copy(homeCardOrder = order) }
    }

    fun setHomeCardShapes(shapes: String) {
        repo.homeCardShapes = shapes
        _uiState.update { it.copy(homeCardShapes = shapes) }
    }

    fun setCardsGlass(enabled: Boolean) {
        repo.cardsGlass = enabled
        _uiState.update { it.copy(cardsGlass = enabled) }
    }

    fun setBackgroundImageAlpha(alpha: Int) {
        val v = alpha.coerceIn(0, 100)
        repo.backgroundImageAlpha = v
        _uiState.update { it.copy(backgroundImageAlpha = v) }
    }

    fun setBackgroundImageAlign(align: Int) {
        val v = align.coerceIn(0, 2)
        repo.backgroundImageAlign = v
        _uiState.update { it.copy(backgroundImageAlign = v) }
    }

    fun setCardImageAlpha(alpha: Int) {
        val v = alpha.coerceIn(0, 100)
        repo.cardImageAlpha = v
        _uiState.update { it.copy(cardImageAlpha = v) }
    }

    fun setCardImageAlign(align: Int) {
        val v = align.coerceIn(0, 2)
        repo.cardImageAlign = v
        _uiState.update { it.copy(cardImageAlign = v) }
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.update { it.copy(enablePredictiveBack = enabled) }
    }

    fun setEnableBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale) }
    }

    fun setEnableWebDebugging(enabled: Boolean) {
        repo.enableWebDebugging = enabled
        _uiState.update { it.copy(enableWebDebugging = enabled) }
    }

    fun setSuCompatMode(mode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (mode) {
                0 -> if (repo.setSuEnabled(true)) {
                    repo.execKsudFeatureSave()
                    repo.setSuCompatModePref(0)
                    _uiState.update { it.copy(suCompatMode = 0, isSuEnabled = true) }
                }

                1 -> if (repo.setSuEnabled(true)) {
                    repo.execKsudFeatureSave()
                    if (repo.setSuEnabled(false)) {
                        // "Disable until reboot" implies it should be enabled on next boot.
                        // We set the preference to 0 (Enabled) to match the persistent state.
                        repo.setSuCompatModePref(0)
                        _uiState.update { it.copy(suCompatMode = 1, isSuEnabled = false) }
                    }
                }

                2 -> if (repo.setSuEnabled(false)) {
                    repo.execKsudFeatureSave()
                    repo.setSuCompatModePref(2)
                    _uiState.update { it.copy(suCompatMode = 2, isSuEnabled = false) }
                }
            }
        }
    }

    fun setKernelUmountEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setKernelUmountEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isKernelUmountEnabled = enabled) }
            }
        }
    }

    fun setSelinuxHideEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = repo.setSelinuxHideEnabled(enabled)
            repo.execKsudFeatureSave()
            _uiState.update { it.copy(isSelinuxHideEnabled = enabled) }
            when (status) {
                0 -> {}
                -OsConstants.EAGAIN -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ksuApp, R.string.settings_selinux_hide_reboot_required,
                            Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ksuApp, ksuApp.getString(R.string.settings_selinux_hide_failed, status),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun setAutoJailbreak(enabled: Boolean) {
        repo.autoJailbreak = enabled
        _uiState.update { it.copy(autoJailbreak = enabled) }
    }

    fun setSulogEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setSulogEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isSulogEnabled = enabled) }
            }
        }
    }

    fun setAdbRootEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setAdbRootEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isAdbRootEnabled = enabled) }
            }
        }
    }

    fun setDefaultUmountModules(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setDefaultUmountModules(enabled)) {
                _uiState.update { it.copy(isDefaultUmountModules = enabled) }
            }
        }
    }
}
