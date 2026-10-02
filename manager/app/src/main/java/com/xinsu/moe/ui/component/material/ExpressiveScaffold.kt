package com.xinsu.moe.ui.component.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xinsu.moe.ui.theme.scaffoldContainerColor

/**
 * XinovaSU 的 Material 页面骨架。
 *
 * 与上游 `ExpressiveScaffold` 的唯一差异在 `containerColor` 的默认值：上游直接用不透明的
 * `surfaceContainer`，而本项目支持「渐变 / 插画 / 剧场」三种应用级背景 —— 背景启用时页面
 * 骨架必须让位（透明），否则会把背景整块盖住，二次元插画与着色器背景就永远露不出来。
 * 这里的默认值因此走 [scaffoldContainerColor]，让每个页面自动跟随用户在设置里选的背景样式。
 */
@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = scaffoldContainerColor(MaterialTheme.colorScheme.surfaceContainer),
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

/**
 * 顶栏配色同样背景感知：背景启用时容器透明，让插画/着色器背景能延伸到状态栏区域，
 * 而滚动后仍保留一层轻微遮罩保证标题可读。
 */
@Composable
fun expressiveTopAppBarColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor: Color = containerColor,
): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = scaffoldContainerColor(containerColor),
    scrolledContainerColor = scrolledContainerColor,
)
