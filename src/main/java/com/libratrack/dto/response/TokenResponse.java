package com.libratrack.dto.response;
import java.time.LocalDateTime;

public record TokenResponse(
        String token,
        LocalDateTime expiresAt,
        String fullName
) {}