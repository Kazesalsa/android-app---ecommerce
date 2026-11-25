package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.local.Product;
import com.example.btday4.model.ProductWithRating;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.tracking.RecommendationEngine;
import com.example.btday4.tracking.UserEventEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RecommendationEngineTest {

    private static final long NOW = 1_760_000_000_000L;
    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    @Test
    public void searchKeywordBoostsMatchingProductToTop() {
        ProductWithRating iphone = item("iphone_15", "iPhone 15", "Điện thoại", "Apple", "Chip A16", 10, 4.5, 5);
        ProductWithRating keychron = item("keychron_k8", "Bàn phím cơ Keychron K8", "Phụ kiện", "Keychron", "Gateron", 10, 4.5, 5);

        UserEventEntity searchEvent = event(BehaviorTracker.EVENT_SEARCH, "keychron", "results=1", 0L, NOW - 10_000L);
        Map<String, Double> scores = RecommendationEngine.computeScores(
                Arrays.asList(iphone, keychron),
                Collections.singletonList(searchEvent),
                "",
                NOW
        );

        assertTrue(scores.get("keychron_k8") > scores.get("iphone_15"));
    }

    @Test
    public void longerViewDwellAndRecencyWinOverShortOrOldView() {
        ProductWithRating laptop = item("macbook_air", "MacBook Air M3", "Laptop", "Apple", "M3", 10, 4.5, 5);
        ProductWithRating mouse = item("mx_master", "Chuột Logitech MX Master", "Phụ kiện", "Logitech", "8000 DPI", 10, 4.5, 5);

        UserEventEntity recentLongView = event(BehaviorTracker.EVENT_VIEW_PRODUCT, "macbook_air", "", 45_000L, NOW - 60_000L);
        UserEventEntity oldShortView = event(BehaviorTracker.EVENT_VIEW_PRODUCT, "mx_master", "", 3_000L, NOW - (30 * ONE_DAY_MS));

        Map<String, Double> scores = RecommendationEngine.computeScores(
                Arrays.asList(laptop, mouse),
                Arrays.asList(recentLongView, oldShortView),
                "",
                NOW
        );

        assertTrue(scores.get("macbook_air") > scores.get("mx_master"));
    }

    @Test
    public void outOfStockProductIsPushedToBottomEvenWhenSearched() {
        ProductWithRating outOfStock = item("iphone_15", "iPhone 15", "Điện thoại", "Apple", "A16", 0, 5.0, 20);
        ProductWithRating inStock = item("airpods_2", "AirPods Pro 2", "Phụ kiện", "Apple", "ANC", 5, 4.0, 2);

        UserEventEntity search = event(BehaviorTracker.EVENT_SEARCH, "iphone", "results=1", 0L, NOW - 1_000L);
        Map<String, Double> scores = RecommendationEngine.computeScores(
                Arrays.asList(outOfStock, inStock),
                Collections.singletonList(search),
                "",
                NOW
        );

        assertTrue(scores.get("airpods_2") > scores.get("iphone_15"));
    }

    @Test
    public void genderProvidesColdStartHintWhenNoHistory() {
        ProductWithRating watch = item("apple_watch", "Apple Watch Series 10", "Phụ kiện", "Apple", "OLED", 10, 4.5, 5);
        ProductWithRating keyboard = item("keychron_k8", "Bàn phím cơ Keychron K8", "Phụ kiện", "Keychron", "TKL", 10, 4.5, 5);

        Map<String, Double> femaleScores = RecommendationEngine.computeScores(
                Arrays.asList(watch, keyboard),
                Collections.emptyList(),
                "Nữ",
                NOW
        );
        Map<String, Double> maleScores = RecommendationEngine.computeScores(
                Arrays.asList(watch, keyboard),
                Collections.emptyList(),
                "Nam",
                NOW
        );

        assertTrue(femaleScores.get("apple_watch") > femaleScores.get("keychron_k8"));
        assertTrue(maleScores.get("keychron_k8") > maleScores.get("apple_watch"));
    }

    @Test
    public void purchaseCrossSellsSameBrandAccessories() {
        ProductWithRating iphone = item("iphone_15", "iPhone 15", "Điện thoại", "Apple", "A16", 10, 4.5, 5);
        ProductWithRating airpods = item("airpods_2", "AirPods Pro 2", "Phụ kiện", "Apple", "ANC", 10, 4.5, 5);

        UserEventEntity purchase = event(
                BehaviorTracker.EVENT_PURCHASE,
                "ORD-1",
                "method=COD|total=1000|items=1|products=iphone_15",
                0L,
                NOW - 5_000L
        );

        Map<String, Double> scores = RecommendationEngine.computeScores(
                Arrays.asList(iphone, airpods),
                Collections.singletonList(purchase),
                "",
                NOW
        );

        assertTrue(scores.get("airpods_2") > scores.get("iphone_15"));
        assertEquals(2, scores.size());
    }

    @Test
    public void sessionCacheReusesScoresWithinTwelveHoursAndExpiresAfter() {
        RecommendationEngine.clearCache();
        Map<String, Double> computed = Collections.singletonMap("iphone_15", 42.0);
        RecommendationEngine.cacheScores("tester", "Nam", computed, NOW);

        Map<String, Double> withinWindow = RecommendationEngine.getCachedScores(
                "tester", "Nam", NOW + RecommendationEngine.CACHE_TTL_MS - 1_000L
        );
        assertNotNull(withinWindow);
        assertEquals(42.0, withinWindow.get("iphone_15"), 0.001);

        assertNull(RecommendationEngine.getCachedScores(
                "tester", "Nam", NOW + RecommendationEngine.CACHE_TTL_MS
        ));
        assertNull(RecommendationEngine.getCachedScores("other_user", "Nam", NOW + 1_000L));
    }

    @Test
    public void clearCacheForcesRecomputeAfterLogout() {
        RecommendationEngine.clearCache();
        RecommendationEngine.cacheScores("tester", "", Collections.singletonMap("keychron_k8", 30.0), NOW);
        assertNotNull(RecommendationEngine.getCachedScores("tester", "", NOW + 5_000L));

        RecommendationEngine.clearCache();
        assertNull(RecommendationEngine.getCachedScores("tester", "", NOW + 5_000L));
    }

    @Test
    public void buildTrackingSummaryAggregatesCountsAndTopSignals() {
        List<UserEventEntity> events = Arrays.asList(
                event(BehaviorTracker.EVENT_SEARCH, "macbook", "results=1", 0L, NOW - 10_000L),
                event(BehaviorTracker.EVENT_VIEW_PRODUCT, "macbook_air_m3", "", 15_000L, NOW - 8_000L),
                event(BehaviorTracker.EVENT_FILTER_CATEGORY, "Laptop", "", 0L, NOW - 5_000L)
        );
        Map<String, Object> summary = RecommendationEngine.buildTrackingSummary("tester", events, NOW);

        assertEquals("tester", summary.get("username"));
        assertEquals(3, summary.get("totalEvents"));
        assertEquals(15_000L, summary.get("totalProductViewMs"));
        assertEquals(Collections.singletonList("macbook"), summary.get("recentSearches"));
        assertEquals(Collections.singletonList("macbook_air_m3"), summary.get("recentViewedProducts"));
        assertEquals(Collections.singletonList("Laptop"), summary.get("recentCategories"));
    }

    @Test
    public void accentInsensitiveVietnameseSearchBoostsMatchingProduct() {
        ProductWithRating iphone = item("iphone_15", "iPhone 15", "Điện thoại", "Apple", "Chip A16", 10, 4.8, 10);
        ProductWithRating mouse = item("mx_master", "Chuột Logitech MX Master 3S", "Phụ kiện", "Logitech", "8000 DPI", 10, 4.5, 5);

        UserEventEntity searchEvent = event(BehaviorTracker.EVENT_SEARCH, "chuot", "results=1", 0L, NOW - 5_000L);
        Map<String, Double> scores = RecommendationEngine.computeScores(
                Arrays.asList(iphone, mouse),
                Collections.singletonList(searchEvent),
                "",
                NOW
        );

        assertTrue(scores.get("mx_master") > scores.get("iphone_15"));
    }

    private static ProductWithRating item(
            String id,
            String name,
            String category,
            String brand,
            String shortInfo,
            int stock,
            double rating,
            int reviewCount
    ) {
        ProductWithRating pwr = new ProductWithRating();
        pwr.product = new Product(id, name, "10.000 ₫", "12.000 ₫", shortInfo, category, "", brand, "", "", "", stock);
        pwr.avgRating = rating;
        pwr.reviewCount = reviewCount;
        return pwr;
    }

    private static UserEventEntity event(String type, String targetId, String detail, long durationMs, long timestamp) {
        return new UserEventEntity("sess-1", "tester", "TestScreen", type, targetId, detail, durationMs, timestamp);
    }
}
