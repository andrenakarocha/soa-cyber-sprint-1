package com.henryplatform.telemetry.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// SEC: tabela de auditoria imutável — registros só são inseridos, nunca atualizados ou deletados
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    public void prePersist() {
        this.occurredAt = LocalDateTime.now();
    }
}
