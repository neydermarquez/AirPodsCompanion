package com.soren.airpodscompanion

import android.content.Context
import android.util.Base64
import com.soren.airpodscompanion.data.local.ActivityEventEntity
import com.soren.airpodscompanion.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

enum class ConnectionEventType { CONNECTED, DISCONNECTED, PAIRED, BATTERY, AUDIO, MONITOR, ERROR }

data class ConnectionEvent(
    val timestamp: Long,
    val type: ConnectionEventType,
    val deviceName: String,
    val detail: String
)

class ConnectionHistoryStore(private val context: Context) {
    private val dao = DatabaseProvider.get(context).dao()
    private val legacy = context.getSharedPreferences("connection_history", Context.MODE_PRIVATE)

    init {
        migrateLegacy()
    }

    fun load(): List<ConnectionEvent> = io {
        val settings = HistoryRetentionPreferences(context).load()
        dao.activity(cutoff(settings.retentionDays), settings.maxActivityEvents).mapNotNull { entity ->
            runCatching {
                ConnectionEvent(
                    entity.timestamp,
                    ConnectionEventType.valueOf(entity.type),
                    entity.deviceName,
                    entity.detail
                )
            }.getOrNull()
        }
    }

    fun append(event: ConnectionEvent): List<ConnectionEvent> {
        val current = load()
        val duplicate = current.firstOrNull()?.let {
            it.type == event.type && it.deviceName == event.deviceName &&
                event.timestamp - it.timestamp in 0..DEDUPLICATION_WINDOW_MS
        } == true
        if (duplicate) return current
        io { dao.insertActivity(listOf(event.toEntity())) }
        applyRetention()
        return load()
    }

    fun clear() = io { dao.clearActivity() }

    fun applyRetention(): List<ConnectionEvent> {
        val settings = HistoryRetentionPreferences(context).load()
        io {
            dao.deleteOldActivity(cutoff(settings.retentionDays))
            dao.trimActivity(settings.maxActivityEvents)
        }
        return load()
    }

    private fun migrateLegacy() {
        if (legacy.getBoolean(KEY_MIGRATED, false)) return
        val events = legacy.getStringSet(KEY_EVENTS, emptySet()).orEmpty().mapNotNull(::decode)
        if (events.isNotEmpty()) io { dao.insertActivity(events.map { it.toEntity() }) }
        legacy.edit().remove(KEY_EVENTS).putBoolean(KEY_MIGRATED, true).apply()
    }

    private fun ConnectionEvent.toEntity() = ActivityEventEntity(
        "$timestamp-${type.name}-${deviceName.hashCode()}-${detail.hashCode()}",
        timestamp,
        type.name,
        deviceName,
        detail
    )

    private fun decode(value: String): ConnectionEvent? {
        val parts = value.split("|", limit = 4)
        if (parts.size != 4) return null
        return runCatching {
            ConnectionEvent(
                parts[0].toLong(),
                ConnectionEventType.valueOf(parts[1]),
                decodeText(parts[2]),
                decodeText(parts[3])
            )
        }.getOrNull()
    }

    private fun decodeText(value: String): String =
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE).toString(Charsets.UTF_8)

    private fun cutoff(days: Int) =
        System.currentTimeMillis() - days * 24L * 60L * 60L * 1_000L

    private fun <T> io(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private companion object {
        const val KEY_EVENTS = "events"
        const val KEY_MIGRATED = "room_migrated"
        const val DEDUPLICATION_WINDOW_MS = 3_000L
    }
}
