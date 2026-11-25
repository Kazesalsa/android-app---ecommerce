package com.example.btday4.data.local;

import com.example.btday4.util.FormatUtils;
import com.google.firebase.firestore.Exclude;
import java.util.Locale;

/**
 * Firestore POJO representing a reusable discount voucher stored in `vouchers/{code}`.
 * Can be created or updated directly in Firebase Console without app changes.
 */
public class VoucherEntity {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";

    private String docId = "";
    private String code = "";
    private String title = "";
    private String description = "";
    private String discountType = TYPE_FIXED;
    private long discountValue = 0L;
    private long maxDiscount = 0L;
    private long minOrderAmount = 0L;
    private int usageLimit = 0;
    private int usedCount = 0;
    private boolean active = true;
    private long expiresAt = 0L;

    public VoucherEntity() {
    }

    public VoucherEntity(
            String code,
            String title,
            String description,
            String discountType,
            long discountValue,
            long maxDiscount,
            long minOrderAmount,
            int usageLimit,
            int usedCount,
            boolean active,
            long expiresAt
    ) {
        this.code = normalizeCode(code);
        this.docId = this.code;
        this.title = title == null ? "" : title.trim();
        this.description = description == null ? "" : description.trim();
        this.discountType = discountType == null ? TYPE_FIXED : discountType.trim().toUpperCase(Locale.US);
        this.discountValue = Math.max(0L, discountValue);
        this.maxDiscount = Math.max(0L, maxDiscount);
        this.minOrderAmount = Math.max(0L, minOrderAmount);
        this.usageLimit = Math.max(0, usageLimit);
        this.usedCount = Math.max(0, usedCount);
        this.active = active;
        this.expiresAt = Math.max(0L, expiresAt);
    }

    private static String normalizeCode(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.US);
    }

    @Exclude
    public String getDocId() {
        return (docId == null || docId.isEmpty()) ? getCode() : docId;
    }

    @Exclude
    public void setDocId(String docId) {
        this.docId = docId == null ? "" : docId.trim();
    }

    public String getCode() {
        return normalizeCode(code);
    }

    public void setCode(String code) {
        this.code = normalizeCode(code);
    }

    public String getTitle() {
        return title == null ? "" : title;
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
        return discountType == null || discountType.isEmpty()
                ? TYPE_FIXED
                : discountType.trim().toUpperCase(Locale.US);
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType == null ? TYPE_FIXED : discountType.trim().toUpperCase(Locale.US);
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(long discountValue) {
        this.discountValue = Math.max(0L, discountValue);
    }

    public long getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(long maxDiscount) {
        this.maxDiscount = Math.max(0L, maxDiscount);
    }

    public long getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(long minOrderAmount) {
        this.minOrderAmount = Math.max(0L, minOrderAmount);
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = Math.max(0L, expiresAt);
    }

    @Exclude
    public boolean isPercent() {
        return TYPE_PERCENT.equalsIgnoreCase(getDiscountType());
    }

    @Exclude
    public boolean isExpired(long nowMillis) {
        return expiresAt > 0L && nowMillis > expiresAt;
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
    public boolean isAvailable(long nowMillis) {
        return active && !isExpired(nowMillis) && hasRemainingUses();
    }

    @Exclude
    public long computeDiscount(long subtotal) {
        if (subtotal <= 0L || subtotal < minOrderAmount || discountValue <= 0L) {
            return 0L;
        }
        if (isPercent()) {
            long cappedPct = Math.min(100L, discountValue);
            long raw = Math.round(subtotal * (cappedPct / 100.0));
            if (maxDiscount > 0L) {
                raw = Math.min(raw, maxDiscount);
            }
            return Math.min(raw, subtotal);
        }
        return Math.min(discountValue, subtotal);
    }

    /**
     * Returns null when eligible for checkout, or a clear Vietnamese reason when ineligible.
     */
    @Exclude
    public String validateForOrder(long subtotal, long nowMillis) {
        if (!active) {
            return "Mã giảm giá hiện đang tạm ngưng";
        }
        if (isExpired(nowMillis)) {
            return "Mã giảm giá đã hết hạn sử dụng";
        }
        if (!hasRemainingUses()) {
            return "Mã giảm giá đã hết lượt sử dụng";
        }
        if (subtotal < minOrderAmount) {
            return "Đơn hàng tối thiểu " + FormatUtils.formatVnd(minOrderAmount) + " để dùng mã này";
        }
        if (computeDiscount(subtotal) <= 0L) {
            return "Mã giảm giá không áp dụng được cho đơn hàng này";
        }
        return null;
    }

    @Exclude
    public String formatShortBadge() {
        if (isPercent()) {
            return "-" + Math.min(100L, discountValue) + "%";
        }
        if (discountValue >= 1_000L && discountValue % 1_000L == 0L) {
            return "-" + (discountValue / 1_000L) + "K";
        }
        return "-" + FormatUtils.formatVnd(discountValue);
    }
}
