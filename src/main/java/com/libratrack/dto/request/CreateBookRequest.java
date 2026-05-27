package com.libratrack.dto.request;
import com.libratrack.enums.BookCategory;
import jakarta.validation.constraints.*;
public record CreateBookRequest(@NotBlank @Size(max=20) String isbn,@NotBlank @Size(max=300) String title,@NotBlank @Size(max=200) String author,@NotNull BookCategory category,String publisher,@Min(1000) @Max(2100) Integer publishedYear,@Min(1) @NotNull Integer totalCopies,@Size(max=2000) String description){}
