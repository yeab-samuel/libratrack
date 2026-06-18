package com.libratrack.service;

import com.libratrack.entity.Book;
import com.libratrack.entity.User;
import com.libratrack.enums.BookCategory;
import com.libratrack.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private JavaMailSender mailSender;
    private User member;
    private Book book;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        member = User.builder()
                .id(1L).email("member@test.com").fullName("Jane Doe")
                .role(Role.STUDENT).universityId("UGR/1234/20").active(true).build();
        book = Book.builder()
                .id(1L).isbn("isbn-1").title("Clean Code").author("Robert Martin")
                .category(BookCategory.SCIENCE).totalCopies(3).build();
    }

    private NotificationService buildService(Optional<JavaMailSender> sender, boolean mailEnabled) {
        NotificationService service = new NotificationService(sender);
        setField(service, "fromAddress", "noreply@libratrack.com");
        setField(service, "mailEnabled", mailEnabled);
        return service;
    }

    private void setField(NotificationService target, String name, Object value) {
        try {
            Field f = NotificationService.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ── sendReservationReady ──────────────────────────────────────────────

    @Test
    void sendReservationReady_MailEnabledAndSenderPresent_SendsEmail() {
        NotificationService service = buildService(Optional.of(mailSender), true);

        service.sendReservationReady(member, book, LocalDate.of(2026, 6, 20));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("member@test.com", msg.getTo()[0]);
        assertTrue(msg.getSubject().contains("Clean Code"));
        assertTrue(msg.getText().contains("Jane Doe"));
    }

    @Test
    void sendReservationReady_MailDisabled_DoesNotSendEmail() {
        NotificationService service = buildService(Optional.of(mailSender), false);

        service.sendReservationReady(member, book, LocalDate.now());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendReservationReady_MailEnabledButNoSenderConfigured_DoesNotThrow() {
        NotificationService service = buildService(Optional.empty(), true);

        assertDoesNotThrow(() -> service.sendReservationReady(member, book, LocalDate.now()));
    }

    @Test
    void sendReservationReady_MailSendThrows_ExceptionIsSwallowed() {
        NotificationService service = buildService(Optional.of(mailSender), true);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.sendReservationReady(member, book, LocalDate.now()));
    }

    // ── sendReservationExpired ────────────────────────────────────────────

    @Test
    void sendReservationExpired_MailEnabledAndSenderPresent_SendsEmail() {
        NotificationService service = buildService(Optional.of(mailSender), true);

        service.sendReservationExpired(member, book);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Expired"));
    }

    @Test
    void sendReservationExpired_MailDisabled_DoesNotSendEmail() {
        NotificationService service = buildService(Optional.of(mailSender), false);

        service.sendReservationExpired(member, book);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ── sendOverdueFineNotice ─────────────────────────────────────────────

    @Test
    void sendOverdueFineNotice_MailEnabledAndSenderPresent_SendsEmail() {
        NotificationService service = buildService(Optional.of(mailSender), true);

        service.sendOverdueFineNotice(member, "Clean Code", new BigDecimal("3.50"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("3.50"));
    }

    @Test
    void sendOverdueFineNotice_MailDisabled_DoesNotSendEmail() {
        NotificationService service = buildService(Optional.of(mailSender), false);

        service.sendOverdueFineNotice(member, "Clean Code", new BigDecimal("3.50"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}