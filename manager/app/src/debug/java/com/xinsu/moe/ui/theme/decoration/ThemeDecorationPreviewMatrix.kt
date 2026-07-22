package com.xinsu.moe.ui.theme.decoration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.UiMode
import com.xinsu.moe.ui.component.decoration.LocalEnergyPreviewElapsedMillis
import com.xinsu.moe.ui.component.material.ExpressiveSwitch
import com.xinsu.moe.ui.component.material.TonalCard
import com.xinsu.moe.ui.component.miuix.EnergyMiuixSwitch
import com.xinsu.moe.ui.component.miuix.MiuixGlassCard
import com.xinsu.moe.ui.theme.AppSettings
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.XinovaSUTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText

private class ThemeDecorationPreviewProvider : PreviewParameterProvider<ThemeDecorationSpec> {
    override val values: Sequence<ThemeDecorationSpec> =
        ThemeDecorationCatalog.all.values.asSequence()
}

private val PreviewCardRoles = listOf(
    DecoratedCardRole.Hero,
    DecoratedCardRole.Monitor,
    DecoratedCardRole.Function,
    DecoratedCardRole.Standard,
    DecoratedCardRole.Compact,
)

private fun ThemeDecorationSpec.previewSettings() = AppSettings(
    colorMode = ColorMode.LIGHT,
    keyColor = 0,
    paletteStyle = PaletteStyle.TonalSpot,
    colorSpec = ColorSpec.SpecVersion.SPEC_2025,
    themePresetId = themeId,
)

private fun DecoratedCardRole.previewContent(): String = when (this) {
    DecoratedCardRole.Hero ->
        "Hero · Long title and description that wraps to stress the content-safe layout."
    DecoratedCardRole.Monitor -> "CPU 37% · Memory 62% · Battery 84%"
    DecoratedCardRole.Function -> "Function · Run full consistency check"
    DecoratedCardRole.Standard -> "Standard · Supporting description"
    DecoratedCardRole.Compact -> "Compact"
}

@Preview(
    name = "Material theme decorations",
    widthDp = 420,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun MaterialThemeDecorationPreview(
    @PreviewParameter(ThemeDecorationPreviewProvider::class, limit = 32)
    spec: ThemeDecorationSpec,
) {
    CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
        XinovaSUTheme(appSettings = spec.previewSettings(), uiMode = UiMode.Material) {
            MaterialPreviewMatrix(spec)
        }
    }
}

@Preview(
    name = "Miuix theme decorations",
    widthDp = 420,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun MiuixThemeDecorationPreview(
    @PreviewParameter(ThemeDecorationPreviewProvider::class, limit = 32)
    spec: ThemeDecorationSpec,
) {
    CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
        XinovaSUTheme(appSettings = spec.previewSettings(), uiMode = UiMode.Miuix) {
            MiuixPreviewMatrix(spec)
        }
    }
}

@Composable
private fun MaterialPreviewMatrix(spec: ThemeDecorationSpec) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MaterialText(text = "${spec.themeId} · ${spec.motif.name}")
        PreviewCardRoles.forEach { role ->
            TonalCard(
                modifier = Modifier.fillMaxWidth(),
                role = role,
                active = role == DecoratedCardRole.Hero,
            ) {
                MaterialText(
                    text = role.previewContent(),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MaterialText(text = "Off")
                ExpressiveSwitch(
                    checked = false,
                    onCheckedChange = null,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MaterialText(text = "Mid")
                CompositionLocalProvider(
                    LocalEnergyPreviewElapsedMillis provides EnergyTimeline.MidPreviewMillis,
                ) {
                    ExpressiveSwitch(
                        checked = true,
                        onCheckedChange = null,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MaterialText(text = "On")
                ExpressiveSwitch(
                    checked = true,
                    onCheckedChange = null,
                )
            }
        }
    }
}

@Composable
private fun MiuixPreviewMatrix(spec: ThemeDecorationSpec) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiuixText(text = "${spec.themeId} · ${spec.motif.name}")
        PreviewCardRoles.forEach { role ->
            MiuixGlassCard(
                modifier = Modifier.fillMaxWidth(),
                role = role,
                active = role == DecoratedCardRole.Hero,
            ) {
                MiuixText(text = role.previewContent())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiuixText(text = "Off")
                EnergyMiuixSwitch(
                    checked = false,
                    onCheckedChange = null,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiuixText(text = "Mid")
                CompositionLocalProvider(
                    LocalEnergyPreviewElapsedMillis provides EnergyTimeline.MidPreviewMillis,
                ) {
                    EnergyMiuixSwitch(
                        checked = true,
                        onCheckedChange = null,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiuixText(text = "On")
                EnergyMiuixSwitch(
                    checked = true,
                    onCheckedChange = null,
                )
            }
        }
    }
}
