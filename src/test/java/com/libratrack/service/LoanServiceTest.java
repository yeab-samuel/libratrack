package com.libratrack.service;
import com.libratrack.dto.request.CreateLoanRequest;
import com.libratrack.dto.request.ExtendLoanRequest;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.*;
import com.libratrack.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    @Mock LoanRepository loanRepository;
    @Mock UserRepository userRepository;
    @Mock BookCopyRepository copyRepository;
    @Mock FineRecordRepository fineRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock ReservationService reservationService;

    @InjectMocks LoanService loanService;

    private User student;
    private BookCopy copy;
    @BeforeEach void setup(){
        try {
            student = User.builder().id(1L).email("s@test.com").role(Role.STUDENT).fullName("Student").active(true).build();
            Book book = Book.builder().id(1L).title("Book").build();
            copy = BookCopy.builder().id(1L).book(book).copyNumber("C-001").status(CopyStatus.AVAILABLE).condition(CopyCondition.GOOD).build();

            // Set the limit values using reflection
            Field maxLoansStudentField = LoanService.class.getDeclaredField("maxLoansStudent");
            maxLoansStudentField.setAccessible(true);
            maxLoansStudentField.set(loanService, 3);

            Field maxLoansFacultyField = LoanService.class.getDeclaredField("maxLoansFaculty");
            maxLoansFacultyField.setAccessible(true);
            maxLoansFacultyField.set(loanService, 5);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set borrow limits for testing", e);
        }
    }
    @Test void createLoan_HappyPath(){
        User lib=User.builder().id(2L).email("lib@test.com").role(Role.LIBRARIAN).fullName("Lib").active(true).build();
        Loan saved=Loan.builder().id(1L).member(student).bookCopy(copy).dueDate(LocalDate.now().plusDays(14)).processedBy(lib).build();
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(fineRepository.existsByMemberAndStatus(student,FineStatus.UNPAID)).thenReturn(false);
        when(loanRepository.countByMemberAndStatus(student,LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.countByMemberAndStatus(student,LoanStatus.OVERDUE)).thenReturn(0L);
        // when(loanRepository.countByMemberAndStatus(student, LoanStatus.BORROWED)).thenReturn(0L);
        // when(loanRepository.countByMember(student)).thenReturn(0L);
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(lib));
        when(loanRepository.save(any())).thenReturn(saved);
        when(reservationRepository.findFirstByBookAndStatusOrderByQueuePositionAsc(
                any(Book.class), eq(ReservationStatus.NOTIFIED)))
                .thenReturn(Optional.empty());
        var result=loanService.createLoan(new CreateLoanRequest(1L,1L,LocalDate.now().plusDays(14)),"lib@test.com");
        assertNotNull(result);assertEquals(1L,result.id());
    }
    @Test void createLoan_CopyNotAvailable_Throws409(){copy.setStatus(CopyStatus.ON_LOAN);when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        assertThrows(NoCopyAvailableException.class,()->loanService.createLoan(new CreateLoanRequest(1L,1L,LocalDate.now().plusDays(14)),"lib@test.com"));}
    @Test void createLoan_UnpaidFine_Throws422(){when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));when(userRepository.findById(1L)).thenReturn(Optional.of(student));when(fineRepository.existsByMemberAndStatus(student,FineStatus.UNPAID)).thenReturn(true);
        when(reservationRepository.findFirstByBookAndStatusOrderByQueuePositionAsc(
                any(Book.class), any(ReservationStatus.class)))
                .thenReturn(Optional.empty());
        assertThrows(UnpaidFineException.class,()->loanService.createLoan(new CreateLoanRequest(1L,1L,LocalDate.now().plusDays(14)),"lib@test.com"));}
    @Test void createLoan_BorrowLimit_Throws422(){when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));when(userRepository.findById(1L)).thenReturn(Optional.of(student));when(fineRepository.existsByMemberAndStatus(student,FineStatus.UNPAID)).thenReturn(false);when(loanRepository.countByMemberAndStatus(student,LoanStatus.ACTIVE)).thenReturn(3L);when(loanRepository.countByMemberAndStatus(student,LoanStatus.OVERDUE)).thenReturn(0L);
        when(reservationRepository.findFirstByBookAndStatusOrderByQueuePositionAsc(
                any(Book.class), any(ReservationStatus.class)))
                .thenReturn(Optional.empty());
        assertThrows(BorrowLimitExceededException.class,()->loanService.createLoan(new CreateLoanRequest(1L,1L,LocalDate.now().plusDays(14)),"lib@test.com"));}

    // ── extendLoan tests ──────────────────────────────────────────────────────

    @Test
    void extendLoan_HappyPath_ReturnsDTOWithNewDate() {
        User faculty = User.builder().id(1L).email("f@test.com").role(Role.FACULTY).fullName("Faculty").active(true).build();
        LocalDate currentDue = LocalDate.now().plusDays(7);
        LocalDate newDue = LocalDate.now().plusDays(21);
        Loan loan = Loan.builder().id(1L).member(faculty).bookCopy(copy).dueDate(currentDue).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("f@test.com")).thenReturn(Optional.of(faculty));
        when(loanRepository.save(any())).thenReturn(loan);
        var result = loanService.extendLoan(1L, new ExtendLoanRequest(newDue), "f@test.com");
        assertNotNull(result);
        assertEquals(newDue, loan.getDueDate());
    }

    @Test
    void extendLoan_NonOwner_ThrowsAccessDenied() {
        User faculty = User.builder().id(1L).email("f@test.com").role(Role.FACULTY).fullName("Faculty").active(true).build();
        User other = User.builder().id(2L).email("other@test.com").role(Role.FACULTY).fullName("Other").active(true).build();
        Loan loan = Loan.builder().id(1L).member(other).bookCopy(copy).dueDate(LocalDate.now().plusDays(7)).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("f@test.com")).thenReturn(Optional.of(faculty));
        assertThrows(AccessDeniedException.class,
                () -> loanService.extendLoan(1L, new ExtendLoanRequest(LocalDate.now().plusDays(21)), "f@test.com"));
    }

    @Test
    void extendLoan_OverdueLoan_ThrowsNoCopyAvailableException() {
        User faculty = User.builder().id(1L).email("f@test.com").role(Role.FACULTY).fullName("Faculty").active(true).build();
        Loan loan = Loan.builder().id(1L).member(faculty).bookCopy(copy).dueDate(LocalDate.now().minusDays(5)).status(LoanStatus.OVERDUE).build();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("f@test.com")).thenReturn(Optional.of(faculty));
        assertThrows(NoCopyAvailableException.class,
                () -> loanService.extendLoan(1L, new ExtendLoanRequest(LocalDate.now().plusDays(7)), "f@test.com"));
    }

    @Test
    void extendLoan_NewDateNotAfterCurrentDue_ThrowsIllegalArgument() {
        User faculty = User.builder().id(1L).email("f@test.com").role(Role.FACULTY).fullName("Faculty").active(true).build();
        LocalDate currentDue = LocalDate.now().plusDays(14);
        Loan loan = Loan.builder().id(1L).member(faculty).bookCopy(copy).dueDate(currentDue).status(LoanStatus.ACTIVE).build();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("f@test.com")).thenReturn(Optional.of(faculty));
        // Same date as current due — not after
        assertThrows(IllegalArgumentException.class,
                () -> loanService.extendLoan(1L, new ExtendLoanRequest(currentDue), "f@test.com"));
    }
}
