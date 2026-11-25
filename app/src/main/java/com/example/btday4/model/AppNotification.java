package com.example.btday4.model;

import androidx.annotation.NonNull;

/**
 * Domain model representing an in-app notification shown in the top banner and Notification Center tab.
 */
public class AppNotification {

    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_VOUCHER = "VOUCHER";
    public static final String TYPE_CART = "CART";
    public static final String TYPE_ACCOUNT = "ACCOUNT";
    public static final String TYPE_ALERT = "ALERT";

    @NonNull
    private final String id;
    @NonNull
    private final String type;
    @NonNull
    private final String title;
    @NonNull
    private final String message;
    @NonNull
    private final String targetId;
    private final long createdAt;
    private boolean read;

    public AppNotification(
            @NonNull String id,
            @NonNull String type,
            @NonNull String title,
            @NonNull String message,
            @NonNull String targetId,
            long createdAt,
            boolean read
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetId = targetId;
        this.createdAt = createdAt;
        this.read = read;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getTargetId() {
        return targetId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
