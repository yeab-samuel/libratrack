package com.libratrack.controller;

import com.libratrack.dto.request.BookRatingRequest;
import com.libratrack.dto.response.RatingSummaryDTO;
import com.libratrack.entity.User;
import com.libratrack.service.BookRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Rating endpoints nested under /api/books/{bookId}.
 *
 * POST /api/books/{bookId}/ratings  — submit or update a rating (STUDENT/FACULTY)
 * GET  /api/books/{bookId}/ratings  — get average + count for a book (public)
 *                                     includes myRating if the caller is authenticated
 */
@RestController
@RequestMapping("/api/books/{bookId}/ratings")
@RequiredArgsConstructor
public class BookRatingController {

    private final BookRatingService bookRatingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY')")
    public RatingSummaryDTO submitRating(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRatingRequest req,
            Authentication auth) {
        User user = (User) auth.getPrincipal();
        return bookRatingService.submitRating(bookId, req.stars(), user);
    }

    @GetMapping
    public RatingSummaryDTO getRatingSummary(
            @PathVariable Long bookId,
            Authentication auth) {
        Long memberId = (auth != null && auth.getPrincipal() instanceof User u)
            ? u.getId() : null;
        return bookRatingService.getSummary(bookId, memberId);
    }
}
