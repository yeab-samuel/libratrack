package com.libratrack.scheduler;

import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.repository.*;
import com.libratrack.service.FineCalculator;
import com.libratrack.service.NotificationService;
import com.libratrack.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueFineSchedulerTest {

    @Mock LoanRepository loanRepository;
    @Mock FineRecordRepository fineRepository;
    @Mock TokenBlacklistRepository tokenBlacklistRepository;
    @Mock ReservationService reservationService;
    @Mock NotificationService notificationService;
    @Mock FineCalculator fineCalculator;

    @InjectMocks OverdueFineScheduler scheduler;

    private User member;
    private Book book;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        member = User.builder()
                .id(1L).email("member@test.com").role(Role.STUDENT)
                .fullName("Member One").universityId("UGR/1234/20").active(true).build();
        book = Book.builder()
                .id(1L).isbn("isbn-1").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).totalCopies(1).build();
        copy = BookCopy.builder()
                .id(1L).book(book).copyNumber("C-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build();

        // Default Phase 2 stub — most tests don't exercise it; return empty so
        // Phase 2 completes without side-effects and tests stay focused.
        lenient().when(loanRepository.findAllByStatus(LoanStatus.OVERDUE))
                .thenReturn(List.of());
    }

    // ── calculateOverdueFines — Phase 1 (ACTIVE → OVERDUE) ──────────────────

    @Test
    void calculateOverdueFines_NoOverdueLoans_DoesNothing() {
        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.calculateOverdueFines();

        verify(loanRepository, never()).save(any());
        verify(fineRepository, never()).save(any());
        verifyNoInteractions(notificationService, fineCalculator);
    }

    @Test
    void calculateOverdueFines_WithinGracePeriod_LeavesLoanActiveAndDoesNotFine() {
        // 1 day late and 2 days late are both within the grace period per
        // FineCalculator — neither should be touched at all: no status
        // change, no fine, no notification. They'll be re-evaluated on a later run.
        Loan oneDay = Loan.builder()
                .id(1L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(1)).status(LoanStatus.ACTIVE).build();
        Loan twoDays = Loan.builder()
                .id(2L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(2)).status(LoanStatus.ACTIVE).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(oneDay, twoDays));
        when(fineCalculator.isBillable(1)).thenReturn(false);
        when(fineCalculator.isBillable(2)).thenReturn(false);

        scheduler.calculateOverdueFines();

        assertEquals(LoanStatus.ACTIVE, oneDay.getStatus());
        assertEquals(LoanStatus.ACTIVE, twoDays.getStatus());
        verify(loanRepository, never()).save(any());
        verify(fineRepository, never()).findByLoan(any());
        verify(fineRepository, never()).save(any());
        verify(fineCalculator, never()).calculate(anyLong());
        verifyNoInteractions(notificationService);
    }

    @Test
    void calculateOverdueFines_NoExistingFine_CreatesNewFineRecord() {
        Loan loan = Loan.builder()
                .id(1L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(3)).status(LoanStatus.ACTIVE).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(loan));
        when(fineRepository.findByLoan(loan)).thenReturn(Optional.empty());
        when(fineCalculator.isBillable(3)).thenReturn(true);
        when(fineCalculator.calculate(3)).thenReturn(new BigDecimal("0.50"));

        scheduler.calculateOverdueFines();

        assertEquals(LoanStatus.OVERDUE, loan.getStatus());
        verify(loanRepository).save(loan);

        ArgumentCaptor<FineRecord> captor = ArgumentCaptor.forClass(FineRecord.class);
        verify(fineRepository).save(captor.capture());
        FineRecord saved = captor.getValue();
        assertEquals(loan, saved.getLoan());
        assertEquals(member, saved.getMember());
        assertEquals(0, saved.getAmount().compareTo(new BigDecimal("0.50")));

        verify(notificationService).sendOverdueFineNotice(
                eq(member.getFullName()), eq(member.getEmail()), eq(member.getUniversityId()),
                eq("Clean Code"), eq(new BigDecimal("0.50")));
    }

    @Test
    void calculateOverdueFines_ExistingFine_UpdatesAmountOnSameRecord() {
        Loan loan = Loan.builder()
                .id(1L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(5)).status(LoanStatus.ACTIVE).build();
        FineRecord existing = FineRecord.builder()
                .id(9L).loan(loan).member(member)
                .amount(new BigDecimal("1.00")).status(FineStatus.UNPAID).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(loan));
        when(fineRepository.findByLoan(loan)).thenReturn(Optional.of(existing));
        when(fineCalculator.isBillable(5)).thenReturn(true);
        when(fineCalculator.calculate(5)).thenReturn(new BigDecimal("1.50"));

        scheduler.calculateOverdueFines();

        ArgumentCaptor<FineRecord> captor = ArgumentCaptor.forClass(FineRecord.class);
        verify(fineRepository).save(captor.capture());
        FineRecord saved = captor.getValue();

        assertSame(existing, saved, "Should update the existing FineRecord, not create a new one");
        assertEquals(0, saved.getAmount().compareTo(new BigDecimal("1.50")));
        verify(notificationService).sendOverdueFineNotice(
                eq(member.getFullName()), eq(member.getEmail()), eq(member.getUniversityId()),
                eq("Clean Code"), eq(new BigDecimal("1.50")));
    }

    @Test
    void calculateOverdueFines_MultipleLoans_ProcessesEachIndependently() {
        Loan loan1 = Loan.builder()
                .id(1L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(4)).status(LoanStatus.ACTIVE).build();

        User member2 = User.builder()
                .id(2L).email("member2@test.com").role(Role.FACULTY)
                .fullName("Member Two").universityId("FAC/5678/20").active(true).build();
        Book book2 = Book.builder()
                .id(2L).isbn("isbn-2").title("Effective Java").author("Joshua Bloch")
                .category(BookCategory.ENGINEERING).totalCopies(1).build();
        BookCopy copy2 = BookCopy.builder()
                .id(2L).book(book2).copyNumber("C-002")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build();
        Loan loan2 = Loan.builder()
                .id(2L).member(member2).bookCopy(copy2)
                .dueDate(LocalDate.now().minusDays(6)).status(LoanStatus.ACTIVE).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(loan1, loan2));
        when(fineRepository.findByLoan(any())).thenReturn(Optional.empty());
        when(fineCalculator.isBillable(4)).thenReturn(true);
        when(fineCalculator.calculate(4)).thenReturn(new BigDecimal("1.00"));
        when(fineCalculator.isBillable(6)).thenReturn(true);
        when(fineCalculator.calculate(6)).thenReturn(new BigDecimal("2.00"));

        scheduler.calculateOverdueFines();

        assertEquals(LoanStatus.OVERDUE, loan1.getStatus());
        assertEquals(LoanStatus.OVERDUE, loan2.getStatus());
        verify(loanRepository, times(2)).save(any());
        verify(fineRepository, times(2)).save(any());
        verify(notificationService, times(2))
                .sendOverdueFineNotice(any(), any(), any(), any(), any());
    }

    // ── calculateOverdueFines — Phase 2 (OVERDUE → silent recalculation) ────

    @Test
    void calculateOverdueFines_AlreadyOverdueLoan_RecalculatesFineWithoutNotification() {
        // This is the core bug scenario: a loan that went overdue two nights
        // ago. Phase 1 won't see it (it's OVERDUE, not ACTIVE). Phase 2 must
        // pick it up and update the fine amount — without sending another email.
        Loan overdуeLoan = Loan.builder()
                .id(10L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(5)).status(LoanStatus.OVERDUE).build();
        FineRecord existingFine = FineRecord.builder()
                .id(20L).loan(overdуeLoan).member(member)
                .amount(new BigDecimal("0.50"))   // frozen at day-3 amount
                .status(FineStatus.UNPAID).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());
        when(loanRepository.findAllByStatus(LoanStatus.OVERDUE))
                .thenReturn(List.of(overdуeLoan));
        when(fineRepository.findByLoan(overdуeLoan)).thenReturn(Optional.of(existingFine));
        when(fineCalculator.calculate(5)).thenReturn(new BigDecimal("1.50"));

        scheduler.calculateOverdueFines();

        // Fine amount must reflect today's full accrual, not the frozen day-3 amount
        assertEquals(0, existingFine.getAmount().compareTo(new BigDecimal("1.50")),
                "Phase 2 must update the fine to the current accrued amount");
        verify(fineRepository).save(existingFine);

        // Crucially: no second email — the member was already notified on night 1
        verifyNoInteractions(notificationService);
    }

    @Test
    void calculateOverdueFines_AlreadyOverduePaidFine_SkipsUpdate() {
        // A PAID fine should never be re-opened by the nightly recalculation.
        Loan overdуeLoan = Loan.builder()
                .id(11L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(4)).status(LoanStatus.OVERDUE).build();
        FineRecord paidFine = FineRecord.builder()
                .id(21L).loan(overdуeLoan).member(member)
                .amount(new BigDecimal("1.00"))
                .status(FineStatus.PAID).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());
        when(loanRepository.findAllByStatus(LoanStatus.OVERDUE))
                .thenReturn(List.of(overdуeLoan));
        when(fineRepository.findByLoan(overdуeLoan)).thenReturn(Optional.of(paidFine));

        scheduler.calculateOverdueFines();

        // Amount must not change and must not be saved
        assertEquals(0, paidFine.getAmount().compareTo(new BigDecimal("1.00")));
        verify(fineRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void calculateOverdueFines_AlreadyOverdueNoFineRecord_DoesNotCreateDuplicate() {
        // Edge case: OVERDUE loan with no fine record (e.g. data inconsistency).
        // Phase 2 must not create a new record — only Phase 1 does that.
        Loan overdуeLoan = Loan.builder()
                .id(12L).member(member).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(4)).status(LoanStatus.OVERDUE).build();

        when(loanRepository.findAllByStatusAndDueDateBefore(eq(LoanStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());
        when(loanRepository.findAllByStatus(LoanStatus.OVERDUE))
                .thenReturn(List.of(overdуeLoan));
        when(fineRepository.findByLoan(overdуeLoan)).thenReturn(Optional.empty());

        scheduler.calculateOverdueFines();

        verify(fineRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    // ── expireStaleReservations ──────────────────────────────────────────────

    @Test
    void expireStaleReservations_DelegatesToReservationService() {
        scheduler.expireStaleReservations();

        verify(reservationService, times(1)).expireStaleNotifications();
        verifyNoMoreInteractions(reservationService);
    }

    // ── cleanExpiredTokens ───────────────────────────────────────────────────

    @Test
    void cleanExpiredTokens_DeletesTokensExpiredBeforeNow() {
        scheduler.cleanExpiredTokens();

        verify(tokenBlacklistRepository, times(1))
                .deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}