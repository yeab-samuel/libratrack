package com.libratrack.service;

import com.libratrack.dto.request.CreateCopyRequest;
import com.libratrack.dto.request.UpdateCopyStatusRequest;
import com.libratrack.entity.Book;
import com.libratrack.entity.BookCopy;
import com.libratrack.enums.BookCategory;
import com.libratrack.enums.CopyCondition;
import com.libratrack.enums.CopyStatus;
import com.libratrack.exception.DuplicateResourceException;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.BookCopyRepository;
import com.libratrack.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookCopyServiceTest {

    @Mock BookCopyRepository copyRepository;
    @Mock BookRepository bookRepository;
    @InjectMocks BookCopyService bookCopyService;

    private Book book;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        book = Book.builder().id(10L).isbn("978-1").title("Test Book")
                .author("Author").category(BookCategory.SCIENCE).totalCopies(2).build();
        copy = BookCopy.builder().id(1L).book(book).copyNumber("COPY-001")
                .condition(CopyCondition.GOOD).status(CopyStatus.AVAILABLE).build();
    }

    @Test
    void getCopiesByBook_HappyPath_ReturnsList() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(copyRepository.findAllByBook(book)).thenReturn(List.of(copy));
        var result = bookCopyService.getCopiesByBook(10L);
        assertEquals(1, result.size());
        assertEquals("COPY-001", result.get(0).copyNumber());
    }

    @Test
    void getCopiesByBook_EmptyBook_ReturnsEmptyList() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(copyRepository.findAllByBook(book)).thenReturn(List.of());
        assertTrue(bookCopyService.getCopiesByBook(10L).isEmpty());
    }

    @Test
    void getCopiesByBook_BookNotFound_Throws404() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookCopyService.getCopiesByBook(99L));
        verify(copyRepository, never()).findAllByBook(any());
    }

    @Test
    void getAvailableCopiesByBook_ReturnsOnlyAvailable() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(copyRepository.findByBookAndStatus(book, CopyStatus.AVAILABLE))
                .thenReturn(List.of(copy));
        var result = bookCopyService.getAvailableCopiesByBook(10L);
        assertEquals(1, result.size());
        assertEquals(CopyStatus.AVAILABLE, result.get(0).status());
    }

    @Test
    void getAvailableCopiesByBook_BookNotFound_Throws404() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> bookCopyService.getAvailableCopiesByBook(99L));
    }

    @Test
    void addCopy_HappyPath_ReturnsSavedCopy() {
        var req = new CreateCopyRequest("COPY-002", CopyCondition.NEW);
        var newCopy = BookCopy.builder().id(2L).book(book).copyNumber("COPY-002")
                .condition(CopyCondition.NEW).status(CopyStatus.AVAILABLE).build();
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(copyRepository.existsByCopyNumber("COPY-002")).thenReturn(false);
        when(copyRepository.save(any())).thenReturn(newCopy);
        var result = bookCopyService.addCopy(10L, req);
        assertEquals("COPY-002", result.copyNumber());
        verify(copyRepository).save(any(BookCopy.class));
    }

    @Test
    void addCopy_DuplicateCopyNumber_ThrowsDuplicateException() {
        var req = new CreateCopyRequest("COPY-001", CopyCondition.GOOD);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(copyRepository.existsByCopyNumber("COPY-001")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> bookCopyService.addCopy(10L, req));
        verify(copyRepository, never()).save(any());
    }

    @Test
    void addCopy_BookNotFound_Throws404() {
        var req = new CreateCopyRequest("COPY-001", CopyCondition.GOOD);
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookCopyService.addCopy(99L, req));
        verify(copyRepository, never()).save(any());
    }

    @Test
    void updateCopyStatus_StatusOnly_UpdatesStatus() {
        var req = new UpdateCopyStatusRequest(CopyStatus.UNDER_REPAIR, null);
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(copyRepository.save(any())).thenReturn(copy);
        bookCopyService.updateCopyStatus(1L, req);
        assertEquals(CopyStatus.UNDER_REPAIR, copy.getStatus());
        assertEquals(CopyCondition.GOOD, copy.getCondition());
    }

    @Test
    void updateCopyStatus_StatusAndCondition_UpdatesBoth() {
        var req = new UpdateCopyStatusRequest(CopyStatus.UNDER_REPAIR, CopyCondition.DAMAGED);
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(copyRepository.save(any())).thenReturn(copy);
        bookCopyService.updateCopyStatus(1L, req);
        assertEquals(CopyStatus.UNDER_REPAIR, copy.getStatus());
        assertEquals(CopyCondition.DAMAGED, copy.getCondition());
    }

    @Test
    void updateCopyStatus_CopyNotFound_Throws404() {
        var req = new UpdateCopyStatusRequest(CopyStatus.AVAILABLE, null);
        when(copyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> bookCopyService.updateCopyStatus(99L, req));
        verify(copyRepository, never()).save(any());
    }

    @Test
    void updateCopyStatus_MarkAvailable_SavesPersisted() {
        copy.setStatus(CopyStatus.ON_LOAN);
        var req = new UpdateCopyStatusRequest(CopyStatus.AVAILABLE, CopyCondition.WORN);
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(copyRepository.save(any())).thenReturn(copy);
        bookCopyService.updateCopyStatus(1L, req);
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        assertEquals(CopyCondition.WORN, copy.getCondition());
    }
}