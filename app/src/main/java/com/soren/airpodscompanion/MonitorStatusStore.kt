package com.soren.airpodscompanion

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MonitorState(val label: String) {
    DISABLED("Desactivado"),
    STARTING("Iniciando"),
    RUNNING("Activo"),
    WARNING("Activo con limitaciones"),
    STOPPED("Detenido")
}

data class MonitorStatus(
    val state: MonitorState,
    val updatedAt: Long,
    val recoveryCount: Int,
    val detail: String
)

class MonitorStatusStore private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("monitor_status", Context.MODE_PRIVATE)
    private val history = ConnectionHistoryStore(context)
    private val _status = MutableStateFlow(load())
    val status: StateFlow<MonitorStatus> = _status.asStateFlow()

    fun record(state: MonitorState, detail: String) {
        val previous = _status.value
        val recovery = if (state == MonitorState.STARTING && previous.state == MonitorState.RUNNING) {
            previous.recoveryCount + 1
        } else previous.recoveryCount
        val value = MonitorStatus(state, System.currentTimeMillis(), recovery, detail)
        preferences.edit()
            .putString("state", state.name)
            .putLong("updated_at", value.updatedAt)
            .putInt("recovery_count", recovery)
            .putString("detail", detail)
            .apply()
        _status.value = value
        history.append(
            ConnectionEvent(
                timestamp = value.updatedAt,
                type = ConnectionEventType.MONITOR,
                deviceName = "Supervisión",
                detail = "${state.label}: $detail"
            )
        )
    }

    private fun load() = MonitorStatus(
        state = runCatching {
            MonitorState.valueOf(preferences.getString("state", MonitorState.STOPPED.name)!!)
        }.getOrDefault(MonitorState.STOPPED),
        updatedAt = preferences.getLong("updated_at", 0L),
        recoveryCount = preferences.getInt("recovery_count", 0),
        detail = preferences.getString("detail", "Sin actividad registrada") ?: "Sin actividad registrada"
    )

    companion object {
        @Volatile private var instance: MonitorStatusStore? = null
        fun get(context: Context): MonitorStatusStore =
            instance ?: synchronized(this) {
                instance ?: MonitorStatusStore(context.applicationContext).also { instance = it }
            }
    }
}

data class ManufacturerGuidance(
    val manufacturer: String,
    val title: String,
    val detail: String
)

object ManufacturerBackgroundGuide {
    fun current(): ManufacturerGuidance {
        val manufacturer = Build.MANUFACTURER.ifBlank { "Android" }
        return when (manufacturer.lowercase()) {
            "samsung" -> ManufacturerGuidance(
                manufacturer, "Batería en segundo plano",
                "En Samsung, revisa que AirPods Companion no esté en Aplicaciones suspendidas."
            )
            "xiaomi", "redmi", "poco" -> ManufacturerGuidance(
                manufacturer, "Inicio automático y batería",
                "En Xiaomi, permite Inicio automático y selecciona Sin restricciones de batería."
            )
            "motorola" -> ManufacturerGuidance(
                manufacturer, "Uso de batería",
                "En Motorola, permite actividad en segundo plano para mantener la supervisión."
            )
            "oneplus", "oppo", "realme" -> ManufacturerGuidance(
                manufacturer, "Optimización del sistema",
                "Permite ejecución en segundo plano y desactiva la optimización para esta app."
            )
            "huawei", "honor" -> ManufacturerGuidance(
                manufacturer, "Inicio de aplicaciones",
                "Administra manualmente el inicio y permite actividad secundaria."
            )
            else -> ManufacturerGuidance(
                manufacturer, "Optimización de batería",
                "Usa los ajustes de Android si el monitor deja de actualizarse con la app cerrada."
            )
        }
    }
}
