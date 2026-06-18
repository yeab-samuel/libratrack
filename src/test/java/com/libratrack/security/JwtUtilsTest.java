package com.libratrack.security;

import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String VALID_SECRET =
            "this-is-a-32-character-or-longer-secret-key-for-jwt-testing";

    private JwtUtils jwtUtils;
    private User user;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        setSecret(jwtUtils, VALID_SECRET);

        user = User.builder()
                .id(1L).email("student@test.com").role(Role.STUDENT)
                .fullName("Test Student").active(true).passwordHash("hashed").build();
    }

    private void setSecret(JwtUtils target, String secret) {
        try {
            Field f = JwtUtils.class.getDeclaredField("jwtSecret");
            f.setAccessible(true);
            f.set(target, secret);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set jwtSecret for testing", e);
        }
    }

    // ── generateToken / extractUsername / extractJti ────────────────────────

    @Test
    void generateToken_ThenExtractUsername_RoundTrips() {
        String token = jwtUtils.generateToken(user, "jti-001");
        assertEquals("student@test.com", jwtUtils.extractUsername(token));
    }

    @Test
    void generateToken_ThenExtractJti_ReturnsSameJti() {
        String token = jwtUtils.generateToken(user, "jti-002");
        assertEquals("jti-002", jwtUtils.extractJti(token));
    }

    // ── isTokenValid ──────────────────────────────────────────────────────

    @Test
    void isTokenValid_MatchingUserAndUnexpired_ReturnsTrue() {
        String token = jwtUtils.generateToken(user, "jti-003");
        assertTrue(jwtUtils.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_DifferentUser_ReturnsFalse() {
        String token = jwtUtils.generateToken(user, "jti-004");
        User other = User.builder()
                .id(2L).email("other@test.com").role(Role.STUDENT)
                .fullName("Other").active(true).passwordHash("hashed").build();

        assertFalse(jwtUtils.isTokenValid(token, other));
    }

    @Test
    void isTokenValid_MalformedToken_ReturnsFalseInsteadOfThrowing() {
        assertFalse(jwtUtils.isTokenValid("not-a-real-jwt-token", user));
    }

    // ── extractClaims (indirectly, via malformed input) ─────────────────────

    @Test
    void extractUsername_MalformedToken_ThrowsJwtException() {
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> jwtUtils.extractUsername("garbage-token-value"));
    }

    // ── getExpiresAt ──────────────────────────────────────────────────────

    @Test
    void getExpiresAt_ReturnsApproximately24HoursFromNow() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime expiresAt = jwtUtils.getExpiresAt();
        long hoursAhead = ChronoUnit.HOURS.between(before, expiresAt);

        assertTrue(hoursAhead >= 23 && hoursAhead <= 24,
                "Expected ~24 hours ahead, got " + hoursAhead);
    }

    // ── validate (@PostConstruct guard) ──────────────────────────────────────

    @Test
    void validate_NullSecret_ThrowsIllegalStateException() {
        JwtUtils u = new JwtUtils();
        setSecret(u, null);
        assertThrows(IllegalStateException.class, u::validate);
    }

    @Test
    void validate_TooShortSecret_ThrowsIllegalStateException() {
        JwtUtils u = new JwtUtils();
        setSecret(u, "too-short");
        assertThrows(IllegalStateException.class, u::validate);
    }

    @Test
    void validate_ValidSecret_DoesNotThrow() {
        assertDoesNotThrow(jwtUtils::validate);
    }
}