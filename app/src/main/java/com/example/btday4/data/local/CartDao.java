package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import java.util.List;

/**
 * Room DAO for managing per-account shopping cart line items and joining with products.
 */
@Dao
public interface CartDao {

    class CartRow {
        @Embedded
        public CartItemEntity entry;

        @Relation(parentColumn = "product_id", entityColumn = "id")
        public Product product;
    }

    @Transaction
    @Query("SELECT * FROM cart_items WHERE username = :username ORDER BY updated_at DESC")
    List<CartRow> getAllWithProduct(String username);

    @Query("SELECT * FROM cart_items WHERE username = :username AND product_id = :productId LIMIT 1")
    CartItemEntity find(String username, String productId);

    /**
     * Safe to REPLACE: cart_items is a leaf table with no dependent foreign keys.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CartItemEntity item);

    @Query("DELETE FROM cart_items WHERE username = :username AND product_id = :productId")
    void delete(String username, String productId);

    @Query("DELETE FROM cart_items WHERE username = :username AND product_id IN (:productIds)")
    void deleteProducts(String username, List<String> productIds);

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE username = :username")
    int getTotalQuantity(String username);
}
