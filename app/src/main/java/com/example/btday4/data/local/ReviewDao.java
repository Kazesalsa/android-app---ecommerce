package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.btday4.model.UserReviewWithProduct;
import java.util.List;

/**
 * Room DAO for product ratings and reviews.
 */
@Dao
public interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE product_id = :productId ORDER BY created_at DESC")
    List<ReviewEntity> getByProductId(String productId);

    @Query("SELECT * FROM reviews WHERE product_id = :productId AND username = :username LIMIT 1")
    ReviewEntity findByProductAndUser(String productId, String username);

    @Query(
            "SELECT r.*, p.name AS product_name, p.image_url AS product_image_url "
                    + "FROM reviews r "
                    + "INNER JOIN products p ON r.product_id = p.id "
                    + "WHERE r.username = :username "
                    + "ORDER BY r.created_at DESC"
    )
    List<UserReviewWithProduct> getUserReviewsWithProduct(String username);

    @Query("SELECT COUNT(*) FROM reviews WHERE username = :username")
    int countByUsername(String username);

    @Query("SELECT COUNT(*) FROM reviews")
    int countAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ReviewEntity review);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMissing(List<ReviewEntity> reviews);

    @Query("DELETE FROM reviews WHERE id = :reviewId")
    void deleteById(String reviewId);
}
