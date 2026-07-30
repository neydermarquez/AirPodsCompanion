package com.soren.airpodscompanion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothConnectionPolicyTest {
    @Test
    fun `recognizes a device that publishes an AirPods name`() {
        assertTrue(
            BluetoothConnectionPolicy.isRecognizedAirPods(
                publishedName = "AirPods Pro",
                address = "AA:BB",
                knownAirPodsAddress = null
            )
        )
    }

    @Test
    fun `keeps recognizing previously known AirPods after they are renamed`() {
        assertTrue(
            BluetoothConnectionPolicy.isRecognizedAirPods(
                publishedName = "Audífonos de Soren",
                address = "AA:BB",
                knownAirPodsAddress = "AA:BB"
            )
        )
    }

    @Test
    fun `rejects unrelated Bluetooth audio devices`() {
        assertFalse(
            BluetoothConnectionPolicy.isRecognizedAirPods(
                publishedName = "Altavoz",
                address = "CC:DD",
                knownAirPodsAddress = "AA:BB"
            )
        )
    }

    @Test
    fun `does not report a battery change when only observation time changes`() {
        val previous = AirPodsBatteryState(
            combined = ComponentBattery(percent = 80, observedAt = 1L)
        )
        val current = AirPodsBatteryState(
            combined = ComponentBattery(percent = 80, observedAt = 2L)
        )

        assertFalse(BluetoothConnectionPolicy.batteryReadingsChanged(previous, current))
    }

    @Test
    fun `reports battery percentage and charging changes immediately`() {
        val previous = AirPodsBatteryState(
            left = ComponentBattery(percent = 80, charging = false, observedAt = 1L)
        )
        val current = AirPodsBatteryState(
            left = ComponentBattery(percent = 75, charging = true, observedAt = 2L)
        )

        assertTrue(BluetoothConnectionPolicy.batteryReadingsChanged(previous, current))
    }

    @Test
    fun `combined updates preserve real component readings`() {
        val previous = AirPodsBatteryState(
            left = ComponentBattery(percent = 81, charging = false, observedAt = 10L),
            right = ComponentBattery(percent = 76, charging = false, observedAt = 10L),
            case = ComponentBattery(percent = 54, charging = true, observedAt = 10L)
        )
        val incoming = AirPodsBatteryState(
            combined = ComponentBattery(percent = 70, observedAt = 20L)
        )

        val merged = BluetoothConnectionPolicy.mergeBatteryReadings(previous, incoming)

        assertEquals(previous.left, merged.left)
        assertEquals(previous.right, merged.right)
        assertEquals(previous.case, merged.case)
        assertEquals(70, merged.combined.percent)
    }

    @Test
    fun `charging-only updates preserve the last real percentage`() {
        val previous = AirPodsBatteryState(
            case = ComponentBattery(percent = 54, charging = false, observedAt = 10L)
        )
        val incoming = AirPodsBatteryState(
            case = ComponentBattery(charging = true, observedAt = 20L)
        )

        val merged = BluetoothConnectionPolicy.mergeBatteryReadings(previous, incoming)

        assertEquals(54, merged.case.percent)
        assertEquals(true, merged.case.charging)
        assertEquals(20L, merged.case.observedAt)
    }

    @Test
    fun `low battery prefers the lowest fresh individual component`() {
        val battery = AirPodsBatteryState(
            left = ComponentBattery(percent = 18, observedAt = 1_000L),
            right = ComponentBattery(percent = 72, observedAt = 1_000L),
            case = ComponentBattery(percent = 35, observedAt = 1_000L),
            combined = ComponentBattery(percent = 60, observedAt = 1_000L)
        )

        assertEquals(
            "auricular izquierdo" to 18,
            BluetoothConnectionPolicy.lowestFreshBattery(battery, now = 2_000L)
        )
    }

    @Test
    fun `low battery ignores expired readings`() {
        val expiredAt = 1_000L
        val now = expiredAt + ComponentBattery.BATTERY_FRESHNESS_MS + 1L
        val battery = AirPodsBatteryState(
            left = ComponentBattery(percent = 5, observedAt = expiredAt),
            combined = ComponentBattery(percent = 48, observedAt = now)
        )

        assertEquals(
            "AirPods" to 48,
            BluetoothConnectionPolicy.lowestFreshBattery(battery, now)
        )
    }
}
