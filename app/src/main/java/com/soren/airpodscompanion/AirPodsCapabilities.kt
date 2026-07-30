package com.soren.airpodscompanion

enum class CapabilityAccess(val label: String) {
    CONTROLLABLE("Controlable"),
    DETECTABLE("Detectable"),
    PHYSICAL_CONTROL("Control físico"),
    SYSTEM_MANAGED("Gestionado por Android"),
    NOT_SUPPORTED("No disponible en Android"),
    NEEDS_EVIDENCE("Pendiente de evidencia")
}

enum class AirPodsFeature(val title: String) {
    MEDIA_CONTROLS("Reproducción y volumen"),
    CALL_AUDIO("Audio de llamadas"),
    MICROPHONE("Micrófono"),
    MICROPHONE_MUTE("Silenciar micrófono"),
    AUDIO_CODEC("Códec Bluetooth"),
    LOCK_SCREEN_CONTROLS("Controles en pantalla bloqueada"),
    LATENCY_DIAGNOSTIC("Diagnóstico de latencia"),
    LOW_POWER_MODE("Modo de bajo consumo"),
    DEVICE_NAME("Nombre del dispositivo"),
    ANC("Cancelación de ruido"),
    TRANSPARENCY("Transparencia"),
    ADAPTIVE_AUDIO("Audio adaptativo"),
    CONVERSATION_AWARENESS("Conciencia de conversación"),
    PERSONALIZED_VOLUME("Volumen personalizado"),
    ADAPTIVE_EQ("Ecualización adaptativa"),
    LOUD_SOUND_REDUCTION("Reducción de sonidos fuertes"),
    EAR_DETECTION("Detección de oído"),
    AUTO_PAUSE("Pausa automática"),
    DOUBLE_TAP("Doble toque"),
    PRESS_CONTROLS("Gestos y controles"),
    DIGITAL_CROWN("Digital Crown"),
    LISTENING_MODE_BUTTON("Botón de modo de escucha"),
    CUSTOM_ACTIONS("Personalización de acciones"),
    SPATIAL_AUDIO("Audio espacial"),
    HEAD_TRACKING("Seguimiento de cabeza"),
    PERSONALIZED_SPATIAL("Audio espacial personalizado"),
    HEAD_GESTURES("Gestos de cabeza"),
    CALL_HEAD_GESTURES("Aceptar o rechazar llamadas con gestos"),
    NOTIFICATION_GESTURES("Responder notificaciones con gestos"),
    ANNOUNCEMENTS("Anuncios de llamadas y notificaciones"),
    SIRI("Siri"),
    ICLOUD("Integración con iCloud"),
    APPLE_AUTO_SWITCH("Cambio entre dispositivos Apple"),
    FIND_MY("Red Buscar"),
    FIRMWARE_UPDATE("Actualización de firmware"),
    HEARING_HEALTH("Funciones de salud auditiva"),
    HEARING_TEST("Prueba de audición"),
    HEARING_AID("Función de audífono"),
    HEARING_PROTECTION("Protección auditiva avanzada"),
    APPLE_INTELLIGENCE("Funciones de Apple Intelligence"),
    LIVE_TRANSLATION("Traducción en vivo")
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
        val generation1 = "1.ª" in normalized
        val supportsNoiseModes = pro || max || airPods4Anc
        val supportsAdaptive = "pro 2" in normalized || "pro 3" in normalized || airPods4Anc
        val pressureControls = pro || max || generation3 || generation4
        val supportsSpatial = pro || max || generation3 || generation4
        val uncertainModel = normalized == "airpods" || normalized == "modelo no determinado"
        fun modelAccess(
            supported: Boolean,
            supportedAccess: CapabilityAccess
        ) = when {
            uncertainModel -> CapabilityAccess.NEEDS_EVIDENCE
            supported -> supportedAccess
            else -> CapabilityAccess.NOT_SUPPORTED
        }
        return ModelCapabilities(
            model,
            mapOf(
                AirPodsFeature.MEDIA_CONTROLS to CapabilityAccess.CONTROLLABLE,
                AirPodsFeature.CALL_AUDIO to CapabilityAccess.SYSTEM_MANAGED,
                AirPodsFeature.MICROPHONE to CapabilityAccess.DETECTABLE,
                AirPodsFeature.MICROPHONE_MUTE to CapabilityAccess.CONTROLLABLE,
                AirPodsFeature.AUDIO_CODEC to CapabilityAccess.DETECTABLE,
                AirPodsFeature.LOCK_SCREEN_CONTROLS to CapabilityAccess.SYSTEM_MANAGED,
                AirPodsFeature.LATENCY_DIAGNOSTIC to CapabilityAccess.NEEDS_EVIDENCE,
                AirPodsFeature.LOW_POWER_MODE to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.DEVICE_NAME to CapabilityAccess.SYSTEM_MANAGED,
                AirPodsFeature.ANC to modelAccess(supportsNoiseModes, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.TRANSPARENCY to modelAccess(supportsNoiseModes, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.ADAPTIVE_AUDIO to modelAccess(supportsAdaptive, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.CONVERSATION_AWARENESS to modelAccess(supportsAdaptive, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.PERSONALIZED_VOLUME to modelAccess(supportsAdaptive, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.ADAPTIVE_EQ to modelAccess(pro || generation3 || generation4, CapabilityAccess.SYSTEM_MANAGED),
                AirPodsFeature.LOUD_SOUND_REDUCTION to modelAccess(
                    "pro 2" in normalized || "pro 3" in normalized,
                    CapabilityAccess.PHYSICAL_CONTROL
                ),
                AirPodsFeature.EAR_DETECTION to CapabilityAccess.NEEDS_EVIDENCE,
                AirPodsFeature.AUTO_PAUSE to CapabilityAccess.NEEDS_EVIDENCE,
                AirPodsFeature.DOUBLE_TAP to modelAccess(generation1 || generation2, CapabilityAccess.SYSTEM_MANAGED),
                AirPodsFeature.PRESS_CONTROLS to when {
                    max -> CapabilityAccess.PHYSICAL_CONTROL
                    pressureControls || generation2 || "airpods" in normalized -> CapabilityAccess.SYSTEM_MANAGED
                    else -> CapabilityAccess.NEEDS_EVIDENCE
                },
                AirPodsFeature.DIGITAL_CROWN to modelAccess(max, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.LISTENING_MODE_BUTTON to modelAccess(max, CapabilityAccess.PHYSICAL_CONTROL),
                AirPodsFeature.CUSTOM_ACTIONS to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.SPATIAL_AUDIO to modelAccess(supportsSpatial, CapabilityAccess.DETECTABLE),
                AirPodsFeature.HEAD_TRACKING to modelAccess(supportsSpatial, CapabilityAccess.DETECTABLE),
                AirPodsFeature.PERSONALIZED_SPATIAL to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.HEAD_GESTURES to modelAccess(
                    generation4 || "pro 2" in normalized || "pro 3" in normalized,
                    CapabilityAccess.SYSTEM_MANAGED
                ),
                AirPodsFeature.CALL_HEAD_GESTURES to modelAccess(
                    generation4 || "pro 2" in normalized || "pro 3" in normalized,
                    CapabilityAccess.SYSTEM_MANAGED
                ),
                AirPodsFeature.NOTIFICATION_GESTURES to modelAccess(
                    generation4 || "pro 2" in normalized || "pro 3" in normalized,
                    CapabilityAccess.SYSTEM_MANAGED
                ),
                AirPodsFeature.ANNOUNCEMENTS to CapabilityAccess.SYSTEM_MANAGED,
                AirPodsFeature.SIRI to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.ICLOUD to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.APPLE_AUTO_SWITCH to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.FIND_MY to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.FIRMWARE_UPDATE to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.HEARING_HEALTH to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.HEARING_TEST to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.HEARING_AID to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.HEARING_PROTECTION to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.APPLE_INTELLIGENCE to CapabilityAccess.NOT_SUPPORTED,
                AirPodsFeature.LIVE_TRANSLATION to CapabilityAccess.NOT_SUPPORTED
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
