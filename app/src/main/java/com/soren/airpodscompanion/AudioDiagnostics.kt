package com.soren.airpodscompanion

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import androidx.annotation.RequiresApi

data class AudioDiagnosticState(
    val bluetoothOutput: Boolean,
    val bluetoothMicrophone: Boolean,
    val callActive: Boolean,
    val microphoneMuted: Boolean,
    val spatialAudioSupported: Boolean,
    val spatialAudioAvailable: Boolean,
    val spatialAudioEnabled: Boolean,
    val headTrackerAvailable: Boolean
)

class AudioDiagnostics(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun inspect(): AudioDiagnosticState {
        val outputs = audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        val inputs = audio?.getDevices(AudioManager.GET_DEVICES_INPUTS).orEmpty()
        val spatial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) inspectSpatializer() else SpatialState()
        return AudioDiagnosticState(
            bluetoothOutput = outputs.any { it.isBluetooth() },
            bluetoothMicrophone = inputs.any { it.isBluetooth() },
            callActive = audio?.mode in listOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION),
            microphoneMuted = audio?.isMicrophoneMute == true,
            spatialAudioSupported = spatial.supported,
            spatialAudioAvailable = spatial.available,
            spatialAudioEnabled = spatial.enabled,
            headTrackerAvailable = spatial.headTracker
        )
    }

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private fun inspectSpatializer(): SpatialState {
        val spatializer = audio?.spatializer ?: return SpatialState()
        return SpatialState(
            supported = spatializer.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE,
            available = spatializer.isAvailable,
            enabled = spatializer.isEnabled,
            headTracker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                spatializer.isHeadTrackerAvailable
            } else false
        )
    }

    fun toggleMicrophone(): Boolean {
        val state = inspect()
        if (!state.callActive || !state.bluetoothMicrophone) return false
        @Suppress("DEPRECATION")
        audio?.isMicrophoneMute = !state.microphoneMuted
        return true
    }

    private fun AudioDeviceInfo.isBluetooth() = type in setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER
    )

    private data class SpatialState(
        val supported: Boolean = false,
        val available: Boolean = false,
        val enabled: Boolean = false,
        val headTracker: Boolean = false
    )
}
