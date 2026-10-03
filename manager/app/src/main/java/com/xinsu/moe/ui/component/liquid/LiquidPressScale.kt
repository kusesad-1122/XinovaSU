package com.xinsu.moe.ui.component.liquid

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 只在"按住"期间生效的轻微缩放 —— 让玻璃卡片有一点点可触碰的液体感，
 * 松手即回到 1f。常态下 [animateFloatAsState] 的目标值恒为 1f，不产生任何每帧工作，
 * 因此在长列表里滚动时不会带来额外开销（需求：列表滚动不能卡顿）。
 *
 * 实现要点：在 [PointerEventPass.Initial] 阶段读取指针状态，只观察、不消费事件，
 * 所以不会抢走 MiuixCard / 列表滚动的手势。
 *
 * @param enabled 关闭时不挂任何 modifier 与 pointerInput 节点，可给超长列表逐项关掉。
 * @param pressedScale 按住时的缩放比例，默认取自 [LiquidGlassSpec.PressedScale]。
 */
@Composable
fun Modifier.liquidPressScale(
    enabled: Boolean = true,
    pressedScale: Float = LiquidGlassSpec.PressedScale,
): Modifier {
    if (!enabled) return this

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = LiquidGlassSpec.PressSpringDamping,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "liquidPressScale",
    )

    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val nowPressed = event.changes.any { it.pressed }
                    if (nowPressed != pressed) {
                        pressed = nowPressed
                    }
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
