package com.libratrack.service;

import com.libratrack.entity.Book;
import com.libratrack.entity.User;
import com.libratrack.enums.BookCategory;
import com.libratrack.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationService service;
    private User member;
    private Book book;

    @BeforeEach
    void setUp() {
        // Spy so we can stub/verify the package-private send() method
        service = spy(new NotificationService());
        setField(service, "mailEnabled", false);
        setField(service, "apiKey", "");

        member = User.builder()
                .id(1L).email("member@test.com").fullName("Jane Doe")
                .role(Role.STUDENT).universityId("UGR/1234/20").active(true).build();
        book = Book.builder()
                .id(1L).isbn("isbn-1").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).totalCopies(3).build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = NotificationService.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ── sendReservationReady ──────────────────────────────────────────────

    @Test
    void sendReservationReady_MailDisabled_SendNotCalled() {
        service.sendReservationReady(member, book, LocalDate.of(2026, 6, 26));
        verify(service, never()).send(any(), any(), any());
    }

    @Test
    void sendReservationReady_MailEnabled_SendCalledWithCorrectArgs() {
        setField(service, "mailEnabled", true);
        setField(service, "apiKey", "re_test");
        doNothing().when(service).send(any(), any(), any());

        service.sendReservationReady(member, book, LocalDate.of(2026, 6, 26));

        verify(service).send(
                eq("member@test.com"),
                contains("Clean Code"),
                contains("Jane Doe")
        );
    }

    @Test
    void sendReservationReady_DoesNotThrow() {
        assertDoesNotThrow(() ->
                service.sendReservationReady(member, book, LocalDate.now()));
    }

    // ── sendReservationExpired ────────────────────────────────────────────

    @Test
    void sendReservationExpired_MailDisabled_SendNotCalled() {
        service.sendReservationExpired(member, book);
        verify(service, never()).send(any(), any(), any());
    }

    @Test
    void sendReservationExpired_MailEnabled_SendCalledWithExpiredSubject() {
        setField(service, "mailEnabled", true);
        setField(service, "apiKey", "re_test");
        doNothing().when(service).send(any(), any(), any());

        service.sendReservationExpired(member, book);

        verify(service).send(eq("member@test.com"), contains("Expired"), any());
    }

    // ── sendOverdueFineNotice ─────────────────────────────────────────────
    // NOTE: sendOverdueFineNotice now takes plain fields (not a User entity)
    // so it's safe to call from an @Async thread — see NotificationService
    // class-level javadoc for why. Tests pass the member's fields directly.

    @Test
    void sendOverdueFineNotice_MailDisabled_SendNotCalled() {
        service.sendOverdueFineNotice(
                member.getFullName(), member.getEmail(), member.getUniversityId(),
                "Clean Code", new BigDecimal("3.50"));
        verify(service, never()).send(any(), any(), any());
    }

    @Test
    void sendOverdueFineNotice_MailEnabled_SendCalledWithAmount() {
        setField(service, "mailEnabled", true);
        setField(service, "apiKey", "re_test");
        doNothing().when(service).send(any(), any(), any());

        service.sendOverdueFineNotice(
                member.getFullName(), member.getEmail(), member.getUniversityId(),
                "Clean Code", new BigDecimal("3.50"));

        verify(service).send(eq("member@test.com"), contains("Fine"), contains("3.50"));
    }

    // ── send (core method) ────────────────────────────────────────────────

    @Test
    void send_MailDisabledOrNoKey_ReturnsImmediately() {
        // mailEnabled=false and apiKey="" — must not throw
        assertDoesNotThrow(() ->
                service.send("test@example.com", "Subject", "Body"));
    }
}