package com.libratrack.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name="token_blacklist",indexes=@Index(name="idx_token_jti",columnList="token_jti"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TokenBlacklist {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="token_jti",nullable=false,unique=true) private String tokenJti;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
}
