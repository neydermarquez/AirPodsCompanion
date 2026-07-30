package com.soren.airpodscompanion

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AirPodsMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: AirPodsRepository
    private lateinit var snapshots: DeviceSnapshotStore
    private var observation: Job? = null
    private var previousConnectedAddress: String? = null
    private var previousBattery: Int? = null
    private lateinit var monitorStatus: MonitorStatusStore
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var a2dpProfile: BluetoothA2dp? = null
    private val hiddenApiManager = HiddenApiManager()
    private var lastCodecSignature: String? = null

    private val a2dpProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.A2DP) return
            val a2dp = proxy as? BluetoothA2dp ?: return
            a2dpProfile = a2dp
            Log.d("AirPods", "Perfil A2DP conectado")
            repository.controller.state.value.connectedDevice?.let { onAirPodsConnected(a2dp, it.address) }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                Log.d("AirPods", "A2DP Profile disconnected.")
                a2dpProfile = null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        monitorStatus = MonitorStatusStore.get(this)
        monitorStatus.record(MonitorState.STARTING, "Servicio creado por Android")
        if (!BackgroundPreferences(this).monitoringEnabled()) {
            monitorStatus.record(MonitorState.DISABLED, "Supervisión desactivada por el usuario")
            stopSelf()
            return
        }
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter ?: run {
                monitorStatus.record(MonitorState.STOPPED, "Bluetooth no disponible")
                stopSelf()
                return
            }

        repository = AirPodsRepository.get(this)
        snapshots = DeviceSnapshotStore(this)
        snapshots.load().takeIf { it.connected }?.let {
            previousConnectedAddress = it.address
            previousBattery = it.batteryPercent
        }
        AirPodsNotifications.createChannels(this)
        startForeground(AirPodsNotifications.SERVICE_ID, AirPodsNotifications.monitoring(this, snapshots.load()))
        repository.acquire(RepositoryOwner.MONITOR_SERVICE)
        monitorStatus.record(MonitorState.RUNNING, "Observando el repositorio Bluetooth")
        repository.controller.reloadHistory()
        observation = scope.launch {
            repository.controller.state.collectLatest(::handleState)
        }
        registerA2dpProfileListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        if (BackgroundPreferences(this).monitoringEnabled()) START_STICKY else {
            stopSelf()
            START_NOT_STICKY
        }

    private fun registerA2dpProfileListener() {
        runCatching {
            bluetoothAdapter.getProfileProxy(this, a2dpProfileListener, BluetoothProfile.A2DP)
        }.onFailure {
            Log.e("AirPods", "No se pudo obtener proxy A2DP", it)
            monitorStatus.record(MonitorState.WARNING, "Perfil A2DP no disponible")
        }
    }

    override fun onDestroy() {
        a2dpProfile?.let {
            runCatching { bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
        }
        observation?.cancel()
        scope.cancel()
        if (::repository.isInitialized) repository.release(RepositoryOwner.MONITOR_SERVICE)
        if (::monitorStatus.isInitialized) {
            val disabled = !BackgroundPreferences(this).monitoringEnabled()
            monitorStatus.record(
                if (disabled) MonitorState.DISABLED else MonitorState.STOPPED,
                if (disabled) "Detenido por el usuario" else "Servicio detenido por Android o por la aplicación"
            )
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleState(state: BluetoothUiState) {
        val connected = state.connectedDevice
        val currentAddress = connected?.address
        if (currentAddress != previousConnectedAddress) {
            when {
                connected != null -> AirPodsNotifications.event(
                    this, AirPodsNotificationType.CONNECTED,
                    "${connected.name} conectado", "El audio Bluetooth está disponible."
                )
                previousConnectedAddress != null -> AirPodsNotifications.event(
                    this, AirPodsNotificationType.DISCONNECTED,
                    "AirPods desconectados", "Se perdió la conexión Bluetooth."
                )
            }
            previousConnectedAddress = currentAddress
            currentAddress?.let { address ->
                inspectCurrentCodec(address)
            }
        }

        if (connected != null) {
            inspectCurrentCodec(currentAddress)
        }

        val batteryReading = connected?.battery?.let(BluetoothConnectionPolicy::lowestFreshBattery)
        val battery = batteryReading?.second
        val threshold = NotificationPreferences(this).load().lowBatteryThreshold
        if (battery != null && battery <= threshold && (previousBattery == null || previousBattery!! > threshold)) {
            AirPodsNotifications.event(
                this, AirPodsNotificationType.LOW_BATTERY,
                "Batería baja", "${batteryReading.first.replaceFirstChar(Char::uppercase)}: $battery%."
            )
        }
        previousBattery = battery

        getSystemService(NotificationManager::class.java).notify(
            AirPodsNotifications.SERVICE_ID,
            AirPodsNotifications.monitoring(this, snapshots.load())
        )
    }

    @SuppressLint("MissingPermission")
    private fun onAirPodsConnected(a2dp: BluetoothA2dp, connectedAddress: String) {
        if (!hasBluetoothConnectPermission()) return
        val target = a2dp.connectedDevices.firstOrNull { device ->
            device.address == connectedAddress && isAirPods(device)
        } ?: a2dp.connectedDevices.firstOrNull(::isAirPods) ?: return
        val codecConfig = hiddenApiManager.getCodecConfig(a2dp)
        val signature = "${target.address}:${codecConfig?.javaClass?.name ?: "unknown"}:${codecConfig?.hashCode() ?: 0}"
        if (signature == lastCodecSignature) return
        lastCodecSignature = signature
        Log.d("AirPods", "Active Codec: ${target.name} -> $codecConfig")
    }

    private fun inspectCurrentCodec(connectedAddress: String?) {
        val a2dp = a2dpProfile ?: return
        val address = connectedAddress ?: return
        onAirPodsConnected(a2dp, address)
    }

    @SuppressLint("MissingPermission")
    private fun isAirPods(device: BluetoothDevice): Boolean =
        runCatching { device.name }.getOrNull()?.contains("airpods", ignoreCase = true) == true

    private fun hasBluetoothConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}

fun triggerReconnect(context: Context, device: BluetoothDevice) {
    runCatching {
        val pairingIntent = Intent(BluetoothDevice.ACTION_PAIRING_REQUEST).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        }
        context.sendBroadcast(pairingIntent)
    }

    runCatching {
        val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(settingsIntent)
    }
}

fun setAudioPolicy(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    try {
        audioManager.setParameters("A2dpSuspended=false;")
        audioManager.setParameters("bluetooth_enabled=true;")
    } catch (e: SecurityException) {
        Log.e("AudioPolicy", "Permission denied to set audio parameters", e)
    } catch (e: IllegalArgumentException) {
        Log.w("AudioPolicy", "Parámetro de audio no soportado", e)
    }
}
