package com.soren.airpodscompanion

import android.content.Context
import android.util.Base64

class BatteryStateStore(context: Context) {
    private val preferences = context.getSharedPreferences("battery_states", Context.MODE_PRIVATE)

    fun load(model: String): AirPodsBatteryState =
        preferences.getString(key(model), null)?.let(::decode) ?: AirPodsBatteryState()

    fun save(model: String, state: AirPodsBatteryState) {
        preferences.edit().putString(key(model), encode(state)).apply()
    }

    private fun key(model: String) =
        Base64.encodeToString(model.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun encode(state: AirPodsBatteryState) =
        listOf(state.left, state.right, state.case, state.combined)
            .joinToString(";") { component ->
                listOf(component.percent ?: "", component.charging ?: "", component.observedAt ?: "").joinToString(",")
            }

    private fun decode(value: String): AirPodsBatteryState? {
        val components = value.split(";").map(::decodeComponent)
        if (components.size != 4 || components.any { it == null }) return null
        return AirPodsBatteryState(
            left = components[0]!!,
            right = components[1]!!,
            case = components[2]!!,
            combined = components[3]!!
        )
    }

    private fun decodeComponent(value: String): ComponentBattery? {
        val parts = value.split(",", limit = 3)
        if (parts.size != 3) return null
        return ComponentBattery(
            percent = parts[0].toIntOrNull(),
            charging = parts[1].takeIf(String::isNotEmpty)?.toBooleanStrictOrNull(),
            observedAt = parts[2].toLongOrNull()
        )
    }
}
