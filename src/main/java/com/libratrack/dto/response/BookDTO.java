package com.libratrack.dto.response;
import com.libratrack.enums.BookCategory;
import java.time.LocalDateTime;
public record BookDTO(Long id,String isbn,String title,String author,BookCategory category,String publisher,Integer publishedYear,Integer totalCopies,String description,LocalDateTime createdAt){}
