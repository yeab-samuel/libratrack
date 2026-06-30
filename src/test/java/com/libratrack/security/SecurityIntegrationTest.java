package com.libratrack.security;

import com.libratrack.entity.UniversityRegistry;
import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.repository.UniversityRegistryRepository;
import com.libratrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Autowired UniversityRegistryRepository registryRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Unique university ID per call, e.g. TST/000001/26 — matches the
    // DEPT/SERIAL/YEAR format the registry and register endpoint expect.
    private static final AtomicInteger SERIAL = new AtomicInteger(1);
    private static String nextUniversityId() {
        return String.format("TST/%06d/26", SERIAL.getAndIncrement());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Seeds a registry entry (registration is gated on the registry) then
     * registers and logs in that exact person. Login uses "identifier"
     * (the university ID) + "password" — matching the real /api/auth/login
     * contract, not email/password.
     *
     * ADMIN and LIBRARIAN are staff roles that AuthService#register
     * deliberately refuses to self-register (see "Cannot self-register as
     * ADMIN/LIBRARIAN" guard) — that's correct production behavior, mirroring
     * the real-world rule that staff accounts are created by an existing
     * admin via /api/admin/staff, not by the public registration form. Since
     * there's no admin yet when these tests bootstrap their first staff
     * account, we seed those roles straight into the repository instead,
     * matching exactly what AuthService#createStaff would have produced.
     */
    private String registerAndLogin(String email, String role) {
        String universityId = nextUniversityId();
        Role roleEnum = Role.valueOf(role);

        if (roleEnum == Role.ADMIN || roleEnum == Role.LIBRARIAN) {
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode("Password1!"))
                    .role(roleEnum)
                    .fullName("Test User")
                    .universityId(universityId)
                    .build());
        } else {
            registryRepository.save(UniversityRegistry.builder()
                    .universityId(universityId)
                    .fullName("Test User")
                    .role(roleEnum)
                    .active(true)
                    .build());

            rest.postForEntity("/api/auth/register",
                    Map.of("fullName", "Test User", "email", email,
                            "password", "Password1!", "role", role,
                            "universityId", universityId),
                    Object.class);
        }

        var resp = rest.postForEntity("/api/auth/login",
                Map.of("identifier", universityId, "password", "Password1!"),
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
        String libUniversityId = nextUniversityId();
        registryRepository.save(UniversityRegistry.builder()
                .universityId(libUniversityId).fullName("Librarian")
                .role(Role.LIBRARIAN).active(true).build());
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "Librarian", "email", "lib-bola@test.com",
                        "password", "Password1!", "role", "LIBRARIAN",
                        "universityId", libUniversityId),
                Object.class);

        // Register student A (owner of the loan)
        String tokenA = registerAndLogin("student-a-bola@test.com");

        // Register student B (the attacker)
        String tokenB = registerAndLogin("student-b-bola@test.com");

        // Student B cannot call GET /api/loans (all-loans admin/librarian endpoint)
        // — a role-level BOLA check.
        var resp = rest.exchange("/api/loans",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(tokenB)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());

        // And student B cannot list all fines (admin/librarian endpoint).
        var finesResp = rest.exchange("/api/fines",
                HttpMethod.GET, new HttpEntity<>(bearerHeaders(tokenB)), Object.class);
        assertEquals(HttpStatus.FORBIDDEN, finesResp.getStatusCode());
    }

    // ── test 5: librarian cannot access admin-only endpoint → 403 ────────────

    @Test
    void librarian_CannotAccess_AdminOnlyEndpoint() {
        String libToken = registerAndLogin("lib2-sec@test.com", "LIBRARIAN");

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
        String adminToken = registerAndLogin("admin-objbola@test.com", "ADMIN");
        String libToken = registerAndLogin("lib-objbola@test.com", "LIBRARIAN");

        // Register student A (loan owner) — need raw id, so register directly
        String studentAUniversityId = nextUniversityId();
        registryRepository.save(UniversityRegistry.builder()
                .universityId(studentAUniversityId).fullName("StudentA")
                .role(Role.STUDENT).active(true).build());
        rest.postForEntity("/api/auth/register",
                Map.of("fullName", "StudentA", "email", "obj-bola-a@test.com",
                        "password", "Password1!", "role", "STUDENT",
                        "universityId", studentAUniversityId),
                Object.class);

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