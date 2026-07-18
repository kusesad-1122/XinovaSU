package com.xinsu.moe.ui.theme.tokens

/**
 * Hand-curated composition and colour recipes for the non-pressure artwork set.
 *
 * The source images are analysed only by the offline tool under tools/theme_pipeline. Runtime
 * code receives fixed seeds, focal geometry, safe regions and atmosphere values from this file.
 * Semantic tones are expanded deterministically and every documented foreground/background pair
 * is checked by [ThemeTokenBundle] when the catalog is initialised.
 */
internal fun curatedThemeBundles(): List<ThemeTokenBundle> = listOf(
    curated(
        id = "ink-white-companions", displayNameKey = "theme_ink_white_companions",
        sourceFilename = "0A1E02817F50E67996CC9C814A7E3AA3.jpg",
        resourceName = "theme_ink_white_companions.jpg", width = 992, height = 1440,
        sha256 = "595158a7164f638ec3b08ae2bd79dc6bbc883b4988c83715e2ce5b5149d2dc61",
        focal = point(0.5f, 0.66f), safe = rect(0.24f, 0.34f, 0.77f, 0.86f),
        rail = rail(ScrimEdge.Top, 0f, 0.32f, 0.08f),
        primary = 0xFF252624L, secondary = 0xFF565655L, tertiary = 0xFF949493L,
        canvas = 0xFFD5D5D5L, focus = 0xFF111111L,
        roles = roles("黑色制服与墨线", "灰阶服装", "纸面纯白高光", "黑白纸张纹理"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.78f, glowIntensity = 0.08f,
    ),
    curated(
        id = "twin-peach-heartstrings", displayNameKey = "theme_twin_peach_heartstrings",
        sourceFilename = "0BF4BF2A4133A4E571C0AA624A825D87.jpg",
        resourceName = "theme_twin_peach_heartstrings.jpg", width = 1085, height = 1440,
        sha256 = "eb52c24dcc3429c55a8609ef18b152535094cbf83fb00a423027666468e3547d",
        focal = point(0.53f, 0.58f), safe = rect(0.22f, 0.17f, 0.78f, 0.94f),
        rail = rail(ScrimEdge.Top, 0f, 0.15f, 0.12f),
        primary = 0xFFB43E77L, secondary = 0xFF497A91L, tertiary = 0xFF66705DL,
        canvas = 0xFFE1D7D6L, focus = 0xFFD45A91L,
        roles = roles("双桃发色", "运动短裤蓝", "酒红发饰", "轻快暖白纸面"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Comfortable,
        cardOpacity = 0.8f, glowIntensity = 0.2f,
    ),
    curated(
        id = "winter-blue-scarf", displayNameKey = "theme_winter_blue_scarf",
        sourceFilename = "0E6936072A8D40EE0C7B1D11807DDEA7.jpg",
        resourceName = "theme_winter_blue_scarf.jpg", width = 1448, height = 1086,
        sha256 = "d4f8c310766f0a0fb42b1342e52af9415788d3ced90873e01c85bcd6c684cd02",
        focal = point(0.73f, 0.48f), safe = rect(0.58f, 0.08f, 0.89f, 0.92f),
        rail = rail(ScrimEdge.Top, 0f, 0.06f, 0.16f),
        primary = 0xFF35517DL, secondary = 0xFFA35F58L, tertiary = 0xFF7584B2L,
        canvas = 0xFFC0C2D8L, focus = 0xFF245EADL,
        roles = roles("雪夜制服蓝", "棕红发色", "亮蓝瞳色", "冬日雪雾"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.74f, glowIntensity = 0.25f,
    ),
    curated(
        id = "dusk-iron-wind", displayNameKey = "theme_dusk_iron_wind",
        sourceFilename = "11F610D3776BDD160FA0E4DACA17A3A9.jpg",
        resourceName = "theme_dusk_iron_wind.jpg", width = 1920, height = 1080,
        sha256 = "26c7ce6c24974b0b385b861b5c2cb6b49e4520e2b02fc42faaae183c44a4d85c",
        focal = point(0.18f, 0.43f), safe = rect(0.05f, 0.03f, 0.25f, 0.98f),
        rail = rail(ScrimEdge.End, 0.84f, 1f, 0.3f),
        primary = 0xFF272A42L, secondary = 0xFF625B6AL, tertiary = 0xFFA86F62L,
        canvas = 0xFF837B8CL, focus = 0xFFE6A184L,
        roles = roles("深海军蓝制服", "暮色灰紫", "夕照暖光", "铁桥与夜海阴影"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Compact,
        cardOpacity = 0.66f, glowIntensity = 0.34f,
    ),
    curated(
        id = "cloud-slope-stars", displayNameKey = "theme_cloud_slope_stars",
        sourceFilename = "1521B8BB3F8239CA188AF6D183C8C1D9.jpg",
        resourceName = "theme_cloud_slope_stars.jpg", width = 1440, height = 2278,
        sha256 = "7a576c0d0ee2cecab78ac325d6a922b114aae7b14187ce7d1d9bad278cb74f00",
        focal = point(0.3f, 0.62f), safe = rect(0f, 0.27f, 0.56f, 1f),
        rail = rail(ScrimEdge.Top, 0f, 0.25f, 0.18f),
        primary = 0xFF667690L, secondary = 0xFF454733L, tertiary = 0xFFA05E6AL,
        canvas = 0xFFD2D7E1L, focus = 0xFF8298B8L,
        roles = roles("天空灰蓝", "草坡深绿", "领巾暖红", "阴云与空气感"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.72f, glowIntensity = 0.2f,
    ),
    curated(
        id = "sakura-crown-overture", displayNameKey = "theme_sakura_crown_overture",
        sourceFilename = "1FEA93741D17CC6EE13A8BC9E8BF0510.jpg",
        resourceName = "theme_sakura_crown_overture.jpg", width = 1920, height = 1080,
        sha256 = "15275a123f7a218c2b0447461a7110cd03d7e8c0f481fff00db19bff1e6d0dc0",
        focal = point(0.69f, 0.42f), safe = rect(0.55f, 0.02f, 0.78f, 0.96f),
        rail = rail(ScrimEdge.End, 0.9f, 1f, 0.12f),
        primary = 0xFF9A4E6EL, secondary = 0xFF7A6C81L, tertiary = 0xFF8A6335L,
        canvas = 0xFFF4EBF5L, focus = 0xFFD58BA6L,
        roles = roles("樱花玫粉", "银灰长发", "皇冠暖金", "柔光紫白背景"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.76f, glowIntensity = 0.26f,
    ),
    curated(
        id = "golden-eye-cat-courtyard", displayNameKey = "theme_golden_eye_cat_courtyard",
        sourceFilename = "232493FF62B3C161DEFBE3A06F1D13C1.jpg",
        resourceName = "theme_golden_eye_cat_courtyard.jpg", width = 1440, height = 2564,
        sha256 = "35bb25246f2e062e2a741b9f02341b93496578df4ea510b400766d2d691c97fc",
        focal = point(0.58f, 0.6f), safe = rect(0.16f, 0.22f, 0.97f, 0.9f),
        rail = rail(ScrimEdge.Top, 0f, 0.19f, 0.24f),
        primary = 0xFF8B6218L, secondary = 0xFF8C2F32L, tertiary = 0xFF405133L,
        canvas = 0xFF939785L, focus = 0xFFD49A2AL,
        roles = roles("金色发瞳", "红色领结", "庭院叶绿", "制服与院落阴影"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Comfortable,
        cardOpacity = 0.74f, glowIntensity = 0.24f,
    ),
    curated(
        id = "mint-pull", displayNameKey = "theme_mint_pull",
        sourceFilename = "2C0A726DEF976FF8285891E7331CF062.jpg",
        resourceName = "theme_mint_pull.jpg", width = 810, height = 1440,
        sha256 = "4fdd40b2c0c2f935576fd8679767c8f8bbd36a6fe3d284fdceccdf508cc0b65c",
        focal = point(0.48f, 0.42f), safe = rect(0.08f, 0.03f, 0.78f, 0.98f),
        rail = rail(ScrimEdge.End, 0.88f, 1f, 0.2f),
        primary = 0xFF4A7667L, secondary = 0xFF4D6078L, tertiary = 0xFFA44645L,
        canvas = 0xFFB5C1BAL, focus = 0xFF78B79FL,
        roles = roles("薄荷长发", "制服灰蓝", "红色领结", "树影与清凉高光"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Comfortable,
        cardOpacity = 0.7f, glowIntensity = 0.28f,
    ),
    curated(
        id = "ink-order-poster", displayNameKey = "theme_ink_order_poster",
        sourceFilename = "32590DE0FF6E76FE4C1043B10F72FAD8.jpg",
        resourceName = "theme_ink_order_poster.jpg", width = 1440, height = 1920,
        sha256 = "08bc9cde8b5d53c34b00e8c67de2ada44e11b48db6217d3735ae03d0a4a0dcf7",
        focal = point(0.72f, 0.37f), safe = rect(0.55f, 0.04f, 0.9f, 0.88f),
        rail = rail(ScrimEdge.Start, 0f, 0.25f, 0.1f),
        primary = 0xFF1D181FL, secondary = 0xFF565156L, tertiary = 0xFF8A8580L,
        canvas = 0xFFE5E0D6L, focus = 0xFF111111L,
        roles = roles("冷黑墨色", "石墨灰", "纸白留痕", "高对比海报纸面"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Compact,
        cardOpacity = 0.82f, glowIntensity = 0.06f,
    ),
    curated(
        id = "cobalt-night-dress", displayNameKey = "theme_cobalt_night_dress",
        sourceFilename = "40DD6B4383F52CC25661DD6131F2A498.jpg",
        resourceName = "theme_cobalt_night_dress.jpg", width = 1440, height = 2062,
        sha256 = "c16b3eb365e9fd9b89498a8be4212971d73c953e83f4b3db06ba432e5d38a0f7",
        focal = point(0.52f, 0.57f), safe = rect(0.25f, 0.3f, 0.78f, 0.98f),
        rail = rail(ScrimEdge.Top, 0f, 0.25f, 0.24f),
        primary = 0xFF2656BAL, secondary = 0xFF4A86DBL, tertiary = 0xFF3F7049L,
        canvas = 0xFF192440L, focus = 0xFF66A9F1L,
        roles = roles("天空钴蓝", "云层亮蓝", "风叶绿色", "黑裙与夜蓝阴影"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Airy,
        cardOpacity = 0.64f, glowIntensity = 0.44f,
    ),
    curated(
        id = "clear-sky-blue-ribbon", displayNameKey = "theme_clear_sky_blue_ribbon",
        sourceFilename = "4588AAD2877227D8848C715102D2EB28.jpg",
        resourceName = "theme_clear_sky_blue_ribbon.jpg", width = 1920, height = 1080,
        sha256 = "9b62f83899a5eebd2da87331b6639e995c29ed7eeaedeae1ce1425e5998d548a",
        focal = point(0.72f, 0.43f), safe = rect(0.62f, 0.05f, 0.84f, 0.95f),
        rail = rail(ScrimEdge.Top, 0f, 0.04f, 0.24f),
        primary = 0xFF3C5178L, secondary = 0xFF7B6F8AL, tertiary = 0xFFA86D78L,
        canvas = 0xFF9AABC7L, focus = 0xFF5E769BL,
        roles = roles("晴空蓝", "灰紫裙影", "暖白日光", "蓝缎裙装与天空"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.7f, glowIntensity = 0.28f,
    ),
    curated(
        id = "windfield-doll", displayNameKey = "theme_windfield_doll",
        sourceFilename = "695B928B5B8BFAD0E65B3355949EDD17.jpg",
        resourceName = "theme_windfield_doll.jpg", width = 1920, height = 1442,
        sha256 = "dc40e5bd438b5e3dcb7b23fc5c58c6649add68cb665388732196a80331e48adb",
        focal = point(0.75f, 0.58f), safe = rect(0.61f, 0.22f, 0.9f, 0.96f),
        rail = rail(ScrimEdge.Top, 0f, 0.18f, 0.3f),
        primary = 0xFF515B18L, secondary = 0xFF2A5870L, tertiary = 0xFF98A331L,
        canvas = 0xFF575C4DL, focus = 0xFFDAE04CL,
        roles = roles("风场橄榄绿", "礼服深蓝", "草尖亮黄绿", "阴云与森林暗部"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Comfortable,
        cardOpacity = 0.68f, glowIntensity = 0.3f,
    ),
    curated(
        id = "crimson-eye-jump", displayNameKey = "theme_crimson_eye_jump",
        sourceFilename = "75E8D9ADE2A4591B654F43134E8C0140.jpg",
        resourceName = "theme_crimson_eye_jump.jpg", width = 1440, height = 3140,
        sha256 = "0e660a8fb49a8550b183f49314eb1f58c1b834165be1749a89593639346f1e94",
        focal = point(0.5f, 0.7f), safe = rect(0f, 0.38f, 1f, 1f),
        rail = rail(ScrimEdge.Top, 0f, 0.34f, 0.08f),
        primary = 0xFF1A386CL, secondary = 0xFF9A3F2BL, tertiary = 0xFF7B1725L,
        canvas = 0xFFC2C4D3L, focus = 0xFF4B73A7L,
        roles = roles("制服深蓝", "赤棕发色", "酒红蝴蝶结", "纯白跳镜背景"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.8f, glowIntensity = 0.18f,
    ),
    curated(
        id = "cream-street-corner", displayNameKey = "theme_cream_street_corner",
        sourceFilename = "7788D7E5779624A11DE1B69FC0226893.jpg",
        resourceName = "theme_cream_street_corner.jpg", width = 1440, height = 2560,
        sha256 = "f6be878aa0be32a1d61f50396e7a031b5a436180632e042eff38fc0a049d6d30",
        focal = point(0.42f, 0.58f), safe = rect(0.15f, 0.22f, 0.68f, 0.98f),
        rail = rail(ScrimEdge.End, 0.88f, 1f, 0.24f),
        primary = 0xFF3E4D5AL, secondary = 0xFFA86F78L, tertiary = 0xFF8B6E3FL,
        canvas = 0xFFC1CBD1L, focus = 0xFF92B9E5L,
        roles = roles("蓝灰提袋", "雾粉针织", "奶油暖光", "街角建筑与日影"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Comfortable,
        cardOpacity = 0.76f, glowIntensity = 0.2f,
    ),
    curated(
        id = "black-rose-stone-court", displayNameKey = "theme_black_rose_stone_court",
        sourceFilename = "7DF499D2C7BD15EB0E51E650A5A6441A.jpg",
        resourceName = "theme_black_rose_stone_court.jpg", width = 813, height = 1440,
        sha256 = "eac94752d01b6f6dde4a8e58075d254f8f5ace65d59f11b664ecc82981f2e1f1",
        focal = point(0.5f, 0.46f), safe = rect(0.12f, 0.04f, 0.82f, 0.9f),
        rail = rail(ScrimEdge.Bottom, 0.94f, 1f, 0.22f),
        primary = 0xFF27345FL, secondary = 0xFF8F1933L, tertiary = 0xFF4D4E37L,
        canvas = 0xFF6F7163L, focus = 0xFFB32143L,
        roles = roles("深蓝裙摆", "绯红瞳色", "苔石橄榄", "黑发与石墙阴影"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Compact,
        cardOpacity = 0.7f, glowIntensity = 0.3f,
    ),
    curated(
        id = "pink-mist-night-window", displayNameKey = "theme_pink_mist_night_window",
        sourceFilename = "CB43A7800E9534195B018D0867B426B0.jpg",
        resourceName = "theme_pink_mist_night_window.jpg", width = 864, height = 1440,
        sha256 = "59a139e4cd95370c22a19fc9132fe7b874d378df8bd78e2e72f03a9949935d90",
        focal = point(0.4f, 0.42f), safe = rect(0.03f, 0.03f, 0.73f, 0.92f),
        rail = rail(ScrimEdge.Bottom, 0.94f, 1f, 0.24f),
        primary = 0xFF9A4679L, secondary = 0xFF4D3E3CL, tertiary = 0xFF64799CL,
        canvas = 0xFFE0D3DEL, focus = 0xFFC05D9EL,
        roles = roles("粉色外套", "夜窗棕黑", "霓虹粉紫", "深蓝窗框与室内阴影"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Comfortable,
        cardOpacity = 0.68f, glowIntensity = 0.42f,
    ),
    curated(
        id = "frost-white-crimson-eye", displayNameKey = "theme_frost_white_crimson_eye",
        sourceFilename = "D6EA253F6D7F57DE509845A87EE3796D.jpg",
        resourceName = "theme_frost_white_crimson_eye.jpg", width = 1264, height = 1580,
        sha256 = "c49c614cb5ca7c2fa7cef9dd47131284fc35736748749979c087372a31b0140f",
        focal = point(0.78f, 0.48f), safe = rect(0.65f, 0.05f, 0.98f, 0.96f),
        rail = rail(ScrimEdge.Start, 0f, 0.35f, 0.08f),
        primary = 0xFF5A6E94L, secondary = 0xFF9C2440L, tertiary = 0xFFABB2C4L,
        canvas = 0xFFF3EFE6L, focus = 0xFFB52A49L,
        roles = roles("冰蓝发色", "绯红瞳色", "霜白高光", "暖白衣装与留白"),
        preferred = ThemeModePreference.Light, density = ThemeDensity.Airy,
        cardOpacity = 0.8f, glowIntensity = 0.16f,
    ),
    curated(
        id = "blue-flame-cat-shadow", displayNameKey = "theme_blue_flame_cat_shadow",
        sourceFilename = "FC1557EDD2F7352DA626DE0E1341046D.jpg",
        resourceName = "theme_blue_flame_cat_shadow.jpg", width = 900, height = 1500,
        sha256 = "5d357c1afd87cdff008c058f3aed9556b9b36b4729fa2fabddfb78e3bb217a13",
        focal = point(0.46f, 0.46f), safe = rect(0.1f, 0.02f, 0.82f, 0.92f),
        rail = rail(ScrimEdge.Bottom, 0.94f, 1f, 0.16f),
        primary = 0xFF283667L, secondary = 0xFF168CCCL, tertiary = 0xFFA93250L,
        canvas = 0xFFBAB9CBL, focus = 0xFF25A9E0L,
        roles = roles("深蓝制服与发色", "袖带亮青", "红瞳与手套", "纸白背景与制服暗部"),
        preferred = ThemeModePreference.Dark, density = ThemeDensity.Compact,
        cardOpacity = 0.72f, glowIntensity = 0.4f,
    ),
)

private data class AccentSet(
    val color: Long,
    val onColor: Long,
    val container: Long,
    val onContainer: Long,
    val inverse: Long,
)

private fun curated(
    id: String,
    displayNameKey: String,
    sourceFilename: String,
    resourceName: String,
    width: Int,
    height: Int,
    sha256: String,
    focal: NormalizedPoint,
    safe: NormalizedRect,
    rail: ScrimRail,
    primary: Long,
    secondary: Long,
    tertiary: Long,
    canvas: Long,
    focus: Long,
    roles: Map<String, String>,
    preferred: ThemeModePreference,
    density: ThemeDensity,
    cardOpacity: Float,
    glowIntensity: Float,
): ThemeTokenBundle = ThemeTokenBundle(
    id = id,
    displayNameKey = displayNameKey,
    artwork = ThemeArtworkTokens(
        resourceName = resourceName,
        width = width,
        height = height,
        focalPoint = focal,
        subjectSafeRect = safe,
        scaleMode = ArtworkScaleMode.Crop,
        scrimRails = listOf(rail),
    ),
    light = semanticFromSeeds(primary, secondary, tertiary, canvas, focus, isDark = false),
    dark = semanticFromSeeds(primary, secondary, tertiary, canvas, focus, isDark = true),
    atmosphere = AtmosphereTokens(
        cardOpacity = cardOpacity,
        chromeOpacity = (cardOpacity - 0.12f).coerceIn(0f, 1f),
        readableOpacity = 0.92f,
        outlineOpacity = if (preferred == ThemeModePreference.Dark) 0.34f else 0.28f,
        glowColor = if (preferred == ThemeModePreference.Dark) mix(focus, WHITE, 0.34f) else focus,
        glowIntensity = glowIntensity,
        localScrimStrength = rail.strength,
        cornerRadiusDp = if (density == ThemeDensity.Compact) 20f else 26f,
        blurRadiusDp = if (preferred == ThemeModePreference.Dark) 32f else 26f,
        density = density,
        preferredMode = preferred,
    ),
    provenance = ThemeProvenance(
        sourceFilename = sourceFilename,
        sha256 = sha256,
        rightsStatus = ThemeRightsStatus.UserProvidedPendingPublicationConfirmation,
        roleNotes = roles,
        pipelineVersion = "1.0.0",
        contrastValidated = true,
    ),
)

private fun semanticFromSeeds(
    primarySeed: Long,
    secondarySeed: Long,
    tertiarySeed: Long,
    canvasSeed: Long,
    focusSeed: Long,
    isDark: Boolean,
): SemanticColorTokens {
    val primary = accent(primarySeed, isDark)
    val secondary = accent(secondarySeed, isDark)
    val tertiary = accent(tertiarySeed, isDark)
    val background = if (isDark) mix(canvasSeed, BLACK, 0.84f) else mix(canvasSeed, WHITE, 0.94f)
    val surface = if (isDark) mix(canvasSeed, BLACK, 0.78f) else mix(canvasSeed, WHITE, 0.985f)
    val surfaceVariant = if (isDark) mix(canvasSeed, BLACK, 0.58f) else mix(canvasSeed, WHITE, 0.8f)
    val onBackground = readable(if (isDark) mix(canvasSeed, WHITE, 0.82f) else mix(canvasSeed, BLACK, 0.82f), background)
    val onSurface = readable(onBackground, surface)
    val onSurfaceVariant = readable(
        if (isDark) mix(canvasSeed, WHITE, 0.72f) else mix(canvasSeed, BLACK, 0.7f),
        surfaceVariant,
    )
    val inverseSurface = onBackground
    val inverseOnSurface = readable(background, inverseSurface)

    return SemanticColorTokens(
        primary = primary.color,
        onPrimary = primary.onColor,
        primaryContainer = primary.container,
        onPrimaryContainer = primary.onContainer,
        inversePrimary = primary.inverse,
        secondary = secondary.color,
        onSecondary = secondary.onColor,
        secondaryContainer = secondary.container,
        onSecondaryContainer = secondary.onContainer,
        tertiary = tertiary.color,
        onTertiary = tertiary.onColor,
        tertiaryContainer = tertiary.container,
        onTertiaryContainer = tertiary.onContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = if (isDark) mix(canvasSeed, BLACK, 0.92f) else WHITE,
        surfaceContainerLow = if (isDark) mix(canvasSeed, BLACK, 0.82f) else mix(canvasSeed, WHITE, 0.95f),
        surfaceContainer = if (isDark) mix(canvasSeed, BLACK, 0.75f) else mix(canvasSeed, WHITE, 0.9f),
        surfaceContainerHigh = if (isDark) mix(canvasSeed, BLACK, 0.66f) else mix(canvasSeed, WHITE, 0.84f),
        surfaceContainerHighest = if (isDark) mix(canvasSeed, BLACK, 0.56f) else mix(canvasSeed, WHITE, 0.76f),
        surfaceBright = if (isDark) mix(canvasSeed, BLACK, 0.48f) else mix(canvasSeed, WHITE, 0.99f),
        surfaceDim = if (isDark) mix(canvasSeed, BLACK, 0.91f) else mix(canvasSeed, WHITE, 0.72f),
        outline = if (isDark) mix(canvasSeed, WHITE, 0.56f) else mix(canvasSeed, BLACK, 0.48f),
        outlineVariant = if (isDark) mix(canvasSeed, BLACK, 0.42f) else mix(canvasSeed, WHITE, 0.62f),
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = if (isDark) 0xFFFFB4ABL else 0xFFBA1A1AL,
        onError = if (isDark) 0xFF5C1A1AL else WHITE,
        errorContainer = if (isDark) 0xFF7A2A2AL else 0xFFFFDAD6L,
        onErrorContainer = if (isDark) 0xFFFFDAD6L else 0xFF410002L,
        scrim = BLACK,
        focus = if (isDark) mix(focusSeed, WHITE, 0.36f) else safeBackground(focusSeed, WHITE, BLACK),
    )
}

private fun accent(seed: Long, isDark: Boolean): AccentSet {
    val lightStrong = safeBackground(seed, WHITE, BLACK)
    val darkStrong = mix(seed, WHITE, 0.58f)
    return if (isDark) {
        val container = mix(seed, BLACK, 0.52f)
        AccentSet(
            color = darkStrong,
            onColor = readable(mix(seed, BLACK, 0.75f), darkStrong),
            container = container,
            onContainer = readable(mix(seed, WHITE, 0.82f), container),
            inverse = lightStrong,
        )
    } else {
        val container = mix(seed, WHITE, 0.84f)
        AccentSet(
            color = lightStrong,
            onColor = WHITE,
            container = container,
            onContainer = readable(mix(seed, BLACK, 0.72f), container),
            inverse = darkStrong,
        )
    }
}

private fun readable(candidate: Long, background: Long): Long {
    if (ThemeColorMath.contrastRatio(candidate, background) >= MIN_CONTRAST) return candidate
    val blackRatio = ThemeColorMath.contrastRatio(BLACK, background)
    val whiteRatio = ThemeColorMath.contrastRatio(WHITE, background)
    val target = if (blackRatio >= whiteRatio) BLACK else WHITE
    for (step in 1..20) {
        val adjusted = mix(candidate, target, step / 20f)
        if (ThemeColorMath.contrastRatio(adjusted, background) >= MIN_CONTRAST) return adjusted
    }
    return target
}

private fun safeBackground(candidate: Long, foreground: Long, target: Long): Long {
    if (ThemeColorMath.contrastRatio(foreground, candidate) >= MIN_CONTRAST) return candidate
    for (step in 1..20) {
        val adjusted = mix(candidate, target, step / 20f)
        if (ThemeColorMath.contrastRatio(foreground, adjusted) >= MIN_CONTRAST) return adjusted
    }
    return target
}

private fun mix(from: Long, to: Long, amount: Float): Long {
    val ratio = amount.coerceIn(0f, 1f)
    fun channel(start: Int, end: Int): Int = (start + (end - start) * ratio).toInt().coerceIn(0, 255)
    return ThemeColorMath.argb(
        alpha = 255,
        red = channel(ThemeColorMath.red(from), ThemeColorMath.red(to)),
        green = channel(ThemeColorMath.green(from), ThemeColorMath.green(to)),
        blue = channel(ThemeColorMath.blue(from), ThemeColorMath.blue(to)),
    )
}

private fun point(x: Float, y: Float) = NormalizedPoint(x, y)
private fun rect(left: Float, top: Float, right: Float, bottom: Float) = NormalizedRect(left, top, right, bottom)
private fun rail(edge: ScrimEdge, start: Float, end: Float, strength: Float) = ScrimRail(edge, start, end, strength)
private fun roles(primary: String, secondary: String, focus: String, surface: String) = mapOf(
    "primary" to primary,
    "secondary" to secondary,
    "focus" to focus,
    "surface" to surface,
)

private const val MIN_CONTRAST = 4.5
private const val WHITE = 0xFFFFFFFFL
private const val BLACK = 0xFF000000L
