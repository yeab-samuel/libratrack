package com.libratrack.dto.response;

/**
 * Returned after submitting a rating and by the GET summary endpoint.
 * average   — current average across all raters (null if no ratings yet)
 * count     — total number of ratings
 * myRating  — the requesting member's own rating (null if not yet rated)
 */
public record RatingSummaryDTO(Double average, Long count, Integer myRating) {}
