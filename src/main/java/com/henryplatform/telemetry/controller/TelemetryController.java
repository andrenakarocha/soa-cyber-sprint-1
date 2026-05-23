package com.henryplatform.telemetry.controller;

import com.henryplatform.telemetry.dto.*;
import com.henryplatform.telemetry.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles/{vin}")
@RequiredArgsConstructor
@Validated
@Tag(name = "Telemetry", description = "Vehicle telemetry data — feeds the HENRY AI assistant with real-time OBD-II readings")
@SecurityRequirement(name = "bearerAuth")
public class TelemetryController {

    // SEC: regex VIN ISO 3779 — 17 chars alfanuméricos excluindo I, O e Q (ambíguos no chassi físico)
    private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";

    private final TelemetryService telemetryService;

    @PostMapping("/readings")
    @Operation(summary = "Register a new OBD-II telemetry reading",
               description = "Receives vehicle telemetry data and calculates the HENRY health score. Requires MECHANIC or higher role.")
    public ResponseEntity<TelemetryReadingResponse> createReading(
            // SEC: @Pattern no path variable impede VINs inválidos mesmo quando não há body para validar
            @PathVariable @Pattern(regexp = VIN_PATTERN, message = "Invalid VIN format") String vin,
            @Valid @RequestBody TelemetryReadingRequest request) {
        // SEC: @Valid dispara Bean Validation antes de qualquer lógica — rejeita input malformado na borda
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(telemetryService.registerReading(vin, request));
    }

    @GetMapping("/readings")
    @Operation(summary = "List telemetry readings for a vehicle",
               description = "Returns paginated reading history. CUSTOMER role is limited to their own vehicles.")
    public ResponseEntity<Page<TelemetryReadingResponse>> getReadings(
            @PathVariable @Pattern(regexp = VIN_PATTERN, message = "Invalid VIN format") String vin,
            Pageable pageable) {
        return ResponseEntity.ok(telemetryService.getReadings(vin, pageable));
    }

    @GetMapping("/health-score")
    @Operation(summary = "Get current HENRY health score",
               description = "Calculates the vehicle health score from the latest telemetry reading. This is the value displayed in the HENRY client app.")
    public ResponseEntity<HealthScoreResponse> getHealthScore(
            @PathVariable @Pattern(regexp = VIN_PATTERN, message = "Invalid VIN format") String vin) {
        return ResponseEntity.ok(telemetryService.getHealthScore(vin));
    }

    @PatchMapping("/readings/{readingId}")
    @Operation(summary = "Update a reading (mark as processed)",
               description = "Used by mechanics to confirm a reading was analyzed. Requires MECHANIC or higher role.")
    public ResponseEntity<TelemetryReadingResponse> patchReading(
            @PathVariable @Pattern(regexp = VIN_PATTERN, message = "Invalid VIN format") String vin,
            @PathVariable Long readingId,
            @RequestBody PatchReadingRequest request) {
        return ResponseEntity.ok(telemetryService.patchReading(vin, readingId, request));
    }

    @DeleteMapping("/readings/{readingId}")
    @Operation(summary = "Delete a telemetry reading",
               description = "Permanently removes a reading. Restricted to FORD_ADMIN role only.")
    public ResponseEntity<Void> deleteReading(
            @PathVariable @Pattern(regexp = VIN_PATTERN, message = "Invalid VIN format") String vin,
            @PathVariable Long readingId) {
        telemetryService.deleteReading(vin, readingId);
        return ResponseEntity.noContent().build();
    }
}
