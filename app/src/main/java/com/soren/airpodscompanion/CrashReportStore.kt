package com.soren.airpodscompanion

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class CrashReportStore(private val context: Context) {
    private val directory = File(context.noBackupFilesDir, "crash_reports")

    fun capture(thread: Thread, throwable: Throwable) {
        if (!DiagnosticConsentStore(context).load().crashReports) return
        runCatching {
            directory.mkdirs()
            val appVersion = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
            val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }
                .toString()
                .take(MAX_TRACE_LENGTH)
            val payload = buildString {
                append("{\n")
                append("  \"timestamp\":${System.currentTimeMillis()},\n")
                append("  \"appVersion\":\"${json(appVersion)}\",\n")
                append("  \"androidApi\":${Build.VERSION.SDK_INT},\n")
                append("  \"manufacturer\":\"${json(Build.MANUFACTURER)}\",\n")
                append("  \"model\":\"${json(Build.MODEL)}\",\n")
                append("  \"thread\":\"${json(thread.name)}\",\n")
                append("  \"exception\":\"${json(throwable.javaClass.name)}\",\n")
                append("  \"message\":\"${json(throwable.message.orEmpty())}\",\n")
                append("  \"stackTrace\":\"${json(trace)}\"\n")
                append("}")
            }
            File(directory, "crash-${System.currentTimeMillis()}.json").writeText(payload)
            trim()
        }
    }

    fun reports(): List<String> =
        directory.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending(File::lastModified)
            ?.take(MAX_REPORTS)
            ?.mapNotNull { runCatching { it.readText() }.getOrNull() }
            .orEmpty()

    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
        directory.delete()
    }

    private fun trim() {
        directory.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_REPORTS)
            ?.forEach { it.delete() }
    }

    private fun json(value: String) = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")

    private companion object {
        const val MAX_REPORTS = 5
        const val MAX_TRACE_LENGTH = 24_000
    }
}
