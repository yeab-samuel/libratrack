package com.libratrack.dto.request;
import com.libratrack.enums.BookCategory;
import jakarta.validation.constraints.*;
public record UpdateBookRequest(@Size(max=300) String title,@Size(max=200) String author,BookCategory category,String publisher,@Min(1000) @Max(2100) Integer publishedYear,@Min(1) Integer totalCopies,@Size(max=2000) String description){}
