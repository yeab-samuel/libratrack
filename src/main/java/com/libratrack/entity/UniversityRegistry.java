package com.libratrack.entity;
import com.libratrack.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "university_registry",
    indexes = { @Index(name = "idx_registry_uid", columnList = "university_id") })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UniversityRegistry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false, unique = true, length = 50)
    private String universityId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 100)
    private String department;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
