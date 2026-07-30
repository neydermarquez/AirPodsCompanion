package com.soren.airpodscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolAnalyzerTest {
    @Test fun findsOnlyStableChangedBytesAfterThreeRepetitions() {
        val samples = buildList {
            repeat(3) { repetition ->
                add(sample(CaptureScenario.ANC, "0711${repetition.toString(16).padStart(2, '0')}", session = "anc-$repetition"))
                add(sample(CaptureScenario.TRANSPARENCY, "0722${(repetition + 8).toString(16).padStart(2, '0')}", session = "transparency-$repetition"))
            }
        }
        val result = ProtocolAnalyzer.compare(samples, CaptureScenario.ANC, CaptureScenario.TRANSPARENCY).single()
        assertTrue(result.reproducible)
        assertEquals(listOf(StableByte(1, 0x11, 0x22)), result.stableDifferences)
    }

    @Test fun rejectsDifferenceWithFewerThanThreeRepetitions() {
        val samples = listOf(
            sample(CaptureScenario.ANC, "0711"),
            sample(CaptureScenario.TRANSPARENCY, "0722")
        )
        assertFalse(ProtocolAnalyzer.compare(samples, CaptureScenario.ANC, CaptureScenario.TRANSPARENCY).single().reproducible)
    }

    @Test fun repeatedPacketsFromOneCaptureDoNotCountAsIndependentRepetitions() {
        val samples = buildList {
            repeat(8) {
                add(sample(CaptureScenario.ANC, "0711", session = "anc-one-session"))
                add(sample(CaptureScenario.TRANSPARENCY, "0722", session = "transparency-one-session"))
            }
        }

        val result = ProtocolAnalyzer.compare(
            samples,
            CaptureScenario.ANC,
            CaptureScenario.TRANSPARENCY
        ).single()

        assertEquals(1, result.samplesPerScenario)
        assertFalse(result.reproducible)
    }

    @Test fun changingBytesInsideOneSessionAreNotReportedAsStable() {
        val samples = buildList {
            repeat(3) { repetition ->
                add(sample(CaptureScenario.ANC, "0711", session = "anc-$repetition"))
                add(sample(CaptureScenario.ANC, "0712", session = "anc-$repetition"))
                add(sample(CaptureScenario.TRANSPARENCY, "0722", session = "transparency-$repetition"))
            }
        }

        val result = ProtocolAnalyzer.compare(
            samples,
            CaptureScenario.ANC,
            CaptureScenario.TRANSPARENCY
        ).single()

        assertTrue(result.stableDifferences.isEmpty())
        assertFalse(result.reproducible)
    }

    @Test fun ignoresTextualHfpPayloadsUntilAParserIsDefined() {
        val samples = listOf(
            sample(CaptureScenario.ANC, "BATTERY,4,5", "HFP +XEVENT"),
            sample(CaptureScenario.TRANSPARENCY, "BATTERY,3,5", "HFP +XEVENT")
        )
        assertTrue(ProtocolAnalyzer.compare(samples, CaptureScenario.ANC, CaptureScenario.TRANSPARENCY).isEmpty())
    }

    private fun sample(
        scenario: CaptureScenario,
        payload: String,
        source: String = "BLE Apple manufacturer data",
        session: String = "${scenario.name}-single"
    ) = ProtocolSample(1L, "AirPods Pro", scenario, source, payload, null, session)
}
