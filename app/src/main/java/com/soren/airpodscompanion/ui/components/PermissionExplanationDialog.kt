package com.soren.airpodscompanion.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionExplanationDialog(
    title: String,
    detail: String,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } }
    )
}
