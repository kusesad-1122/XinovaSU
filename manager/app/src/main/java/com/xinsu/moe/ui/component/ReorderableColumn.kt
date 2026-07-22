package com.xinsu.moe.ui.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A minimal long-press drag-to-reorder vertical list. Renderer-neutral (pure foundation/ui), so
 * both the Miuix and Material appearance screens can host it. The [row] slot renders each item and
 * must attach the provided [dragHandle] modifier to whatever element should start the reorder drag
 * on long-press. [onMove] is invoked once, on drop, with the final order.
 *
 * Rows are keyed by [keyOf] so Compose preserves each node (and its in-flight drag gesture) across
 * reorders. Fixed [rowHeight] keeps the swap math simple and predictable.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> Any,
    onMove: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 56.dp,
    row: @Composable (item: T, dragHandle: Modifier, dragging: Boolean) -> Unit,
) {
    val working = remember { mutableStateListOf<T>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offsetY by remember { mutableStateOf(0f) }
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }

    // Sync from the source list whenever it changes, but never while a drag is mid-flight (that
    // would clobber the working order the user is actively rearranging).
    LaunchedEffect(items) {
        if (draggingKey == null) {
            working.clear()
            working.addAll(items)
        }
    }

    Column(modifier) {
        working.forEach { item ->
            val itemKey = keyOf(item)
            key(itemKey) {
                val dragging = itemKey == draggingKey
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) offsetY else 0f },
                ) {
                    val handle = Modifier.pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingKey = itemKey
                                offsetY = 0f
                            },
                            onDragEnd = {
                                draggingKey = null
                                offsetY = 0f
                                onMove(working.toList())
                            },
                            onDragCancel = {
                                draggingKey = null
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                                val cur = working.indexOfFirst { keyOf(it) == itemKey }
                                if (cur >= 0) {
                                    if (offsetY > rowPx / 2f && cur < working.lastIndex) {
                                        working.add(cur + 1, working.removeAt(cur))
                                        offsetY -= rowPx
                                    } else if (offsetY < -rowPx / 2f && cur > 0) {
                                        working.add(cur - 1, working.removeAt(cur))
                                        offsetY += rowPx
                                    }
                                }
                            },
                        )
                    }
                    row(item, handle, dragging)
                }
            }
        }
    }
}
