package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Firestore entity storing customer ratings and reviews per product.
 */
@IgnoreExtraProperties
public class ReviewEntity {

    @NonNull
    private String id;

    @NonNull
    private String productId;

    @NonNull
    private String username;

    @NonNull
    private String authorName;

    private int rating;
    private String comment;
    private long createdAt;

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
