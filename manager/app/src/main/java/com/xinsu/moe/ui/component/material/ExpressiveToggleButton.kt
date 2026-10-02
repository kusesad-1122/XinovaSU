package com.xinsu.moe.ui.component.material

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// ToggleButton / ToggleButtonShapes / ToggleButtonColors are @material3expressive APIs.
// Without this opt-in the `ToggleButtonDefaults.colors(...)` call below fails to resolve
// (the project compiles material3 1.5.0-alpha19, where these are still experimental).
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = expressiveToggleButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shapes = shapes,
        content = content,
    )
}

/**
 * 调用的是 `toggleButtonColors(...)` 而非上游的 `colors(...)`：
 * material3 1.5.0-alpha19（本项目版本）里该工厂方法名为 `toggleButtonColors`，
 * 到 alpha28 才更名为 `colors`。签名兼容 —— 两者都是 6 个 Color 参数
 * （containerColor / contentColor / disabledContainerColor /
 * disabledContentColor / checkedContainerColor / checkedContentColor）
 * 且全部带默认值。
 *
 * 从 alpha19 的 AAR 里反查 class 常量池确认：
 *   androidx/compose/material3/ToggleButtonDefaults.class
 *   -> toggleButtonColors-5tl4gsc，签名为 (JJJJJJ...)ToggleButtonColors
 *   -> 不存在独立的 `colors` 方法
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun expressiveToggleButtonColors(
    checkedContainerColor: Color = MaterialTheme.colorScheme.primary,
    checkedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
    checkedContainerColor = checkedContainerColor,
    checkedContentColor = checkedContentColor,
    containerColor = containerColor,
    contentColor = contentColor,
)
