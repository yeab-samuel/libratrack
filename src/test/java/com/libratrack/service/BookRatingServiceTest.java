package com.libratrack.service;

import com.libratrack.dto.response.RatingSummaryDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookRatingServiceTest {

    @Mock BookRatingRepository bookRatingRepository;
    @Mock BookRepository bookRepository;
    @Mock LoanRepository loanRepository;
    @InjectMocks BookRatingService service;

    private User member;
    private Book book;

    @BeforeEach
    void setUp() {
        member = User.builder()
                .id(1L).email("member@test.com").fullName("Jane Doe")
                .role(Role.STUDENT).active(true).build();
        book = Book.builder()
                .id(1L).isbn("isbn-1").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).totalCopies(3).build();
    }

    private BookRatingRepository.RatingStats statsOf(Double average, Long count) {
        BookRatingRepository.RatingStats stats = mock(BookRatingRepository.RatingStats.class);
        when(stats.getAverage()).thenReturn(average);
        when(stats.getCount()).thenReturn(count);
        return stats;
    }

    // ── submitRating ──────────────────────────────────────────────────────

    @Test
    void submitRating_MemberHasNotReturnedBook_ThrowsIllegalStateException() {
        when(loanRepository.hasReturnedBook(1L, 1L, LoanStatus.RETURNED)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.submitRating(1L, 5, member));
        verifyNoInteractions(bookRepository);
    }

    @Test
    void submitRating_BookNotFound_ThrowsResourceNotFoundException() {
        when(loanRepository.hasReturnedBook(1L, 1L, LoanStatus.RETURNED)).thenReturn(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.submitRating(1L, 5, member));
    }

    @Test
    void submitRating_NoExistingRating_CreatesNewRating() {
        when(loanRepository.hasReturnedBook(1L, 1L, LoanStatus.RETURNED)).thenReturn(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRatingRepository.findByBookIdAndMemberId(1L, 1L)).thenReturn(Optional.empty());

        // Build the stats mock BEFORE the outer when(...) call. Creating a mock
        // (statsOf) inside the argument expression of an in-progress
        // when(...).thenReturn(...) chain confuses Mockito's stubbing tracker
        // and throws UnfinishedStubbingException — this avoids that trap.
        List<BookRatingRepository.RatingStats> stats = List.of(statsOf(5.0, 1L));
        when(bookRatingRepository.findStatsByBookIds(List.of(1L))).thenReturn(stats);

        RatingSummaryDTO result = service.submitRating(1L, 5, member);

        ArgumentCaptor<BookRating> captor = ArgumentCaptor.forClass(BookRating.class);
        verify(bookRatingRepository).save(captor.capture());
        BookRating saved = captor.getValue();
        assertEquals(book, saved.getBook());
        assertEquals(member, saved.getMember());
        assertEquals(5, saved.getRating());
        assertEquals(new RatingSummaryDTO(5.0, 1L, 5), result);
    }

    @Test
    void submitRating_ExistingRating_UpdatesSameRecord() {
        BookRating existing = BookRating.builder().id(9L).book(book).member(member).rating(3).build();

        when(loanRepository.hasReturnedBook(1L, 1L, LoanStatus.RETURNED)).thenReturn(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRatingRepository.findByBookIdAndMemberId(1L, 1L)).thenReturn(Optional.of(existing));

        List<BookRatingRepository.RatingStats> stats = List.of(statsOf(4.0, 1L));
        when(bookRatingRepository.findStatsByBookIds(List.of(1L))).thenReturn(stats);

        service.submitRating(1L, 4, member);

        ArgumentCaptor<BookRating> captor = ArgumentCaptor.forClass(BookRating.class);
        verify(bookRatingRepository).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertEquals(4, existing.getRating());
    }

    // ── getSummary ────────────────────────────────────────────────────────

    @Test
    void getSummary_NullMemberId_SkipsLookupAndReturnsNullMyRating() {
        when(bookRatingRepository.findStatsByBookIds(List.of(1L))).thenReturn(List.of());

        RatingSummaryDTO result = service.getSummary(1L, null);

        verify(bookRatingRepository, never()).findByBookIdAndMemberId(any(), any());
        assertEquals(new RatingSummaryDTO(null, 0L, null), result);
    }

    @Test
    void getSummary_MemberIdProvidedButNoRatingYet_ReturnsNullMyRating() {
        when(bookRatingRepository.findByBookIdAndMemberId(1L, 2L)).thenReturn(Optional.empty());

        List<BookRatingRepository.RatingStats> stats = List.of(statsOf(4.5, 2L));
        when(bookRatingRepository.findStatsByBookIds(List.of(1L))).thenReturn(stats);

        RatingSummaryDTO result = service.getSummary(1L, 2L);

        assertEquals(new RatingSummaryDTO(4.5, 2L, null), result);
    }

    @Test
    void getSummary_MemberHasExistingRating_ReturnsMyRating() {
        BookRating existing = BookRating.builder().id(9L).book(book).member(member).rating(5).build();
        when(bookRatingRepository.findByBookIdAndMemberId(1L, 2L)).thenReturn(Optional.of(existing));

        List<BookRatingRepository.RatingStats> stats = List.of(statsOf(4.5, 2L));
        when(bookRatingRepository.findStatsByBookIds(List.of(1L))).thenReturn(stats);

        RatingSummaryDTO result = service.getSummary(1L, 2L);

        assertEquals(new RatingSummaryDTO(4.5, 2L, 5), result);
    }
}