package com.xinsu.moe.ui.theme.tokens

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class ThemeTokenBundle(
    val id: String,
    val displayNameKey: String,
    val artwork: ThemeArtworkTokens,
    val light: SemanticColorTokens,
    val dark: SemanticColorTokens,
    val atmosphere: AtmosphereTokens,
    val provenance: ThemeProvenance,
) {
    init {
        require(THEME_ID.matches(id)) { "Invalid theme id: $id" }
        require(displayNameKey.isNotBlank()) { "displayNameKey must not be blank" }
        if (provenance.contrastValidated) {
            require(light.contrastResults().all(ContrastResult::passes)) {
                "Light theme contains a documented contrast failure"
            }
            require(dark.contrastResults().all(ContrastResult::passes)) {
                "Dark theme contains a documented contrast failure"
            }
        }
    }

    companion object {
        private val THEME_ID = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
    }
}

@Serializable
@Immutable
data class SemanticColorTokens(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val inversePrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val surfaceBright: Long,
    val surfaceDim: Long,
    val outline: Long,
    val outlineVariant: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val scrim: Long,
    val focus: Long,
) {
    init {
        allColors().forEach { (name, value) ->
            require(value in 0L..0xFFFFFFFFL) { "$name must be an unsigned 32-bit ARGB value" }
            require(ThemeColorMath.alpha(value) == 255) {
                "$name must be opaque; glass alpha belongs to AtmosphereTokens"
            }
        }
        foregroundColors().forEach { (name, value) ->
            require(ThemeColorMath.alpha(value) == 255) { "$name must remain opaque" }
        }
    }

    fun contrastResults(): List<ContrastResult> = listOf(
        contrast("onPrimary/primary", onPrimary, primary),
        contrast("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
        contrast("onSecondary/secondary", onSecondary, secondary),
        contrast("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
        contrast("onTertiary/tertiary", onTertiary, tertiary),
        contrast("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
        contrast("onBackground/background", onBackground, background),
        contrast("onSurface/surface", onSurface, surface),
        contrast("onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant),
        contrast("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
        contrast("onError/error", onError, error),
        contrast("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
    )

    private fun contrast(name: String, foreground: Long, background: Long) = ContrastResult(
        name = name,
        foreground = foreground,
        background = background,
        ratio = ThemeColorMath.contrastRatio(foreground, background),
        minimumRatio = 4.5,
    )

    private fun foregroundColors(): Map<String, Long> = mapOf(
        "onPrimary" to onPrimary,
        "onPrimaryContainer" to onPrimaryContainer,
        "onSecondary" to onSecondary,
        "onSecondaryContainer" to onSecondaryContainer,
        "onTertiary" to onTertiary,
        "onTertiaryContainer" to onTertiaryContainer,
        "onBackground" to onBackground,
        "onSurface" to onSurface,
        "onSurfaceVariant" to onSurfaceVariant,
        "inverseOnSurface" to inverseOnSurface,
        "onError" to onError,
        "onErrorContainer" to onErrorContainer,
    )

    private fun allColors(): Map<String, Long> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "scrim" to scrim,
        "focus" to focus,
    )
}

@Serializable
@Immutable
data class ContrastResult(
    val name: String,
    val foreground: Long,
    val background: Long,
    val ratio: Double,
    val minimumRatio: Double,
) {
    val passes: Boolean get() = ratio >= minimumRatio
}

@Serializable
@Immutable
data class ThemeArtworkTokens(
    val resourceName: String,
    val width: Int,
    val height: Int,
    val focalPoint: NormalizedPoint,
    val subjectSafeRect: NormalizedRect,
    val scaleMode: ArtworkScaleMode,
    val scrimRails: List<ScrimRail> = emptyList(),
) {
    init {
        require(RESOURCE_NAME.matches(resourceName)) { "Invalid artwork resource name: $resourceName" }
        require(width > 0 && height > 0) { "Artwork dimensions must be positive" }
    }

    companion object {
        private val RESOURCE_NAME = Regex("^theme_[a-z0-9_]+\\.jpg$")
    }
}

@Serializable
@Immutable
data class NormalizedPoint(val x: Float, val y: Float) {
    init {
        require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) {
            "Normalized point must be inside 0..1"
        }
    }
}

@Serializable
@Immutable
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f }) {
            "Normalized rectangle coordinates must be inside 0..1"
        }
        require(left < right && top < bottom) { "Normalized rectangle must have positive area" }
    }
}

@Serializable
enum class ArtworkScaleMode { Crop, FitHeight, FitWidth }

@Serializable
enum class ScrimEdge { Top, Bottom, Start, End }

@Serializable
@Immutable
data class ScrimRail(
    val edge: ScrimEdge,
    val start: Float,
    val end: Float,
    val strength: Float,
) {
    init {
        require(start.isFinite() && end.isFinite() && start in 0f..1f && end in 0f..1f && start < end) {
            "Scrim rail range must be ordered inside 0..1"
        }
        require(strength.isFinite() && strength in 0f..1f) { "Scrim strength must be inside 0..1" }
    }
}

@Serializable
@Immutable
data class AtmosphereTokens(
    val cardOpacity: Float,
    val chromeOpacity: Float,
    val readableOpacity: Float,
    val outlineOpacity: Float,
    val glowColor: Long,
    val glowIntensity: Float,
    val localScrimStrength: Float,
    val cornerRadiusDp: Float,
    val blurRadiusDp: Float,
    val density: ThemeDensity,
    val preferredMode: ThemeModePreference,
) {
    init {
        mapOf(
            "cardOpacity" to cardOpacity,
            "chromeOpacity" to chromeOpacity,
            "readableOpacity" to readableOpacity,
            "outlineOpacity" to outlineOpacity,
            "glowIntensity" to glowIntensity,
            "localScrimStrength" to localScrimStrength,
        ).forEach { (name, value) ->
            require(value.isFinite() && value in 0f..1f) { "$name must be inside 0..1" }
        }
        require(cornerRadiusDp.isFinite() && cornerRadiusDp >= 0f) { "cornerRadiusDp must be non-negative" }
        require(blurRadiusDp.isFinite() && blurRadiusDp >= 0f) { "blurRadiusDp must be non-negative" }
        require(glowColor in 0L..0xFFFFFFFFL) { "glowColor must be an unsigned 32-bit ARGB value" }
    }
}

@Serializable
enum class ThemeDensity { Compact, Comfortable, Airy }

@Serializable
enum class ThemeModePreference { Light, Dark, System }

@Serializable
@Immutable
data class ThemeProvenance(
    val sourceFilename: String,
    val sha256: String,
    val rightsStatus: ThemeRightsStatus,
    val roleNotes: Map<String, String>,
    val pipelineVersion: String,
    val contrastValidated: Boolean,
) {
    init {
        require(sourceFilename.endsWith(".jpg", ignoreCase = true)) { "sourceFilename must be a JPG" }
        require(SHA256.matches(sha256)) { "sha256 must be 64 lowercase hexadecimal characters" }
        require(pipelineVersion.isNotBlank()) { "pipelineVersion must not be blank" }
        REQUIRED_ROLES.forEach { role ->
            require(!roleNotes[role].isNullOrBlank()) { "Missing provenance role note: $role" }
        }
    }

    companion object {
        private val SHA256 = Regex("^[0-9a-f]{64}$")
        private val REQUIRED_ROLES = listOf("primary", "secondary", "focus", "surface")
    }
}

@Serializable
enum class ThemeRightsStatus {
    UserProvidedPendingPublicationConfirmation,
    ClearedForPublicDistribution,
}
