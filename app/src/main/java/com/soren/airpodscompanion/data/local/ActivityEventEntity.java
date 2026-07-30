package com.soren.airpodscompanion.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_events")
public class ActivityEventEntity {
    @PrimaryKey @NonNull public String id;
    public long timestamp;
    @NonNull public String type;
    @NonNull public String deviceName;
    @NonNull public String detail;

    public ActivityEventEntity(@NonNull String id, long timestamp, @NonNull String type,
            @NonNull String deviceName, @NonNull String detail) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.deviceName = deviceName;
        this.detail = detail;
    }
}
