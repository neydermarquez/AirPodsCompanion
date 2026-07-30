package com.soren.airpodscompanion

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import java.lang.reflect.Method

private const val HIDDEN_API_BYPASS = "dev.rikka.tools.reflection.HiddenApiBypass"

class HiddenBluetoothCompat(context: Context) {
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val nativeBridge = NativeHiddenApiBridge

    init {
        installHiddenApiExemptions()
    }

    fun getA2dpCodecConfig(a2dp: BluetoothA2dp): Any? {
        nativeBridge.getCodecConfig(a2dp)?.let { return it }
        return invokeNoArg(a2dp, "getCodecConfig")
    }

    fun setA2dpCodecPreference(a2dp: BluetoothA2dp, codecConfig: Any): Boolean {
        val native = nativeBridge.setCodecConfigPreference(a2dp, codecConfig)
        if (native != null) return native
        return invokeBoolean(a2dp, "setCodecConfigPreference", codecConfig)
    }

    fun getA2dpCodecStatus(a2dp: BluetoothA2dp): Any? {
        nativeBridge.getCodecStatus(a2dp)?.let { return it }
        return invokeNoArg(a2dp, "getCodecStatus")
    }

    fun setBluetoothA2dpEnabled(enabled: Boolean): Boolean {
        val native = audioManager?.let { nativeBridge.setBluetoothA2dpOn(it, enabled) } ?: false
        if (native) return true
        return invokeBoolean(audioManager ?: return false, "setBluetoothA2dpOn", enabled)
    }

    fun getAudioParameters(keys: String): String? {
        val native = audioManager?.let { nativeBridge.getParameters(it, keys) }
        if (native != null) return native
        return invokeString(audioManager ?: return null, "getParameters", keys)
    }

    fun setAudioParameters(keyValuePairs: String): Boolean {
        val native = audioManager?.let { nativeBridge.setParameters(it, keyValuePairs) } ?: false
        if (native) return true
        return invokeVoid(audioManager ?: return false, "setParameters", keyValuePairs)
    }

    fun getAdapterConnectionState(): Int? {
        val native = nativeBridge.getConnectionState(adapter) ?: return null
        return native
    }

    fun disconnectFromAdapter(device: BluetoothDevice): Boolean {
        val native = nativeBridge.disconnect(adapter, device) ?: false
        if (native) return true
        return invokeBoolean(adapter, "disconnect", device)
    }

    fun getAdapterLeConnectionState(device: BluetoothDevice): Int? {
        val native = nativeBridge.getLeConnectionState(adapter, device)
        if (native != null) return native
        return invokeInt(adapter, "getLeConnectionState", device)
    }

    private fun installHiddenApiExemptions() {
        runCatching {
            val bypassClass = Class.forName(HIDDEN_API_BYPASS)
        val method = runCatching { bypassClass.getDeclaredMethod("addHiddenApiExemptions", String::class.java) }
                .getOrElse { bypassClass.getDeclaredMethod("addHiddenApiExemptions", Array<String>::class.java) }
            method.isAccessible = true
            val arg: Any = when (method.parameterTypes.firstOrNull()) {
                Array<String>::class.java -> arrayOf(
                    BluetoothA2dp::class.java.name,
                    AudioManager::class.java.name,
                    BluetoothAdapter::class.java.name
                )
                else -> BluetoothA2dp::class.java.name
            }
            method.invoke(null, arg)
        }
    }

    private fun invokeNoArg(target: Any, methodName: String): Any? {
        return runCatching {
            findMethod(target::class.java, methodName).invoke(target)
        }.getOrNull()
    }

    private fun invokeString(target: Any, methodName: String, arg: String): String? {
        return runCatching {
            findMethod(target::class.java, methodName, String::class.java).invoke(target, arg)?.toString()
        }.getOrNull()
    }

    private fun invokeVoid(target: Any, methodName: String, arg: String): Boolean {
        return runCatching {
            findMethod(target::class.java, methodName, String::class.java).invoke(target, arg)
            true
        }.getOrDefault(false)
    }

    private fun invokeBoolean(target: Any?, methodName: String, arg: Any? = null): Boolean {
        return runCatching {
            if (target == null) return false
            val method = if (arg == null) findMethod(target::class.java, methodName)
            else findMethod(target::class.java, methodName, arg::class.java)
            val result = if (arg == null) method.invoke(target) else method.invoke(target, arg)
            when (result) {
                is Boolean -> result
                is Number -> result.toInt() != 0
                is String -> result == "true"
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun invokeInt(target: Any?, methodName: String, arg: Any? = null): Int? {
        if (target == null) return null
        val method = if (arg == null) findMethod(target::class.java, methodName)
        else findMethod(target::class.java, methodName, arg::class.java)
        return runCatching {
            val result = if (arg == null) method.invoke(target) else method.invoke(target, arg)
            (result as? Number)?.toInt()
        }.getOrNull()
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method {
        return runCatching { clazz.getDeclaredMethod(name, *parameterTypes) }
            .getOrElse {
                runCatching { clazz.getMethod(name, *parameterTypes) }
                    .getOrElse {
                        resolveCompatibleMethod(clazz, name, parameterTypes.asList())
                    }
            }
    }

    private fun resolveCompatibleMethod(
        clazz: Class<*>,
        name: String,
        parameterTypes: List<Class<*>>
    ): Method {
        val candidates = clazz.methods + clazz.declaredMethods
        return candidates.firstOrNull { method ->
            method.name == name &&
                    method.parameterTypes.size == parameterTypes.size &&
                    method.parameterTypes.zip(parameterTypes).all { (declared, expected) ->
                        declared.isAssignableFrom(expected)
                    }
        } ?: throw NoSuchMethodException("${clazz.name}.$name")
    }

    private object NativeHiddenApiBridge {
        private val isAvailable = runCatching { System.loadLibrary("bluetooth_hidden_bridge") }.isSuccess

        fun getCodecConfig(a2dp: BluetoothA2dp): Any? = if (isAvailable) {
            runCatching { getCodecConfigNative(a2dp) }.getOrNull()
        } else null

        fun setCodecConfigPreference(a2dp: BluetoothA2dp, codecConfig: Any): Boolean? = if (isAvailable) {
            runCatching { setCodecConfigPreferenceNative(a2dp, codecConfig) }.getOrNull()
        } else null

        fun getCodecStatus(a2dp: BluetoothA2dp): Any? = if (isAvailable) {
            runCatching { getCodecStatusNative(a2dp) }.getOrNull()
        } else null

        fun getParameters(audioManager: AudioManager, keys: String): String? = if (isAvailable) {
            runCatching { getParametersNative(audioManager, keys) }.getOrNull()
        } else null

        fun setParameters(audioManager: AudioManager, keyValuePairs: String): Boolean = if (isAvailable) {
            runCatching { setParametersNative(audioManager, keyValuePairs) }.getOrNull() ?: false
        } else false

        fun setBluetoothA2dpOn(audioManager: AudioManager, enabled: Boolean): Boolean = if (isAvailable) {
            runCatching { setBluetoothA2dpOnNative(audioManager, enabled) }.getOrNull() ?: false
        } else false

        fun getConnectionState(adapter: BluetoothAdapter?): Int? = if (isAvailable && adapter != null) {
            runCatching { getConnectionStateNative(adapter) }.getOrNull()
        } else null

        fun disconnect(adapter: BluetoothAdapter?, device: BluetoothDevice): Boolean? = if (isAvailable && adapter != null) {
            runCatching { disconnectNative(adapter, device) }.getOrNull()
        } else null

        fun getLeConnectionState(adapter: BluetoothAdapter?, device: BluetoothDevice): Int? = if (isAvailable && adapter != null) {
            runCatching { getLeConnectionStateNative(adapter, device) }.getOrNull()
        } else null

        @JvmStatic
        private external fun getCodecConfigNative(a2dp: BluetoothA2dp): Any?

        @JvmStatic
        private external fun setCodecConfigPreferenceNative(a2dp: BluetoothA2dp, codecConfig: Any): Boolean

        @JvmStatic
        private external fun getCodecStatusNative(a2dp: BluetoothA2dp): Any?

        @JvmStatic
        private external fun getParametersNative(audioManager: AudioManager, keys: String): String?

        @JvmStatic
        private external fun setParametersNative(audioManager: AudioManager, keyValuePairs: String): Boolean

        @JvmStatic
        private external fun setBluetoothA2dpOnNative(audioManager: AudioManager, enabled: Boolean): Boolean

        @JvmStatic
        private external fun getConnectionStateNative(adapter: BluetoothAdapter): Int

        @JvmStatic
        private external fun disconnectNative(adapter: BluetoothAdapter, device: BluetoothDevice): Boolean

        @JvmStatic
        private external fun getLeConnectionStateNative(adapter: BluetoothAdapter, device: BluetoothDevice): Int
    }
}
