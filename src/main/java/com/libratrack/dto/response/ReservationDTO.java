package com.libratrack.dto.response;
import com.libratrack.enums.ReservationStatus;
import java.time.*;
public record ReservationDTO(Long id,Long memberId,String memberName,Long bookId,String bookTitle,Integer queuePosition,ReservationStatus status,LocalDateTime reservedAt,LocalDateTime notifiedAt,LocalDate expiresAt){}
