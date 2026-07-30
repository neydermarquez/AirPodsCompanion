package com.soren.airpodscompanion.navigation

enum class AppDestination(val route: String, val label: String) {
    HOME("home", "Inicio"),
    DEVICES("devices", "Dispositivos"),
    ACTIVITY("activity", "Actividad"),
    SETTINGS("settings", "Ajustes");

    companion object {
        fun fromIndex(index: Int) = entries.getOrElse(index) { HOME }
        fun indexOf(route: String?) = entries.indexOfFirst { it.route == route }.coerceAtLeast(0)
    }
}
