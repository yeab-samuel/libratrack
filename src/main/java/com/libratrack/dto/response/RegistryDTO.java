package com.libratrack.dto.response;

import com.libratrack.entity.UniversityRegistry;
import com.libratrack.enums.Role;
import java.time.LocalDateTime;

public record RegistryDTO(
        Long id,
        String universityId,
        String fullName,
        Role role,
        String department,
        Boolean active,
        LocalDateTime createdAt
) {
    public static RegistryDTO from(UniversityRegistry e) {
        return new RegistryDTO(
                e.getId(), e.getUniversityId(), e.getFullName(),
                e.getRole(), e.getDepartment(), e.getActive(), e.getCreatedAt()
        );
    }
}