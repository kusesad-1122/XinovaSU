package com.xinsu.moe.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Optional decorative app background. Kept intentionally minimal ("简约"):
//  - Gradient: a subtle wash derived from the *current* color scheme (so it tracks whatever
//    pastel preset / dynamic theme is active), with a center scrim that pulls the middle back
//    toward the surface color so cards and text over it stay readable.
//  - Image: a user-picked picture (e.g. anime art) drawn center-cropped behind the content,
//    with a stronger scrim for readability over arbitrary images.
//  - Portrait: a visual-novel look — a mostly-empty surface canvas with a single character
//    illustration fit to one side (bottom-right), its inner edge feathered into the canvas so
//    there is no hard rectangle. The text column stays over calm surface for readability.
//  - PortraitLeft: the Portrait composition mirrored to the bottom-left, for art framed the
//    other way.
//  - Stage: a cinematic "scene" — the illustration is full-bleed (center-cropped) behind the
//    content with soft letterbox scrims at the top and bottom so the middle band (usually the
//    character's face) stays clear while the app bar and lower cards keep their contrast.
//  - Bloom: a dreamy, painterly wash — the full-bleed illustration is softened by a pastel
//    veil and a large accent bloom pulled from the active palette, for a kawaii, hazy look.
// Pure core Compose — no runtime shader, no image-loading dependency — works on every
// supported API level.
enum class BackgroundStyle {
    None,
    Gradient,
    Image,
    Portrait,
    PortraitLeft,
    Stage,
    Bloom;

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
        this == BackgroundStyle.Portrait ||
        this == BackgroundStyle.PortraitLeft ||
        this == BackgroundStyle.Stage ||
        this == BackgroundStyle.Bloom

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
) {
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
                        alignment = imageAlignment,
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

        BackgroundStyle.Portrait ->
            if (imageBitmap != null) {
                PortraitLayer(modifier, imageBitmap, base, isDark, imageAlpha, alignStart = false)
            } else {
                Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))
            }

        BackgroundStyle.PortraitLeft ->
            if (imageBitmap != null) {
                PortraitLayer(modifier, imageBitmap, base, isDark, imageAlpha, alignStart = true)
            } else {
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
                        alignment = imageAlignment,
                        alpha = imageAlpha,
                        modifier = Modifier.matchParentSize(),
                    )
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
            } else {
                Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))
            }

        BackgroundStyle.Bloom ->
            if (imageBitmap != null) {
                // Full-bleed illustration softened by a pastel veil plus a large accent bloom
                // pulled from the active palette, for a dreamy, hazy, kawaii feel.
                Box(modifier) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = imageAlignment,
                        alpha = imageAlpha,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(base.copy(alpha = if (isDark) 0.44f else 0.34f))
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentA.copy(alpha = if (isDark) 0.32f else 0.24f),
                                            accentC.copy(alpha = if (isDark) 0.20f else 0.15f),
                                            Color.Transparent,
                                        ),
                                        center = Offset(size.width * 0.5f, size.height * 0.36f),
                                        radius = size.maxDimension * 0.82f,
                                    ),
                                )
                            },
                    )
                }
            } else {
                Box(modifier.paletteGradient(accentA, accentB, accentC, base, scrim, isDark))
            }

        BackgroundStyle.None -> Unit
    }
}

// Shared visual-novel character composition: a solid canvas that tracks the theme surface, the
// character illustration fit to one bottom corner at partial width, and a horizontal feather
// that dissolves the sprite's inner edge into the canvas so the opposite text column stays calm.
@Composable
private fun PortraitLayer(
    modifier: Modifier,
    imageBitmap: ImageBitmap,
    base: Color,
    isDark: Boolean,
    imageAlpha: Float,
    alignStart: Boolean,
) {
    val alignment = if (alignStart) Alignment.BottomStart else Alignment.BottomEnd
    // Feather runs toward the text column: solid base fades to clear over the sprite's side.
    val featherColors = if (alignStart) {
        listOf(base.copy(alpha = 0f), base)
    } else {
        listOf(base, base.copy(alpha = 0f))
    }
    Box(modifier.drawBehind { drawRect(base) }) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = alignment,
            alpha = (if (isDark) 0.88f else 0.96f) * imageAlpha,
            modifier = Modifier
                .align(alignment)
                .fillMaxHeight()
                .fillMaxWidth(0.62f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(colors = featherColors)),
        )
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
