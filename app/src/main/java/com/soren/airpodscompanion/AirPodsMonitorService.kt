package com.soren.airpodscompanion

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
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

    override fun onCreate() {
        super.onCreate()
        monitorStatus = MonitorStatusStore.get(this)
        monitorStatus.record(MonitorState.STARTING, "Servicio creado por Android")
        if (!BackgroundPreferences(this).monitoringEnabled()) {
            monitorStatus.record(MonitorState.DISABLED, "Supervisión desactivada por el usuario")
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        if (BackgroundPreferences(this).monitoringEnabled()) START_STICKY else {
            stopSelf()
            START_NOT_STICKY
        }

    override fun onDestroy() {
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
        }
        val battery = connected?.battery?.combined?.percent
        val threshold = NotificationPreferences(this).load().lowBatteryThreshold
        if (battery != null && battery <= threshold && (previousBattery == null || previousBattery!! > threshold)) {
            AirPodsNotifications.event(
                this, AirPodsNotificationType.LOW_BATTERY,
                "Batería baja", "${connected.name} tiene $battery% de batería."
            )
        }
        previousBattery = battery
        getSystemService(NotificationManager::class.java).notify(
            AirPodsNotifications.SERVICE_ID,
            AirPodsNotifications.monitoring(this, snapshots.load())
        )
    }
}
