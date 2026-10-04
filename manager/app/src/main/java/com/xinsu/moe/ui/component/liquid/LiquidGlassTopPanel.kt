// ═══════════════════════════════════════════════════════════════════════════
// 组件 2：顶部下拉液态玻璃面板（from-top drag panel）
//
// 与组件 1 完全独立、互不冲突：组件 1 是右上角那一点的点击/形变，
// 组件 2 只在**屏幕最顶部一条窄带**上响应竖向拖拽。
//
// 交互：
//   • 关闭态：面板整体躺在屏幕顶边之外（y = -panelHeight），只有顶部 28dp 的
//     起拖窄带可交互；手指按住向下拖 → 面板跟着手指被"拉"出来。
//   • 松手：按拖拽比例 + 甩动速度停靠 —— 要么完全收回，要么完全展开（占屏幕约一半高）。
//   • 展开态：面板顶部有一条短拖拽指示横条；向上滑把它推回屏幕外。
//   • 停靠动画用欠阻尼弹簧（damping 0.62），到位后有一下轻微的液体抖动回弹。
//
// 材质与组件 1 完全一致：同一份 LiquidGlassSpec 参数、同一个 backdrop、同一套 RuntimeShader。
// 面板本体圆角 22dp / 背景模糊 22dp / 边缘高光。
// ═══════════════════════════════════════════════════════════════════════════
package com.xinsu.moe.ui.component.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.roundToInt

/**
 * 组件 2 的面板状态。
 *
 * 内部持有两条来源：拖拽中的实时值（[dragFraction]，零分配、不启协程）与停靠用的
 * [animated]。对外只暴露 [fraction]，所以调用方不必关心当前是"手在拖"还是"弹簧在跑"。
 */
@Stable
class LiquidGlassTopPanelState {
    internal val animated = Animatable(0f)
    internal var isDragging by mutableStateOf(false)
    internal var dragFraction by mutableFloatStateOf(0f)

    /** 0 = 完全隐藏在屏幕顶部之外；1 = 完全展开。 */
    val fraction: Float
        get() = if (isDragging) dragFraction else animated.value

    val isExpanded: Boolean
        get() = fraction > 0.5f

    /** 程序化展开（例如从别处触发）。 */
    suspend fun expand() {
        isDragging = false
        animated.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = LiquidGlassSpec.PanelSpringDamping,
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = 0.001f,
            ),
        )
    }

    /** 程序化收回。 */
    suspend fun collapse() {
        isDragging = false
        animated.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = LiquidGlassSpec.PanelSpringDamping,
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = 0.001f,
            ),
        )
    }
}

@Composable
fun rememberLiquidGlassTopPanelState(): LiquidGlassTopPanelState =
    remember { LiquidGlassTopPanelState() }

/** 面板形状：四角 22dp 圆角（展开后整块浮在页面之上，四角都能看见）。 */
private val PanelShape = RoundedCornerShape(LiquidGlassSpec.PanelCornerRadius)

/**
 * 顶部下拉液态玻璃面板。
 *
 * 用法（挂载在页面最外层 Box 的最后一个子节点即可，不改动任何既有布局）：
 * ```
 * val panelState = rememberLiquidGlassTopPanelState()
 * LiquidGlassTopPanel(state = panelState) {
 *     // 预留内容区：这里放业务 UI，组件本身不关心内容
 * }
 * ```
 *
 * @param modifier 作用在**全屏宿主**上（gesture layer）。
 * @param backdrop 采样的背景图层；默认取全应用那张（[com.xinsu.moe.ui.theme.LocalCardBackdrop]）。
 * @param content 面板内容插槽，垂直排列，已带好顶部拖拽条与内边距。
 */
@Composable
fun LiquidGlassTopPanel(
    state: LiquidGlassTopPanelState,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = com.xinsu.moe.ui.theme.LocalCardBackdrop.current,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelHeight = maxHeight * LiquidGlassSpec.PanelHeightFraction
        val panelHeightPx = with(density) { panelHeight.toPx() }
        // ⚠️ 起点必须在**系统强制手势区之外**：屏幕最顶端那一条（实测 0–202px）是 SystemUI
        // 的下拉通知手势区，应用收不到那里的 touch，起拖带压上去就等于失效。
        // 用 mandatorySystemGestures 而不是 statusBars —— 后者只到 160px，仍会被系统抢走。
        val topGestureInsetPx = with(density) {
            WindowInsets.mandatorySystemGestures.asPaddingValues().calculateTopPadding().toPx()
        }
        // 展开时面板顶停在该区之下一点，22dp 圆角与拖拽条都露得出来。
        val openInsetPx = topGestureInsetPx + with(density) { 8.dp.toPx() }
        val travelPx = panelHeightPx + openInsetPx

        val fraction: () -> Float = { state.fraction }

        // 拖拽 → 直接改状态值（不经协程），保证跟手不掉帧。
        fun onDragDelta(deltaY: Float) {
            if (!state.isDragging) {
                state.isDragging = true
                state.dragFraction = state.animated.value
            }
            state.dragFraction = (state.dragFraction + deltaY / travelPx).coerceIn(0f, 1f)
        }

        // 松手停靠：先看甩动速度，再看拖拽比例。
        fun settle(velocityY: Float) {
            scope.launch {
                val current = if (state.isDragging) state.dragFraction else state.animated.value
                state.isDragging = false
                state.animated.snapTo(current)
                val shouldOpen = when {
                    velocityY > LiquidGlassSpec.PanelVelocityThreshold -> true
                    velocityY < -LiquidGlassSpec.PanelVelocityThreshold -> false
                    else -> current > LiquidGlassSpec.PanelSnapThreshold
                }
                state.animated.animateTo(
                    targetValue = if (shouldOpen) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = LiquidGlassSpec.PanelSpringDamping,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = 0.001f,
                    ),
                )
            }
        }

        val dragState = rememberDraggableState { deltaY -> onDragDelta(deltaY) }
        val dragModifier = Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocityY -> settle(velocityY) },
        )

        // ── 起拖窄带：关闭态下唯一可交互的区域（屏幕最顶部，不与其他滚动/点击冲突）。
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                // 用固定宽度而不是 fillMaxWidth + padding：padding 不会缩小命中区域，
                // 右侧那片仍会盖住电源触发器、把点击吃掉。
                .width(maxWidth - LiquidGlassSpec.PanelEdgeStripRightInset)
                // 整条起拖带整体下移到系统手势区之下。
                .offset { IntOffset(0, topGestureInsetPx.roundToInt()) }
                .height(LiquidGlassSpec.PanelEdgeStrip)
                // 始终挂着拖拽：面板本体在它之上，同一次手势只会被其中一个消费，
                // 所以不需要按状态去开关（那会每帧重组）。
                .then(dragModifier)
        )

        // ── 面板本体：同一个液态玻璃表面，从屏幕顶部外被拉出来。
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .offset {
                    // f=0 → 完全在外；f=1 → 顶部留 20dp 缝。
                    val y = -panelHeightPx + (travelPx) * fraction()
                    IntOffset(0, y.roundToInt())
                }
                .then(dragModifier)
                .graphicsLayer {
                    shape = PanelShape
                    clip = true
                }
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shapeProvider = { PanelShape },
                    refractionHeight = LiquidGlassSpec.PanelRefractionHeight,
                    refractionAmount = LiquidGlassSpec.PanelRefractionAmount,
                    blurRadius = LiquidGlassSpec.PanelBlurRadius,
                    highlight = { isDark ->
                        val preset =
                            if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
                        preset.copy(width = LiquidGlassSpec.HighlightWidth)
                    },
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 拖拽指示横条（样式与底栏一致：短、圆头、低对比）。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = LiquidGlassSpec.PanelHandleWidth,
                                height = LiquidGlassSpec.PanelHandleHeight,
                            )
                            .graphicsLayer { alpha = 0.45f }
                            .background(
                                color = Color.Gray,
                                shape = RoundedCornerShape(percent = 50),
                            )
                    )
                }
                // 内容插槽 —— 组件不写任何业务。
                content()
            }
        }
    }
}

// ───────────────────────────── 预览 ─────────────────────────────
//
// 自包含预览：背景用 layerBackdrop 绑到同一个 backdrop，能直接看到真实折射/色散。

@Preview(name = "组件2 顶部面板 · 隐藏", widthDp = 360, heightDp = 640)
@Composable
private fun TopPanelHiddenPreview() {
    TopPanelPreviewSurface(startFraction = 0f)
}

@Preview(name = "组件2 顶部面板 · 展开", widthDp = 360, heightDp = 640)
@Composable
private fun TopPanelExpandedPreview() {
    TopPanelPreviewSurface(startFraction = 1f)
}

@Composable
private fun TopPanelPreviewSurface(startFraction: Float) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF9BB7D4))
        drawContent()
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF10233A))) {
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
                    .offset(150.dp, 320.dp)
                    .background(Color(0xFFE8A0B4)),
            )
        }
        // 预览里没有弹簧动画，直接把进度摆到目标值。
        val state = remember { LiquidGlassTopPanelState() }
        LaunchedEffectFraction(state, startFraction)
        LiquidGlassTopPanel(state = state, backdrop = backdrop)
    }
}

/** 预览专用：把状态一次性摆到目标进度（预览里没有手势，也就没有弹簧动画）。 */
@Composable
private fun LaunchedEffectFraction(state: LiquidGlassTopPanelState, target: Float) {
    LaunchedEffect(target) {
        state.isDragging = false
        state.animated.snapTo(target)
    }
}

