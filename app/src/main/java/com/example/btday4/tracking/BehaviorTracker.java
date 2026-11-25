package com.example.btday4.tracking;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.data.local.Product;
import com.example.btday4.util.Constants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Automatically tracks app sessions, screen/tab dwell times, searches, and shopping interactions
 * into the isolated TrackingDatabase without touching AppDatabase or Firestore.
 */
public final class BehaviorTracker implements Application.ActivityLifecycleCallbacks {

    public static final String EVENT_APP_OPEN = "APP_OPEN";
    public static final String EVENT_APP_BACKGROUND = "APP_BACKGROUND";
    public static final String EVENT_SCREEN_VIEW = "SCREEN_VIEW";
    public static final String EVENT_TAB_VIEW = "TAB_VIEW";
    public static final String EVENT_SEARCH = "SEARCH";
    public static final String EVENT_FILTER_CATEGORY = "FILTER_CATEGORY";
    public static final String EVENT_SORT = "SORT";
    public static final String EVENT_VIEW_PRODUCT = "VIEW_PRODUCT";
    public static final String EVENT_ADD_TO_CART = "ADD_TO_CART";
    public static final String EVENT_UPDATE_CART = "UPDATE_CART";
    public static final String EVENT_REMOVE_CART = "REMOVE_CART";
    public static final String EVENT_BUY_NOW = "BUY_NOW";
    public static final String EVENT_CHECKOUT_START = "CHECKOUT_START";
    public static final String EVENT_PURCHASE = "PURCHASE";

    private static final long RETENTION_MS = 30L * 24L * 60L * 60L * 1000L;

    private static volatile BehaviorTracker instance;

    private final Context appContext;
    private final SharedPreferences sessionPrefs;
    private final Map<Integer, Long> activityResumeTimes = new HashMap<>();

    private String sessionId = newSessionId();
    private int startedActivities = 0;
    private long sessionStartElapsedMs = 0L;

    private String activeTabName = null;
    private long activeTabStartElapsedMs = 0L;

    private BehaviorTracker(Application application) {
        this.appContext = application.getApplicationContext();
        this.sessionPrefs = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        application.registerActivityLifecycleCallbacks(this);
        pruneOldEvents();
    }

    public static void init(@NonNull Application application) {
        if (instance == null) {
            synchronized (BehaviorTracker.class) {
                if (instance == null) {
                    instance = new BehaviorTracker(application);
                }
            }
        }
    }

    @Nullable
    public static BehaviorTracker getInstance() {
        return instance;
    }

    public void onTabSelected(@NonNull String tabName) {
        long now = SystemClock.elapsedRealtime();
        if (activeTabName != null && !activeTabName.equals(tabName) && activeTabStartElapsedMs > 0L) {
            long dwellMs = Math.max(0L, now - activeTabStartElapsedMs);
            record("MainActivity:" + activeTabName, EVENT_TAB_VIEW, activeTabName, "switch_to:" + tabName, dwellMs);
        }
        activeTabName = tabName;
        activeTabStartElapsedMs = now;
    }

    public void onMainResumed(@NonNull String currentTabName) {
        activeTabName = currentTabName;
        activeTabStartElapsedMs = SystemClock.elapsedRealtime();
    }

    public void onMainPaused() {
        if (activeTabName != null && activeTabStartElapsedMs > 0L) {
            long dwellMs = Math.max(0L, SystemClock.elapsedRealtime() - activeTabStartElapsedMs);
            record("MainActivity:" + activeTabName, EVENT_TAB_VIEW, activeTabName, "pause", dwellMs);
            activeTabStartElapsedMs = 0L;
        }
    }

    public void trackSearch(@NonNull String query, int resultCount) {
        String clean = query.trim();
        if (clean.isEmpty()) {
            return;
        }
        record("MainActivity:Products", EVENT_SEARCH, clean, "results=" + resultCount, 0L);
    }

    public void trackCategoryFilter(@NonNull String category) {
        record("MainActivity:Products", EVENT_FILTER_CATEGORY, category, "", 0L);
    }

    public void trackSort(@NonNull String sortOption) {
        record("MainActivity:Products", EVENT_SORT, sortOption, "", 0L);
    }

    public void trackProductView(@NonNull Product product, long durationMs) {
        String detail = safe(product.getName())
                + "|category=" + safe(product.getCategory())
                + "|brand=" + safe(product.getBrand())
                + "|price=" + safe(product.getPrice());
        record("ProductDetailActivity", EVENT_VIEW_PRODUCT, safe(product.getId()), detail, Math.max(0L, durationMs));
    }

    public void trackAction(
            @NonNull String screenName,
            @NonNull String eventType,
            @Nullable String targetId,
            @Nullable String detail
    ) {
        record(screenName, eventType, safe(targetId), safe(detail), 0L);
    }

    private void record(
            @NonNull String screenName,
            @NonNull String eventType,
            @NonNull String targetId,
            @NonNull String detail,
            long durationMs
    ) {
        UserEventEntity event = new UserEventEntity(
                sessionId,
                resolveUsername(),
                screenName,
                eventType,
                targetId,
                detail,
                durationMs,
                System.currentTimeMillis()
        );
        TrackingDatabase.runAsync(appContext, db -> db.userEventDao().insert(event));
    }

    private void flushProfileSyncNow() {
        String username = resolveUsername();
        if (!"guest".equals(username)) {
            RecommendationEngine.syncTrackingSummaryToFirestore(appContext, username);
        }
    }

    private void pruneOldEvents() {
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        TrackingDatabase.runAsync(appContext, db -> db.userEventDao().deleteOlderThan(cutoff));
    }

    private String resolveUsername() {
        String username = sessionPrefs.getString(Constants.KEY_USERNAME, "");
        return (username == null || username.trim().isEmpty()) ? "guest" : username.trim();
    }

    private static String newSessionId() {
        return UUID.randomUUID().toString();
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (startedActivities == 0) {
            sessionId = newSessionId();
            sessionStartElapsedMs = SystemClock.elapsedRealtime();
            record(activity.getClass().getSimpleName(), EVENT_APP_OPEN, "", "", 0L);
        }
        startedActivities++;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        activityResumeTimes.put(System.identityHashCode(activity), SystemClock.elapsedRealtime());
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Long enteredAt = activityResumeTimes.remove(System.identityHashCode(activity));
        long dwellMs = enteredAt == null ? 0L : Math.max(0L, SystemClock.elapsedRealtime() - enteredAt);
        record(activity.getClass().getSimpleName(), EVENT_SCREEN_VIEW, "", "", dwellMs);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0 && !activity.isChangingConfigurations()) {
            long sessionMs = sessionStartElapsedMs > 0L
                    ? Math.max(0L, SystemClock.elapsedRealtime() - sessionStartElapsedMs)
                    : 0L;
            record(activity.getClass().getSimpleName(), EVENT_APP_BACKGROUND, "", "", sessionMs);
            flushProfileSyncNow();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        activityResumeTimes.remove(System.identityHashCode(activity));
    }
}
