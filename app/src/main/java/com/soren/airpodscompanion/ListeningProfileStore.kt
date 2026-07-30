package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

enum class ListeningProfile(val title: String, val detail: String) {
    MUSIC("Música", "Multimedia equilibrada y avisos esenciales"),
    CALLS("Llamadas", "Micrófono, conexión y avisos de llamada"),
    GAMES("Juegos", "Volumen moderado y diagnóstico de salida"),
    OFFICE("Oficina", "Volumen discreto y avisos de batería")
}

data class ListeningProfileSettings(
    val volumePercent: Int,
    val connectionNotifications: Boolean,
    val disconnectionNotifications: Boolean,
    val lowBatteryNotifications: Boolean,
    val showOutputDiagnostics: Boolean,
    val showMicrophoneDiagnostics: Boolean,
    val showSpatialDiagnostics: Boolean
)

class ListeningProfileStore(context: Context) {
    private val preferences = AppPreferencesRepository.get(context)

    fun load(): ListeningProfile = runCatching {
        ListeningProfile.valueOf(preferences.string(NAMESPACE, "selected", null) ?: "")
    }.getOrDefault(ListeningProfile.MUSIC)

    fun save(profile: ListeningProfile) {
        preferences.putString(NAMESPACE, "selected", profile.name)
    }

    fun settings(profile: ListeningProfile): ListeningProfileSettings {
        val defaults = defaults(profile)
        val prefix = "${profile.name.lowercase()}_"
        return ListeningProfileSettings(
            preferences.int(NAMESPACE, prefix + "volume", defaults.volumePercent).coerceIn(10, 100),
            preferences.boolean(NAMESPACE, prefix + "connect", defaults.connectionNotifications),
            preferences.boolean(NAMESPACE, prefix + "disconnect", defaults.disconnectionNotifications),
            preferences.boolean(NAMESPACE, prefix + "battery", defaults.lowBatteryNotifications),
            preferences.boolean(NAMESPACE, prefix + "diag_output", defaults.showOutputDiagnostics),
            preferences.boolean(NAMESPACE, prefix + "diag_mic", defaults.showMicrophoneDiagnostics),
            preferences.boolean(NAMESPACE, prefix + "diag_spatial", defaults.showSpatialDiagnostics)
        )
    }

    fun saveSettings(profile: ListeningProfile, settings: ListeningProfileSettings) {
        val prefix = "${profile.name.lowercase()}_"
        preferences.putInt(NAMESPACE, prefix + "volume", settings.volumePercent.coerceIn(10, 100))
        preferences.putBoolean(NAMESPACE, prefix + "connect", settings.connectionNotifications)
        preferences.putBoolean(NAMESPACE, prefix + "disconnect", settings.disconnectionNotifications)
        preferences.putBoolean(NAMESPACE, prefix + "battery", settings.lowBatteryNotifications)
        preferences.putBoolean(NAMESPACE, prefix + "diag_output", settings.showOutputDiagnostics)
        preferences.putBoolean(NAMESPACE, prefix + "diag_mic", settings.showMicrophoneDiagnostics)
        preferences.putBoolean(NAMESPACE, prefix + "diag_spatial", settings.showSpatialDiagnostics)
    }

    private fun defaults(profile: ListeningProfile) = when (profile) {
        ListeningProfile.MUSIC -> ListeningProfileSettings(70, true, true, true, true, false, true)
        ListeningProfile.CALLS -> ListeningProfileSettings(60, true, true, true, true, true, false)
        ListeningProfile.GAMES -> ListeningProfileSettings(55, true, true, false, true, true, false)
        ListeningProfile.OFFICE -> ListeningProfileSettings(40, false, true, true, false, true, false)
    }

    private companion object { const val NAMESPACE = "listening_profile" }
}
