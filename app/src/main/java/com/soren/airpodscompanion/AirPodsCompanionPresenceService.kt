package com.soren.airpodscompanion

import android.companion.CompanionDeviceService
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi

@RequiresApi(31)
class AirPodsCompanionPresenceService : CompanionDeviceService() {
    @Deprecated("Android callback required through API 35")
    override fun onDeviceAppeared(address: String) {
        MonitorStatusStore.get(this).record(MonitorState.STARTING, "AirPods asociados detectados")
        if (BackgroundPreferences(this).monitoringEnabled()) {
            runCatching {
                ContextCompat.startForegroundService(this, Intent(this, AirPodsMonitorService::class.java))
            }.onFailure {
                MonitorStatusStore.get(this).record(MonitorState.STOPPED, "No se pudo despertar el monitor")
            }
        }
    }

    @Deprecated("Android callback required through API 35")
    override fun onDeviceDisappeared(address: String) {
        MonitorStatusStore.get(this).record(MonitorState.RUNNING, "AirPods asociados fuera de alcance")
    }
}
