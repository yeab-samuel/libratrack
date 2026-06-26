package com.libratrack.service;

import com.libratrack.dto.response.BookRatingDTO;
import com.libratrack.dto.response.RatingSummaryDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.LoanStatus;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookRatingService {

    private final BookRatingRepository bookRatingRepository;
    private final BookRepository       bookRepository;
    private final LoanRepository       loanRepository;

    /**
     * Submit or update a rating for a book, with an optional review text.
     * Gate: the member must have at least one RETURNED loan for this book.
     * Re-rating is allowed — the previous rating and review are overwritten.
     */
    @Transactional
    public RatingSummaryDTO submitRating(Long bookId, Integer stars, String reviewText, User member) {
        boolean hasReturned = loanRepository.hasReturnedBook(
                member.getId(), bookId, LoanStatus.RETURNED);
        if (!hasReturned)
            throw new IllegalStateException(
                    "You can only rate books you have returned to the library.");

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        // Upsert: update existing rating, or create a new one
        BookRating rating = bookRatingRepository
                .findByBookIdAndMemberId(bookId, member.getId())
                .orElse(BookRating.builder().book(book).member(member).build());
        rating.setRating(stars);
        rating.setReviewText(reviewText);
        bookRatingRepository.save(rating);

        return buildSummary(bookId, stars);
    }

    /** Get the average rating + count for a book, plus the requesting member's own rating. */
    @Transactional(readOnly = true)
    public RatingSummaryDTO getSummary(Long bookId, Long memberId) {
        Integer myRating = (memberId == null) ? null :
                bookRatingRepository.findByBookIdAndMemberId(bookId, memberId)
                        .map(BookRating::getRating).orElse(null);
        return buildSummary(bookId, myRating);
    }

    /**
     * Return individual reviews (with text) for a book, newest first.
     * Only includes entries that actually have review text.
     */
    @Transactional(readOnly = true)
    public List<BookRatingDTO> getReviews(Long bookId) {
        return bookRatingRepository
                .findByBookIdOrderByCreatedAtDesc(bookId)
                .stream()
                .filter(r -> r.getReviewText() != null && !r.getReviewText().isBlank())
                .map(r -> new BookRatingDTO(
                        r.getMember().getFullName(),
                        r.getRating(),
                        r.getReviewText(),
                        r.getCreatedAt()))
                .toList();
    }

    private RatingSummaryDTO buildSummary(Long bookId, Integer myRating) {
        var stats = bookRatingRepository.findStatsByBookIds(List.of(bookId));
        if (stats.isEmpty()) return new RatingSummaryDTO(null, 0L, myRating);
        var s = stats.get(0);
        return new RatingSummaryDTO(s.getAverage(), s.getCount(), myRating);
    }
}