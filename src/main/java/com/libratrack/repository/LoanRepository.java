package com.libratrack.repository;
import com.libratrack.entity.*;
import com.libratrack.enums.LoanStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface LoanRepository extends JpaRepository<Loan,Long> {
    @Query("SELECT l FROM Loan l JOIN FETCH l.bookCopy bc JOIN FETCH bc.book JOIN FETCH l.member WHERE l.member = :member")
    Page<Loan> findByMember(@Param("member") User member, Pageable pageable);
    @Query("SELECT l FROM Loan l JOIN FETCH l.bookCopy bc JOIN FETCH bc.book JOIN FETCH l.member WHERE l.status = :status")
    Page<Loan> findByStatus(@Param("status") LoanStatus status, Pageable pageable);
    List<Loan> findAllByStatusAndDueDateBefore(LoanStatus status,LocalDate date);
    long countByMemberAndStatus(User member,LoanStatus status);
    @Query("SELECT l FROM Loan l JOIN FETCH l.bookCopy bc JOIN FETCH bc.book JOIN FETCH l.member WHERE (:memberId IS NULL OR l.member.id=:memberId) AND (:status IS NULL OR l.status=:status)")
    Page<Loan> findWithFilters(@Param("memberId")Long memberId,@Param("status")LoanStatus status,Pageable pageable);

    /** Used by BookRatingService to verify the member has returned this book before allowing rating. */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Loan l " +
            "WHERE l.member.id = :memberId AND l.bookCopy.book.id = :bookId AND l.status = :status")
    boolean hasReturnedBook(@Param("memberId") Long memberId,
                            @Param("bookId")   Long bookId,
                            @Param("status")   LoanStatus status);
}
