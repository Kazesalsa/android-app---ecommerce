package com.example.btday4.model;

/**
 * In-app notification item displayed in the Notifications tab.
 */
public class NotificationItem {

    public enum Type {
        PROMO,
        CART,
        PAYMENT,
        ORDER
    }

    private final String id;
    private final Type type;
    private final String title;
    private final String message;
    private final long timestamp;
    private final String targetId;
    private final String promoCode;
    private final boolean read;

    public NotificationItem(
            String id,
            Type type,
            String title,
            String message,
            long timestamp,
            String targetId,
            String promoCode,
            boolean read) {
        this.id = id == null ? "" : id;
        this.type = type == null ? Type.ORDER : type;
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.timestamp = timestamp;
        this.targetId = targetId == null ? "" : targetId;
        this.promoCode = promoCode == null ? "" : promoCode;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public boolean isRead() {
        return read;
    }
}
