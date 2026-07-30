package com.example.airpodscompanion

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
        assertEquals(80, AirPodsSignalParser.parseVendorBattery("+IPHONEACCEV", listOf("1", "1", "7")))
    }

    @Test fun rejectsUnknownBatteryEvent() {
        assertNull(AirPodsSignalParser.parseVendorBattery("+UNKNOWN", listOf("1", "1", "7")))
    }
}
