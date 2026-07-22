package com.xinsu.moe.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xinsu.moe.R

/**
 * The reorderable, reshapeable cards on the home dashboard. Their order and per-card corner shape
 * are user-customizable (see the "home card layout" editor in the appearance settings) and persist
 * through prefs. Warning banners and the update card are contextual and stay pinned above these.
 */
enum class HomeCardId {
    Status, Info, Hardware, Donate, LearnMore, LearnKernelSU;

    companion object {
        val DEFAULT_ORDER: List<HomeCardId> = entries
        fun fromName(value: String?): HomeCardId? = entries.firstOrNull { it.name == value }
    }
}

/** Per-card corner style. [Default] keeps each renderer's normal card shape. */
enum class HomeCardShape {
    Default, Sharp, Small, Large, Pill;

    fun next(): HomeCardShape = entries[(ordinal + 1) % entries.size]

    /** Explicit corner radius, or null to keep the renderer's default card shape. */
    fun cornerRadiusOrNull(): Dp? = when (this) {
        Default -> null
        Sharp -> 0.dp
        Small -> 10.dp
        Large -> 24.dp
        Pill -> 1000.dp // clamped down to a capsule by the shape at draw time
    }

    companion object {
        fun fromName(value: String?): HomeCardShape =
            entries.firstOrNull { it.name == value } ?: Default
    }
}

// Order CSV: "Status,Info,...". Unknown names are dropped; any card missing from the stored value
// is appended in default order so newly-added cards always appear.
fun parseHomeCardOrder(csv: String?): List<HomeCardId> {
    if (csv.isNullOrBlank()) return HomeCardId.DEFAULT_ORDER
    val parsed = csv.split(",").mapNotNull { HomeCardId.fromName(it.trim()) }.distinct()
    val missing = HomeCardId.DEFAULT_ORDER.filter { it !in parsed }
    return parsed + missing
}

fun serializeHomeCardOrder(order: List<HomeCardId>): String = order.joinToString(",") { it.name }

// Shape CSV: "Status:Large,Info:Sharp". Only non-Default entries are stored.
fun parseHomeCardShapes(csv: String?): Map<HomeCardId, HomeCardShape> {
    if (csv.isNullOrBlank()) return emptyMap()
    return csv.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 2) return@mapNotNull null
        val id = HomeCardId.fromName(parts[0].trim()) ?: return@mapNotNull null
        id to HomeCardShape.fromName(parts[1].trim())
    }.toMap()
}

fun serializeHomeCardShapes(shapes: Map<HomeCardId, HomeCardShape>): String =
    shapes.entries
        .filter { it.value != HomeCardShape.Default }
        .joinToString(",") { "${it.key.name}:${it.value.name}" }

@Composable
fun homeCardLabel(id: HomeCardId): String = stringResource(
    when (id) {
        HomeCardId.Status -> R.string.home_card_status
        HomeCardId.Info -> R.string.home_card_info
        HomeCardId.Hardware -> R.string.home_card_hardware
        HomeCardId.Donate -> R.string.home_card_donate
        HomeCardId.LearnMore -> R.string.home_card_learn_more
        HomeCardId.LearnKernelSU -> R.string.home_card_learn_kernelsu
    }
)

@Composable
fun homeCardShapeLabel(shape: HomeCardShape): String = stringResource(
    when (shape) {
        HomeCardShape.Default -> R.string.card_shape_default
        HomeCardShape.Sharp -> R.string.card_shape_sharp
        HomeCardShape.Small -> R.string.card_shape_small
        HomeCardShape.Large -> R.string.card_shape_large
        HomeCardShape.Pill -> R.string.card_shape_pill
    }
)
