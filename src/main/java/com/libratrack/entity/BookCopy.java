package com.libratrack.entity;
import com.libratrack.enums.CopyCondition;
import com.libratrack.enums.CopyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name="book_copies",
  uniqueConstraints=@UniqueConstraint(name="uq_copy_number",columnNames="copy_number"),
  indexes={@Index(name="idx_copies_book_id",columnList="book_id"),
           @Index(name="idx_copies_status",columnList="status")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude="book")
public class BookCopy {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="book_id",nullable=false) private Book book;
    @Column(name="copy_number",nullable=false,unique=true,length=30) @NotBlank private String copyNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CopyCondition condition;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private CopyStatus status=CopyStatus.AVAILABLE;
    @CreationTimestamp @Column(name="added_at",nullable=false,updatable=false) private LocalDateTime addedAt;
}
