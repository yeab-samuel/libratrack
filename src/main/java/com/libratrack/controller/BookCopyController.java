package com.libratrack.controller;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.CopyDTO;
import com.libratrack.service.BookCopyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor
public class BookCopyController {
    private final BookCopyService bookCopyService;

    @GetMapping("/api/books/{bookId}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<CopyDTO>> getCopies(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookCopyService.getCopiesByBook(bookId));
    }

    /** Open to all authenticated users — needed for self-service borrow modal */
    @GetMapping("/api/books/{bookId}/copies/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CopyDTO>> getAvailableCopies(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookCopyService.getAvailableCopiesByBook(bookId));
    }

    @PostMapping("/api/books/{bookId}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CopyDTO> addCopy(@PathVariable Long bookId,
                                           @Valid @RequestBody CreateCopyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookCopyService.addCopy(bookId, req));
    }

    @PatchMapping("/api/copies/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CopyDTO> updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody UpdateCopyStatusRequest req) {
        return ResponseEntity.ok(bookCopyService.updateCopyStatus(id, req));
    }
}