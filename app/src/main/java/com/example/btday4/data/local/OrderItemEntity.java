package com.example.btday4.data.local;

import androidx.annotation.NonNull;

/**
 * Firestore POJO storing an immutable snapshot of a purchased product line item inside an order.
 */
public class OrderItemEntity {

    @NonNull
    private String orderId;

    @NonNull
    private String productId;

    @NonNull
    private String productName;

    private String productImageUrl;

    private long unitPrice;

    private int quantity;

    public OrderItemEntity() {
        this.orderId = "";
        this.productId = "";
        this.productName = "";
    }

    public OrderItemEntity(
            @NonNull String orderId,
            @NonNull String productId,
            @NonNull String productName,
            String productImageUrl,
            long unitPrice,
            int quantity
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.unitPrice = unitPrice;
        this.quantity = Math.max(1, quantity);
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(@NonNull String orderId) {
        this.orderId = orderId;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    public void setProductId(@NonNull String productId) {
        this.productId = productId;
    }

    @NonNull
    public String getProductName() {
        return productName;
    }

    public void setProductName(@NonNull String productName) {
        this.productName = productName;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public long lineSubtotal() {
        return unitPrice * Math.max(1, quantity);
    }
}
