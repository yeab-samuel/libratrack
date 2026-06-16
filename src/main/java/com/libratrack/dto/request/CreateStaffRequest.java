package com.libratrack.dto.request;
import com.libratrack.enums.Role;
import jakarta.validation.constraints.*;

/**
 * Used by ADMIN to create LIBRARIAN or ADMIN accounts.
 *
 * Staff ID formats:
 *   LIB/XXXX/YY — librarians   e.g. LIB/0042/19
 *   ADM/XXX/YY  — admins       e.g. ADM/001/00
 *
 * Staff log in with this ID, same as students/faculty log in with their
 * university ID. Email is stored for notifications only.
 */
public record CreateStaffRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role,
        @NotBlank
        @Pattern(
                regexp  = "^(LIB|ADM)/\\d{3,6}/\\d{2}$",
                message = "Staff ID format: LIB/XXXX/YY for librarians, ADM/XXX/YY for admins"
        )
        String staffId
) {}