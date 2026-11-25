package com.example.btday4.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pure formatting helpers for VND prices, discounts, timestamps, and user initials.
 * Uses ThreadLocal formatters to avoid repeated allocations during RecyclerView binding.
 */
public final class FormatUtils {

    private static final ThreadLocal<DecimalFormat> VND_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        formatter.setGroupingUsed(true);
        return formatter;
    });

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    );

    private static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMATTER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    );

    private FormatUtils() {
    }

    public static long parseVnd(String priceText) {
        if (priceText == null || priceText.isEmpty()) {
            return 0L;
        }
        long value = 0L;
        boolean hasDigit = false;
        for (int i = 0; i < priceText.length(); i++) {
            char c = priceText.charAt(i);
            if (c >= '0' && c <= '9') {
                hasDigit = true;
                int digit = c - '0';
                if (value > (Long.MAX_VALUE - digit) / 10L) {
                    return 0L;
                }
                value = value * 10L + digit;
            }
        }
        return hasDigit ? value : 0L;
    }

    public static String formatVnd(long amount) {
        DecimalFormat formatter = VND_FORMATTER.get();
        return (formatter != null ? formatter.format(Math.max(0L, amount)) : String.valueOf(Math.max(0L, amount)))
                + " ₫";
    }

    public static String computeLineTotal(String unitPrice, int quantity) {
        return formatVnd(parseVnd(unitPrice) * Math.max(1, quantity));
    }

    /**
     * Returns discount percentage (e.g., 13 for -13%) or 0 if not discounted.
     */
    public static int computeDiscountPercent(String currentPrice, String originalPrice) {
        long current = parseVnd(currentPrice);
        long original = parseVnd(originalPrice);
        if (original <= 0L || current <= 0L || current >= original) {
            return 0;
        }
        return (int) Math.round(((double) (original - current) / original) * 100.0);
    }

    public static long computeShippingFee(long subtotal) {
        if (subtotal <= 0L || subtotal >= Constants.FREE_SHIPPING_THRESHOLD) {
            return 0L;
        }
        return Constants.STANDARD_SHIPPING_FEE;
    }

    public static String formatDate(long epochMillis) {
        SimpleDateFormat fmt = DATE_FORMATTER.get();
        return fmt != null ? fmt.format(new Date(epochMillis)) : "";
    }

    public static String formatDateTime(long epochMillis) {
        SimpleDateFormat fmt = DATE_TIME_FORMATTER.get();
        return fmt != null ? fmt.format(new Date(epochMillis)) : "";
    }

    public static String formatRating(double rating) {
        return String.format(Locale.US, "%.1f", rating);
    }

    /**
     * Extracts up to 2 uppercase initials from a display name for minimalist avatars.
     */
    public static String extractInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String single = parts[0];
            return single.substring(0, Math.min(2, single.length())).toUpperCase(Locale.getDefault());
        }
        char first = parts[0].charAt(0);
        char last = parts[parts.length - 1].charAt(0);
        return ("" + first + last).toUpperCase(Locale.getDefault());
    }
}
