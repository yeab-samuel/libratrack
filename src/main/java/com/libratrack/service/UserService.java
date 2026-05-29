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
}
