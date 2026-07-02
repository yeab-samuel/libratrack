package com.libratrack.service;

import com.libratrack.entity.Book;
import com.libratrack.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Sends transactional email notifications via Resend HTTP API.
 * Falls back to log-only mode when MAIL_ENABLED=false or RESEND_API_KEY is not set.
 * All public methods are @Async — a slow or failed send never blocks the caller's request.
 *
 * IMPORTANT: async methods must only ever receive plain values (String, BigDecimal, etc.),
 * never JPA-managed entities such as User/Book/Loan. By the time the async thread runs,
 * the caller's transaction/Hibernate session is already closed, so touching a lazy proxy
 * (e.g. calling user.getFullName()) from here throws
 * "IllegalStateException: Illegal pop() with non-matching JdbcValuesSourceProcessingState"
 * and kills the whole calling transaction. Resolve any needed fields in the caller,
 * while still inside its transaction, and pass those resolved values in.
 */
@Service
@Slf4j
public class NotificationService {

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    // Package-private so tests can substitute a mock/spy without touching HttpClient internals
    HttpClient httpClient = HttpClient.newHttpClient();

    private static final String RESEND_URL  = "https://api.resend.com/emails";
    private static final String FROM_HEADER = "LibraTrack System <onboarding@resend.dev>";

    /** Returns true when mail is configured and enabled. */
    private boolean isEnabled() {
        return mailEnabled && apiKey != null && !apiKey.isBlank();
    }

    /** Notify member that their reserved copy is ready to collect. */
    @Async
    public void sendReservationReady(User member, Book book, LocalDate expiresAt) {
        log.info("[NOTIFY] Reservation ready — {} ({}) → \"{}\" collect by {}",
                member.getFullName(), member.getEmail(), book.getTitle(), expiresAt);
        if (!isEnabled()) {
            log.info("[NOTIFY] Mail disabled / no API key — skipping send to {}", member.getEmail());
            return;
        }
        send(member.getEmail(),
                "LibraTrack — Book Ready: " + book.getTitle(),
                String.format(
                        "Dear %s,\n\n" +
                                "Great news! A copy of \"%s\" by %s is now available for you.\n\n" +
                                "Please collect it from the library before %s.\n" +
                                "If you do not collect by this date your reservation will expire\n" +
                                "and the copy will be offered to the next person in the queue.\n\n" +
                                "University ID: %s\n\n" +
                                "— LibraTrack Library System",
                        member.getFullName(), book.getTitle(), book.getAuthor(),
                        expiresAt, member.getUniversityId()
                )
        );
    }

    /** Notify member that their reservation has expired without being collected. */
    @Async
    public void sendReservationExpired(User member, Book book) {
        log.info("[NOTIFY] Reservation expired — {} ({}) for \"{}\"",
                member.getFullName(), member.getEmail(), book.getTitle());
        if (!isEnabled()) {
            log.info("[NOTIFY] Mail disabled / no API key — skipping send to {}", member.getEmail());
            return;
        }
        send(member.getEmail(),
                "LibraTrack — Reservation Expired: " + book.getTitle(),
                String.format(
                        "Dear %s,\n\n" +
                                "Your reservation for \"%s\" has expired because it was not collected " +
                                "within the 3-day window.\n\n" +
                                "You may place a new reservation if you still need this book.\n\n" +
                                "— LibraTrack Library System",
                        member.getFullName(), book.getTitle()
                )
        );
    }

    /**
     * Notify member that an overdue fine has been applied to their account.
     *
     * Takes plain fields (not a User entity) so this method is safe to call
     * asynchronously — see the class-level note for why.
     */
    @Async
    public void sendOverdueFineNotice(String memberFullName, String memberEmail,
                                      String universityId, String bookTitle, BigDecimal amount) {
        log.info("[NOTIFY] Overdue fine — {} ({}) — {} — ${}",
                memberFullName, memberEmail, bookTitle, amount);
        if (!isEnabled()) {
            log.info("[NOTIFY] Mail disabled / no API key — skipping send to {}", memberEmail);
            return;
        }
        send(memberEmail,
                "LibraTrack — Overdue Fine: " + bookTitle,
                String.format(
                        "Dear %s,\n\n" +
                                "Your loan for \"%s\" is overdue.\n" +
                                "Current fine: $%.2f\n\n" +
                                "Please return the book and settle your fine at the library desk.\n\n" +
                                "— LibraTrack Library System",
                        memberFullName, bookTitle, amount
                )
        );
    }

    /**
     * Core HTTP send — calls Resend's /emails endpoint.
     * Package-private so tests can stub this method via Mockito spy
     * without needing to mock the JDK HttpClient.
     */
    void send(String to, String subject, String text) {
        try {
            String escaped = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");
            String escapedSubject = subject.replace("\\", "\\\\").replace("\"", "\\\"");
            String json = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"text\":\"%s\"}",
                    FROM_HEADER, to, escapedSubject, escaped
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[NOTIFY] Email sent to {}: {}", to, subject);
            } else {
                log.warn("[NOTIFY] Resend returned {} for {}: {}", response.statusCode(), to, response.body());
            }
        } catch (Exception e) {
            log.warn("[NOTIFY] Email delivery failed for {} — {}", to, e.getMessage());
        }
    }
}