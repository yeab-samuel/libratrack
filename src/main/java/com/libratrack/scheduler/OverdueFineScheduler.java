package com.libratrack.scheduler;
import com.libratrack.entity.*;
import com.libratrack.enums.*;
import com.libratrack.repository.*;
import com.libratrack.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.daily-fine-rate:0.50}") private BigDecimal dailyFineRate;

    /** Runs at 01:00 every night — marks loans overdue and accrues fines */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void calculateOverdueFines() {
        var loans = loanRepository.findAllByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now());
        log.info("Overdue scheduler: {} loans to process", loans.size());
        for (Loan loan : loans) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            long days = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            BigDecimal amount = dailyFineRate.multiply(BigDecimal.valueOf(days));

            fineRepository.findByLoan(loan).ifPresentOrElse(
                f -> { f.setAmount(amount); fineRepository.save(f); },
                () -> fineRepository.save(FineRecord.builder()
                    .loan(loan).member(loan.getMember()).amount(amount).build())
            );
            notificationService.sendOverdueFineNotice(
                loan.getMember(), loan.getBookCopy().getBook().getTitle(), amount);
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
