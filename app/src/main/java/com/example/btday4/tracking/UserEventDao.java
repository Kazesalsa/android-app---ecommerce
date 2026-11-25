package com.example.btday4.tracking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UserEventDao {

    @Insert
    void insert(UserEventEntity event);

    @Query("SELECT * FROM user_events WHERE username = :username ORDER BY timestamp DESC LIMIT :limit")
    List<UserEventEntity> getRecentForUser(String username, int limit);

    @Query("SELECT * FROM user_events WHERE username = :username AND event_type = :eventType ORDER BY timestamp DESC LIMIT :limit")
    List<UserEventEntity> getRecentByUserAndType(String username, String eventType, int limit);

    @Query("DELETE FROM user_events WHERE timestamp < :cutoffTimestamp")
    void deleteOlderThan(long cutoffTimestamp);
}
