package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Room entity storing shopping cart line items scoped per account and linked to products.
 */
@Entity(
        tableName = "cart_items",
        primaryKeys = {"username", "product_id"},
        foreignKeys = @ForeignKey(
                entity = Product.class,
                parentColumns = "id",
                childColumns = "product_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("product_id")
)
public class CartItemEntity {

    @NonNull
    @ColumnInfo(name = "username")
    private final String username;

    @NonNull
    @ColumnInfo(name = "product_id")
    private final String productId;

    @ColumnInfo(name = "quantity")
    private final int quantity;

    @ColumnInfo(name = "updated_at")
    private final long updatedAt;

    public CartItemEntity(@NonNull String username, @NonNull String productId, int quantity, long updatedAt) {
        this.username = username;
        this.productId = productId;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getUsername() {
        return username;
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
