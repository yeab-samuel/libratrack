package com.libratrack.service;

import com.libratrack.dto.request.CreateReservationRequest;
import com.libratrack.dto.response.ReservationDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.*;
import com.libratrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock BookRepository bookRepository;
    @Mock BookCopyRepository copyRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ReservationService reservationService;

    private User student;
    private User admin;
    private Book book;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .email("student@test.com").fullName("Test Student")
                .role(Role.STUDENT).active(true).build();
        setId(student, 1L);

        admin = User.builder()
                .email("admin@test.com").fullName("Admin User")
                .role(Role.ADMIN).active(true).build();
        setId(admin, 2L);

        book = Book.builder()
                .isbn("978-0001").title("Test Book").author("Author")
                .category(BookCategory.SCIENCE).totalCopies(1).build();
        setId(book, 10L);

        reservation = Reservation.builder()
                .member(student).book(book).queuePosition(1).build();
        setId(reservation, 100L);
    }

    // Helper to set ID via reflection (entities use @GeneratedValue)
    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createReservation_WhenNoCopiesAvailableAndNoExisting_Succeeds() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(copyRepository.countByBookAndStatus(book, CopyStatus.AVAILABLE)).thenReturn(0L);
        when(reservationRepository.existsByMemberAndBookAndStatusIn(any(), any(), any())).thenReturn(false);
        when(reservationRepository.findMaxQueuePositionByBook(book)).thenReturn(0);
        when(reservationRepository.save(any())).thenReturn(reservation);

        ReservationDTO result = reservationService.createReservation(
                new CreateReservationRequest(10L), "student@test.com");

        assertNotNull(result);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_WhenCopyAvailable_ThrowsNoCopyAvailableException() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(copyRepository.countByBookAndStatus(book, CopyStatus.AVAILABLE)).thenReturn(1L);

        assertThrows(NoCopyAvailableException.class, () ->
                reservationService.createReservation(
                        new CreateReservationRequest(10L), "student@test.com"));
    }

    @Test
    void createReservation_WhenDuplicateActiveReservation_ThrowsDuplicateResourceException() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(copyRepository.countByBookAndStatus(book, CopyStatus.AVAILABLE)).thenReturn(0L);
        when(reservationRepository.existsByMemberAndBookAndStatusIn(
                eq(student), eq(book), eq(List.of(ReservationStatus.WAITING, ReservationStatus.NOTIFIED))))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                reservationService.createReservation(
                        new CreateReservationRequest(10L), "student@test.com"));
    }

    @Test
    void cancelReservation_ByOwner_Succeeds() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));

        reservationService.cancelReservation(100L, "student@test.com");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservation_ByDifferentStudent_ThrowsAccessDeniedException() {
        User otherStudent = User.builder()
                .email("other@test.com").fullName("Other").role(Role.STUDENT).active(true).build();
        setId(otherStudent, 99L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherStudent));

        assertThrows(AccessDeniedException.class, () ->
                reservationService.cancelReservation(100L, "other@test.com"));
    }

    @Test
    void cancelReservation_ByAdmin_Succeeds() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        reservationService.cancelReservation(100L, "admin@test.com");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void notifyNextInQueue_WhenWaitingReservationExists_SetsStatusToNotified() {
        when(reservationRepository.findFirstByBookAndStatusOrderByQueuePositionAsc(book, ReservationStatus.WAITING))
                .thenReturn(Optional.of(reservation));

        reservationService.notifyNextInQueue(book);

        assertEquals(ReservationStatus.NOTIFIED, reservation.getStatus());
        assertNotNull(reservation.getNotifiedAt());
        assertNotNull(reservation.getExpiresAt());
        verify(reservationRepository).save(reservation);
    }
}
