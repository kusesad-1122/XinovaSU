package com.xinsu.moe.ui.theme.tokens

import kotlin.math.max

data class ResolvedArtworkPlacement(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val scaledWidth: Float,
    val scaledHeight: Float,
    val safeRect: PixelRect,
    val coversViewport: Boolean,
    val keepsSafeRectVisible: Boolean,
)

data class PixelRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun intersects(other: PixelRect): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

object ThemeArtworkResolver {
    fun resolve(
        artwork: ThemeArtworkTokens,
        viewportWidth: Float,
        viewportHeight: Float,
    ): ResolvedArtworkPlacement {
        require(viewportWidth.isFinite() && viewportWidth > 0f) { "viewportWidth must be positive" }
        require(viewportHeight.isFinite() && viewportHeight > 0f) { "viewportHeight must be positive" }

        val widthScale = viewportWidth / artwork.width
        val heightScale = viewportHeight / artwork.height
        val scale = when (artwork.scaleMode) {
            ArtworkScaleMode.Crop -> max(widthScale, heightScale)
            ArtworkScaleMode.FitHeight -> heightScale
            ArtworkScaleMode.FitWidth -> widthScale
        }
        val scaledWidth = artwork.width * scale
        val scaledHeight = artwork.height * scale
        val idealX = viewportWidth / 2f - artwork.focalPoint.x * scaledWidth
        val idealY = viewportHeight / 2f - artwork.focalPoint.y * scaledHeight

        val offsetX = resolveAxisOffset(
            ideal = idealX,
            viewport = viewportWidth,
            scaledExtent = scaledWidth,
            safeStart = artwork.subjectSafeRect.left * scaledWidth,
            safeEnd = artwork.subjectSafeRect.right * scaledWidth,
        )
        val offsetY = resolveAxisOffset(
            ideal = idealY,
            viewport = viewportHeight,
            scaledExtent = scaledHeight,
            safeStart = artwork.subjectSafeRect.top * scaledHeight,
            safeEnd = artwork.subjectSafeRect.bottom * scaledHeight,
        )
        val safeRect = PixelRect(
            left = artwork.subjectSafeRect.left * scaledWidth + offsetX,
            top = artwork.subjectSafeRect.top * scaledHeight + offsetY,
            right = artwork.subjectSafeRect.right * scaledWidth + offsetX,
            bottom = artwork.subjectSafeRect.bottom * scaledHeight + offsetY,
        )
        val epsilon = 0.01f
        return ResolvedArtworkPlacement(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            scaledWidth = scaledWidth,
            scaledHeight = scaledHeight,
            safeRect = safeRect,
            coversViewport = scaledWidth + epsilon >= viewportWidth && scaledHeight + epsilon >= viewportHeight,
            keepsSafeRectVisible = safeRect.left >= -epsilon && safeRect.top >= -epsilon &&
                safeRect.right <= viewportWidth + epsilon && safeRect.bottom <= viewportHeight + epsilon,
        )
    }

    fun scrimRect(
        rail: ScrimRail,
        viewportWidth: Float,
        viewportHeight: Float,
    ): PixelRect = when (rail.edge) {
        ScrimEdge.Top, ScrimEdge.Bottom -> PixelRect(
            left = 0f,
            top = rail.start * viewportHeight,
            right = viewportWidth,
            bottom = rail.end * viewportHeight,
        )
        ScrimEdge.Start, ScrimEdge.End -> PixelRect(
            left = rail.start * viewportWidth,
            top = 0f,
            right = rail.end * viewportWidth,
            bottom = viewportHeight,
        )
    }

    private fun resolveAxisOffset(
        ideal: Float,
        viewport: Float,
        scaledExtent: Float,
        safeStart: Float,
        safeEnd: Float,
    ): Float {
        val cropMin = minOf(0f, viewport - scaledExtent)
        val cropMax = maxOf(0f, viewport - scaledExtent)
        val safeMin = -safeStart
        val safeMax = viewport - safeEnd
        val allowedMin = maxOf(cropMin, safeMin)
        val allowedMax = minOf(cropMax, safeMax)
        return if (allowedMin <= allowedMax) {
            ideal.coerceIn(allowedMin, allowedMax)
        } else {
            ideal.coerceIn(cropMin, cropMax)
        }
    }
}
