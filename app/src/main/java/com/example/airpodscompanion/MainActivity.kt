package com.example.airpodscompanion

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import com.example.airpodscompanion.ui.theme.AirPodsCompanionTheme
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent { AirPodsCompanionApp() }
    }
}

@Composable
private fun AirPodsCompanionApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val lifecycleOwner = activity
    val controller = remember { BluetoothController(context.applicationContext) }
    val mediaControls = remember { MediaControls(context.applicationContext) }
    val bluetoothState by controller.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        controller.refresh()
        if (controller.hasPermissions()) controller.scan()
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        controller.refresh()
        if (controller.hasPermissions()) controller.scan()
    }
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    controller.start()
                    if (controller.hasPermissions()) controller.scan()
                }
                Lifecycle.Event.ON_STOP -> controller.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }
    }
    AirPodsCompanionTheme {
        var showBrandIntro by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(1_200)
            showBrandIntro = false
        }

        if (showBrandIntro) {
            BrandIntro()
        } else {
            AppShell(
                bluetoothState = bluetoothState,
                onPermission = { permissionLauncher.launch(controller.requiredPermissions()) },
                onEnableBluetooth = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                onScan = controller::scan,
                mediaControls = mediaControls
            )
        }
    }
}

@Composable
private fun BrandIntro() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.airpods_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(R.drawable.airpods_logo),
            contentDescription = "AirPods Companion",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.62f)
                .offset(y = (-44).dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AppShell(
    bluetoothState: BluetoothUiState,
    onPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScan: () -> Unit,
    mediaControls: MediaControls
) {
    var tab by remember { mutableIntStateOf(0) }
    val background = MaterialTheme.colorScheme.background
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(background)) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TopBar()
                when (tab) {
                    0 -> HomeScreen(bluetoothState, onPermission, onEnableBluetooth, onScan)
                    1 -> DevicesScreen(bluetoothState, onPermission, onEnableBluetooth, onScan, mediaControls)
                    2 -> ActivityScreen(bluetoothState)
                    else -> SettingsScreen()
                }
            }
            NavigationBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TopBar() {
    val logoColor = MaterialTheme.colorScheme.onBackground
    val logoFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, logoColor.red * 255f,
                0f, 0f, 0f, 0f, logoColor.green * 255f,
                0f, 0f, 0f, 0f, logoColor.blue * 255f,
                -1f, -1f, -1f, 0f, 690f
            )
        )
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Image(
                painter = painterResource(R.drawable.airpods_logo),
                contentDescription = "AirPods Companion",
                modifier = Modifier.width(124.dp).height(42.dp),
                contentScale = ContentScale.Fit,
                colorFilter = logoFilter
            )
            Text("AirPods Companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .58f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text("Edición única", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: BluetoothUiState, onPermission: () -> Unit, onEnableBluetooth: () -> Unit, onScan: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ConnectionCard(state, onPermission, onEnableBluetooth, onScan)
        SectionHeader("Qué podrás consultar", "Al conectar")
        HomeCapabilities()
        PrivacyNote()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun HomeCapabilities() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            CapabilityRow(0, "Batería", "Auriculares y estuche compatibles")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CapabilityRow(1, "Compatibilidad", "Funciones disponibles para tu modelo")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CapabilityRow(2, "Diagnóstico", "Conexión, permisos y estado")
        }
    }
}

@Composable
private fun CapabilityRow(index: Int, title: String, detail: String) {
    val icon = when (index) {
        0 -> Icons.Outlined.Headphones
        1 -> Icons.Outlined.Shield
        else -> Icons.Outlined.Timeline
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionCard(state: BluetoothUiState, onPermission: () -> Unit, onEnableBluetooth: () -> Unit, onScan: () -> Unit) {
    val connected = state.connectedDevice
    val statusTitle = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> "Permiso necesario"
        BluetoothStatus.DISABLED -> "Bluetooth apagado"
        BluetoothStatus.UNSUPPORTED -> "Bluetooth no disponible"
        BluetoothStatus.CONNECTED -> connected?.name ?: "AirPods conectados"
        BluetoothStatus.ERROR -> "No se pudo buscar"
        else -> "Buscando AirPods"
    }
    val statusDetail = when (state.status) {
        BluetoothStatus.CONNECTED -> "Conectado mediante Bluetooth"
        BluetoothStatus.PERMISSION_REQUIRED -> "Autoriza dispositivos cercanos"
        BluetoothStatus.DISABLED -> "Actívalo para continuar"
        BluetoothStatus.READY -> "Listo para buscar"
        BluetoothStatus.ERROR -> state.error ?: "Inténtalo nuevamente"
        else -> "Detección automática activa"
    }
    val action = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> onPermission
        BluetoothStatus.DISABLED -> onEnableBluetooth
        BluetoothStatus.READY, BluetoothStatus.ERROR -> onScan
        else -> null
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .52f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(statusTitle, fontWeight = FontWeight.SemiBold)
                Text(statusDetail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.status == BluetoothStatus.SEARCHING) CircularProgressIndicator(
                modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(if (connected != null) "Dispositivo reconocido" else "Tus AirPods aparecerán aquí", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Si ya están conectados por Bluetooth, la app los reconocerá automáticamente. Si no, abre el estuche y mantenlos cerca.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable { action() } else Modifier),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (action != null) statusDetail else "Conexión inteligente", fontWeight = FontWeight.SemiBold)
                    Text(if (connected != null) "Perfil de audio activo" else "Vinculados y dispositivos cercanos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "La detección se realiza en este dispositivo y requiere Bluetooth.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun PrivacyNote() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        androidx.compose.material3.Icon(
            Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Sin root ni modificaciones del sistema. Las limitaciones se mostrarán con claridad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DevicesScreen(state: BluetoothUiState, onPermission: () -> Unit, onEnableBluetooth: () -> Unit, onScan: () -> Unit, mediaControls: MediaControls) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Dispositivos", "Detección automática")
        DeviceDiscoveryPanel(state, onPermission, onEnableBluetooth, onScan)
        if (state.connectedDevice != null) {
            SectionHeader("Multimedia", "Control del sistema")
            MediaControlPanel(mediaControls)
        }
        if (state.connectedDevice == null) {
            SectionHeader("Conexión sencilla", "Sin pasos innecesarios")
            ConnectionGuide()
        }
        AcrylicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Controles solo cuando correspondan", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Batería, audio y gestos aparecerán únicamente al reconocer un dispositivo compatible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun MediaControlPanel(controls: MediaControls) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaControl(Icons.AutoMirrored.Outlined.VolumeDown, "Bajar volumen", controls::volumeDown)
            MediaControl(Icons.Outlined.SkipPrevious, "Anterior", controls::previous)
            MediaControl(Icons.Outlined.PlayArrow, "Reproducir o pausar", controls::playPause, true)
            MediaControl(Icons.Outlined.SkipNext, "Siguiente", controls::next)
            MediaControl(Icons.AutoMirrored.Outlined.VolumeUp, "Subir volumen", controls::volumeUp)
        }
    }
}

@Composable
private fun MediaControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    action: () -> Unit,
    prominent: Boolean = false
) {
    Surface(
        modifier = Modifier.size(if (prominent) 48.dp else 42.dp),
        shape = RoundedCornerShape(if (prominent) 16.dp else 14.dp),
        color = if (prominent) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (prominent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    ) {
        Box(Modifier.fillMaxSize().clickable(onClick = action), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, contentDescription = description, modifier = Modifier.size(if (prominent) 25.dp else 22.dp))
        }
    }
}

@Composable
private fun DeviceDiscoveryPanel(state: BluetoothUiState, onPermission: () -> Unit, onEnableBluetooth: () -> Unit, onScan: () -> Unit) {
    val action = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> onPermission
        BluetoothStatus.DISABLED -> onEnableBluetooth
        BluetoothStatus.READY, BluetoothStatus.ERROR -> onScan
        else -> null
    }
    val title = state.connectedDevice?.name ?: when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> "Autoriza la detección"
        BluetoothStatus.DISABLED -> "Bluetooth apagado"
        BluetoothStatus.UNSUPPORTED -> "Bluetooth no disponible"
        BluetoothStatus.ERROR -> "Error de búsqueda"
        else -> "Buscando dispositivos"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .52f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(27.dp)
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (state.connectedDevice != null) "Conectado" else "Bluetooth y cercanía", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.status == BluetoothStatus.SEARCHING) CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                Modifier.fillMaxWidth().clickable { action() },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    when (state.status) {
                        BluetoothStatus.PERMISSION_REQUIRED -> "Permitir dispositivos cercanos"
                        BluetoothStatus.DISABLED -> "Activar Bluetooth"
                        else -> "Buscar nuevamente"
                    },
                    Modifier.padding(vertical = 13.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val connected = state.connectedDevice
        Spacer(Modifier.height(20.dp))
        if (connected != null) {
            Text("Información disponible", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            DeviceFact(
                "Modelo",
                connected.identifiedModel,
                when (connected.identification) {
                    IdentificationConfidence.EXACT_VARIANT -> "Variante publicada en el nombre Bluetooth"
                    IdentificationConfidence.GENERATION -> "Generación publicada; variante no distinguible"
                    IdentificationConfidence.FAMILY -> "Familia identificada; generación no publicada"
                    IdentificationConfidence.UNKNOWN -> "Android no publicó información suficiente"
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            DeviceFact(
                "Batería",
                connected.batteryPercent?.let { "$it%" } ?: "No informada",
                if (connected.batteryPercent != null) "Publicada por el perfil Bluetooth"
                else "El teléfono o los AirPods no publicaron este dato"
            )
        } else {
            Text("Conecta sin pasos extra", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "La app reconocerá primero los AirPods que ya estén conectados a tu teléfono. También buscará modelos cercanos listos para enlazar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            DiscoveryLine(Icons.Outlined.Bluetooth, "Vinculados por Bluetooth", "Reconocimiento automático")
            HorizontalDivider(Modifier.padding(start = 33.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            DiscoveryLine(Icons.Outlined.Timeline, "AirPods cercanos", "Detección al abrir el estuche")
        }
    }
}

@Composable
private fun DeviceFact(label: String, value: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
        Text(
            detail,
            modifier = Modifier.weight(1.25f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DiscoveryLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionGuide() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            GuideRow("1", "Activa Bluetooth", "La detección comenzará automáticamente")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            GuideRow("2", "Abre el estuche", "Mantén los AirPods cerca del teléfono")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            GuideRow("3", "Continúa aquí", "El dispositivo aparecerá cuando esté disponible")
        }
    }
}

@Composable
private fun GuideRow(step: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            step,
            modifier = Modifier.width(28.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityScreen(state: BluetoothUiState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Actividad", "Historial real")
        ActivityEmptyState(state)
        SectionHeader("Qué se registrará", "Al conectar")
        ActivityEventGroup()
        AcrylicCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Historial privado", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "La actividad permanecerá en este dispositivo y no incluirá eventos simulados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Ajustes", "Preferencias")
        SectionHeader("Aplicación", "Control y privacidad")
        PreferencesGroup()
        SectionHeader("Compatibilidad", "Limitaciones de Android")
        CompatibilityGroup()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ActivityEmptyState(state: BluetoothUiState) {
    val connected = state.connectedDevice
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .52f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        androidx.compose.material3.Icon(
            Icons.Outlined.Timeline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(if (connected != null) "Conexión activa" else "Aún no hay actividad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (connected != null) "${connected.name} está conectado. Los próximos cambios de estado aparecerán aquí."
            else "Cuando se reconozca un dispositivo compatible, aquí aparecerán únicamente cambios y diagnósticos obtenidos de forma real.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(connected?.name ?: "Esperando un dispositivo", fontWeight = FontWeight.SemiBold)
                    Text(if (connected != null) "Bluetooth conectado" else "Sin datos registrados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActivityEventGroup() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SettingLikeRow(Icons.Outlined.Bluetooth, "Conexión", "Conexiones, desconexiones y reconexión")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(Icons.Outlined.Headphones, "Dispositivo", "Modelo, disponibilidad y compatibilidad")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(Icons.Outlined.Timeline, "Diagnóstico", "Batería, señal y cambios relevantes")
        }
    }
}

@Composable
private fun PreferencesGroup() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SettingLikeRow(Icons.Outlined.Shield, "Privacidad", "Procesamiento local, sin cuenta de Apple", "Incluido")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(Icons.Outlined.Timeline, "Notificaciones", "Conexión, desconexión y batería baja", "Próximamente")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(Icons.Outlined.Settings, "Accesibilidad", "Contraste y tamaño del sistema", "Sistema")
        }
    }
}

@Composable
private fun CompatibilityGroup() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            CompatibilityLine("Audio espacial", "Pendiente")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine("Cancelación de ruido", "Limitada")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine("Buscar", "No disponible")
        }
    }
}

@Composable
private fun SettingLikeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    status: String? = null
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status != null) {
            Spacer(Modifier.width(8.dp))
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompatibilityLine(title: String, status: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Box(
        modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, label ->
                    val active = index == selected
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent
                    ) {
                        Column(
                            Modifier.clickable { onSelect(index) }.padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NavIcon(index, active)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
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
