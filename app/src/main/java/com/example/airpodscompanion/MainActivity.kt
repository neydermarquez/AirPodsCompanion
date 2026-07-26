package com.example.airpodscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airpodscompanion.ui.theme.AirPodsCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirPodsCompanionTheme {
                AirPodsCompanionApp()
            }
        }
    }
}

@Composable
private fun AirPodsCompanionApp() {
    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Header()
            ConnectionCard()
            DeviceCard()
            QuickActions()
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AirPods Companion",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tu experiencia, más conectada",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        Text("⚙", fontSize = 27.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ConnectionCard() {
    AcrylicCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ᛒ", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Sin dispositivo conectado", fontWeight = FontWeight.SemiBold)
                    Text("Activa Bluetooth para comenzar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard() {
    AcrylicCard {
        Text("Tus AirPods", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Esperando conexión", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("AirPods Companion", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("▰", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActions() {
    Text("Acciones rápidas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction("Batería", "--%")
        QuickAction("Modo", "Automático")
    }
}

@Composable
private fun QuickAction(title: String, value: String) {
    AcrylicCard(modifier = Modifier.weight(1f)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AcrylicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 5.dp,
        shadowElevation = 8.dp,
        content = {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    )
}
