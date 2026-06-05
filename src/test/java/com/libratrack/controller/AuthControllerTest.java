package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.LoginRequest;
import com.libratrack.dto.request.RegisterRequest;
import com.libratrack.dto.response.TokenResponse;
import com.libratrack.dto.response.UserDTO;
import com.libratrack.enums.Role;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;


import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean
    private com.libratrack.security.JwtUtils jwtUtils;

    @MockBean
    private com.libratrack.security.JwtAuthFilter jwtAuthFilter;

    @MockBean
    private com.libratrack.repository.UserRepository userRepository;

    @MockBean
    private com.libratrack.repository.BookRepository bookRepository;

    @MockBean
    private com.libratrack.repository.BookCopyRepository bookCopyRepository;

    @MockBean
    private com.libratrack.repository.LoanRepository loanRepository;

    @MockBean
    private com.libratrack.repository.FineRecordRepository fineRepository;

    @MockBean
    private com.libratrack.repository.ReservationRepository reservationRepository;

    @MockBean
    private com.libratrack.service.NotificationService notificationService;


    @Test
    void register_ValidRequest_Returns201() throws Exception {
        UserDTO dto = new UserDTO(1L, "john@test.com", Role.STUDENT,
                "John Doe", "ATE/9305/14", true, LocalDateTime.now());
        when(authService.register(any())).thenReturn(dto);

        RegisterRequest req = new RegisterRequest(
                "John Doe", "john@test.com", "password123", Role.STUDENT, "ATE/9305/14");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void register_DuplicateEmail_Returns409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Email already registered"));

        RegisterRequest req = new RegisterRequest(
                "John Doe", "john@test.com", "password123", Role.STUDENT, "ATE/9305/14");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "John Doe", "not-an-email", "password123", Role.STUDENT, "ATE/9305/14");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_BlankFullName_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"john@test.com\","
                                + "\"password\":\"password123\",\"role\":\"STUDENT\","
                                + "\"universityId\":\"ATE/9305/14\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortPassword_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"John\",\"email\":\"john@test.com\","
                                + "\"password\":\"short\",\"role\":\"STUDENT\","
                                + "\"universityId\":\"ATE/9305/14\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidCredentials_Returns200WithToken() throws Exception {
        String token = "jwt-token-here";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        TokenResponse tokenResponse = new TokenResponse(token, expiresAt, "John Doe");
        when(authService.login(any())).thenReturn(tokenResponse);

        LoginRequest req = new LoginRequest("john@test.com", "password123");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-here"));
    }

    @Test
    void login_WrongPassword_Returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest("john@test.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}