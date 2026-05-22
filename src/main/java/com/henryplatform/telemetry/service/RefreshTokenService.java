package com.henryplatform.telemetry.service;

import com.henryplatform.telemetry.model.RefreshToken;
import com.henryplatform.telemetry.repository.RefreshTokenRepository;
import com.henryplatform.telemetry.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(String subject, UserRole role) {
        // SEC: token gerado com UUID aleatório criptograficamente seguro
        String rawToken = UUID.randomUUID().toString();

        RefreshToken entity = RefreshToken.builder()
                // SEC: armazena apenas o hash SHA-256 — token original nunca persiste no banco
                .tokenHash(sha256(rawToken))
                .subject(subject)
                .role(role.name())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public RefreshToken validateAndRotate(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AccessDeniedException("Invalid refresh token"));

        if (token.getRevoked() || token.isExpired()) {
            // SEC: token revogado ou expirado é deletado imediatamente — previne reuso
            refreshTokenRepository.delete(token);
            throw new AccessDeniedException("Refresh token expired or revoked");
        }

        // SEC: rotação obrigatória — cada uso invalida o token anterior e emite um novo
        //      previne que um token roubado seja usado indefinidamente
        refreshTokenRepository.delete(token);
        return token;
    }

    @Transactional
    public void revokeAllForSubject(String subject) {
        // SEC: revogação em massa para logout ou detecção de comprometimento de conta
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getSubject().equals(subject))
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
