package com.example.btday4.model;

import com.example.btday4.data.local.Product;

/**
 * Domain model combining a Product with its live aggregate rating from Firestore reviews.
 */
public class ProductWithRating {

    public Product product;
    public double avgRating;
    public int reviewCount;

    public ProductWithRating() {
    }

    public ProductWithRating(Product product, double avgRating, int reviewCount) {
        this.product = product;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}
