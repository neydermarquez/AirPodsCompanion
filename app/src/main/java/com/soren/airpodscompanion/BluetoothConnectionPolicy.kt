package com.soren.airpodscompanion

internal object BluetoothConnectionPolicy {
    fun isRecognizedAirPods(
        publishedName: String?,
        address: String,
        knownAirPodsAddress: String?
    ): Boolean =
        publishedName?.contains("airpods", ignoreCase = true) == true ||
            knownAirPodsAddress == address

    fun batteryReadingsChanged(
        previous: AirPodsBatteryState,
        current: AirPodsBatteryState
    ): Boolean =
        previous.left.percent != current.left.percent ||
            previous.left.charging != current.left.charging ||
            previous.right.percent != current.right.percent ||
            previous.right.charging != current.right.charging ||
            previous.case.percent != current.case.percent ||
            previous.case.charging != current.case.charging ||
            previous.combined.percent != current.combined.percent ||
            previous.combined.charging != current.combined.charging

    fun mergeBatteryReadings(
        previous: AirPodsBatteryState,
        incoming: AirPodsBatteryState
    ): AirPodsBatteryState = AirPodsBatteryState(
        left = mergeComponent(previous.left, incoming.left),
        right = mergeComponent(previous.right, incoming.right),
        case = mergeComponent(previous.case, incoming.case),
        combined = mergeComponent(previous.combined, incoming.combined)
    )

    fun lowestFreshBattery(
        battery: AirPodsBatteryState,
        now: Long = System.currentTimeMillis()
    ): Pair<String, Int>? {
        val components = listOf(
            "auricular izquierdo" to battery.left,
            "auricular derecho" to battery.right,
            "estuche" to battery.case
        ).mapNotNull { (label, reading) ->
            reading.percent?.takeIf { reading.isFresh(now) }?.let { label to it }
        }
        return components.minByOrNull { it.second }
            ?: battery.combined.percent
                ?.takeIf { battery.combined.isFresh(now) }
                ?.let { "AirPods" to it }
    }

    private fun mergeComponent(
        previous: ComponentBattery,
        incoming: ComponentBattery
    ): ComponentBattery =
        if (incoming.percent != null || incoming.charging != null) {
            ComponentBattery(
                percent = incoming.percent ?: previous.percent,
                charging = incoming.charging ?: previous.charging,
                observedAt = incoming.observedAt ?: previous.observedAt
            )
        } else {
            previous
        }
}
