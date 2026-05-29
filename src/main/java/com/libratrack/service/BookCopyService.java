package com.libratrack.service;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.CopyDTO;
import com.libratrack.entity.*;
import com.libratrack.enums.CopyStatus;
import com.libratrack.exception.*;
import com.libratrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class BookCopyService {
    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<CopyDTO> getCopiesByBook(Long bookId) {
        Book b = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        return copyRepository.findAllByBook(b).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CopyDTO> getAvailableCopiesByBook(Long bookId) {
        Book b = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        return copyRepository.findByBookAndStatus(b, CopyStatus.AVAILABLE)
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public CopyDTO addCopy(Long bookId, CreateCopyRequest req) {
        Book b = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        if (copyRepository.existsByCopyNumber(req.copyNumber()))
            throw new DuplicateResourceException("Copy number exists: " + req.copyNumber());
        return toDTO(copyRepository.save(
                BookCopy.builder().book(b).copyNumber(req.copyNumber()).condition(req.condition()).build()));
    }

    @Transactional
    public CopyDTO updateCopyStatus(Long id, UpdateCopyStatusRequest req) {
        BookCopy c = copyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found: " + id));
        c.setStatus(req.status());
        if (req.condition() != null) c.setCondition(req.condition());
        return toDTO(copyRepository.save(c));
    }

    public CopyDTO toDTO(BookCopy c) {
        return new CopyDTO(c.getId(), c.getBook().getId(), c.getCopyNumber(),
                c.getCondition(), c.getStatus(), c.getAddedAt());
    }
}