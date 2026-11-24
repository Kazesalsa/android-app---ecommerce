package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.btday4.model.ProductWithRating;
import java.util.List;

/**
 * Room DAO for querying, seeding, and updating products while preserving foreign-key children.
 */
@Dao
public interface ProductDao {

    @Query("SELECT * FROM products")
    List<Product> getAll();

    @Query(
            "SELECT p.*, "
                    + "COALESCE(AVG(r.rating), 0.0) AS avg_rating, "
                    + "COUNT(r.id) AS review_count "
                    + "FROM products p "
                    + "LEFT JOIN reviews r ON p.id = r.product_id "
                    + "GROUP BY p.id"
    )
    List<ProductWithRating> getAllWithRating();

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    Product findById(String id);

    @Query("SELECT COUNT(*) FROM products")
    int count();

    @Query(
            "SELECT COUNT(*) FROM products "
                    + "WHERE image_url IS NULL OR image_url = '' "
                    + "OR description IS NULL OR description = '' "
                    + "OR specs IS NULL OR specs = ''"
    )
    int countMissingDetails();

    /**
     * Uses IGNORE instead of REPLACE to avoid triggering ON DELETE CASCADE on cart_items and reviews.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMissing(List<Product> products);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSingle(Product product);

    @Update
    void updateAll(List<Product> products);

    @Update
    void updateSingle(Product product);
}
