package com.libratrack.service;

import com.libratrack.dto.response.UserDTO;
import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthService authService;
    @InjectMocks UserService userService;

    private User user;
    private UserDTO dto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("u@test.com").fullName("Test User")
                .role(Role.STUDENT).universityId("UGR/1234/20").active(true).build();
        dto = new UserDTO(1L, "u@test.com", Role.STUDENT, "Test User", "UGR/1234/20", true, null);
        pageable = PageRequest.of(0, 20);
    }

    // ── getUsers ──────────────────────────────────────────────────────────

    @Test
    void getUsers_RoleAndActiveBothProvided_UsesFindByRoleAndActive() {
        when(userRepository.findByRoleAndActive(Role.STUDENT, true, pageable))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(authService.toDTO(user)).thenReturn(dto);

        Page<UserDTO> result = userService.getUsers(Role.STUDENT, true, pageable);

        assertEquals(List.of(dto), result.getContent());
        verify(userRepository, never()).findByRole(any(), any());
        verify(userRepository, never()).findByActive(any(), any());
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_OnlyRoleProvided_UsesFindByRole() {
        when(userRepository.findByRole(Role.LIBRARIAN, pageable))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(authService.toDTO(user)).thenReturn(dto);

        userService.getUsers(Role.LIBRARIAN, null, pageable);

        verify(userRepository).findByRole(Role.LIBRARIAN, pageable);
        verify(userRepository, never()).findByRoleAndActive(any(), any(), any());
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_OnlyActiveProvided_UsesFindByActive() {
        when(userRepository.findByActive(false, pageable))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(authService.toDTO(user)).thenReturn(dto);

        userService.getUsers(null, false, pageable);

        verify(userRepository).findByActive(false, pageable);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_NeitherProvided_UsesFindAll() {
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(authService.toDTO(user)).thenReturn(dto);

        userService.getUsers(null, null, pageable);

        verify(userRepository).findAll(pageable);
    }

    // ── getUserById ───────────────────────────────────────────────────────

    @Test
    void getUserById_Found_ReturnsDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authService.toDTO(user)).thenReturn(dto);

        assertEquals(dto, userService.getUserById(1L));
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ── getUserByUniversityId ─────────────────────────────────────────────

    @Test
    void getUserByUniversityId_Found_ReturnsDTO() {
        when(userRepository.findByUniversityId("UGR/1234/20")).thenReturn(Optional.of(user));
        when(authService.toDTO(user)).thenReturn(dto);

        assertEquals(dto, userService.getUserByUniversityId("UGR/1234/20"));
    }

    @Test
    void getUserByUniversityId_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByUniversityId("UNKNOWN/0/00")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUniversityId("UNKNOWN/0/00"));
    }

    // ── deactivateUser / activateUser ────────────────────────────────────

    @Test
    void deactivateUser_Found_SetsInactiveAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(authService.toDTO(user)).thenReturn(dto);

        userService.deactivateUser(1L);

        assertFalse(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deactivateUser(99L));
    }

    @Test
    void activateUser_Found_SetsActiveAndSaves() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(authService.toDTO(user)).thenReturn(dto);

        userService.activateUser(1L);

        assertTrue(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.activateUser(99L));
    }
}