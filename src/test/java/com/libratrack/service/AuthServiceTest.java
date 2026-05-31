package com.libratrack.service;

import com.libratrack.dto.request.*;
import com.libratrack.dto.response.TokenResponse;
import com.libratrack.entity.TokenBlacklist;
import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.repository.*;
import com.libratrack.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UniversityRegistryRepository registryRepository;
    @Mock TokenBlacklistRepository blacklistRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtUtils jwtUtils;
    @InjectMocks AuthService authService;

    @Test
    void register_HappyPath() {
        var req = new RegisterRequest("John", "john@test.com", "password123", Role.STUDENT, "ATE/9305/14");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUniversityId(req.universityId())).thenReturn(false);
        when(registryRepository.findByUniversityIdAndRole(req.universityId(), req.role()))
                .thenReturn(Optional.of(
                        com.libratrack.entity.UniversityRegistry.builder()
                                .universityId(req.universityId()).role(req.role())
                                .fullName("John").active(true).build()));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(
                User.builder().id(1L).email(req.email()).role(req.role())
                        .fullName(req.fullName()).universityId(req.universityId()).active(true).build());

        var r = authService.register(req);
        assertEquals("john@test.com", r.email());
        assertEquals(Role.STUDENT, r.role());
    }

    @Test
    void register_DuplicateEmail_Throws409() {
        var req = new RegisterRequest("J", "j@t.com", "pass12345", Role.STUDENT, "ATE/1234/14");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
    }

    @Test
    void login_HappyPath_ReturnsToken() {
        var req = new LoginRequest("john@test.com", "password123");
        User user = User.builder().id(1L).email("john@test.com").role(Role.STUDENT)
                .fullName("John").active(true).passwordHash("hashed").build();

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any(), any())).thenReturn("jwt-token");
        when(jwtUtils.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(24));

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("John", response.fullName());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_WrongPassword_ThrowsBadCredentials() {
        var req = new LoginRequest("john@test.com", "wrongpassword");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager).authenticate(any());
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void logout_ValidToken_BlacklistsJti() {
        String jti = "test-jti-uuid";
        String token = "header.payload.signature";
        String bearer = "Bearer " + token;

        when(jwtUtils.extractJti(token)).thenReturn(jti);
        when(jwtUtils.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(24));
        when(blacklistRepository.save(any())).thenReturn(
                TokenBlacklist.builder().tokenJti(jti)
                        .expiresAt(LocalDateTime.now().plusHours(24)).build());

        authService.logout(bearer);
        verify(blacklistRepository).save(argThat(bl -> jti.equals(bl.getTokenJti())));
    }

    @Test
    void logout_NullBearer_DoesNothing() {
        authService.logout(null);
        verifyNoInteractions(blacklistRepository);
    }

    @Test
    void logout_MissingBearerPrefix_DoesNothing() {
        authService.logout("invalidtoken");
        verifyNoInteractions(blacklistRepository);
    }
}