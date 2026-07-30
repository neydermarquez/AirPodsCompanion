package com.soren.airpodscompanion

import android.content.Context

data class DeviceSnapshot(
    val name: String?,
    val address: String?,
    val connected: Boolean,
    val batteryPercent: Int?,
    val batteryObservedAt: Long?,
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val reconnectRequestedAt: Long? = null
) {
    val batteryFresh: Boolean
        get() = batteryPercent != null && batteryObservedAt != null &&
            System.currentTimeMillis() - batteryObservedAt <= ComponentBattery.BATTERY_FRESHNESS_MS
    val reconnecting: Boolean
        get() = !connected && reconnectRequestedAt != null &&
            System.currentTimeMillis() - reconnectRequestedAt <= RECONNECT_WINDOW_MS

    companion object {
        const val RECONNECT_WINDOW_MS = 10_000L
    }
}

class DeviceSnapshotStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("device_snapshot", Context.MODE_PRIVATE)

    fun load() = DeviceSnapshot(
        preferences.getString("name", null),
        preferences.getString("address", null),
        preferences.getBoolean("connected", false),
        preferences.getInt("battery", -1).takeIf { it >= 0 },
        preferences.getLong("battery_observed_at", 0L).takeIf { it > 0 },
        preferences.getInt("left_battery", -1).takeIf { it >= 0 },
        preferences.getInt("right_battery", -1).takeIf { it >= 0 },
        preferences.getInt("case_battery", -1).takeIf { it >= 0 },
        preferences.getLong("reconnect_requested_at", 0L).takeIf { it > 0 }
    )

    fun save(device: AirPodsDevice?) {
        val previous = load()
        preferences.edit()
            .putString("name", device?.name ?: previous.name)
            .putString("address", device?.address ?: previous.address)
            .putBoolean("connected", device?.connected == true)
            .putInt("battery", device?.battery?.combined?.percent ?: previous.batteryPercent ?: -1)
            .putLong(
                "battery_observed_at",
                device?.battery?.combined?.observedAt ?: previous.batteryObservedAt ?: 0L
            )
            .putInt("left_battery", device?.battery?.left?.percent ?: previous.leftBatteryPercent ?: -1)
            .putInt("right_battery", device?.battery?.right?.percent ?: previous.rightBatteryPercent ?: -1)
            .putInt("case_battery", device?.battery?.case?.percent ?: previous.caseBatteryPercent ?: -1)
            .putLong("reconnect_requested_at", if (device?.connected == true) 0L else previous.reconnectRequestedAt ?: 0L)
            .apply()
        AirPodsWidget.updateAll(context)
    }

    fun markReconnecting() {
        preferences.edit().putLong("reconnect_requested_at", System.currentTimeMillis()).apply()
        AirPodsWidget.updateAll(context)
    }

    fun clearReconnecting() {
        preferences.edit().putLong("reconnect_requested_at", 0L).apply()
        AirPodsWidget.updateAll(context)
    }

    fun updateBattery(name: String, percent: Int) {
        preferences.edit().putString("name", name).putInt("battery", percent)
            .putLong("battery_observed_at", System.currentTimeMillis()).apply()
        AirPodsWidget.updateAll(context)
    }
}
