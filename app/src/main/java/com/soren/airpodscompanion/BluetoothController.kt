package com.soren.airpodscompanion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BluetoothStatus {
    UNSUPPORTED, PERMISSION_REQUIRED, DISABLED, SEARCHING, PAIRING, RECONNECTING, READY, CONNECTED, ERROR
}

data class AirPodsDevice(
    val name: String,
    val address: String,
    val bonded: Boolean,
    val connected: Boolean,
    val identifiedModel: String,
    val identification: IdentificationConfidence,
    val battery: AirPodsBatteryState = AirPodsBatteryState(),
    val batteryEvidence: BatteryEvidence? = null,
    val capabilities: ModelCapabilities? = null
)

enum class IdentificationConfidence { EXACT_VARIANT, GENERATION, FAMILY, UNKNOWN }

data class ComponentBattery(
    val percent: Int? = null,
    val charging: Boolean? = null,
    val observedAt: Long? = null
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean =
        percent != null && observedAt != null && now - observedAt <= BATTERY_FRESHNESS_MS

    companion object {
        const val BATTERY_FRESHNESS_MS = 10 * 60 * 1_000L
    }
}

data class AirPodsBatteryState(
    val left: ComponentBattery = ComponentBattery(),
    val right: ComponentBattery = ComponentBattery(),
    val case: ComponentBattery = ComponentBattery(),
    val combined: ComponentBattery = ComponentBattery()
)

data class BluetoothUiState(
    val status: BluetoothStatus = BluetoothStatus.PERMISSION_REQUIRED,
    val devices: List<AirPodsDevice> = emptyList(),
    val error: String? = null,
    val history: List<ConnectionEvent> = emptyList(),
    val protocolCapture: ProtocolCaptureState = ProtocolCaptureState(),
    val codecLabel: String? = null
) {
    val connectedDevice: AirPodsDevice? get() = devices.firstOrNull { it.connected }
}

class BluetoothController(private val context: Context) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter
    private val hiddenCompat = HiddenBluetoothCompat(context)
    private val _state = MutableStateFlow(BluetoothUiState(history = ConnectionHistoryStore(context).load()))
    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()
    private val found = linkedMapOf<String, AirPodsDevice>()
    private val batteryByAddress = mutableMapOf<String, AirPodsBatteryState>()
    private val historyStore = ConnectionHistoryStore(context)
    private val batteryEvidenceStore = BatteryEvidenceStore(context)
    private val batteryStateStore = BatteryStateStore(context)
    private val snapshotStore = DeviceSnapshotStore(context)
    private val captureStore = ProtocolCaptureStore(context)
    private val aliasStore = DeviceAliasStore(context)
    private val capabilityEngine = AirPodsCapabilityEngine(captureStore)
    private val initialSnapshot = snapshotStore.load()
    private var history = _state.value.history
    private val nearby = linkedMapOf<String, BluetoothDevice>()
    private val proxies = mutableMapOf<Int, BluetoothProfile>()
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var bleCapturing = false
    private var pendingReconnectAddress: String? = null
    private var reconnectGeneration = 0
    private var stableHistoryAddress: String? = initialSnapshot.takeIf { it.connected }?.address
    private var stableHistoryName: String = initialSnapshot.takeIf { it.connected }?.name ?: "AirPods"
    private var pendingHistoryAddress: String? = null
    private var hasPendingHistoryTransition = false
    private var historyTransitionGeneration = 0
    private val batteryPoll = object : Runnable {
        override fun run() {
            if (!registered) return
            pollConnectedBattery()
            handler.postDelayed(this, BATTERY_POLL_INTERVAL_MS)
        }
    }
    private val connectionPoll = object : Runnable {
        override fun run() {
            if (!registered) return
            refresh()
            handler.postDelayed(this, CONNECTION_POLL_INTERVAL_MS)
        }
    }

    private val protocolScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val bytes = result.scanRecord?.getManufacturerSpecificData(APPLE_COMPANY_ID) ?: return
            captureStore.append(
                model = _state.value.connectedDevice?.identifiedModel ?: "AirPods no determinado",
                source = "BLE Apple manufacturer data",
                payload = bytes.toHex(),
                rssi = result.rssi
            )
            publishCaptureState()
        }

        override fun onScanFailed(errorCode: Int) {
            bleCapturing = false
            reportError("La captura BLE no pudo iniciarse (código $errorCode).")
            stopProtocolCapture()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                BluetoothDevice.ACTION_NAME_CHANGED,
                BluetoothDevice.ACTION_UUID -> refresh()
                BluetoothDevice.ACTION_ACL_CONNECTED -> handleAclState(intent, connected = true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleAclState(intent, connected = false)
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> handleProfileConnection(intent)
                ACTION_CODEC_CONFIG_CHANGED -> refresh()
                BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED,
                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> handleAudioState(intent)
                ACTION_BATTERY_LEVEL_CHANGED -> readAndroidBatteryEvent(intent)
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> handleBondState(intent)
                BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> readBatteryEvent(intent)
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        runCatching { device.address }.getOrNull()?.let { nearby[it] = device }
                        addDevice(device, connected = false)
                    }
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            proxies[profile] = proxy
            attemptPendingReconnect()
            refresh()
        }

        override fun onServiceDisconnected(profile: Int) {
            proxies.remove(profile)
            refresh()
        }
    }

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun start() {
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_UUID)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                addAction(ACTION_CODEC_CONFIG_CHANGED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
                addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
                addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + ".76")
            }
            // Bluetooth profile/device events are emitted by the system Bluetooth process,
            // which is outside this app UID. Android 13 therefore requires an exported
            // dynamic receiver; NOT_EXPORTED silently drops these broadcasts.
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            registered = true
        }
        refresh()
        handler.removeCallbacks(batteryPoll)
        handler.post(batteryPoll)
        handler.removeCallbacks(connectionPoll)
        handler.post(connectionPoll)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        adapter?.cancelDiscoverySafely()
        stopBleCapture()
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
        proxies.toList().forEach { (profile, proxy) -> adapter?.closeProfileProxy(profile, proxy) }
        proxies.clear()
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        val bluetooth = adapter
        when {
            bluetooth == null -> {
                _state.value = BluetoothUiState(BluetoothStatus.UNSUPPORTED, history = history)
                AirPodsWidget.updateAll(context)
            }
            !hasPermissions() -> {
                _state.value = BluetoothUiState(BluetoothStatus.PERMISSION_REQUIRED, history = history)
                AirPodsWidget.updateAll(context)
            }
            !bluetooth.isEnabled -> {
                _state.value = BluetoothUiState(BluetoothStatus.DISABLED, history = history)
                scheduleAggregateConnectionHistory(device = null)
                AirPodsWidget.updateAll(context)
            }
            else -> {
                found.clear()
                runCatching {
                    bluetooth.bondedDevices.forEach { addDevice(it, connected = false) }
                    nearby.values.forEach { addDevice(it, connected = false) }
                    ensureProfile(BluetoothProfile.A2DP)
                    ensureProfile(BluetoothProfile.HEADSET)
                    proxies.values.flatMap { it.connectedDevices }.forEach { addDevice(it, connected = true) }
                    val devices = found.values.sortedWith(compareByDescending<AirPodsDevice> { it.connected }.thenBy { it.name })
                    if (devices.any { it.connected } && bluetooth.isDiscovering) bluetooth.cancelDiscovery()
                    val status = when {
                        devices.any { it.connected } -> BluetoothStatus.CONNECTED
                        pendingReconnectAddress != null -> BluetoothStatus.RECONNECTING
                        bluetooth.isDiscovering -> BluetoothStatus.SEARCHING
                        else -> BluetoothStatus.READY
                    }
                    _state.value = BluetoothUiState(
                        status,
                        devices,
                        history = history,
                        protocolCapture = _state.value.protocolCapture,
                        codecLabel = devices.firstOrNull { it.connected }?.let {
                            readCodecLabel(it.address)
                        }
                    )
                    scheduleAggregateConnectionHistory(devices.firstOrNull { it.connected })
                    if (devices.any { it.connected }) {
                        pendingReconnectAddress = null
                        reconnectGeneration += 1
                        snapshotStore.clearReconnecting()
                    }
                    snapshotStore.save(devices.firstOrNull { it.connected })
                }.onFailure {
                    val message = it.localizedMessage ?: "Error Bluetooth desconocido"
                    record(ConnectionEventType.ERROR, "Bluetooth", message)
                    _state.value = BluetoothUiState(BluetoothStatus.ERROR, error = message, history = history)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun scan() {
        val bluetooth = adapter ?: return refresh()
        if (!hasPermissions() || !bluetooth.isEnabled) return refresh()
        runCatching {
            nearby.clear()
            if (bluetooth.isDiscovering) bluetooth.cancelDiscovery()
            if (!bluetooth.startDiscovery()) {
                reportError("Android no pudo iniciar la búsqueda.")
            } else {
                _state.value = _state.value.copy(status = BluetoothStatus.SEARCHING, error = null)
            }
        }.onFailure {
            reportError(it.localizedMessage ?: "Error al iniciar la búsqueda.")
        }
    }

    @SuppressLint("MissingPermission")
    fun pair(address: String) {
        val bluetooth = adapter ?: return refresh()
        if (!hasPermissions() || !bluetooth.isEnabled) return refresh()
        runCatching {
            bluetooth.cancelDiscoverySafely()
            val device = bluetooth.getRemoteDevice(address)
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> reconnect(address)
                BluetoothDevice.BOND_BONDING -> _state.value = _state.value.copy(status = BluetoothStatus.PAIRING, error = null)
                else -> {
                    _state.value = _state.value.copy(status = BluetoothStatus.PAIRING, error = null)
                    if (!device.createBond()) {
                        reportError("Android no pudo iniciar el emparejamiento.", runCatching { device.name }.getOrNull() ?: "AirPods")
                    }
                }
            }
        }.onFailure {
            reportError(it.localizedMessage ?: "Error durante el emparejamiento.")
        }
    }

    @SuppressLint("MissingPermission")
    fun reconnect(address: String) {
        val bluetooth = adapter ?: return refresh()
        if (!hasPermissions() || !bluetooth.isEnabled) return refresh()
        runCatching {
            val device = bluetooth.getRemoteDevice(address)
            if (device.bondState != BluetoothDevice.BOND_BONDED) return pair(address)
            if (proxies.values.any { device in it.connectedDevices }) return refresh()
            bluetooth.cancelDiscoverySafely()
            snapshotStore.markReconnecting()
            _state.value = _state.value.copy(status = BluetoothStatus.RECONNECTING, error = null)
            pendingReconnectAddress = address
            reconnectGeneration += 1
            val generation = reconnectGeneration
            ensureProfile(BluetoothProfile.A2DP)
            ensureProfile(BluetoothProfile.HEADSET)
            attemptProfileConnection(device)
            device.fetchUuidsWithSdp()
            handler.postDelayed({
                if (pendingReconnectAddress == address && reconnectGeneration == generation) {
                    attemptProfileConnection(device)
                }
            }, PROFILE_RETRY_DELAY_MS)
            handler.postDelayed({
                if (pendingReconnectAddress != address || reconnectGeneration != generation) return@postDelayed
                refresh()
                if (_state.value.connectedDevice == null) {
                    pendingReconnectAddress = null
                    snapshotStore.clearReconnecting()
                    reportError(
                        "Android no permitió completar la conexión automática. Pon los AirPods en modo de enlace y usa “Abrir Bluetooth” para terminarla.",
                        runCatching { device.name }.getOrNull() ?: "AirPods"
                    )
                }
            }, RECONNECT_TIMEOUT_MS)
        }.onFailure {
            reportError(it.localizedMessage ?: "Error durante la reconexión.")
        }
    }

    fun resumeConnection() {
        start()
        handler.postDelayed({
            refresh()
            val current = _state.value
            if (current.connectedDevice == null) {
                current.devices.firstOrNull { it.bonded }?.let { reconnect(it.address) } ?: scan()
            }
        }, 700)
    }

    @SuppressLint("MissingPermission")
    private fun handleBondState(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return refresh()
        when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
            BluetoothDevice.BOND_BONDED -> {
                addDevice(device, connected = false)
                record(ConnectionEventType.PAIRED, runCatching { device.name }.getOrNull() ?: "AirPods", "Emparejamiento completado")
                runCatching { device.address }.getOrNull()?.let(::reconnect)
            }
            BluetoothDevice.BOND_BONDING -> _state.value = _state.value.copy(status = BluetoothStatus.PAIRING, error = null)
            else -> refresh()
        }
    }

    @SuppressLint("MissingPermission")
    private fun ensureProfile(profile: Int) {
        if (proxies[profile] == null) adapter?.getProfileProxy(context, profileListener, profile)
    }

    private fun attemptPendingReconnect() {
        val address = pendingReconnectAddress ?: return
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        attemptProfileConnection(device)
    }

    private fun attemptProfileConnection(device: BluetoothDevice): Boolean {
        val deviceConnectionStarted = hiddenCompat.connectDevice(device)
        val profiles = listOfNotNull(
            proxies[BluetoothProfile.A2DP],
            proxies[BluetoothProfile.HEADSET]
        )
        return profiles.fold(deviceConnectionStarted) { started, profile ->
            hiddenCompat.connectProfile(profile, device) || started
        }
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, connected: Boolean) {
        val address = runCatching { device.address }.getOrNull() ?: return
        val snapshot = snapshotStore.load()
        val publishedName = runCatching { device.name }.getOrNull()
        val knownAddress = snapshot.address == address
        val sourceName = publishedName ?: snapshot.name.takeIf { knownAddress } ?: return
        if (!BluetoothConnectionPolicy.isRecognizedAirPods(sourceName, address, snapshot.address)) return
        val name = aliasStore.get(address)
            ?: runCatching { if (Build.VERSION.SDK_INT >= 30) device.alias else null }.getOrNull()
            ?: sourceName
        val previous = found[address]
        val identification = AirPodsSignalParser.identify(
            sourceName.takeIf { it.contains("airpods", ignoreCase = true) } ?: snapshot.name ?: sourceName
        )
        found[address] = AirPodsDevice(
            name = name,
            address = address,
            bonded = device.bondState == BluetoothDevice.BOND_BONDED,
            connected = connected || previous?.connected == true,
            identifiedModel = identification.first,
            identification = identification.second,
            battery = batteryByAddress[address] ?: previous?.battery ?: batteryStateStore.load(identification.first),
            batteryEvidence = batteryEvidenceStore.find(identification.first)?.copy(model = identification.first),
            capabilities = capabilityEngine.forModel(identification.first)
        )
        val devices = found.values.sortedWith(compareByDescending<AirPodsDevice> { it.connected }.thenBy { it.name })
        _state.value = _state.value.copy(
            status = if (devices.any { it.connected }) BluetoothStatus.CONNECTED else _state.value.status,
            devices = devices
        )
    }


    @SuppressLint("MissingPermission")
    private fun readBatteryEvent(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return
        val command = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD) ?: return
        @Suppress("DEPRECATION")
        val args = (intent.getSerializableExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>)
            ?.mapNotNull { it?.toString() } ?: return
        val capturedName = runCatching { device.name }.getOrNull() ?: "AirPods"
        captureStore.append(
            model = AirPodsSignalParser.identify(capturedName).first,
            source = "HFP $command",
            payload = args.joinToString(",")
        )
        publishCaptureState()
        val signal = AirPodsSignalParser.parseVendorBattery(command, args) ?: return
        applyBatterySignal(device, signal)
    }

    private fun readAndroidBatteryEvent(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return
        val percent = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1).takeIf { it in 0..100 } ?: return
        applyAndroidBatteryLevel(device, percent)
    }

    @SuppressLint("MissingPermission")
    private fun pollConnectedBattery() {
        if (!hasPermissions() || adapter?.isEnabled != true) return
        proxies.values
            .flatMap { it.connectedDevices }
            .distinctBy { runCatching { it.address }.getOrNull() }
            .forEach { device ->
                hiddenCompat.getBatteryLevel(device)?.let { applyAndroidBatteryLevel(device, it) }
            }
    }

    private fun applyAndroidBatteryLevel(device: BluetoothDevice, percent: Int) {
        val signal = ParsedBatterySignal(
            battery = AirPodsBatteryState(
                combined = ComponentBattery(
                    percent = percent,
                    observedAt = System.currentTimeMillis()
                )
            ),
            source = BatterySignalSource.ANDROID_DEVICE
        )
        applyBatterySignal(device, signal)
    }

    @SuppressLint("MissingPermission")
    private fun applyBatterySignal(device: BluetoothDevice, signal: ParsedBatterySignal) {
        val address = runCatching { device.address }.getOrNull() ?: return
        val snapshot = snapshotStore.load()
        val name = runCatching { device.name }.getOrNull() ?: snapshot.name ?: "AirPods"
        if (!BluetoothConnectionPolicy.isRecognizedAirPods(name, address, snapshot.address)) return
        val previous = batteryByAddress[address] ?: AirPodsBatteryState()
        val battery = BluetoothConnectionPolicy.mergeBatteryReadings(previous, signal.battery)
        val model = AirPodsSignalParser.identify(
            name.takeIf { it.contains("airpods", ignoreCase = true) } ?: snapshot.name ?: name
        ).first
        batteryEvidenceStore.record(model, signal)
        batteryStateStore.save(model, battery)
        batteryByAddress[address] = battery
        if (BluetoothConnectionPolicy.batteryReadingsChanged(previous, battery)) {
            val detail = battery.combined.percent?.let {
                "Batería general publicada: $it% · ${signal.source.label}"
            } ?: "Batería por componente actualizada · ${signal.source.label}"
            record(ConnectionEventType.BATTERY, name, detail)
        }
        addDevice(device, connected = found[address]?.connected == true)
        snapshotStore.save(_state.value.connectedDevice)
    }

    fun clearHistory() {
        historyStore.clear()
        history = emptyList()
        _state.value = _state.value.copy(history = history)
    }

    fun reloadHistory() {
        historyStore.applyRetention()
        captureStore.applyRetention()
        history = historyStore.load()
        _state.value = _state.value.copy(history = history, protocolCapture = captureStore.state())
    }

    fun deleteProtocolSession(sessionId: String) {
        captureStore.deleteSession(sessionId)
        publishCaptureState()
    }

    fun exportProtocolSession(sessionId: String): String =
        captureStore.exportAnonymous(sessionId)

    fun compareProtocolSession(sessionId: String): List<ScenarioComparison> =
        captureStore.comparisonsForSession(sessionId)

    @SuppressLint("MissingPermission")
    fun startProtocolCapture(scenario: CaptureScenario) {
        val bluetooth = adapter
        if (bluetooth == null || !hasPermissions() || !bluetooth.isEnabled) return refresh()
        if (_state.value.connectedDevice == null) {
            reportError("Conecta los AirPods antes de iniciar una prueba local.")
            return
        }
        bluetooth.cancelDiscoverySafely()
        captureStore.start(scenario)
        publishCaptureState()
        val scanner = bluetooth.bluetoothLeScanner
        if (scanner == null) {
            reportError("Este teléfono no ofrece escaneo Bluetooth LE.")
            stopProtocolCapture()
            return
        }
        runCatching {
            scanner.startScan(
                emptyList(),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                protocolScanCallback
            )
            bleCapturing = true
            handler.postDelayed(::stopProtocolCapture, CAPTURE_DURATION_MS)
        }.onFailure {
            captureStore.stop()
            reportError(it.localizedMessage ?: "No fue posible iniciar la captura diagnóstica.")
        }
    }

    fun stopProtocolCapture() {
        stopBleCapture()
        captureStore.stop()
        publishCaptureState()
        refresh()
    }

    @SuppressLint("MissingPermission")
    private fun stopBleCapture() {
        if (!bleCapturing || !hasPermissions()) return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(protocolScanCallback) }
        bleCapturing = false
    }

    private fun publishCaptureState() {
        _state.value = _state.value.copy(protocolCapture = captureStore.state())
    }

    private fun record(type: ConnectionEventType, deviceName: String, detail: String) {
        val latest = history.firstOrNull()
        if (latest != null && latest.type == type && latest.deviceName == deviceName &&
            System.currentTimeMillis() - latest.timestamp < 2_000
        ) return
        history = historyStore.append(ConnectionEvent(System.currentTimeMillis(), type, deviceName, detail))
        _state.value = _state.value.copy(history = history)
    }

    private fun scheduleAggregateConnectionHistory(device: AirPodsDevice?) {
        val address = device?.address
        if (address == stableHistoryAddress) {
            hasPendingHistoryTransition = false
            pendingHistoryAddress = null
            historyTransitionGeneration += 1
            return
        }
        if (hasPendingHistoryTransition && pendingHistoryAddress == address) return

        hasPendingHistoryTransition = true
        pendingHistoryAddress = address
        historyTransitionGeneration += 1
        val generation = historyTransitionGeneration
        handler.postDelayed({
            if (!hasPendingHistoryTransition || generation != historyTransitionGeneration) return@postDelayed
            val latest = _state.value.connectedDevice
            if (latest?.address != address || stableHistoryAddress == address) {
                hasPendingHistoryTransition = false
                pendingHistoryAddress = null
                return@postDelayed
            }

            val previousName = stableHistoryName
            stableHistoryAddress = address
            stableHistoryName = latest?.name ?: previousName
            hasPendingHistoryTransition = false
            pendingHistoryAddress = null
            record(
                if (latest != null) ConnectionEventType.CONNECTED else ConnectionEventType.DISCONNECTED,
                latest?.name ?: previousName,
                if (latest != null) "Conexión Bluetooth establecida" else "Conexión Bluetooth finalizada"
            )
        }, if (address == null) DISCONNECTION_STABILITY_MS else CONNECTION_STABILITY_MS)
    }

    private fun reportError(message: String, deviceName: String = "Bluetooth") {
        record(ConnectionEventType.ERROR, deviceName, message)
        AppErrorCenter.report(message)
        _state.value = _state.value.copy(status = BluetoothStatus.ERROR, error = message, history = history)
    }

    @SuppressLint("MissingPermission")
    private fun handleProfileConnection(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
                pendingReconnectAddress = null
                reconnectGeneration += 1
                snapshotStore.clearReconnecting()
                if (device != null) addDevice(device, connected = true)
            }
        }
        handler.postDelayed(::refresh, PROFILE_STATE_SETTLE_MS)
    }

    @SuppressLint("MissingPermission")
    private fun handleAclState(intent: Intent, connected: Boolean) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return refresh()
        if (connected) {
            addDevice(device, connected = true)
            snapshotStore.save(_state.value.connectedDevice)
            pollConnectedBattery()
        }
        handler.postDelayed(::refresh, PROFILE_STATE_SETTLE_MS)
    }

    @SuppressLint("MissingPermission")
    private fun handleAudioState(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return
        val name = runCatching { device.name }.getOrNull() ?: "AirPods"
        if (!name.contains("airpods", true)) return
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
        val detail = when (intent.action) {
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED ->
                if (state == BluetoothA2dp.STATE_PLAYING) "A2DP inició reproducción multimedia"
                else "A2DP dejó de reproducir"
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> when (state) {
                BluetoothHeadset.STATE_AUDIO_CONNECTED -> "SCO conectó audio de llamada"
                BluetoothHeadset.STATE_AUDIO_CONNECTING -> "SCO está conectando"
                else -> "SCO desconectó audio de llamada"
            }
            else -> return
        }
        captureStore.append(
            model = AirPodsSignalParser.identify(name).first,
            source = when (intent.action) {
                BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> "Android A2DP playback state"
                else -> "Android HFP audio state"
            },
            payload = "%02X".format(state and 0xFF)
        )
        publishCaptureState()
        record(ConnectionEventType.AUDIO, name, detail)
    }


    @SuppressLint("MissingPermission")
    private fun BluetoothAdapter.cancelDiscoverySafely() {
        if (hasPermissions() && isDiscovering) runCatching { cancelDiscovery() }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentA2dpCodecConfig(): String? {
        val a2dp = proxies[BluetoothProfile.A2DP] as? BluetoothA2dp ?: return null
        val device = _state.value.connectedDevice?.address
            ?.let { runCatching { adapter?.getRemoteDevice(it) }.getOrNull() }
            ?: return null
        return hiddenCompat.getA2dpCodecConfig(a2dp, device)?.toString()
    }

    @SuppressLint("MissingPermission")
    fun getA2dpCodecStatus(): String? {
        val a2dp = proxies[BluetoothProfile.A2DP] as? BluetoothA2dp ?: return null
        val device = _state.value.connectedDevice?.address
            ?.let { runCatching { adapter?.getRemoteDevice(it) }.getOrNull() }
            ?: return null
        return hiddenCompat.getA2dpCodecStatus(a2dp, device)?.toString()
    }

    @SuppressLint("MissingPermission")
    fun renameDevice(address: String, alias: String): Boolean {
        val normalized = alias.trim().take(48)
        if (normalized.isBlank()) return false
        aliasStore.save(address, normalized)
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
        if (Build.VERSION.SDK_INT >= 31 && device != null && hasPermissions()) {
            runCatching { device.setAlias(normalized) }
        }
        refresh()
        return true
    }

    private fun readCodecLabel(address: String): String? {
        val a2dp = proxies[BluetoothProfile.A2DP] as? BluetoothA2dp ?: return null
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return null
        val status = hiddenCompat.getA2dpCodecStatus(a2dp, device)
        val config = status?.let {
            runCatching { it.javaClass.getMethod("getCodecConfig").invoke(it) }.getOrNull()
        } ?: hiddenCompat.getA2dpCodecConfig(a2dp, device)
        return codecName(config)
    }

    private fun codecName(config: Any?): String? {
        if (config == null) return null
        val extended = runCatching {
            config.javaClass.getMethod("getExtendedCodecType").invoke(config)
        }.getOrNull()
        val extendedName = extended?.let {
            runCatching { it.javaClass.getMethod("getCodecName").invoke(it)?.toString() }.getOrNull()
        }
        if (!extendedName.isNullOrBlank()) return extendedName
        val type = runCatching {
            (config.javaClass.getMethod("getCodecType").invoke(config) as? Number)?.toInt()
        }.getOrNull()
        return when (type) {
            0 -> "SBC"
            1 -> "AAC"
            2 -> "aptX"
            3 -> "aptX HD"
            4 -> "LDAC"
            5 -> "LC3"
            6 -> "Opus"
            else -> config.toString()
                .lineSequence()
                .firstOrNull { "codec" in it.lowercase() }
                ?.substringAfter("=")
                ?.substringBefore(",")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
    }

    @SuppressLint("MissingPermission")
    fun setA2dpCodecPreference(codecConfig: Any): Boolean {
        val a2dp = proxies[BluetoothProfile.A2DP] as? BluetoothA2dp ?: return false
        return hiddenCompat.setA2dpCodecPreference(a2dp, codecConfig)
    }

    fun setA2dpAudioPolicy(enabled: Boolean): Boolean {
        return hiddenCompat.setBluetoothA2dpEnabled(enabled)
    }

    fun getAudioParams(keys: String): String? {
        return hiddenCompat.getAudioParameters(keys)
    }

    fun setAudioParams(keyValuePairs: String): Boolean {
        return hiddenCompat.setAudioParameters(keyValuePairs)
    }

    fun getLowLevelAdapterConnectionState(): Int? {
        return hiddenCompat.getAdapterConnectionState()
    }

    @SuppressLint("MissingPermission")
    fun disconnectDeviceFromAdapter(deviceAddress: String): Boolean {
        val target = runCatching { adapter?.getRemoteDevice(deviceAddress) }.getOrNull() ?: return false
        return hiddenCompat.disconnectFromAdapter(target)
    }

    @SuppressLint("MissingPermission")
    fun getLowLevelLeConnectionState(deviceAddress: String): Int? {
        val target = runCatching { adapter?.getRemoteDevice(deviceAddress) }.getOrNull() ?: return null
        return hiddenCompat.getAdapterLeConnectionState(target)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    private companion object {
        const val APPLE_COMPANY_ID = 0x004C
        const val CAPTURE_DURATION_MS = 15_000L
        const val PROFILE_RETRY_DELAY_MS = 1_200L
        const val PROFILE_STATE_SETTLE_MS = 350L
        const val RECONNECT_TIMEOUT_MS = 12_000L
        const val BATTERY_POLL_INTERVAL_MS = 15_000L
        const val CONNECTION_POLL_INTERVAL_MS = 2_000L
        const val CONNECTION_STABILITY_MS = 2_000L
        const val DISCONNECTION_STABILITY_MS = 12_000L
        const val ACTION_CODEC_CONFIG_CHANGED = "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED"
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
