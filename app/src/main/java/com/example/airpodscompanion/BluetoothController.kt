package com.example.airpodscompanion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BluetoothStatus {
    UNSUPPORTED, PERMISSION_REQUIRED, DISABLED, SEARCHING, READY, CONNECTED, ERROR
}

data class AirPodsDevice(
    val name: String,
    val address: String,
    val bonded: Boolean,
    val connected: Boolean,
    val identifiedModel: String,
    val identification: IdentificationConfidence,
    val batteryPercent: Int? = null
)

enum class IdentificationConfidence { EXACT_VARIANT, GENERATION, FAMILY, UNKNOWN }

data class BluetoothUiState(
    val status: BluetoothStatus = BluetoothStatus.PERMISSION_REQUIRED,
    val devices: List<AirPodsDevice> = emptyList(),
    val error: String? = null
) {
    val connectedDevice: AirPodsDevice? get() = devices.firstOrNull { it.connected }
}

class BluetoothController(private val context: Context) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter
    private val _state = MutableStateFlow(BluetoothUiState())
    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()
    private val found = linkedMapOf<String, AirPodsDevice>()
    private val batteryByAddress = mutableMapOf<String, Int>()
    private val proxies = mutableMapOf<Int, BluetoothProfile>()
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> refresh()
                BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> readBatteryEvent(intent)
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) addDevice(device, connected = false)
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            proxies[profile] = proxy
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
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
                addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + ".76")
            }
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            registered = true
        }
        refresh()
    }

    fun stop() {
        adapter?.cancelDiscoverySafely()
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
            bluetooth == null -> _state.value = BluetoothUiState(BluetoothStatus.UNSUPPORTED)
            !hasPermissions() -> _state.value = BluetoothUiState(BluetoothStatus.PERMISSION_REQUIRED)
            !bluetooth.isEnabled -> _state.value = BluetoothUiState(BluetoothStatus.DISABLED)
            else -> {
                found.clear()
                runCatching {
                    bluetooth.bondedDevices.forEach { addDevice(it, connected = false) }
                    ensureProfile(BluetoothProfile.A2DP)
                    ensureProfile(BluetoothProfile.HEADSET)
                    proxies.values.flatMap { it.connectedDevices }.forEach { addDevice(it, connected = true) }
                    val devices = found.values.sortedWith(compareByDescending<AirPodsDevice> { it.connected }.thenBy { it.name })
                    if (devices.any { it.connected } && bluetooth.isDiscovering) bluetooth.cancelDiscovery()
                    val status = when {
                        devices.any { it.connected } -> BluetoothStatus.CONNECTED
                        bluetooth.isDiscovering -> BluetoothStatus.SEARCHING
                        else -> BluetoothStatus.READY
                    }
                    _state.value = BluetoothUiState(status, devices)
                }.onFailure {
                    _state.value = BluetoothUiState(BluetoothStatus.ERROR, error = it.localizedMessage)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun scan() {
        val bluetooth = adapter ?: return refresh()
        if (!hasPermissions() || !bluetooth.isEnabled) return refresh()
        runCatching {
            if (bluetooth.isDiscovering) bluetooth.cancelDiscovery()
            if (!bluetooth.startDiscovery()) {
                _state.value = _state.value.copy(status = BluetoothStatus.ERROR, error = "Android no pudo iniciar la búsqueda.")
            } else {
                _state.value = _state.value.copy(status = BluetoothStatus.SEARCHING, error = null)
            }
        }.onFailure {
            _state.value = _state.value.copy(status = BluetoothStatus.ERROR, error = it.localizedMessage)
        }
    }

    @SuppressLint("MissingPermission")
    private fun ensureProfile(profile: Int) {
        if (proxies[profile] == null) adapter?.getProfileProxy(context, profileListener, profile)
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, connected: Boolean) {
        val name = runCatching { device.name }.getOrNull() ?: return
        if (!name.contains("airpods", ignoreCase = true)) return
        val address = runCatching { device.address }.getOrNull() ?: name
        val previous = found[address]
        val identification = AirPodsSignalParser.identify(name)
        found[address] = AirPodsDevice(
            name = name,
            address = address,
            bonded = device.bondState == BluetoothDevice.BOND_BONDED,
            connected = connected || previous?.connected == true,
            identifiedModel = identification.first,
            identification = identification.second,
            batteryPercent = batteryByAddress[address] ?: previous?.batteryPercent
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
        val percent = AirPodsSignalParser.parseVendorBattery(command, args) ?: return
        val address = runCatching { device.address }.getOrNull() ?: return
        batteryByAddress[address] = percent
        addDevice(device, connected = found[address]?.connected == true)
    }


    @SuppressLint("MissingPermission")
    private fun BluetoothAdapter.cancelDiscoverySafely() {
        if (hasPermissions() && isDiscovering) runCatching { cancelDiscovery() }
    }
}
