package com.soren.airpodscompanion.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AirPodsDao {
    @Query("SELECT * FROM activity_events WHERE timestamp >= :cutoff ORDER BY timestamp DESC LIMIT :limit")
    List<ActivityEventEntity> activity(long cutoff, int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertActivity(List<ActivityEventEntity> events);

    @Query("DELETE FROM activity_events")
    void clearActivity();

    @Query("DELETE FROM activity_events WHERE timestamp < :cutoff")
    void deleteOldActivity(long cutoff);

    @Query("DELETE FROM activity_events WHERE id NOT IN (SELECT id FROM activity_events ORDER BY timestamp DESC LIMIT :limit)")
    void trimActivity(int limit);

    @Query("SELECT * FROM protocol_samples WHERE timestamp >= :cutoff ORDER BY timestamp ASC LIMIT :limit")
    List<ProtocolSampleEntity> protocol(long cutoff, int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProtocol(List<ProtocolSampleEntity> samples);

    @Query("DELETE FROM protocol_samples WHERE sessionId = :sessionId")
    void deleteSession(String sessionId);

    @Query("DELETE FROM protocol_samples")
    void clearProtocol();

    @Query("DELETE FROM protocol_samples WHERE timestamp < :cutoff")
    void deleteOldProtocol(long cutoff);

    @Query("DELETE FROM protocol_samples WHERE id NOT IN (SELECT id FROM protocol_samples ORDER BY timestamp DESC LIMIT :limit)")
    void trimProtocol(int limit);
}
