package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

data class HistoryRetentionSettings(
    val retentionDays: Int = 30,
    val maxActivityEvents: Int = 100,
    val maxProtocolSamples: Int = 500
)

class HistoryRetentionPreferences(context: Context) {
    private val preferences = AppPreferencesRepository.get(context)

    fun load() = HistoryRetentionSettings(
        retentionDays = preferences.int(NAMESPACE, "days", 30).coerceIn(1, 365),
        maxActivityEvents = preferences.int(NAMESPACE, "activity_limit", 100).coerceIn(25, 500),
        maxProtocolSamples = preferences.int(NAMESPACE, "protocol_limit", 500).coerceIn(100, 2_000)
    )

    fun save(settings: HistoryRetentionSettings) {
        preferences.putInt(NAMESPACE, "days", settings.retentionDays.coerceIn(1, 365))
        preferences.putInt(NAMESPACE, "activity_limit", settings.maxActivityEvents.coerceIn(25, 500))
        preferences.putInt(NAMESPACE, "protocol_limit", settings.maxProtocolSamples.coerceIn(100, 2_000))
    }

    private companion object { const val NAMESPACE = "history_retention" }
}
