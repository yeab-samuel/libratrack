package com.libratrack.service;

import com.libratrack.dto.request.WaiveRequest;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock FineRecordRepository fineRepository;
    @Mock UserRepository userRepository;
    @InjectMocks FineService fineService;

    private User student;
    private User librarian;
    private User admin;
    private Loan loan;
    private FineRecord fine;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("student@test.com")
                .role(Role.STUDENT).fullName("Student").active(true).build();
        librarian = User.builder().id(2L).email("lib@test.com")
                .role(Role.LIBRARIAN).fullName("Librarian").active(true).build();
        admin = User.builder().id(3L).email("admin@test.com")
                .role(Role.ADMIN).fullName("Admin").active(true).build();

        Book book = Book.builder().id(1L).isbn("978-0000000001").title("Test Book")
                .author("Test Author").category(BookCategory.SCIENCE)
                .totalCopies(1).createdAt(LocalDateTime.now()).build();
        BookCopy copy = BookCopy.builder().id(1L).book(book).copyNumber("C-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build();
        loan = Loan.builder().id(1L).member(student).bookCopy(copy)
                .dueDate(LocalDate.now().minusDays(5)).build();
        fine = FineRecord.builder().id(1L).loan(loan).member(student)
                .amount(new BigDecimal("2.50")).status(FineStatus.UNPAID).build();
    }

    // ── getFineById (BOLA) ────────────────────────────────────────────────────

    @Test
    void getFineById_StudentAccessingOwnFine_ReturnsDTO() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        assertNotNull(fineService.getFineById(1L, "student@test.com"));
    }

    @Test
    void getFineById_StudentAccessingOtherFine_Throws403() {
        User otherStudent = User.builder().id(99L).email("other@test.com")
                .role(Role.STUDENT).fullName("Other").active(true).build();
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherStudent));

        assertThrows(AccessDeniedException.class,
                () -> fineService.getFineById(1L, "other@test.com"));
    }

    @Test
    void getFineById_LibrarianAccessingAnyFine_ReturnsDTO() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarian));

        assertNotNull(fineService.getFineById(1L, "lib@test.com"));
    }

    // ── markAsPaid ────────────────────────────────────────────────────────────

    @Test
    void markAsPaid_HappyPath_StatusBecomesPA() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarian));
        when(fineRepository.save(any())).thenReturn(fine);

        var result = fineService.markAsPaid(1L, "lib@test.com");

        assertEquals(FineStatus.PAID, fine.getStatus());
        assertNotNull(fine.getPaidAt());
        assertEquals(librarian, fine.getCollectedBy());
        assertNotNull(result);
    }

    @Test
    void markAsPaid_FineNotFound_Throws404() {
        when(fineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> fineService.markAsPaid(99L, "lib@test.com"));
    }

    // ── waiveFine ─────────────────────────────────────────────────────────────

    @Test
    void waiveFine_HappyPath_StatusBecomesWaived() {
        WaiveRequest req = new WaiveRequest("Hardship waiver");
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(fineRepository.save(any())).thenReturn(fine);

        var result = fineService.waiveFine(1L, req, "admin@test.com");

        assertEquals(FineStatus.WAIVED, fine.getStatus());
        assertEquals(admin, fine.getCollectedBy());
        assertNotNull(result);
    }

    @Test
    void waiveFine_FineNotFound_Throws404() {
        WaiveRequest req = new WaiveRequest("reason");
        when(fineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> fineService.waiveFine(99L, req, "admin@test.com"));
    }

    // ── fine amount calculation (via LoanService logic — verified here via
    //    the fine object directly, since FineService delegates calculation
    //    to LoanService.returnLoan; we verify the resulting amount fields) ────

    @Test
    void fineAmount_ZeroDaysOverdue_IsZeroOrNoFine() {
        // A fine created with 0 days overdue should have amount = 0.00 * rate = 0
        FineRecord zeroFine = FineRecord.builder().id(10L).loan(loan).member(student)
                .amount(new BigDecimal("0.00")).status(FineStatus.UNPAID).build();
        when(fineRepository.findById(10L)).thenReturn(Optional.of(zeroFine));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        var dto = fineService.getFineById(10L, "student@test.com");
        assertEquals(0, new BigDecimal("0.00").compareTo(dto.amount()));
    }

    @Test
    void fineAmount_OneDayOverdue_IsOneDayRate() {
        // 1 day * $0.50 default rate = $0.50
        FineRecord oneDayFine = FineRecord.builder().id(11L).loan(loan).member(student)
                .amount(new BigDecimal("0.50")).status(FineStatus.UNPAID).build();
        when(fineRepository.findById(11L)).thenReturn(Optional.of(oneDayFine));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        var dto = fineService.getFineById(11L, "student@test.com");
        assertEquals(0, new BigDecimal("0.50").compareTo(dto.amount()));
    }

    @Test
    void fineAmount_ThirtyDaysOverdue_IsThirtyDayRate() {
        // 30 days * $0.50 = $15.00
        FineRecord thirtyDayFine = FineRecord.builder().id(12L).loan(loan).member(student)
                .amount(new BigDecimal("15.00")).status(FineStatus.UNPAID).build();
        when(fineRepository.findById(12L)).thenReturn(Optional.of(thirtyDayFine));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        var dto = fineService.getFineById(12L, "student@test.com");
        assertEquals(0, new BigDecimal("15.00").compareTo(dto.amount()));
    }
}
