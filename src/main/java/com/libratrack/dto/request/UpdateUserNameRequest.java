package com.libratrack.dto.request;
import jakarta.validation.constraints.*;

/**
 * Admin-only correction of a user's full name on file.
 *
 * Registration enforces that the typed name matches the registry at
 * sign-up time, but a mismatch can still slip through if an account was
 * created before that check existed, or if a librarian needs to correct a
 * typo after verifying the person's physical ID in person. This endpoint
 * exists for that out-of-band correction, made by a trusted admin rather
 * than self-serve.
 */
public record UpdateUserNameRequest(
        @NotBlank @Size(min = 2, max = 200) String fullName
) {}