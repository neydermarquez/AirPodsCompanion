package com.soren.airpodscompanion.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
    entities = {ActivityEventEntity.class, ProtocolSampleEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AirPodsDatabase extends RoomDatabase {
    public abstract AirPodsDao dao();
}
