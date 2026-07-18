package com.xinsu.moe.ui.screen.colorpalette

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xinsu.moe.R
import com.xinsu.moe.ui.theme.ThemeGalleryMood
import com.xinsu.moe.ui.theme.ThemeTemplate
import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import com.xinsu.moe.ui.theme.tokens.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The 21 artwork sources live in drawable-nodpi at full resolution (up to ~1440x3140). Decoding
// them at native size for small gallery/preview targets can OOM on low-heap devices, so decode a
// downsampled bitmap off the main thread instead of painterResource().
@Composable
private fun rememberDownsampledArtwork(resId: Int, maxPx: Int = 1080): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, resId, maxPx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(context.resources, resId, bounds)
                var sample = 1
                while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

/** Shared artwork renderer so Material and Miuix present the same focal crop and palette. */
@Composable
internal fun ThemeArtworkImage(
    template: ThemeTemplate,
    modifier: Modifier = Modifier,
    bottomScrim: Boolean = false,
) {
    val bundle = remember(template.tokenBundleId) { BuiltInThemeCatalog.byId(template.tokenBundleId) }
    val focal = bundle?.artwork?.focalPoint
    val alignment = remember(focal) {
        BiasAlignment(
            horizontalBias = ((focal?.x ?: 0.5f) * 2f - 1f).coerceIn(-1f, 1f),
            verticalBias = ((focal?.y ?: 0.5f) * 2f - 1f).coerceIn(-1f, 1f),
        )
    }
    val artworkRes = template.artworkRes
    val bitmap = if (artworkRes != null) rememberDownsampledArtwork(artworkRes) else null
    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(template.nameRes),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = alignment,
            )
        } else {
            // Placeholder while decoding, or when a template has no artwork.
            Box(Modifier.fillMaxSize().background(Color(0x22000000)))
        }
        if (bottomScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f)),
                            startY = 120f,
                        )
                    )
            )
        }
    }
}

internal fun ThemeTemplate.swatches(isDark: Boolean): List<Color> {
    val bundle = BuiltInThemeCatalog.byId(tokenBundleId) ?: return emptyList()
    val colors = if (isDark) bundle.dark else bundle.light
    return listOf(
        colors.primary.toComposeColor(),
        colors.secondary.toComposeColor(),
        colors.tertiary.toComposeColor(),
        colors.surfaceVariant.toComposeColor(),
    )
}

internal val themeGalleryFilters: List<ThemeGalleryMood?> = listOf(
    null,
    ThemeGalleryMood.Minimal,
    ThemeGalleryMood.Playful,
    ThemeGalleryMood.Daylight,
    ThemeGalleryMood.Cinematic,
    ThemeGalleryMood.Nocturne,
)

@Composable
internal fun themeGalleryFilterLabel(mood: ThemeGalleryMood?): String = stringResource(
    when (mood) {
        null -> R.string.theme_filter_all
        ThemeGalleryMood.Minimal -> R.string.theme_filter_minimal
        ThemeGalleryMood.Playful -> R.string.theme_filter_playful
        ThemeGalleryMood.Daylight -> R.string.theme_filter_daylight
        ThemeGalleryMood.Cinematic -> R.string.theme_filter_cinematic
        ThemeGalleryMood.Nocturne -> R.string.theme_filter_nocturne
        ThemeGalleryMood.Legacy -> R.string.settings_theme_preset
    }
)
