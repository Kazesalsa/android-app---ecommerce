package com.example.btday4.tracking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.model.ProductWithRating;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * On-device product ranking engine combining search intent, view dwell time, shopping actions,
 * cold-start gender hints, and product quality/stock from the isolated TrackingDatabase.
 */
public final class RecommendationEngine {

    private static final int MAX_EVENTS_TO_ANALYZE = 200;
    private static final double MILLIS_PER_DAY = 24.0 * 60.0 * 60.0 * 1000.0;
    private static final double HALF_LIFE_DAYS = 7.0;
    private static final double MAX_DWELL_SECONDS = 60.0;
    public static final long CACHE_TTL_MS = 12L * 60L * 60L * 1000L;

    private static final Object CACHE_LOCK = new Object();
    private static volatile Handler mainHandler;
    private static String cachedUsername;
    private static String cachedGender;
    private static long cachedComputedAtMs;
    private static Map<String, Double> cachedScores;

    private RecommendationEngine() {
    }

    /**
     * Clears the in-memory session ranking cache (called on login/logout so the next session refreshes).
     */
    public static void clearCache() {
        synchronized (CACHE_LOCK) {
            cachedUsername = null;
            cachedGender = null;
            cachedComputedAtMs = 0L;
            cachedScores = null;
        }
    }

    @Nullable
    public static Map<String, Double> getCachedScores(
            @Nullable String username,
            @Nullable String gender,
            long nowMillis
    ) {
        String cleanUser = normalizeUser(username);
        String cleanGender = normalizeGender(gender);
        synchronized (CACHE_LOCK) {
            if (cachedScores == null) {
                return null;
            }
            if (!cleanUser.equals(cachedUsername) || !cleanGender.equals(cachedGender)) {
                return null;
            }
            if (nowMillis - cachedComputedAtMs >= CACHE_TTL_MS) {
                return null;
            }
            return new HashMap<>(cachedScores);
        }
    }

    public static void cacheScores(
            @Nullable String username,
            @Nullable String gender,
            @NonNull Map<String, Double> scores,
            long nowMillis
    ) {
        String cleanUser = normalizeUser(username);
        String cleanGender = normalizeGender(gender);
        synchronized (CACHE_LOCK) {
            cachedUsername = cleanUser;
            cachedGender = cleanGender;
            cachedComputedAtMs = nowMillis;
            cachedScores = new HashMap<>(scores);
        }
    }

    private static String normalizeUser(@Nullable String username) {
        return (username == null || username.trim().isEmpty()) ? "guest" : username.trim();
    }

    private static String normalizeGender(@Nullable String gender) {
        return gender == null ? "" : gender.trim();
    }

    /**
     * Returns session-cached scores (valid for 12h or until logout/account change), or computes once
     * from TrackingDatabase so the product list stays stable during browsing.
     */
    public static void loadScores(
            @NonNull Context context,
            @Nullable String username,
            @Nullable String gender,
            @NonNull List<ProductWithRating> products,
            @NonNull Consumer<Map<String, Double>> callback
    ) {
        long now = System.currentTimeMillis();
        Map<String, Double> cached = getCachedScores(username, gender, now);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        String cleanUser = normalizeUser(username);
        String cleanGender = normalizeGender(gender);
        List<ProductWithRating> snapshot = new ArrayList<>(products);
        Context appContext = context.getApplicationContext();
        Handler handler = mainHandler;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
            mainHandler = handler;
        }
        final Handler uiHandler = handler;
        TrackingDatabase.runAsync(appContext, db -> {
            List<UserEventEntity> events = db.userEventDao().getRecentForUser(cleanUser, MAX_EVENTS_TO_ANALYZE);
            long computedAt = System.currentTimeMillis();
            Map<String, Double> scores = computeScores(snapshot, events, cleanGender, computedAt);
            cacheScores(cleanUser, cleanGender, scores, computedAt);
            Log.d("RecommendationEngine", "Scored " + scores.size() + " products from "
                    + (events == null ? 0 : events.size()) + " events for user=" + cleanUser);
            uiHandler.post(() -> callback.accept(scores));
        });
    }

    /**
     * Flushes one compact tracking summary document to Firestore (called on app close/background or logout).
     */
    public static void syncTrackingSummaryToFirestore(@NonNull Context context, @Nullable String username) {
        String cleanUser = normalizeUser(username);
        if ("guest".equals(cleanUser)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        TrackingDatabase.runAsync(appContext, db -> {
            List<UserEventEntity> events = db.userEventDao().getRecentForUser(cleanUser, MAX_EVENTS_TO_ANALYZE);
            if (events == null || events.isEmpty()) {
                return;
            }
            List<UserEventEntity> searchEvents = db.userEventDao()
                    .getRecentByUserAndType(cleanUser, BehaviorTracker.EVENT_SEARCH, 20);
            long now = System.currentTimeMillis();
            Map<String, Object> summary = buildTrackingSummary(cleanUser, events, now);
            if (searchEvents != null && !searchEvents.isEmpty()) {
                Set<String> latestSearches = new LinkedHashSet<>();
                for (UserEventEntity se : searchEvents) {
                    if (se != null && !se.getTargetId().trim().isEmpty() && latestSearches.size() < 10) {
                        latestSearches.add(se.getTargetId().trim());
                    }
                }
                summary.put("recentSearches", new ArrayList<>(latestSearches));
            }
            FirestoreHelper.saveTrackingProfile(cleanUser, summary);
        });
    }

    /**
     * Aggregates raw local Room tracking events into a single compact Firestore document.
     */
    @NonNull
    public static Map<String, Object> buildTrackingSummary(
            @NonNull String username,
            @Nullable List<UserEventEntity> events,
            long nowMillis
    ) {
        Map<String, Integer> eventCounts = new HashMap<>();
        Set<String> recentSearches = new LinkedHashSet<>();
        Set<String> recentViewedProducts = new LinkedHashSet<>();
        Set<String> recentCategories = new LinkedHashSet<>();
        long totalViewDurationMs = 0L;

        if (events != null) {
            for (UserEventEntity event : events) {
                if (event == null) {
                    continue;
                }
                String type = event.getEventType();
                eventCounts.merge(type, 1, Integer::sum);
                String target = event.getTargetId().trim();
                if (BehaviorTracker.EVENT_SEARCH.equals(type) && !target.isEmpty() && recentSearches.size() < 10) {
                    recentSearches.add(target);
                } else if (BehaviorTracker.EVENT_VIEW_PRODUCT.equals(type)) {
                    totalViewDurationMs += Math.max(0L, event.getDurationMs());
                    if (!target.isEmpty() && recentViewedProducts.size() < 10) {
                        recentViewedProducts.add(target);
                    }
                } else if (BehaviorTracker.EVENT_FILTER_CATEGORY.equals(type)
                        && !target.isEmpty()
                        && recentCategories.size() < 5) {
                    recentCategories.add(target);
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("username", username);
        summary.put("updatedAt", nowMillis);
        summary.put("totalEvents", events == null ? 0 : events.size());
        summary.put("totalProductViewMs", totalViewDurationMs);
        summary.put("recentSearches", new ArrayList<>(recentSearches));
        summary.put("recentViewedProducts", new ArrayList<>(recentViewedProducts));
        summary.put("recentCategories", new ArrayList<>(recentCategories));
        summary.put("eventCounts", eventCounts);
        return summary;
    }

    /**
     * Pure scoring function for deterministic ranking and unit testing.
     */
    @NonNull
    public static Map<String, Double> computeScores(
            @NonNull List<ProductWithRating> products,
            @Nullable List<UserEventEntity> events,
            @Nullable String gender,
            long nowMillis
    ) {
        Map<String, Double> scores = new HashMap<>();
        if (products.isEmpty()) {
            return scores;
        }

        Map<String, Product> productById = new HashMap<>();
        for (ProductWithRating item : products) {
            if (item != null && item.product != null) {
                productById.put(item.product.getId(), item.product);
            }
        }

        Map<String, Double> directProductBoost = new HashMap<>();
        Map<String, Double> brandAffinity = new HashMap<>();
        Map<String, Double> categoryAffinity = new HashMap<>();
        Map<String, Double> accessoryBrandCrossSell = new HashMap<>();
        List<WeightedQuery> searchQueries = new ArrayList<>();

        if (events != null) {
            for (UserEventEntity event : events) {
                if (event == null) {
                    continue;
                }
                double decay = timeDecay(nowMillis, event.getTimestamp());
                String type = event.getEventType();
                String targetId = safeLower(event.getTargetId());

                if (BehaviorTracker.EVENT_SEARCH.equals(type)) {
                    String qFolded = foldText(targetId);
                    if (!qFolded.isEmpty()) {
                        searchQueries.add(new WeightedQuery(qFolded, decay));
                    }
                } else if (BehaviorTracker.EVENT_FILTER_CATEGORY.equals(type)) {
                    if (!targetId.isEmpty() && !targetId.equals("tất cả") && !targetId.equals("all")) {
                        addScore(categoryAffinity, targetId, 10.0 * decay);
                    }
                } else if (BehaviorTracker.EVENT_VIEW_PRODUCT.equals(type)) {
                    double seconds = Math.min(MAX_DWELL_SECONDS, Math.max(0.0, event.getDurationMs() / 1000.0));
                    double dwellPoints = seconds * 0.35 * decay;
                    if (dwellPoints > 0.0 && !event.getTargetId().isEmpty()) {
                        addScore(directProductBoost, event.getTargetId(), dwellPoints);
                        Product viewed = productById.get(event.getTargetId());
                        if (viewed != null) {
                            addScore(brandAffinity, safeLower(viewed.getBrand()), dwellPoints * 0.4);
                            addScore(categoryAffinity, safeLower(viewed.getCategory()), dwellPoints * 0.4);
                        }
                    }
                } else if (BehaviorTracker.EVENT_ADD_TO_CART.equals(type)
                        || BehaviorTracker.EVENT_BUY_NOW.equals(type)) {
                    if (!event.getTargetId().isEmpty()) {
                        addScore(directProductBoost, event.getTargetId(), 18.0 * decay);
                        Product p = productById.get(event.getTargetId());
                        if (p != null) {
                            addScore(brandAffinity, safeLower(p.getBrand()), 8.0 * decay);
                            addScore(categoryAffinity, safeLower(p.getCategory()), 8.0 * decay);
                        }
                    }
                } else if (BehaviorTracker.EVENT_REMOVE_CART.equals(type)) {
                    if (!event.getTargetId().isEmpty()) {
                        addScore(directProductBoost, event.getTargetId(), -6.0 * decay);
                    }
                } else if (BehaviorTracker.EVENT_PURCHASE.equals(type)) {
                    for (String purchasedId : extractPurchasedProductIds(event.getDetail())) {
                        addScore(directProductBoost, purchasedId, -10.0 * decay);
                        Product bought = productById.get(purchasedId);
                        if (bought != null && !safeLower(bought.getBrand()).isEmpty()) {
                            addScore(accessoryBrandCrossSell, safeLower(bought.getBrand()), 10.0 * decay);
                        }
                    }
                }
            }
        }

        String normalizedGender = safeLower(gender);

        for (ProductWithRating item : products) {
            if (item == null || item.product == null) {
                continue;
            }
            Product p = item.product;
            String id = p.getId();
            String nameLower = safeLower(p.getName());
            String brandLower = safeLower(p.getBrand());
            String categoryLower = safeLower(p.getCategory());
            String infoLower = safeLower(p.getShortInfo());

            double score = 0.0;

            // 1. Search intent match (accent-insensitive so "chuot" matches "Chuột")
            String nameFolded = foldText(nameLower);
            String brandFolded = foldText(brandLower);
            String categoryFolded = foldText(categoryLower);
            String infoFolded = foldText(infoLower);
            for (WeightedQuery wq : searchQueries) {
                if (nameFolded.contains(wq.query) || brandFolded.contains(wq.query)) {
                    score += 25.0 * wq.weight;
                } else if (categoryFolded.contains(wq.query) || infoFolded.contains(wq.query)) {
                    score += 12.0 * wq.weight;
                }
            }

            // 2. Direct product dwell / cart / purchase adjustments
            score += directProductBoost.getOrDefault(id, 0.0);

            // 3. Brand, category, and post-purchase accessory cross-sell affinity
            score += brandAffinity.getOrDefault(brandLower, 0.0);
            score += categoryAffinity.getOrDefault(categoryLower, 0.0);
            if (categoryLower.contains("phụ kiện")) {
                score += accessoryBrandCrossSell.getOrDefault(brandLower, 0.0);
            }

            // 4. Cold-start gender hint (kept light so real user behavior always dominates)
            score += genderHintScore(normalizedGender, nameLower, categoryLower, infoLower);

            // 5. Product quality + stock availability
            score += (item.avgRating * 2.0) + (Math.min(item.reviewCount, 20) * 0.2);
            if (p.getStock() <= 0) {
                score -= 100.0;
            }

            scores.put(id, score);
        }

        return scores;
    }

    private static double genderHintScore(
            String normalizedGender,
            String nameLower,
            String categoryLower,
            String infoLower
    ) {
        if (normalizedGender.isEmpty()) {
            return 0.0;
        }
        String combined = nameLower + " " + categoryLower + " " + infoLower;
        if (normalizedGender.equals("nữ") || normalizedGender.equals("female")) {
            if (combined.contains("watch") || combined.contains("airpods") || combined.contains("ipad")) {
                return 8.0;
            }
            if (combined.contains("air") || combined.contains("phụ kiện")) {
                return 5.0;
            }
        } else if (normalizedGender.equals("nam") || normalizedGender.equals("male")) {
            if (combined.contains("bàn phím") || combined.contains("chuột") || combined.contains("màn hình")) {
                return 8.0;
            }
            if (combined.contains("pro") || combined.contains("laptop")) {
                return 5.0;
            }
        }
        return 0.0;
    }

    private static double timeDecay(long nowMillis, long eventTimestamp) {
        double daysAgo = Math.max(0.0, (nowMillis - eventTimestamp) / MILLIS_PER_DAY);
        return 1.0 / (1.0 + (daysAgo / HALF_LIFE_DAYS));
    }

    private static Set<String> extractPurchasedProductIds(@Nullable String detail) {
        Set<String> ids = new HashSet<>();
        if (detail == null || detail.isEmpty()) {
            return ids;
        }
        for (String part : detail.split("\\|")) {
            if (part.startsWith("products=")) {
                String raw = part.substring("products=".length());
                for (String id : raw.split(",")) {
                    String clean = id.trim();
                    if (!clean.isEmpty()) {
                        ids.add(clean);
                    }
                }
            }
        }
        return ids;
    }

    private static void addScore(Map<String, Double> map, String key, double delta) {
        if (key == null || key.isEmpty()) {
            return;
        }
        map.put(key, map.getOrDefault(key, 0.0) + delta);
    }

    private static String safeLower(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.getDefault());
    }

    @NonNull
    public static String foldText(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.getDefault()), Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                sb.append(c == 'đ' ? 'd' : c);
            }
        }
        return sb.toString();
    }

    private static final class WeightedQuery {
        final String query;
        final double weight;

        WeightedQuery(String query, double weight) {
            this.query = query;
            this.weight = weight;
        }
    }
}
