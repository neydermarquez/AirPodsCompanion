package com.soren.airpodscompanion

import android.bluetooth.BluetoothA2dp
import android.util.Log
import java.lang.reflect.Method

class HiddenApiManager {
    private val a2dpClass by lazy {
        runCatching { Class.forName("android.bluetooth.BluetoothA2dp") }.getOrNull()
    }

    private fun findNoArgMethod(clazz: Class<*>, name: String): Method? =
        runCatching { clazz.getDeclaredMethod(name) }.getOrElse {
            runCatching { clazz.getMethod(name) }.getOrNull()
        }

    private fun findSingleArgMethod(clazz: Class<*>, name: String): Method? =
        runCatching { clazz.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.size == 1 } }.getOrNull()
            ?: runCatching { clazz.methods.firstOrNull { it.name == name && it.parameterTypes.size == 1 } }.getOrNull()

    fun getCodecConfig(a2dp: BluetoothA2dp): Any? {
        val method = a2dpClass
            ?.let { findNoArgMethod(it, "getCodecConfig") }
            ?: return runCatching { a2dp.javaClass.getMethod("getCodecConfig").invoke(a2dp) }.getOrNull()
        return invokeAny(a2dp, method)
    }

    fun setCodecConfig(a2dp: BluetoothA2dp, codecConfig: Any): Boolean {
        val method = a2dpClass
            ?.let { findSingleArgMethod(it, "setCodecConfigPreference") }
            ?: return false
        return invokeBoolean(a2dp, method, codecConfig)
    }

    private fun invokeAny(target: Any, method: Method): Any? =
        runCatching {
            method.isAccessible = true
            try {
                method.invoke(target)
            } finally {
                method.isAccessible = false
            }
        }.onFailure { e ->
            Log.e("HiddenApi", "Failed to invoke ${method.name}", e)
        }.getOrNull()

    private fun invokeBoolean(target: Any, method: Method, arg: Any): Boolean {
        val result = runCatching {
            method.isAccessible = true
            try {
                method.invoke(target, arg)
            } finally {
                method.isAccessible = false
            }
        }.onFailure { e ->
            Log.e("HiddenApi", "Failed to invoke ${method.name}", e)
        }.getOrNull()
        return when (result) {
            is Boolean -> result
            is Number -> result.toInt() != 0
            is String -> result.equals("true", ignoreCase = true)
            else -> false
        }
    }
}
