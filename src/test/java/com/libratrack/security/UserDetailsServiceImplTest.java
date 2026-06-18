package com.libratrack.security;

import com.libratrack.entity.User;
import com.libratrack.enums.Role;
import com.libratrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_ExistingEmail_ReturnsUser() {
        User user = User.builder()
                .id(1L).email("found@test.com").role(Role.LIBRARIAN)
                .fullName("Found User").active(true).passwordHash("hashed").build();
        when(userRepository.findByEmail("found@test.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("found@test.com");

        assertSame(user, result);
        assertEquals("found@test.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_UnknownEmail_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@test.com"));
        assertTrue(ex.getMessage().contains("missing@test.com"));
    }
}