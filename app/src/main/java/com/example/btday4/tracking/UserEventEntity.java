package com.example.btday4.tracking;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Isolated Room entity capturing user navigation, screen dwell time, searches, and shopping actions.
 */
@Entity(
        tableName = "user_events",
        indices = {
                @Index("session_id"),
                @Index("event_type"),
                @Index("timestamp"),
                @Index({"username", "timestamp"})
        }
)
public class UserEventEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "session_id")
    private String sessionId;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "screen_name")
    private String screenName;

    @NonNull
    @ColumnInfo(name = "event_type")
    private String eventType;

    @NonNull
    @ColumnInfo(name = "target_id")
    private String targetId;

    @NonNull
    @ColumnInfo(name = "detail")
    private String detail;

    @ColumnInfo(name = "duration_ms")
    private long durationMs;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public UserEventEntity(
            @NonNull String sessionId,
            @NonNull String username,
            @NonNull String screenName,
            @NonNull String eventType,
            @NonNull String targetId,
            @NonNull String detail,
            long durationMs,
            long timestamp
    ) {
        this.sessionId = sessionId;
        this.username = username;
        this.screenName = screenName;
        this.eventType = eventType;
        this.targetId = targetId;
        this.detail = detail;
        this.durationMs = durationMs;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getScreenName() {
        return screenName;
    }

    @NonNull
    public String getEventType() {
        return eventType;
    }

    @NonNull
    public String getTargetId() {
        return targetId;
    }

    @NonNull
    public String getDetail() {
        return detail;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
