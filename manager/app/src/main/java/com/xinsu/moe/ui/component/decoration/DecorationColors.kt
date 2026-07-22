package com.xinsu.moe.ui.component.decoration

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class DecorationColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val outline: Color,
    val inverse: Color,
    val surfaceGlow: Color,
)

@Composable
@ReadOnlyComposable
fun materialDecorationColors(): DecorationColors {
    val scheme = MaterialTheme.colorScheme
    return DecorationColors(
        primary = scheme.primary,
        secondary = scheme.secondary,
        tertiary = scheme.tertiary,
        outline = scheme.outline,
        inverse = scheme.inversePrimary,
        surfaceGlow = scheme.surfaceBright,
    )
}

@Composable
@ReadOnlyComposable
fun miuixDecorationColors(): DecorationColors {
    val scheme = MiuixTheme.colorScheme
    return DecorationColors(
        primary = scheme.primary,
        secondary = scheme.secondary,
        tertiary = scheme.tertiaryContainer,
        outline = scheme.outline,
        inverse = scheme.onPrimary,
        surfaceGlow = scheme.surfaceContainerHigh,
    )
}
