package com.soren.airpodscompanion

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soren.airpodscompanion.ui.theme.AirPodsCompanionTheme

class AirPodsWidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (!WidgetPreferences.validWidgetId(widgetId)) {
            finish()
            return
        }

        setContent {
            AirPodsCompanionTheme {
                var selected by remember { mutableStateOf(WidgetPreferences(this).load(widgetId)) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Configurar widget", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Elige cuánto detalle mostrar. Nunca se presentarán datos que Android no haya leído.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        WidgetPresentation.entries.forEach { option ->
                            Surface(
                                Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                    .clickable { selected = option },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected == option, { selected = option })
                                    Column(Modifier.padding(start = 10.dp)) {
                                        Text(
                                            when (option) {
                                                WidgetPresentation.AUTOMATIC -> "Automático"
                                                WidgetPresentation.COMPACT -> "Compacto"
                                                WidgetPresentation.DETAILED -> "Ampliado"
                                            }
                                        )
                                        Text(
                                            when (option) {
                                                WidgetPresentation.AUTOMATIC -> "Se adapta al espacio disponible"
                                                WidgetPresentation.COMPACT -> "Nombre y estado principal"
                                                WidgetPresentation.DETAILED -> "Incluye lecturas individuales disponibles"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                WidgetPreferences(this@AirPodsWidgetConfigActivity).save(widgetId, selected)
                                AirPodsWidget.update(this@AirPodsWidgetConfigActivity, widgetId)
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                )
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Añadir widget") }
                    }
                }
            }
        }
    }
}
