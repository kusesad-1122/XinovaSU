package com.xinsu.moe.ui.component.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xinsu.moe.ui.component.decoration.EnergySwitchVisual
import com.xinsu.moe.ui.component.decoration.miuixDecorationColors
import top.yukonga.miuix.kmp.basic.SwitchColors
import top.yukonga.miuix.kmp.basic.SwitchDefaults

@Composable
fun EnergyMiuixSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.switchColors(),
    enabled: Boolean = true,
) {
    EnergySwitchVisual(
        checked = checked,
        enabled = enabled,
        colors = miuixDecorationColors(),
        modifier = Modifier,
    ) {
        top.yukonga.miuix.kmp.basic.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = colors,
            enabled = enabled,
        )
    }
}
