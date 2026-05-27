package com.libratrack.repository;
import com.libratrack.entity.*;
import com.libratrack.enums.FineStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface FineRecordRepository extends JpaRepository<FineRecord,Long> {
    Optional<FineRecord> findByLoan(Loan loan);
    boolean existsByMemberAndStatus(User member,FineStatus status);
    Page<FineRecord> findByMember(User member,Pageable pageable);
    List<FineRecord> findByStatusAndCreatedAtBetween(FineStatus status,LocalDateTime from,LocalDateTime to);
    @Query("SELECT f FROM FineRecord f WHERE (:memberId IS NULL OR f.member.id=:memberId) AND (:status IS NULL OR f.status=:status)")
    Page<FineRecord> findWithFilters(@Param("memberId")Long memberId,@Param("status")FineStatus status,Pageable pageable);
}
