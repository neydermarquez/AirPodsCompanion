package com.soren.airpodscompanion

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.Manifest
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import com.soren.airpodscompanion.ui.theme.AirPodsCompanionTheme
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import com.soren.airpodscompanion.navigation.AppDestination
import com.soren.airpodscompanion.ui.components.PermissionExplanationDialog
import com.soren.airpodscompanion.ui.screens.PrivacySection

@Composable
fun AirPodsCompanionApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val lifecycleOwner = activity
    val repository = remember { AirPodsRepository.get(context.applicationContext) }
    val controller = repository.controller
    val mediaControls = remember { MediaControls(context.applicationContext) }
    val diagnostics = remember { AudioDiagnostics(context.applicationContext) }
    val profileStore = remember { ListeningProfileStore(context.applicationContext) }
    val retentionStore = remember { HistoryRetentionPreferences(context.applicationContext) }
    val backgroundPreferences = remember { BackgroundPreferences(context.applicationContext) }
    val monitorStatusStore = remember { MonitorStatusStore.get(context.applicationContext) }
    val notificationPreferences = remember { NotificationPreferences(context.applicationContext) }
    val companionCoordinator = remember { CompanionCoordinator(context.applicationContext) }
    val privacyRepository = remember { PrivacyRepository(context.applicationContext) }
    val appViewModel: AppViewModel = viewModel()
    val monitorStatus by monitorStatusStore.status.collectAsState()
    val manufacturerGuidance = remember { ManufacturerBackgroundGuide.current() }
    val selectedProfile by appViewModel.selectedProfile.collectAsState()
    val profileSettings by appViewModel.profileSettings.collectAsState()
    val retentionSettings by appViewModel.retentionSettings.collectAsState()
    var pendingProtocolExport by remember { mutableStateOf<String?>(null) }
    var showBluetoothPermissionExplanation by remember { mutableStateOf(false) }
    var showNotificationPermissionExplanation by remember { mutableStateOf(false) }
    var startAfterReboot by remember { mutableStateOf(backgroundPreferences.startAfterReboot()) }
    var monitoringEnabled by remember { mutableStateOf(backgroundPreferences.monitoringEnabled()) }
    var batteryOptimizationDisabled by remember { mutableStateOf(false) }
    var backgroundRestricted by remember { mutableStateOf(false) }
    val notificationSettings by appViewModel.notificationSettings.collectAsState()
    var companionAssociated by remember { mutableStateOf(companionCoordinator.associated()) }
    var companionMessage by remember { mutableStateOf<String?>(null) }
    val bluetoothState by controller.state.collectAsState()
    val centralizedError by AppErrorCenter.message.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingProtocolExport
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            }.onFailure { AppErrorCenter.report("No fue posible guardar el archivo seleccionado.") }
        }
        pendingProtocolExport = null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        controller.refresh()
        if (controller.hasPermissions()) {
            if (monitoringEnabled) {
                ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
            }
            controller.scan()
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (controller.hasPermissions() && monitoringEnabled) {
            ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
        }
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        controller.refresh()
        if (controller.hasPermissions()) controller.scan()
    }
    val companionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        companionAssociated = companionCoordinator.associated()
        companionMessage = if (companionAssociated && companionCoordinator.startPresenceObservation()) {
            "Presencia asociada activa"
        } else if (companionAssociated) {
            "Asociado; la presencia requiere Android 12 o superior"
        } else "No se completó la asociación"
    }
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    batteryOptimizationDisabled = context.getSystemService(PowerManager::class.java)
                        ?.isIgnoringBatteryOptimizations(context.packageName) == true
                    backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
                    } else false
                    repository.acquire(RepositoryOwner.ACTIVITY)
                    controller.resumeConnection()
                    if (controller.hasPermissions() && monitoringEnabled) {
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            showNotificationPermissionExplanation = true
                        } else {
                            ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> repository.release(RepositoryOwner.ACTIVITY)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            repository.release(RepositoryOwner.ACTIVITY)
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
                onPermission = { showBluetoothPermissionExplanation = true },
                onEnableBluetooth = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                onScan = controller::scan,
                onPair = controller::pair,
                onReconnect = controller::reconnect,
                onClearHistory = controller::clearHistory,
                mediaControls = mediaControls,
                diagnostics = diagnostics,
                selectedProfile = selectedProfile,
                profileSettings = profileSettings,
                onProfile = {
                    appViewModel.selectProfile(it)
                    val settings = profileStore.settings(it)
                    if (bluetoothState.connectedDevice != null) {
                        mediaControls.setMusicVolumePercent(settings.volumePercent)
                    }
                    val notifications = notificationSettings.copy(
                        connection = settings.connectionNotifications,
                        disconnection = settings.disconnectionNotifications,
                        lowBattery = settings.lowBatteryNotifications
                    )
                    appViewModel.updateNotifications(notifications)
                },
                onProfileSettings = {
                    appViewModel.updateProfile(it)
                    if (bluetoothState.connectedDevice != null) {
                        mediaControls.setMusicVolumePercent(it.volumePercent)
                    }
                    val notifications = notificationSettings.copy(
                        connection = it.connectionNotifications,
                        disconnection = it.disconnectionNotifications,
                        lowBattery = it.lowBatteryNotifications
                    )
                    appViewModel.updateNotifications(notifications)
                },
                onStartCapture = controller::startProtocolCapture,
                onStopCapture = controller::stopProtocolCapture,
                onDeleteProtocolSession = controller::deleteProtocolSession,
                onCompareProtocolSession = controller::compareProtocolSession,
                onExportProtocolSession = { session ->
                    pendingProtocolExport = controller.exportProtocolSession(session.id)
                    exportLauncher.launch(
                        "airpods-${session.scenario.name.lowercase()}-${session.startedAt}.json"
                    )
                },
                onExportAllData = {
                    pendingProtocolExport = privacyRepository.exportAll()
                    exportLauncher.launch("airpods-companion-datos.json")
                },
                onDeleteAllData = {
                    context.stopService(Intent(context, AirPodsMonitorService::class.java))
                    privacyRepository.deleteAll()
                    controller.reloadHistory()
                    appViewModel.reloadAfterDataDeletion()
                },
                retentionSettings = retentionSettings,
                onRetentionSettings = {
                    appViewModel.updateRetention(it)
                    controller.reloadHistory()
                },
                startAfterReboot = startAfterReboot,
                onStartAfterReboot = {
                    backgroundPreferences.setStartAfterReboot(it)
                    startAfterReboot = it
                },
                monitoringEnabled = monitoringEnabled,
                onMonitoringEnabled = { enabled ->
                    backgroundPreferences.setMonitoringEnabled(enabled)
                    monitoringEnabled = enabled
                    if (enabled && controller.hasPermissions()) {
                        ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
                    } else if (!enabled) {
                        context.stopService(Intent(context, AirPodsMonitorService::class.java))
                    }
                },
                batteryOptimizationDisabled = batteryOptimizationDisabled,
                onBatteryOptimization = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
                monitorStatus = monitorStatus,
                manufacturerGuidance = manufacturerGuidance,
                backgroundRestricted = backgroundRestricted,
                notificationSettings = notificationSettings,
                onNotificationSettings = {
                    appViewModel.updateNotifications(it)
                },
                companionSupported = companionCoordinator.supported(),
                companionAssociated = companionAssociated,
                companionMessage = companionMessage,
                onCompanionAssociation = {
                    companionCoordinator.requestAssociation(
                        onChooser = {
                            companionLauncher.launch(IntentSenderRequest.Builder(it).build())
                        },
                        onFailure = { companionMessage = it }
                    )
                }
            )
        }
        if (showBluetoothPermissionExplanation) {
            PermissionExplanationDialog(
                title = "Acceso a dispositivos cercanos",
                detail = "Se usa para reconocer AirPods conectados, buscar dispositivos y leer los estados Bluetooth que Android publique. No se utiliza para obtener tu ubicación.",
                onDismiss = { showBluetoothPermissionExplanation = false },
                onContinue = {
                    showBluetoothPermissionExplanation = false
                    permissionLauncher.launch(controller.requiredPermissions())
                }
            )
        }
        if (showNotificationPermissionExplanation) {
            PermissionExplanationDialog(
                title = "Notificaciones de conexión",
                detail = "Se usan para avisarte de conexión, desconexión, batería baja y para mantener la supervisión autorizada en segundo plano.",
                onDismiss = { showNotificationPermissionExplanation = false },
                onContinue = {
                    showNotificationPermissionExplanation = false
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
        centralizedError?.let { message ->
            AlertDialog(
                onDismissRequest = AppErrorCenter::clear,
                title = { Text("No se pudo completar la acción") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = AppErrorCenter::clear) { Text("Entendido") } }
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
    onPair: (String) -> Unit,
    onReconnect: (String) -> Unit,
    onClearHistory: () -> Unit,
    mediaControls: MediaControls,
    diagnostics: AudioDiagnostics,
    selectedProfile: ListeningProfile,
    profileSettings: ListeningProfileSettings,
    onProfile: (ListeningProfile) -> Unit,
    onProfileSettings: (ListeningProfileSettings) -> Unit,
    onStartCapture: (CaptureScenario) -> Unit,
    onStopCapture: () -> Unit,
    onDeleteProtocolSession: (String) -> Unit,
    onCompareProtocolSession: (String) -> List<ScenarioComparison>,
    onExportProtocolSession: (ProtocolSession) -> Unit,
    onExportAllData: () -> Unit,
    onDeleteAllData: () -> Unit,
    retentionSettings: HistoryRetentionSettings,
    onRetentionSettings: (HistoryRetentionSettings) -> Unit,
    startAfterReboot: Boolean,
    onStartAfterReboot: (Boolean) -> Unit,
    monitoringEnabled: Boolean,
    onMonitoringEnabled: (Boolean) -> Unit,
    batteryOptimizationDisabled: Boolean,
    onBatteryOptimization: () -> Unit,
    monitorStatus: MonitorStatus,
    manufacturerGuidance: ManufacturerGuidance,
    backgroundRestricted: Boolean,
    notificationSettings: NotificationSettings,
    onNotificationSettings: (NotificationSettings) -> Unit,
    companionSupported: Boolean,
    companionAssociated: Boolean,
    companionMessage: String?,
    onCompanionAssociation: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = AppDestination.indexOf(backStackEntry?.destination?.route)
    LaunchedEffect(backStackEntry?.destination?.route) {
        backStackEntry?.destination?.route?.let {
            DiagnosticConsentStore(context).recordMetric("screen_$it")
        }
    }
    var connectionPopup by remember { mutableStateOf<ConnectionPopupModel?>(null) }
    var previousAddress by remember { mutableStateOf<String?>(null) }
    var connectionStateInitialized by remember { mutableStateOf(false) }
    val background = MaterialTheme.colorScheme.background
    LaunchedEffect(bluetoothState.status, bluetoothState.connectedDevice?.address) {
        val connected = bluetoothState.connectedDevice
        if (!connectionStateInitialized) {
            previousAddress = connected?.address
            connectionStateInitialized = true
            if (connected != null && notificationSettings.connection) {
                connectionPopup = ConnectionPopupModel.connected(connected)
            }
            return@LaunchedEffect
        }
        connectionPopup = when {
            bluetoothState.status == BluetoothStatus.RECONNECTING && notificationSettings.connection ->
                ConnectionPopupModel.reconnecting(
                    bluetoothState.devices.firstOrNull { it.bonded }?.name ?: "AirPods"
                )
            connected != null && connected.address != previousAddress && notificationSettings.connection ->
                ConnectionPopupModel.connected(connected)
            connected == null && previousAddress != null && notificationSettings.disconnection ->
                ConnectionPopupModel.lost()
            else -> connectionPopup
        }
        previousAddress = connected?.address ?: previousAddress.takeIf {
            bluetoothState.status == BluetoothStatus.RECONNECTING
        }
    }
    LaunchedEffect(connectionPopup) {
        if (connectionPopup != null) {
            delay(5_500)
            connectionPopup = null
        }
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(background)) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TopBar()
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.HOME.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppDestination.HOME.route) {
                        HomeScreen(bluetoothState, onPermission, onEnableBluetooth, onScan)
                    }
                    composable(AppDestination.DEVICES.route) {
                        DevicesScreen(
                            bluetoothState,
                            onPermission,
                            onEnableBluetooth,
                            onScan,
                            onPair,
                            onReconnect,
                            mediaControls,
                            diagnostics
                        )
                    }
                    composable(AppDestination.ACTIVITY.route) {
                        ActivityScreen(
                        bluetoothState,
                        onClearHistory,
                        onDeleteProtocolSession,
                        onCompareProtocolSession,
                        onExportProtocolSession,
                        retentionSettings,
                        onRetentionSettings
                        )
                    }
                    composable(AppDestination.SETTINGS.route) {
                        SettingsScreen(
                        diagnostics,
                        selectedProfile,
                        profileSettings,
                        onProfile,
                        onProfileSettings,
                        bluetoothState.protocolCapture,
                        onStartCapture,
                        onStopCapture,
                        startAfterReboot,
                        onStartAfterReboot,
                        monitoringEnabled,
                        onMonitoringEnabled,
                        batteryOptimizationDisabled,
                        onBatteryOptimization,
                        monitorStatus,
                        manufacturerGuidance,
                        backgroundRestricted,
                        notificationSettings,
                        onNotificationSettings,
                        companionSupported,
                        companionAssociated,
                        companionMessage,
                        onCompanionAssociation,
                        onExportAllData,
                        onDeleteAllData
                        )
                    }
                }
            }
            NavigationBar(
                selectedTab,
                { index ->
                    val destination = AppDestination.fromIndex(index)
                    navController.navigate(destination.route, navOptions {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    })
                },
                Modifier.align(Alignment.BottomCenter)
            )
            connectionPopup?.let { popup ->
                ConnectionPopup(
                    model = popup,
                    onDismiss = { connectionPopup = null },
                    onReconnect = {
                        DeviceSnapshotStore(context).load().address?.let(onReconnect)
                        connectionPopup = ConnectionPopupModel.reconnecting(
                            DeviceSnapshotStore(context).load().name ?: "AirPods"
                        )
                    }
                )
            }
        }
    }
}

private enum class ConnectionPopupKind { CONNECTED, RECONNECTING, LOST }

private data class ConnectionPopupModel(
    val kind: ConnectionPopupKind,
    val title: String,
    val detail: String,
    val battery: String? = null
) {
    companion object {
        fun connected(device: AirPodsDevice): ConnectionPopupModel {
            val battery = device.battery.combined
            return ConnectionPopupModel(
                kind = ConnectionPopupKind.CONNECTED,
                title = device.name,
                detail = when {
                    battery.isFresh() -> "Conectados y listos"
                    battery.percent != null -> "Conectados · batería de una lectura anterior"
                    else -> "Conectados · batería no publicada"
                },
                battery = battery.percent?.let {
                    if (battery.isFresh()) "$it%" else "Última lectura $it%"
                }
            )
        }

        fun reconnecting(name: String) = ConnectionPopupModel(
            ConnectionPopupKind.RECONNECTING,
            name,
            "Intentando restablecer los perfiles de audio"
        )

        fun lost() = ConnectionPopupModel(
            ConnectionPopupKind.LOST,
            "Conexión perdida",
            "Los AirPods dejaron de estar disponibles"
        )
    }
}

@Composable
private fun ConnectionPopup(
    model: ConnectionPopupModel,
    onDismiss: () -> Unit,
    onReconnect: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).semantics {
                liveRegion = if (model.kind == ConnectionPopupKind.LOST) {
                    LiveRegionMode.Assertive
                } else LiveRegionMode.Polite
                stateDescription = "${model.title}. ${model.detail}"
            },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
            shadowElevation = 18.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = .18f)
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        if (model.kind == ConnectionPopupKind.RECONNECTING) Icons.Outlined.Refresh
                        else Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(model.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            model.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { role = Role.Button }
                            .clickable(onClick = onDismiss)
                            .padding(12.dp)
                    )
                }
                model.battery?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (model.kind == ConnectionPopupKind.LOST) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onReconnect),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Reconectar",
                            Modifier.padding(vertical = 13.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeDeviceHero(state, onPermission, onEnableBluetooth, onScan)
        HomeEssentials(state)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun HomeDeviceHero(
    state: BluetoothUiState,
    onPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScan: () -> Unit
) {
    val device = state.connectedDevice
    val connected = device != null
    val statusTitle = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> "Permiso necesario"
        BluetoothStatus.DISABLED -> "Bluetooth apagado"
        BluetoothStatus.UNSUPPORTED -> "Bluetooth no disponible"
        BluetoothStatus.PAIRING -> "Emparejando"
        BluetoothStatus.RECONNECTING -> "Reconectando"
        BluetoothStatus.CONNECTED -> "Conectados"
        BluetoothStatus.ERROR -> "No se pudo buscar"
        BluetoothStatus.READY -> "Listos para conectar"
        BluetoothStatus.SEARCHING -> "Buscando"
    }
    val statusDetail = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> "Autoriza dispositivos cercanos"
        BluetoothStatus.DISABLED -> "Activa Bluetooth para continuar"
        BluetoothStatus.UNSUPPORTED -> "Este teléfono no ofrece Bluetooth compatible"
        BluetoothStatus.PAIRING -> "Confirma la solicitud de Android"
        BluetoothStatus.RECONNECTING -> "Restaurando la conexión de audio"
        BluetoothStatus.CONNECTED -> device?.identifiedModel ?: "Audio Bluetooth activo"
        BluetoothStatus.ERROR -> state.error ?: "Vuelve a intentarlo"
        BluetoothStatus.READY -> "Abre el estuche y mantenlo cerca"
        BluetoothStatus.SEARCHING -> "Detección automática en curso"
    }
    val actionLabel = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> "Autorizar"
        BluetoothStatus.DISABLED -> "Activar Bluetooth"
        BluetoothStatus.READY, BluetoothStatus.ERROR -> "Buscar ahora"
        else -> null
    }
    val action = when (state.status) {
        BluetoothStatus.PERMISSION_REQUIRED -> onPermission
        BluetoothStatus.DISABLED -> onEnableBluetooth
        BluetoothStatus.READY, BluetoothStatus.ERROR -> onScan
        else -> null
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .62f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .18f)
        )
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        device?.name ?: "Tus AirPods",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        statusDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (connected) {
                        Color(0xFF1FAF83).copy(alpha = .12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)
                    }
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(7.dp).clip(RoundedCornerShape(50))
                                .background(
                                    if (connected) Color(0xFF1FAF83)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)
                                )
                        )
                        Text(
                            statusTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (connected) Color(0xFF147A60)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Image(
                painter = painterResource(R.drawable.airpods_product_render),
                contentDescription = "Dos auriculares inalámbricos y su estuche de carga",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                DeviceVisual(
                    label = "Auriculares",
                    battery = earbudsBatteryText(device),
                    fresh = earbudsBatteryFresh(device),
                    modifier = Modifier.weight(1f),
                    connected = connected
                )
                DeviceVisual(
                    label = "Estuche",
                    battery = batteryText(device?.battery?.case),
                    fresh = device?.battery?.case?.isFresh() == true,
                    modifier = Modifier.weight(1f),
                    connected = connected
                )
            }

            if (state.status in listOf(
                    BluetoothStatus.SEARCHING,
                    BluetoothStatus.PAIRING,
                    BluetoothStatus.RECONNECTING
                )
            ) {
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else if (action != null && actionLabel != null) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = actionLabel
                        }
                        .clickable(onClick = action),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        actionLabel,
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceVisual(
    label: String,
    battery: String,
    fresh: Boolean,
    modifier: Modifier,
    connected: Boolean
) {
    Column(
        modifier.semantics {
            contentDescription = "$label. $battery" +
                if (fresh) "" else ". Sin lectura actual"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            if (connected) battery else "—",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (fresh) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (connected && !fresh) {
            Text(
                "Sin lectura",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun batteryText(battery: ComponentBattery?): String =
    battery?.takeIf { it.isFresh() }?.percent?.let { "$it%" } ?: "—"

private fun earbudsBatteryFresh(device: AirPodsDevice?): Boolean {
    if (device == null) return false
    return device.battery.left.isFresh() ||
        device.battery.right.isFresh() ||
        device.battery.combined.isFresh()
}

private fun earbudsBatteryText(device: AirPodsDevice?): String {
    if (device == null) return "—"
    val left = device.battery.left.takeIf { it.isFresh() }?.percent
    val right = device.battery.right.takeIf { it.isFresh() }?.percent
    if (left != null || right != null) {
        return listOfNotNull(
            left?.let { "L $it%" },
            right?.let { "R $it%" }
        ).joinToString(" · ")
    }
    return batteryText(device.battery.combined)
}

@Composable
private fun HomeEssentials(state: BluetoothUiState) {
    val connected = state.connectedDevice
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .14f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                if (connected != null) Icons.Outlined.Bluetooth else Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (connected != null) "Conexión activa" else "Detección privada",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (connected != null) {
                        "Bluetooth de Android · datos reales disponibles"
                    } else {
                        "Local, automática y sin cuenta de Apple"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            detail,
            Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
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
private fun DevicesScreen(
    state: BluetoothUiState,
    onPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScan: () -> Unit,
    onPair: (String) -> Unit,
    onReconnect: (String) -> Unit,
    mediaControls: MediaControls,
    diagnostics: AudioDiagnostics
) {
    val connectedDevice = state.connectedDevice
    val context = LocalContext.current
    var connectedAudioState by remember(connectedDevice?.address) {
        mutableStateOf(diagnostics.inspect())
    }
    LaunchedEffect(connectedDevice?.address) {
        while (connectedDevice != null) {
            connectedAudioState = diagnostics.inspect()
            delay(1_000)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Dispositivos", "Detección automática")
        DeviceDiscoveryPanel(state, onPermission, onEnableBluetooth, onScan)
        val available = state.devices.filterNot { it.connected }
        if (available.isNotEmpty()) {
            SectionHeader("Disponibles", if (state.status == BluetoothStatus.PAIRING) "Emparejando" else "${available.size} encontrados")
            AvailableDevices(available, state.status, onPair, onReconnect)
        }
        if (connectedDevice != null) {
            SectionHeader("Multimedia", "Control del sistema")
            MediaControlPanel(mediaControls)
            SectionHeader("Estado de audio", "Lecturas actuales de Android")
            ConnectedAudioStatusPanel(
                connectedAudioState,
                onToggleMicrophone = {
                    diagnostics.toggleMicrophone()
                    connectedAudioState = diagnostics.inspect()
                }
            )
            ConnectedDeviceActionsPanel {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            SectionHeader("Funciones del modelo", "Compatibilidad completa")
            ConnectedFeatureInventory(connectedDevice)
        }
        if (connectedDevice == null) {
            SectionHeader("Conexión sencilla", "Sin pasos innecesarios")
            ConnectionGuide()
        }
        if (connectedDevice == null) AcrylicCard {
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
private fun ConnectedAudioStatusPanel(
    state: AudioDiagnosticState,
    onToggleMicrophone: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            CompatibilityLine(
                "Salida multimedia",
                if (state.bluetoothOutput) "Activa por Bluetooth" else "Conectada, sin audio activo"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine("Códec A2DP", "No publicado por la API pública")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine(
                "Micrófono Bluetooth",
                if (state.bluetoothMicrophone) "Disponible" else "No activo en esta ruta"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine(
                "Llamada",
                when {
                    !state.callActive -> "Sin llamada activa"
                    state.microphoneMuted -> "Activa · micrófono silenciado"
                    else -> "Activa · micrófono disponible"
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Row(
                Modifier.fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .semantics {
                        role = Role.Button
                        stateDescription = when {
                            !state.callActive -> "No disponible, no hay una llamada activa"
                            !state.bluetoothMicrophone -> "No disponible en la ruta actual"
                            state.microphoneMuted -> "Micrófono silenciado"
                            else -> "Micrófono activo"
                        }
                    }
                    .clickable(
                        enabled = state.callActive && state.bluetoothMicrophone,
                        onClick = onToggleMicrophone
                    )
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.microphoneMuted) "Activar micrófono" else "Silenciar micrófono",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (state.callActive && state.bluetoothMicrophone) "Controlar" else "No disponible",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.callActive && state.bluetoothMicrophone) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine(
                "Audio espacial de Android",
                when {
                    !state.spatialAudioSupported -> "No compatible en este teléfono"
                    state.spatialAudioEnabled && state.spatialAudioAvailable -> "Activo"
                    state.spatialAudioAvailable -> "Disponible"
                    else -> "No disponible en esta ruta"
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine(
                "Seguimiento de cabeza",
                if (state.headTrackerAvailable) "Detectado por Android" else "No publicado"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine("Firmware", "No publicado por Android")
        }
    }
}

@Composable
private fun ConnectedDeviceActionsPanel(onBluetoothSettings: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Administrar nombre y vinculación en los ajustes Bluetooth de Android"
            }
            .clickable(onClick = onBluetoothSettings),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .14f)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Administrar dispositivo", fontWeight = FontWeight.SemiBold)
                Text(
                    "Nombre, vinculación y opciones disponibles en Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Abrir",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConnectedFeatureInventory(device: AirPodsDevice) {
    val capabilities = device.capabilities ?: AirPodsCapabilityRegistry.forModel(device.identifiedModel)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Los estados describen lo que esta app puede hacer o comprobar. “Control físico” significa que la función pertenece a los AirPods, pero Android no ofrece un control público equivalente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FeatureCapabilityGroup(
            "Audio y llamadas",
            listOf(
                AirPodsFeature.MEDIA_CONTROLS,
                AirPodsFeature.CALL_AUDIO,
                AirPodsFeature.MICROPHONE,
                AirPodsFeature.MICROPHONE_MUTE,
                AirPodsFeature.AUDIO_CODEC,
                AirPodsFeature.LOCK_SCREEN_CONTROLS,
                AirPodsFeature.LATENCY_DIAGNOSTIC,
                AirPodsFeature.LOW_POWER_MODE,
                AirPodsFeature.DEVICE_NAME
            ),
            capabilities
        )
        FeatureCapabilityGroup(
            "Modos de escucha",
            listOf(
                AirPodsFeature.ANC,
                AirPodsFeature.TRANSPARENCY,
                AirPodsFeature.ADAPTIVE_AUDIO,
                AirPodsFeature.CONVERSATION_AWARENESS,
                AirPodsFeature.PERSONALIZED_VOLUME,
                AirPodsFeature.ADAPTIVE_EQ,
                AirPodsFeature.LOUD_SOUND_REDUCTION
            ),
            capabilities
        )
        FeatureCapabilityGroup(
            "Sensores y controles",
            listOf(
                AirPodsFeature.EAR_DETECTION,
                AirPodsFeature.AUTO_PAUSE,
                AirPodsFeature.DOUBLE_TAP,
                AirPodsFeature.PRESS_CONTROLS,
                AirPodsFeature.DIGITAL_CROWN,
                AirPodsFeature.LISTENING_MODE_BUTTON,
                AirPodsFeature.CUSTOM_ACTIONS,
                AirPodsFeature.HEAD_GESTURES,
                AirPodsFeature.CALL_HEAD_GESTURES,
                AirPodsFeature.NOTIFICATION_GESTURES
            ),
            capabilities
        )
        FeatureCapabilityGroup(
            "Audio espacial",
            listOf(
                AirPodsFeature.SPATIAL_AUDIO,
                AirPodsFeature.HEAD_TRACKING,
                AirPodsFeature.PERSONALIZED_SPATIAL
            ),
            capabilities
        )
        FeatureCapabilityGroup(
            "Servicios de ecosistema",
            listOf(
                AirPodsFeature.ANNOUNCEMENTS,
                AirPodsFeature.SIRI,
                AirPodsFeature.ICLOUD,
                AirPodsFeature.APPLE_AUTO_SWITCH,
                AirPodsFeature.FIND_MY,
                AirPodsFeature.FIRMWARE_UPDATE,
                AirPodsFeature.HEARING_HEALTH,
                AirPodsFeature.HEARING_TEST,
                AirPodsFeature.HEARING_AID,
                AirPodsFeature.HEARING_PROTECTION,
                AirPodsFeature.APPLE_INTELLIGENCE,
                AirPodsFeature.LIVE_TRANSLATION
            ),
            capabilities
        )
    }
}

@Composable
private fun FeatureCapabilityGroup(
    title: String,
    features: List<AirPodsFeature>,
    capabilities: ModelCapabilities
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                title,
                modifier = Modifier.padding(top = 15.dp, bottom = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            features.forEachIndexed { index, feature ->
                CompatibilityLine(feature.title, capabilities.access(feature).label)
                if (index < features.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                }
            }
        }
    }
}

@Composable
private fun AvailableDevices(
    devices: List<AirPodsDevice>,
    status: BluetoothStatus,
    onPair: (String) -> Unit,
    onReconnect: (String) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            devices.forEachIndexed { index, device ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (device.bonded) "Vinculado · listo para reconectar" else device.identifiedModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        when {
                            status == BluetoothStatus.PAIRING -> "Esperando"
                            status == BluetoothStatus.RECONNECTING && device.bonded -> "Conectando"
                            device.bonded -> "Reconectar"
                            else -> "Emparejar"
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription =
                                    "${if (device.bonded) "Reconectar" else "Emparejar"} ${device.name}"
                            }
                            .clickable(enabled = status != BluetoothStatus.PAIRING && status != BluetoothStatus.RECONNECTING) {
                                if (device.bonded) onReconnect(device.address) else onPair(device.address)
                            }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index < devices.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                }
            }
        }
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
        BluetoothStatus.PAIRING -> "Emparejando AirPods"
        BluetoothStatus.RECONNECTING -> "Reconectando AirPods"
        BluetoothStatus.ERROR -> "Error de búsqueda"
        else -> "Buscando dispositivos"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .52f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f), RoundedCornerShape(24.dp))
            .semantics {
                liveRegion = LiveRegionMode.Polite
                stateDescription = title
            }
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
            if (state.status in listOf(BluetoothStatus.SEARCHING, BluetoothStatus.PAIRING, BluetoothStatus.RECONNECTING)) CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                Modifier.fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .semantics { role = Role.Button }
                    .clickable { action() },
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
                "Estado",
                "Conectado",
                if (connected.bonded) "Vinculado en Bluetooth de Android" else "Conexión activa sin vínculo guardado"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Spacer(Modifier.height(13.dp))
            Text("Batería por componente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val largeText = LocalDensity.current.fontScale >= 1.3f
            val batteryDescription = listOf(
                "Izquierdo ${connected.battery.left.percent?.let { "$it por ciento" } ?: "sin lectura"}",
                "Derecho ${connected.battery.right.percent?.let { "$it por ciento" } ?: "sin lectura"}",
                "Estuche ${connected.battery.case.percent?.let { "$it por ciento" } ?: "sin lectura"}"
            ).joinToString(". ")
            if (largeText) {
                Column(
                    Modifier.fillMaxWidth().semantics {
                        liveRegion = LiveRegionMode.Polite
                        stateDescription = batteryDescription
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatteryComponent("Izquierdo", connected.battery.left, Modifier.fillMaxWidth())
                    BatteryComponent("Derecho", connected.battery.right, Modifier.fillMaxWidth())
                    BatteryComponent("Estuche", connected.battery.case, Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().semantics {
                        liveRegion = LiveRegionMode.Polite
                        stateDescription = batteryDescription
                    },
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BatteryComponent("Izquierdo", connected.battery.left, Modifier.weight(1f))
                    BatteryComponent("Derecho", connected.battery.right, Modifier.weight(1f))
                    BatteryComponent("Estuche", connected.battery.case, Modifier.weight(1f))
                }
            }
            if (connected.battery.combined.percent != null) {
                Spacer(Modifier.height(10.dp))
                BatteryComponent("Batería general", connected.battery.combined, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            Text(
                batteryEvidenceDescription(connected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun batteryEvidenceDescription(device: AirPodsDevice): String {
    val evidence = device.batteryEvidence
    return when {
        evidence?.hasIndividualComponents == true ->
            "Este teléfono publicó batería separada para este modelo mediante ${evidence.source.label.lowercase()}."
        device.battery.combined.percent != null ->
            "Android publicó ${device.battery.combined.percent}% general mediante ${evidence?.source?.label?.lowercase() ?: "el perfil Bluetooth"}, sin separar izquierda, derecha y estuche."
        evidence?.hasCombined == true ->
            "Este modelo ya publicó batería general en este teléfono, pero todavía no hay una lectura en la conexión actual."
        else ->
            "Aún no existe una señal de batería reproducible para este modelo en este teléfono."
    }
}

@Composable
private fun BatteryComponent(label: String, battery: ComponentBattery, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(
            battery.percent?.let { if (battery.isFresh()) "$it%" else "$it%*" } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (battery.charging == true) {
            Text("Cargando", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        } else if (battery.percent != null && !battery.isFresh()) {
            Text("Última lectura", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ActivityScreen(
    state: BluetoothUiState,
    onClearHistory: () -> Unit,
    onDeleteProtocolSession: (String) -> Unit,
    onCompareProtocolSession: (String) -> List<ScenarioComparison>,
    onExportProtocolSession: (ProtocolSession) -> Unit,
    retentionSettings: HistoryRetentionSettings,
    onRetentionSettings: (HistoryRetentionSettings) -> Unit
) {
    var exportCandidate by remember { mutableStateOf<ProtocolSession?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Actividad", "Historial real")
        if (state.history.isEmpty()) ActivityEmptyState(state) else ActivityHistory(state.history, onClearHistory)
        SectionHeader("Laboratorio", "Sesiones separadas")
        ProtocolSessionsCard(
            state.protocolCapture.sessions,
            onDeleteProtocolSession,
            onCompareProtocolSession,
            { exportCandidate = it }
        )
        SectionHeader("Conservación", "Control local")
        RetentionCard(retentionSettings, onRetentionSettings)
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
    exportCandidate?.let { session ->
        AlertDialog(
            onDismissRequest = { exportCandidate = null },
            title = { Text("Exportar evidencia anónima") },
            text = {
                Text(
                    "Se exportarán escenario, modelo declarado, marcas de tiempo, origen técnico, señal y payload. " +
                        "No se incluyen direcciones Bluetooth, cuentas ni identificadores personales."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    exportCandidate = null
                    onExportProtocolSession(session)
                }) { Text("Autorizar exportación") }
            },
            dismissButton = {
                TextButton(onClick = { exportCandidate = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProtocolSessionsCard(
    sessions: List<ProtocolSession>,
    onDelete: (String) -> Unit,
    onCompare: (String) -> List<ScenarioComparison>,
    onExport: (ProtocolSession) -> Unit
) {
    var expandedSession by remember { mutableStateOf<String?>(null) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (sessions.isEmpty()) {
                Text(
                    "Las capturas completadas aparecerán aquí como sesiones independientes.",
                    Modifier.padding(vertical = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            sessions.forEachIndexed { index, session ->
                val expanded = expandedSession == session.id
                Column(
                    Modifier.fillMaxWidth().clickable {
                        expandedSession = session.id.takeUnless { expanded }
                    }.padding(vertical = 13.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(session.scenario.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${session.sampleCount} muestras · ${session.sources.size} fuentes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(session.startedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            session.model,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val comparisons = onCompare(session.id)
                        Text(
                            if (comparisons.isEmpty()) {
                                "Aún no hay otra sesión comparable con el mismo origen."
                            } else {
                                "${comparisons.count { it.reproducible }} resultados reproducibles de ${comparisons.size} comparaciones."
                            },
                            Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (comparisons.any { it.reproducible }) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Exportar",
                                Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                                    .semantics { role = Role.Button }
                                    .clickable { onExport(session) }.padding(vertical = 14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Eliminar",
                                Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                                    .semantics { role = Role.Button }
                                    .clickable {
                                    expandedSession = null
                                    onDelete(session.id)
                                }.padding(vertical = 14.dp),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (index < sessions.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                }
            }
        }
    }
}

@Composable
private fun RetentionCard(
    settings: HistoryRetentionSettings,
    onSettings: (HistoryRetentionSettings) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            RetentionSelector(
                "Conservar datos",
                "${settings.retentionDays} días",
                { onSettings(settings.copy(retentionDays = previousRetentionDays(settings.retentionDays))) },
                { onSettings(settings.copy(retentionDays = nextRetentionDays(settings.retentionDays))) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            RetentionSelector(
                "Eventos de actividad",
                "Máximo ${settings.maxActivityEvents}",
                { onSettings(settings.copy(maxActivityEvents = (settings.maxActivityEvents - 25).coerceAtLeast(25))) },
                { onSettings(settings.copy(maxActivityEvents = (settings.maxActivityEvents + 25).coerceAtMost(500))) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            RetentionSelector(
                "Muestras técnicas",
                "Máximo ${settings.maxProtocolSamples}",
                { onSettings(settings.copy(maxProtocolSamples = (settings.maxProtocolSamples - 100).coerceAtLeast(100))) },
                { onSettings(settings.copy(maxProtocolSamples = (settings.maxProtocolSamples + 100).coerceAtMost(2_000))) }
            )
        }
    }
}

@Composable
private fun RetentionSelector(
    title: String,
    value: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "−",
            Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { role = Role.Button; contentDescription = "Disminuir $title" }
                .clickable(onClick = onPrevious).padding(14.dp),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            "+",
            Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { role = Role.Button; contentDescription = "Aumentar $title" }
                .clickable(onClick = onNext).padding(14.dp),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

private fun previousRetentionDays(current: Int): Int =
    listOf(1, 7, 30, 90, 180, 365).lastOrNull { it < current } ?: 1

private fun nextRetentionDays(current: Int): Int =
    listOf(1, 7, 30, 90, 180, 365).firstOrNull { it > current } ?: 365

@Composable
private fun ActivityHistory(events: List<ConnectionEvent>, onClear: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            events.take(20).forEachIndexed { index, event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.Top) {
                    androidx.compose.material3.Icon(
                        when (event.type) {
                            ConnectionEventType.CONNECTED, ConnectionEventType.PAIRED -> Icons.Outlined.Headphones
                            ConnectionEventType.BATTERY -> Icons.Outlined.Timeline
                            ConnectionEventType.AUDIO -> Icons.Outlined.Headphones
                            ConnectionEventType.MONITOR -> Icons.Outlined.Timeline
                            ConnectionEventType.ERROR -> Icons.Outlined.Settings
                            ConnectionEventType.DISCONNECTED -> Icons.Outlined.Bluetooth
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(historyTitle(event.type), fontWeight = FontWeight.SemiBold)
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(event.deviceName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < events.take(20).lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Text(
                "Borrar historial",
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClear).padding(vertical = 14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun historyTitle(type: ConnectionEventType): String = when (type) {
    ConnectionEventType.CONNECTED -> "Conectado"
    ConnectionEventType.DISCONNECTED -> "Desconectado"
    ConnectionEventType.PAIRED -> "Emparejado"
    ConnectionEventType.BATTERY -> "Batería actualizada"
    ConnectionEventType.AUDIO -> "Ruta de audio"
    ConnectionEventType.MONITOR -> "Supervisión"
    ConnectionEventType.ERROR -> "Error"
}

@Composable
private fun SettingsScreen(
    diagnostics: AudioDiagnostics,
    selectedProfile: ListeningProfile,
    profileSettings: ListeningProfileSettings,
    onProfile: (ListeningProfile) -> Unit,
    onProfileSettings: (ListeningProfileSettings) -> Unit,
    captureState: ProtocolCaptureState,
    onStartCapture: (CaptureScenario) -> Unit,
    onStopCapture: () -> Unit,
    startAfterReboot: Boolean,
    onStartAfterReboot: (Boolean) -> Unit,
    monitoringEnabled: Boolean,
    onMonitoringEnabled: (Boolean) -> Unit,
    batteryOptimizationDisabled: Boolean,
    onBatteryOptimization: () -> Unit,
    monitorStatus: MonitorStatus,
    manufacturerGuidance: ManufacturerGuidance,
    backgroundRestricted: Boolean,
    notificationSettings: NotificationSettings,
    onNotificationSettings: (NotificationSettings) -> Unit,
    companionSupported: Boolean,
    companionAssociated: Boolean,
    companionMessage: String?,
    onCompanionAssociation: () -> Unit,
    onExportAllData: () -> Unit,
    onDeleteAllData: () -> Unit
) {
    var diagnosticState by remember { mutableStateOf(diagnostics.inspect()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader("Ajustes", "Preferencias")
        SectionHeader("Aplicación", "Control y privacidad")
        PreferencesGroup(
            startAfterReboot,
            onStartAfterReboot,
            monitoringEnabled,
            onMonitoringEnabled,
            batteryOptimizationDisabled,
            onBatteryOptimization,
            monitorStatus,
            manufacturerGuidance,
            backgroundRestricted,
            notificationSettings,
            onNotificationSettings,
            companionSupported,
            companionAssociated,
            companionMessage,
            onCompanionAssociation
        )
        SectionHeader("Servicios de Apple", "Acceso seguro")
        ICloudFindCard()
        SectionHeader("Privacidad", "Tus datos")
        PrivacySection(onExportAllData, onDeleteAllData)
        SectionHeader("Perfil", "Preferencia local")
        ProfileSelector(selectedProfile, profileSettings, onProfile, onProfileSettings)
        SectionHeader("Diagnóstico de audio", "Estado publicado por Android")
        AudioDiagnosticCard(diagnosticState, profileSettings) {
            diagnostics.toggleMicrophone()
            diagnosticState = diagnostics.inspect()
        }
        SectionHeader("Laboratorio", "Evidencia local")
        ProtocolCaptureCard(captureState, onStartCapture, onStopCapture)
        SectionHeader("Compatibilidad", "Limitaciones de Android")
        CompatibilityGroup()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ICloudFindCard() {
    val context = LocalContext.current
    var launchError by remember { mutableStateOf(false) }

    AcrylicCard {
        SettingLikeRow(
            icon = Icons.Outlined.LocationOn,
            title = "Buscar en iCloud",
            detail = "Localiza tus AirPods mediante el servicio oficial de Apple",
            status = "Abrir",
            onClick = {
                launchError = !ICloudFindLauncher.open(context)
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
        Row(
            Modifier.padding(top = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                if (launchError) {
                    "No hay un navegador compatible con pestañas seguras. Instala o actualiza tu navegador e inténtalo nuevamente."
                } else {
                    "Apple administra el inicio de sesión dentro de una pestaña segura. " +
                        "AirPods Companion no puede leer contraseñas, cookies, códigos ni ubicaciones."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (launchError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProtocolCaptureCard(
    state: ProtocolCaptureState,
    onStart: (CaptureScenario) -> Unit,
    onStop: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Registra durante 15 segundos señales Bluetooth observables, sin guardar direcciones del dispositivo.",
                modifier = Modifier.padding(top = 15.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CaptureScenario.entries.forEach { scenario ->
                val active = state.activeScenario == scenario
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = state.activeScenario == null || active) {
                            if (active) onStop() else onStart(scenario)
                        }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(scenario.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            active -> "Detener"
                            state.activeScenario != null -> "En espera"
                            else -> "Capturar"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.activeScenario == null || active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Text(
                "${state.sampleCount} muestras locales" +
                    (state.lastCompletedScenario?.let { " · Última: ${it.title}" } ?: "") +
                    "\n${state.scenariosWithEvidence} escenarios con 3+ muestras · " +
                    "${state.reproducibleComparisons} comparaciones reproducibles",
                modifier = Modifier.padding(vertical = 13.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSelector(
    selected: ListeningProfile,
    settings: ListeningProfileSettings,
    onSelect: (ListeningProfile) -> Unit,
    onSettings: (ListeningProfileSettings) -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ListeningProfile.entries.forEachIndexed { index, profile ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(profile) }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.title, fontWeight = FontWeight.SemiBold)
                        Text(profile.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.RadioButton(
                        selected = selected == profile,
                        onClick = { onSelect(profile) }
                    )
                }
                if (index < ListeningProfile.entries.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Column(Modifier.padding(vertical = 14.dp)) {
                Text("Perfil activo: ${selected.title}", fontWeight = FontWeight.Bold)
                Text(
                    "${settings.volumePercent}% de volumen · " +
                        listOfNotNull(
                            "conexión".takeIf { settings.connectionNotifications },
                            "desconexión".takeIf { settings.disconnectionNotifications },
                            "batería baja".takeIf { settings.lowBatteryNotifications }
                        ).joinToString(", ").ifEmpty { "sin avisos de evento" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volumen", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        "−",
                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { role = Role.Button; contentDescription = "Bajar volumen del perfil" }
                            .clickable {
                            onSettings(settings.copy(volumePercent = (settings.volumePercent - 5).coerceAtLeast(10)))
                        }.padding(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${settings.volumePercent}%", Modifier.width(48.dp), textAlign = TextAlign.Center)
                    Text(
                        "+",
                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { role = Role.Button; contentDescription = "Subir volumen del perfil" }
                            .clickable {
                            onSettings(settings.copy(volumePercent = (settings.volumePercent + 5).coerceAtMost(100)))
                        }.padding(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                ProfileToggle("Avisar al conectar", settings.connectionNotifications) {
                    onSettings(settings.copy(connectionNotifications = it))
                }
                ProfileToggle("Avisar al desconectar", settings.disconnectionNotifications) {
                    onSettings(settings.copy(disconnectionNotifications = it))
                }
                ProfileToggle("Avisar por batería baja", settings.lowBatteryNotifications) {
                    onSettings(settings.copy(lowBatteryNotifications = it))
                }
                Text(
                    "Diagnóstico visible",
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
                ProfileToggle("Salida Bluetooth", settings.showOutputDiagnostics) {
                    onSettings(settings.copy(showOutputDiagnostics = it))
                }
                ProfileToggle("Micrófono y llamadas", settings.showMicrophoneDiagnostics) {
                    onSettings(settings.copy(showMicrophoneDiagnostics = it))
                }
                ProfileToggle("Audio espacial", settings.showSpatialDiagnostics) {
                    onSettings(settings.copy(showSpatialDiagnostics = it))
                }
            }
        }
    }
}

@Composable
private fun ProfileToggle(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun AudioDiagnosticCard(
    state: AudioDiagnosticState,
    settings: ListeningProfileSettings,
    onToggleMic: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (settings.showOutputDiagnostics) {
                CompatibilityLine("Salida Bluetooth", if (state.bluetoothOutput) "Disponible" else "No detectada")
            }
            if (settings.showSpatialDiagnostics) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                CompatibilityLine(
                    "Audio espacial de Android",
                    when {
                        !state.spatialAudioSupported -> "No compatible"
                        state.spatialAudioEnabled && state.spatialAudioAvailable -> "Activo"
                        state.spatialAudioAvailable -> "Disponible"
                        else -> "No disponible en esta ruta"
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                CompatibilityLine("Seguimiento de cabeza", if (state.headTrackerAvailable) "Detectado" else "No publicado")
            }
            if (settings.showMicrophoneDiagnostics) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                CompatibilityLine("Micrófono Bluetooth", if (state.bluetoothMicrophone) "Disponible" else "No detectado")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                Row(
                Modifier.fillMaxWidth()
                    .clickable(enabled = state.callActive && state.bluetoothMicrophone, onClick = onToggleMic)
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Text("Micrófono en llamada", fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        !state.callActive -> "Sin llamada activa"
                        state.microphoneMuted -> "Silenciado"
                        else -> "Activo"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.callActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
            }
        }
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
private fun PreferencesGroup(
    startAfterReboot: Boolean,
    onStartAfterReboot: (Boolean) -> Unit,
    monitoringEnabled: Boolean,
    onMonitoringEnabled: (Boolean) -> Unit,
    batteryOptimizationDisabled: Boolean,
    onBatteryOptimization: () -> Unit,
    monitorStatus: MonitorStatus,
    manufacturerGuidance: ManufacturerGuidance,
    backgroundRestricted: Boolean,
    notificationSettings: NotificationSettings,
    onNotificationSettings: (NotificationSettings) -> Unit,
    companionSupported: Boolean,
    companionAssociated: Boolean,
    companionMessage: String?,
    onCompanionAssociation: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            SettingLikeRow(Icons.Outlined.Shield, "Privacidad", "Procesamiento local, sin cuenta de Apple", "Incluido")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            NotificationSettingRow(
                "Avisar al conectar",
                notificationSettings.connection
            ) { onNotificationSettings(notificationSettings.copy(connection = it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            NotificationSettingRow(
                "Avisar al desconectar",
                notificationSettings.disconnection
            ) { onNotificationSettings(notificationSettings.copy(disconnection = it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            NotificationSettingRow(
                "Batería baja",
                notificationSettings.lowBattery
            ) { onNotificationSettings(notificationSettings.copy(lowBattery = it)) }
            if (notificationSettings.lowBattery) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Umbral", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        "−",
                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { role = Role.Button; contentDescription = "Reducir umbral de batería" }
                            .clickable {
                            onNotificationSettings(
                                notificationSettings.copy(
                                    lowBatteryThreshold = (notificationSettings.lowBatteryThreshold - 5).coerceAtLeast(10)
                                )
                            )
                        }.padding(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${notificationSettings.lowBatteryThreshold}%", Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    Text(
                        "+",
                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { role = Role.Button; contentDescription = "Aumentar umbral de batería" }
                            .clickable {
                            onNotificationSettings(
                                notificationSettings.copy(
                                    lowBatteryThreshold = (notificationSettings.lowBatteryThreshold + 5).coerceAtMost(50)
                                )
                            )
                        }.padding(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Row(
                Modifier.fillMaxWidth().clickable { onMonitoringEnabled(!monitoringEnabled) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(Icons.Outlined.Headphones, null, Modifier.size(21.dp), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Supervisión", fontWeight = FontWeight.SemiBold)
                    Text("Conexión y batería en segundo plano", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(checked = monitoringEnabled, onCheckedChange = onMonitoringEnabled)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(
                Icons.Outlined.Timeline,
                "Estado del monitor",
                monitorStatus.detail,
                monitorStatus.state.label +
                    if (monitorStatus.recoveryCount > 0) " · ${monitorStatus.recoveryCount} rec." else ""
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(
                Icons.Outlined.Bluetooth,
                "Dispositivo complementario",
                companionMessage ?: when {
                    companionAssociated -> "Android puede despertar la app por presencia"
                    companionSupported -> "Asociación opcional para mejorar el segundo plano"
                    else -> "No disponible en este teléfono"
                },
                if (companionAssociated) "Asociado" else if (companionSupported) "Configurar" else "No compatible",
                onClick = if (companionSupported && !companionAssociated) onCompanionAssociation else null
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(
                Icons.Outlined.Shield,
                manufacturerGuidance.title,
                if (backgroundRestricted) {
                    "Android restringió la actividad en segundo plano. ${manufacturerGuidance.detail}"
                } else manufacturerGuidance.detail,
                if (backgroundRestricted) "Restringida" else manufacturerGuidance.manufacturer
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            Row(
                Modifier.fillMaxWidth().clickable { onStartAfterReboot(!startAfterReboot) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Supervisar tras reiniciar", fontWeight = FontWeight.SemiBold)
                    Text("Solo cuando lo autorices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(
                    checked = startAfterReboot,
                    onCheckedChange = onStartAfterReboot
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(
                Icons.Outlined.Settings,
                "Optimización de batería",
                if (batteryOptimizationDisabled) "Sin restricciones del sistema" else "Android puede limitar la supervisión",
                if (batteryOptimizationDisabled) "Correcto" else "Revisar",
                onClick = onBatteryOptimization
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            SettingLikeRow(Icons.Outlined.Settings, "Accesibilidad", "Contraste y tamaño del sistema", "Sistema")
        }
    }
}

@Composable
private fun NotificationSettingRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChecked)
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
            CompatibilityLine("Audio espacial", "Según Android")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
            CompatibilityLine("ANC y Transparencia", "Control físico")
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
    status: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    val largeText = LocalDensity.current.fontScale >= 1.3f
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
                            Modifier
                                .sizeIn(minHeight = 48.dp)
                                .semantics {
                                    role = Role.Tab
                                    this.selected = active
                                    contentDescription = label
                                }
                                .clickable { onSelect(index) }
                                .padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NavIcon(index, active)
                            if (!largeText) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
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
