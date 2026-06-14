package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.CreateCopyRequest;
import com.libratrack.dto.request.UpdateCopyStatusRequest;
import com.libratrack.dto.response.CopyDTO;
import com.libratrack.enums.CopyCondition;
import com.libratrack.enums.CopyStatus;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.service.BookCopyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookCopyController.class)
class BookCopyControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BookCopyService bookCopyService;

    private CopyDTO sampleCopy() {
        return new CopyDTO(1L, 10L, "COPY-001", CopyCondition.GOOD,
                CopyStatus.AVAILABLE, LocalDateTime.now());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void getCopies_AsLibrarian_Returns200() throws Exception {
        when(bookCopyService.getCopiesByBook(10L)).thenReturn(List.of(sampleCopy()));
        mockMvc.perform(get("/api/books/10/copies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].copyNumber").value("COPY-001"));
    }

    @Test @WithMockUser(roles = "STUDENT")
    void getCopies_AsStudent_Returns403() throws Exception {
        mockMvc.perform(get("/api/books/10/copies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCopies_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/books/10/copies"))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void getCopies_BookNotFound_Returns404() throws Exception {
        when(bookCopyService.getCopiesByBook(99L))
                .thenThrow(new ResourceNotFoundException("Book not found: 99"));
        mockMvc.perform(get("/api/books/99/copies"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "STUDENT")
    void getAvailableCopies_AsStudent_Returns200() throws Exception {
        when(bookCopyService.getAvailableCopiesByBook(10L)).thenReturn(List.of(sampleCopy()));
        mockMvc.perform(get("/api/books/10/copies/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void addCopy_AsLibrarian_Returns201() throws Exception {
        var req = new CreateCopyRequest("COPY-001", CopyCondition.NEW);
        when(bookCopyService.addCopy(eq(10L), any())).thenReturn(sampleCopy());
        mockMvc.perform(post("/api/books/10/copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.copyNumber").value("COPY-001"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void addCopy_AsAdmin_Returns201() throws Exception {
        var req = new CreateCopyRequest("COPY-002", CopyCondition.GOOD);
        var dto = new CopyDTO(2L, 10L, "COPY-002", CopyCondition.GOOD,
                CopyStatus.AVAILABLE, LocalDateTime.now());
        when(bookCopyService.addCopy(eq(10L), any())).thenReturn(dto);
        mockMvc.perform(post("/api/books/10/copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "STUDENT")
    void addCopy_AsStudent_Returns403() throws Exception {
        var req = new CreateCopyRequest("COPY-001", CopyCondition.NEW);
        mockMvc.perform(post("/api/books/10/copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void addCopy_DuplicateCopyNumber_Returns409() throws Exception {
        var req = new CreateCopyRequest("COPY-DUPE", CopyCondition.GOOD);
        when(bookCopyService.addCopy(eq(10L), any()))
                .thenThrow(new DuplicateResourceException("Copy number exists: COPY-DUPE"));
        mockMvc.perform(post("/api/books/10/copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void addCopy_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/books/10/copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyNumber\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void updateStatus_AsLibrarian_Returns200() throws Exception {
        var req = new UpdateCopyStatusRequest(CopyStatus.UNDER_REPAIR, CopyCondition.WORN);
        var updated = new CopyDTO(1L, 10L, "COPY-001", CopyCondition.WORN,
                CopyStatus.UNDER_REPAIR, LocalDateTime.now());
        when(bookCopyService.updateCopyStatus(eq(1L), any())).thenReturn(updated);
        mockMvc.perform(patch("/api/copies/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REPAIR"));
    }

    @Test @WithMockUser(roles = "STUDENT")
    void updateStatus_AsStudent_Returns403() throws Exception {
        var req = new UpdateCopyStatusRequest(CopyStatus.AVAILABLE, null);
        mockMvc.perform(patch("/api/copies/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void updateStatus_CopyNotFound_Returns404() throws Exception {
        var req = new UpdateCopyStatusRequest(CopyStatus.AVAILABLE, null);
        when(bookCopyService.updateCopyStatus(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Copy not found: 99"));
        mockMvc.perform(patch("/api/copies/99/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "LIBRARIAN")
    void updateStatus_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(patch("/api/copies/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}