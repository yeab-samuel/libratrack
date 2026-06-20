package com.libratrack.service;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.BookDTO;
import com.libratrack.entity.Book;
import com.libratrack.enums.BookCategory;
import com.libratrack.enums.CopyStatus;
import com.libratrack.exception.*;
import com.libratrack.repository.BookCopyRepository;
import com.libratrack.repository.BookRatingRepository;
import com.libratrack.repository.BookRepository;
import com.libratrack.specification.BookSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookRatingRepository bookRatingRepository;
    private final BookCopyRepository bookCopyRepository;

    @Transactional public BookDTO createBook(CreateBookRequest req){
        if(bookRepository.existsByIsbn(req.isbn())) throw new DuplicateResourceException("ISBN exists: "+req.isbn());
        return toDTO(bookRepository.save(Book.builder().isbn(req.isbn()).title(req.title()).author(req.author()).category(req.category()).publisher(req.publisher()).publishedYear(req.publishedYear()).totalCopies(req.totalCopies()).description(req.description()).build()));
    }
    @Transactional(readOnly=true) public BookDTO getBook(Long id){return toDTO(bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book not found: "+id)));}

    @Transactional(readOnly=true) public Page<BookDTO> searchBooks(String title,String author,BookCategory category,Integer year,Boolean available,Pageable pageable){
        Page<Book> page = bookRepository.findAll(BookSpecification.withFilters(title,author,category,year,available),pageable);
        Map<Long,BookRatingRepository.RatingStats> ratingStats = ratingStatsByBookId(page.getContent());
        Map<Long,Long> availableCounts = availableCountsByBookId(page.getContent());
        return page.map(b -> toDTO(b, ratingStats.get(b.getId()), availableCounts.getOrDefault(b.getId(), 0L)));
    }

    @Transactional public BookDTO updateBook(Long id,UpdateBookRequest req){
        Book b=bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book not found: "+id));
        if(req.title()!=null)b.setTitle(req.title());if(req.author()!=null)b.setAuthor(req.author());if(req.category()!=null)b.setCategory(req.category());if(req.publisher()!=null)b.setPublisher(req.publisher());if(req.publishedYear()!=null)b.setPublishedYear(req.publishedYear());if(req.totalCopies()!=null)b.setTotalCopies(req.totalCopies());if(req.description()!=null)b.setDescription(req.description());
        return toDTO(bookRepository.save(b));
    }
    @Transactional public void deleteBook(Long id){if(!bookRepository.existsById(id))throw new ResourceNotFoundException("Book not found: "+id);bookRepository.deleteById(id);}

    /** Single-book lookup: fetches this book's rating stats and live available-copy count individually. */
    public BookDTO toDTO(Book b){
        var ratingStats = bookRatingRepository.findStatsByBookIds(List.of(b.getId()));
        long available = bookCopyRepository.countByBookAndStatus(b, CopyStatus.AVAILABLE);
        return toDTO(b, ratingStats.isEmpty() ? null : ratingStats.get(0), available);
    }

    private BookDTO toDTO(Book b, BookRatingRepository.RatingStats ratingStats, long availableCopies){
        Double avg = ratingStats != null ? ratingStats.getAverage() : null;
        Long count = ratingStats != null ? ratingStats.getCount() : 0L;
        return new BookDTO(b.getId(),b.getIsbn(),b.getTitle(),b.getAuthor(),b.getCategory(),b.getPublisher(),b.getPublishedYear(),b.getTotalCopies(),(int) availableCopies,b.getDescription(),b.getCreatedAt(),avg,count);
    }

    /** Batch-load rating stats for a page of books to avoid N+1 queries. */
    private Map<Long,BookRatingRepository.RatingStats> ratingStatsByBookId(List<Book> books){
        if(books.isEmpty()) return Map.of();
        List<Long> ids = books.stream().map(Book::getId).collect(Collectors.toList());
        return bookRatingRepository.findStatsByBookIds(ids).stream()
                .collect(Collectors.toMap(BookRatingRepository.RatingStats::getBookId, s -> s));
    }

    /** Batch-load AVAILABLE copy counts for a page of books to avoid N+1 queries. */
    private Map<Long,Long> availableCountsByBookId(List<Book> books){
        if(books.isEmpty()) return Map.of();
        List<Long> ids = books.stream().map(Book::getId).collect(Collectors.toList());
        return bookCopyRepository.countAvailableByBookIds(ids).stream()
                .collect(Collectors.toMap(BookCopyRepository.AvailabilityCount::getBookId, BookCopyRepository.AvailabilityCount::getCount));
    }
}