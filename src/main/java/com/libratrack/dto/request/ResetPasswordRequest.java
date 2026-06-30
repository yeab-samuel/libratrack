package com.libratrack.dto.request;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}