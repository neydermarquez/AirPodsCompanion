package com.soren.airpodscompanion

import android.appwidget.AppWidgetManager
import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

enum class WidgetPresentation { AUTOMATIC, COMPACT, DETAILED }

class WidgetPreferences(context: Context) {
    private val preferences = AppPreferencesRepository.get(context)

    fun load(widgetId: Int): WidgetPresentation =
        preferences.string(NAMESPACE, key(widgetId), null)
            ?.let { runCatching { WidgetPresentation.valueOf(it) }.getOrNull() }
            ?: WidgetPresentation.AUTOMATIC

    fun save(widgetId: Int, presentation: WidgetPresentation) {
        preferences.putString(NAMESPACE, key(widgetId), presentation.name)
    }

    fun remove(widgetId: Int) {
        preferences.putString(NAMESPACE, key(widgetId), null)
    }

    private fun key(widgetId: Int) = "presentation_$widgetId"

    companion object {
        private const val NAMESPACE = "widget_preferences"
        fun validWidgetId(intentValue: Int): Boolean =
            intentValue != AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
