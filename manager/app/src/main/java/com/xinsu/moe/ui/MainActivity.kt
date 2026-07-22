package com.xinsu.moe.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.MutableStateFlow
import com.xinsu.moe.Natives
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.bottombar.BottomBar
import com.xinsu.moe.ui.component.bottombar.MainPagerState
import com.xinsu.moe.ui.component.bottombar.SideRail
import com.xinsu.moe.ui.component.bottombar.rememberMainPagerState
import com.xinsu.moe.ui.component.dialog.rememberConfirmDialog
import com.xinsu.moe.ui.navigation3.HandleDeepLink
import com.xinsu.moe.ui.navigation3.LocalNavigator
import com.xinsu.moe.ui.navigation3.Navigator
import com.xinsu.moe.ui.navigation3.Route
import com.xinsu.moe.ui.navigation3.rememberNavigator
import com.xinsu.moe.ui.screen.about.AboutScreen
import com.xinsu.moe.ui.screen.appprofile.AppProfileScreen
import com.xinsu.moe.ui.screen.colorpalette.ColorPaletteScreen
import com.xinsu.moe.ui.screen.executemoduleaction.ExecuteModuleActionScreen
import com.xinsu.moe.ui.screen.flash.FlashIt
import com.xinsu.moe.ui.screen.flash.FlashScreen
import com.xinsu.moe.ui.screen.functions.FunctionsScreen
import com.xinsu.moe.ui.screen.home.HomePager
import com.xinsu.moe.ui.screen.install.InstallScreen
import com.xinsu.moe.ui.screen.module.ModulePager
import com.xinsu.moe.ui.screen.modulerepo.ModuleRepoDetailScreen
import com.xinsu.moe.ui.screen.modulerepo.ModuleRepoScreen
import com.xinsu.moe.ui.screen.settings.SettingPager
import com.xinsu.moe.ui.screen.sulog.SulogScreen
import com.xinsu.moe.ui.screen.superuser.SuperUserPager
import com.xinsu.moe.ui.screen.template.AppProfileTemplateScreen
import com.xinsu.moe.ui.screen.templateeditor.TemplateEditorScreen
import com.xinsu.moe.ui.theme.XinovaSUTheme
import com.xinsu.moe.ui.theme.AppBackgroundLayer
import com.xinsu.moe.ui.theme.BackgroundStyle
import com.xinsu.moe.ui.theme.isActive
import com.xinsu.moe.ui.theme.isInDarkTheme
import com.xinsu.moe.ui.theme.horizontalAlignmentFor
import com.xinsu.moe.ui.theme.rememberBackgroundImageBitmap
import com.xinsu.moe.ui.theme.scaffoldContainerColor
import com.xinsu.moe.ui.theme.usesImage
import com.xinsu.moe.ui.theme.LocalBackgroundImageUri
import com.xinsu.moe.ui.theme.LocalBackgroundImageAlpha
import com.xinsu.moe.ui.theme.LocalBackgroundImageAlign
import com.xinsu.moe.ui.theme.LocalBackgroundStyle
import com.xinsu.moe.ui.theme.LocalCardImageUri
import com.xinsu.moe.ui.theme.LocalCardImageAlpha
import com.xinsu.moe.ui.theme.LocalCardImageAlign
import com.xinsu.moe.ui.theme.LocalCardOpacity
import com.xinsu.moe.ui.theme.LocalCardBackdrop
import com.xinsu.moe.ui.theme.LocalGlassCardsSetting
import com.xinsu.moe.ui.theme.LocalHomeCardOrder
import com.xinsu.moe.ui.theme.LocalHomeCardShapes
import com.xinsu.moe.ui.theme.LocalColorMode
import com.xinsu.moe.ui.theme.LocalThemePreset
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.LocalThemeTokenBundle
import com.xinsu.moe.ui.theme.LocalEnableBlur
import com.xinsu.moe.ui.theme.LocalEnableFloatingBottomBar
import com.xinsu.moe.ui.theme.LocalEnableFloatingBottomBarBlur
import com.xinsu.moe.ui.theme.BuiltInThemes
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationCatalog
import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import com.xinsu.moe.ui.util.getFileName
import com.xinsu.moe.ui.util.install
import com.xinsu.moe.ui.util.rememberBlurBackdrop
import com.xinsu.moe.ui.util.rememberContentReady
import com.xinsu.moe.ui.util.rootAvailable
import com.xinsu.moe.ui.viewmodel.MainActivityViewModel
import com.xinsu.moe.ui.viewmodel.MainPagerConfig
import com.xinsu.moe.ui.webui.WebUIActivity
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    private val intentState = MutableStateFlow(0)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isManager = Natives.isManager
        if (isManager && !Natives.requireNewKernel()) install()

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())
            val themeTokenBundle = remember(appSettings.themePresetId) {
                BuiltInThemes.byId(appSettings.themePresetId)
                    ?.tokenBundleId
                    ?.let(BuiltInThemeCatalog::byId)
            }
            val themeDecorationSpec = remember(
                appSettings.themePresetId,
                appSettings.keyColor,
                appSettings.colorMode,
            ) {
                ThemeDecorationCatalog.resolve(
                    themeId = appSettings.themePresetId,
                    keyColor = appSettings.keyColor,
                    colorMode = appSettings.colorMode,
                )
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalThemePreset provides appSettings.themePreset,
                LocalThemeTokenBundle provides themeTokenBundle,
                LocalThemeDecorationSpec provides themeDecorationSpec,
                LocalBackgroundStyle provides uiState.backgroundStyle,
                LocalBackgroundImageUri provides uiState.backgroundImageUri,
                LocalBackgroundImageAlpha provides uiState.backgroundImageAlpha,
                LocalBackgroundImageAlign provides uiState.backgroundImageAlign,
                LocalCardImageUri provides uiState.cardImageUri,
                LocalCardImageAlpha provides uiState.cardImageAlpha,
                LocalCardImageAlign provides uiState.cardImageAlign,
                LocalCardOpacity provides uiState.cardOpacity,
                LocalHomeCardOrder provides uiState.homeCardOrder,
                LocalHomeCardShapes provides uiState.homeCardShapes,
                LocalGlassCardsSetting provides uiState.cardsGlass,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalUiMode provides uiMode,
            ) {
                XinovaSUTheme(appSettings = appSettings, uiMode = uiMode) {
                    HandleDeepLink(intentState = intentState.collectAsStateWithLifecycle())
                    ZipFileIntentHandler(intentState = intentState, isManager = isManager)
                    ShortcutIntentHandler(intentState = intentState)
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        NavDisplay(
                            backStack = navigator.backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            onBack = {
                                when (val top = navigator.current()) {
                                    is Route.TemplateEditor -> {
                                        if (!top.readOnly) {
                                            navigator.setResult("template_edit", true)
                                        } else {
                                            navigator.pop()
                                        }
                                    }

                                    else -> navigator.pop()
                                }
                            },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.About> { AboutScreen() }
                                entry<Route.Functions> { FunctionsScreen() }
                                entry<Route.Sulog> { SulogScreen() }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.AppProfileTemplate> { AppProfileTemplateScreen() }
                                entry<Route.TemplateEditor> { key -> TemplateEditorScreen(key.template, key.readOnly) }
                                entry<Route.AppProfile> { key -> AppProfileScreen(key.uid) }
                                entry<Route.ModuleRepo> { ModuleRepoScreen() }
                                entry<Route.ModuleRepoDetail> { key -> ModuleRepoDetailScreen(key.module) }
                                entry<Route.Install> { InstallScreen() }
                                entry<Route.Flash> { key -> FlashScreen(key.flashIt) }
                                entry<Route.ExecuteModuleAction> { key -> ExecuteModuleActionScreen(key.moduleId, key.fromShortcut) }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.SuperUser> { mainScreenEntry() }
                                entry<Route.Module> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            }
                        )
                    }

                    // Single app-wide decorative background: drawn once behind the whole
                    // NavDisplay so it sits behind EVERY screen (main pages and detail routes),
                    // not just the pager. Each route's Scaffold goes transparent via
                    // scaffoldContainerColor to let it show through.
                    val bgStyle = uiState.backgroundStyle
                    val bgDark = isInDarkTheme()
                    val bgMaterial = uiMode == UiMode.Material
                    val bgImage = rememberBackgroundImageBitmap(
                        if (bgStyle.usesImage) uiState.backgroundImageUri else null
                    )
                    val bgImageAlpha = (uiState.backgroundImageAlpha / 100f).coerceIn(0f, 1f)
                    val bgImageAlignment = horizontalAlignmentFor(uiState.backgroundImageAlign)
                    val glassBase = if (bgMaterial) MaterialTheme.colorScheme.background else MiuixTheme.colorScheme.background
                    // A backdrop capturing ONLY the background region (base + decorative layer), so
                    // "glass cards" can frost what's behind them without sampling the cards (which
                    // would feed back on itself). Miuix-blur only; null when blur is off/unsupported.
                    val cardBackdrop = if (uiState.cardsGlass && uiState.enableBlur && isRenderEffectSupported()) {
                        rememberLayerBackdrop {
                            drawRect(glassBase)
                            drawContent()
                        }
                    } else {
                        null
                    }
                    val rootContent = @Composable {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .then(if (cardBackdrop != null) Modifier.layerBackdrop(cardBackdrop) else Modifier)
                            ) {
                                if (bgStyle.isActive) {
                                    AppBackgroundLayer(
                                        modifier = Modifier.matchParentSize(),
                                        style = bgStyle,
                                        imageBitmap = bgImage,
                                        imageAlpha = bgImageAlpha,
                                        imageAlignment = bgImageAlignment,
                                        artworkTokens = themeTokenBundle?.artwork,
                                        atmosphereTokens = themeTokenBundle?.atmosphere,
                                        accentA = if (bgMaterial) MaterialTheme.colorScheme.primary else MiuixTheme.colorScheme.primary,
                                        accentB = if (bgMaterial) MaterialTheme.colorScheme.secondary else MiuixTheme.colorScheme.secondary,
                                        accentC = if (bgMaterial) MaterialTheme.colorScheme.tertiary else MiuixTheme.colorScheme.tertiaryContainer,
                                        base = glassBase,
                                        scrim = if (bgMaterial) MaterialTheme.colorScheme.surface else MiuixTheme.colorScheme.surface,
                                        isDark = bgDark,
                                    )
                                }
                            }
                            CompositionLocalProvider(LocalCardBackdrop provides cardBackdrop) {
                                navDisplay()
                            }
                        }
                    }
                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold { rootContent() }
                        UiMode.Miuix -> Scaffold { rootContent() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Increment intentState to trigger LaunchedEffect re-execution
        intentState.value += 1
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val isManager = Natives.isManager
    val isFullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()
    var userScrollEnabled by remember(isFullFeatured) { mutableStateOf(isFullFeatured) }
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface // Blur is not used in Material, this is just a placeholder
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    // When a decorative background is active it is drawn once, app-wide, behind the whole
    // NavDisplay. These wrapper Scaffolds must go transparent so that hoisted layer shows
    // through the four main pages just like it does on detail routes.
    val scaffoldBase = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.background
        UiMode.Miuix -> MiuixTheme.colorScheme.background
    }
    val mainContainerColor = scaffoldContainerColor(scaffoldBase)
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useNavigationRail = isLandscape && !(uiMode == UiMode.Miuix && enableFloatingBottomBar)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    userScrollEnabled = userScrollEnabled,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) HomePager(navController, bottomInnerPadding, isCurrentPage)
                        1 -> if (isCurrentPage || contentReady) SuperUserPager(navController, bottomInnerPadding, isCurrentPage)
                        2 -> if (isCurrentPage || contentReady) ModulePager(bottomInnerPadding, isCurrentPage)
                        3 -> if (isCurrentPage || contentReady) SettingPager(navController, bottomInnerPadding)
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    containerColor = mainContainerColor
                ) {
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold(
                    containerColor = mainContainerColor
                ) { _ ->
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = mainContainerColor
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(
                    bottomBar = bottomBar,
                    containerColor = mainContainerColor
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}

/**
 * Handles ZIP file installation from external apps (e.g., file managers).
 * - In normal mode: Shows a confirmation dialog before installation
 * - In safe mode: Shows a Toast notification and prevents installation
 */
@SuppressLint("StringFormatInvalid", "LocalContextGetResourceValueCall")
@Composable
private fun ZipFileIntentHandler(
    intentState: MutableStateFlow<Int>,
    isManager: Boolean,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    var zipUri by remember { mutableStateOf<Uri?>(null) }
    val isSafeMode = Natives.isSafeMode
    val clearZipUri = { zipUri = null }
    val navigator = LocalNavigator.current

    val installDialog = rememberConfirmDialog(
        onConfirm = {
            zipUri?.let { uri -> navigator.push(Route.Flash(FlashIt.FlashModules(listOf(uri)))) }
            clearZipUri()
        },
        onDismiss = clearZipUri
    )

    fun getDisplayName(uri: Uri): String {
        return uri.getFileName(context) ?: uri.lastPathSegment ?: "Unknown"
    }

    val intentStateValue by intentState.collectAsStateWithLifecycle()
    LaunchedEffect(intentStateValue) {
        val currentIntent = activity.intent
        val uri = currentIntent?.data ?: return@LaunchedEffect

        if (!isManager || uri.scheme != "content" || currentIntent.type != "application/zip") {
            return@LaunchedEffect
        }

        activity.intent.data = null
        activity.intent.type = null

        if (isSafeMode) {
            Toast.makeText(context, context.getString(R.string.safe_mode_module_disabled), Toast.LENGTH_SHORT).show()
        } else {
            zipUri = uri
            installDialog.showConfirm(
                title = context.getString(R.string.module),
                content = context.getString(
                    R.string.module_install_prompt_with_name,
                    "\n${getDisplayName(uri)}"
                )
            )
        }
    }
}

@Composable
private fun ShortcutIntentHandler(
    intentState: MutableStateFlow<Int>,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    val intentStateValue by intentState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(intentStateValue) {
        val intent = activity.intent
        val type = intent?.getStringExtra("shortcut_type") ?: return@LaunchedEffect

        when (type) {
            "module_action" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                navigator.push(Route.ExecuteModuleAction(moduleId, fromShortcut = true))
                intent.removeExtra("shortcut_type")
                intent.removeExtra("module_id")
            }

            "module_webui" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                val webIntent = Intent(context, WebUIActivity::class.java)
                    .setData("xinovasu://webui/$moduleId".toUri())
                context.startActivity(webIntent)
            }

            else -> return@LaunchedEffect
        }
    }
}
