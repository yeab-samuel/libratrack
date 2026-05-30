package com.libratrack.service;
import com.libratrack.dto.request.CreateReservationRequest;
import com.libratrack.dto.response.ReservationDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.exception.*;
import com.libratrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservationDTO createReservation(CreateReservationRequest req, String email) {
        Book book = bookRepository.findById(req.bookId())
            .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + req.bookId()));
        User member = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (copyRepository.countByBookAndStatus(book, CopyStatus.AVAILABLE) > 0)
            throw new NoCopyAvailableException("Copies available. Borrow directly from the catalogue.");

        if (reservationRepository.existsByMemberAndBookAndStatusIn(
                member, book, List.of(ReservationStatus.WAITING, ReservationStatus.NOTIFIED)))
            throw new DuplicateResourceException("You already have an active reservation for this book.");

        // FACULTY get priority: inserted before any STUDENT entries in the queue
        int pos;
        if (member.getRole() == Role.FACULTY) {
            int firstStudentPos = reservationRepository
                .findFirstStudentQueuePosition(book)
                .orElse(reservationRepository.findMaxQueuePositionByBook(book) + 1);
            // Shift all students at or after that position down by 1
            reservationRepository.incrementQueuePositionsFrom(book, firstStudentPos);
            pos = firstStudentPos;
        } else {
            pos = reservationRepository.findMaxQueuePositionByBook(book) + 1;
        }

        Reservation saved = reservationRepository.save(
            Reservation.builder()
                .member(member)
                .book(book)
                .queuePosition(pos)
                .build()
        );
        log.info("Reservation created: user={} book='{}' pos={} role={}",
            member.getEmail(), book.getTitle(), pos, member.getRole());
        return toDTO(saved);
    }

    @Transactional
    public void cancelReservation(Long id, String email) {
        Reservation r = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        User caller = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (caller.getRole() != Role.ADMIN && !r.getMember().getId().equals(caller.getId()))
            throw new AccessDeniedException("Access denied");

        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    @Transactional(readOnly = true)
    public Page<ReservationDTO> getMyReservations(String email, Pageable pageable) {
        User m = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reservationRepository.findByMember(m, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ReservationDTO> getAllReservations(Long bookId, ReservationStatus status, Pageable pageable) {
        return reservationRepository.findWithFilters(bookId, status, pageable).map(this::toDTO);
    }

    /**
     * Called when a copy is returned.
     * Finds the next WAITING reservation (faculty-priority order) and notifies them.
     */
    @Transactional
    public void notifyNextInQueue(Book book) {
        reservationRepository
            .findFirstByBookAndStatusOrderByQueuePositionAsc(book, ReservationStatus.WAITING)
            .ifPresent(r -> {
                LocalDate expiresAt = LocalDate.now().plusDays(3);
                r.setStatus(ReservationStatus.NOTIFIED);
                r.setNotifiedAt(LocalDateTime.now());
                r.setExpiresAt(expiresAt);
                reservationRepository.save(r);
                log.info("Notified user={} for book='{}' — collect by {}",
                    r.getMember().getEmail(), book.getTitle(), expiresAt);
                notificationService.sendReservationReady(r.getMember(), book, expiresAt);
            });
    }

    /**
     * Called by the scheduler to expire NOTIFIED reservations that passed their 3-day window.
     * After expiry, the next person in queue is notified.
     */
    @Transactional
    public void expireStaleNotifications() {
        reservationRepository.findExpiredNotifications(LocalDate.now()).forEach(r -> {
            log.info("Reservation expired: user={} book='{}'",
                r.getMember().getEmail(), r.getBook().getTitle());
            notificationService.sendReservationExpired(r.getMember(), r.getBook());
            r.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(r);
            notifyNextInQueue(r.getBook());
        });
    }

    public ReservationDTO toDTO(Reservation r) {
        return new ReservationDTO(
            r.getId(), r.getMember().getId(), r.getMember().getFullName(),
            r.getBook().getId(), r.getBook().getTitle(),
            r.getQueuePosition(), r.getStatus(),
            r.getReservedAt(), r.getNotifiedAt(), r.getExpiresAt()
        );
    }
}
