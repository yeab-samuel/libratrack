package com.libratrack.controller;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.BookDTO;
import com.libratrack.enums.BookCategory;
import com.libratrack.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/books") @RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    @GetMapping("/search") public ResponseEntity<Page<BookDTO>> search(@RequestParam(required=false) String title,@RequestParam(required=false) String author,@RequestParam(required=false) BookCategory category,@RequestParam(required=false) Integer publishedYear,@RequestParam(required=false) Boolean available,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(bookService.searchBooks(title,author,category,publishedYear,available,pageable));}
    @GetMapping("/{id}") public ResponseEntity<BookDTO> getBook(@PathVariable Long id){return ResponseEntity.ok(bookService.getBook(id));}
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')") public ResponseEntity<BookDTO> createBook(@Valid @RequestBody CreateBookRequest req){return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(req));}
    @PatchMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<BookDTO> updateBook(@PathVariable Long id,@Valid @RequestBody UpdateBookRequest req){return ResponseEntity.ok(bookService.updateBook(id,req));}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deleteBook(@PathVariable Long id){bookService.deleteBook(id);return ResponseEntity.noContent().build();}
}
