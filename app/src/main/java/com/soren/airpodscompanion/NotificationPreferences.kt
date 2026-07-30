package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

enum class AirPodsNotificationType { CONNECTED, DISCONNECTED, LOW_BATTERY }

data class NotificationSettings(
    val connection: Boolean = true,
    val disconnection: Boolean = true,
    val lowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20
)

class NotificationPreferences(context: Context) {
    private val preferences = AppPreferencesRepository.get(context)

    fun load() = NotificationSettings(
        connection = preferences.boolean(NAMESPACE, "connection", true),
        disconnection = preferences.boolean(NAMESPACE, "disconnection", true),
        lowBattery = preferences.boolean(NAMESPACE, "low_battery", true),
        lowBatteryThreshold = preferences.int(NAMESPACE, "threshold", 20).coerceIn(10, 50)
    )

    fun save(settings: NotificationSettings) {
        preferences.putBoolean(NAMESPACE, "connection", settings.connection)
        preferences.putBoolean(NAMESPACE, "disconnection", settings.disconnection)
        preferences.putBoolean(NAMESPACE, "low_battery", settings.lowBattery)
        preferences.putInt(NAMESPACE, "threshold", settings.lowBatteryThreshold.coerceIn(10, 50))
    }

    fun enabled(type: AirPodsNotificationType): Boolean {
        val settings = load()
        return when (type) {
            AirPodsNotificationType.CONNECTED -> settings.connection
            AirPodsNotificationType.DISCONNECTED -> settings.disconnection
            AirPodsNotificationType.LOW_BATTERY -> settings.lowBattery
        }
    }

    private companion object { const val NAMESPACE = "notification_preferences" }
}
