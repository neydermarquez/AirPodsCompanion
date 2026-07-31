package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.local.DatabaseProvider
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PrivacyRepository(private val context: Context) {
    fun exportAll(): String {
        val preferences = AppPreferencesRepository.get(context).snapshot()
            .filterKeys { !it.startsWith("_meta.") }
        val activity = ConnectionHistoryStore(context).load()
        val protocol = ProtocolCaptureStore(context).load()
        val crashes = CrashReportStore(context).reports()
        val metrics = LocalMetricStore(context).snapshot()
        val aliases = DeviceAliasStore(context).aliases()
        return buildString {
            append("{\n  \"format\":\"airpods-companion-user-data-v1\",\n")
            append("  \"exportedAt\":${System.currentTimeMillis()},\n")
            append("  \"preferences\":{")
            preferences.entries.forEachIndexed { index, entry ->
                append("\"${json(entry.key)}\":${jsonValue(entry.value)}")
                if (index < preferences.size - 1) append(",")
            }
            append("},\n  \"activity\":[\n")
            activity.forEachIndexed { index, event ->
                append("    {\"timestamp\":${event.timestamp},\"type\":\"${event.type.name}\",")
                append("\"device\":\"${json(event.deviceName)}\",\"detail\":\"${json(event.detail)}\"}")
                if (index < activity.lastIndex) append(",")
                append("\n")
            }
            append("  ],\n  \"protocolEvidence\":[\n")
            protocol.forEachIndexed { index, sample ->
                append("    {\"session\":\"${json(sample.sessionId)}\",\"timestamp\":${sample.timestamp},")
                append("\"model\":\"${json(sample.model)}\",\"scenario\":\"${sample.scenario.name}\",")
                append("\"source\":\"${json(sample.source)}\",\"payload\":\"${json(sample.payload)}\",")
                append("\"rssi\":${sample.rssi ?: "null"}}")
                if (index < protocol.lastIndex) append(",")
                append("\n")
            }
            append("  ],\n  \"localMetrics\":{")
            metrics.entries.forEachIndexed { index, entry ->
                append("\"${json(entry.key)}\":${entry.value}")
                if (index < metrics.size - 1) append(",")
            }
            append("},\n  \"localCrashReports\":[\n")
            crashes.forEachIndexed { index, report ->
                append(report.prependIndent("    "))
                if (index < crashes.lastIndex) append(",")
                append("\n")
            }
            append("  ],\n  \"deviceAliases\":[")
            aliases.forEachIndexed { index, alias ->
                append("\"${json(alias)}\"")
                if (index < aliases.lastIndex) append(",")
            }
            append("]\n}")
        }
    }

    fun deleteAll() {
        runBlocking(Dispatchers.IO) {
            DatabaseProvider.get(context).dao().apply {
                clearActivity()
                clearProtocol()
            }
        }
        AppPreferencesRepository.get(context).clearAll()
        CrashReportStore(context).clear()
        LocalMetricStore(context).clear()
        LEGACY_STORES.forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        AirPodsWidget.updateAll(context)
    }

    private fun json(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n")

    private fun jsonValue(value: Any): String = when (value) {
        is Number, is Boolean -> value.toString()
        else -> "\"${json(value.toString())}\""
    }

    private companion object {
        val LEGACY_STORES = listOf(
            "background_preferences", "notification_preferences", "listening_profile",
            "history_retention", "widget_preferences", "protocol_capture",
            "device_snapshot", "battery_states", "battery_evidence", "monitor_status",
            "device_aliases"
        )
    }
}
