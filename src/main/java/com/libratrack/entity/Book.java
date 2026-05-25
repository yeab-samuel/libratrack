package com.libratrack.entity;
import com.libratrack.enums.BookCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.*;
@Entity
@Table(name="books",
  uniqueConstraints=@UniqueConstraint(name="uq_books_isbn",columnNames="isbn"),
  indexes={@Index(name="idx_books_isbn",columnList="isbn"),
           @Index(name="idx_books_category",columnList="category"),
           @Index(name="idx_books_author",columnList="author")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude={"copies","reservations"})
public class Book {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=20) @NotBlank private String isbn;
    @Column(nullable=false,length=300) @NotBlank private String title;
    @Column(nullable=false,length=200) @NotBlank private String author;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private BookCategory category;
    @Column(length=200) private String publisher;
    @Column(name="published_year") @Min(1000) @Max(2100) private Integer publishedYear;
    @Column(name="total_copies",nullable=false) @Min(1) private Integer totalCopies;
    @Column(length=2000) private String description;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @OneToMany(mappedBy="book",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<BookCopy> copies=new ArrayList<>();
    @OneToMany(mappedBy="book",fetch=FetchType.LAZY) private List<Reservation> reservations=new ArrayList<>();
}
