package com.libratrack.dto.response;
import com.libratrack.enums.Role;
import java.time.LocalDateTime;

public record UserDTO(
    Long id,
    String email,
    Role role,
    String fullName,
    String universityId,
    Boolean active,
    LocalDateTime createdAt
) {}
