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
     * 背景模糊半径。22dp 是"透出背景但仍能分辨"的平衡点：
     * 再大背景糊成一片、卡片失去层次；再小背景细节会干扰前景文字。
     */
    val BlurRadius: Dp = 22.dp

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
     * 折射高度：玻璃边缘产生透镜位移的带宽。与 [RefractionAmount] 一起决定
     * "边缘液体感"的强度，取值需小于卡片最小边的一半，否则中缝也会开始扭曲。
     */
    val RefractionHeight: Dp = 14.dp

    /**
     * 折射位移幅度（内部按负值传给 shader，方向朝内）。
     */
    val RefractionAmount: Dp = 16.dp

    /**
     * 按压时的轻微弹性形变比例。常态恒为 1f（完全静止，不参与任何动画）。
     * 取 0.982 而非更夸张的 0.95 —— 需求明确要求"禁用过度流体形变"。
     */
    const val PressedScale: Float = 0.982f

    /**
     * 底色不透明度（浅色模式）：白色基底越不透明，深色文字越安全。
     * 0.62 是在真机浅色壁纸上反复比对后能同时满足"透出背景"与"文字对比度过 WCAG AA"的值。
     */
    const val LightScrimAlpha: Float = 0.62f

    /**
     * 底色不透明度（深色模式）：深色基底吸收更多背景光，浅色文字同样需要足够亮度对比。
     */
    const val DarkScrimAlpha: Float = 0.50f

    /** 按压弹簧：阻尼略低于临界，形成一次回弹；刚度偏低，避免"弹跳感"。 */
    const val PressSpringDamping: Float = 0.6f

    /** 是否启用色散。0（或负）视为关闭，避免无谓的 shader 分支。 */
    fun dispersionOrDefault(dispersion: Float = Dispersion): Float =
        if (dispersion > 0f) dispersion else 0f
}
