package com.xinsu.moe.ui.component.material

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.xinsu.moe.ui.theme.LocalCardOpacity

// Applies the user's card-opacity preference so every tonal card can go translucent and let the
// app-wide background show through. At 100% (the default) the color is unchanged.
private fun Color.withCardOpacity(fraction: Float): Color =
    if (fraction >= 1f) this else copy(alpha = alpha * fraction)

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val op = (LocalCardOpacity.current / 100f).coerceIn(0f, 1f)
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = containerColor.withCardOpacity(op)),
        shape = shape
    ) {
        content()
    }
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    val op = (LocalCardOpacity.current / 100f).coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor.withCardOpacity(op)),
        shape = shape
    ) {
        content()
    }
}
