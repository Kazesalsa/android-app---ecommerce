package com.example.btday4.model;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Ignore;
import com.example.btday4.data.local.Product;

/**
 * Room query projection combining a Product with its live aggregate rating from the reviews table.
 */
public class ProductWithRating {

    @Embedded
    public Product product;

    @ColumnInfo(name = "avg_rating")
    public double avgRating;

    @ColumnInfo(name = "review_count")
    public int reviewCount;

    public ProductWithRating() {
    }

    @Ignore
    public ProductWithRating(Product product, double avgRating, int reviewCount) {
        this.product = product;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}
