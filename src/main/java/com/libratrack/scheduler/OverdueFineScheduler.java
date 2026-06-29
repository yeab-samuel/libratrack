package com.libratrack.scheduler;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.repository.*;
import com.libratrack.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Component @RequiredArgsConstructor @Slf4j
public class OverdueFineScheduler {
    private final LoanRepository loanRepository;
    private final FineRecordRepository fineRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final FineCalculator fineCalculator;

    /**
     * Runs at 01:00 every night. Two phases:
     *
     * Phase 1 — ACTIVE loans that are now past their due date:
     *   Flips them to OVERDUE, creates a fine record (or updates if one somehow
     *   already exists), and sends a one-time overdue notification email.
     *   Loans still inside the grace window are skipped and re-evaluated on the
     *   next nightly run.
     *
     * Phase 2 — Already-OVERDUE loans:
     *   Recalculates the accruing fine for every UNPAID overdue loan so the
     *   amount actually grows day-over-day. No email is sent here — the member
     *   was already notified on the night they first went overdue.
     *
     * Without Phase 2, the query in Phase 1 only ever catches a loan once
     * (ACTIVE → OVERDUE), so the fine amount would freeze after the very first
     * night and never accrue again.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void calculateOverdueFines() {

        // ── Phase 1: newly-overdue (ACTIVE → OVERDUE) ────────────────────────
        var newlyOverdue = loanRepository.findAllByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now());
        log.info("Overdue scheduler Phase 1: {} newly-overdue loans", newlyOverdue.size());

        for (Loan loan : newlyOverdue) {
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            if (!fineCalculator.isBillable(daysLate)) continue;

            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            BigDecimal amount = fineCalculator.calculate(daysLate);

            fineRepository.findByLoan(loan).ifPresentOrElse(
                    f -> { f.setAmount(amount); fineRepository.save(f); },
                    () -> fineRepository.save(FineRecord.builder()
                            .loan(loan).member(loan.getMember()).amount(amount).build())
            );
            // One-time notification — sent only on the first night a loan goes overdue.
            notificationService.sendOverdueFineNotice(
                    loan.getMember(), loan.getBookCopy().getBook().getTitle(), amount);
        }

        // ── Phase 2: already-overdue — recalculate daily without re-notifying ─
        var alreadyOverdue = loanRepository.findAllByStatus(LoanStatus.OVERDUE);
        log.info("Overdue scheduler Phase 2: {} already-overdue loans to recalculate", alreadyOverdue.size());

        for (Loan loan : alreadyOverdue) {
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            BigDecimal updatedAmount = fineCalculator.calculate(daysLate);
            fineRepository.findByLoan(loan).ifPresent(fine -> {
                if (fine.getStatus() == FineStatus.UNPAID) {
                    fine.setAmount(updatedAmount);
                    fineRepository.save(fine);
                }
            });
        }
    }

    /**
     * Runs at 00:10 every night — expires NOTIFIED reservations past their 3-day window
     * and notifies the next person in queue automatically.
     */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void expireStaleReservations() {
        log.info("Reservation expiry scheduler running");
        reservationService.expireStaleNotifications();
    }

    /** Runs at 02:00 every night — cleans up expired JWT tokens from the blacklist */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        tokenBlacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Token blacklist cleanup complete");
    }
}