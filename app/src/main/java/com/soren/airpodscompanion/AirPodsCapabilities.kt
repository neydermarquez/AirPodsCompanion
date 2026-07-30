package com.soren.airpodscompanion

enum class CapabilityAccess(val label: String) {
    CONTROLLABLE("Controlable"),
    DETECTABLE("Detectable"),
    PHYSICAL_CONTROL("Control físico"),
    SYSTEM_MANAGED("Gestionado por Android"),
    NOT_SUPPORTED("No compatible"),
    NEEDS_EVIDENCE("Pendiente de evidencia")
}

enum class AirPodsFeature(val title: String) {
    ANC("Cancelación de ruido"),
    TRANSPARENCY("Transparencia"),
    ADAPTIVE_AUDIO("Audio adaptativo"),
    EAR_DETECTION("Detección de oído"),
    AUTO_PAUSE("Pausa automática"),
    PRESS_CONTROLS("Gestos y controles"),
    SPATIAL_AUDIO("Audio espacial")
}

data class ModelCapabilities(
    val model: String,
    val features: Map<AirPodsFeature, CapabilityAccess>
) {
    fun access(feature: AirPodsFeature) = features[feature] ?: CapabilityAccess.NEEDS_EVIDENCE
}

object AirPodsCapabilityRegistry {
    fun forModel(model: String): ModelCapabilities {
        val normalized = model.lowercase()
        val pro = "pro" in normalized
        val max = "max" in normalized
        val airPods4Anc = "airpods 4 con anc" in normalized
        val generation4 = "airpods 4" in normalized
        val generation3 = "3.ª" in normalized
        val generation2 = "2.ª" in normalized
        val supportsNoiseModes = pro || max || airPods4Anc
        val supportsAdaptive = "pro 2" in normalized || "pro 3" in normalized || airPods4Anc
        val pressureControls = pro || max || generation3 || generation4
        return ModelCapabilities(
            model,
            mapOf(
                AirPodsFeature.ANC to if (supportsNoiseModes) CapabilityAccess.PHYSICAL_CONTROL else CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.TRANSPARENCY to if (supportsNoiseModes) CapabilityAccess.PHYSICAL_CONTROL else CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.ADAPTIVE_AUDIO to if (supportsAdaptive) CapabilityAccess.PHYSICAL_CONTROL else CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.EAR_DETECTION to if (model == "Modelo no determinado") {
                    CapabilityAccess.NEEDS_EVIDENCE
                } else CapabilityAccess.NEEDS_EVIDENCE,
                AirPodsFeature.AUTO_PAUSE to CapabilityAccess.NEEDS_EVIDENCE,
                AirPodsFeature.PRESS_CONTROLS to when {
                    max -> CapabilityAccess.PHYSICAL_CONTROL
                    pressureControls || generation2 || "airpods" in normalized -> CapabilityAccess.SYSTEM_MANAGED
                    else -> CapabilityAccess.NEEDS_EVIDENCE
                },
                AirPodsFeature.SPATIAL_AUDIO to if (
                    pro || max || generation3 || generation4
                ) CapabilityAccess.DETECTABLE else CapabilityAccess.NOT_SUPPORTED
            )
        )
    }
}

class AirPodsCapabilityEngine(private val captureStore: ProtocolCaptureStore) {
    fun forModel(model: String): ModelCapabilities {
        val base = AirPodsCapabilityRegistry.forModel(model)
        val reproducible = ProtocolAnalyzer.reproducibleComparisons(
            captureStore.load().filter { it.model == model }
        )
        if (reproducible.isEmpty()) return base
        val detectedScenarios = reproducible.flatMap { listOf(it.first, it.second) }.toSet()
        val updates = buildMap {
            if (CaptureScenario.ANC in detectedScenarios) put(AirPodsFeature.ANC, CapabilityAccess.DETECTABLE)
            if (CaptureScenario.TRANSPARENCY in detectedScenarios) put(AirPodsFeature.TRANSPARENCY, CapabilityAccess.DETECTABLE)
            if (CaptureScenario.ADAPTIVE in detectedScenarios) put(AirPodsFeature.ADAPTIVE_AUDIO, CapabilityAccess.DETECTABLE)
            if (CaptureScenario.LEFT_EAR in detectedScenarios || CaptureScenario.RIGHT_EAR in detectedScenarios) {
                put(AirPodsFeature.EAR_DETECTION, CapabilityAccess.DETECTABLE)
            }
            if (CaptureScenario.GESTURE in detectedScenarios) put(AirPodsFeature.PRESS_CONTROLS, CapabilityAccess.DETECTABLE)
        }
        return base.copy(features = base.features + updates)
    }
}
