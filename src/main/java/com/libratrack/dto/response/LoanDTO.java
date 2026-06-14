package com.libratrack.dto.response;
import com.libratrack.enums.LoanStatus;
import java.time.*;
public record LoanDTO(Long id,Long memberId,String memberName,Long bookId,Long bookCopyId,String copyNumber,String bookTitle,LocalDateTime issuedAt,LocalDate dueDate,LocalDateTime returnedAt,LoanStatus status,Long processedById){}