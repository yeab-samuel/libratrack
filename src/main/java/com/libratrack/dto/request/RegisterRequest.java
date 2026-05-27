package com.libratrack.dto.request;
import com.libratrack.enums.Role;
import jakarta.validation.constraints.*;

/**
 * Self-registration request for STUDENT and FACULTY only.
 * universityId must match the format DEPT/SERIAL/YEAR e.g. ATE/9305/14
 * and must exist in the university_registry table.
 */
public record RegisterRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull Role role,
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2,5}/\\d{3,6}/\\d{2}$",
             message = "University ID must be in format DEPT/SERIAL/YEAR e.g. ATE/9305/14")
    String universityId
) {}
