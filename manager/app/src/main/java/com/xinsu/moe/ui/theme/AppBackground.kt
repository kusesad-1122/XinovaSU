package com.xinsu.moe.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.net.toUri
import com.xinsu.moe.ui.theme.tokens.AtmosphereTokens
import com.xinsu.moe.ui.theme.tokens.ScrimEdge
import com.xinsu.moe.ui.theme.tokens.ScrimRail
import com.xinsu.moe.ui.theme.tokens.ThemeArtworkResolver
import com.xinsu.moe.ui.theme.tokens.ThemeArtworkTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Optional decorative app background. Kept intentionally minimal ("简约"):
//  - Gradient: a subtle wash derived from the *current* color scheme (so it tracks whatever
//    pastel preset / dynamic theme is active), with a center scrim that pulls the middle back
//    toward the surface color so cards and text over it stay readable.
//  - Image: a user-picked picture (e.g. anime art) drawn center-cropped behind the content,
//    with a stronger scrim for readability over arbitrary images.
//  - Stage: a cinematic "scene" — the illustration is full-bleed (center-cropped) behind the
//    content with soft letterbox scrims at the top and bottom so the middle band (usually the
//    character's face) stays clear while the app bar and lower cards keep their contrast.
// Pure core Compose — no runtime shader, no image-loading dependency — works on every
// supported API level.
enum class BackgroundStyle {
    None,
    Gradient,
    Image,
    Stage;

    companion object {
        fun fromName(value: String?): BackgroundStyle =
            entries.firstOrNull { it.name == value } ?: None
    }
}

val BackgroundStyle.isActive: Boolean get() = this != BackgroundStyle.None

// Every style except the plain gradient paints a user-supplied illustration, so the image
// picker row must be offered for all of them.
val BackgroundStyle.usesImage: Boolean
    get() = this == BackgroundStyle.Image ||
        this == BackgroundStyle.Stage

// Maps a stored 0/1/2 position preference to a horizontal alignment (start/center/end) while
// keeping the vertical centered — used to nudge Crop-scaled background and card images sideways.
fun horizontalAlignmentFor(value: Int): Alignment = when (value) {
    0 -> Alignment.CenterStart
    2 -> Alignment.CenterEnd
    else -> Alignment.Center
}

// Card container opacity (the 0-100 pref) as a 0f..1f fraction. Multiply a card's container color
// alpha by this so the app background shows through for a translucent "glass" look.
@Composable
fun cardOpacityFraction(): Float = (LocalCardOpacity.current / 100f).coerceIn(0f, 1f)

// A page hosts its own opaque Scaffold surface by default. When the shared app-wide background
// layer is active we make each main page's Scaffold transparent so that single layer shows
// through every page; otherwise the page keeps its normal [opaque] surface.
@Composable
@ReadOnlyComposable
fun scaffoldContainerColor(opaque: Color): Color =
    if (LocalBackgroundStyle.current.isActive) Color.Transparent else opaque

// Decodes a content-uri image to a downsampled ImageBitmap off the main thread. Returns null
// while loading, when the uri is blank, or on any failure (missing permission, bad file, ...).
@Composable
fun rememberBackgroundImageBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = uriString.toUri()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    // Downsample so a large photo does not blow up memory.
                    val maxDim = 1600
                    var sample = 1
                    while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                        sample *= 2
                    }
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, opts)
                    }?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

// Draws the decorative background (behind the caller's content) for the active style. Placed as
// a match-parent-size layer at the back of the home content.
@Composable
fun AppBackgroundLayer(
    modifier: Modifier,
    style: BackgroundStyle,
    imageBitmap: ImageBitmap?,
    accentA: Color,
    accentB: Color,
    accentC: Color,
    base: Color,
    scrim: Color,
    isDark: Boolean,
    imageAlpha: Float = 1f,
    imageAlignment: Alignment = Alignment.Center,
    artworkTokens: ThemeArtworkTokens? = null,
    atmosphereTokens: AtmosphereTokens? = null,
) {
    val effectiveImageAlignment = artworkTokens?.let { artwork ->
        remember(artwork) { ArtworkFocalAlignment(artwork) }
    } ?: imageAlignment

    when (style) {
        BackgroundStyle.Gradient ->
            Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))

        BackgroundStyle.Image ->
            if (imageBitmap != null) {
                Box(modifier) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = effectiveImageAlignment,
                        alpha = imageAlpha,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(scrim.copy(alpha = if (isDark) 0.5f else 0.4f)),
                    )
                }
            } else {
                // Not loaded yet (or failed): fall back to the palette gradient so the screen
                // never flashes an empty/opaque block.
                Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))
            }

        BackgroundStyle.Stage ->
            if (imageBitmap != null) {
                // Full-bleed illustration behind everything, framed by soft letterbox scrims at
                // top and bottom so the clear middle band frames the character while the app bar
                // and lower cards keep readable contrast.
                Box(modifier) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = effectiveImageAlignment,
                        alpha = imageAlpha,
                        modifier = Modifier.matchParentSize(),
                    )
                    if (artworkTokens != null && atmosphereTokens != null) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .themeScrimRails(
                                    rails = artworkTokens.scrimRails,
                                    scrim = scrim,
                                    isDark = isDark,
                                    defaultStrength = atmosphereTokens.localScrimStrength,
                                ),
                        )
                    } else {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to base.copy(alpha = if (isDark) 0.80f else 0.64f),
                                        0.30f to scrim.copy(alpha = if (isDark) 0.30f else 0.20f),
                                        0.66f to scrim.copy(alpha = if (isDark) 0.30f else 0.20f),
                                        1f to base.copy(alpha = if (isDark) 0.88f else 0.74f),
                                    ),
                                ),
                        )
                    }
                }
            } else {
                Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))
            }

        BackgroundStyle.None -> Unit
    }
}

private class ArtworkFocalAlignment(
    private val artwork: ThemeArtworkTokens,
) : Alignment {
    override fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset {
        val placement = ThemeArtworkResolver.resolve(
            artwork = artwork,
            viewportWidth = space.width.toFloat(),
            viewportHeight = space.height.toFloat(),
        )
        return IntOffset(
            x = placement.offsetX.roundToInt(),
            y = placement.offsetY.roundToInt(),
        )
    }
}

private fun Modifier.themeScrimRails(
    rails: List<ScrimRail>,
    scrim: Color,
    isDark: Boolean,
    defaultStrength: Float,
): Modifier = drawBehind {
    rails.forEach { rail ->
        val alpha = ((rail.strength + defaultStrength) / 2f * if (isDark) 1f else 0.9f)
            .coerceIn(0f, 0.72f)
        val opaque = scrim.copy(alpha = alpha)
        when (rail.edge) {
            ScrimEdge.Top -> {
                val startY = rail.start * size.height
                val endY = rail.end * size.height
                drawRect(
                    brush = Brush.verticalGradient(listOf(opaque, Color.Transparent), startY, endY),
                    topLeft = Offset(0f, startY),
                    size = Size(size.width, endY - startY),
                )
            }
            ScrimEdge.Bottom -> {
                val startY = rail.start * size.height
                val endY = rail.end * size.height
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, opaque), startY, endY),
                    topLeft = Offset(0f, startY),
                    size = Size(size.width, endY - startY),
                )
            }
            ScrimEdge.Start -> {
                val startX = rail.start * size.width
                val endX = rail.end * size.width
                drawRect(
                    brush = Brush.horizontalGradient(listOf(opaque, Color.Transparent), startX, endX),
                    topLeft = Offset(startX, 0f),
                    size = Size(endX - startX, size.height),
                )
            }
            ScrimEdge.End -> {
                val startX = rail.start * size.width
                val endX = rail.end * size.width
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, opaque), startX, endX),
                    topLeft = Offset(startX, 0f),
                    size = Size(endX - startX, size.height),
                )
            }
        }
    }
}

// base -> soft pastel accent wash -> center readability scrim, drawn behind the node's content.
private fun Modifier.paletteGradient(
    accentA: Color,
    accentB: Color,
    accentC: Color,
    base: Color,
    scrim: Color,
    isDark: Boolean,
): Modifier = drawBehind {
    val strength = if (isDark) 0.22f else 0.14f
    drawRect(base)
    drawRect(
        Brush.linearGradient(
            colors = listOf(
                accentA.copy(alpha = strength),
                accentC.copy(alpha = strength * 0.9f),
                accentB.copy(alpha = strength),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                scrim.copy(alpha = if (isDark) 0.45f else 0.32f),
                Color.Transparent,
            ),
            center = Offset(size.width / 2f, size.height * 0.42f),
            radius = size.maxDimension * 0.9f,
        )
    )
}
