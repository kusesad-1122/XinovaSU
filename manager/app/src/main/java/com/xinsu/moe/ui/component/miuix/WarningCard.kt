package com.xinsu.moe.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinsu.moe.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType


@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    MiuixGlassCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        containerColor = color ?: when {
            isDynamicColor -> colorScheme.errorContainer
            isInDarkTheme() -> Color(0XFF310808)
            else -> Color(0xFFF8E2E2)
        },
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = if (isDynamicColor) colorScheme.onErrorContainer else Color(0xFFF72727),
                fontSize = 14.sp
            )
            action?.invoke()
        }
    }
}
