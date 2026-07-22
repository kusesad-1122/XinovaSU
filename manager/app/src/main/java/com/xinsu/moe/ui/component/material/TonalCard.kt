package com.xinsu.moe.ui.component.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.xinsu.moe.ui.component.decoration.DecoratedCardContent
import com.xinsu.moe.ui.component.decoration.materialDecorationColors
import com.xinsu.moe.ui.theme.LocalGlassCard
import com.xinsu.moe.ui.theme.LocalHomeCardCornerRadius
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.glassExplicitContainerColor

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shape: Shape = LocalHomeCardCornerRadius.current?.let { RoundedCornerShape(it) } ?: MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: () -> Unit,
    role: DecoratedCardRole = DecoratedCardRole.Standard,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseContainerColor = containerColor?.let { glassExplicitContainerColor(it) }
        ?: MaterialTheme.colorScheme.surfaceContainerLow
    // Material has no backdrop blur; approximate "glass" with a translucent container so the app
    // background shows through.
    val resolvedContainerColor = if (LocalGlassCard.current) {
        baseContainerColor.copy(alpha = 0.55f)
    } else {
        baseContainerColor
    }
    val decorationColors = materialDecorationColors()
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = resolvedContainerColor),
        shape = shape,
    ) {
        DecoratedCardContent(
            role = role,
            colors = decorationColors,
            active = active,
        ) {
            content()
        }
    }
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shape: Shape = LocalHomeCardCornerRadius.current?.let { RoundedCornerShape(it) } ?: MaterialTheme.shapes.large,
    role: DecoratedCardRole = DecoratedCardRole.Standard,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseContainerColor = containerColor?.let { glassExplicitContainerColor(it) }
        ?: MaterialTheme.colorScheme.surfaceContainerLow
    // Material has no backdrop blur; approximate "glass" with a translucent container so the app
    // background shows through.
    val resolvedContainerColor = if (LocalGlassCard.current) {
        baseContainerColor.copy(alpha = 0.55f)
    } else {
        baseContainerColor
    }
    val decorationColors = materialDecorationColors()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = resolvedContainerColor),
        shape = shape,
    ) {
        DecoratedCardContent(
            role = role,
            colors = decorationColors,
            active = active,
        ) {
            content()
        }
    }
}
