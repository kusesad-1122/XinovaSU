// 液态玻璃的统一视觉规格 —— 单一事实源。
//
// 两套引擎共用这里的所有数值，切换引擎只换实现、不换观感：
//   1) Miuix 引擎：top.yukonga.miuix.kmp.blur 的 Backdrop API（默认，全应用单次图层捕获）
//   2) Kyant 引擎：io.github.kyant0:backdrop 的 Backdrop API（见 KyantLiquidGlassCard.kt）
// 两者的折射/色散算法同源 —— 本目录 Lens.kt 即 Adapted from Kyant0/AndroidLiquidGlass。
//
// 能力边界（由库内部自行降级，无需调用方判断）：
//   API 31–32：只有 BlurEffect，得到"模糊+底色+高光"；折射与色散自动跳过
//   API 33+  ：AGSL RuntimeShader 可用，折射 + 色散 + 高光完整生效
package com.xinsu.moe.ui.component.liquid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object LiquidGlassSpec {

    /**
     * 卡片圆角。与原设计稿的卡片圆角保持一致 —— 只换材质，不动版式。
     */
    val CardCornerRadius: Dp = 18.dp

    /**
     * 背景模糊半径（默认值；实际取值由设置里的"毛玻璃模糊度"滑块经
     * [com.xinsu.moe.ui.theme.LocalGlassBlurRadius] 下发）。
     *
     * ⚠️ 单位坑（实测踩过）：miuix 的 `blur(radiusX)` / `textureBlur(blurRadius)` 收的是 **px**，
     * 不是 dp —— 内部 `sigma = radius × BLUR_RADIUS_TO_SIGMA`。
     * 最初按需求写的"22dp"换算成 px（22 × 3.5 = 77px）后，背景被抹成一片白雾，
     * 更致命的是**折射和色散失去可折射的细节而完全看不见**，观感只剩模糊 ——
     * 这正是"看不出液态玻璃效果"的根因。
     *
     * 参照用户认可的悬浮底栏（`blur(4.dp.toPx())` ≈ 14px + 折射 24dp）：
     * 卡片面积更大、要承载文字，取 6dp（≈21px）—— 背景可辨、文字清晰、折射可见。
     */
    val BlurRadius: Dp = 6.dp

    /** 滑块下限：再低就几乎没有毛玻璃质感了。 */
    val MinBlurRadius: Dp = 0.dp

    /**
     * 滑块上限。超过 24dp（≈84px）背景就彻底糊成一片，
     * 折射与色散会失去可折射的细节而"消失"—— 这正是当初"看不出液态玻璃"的根因。
     */
    val MaxBlurRadius: Dp = 24.dp

    /**
     * 色散（色差）强度。
     *
     * 注意与上游的差异：Kyant 官方 2.0.1 的 lens() 只暴露 `chromaticAberration: Boolean`，
     * shader 里的强度被硬编码为 1.0（观感很夸张）。本项目的移植版把它改成了 Float 入参
     * （见 Lens.kt 的 `chromaticAberration` uniform），所以这里能精确取 0.02 —— 只在边缘
     * 产生极细的彩色描边，不抢内容。
     */
    const val Dispersion: Float = 0.02f

    /**
     * 边缘高光描边厚度。1.2dp 落在"看得见但不刺眼"的区间：
     * 低于 1dp 在 3x 屏上会亚像素化闪烁，高于 2dp 会变成明显的白边。
     */
    val HighlightWidth: Dp = 1.2.dp

    /**
     * 折射高度：玻璃边缘产生透镜位移的带宽。取值需小于卡片最小边的一半，
     * 否则中缝也会开始扭曲。
     *
     * 与悬浮底栏对齐（底栏用 24dp）—— 之前给 14dp，比底栏还弱，
     * 实测"卡片边缘折射强度还不如底栏"，所以拉到同一量级。
     */
    val RefractionHeight: Dp = 24.dp

    /**
     * 折射位移幅度（内部按负值传给 shader，方向朝内）。
     * 底栏用 24dp；卡片面积更大，给 28dp 让边缘液体感更明确。
     */
    val RefractionAmount: Dp = 28.dp

    /**
     * 按压时的轻微弹性形变比例。常态恒为 1f（完全静止，不参与任何动画）。
     * 取 0.982 而非更夸张的 0.95 —— 需求明确要求"禁用过度流体形变"。
     */
    const val PressedScale: Float = 0.982f

    /**
     * 底色不透明度（浅色模式）。
     *
     * 原本取 0.62，真机实测在浅色壁纸上"卡片一片奶白"—— 用户反馈"还是会有这种白"。
     * 降到 0.30：壁纸与折射能真正透出来，卡片恢复玻璃的通透感；
     * 深色正文压在"模糊后的浅色壁纸 + 30% 白"上仍满足 WCAG AA（≥4.5:1）。
     */
    const val LightScrimAlpha: Float = 0.30f

    /**
     * 底色不透明度（深色模式）：深色基底吸收更多背景光，浅色文字同样需要足够亮度对比。
     * 0.46 是"能透出背景"与"浅色文字可读"的平衡点。
     */
    const val DarkScrimAlpha: Float = 0.46f

    /** 按压弹簧：阻尼略低于临界，形成一次回弹；刚度偏低，避免"弹跳感"。 */
    const val PressSpringDamping: Float = 0.6f

    /**
     * 栏（顶栏 / 底栏 / 导航栏）的底色不透明度。
     *
     * 原毛玻璃档写的是 `surfaceColor.copy(0.87f)` —— 87% 不透明白，
     * 实测用户反馈"顶栏上一片白色，特别突兀"，把壁纸完全盖掉。
     * 先降到 0.35，实测顶栏仍有可见的白雾感，再降到 0.24：
     * 模糊后的壁纸能透出来，标题仍靠字号与字重保证可读。
     */
    const val BarScrimAlpha: Float = 0.24f

    /**
     * 顶栏 / 底栏玻璃的圆角。
     *
     * 原先栏用的是 [androidx.compose.ui.graphics.RectangleShape]（直角），
     * 实测用户反馈"顶栏这个模糊是方形的，可以考虑把它的边角变成圆润的"。
     * 栏贴在屏幕顶边，所以只圆**底部两角**（顶部两角与屏幕边缘齐平，圆了反而露壁纸）。
     * 与卡片圆角同族（18dp）但略小，读起来像同一套材质。
     */
    val BarCornerRadius: Dp = 16.dp

    /**
     * 电源按钮那类"圆形玻璃"的直径 —— 见
     * [com.xinsu.moe.ui.component.liquid.Modifier.liquidGlassCircle]。
     */
    val CircleButtonSize: Dp = 40.dp

    /**
     * 圆形玻璃的折射参数。圆形直径只有 40dp（半径 20dp），
     * 用卡片那套 24/28dp 会让整个圆面都在扭曲，所以按半径的一半取值。
     */
    val CircleRefractionHeight: Dp = 9.dp
    val CircleRefractionAmount: Dp = 9.dp

    // ═══════════════════════════════════════════════════════════════════════
    // 组件 1：右上角液态玻璃电源触发器（圆形 ⇄ 卡片 的同一表面形变）
    // ═══════════════════════════════════════════════════════════════════════

    /** 触发器直径。 */
    val TriggerSize: Dp = 56.dp

    /** 触发器材质：背景模糊 20dp。 */
    val TriggerBlurRadius: Dp = 20.dp

    /** 触发器色散，比卡片更克制（0.015）。 */
    const val TriggerDispersion: Float = 0.015f

    /** 触发器边缘高光厚度。 */
    val TriggerHighlightWidth: Dp = 1.2.dp

    /** 触发器（56dp 圆）的折射带宽。 */
    val TriggerRefractionHeight: Dp = 12.dp
    val TriggerRefractionAmount: Dp = 12.dp

    /**
     * 按下时的液态挤压：整体收缩到 0.90，同时横向鼓出、纵向压扁 ——
     * 模拟液体被按压时的表面张力形变（而不是生硬的整体缩放）。
     */
    const val TriggerPressedScale: Float = 0.90f
    const val TriggerPressedBulge: Float = 0.05f

    /** 液态挤压的弹簧（按下/回弹都用它）。 */
    const val TriggerPressDamping: Float = 0.42f

    /**
     * 展开卡片的目标宽度。宽度固定、高度由内容决定 ——
     * 这样"表面生长"只需要在 layout 阶段插值宽高，右上角锚点也能直接算出偏移。
     */
    val PowerCardWidth: Dp = 248.dp

    /** 展开/收拢的弹簧：低刚度 + 略欠阻尼，整段约 0.35–0.45s，带一次液体回弹。 */
    const val MorphSpringDamping: Float = 0.72f

    /** 内容淡出时长（关闭时先内容淡出、再形状收拢）。 */
    const val ContentFadeOutMs: Int = 110

    /** 内容淡入延时（等表面长开一点再显字，避免文字在小窗口里挤作一团）。 */
    const val ContentFadeInDelayMs: Int = 90
    const val ContentFadeInMs: Int = 150

    // ═══════════════════════════════════════════════════════════════════════
    // 组件 2：顶部下拉液态玻璃面板（from-top drag panel）
    // ═══════════════════════════════════════════════════════════════════════

    /** 面板圆角 22dp。 */
    val PanelCornerRadius: Dp = 22.dp

    /** 面板材质模糊 22dp。 */
    val PanelBlurRadius: Dp = 22.dp

    /** 面板折射参数（与卡片同族）。 */
    val PanelRefractionHeight: Dp = 22.dp
    val PanelRefractionAmount: Dp = 24.dp

    /** 顶部拖拽指示横条。 */
    val PanelHandleWidth: Dp = 44.dp
    val PanelHandleHeight: Dp = 4.dp

    /** 面板展开后占屏幕高度的比例（"停留在屏幕上半部分"）。 */
    const val PanelHeightFraction: Float = 0.52f

    /** 关闭状态下，屏幕最顶部用于起拖的区域高度。 */
    val PanelEdgeStrip: Dp = 28.dp

    /**
     * 起拖带右侧预留宽度。
     *
     * 右上角是组件 1 的电源触发器，**点击优先级高于拖拽**：
     * 起拖带如果横跨整屏就会盖在触发器上面，把点击吃掉（实测就是这么坏的）。
     * 所以起拖带只覆盖左侧这部分宽度，右侧这一带完全留给触发器。
     */
    val PanelEdgeStripRightInset: Dp = 112.dp

    /** 松手后的停靠阈值：拖过这个比例就展开，否则完全收回。 */
    const val PanelSnapThreshold: Float = 0.42f

    /** 停靠时的速度阈值（px/s）：快速甩一下就切换状态。 */
    const val PanelVelocityThreshold: Float = 900f

    /** 停靠弹簧：欠阻尼 → 到位后有一下轻微的液体抖动。 */
    const val PanelSpringDamping: Float = 0.62f

    /** 是否启用色散。0（或负）视为关闭，避免无谓的 shader 分支。 */
    fun dispersionOrDefault(dispersion: Float = Dispersion): Float =
        if (dispersion > 0f) dispersion else 0f
}
