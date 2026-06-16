package com.libratrack.dto.request;
import jakarta.validation.constraints.*;

/** Everyone logs in with their university/staff ID (email is notification-only). */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}