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
 * Development-only endpoints for triggering scheduled jobs on demand.
 * These exist solely so you can test notification flows without waiting
 * for the 01:00 AM cron job. Restricted to ADMIN role and only active
 * when the "default" (local dev) profile is active — they are NOT
 * available in production.
 *
 * Usage (from the backend terminal or any REST client like curl):
 *
 *   POST /api/dev/trigger/overdue-fines
 *   POST /api/dev/trigger/expire-reservations
 *
 * Watch the backend console for [NOTIFY] log lines after calling these.
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("default")   // only active locally — not in prod profile
public class DevToolsController {

    private final OverdueFineScheduler scheduler;

    @PostMapping("/trigger/overdue-fines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerOverdueFines() {
        scheduler.calculateOverdueFines();
        return ResponseEntity.ok(Map.of(
                "triggered", "calculateOverdueFines",
                "at", LocalDateTime.now().toString(),
                "note", "Check the backend console for [NOTIFY] log lines."
        ));
    }

    @PostMapping("/trigger/expire-reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerExpireReservations() {
        scheduler.expireStaleReservations();
        return ResponseEntity.ok(Map.of(
                "triggered", "expireStaleReservations",
                "at", LocalDateTime.now().toString(),
                "note", "Check the backend console for [NOTIFY] log lines."
        ));
    }
}