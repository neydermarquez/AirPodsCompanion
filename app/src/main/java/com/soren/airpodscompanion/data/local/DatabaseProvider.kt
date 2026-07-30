package com.soren.airpodscompanion.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AirPodsDatabase? = null

    fun get(context: Context): AirPodsDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AirPodsDatabase::class.java,
                "airpods_local.db"
            ).build().also { instance = it }
        }
}
