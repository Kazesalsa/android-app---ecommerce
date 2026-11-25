package com.example.btday4.model;

import java.util.Locale;

/**
 * Immutable DTO representing a parsed response from PayOS v2 payment-requests APIs.
 */
public final class PayOsPaymentInfo {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private final long orderCode;
    private final String paymentLinkId;
    private final long amount;
    private final String description;
    private final String checkoutUrl;
    private final String qrCode;
    private final String bin;
    private final String accountNumber;
    private final String accountName;
    private final String status;
    private final long expiredAtSeconds;

    public PayOsPaymentInfo(
            long orderCode,
            String paymentLinkId,
            long amount,
            String description,
            String checkoutUrl,
            String qrCode,
            String bin,
            String accountNumber,
            String accountName,
            String status,
            long expiredAtSeconds
    ) {
        this.orderCode = orderCode;
        this.paymentLinkId = safe(paymentLinkId);
        this.amount = amount;
        this.description = safe(description);
        this.checkoutUrl = safe(checkoutUrl);
        this.qrCode = safe(qrCode);
        this.bin = safe(bin);
        this.accountNumber = safe(accountNumber);
        this.accountName = safe(accountName);
        this.status = status == null || status.trim().isEmpty() ? STATUS_PENDING : status.trim().toUpperCase(Locale.US);
        this.expiredAtSeconds = expiredAtSeconds;
    }

    public long getOrderCode() {
        return orderCode;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public long getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getBin() {
        return bin;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getStatus() {
        return status;
    }

    public long getExpiredAtSeconds() {
        return expiredAtSeconds;
    }

    public boolean isPaid() {
        return STATUS_PAID.equalsIgnoreCase(status);
    }

    public boolean isCancelledOrExpired() {
        return STATUS_CANCELLED.equalsIgnoreCase(status) || STATUS_EXPIRED.equalsIgnoreCase(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
