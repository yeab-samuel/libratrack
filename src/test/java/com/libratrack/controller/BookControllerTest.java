package com.libratrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libratrack.dto.request.CreateBookRequest;
import com.libratrack.dto.request.UpdateBookRequest;
import com.libratrack.dto.response.BookDTO;
import com.libratrack.enums.BookCategory;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BookService bookService;

    private BookDTO sampleDTO() {
        return new BookDTO(1L, "978-1", "Title", "Author",
                BookCategory.SCIENCE, null, 2020, 2, null, LocalDateTime.now(), null, 0L);
    }

    // ── POST /api/books ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createBook_AsLibrarian_Returns201() throws Exception {
        var req = new CreateBookRequest("978-1", "Title", "Author",
                BookCategory.SCIENCE, null, 2020, 2, null);
        when(bookService.createBook(any())).thenReturn(sampleDTO());

        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("978-1"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBook_AsStudent_Returns403() throws Exception {
        var req = new CreateBookRequest("978-1", "T", "A",
                BookCategory.SCIENCE, null, null, 1, null);
        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBook_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createBook_DuplicateIsbn_Returns409() throws Exception {
        var req = new CreateBookRequest("978-DUPE", "Title", "Author",
                BookCategory.SCIENCE, null, 2020, 2, null);
        when(bookService.createBook(any()))
                .thenThrow(new DuplicateResourceException("ISBN exists: 978-DUPE"));

        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ── GET /api/books/search ─────────────────────────────────────────────────

    @Test
    void searchBooks_NoAuth_Returns200() throws Exception {
        when(bookService.searchBooks(any(), any(), any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mockMvc.perform(get("/api/books/search"))
                .andExpect(status().isOk());
    }

    // ── GET /api/books/{id} ───────────────────────────────────────────────────

    @Test
    void getBook_NotFound_Returns404() throws Exception {
        when(bookService.getBook(99L))
                .thenThrow(new ResourceNotFoundException("Book not found: 99"));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/books/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBook_AsAdmin_Returns200() throws Exception {
        var req = new UpdateBookRequest("New Title", null, null, null, null, null, null);
        var updated = new BookDTO(1L, "978-1", "New Title", "Author",
                BookCategory.SCIENCE, null, 2020, 2, null, LocalDateTime.now(), null, 0L);
        when(bookService.updateBook(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void updateBook_AsLibrarian_Returns403() throws Exception {
        var req = new UpdateBookRequest("New Title", null, null, null, null, null, null);
        mockMvc.perform(patch("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBook_NotFound_Returns404() throws Exception {
        var req = new UpdateBookRequest("X", null, null, null, null, null, null);
        when(bookService.updateBook(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Book not found: 99"));

        mockMvc.perform(patch("/api/books/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/books/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBook_AsAdmin_Returns204() throws Exception {
        mockMvc.perform(delete("/api/books/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void deleteBook_AsLibrarian_Returns403() throws Exception {
        mockMvc.perform(delete("/api/books/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBook_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found: 99"))
                .when(bookService).deleteBook(99L);

        mockMvc.perform(delete("/api/books/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
