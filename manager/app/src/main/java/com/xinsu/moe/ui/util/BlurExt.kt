package com.xinsu.moe.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.xinsu.moe.ui.component.liquid.LiquidGlassSpec
import com.xinsu.moe.ui.theme.LocalCardBackdrop
import com.xinsu.moe.ui.theme.LocalEnableBlur
import com.xinsu.moe.ui.theme.LocalGlassBlurRadius
import com.xinsu.moe.ui.theme.LocalLiquidGlassSetting
import com.xinsu.moe.ui.theme.LocalTopBarGlassSetting
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
// miuix 0.9.2 起该符号从 kmp.blur 搬到 kmp.shader
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 为栏创建一个只采样"应用背景层"的 backdrop。
 *
 * [enableBlur] 是调用方自己那层的意图（栏的毛玻璃档），但**液态玻璃不该依赖模糊开关** ——
 * 实测用户一关"模糊"，液态玻璃效果就整片消失。所以这里把两个液态玻璃开关一起或进来：
 * 只要 模糊 / 液态玻璃 / 顶栏液态玻璃 任一开启就建 backdrop，具体某个面用不用由各面自己决定。
 */
@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    val anyGlassTierEnabled =
        enableBlur || LocalLiquidGlassSetting.current || LocalTopBarGlassSetting.current
    if (!anyGlassTierEnabled || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 栏材质宿主：顶栏、非悬浮底栏、横屏导航栏都走这里，所以栏的玻璃材质只需在这一处切换。
 *
 * 两档都走 textureBlur 管线（不用 drawBackdrop + lens，原因见下方 useLiquidGlass 分支的注释），
 * 区别只在于液态玻璃档额外给一条边缘高光：
 *   • [liquidGlass] = true → 模糊 + [LiquidGlassSpec.BarScrimAlpha] 底色 + [LiquidGlassSpec.HighlightWidth] 高光
 *   • 「模糊」开关 = true  → 模糊 + [LiquidGlassSpec.BarScrimAlpha] 底色（无高光）
 *   • 两者都关            → 完全透明，壁纸直接透出来
 *
 * [liquidGlass] 默认取 [LocalTopBarGlassSetting]：顶栏由独立开关控制；底栏 / 导航栏的调用点
 * 显式传入 [barLiquidGlassEnabled]（跟随"液态玻璃"总开关）。
 *
 * @param useAppBackdrop 是否优先采样「应用背景层」那个全应用 backdrop（[LocalCardBackdrop]）。
 *   顶栏必须为 true —— 顶栏下方通常是页面的**空白区**（LazyColumn 的 contentPadding），
 *   而页面自己的 backdrop 基底是**不透明 surface 白**，采样到那一块时顶栏就是一整条纯白，
 *   这正是用户反馈的"每个页面都会出现的白"。改用捕获壁纸的全局 backdrop 后，
 *   顶栏才真正是"模糊的壁纸 + 淡底色"。
 *   底栏 / 导航栏保持 false：它们浮在内容之上，要模糊的是滚过去的卡片，不是壁纸。
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    liquidGlass: Boolean = LocalTopBarGlassSetting.current,
    useAppBackdrop: Boolean = true,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val density = LocalDensity.current
    val frostedTierEnabled = LocalEnableBlur.current
    val blurRadiusDp = LocalGlassBlurRadius.current
    val appBackdrop = LocalCardBackdrop.current
    val barBackdrop = if (useAppBackdrop) appBackdrop ?: backdrop else backdrop
    val useLiquidGlass = blurActive && liquidGlass && barBackdrop != null && isRuntimeShaderSupported()
    // 毛玻璃档必须同时满足「模糊」开关打开：backdrop 现在会因为液态玻璃开关而存在，
    // 只看 backdrop != null 的话，关掉模糊后顶栏仍会铺一层白（实测会退化成用户截图那张）。
    val useFrosted = blurActive && !useLiquidGlass && frostedTierEnabled && barBackdrop != null

    // 栏贴在屏幕顶边，只圆底部两角（顶部两角与屏幕边缘齐平）。原先是 RectangleShape 直角，
    // 实测用户反馈"顶栏这个模糊是方形的，可以考虑把它的边角变成圆润的"。
    val barShape = RoundedCornerShape(
        bottomStart = LiquidGlassSpec.BarCornerRadius,
        bottomEnd = LiquidGlassSpec.BarCornerRadius,
    )
    val barRadiusPx = with(density) { blurRadiusDp.toPx() }

    val barModifier = when {
        // ⚠️ 栏上不能给 Highlight —— 实测 miuix 的 Highlight（BloomStroke）在整幅矩形栏上
        // 会把表面渲染成纯白：同样 0.35 底色、同样 textureBlur，加 highlight 后顶栏像素是
        // 全平 (254,254,254)，不加则能看到模糊后的壁纸（164,189,157 / 171,195,246 这类真实色彩）。
        // 之前"顶栏是方的、里面多出两个圆润边角"也是同一来源。
        // 另外栏也不能走 drawBackdrop + lens：折射要把采样窗口按 refractionAmount 外扩
        // （98px），栏贴着屏幕上边缘，外扩区落到捕获图层外被钳到 backdrop 的白色基底 → 整幅变白。
        // 结论：栏统一走 textureBlur + 低透明底色，靠底色和后景表现玻璃质感。
        useLiquidGlass -> Modifier.textureBlur(
            backdrop = barBackdrop,
            shape = barShape,
            blurRadius = barRadiusPx,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = surfaceColor.copy(LiquidGlassSpec.BarScrimAlpha)),
                ),
            ),
        )

        useFrosted -> Modifier.textureBlur(
            backdrop = barBackdrop,
            shape = barShape,
            // 原先写死 25f；现在跟"毛玻璃模糊度"滑块统一，两个档位的观感才一致。
            blurRadius = barRadiusPx,
            colors = BlurColors(
                blendColors = listOf(
                    // 原来是 0.87 —— 87% 不透明白把壁纸盖成一片纯白（用户反馈的"突兀白条"）。
                    BlendColorEntry(color = surfaceColor.copy(LiquidGlassSpec.BarScrimAlpha)),
                ),
            ),
        )

        else -> Modifier
    }

    Box(modifier = barModifier) {
        content()
    }
}

/**
 * 底栏 / 横屏导航栏是否使用液态玻璃 —— 跟随"液态玻璃"总开关（[LocalLiquidGlassSetting]）。
 * 顶栏不用这个，顶栏走 [BlurredBar] 的默认值，由独立的顶栏开关控制。
 */
@Composable
fun barLiquidGlassEnabled(): Boolean = LocalLiquidGlassSetting.current
