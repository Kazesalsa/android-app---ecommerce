package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.local.UserEntity;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.model.RatingSummary;
import com.example.btday4.model.Voucher;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DomainLogicTest {

    @Test
    public void ratingSummaryComputesAverageAndDistributionAccurately() {
        RatingSummary empty = RatingSummary.fromReviews(Collections.emptyList());
        assertEquals(0, empty.getTotalCount());
        assertEquals(0.0, empty.getAverageRating(), 0.001);
        assertEquals(0, empty.getPercentForStar(5));

        ReviewEntity r1 = new ReviewEntity("1", "p1", "u1", "A", 5, "Good", 1L);
        ReviewEntity r2 = new ReviewEntity("2", "p1", "u2", "B", 5, "Great", 2L);
        ReviewEntity r3 = new ReviewEntity("3", "p1", "u3", "C", 4, "Nice", 3L);
        ReviewEntity r4 = new ReviewEntity("4", "p1", "u4", "D", 2, "Meh", 4L);

        RatingSummary summary = RatingSummary.fromReviews(Arrays.asList(r1, r2, r3, r4));
        assertEquals(4, summary.getTotalCount());
        assertEquals(4.0, summary.getAverageRating(), 0.001);
        assertEquals(2, summary.getCountForStar(5));
        assertEquals(50, summary.getPercentForStar(5));
        assertEquals(1, summary.getCountForStar(4));
        assertEquals(25, summary.getPercentForStar(4));
        assertEquals(1, summary.getCountForStar(2));
        assertEquals(0, summary.getCountForStar(3));
    }

    @Test
    public void formatUtilsComputesShippingDiscountAndInitials() {
        assertEquals(Constants.STANDARD_SHIPPING_FEE, FormatUtils.computeShippingFee(21_900L));
        assertEquals(0L, FormatUtils.computeShippingFee(50_000L));
        assertEquals(0L, FormatUtils.computeShippingFee(199_900L));

        assertEquals(13, FormatUtils.computeDiscountPercent("199.900 ₫", "229.900 ₫"));
        assertEquals(0, FormatUtils.computeDiscountPercent("229.900 ₫", "199.900 ₫"));

        assertEquals("NT", FormatUtils.extractInitials("Nguyễn Tấn Tài"));
        assertEquals("AD", FormatUtils.extractInitials("Admin"));
    }

    @Test
    public void validationUtilsChecksPhoneEmailAndPassword() {
        assertTrue(ValidationUtils.isValidPhone("0912345678"));
        assertTrue(ValidationUtils.isValidPhone("0912 345 678"));
        assertTrue(ValidationUtils.isValidPhone("+84912345678"));
        assertFalse(ValidationUtils.isValidPhone("12345"));
        assertFalse(ValidationUtils.isValidPhone("abcdefghij"));

        assertTrue(ValidationUtils.isValidEmail("tantai@techstore.vn"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));

        assertTrue(ValidationUtils.isValidPassword("123456"));
        assertFalse(ValidationUtils.isValidPassword("12345"));

        assertTrue(ValidationUtils.isValidBirthDate(29, 2, 2000, 2026));
        assertFalse(ValidationUtils.isValidBirthDate(29, 2, 2001, 2026));
        assertTrue(ValidationUtils.isValidBirthDate(15, 8, 2003, 2026));
        assertFalse(ValidationUtils.isValidBirthDate(31, 4, 2003, 2026));
        assertFalse(ValidationUtils.isValidBirthDate(1, 1, 2030, 2026));
    }

    @Test
    public void voucherComputesPercentFixedFreeshipAndPayOsFloorSafely() {
        long future = System.currentTimeMillis() + 86_400_000L;

        Voucher percentCapped = new Voucher(
                "technew15",
                "Giảm 15%",
                "Tối đa 25K cho đơn từ 50K",
                Voucher.TYPE_PERCENT,
                15L,
                25_000L,
                50_000L,
                future,
                "-15%",
                false
        );
        assertEquals("TECHNEW15", percentCapped.getCode());
        assertEquals(0L, percentCapped.computeDiscount(49_900L, 300L));
        assertEquals(100L, percentCapped.amountNeededToUnlock(49_900L));
        assertEquals(15_000L, percentCapped.computeDiscount(100_000L, 0L));
        assertEquals(25_000L, percentCapped.computeDiscount(200_000L, 0L));

        Voucher freeship = new Voucher(
                "FREESHIP",
                "Miễn phí ship",
                "Đơn từ 20K",
                Voucher.TYPE_FREESHIP,
                100L,
                0L,
                20_000L,
                future,
                "FREESHIP",
                false
        );
        assertEquals(300L, freeship.computeDiscount(30_000L, 300L));
        assertEquals(0L, freeship.computeDiscount(60_000L, 0L));

        // PayOS minimum payable floor (2.000 ₫) must never be violated by huge fixed discounts.
        Voucher hugeFixed = new Voucher(
                "HUGE50K",
                "Giảm 50K",
                "Đơn từ 10K",
                Voucher.TYPE_FIXED,
                50_000L,
                0L,
                10_000L,
                future,
                "-50K",
                false
        );
        long discountOnSmallOrder = hugeFixed.computeDiscount(20_000L, 300L);
        assertEquals(20_300L - Constants.MIN_PAYABLE_TOTAL, discountOnSmallOrder);
        assertEquals(Constants.MIN_PAYABLE_TOTAL, 20_000L + 300L - discountOnSmallOrder);
    }

    @Test
    public void voucherRepositoryPicksBestClaimedUnusedNonExpiredVoucher() {
        long now = 1_700_000_000_000L;
        Voucher v5k = new Voucher("FLASH5K", "5K", "", Voucher.TYPE_FIXED, 5_000L, 0L, 20_000L, now + 10_000L, "-5K", false);
        Voucher v25k = new Voucher("TECHNEW15", "15%", "", Voucher.TYPE_PERCENT, 15L, 25_000L, 50_000L, now + 10_000L, "-15%", false);
        Voucher v50kUnclaimed = new Voucher("BIG50K", "50K", "", Voucher.TYPE_FIXED, 50_000L, 0L, 50_000L, now + 10_000L, "-50K", false);
        Voucher v40kUsed = new Voucher("USED40K", "40K", "", Voucher.TYPE_FIXED, 40_000L, 0L, 50_000L, now + 10_000L, "-40K", false);
        Voucher v30kExpired = new Voucher("OLD30K", "30K", "", Voucher.TYPE_FIXED, 30_000L, 0L, 50_000L, now - 1L, "-30K", false);

        List<VoucherItem> wallet = Arrays.asList(
                new VoucherItem(v5k, true, false),
                new VoucherItem(v25k, true, false),
                new VoucherItem(v50kUnclaimed, false, false),
                new VoucherItem(v40kUsed, true, true),
                new VoucherItem(v30kExpired, true, false)
        );

        Voucher bestFor120k = VoucherRepository.findBestApplicable(wallet, 120_000L, 0L, now);
        assertNotNull(bestFor120k);
        assertEquals("TECHNEW15", bestFor120k.getCode());

        Voucher bestFor30k = VoucherRepository.findBestApplicable(wallet, 30_000L, 300L, now);
        assertNotNull(bestFor30k);
        assertEquals("FLASH5K", bestFor30k.getCode());

        assertNull(VoucherRepository.findBestApplicable(wallet, 10_000L, 300L, now));
    }

    @Test
    public void userAvatarAndOrderVoucherEntitiesPreserveFields() {
        UserEntity user = new UserEntity(
                "tantai",
                "Nguyễn Tấn Tài",
                "secret123",
                "0912345678",
                "TP.HCM",
                "25/11/2026 16:00",
                "tantai@techstore.vn",
                "Nam",
                "15/08/2003",
                "preset:2"
        );
        assertEquals("preset:2", user.getAvatarUrl());

        OrderEntity order = new OrderEntity(
                "DH-20261125-0001",
                "tantai",
                "Nguyễn Tấn Tài",
                "0912345678",
                "Quận 1, TP.HCM",
                "",
                "VIETQR",
                100_000L,
                0L,
                85_000L,
                "TECHNEW15",
                15_000L,
                "PENDING_PAYMENT",
                123456L
        );
        assertEquals("TECHNEW15", order.getVoucherCode());
        assertEquals(15_000L, order.getVoucherDiscount());
        assertEquals(85_000L, order.getTotalAmount());

        // Legacy PENDING_CONFIRMATION maps cleanly to PROCESSING (Đang giao hàng)
        assertEquals(
                com.example.btday4.model.OrderStatus.PROCESSING,
                com.example.btday4.model.OrderStatus.fromCode("PENDING_CONFIRMATION")
        );
        assertEquals("Đang giao hàng", com.example.btday4.model.OrderStatus.PROCESSING.getLabel());

        com.example.btday4.model.AppNotification notif = new com.example.btday4.model.AppNotification(
                "n1",
                com.example.btday4.model.AppNotification.TYPE_ORDER,
                "Đặt hàng thành công",
                "Đơn DH-20261125-0001 đang được giao",
                "DH-20261125-0001",
                123456L,
                false
        );
        assertFalse(notif.isRead());
        notif.setRead(true);
        assertTrue(notif.isRead());
    }
}
