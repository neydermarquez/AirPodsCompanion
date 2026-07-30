package com.soren.airpodscompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.soren.airpodscompanion.DiagnosticConsent
import com.soren.airpodscompanion.DiagnosticConsentStore

@Composable
fun PrivacySection(onExportAll: () -> Unit, onDeleteAll: () -> Unit) {
    val context = LocalContext.current
    val consentStore = remember { DiagnosticConsentStore(context.applicationContext) }
    var diagnosticConsent by remember { mutableStateOf(consentStore.load()) }
    var confirmExport by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            PrivacyRow(
                Icons.Outlined.Shield,
                "Procesamiento local",
                "Sin cuenta de Apple. Los diagnósticos no se transmiten automáticamente.",
                "Privado"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            ConsentRow(
                title = "Informes de fallos",
                detail = "Guarda hasta 5 informes locales para que puedas exportarlos",
                checked = diagnosticConsent.crashReports
            ) {
                diagnosticConsent = diagnosticConsent.copy(crashReports = it)
                consentStore.save(diagnosticConsent)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            ConsentRow(
                title = "Métricas de uso",
                detail = "Cuenta aperturas y pantallas localmente, sin identificadores",
                checked = diagnosticConsent.usageMetrics
            ) {
                diagnosticConsent = diagnosticConsent.copy(usageMetrics = it)
                consentStore.save(diagnosticConsent)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            PrivacyRow(
                Icons.Outlined.Timeline,
                "Exportar mis datos",
                "Preferencias, actividad y evidencia técnica en un archivo JSON",
                "Autorizar"
            ) { confirmExport = true }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            PrivacyRow(
                Icons.Outlined.Settings,
                "Borrar todos los datos",
                "Elimina preferencias, historial, batería guardada y sesiones locales",
                "Eliminar"
            ) { confirmDelete = true }
        }
    }
    if (confirmExport) {
        AlertDialog(
            onDismissRequest = { confirmExport = false },
            title = { Text("Exportar todos tus datos") },
            text = {
                Text(
                    "El archivo incluirá preferencias, nombres registrados, actividad y evidencia técnica. " +
                        "Tú eliges dónde guardarlo y la app no lo envía a ningún servidor."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmExport = false
                    onExportAll()
                }) { Text("Autorizar") }
            },
            dismissButton = { TextButton(onClick = { confirmExport = false }) { Text("Cancelar") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Borrar datos locales") },
            text = {
                Text(
                    "Esta acción elimina permanentemente configuraciones, historial, lecturas guardadas y sesiones técnicas. " +
                        "No afecta el emparejamiento Bluetooth de Android."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAll()
                }) { Text("Borrar definitivamente", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ConsentRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable { onChecked(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PrivacyRow(
    icon: ImageVector,
    title: String,
    detail: String,
    status: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.sizeIn(minHeight = 48.dp)
                        .semantics { role = Role.Button }
                        .clickable(onClick = onClick)
                } else Modifier
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(21.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
