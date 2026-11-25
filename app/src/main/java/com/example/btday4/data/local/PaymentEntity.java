package com.example.btday4.data.local;

import androidx.annotation.NonNull;

/**
 * Firestore POJO tracking a PayOS VietQR payment transaction linked to an Order.
 */
public class PaymentEntity {

    private long orderCode;

    @NonNull
    private String orderId;

    private String paymentLinkId;

    private long amount;

    private String description;

    private String checkoutUrl;

    private String qrCode;

    private String bankBin;

    private String accountNumber;

    private String accountName;

    @NonNull
    private String status;

    private long expiredAtSeconds;

    private long createdAtMillis;

    private long paidAtMillis;

    public PaymentEntity() {
        this.orderId = "";
        this.status = "PENDING";
    }

    public PaymentEntity(
            long orderCode,
            @NonNull String orderId,
            String paymentLinkId,
            long amount,
            String description,
            String checkoutUrl,
            String qrCode,
            String bankBin,
            String accountNumber,
            String accountName,
            @NonNull String status,
            long expiredAtSeconds,
            long createdAtMillis,
            long paidAtMillis
    ) {
        this.orderCode = orderCode;
        this.orderId = orderId;
        this.paymentLinkId = paymentLinkId;
        this.amount = amount;
        this.description = description;
        this.checkoutUrl = checkoutUrl;
        this.qrCode = qrCode;
        this.bankBin = bankBin;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.status = status;
        this.expiredAtSeconds = expiredAtSeconds;
        this.createdAtMillis = createdAtMillis;
        this.paidAtMillis = paidAtMillis;
    }

    public long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(long orderCode) {
        this.orderCode = orderCode;
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(@NonNull String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(String paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getBankBin() {
        return bankBin;
    }

    public void setBankBin(String bankBin) {
        this.bankBin = bankBin;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public long getExpiredAtSeconds() {
        return expiredAtSeconds;
    }

    public void setExpiredAtSeconds(long expiredAtSeconds) {
        this.expiredAtSeconds = expiredAtSeconds;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public void setCreatedAtMillis(long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

    public long getPaidAtMillis() {
        return paidAtMillis;
    }

    public void setPaidAtMillis(long paidAtMillis) {
        this.paidAtMillis = paidAtMillis;
    }

    public boolean isStillReusable(long nowEpochSeconds) {
        return ("PENDING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status))
                && qrCode != null
                && !qrCode.trim().isEmpty()
                && expiredAtSeconds > nowEpochSeconds + 60L;
    }
}
