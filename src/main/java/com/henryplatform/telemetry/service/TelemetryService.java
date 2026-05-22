package com.henryplatform.telemetry.service;

import com.henryplatform.telemetry.audit.AuditLogger;
import com.henryplatform.telemetry.dto.*;
import com.henryplatform.telemetry.model.TelemetryReading;
import com.henryplatform.telemetry.repository.TelemetryReadingRepository;
import com.henryplatform.telemetry.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryReadingRepository readingRepo;
    private final VehicleRepository vehicleRepo;
    private final HealthScoreService healthScoreService;
    private final AuditLogger auditLogger;

    @Transactional
    public TelemetryReadingResponse registerReading(String vin, TelemetryReadingRequest req) {
        vehicleRepo.findById(vin)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found: " + vin));

        TelemetryReading reading = TelemetryReading.builder()
                .vin(vin)
                // SEC: valores cifrados pelo EncryptedFieldConverter antes de persistir
                .engineTempCelsius(String.valueOf(req.getEngineTempCelsius()))
                .oilLifePercent(String.valueOf(req.getOilLifePercent()))
                .rpm(req.getRpm())
                .fuelLevelPercent(req.getFuelLevelPercent())
                .faultCodes(req.getFaultCodes())
                .build();

        var healthScore = healthScoreService.calculate(vin, reading);
        reading.setHealthScore(healthScore.getScore());

        reading = readingRepo.save(reading);
        auditLogger.log("CREATE_READING", "TelemetryReading", String.valueOf(reading.getId()));
        return toResponse(reading);
    }

    public Page<TelemetryReadingResponse> getReadings(String vin, Pageable pageable) {
        // SEC: verifica se o veículo pertence ao usuário autenticado quando a role é CUSTOMER
        enforceOwnershipIfCustomer(vin);
        auditLogger.log("READ_READINGS", "Vehicle", vin);
        return readingRepo.findByVin(vin, pageable).map(this::toResponse);
    }

    public HealthScoreResponse getHealthScore(String vin) {
        enforceOwnershipIfCustomer(vin);
        TelemetryReading latest = readingRepo.findTopByVinOrderByRecordedAtDesc(vin)
                .orElseThrow(() -> new EntityNotFoundException("No readings found for VIN: " + vin));
        auditLogger.log("READ_HEALTH_SCORE", "Vehicle", vin);
        return healthScoreService.calculate(vin, latest);
    }

    @Transactional
    public TelemetryReadingResponse patchReading(String vin, Long readingId, PatchReadingRequest req) {
        TelemetryReading reading = readingRepo.findById(readingId)
                .filter(r -> r.getVin().equals(vin))
                .orElseThrow(() -> new EntityNotFoundException("Reading not found"));

        if (req.getProcessed() != null) reading.setProcessed(req.getProcessed());

        reading = readingRepo.save(reading);
        auditLogger.log("PATCH_READING", "TelemetryReading", String.valueOf(readingId));
        return toResponse(reading);
    }

    @Transactional
    public void deleteReading(String vin, Long readingId) {
        TelemetryReading reading = readingRepo.findById(readingId)
                .filter(r -> r.getVin().equals(vin))
                .orElseThrow(() -> new EntityNotFoundException("Reading not found"));
        readingRepo.delete(reading);
        auditLogger.log("DELETE_READING", "TelemetryReading", String.valueOf(readingId));
    }

    // SEC: CUSTOMER só acessa dados do veículo vinculado ao seu ownerId — isolamento horizontal
    private void enforceOwnershipIfCustomer(String vin) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        if (isCustomer) {
            vehicleRepo.findByVinAndOwnerId(vin, auth.getName())
                    .orElseThrow(() -> new AccessDeniedException("Access denied to VIN: " + vin));
        }
    }

    private TelemetryReadingResponse toResponse(TelemetryReading r) {
        // SEC: campos podem estar como "ANONYMIZED" após política LGPD — devolvemos null nesses casos
        Double temp = null;
        Integer oil = null;
        try { temp = r.getEngineTempCelsius() != null ? Double.parseDouble(r.getEngineTempCelsius()) : null; } catch (NumberFormatException ignored) {}
        try { oil  = r.getOilLifePercent()    != null ? Integer.parseInt(r.getOilLifePercent())     : null; } catch (NumberFormatException ignored) {}

        return TelemetryReadingResponse.builder()
                .id(r.getId())
                .vin(r.getVin())
                .engineTempCelsius(temp)
                .oilLifePercent(oil)
                .rpm(r.getRpm())
                .fuelLevelPercent(r.getFuelLevelPercent())
                .faultCodes(r.getFaultCodes())
                .healthScore(r.getHealthScore())
                .processed(r.getProcessed())
                .recordedAt(r.getRecordedAt())
                .build();
    }
}
