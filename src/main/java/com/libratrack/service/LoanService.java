package com.libratrack.service;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.LoanDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.*;
import com.libratrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Service @RequiredArgsConstructor @Slf4j
public class LoanService {
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookCopyRepository copyRepository;
    private final FineRecordRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Value("${app.daily-fine-rate:0.50}") private BigDecimal dailyFineRate;
    @Value("${app.max-loans-student:3}")  private int maxLoansStudent;
    @Value("${app.max-loans-faculty:5}")  private int maxLoansFaculty;

    // ── STUDENT / FACULTY self-service borrow ─────────────────────────────────

    /**
     * A logged-in STUDENT or FACULTY member borrows a copy directly.
     * No librarian interaction needed — the authenticated user is the borrower.
     */
    @Transactional
    public LoanDTO borrowDirectly(BorrowRequest req, String memberEmail) {
        User member = userRepository.findByEmail(memberEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BookCopy copy = copyRepository.findById(req.bookCopyId())
            .orElseThrow(() -> new ResourceNotFoundException("Copy not found: " + req.bookCopyId()));

        if (copy.getStatus() != CopyStatus.AVAILABLE)
            throw new NoCopyAvailableException("Copy is not available: " + copy.getCopyNumber());

        // Enforce reservation priority: if someone is NOTIFIED for this book, only they can borrow
        checkReservationEnforcement(copy.getBook(), member);

        validateBorrowEligibility(member);

        copy.setStatus(CopyStatus.ON_LOAN);
        copyRepository.save(copy);

        Loan loan = loanRepository.save(Loan.builder()
            .member(member)
            .bookCopy(copy)
            .dueDate(req.dueDate())
            .processedBy(member)   // self-service: member is their own processor
            .build());

        // If this member had a WAITING or NOTIFIED reservation for this book, fulfil it
        reservationRepository
            .findFirstByBookAndStatusOrderByQueuePositionAsc(copy.getBook(), ReservationStatus.NOTIFIED)
            .filter(r -> r.getMember().getId().equals(member.getId()))
            .ifPresent(r -> {
                r.setStatus(ReservationStatus.FULFILLED);
                reservationRepository.save(r);
            });
        reservationRepository
            .findFirstByBookAndStatusOrderByQueuePositionAsc(copy.getBook(), ReservationStatus.WAITING)
            .filter(r -> r.getMember().getId().equals(member.getId()))
            .ifPresent(r -> {
                r.setStatus(ReservationStatus.FULFILLED);
                reservationRepository.save(r);
            });

        log.info("Self-borrow: user={} copy={} due={}", memberEmail, copy.getCopyNumber(), req.dueDate());
        return toDTO(loan);
    }

    // ── LIBRARIAN / ADMIN counter loan ───────────────────────────────────────

    /**
     * Librarian or Admin issues a loan on behalf of a member at the counter.
     */
    @Transactional
    public LoanDTO createLoan(CreateLoanRequest req, String staffEmail) {
        BookCopy copy = copyRepository.findById(req.bookCopyId())
            .orElseThrow(() -> new ResourceNotFoundException("Copy not found: " + req.bookCopyId()));

        if (copy.getStatus() != CopyStatus.AVAILABLE)
            throw new NoCopyAvailableException("Copy not available: " + copy.getCopyNumber());

        User member = userRepository.findById(req.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + req.memberId()));

        if (!member.isEnabled())
            throw new ResourceNotFoundException("Member account is inactive.");

        // Enforce reservation priority
        checkReservationEnforcement(copy.getBook(), member);

        validateBorrowEligibility(member);

        User staff = userRepository.findByEmail(staffEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        copy.setStatus(CopyStatus.ON_LOAN);
        copyRepository.save(copy);

        Loan loan = loanRepository.save(Loan.builder()
            .member(member)
            .bookCopy(copy)
            .dueDate(req.dueDate())
            .processedBy(staff)
            .build());

        // Fulfil reservation if member had one
        reservationRepository
            .findFirstByBookAndStatusOrderByQueuePositionAsc(copy.getBook(), ReservationStatus.NOTIFIED)
            .filter(r -> r.getMember().getId().equals(member.getId()))
            .ifPresent(r -> {
                r.setStatus(ReservationStatus.FULFILLED);
                reservationRepository.save(r);
            });

        log.info("Counter loan: staff={} member={} copy={}", staffEmail, member.getEmail(), copy.getCopyNumber());
        return toDTO(loan);
    }

    // ── RETURN ────────────────────────────────────────────────────────────────

    @Transactional
    public LoanDTO returnLoan(Long loanId, String staffEmail) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED)
            throw new NoCopyAvailableException("Loan already returned.");

        User staff = userRepository.findByEmail(staffEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        loan.setReturnedAt(LocalDateTime.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setProcessedBy(staff);

        // Calculate fine if overdue
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            long days = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            BigDecimal amount = dailyFineRate.multiply(BigDecimal.valueOf(days));
            fineRepository.findByLoan(loan).ifPresentOrElse(
                f -> { f.setAmount(amount); fineRepository.save(f); },
                () -> fineRepository.save(FineRecord.builder()
                    .loan(loan).member(loan.getMember()).amount(amount).build())
            );
            log.info("Fine created: member={} days={} amount={}", loan.getMember().getEmail(), days, amount);
        }

        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copyRepository.save(copy);
        loanRepository.save(loan);

        // Notify next person in queue (faculty-priority order)
        reservationService.notifyNextInQueue(copy.getBook());
        return toDTO(loan);
    }

    // ── QUERIES ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<LoanDTO> getAllLoans(Long memberId, LoanStatus status, Pageable pageable) {
        return loanRepository.findWithFilters(memberId, status, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LoanDTO> getMyLoans(String email, Pageable pageable) {
        User m = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return loanRepository.findByMember(m, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public LoanDTO getLoan(Long id, String email) {
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + id));
        User caller = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if ((caller.getRole() == Role.STUDENT || caller.getRole() == Role.FACULTY)
                && !loan.getMember().getId().equals(caller.getId()))
            throw new AccessDeniedException("Access denied to loan: " + id);
        return toDTO(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanDTO> getOverdueLoans(Pageable pageable) {
        return loanRepository.findByStatus(LoanStatus.OVERDUE, pageable).map(this::toDTO);
    }

    /** FACULTY only — extend their own loan's due date. */
    @Transactional
    public LoanDTO extendLoan(Long loanId, ExtendLoanRequest req, String facultyEmail) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        User caller = userRepository.findByEmail(facultyEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!loan.getMember().getId().equals(caller.getId()))
            throw new AccessDeniedException("You can only extend your own loans.");
        if (loan.getStatus() != LoanStatus.ACTIVE)
            throw new NoCopyAvailableException("Only ACTIVE loans can be extended.");
        if (!req.newDueDate().isAfter(loan.getDueDate()))
            throw new IllegalArgumentException("New due date must be after current due date: " + loan.getDueDate());

        loan.setDueDate(req.newDueDate());
        return toDTO(loanRepository.save(loan));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * If a NOTIFIED reservation exists for this book, only the notified member may borrow it.
     * Protects the reservation queue from being bypassed.
     */
    private void checkReservationEnforcement(Book book, User member) {
        reservationRepository
            .findFirstByBookAndStatusOrderByQueuePositionAsc(book, ReservationStatus.NOTIFIED)
            .ifPresent(notified -> {
                if (!notified.getMember().getId().equals(member.getId()))
                    throw new NoCopyAvailableException(
                        "This book is currently reserved for another member who has been notified. " +
                        "Please wait for their collection window to expire.");
            });
    }

    private void validateBorrowEligibility(User member) {
        if (fineRepository.existsByMemberAndStatus(member, FineStatus.UNPAID))
            throw new UnpaidFineException("Member has unpaid fines. Please settle before borrowing.");

        long active  = loanRepository.countByMemberAndStatus(member, LoanStatus.ACTIVE);
        long overdue = loanRepository.countByMemberAndStatus(member, LoanStatus.OVERDUE);
        int  limit   = (member.getRole() == Role.FACULTY) ? maxLoansFaculty : maxLoansStudent;
        if (active + overdue >= limit)
            throw new BorrowLimitExceededException(
                "Borrow limit of " + limit + " reached. Return a book first.");
    }

    public LoanDTO toDTO(Loan l) {
        return new LoanDTO(
                l.getId(), l.getMember().getId(), l.getMember().getFullName(),
                l.getBookCopy().getBook().getId(),
                l.getBookCopy().getId(), l.getBookCopy().getCopyNumber(),
                l.getBookCopy().getBook().getTitle(),
                l.getIssuedAt(), l.getDueDate(), l.getReturnedAt(), l.getStatus(),
                l.getProcessedBy() != null ? l.getProcessedBy().getId() : null
        );
    }
}
