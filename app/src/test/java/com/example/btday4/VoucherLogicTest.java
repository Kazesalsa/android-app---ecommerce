package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.local.VoucherEntity;
import com.example.btday4.data.repository.VoucherRepository;
import java.util.List;
import org.junit.Test;

public class VoucherLogicTest {

    @Test
    public void percentVoucherComputesDiscountWithAndWithoutCap() {
        VoucherEntity capped = new VoucherEntity(
                "TECH10",
                "Giảm 10%",
                "",
                VoucherEntity.TYPE_PERCENT,
                10L,
                50_000L,
                50_000L,
                100,
                0,
                true,
                0L
        );
        // 10% of 200.000 = 20.000 (below 50.000 cap)
        assertEquals(20_000L, capped.computeDiscount(200_000L));
        // 10% of 800.000 = 80.000 -> capped at 50.000
        assertEquals(50_000L, capped.computeDiscount(800_000L));
        // Below minOrderAmount (50.000) -> 0
        assertEquals(0L, capped.computeDiscount(40_000L));

        VoucherEntity uncapped = new VoucherEntity(
                "VIP20",
                "Giảm 20%",
                "",
                VoucherEntity.TYPE_PERCENT,
                20L,
                0L,
                0L,
                0,
                0,
                true,
                0L
        );
        assertEquals(60_000L, uncapped.computeDiscount(300_000L));
    }

    @Test
    public void fixedVoucherClampsToSubtotalAndValidatesStates() {
        long now = 1_700_000_000_000L;
        VoucherEntity fixed = new VoucherEntity(
                "GIAM20K",
                "Giảm 20K",
                "",
                VoucherEntity.TYPE_FIXED,
                20_000L,
                0L,
                10_000L,
                5,
                2,
                true,
                now + 60_000L
        );

        // Reusable as long as usedCount < usageLimit
        assertNull(fixed.validateForOrder(50_000L, now));
        assertEquals(20_000L, fixed.computeDiscount(50_000L));
        // Clamped when subtotal < discountValue
        assertEquals(15_000L, fixed.computeDiscount(15_000L));
        assertEquals(3, fixed.getRemainingUses());

        // Below minimum order
        assertNotNull(fixed.validateForOrder(5_000L, now));

        // Exhausted
        fixed.setUsedCount(5);
        assertFalse(fixed.hasRemainingUses());
        assertNotNull(fixed.validateForOrder(50_000L, now));

        // Unlimited (usageLimit == 0)
        fixed.setUsageLimit(0);
        assertTrue(fixed.hasRemainingUses());
        assertNull(fixed.validateForOrder(50_000L, now));

        // Expired
        assertNotNull(fixed.validateForOrder(50_000L, now + 120_000L));

        // Inactive
        fixed.setActive(false);
        assertNotNull(fixed.validateForOrder(50_000L, now));
    }

    @Test
    public void defaultVouchersMatchDemoStoreScale() {
        List<VoucherEntity> defaults = VoucherRepository.createDefaultVouchers();
        assertEquals(3, defaults.size());
        long now = System.currentTimeMillis();
        for (VoucherEntity v : defaults) {
            assertTrue(v.isAvailable(now));
            assertFalse(v.getCode().isEmpty());
        }
    }
}
