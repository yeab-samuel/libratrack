package com.libratrack.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.libratrack.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.*;
@Entity
@Table(name="users",
  uniqueConstraints={@UniqueConstraint(name="uq_users_email",columnNames="email"),
                     @UniqueConstraint(name="uq_users_student_id",columnNames="student_id")},
  indexes={@Index(name="idx_users_email",columnList="email"),
           @Index(name="idx_users_role",columnList="role")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of="id") @ToString(exclude={"loans","reservations","fines"})
public class User implements UserDetails {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) @Email @NotBlank private String email;
    @Column(name="password_hash",nullable=false) @JsonIgnore private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
    @Column(name="full_name",nullable=false,length=200) @NotBlank private String fullName;
    @Column(name="university_id",length = 50) private String universityId;
    @Column(nullable=false) @Builder.Default private Boolean active=true;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @OneToMany(mappedBy="member",fetch=FetchType.LAZY) @JsonIgnore private List<Loan> loans=new ArrayList<>();
    @OneToMany(mappedBy="member",fetch=FetchType.LAZY) @JsonIgnore private List<Reservation> reservations=new ArrayList<>();
    @OneToMany(mappedBy="member",fetch=FetchType.LAZY) @JsonIgnore private List<FineRecord> fines=new ArrayList<>();
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));}
    @Override public String getPassword(){return passwordHash;}
    @Override public String getUsername(){return email;}
    @Override public boolean isEnabled(){return Boolean.TRUE.equals(active);}
}
