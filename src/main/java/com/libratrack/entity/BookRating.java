package com.libratrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * One star rating (1–5) per member per book.
 * A rating can only be submitted after the member has returned the book
 * (enforced in BookRatingService, not here).
 * Re-rating the same book updates the existing row via upsert.
 */
@Entity
@Table(name = "book_ratings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rating_book_member",
        columnNames = {"book_id", "member_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BookRating {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(nullable = false)
    @Min(1) @Max(5)
    private Integer rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
