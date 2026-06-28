package com.libratrack.controller;

import com.libratrack.scheduler.OverdueFineScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unauthenticated endpoints intended ONLY for an external cron service
 * (e.g. cron-job.org) to call on a schedule.
 *
 * Why this exists: Render's free tier puts the app to sleep after a period
 * of inactivity. Spring's internal @Scheduled cron jobs only fire if the
 * JVM is actually running at that exact moment — if the instance is
 * asleep at 1am, the nightly OverdueFineScheduler silently never runs.
 * An external pinger hitting this endpoint wakes the instance up AND
 * runs the job, regardless of Render's sleep state.
 *
 * Protected by a long static shared secret (CRON_SECRET env var) passed
 * as a query parameter, rather than a JWT. JWTs expire, which would
 * silently break this automation later; a static secret does not.
 */
@RestController
@RequestMapping("/api/cron")
@RequiredArgsConstructor
public class CronController {

    private final OverdueFineScheduler scheduler;

    @Value("${app.cron-secret}")
    private String cronSecret;

    @PostMapping("/overdue-fines")
    public ResponseEntity<?> triggerOverdueFines(@RequestParam String key) {
        if (!cronSecret.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid key"));
        }
        scheduler.calculateOverdueFines();
        return ResponseEntity.ok(Map.of(
                "triggered", "calculateOverdueFines",
                "at", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/expire-reservations")
    public ResponseEntity<?> triggerExpireReservations(@RequestParam String key) {
        if (!cronSecret.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid key"));
        }
        scheduler.expireStaleReservations();
        return ResponseEntity.ok(Map.of(
                "triggered", "expireStaleReservations",
                "at", LocalDateTime.now().toString()
        ));
    }
}