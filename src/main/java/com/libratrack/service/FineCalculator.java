package com.libratrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Single source of truth for the overdue-fine formula. Both the nightly
 * {@code OverdueFineScheduler} and {@code LoanService}'s manual-return path
 * use this rather than each computing the fine independently, so the two
 * can never quietly drift out of sync.
 *
 * Tiered structure, based on how many days past the due date a loan is:
 *   - First {graceDays} days late: free (grace period) — no fine at all.
 *   - Next days, up to {escalationDays} late: billed at the normal rate.
 *   - Beyond {escalationDays} late: billed at double the normal rate.
 *
 * {escalationDays} is intentionally a separate setting from the max loan
 * duration caps (14 days students / 30 faculty) even though it currently
 * defaults to the same number — one controls how far out a due date can be
 * set, this one controls when the fine rate escalates, and they may need to
 * be tuned independently later (e.g. if the loan caps ever differ by role
 * but the escalation point should stay flat for everyone).
 */
@Component
public class FineCalculator {

    @Value("${app.daily-fine-rate:0.50}")
    private BigDecimal dailyFineRate;
    @Value("${app.fine-grace-days:2}")
    private int graceDays;
    @Value("${app.fine-escalation-days:14}")
    private int escalationDays;

    /**
     * Computes the total fine owed for a loan that is {@code daysLate} days
     * past its due date. Returns {@link BigDecimal#ZERO} while still within
     * the grace period.
     */
    public BigDecimal calculate(long daysLate) {
        if (daysLate <= graceDays) return BigDecimal.ZERO;

        long normalRateDays = Math.max(0, Math.min(daysLate, escalationDays) - graceDays);
        long doubleRateDays = Math.max(0, daysLate - Math.max(graceDays, escalationDays));

        BigDecimal normalPortion = dailyFineRate.multiply(BigDecimal.valueOf(normalRateDays));
        BigDecimal doublePortion = dailyFineRate.multiply(BigDecimal.valueOf(2))
                .multiply(BigDecimal.valueOf(doubleRateDays));

        return normalPortion.add(doublePortion);
    }

    /** True once a loan has exceeded the grace period and genuinely owes something. */
    public boolean isBillable(long daysLate) {
        return daysLate > graceDays;
    }
}