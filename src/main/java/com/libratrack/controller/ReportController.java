package com.libratrack.controller;
import com.libratrack.dto.response.*;
import com.libratrack.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportController {
    private final LoanService loanService;
    private final FineService fineService;
    @GetMapping("/overdue") @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')") public ResponseEntity<Page<LoanDTO>> overdueLoans(@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(loanService.getOverdueLoans(pageable));}
    @GetMapping("/fines-summary") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<FineSummaryDTO> finesSummary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fromDate,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate toDate){return ResponseEntity.ok(fineService.getFinesSummary(fromDate,toDate));}
}
