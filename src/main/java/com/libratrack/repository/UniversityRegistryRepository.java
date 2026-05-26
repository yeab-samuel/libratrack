package com.libratrack.repository;
import com.libratrack.entity.UniversityRegistry;
import com.libratrack.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UniversityRegistryRepository extends JpaRepository<UniversityRegistry, Long> {
    Optional<UniversityRegistry> findByUniversityId(String universityId);
    boolean existsByUniversityId(String universityId);
    Optional<UniversityRegistry> findByUniversityIdAndRole(String universityId, Role role);
}
