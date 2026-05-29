package com.libratrack.service;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.*;
import com.libratrack.entity.*;
import com.libratrack.enums.Role;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.*;
import com.libratrack.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UniversityRegistryRepository registryRepository;
    private final TokenBlacklistRepository blacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public UserDTO register(RegisterRequest req) {
        if (req.role() == Role.ADMIN || req.role() == Role.LIBRARIAN)
            throw new IllegalArgumentException(
                "Cannot self-register as " + req.role() + ". Contact your administrator.");

        if (userRepository.existsByEmail(req.email()))
            throw new DuplicateResourceException("Email already registered: " + req.email());

        if (userRepository.existsByUniversityId(req.universityId()))
            throw new DuplicateResourceException("University ID already registered: " + req.universityId());

        // Cross-check against the campus registry
        UniversityRegistry entry = registryRepository
            .findByUniversityIdAndRole(req.universityId(), req.role())
            .orElseThrow(() -> new ResourceNotFoundException(
                "University ID '" + req.universityId() + "' not found in the campus registry " +
                "for role " + req.role() + ". Contact the registrar's office."));

        if (!entry.getActive())
            throw new IllegalArgumentException("University ID '" + req.universityId() + "' is inactive in the registry.");

        User u = User.builder()
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .role(req.role())
            .fullName(req.fullName())
            .universityId(req.universityId())
            .build();
        return toDTO(userRepository.save(u));
    }

    /** Admin-only: create a LIBRARIAN or ADMIN account (no university ID needed). */
    @Transactional
    public UserDTO createStaff(CreateStaffRequest req) {
        if (req.role() == Role.STUDENT || req.role() == Role.FACULTY)
            throw new IllegalArgumentException("Use /api/auth/register for student/faculty accounts.");

        if (userRepository.existsByEmail(req.email()))
            throw new DuplicateResourceException("Email already registered: " + req.email());

        User u = User.builder()
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .role(req.role())
            .fullName(req.fullName())
            .build();
        return toDTO(userRepository.save(u));
    }

    public TokenResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User u = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String jti = UUID.randomUUID().toString();
        return new TokenResponse(jwtUtils.generateToken(u, jti), jwtUtils.getExpiresAt(), u.getFullName());    }

    @Transactional
    public void logout(String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ")) return;
        String token = bearer.substring(7);
        blacklistRepository.save(TokenBlacklist.builder()
            .tokenJti(jwtUtils.extractJti(token))
            .expiresAt(jwtUtils.getExpiresAt())
            .build());
    }

    public UserDTO toDTO(User u) {
        return new UserDTO(u.getId(), u.getEmail(), u.getRole(),
            u.getFullName(), u.getUniversityId(), u.getActive(), u.getCreatedAt());
    }
}
