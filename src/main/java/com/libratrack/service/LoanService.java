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
    private final FineCalculator fineCalculator;

    @Value("${app.max-loans-student:3}")
    private int maxLoansStudent;
    @Value("${app.max-loans-faculty:5}")
    private int maxLoansFaculty;
    @Value("${app.max-loan-days-student:14}")
    private int maxLoanDaysStudent;
    @Value("${app.max-loan-days-faculty:30}")
    private int maxLoanDaysFaculty;

    // ── STUDENT / FACULTY self-service borrow ─────────────────────────────────

    @Transactional
    public LoanDTO borrowDirectly(BorrowRequest req, String memberEmail) {
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateLoanDuration(req.dueDate(), member.getRole());

        BookCopy copy = copyRepository.findById(req.bookCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found: " + req.bookCopyId()));

        if (copy.getStatus() != CopyStatus.AVAILABLE)
            throw new NoCopyAvailableException("Copy is not available: " + copy.getCopyNumber());

        checkReservationEnforcement(copy.getBook(), member);
        validateBorrowEligibility(member);

        copy.setStatus(CopyStatus.ON_LOAN);
        copyRepository.save(copy);

        Loan loan = loanRepository.save(Loan.builder()
                .member(member)
                .bookCopy(copy)
                .dueDate(req.dueDate())
                .processedBy(member)
                .build());

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

        validateLoanDuration(req.dueDate(), member.getRole());

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

        if (LocalDate.now().isAfter(loan.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            if (fineCalculator.isBillable(daysLate)) {
                BigDecimal amount = fineCalculator.calculate(daysLate);
                fineRepository.findByLoan(loan).ifPresentOrElse(
                        f -> {
                            f.setAmount(amount);
                            fineRepository.save(f);
                        },
                        () -> fineRepository.save(FineRecord.builder()
                                .loan(loan).member(loan.getMember()).amount(amount).build())
                );
                log.info("Fine created: member={} daysLate={} amount={}",
                        loan.getMember().getEmail(), daysLate, amount);
            }
        }

        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copyRepository.save(copy);
        loanRepository.save(loan);

        reservationService.notifyNextInQueue(copy.getBook());
        return toDTO(loan);
    }

    // ── QUERIES ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<LoanDTO> getAllLoans(Long memberId, LoanStatus status, Pageable pageable) {
        return loanRepository.findWithFilters(memberId, status, pageable).map(this::toDTO);
    }

    /**
     * STUDENT / FACULTY — view their own loans, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<LoanDTO> getMyLoans(String email, LoanStatus status, Pageable pageable) {
        User m = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return loanRepository.findWithFilters(m.getId(), status, pageable).map(this::toDTO);
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

        validateLoanDuration(req.newDueDate(), loan.getMember().getRole());

        loan.setDueDate(req.newDueDate());
        return toDTO(loanRepository.save(loan));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Caps how far out a due date (new borrow, counter loan, or extension) can
     * be set, based on the member's role — 14 days for students, 30 for
     * faculty by default. Always measured from today, not from issuedAt, so
     * the same rule applies cleanly whether this is an initial borrow or a
     * later extension.
     */
    private void validateLoanDuration(LocalDate dueDate, Role role) {
        int maxDays = (role == Role.FACULTY) ? maxLoanDaysFaculty : maxLoanDaysStudent;
        LocalDate latestAllowed = LocalDate.now().plusDays(maxDays);
        if (dueDate.isAfter(latestAllowed))
            throw new IllegalArgumentException(
                    "Due date exceeds the maximum loan period of " + maxDays + " days for " + role +
                            ". Latest allowed due date: " + latestAllowed);
    }

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

        long active = loanRepository.countByMemberAndStatus(member, LoanStatus.ACTIVE);
        long overdue = loanRepository.countByMemberAndStatus(member, LoanStatus.OVERDUE);
        int limit = (member.getRole() == Role.FACULTY) ? maxLoansFaculty : maxLoansStudent;
        if (active + overdue >= limit)
            throw new BorrowLimitExceededException(
                    "Borrow limit of " + limit + " reached. Return a book first.");
    }

    public LoanDTO toDTO(Loan l) {
        return new LoanDTO(
                l.getId(), l.getMember().getId(), l.getMember().getFullName(),
                l.getMember().getUniversityId(),
                l.getBookCopy().getBook().getId(),
                l.getBookCopy().getId(), l.getBookCopy().getCopyNumber(),
                l.getBookCopy().getBook().getTitle(),
                l.getIssuedAt(), l.getDueDate(), l.getReturnedAt(), l.getStatus(),
                l.getProcessedBy() != null ? l.getProcessedBy().getId() : null
        );
    }
}