package com.libratrack.service;

import com.libratrack.dto.request.CreateBookRequest;
import com.libratrack.entity.Book;
import com.libratrack.enums.BookCategory;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.BookRepository;
import com.libratrack.specification.BookSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @InjectMocks BookService bookService;

    // ── createBook ────────────────────────────────────────────────────────────

    @Test
    void createBook_HappyPath() {
        var req = new CreateBookRequest("978-1", "Title", "Author",
                BookCategory.SCIENCE, null, null, 2, null);
        when(bookRepository.existsByIsbn(req.isbn())).thenReturn(false);
        when(bookRepository.save(any())).thenReturn(
                Book.builder().id(1L).isbn(req.isbn()).title(req.title())
                        .author(req.author()).category(req.category())
                        .totalCopies(req.totalCopies()).build());

        var r = bookService.createBook(req);

        assertEquals("978-1", r.isbn());
        assertEquals("Title", r.title());
    }

    @Test
    void createBook_DuplicateISBN_Throws409() {
        var req = new CreateBookRequest("978-1", "T", "A",
                BookCategory.SCIENCE, null, null, 1, null);
        when(bookRepository.existsByIsbn(req.isbn())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> bookService.createBook(req));
    }

    // ── getBook ───────────────────────────────────────────────────────────────

    @Test
    void getBook_Found_ReturnsDTO() {
        Book book = Book.builder().id(5L).isbn("978-5").title("Found Book")
                .author("Author").category(BookCategory.LAW).totalCopies(3).build();
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book));

        var r = bookService.getBook(5L);

        assertEquals(5L, r.id());
        assertEquals("Found Book", r.title());
    }

    @Test
    void getBook_NotFound_ThrowsResourceNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.getBook(99L));
    }

    // ── searchBooks ───────────────────────────────────────────────────────────

    @Test
    void searchBooks_WithAllParams_ReturnsPagedResults() {
        Book book = Book.builder().id(1L).isbn("978-1").title("Java Programming")
                .author("Bloch").category(BookCategory.ENGINEERING)
                .publishedYear(2020).totalCopies(2).build();
        Page<Book> page = new PageImpl<>(List.of(book));
        Pageable pageable = PageRequest.of(0, 10);

        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = bookService.searchBooks("Java", "Bloch",
                BookCategory.ENGINEERING, 2020, true, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Programming", result.getContent().get(0).title());
    }

    @Test
    void searchBooks_WithNoParams_ReturnsAllPaginated() {
        Book b1 = Book.builder().id(1L).isbn("978-1").title("Book One")
                .author("A1").category(BookCategory.SCIENCE).totalCopies(1).build();
        Book b2 = Book.builder().id(2L).isbn("978-2").title("Book Two")
                .author("A2").category(BookCategory.HUMANITIES).totalCopies(2).build();
        Page<Book> page = new PageImpl<>(List.of(b1, b2));
        Pageable pageable = PageRequest.of(0, 20);

        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = bookService.searchBooks(null, null, null, null, null, pageable);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchBooks_NoResults_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        var result = bookService.searchBooks("NonExistent", null, null, null, null, pageable);

        assertTrue(result.isEmpty());
    }
}
