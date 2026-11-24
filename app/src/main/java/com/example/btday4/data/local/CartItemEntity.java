package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Room entity storing shopping cart line items linked to products.
 */
@Entity(
        tableName = "cart_items",
        foreignKeys = @ForeignKey(
                entity = Product.class,
                parentColumns = "id",
                childColumns = "product_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class CartItemEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "product_id")
    private final String productId;

    @ColumnInfo(name = "quantity")
    private final int quantity;

    @ColumnInfo(name = "updated_at")
    private final long updatedAt;

    public CartItemEntity(@NonNull String productId, int quantity, long updatedAt) {
        this.productId = productId;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
