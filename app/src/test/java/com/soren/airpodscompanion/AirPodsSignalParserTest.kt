package com.soren.airpodscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirPodsSignalParserTest {
    @Test fun identifiesExplicitVariant() {
        assertEquals(
            "AirPods Pro 2 (USB-C)" to IdentificationConfidence.EXACT_VARIANT,
            AirPodsSignalParser.identify("Soren AirPods Pro 2 USB-C")
        )
    }

    @Test fun doesNotInventGeneration() {
        assertEquals(
            "AirPods Pro" to IdentificationConfidence.FAMILY,
            AirPodsSignalParser.identify("AirPods Pro de Soren")
        )
    }

    @Test fun parsesAppleAccessoryBattery() {
        val signal = AirPodsSignalParser.parseVendorBattery("+IPHONEACCEV", listOf("1", "1", "7"))
        assertEquals(80, signal?.battery?.combined?.percent)
        assertEquals(BatterySignalSource.APPLE_ACCESSORY, signal?.source)
    }

    @Test fun parsesBatteryWhenAppleEventContainsAdditionalPairs() {
        val signal = AirPodsSignalParser.parseVendorBattery(
            "+IPHONEACCEV",
            listOf("3", "2", "1", "1", "4", "3", "0")
        )
        assertEquals(50, signal?.battery?.combined?.percent)
    }

    @Test fun parsesStandardHeadsetBattery() {
        val signal = AirPodsSignalParser.parseVendorBattery("+XEVENT", listOf("BATTERY", "3", "5"))
        assertEquals(75, signal?.battery?.combined?.percent)
        assertEquals(BatterySignalSource.ANDROID_XEVENT, signal?.source)
    }

    @Test fun rejectsOutOfRangeBatteryValues() {
        assertNull(AirPodsSignalParser.parseVendorBattery("+IPHONEACCEV", listOf("1", "1", "12")))
        assertNull(AirPodsSignalParser.parseVendorBattery("+XEVENT", listOf("BATTERY", "5", "5")))
    }

    @Test fun rejectsUnknownBatteryEvent() {
        assertNull(AirPodsSignalParser.parseVendorBattery("+UNKNOWN", listOf("1", "1", "7")))
    }

    @Test fun distinguishesFreshAndExpiredBatteryReadings() {
        val now = 1_000_000L
        assertEquals(true, ComponentBattery(80, observedAt = now).isFresh(now))
        assertEquals(
            false,
            ComponentBattery(80, observedAt = now - ComponentBattery.BATTERY_FRESHNESS_MS - 1).isFresh(now)
        )
    }
}
