package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.model.NotificationItem;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class NotificationRepositoryTest {

    @Test
    public void createSeedNotificationsReturnsWelcomePromoAndFreeship() {
        long now = 1_700_000_000_000L;
        List<NotificationItem> seeded = NotificationRepository.createSeedNotifications(now);

        assertEquals(2, seeded.size());

        NotificationItem promo = seeded.get(0);
        assertEquals("promo_welcome_10", promo.getId());
        assertEquals(NotificationItem.Type.PROMO, promo.getType());
        assertEquals("TECH10", promo.getPromoCode());
        assertFalse(promo.isRead());
        assertTrue(promo.getTimestamp() < now);

        NotificationItem freeship = seeded.get(1);
        assertEquals("promo_freeship", freeship.getId());
        assertEquals(NotificationItem.Type.PROMO, freeship.getType());
        assertEquals("", freeship.getPromoCode());
        assertFalse(freeship.isRead());
    }

    @Test
    public void firestoreMapSerializationRoundTripsAllFieldsAndHandlesMissingDefaults() {
        NotificationItem original = new NotificationItem(
                "n1",
                NotificationItem.Type.PROMO,
                "Tặng mã TECH10",
                "Giảm 10%",
                1_700_000_002_000L,
                "order_1",
                "TECH10",
                true
        );

        Map<String, Object> map = NotificationRepository.toFirestoreMap(original);
        NotificationItem parsed = NotificationRepository.fromFirestoreMap("fallback_id", map);

        assertEquals("n1", parsed.getId());
        assertEquals(NotificationItem.Type.PROMO, parsed.getType());
        assertEquals("Tặng mã TECH10", parsed.getTitle());
        assertEquals("Giảm 10%", parsed.getMessage());
        assertEquals(1_700_000_002_000L, parsed.getTimestamp());
        assertEquals("order_1", parsed.getTargetId());
        assertEquals("TECH10", parsed.getPromoCode());
        assertTrue(parsed.isRead());

        NotificationItem fallback = NotificationRepository.fromFirestoreMap("doc_123", null);
        assertEquals("doc_123", fallback.getId());
        assertEquals(NotificationItem.Type.ORDER, fallback.getType());
        assertFalse(fallback.isRead());

        map.put("id", "");
        map.put("type", "UNKNOWN_TYPE");
        NotificationItem unknownType = NotificationRepository.fromFirestoreMap("doc_999", map);
        assertEquals("doc_999", unknownType.getId());
        assertEquals(NotificationItem.Type.ORDER, unknownType.getType());
    }
}
