package com.soren.airpodscompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsCapabilitiesTest {
    @Test fun pro2UsesPhysicalNoiseControlsAndDetectableSpatialAudio() {
        val capabilities = AirPodsCapabilityRegistry.forModel("AirPods Pro 2 (USB-C)")
        assertEquals(CapabilityAccess.PHYSICAL_CONTROL, capabilities.access(AirPodsFeature.ANC))
        assertEquals(CapabilityAccess.PHYSICAL_CONTROL, capabilities.access(AirPodsFeature.ADAPTIVE_AUDIO))
        assertEquals(CapabilityAccess.DETECTABLE, capabilities.access(AirPodsFeature.SPATIAL_AUDIO))
    }

    @Test fun secondGenerationDoesNotAdvertiseAnc() {
        val capabilities = AirPodsCapabilityRegistry.forModel("AirPods (2.ª generación)")
        assertEquals(CapabilityAccess.NOT_SUPPORTED, capabilities.access(AirPodsFeature.ANC))
        assertEquals(CapabilityAccess.NOT_SUPPORTED, capabilities.access(AirPodsFeature.SPATIAL_AUDIO))
    }

    @Test fun everyPublishedFeatureHasAnExplicitState() {
        val capabilities = AirPodsCapabilityRegistry.forModel("AirPods Pro 2 (USB-C)")
        AirPodsFeature.entries.forEach { feature ->
            assertEquals(true, capabilities.features.containsKey(feature))
        }
    }

    @Test fun genericAirPodsNameDoesNotClaimMissingModelFeatures() {
        val capabilities = AirPodsCapabilityRegistry.forModel("AirPods")
        assertEquals(CapabilityAccess.NEEDS_EVIDENCE, capabilities.access(AirPodsFeature.ANC))
        assertEquals(CapabilityAccess.NEEDS_EVIDENCE, capabilities.access(AirPodsFeature.SPATIAL_AUDIO))
    }

    @Test fun appleOnlyServicesAreNeverAdvertisedAsAndroidFeatures() {
        val capabilities = AirPodsCapabilityRegistry.forModel("AirPods Pro 2 (USB-C)")
        assertEquals(CapabilityAccess.NOT_SUPPORTED, capabilities.access(AirPodsFeature.ICLOUD))
        assertEquals(CapabilityAccess.NOT_SUPPORTED, capabilities.access(AirPodsFeature.FIND_MY))
        assertEquals(CapabilityAccess.NOT_SUPPORTED, capabilities.access(AirPodsFeature.FIRMWARE_UPDATE))
    }
}
