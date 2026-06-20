package com.libratrack.service;
import com.libratrack.dto.response.UserDTO;
import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(Role role, Boolean active, Pageable pageable) {
        if (role != null && active != null) return userRepository.findByRoleAndActive(role, active, pageable).map(authService::toDTO);
        if (role != null)   return userRepository.findByRole(role, pageable).map(authService::toDTO);
        if (active != null) return userRepository.findByActive(active, pageable).map(authService::toDTO);
        return userRepository.findAll(pageable).map(authService::toDTO);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return authService.toDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByUniversityId(String universityId) {
        return authService.toDTO(userRepository.findByUniversityId(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with university ID: " + universityId)));
    }

    @Transactional
    public UserDTO deactivateUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        u.setActive(false);
        return authService.toDTO(userRepository.save(u));
    }

    @Transactional
    public UserDTO activateUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        u.setActive(true);
        return authService.toDTO(userRepository.save(u));
    }

    /**
     * Admin-only correction of a user's full name on file — e.g. fixing a
     * name that doesn't match the registry for their university ID. This is
     * a deliberate out-of-band fix: the librarian/admin verifies the
     * person's physical ID in person, then corrects the record directly,
     * rather than requiring the account to be deleted and re-registered
     * (which would orphan their existing loan/fine/reservation history).
     */
    @Transactional
    public UserDTO updateFullName(Long id, String fullName) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        u.setFullName(fullName.trim());
        return authService.toDTO(userRepository.save(u));
    }
}