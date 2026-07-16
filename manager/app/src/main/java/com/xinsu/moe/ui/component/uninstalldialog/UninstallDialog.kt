package com.xinsu.moe.ui.component.uninstalldialog

import androidx.compose.runtime.Composable
import com.xinsu.moe.ui.LocalUiMode
import com.xinsu.moe.ui.UiMode

@Composable
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> UninstallDialogMiuix(show, onDismissRequest)
        UiMode.Material -> UninstallDialogMaterial(show, onDismissRequest)
    }
}
