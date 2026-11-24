package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room and Firestore entity storing customer ratings and reviews per product.
 */
@Entity(
        tableName = "reviews",
        foreignKeys = @ForeignKey(
                entity = Product.class,
                parentColumns = "id",
                childColumns = "product_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "product_id"),
                @Index(value = "username")
        }
)
public class ReviewEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "product_id")
    private String productId;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "author_name")
    private String authorName;

    @ColumnInfo(name = "rating")
    private int rating;

    @ColumnInfo(name = "comment")
    private String comment;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @Ignore
    public ReviewEntity() {
        this.id = "";
        this.productId = "";
        this.username = "";
        this.authorName = "";
    }

    public ReviewEntity(
            @NonNull String id,
            @NonNull String productId,
            @NonNull String username,
            @NonNull String authorName,
            int rating,
            String comment,
            long createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.username = username;
        this.authorName = authorName;
        this.rating = Math.max(1, Math.min(5, rating));
        this.comment = comment;
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
    public String getProductId() {
        return productId;
    }

    public void setProductId(@NonNull String productId) {
        this.productId = productId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(@NonNull String authorName) {
        this.authorName = authorName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = Math.max(1, Math.min(5, rating));
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
