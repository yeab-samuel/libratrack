package com.libratrack.controller;

import com.libratrack.scheduler.OverdueFineScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevToolsControllerTest {

    @Mock  OverdueFineScheduler scheduler;
    @InjectMocks DevToolsController controller;

    @Test
    void triggerOverdueFines_calls_scheduler_and_returns_200() {
        ResponseEntity<?> res = controller.triggerOverdueFines();

        verify(scheduler, times(1)).calculateOverdueFines();
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("triggered")).isEqualTo("calculateOverdueFines");
        assertThat(body).containsKey("at");
        assertThat(body).containsKey("note");
    }

    @Test
    void triggerExpireReservations_calls_scheduler_and_returns_200() {
        ResponseEntity<?> res = controller.triggerExpireReservations();

        verify(scheduler, times(1)).expireStaleReservations();
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("triggered")).isEqualTo("expireStaleReservations");
        assertThat(body).containsKey("at");
    }

    @Test
    void triggerOverdueFines_does_not_call_expiry_scheduler() {
        controller.triggerOverdueFines();
        verify(scheduler, never()).expireStaleReservations();
    }

    @Test
    void triggerExpireReservations_does_not_call_fines_scheduler() {
        controller.triggerExpireReservations();
        verify(scheduler, never()).calculateOverdueFines();
    }
}