package com.soren.airpodscompanion

import android.content.Context
import android.util.Base64
import com.soren.airpodscompanion.data.local.DatabaseProvider
import com.soren.airpodscompanion.data.local.ProtocolSampleEntity
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

enum class CaptureScenario(val title: String) {
    ANC("Cancelación de ruido"),
    TRANSPARENCY("Transparencia"),
    ADAPTIVE("Audio adaptativo"),
    LEFT_EAR("Auricular izquierdo"),
    RIGHT_EAR("Auricular derecho"),
    CASE_OPEN("Estuche abierto"),
    CHARGING("Carga"),
    GESTURE("Gesto o control físico")
}

data class ProtocolSample(
    val timestamp: Long,
    val model: String,
    val scenario: CaptureScenario,
    val source: String,
    val payload: String,
    val rssi: Int?,
    val sessionId: String = "legacy"
)

data class ProtocolSession(
    val id: String,
    val scenario: CaptureScenario,
    val model: String,
    val startedAt: Long,
    val endedAt: Long,
    val sampleCount: Int,
    val sources: List<String>
)

data class ProtocolCaptureState(
    val activeScenario: CaptureScenario? = null,
    val sampleCount: Int = 0,
    val lastCompletedScenario: CaptureScenario? = null,
    val scenariosWithEvidence: Int = 0,
    val reproducibleComparisons: Int = 0,
    val sessions: List<ProtocolSession> = emptyList()
)

/**
 * Local diagnostic evidence. Device addresses and user identifiers are never captured.
 */
class ProtocolCaptureStore(private val context: Context) {
    private val preferences = AppPreferencesRepository.get(context)
    private val dao = DatabaseProvider.get(context).dao()
    private val legacy = context.getSharedPreferences(NAMESPACE, Context.MODE_PRIVATE)

    init { migrateLegacySamples() }

    fun state(): ProtocolCaptureState {
        val active = preferences.string(NAMESPACE, KEY_ACTIVE, null)?.let { runCatching { CaptureScenario.valueOf(it) }.getOrNull() }
        val completed = preferences.string(NAMESPACE, KEY_COMPLETED, null)?.let { runCatching { CaptureScenario.valueOf(it) }.getOrNull() }
        val samples = load()
        return ProtocolCaptureState(
            active,
            samples.size,
            completed,
            samples.groupingBy(ProtocolSample::scenario).eachCount().count { it.value >= ProtocolAnalyzer.MIN_REPETITIONS },
            ProtocolAnalyzer.reproducibleComparisons(samples).size,
            sessions(samples)
        )
    }

    fun start(scenario: CaptureScenario) {
        preferences.putString(NAMESPACE, KEY_ACTIVE, scenario.name)
        preferences.putString(NAMESPACE, KEY_ACTIVE_SESSION, "${System.currentTimeMillis()}-${scenario.name}")
    }

    fun stop() {
        val active = state().activeScenario
        preferences.putString(NAMESPACE, KEY_ACTIVE, null)
        preferences.putString(NAMESPACE, KEY_ACTIVE_SESSION, null)
        if (active != null) preferences.putString(NAMESPACE, KEY_COMPLETED, active.name)
    }

    fun append(model: String, source: String, payload: String, rssi: Int? = null) {
        val scenario = state().activeScenario ?: return
        val sessionId = preferences.string(NAMESPACE, KEY_ACTIVE_SESSION, null) ?: return
        val sample = ProtocolSample(System.currentTimeMillis(), model, scenario, source, payload, rssi, sessionId)
        val settings = HistoryRetentionPreferences(context).load()
        io { dao.insertProtocol(listOf(sample.toEntity())) }
        applyRetention()
    }

    fun load(): List<ProtocolSample> =
        HistoryRetentionPreferences(context).load().let { settings ->
            io { dao.protocol(cutoff(settings.retentionDays), settings.maxProtocolSamples) }
                .mapNotNull(::fromEntity)
        }

    fun deleteSession(sessionId: String) {
        io { dao.deleteSession(sessionId) }
    }

    fun applyRetention(): List<ProtocolSample> {
        val settings = HistoryRetentionPreferences(context).load()
        io {
            dao.deleteOldProtocol(cutoff(settings.retentionDays))
            dao.trimProtocol(settings.maxProtocolSamples)
        }
        return load()
    }

    fun exportAnonymous(sessionId: String): String {
        val samples = load().filter { it.sessionId == sessionId }
        val session = sessions(samples).firstOrNull()
        return buildString {
            append("{\n")
            append("  \"format\": \"airpods-companion-protocol-v1\",\n")
            append("  \"scenario\": \"${session?.scenario?.name.orEmpty()}\",\n")
            append("  \"model\": \"${json(session?.model.orEmpty())}\",\n")
            append("  \"samples\": [\n")
            samples.forEachIndexed { index, sample ->
                append("    {\"timestamp\":${sample.timestamp},\"source\":\"${json(sample.source)}\",")
                append("\"payload\":\"${json(sample.payload)}\",\"rssi\":${sample.rssi ?: "null"}}")
                if (index < samples.lastIndex) append(",")
                append("\n")
            }
            append("  ]\n}")
        }
    }

    fun comparisonsForSession(sessionId: String): List<ScenarioComparison> {
        val target = load().firstOrNull { it.sessionId == sessionId } ?: return emptyList()
        return CaptureScenario.entries.filter { it != target.scenario }
            .flatMap { ProtocolAnalyzer.compare(load(), target.scenario, it) }
    }

    private fun sessions(samples: List<ProtocolSample>): List<ProtocolSession> =
        samples.groupBy(ProtocolSample::sessionId).mapNotNull { (id, grouped) ->
            val first = grouped.minByOrNull(ProtocolSample::timestamp) ?: return@mapNotNull null
            ProtocolSession(
                id,
                first.scenario,
                first.model,
                grouped.minOf(ProtocolSample::timestamp),
                grouped.maxOf(ProtocolSample::timestamp),
                grouped.size,
                grouped.map(ProtocolSample::source).distinct()
            )
        }.sortedByDescending(ProtocolSession::startedAt)

    private fun encode(sample: ProtocolSample) = listOf(
        encodeText(sample.sessionId),
        sample.timestamp,
        sample.scenario.name,
        encodeText(sample.model),
        encodeText(sample.source),
        encodeText(sample.payload),
        sample.rssi ?: ""
    ).joinToString("|")

    private fun ProtocolSample.toEntity() = ProtocolSampleEntity(
        "$sessionId-$timestamp-${source.hashCode()}-${payload.hashCode()}",
        sessionId,
        timestamp,
        model,
        scenario.name,
        source,
        payload,
        rssi
    )

    private fun fromEntity(entity: ProtocolSampleEntity): ProtocolSample? = runCatching {
        ProtocolSample(
            entity.timestamp,
            entity.model,
            CaptureScenario.valueOf(entity.scenario),
            entity.source,
            entity.payload,
            entity.rssi,
            entity.sessionId
        )
    }.getOrNull()

    private fun migrateLegacySamples() {
        if (legacy.getBoolean(KEY_ROOM_MIGRATED, false)) return
        val samples = legacy.getStringSet(KEY_SAMPLES, emptySet()).orEmpty().mapNotNull(::decode)
        if (samples.isNotEmpty()) io { dao.insertProtocol(samples.map { it.toEntity() }) }
        legacy.edit().remove(KEY_SAMPLES).putBoolean(KEY_ROOM_MIGRATED, true).apply()
    }

    private fun <T> io(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private fun decode(value: String): ProtocolSample? {
        val parts = value.split("|")
        if (parts.size !in 6..7) return null
        return runCatching {
            val offset = if (parts.size == 7) 1 else 0
            ProtocolSample(
                timestamp = parts[offset].toLong(),
                model = decodeText(parts[offset + 2]),
                scenario = CaptureScenario.valueOf(parts[offset + 1]),
                source = decodeText(parts[offset + 3]),
                payload = decodeText(parts[offset + 4]),
                rssi = parts[offset + 5].toIntOrNull(),
                sessionId = if (offset == 1) decodeText(parts[0]) else "legacy-${parts[1]}"
            )
        }.getOrNull()
    }

    private fun cutoff(days: Int) = System.currentTimeMillis() - days * 24L * 60L * 60L * 1_000L

    private fun json(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun encodeText(value: String) =
        Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decodeText(value: String) =
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE).toString(Charsets.UTF_8)

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_ACTIVE_SESSION = "active_session"
        const val KEY_COMPLETED = "completed"
        const val KEY_SAMPLES = "samples"
        const val KEY_ROOM_MIGRATED = "room_migrated"
        const val NAMESPACE = "protocol_capture"
    }
}
