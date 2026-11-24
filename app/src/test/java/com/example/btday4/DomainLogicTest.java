package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.model.RatingSummary;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import java.util.Arrays;
import java.util.Collections;
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
}
