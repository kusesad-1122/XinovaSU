package com.xinsu.moe.ui.component.decoration

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationCatalog
import com.xinsu.moe.ui.theme.decoration.ThemeMorphFrame
import com.xinsu.moe.ui.theme.decoration.ThemeMorphTimeline
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpec

/**
 * 当前主题形变帧。绘制层读它来决定画什么、以及要不要叠加过渡光扫。
 * 形变结束后这里就是目标配方本身，所以绘制层不需要区分"平时"和"形变中"两套代码路径。
 */
val LocalThemeMorphFrame = staticCompositionLocalOf {
    ThemeMorphFrame(
        spec = ThemeDecorationCatalog.defaultSpec,
        progress = 1f,
        eased = 1f,
        sweep = 0f,
        emitTransitionParticles = false,
    )
}

/**
 * 在 [targetSpec] 变化时把装饰从旧配方平滑形变到新配方。
 *
 * 挂载点应当在 App 根部（与 [LocalThemeDecorationSpec] 的提供点同层），这样整棵树的装饰卡
 * 都会跟着一起过渡 —— 用户在主题画廊里点一下，看到的是全屏卡片同步变形，而不是部分卡片
 * 先变、部分卡片后变。
 *
 * 关键取舍：
 *  - 首次组合（没有"旧配方"）直接落到目标值，不播放形变，否则每次进设置都会闪一次；
 *  - 结构完全相同的两套配方（[ThemeMorphTimeline.sameStructure]）跳过形变，只换配色 ——
 *    例如在两个 keyColor 之间切换时，图案没变，强行播放 760ms 的形变只会拖慢响应；
 *  - 系统关闭动画（无障碍"移除动画"）时退化为瞬时切换。
 */
@Composable
fun RememberThemeMorphHost(
    targetSpec: ThemeDecorationSpec,
    content: @Composable () -> Unit,
) {
    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    val progress = remember { Animatable(1f) }
    // 形变起点：上一套已提交的配方。用 mutableStateOf 而非裸数组 —— 组合期读、协程期写，
    // 必须是可观察状态，否则读取方拿不到通知。
    var morphFrom by remember { mutableStateOf(targetSpec) }
    // 首次组合时为 false，之后恒为 true。用于区分"进入 App"与"切换主题"，
    // 前者不应播放形变。
    val initialized = remember { mutableStateOf(false) }

    LaunchedEffect(targetSpec, reduceMotion) {
        val prior = morphFrom
        if (!initialized.value || reduceMotion ||
            ThemeMorphTimeline.sameStructure(prior, targetSpec)
        ) {
            // 同结构（仅换配色）或系统关闭动画：直接落到目标，不产生中间态。
            morphFrom = targetSpec
            initialized.value = true
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        initialized.value = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ThemeMorphTimeline.DurationMillis),
        )
        // 动画结束后才把起点推到新配方，下次切换才有正确的 from。
        morphFrom = targetSpec
    }

    val frame = remember(progress.value, morphFrom, targetSpec) {
        ThemeMorphTimeline.frame(
            from = morphFrom,
            to = targetSpec,
            rawProgress = progress.value,
            sameRecipe = ThemeMorphTimeline.sameStructure(morphFrom, targetSpec),
        )
    }

    CompositionLocalProvider(LocalThemeMorphFrame provides frame) {
        content()
    }
}

/**
 * 形变过渡光扫：一条从起点扫向终点的柔光带，只在 [LocalThemeMorphFrame] 的 sweep 窗口内绘制。
 *
 * 由各卡片的 drawBehind 自行调用，可自行决定叠在装饰之上还是之下。[sweep] 为 0（未处于形变
 * 窗口内）或亮度已衰减到 0 时直接返回，稳态下没有额外绘制开销。
 */
fun DrawScope.drawThemeMorphSweep(
    accent: Color,
    sweep: Float,
    edgeInset: Float = 0f,
) {
    // 亮度算法集中在 ThemeMorphTimeline，避免与时间线实现漂移。
    val alpha = ThemeMorphTimeline.sweepAlpha(sweep)
    if (alpha <= 0.01f) return
    val bandWidth = size.width * 0.32f
    val x = edgeInset + (size.width - edgeInset * 2f) * sweep
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to accent.copy(alpha = alpha),
            1f to Color.Transparent,
            startX = x - bandWidth,
            endX = x + bandWidth,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, size.height),
    )
}
