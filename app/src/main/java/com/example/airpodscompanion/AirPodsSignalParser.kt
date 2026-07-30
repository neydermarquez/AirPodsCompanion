package com.example.airpodscompanion

internal object AirPodsSignalParser {
    fun parseVendorBattery(command: String, args: List<String>): Int? = when (command.uppercase()) {
        "+IPHONEACCEV" -> {
            val values = args.mapNotNull { it.toIntOrNull() }
            if (values.size < 3) null else {
                val pairCount = values.first()
                val raw = values.drop(1).chunked(2).take(pairCount)
                    .firstOrNull { it.size == 2 && it[0] == 1 }?.get(1)
                raw?.let { ((it + 1) * 10).coerceIn(0, 100) }
            }
        }
        "+XEVENT" -> {
            if (args.firstOrNull()?.equals("BATTERY", ignoreCase = true) != true) null
            else {
                val level = args.getOrNull(1)?.toIntOrNull()
                val levels = args.getOrNull(2)?.toIntOrNull()
                if (level == null || levels == null || levels <= 1) null
                else (level * 100 / (levels - 1)).coerceIn(0, 100)
            }
        }
        else -> null
    }

    fun identify(name: String): Pair<String, IdentificationConfidence> {
        val normalized = name.lowercase()
        return when {
            "airpods pro 2" in normalized && "usb-c" in normalized -> "AirPods Pro 2 (USB-C)" to IdentificationConfidence.EXACT_VARIANT
            "airpods pro 2" in normalized && "lightning" in normalized -> "AirPods Pro 2 (Lightning)" to IdentificationConfidence.EXACT_VARIANT
            "airpods pro 3" in normalized -> "AirPods Pro 3" to IdentificationConfidence.GENERATION
            "airpods pro 2" in normalized -> "AirPods Pro 2" to IdentificationConfidence.GENERATION
            "airpods pro" in normalized -> "AirPods Pro" to IdentificationConfidence.FAMILY
            "airpods max" in normalized && "usb-c" in normalized -> "AirPods Max (USB-C)" to IdentificationConfidence.EXACT_VARIANT
            "airpods max" in normalized && "lightning" in normalized -> "AirPods Max (Lightning)" to IdentificationConfidence.EXACT_VARIANT
            "airpods max 2" in normalized -> "AirPods Max 2" to IdentificationConfidence.GENERATION
            "airpods max" in normalized -> "AirPods Max" to IdentificationConfidence.FAMILY
            "airpods 4" in normalized && "anc" in normalized -> "AirPods 4 con ANC" to IdentificationConfidence.EXACT_VARIANT
            "airpods 4" in normalized -> "AirPods 4" to IdentificationConfidence.GENERATION
            "airpods 3" in normalized -> "AirPods (3.ª generación)" to IdentificationConfidence.GENERATION
            "airpods 2" in normalized -> "AirPods (2.ª generación)" to IdentificationConfidence.GENERATION
            "airpods" in normalized -> "AirPods" to IdentificationConfidence.FAMILY
            else -> "Modelo no determinado" to IdentificationConfidence.UNKNOWN
        }
    }
}

