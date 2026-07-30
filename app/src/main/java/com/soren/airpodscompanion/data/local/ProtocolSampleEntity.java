package com.soren.airpodscompanion.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "protocol_samples")
public class ProtocolSampleEntity {
    @PrimaryKey @NonNull public String id;
    @NonNull public String sessionId;
    public long timestamp;
    @NonNull public String model;
    @NonNull public String scenario;
    @NonNull public String source;
    @NonNull public String payload;
    public Integer rssi;

    public ProtocolSampleEntity(@NonNull String id, @NonNull String sessionId, long timestamp,
            @NonNull String model, @NonNull String scenario, @NonNull String source,
            @NonNull String payload, Integer rssi) {
        this.id = id;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.model = model;
        this.scenario = scenario;
        this.source = source;
        this.payload = payload;
        this.rssi = rssi;
    }
}
