package com.xinsu.moe.ui.theme.decoration

import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * 主题装饰形变（theme morph）的纯策略层。
 *
 * 现状问题：切换主题时 [ThemeDecorationSpec] 是被整体替换的，卡牌上的 frame / pedestal /
 * particle 会在一帧内从旧配方硬切到新配方。配合 [EnergyTimeline] 已有的开关动效，主题之间
 * 的切换反而是整个界面里最生硬的一步 —— 32 套配方各画各的，视觉上没有"同一张卡变形过去"
 * 的连续感。
 *
 * 这里的做法：把「从旧配方到新配方」拆成一段有节奏的时间线（对齐 [EnergyTimeline] 的分段
 * 思路），并用逐枚举的插值函数把两套配方混合起来。之所以逐个枚举手写而不用 ordinal 线性
 * 插值，是因为 frame / particle 这类枚举的相邻项之间没有任何形变关系，按序号混参会得到
 * "毫不相干的两个图案叠在一起"的糊状中间态。
 *
 * 全部为纯函数，便于在不依赖 Compose 运行环境的前提下推理与验证。
 */
@Immutable
data class ThemeMorphFrame(
    /** 混合后的装饰配方，供绘制层直接消费。 */
    val spec: ThemeDecorationSpec,
    /** 0f..1f，整体形变进度。绘制层可用它做统一的淡入淡出与描边呼吸。 */
    val progress: Float,
    /** 0f..1f，插值权重曲线（已做缓动），用于让起步/收尾更柔和。 */
    val eased: Float,
    /**
     * 形变期间需要叠加的"过渡光扫"。0f 表示不画。
     * 让主题切换有一条明确的方向感，而不是整张卡同时淡入。
     */
    val sweep: Float,
    /** 形变期间粒子是否允许发射（低端机 / 省电 / 减弱动画时为 false）。 */
    val emitTransitionParticles: Boolean,
)

object ThemeMorphTimeline {
    const val DurationMillis = 760

    /**
     * 返回从 [from] 到 [to] 在 [rawProgress]（0f..1f）处的形变帧。
     *
     * [sameRecipe] 为 true 表示两次解析出的是同一套配方（例如从 MaterialKolor 动态取色切到
     * 同一个 keyColor），此时直接返回目标帧、不产生中间态，避免无意义的重绘。
     */
    fun frame(
        from: ThemeDecorationSpec,
        to: ThemeDecorationSpec,
        rawProgress: Float,
        sameRecipe: Boolean = false,
        allowSweep: Boolean = true,
    ): ThemeMorphFrame {
        val t = rawProgress.coerceIn(0f, 1f)
        if (sameRecipe) {
            return ThemeMorphFrame(
                spec = to,
                progress = 1f,
                eased = 1f,
                sweep = 0f,
                emitTransitionParticles = false,
            )
        }
        val eased = ease(t)
        return ThemeMorphFrame(
            spec = blend(from, to, eased),
            progress = t,
            eased = eased,
            // 光扫在前 72% 内走完，剩余时间留白给余韵，避免尾巴上出现一条突兀的亮线。
            sweep = if (allowSweep) sweepAt(t) else 0f,
            emitTransitionParticles = allowSweep,
        )
    }

    /**
     * 缓动：起步慢、中段快、收尾稳。用 smoothstep 的变体（3t²-2t³）在 t=0.5 处导数最大，
     * 正好让"旧装饰退去、新装饰涌入"发生在用户注意力最集中的那一瞬。
     */
    fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * 过渡光扫在 [rawProgress] 处的位置。0f..1f 扫过卡片宽度，[SweepEnd] 表示完全移出右边界。
     *
     * 关键设计：位置在整个时间线上**连续单调**推进到 [SweepEnd]，不设"窗口结束后归零"的
     * 提前返回。是否绘制完全交给 [sweepAlpha] 判断（越过边界时它自然衰减到 0）。
     * 早期版本在窗口末尾直接 return 0，结果光带在卡片中间凭空消失 —— 这正是本函数注释里
     * 明确要避免的突兀感。位置与"是否可见"是两个职责，不该共用一个返回值。
     */
    fun sweepAt(t: Float): Float = ease(t.coerceIn(0f, 1f)) * SweepEnd

    /**
     * 光扫亮度。[position] 为 0（尚未开始）时为 0，越靠近卡片中线越亮，两侧对称衰减，
     * 到 [SweepEnd]（扫出右边界）时恰好衰减到 0。
     *
     * 衰减用 smoothstep 而非线性，这样在"刚扫出"和"刚进入"两端亮度都平滑地趋于 0 ——
     * 若用线性衰减，位置越过边界那一帧会因为硬截断而出现亮度阶跃。
     */
    fun sweepAlpha(position: Float): Float {
        if (position <= 0f || position >= SweepEnd) return 0f
        // 归一化到 0..1：0 = 起点，1 = 扫出边界，0.5 = 卡片中线。
        val normalized = position / SweepEnd
        val distanceFromCenter = ((normalized - 0.5f) * 2f).coerceIn(-1f, 1f)
        val shaped = 1f - distanceFromCenter * distanceFromCenter
        // smoothstep 收尾：两端一阶导为 0，避免边界处出现亮度斜率突变。
        return SweepMaxAlpha * shaped * shaped * (3f - 2f * shaped)
    }

    /** 光扫标量的终点：1.25 保证光带完全移出卡片右侧。 */
    const val SweepEnd = 1.25f

    /** 光扫峰值亮度。刻意压低，避免在浅色二次元主题上盖住插画。 */
    const val SweepMaxAlpha = 0.30f

    /**
     * 逐维度混合两套配方。
     *
     * 混合规则分三类：
     *  - 连续数值（幅度、粒子数、相位、留白比例）直接线性插值；
     *  - 造型枚举（frame / pedestal / motif / ambient / path / particle / rail）在
     *    [ThemeMorphTimeline.StyleSwapShare] 之前保持旧样式，之后整体切到新样式 —— 但切换
     *    不是硬切，而是配合上面的 sweep 光扫，让观感是"扫过去之后变了"；
     *  - 强调色（accents）取目标主题，因为配色属于主题身份的一部分，混色会产生浑浊的中间色。
     */
    fun blend(
        from: ThemeDecorationSpec,
        to: ThemeDecorationSpec,
        eased: Float,
    ): ThemeDecorationSpec {
        val swapDone = eased >= StyleSwapShare
        return ThemeDecorationSpec(
            themeId = to.themeId,
            motif = if (swapDone) to.motif else from.motif,
            frame = FrameRecipe(
                style = if (swapDone) to.frame.style else from.frame.style,
                lineCount = lerpInt(from.frame.lineCount, to.frame.lineCount, eased),
                cutCorners = lerpInt(from.frame.cutCorners, to.frame.cutCorners, eased),
                insetStroke = if (swapDone) to.frame.insetStroke else from.frame.insetStroke,
                accent = to.frame.accent,
            ),
            iconPedestal = IconPedestalRecipe(
                style = if (swapDone) to.iconPedestal.style else from.iconPedestal.style,
                ringCount = lerpInt(from.iconPedestal.ringCount, to.iconPedestal.ringCount, eased),
                accent = to.iconPedestal.accent,
            ),
            ambient = AmbientRecipe(
                style = if (swapDone) to.ambient.style else from.ambient.style,
                driftAxis = if (swapDone) to.ambient.driftAxis else from.ambient.driftAxis,
                amplitude = lerp(from.ambient.amplitude, to.ambient.amplitude, eased),
            ),
            energy = EnergyRecipe(
                path = if (swapDone) to.energy.path else from.energy.path,
                particle = if (swapDone) to.energy.particle else from.energy.particle,
                direction = if (swapDone) to.energy.direction else from.energy.direction,
                baseParticles = lerpInt(from.energy.baseParticles, to.energy.baseParticles, eased),
                // 相位是 0f..1f 的循环偏移，跨 0/1 边界时按最短弧插值，否则回转会倒着转一整圈。
                phaseOffset = lerpPhase(from.energy.phaseOffset, to.energy.phaseOffset, eased),
                accents = to.accents,
            ),
            layout = CardLayoutRecipe(
                titleRail = if (swapDone) to.layout.titleRail else from.layout.titleRail,
                badgeAnchor = if (swapDone) to.layout.badgeAnchor else from.layout.badgeAnchor,
                safeInsetFraction = lerp(
                    from.layout.safeInsetFraction,
                    to.layout.safeInsetFraction,
                    eased,
                ),
            ),
            accents = to.accents,
        )
    }

    /**
     * 造型枚举的切换点。留出前 34% 让旧样式淡出，避免"点一下瞬间全变了"的突兀。
     */
    const val StyleSwapShare = 0.34f

    /** 两套配方是否结构等价（用于跳过无意义的形变）。 */
    fun sameStructure(a: ThemeDecorationSpec, b: ThemeDecorationSpec): Boolean =
        a.structuralFingerprint() == b.structuralFingerprint()

    private fun lerp(from: Float, to: Float, t: Float): Float =
        from + (to - from) * t.coerceIn(0f, 1f)

    private fun lerpInt(from: Int, to: Int, t: Float): Int {
        val bounded = t.coerceIn(0f, 1f)
        val value = from + (to - from) * bounded
        // .5f 向上取整：整数属性在中点就位，避免出现比任何一端都大的第三种值。
        return kotlin.math.round(value).toInt()
    }

    /** 循环相位按最短弧插值，处理 0.9 -> 0.1 这种跨界情况。 */
    private fun lerpPhase(from: Float, to: Float, t: Float): Float {
        val bounded = t.coerceIn(0f, 1f)
        var delta = to - from
        if (abs(delta) > 0.5f) {
            delta -= if (delta > 0f) 1f else -1f
        }
        val next = from + delta * bounded
        return ((next % 1f) + 1f) % 1f
    }
}
