package com.libratrack.controller;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.LoanDTO;
import com.libratrack.enums.LoanStatus;
import com.libratrack.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/loans") @RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    /** STUDENT / FACULTY — borrow a copy directly (self-service, no librarian needed) */
    @PostMapping("/borrow")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY')")
    public ResponseEntity<LoanDTO> borrowDirectly(
            @Valid @RequestBody BorrowRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(loanService.borrowDirectly(req, auth.getName()));
    }

    /** LIBRARIAN / ADMIN — issue a loan at the counter on behalf of a member */
    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<LoanDTO> createLoan(
            @Valid @RequestBody CreateLoanRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(loanService.createLoan(req, auth.getName()));
    }

    /** LIBRARIAN / ADMIN — process a book return */
    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<LoanDTO> returnLoan(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(loanService.returnLoan(id, auth.getName()));
    }

    /** ADMIN / LIBRARIAN — view all loans with optional filters */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<LoanDTO>> getAllLoans(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(memberId, status, pageable));
    }

    /** STUDENT / FACULTY — view their own loans */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY')")
    public ResponseEntity<Page<LoanDTO>> getMyLoans(
            Authentication auth, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.getMyLoans(auth.getName(), pageable));
    }

    /** Any authenticated user — view a specific loan (access-checked in service) */
    @GetMapping("/{id}")
    public ResponseEntity<LoanDTO> getLoan(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(loanService.getLoan(id, auth.getName()));
    }

    /** FACULTY only — extend their own loan's due date */
    @PatchMapping("/{id}/extend")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<LoanDTO> extendLoan(
            @PathVariable Long id,
            @Valid @RequestBody ExtendLoanRequest req,
            Authentication auth) {
        return ResponseEntity.ok(loanService.extendLoan(id, req, auth.getName()));
    }
}
