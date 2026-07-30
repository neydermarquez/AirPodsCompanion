package com.soren.airpodscompanion

import android.content.Context
import java.io.File
import java.util.Properties

class LocalMetricStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "local_metrics.properties")

    @Synchronized
    fun increment(name: String) {
        val metrics = read()
        metrics.setProperty(name, ((metrics.getProperty(name)?.toIntOrNull() ?: 0) + 1).toString())
        file.parentFile?.mkdirs()
        file.outputStream().use { metrics.store(it, null) }
    }

    @Synchronized
    fun snapshot(): Map<String, Int> {
        val metrics = read()
        return metrics.stringPropertyNames()
            .associateWith { name -> metrics.getProperty(name)?.toIntOrNull() ?: 0 }
    }

    fun clear() {
        file.delete()
    }

    private fun read() = Properties().also { properties ->
        if (file.isFile) runCatching {
            file.inputStream().use(properties::load)
        }
    }
}
