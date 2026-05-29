package com.libratrack.service;
import com.libratrack.dto.request.*;
import com.libratrack.dto.response.BookDTO;
import com.libratrack.entity.Book;
import com.libratrack.enums.BookCategory;
import com.libratrack.exception.*;
import com.libratrack.repository.BookRepository;
import com.libratrack.specification.BookSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    @Transactional public BookDTO createBook(CreateBookRequest req){
        if(bookRepository.existsByIsbn(req.isbn())) throw new DuplicateResourceException("ISBN exists: "+req.isbn());
        return toDTO(bookRepository.save(Book.builder().isbn(req.isbn()).title(req.title()).author(req.author()).category(req.category()).publisher(req.publisher()).publishedYear(req.publishedYear()).totalCopies(req.totalCopies()).description(req.description()).build()));
    }
    @Transactional(readOnly=true) public BookDTO getBook(Long id){return toDTO(bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book not found: "+id)));}
    @Transactional(readOnly=true) public Page<BookDTO> searchBooks(String title,String author,BookCategory category,Integer year,Boolean available,Pageable pageable){return bookRepository.findAll(BookSpecification.withFilters(title,author,category,year,available),pageable).map(this::toDTO);}
    @Transactional public BookDTO updateBook(Long id,UpdateBookRequest req){
        Book b=bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book not found: "+id));
        if(req.title()!=null)b.setTitle(req.title());if(req.author()!=null)b.setAuthor(req.author());if(req.category()!=null)b.setCategory(req.category());if(req.publisher()!=null)b.setPublisher(req.publisher());if(req.publishedYear()!=null)b.setPublishedYear(req.publishedYear());if(req.totalCopies()!=null)b.setTotalCopies(req.totalCopies());if(req.description()!=null)b.setDescription(req.description());
        return toDTO(bookRepository.save(b));
    }
    @Transactional public void deleteBook(Long id){if(!bookRepository.existsById(id))throw new ResourceNotFoundException("Book not found: "+id);bookRepository.deleteById(id);}
    public BookDTO toDTO(Book b){return new BookDTO(b.getId(),b.getIsbn(),b.getTitle(),b.getAuthor(),b.getCategory(),b.getPublisher(),b.getPublishedYear(),b.getTotalCopies(),b.getDescription(),b.getCreatedAt());}
}
