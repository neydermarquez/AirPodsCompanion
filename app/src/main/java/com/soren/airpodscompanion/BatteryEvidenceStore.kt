package com.soren.airpodscompanion

import android.content.Context
import android.util.Base64

data class BatteryEvidence(
    val model: String,
    val source: BatterySignalSource,
    val hasLeft: Boolean,
    val hasRight: Boolean,
    val hasCase: Boolean,
    val hasCombined: Boolean,
    val lastObservedAt: Long
) {
    val hasIndividualComponents: Boolean get() = hasLeft || hasRight || hasCase
}

/**
 * Stores only compatibility evidence. Bluetooth addresses and raw vendor payloads are deliberately
 * excluded so diagnostics remain local, minimal and safe to use with real devices.
 */
class BatteryEvidenceStore(context: Context) {
    private val preferences = context.getSharedPreferences("battery_evidence", Context.MODE_PRIVATE)

    fun find(model: String): BatteryEvidence? =
        preferences.getString(key(model), null)?.let(::decode)

    fun record(model: String, signal: ParsedBatterySignal): BatteryEvidence {
        val previous = find(model)
        val battery = signal.battery
        val updated = BatteryEvidence(
            model = model,
            source = signal.source,
            hasLeft = previous?.hasLeft == true || battery.left.percent != null,
            hasRight = previous?.hasRight == true || battery.right.percent != null,
            hasCase = previous?.hasCase == true || battery.case.percent != null,
            hasCombined = previous?.hasCombined == true || battery.combined.percent != null,
            lastObservedAt = System.currentTimeMillis()
        )
        preferences.edit().putString(key(model), encode(updated)).apply()
        return updated
    }

    private fun key(model: String): String =
        Base64.encodeToString(model.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun encode(value: BatteryEvidence): String = listOf(
        value.source.name,
        value.hasLeft,
        value.hasRight,
        value.hasCase,
        value.hasCombined,
        value.lastObservedAt
    ).joinToString("|")

    private fun decode(value: String): BatteryEvidence? {
        val parts = value.split("|")
        if (parts.size != 6) return null
        return runCatching {
            BatteryEvidence(
                model = "",
                source = BatterySignalSource.valueOf(parts[0]),
                hasLeft = parts[1].toBooleanStrict(),
                hasRight = parts[2].toBooleanStrict(),
                hasCase = parts[3].toBooleanStrict(),
                hasCombined = parts[4].toBooleanStrict(),
                lastObservedAt = parts[5].toLong()
            )
        }.getOrNull()
    }
}
