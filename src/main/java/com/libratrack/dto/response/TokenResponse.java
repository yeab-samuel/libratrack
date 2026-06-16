package com.libratrack.dto.response;
import com.libratrack.enums.Role;
import java.time.LocalDateTime;

public record TokenResponse(
        String token,
        LocalDateTime expiresAt,
        String fullName,
        Role role,
        String universityId
) {}