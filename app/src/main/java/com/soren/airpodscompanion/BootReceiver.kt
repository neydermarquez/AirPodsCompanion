package com.soren.airpodscompanion

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val preferences = BackgroundPreferences(context)
        if (!preferences.monitoringEnabled() || !preferences.startAfterReboot()) return
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            MonitorStatusStore.get(context).record(MonitorState.STARTING, "Recuperación solicitada después del reinicio")
            ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
        }.onFailure {
            MonitorStatusStore.get(context).record(MonitorState.STOPPED, "Android rechazó la recuperación: ${it.javaClass.simpleName}")
        }
    }
}
