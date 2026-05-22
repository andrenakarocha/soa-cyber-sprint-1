package com.henryplatform.telemetry.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// SEC: refresh tokens persistidos no banco permitem revogação individual — diferente de JWT de acesso
//      que são stateless e não podem ser invalidados antes do vencimento
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SEC: token armazenado como hash SHA-256 — mesmo com dump do banco, o token original não é recuperável
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // SEC: revogação explícita permite invalidar tokens antes do vencimento (logout, comprometimento)
    @Column(name = "revoked", nullable = false)
    private Boolean revoked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.revoked = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
