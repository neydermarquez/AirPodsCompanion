package com.example.airpodscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.core.view.WindowCompat
import com.example.airpodscompanion.ui.theme.AirPodsCompanionTheme

private enum class ThemeChoice(val preferenceValue: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        setContent { AirPodsCompanionApp() }
    }
}

@Composable
private fun AirPodsCompanionApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("airpods_preferences", 0) }
    var themeChoice by remember {
        mutableStateOf(
            when (preferences.getString("theme", ThemeChoice.SYSTEM.preferenceValue)) {
                ThemeChoice.LIGHT.preferenceValue -> ThemeChoice.LIGHT
                ThemeChoice.DARK.preferenceValue -> ThemeChoice.DARK
                else -> ThemeChoice.SYSTEM
            }
        )
    }
    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    AirPodsCompanionTheme(darkTheme = darkTheme) {
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(900)
            showSplash = false
        }
        if (showSplash) {
            BrandSplash(themeChoice)
        } else {
            AppShell(themeChoice) {
                themeChoice = it
                preferences.edit().putString("theme", it.preferenceValue).apply()
            }
        }
    }
}

@Composable
private fun BrandSplash(themeChoice: ThemeChoice) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(com.example.airpodscompanion.R.drawable.airpods_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (themeChoice == ThemeChoice.DARK) .18f else .10f
        )
        Image(
            painter = painterResource(com.example.airpodscompanion.R.drawable.airpods_logo_clean),
            contentDescription = "AirPods Companion",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(.78f).padding(24.dp)
        )
    }
}

@Composable
private fun AppShell(themeChoice: ThemeChoice, onThemeChange: (ThemeChoice) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val background = MaterialTheme.colorScheme.background
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(background)) {
            Image(
                painter = painterResource(com.example.airpodscompanion.R.drawable.airpods_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (themeChoice == ThemeChoice.DARK) .16f else .10f
            )
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = .88f)))
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TopBar(tab)
                when (tab) {
                    0 -> HomeScreen()
                    1 -> DevicesScreen()
                    2 -> ActivityScreen()
                    else -> SettingsScreen(themeChoice, onThemeChange)
                }
            }
            NavigationBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TopBar(tab: Int) {
    val titles = listOf("Inicio", "Dispositivos", "Actividad", "Ajustes")
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(com.example.airpodscompanion.R.drawable.airpods_logo_mark),
                    contentDescription = "AirPods Companion",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titles[tab], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("AirPods Companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("v1.0", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ConnectionCard()
        SectionHeader("Qué podrás consultar", "Al conectar")
        InfoRow("Batería", "Auriculares y estuche, cuando el modelo lo permita")
        InfoRow("Compatibilidad", "Estado real por función y modelo probado")
        InfoRow("Diagnóstico", "Conexión, permisos y errores relevantes")
        PrivacyNote()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ConnectionCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .46f))
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), RoundedCornerShape(24.dp)).padding(22.dp)
    ) {
        StatusBadge("SIN DISPOSITIVO CONECTADO")
        Spacer(Modifier.height(22.dp))
        Text("Conecta tus AirPods", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("La aplicación mostrará solo la información que pueda leer de forma real en Android.", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary) {
            Text("Flujo de conexión preparado", Modifier.padding(vertical = 15.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("Necesitarás Bluetooth activo y permisos cercanos.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusBadge(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .7f)) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(title: String, detail: String) {
    AcrylicCard {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyNote() {
    Text("Sin root ni modificaciones del sistema. Las funciones limitadas se indicarán claramente.", Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DevicesScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ConnectedDevicePreview()
        StateBanner("Bluetooth apagado", "Activa Bluetooth para buscar AirPods cercanos.", "Próximamente")
        SectionHeader("Estados alternos", "Vista visual")
        StateRow("Buscando dispositivos", "Escaneando AirPods cercanos", true)
        StateRow("Conexión perdida", "Comprueba que tus AirPods estén cerca", false)
        StateRow("Permiso pendiente", "Se necesita acceso a dispositivos cercanos", false)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ConnectedDevicePreview() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("AirPods Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Vista de diseño · conectado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BatteryChip("Izquierdo", "86%", Modifier.weight(1f))
            BatteryChip("Derecho", "84%", Modifier.weight(1f))
            BatteryChip("Estuche", "72%", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text("Modo de escucha", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("Cancelación", true, Modifier.weight(1f))
            ModeChip("Transparencia", false, Modifier.weight(1f))
            ModeChip("Adaptativo", false, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        AcrylicCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Audio espacial", fontWeight = FontWeight.SemiBold)
                    Text("Compatible con limitaciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Preparado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Controles preparados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        InfoRow("Multimedia", "Reproducir, pausar, anterior, siguiente y volumen")
        InfoRow("Llamadas y micrófono", "Silenciar, activar y cambiar fuente de audio")
        InfoRow("Gestos y sensores", "Detección de oído, presión, doble toque y Digital Crown")
        InfoRow("Perfiles", "Música, llamadas, juegos y oficina")
        Text("Funciones dependientes de Apple", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        CompatibilityRow("Siri e iCloud", "No disponible en Android")
        CompatibilityRow("Buscar y firmware", "No disponible en Android")
        CompatibilityRow("Salud auditiva", "Pendiente de investigación")
    }
}

@Composable
private fun BatteryChip(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .58f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0f else .18f))
    ) {
        Text(label, Modifier.padding(horizontal = 5.dp, vertical = 10.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActivityScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Actividad", "Cuando exista conexión")
        AcrylicCard { EmptyLine("Sin actividad reciente", "Los eventos reales aparecerán después de conectar un dispositivo.") }
        SectionHeader("Diagnóstico", "Estado preparado")
        InfoRow("Conexión", "Latencia, señal y reconexión automática")
        InfoRow("Micrófono", "Disponibilidad para llamadas y grabación")
        InfoRow("Batería", "Lectura por auricular y estuche")
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun SettingsScreen(themeChoice: ThemeChoice, onThemeChange: (ThemeChoice) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Preferencias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ThemeSelector(themeChoice, onThemeChange)
        InfoRow("Privacidad", "Sin cuentas de Apple ni datos innecesarios")
        InfoRow("Notificaciones", "Conexión, desconexión y batería baja")
        InfoRow("Accesibilidad", "Contraste, tamaño de texto y controles claros")
        SectionHeader("Compatibilidad", "Por modelo")
        CompatibilityRow("Audio espacial", "Pendiente de investigación")
        CompatibilityRow("Cancelación de ruido", "Compatible con limitaciones")
        CompatibilityRow("Buscar", "No disponible en Android")
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ThemeSelector(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AcrylicCard {
        Column {
            Text("Tema", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                when (selected) {
                    ThemeChoice.SYSTEM -> "Se adapta automáticamente al tema del sistema"
                    ThemeChoice.LIGHT -> "Tema claro seleccionado manualmente"
                    ThemeChoice.DARK -> "Tema oscuro seleccionado manualmente"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Box {
                Surface(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable { expanded = true },
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(themeLabel(selected), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Box(Modifier.size(8.dp).border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .96f))
                ) {
                    ThemeChoice.values().forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(themeLabel(choice), fontWeight = if (choice == selected) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onSelect(choice); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

private fun themeLabel(choice: ThemeChoice): String = when (choice) {
    ThemeChoice.SYSTEM -> "Automático"
    ThemeChoice.LIGHT -> "Claro"
    ThemeChoice.DARK -> "Oscuro"
}

@Composable
private fun StateBanner(title: String, detail: String, action: String) {
    AcrylicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StateRow(title: String, detail: String, active: Boolean) {
    AcrylicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(if (active) 12.dp else 9.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompatibilityRow(title: String, status: String) {
    AcrylicCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyLine(title: String, detail: String) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaceholderScreen(title: String, detail: String) {
    Column(Modifier.fillMaxSize().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(detail, Modifier.padding(horizontal = 26.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AcrylicCard(content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .58f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun NavigationBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val items = listOf("Inicio", "Dispositivos", "Actividad", "Ajustes")
    Surface(
        modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .62f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .13f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEachIndexed { index, label ->
                val active = index == selected
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).clickable { onSelect(index) }.padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavIcon(index, active)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun NavIcon(index: Int, active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when (index) {
        0 -> Icons.Outlined.Home
        1 -> Icons.Outlined.Headphones
        2 -> Icons.Outlined.Timeline
        else -> Icons.Outlined.Settings
    }
    androidx.compose.material3.Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
}
