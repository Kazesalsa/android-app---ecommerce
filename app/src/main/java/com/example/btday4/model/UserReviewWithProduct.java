package com.example.btday4.model;

import com.example.btday4.data.local.ReviewEntity;

/**
 * Domain model pairing a user's review with basic product info for Profile -> My Reviews.
 */
public class UserReviewWithProduct {

    public ReviewEntity review;
    public String productName;
    public String productImageUrl;
}
