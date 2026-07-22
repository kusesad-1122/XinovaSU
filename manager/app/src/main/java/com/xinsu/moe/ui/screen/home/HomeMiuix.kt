package com.xinsu.moe.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinsu.moe.KernelVersion
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.dialog.rememberConfirmDialog
import com.xinsu.moe.ui.component.miuix.MiuixGlassCard
import com.xinsu.moe.ui.component.miuix.WarningCard
import com.xinsu.moe.ui.component.rebootlistpopup.RebootListPopupMiuix
import com.xinsu.moe.ui.theme.LocalEnableBlur
import com.xinsu.moe.ui.theme.LocalBackgroundStyle
import com.xinsu.moe.ui.theme.LocalCardImageUri
import com.xinsu.moe.ui.theme.LocalCardImageAlpha
import com.xinsu.moe.ui.theme.LocalCardImageAlign
import com.xinsu.moe.ui.theme.LocalGlassCard
import com.xinsu.moe.ui.theme.LocalGlassCardsSetting
import com.xinsu.moe.ui.theme.LocalHomeCardCornerRadius
import com.xinsu.moe.ui.theme.LocalHomeCardOrder
import com.xinsu.moe.ui.theme.LocalHomeCardShapes
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.horizontalAlignmentFor
import com.xinsu.moe.ui.theme.LocalThemePreset
import com.xinsu.moe.ui.theme.isActive
import com.xinsu.moe.ui.theme.rememberBackgroundImageBitmap
import com.xinsu.moe.ui.theme.isInDarkTheme
import com.xinsu.moe.ui.theme.scaffoldContainerColor
import com.xinsu.moe.ui.util.BlurredBar
import com.xinsu.moe.ui.util.module.LatestVersionInfo
import com.xinsu.moe.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    // When a decorative background is active, keep the top bar transparent too so the background
    // reaches the very top instead of leaving a solid strip behind the large title.
    val backgroundActive = LocalBackgroundStyle.current.isActive
    val barColor = if (blurActive || backgroundActive) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        containerColor = scaffoldContainerColor(colorScheme.surface),
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
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
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.showManagerPrBuildWarning) {
                            WarningCard(stringResource(id = R.string.home_pr_build_warning))
                        } else if (state.showKernelPrBuildWarning) {
                            WarningCard(stringResource(id = R.string.home_pr_kernel_warning))
                        }
                        if (state.showVersionMismatchWarning) {
                            WarningCard(
                                stringResource(
                                    id = R.string.home_version_mismatch,
                                    state.currentManagerVersionCode,
                                    state.ksuVersion ?: 0
                                )
                            )
                        }
                        if (state.showGkiWarning) {
                            WarningCard(stringResource(id = R.string.home_gki_warning))
                        }
                        if (state.showRequireKernelWarning) {
                            WarningCard(
                                stringResource(
                                    id = R.string.require_kernel_version,
                                    state.ksuVersion ?: 0, com.xinsu.moe.Natives.MINIMAL_SUPPORTED_KERNEL
                                ),
                            )
                        }
                        if (state.showRootWarning) {
                            WarningCard(stringResource(id = R.string.grant_root_failed))
                        }
                        if (state.checkUpdateEnabled) {
                            UpdateCard(state = state, actions = actions)
                        }
                        // Cards render in the user-chosen order, each with its own corner shape.
                        val cardShapes = LocalHomeCardShapes.current
                        val glassOn = LocalGlassCardsSetting.current
                        LocalHomeCardOrder.current.forEach { cardId ->
                            CompositionLocalProvider(
                                LocalHomeCardCornerRadius provides
                                    (cardShapes[cardId] ?: HomeCardShape.Default).cornerRadiusOrNull(),
                                LocalGlassCard provides glassOn,
                            ) {
                                when (cardId) {
                                    HomeCardId.Status -> StatusCard(state = state, actions = actions)
                                    HomeCardId.Info -> InfoCard(systemInfo = state.systemInfo)
                                    HomeCardId.Hardware -> HardwareCard()
                                    HomeCardId.Donate -> DonateCard(onOpenUrl = actions.onOpenUrl)
                                    HomeCardId.LearnMore -> LearnMoreCard(onOpenUrl = actions.onOpenUrl)
                                    HomeCardId.LearnKernelSU -> LearnKernelSUCard(onOpenUrl = actions.onOpenUrl)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val newVersion = state.latestVersionInfo
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)
    val updateDialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) })

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            color = colorScheme.outline,
            onClick = {
                if (newVersion.changelog.isEmpty()) {
                    actions.onOpenUrl(newVersion.downloadUrl)
                } else {
                    updateDialog.showConfirm(
                        title = title,
                        content = newVersion.changelog,
                        markdown = true,
                        confirm = updateText
                    )
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.app_name),
            actions = {
                RebootListPopupMiuix()
            },
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    Column {
        when {
            state.ksuVersion != null -> {
                val workingState = buildString {
                    if (state.isSafeMode) {
                        append(" [${stringResource(id = R.string.safe_mode)}]")
                    }
                    if (state.isLateLoadMode) {
                        append(" [${stringResource(id = R.string.jailbreak_mode)}]")
                    }
                }
                val workingMode = when (state.lkmMode) {
                    null -> ""
                    true -> " <LKM>"
                    else -> " <GKI>"
                }
                val workingText = "${stringResource(id = R.string.home_working)}$workingMode$workingState"

                // When a hand-tuned theme preset (or Monet) is active, let the "Working" card
                // follow the theme's own container/accent instead of the signature green so it
                // no longer clashes with the selected palette.
                val themedWorkingCard = isDynamicColor || LocalThemePreset.current.isActive
                val cardImage = rememberBackgroundImageBitmap(
                    LocalCardImageUri.current.takeIf { it.isNotBlank() }
                )
                val cardImageAlpha = (LocalCardImageAlpha.current / 100f).coerceIn(0f, 1f)
                val cardImageAlignment = horizontalAlignmentFor(LocalCardImageAlign.current)
                val workingCardColor = when {
                    themedWorkingCard -> colorScheme.secondaryContainer
                    isInDarkTheme() -> Color(0xFF1A3825)
                    else -> Color(0xFFDFFAE4)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        containerColor = workingCardColor,
                        onClick = {
                            if (!state.isLateLoadMode) {
                                actions.onInstallClick()
                            }
                        },
                        showIndication = !state.isLateLoadMode,
                        pressFeedbackType = PressFeedbackType.Tilt,
                        role = DecoratedCardRole.Hero,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (cardImage != null) {
                                // User-picked illustration fills the whole card; a gradient tints
                                // the text side back toward the card color so text stays readable.
                                Image(
                                    bitmap = cardImage,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = cardImageAlignment,
                                    alpha = cardImageAlpha,
                                    modifier = Modifier.matchParentSize(),
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    workingCardColor.copy(alpha = 0.82f),
                                                    workingCardColor.copy(alpha = 0.32f),
                                                    Color.Transparent,
                                                ),
                                            ),
                                        ),
                                )
                            }
                            // The check/ring stays the card's signature even with an image; over a
                            // picture it is dimmed a little so it reads as a badge, not clutter.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(38.dp, 45.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    modifier = Modifier.size(170.dp),
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    tint = when {
                                        cardImage != null -> Color.White.copy(alpha = 0.85f)
                                        themedWorkingCard -> colorScheme.primary.copy(alpha = 0.8f)
                                        else -> Color(0xFF36D167)
                                    },
                                    contentDescription = null
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(all = 16.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = workingText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.home_working_version, state.ksuVersion),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        MiuixGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            insideMargin = PaddingValues(16.dp),
                            onClick = { actions.onSuperuserClick() },
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Tilt,
                            role = DecoratedCardRole.Compact,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.superuser),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = state.superuserCount.toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        MiuixGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            insideMargin = PaddingValues(16.dp),
                            onClick = { actions.onModuleClick() },
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Tilt,
                            role = DecoratedCardRole.Compact,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.module),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = state.moduleCount.toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            state.kernelVersion.isGKI() -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiuixGlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!state.isLateLoadMode) {
                                actions.onInstallClick()
                            }
                        },
                        showIndication = !state.isLateLoadMode,
                        pressFeedbackType = PressFeedbackType.Sink,
                        role = DecoratedCardRole.Hero,
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_not_installed),
                            summary = stringResource(R.string.home_click_to_install),
                            startAction = {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    stringResource(R.string.home_not_installed),
                                    modifier = Modifier.padding(end = 6.dp),
                                    tint = colorScheme.onBackground,
                                )
                            },
                            endActions = {
                                if (state.isSELinuxPermissive) {
                                    TextButton(
                                        text = stringResource(R.string.home_jailbreak),
                                        onClick = actions.onJailbreakClick,
                                        colors = ButtonDefaults.textButtonColorsPrimary()
                                    )
                                }
                            }
                        )
                    }
                }
            }

            else -> {
                MiuixGlassCard(
                    onClick = {
                        if (!state.isLateLoadMode) {
                            actions.onInstallClick()
                        }
                    },
                    showIndication = !state.isLateLoadMode,
                    pressFeedbackType = PressFeedbackType.Sink,
                    role = DecoratedCardRole.Hero,
                ) {
                    BasicComponent(
                        title = stringResource(R.string.home_unsupported),
                        summary = stringResource(R.string.home_unsupported_reason),
                        startAction = {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                stringResource(R.string.home_unsupported),
                                modifier = Modifier.padding(end = 16.dp),
                                tint = colorScheme.onBackground,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnMoreCard(
    onOpenUrl: (String) -> Unit,
) {
    val url = stringResource(R.string.home_learn_xinovasu_url)
    MiuixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        role = DecoratedCardRole.Compact,
    ) {
        BasicComponent(
            title = stringResource(R.string.home_learn_xinovasu),
            summary = stringResource(R.string.home_click_to_learn_xinovasu),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = { onOpenUrl(url) }
        )
    }
}

@Composable
private fun LearnKernelSUCard(
    onOpenUrl: (String) -> Unit,
) {
    val url = stringResource(R.string.home_learn_kernelsu_url)
    MiuixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        role = DecoratedCardRole.Compact,
    ) {
        BasicComponent(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = { onOpenUrl(url) }
        )
    }
}

@Composable
private fun DonateCard(onOpenUrl: (String) -> Unit) {
    MiuixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        role = DecoratedCardRole.Compact,
    ) {
        BasicComponent(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = { onOpenUrl("https://patreon.com/weishu") },
            insideMargin = PaddingValues(18.dp)
        )
    }
}

@Composable
private fun InfoCard(systemInfo: SystemInfo) {
    @Composable
    fun InfoText(
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp
    ) {
        Text(
            text = title,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
        Text(
            text = content,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
        )
    }

    MiuixGlassCard(
        role = DecoratedCardRole.Standard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            InfoText(title = stringResource(R.string.home_manager_version), content = systemInfo.managerVersion)
            InfoText(title = stringResource(R.string.home_kernel), content = systemInfo.kernelVersion)
            InfoText(title = stringResource(R.string.home_device_model), content = systemInfo.deviceModel)
            InfoText(title = stringResource(R.string.home_fingerprint), content = systemInfo.fingerprint)
            val selinuxDisplay = when (systemInfo.selinuxStatus) {
                "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
                "Permissive" -> stringResource(R.string.selinux_status_permissive)
                "Disabled" -> stringResource(R.string.selinux_status_disabled)
                else -> stringResource(R.string.selinux_status_unknown)
            }
            InfoText(
                title = stringResource(R.string.home_selinux_status),
                content = selinuxDisplay,
            )
            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }
            InfoText(
                title = stringResource(R.string.home_seccomp_status),
                content = seccompDisplay,
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
private fun HardwareCard() {
    val stats = rememberHardwareStats(active = true)
    MiuixGlassCard(
        role = DecoratedCardRole.Monitor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_hw_title),
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            if (stats != null) {
                hardwareTiles(stats).chunked(2).forEach { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTiles.forEach { (label, value) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Text(
                                    text = value,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                        if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(name = "Activated")
@Composable
private fun StatusCardActivatedPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true, superuserCount = 5, moduleCount = 10),
        actions = HomeActions({}, {}, {}, {})
    )
}

@Preview(name = "Not Activated")
@Composable
private fun StatusCardNotActivatedPreview() {
    StatusCard(state = previewHomeScreenState(ksuVersion = null, lkmMode = null), actions = HomeActions({}, {}, {}, {}))
}

@Preview(name = "Permissive")
@Composable
private fun StatusCardPermissivePreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive"),
        actions = HomeActions({}, {}, {}, {})
    )
}

@Preview(name = "Jailbreak")
@Composable
private fun StatusCardJailbreakPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true, superuserCount = 5, moduleCount = 10),
        actions = HomeActions({}, {}, {}, {})
    )
}

private val previewSystemInfo = SystemInfo(
    kernelVersion = "6.12.23-android16-5-g123456789000-abogki123456789-4k",
    managerVersion = "3.0.0 (30000)",
    deviceModel = "Xiaomi 17 Pro Max",
    fingerprint = "Xiaomi/popsicle/popsicle:16/BQ2A.250705.001-BP2A.250605.031.A3/OS3.0.313.0.WPBCNXM:user/release-keys",
    selinuxStatus = "Enforcing",
    seccompStatus = 2
)

private val previewUriHandler = object : UriHandler {
    override fun openUri(uri: String) {}
}

@Composable
private fun HomeScreenPreviewContent(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    superuserCount: Int = 0,
    moduleCount: Int = 0,
    selinuxStatus: String = "Enforcing",
) {
    CompositionLocalProvider(LocalUriHandler provides previewUriHandler) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val actions = HomeActions({}, {}, {}, {})
            StatusCard(
                state = previewHomeScreenState(
                    ksuVersion = ksuVersion,
                    lkmMode = lkmMode,
                    isSafeMode = isSafeMode,
                    isLateLoadMode = isLateLoadMode,
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    selinuxStatus = selinuxStatus,
                ),
                actions = actions
            )
            InfoCard(previewSystemInfo.copy(selinuxStatus = selinuxStatus))
            DonateCard(onOpenUrl = {})
            LearnMoreCard(onOpenUrl = {})
        }
    }
}

@Preview(name = "Home Activated", showBackground = true)
@Composable
private fun HomeScreenActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true, superuserCount = 5, moduleCount = 10)
}

@Preview(name = "Home Not Activated", showBackground = true)
@Composable
private fun HomeScreenNotActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null)
}

@Preview(name = "Home Permissive", showBackground = true)
@Composable
private fun HomeScreenPermissivePreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive")
}

@Preview(name = "Home Jailbreak", showBackground = true)
@Composable
private fun HomeScreenJailbreakPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true, superuserCount = 5, moduleCount = 10)
}

private fun previewHomeScreenState(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    superuserCount: Int = 0,
    moduleCount: Int = 0,
    selinuxStatus: String = "Enforcing",
) = HomeUiState(
    kernelVersion = KernelVersion(6, 1, 0),
    ksuVersion = ksuVersion,
    lkmMode = lkmMode,
    isManager = true,
    isManagerPrBuild = false,
    isKernelPrBuild = false,
    requiresNewKernel = false,
    isRootAvailable = ksuVersion != null,
    isSafeMode = isSafeMode,
    isLateLoadMode = isLateLoadMode,
    checkUpdateEnabled = false,
    latestVersionInfo = LatestVersionInfo(),
    currentManagerVersionCode = 10000,
    superuserCount = superuserCount,
    moduleCount = moduleCount,
    systemInfo = previewSystemInfo.copy(selinuxStatus = selinuxStatus),
)
