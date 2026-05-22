package com.henryplatform.telemetry.audit;

import com.henryplatform.telemetry.model.AuditLog;
import com.henryplatform.telemetry.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// SEC: auditoria centralizada — toda ação sensível gera registro rastreável com actor, ação e alvo
@Component
@RequiredArgsConstructor
public class AuditLogger {

    // SEC: logger separado para auditoria — permite rotear para SIEM independente do log de aplicação
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String resourceType, String resourceId) {
        String actor = resolveActor();
        // SEC: formato estruturado permite queries precisas em ferramentas SIEM
        auditLog.info("action={} actor={} resourceType={} resourceId={}",
                action, actor, resourceType, resourceId);
        // SEC: persiste no banco para trilha de auditoria consultável e imutável
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build());
    }

    public void logFailure(String action, String resourceType, String reason) {
        String actor = resolveActor();
        // SEC: falhas de acesso logadas — padrões anormais podem indicar ataque em curso
        auditLog.warn("action={} actor={} resourceType={} status=DENIED reason={}",
                action, actor, resourceType, reason);
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action + "_DENIED")
                .resourceType(resourceType)
                .resourceId(reason)
                .build());
    }

    private String resolveActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "ANONYMOUS";
        }
        return auth.getName();
    }
}
