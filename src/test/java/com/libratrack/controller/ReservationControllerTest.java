package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.CreateReservationRequest;
import com.libratrack.dto.response.ReservationDTO;
import com.libratrack.enums.ReservationStatus;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.NoCopyAvailableException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReservationService reservationService;

    // ── 1. createReservation_AsStudent_Returns201 ────────────────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void createReservation_AsStudent_Returns201() throws Exception {
        ReservationDTO dto = new ReservationDTO(
            1L, 5L, "Student Name", 10L, "Clean Code",
            1, ReservationStatus.WAITING, LocalDateTime.now(), null, null);
        when(reservationService.createReservation(any(), any())).thenReturn(dto);

        CreateReservationRequest req = new CreateReservationRequest(10L);
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.queuePosition").value(1));
    }

    // ── 2. createReservation_AsLibrarian_Returns403 ──────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createReservation_AsLibrarian_Returns403() throws Exception {
        CreateReservationRequest req = new CreateReservationRequest(10L);
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    // ── 3. createReservation_NotAuthenticated_Returns401 ────────────────────
    @Test
    void createReservation_NotAuthenticated_Returns401() throws Exception {
        CreateReservationRequest req = new CreateReservationRequest(10L);
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    // ── 4. createReservation_CopiesAvailable_Returns409 ─────────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void createReservation_CopiesAvailable_Returns409() throws Exception {
        when(reservationService.createReservation(any(), any()))
            .thenThrow(new NoCopyAvailableException("Copies available. Borrow directly."));

        CreateReservationRequest req = new CreateReservationRequest(10L);
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    // ── 5. createReservation_DuplicateReservation_Returns409 ─────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void createReservation_DuplicateReservation_Returns409() throws Exception {
        when(reservationService.createReservation(any(), any()))
            .thenThrow(new DuplicateResourceException("Already have active reservation for this book."));

        CreateReservationRequest req = new CreateReservationRequest(10L);
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    // ── 6. createReservation_InvalidBody_Returns400 ──────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void createReservation_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":null}"))
            .andExpect(status().isBadRequest());
    }

    // ── 7. cancelReservation_AsStudent_Returns204 ────────────────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void cancelReservation_AsStudent_Returns204() throws Exception {
        doNothing().when(reservationService).cancelReservation(eq(1L), any());
        mockMvc.perform(delete("/api/reservations/1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    // ── 8. cancelReservation_NotFound_Returns404 ─────────────────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void cancelReservation_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Reservation not found: 99"))
            .when(reservationService).cancelReservation(eq(99L), any());
        mockMvc.perform(delete("/api/reservations/99").with(csrf()))
            .andExpect(status().isNotFound());
    }

    // ── 9. cancelReservation_AsLibrarian_Returns403 ──────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void cancelReservation_AsLibrarian_Returns403() throws Exception {
        mockMvc.perform(delete("/api/reservations/1").with(csrf()))
            .andExpect(status().isForbidden());
    }

    // ── 10. getMyReservations_AsStudent_Returns200 ───────────────────────────
    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void getMyReservations_AsStudent_Returns200() throws Exception {
        when(reservationService.getMyReservations(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/reservations/mine"))
            .andExpect(status().isOk());
    }

    // ── 11. getMyReservations_NotAuthenticated_Returns401 ───────────────────
    @Test
    void getMyReservations_NotAuthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/reservations/mine"))
            .andExpect(status().isUnauthorized());
    }

    // ── 12. getAllReservations_AsAdmin_Returns200 ────────────────────────────
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllReservations_AsAdmin_Returns200() throws Exception {
        when(reservationService.getAllReservations(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/reservations"))
            .andExpect(status().isOk());
    }

    // ── 13. getAllReservations_AsStudent_Returns403 ──────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void getAllReservations_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/reservations"))
            .andExpect(status().isForbidden());
    }

    // ── 14. getAllReservations_AsLibrarian_Returns200 ────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getAllReservations_AsLibrarian_Returns200() throws Exception {
        when(reservationService.getAllReservations(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/reservations"))
            .andExpect(status().isOk());
    }

    // ── 15. cancelReservation_OtherUsersReservation_Returns403 ──────────────
    @Test
    @WithMockUser(username = "student_b@test.com", roles = "STUDENT")
    void cancelReservation_OtherUsersReservation_Returns403() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException(
            "You can only cancel your own reservations."))
            .when(reservationService).cancelReservation(anyLong(), any());
        mockMvc.perform(delete("/api/reservations/1").with(csrf()))
            .andExpect(status().isForbidden());
    }

}
