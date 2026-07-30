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
            val firstPayloads = samples.payloads(first, source)
            val secondPayloads = samples.payloads(second, source)
            if (firstPayloads.isEmpty() || secondPayloads.isEmpty()) return@mapNotNull null
            val commonSize = (firstPayloads + secondPayloads).minOf(ByteArray::size)
            val differences = (0 until commonSize).mapNotNull { index ->
                val firstValues = firstPayloads.map { it[index].toInt() and 0xFF }.distinct()
                val secondValues = secondPayloads.map { it[index].toInt() and 0xFF }.distinct()
                if (firstValues.size == 1 && secondValues.size == 1 && firstValues.single() != secondValues.single()) {
                    StableByte(index, firstValues.single(), secondValues.single())
                } else null
            }
            val repetitions = minOf(firstPayloads.size, secondPayloads.size)
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

    private fun List<ProtocolSample>.payloads(scenario: CaptureScenario, source: String): List<ByteArray> =
        filter { it.scenario == scenario && it.source == source }
            .mapNotNull { it.payload.hexToBytesOrNull() }

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
