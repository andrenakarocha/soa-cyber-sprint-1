package com.henryplatform.telemetry.service;

import com.henryplatform.telemetry.audit.AuditLogger;
import com.henryplatform.telemetry.model.TelemetryReading;
import com.henryplatform.telemetry.repository.RefreshTokenRepository;
import com.henryplatform.telemetry.repository.TelemetryReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetentionService {

    private final TelemetryReadingRepository readingRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final AuditLogger auditLogger;

    @Value("${security.retention.max-days:90}")
    private int maxRetentionDays;

    // SEC: job automático de anonimização — executado toda madrugada às 02:00
    //      garante que dados pessoais não acumulam além da política de retenção (LGPD art. 15)
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runRetentionPolicy() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(maxRetentionDays);
        anonymizeOldReadings(cutoff);
        purgeExpiredTokens(cutoff);
        auditLogger.log("RETENTION_POLICY_EXECUTED", "RetentionService",
                "cutoff=" + cutoff + " maxDays=" + maxRetentionDays);
    }

    @Transactional
    public int anonymizeOldReadings(LocalDateTime cutoff) {
        List<TelemetryReading> old = readingRepo.findAll().stream()
                .filter(r -> r.getRecordedAt().isBefore(cutoff))
                .toList();

        for (TelemetryReading r : old) {
            // SEC: pseudonimização — substitui dados sensíveis por placeholder irreversível
            //      VIN mantido para rastreabilidade operacional, mas dados de saúde são apagados
            r.setEngineTempCelsius("ANONYMIZED");
            r.setOilLifePercent("ANONYMIZED");
            r.setFaultCodes(null);
            readingRepo.save(r);
        }

        auditLogger.log("ANONYMIZED_READINGS", "TelemetryReading",
                "count=" + old.size() + " olderThan=" + cutoff);
        return old.size();
    }

    @Transactional
    public void purgeExpiredTokens(LocalDateTime cutoff) {
        // SEC: refresh tokens expirados ou revogados são removidos fisicamente para evitar acúmulo
        refreshTokenRepo.deleteExpiredAndRevoked(cutoff);
    }
}
