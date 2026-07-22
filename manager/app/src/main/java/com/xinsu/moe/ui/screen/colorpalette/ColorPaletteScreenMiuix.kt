package com.xinsu.moe.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.ReorderableColumn
import com.xinsu.moe.ui.component.miuix.MiuixGlassCard
import com.xinsu.moe.ui.screen.home.HomeCardShape
import com.xinsu.moe.ui.screen.home.homeCardLabel
import com.xinsu.moe.ui.screen.home.homeCardShapeLabel
import com.xinsu.moe.ui.screen.home.serializeHomeCardOrder
import com.xinsu.moe.ui.screen.home.serializeHomeCardShapes
import com.xinsu.moe.ui.component.miuix.ScaleDialog
import com.xinsu.moe.ui.theme.BackgroundStyle
import com.xinsu.moe.ui.theme.BuiltInThemes
import com.xinsu.moe.ui.theme.LocalEnableBlur
import com.xinsu.moe.ui.theme.ThemeBundle
import com.xinsu.moe.ui.theme.importBackgroundImage
import com.xinsu.moe.ui.theme.importCardImage
import com.xinsu.moe.ui.theme.keyColorOptions
import com.xinsu.moe.ui.theme.legacySelectableKawaiiPalettes
import com.xinsu.moe.ui.theme.scaffoldContainerColor
import com.xinsu.moe.ui.theme.usesImage
import com.xinsu.moe.ui.theme.LocalBackgroundStyle
import com.xinsu.moe.ui.theme.isActive
import kotlinx.coroutines.launch
import com.xinsu.moe.ui.util.BlurredBar
import com.xinsu.moe.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import com.xinsu.moe.ui.component.miuix.EnergyMiuixSwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ColorPaletteScreenMiuix(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlurState = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlurState)
    val blurActive = backdrop != null
    val barColor = if (blurActive || LocalBackgroundStyle.current.isActive) Color.Transparent else colorScheme.surface
    val context = LocalContext.current
    val imageImportScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            imageImportScope.launch {
                val stored = importBackgroundImage(context, uri)
                if (stored != null) actions.onSetBackgroundImageUri(stored)
            }
        }
    }
    val cardImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            imageImportScope.launch {
                val stored = importCardImage(context, uri)
                if (stored != null) actions.onSetCardImageUri(stored)
            }
        }
    }
    val exportThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ThemeBundle.MIME)
    ) { uri -> if (uri != null) actions.onExportTheme(uri) }
    val importThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) actions.onImportTheme(uri) }
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()

    Scaffold(
        containerColor = scaffoldContainerColor(colorScheme.surface),
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        IconButton(
                            onClick = actions.onBack
                        ) {
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
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val showScaleDialog = rememberSaveable { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(32.dp))
                    ThemeStudioMiuix(
                        currentThemeId = uiState.themePreset,
                        isDark = isDark,
                        onApply = actions.onApplyThemeTemplate,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val themeItems = listOf(
                        stringResource(id = R.string.settings_theme_mode_system),
                        stringResource(id = R.string.settings_theme_mode_light),
                        stringResource(id = R.string.settings_theme_mode_dark),
                    )
                    TabRow(
                        tabs = themeItems,
                        selectedTabIndex = (if (uiState.themeMode >= 3) uiState.themeMode - 3 else uiState.themeMode).coerceIn(0, 2),
                        onTabSelected = { index ->
                            actions.onSetThemeMode(index)
                        },
                        height = 48.dp,
                    )

                    Text(
                        text = stringResource(R.string.theme_fine_tune),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 24.dp),
                    )
                    Text(
                        text = stringResource(R.string.theme_fine_tune_summary),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )

                    MiuixGlassCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        val presetItems = listOf(
                            stringResource(id = R.string.theme_preset_none),
                            stringResource(id = R.string.theme_preset_sakura),
                            stringResource(id = R.string.theme_preset_mint),
                            stringResource(id = R.string.theme_preset_lavender),
                            stringResource(id = R.string.theme_preset_cyber),
                            stringResource(id = R.string.theme_preset_obsidian),
                            stringResource(id = R.string.theme_preset_mica),
                            stringResource(id = R.string.theme_preset_ember),
                            stringResource(id = R.string.theme_preset_jade),
                            stringResource(id = R.string.theme_preset_sakuravn),
                            stringResource(id = R.string.theme_preset_snow),
                            stringResource(id = R.string.theme_preset_moonlit),
                        )
                        val presetValues = legacySelectableKawaiiPalettes
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_theme_preset),
                            items = presetItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_theme_preset),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = presetValues.indexOf(state.currentThemePreset).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                actions.onSetThemePreset(presetValues[index].name)
                            }
                        )

                        val bgItems = listOf(
                            stringResource(id = R.string.background_style_none),
                            stringResource(id = R.string.background_style_gradient),
                            stringResource(id = R.string.background_style_image),
                            stringResource(id = R.string.background_style_stage),
                        )
                        val bgValues = BackgroundStyle.entries
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_background_style),
                            items = bgItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Gradient,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_background_style),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = bgValues.indexOf(state.currentBackgroundStyle).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                actions.onSetBackgroundStyle(bgValues[index].name)
                            }
                        )

                        AnimatedVisibility(
                            visible = state.currentBackgroundStyle.usesImage
                        ) {
                            Column {
                                ArrowPreference(
                                    title = stringResource(id = R.string.settings_background_image),
                                    summary = stringResource(id = R.string.settings_background_image_summary),
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.Image,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(id = R.string.settings_background_image),
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    endActions = {
                                        if (state.currentBackgroundImageUri.isNotBlank()) {
                                            IconButton(onClick = { actions.onSetBackgroundImageUri("") }) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = stringResource(id = R.string.image_clear),
                                                    tint = colorScheme.onSurfaceVariantActions,
                                                )
                                            }
                                        }
                                    },
                                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                )
                                ImageOpacityPreference(
                                    title = stringResource(id = R.string.settings_image_opacity),
                                    value = state.currentBackgroundImageAlpha,
                                    onChange = actions.onSetBackgroundImageAlpha,
                                )
                                ImagePositionPreference(
                                    value = state.currentBackgroundImageAlign,
                                    onChange = actions.onSetBackgroundImageAlign,
                                )
                            }
                        }

                        ArrowPreference(
                            title = stringResource(id = R.string.settings_card_image),
                            summary = stringResource(id = R.string.settings_card_image_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Image,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_card_image),
                                    tint = colorScheme.onBackground
                                )
                            },
                            endActions = {
                                if (state.currentCardImageUri.isNotBlank()) {
                                    IconButton(onClick = { actions.onSetCardImageUri("") }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(id = R.string.image_clear),
                                            tint = colorScheme.onSurfaceVariantActions,
                                        )
                                    }
                                }
                            },
                            onClick = { cardImagePickerLauncher.launch(arrayOf("image/*")) },
                        )
                        ImageOpacityPreference(
                            title = stringResource(id = R.string.settings_image_opacity),
                            value = state.currentCardImageAlpha,
                            onChange = actions.onSetCardImageAlpha,
                        )
                        ImagePositionPreference(
                            value = state.currentCardImageAlign,
                            onChange = actions.onSetCardImageAlign,
                        )
                        ImageOpacityPreference(
                            title = stringResource(id = R.string.settings_card_opacity),
                            value = state.currentCardOpacity,
                            onChange = actions.onSetCardOpacity,
                        )

                        ArrowPreference(
                            title = stringResource(id = R.string.settings_export_theme),
                            summary = stringResource(id = R.string.settings_export_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Upload,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_export_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = { exportThemeLauncher.launch(ThemeBundle.DEFAULT_FILENAME) },
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_import_theme),
                            summary = stringResource(id = R.string.settings_import_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Download,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_import_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = { importThemeLauncher.launch(arrayOf("*/*")) },
                        )
                    }

                    MiuixGlassCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        EnergyMiuixSwitchPreference(
                            title = stringResource(id = R.string.settings_glass_cards),
                            summary = stringResource(id = R.string.settings_glass_cards_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.BlurOn,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_glass_cards),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = state.currentCardsGlass,
                            onCheckedChange = actions.onSetCardsGlass,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_home_card_layout),
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_home_card_layout_summary),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        ReorderableColumn(
                            items = state.currentHomeCardOrder,
                            keyOf = { it.name },
                            onMove = { actions.onSetHomeCardOrder(serializeHomeCardOrder(it)) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) { id, dragHandle, dragging ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = null,
                                    tint = if (dragging) colorScheme.primary else colorScheme.onSurfaceVariantActions,
                                    modifier = dragHandle.padding(end = 12.dp),
                                )
                                Text(
                                    text = homeCardLabel(id),
                                    modifier = Modifier.weight(1f),
                                    color = colorScheme.onSurface,
                                )
                                val shape = state.currentHomeCardShapes[id] ?: HomeCardShape.Default
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(colorScheme.secondaryContainer)
                                        .clickable {
                                            val next = state.currentHomeCardShapes.toMutableMap()
                                                .apply { put(id, shape.next()) }
                                            actions.onSetHomeCardShapes(serializeHomeCardShapes(next))
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = homeCardShapeLabel(shape),
                                        color = colorScheme.onSecondaryContainer,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }

                    MiuixGlassCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        EnergyMiuixSwitchPreference(
                            title = stringResource(id = R.string.settings_monet),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_monet),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.miuixMonet,
                            onCheckedChange = {
                                actions.onSetMiuixMonet(it)
                            }
                        )

                        AnimatedVisibility(
                            visible = uiState.miuixMonet
                        ) {
                            Column {
                                val colorItems = listOf(
                                    stringResource(id = R.string.settings_key_color_default),
                                    stringResource(id = R.string.color_red),
                                    stringResource(id = R.string.color_pink),
                                    stringResource(id = R.string.color_purple),
                                    stringResource(id = R.string.color_deep_purple),
                                    stringResource(id = R.string.color_indigo),
                                    stringResource(id = R.string.color_blue),
                                    stringResource(id = R.string.color_cyan),
                                    stringResource(id = R.string.color_teal),
                                    stringResource(id = R.string.color_green),
                                    stringResource(id = R.string.color_yellow),
                                    stringResource(id = R.string.color_amber),
                                    stringResource(id = R.string.color_orange),
                                    stringResource(id = R.string.color_brown),
                                    stringResource(id = R.string.color_blue_grey),
                                    stringResource(id = R.string.color_sakura),
                                )
                                val colorValues = listOf(0) + keyColorOptions
                                OverlayDropdownPreference(
                                    title = stringResource(id = R.string.settings_key_color),
                                    items = colorItems,
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.Colorize,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(id = R.string.settings_key_color),
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    selectedIndex = colorValues.indexOf(uiState.keyColor).takeIf { it >= 0 } ?: 0,
                                    onSelectedIndexChange = { index ->
                                        actions.onSetKeyColor(colorValues[index])
                                    }
                                )

                                AnimatedVisibility(
                                    visible = uiState.keyColor != 0
                                ) {
                                    Column {
                                        val styles = PaletteStyle.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_style),
                                            startAction = {
                                                Icon(
                                                    Icons.Rounded.Style,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    contentDescription = stringResource(id = R.string.settings_color_style),
                                                    tint = colorScheme.onBackground
                                                )
                                            },
                                            items = styles.map { it.name },
                                            selectedIndex = styles.indexOfFirst { it.name == uiState.colorStyle }.coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                actions.onSetColorStyle(styles[index].name)
                                            }
                                        )

                                        val specs = ColorSpec.SpecVersion.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_spec),
                                            startAction = {
                                                Icon(
                                                    Icons.Rounded.DesignServices,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    contentDescription = stringResource(id = R.string.settings_color_spec),
                                                    tint = colorScheme.onBackground
                                                )
                                            },
                                            items = specs.map { it.name },
                                            selectedIndex = specs.indexOfFirst { it.name == uiState.colorSpec }.coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                actions.onSetColorSpec(specs[index].name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    MiuixGlassCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            EnergyMiuixSwitchPreference(
                                title = stringResource(id = R.string.settings_enable_blur),
                                summary = stringResource(id = R.string.settings_enable_blur_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BlurOn,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_blur),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableBlur,
                                onCheckedChange = {
                                    actions.onSetEnableBlur(it)
                                }
                            )
                        }
                        EnergyMiuixSwitchPreference(
                            title = stringResource(id = R.string.settings_floating_bottom_bar),
                            summary = stringResource(id = R.string.settings_floating_bottom_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.CallToAction,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_floating_bottom_bar),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableFloatingBottomBar,
                            onCheckedChange = {
                                actions.onSetEnableFloatingBottomBar(it)
                            }
                        )
                        AnimatedVisibility(visible = uiState.enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            EnergyMiuixSwitchPreference(
                                title = stringResource(id = R.string.settings_enable_glass),
                                summary = stringResource(id = R.string.settings_enable_glass_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.WaterDrop,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_glass),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableFloatingBottomBarBlur,
                                onCheckedChange = {
                                    actions.onSetEnableFloatingBottomBarBlur(it)
                                }
                            )
                        }
                    }

                    MiuixGlassCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            EnergyMiuixSwitchPreference(
                                title = stringResource(id = R.string.settings_enable_predictive_back),
                                summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                startAction = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.MenuOpen,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_predictive_back),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enablePredictiveBack,
                                onCheckedChange = {
                                    actions.onSetEnablePredictiveBack(it)
                                }
                            )
                        }

                        var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_page_scale),
                            summary = stringResource(id = R.string.settings_page_scale_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.AspectRatio,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_page_scale),
                                    tint = colorScheme.onBackground
                                )
                            },
                            endActions = {
                                Text(
                                    text = "${(sliderValue * 100).toInt()}%",
                                    color = colorScheme.onSurfaceVariantActions,
                                )
                            },
                            onClick = { showScaleDialog.value = !showScaleDialog.value },
                            holdDownState = showScaleDialog.value,
                            bottomAction = {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sliderValue = it
                                    },
                                    onValueChangeFinished = {
                                        actions.onSetPageScale(sliderValue)
                                    },
                                    valueRange = 0.8f..1.1f,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                        )
                        ScaleDialog(
                            show = showScaleDialog.value,
                            onDismissRequest = { showScaleDialog.value = false },
                            volumeState = { uiState.pageScale },
                            onVolumeChange = {
                                actions.onSetPageScale(it)
                            }
                        )
                    }
                }
                item {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() +
                                    12.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeStudioMiuix(
    currentThemeId: String,
    isDark: Boolean,
    onApply: (String) -> Unit,
) {
    val artworkThemes = remember { BuiltInThemes.all.filter { it.tokenBundleId != null } }
    val initialId = currentThemeId.takeIf { id -> artworkThemes.any { it.id == id } }
        ?: artworkThemes.first().id
    var previewId by rememberSaveable(currentThemeId) { mutableStateOf(initialId) }
    val preview = artworkThemes.firstOrNull { it.id == previewId } ?: artworkThemes.first()
    val isApplied = currentThemeId == preview.id
    var filterKey by rememberSaveable { mutableStateOf("all") }
    val selectedFilter = themeGalleryFilters.firstOrNull { (it?.name ?: "all") == filterKey }
    val visibleThemes = artworkThemes.filter { selectedFilter == null || it.galleryMood == selectedFilter }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.theme_studio_title),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = stringResource(R.string.theme_studio_summary),
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        MiuixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ThemeArtworkImage(
                    template = preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomEnd = 0.dp,
                                bottomStart = 0.dp,
                            )
                        ),
                    bottomScrim = true,
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(preview.nameRes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isApplied) colorScheme.surfaceVariant else colorScheme.primary)
                                .clickable(enabled = !isApplied) { onApply(preview.id) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = stringResource(if (isApplied) R.string.theme_applied else R.string.theme_apply),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isApplied) colorScheme.onSurfaceVariantActions else colorScheme.onPrimary,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.swatches(isDark).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(swatch)
                                    .border(1.dp, colorScheme.outline, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                    preview.summaryRes?.let { summaryRes ->
                        Text(
                            text = stringResource(summaryRes),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.theme_gallery_count),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(themeGalleryFilters, key = { it?.name ?: "all" }) { mood ->
                val selected = mood == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainer)
                        .border(
                            1.dp,
                            if (selected) colorScheme.secondary else colorScheme.outline,
                            RoundedCornerShape(18.dp),
                        )
                        .clickable {
                            filterKey = mood?.name ?: "all"
                            if (mood != null) {
                                artworkThemes.firstOrNull { it.galleryMood == mood }?.let { previewId = it.id }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = themeGalleryFilterLabel(mood),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visibleThemes, key = { it.id }) { template ->
                val selected = template.id == preview.id
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colorScheme.surfaceContainer)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) colorScheme.primary else colorScheme.outline,
                            shape = RoundedCornerShape(22.dp),
                        )
                        .clickable { previewId = template.id },
                ) {
                    ThemeArtworkImage(
                        template = template,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp),
                        bottomScrim = true,
                    )
                    Text(
                        text = stringResource(template.nameRes),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) colorScheme.primary else colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}

// Opacity (0-100%) control for a decorative image / card, styled like the page-scale slider row.
@Composable
private fun ImageOpacityPreference(
    title: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    ArrowPreference(
        title = title,
        startAction = {
            Icon(
                Icons.Rounded.Image,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = title,
                tint = colorScheme.onBackground
            )
        },
        endActions = {
            Text(
                text = "${sliderValue.toInt()}%",
                color = colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = {},
        bottomAction = {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onChange(sliderValue.toInt()) },
                valueRange = 0f..100f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )
}

// Left / Center / Right horizontal position control for a decorative image.
@Composable
private fun ImagePositionPreference(
    value: Int,
    onChange: (Int) -> Unit,
) {
    val items = listOf(
        stringResource(id = R.string.image_position_left),
        stringResource(id = R.string.image_position_center),
        stringResource(id = R.string.image_position_right),
    )
    OverlayDropdownPreference(
        title = stringResource(id = R.string.settings_image_position),
        items = items,
        startAction = {
            Icon(
                Icons.Rounded.AspectRatio,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(id = R.string.settings_image_position),
                tint = colorScheme.onBackground
            )
        },
        selectedIndex = value.coerceIn(0, 2),
        onSelectedIndexChange = { onChange(it) }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    val dynamicCs = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )

    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 12.sp,
                        color = textColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentCardColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(.1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(.1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                }

            }

            if (enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f)
                                else navBarColor
                            )
                            .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) iconColor else textColor)
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
