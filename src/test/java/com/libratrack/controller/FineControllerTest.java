package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.WaiveRequest;
import com.libratrack.dto.response.FineDTO;
import com.libratrack.enums.FineStatus;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.service.FineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FineController.class)
class FineControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean FineService fineService;

    @Test @WithMockUser(roles = "STUDENT")
    void getMyFines_AsStudent_Returns200() throws Exception {
        when(fineService.getMyFines(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/fines/mine")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STUDENT")
    void getAllFines_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/fines")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void getAllFines_AsLibrarian_Returns200() throws Exception {
        when(fineService.getAllFines(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/fines")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void payFine_AsLibrarian_Returns200() throws Exception {
        FineDTO dto = new FineDTO(1L, 10L, 5L, "Student Name", "Clean Code",
                BigDecimal.valueOf(2.50), FineStatus.PAID, LocalDateTime.now(), 3L, null, LocalDateTime.now());
        when(fineService.markAsPaid(eq(1L), any())).thenReturn(dto);
        mockMvc.perform(patch("/api/fines/1/pay").with(csrf())).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STUDENT")
    void payFine_AsStudent_Returns403() throws Exception {
        mockMvc.perform(patch("/api/fines/1/pay").with(csrf())).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void waiveFine_AsAdmin_Returns200() throws Exception {
        FineDTO dto = new FineDTO(1L, 10L, 5L, "Student Name", "Clean Code",
                BigDecimal.valueOf(2.50), FineStatus.WAIVED, null, 2L, "Hardship waiver", LocalDateTime.now());
        when(fineService.waiveFine(eq(1L), any(), any())).thenReturn(dto);
        WaiveRequest req = new WaiveRequest("Hardship waiver");
        mockMvc.perform(patch("/api/fines/1/waive").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void waiveFine_AsLibrarian_Returns403() throws Exception {
        WaiveRequest req = new WaiveRequest("Trying to waive");
        mockMvc.perform(patch("/api/fines/1/waive").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void waiveFine_NotFound_Returns404() throws Exception {
        when(fineService.waiveFine(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Fine not found: 99"));
        WaiveRequest req = new WaiveRequest("Reason");
        mockMvc.perform(patch("/api/fines/99/waive").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}