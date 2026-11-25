package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.model.AppNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Per-user local notification repository powering the Bottom Navigation "Thông báo" tab and badge.
 */
public final class NotificationRepository {

    public interface Listener {
        void onNotificationsChanged();
    }

    private static final String PREF_NOTIFICATIONS = "btday4_in_app_notifications";
    private static final int MAX_STORED_PER_USER = 60;
    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private NotificationRepository() {
    }

    public static void addListener(@NonNull Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(@NonNull Listener listener) {
        LISTENERS.remove(listener);
    }

    @NonNull
    public static List<AppNotification> getNotifications(@Nullable Context context, @Nullable String username) {
        if (context == null) {
            return new ArrayList<>();
        }
        String cleanUser = normalizeUser(username);
        ensureStarterWelcome(context, cleanUser);
        return readList(context, cleanUser);
    }

    public static int getUnreadCount(@Nullable Context context, @Nullable String username) {
        int count = 0;
        for (AppNotification item : getNotifications(context, username)) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public static void push(
            @Nullable Context context,
            @Nullable String username,
            @NonNull String type,
            @NonNull String title,
            @NonNull String message,
            @Nullable String targetId
    ) {
        if (context == null || message.trim().isEmpty()) {
            return;
        }
        String cleanUser = normalizeUser(username);
        ensureStarterWelcome(context, cleanUser);

        List<AppNotification> current = readList(context, cleanUser);
        AppNotification created = new AppNotification(
                UUID.randomUUID().toString(),
                type,
                title.trim(),
                message.trim(),
                targetId == null ? "" : targetId.trim(),
                System.currentTimeMillis(),
                false
        );
        current.add(0, created);
        if (current.size() > MAX_STORED_PER_USER) {
            current = new ArrayList<>(current.subList(0, MAX_STORED_PER_USER));
        }
        writeList(context, cleanUser, current);
        notifyListeners();
    }

    public static void markRead(@Nullable Context context, @Nullable String username, @NonNull String notificationId) {
        if (context == null) {
            return;
        }
        String cleanUser = normalizeUser(username);
        List<AppNotification> list = readList(context, cleanUser);
        boolean changed = false;
        for (AppNotification item : list) {
            if (item.getId().equals(notificationId) && !item.isRead()) {
                item.setRead(true);
                changed = true;
            }
        }
        if (changed) {
            writeList(context, cleanUser, list);
            notifyListeners();
        }
    }

    public static void markAllRead(@Nullable Context context, @Nullable String username) {
        if (context == null) {
            return;
        }
        String cleanUser = normalizeUser(username);
        List<AppNotification> list = readList(context, cleanUser);
        boolean changed = false;
        for (AppNotification item : list) {
            if (!item.isRead()) {
                item.setRead(true);
                changed = true;
            }
        }
        if (changed) {
            writeList(context, cleanUser, list);
            notifyListeners();
        }
    }

    public static void clearAll(@Nullable Context context, @Nullable String username) {
        if (context == null) {
            return;
        }
        String cleanUser = normalizeUser(username);
        writeList(context, cleanUser, new ArrayList<>());
        notifyListeners();
    }

    private static void ensureStarterWelcome(Context context, String cleanUser) {
        SharedPreferences p = prefs(context);
        String seedKey = "welcome_seeded_" + cleanUser;
        if (p.getBoolean(seedKey, false)) {
            return;
        }
        p.edit().putBoolean(seedKey, true).apply();
        List<AppNotification> list = readList(context, cleanUser);
        if (list.isEmpty()) {
            list.add(new AppNotification(
                    UUID.randomUUID().toString(),
                    AppNotification.TYPE_VOUCHER,
                    "Quà tặng thành viên TechStore",
                    "Mã TECHNEW15 (-15%) và FREESHIP đã sẵn sàng trong Kho Voucher của bạn.",
                    "TECHNEW15",
                    System.currentTimeMillis(),
                    false
            ));
            writeList(context, cleanUser, list);
        }
    }

    private static List<AppNotification> readList(Context context, String cleanUser) {
        String raw = prefs(context).getString("items_" + cleanUser, "[]");
        List<AppNotification> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                list.add(new AppNotification(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("type", AppNotification.TYPE_ORDER),
                        obj.optString("title", "Thông báo"),
                        obj.optString("message", ""),
                        obj.optString("targetId", ""),
                        obj.optLong("createdAt", 0L),
                        obj.optBoolean("read", false)
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static void writeList(Context context, String cleanUser, List<AppNotification> list) {
        JSONArray arr = new JSONArray();
        try {
            for (AppNotification item : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("type", item.getType());
                obj.put("title", item.getTitle());
                obj.put("message", item.getMessage());
                obj.put("targetId", item.getTargetId());
                obj.put("createdAt", item.getCreatedAt());
                obj.put("read", item.isRead());
                arr.put(obj);
            }
        } catch (Exception ignored) {
        }
        prefs(context).edit().putString("items_" + cleanUser, arr.toString()).apply();
    }

    private static void notifyListeners() {
        MAIN_HANDLER.post(() -> {
            for (Listener listener : LISTENERS) {
                listener.onNotificationsChanged();
            }
        });
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NOTIFICATIONS, Context.MODE_PRIVATE);
    }

    private static String normalizeUser(@Nullable String username) {
        return username == null || username.trim().isEmpty()
                ? "guest"
                : username.trim().toLowerCase(Locale.US);
    }
}
