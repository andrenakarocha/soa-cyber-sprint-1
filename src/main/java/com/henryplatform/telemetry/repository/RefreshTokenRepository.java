package com.henryplatform.telemetry.repository;

import com.henryplatform.telemetry.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // SEC: limpeza de tokens expirados — evita acúmulo de dados desnecessários (política de retenção)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff OR rt.revoked = true")
    void deleteExpiredAndRevoked(LocalDateTime cutoff);
}
