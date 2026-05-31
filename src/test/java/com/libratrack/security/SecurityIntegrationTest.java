package com.libratrack.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("JWT_SECRET", () -> "test-secret-must-be-at-least-32chars!!");
    }

    @Autowired TestRestTemplate rest;

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String email, String role) {
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "Test User", "email", email,
                        "password", "Password1!", "role", role),
                Object.class);
        var resp = rest.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "Password1!"),
                Map.class);
        return (String) resp.getBody().get("token");
    }

    private String registerAndLogin(String email) {
        return registerAndLogin(email, "STUDENT");
    }

    private HttpHeaders bearerHeaders(String token) {
        var h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    // ── test 1: unauthenticated access → 401 ─────────────────────────────────

    @Test
    void unauthenticated_CannotAccess_ProtectedEndpoint() {
        var resp = rest.getForEntity("/api/loans/mine", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ── test 2: wrong role → 403 ──────────────────────────────────────────────

    @Test
    void student_CannotAccess_AdminEndpoint() {
        String token = registerAndLogin("student-admin@test.com");
        var resp = rest.exchange("/api/admin/users",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ── test 3: blacklisted token → 401 ───────────────────────────────────────

    @Test
    void logout_ThenBlacklistedToken_Returns401() {
        String token = registerAndLogin("blacklist@test.com");
        var headers = bearerHeaders(token);

        // confirm token works before logout
        var before = rest.exchange("/api/loans/mine",
                HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        assertEquals(HttpStatus.OK, before.getStatusCode());

        // logout
        rest.exchange("/api/auth/logout",
                HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        // same token must now be rejected
        var after = rest.exchange("/api/loans/mine",
                HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, after.getStatusCode());
    }

    // ── test 4: BOLA — student A cannot read student B's loan ─────────────────

    @Test
    void studentA_CannotAccess_LoanOwnedByStudentB() {
        // Register librarian to create loan
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "Librarian", "email", "lib-bola@test.com",
                        "password", "Password1!", "role", "LIBRARIAN"),
                Object.class);
        String libToken = ((Map<?, ?>) rest.postForEntity("/api/auth/login",
                Map.of("email", "lib-bola@test.com", "password", "Password1!"),
                Map.class).getBody()).get("token").toString();

        // Register student A (owner of the loan)
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "Student A", "email", "student-a-bola@test.com",
                        "password", "Password1!", "role", "STUDENT"),
                Object.class);
        // Get student A's id from login
        var loginA = rest.postForEntity("/api/auth/login",
                Map.of("email", "student-a-bola@test.com", "password", "Password1!"),
                Map.class);
        String tokenA = (String) loginA.getBody().get("token");

        // Register student B (the attacker)
        String tokenB = registerAndLogin("student-b-bola@test.com");

        // Create a book and copy so the loan endpoint can be exercised
        // We check via GET /api/loans/{id} — if the loan doesn't exist, 404 is
        // returned for both students. Use a known non-existent loan ID to verify
        // that student B gets 404 (not the loan data) while the real BOLA check
        // is confirmed: student B is blocked from accessing loan id=1 even if it
        // existed, because ownership check fires first.
        // More directly, we verify that student B cannot call GET /api/loans
        // (all-loans admin endpoint) — which is a role-level BOLA check.
        var resp = rest.exchange("/api/loans",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(tokenB)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());

        // And verify student B cannot see student A's /api/fines/mine (own-only)
        // by trying /api/fines/{id} with an ID belonging to another member.
        // Since no fines exist yet, 404 is the correct response for any user —
        // but we verify the BOLA gate works by confirming student B cannot list
        // all fines (admin/librarian endpoint).
        var finesResp = rest.exchange("/api/fines",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(tokenB)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, finesResp.getStatusCode());
    }

    // ── test 5: librarian cannot access admin-only endpoint → 403 ────────────

    @Test
    void librarian_CannotAccess_AdminOnlyEndpoint() {
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "Librarian2", "email", "lib2-sec@test.com",
                        "password", "Password1!", "role", "LIBRARIAN"),
                Object.class);
        var loginResp = rest.postForEntity("/api/auth/login",
                Map.of("email", "lib2-sec@test.com", "password", "Password1!"),
                Map.class);
        String libToken = (String) loginResp.getBody().get("token");

        // PATCH /api/admin/users/{id}/deactivate is ADMIN only
        var resp = rest.exchange("/api/admin/users/1/deactivate",
                HttpMethod.PATCH, new HttpEntity<>(bearerHeaders(libToken)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ── test 6: public endpoints accessible without auth ─────────────────────

    @Test
    void publicEndpoints_AccessibleWithoutAuth() {
        var searchResp = rest.getForEntity("/api/books/search", Object.class);
        assertEquals(HttpStatus.OK, searchResp.getStatusCode());
    }

    // ── test 7: real object-level BOLA — student B cannot read student A's loan ─

    @Test
    void studentB_CannotAccess_LoanOwnedByStudentA() {
        // Register admin
        rest.postForEntity("/api/auth/register",
                Map.of("fullName","AdminBola","email","admin-objbola@test.com",
                        "password","Password1!","role","ADMIN"), Object.class);
        String adminToken = ((Map<?,?>) rest.postForEntity("/api/auth/login",
                Map.of("email","admin-objbola@test.com","password","Password1!"),
                Map.class).getBody()).get("token").toString();

        // Register librarian
        rest.postForEntity("/api/auth/register",
                Map.of("fullName","LibBola","email","lib-objbola@test.com",
                        "password","Password1!","role","LIBRARIAN"), Object.class);
        String libToken = ((Map<?,?>) rest.postForEntity("/api/auth/login",
                Map.of("email","lib-objbola@test.com","password","Password1!"),
                Map.class).getBody()).get("token").toString();

        // Register student A (loan owner)
        rest.postForEntity("/api/auth/register",
                Map.of("fullName","StudentA","email","obj-bola-a@test.com",
                        "password","Password1!","role","STUDENT"), Object.class);

        // Register student B (attacker)
        String tokenB = registerAndLogin("obj-bola-b@test.com");

        // Get student A's user id via admin endpoint
        var usersResp = rest.exchange("/api/admin/users?page=0&size=100",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(adminToken)), Map.class);
        @SuppressWarnings("unchecked")
        java.util.List<Map<?,?>> content =
                (java.util.List<Map<?,?>>) ((Map<?,?>) usersResp.getBody()).get("content");
        Long studentAId = content.stream()
                .filter(u -> "obj-bola-a@test.com".equals(u.get("email")))
                .map(u -> ((Number) u.get("id")).longValue())
                .findFirst().orElseThrow();

        // Create book + copy
        var bookResp = rest.exchange("/api/books", HttpMethod.POST,
                new HttpEntity<>(Map.of("isbn","BOLA-OBJ-001","title","BOLA Book",
                        "author","X","category","OTHER","totalCopies",1),
                        bearerHeaders(adminToken)), Map.class);
        Long bookId = ((Number) bookResp.getBody().get("id")).longValue();

        var copyResp = rest.exchange("/api/books/" + bookId + "/copies", HttpMethod.POST,
                new HttpEntity<>(Map.of("copyNumber","BOLA-OBJ-C001","condition","GOOD"),
                        bearerHeaders(libToken)), Map.class);
        Long copyId = ((Number) copyResp.getBody().get("id")).longValue();

        // Issue loan to student A
        var loanResp = rest.exchange("/api/loans", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "memberId", studentAId,
                        "bookCopyId", copyId,
                        "dueDate", LocalDate.now().plusDays(14).toString()),
                        bearerHeaders(libToken)), Map.class);
        Long loanId = ((Number) loanResp.getBody().get("id")).longValue();

        // Student B tries to read student A's loan — must be 403
        var resp = rest.exchange("/api/loans/" + loanId,
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(tokenB)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
