package com.soren.airpodscompanion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RECONNECT) return
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
        if (BackgroundPreferences(context).monitoringEnabled()) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, AirPodsMonitorService::class.java))
            }
        }
        AirPodsRepository.get(context).controller.reconnect(address)
    }

    companion object {
        const val ACTION_RECONNECT = "com.soren.airpodscompanion.action.RECONNECT"
        const val EXTRA_ADDRESS = "address"
    }
}
