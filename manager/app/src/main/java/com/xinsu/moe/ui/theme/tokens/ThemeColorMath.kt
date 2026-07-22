package com.xinsu.moe.ui.theme.tokens

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ThemeColorMath {
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Long {
        require(alpha in 0..255 && red in 0..255 && green in 0..255 && blue in 0..255) {
            "ARGB channels must be in 0..255"
        }
        return (alpha.toLong() shl 24) or
            (red.toLong() shl 16) or
            (green.toLong() shl 8) or
            blue.toLong()
    }

    fun alpha(color: Long): Int = ((color ushr 24) and 0xFF).toInt()
    fun red(color: Long): Int = ((color ushr 16) and 0xFF).toInt()
    fun green(color: Long): Int = ((color ushr 8) and 0xFF).toInt()
    fun blue(color: Long): Int = (color and 0xFF).toInt()

    fun contrastRatio(foreground: Long, background: Long): Double {
        val opaqueForeground = compositeOver(foreground, background)
        val foregroundLuminance = relativeLuminance(opaqueForeground)
        val backgroundLuminance = relativeLuminance(background)
        val lighter = max(foregroundLuminance, backgroundLuminance)
        val darker = min(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun compositeOver(foreground: Long, background: Long): Long {
        val foregroundAlpha = alpha(foreground) / 255.0
        if (foregroundAlpha >= 1.0) return foreground
        require(alpha(background) == 255) { "Contrast backgrounds must be opaque" }

        fun compositeChannel(foregroundChannel: Int, backgroundChannel: Int): Int =
            (foregroundChannel * foregroundAlpha + backgroundChannel * (1.0 - foregroundAlpha))
                .toInt()
                .coerceIn(0, 255)

        return argb(
            alpha = 255,
            red = compositeChannel(red(foreground), red(background)),
            green = compositeChannel(green(foreground), green(background)),
            blue = compositeChannel(blue(foreground), blue(background)),
        )
    }

    private fun relativeLuminance(color: Long): Double {
        require(alpha(color) == 255) { "Relative luminance requires an opaque color" }

        fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * linear(red(color)) +
            0.7152 * linear(green(color)) +
            0.0722 * linear(blue(color))
    }
}
