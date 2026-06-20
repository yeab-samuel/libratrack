package com.libratrack.repository;
import com.libratrack.entity.*;
import com.libratrack.enums.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface BookCopyRepository extends JpaRepository<BookCopy,Long> {
    List<BookCopy> findAllByBook(Book book);
    Optional<BookCopy> findFirstByBookAndStatus(Book book,CopyStatus status);
    long countByBookAndStatus(Book book,CopyStatus status);
    boolean existsByCopyNumber(String copyNumber);

    List<BookCopy> findByBookAndStatus(Book book, CopyStatus status);

    /**
     * Batch-counts AVAILABLE copies for a list of book IDs in one query.
     * Used by BookService to enrich a page of books without N+1 queries.
     * Books with zero available copies simply don't appear in the result —
     * the caller must default missing entries to 0.
     */
    @Query("SELECT bc.book.id AS bookId, COUNT(bc) AS count FROM BookCopy bc " +
            "WHERE bc.book.id IN :bookIds AND bc.status = com.libratrack.enums.CopyStatus.AVAILABLE " +
            "GROUP BY bc.book.id")
    List<AvailabilityCount> countAvailableByBookIds(@Param("bookIds") List<Long> bookIds);

    /** Projection returned by countAvailableByBookIds. */
    interface AvailabilityCount {
        Long getBookId();
        Long getCount();
    }
}