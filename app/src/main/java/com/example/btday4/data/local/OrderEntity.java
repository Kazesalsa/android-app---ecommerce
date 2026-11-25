package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room and Firestore entity representing a submitted customer order with shipping and payment snapshot.
 */
@Entity(
        tableName = "orders",
        indices = {@Index(value = "username")}
)
public class OrderEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "recipient_name")
    private String recipientName;

    @NonNull
    @ColumnInfo(name = "recipient_phone")
    private String recipientPhone;

    @NonNull
    @ColumnInfo(name = "shipping_address")
    private String shippingAddress;

    @ColumnInfo(name = "note")
    private String note;

    @NonNull
    @ColumnInfo(name = "payment_method")
    private String paymentMethod;

    @ColumnInfo(name = "subtotal_amount")
    private long subtotalAmount;

    @ColumnInfo(name = "shipping_fee")
    private long shippingFee;

    @ColumnInfo(name = "total_amount")
    private long totalAmount;

    @ColumnInfo(name = "voucher_code")
    private String voucherCode;

    @ColumnInfo(name = "voucher_discount")
    private long voucherDiscount;

    @NonNull
    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @Ignore
    public OrderEntity() {
        this.id = "";
        this.username = "";
        this.recipientName = "";
        this.recipientPhone = "";
        this.shippingAddress = "";
        this.paymentMethod = "";
        this.voucherCode = "";
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
            long totalAmount,
            @NonNull String status,
            long createdAt
    ) {
        this(
                id,
                username,
                recipientName,
                recipientPhone,
                shippingAddress,
                note,
                paymentMethod,
                subtotalAmount,
                shippingFee,
                totalAmount,
                "",
                0L,
                status,
                createdAt
        );
    }

    @Ignore
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
            long totalAmount,
            String voucherCode,
            long voucherDiscount,
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
        this.totalAmount = totalAmount;
        this.voucherCode = voucherCode == null ? "" : voucherCode;
        this.voucherDiscount = Math.max(0L, voucherDiscount);
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

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getVoucherCode() {
        return voucherCode == null ? "" : voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode == null ? "" : voucherCode;
    }

    public long getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(long voucherDiscount) {
        this.voucherDiscount = Math.max(0L, voucherDiscount);
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
