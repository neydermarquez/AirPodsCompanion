package com.soren.airpodscompanion

import android.app.Application

class AirPodsCompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashReportStore(this).capture(thread, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        DiagnosticConsentStore(this).recordMetric("app_launch")
    }
}
