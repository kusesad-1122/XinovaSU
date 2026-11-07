// ═══════════════════════════════════════════════════════════════════════════
// 组件 1：右上角液态玻璃电源触发器（圆形 ⇄ 圆角卡片，**同一个液态表面**的形变）
//
// 交互时间线：
//   按下瞬间起        按钮液态挤压收缩（横向鼓出 + 纵向压扁，弹簧 ~120ms）
//   松手 → 展开       同一表面从 56dp 正圆向外延展成圆角卡片，约 0.4s，带一次回弹
//   关闭              列表内容先淡出 → 表面再收拢变回正圆
//
// 关键实现点：**尺寸插值发生在 measure 阶段**（`Modifier.layout` 里 lerp 宽高），
// 而不是对整个卡片做 graphicsLayer 等比缩放。这样文字始终按最终字号排版，
// 只有"表面"在长大 —— 才是"同一表面形态插值"，而不是两个组件的显隐切换。
//
// 材质：模糊 20dp / 色散 0.015 / 边缘高光 1.2dp / 半透明基底，全部走 RuntimeShader。
// ═══════════════════════════════════════════════════════════════════════════
package com.xinsu.moe.ui.component.rebootlistpopup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.xinsu.moe.R
import com.xinsu.moe.ui.component.KsuIsValid
import com.xinsu.moe.ui.component.liquid.LiquidGlassSpec
import com.xinsu.moe.ui.component.liquid.PopupCardCornerRadius
import com.xinsu.moe.ui.component.liquid.liquidGlassSurface
import com.xinsu.moe.ui.component.liquid.liquidGlassTrigger
import com.xinsu.moe.ui.theme.LocalCardBackdrop
import com.xinsu.moe.ui.util.reboot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils

/**
 * 顶栏右上角的液态玻璃电源触发器 + 由它形变展开的电源菜单。
 *
 * 触发器和展开后的卡片是**同一个表面**：卡片展开时右上角始终锚定在触发器原位，
 * 只向左、向下生长，所以看起来就是那颗圆球延展成了一张卡片。
 */
@Composable
fun RebootListPopupMiuix(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopEnd
) {
    val open = remember { mutableStateOf(false) }
    // 触发器在窗口中的外接矩形 —— 形变的锚点与原点全部由它推导，不写死坐标。
    var triggerBounds by remember { mutableStateOf(Rect.Zero) }
    val anchorToEnd = alignment == PopupPositionProvider.Align.TopEnd

    KsuIsValid {
        val backdrop = LocalCardBackdrop.current
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()

        // 阶段一：按下时的液态挤压（0–120ms）。松开自动回弹。
        val squeeze by animateFloatAsState(
            targetValue = if (pressed) LiquidGlassSpec.TriggerPressedScale else 1f,
            animationSpec = spring(
                dampingRatio = LiquidGlassSpec.TriggerPressDamping,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "triggerSqueeze",
        )

        Box(
            modifier = modifier
                .size(LiquidGlassSpec.TriggerSize)
                .onGloballyPositioned { triggerBounds = it.boundsInWindow() }
                .graphicsLayer {
                    // 横向鼓出、纵向压扁：液体被按压时的表面张力形变，而不是生硬的等比缩放。
                    val bulge = LiquidGlassSpec.TriggerPressedBulge * (1f - squeeze)
                    scaleX = squeeze + bulge
                    scaleY = squeeze - bulge
                }
                .liquidGlassTrigger(backdrop)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { open.value = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Close2,
                contentDescription = stringResource(id = R.string.reboot),
                tint = colorScheme.onBackground,
            )
        }

        PowerMorphOverlay(
            open = open,
            triggerBounds = triggerBounds,
            anchorToEnd = anchorToEnd,
            backdrop = backdrop,
        )
    }
}

/**
 * 展开层：一个自绘 overlay，内部只有**一个**会形变的液态玻璃表面。
 *
 * `progress` 0 → 1：直径 56dp 的正圆 → 248dp 宽、内容高度的圆角卡片。
 * 位置始终把右上角（或左上角）钉在触发器原位。
 */
@Composable
private fun PowerMorphOverlay(
    open: MutableState<Boolean>,
    triggerBounds: Rect,
    anchorToEnd: Boolean,
    backdrop: LayerBackdrop?,
) {
    val popupVisible = remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }

    LaunchedEffect(open.value) {
        if (open.value) {
            popupVisible.value = true
            contentAlpha.snapTo(0f)
            // 表面先长开，文字随后淡入 —— 避免文字在小窗口里挤成一团。
            launch {
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = LiquidGlassSpec.ContentFadeInMs,
                        delayMillis = LiquidGlassSpec.ContentFadeInDelayMs,
                    ),
                )
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = LiquidGlassSpec.MorphSpringDamping,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.001f,
                ),
            )
        } else if (popupVisible.value) {
            // 关闭：内容先淡出，表面再收拢回正圆。
            contentAlpha.animateTo(0f, tween(LiquidGlassSpec.ContentFadeOutMs))
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.001f,
                ),
            )
            popupVisible.value = false
        }
    }

    // 返回键：优先关掉弹层（PopupLayout 自带的 back handler 实测拦不住，
    // 会直接把 Activity 退出，所以自己接管）。
    BackHandler(enabled = popupVisible.value) { open.value = false }

    MiuixPopupUtils.PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
        renderInRootScaffold = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 遮罩：浓度直读动画值，与表面同帧增长，不会"遮罩先到、卡片后到"。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = 0.30f * progress.value.coerceIn(0f, 1f)))
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { open.value = false }
            )

            MorphSurface(
                progress = { progress.value },
                contentAlpha = { contentAlpha.value },
                triggerBounds = triggerBounds,
                anchorToEnd = anchorToEnd,
                backdrop = backdrop,
            ) {
                // 卡片内容：与原实现完全一致的电源菜单项（条目背景透明，交给玻璃表面）。
                Column(
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
                ) {
                    val rebootOptions = getRebootListOption()
                    val transparentItems = DropdownDefaults.dropdownColors(
                        containerColor = Color.Transparent,
                    )
                    rebootOptions.forEachIndexed { idx, option ->
                        RebootDropdownItem(
                            option = option,
                            showTopPopup = open,
                            optionSize = rebootOptions.size,
                            index = idx,
                            dropdownColors = transparentItems,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 会形变的液态玻璃表面。
 *
 * - 尺寸：在 measure 阶段把 [LiquidGlassSpec.TriggerSize] 与"内容自然尺寸"做插值，
 *   所以内容永远按最终尺寸排版，只有裁剪窗口在长大。
 * - 圆角：从"半径 = 自身短边一半"（正圆）插值到 [PopupCardCornerRadius]，在 draw 阶段求值。
 * - 裁剪：`graphicsLayer(clip = true, shape = …)` 让内容跟着表面一起被裁成圆 / 圆角矩形。
 */
@Composable
private fun MorphSurface(
    progress: () -> Float,
    contentAlpha: () -> Float,
    triggerBounds: Rect,
    anchorToEnd: Boolean,
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { LiquidGlassSpec.TriggerSize.toPx() }
    val cardWidthPx = with(density) { LiquidGlassSpec.PowerCardWidth.toPx() }

    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val cardWidth = cardWidthPx.roundToInt()
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = cardWidth,
                        maxWidth = cardWidth,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    )
                )
                val p = progress().coerceIn(0f, 1f)
                val w = lerp(triggerPx, cardWidth.toFloat(), p).roundToInt().coerceAtLeast(1)
                val h = lerp(triggerPx, placeable.height.toFloat(), p).roundToInt().coerceAtLeast(1)
                layout(w, h) { placeable.placeRelative(0, 0) }
            }
            .offset {
                val p = progress().coerceIn(0f, 1f)
                val w = lerp(triggerPx, cardWidthPx, p)
                // 右上角（或左上角）钉死在触发器原位：表面只向左/右其中一侧与下方生长。
                val x = if (anchorToEnd) triggerBounds.right - w else triggerBounds.left
                IntOffset(x.roundToInt(), triggerBounds.top.roundToInt())
            }
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                shape = morphShape(p)
                clip = true
            }
            .liquidGlassSurface(
                backdrop = backdrop,
                shapeProvider = { morphShape(progress()) },
                refractionHeight = LiquidGlassSpec.TriggerRefractionHeight,
                refractionAmount = LiquidGlassSpec.TriggerRefractionAmount,
                blurRadius = LiquidGlassSpec.TriggerBlurRadius,
                dispersion = LiquidGlassSpec.TriggerDispersion,
                highlight = { isDark ->
                    val preset =
                        if (isDark) Highlight.GlassStrokeSmallDark else Highlight.GlassStrokeSmallLight
                    preset.copy(width = LiquidGlassSpec.TriggerHighlightWidth)
                },
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            // 触发器上的电源图标：随表面长大而淡出、缩小；
            // 位置与展开前的圆形按钮完全重合，所以"圆 → 卡片"中间不会跳。
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(LiquidGlassSpec.TriggerSize)
                    .graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        alpha = (1f - p * 2f).coerceIn(0f, 1f) * contentAlpha().coerceIn(0f, 1f)
                        val s = 1f - 0.3f * p
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Close2,
                    contentDescription = stringResource(id = R.string.reboot),
                    tint = colorScheme.onBackground,
                )
            }
        }
    }
}

/**
 * 形变形状：`p = 0` 时半径取触发器半径（正圆），`p = 1` 时是卡片圆角。
 * 用缓出曲线让形变前半程更快，收尾像"落定"。
 */
private fun morphShape(p: Float): Shape {
    val t = p.coerceIn(0f, 1f)
    val eased = 1f - (1f - t) * (1f - t) * (1f - t)
    val radius = lerp(LiquidGlassSpec.TriggerSize.value / 2f, PopupCardCornerRadius.value, eased)
    return RoundedCornerShape(radius.dp)
}

@Composable
fun RebootDropdownItem(
    option: RebootListOption,
    showTopPopup: MutableState<Boolean>,
    optionSize: Int,
    index: Int,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
) {
    com.xinsu.moe.ui.component.miuix.DropdownItem(
        text = stringResource(option.labelRes),
        optionSize = optionSize,
        dropdownColors = dropdownColors,
        onSelectedIndexChange = {
            reboot(option.reason)
            showTopPopup.value = false
        },
        index = index
    )
}

// ───────────────────────────── 预览 ─────────────────────────────
//
// 自包含预览：背景层用 Modifier.layerBackdrop 绑到同一个 backdrop，
// Android Studio 里能直接看到真实的折射 / 色散 / 高光（静态渲染不含动画，属正常）。

@Preview(name = "组件1 触发器 · 圆形", widthDp = 360, heightDp = 320)
@Composable
private fun PowerTriggerPreview() {
    PowerTriggerPreviewSurface(progress = 0f)
}

@Preview(name = "组件1 触发器 · 形变中", widthDp = 360, heightDp = 320)
@Composable
private fun PowerTriggerMorphingPreview() {
    PowerTriggerPreviewSurface(progress = 0.55f)
}

@Preview(name = "组件1 触发器 · 展开完成", widthDp = 360, heightDp = 420)
@Composable
private fun PowerTriggerExpandedPreview() {
    PowerTriggerPreviewSurface(progress = 1f)
}

@Composable
private fun PowerTriggerPreviewSurface(progress: Float) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF9BB7D4))
        drawContent()
    }
    val width = LiquidGlassSpec.TriggerSize.value +
        (LiquidGlassSpec.PowerCardWidth.value - LiquidGlassSpec.TriggerSize.value) * progress
    val height = LiquidGlassSpec.TriggerSize.value + 200f * progress

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF10233A))) {
        // 玻璃背后那层：两个色块模拟壁纸，方便肉眼判断折射 / 色散是否过重。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(24.dp, 96.dp)
                    .background(Color(0xFF7ED3A8)),
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(150.dp, 220.dp)
                    .background(Color(0xFFE8A0B4)),
            )
        }
        Box(
            modifier = Modifier
                .offset(340.dp - width.dp - 12.dp, 24.dp)
                .size(width = width.dp, height = height.dp)
                .graphicsLayer { clip = true; shape = morphShape(progress) }
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shapeProvider = { morphShape(progress) },
                    blurRadius = LiquidGlassSpec.TriggerBlurRadius,
                    dispersion = LiquidGlassSpec.TriggerDispersion,
                ),
        )
    }
}
