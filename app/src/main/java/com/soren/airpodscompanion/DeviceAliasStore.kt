package com.soren.airpodscompanion

import android.content.Context
import android.util.Base64

/**
 * Stores a user-facing alias locally. Android may also accept the alias at system level
 * when the device is associated through Companion Device Manager.
 */
class DeviceAliasStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAMESPACE, Context.MODE_PRIVATE)

    fun get(address: String): String? =
        preferences.getString(key(address), null)?.takeIf { it.isNotBlank() }

    fun save(address: String, alias: String) {
        preferences.edit().putString(key(address), alias.trim()).apply()
    }

    fun aliases(): List<String> = preferences.all.values.filterIsInstance<String>().sorted()

    private fun key(address: String): String =
        Base64.encodeToString(address.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

    private companion object {
        const val NAMESPACE = "device_aliases"
    }
}
