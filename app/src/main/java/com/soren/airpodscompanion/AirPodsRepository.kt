package com.soren.airpodscompanion

import android.content.Context

enum class RepositoryOwner { ACTIVITY, MONITOR_SERVICE }

class AirPodsRepository private constructor(context: Context) {
    val controller = BluetoothController(context.applicationContext)
    private val owners = mutableSetOf<RepositoryOwner>()

    @Synchronized
    fun acquire(owner: RepositoryOwner) {
        val wasEmpty = owners.isEmpty()
        owners += owner
        if (wasEmpty) controller.start()
    }

    @Synchronized
    fun release(owner: RepositoryOwner) {
        owners -= owner
        if (owners.isEmpty()) controller.stop()
    }

    companion object {
        @Volatile private var instance: AirPodsRepository? = null

        fun get(context: Context): AirPodsRepository =
            instance ?: synchronized(this) {
                instance ?: AirPodsRepository(context).also { instance = it }
            }
    }
}
