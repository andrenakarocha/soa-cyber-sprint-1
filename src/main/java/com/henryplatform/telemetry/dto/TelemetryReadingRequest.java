package com.henryplatform.telemetry.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TelemetryReadingRequest {

    // SEC: @NotBlank rejeita strings vazias ou de espaços — previne dados inválidos antes de chegar ao banco
    @NotBlank(message = "VIN is required")
    // SEC: padrão VIN estrito — 17 caracteres alfanuméricos — impede injeção via campo de texto livre
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Invalid VIN format")
    private String vin;

    // SEC: faixas numéricas explícitas — valores fora do range físico possível são rejeitados
    @NotNull(message = "Engine temperature is required")
    @DecimalMin(value = "-40.0", message = "Temperature below physical minimum")
    @DecimalMax(value = "150.0", message = "Temperature above physical maximum")
    private Double engineTempCelsius;

    @NotNull(message = "Oil life percent is required")
    @Min(value = 0, message = "Oil life cannot be negative")
    @Max(value = 100, message = "Oil life cannot exceed 100%")
    private Integer oilLifePercent;

    @Min(value = 0, message = "RPM cannot be negative")
    @Max(value = 10000, message = "RPM above physical maximum")
    private Integer rpm;

    @Min(value = 0, message = "Fuel level cannot be negative")
    @Max(value = 100, message = "Fuel level cannot exceed 100%")
    private Integer fuelLevelPercent;

    // SEC: fault codes limitados a 500 chars — previne payloads excessivos que poderiam causar DoS
    @Size(max = 500, message = "Fault codes payload too large")
    private String faultCodes;
}
