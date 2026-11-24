package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity storing an immutable snapshot of a purchased product line item inside an order.
 */
@Entity(
        tableName = "order_items",
        foreignKeys = @ForeignKey(
                entity = OrderEntity.class,
                parentColumns = "id",
                childColumns = "order_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "order_id")}
)
public class OrderItemEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "order_id")
    private String orderId;

    @NonNull
    @ColumnInfo(name = "product_id")
    private String productId;

    @NonNull
    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "product_image_url")
    private String productImageUrl;

    @ColumnInfo(name = "unit_price")
    private long unitPrice;

    @ColumnInfo(name = "quantity")
    private int quantity;

    @Ignore
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
