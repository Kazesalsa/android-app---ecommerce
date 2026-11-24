package com.example.btday4.model;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import com.example.btday4.data.local.ReviewEntity;

/**
 * Room query projection pairing a user's review with basic product info for Profile -> My Reviews.
 */
public class UserReviewWithProduct {

    @Embedded
    public ReviewEntity review;

    @ColumnInfo(name = "product_name")
    public String productName;

    @ColumnInfo(name = "product_image_url")
    public String productImageUrl;
}
