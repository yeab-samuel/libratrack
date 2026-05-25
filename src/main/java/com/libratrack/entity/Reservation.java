package com.libratrack.entity;
import com.libratrack.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name="reservations",
  indexes={@Index(name="idx_res_member_id",columnList="member_id"),
           @Index(name="idx_res_book_status",columnList="book_id, status")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude={"member","book"})
public class Reservation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id",nullable=false) private User member;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="book_id",nullable=false) private Book book;
    @Column(name="queue_position",nullable=false) @Min(1) private Integer queuePosition;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private ReservationStatus status=ReservationStatus.WAITING;
    @CreationTimestamp @Column(name="reserved_at",nullable=false,updatable=false) private LocalDateTime reservedAt;
    @Column(name="notified_at") private LocalDateTime notifiedAt;
    @Column(name="expires_at") private LocalDate expiresAt;
}
