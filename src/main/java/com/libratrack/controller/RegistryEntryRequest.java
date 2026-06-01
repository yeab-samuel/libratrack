package com.libratrack.controller;

import com.libratrack.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegistryEntryRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z]{2,5}/\\d{3,6}/\\d{2}$",
                message = "Format must be DEPT/SERIAL/YEAR e.g. ATE/9305/14")
        String universityId,
        @NotBlank String fullName,
        @NotNull Role role,
        String department
) {}
