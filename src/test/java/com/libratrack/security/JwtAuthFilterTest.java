package com.libratrack.security;

import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.repository.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtUtils jwtUtils;
    @Mock UserDetailsServiceImpl uds;
    @Mock TokenBlacklistRepository blacklist;

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    @InjectMocks JwtAuthFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_NoAuthorizationHeader_SkipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, uds, blacklist);
    }

    @Test
    void doFilterInternal_NonBearerHeader_SkipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, uds, blacklist);
    }

    @Test
    void doFilterInternal_ValidToken_SetsAuthenticationInContext() throws Exception {
        User user = User.builder()
                .id(1L).email("student@test.com").role(Role.STUDENT)
                .fullName("Student").active(true).passwordHash("hashed").build();

        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtUtils.extractUsername("good-token")).thenReturn("student@test.com");
        when(jwtUtils.extractJti("good-token")).thenReturn("jti-1");
        when(blacklist.existsByTokenJti("jti-1")).thenReturn(false);
        when(uds.loadUserByUsername("student@test.com")).thenReturn(user);
        when(jwtUtils.isTokenValid("good-token", user)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(user, auth.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_BlacklistedToken_DoesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted-token");
        when(jwtUtils.extractUsername("blacklisted-token")).thenReturn("student@test.com");
        when(jwtUtils.extractJti("blacklisted-token")).thenReturn("jti-2");
        when(blacklist.existsByTokenJti("jti-2")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(uds, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_TokenFailsValidation_DoesNotAuthenticate() throws Exception {
        User user = User.builder()
                .id(1L).email("student@test.com").role(Role.STUDENT)
                .fullName("Student").active(true).passwordHash("hashed").build();

        when(request.getHeader("Authorization")).thenReturn("Bearer stale-token");
        when(jwtUtils.extractUsername("stale-token")).thenReturn("student@test.com");
        when(jwtUtils.extractJti("stale-token")).thenReturn("jti-3");
        when(blacklist.existsByTokenJti("jti-3")).thenReturn(false);
        when(uds.loadUserByUsername("student@test.com")).thenReturn(user);
        when(jwtUtils.isTokenValid("stale-token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AlreadyAuthenticated_DoesNotReauthenticate() throws Exception {
        Authentication existing =
                new UsernamePasswordAuthenticationToken("preauth-user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtUtils.extractUsername("some-token")).thenReturn("student@test.com");

        filter.doFilterInternal(request, response, chain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(uds, blacklist);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ExceptionDuringProcessing_StillContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken-token");
        when(jwtUtils.extractUsername("broken-token"))
                .thenThrow(new RuntimeException("simulated parsing failure"));

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}