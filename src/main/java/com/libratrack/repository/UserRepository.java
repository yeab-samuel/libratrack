package com.libratrack.repository;
import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUniversityId(String universityId);
    Optional<User> findByUniversityId(String universityId);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByActive(Boolean active, Pageable pageable);
    Page<User> findByRoleAndActive(Role role, Boolean active, Pageable pageable);
}
