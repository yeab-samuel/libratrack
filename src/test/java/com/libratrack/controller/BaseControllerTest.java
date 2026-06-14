package com.libratrack.controller;

import com.libratrack.config.SecurityConfig;
import com.libratrack.repository.TokenBlacklistRepository;
import com.libratrack.security.JwtUtils;
import com.libratrack.security.UserDetailsServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/**
 * Base class for all @WebMvcTest slice tests.
 *
 * @Import(SecurityConfig.class) brings in the real security configuration so
 * that @PreAuthorize annotations on controllers are enforced — without this,
 * @WebMvcTest uses a permissive default that ignores role checks, causing tests
 * that expect 403 to receive 200 instead.
 *
 * JwtAuthFilter is NOT mocked here. Mocking a OncePerRequestFilter bean with
 * @MockBean replaces it with a no-op mock that never calls
 * filterChain.doFilter(), swallowing every request before it reaches the
 * security layer and causing all responses to be 200.
 *
 * Instead, we mock only the beans JwtAuthFilter depends on (JwtUtils,
 * UserDetailsServiceImpl, TokenBlacklistRepository). This lets the real filter
 * be constructed and registered in the chain. For @WithMockUser tests, no
 * Authorization header is present, so JwtAuthFilter's token check is skipped
 * and @WithMockUser's pre-set SecurityContext is used directly.
 */
@Import(SecurityConfig.class)
abstract class BaseControllerTest {

    @MockBean
    JwtUtils jwtUtils;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @MockBean
    TokenBlacklistRepository tokenBlacklistRepository;
}