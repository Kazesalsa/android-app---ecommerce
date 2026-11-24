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
 * Room DAO for managing shopping cart line items and joining with products.
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
    @Query("SELECT * FROM cart_items ORDER BY updated_at DESC")
    List<CartRow> getAllWithProduct();

    @Query("SELECT * FROM cart_items WHERE product_id = :productId LIMIT 1")
    CartItemEntity findByProductId(String productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CartItemEntity item);

    @Query("DELETE FROM cart_items WHERE product_id = :productId")
    void deleteByProductId(String productId);

    @Query("DELETE FROM cart_items")
    void clearAll();

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    int getTotalQuantity();
}
