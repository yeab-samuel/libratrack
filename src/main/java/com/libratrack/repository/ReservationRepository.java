package com.libratrack.repository;
import com.libratrack.entity.*;
import com.libratrack.enums.ReservationStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberAndBookAndStatusIn(User member, Book book, List<ReservationStatus> statuses);

    Optional<Reservation> findFirstByBookAndStatusOrderByQueuePositionAsc(Book book, ReservationStatus status);

    Page<Reservation> findByMember(User member, Pageable pageable);

    @Query("SELECT COALESCE(MAX(r.queuePosition), 0) FROM Reservation r WHERE r.book = :book")
    int findMaxQueuePositionByBook(@Param("book") Book book);

    /** Smallest queue position held by a STUDENT in this book's queue (for faculty priority). */
    @Query("SELECT MIN(r.queuePosition) FROM Reservation r " +
           "WHERE r.book = :book AND r.status IN ('WAITING','NOTIFIED') " +
           "AND r.member.role = 'STUDENT'")
    Optional<Integer> findFirstStudentQueuePosition(@Param("book") Book book);

    /** Shift all WAITING/NOTIFIED entries at or above a given position down by 1. */
    @Modifying
    @Query("UPDATE Reservation r SET r.queuePosition = r.queuePosition + 1 " +
           "WHERE r.book = :book AND r.status IN ('WAITING','NOTIFIED') " +
           "AND r.queuePosition >= :fromPosition")
    void incrementQueuePositionsFrom(@Param("book") Book book, @Param("fromPosition") int fromPosition);

    /** Find all NOTIFIED reservations whose expiry date has passed. */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'NOTIFIED' AND r.expiresAt < :now")
    List<Reservation> findExpiredNotifications(@Param("now") LocalDate now);

    @Query("SELECT r FROM Reservation r WHERE " +
           "(:bookId IS NULL OR r.book.id = :bookId) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<Reservation> findWithFilters(@Param("bookId") Long bookId,
                                      @Param("status") ReservationStatus status,
                                      Pageable pageable);
}
