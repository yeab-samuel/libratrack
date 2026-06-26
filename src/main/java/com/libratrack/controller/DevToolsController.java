package com.libratrack.controller;

import com.libratrack.scheduler.OverdueFineScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin endpoints for triggering scheduled jobs on demand.
 * Restricted to ADMIN role only. Useful for demos, testing,
 * or manually re-running the fine job if the nightly cron ever fails.
 *
 * POST /api/dev/trigger/overdue-fines
 * POST /api/dev/trigger/expire-reservations
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile({"default", "prod"})
public class DevToolsController {

    private final OverdueFineScheduler scheduler;

    @PostMapping("/trigger/overdue-fines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerOverdueFines() {
        scheduler.calculateOverdueFines();
        return ResponseEntity.ok(Map.of(
                "triggered", "calculateOverdueFines",
                "at", LocalDateTime.now().toString(),
                "note", "Fine calculation complete."
        ));
    }

    @PostMapping("/trigger/expire-reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerExpireReservations() {
        scheduler.expireStaleReservations();
        return ResponseEntity.ok(Map.of(
                "triggered", "expireStaleReservations",
                "at", LocalDateTime.now().toString(),
                "note", "Reservation expiry complete."
        ));
    }
}