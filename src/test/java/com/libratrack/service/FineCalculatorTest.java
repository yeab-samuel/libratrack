package com.libratrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive day-by-day coverage of the tiered fine formula. Because the
 * logic now lives in one place rather than being duplicated across
 * LoanService and OverdueFineScheduler, it's worth testing thoroughly here
 * rather than re-deriving expected dollar amounts in both of those test
 * classes for every scenario.
 *
 * Defaults under test: $0.50/day, 2-day grace period, escalation (double
 * rate) starting after 14 days overdue.
 */
class FineCalculatorTest {

    private FineCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FineCalculator();
        setField("dailyFineRate", new BigDecimal("0.50"));
        setField("graceDays", 2);
        setField("escalationDays", 14);
    }

    private void setField(String name, Object value) {
        try {
            Field f = FineCalculator.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(calculator, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ── isBillable ────────────────────────────────────────────────────────

    @Test
    void isBillable_WithinGracePeriod_ReturnsFalse() {
        assertFalse(calculator.isBillable(1));
        assertFalse(calculator.isBillable(2));
    }

    @Test
    void isBillable_PastGracePeriod_ReturnsTrue() {
        assertTrue(calculator.isBillable(3));
    }

    // ── calculate — grace period ─────────────────────────────────────────

    @Test
    void calculate_WithinGracePeriod_ReturnsZero() {
        assertEquals(0, calculator.calculate(1).compareTo(BigDecimal.ZERO));
        assertEquals(0, calculator.calculate(2).compareTo(BigDecimal.ZERO));
    }

    // ── calculate — normal rate tier ─────────────────────────────────────

    @Test
    void calculate_JustPastGrace_ChargesOneBillableDay() {
        // 3 days late, 2-day grace => 1 billable day at $0.50
        assertEquals(0, calculator.calculate(3).compareTo(new BigDecimal("0.50")));
    }

    @Test
    void calculate_FiveDaysLate_ChargesThreeBillableDaysAtNormalRate() {
        // 5 days late, 2-day grace => 3 billable days at $0.50 = $1.50
        assertEquals(0, calculator.calculate(5).compareTo(new BigDecimal("1.50")));
    }

    @Test
    void calculate_ExactlyAtEscalationThreshold_AllDaysAtNormalRate() {
        // 14 days late: days 3-14 = 12 billable days, all still at normal
        // rate since the threshold itself hasn't been exceeded yet.
        assertEquals(0, calculator.calculate(14).compareTo(new BigDecimal("6.00")));
    }

    // ── calculate — escalated (double) rate tier ─────────────────────────

    @Test
    void calculate_OneDayPastEscalationThreshold_AddsOneDoubleRateDay() {
        // 15 days late: 12 days at $0.50 (days 3-14) + 1 day at $1.00 (day 15) = $7.00
        assertEquals(0, calculator.calculate(15).compareTo(new BigDecimal("7.00")));
    }

    @Test
    void calculate_WellPastEscalationThreshold_MixesNormalAndDoubleRates() {
        // 20 days late: 12 days at $0.50 (days 3-14) + 6 days at $1.00 (days 15-20) = $12.00
        assertEquals(0, calculator.calculate(20).compareTo(new BigDecimal("12.00")));
    }

    @Test
    void calculate_FarPastEscalationThreshold_ContinuesAtDoubleRate() {
        // 30 days late: 12 days at $0.50 = $6.00, 16 days at $1.00 = $16.00, total $22.00
        assertEquals(0, calculator.calculate(30).compareTo(new BigDecimal("22.00")));
    }
}