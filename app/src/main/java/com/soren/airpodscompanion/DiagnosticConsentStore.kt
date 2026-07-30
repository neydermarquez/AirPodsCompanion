package com.soren.airpodscompanion

import android.content.Context
import com.soren.airpodscompanion.data.preferences.AppPreferencesRepository

data class DiagnosticConsent(
    val crashReports: Boolean = false,
    val usageMetrics: Boolean = false
)

class DiagnosticConsentStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = AppPreferencesRepository.get(appContext)

    fun load() = DiagnosticConsent(
        crashReports = preferences.boolean(NAMESPACE, CRASH_REPORTS, false),
        usageMetrics = preferences.boolean(NAMESPACE, USAGE_METRICS, false)
    )

    fun save(consent: DiagnosticConsent) {
        preferences.putBoolean(NAMESPACE, CRASH_REPORTS, consent.crashReports)
        preferences.putBoolean(NAMESPACE, USAGE_METRICS, consent.usageMetrics)
        if (!consent.crashReports) CrashReportStore(appContext).clear()
        if (!consent.usageMetrics) LocalMetricStore(appContext).clear()
    }

    fun recordMetric(name: String) {
        if (!load().usageMetrics) return
        val safeName = name.lowercase().replace(Regex("[^a-z0-9_.-]"), "_").take(64)
        LocalMetricStore(appContext).increment(safeName)
    }

    companion object {
        private const val NAMESPACE = "diagnostics_consent"
        private const val CRASH_REPORTS = "crash_reports"
        private const val USAGE_METRICS = "usage_metrics"
    }
}
