package com.libratrack.dto.response;
import com.libratrack.enums.FineStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FineDTO(
    Long id, Long loanId, Long memberId, String memberName,
    String bookTitle,
    BigDecimal amount, FineStatus status,
    LocalDateTime paidAt, Long collectedById,
    String waiveReason, LocalDateTime createdAt
) {}
