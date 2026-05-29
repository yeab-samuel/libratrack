package com.libratrack.service;
import com.libratrack.entity.Book;
import com.libratrack.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Sends email notifications to users.
 * If mail is not configured (SMTP host not set), notifications are logged only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final Optional<JavaMailSender> mailSender;

    @Value("${spring.mail.from:noreply@libratrack.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /** Notify member that their reserved book is ready to collect. */
    public void sendReservationReady(User member, Book book, LocalDate expiresAt) {
        String subject = "LibraTrack — Book Ready: " + book.getTitle();
        String body = String.format(
            "Dear %s,\n\n" +
            "Great news! A copy of \"%s\" by %s is now available for you.\n\n" +
            "Please collect it from the library before %s.\n" +
            "If you do not collect by this date, your reservation will expire\n" +
            "and the book will be offered to the next person in the queue.\n\n" +
            "University ID: %s\n\n" +
            "LibraTrack Library System",
            member.getFullName(), book.getTitle(), book.getAuthor(),
            expiresAt, member.getUniversityId()
        );

        log.info("[NOTIFY] Reservation ready — {} ({}) → \"{}\" collect by {}",
            member.getFullName(), member.getEmail(), book.getTitle(), expiresAt);

        if (mailEnabled && mailSender.isPresent()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(member.getEmail());
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.get().send(msg);
                log.info("[NOTIFY] Email sent to {}", member.getEmail());
            } catch (Exception e) {
                log.warn("[NOTIFY] Email delivery failed for {} — {}", member.getEmail(), e.getMessage());
            }
        }
    }

    /** Notify member that their reservation has expired. */
    public void sendReservationExpired(User member, Book book) {
        log.info("[NOTIFY] Reservation expired — {} ({}) for \"{}\"",
            member.getFullName(), member.getEmail(), book.getTitle());

        if (mailEnabled && mailSender.isPresent()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(member.getEmail());
                msg.setSubject("LibraTrack — Reservation Expired: " + book.getTitle());
                msg.setText(String.format(
                    "Dear %s,\n\nYour reservation for \"%s\" has expired because it was not " +
                    "collected within the 3-day window.\n\nYou may place a new reservation if " +
                    "you still need this book.\n\nLibraTrack Library System",
                    member.getFullName(), book.getTitle()
                ));
                mailSender.get().send(msg);
            } catch (Exception e) {
                log.warn("[NOTIFY] Email delivery failed — {}", e.getMessage());
            }
        }
    }

    /** Notify member their fine is overdue. */
    public void sendOverdueFineNotice(User member, String bookTitle, java.math.BigDecimal amount) {
        log.info("[NOTIFY] Overdue fine — {} ({}) — {} — ${}",
            member.getFullName(), member.getEmail(), bookTitle, amount);

        if (mailEnabled && mailSender.isPresent()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(member.getEmail());
                msg.setSubject("LibraTrack — Overdue Fine: " + bookTitle);
                msg.setText(String.format(
                    "Dear %s,\n\nYour loan for \"%s\" is overdue.\n" +
                    "Current fine: $%.2f\n\nPlease return the book and settle your fine " +
                    "at the library desk.\n\nLibraTrack Library System",
                    member.getFullName(), bookTitle, amount
                ));
                mailSender.get().send(msg);
            } catch (Exception e) {
                log.warn("[NOTIFY] Email delivery failed — {}", e.getMessage());
            }
        }
    }
}
