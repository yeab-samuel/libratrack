package com.libratrack.repository;

import com.libratrack.entity.*;
import com.libratrack.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired LoanRepository loanRepository;
    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;

    private User userA;
    private User userB;
    private BookCopy copyA;
    private BookCopy copyB;
    private BookCopy copyC;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("usera@test.com").passwordHash("hash")
                .fullName("User A").role(Role.STUDENT).active(true).build());
        userB = userRepository.save(User.builder()
                .email("userb@test.com").passwordHash("hash")
                .fullName("User B").role(Role.STUDENT).active(true).build());

        Book book = bookRepository.save(Book.builder()
                .isbn("978-0001").title("Test Book").author("Author")
                .category(BookCategory.SCIENCE).totalCopies(3).build());

        copyA = bookCopyRepository.save(BookCopy.builder()
                .book(book).copyNumber("C-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build());
        copyB = bookCopyRepository.save(BookCopy.builder()
                .book(book).copyNumber("C-002")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build());
        copyC = bookCopyRepository.save(BookCopy.builder()
                .book(book).copyNumber("C-003")
                .condition(CopyCondition.GOOD).status(CopyStatus.ON_LOAN).build());
    }

    // ── 1. (original) countByMemberAndStatus ────────────────────────────────
    @Test
    void countByMemberAndStatus_ReturnsCorrectCount() {
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyA)
                .dueDate(LocalDate.now().plusDays(7))
                .status(LoanStatus.ACTIVE).build());

        assertEquals(1L, loanRepository.countByMemberAndStatus(userA, LoanStatus.ACTIVE));
        assertEquals(0L, loanRepository.countByMemberAndStatus(userA, LoanStatus.RETURNED));
    }

    // ── 2. findByMember_returnsOnlyThatMembersLoans ──────────────────────────
    @Test
    void findByMember_returnsOnlyThatMembersLoans() {
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyA)
                .dueDate(LocalDate.now().plusDays(7))
                .status(LoanStatus.ACTIVE).build());
        loanRepository.save(Loan.builder()
                .member(userB).bookCopy(copyB)
                .dueDate(LocalDate.now().plusDays(7))
                .status(LoanStatus.ACTIVE).build());

        Page<Loan> result = loanRepository.findByMember(userA, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(userA.getId(), result.getContent().get(0).getMember().getId());
    }

    // ── 3. findByStatus_returnsMatchingLoans ─────────────────────────────────
    @Test
    void findByStatus_returnsMatchingLoans() {
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyA)
                .dueDate(LocalDate.now().plusDays(7))
                .status(LoanStatus.ACTIVE).build());
        loanRepository.save(Loan.builder()
                .member(userB).bookCopy(copyB)
                .dueDate(LocalDate.now().minusDays(1))
                .status(LoanStatus.OVERDUE).build());

        Page<Loan> result = loanRepository.findByStatus(LoanStatus.OVERDUE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(LoanStatus.OVERDUE, result.getContent().get(0).getStatus());
    }

    // ── 4. findWithFilters_byMemberAndStatus ─────────────────────────────────
    @Test
    void findWithFilters_byMemberAndStatus() {
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyA)
                .dueDate(LocalDate.now().plusDays(7))
                .status(LoanStatus.ACTIVE).build());
        loanRepository.save(Loan.builder()
                .member(userB).bookCopy(copyB)
                .dueDate(LocalDate.now().minusDays(1))
                .status(LoanStatus.OVERDUE).build());
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyC)
                .dueDate(LocalDate.now().minusDays(5))
                .status(LoanStatus.OVERDUE).build());

        Page<Loan> result = loanRepository.findWithFilters(
                userA.getId(), LoanStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(userA.getId(), result.getContent().get(0).getMember().getId());
        assertEquals(LoanStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

    // ── 5. findOverdueLoans_returnsLoansPastDueDate ──────────────────────────
    @Test
    void findOverdueLoans_returnsLoansPastDueDate() {
        // loan with yesterday's due date and ACTIVE status (not yet marked OVERDUE)
        loanRepository.save(Loan.builder()
                .member(userA).bookCopy(copyA)
                .dueDate(LocalDate.now().minusDays(1))
                .status(LoanStatus.ACTIVE).build());

        List<Loan> overdue = loanRepository.findAllByStatusAndDueDateBefore(
                LoanStatus.ACTIVE, LocalDate.now());

        assertEquals(1, overdue.size());
        assertTrue(overdue.get(0).getDueDate().isBefore(LocalDate.now()));
    }
}
