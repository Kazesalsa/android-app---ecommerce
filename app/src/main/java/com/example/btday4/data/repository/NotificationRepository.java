package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.data.local.VoucherEntity;
import com.example.btday4.model.NotificationItem;
import com.example.btday4.util.Constants;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cloud Firestore-backed per-user notification store under `users/{username}/notifications/{id}`.
 * Syncs promo/voucher, cart, order, and payment notifications across sessions and devices.
 */
public final class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static final int MAX_ITEMS = 60;
    private static final String VOUCHER_NOTIF_PREFIX = "promo_voucher_";

    private NotificationRepository() {
    }

    public static void seedNotificationsIfEmpty(Context context, @Nullable Runnable onDone) {
        String username = resolveUsername(context);
        if (username.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = notificationsRef(db, username);
        col.limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        if (onDone != null) {
                            onDone.run();
                        }
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (NotificationItem item : createSeedNotifications(System.currentTimeMillis())) {
                        batch.set(col.document(item.getId()), toFirestoreMap(item));
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (onDone != null) {
                                    onDone.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to seed notifications", e);
                                if (onDone != null) {
                                    onDone.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check notifications collection", e);
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    @Nullable
    public static ListenerRegistration listenNotifications(
            Context context,
            @NonNull Consumer<List<NotificationItem>> callback
    ) {
        String username = resolveUsername(context);
        if (username.isEmpty()) {
            callback.accept(new ArrayList<>());
            return null;
        }
        return notificationsRef(FirebaseFirestore.getInstance(), username)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Notification snapshot listen error", e);
                        callback.accept(new ArrayList<>());
                        return;
                    }
                    callback.accept(parseAndSort(snapshot == null ? null : snapshot.getDocuments()));
                });
    }

    public static void getUnreadCount(Context context, @NonNull Consumer<Integer> callback) {
        String username = resolveUsername(context);
        if (username.isEmpty()) {
            callback.accept(0);
            return;
        }
        notificationsRef(FirebaseFirestore.getInstance(), username)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> callback.accept(snapshot == null ? 0 : snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to count unread notifications", e);
                    callback.accept(0);
                });
    }

    public static void addNotification(
            Context context,
            NotificationItem.Type type,
            String title,
            String message,
            String targetId
    ) {
        addNotification(context, type, title, message, targetId, "");
    }

    public static void addNotification(
            Context context,
            NotificationItem.Type type,
            String title,
            String message,
            String targetId,
            String promoCode
    ) {
        String username = resolveUsername(context);
        if (username.isEmpty()) {
            return;
        }
        String id = UUID.randomUUID().toString();
        NotificationItem created = new NotificationItem(
                id,
                type,
                title,
                message,
                System.currentTimeMillis(),
                targetId,
                promoCode,
                false
        );
        notificationsRef(FirebaseFirestore.getInstance(), username)
                .document(id)
                .set(toFirestoreMap(created))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification", e));
    }

    public static void markAsRead(Context context, String notificationId) {
        String username = resolveUsername(context);
        if (username.isEmpty() || notificationId == null || notificationId.trim().isEmpty()) {
            return;
        }
        notificationsRef(FirebaseFirestore.getInstance(), username)
                .document(notificationId.trim())
                .update("read", true)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification read", e));
    }

    public static void markAllAsRead(Context context, @Nullable Runnable onDone) {
        String username = resolveUsername(context);
        if (username.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        notificationsRef(db, username)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        if (onDone != null) {
                            onDone.run();
                        }
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (onDone != null) {
                                    onDone.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to mark all notifications read", e);
                                if (onDone != null) {
                                    onDone.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query unread notifications", e);
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    public static void syncNewVouchers(
            Context context,
            List<VoucherEntity> vouchers,
            @Nullable Runnable onDone
    ) {
        String username = resolveUsername(context);
        if (username.isEmpty() || vouchers == null || vouchers.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = notificationsRef(db, username);
        col.get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> existingIds = new HashSet<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            existingIds.add(doc.getId());
                        }
                    }

                    long now = System.currentTimeMillis();
                    WriteBatch batch = db.batch();
                    int pendingWrites = 0;
                    for (VoucherEntity voucher : vouchers) {
                        if (voucher == null || voucher.getCode().isEmpty() || !voucher.isAvailable(now)) {
                            continue;
                        }
                        String code = voucher.getCode();
                        if ("TECH10".equalsIgnoreCase(code)) {
                            continue;
                        }
                        String docId = VOUCHER_NOTIF_PREFIX + code;
                        if (existingIds.contains(docId)) {
                            continue;
                        }
                        String title = voucher.getTitle().isEmpty()
                                ? "Voucher mới: " + code
                                : "Voucher mới: " + voucher.getTitle();
                        String message = voucher.getDescription().isEmpty()
                                ? "Nhập mã " + code + " tại bước Thanh toán để nhận ưu đãi " + voucher.formatShortBadge() + "."
                                : voucher.getDescription();
                        NotificationItem item = new NotificationItem(
                                docId,
                                NotificationItem.Type.PROMO,
                                title,
                                message,
                                now,
                                "",
                                code,
                                false
                        );
                        batch.set(col.document(docId), toFirestoreMap(item));
                        pendingWrites++;
                    }
                    if (pendingWrites == 0) {
                        if (onDone != null) {
                            onDone.run();
                        }
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (onDone != null) {
                                    onDone.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to sync voucher notifications", e);
                                if (onDone != null) {
                                    onDone.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing voucher notifications", e);
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    public static List<NotificationItem> createSeedNotifications(long nowMillis) {
        List<NotificationItem> seed = new ArrayList<>();
        seed.add(new NotificationItem(
                "promo_welcome_10",
                NotificationItem.Type.PROMO,
                "Quà tặng thành viên: Giảm 10%",
                "App tặng bạn mã TECH10 giảm 10% (tối đa 50.000 ₫) cho đơn từ 50.000 ₫. Vào mục Voucher hoặc Thanh toán để áp dụng ngay.",
                nowMillis - 10 * 60_000L,
                "",
                "TECH10",
                false
        ));
        seed.add(new NotificationItem(
                "promo_freeship",
                NotificationItem.Type.PROMO,
                "Miễn phí vận chuyển toàn quốc",
                "Tự động miễn phí giao hàng cho mọi đơn hàng công nghệ từ 50.000 ₫ trở lên.",
                nowMillis - 2 * 60 * 60_000L,
                "",
                "",
                false
        ));
        return seed;
    }

    public static Map<String, Object> toFirestoreMap(NotificationItem item) {
        Map<String, Object> map = new HashMap<>();
        if (item == null) {
            return map;
        }
        map.put("id", item.getId());
        map.put("type", item.getType().name());
        map.put("title", item.getTitle());
        map.put("message", item.getMessage());
        map.put("timestamp", item.getTimestamp());
        map.put("targetId", item.getTargetId());
        map.put("promoCode", item.getPromoCode());
        map.put("read", item.isRead());
        return map;
    }

    public static NotificationItem fromFirestoreMap(
            @Nullable String fallbackId,
            @Nullable Map<String, Object> map
    ) {
        String safeFallbackId = fallbackId == null ? "" : fallbackId;
        if (map == null) {
            return new NotificationItem(safeFallbackId, NotificationItem.Type.ORDER, "", "", 0L, "", "", false);
        }
        String id = stringVal(map.get("id"));
        if (id.isEmpty()) {
            id = safeFallbackId;
        }
        NotificationItem.Type type;
        try {
            String rawType = stringVal(map.get("type"));
            type = rawType.isEmpty() ? NotificationItem.Type.ORDER : NotificationItem.Type.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            type = NotificationItem.Type.ORDER;
        }
        Object readRaw = map.get("read");
        boolean read = readRaw instanceof Boolean && (Boolean) readRaw;
        return new NotificationItem(
                id,
                type,
                stringVal(map.get("title")),
                stringVal(map.get("message")),
                longVal(map.get("timestamp")),
                stringVal(map.get("targetId")),
                stringVal(map.get("promoCode")),
                read
        );
    }

    private static List<NotificationItem> parseAndSort(@Nullable List<DocumentSnapshot> docs) {
        List<NotificationItem> list = new ArrayList<>();
        if (docs != null) {
            for (DocumentSnapshot doc : docs) {
                NotificationItem item = fromFirestoreMap(doc.getId(), doc.getData());
                if (!item.getId().isEmpty()) {
                    list.add(item);
                }
            }
        }
        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        if (list.size() > MAX_ITEMS) {
            return new ArrayList<>(list.subList(0, MAX_ITEMS));
        }
        return list;
    }

    private static CollectionReference notificationsRef(FirebaseFirestore db, String username) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(username)
                .collection(Constants.COLLECTION_NOTIFICATIONS);
    }

    private static String resolveUsername(@Nullable Context context) {
        if (context == null) {
            return "";
        }
        SharedPreferences sessionPrefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String username = sessionPrefs.getString(Constants.KEY_USERNAME, "");
        String clean = username == null ? "" : username.trim();
        return "guest".equals(clean) ? "" : clean;
    }

    private static String stringVal(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long longVal(@Nullable Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
