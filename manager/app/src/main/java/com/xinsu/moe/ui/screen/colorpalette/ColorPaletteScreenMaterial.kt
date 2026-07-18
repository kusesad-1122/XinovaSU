package com.xinsu.moe.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.ReorderableColumn
import com.xinsu.moe.ui.component.material.SegmentedColumn
import com.xinsu.moe.ui.component.material.SegmentedDropdownItem
import com.xinsu.moe.ui.component.material.SegmentedListItem
import com.xinsu.moe.ui.component.material.SegmentedSwitchItem
import com.xinsu.moe.ui.component.material.TonalCard
import com.xinsu.moe.ui.screen.home.HomeCardShape
import com.xinsu.moe.ui.screen.home.homeCardLabel
import com.xinsu.moe.ui.screen.home.homeCardShapeLabel
import com.xinsu.moe.ui.screen.home.serializeHomeCardOrder
import com.xinsu.moe.ui.screen.home.serializeHomeCardShapes
import com.xinsu.moe.ui.theme.BackgroundStyle
import com.xinsu.moe.ui.theme.BuiltInThemes
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.LocalBackgroundStyle
import com.xinsu.moe.ui.theme.isActive
import com.xinsu.moe.ui.theme.scaffoldContainerColor
import com.xinsu.moe.ui.theme.usesImage
import com.xinsu.moe.ui.theme.ThemeBundle
import com.xinsu.moe.ui.theme.importBackgroundImage
import com.xinsu.moe.ui.theme.importCardImage
import com.xinsu.moe.ui.theme.keyColorOptions
import com.xinsu.moe.ui.theme.legacySelectableKawaiiPalettes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val currentKeyColor = uiState.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    val haptic = LocalHapticFeedback.current
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

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        containerColor = scaffoldContainerColor(MaterialTheme.colorScheme.background),
        topBar = {
            val barColor = if (LocalBackgroundStyle.current.isActive) Color.Transparent else MaterialTheme.colorScheme.surface
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = actions.onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barColor,
                    scrolledContainerColor = barColor
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            ThemeStudioMaterial(
                currentThemeId = uiState.themePreset,
                isDark = isDark,
                onApply = actions.onApplyThemeTemplate,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    listOf(ColorMode.SYSTEM) to stringResource(R.string.settings_theme_mode_system),
                    listOf(ColorMode.LIGHT) to stringResource(R.string.settings_theme_mode_light),
                    listOf(ColorMode.DARK) to stringResource(R.string.settings_theme_mode_dark),
                    listOf(ColorMode.DARK_AMOLED) to stringResource(R.string.settings_theme_mode_dark)
                )

                options.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            ToggleButton(
                                checked = currentColorMode in modes,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        actions.onSetColorMode(modes.first())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    rowOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (modes.first()) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.theme_fine_tune),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.theme_fine_tune_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ColorButtonMaterial(
                            color = Color.Unspecified,
                            isSelected = currentKeyColor == 0,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = { actions.onSetKeyColor(0) },
                        )
                    }
                    items(keyColorOptions) { color ->
                        ColorButtonMaterial(
                            color = Color(color),
                            isSelected = currentKeyColor == color,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = { actions.onSetKeyColor(color) },
                        )
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val presetItems = listOf(
                                stringResource(R.string.theme_preset_none),
                                stringResource(R.string.theme_preset_sakura),
                                stringResource(R.string.theme_preset_mint),
                                stringResource(R.string.theme_preset_lavender),
                                stringResource(R.string.theme_preset_cyber),
                                stringResource(R.string.theme_preset_obsidian),
                                stringResource(R.string.theme_preset_mica),
                                stringResource(R.string.theme_preset_ember),
                                stringResource(R.string.theme_preset_jade),
                                stringResource(R.string.theme_preset_sakuravn),
                                stringResource(R.string.theme_preset_snow),
                                stringResource(R.string.theme_preset_moonlit),
                            )
                            val presetValues = legacySelectableKawaiiPalettes
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Palette,
                                title = stringResource(R.string.settings_theme_preset),
                                items = presetItems,
                                selectedIndex = presetValues.indexOf(state.currentThemePreset).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetThemePreset(presetValues[index].name)
                                }
                            )
                        },
                        {
                            val bgItems = listOf(
                                stringResource(R.string.background_style_none),
                                stringResource(R.string.background_style_gradient),
                                stringResource(R.string.background_style_image),
                                stringResource(R.string.background_style_stage),
                            )
                            val bgValues = BackgroundStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Gradient,
                                title = stringResource(R.string.settings_background_style),
                                items = bgItems,
                                selectedIndex = bgValues.indexOf(state.currentBackgroundStyle).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetBackgroundStyle(bgValues[index].name)
                                }
                            )
                        }
                    )
                )

                if (state.currentBackgroundStyle.usesImage) {
                    SegmentedColumn(
                        modifier = Modifier.padding(top = 4.dp),
                        content = listOf(
                            {
                                SegmentedListItem(
                                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Rounded.Image,
                                            contentDescription = stringResource(R.string.settings_background_image)
                                        )
                                    },
                                    headlineContent = {
                                        Text(stringResource(R.string.settings_background_image))
                                    },
                                    supportingContent = {
                                        Text(stringResource(R.string.settings_background_image_summary))
                                    },
                                    trailingContent = {
                                        if (state.currentBackgroundImageUri.isNotBlank()) {
                                            IconButton(onClick = { actions.onSetBackgroundImageUri("") }) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = stringResource(R.string.image_clear)
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        )
                    )
                    ImageOpacitySlider(
                        title = stringResource(R.string.settings_image_opacity),
                        value = state.currentBackgroundImageAlpha,
                        onChange = actions.onSetBackgroundImageAlpha,
                    )
                    ImagePositionSelector(
                        value = state.currentBackgroundImageAlign,
                        onChange = actions.onSetBackgroundImageAlign,
                    )
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = { cardImagePickerLauncher.launch(arrayOf("image/*")) },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.Image,
                                        contentDescription = stringResource(R.string.settings_card_image)
                                    )
                                },
                                headlineContent = { Text(stringResource(R.string.settings_card_image)) },
                                supportingContent = { Text(stringResource(R.string.settings_card_image_summary)) },
                                trailingContent = {
                                    if (state.currentCardImageUri.isNotBlank()) {
                                        IconButton(onClick = { actions.onSetCardImageUri("") }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.image_clear)
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    )
                )

                ImageOpacitySlider(
                    title = stringResource(R.string.settings_image_opacity),
                    value = state.currentCardImageAlpha,
                    onChange = actions.onSetCardImageAlpha,
                )
                ImagePositionSelector(
                    value = state.currentCardImageAlign,
                    onChange = actions.onSetCardImageAlign,
                )
                ImageOpacitySlider(
                    title = stringResource(R.string.settings_card_opacity),
                    value = state.currentCardOpacity,
                    onChange = actions.onSetCardOpacity,
                )

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.BlurOn,
                                title = stringResource(R.string.settings_glass_cards),
                                summary = stringResource(R.string.settings_glass_cards_summary),
                                checked = state.currentCardsGlass,
                                onCheckedChange = actions.onSetCardsGlass,
                            )
                        }
                    )
                )

                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_home_card_layout),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_home_card_layout_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                    )
                    ReorderableColumn(
                        items = state.currentHomeCardOrder,
                        keyOf = { it.name },
                        onMove = { actions.onSetHomeCardOrder(serializeHomeCardOrder(it)) },
                    ) { id, dragHandle, dragging ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 3.dp),
                            color = if (dragging) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = null,
                                    tint = if (dragging) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = dragHandle.padding(end = 12.dp),
                                )
                                Text(
                                    text = homeCardLabel(id),
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val shape = state.currentHomeCardShapes[id] ?: HomeCardShape.Default
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable {
                                            val next = state.currentHomeCardShapes.toMutableMap()
                                                .apply { put(id, shape.next()) }
                                            actions.onSetHomeCardShapes(serializeHomeCardShapes(next))
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = homeCardShapeLabel(shape),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = { exportThemeLauncher.launch(ThemeBundle.DEFAULT_FILENAME) },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.Upload,
                                        contentDescription = stringResource(R.string.settings_export_theme)
                                    )
                                },
                                headlineContent = { Text(stringResource(R.string.settings_export_theme)) },
                                supportingContent = { Text(stringResource(R.string.settings_export_theme_summary)) },
                            )
                        },
                        {
                            SegmentedListItem(
                                onClick = { importThemeLauncher.launch(arrayOf("*/*")) },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.Download,
                                        contentDescription = stringResource(R.string.settings_import_theme)
                                    )
                                },
                                headlineContent = { Text(stringResource(R.string.settings_import_theme)) },
                                supportingContent = { Text(stringResource(R.string.settings_import_theme_summary)) },
                            )
                        },
                    )
                )

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(colorStyle),
                                onItemSelected = { index ->
                                    actions.onSetColorStyle(styles[index].name)
                                }
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetColorSpec(specs[index].name)
                                }
                            )
                        }
                    )
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SegmentedColumn(
                        modifier = Modifier.padding(top = 4.dp),
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.AutoMirrored.Rounded.MenuOpen,
                                    title = stringResource(id = R.string.settings_enable_predictive_back),
                                    summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                    checked = uiState.enablePredictiveBack,
                                    onCheckedChange = actions.onSetEnablePredictiveBack
                                )
                            }
                        )
                    )
                }

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AspectRatio,
                                contentDescription = stringResource(id = R.string.settings_page_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_page_scale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = "${(sliderValue * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { actions.onSetPageScale(sliderValue) },
                            valueRange = 0.8f..1.1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()))
        }
    }
}

@Composable
private fun ThemeStudioMaterial(
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.theme_studio_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.theme_studio_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                ThemeArtworkImage(
                    template = preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { onApply(preview.id) },
                            enabled = !isApplied,
                        ) {
                            Text(stringResource(if (isApplied) R.string.theme_applied else R.string.theme_apply))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.swatches(isDark).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 12.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        }
                    }
                    preview.summaryRes?.let { summaryRes ->
                        Text(
                            text = stringResource(summaryRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.theme_gallery_count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(themeGalleryFilters, key = { it?.name ?: "all" }) { mood ->
                val selected = mood == selectedFilter
                Surface(
                    onClick = {
                        filterKey = mood?.name ?: "all"
                        if (mood != null) {
                            artworkThemes.firstOrNull { it.galleryMood == mood }?.let { previewId = it.id }
                        }
                    },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(
                        text = themeGalleryFilterLabel(mood),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visibleThemes, key = { it.id }) { template ->
                val selected = template.id == preview.id
                Surface(
                    onClick = { previewId = template.id },
                    modifier = Modifier.width(142.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column {
                        ThemeArtworkImage(
                            template = template,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(156.dp),
                            bottomScrim = true,
                        )
                        Text(
                            text = stringResource(template.nameRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        )
                    }
                }
            }
        }
    }
}

// Opacity (0-100%) control for a decorative image / card, styled like the page-scale slider card.
@Composable
private fun ImageOpacitySlider(
    title: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    TonalCard(modifier = Modifier.padding(top = 4.dp)) {
        var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${sliderValue.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onChange(sliderValue.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Left / Center / Right horizontal position control for a decorative image.
@Composable
private fun ImagePositionSelector(
    value: Int,
    onChange: (Int) -> Unit,
) {
    SegmentedColumn(
        modifier = Modifier.padding(top = 4.dp),
        content = listOf(
            {
                val items = listOf(
                    stringResource(R.string.image_position_left),
                    stringResource(R.string.image_position_center),
                    stringResource(R.string.image_position_right),
                )
                SegmentedDropdownItem(
                    icon = Icons.Rounded.AspectRatio,
                    title = stringResource(R.string.settings_image_position),
                    items = items,
                    selectedIndex = value.coerceIn(0, 2),
                    onItemSelected = { onChange(it) }
                )
            }
        )
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val dynamicColor = keyColor == 0

    val colorScheme = if (dynamicColor) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor),
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )

    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.background,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                // top bar
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TonalCard(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            content = { }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TonalCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                shape = RoundedCornerShape(12.dp),
                                content = { }
                            )
                            TonalCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                shape = RoundedCornerShape(12.dp),
                                content = { }
                            )
                        }
                        TonalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            shape = RoundedCornerShape(12.dp),
                            content = { }
                        )
                    }
                }

                // bottom bar
                Surface(
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = if (color == Color.Unspecified) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = color,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
