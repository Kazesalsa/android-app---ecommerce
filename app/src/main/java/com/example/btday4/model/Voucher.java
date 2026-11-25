package com.example.btday4.model;

import androidx.annotation.NonNull;
import com.example.btday4.util.Constants;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Locale;

/**
 * Domain & Firestore model representing a promotional voucher (percentage, fixed VND, or freeship).
 */
@IgnoreExtraProperties
public class Voucher {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_FREESHIP = "FREESHIP";

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
        this.code = code.trim().toUpperCase(Locale.US);
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.expiryMillis = expiryMillis;
        this.badgeLabel = badgeLabel;
        this.secretOnly = secretOnly;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    public void setCode(@NonNull String code) {
        this.code = code == null ? "" : code.trim().toUpperCase(Locale.US);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(long discountValue) {
        this.discountValue = discountValue;
    }

    public long getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(long maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public long getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(long minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }

    public void setExpiryMillis(long expiryMillis) {
        this.expiryMillis = expiryMillis;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    public boolean isSecretOnly() {
        return secretOnly;
    }

    public void setSecretOnly(boolean secretOnly) {
        this.secretOnly = secretOnly;
    }

    public boolean isFreeShipType() {
        return TYPE_FREESHIP.equalsIgnoreCase(discountType);
    }

    public boolean isExpired(long nowMillis) {
        return expiryMillis > 0L && nowMillis > expiryMillis;
    }

    public long amountNeededToUnlock(long subtotal) {
        return Math.max(0L, minOrderAmount - Math.max(0L, subtotal));
    }

    /**
     * Computes actual VND discount applied to an order while preserving PayOS's minimum payable floor.
     */
    public long computeDiscount(long subtotal, long baseShippingFee) {
        if (subtotal <= 0L || subtotal < minOrderAmount) {
            return 0L;
        }
        if (isFreeShipType()) {
            return Math.max(0L, baseShippingFee);
        }
        long rawDiscount;
        if (TYPE_PERCENT.equalsIgnoreCase(discountType)) {
            rawDiscount = Math.round(subtotal * (discountValue / 100.0));
            if (maxDiscountAmount > 0L) {
                rawDiscount = Math.min(rawDiscount, maxDiscountAmount);
            }
        } else {
            rawDiscount = Math.max(0L, discountValue);
        }
        long maxAllowed = Math.max(0L, subtotal + Math.max(0L, baseShippingFee) - Constants.MIN_PAYABLE_TOTAL);
        return Math.max(0L, Math.min(rawDiscount, maxAllowed));
    }

    public boolean isApplicable(long subtotal, long baseShippingFee, long nowMillis) {
        return !isExpired(nowMillis) && computeDiscount(subtotal, baseShippingFee) > 0L;
    }
}
