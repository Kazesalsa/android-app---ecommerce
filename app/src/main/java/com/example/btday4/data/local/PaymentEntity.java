package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room and Firestore entity tracking a PayOS VietQR payment transaction linked to an Order.
 * Mirrors the ThanhToan table design in Payment_PayOS_Module.
 */
@Entity(
        tableName = "payments",
        foreignKeys = @ForeignKey(
                entity = OrderEntity.class,
                parentColumns = "id",
                childColumns = "order_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "order_id")}
)
public class PaymentEntity {

    @PrimaryKey
    @ColumnInfo(name = "order_code")
    private long orderCode;

    @NonNull
    @ColumnInfo(name = "order_id")
    private String orderId;

    @ColumnInfo(name = "payment_link_id")
    private String paymentLinkId;

    @ColumnInfo(name = "amount")
    private long amount;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "checkout_url")
    private String checkoutUrl;

    @ColumnInfo(name = "qr_code")
    private String qrCode;

    @ColumnInfo(name = "bank_bin")
    private String bankBin;

    @ColumnInfo(name = "account_number")
    private String accountNumber;

    @ColumnInfo(name = "account_name")
    private String accountName;

    @NonNull
    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "expired_at")
    private long expiredAtSeconds;

    @ColumnInfo(name = "created_at")
    private long createdAtMillis;

    @ColumnInfo(name = "paid_at")
    private long paidAtMillis;

    @Ignore
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
