package com.xinsu.moe.ui.theme.decoration

import androidx.compose.runtime.Immutable
import com.xinsu.moe.ui.theme.KawaiiPalette

/**
 * 把「32 套装饰配方」映射到「可渲染的主题」。
 *
 * 两套体系的 id 命名不一致，这是本项目一个长期的坑：
 *  - [ThemeDecorationCatalog] 用连字符小写 + 6 个现代主题用首字母大写
 *    （"sakura-street-walk"、"Cyber"、"SakuraVN"…）
 *  - [com.xinsu.moe.ui.theme.BuiltInThemes] 里 artwork 主题用连字符小写，
 *    预设色主题用大写（"Cyber"、"Obsidian"…），且**只有 artwork 主题有 tokenBundleId**
 *
 * 而 [com.xinsu.moe.ui.theme.MaterialXinovaSUTheme] 的配色优先级是：
 *   1. BuiltInThemes.byId(themePresetId)?.tokenBundleId -> BuiltInThemeCatalog
 *   2. appSettings.themePreset.isActive               -> KawaiiPalette 预设色
 *   3. 否则 dynamicColor（系统取色）
 *
 * 所以要在截图里拿到某套装饰配方的**真实配色**，必须传对 themePresetId。
 * 之前第一版截图选了 Sakura + Cyber，背景像素完全相同，正是因为 pastel 族
 * （Sakura/Mint/Lavender/Cyber）共用 KawaiiDark 画布 —— 见 [KawaiiPalette] 文件头注释。
 */
@Immutable
data class RenderableTheme(
    /** [ThemeDecorationCatalog] 里的 recipe id，注意大小写。 */
    val decorationId: String,
    /** 传给 MaterialXinovaSUTheme 的 themePresetId；null 表示该配方无对应预设色。 */
    val presetId: String?,
    /** 人类可读标签，用于测试名与报告。 */
    val label: String,
) {
    val preset: KawaiiPalette? get() = presetId?.let { KawaiiPalette.fromName(it) }
}

/**
 * 全部 32 套装饰配方。
 *
 * 数据来源是 [ThemeDecorationCatalog.AUTHORED_RECIPES] 与
 * [com.xinsu.moe.ui.theme.BuiltInThemes.all] 的对照结果，而非手写 ——
 * 手写必然会和枚举漂移。
 */
object RenderableThemes {

    /** pastel 族：共用 KawaiiDark/KawaiiLight 画布，只有 accents 不同。 */
    private val PASTEL = setOf("Sakura", "Mint", "Lavender", "Cyber", "SakuraVN")

    val all: List<RenderableTheme> = listOf(
        // ── artwork 主题：BuiltInThemes 与 ThemeDecorationCatalog 同名，
        //    有 tokenBundleId，配色走 BuiltInThemeCatalog。
        RenderableTheme("ink-white-companions", "ink-white-companions", "水墨 companions"),
        RenderableTheme("twin-peach-heartstrings", "twin-peach-heartstrings", "双桃"),
        RenderableTheme("winter-blue-scarf", "winter-blue-scarf", "冬蓝围巾"),
        RenderableTheme("dusk-iron-wind", "dusk-iron-wind", "暮色铁风"),
        RenderableTheme("cloud-slope-stars", "cloud-slope-stars", "云坡星"),
        RenderableTheme("sakura-street-walk", "sakura-street-walk", "樱街"),
        RenderableTheme("sakura-crown-overture", "sakura-crown-overture", "樱冠"),
        RenderableTheme("golden-eye-cat-courtyard", "golden-eye-cat-courtyard", "金眸猫"),
        RenderableTheme("mint-pull", "mint-pull", "薄荷"),
        RenderableTheme("ink-order-poster", "ink-order-poster", "水墨海报"),
        RenderableTheme("moonlit-silver-blue", "moonlit-silver-blue", "月银"),
        RenderableTheme("cobalt-night-dress", "cobalt-night-dress", "钴蓝夜裙"),
        RenderableTheme("clear-sky-blue-ribbon", "clear-sky-blue-ribbon", "晴空蓝带"),
        RenderableTheme("windfield-doll", "windfield-doll", "风原"),
        RenderableTheme("crimson-eye-jump", "crimson-eye-jump", "绯红之眼"),
        RenderableTheme("cream-street-corner", "cream-street-corner", "奶油街角"),
        RenderableTheme("black-rose-stone-court", "black-rose-stone-court", "黑蔷薇石庭"),
        RenderableTheme("sea-breeze-song", "sea-breeze-song", "海风歌"),
        RenderableTheme("pink-mist-night-window", "pink-mist-night-window", "粉雾夜窗"),
        RenderableTheme("frost-white-crimson-eye", "frost-white-crimson-eye", "霜白绯眼"),
        RenderableTheme("blue-flame-cat-shadow", "blue-flame-cat-shadow", "蓝焰猫影"),
        // ── 预设色主题：无 tokenBundleId，配色走 KawaiiPalette。
        //    前 5 个属 pastel 族（共用画布），后 6 个各有独立画布。
        RenderableTheme("SakuraVN", "SakuraVN", "樱 VN"),
        RenderableTheme("Snow", "Snow", "雪"),
        RenderableTheme("Moonlit", "Moonlit", "月"),
        RenderableTheme("Sakura", "Sakura", "樱"),
        RenderableTheme("Mint", "Mint", "薄荷"),
        RenderableTheme("Lavender", "Lavender", "薰衣草"),
        RenderableTheme("Cyber", "Cyber", "赛博"),
        RenderableTheme("Obsidian", "Obsidian", "黑曜"),
        RenderableTheme("Mica", "Mica", "云母"),
        RenderableTheme("Ember", "Ember", "余烬"),
        RenderableTheme("Jade", "Jade", "玉"),
    )

    /** 校验：all 的 id 必须与 ThemeDecorationCatalog 的 recipe 一一对应，不多不少。 */
    fun validateAgainstCatalog(): List<String> {
        val catalogIds = ThemeDecorationCatalog.all.keys
        val ourIds = all.map { it.decorationId }.toSet()
        val missing = catalogIds - ourIds
        val extra = ourIds - catalogIds
        return missing.map { "MISSING: $it" } + extra.map { "EXTRA: $it" }
    }

    /** 是否属 pastel 族（共用 KawaiiDark 画布，配色天然接近）。 */
    fun isPastel(presetId: String?): Boolean = presetId in PASTEL
}
