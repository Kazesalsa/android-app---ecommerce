package com.example.btday4.model;

import com.example.btday4.data.local.ReviewEntity;
import java.util.List;

/**
 * Immutable summary of customer ratings (average score, total count, and 1..5 star distribution).
 */
public final class RatingSummary {

    private final double averageRating;
    private final int totalCount;
    private final int[] countsByStar; // index 1..5

    private RatingSummary(double averageRating, int totalCount, int[] countsByStar) {
        this.averageRating = averageRating;
        this.totalCount = totalCount;
        this.countsByStar = countsByStar;
    }

    public static RatingSummary fromReviews(List<ReviewEntity> reviews) {
        int[] counts = new int[6];
        if (reviews == null || reviews.isEmpty()) {
            return new RatingSummary(0.0, 0, counts);
        }
        long sum = 0L;
        int validCount = 0;
        for (ReviewEntity review : reviews) {
            if (review == null) {
                continue;
            }
            int star = Math.max(1, Math.min(5, review.getRating()));
            counts[star]++;
            sum += star;
            validCount++;
        }
        double avg = validCount == 0 ? 0.0 : (double) sum / validCount;
        return new RatingSummary(avg, validCount, counts);
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCountForStar(int star) {
        if (star < 1 || star > 5) {
            return 0;
        }
        return countsByStar[star];
    }

    public int getPercentForStar(int star) {
        if (totalCount <= 0 || star < 1 || star > 5) {
            return 0;
        }
        return (int) Math.round((countsByStar[star] * 100.0) / totalCount);
    }
}
