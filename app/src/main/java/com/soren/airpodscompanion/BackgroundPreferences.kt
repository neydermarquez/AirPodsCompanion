package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

class BackgroundPreferences(context: Context) {
    private val preferences = AppPreferencesRepository.get(context)

    fun startAfterReboot(): Boolean = preferences.boolean(NAMESPACE, KEY_START_AFTER_REBOOT, false)
    fun monitoringEnabled(): Boolean = preferences.boolean(NAMESPACE, KEY_MONITORING_ENABLED, true)

    fun setStartAfterReboot(enabled: Boolean) {
        preferences.putBoolean(NAMESPACE, KEY_START_AFTER_REBOOT, enabled)
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        preferences.putBoolean(NAMESPACE, KEY_MONITORING_ENABLED, enabled)
    }

    private companion object {
        const val KEY_START_AFTER_REBOOT = "start_after_reboot"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}
        const val NAMESPACE = "background_preferences"
