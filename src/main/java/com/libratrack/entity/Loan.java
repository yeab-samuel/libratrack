package com.libratrack.entity;
import com.libratrack.enums.LoanStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name="loans",
  indexes={@Index(name="idx_loans_member_id",columnList="member_id"),
           @Index(name="idx_loans_status",columnList="status"),
           @Index(name="idx_loans_due_date",columnList="due_date")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude={"member","bookCopy","processedBy","fineRecord"})
public class Loan {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id",nullable=false) private User member;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="book_copy_id",nullable=false) private BookCopy bookCopy;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="processed_by") private User processedBy;
    @CreationTimestamp @Column(name="issued_at",nullable=false,updatable=false) private LocalDateTime issuedAt;
    @Column(name="due_date",nullable=false) @NotNull private LocalDate dueDate;
    @Column(name="returned_at") private LocalDateTime returnedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private LoanStatus status=LoanStatus.ACTIVE;
    @OneToOne(mappedBy="loan",cascade=CascadeType.ALL,fetch=FetchType.LAZY) private FineRecord fineRecord;
}
