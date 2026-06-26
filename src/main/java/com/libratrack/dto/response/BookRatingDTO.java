package com.libratrack.dto.response;

import java.time.LocalDateTime;

/**
 * A single user review returned by GET /api/books/{id}/ratings/reviews.
 * Used in the frontend book detail panel to display reader reviews.
 */
public record BookRatingDTO(
        String reviewerName,
        int    stars,
        String reviewText,
        LocalDateTime createdAt
) {}