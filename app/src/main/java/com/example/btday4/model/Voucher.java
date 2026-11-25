package com.example.btday4.model;

import androidx.annotation.NonNull;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Locale;

/**
 * Domain & Firestore model representing a promotional voucher (percentage, fixed VND, or freeship).
 * Designed to be authored and managed directly from Firebase Console (`vouchers` collection).
 */
@IgnoreExtraProperties
public class Voucher {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_FREESHIP = "FREESHIP";

    @Exclude
    private String docId = "";

    @NonNull
    private String code = "";
    private String title = "";
    private String description = "";
    private String discountType = TYPE_FIXED;
    private long discountValue = 0L;
    private long maxDiscountAmount = 0L;
    private long minOrderAmount = 0L;
    private long expiryMillis = 0L;
    private String badgeLabel = "";
    private boolean secretOnly = false;
    private boolean active = true;
    private int usageLimit = 0;
    private int usedCount = 0;

    public Voucher() {
    }

    public Voucher(
            @NonNull String code,
            String title,
            String description,
            String discountType,
            long discountValue,
            long maxDiscountAmount,
            long minOrderAmount,
            long expiryMillis,
            String badgeLabel,
            boolean secretOnly
    ) {
        this(
                code,
                title,
                description,
                discountType,
                discountValue,
                maxDiscountAmount,
                minOrderAmount,
                expiryMillis,
                badgeLabel,
                secretOnly,
                true,
                0,
                0
        );
    }

    public Voucher(
            @NonNull String code,
            String title,
            String description,
            String discountType,
            long discountValue,
            long maxDiscountAmount,
            long minOrderAmount,
            long expiryMillis,
            String badgeLabel,
            boolean secretOnly,
            boolean active,
            int usageLimit,
            int usedCount
    ) {
        this.code = code == null ? "" : code.trim().toUpperCase(Locale.US);
        this.docId = this.code;
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
        this.discountType = discountType == null || discountType.trim().isEmpty()
                ? TYPE_FIXED
                : discountType.trim().toUpperCase(Locale.US);
        this.discountValue = Math.max(0L, discountValue);
        this.maxDiscountAmount = Math.max(0L, maxDiscountAmount);
        this.minOrderAmount = Math.max(0L, minOrderAmount);
        this.expiryMillis = Math.max(0L, expiryMillis);
        this.badgeLabel = badgeLabel == null ? "" : badgeLabel;
        this.secretOnly = secretOnly;
        this.active = active;
        this.usageLimit = Math.max(0, usageLimit);
        this.usedCount = Math.max(0, usedCount);
    }

    @Exclude
    @NonNull
    public String getDocId() {
        return (docId == null || docId.trim().isEmpty()) ? getCode() : docId.trim();
    }

    @Exclude
    public void setDocId(String docId) {
        this.docId = docId == null ? "" : docId.trim();
    }

    @NonNull
    public String getCode() {
        return code;
    }

    public void setCode(@NonNull String code) {
        this.code = code == null ? "" : code.trim().toUpperCase(Locale.US);
    }

    public String getTitle() {
        if (title == null || title.trim().isEmpty()) {
            return code.isEmpty() ? "Mã giảm giá" : "Mã ưu đãi " + code;
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String getDiscountType() {
        return discountType == null || discountType.trim().isEmpty()
                ? TYPE_FIXED
                : discountType.trim().toUpperCase(Locale.US);
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType == null || discountType.trim().isEmpty()
                ? TYPE_FIXED
                : discountType.trim().toUpperCase(Locale.US);
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(long discountValue) {
        this.discountValue = Math.max(0L, discountValue);
    }

    public long getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(long maxDiscountAmount) {
        this.maxDiscountAmount = Math.max(0L, maxDiscountAmount);
    }

    public long getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(long minOrderAmount) {
        this.minOrderAmount = Math.max(0L, minOrderAmount);
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public void setExpiryMillis(long expiryMillis) {
        this.expiryMillis = Math.max(0L, expiryMillis);
    }

    public String getBadgeLabel() {
        if (badgeLabel != null && !badgeLabel.trim().isEmpty()) {
            return badgeLabel;
        }
        if (isFreeShipType()) {
            return "FREE\nSHIP";
        }
        if (TYPE_PERCENT.equalsIgnoreCase(getDiscountType())) {
            return "-" + Math.min(100L, discountValue) + "%";
        }
        if (discountValue >= 1_000L && discountValue % 1_000L == 0L) {
            return "-" + (discountValue / 1_000L) + "K";
        }
        return "-" + FormatUtils.formatVnd(discountValue);
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel == null ? "" : badgeLabel;
    }

    public boolean isSecretOnly() {
        return secretOnly;
    }

    public void setSecretOnly(boolean secretOnly) {
        this.secretOnly = secretOnly;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = Math.max(0, usageLimit);
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = Math.max(0, usedCount);
    }

    @Exclude
    public boolean isFreeShipType() {
        return TYPE_FREESHIP.equalsIgnoreCase(getDiscountType());
    }

    @Exclude
    public boolean isExpired(long nowMillis) {
        return expiryMillis > 0L && nowMillis > expiryMillis;
    }

    @Exclude
    public boolean hasRemainingUses() {
        return usageLimit <= 0 || usedCount < usageLimit;
    }

    @Exclude
    public int getRemainingUses() {
        return usageLimit <= 0 ? -1 : Math.max(0, usageLimit - usedCount);
    }

    @Exclude
    public long amountNeededToUnlock(long subtotal) {
        return Math.max(0L, minOrderAmount - Math.max(0L, subtotal));
    }

    /**
     * Computes actual VND discount applied to an order while preserving PayOS's minimum payable floor.
     */
    @Exclude
    public long computeDiscount(long subtotal, long baseShippingFee) {
        if (subtotal <= 0L || subtotal < minOrderAmount) {
            return 0L;
        }
        if (isFreeShipType()) {
            return Math.max(0L, baseShippingFee);
        }
        long rawDiscount;
        if (TYPE_PERCENT.equalsIgnoreCase(getDiscountType())) {
            rawDiscount = Math.round(subtotal * (Math.min(100L, discountValue) / 100.0));
            if (maxDiscountAmount > 0L) {
                rawDiscount = Math.min(rawDiscount, maxDiscountAmount);
            }
        } else {
            rawDiscount = Math.max(0L, discountValue);
        }
        long maxAllowed = Math.max(0L, subtotal + Math.max(0L, baseShippingFee) - Constants.MIN_PAYABLE_TOTAL);
        return Math.max(0L, Math.min(rawDiscount, maxAllowed));
    }

    @Exclude
    public boolean isApplicable(long subtotal, long baseShippingFee, long nowMillis) {
        return active
                && !isExpired(nowMillis)
                && hasRemainingUses()
                && computeDiscount(subtotal, baseShippingFee) > 0L;
    }
}
