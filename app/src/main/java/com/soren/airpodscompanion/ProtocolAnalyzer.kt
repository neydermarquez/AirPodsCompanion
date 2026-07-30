package com.soren.airpodscompanion

data class StableByte(
    val index: Int,
    val firstValue: Int,
    val secondValue: Int
)

data class ScenarioComparison(
    val first: CaptureScenario,
    val second: CaptureScenario,
    val source: String,
    val samplesPerScenario: Int,
    val stableDifferences: List<StableByte>,
    val reproducible: Boolean
)

object ProtocolAnalyzer {
    const val MIN_REPETITIONS = 3

    fun compare(
        samples: List<ProtocolSample>,
        first: CaptureScenario,
        second: CaptureScenario
    ): List<ScenarioComparison> {
        val sources = samples.filter { it.scenario == first || it.scenario == second }
            .map(ProtocolSample::source)
            .distinct()
        return sources.mapNotNull { source ->
            val firstSessions = samples.payloadsBySession(first, source)
            val secondSessions = samples.payloadsBySession(second, source)
            if (firstSessions.isEmpty() || secondSessions.isEmpty()) return@mapNotNull null
            val allPayloads = (firstSessions.values + secondSessions.values).flatten()
            val commonSize = allPayloads.minOf(ByteArray::size)
            val differences = (0 until commonSize).mapNotNull { index ->
                val firstValues = firstSessions.values
                    .flatten()
                    .map { it[index].toInt() and 0xFF }
                    .distinct()
                val secondValues = secondSessions.values
                    .flatten()
                    .map { it[index].toInt() and 0xFF }
                    .distinct()
                if (firstValues.size == 1 && secondValues.size == 1 && firstValues.single() != secondValues.single()) {
                    StableByte(index, firstValues.single(), secondValues.single())
                } else null
            }
            val repetitions = minOf(firstSessions.size, secondSessions.size)
            ScenarioComparison(
                first,
                second,
                source,
                repetitions,
                differences,
                repetitions >= MIN_REPETITIONS && differences.isNotEmpty()
            )
        }
    }

    fun reproducibleComparisons(samples: List<ProtocolSample>): List<ScenarioComparison> =
        CaptureScenario.entries.flatMapIndexed { index, first ->
            CaptureScenario.entries.drop(index + 1).flatMap { second -> compare(samples, first, second) }
        }.filter(ScenarioComparison::reproducible)

    private fun List<ProtocolSample>.payloadsBySession(
        scenario: CaptureScenario,
        source: String
    ): Map<String, List<ByteArray>> =
        filter { it.scenario == scenario && it.source == source }
            .groupBy(ProtocolSample::sessionId)
            .mapValues { (_, samples) -> samples.mapNotNull { it.payload.hexToBytesOrNull() } }
            .filterValues { it.isNotEmpty() }

    private fun String.hexToBytesOrNull(): ByteArray? {
        val normalized = filterNot(Char::isWhitespace)
        if (normalized.length < 2 || normalized.length % 2 != 0 || normalized.any { it.digitToIntOrNull(16) == null }) {
            return null
        }
        return ByteArray(normalized.length / 2) { index ->
            normalized.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
