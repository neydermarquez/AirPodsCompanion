package com.soren.airpodscompanion.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.airPodsPreferences by preferencesDataStore("airpods_preferences")

class AppPreferencesRepository private constructor(private val context: Context) {
    private val dataStore = context.airPodsPreferences

    init {
        migrateLegacyOnce()
    }

    fun string(namespace: String, key: String, default: String? = null): String? = read {
        it[stringPreferencesKey("$namespace.$key")] ?: default
    }

    fun int(namespace: String, key: String, default: Int): Int = read {
        it[intPreferencesKey("$namespace.$key")] ?: default
    }

    fun boolean(namespace: String, key: String, default: Boolean): Boolean = read {
        it[booleanPreferencesKey("$namespace.$key")] ?: default
    }

    fun putString(namespace: String, key: String, value: String?) = write {
        val target = stringPreferencesKey("$namespace.$key")
        if (value == null) remove(target) else this[target] = value
    }

    fun putInt(namespace: String, key: String, value: Int) = write {
        this[intPreferencesKey("$namespace.$key")] = value
    }

    fun putBoolean(namespace: String, key: String, value: Boolean) = write {
        this[booleanPreferencesKey("$namespace.$key")] = value
    }

    fun removeNamespace(namespace: String) = write {
        asMap().keys.filter { it.name.startsWith("$namespace.") }.forEach { remove(it) }
    }

    fun snapshot(): Map<String, Any> = read { preferences ->
        preferences.asMap().mapKeys { it.key.name }.mapValues { it.value }
    }

    fun clearAll() = write { clear() }

    private fun migrateLegacyOnce() {
        if (boolean(META, "legacy_migrated", false)) return
        LEGACY_NAMESPACES.forEach { namespace ->
            val legacy = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
            legacy.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(namespace, key, value)
                    is Int -> putInt(namespace, key, value)
                    is String -> putString(namespace, key, value)
                }
            }
        }
        putBoolean(META, "legacy_migrated", true)
    }

    private fun <T> read(block: (Preferences) -> T): T =
        runBlocking(Dispatchers.IO) { block(dataStore.data.first()) }

    private fun write(block: androidx.datastore.preferences.core.MutablePreferences.() -> Unit) {
        runBlocking(Dispatchers.IO) { dataStore.edit(block) }
    }

    companion object {
        private const val META = "_meta"
        private val LEGACY_NAMESPACES = listOf(
            "background_preferences",
            "notification_preferences",
            "listening_profile",
            "history_retention",
            "widget_preferences",
            "protocol_capture"
        )
        @Volatile private var instance: AppPreferencesRepository? = null

        fun get(context: Context): AppPreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: AppPreferencesRepository(context.applicationContext).also { instance = it }
            }
    }
}
