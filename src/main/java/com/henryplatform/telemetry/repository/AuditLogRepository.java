package com.henryplatform.telemetry.repository;

import com.henryplatform.telemetry.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
