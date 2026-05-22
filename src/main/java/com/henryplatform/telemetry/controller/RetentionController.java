package com.henryplatform.telemetry.controller;

import com.henryplatform.telemetry.service.RetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/retention")
@RequiredArgsConstructor
@Tag(name = "Retention", description = "Data retention and anonymization policy — LGPD compliance")
@SecurityRequirement(name = "bearerAuth")
public class RetentionController {

    private final RetentionService retentionService;

    // SEC: endpoint restrito a FORD_ADMIN — anonimização é operação destrutiva irreversível
    @PreAuthorize("hasRole('FORD_ADMIN')")
    @PostMapping("/anonymize")
    @Operation(summary = "Anonymize readings older than N days",
               description = "Replaces sensitive telemetry fields (temperature, oil life) with ANONYMIZED placeholder. Irreversible. FORD_ADMIN only.")
    public ResponseEntity<Map<String, Object>> anonymize(
            @RequestParam(defaultValue = "90") int olderThanDays) {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = retentionService.anonymizeOldReadings(cutoff);

        return ResponseEntity.ok(Map.of(
                "anonymized", count,
                "cutoff", cutoff.toString(),
                "policy", "LGPD art. 15 — dados não necessários removidos após " + olderThanDays + " dias"
        ));
    }

    @PreAuthorize("hasRole('FORD_ADMIN')")
    @PostMapping("/purge-tokens")
    @Operation(summary = "Purge expired and revoked refresh tokens",
               description = "Removes expired and revoked refresh tokens from the database. FORD_ADMIN only.")
    public ResponseEntity<Map<String, String>> purgeTokens() {
        retentionService.purgeExpiredTokens(LocalDateTime.now());
        return ResponseEntity.ok(Map.of("status", "Expired and revoked tokens purged successfully"));
    }
}
