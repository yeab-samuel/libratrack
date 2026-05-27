package com.libratrack.repository;
import com.libratrack.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist,Long> {
    boolean existsByTokenJti(String tokenJti);
    void deleteByExpiresAtBefore(LocalDateTime dt);
}
