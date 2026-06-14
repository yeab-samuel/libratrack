package com.libratrack.repository;

import com.libratrack.entity.BookRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookRatingRepository extends JpaRepository<BookRating, Long> {

    Optional<BookRating> findByBookIdAndMemberId(Long bookId, Long memberId);

    /**
     * Batch-load average rating and count for a list of book IDs.
     * Used by BookService to enrich page results without N+1 queries.
     */
    @Query("SELECT r.book.id AS bookId, AVG(r.rating) AS average, COUNT(r) AS count " +
           "FROM BookRating r WHERE r.book.id IN :bookIds GROUP BY r.book.id")
    List<RatingStats> findStatsByBookIds(@Param("bookIds") List<Long> bookIds);

    /** Projection returned by findStatsByBookIds. */
    interface RatingStats {
        Long   getBookId();
        Double getAverage();
        Long   getCount();
    }
}
