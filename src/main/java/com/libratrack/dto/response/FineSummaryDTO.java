package com.libratrack.dto.response;
import java.math.BigDecimal;
import java.time.LocalDate;
public record FineSummaryDTO(BigDecimal totalAmount,long totalCount,LocalDate fromDate,LocalDate toDate){}
