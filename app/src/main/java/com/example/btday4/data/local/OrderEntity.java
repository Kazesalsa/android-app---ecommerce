package com.example.btday4.data.local;

import androidx.annotation.NonNull;

/**
 * Firestore POJO representing a submitted customer order with shipping and payment snapshot.
 */
public class OrderEntity {

    @NonNull
    private String id;

    @NonNull
    private String username;

    @NonNull
    private String recipientName;

    @NonNull
    private String recipientPhone;

    @NonNull
    private String shippingAddress;

    private String note;

    @NonNull
    private String paymentMethod;

    private long subtotalAmount;

    private long shippingFee;

    private String voucherCode;

    private long discountAmount;

    private long totalAmount;

    @NonNull
    private String status;

    private long createdAt;

    public OrderEntity() {
        this.id = "";
        this.username = "";
        this.recipientName = "";
        this.recipientPhone = "";
        this.shippingAddress = "";
        this.paymentMethod = "";
        this.voucherCode = "";
        this.discountAmount = 0L;
        this.status = "";
    }

    public OrderEntity(
            @NonNull String id,
            @NonNull String username,
            @NonNull String recipientName,
            @NonNull String recipientPhone,
            @NonNull String shippingAddress,
            String note,
            @NonNull String paymentMethod,
            long subtotalAmount,
            long shippingFee,
            String voucherCode,
            long discountAmount,
            long totalAmount,
            @NonNull String status,
            long createdAt
    ) {
        this.id = id;
        this.username = username;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.paymentMethod = paymentMethod;
        this.subtotalAmount = subtotalAmount;
        this.shippingFee = shippingFee;
        this.voucherCode = voucherCode == null ? "" : voucherCode;
        this.discountAmount = Math.max(0L, discountAmount);
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(@NonNull String recipientName) {
        this.recipientName = recipientName;
    }

    @NonNull
    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(@NonNull String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    @NonNull
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(@NonNull String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @NonNull
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(@NonNull String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(long subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(long shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getVoucherCode() {
        return voucherCode == null ? "" : voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode == null ? "" : voucherCode;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = Math.max(0L, discountAmount);
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
