package com.libratrack.entity;
import com.libratrack.enums.FineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name="fine_records",
  indexes={@Index(name="idx_fines_member_id",columnList="member_id"),
           @Index(name="idx_fines_status",columnList="status")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude={"loan","member","collectedBy"})
public class FineRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="loan_id",nullable=false,unique=true) private Loan loan;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id",nullable=false) private User member;
    @Column(nullable=false,precision=10,scale=2) @DecimalMin(value="0.00", inclusive=false) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private FineStatus status=FineStatus.UNPAID;
    @Column(name="paid_at") private LocalDateTime paidAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="collected_by") private User collectedBy;
    @Column(name="waive_reason",length=500) private String waiveReason;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}
