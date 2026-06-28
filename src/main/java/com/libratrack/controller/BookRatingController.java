package com.libratrack.controller;

import com.libratrack.dto.request.BookRatingRequest;
import com.libratrack.dto.response.BookRatingDTO;
import com.libratrack.dto.response.RatingSummaryDTO;
import com.libratrack.entity.User;
import com.libratrack.service.BookRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Rating endpoints nested under /api/books/{bookId}.
 *
 * POST /api/books/{bookId}/ratings         — submit or update a rating + optional review
 * GET  /api/books/{bookId}/ratings         — get average + count (+ myRating if authenticated)
 * GET  /api/books/{bookId}/ratings/reviews — list individual text reviews, newest first
 */
@RestController
@RequestMapping("/api/books/{bookId}/ratings")
@RequiredArgsConstructor
public class BookRatingController {

    private final BookRatingService bookRatingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY')")
    public ResponseEntity<RatingSummaryDTO> submitRating(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRatingRequest req,
            Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(
                bookRatingService.submitRating(bookId, req.stars(), req.reviewText(), user));
    }

    @GetMapping
    public ResponseEntity<RatingSummaryDTO> getRatingSummary(
            @PathVariable Long bookId,
            Authentication auth) {
        Long memberId = (auth != null && auth.getPrincipal() instanceof User u)
                ? u.getId() : null;
        return ResponseEntity.ok(bookRatingService.getSummary(bookId, memberId));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<BookRatingDTO>> getReviews(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookRatingService.getReviews(bookId));
    }
}