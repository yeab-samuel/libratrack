package com.libratrack.dto.request;
import com.libratrack.enums.Role;
import jakarta.validation.constraints.*;

/** Used by ADMIN to create LIBRARIAN or ADMIN accounts (no university ID needed). */
public record CreateStaffRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull Role role
) {}
