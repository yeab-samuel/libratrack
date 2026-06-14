package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.CreateLoanRequest;
import com.libratrack.dto.response.LoanDTO;
import com.libratrack.enums.LoanStatus;
import com.libratrack.exception.BorrowLimitExceededException;
import com.libratrack.exception.NoCopyAvailableException;
import com.libratrack.exception.UnpaidFineException;
import com.libratrack.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
class LoanControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean LoanService loanService;

    // ── 1. getMyLoans_NotAuthenticated_Returns401 ────────────────────────────
    @Test
    void getMyLoans_NotAuthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/loans/mine"))
            .andExpect(status().isUnauthorized());
    }

    // ── 2. getMyLoans_AsStudent_Returns200 ───────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyLoans_AsStudent_Returns200() throws Exception {
        when(loanService.getMyLoans(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/loans/mine"))
            .andExpect(status().isOk());
    }

    // ── 3. getAllLoans_AsStudent_Returns403 ──────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void getAllLoans_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/loans"))
            .andExpect(status().isForbidden());
    }

    // ── 4. getAllLoans_AsLibrarian_Returns200 ────────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getAllLoans_AsLibrarian_Returns200() throws Exception {
        when(loanService.getAllLoans(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/loans"))
            .andExpect(status().isOk());
    }

    // ── 5. issueLoan_AsLibrarian_Returns201 ─────────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void issueLoan_AsLibrarian_Returns201() throws Exception {
        LoanDTO dto = new LoanDTO(1L, 1L, "Student Name", 99L, 1L, "C-001",
            "Clean Code", LocalDateTime.now(), LocalDate.now().plusDays(30),
            null, LoanStatus.ACTIVE, 2L);
        when(loanService.createLoan(any(), any())).thenReturn(dto);

        CreateLoanRequest req = new CreateLoanRequest(1L, 1L, LocalDate.now().plusDays(30));
        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    // ── 6. issueLoan_NoCopyAvailable_Returns409 ──────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void issueLoan_NoCopyAvailable_Returns409() throws Exception {
        when(loanService.createLoan(any(), any()))
            .thenThrow(new NoCopyAvailableException("No copy available"));

        CreateLoanRequest req = new CreateLoanRequest(1L, 1L, LocalDate.now().plusDays(30));
        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    // ── 7. issueLoan_UnpaidFine_Returns422 ───────────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void issueLoan_UnpaidFine_Returns422() throws Exception {
        when(loanService.createLoan(any(), any()))
            .thenThrow(new UnpaidFineException("Member has unpaid fines"));

        CreateLoanRequest req = new CreateLoanRequest(1L, 1L, LocalDate.now().plusDays(30));
        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── 8. issueLoan_BorrowLimitExceeded_Returns422 ──────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void issueLoan_BorrowLimitExceeded_Returns422() throws Exception {
        when(loanService.createLoan(any(), any()))
            .thenThrow(new BorrowLimitExceededException("Borrow limit reached"));

        CreateLoanRequest req = new CreateLoanRequest(1L, 1L, LocalDate.now().plusDays(30));
        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── 9. extendLoan_AsFaculty_Returns200 ───────────────────────────────────
    @Test
    @WithMockUser(username = "faculty@test.com", roles = "FACULTY")
    void extendLoan_AsFaculty_Returns200() throws Exception {
        LoanDTO dto = new LoanDTO(1L, 1L, "Faculty Member", 99L, 1L, "C-001",
            "Clean Code", LocalDateTime.now(), LocalDate.now().plusDays(30),
            null, LoanStatus.ACTIVE, null);
        when(loanService.extendLoan(anyLong(), any(), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/loans/1/extend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"2026-07-01\"}"))
            .andExpect(status().isOk());
    }

    // ── 10. extendLoan_AsStudent_Returns403 ──────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void extendLoan_AsStudent_Returns403() throws Exception {
        mockMvc.perform(patch("/api/loans/1/extend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"2026-07-01\"}"))
            .andExpect(status().isForbidden());
    }

    // ── 11. extendLoan_NonOwnedLoan_Returns403 ───────────────────────────────
    @Test
    @WithMockUser(username = "faculty@test.com", roles = "FACULTY")
    void extendLoan_NonOwnedLoan_Returns403() throws Exception {
        when(loanService.extendLoan(anyLong(), any(), any()))
            .thenThrow(new org.springframework.security.access.AccessDeniedException("You can only extend your own loans."));

        mockMvc.perform(patch("/api/loans/1/extend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"2026-07-01\"}"))
            .andExpect(status().isForbidden());
    }

    // ── 12. extendLoan_PastDate_Returns400 ───────────────────────────────────
    @Test
    @WithMockUser(username = "faculty@test.com", roles = "FACULTY")
    void extendLoan_PastDate_Returns400() throws Exception {
        when(loanService.extendLoan(anyLong(), any(), any()))
            .thenThrow(new IllegalArgumentException("New due date must be after current due date"));

        mockMvc.perform(patch("/api/loans/1/extend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"2026-07-01\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── 9b. returnLoan_AsLibrarian_Returns200 ────────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void returnLoan_AsLibrarian_Returns200() throws Exception {
        LoanDTO dto = new LoanDTO(1L, 1L, "Student Name", 99L, 1L, "C-001",
            "Clean Code", LocalDateTime.now(), LocalDate.now().plusDays(30),
            LocalDateTime.now(), LoanStatus.RETURNED, 2L);
        when(loanService.returnLoan(anyLong(), any())).thenReturn(dto);
        mockMvc.perform(patch("/api/loans/1/return").with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    // ── 9c. returnLoan_AsStudent_Returns403 ──────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    void returnLoan_AsStudent_Returns403() throws Exception {
        mockMvc.perform(patch("/api/loans/1/return").with(csrf()))
            .andExpect(status().isForbidden());
    }

    // ── 9d. returnLoan_NotFound_Returns404 ───────────────────────────────────
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void returnLoan_NotFound_Returns404() throws Exception {
        when(loanService.returnLoan(anyLong(), any()))
            .thenThrow(new com.libratrack.exception.ResourceNotFoundException("Loan not found"));
        mockMvc.perform(patch("/api/loans/99/return").with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}